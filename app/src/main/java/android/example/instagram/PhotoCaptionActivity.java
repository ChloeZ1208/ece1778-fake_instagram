package android.example.instagram;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;
import java.util.Map;

public class PhotoCaptionActivity extends AppCompatActivity {

    private String userUID;
    private String timeStamp;
    private String caption;
    private byte[] currentPhotoData;
    private ImageView cameraPhoto;
    private EditText captionTxt;
    private Button cancelBtn;
    private Button postBtn;
    private Switch hashTag;
    private ProgressDialog progressDialog;
    private StorageReference storageRef;
    private FirebaseFirestore db_users;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_caption);

        progressDialog = new ProgressDialog(PhotoCaptionActivity.this);
        storageRef = FirebaseStorage.getInstance().getReference();
        db_users = FirebaseFirestore.getInstance();

        // set views
        cameraPhoto = findViewById(R.id.camera_photo);
        cancelBtn = findViewById(R.id.cancel_btn);
        postBtn = findViewById(R.id.post_btn);
        captionTxt = findViewById(R.id.caption);
        hashTag = findViewById(R.id.hashtag);

        // get all the information of camera photo
        Intent intent = getIntent();
        userUID = intent.getStringExtra("uid");
        timeStamp = intent.getStringExtra("timestamp");
        currentPhotoData = intent.getByteArrayExtra("byteArray");
        // set image bitmap
        Bitmap cameraBitmap = BitmapFactory.decodeByteArray(currentPhotoData,0,currentPhotoData.length);
        cameraPhoto.setImageBitmap(cameraBitmap);

        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(PhotoCaptionActivity.this, ProfileActivity.class));
            }
        });

        postBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                caption = captionTxt.getText().toString();
                if (caption == null) {
                    Toast.makeText(PhotoCaptionActivity.this, "Please add a caption", Toast.LENGTH_SHORT).show();
                } else {
                    savePhotoPath();
                    savePhoto();
                }

            }
        });
    }

    // save uploaded photo to storage
    private void savePhoto() {
        progressDialog.setMessage("Uploading...");
        progressDialog.setMax(1);
        progressDialog.setProgressStyle(1);
        progressDialog.incrementProgressBy(2);
        progressDialog.show();
        String photoName = timeStamp + ".jpeg";
        StorageReference photoReference = storageRef.child(userUID).child(photoName);
        UploadTask uploadTask = photoReference.putBytes(currentPhotoData);
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(PhotoCaptionActivity.this, "Upload failed!", Toast.LENGTH_SHORT).show();
            }
        }).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                progressDialog.dismiss();
                Toast.makeText(PhotoCaptionActivity.this, "Upload successful!", Toast.LENGTH_SHORT).show();
                // Notify adapter new photo inserted
                //photo_time.add(0, timeStamp);
                //adapter.notifyItemInserted(0);
                startActivity(new Intent(PhotoCaptionActivity.this, ProfileActivity.class));
            }
        });
    }

    // save photo Path to firebase
    private void savePhotoPath() {
        Map<String, Object> photo = new HashMap<>();
        photo.put("uid", userUID);
        photo.put("timestamp", timeStamp);
        photo.put("caption", caption);

        db_users.collection("photos")
                .add(photo);
    }
}