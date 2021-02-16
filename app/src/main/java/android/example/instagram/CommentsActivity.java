package android.example.instagram;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CommentsActivity extends AppCompatActivity {

    private ImageView clickPhoto;
    private TextView captionTxt;
    private EditText commentTxt;
    private String newComment;
    private ImageButton commentPostBtn;
    private String clickPath; // "userUID/timestamp.jpeg"
    private String photoOwnerUid; // photo owner uid
    private String timestampPhoto; // photo timestamp
    private String timestampComment;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db_users;
    private StorageReference pathReference;
    private StorageReference storageRef;
    private String currUid; // current login user uid


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comments);

        setTitle("Comments");
        setContentView(R.layout.activity_comments);

        clickPhoto = findViewById(R.id.click_photo);
        captionTxt = findViewById(R.id.caption_txt);
        commentTxt = findViewById(R.id.comment_txt);
        commentPostBtn = findViewById(R.id.comment_post_btn);

        // Get photo, photo timestamp and photo owner uid from clickPath
        Intent intent = getIntent();
        clickPath = intent.getStringExtra("click_path");
        String[] s = clickPath.split("/");
        photoOwnerUid = s[0];
        String[] ss = s[1].split(".jpeg");
        timestampPhoto = ss[0];

        db_users = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference();
        // Get current user uid, not photo owner uid
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null) {
            currUid = currentUser.getUid();
        }
        //Log.d("uid", currUid);
        //Log.d("uid", photoOwnerUid);

        // download photo
        downloadPhoto();

        // download caption
        downloadCaption();

        commentPostBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                newComment = commentTxt.getText().toString();
                if (newComment.isEmpty()) {
                    Toast.makeText(CommentsActivity.this, "Please add a comment", Toast.LENGTH_SHORT).show();
                } else {
                    addComment();
                    commentTxt.getText().clear();
                }
            }
        });
    }

    public void downloadPhoto() {
        pathReference = storageRef.child(clickPath);
        pathReference.getBytes(1024 * 1024).addOnSuccessListener(new OnSuccessListener<byte[]>() {
            @Override
            public void onSuccess(byte[] bytes) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                clickPhoto.setImageBitmap(bitmap);
            }
        });
    }

    public void downloadCaption() {
        db_users.collection("photos")
                .whereEqualTo("timestamp", timestampPhoto)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String caption = document.getString("caption");
                                captionTxt.setText(caption);
                                Log.d("caption", document.getId() + " => " + document.getData());
                            }
                        }
                    }
                });
    }

    public void addComment() {
        timestampComment = new SimpleDateFormat("yyyyMMddHHmmss", Locale.CHINA).format(new Date());
        Map<String, Object> comment = new HashMap<>();
        comment.put("timestampPhoto", timestampPhoto);
        comment.put("timestampComment", timestampComment);
        comment.put("commentUserUid", currUid);
        comment.put("comment", newComment);
        db_users.collection("comments")
                .add(comment);
    }

    // create menu (delete)
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (currUid.equals(photoOwnerUid)) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.delete_menu, menu);
            return true;
        }
        return false;
    }

    // menu-delete post
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.deleteMenu) {
            /*
            * TODO: delete post
             */
            Toast.makeText(this, "Successfully delete this post", Toast.LENGTH_LONG).show();
        }
        return super.onOptionsItemSelected(item);
    }
}