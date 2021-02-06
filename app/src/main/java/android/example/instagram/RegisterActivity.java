package android.example.instagram;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final String TAG = "Signup";
    private ImageView Profile;
    private Button Signup;
    private Bitmap bitmapPic;
    private EditText userEmail, userPassword, userConfirmPassword, userName, userBio;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db_users;
    private ProgressDialog progressDialog;
    private StorageReference storageReference;
    private ByteArrayOutputStream bs;
    private String userUID;
    private String email, password, confirmPassword, username, bio;
    private String user_username, user_bio, user_displayPicPath;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        //set title for activity_register(register)
        setTitle(R.string.register_title);
        setContentView(R.layout.activity_register);

        setupViews();

        mAuth = FirebaseAuth.getInstance();
        db_users = FirebaseFirestore.getInstance();
        progressDialog = new ProgressDialog(this);
        storageReference = FirebaseStorage.getInstance().getReference();


        userEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
            @Override
            public void afterTextChanged(Editable s) {
                if(!isEmailValid(s)){
                    userEmail.setError("Invalid email address");
                }
            }
        });

        userConfirmPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
            @Override
            public void afterTextChanged(Editable s) {
                password = userPassword.getText().toString().trim();
                confirmPassword = userConfirmPassword.getText().toString().trim();
                if(password.length() > 0 && confirmPassword.length() > 0) {
                    if(!confirmPassword.equals(password)) {
                        userConfirmPassword.setError("Password does not match");
                    }
                }
            }
        });

        Signup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (validate()) {
                    progressDialog.setMessage("Loading...");
                    progressDialog.show();

                    mAuth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener(RegisterActivity.this, new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    if (task.isSuccessful()) {
                                        // Sign in success, upload data to firestore
                                        Log.d(TAG, "createUserWithEmail:success");
                                        FirebaseUser user = mAuth.getCurrentUser();
                                        userUID = user.getUid();
                                        // Save username, bio, displayPicPath to firestore "users"
                                        saveData();
                                        // Save display pic to Storage
                                        savePic();
                                    } else {
                                        // If sign in fails, display a message to the user.
                                        Log.w(TAG, "createUserWithEmail:failure", task.getException());
                                        Toast.makeText(RegisterActivity.this, "Signup failed.",
                                                Toast.LENGTH_SHORT).show();
                                    }

                                }
                            });
                }
            }
        });
    }

    // if user is signed in, redirect to profile page
    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null){
            finish();
            startActivity(new Intent(RegisterActivity.this, ProfileActivity.class));
        }
    }

    // invokes an intent to capture a photo
    public void openCamera(View view) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Couldn't get camera", Toast.LENGTH_LONG).show();
        }
    }

    // retrieves the image and displays it in profile
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap originBitmap = (Bitmap) extras.get("data");
            // downscale the captured image to 1024*1024 resolution
            bitmapPic = Bitmap.createScaledBitmap(originBitmap, 1024, 1024, true);
            Profile.setImageBitmap(bitmapPic);
        }
    }

    private void setupViews() {
        Profile = findViewById(R.id.default_profile);
        Signup = findViewById(R.id.reg_signup_btn);
        userEmail = findViewById(R.id.reg_email_text);
        userPassword = findViewById(R.id.reg_password_text);
        userConfirmPassword = findViewById(R.id.reg_confirm_password_text);
        userName = findViewById(R.id.reg_username_text);
        userBio = findViewById(R.id.reg_bio_text);
    }

    // validate all the information are filled
    private Boolean validate(){
        Boolean result = false;

        email = userEmail.getText().toString().trim();
        username = userName.getText().toString().trim();
        bio = userBio.getText().toString().trim();

        if(password.isEmpty() || email.isEmpty() || username.isEmpty() || confirmPassword.isEmpty() || bio.isEmpty()){
            Toast.makeText(RegisterActivity.this, "Please enter all the details", Toast.LENGTH_SHORT).show();
        }
        else{
            result = true;
        }
        return result;
    }

    private boolean isEmailValid(CharSequence email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private void saveData() {
        // Create a new user with username, bio and display profile pic path
        user_username = userName.getText().toString();
        user_bio = userBio.getText().toString();
        user_displayPicPath = userUID + "/" + "displayPic.jpeg";

        Map<String, Object> user = new HashMap<>();
        user.put("username", user_username);
        user.put("bio", user_bio);
        user.put("displayPicPath", user_displayPicPath);

        // Add a new document with user_uid(from authentication)
        db_users.collection("users")
                .document(userUID)
                .set(user);
    }

    private void savePic() {
        StorageReference imageReference = storageReference.child(userUID).child("displayPic.jpeg");  //Users uid/displayPic.jpg
        transferBitmap();
        byte[] data = bs.toByteArray();
        UploadTask uploadTask = imageReference.putBytes(data);
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(RegisterActivity.this, "Upload failed!", Toast.LENGTH_SHORT).show();
            }
        }).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                startActivity(new Intent(RegisterActivity.this, ProfileActivity.class));
                progressDialog.dismiss();
                Toast.makeText(RegisterActivity.this, "Upload successful!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // encode the images into a compressed format
    private void transferBitmap() {
        if (bitmapPic != null ) {
            bs = new ByteArrayOutputStream();
            bitmapPic.compress(Bitmap.CompressFormat.JPEG, 80, bs);
        }
        else {
            Toast.makeText(RegisterActivity.this, "Please take your profile picture.",
                    Toast.LENGTH_SHORT).show();
        }
    }
}



