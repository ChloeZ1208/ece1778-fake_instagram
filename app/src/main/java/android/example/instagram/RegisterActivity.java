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
    private Bitmap bitmapProfile;
    private EditText userEmail, userPassword, userConfirmPassword, userName, userBio;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db_users;
    private ProgressDialog progressDialog;
    private String userUID;
    private String email, password, confirmPassword, username, bio;
    private String user_username, user_bio;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        //set title for main activity(login)
        setTitle(R.string.register_title);
        setContentView(R.layout.activity_register);

        setupViews();

        mAuth = FirebaseAuth.getInstance();
        db_users = FirebaseFirestore.getInstance();
        progressDialog = new ProgressDialog(this);

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
                                        // Save username&bio to firestore "users"
                                        saveData();
                                        // Transfer profile bitmap to profile page(temporary)
                                        transferBitmap();
                                        progressDialog.dismiss();
                                    } else {
                                        // If sign in fails, display a message to the user.
                                        Log.w(TAG, "createUserWithEmail:failure", task.getException());
                                        Toast.makeText(RegisterActivity.this, "Signup failed.",
                                                Toast.LENGTH_SHORT).show();
                                        progressDialog.dismiss();
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
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            bitmapProfile = imageBitmap;
            Profile.setImageBitmap(imageBitmap);
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

    // Transfer profile bitmap to profile page(temporary)
    private void transferBitmap() {
        Intent intent = new Intent(RegisterActivity.this, ProfileActivity.class);
        if (bitmapProfile != null ) {
            ByteArrayOutputStream bs = new ByteArrayOutputStream();
            bitmapProfile.compress(Bitmap.CompressFormat.PNG, 50, bs);
            intent.putExtra("byteArray", bs.toByteArray());
        }
        startActivity(intent);
    }

    private void saveData() {
        // Create a new user with username, bio and display profile pic path
        user_username = userName.getText().toString();
        user_bio = userBio.getText().toString();

        Map<String, Object> user = new HashMap<>();
        user.put("username", user_username);
        user.put("bio", user_bio);
        //user.put("displayPicPath", user_PicPath);

        // Add a new document with user_uid(from authentication)
        db_users.collection("users")
                .document(userUID)
                .set(user);
    }
}



