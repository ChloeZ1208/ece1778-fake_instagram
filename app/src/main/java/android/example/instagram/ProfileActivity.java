package android.example.instagram;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.GridLayoutManager;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.media.ExifInterface;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final String TAG = "Get profile data";
    private ImageView profileThumbnail;
    private TextView username;
    private TextView bio;
    private BottomNavigationView bottomNav;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db_users;
    private String userUID, timeStamp;
    private Bitmap bitmapPhoto;
    private ByteArrayOutputStream baos;
    private byte[] currentPhotoData;
    private ProgressDialog progressDialog;
    private StorageReference storageRef;
    private StorageReference pathReference;

    private GlobalAdapter adapter;
    private RecyclerView recyclerProfile;

    private List<String> photo_time;

    final long ONE_MEGABYTE = 1024 * 1024;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        //set title for activity_profile(profile)
        setTitle("Profile");
        setContentView(R.layout.activity_profile);

        initializeView();
        GridLayoutManager gridLayoutManager = new GridLayoutManager(ProfileActivity.this,3,GridLayoutManager.VERTICAL,false);
        recyclerProfile.setLayoutManager(gridLayoutManager);

        mAuth = FirebaseAuth.getInstance();
        db_users = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference();


        bottomNav.getMenu().findItem(R.id.profile_page).setChecked(true);

        // bottom navigation
        bottomNav.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch(item.getItemId()) {
                    case R.id.profile_page:
                        break;
                    case R.id.global_feed_page:
                        startActivity(new Intent(ProfileActivity.this, GlobalFeedActivity.class));
                        break;
                }
                return true;
            }
        });

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                try {
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(ProfileActivity.this, "Couldn't get camera", Toast.LENGTH_LONG).show();
                }
            }
        });
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null){
            userUID = currentUser.getUid();
            // read data from firestore using userUID and set text
            readData();
            // download display pic from Storage and display
            downloadPic();
            // download all uploaded photos of the current user
            downloadPhotos();
        }
    }


    // retrieves the image - compress it into desirable format&size - upload
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap originBitmap = (Bitmap) extras.get("data");
            bitmapPhoto = Bitmap.createScaledBitmap(originBitmap, 1024, 1024, true);
            timeStamp = new SimpleDateFormat("yyyyMMddHHmmss", Locale.CHINA).format(new Date());
            baos = new ByteArrayOutputStream();
            bitmapPhoto.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            currentPhotoData = baos.toByteArray();

            //Log.d("cameraPhoto", String.valueOf(currentPhotoData.length));

            Intent intent = new Intent(ProfileActivity.this, PhotoCaptionActivity.class);
            intent.putExtra("byteArray", currentPhotoData);
            intent.putExtra("uid", userUID);
            intent.putExtra("timestamp", timeStamp);
            //intent.putExtra("rotationDegree", rotationDegree);
            startActivity(intent);

        }
    }


    // create menu (logout)
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    // menu-logout user
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.logoutMenu) {
            startActivity(new Intent(ProfileActivity.this, MainActivity.class));
            // firebase authentication logout
            FirebaseAuth.getInstance().signOut();
            Toast.makeText(this, "Successfully log out", Toast.LENGTH_LONG).show();
        }
        return super.onOptionsItemSelected(item);
    }


    // download Profile photo as activity starts
    private void downloadPic() {
        String displayPicPath = userUID + "/" + "displayPic.jpeg";
        pathReference = storageRef.child(displayPicPath);
        pathReference.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
            @Override
            public void onSuccess(byte[] bytes) {
                Bitmap displayBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                profileThumbnail.setImageBitmap(displayBitmap);
                Log.d("profile", "Successfully downloaded profile pic");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Log.w("display pic", "Error occurred during downloading");
            }
        });
    }

    private void readData() {
        db_users.collection("users")
                .document(userUID)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                                String userName = document.getString("username");
                                String userBio = document.getString("bio");
                                username.setText(userName);
                                bio.setText(userBio);
                                Log.d("readData", document.getId() + " => " + document.getData());
                        } else {
                            Log.w(TAG, "Error getting documents.", task.getException());
                        }
                    }
                });
    }

    // download uploaded photos as activity starts

    private void downloadPhotos(){
        // query photos(firestore) to get StorageReference
        photo_time = new ArrayList<>();
        List<String> photo_path = new ArrayList<>();
        db_users.collection("photos")
                .whereEqualTo("uid", userUID)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                photo_time.add(document.getString("timestamp"));
                                Log.d("timestamp", document.getString("timestamp"));
                            }
                            // Sorting storageReference in chronological older
                            Collections.sort(photo_time, Collections.reverseOrder());
                            for (String time: photo_time) {
                                String path = userUID + "/" + time + ".jpeg";
                                photo_path.add(path);
                            }

                            // create recycler view-adpater(data)-connect-grid layout manage
                            //Log.d("adapter0", String.valueOf(photo_time.size()));
                            adapter = new GlobalAdapter(ProfileActivity.this, photo_path);
                            recyclerProfile.setAdapter(adapter);

                        } else {
                            Log.d("Uploaded photos", "Error getting documents: ", task.getException());
                        }
                    }
                });
    }

    private void initializeView() {
        recyclerProfile = findViewById(R.id.recycler_profile);
        profileThumbnail = findViewById(R.id.user_profile);
        username = findViewById(R.id.username);
        bio = findViewById(R.id.bio);
        bottomNav = findViewById(R.id.bottom_navigation);
    }

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 0);
        ORIENTATIONS.append(Surface.ROTATION_90, 90);
        ORIENTATIONS.append(Surface.ROTATION_180, 180);
        ORIENTATIONS.append(Surface.ROTATION_270, 270);
    }

    /**
     * Get the angle by which an image must be rotated given the device's current
     * orientation.
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private int getRotationCompensation(String cameraId, Activity activity, boolean isFrontFacing)
            throws CameraAccessException {
        // Get the device's current rotation relative to its "native" orientation.
        // Then, from the ORIENTATIONS table, look up the angle the image must be
        // rotated to compensate for the device's rotation.
        int deviceRotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int rotationCompensation = ORIENTATIONS.get(deviceRotation);

        // Get the device's sensor orientation.
        CameraManager cameraManager = (CameraManager) activity.getSystemService(CAMERA_SERVICE);
        int sensorOrientation = cameraManager
                .getCameraCharacteristics(cameraId)
                .get(CameraCharacteristics.SENSOR_ORIENTATION);

        if (isFrontFacing) {
            rotationCompensation = (sensorOrientation + rotationCompensation) % 360;
        } else { // back-facing
            rotationCompensation = (sensorOrientation - rotationCompensation + 360) % 360;
        }
        return rotationCompensation;
    }

}