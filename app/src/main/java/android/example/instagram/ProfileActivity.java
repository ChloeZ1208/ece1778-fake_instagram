package android.example.instagram;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "Get profile data";
    private ImageView profileThumbnail;
    private TextView username;
    private TextView bio;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db_users;
    private String userUID;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        progressDialog = new ProgressDialog(this);

        setTitle("Profile");
        setContentView(R.layout.activity_profile);

        profileThumbnail = findViewById(R.id.user_profile);
        username = findViewById(R.id.username);
        bio = findViewById(R.id.bio);

        mAuth = FirebaseAuth.getInstance();
        db_users = FirebaseFirestore.getInstance();

        displayProfilePic();

    }


    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null){
            userUID = currentUser.getUid();
            // read data from firestore using userUID and set text
            readData();
        }
    }

    // create menu (logout)
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    // logout user
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

    private void displayProfilePic() {
        if(getIntent().hasExtra("byteArray")) {
            Bitmap b = BitmapFactory.decodeByteArray(
                    getIntent().getByteArrayExtra("byteArray"),0,getIntent().getByteArrayExtra("byteArray").length);
            profileThumbnail.setImageBitmap(b);
        }
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
                                Log.d(TAG, document.getId() + " => " + document.getData());
                        } else {
                            Log.w(TAG, "Error getting documents.", task.getException());
                        }
                    }
                });
    }

}