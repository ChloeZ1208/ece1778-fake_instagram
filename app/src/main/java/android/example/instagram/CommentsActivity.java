package android.example.instagram;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Notification;
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
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
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
    private String currUsername; // current username
    private MaterialToolbar topAppBar;

    private String deletePhotoDocument;
    private List<String> deleteCommentDocument;

    private List<String> comments; // adapter
    private List<String> commentUsername; // adapter
    private List<String> commentProfilePath; // adapter

    private RecyclerView recyclerComment;
    private CommentAdapter adapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comments);

        setTitle("Comments");
        setContentView(R.layout.activity_comments);

        initializeViews();

        GridLayoutManager gridLayoutManager = new GridLayoutManager(CommentsActivity.this,1,GridLayoutManager.VERTICAL,false);
        recyclerComment.setLayoutManager(gridLayoutManager);
        
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

        // download photo
        downloadPhoto();
        // download caption
        downloadCaption();
        // get current login username
        getUsername();

        // set navigation back
        topAppBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }

        });

        // set delete
        if (currUid.equals(photoOwnerUid)) {
            topAppBar.inflateMenu(R.menu.delete_menu);
            topAppBar.setOnMenuItemClickListener(menuItem -> {
                if(menuItem.getItemId() == R.id.deleteMenu) {
                    deletePhotosFirestore();
                    deleteCommentsFirestore();
                    deletePhotoStorage();
                    return true;
                }
                return false;
            });
        }

        // get comments and profile path
        getComments();

        commentPostBtn.setOnClickListener(v -> {
            newComment = commentTxt.getText().toString();
            if (newComment.isEmpty()) {
                Toast.makeText(CommentsActivity.this, "Please add a comment", Toast.LENGTH_SHORT).show();
            } else {
                addComment();
                commentTxt.getText().clear();
            }
        });
    }


    private void getComments() {
        comments = new ArrayList<>();
        commentUsername = new ArrayList<>();
        commentProfilePath = new ArrayList<>();
        Query first = db_users.collection(timestampPhoto).orderBy("timestampComment", Query.Direction.ASCENDING);
            first.get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                comments.add(document.getString("comment"));
                                commentUsername.add(document.getString("username"));
                                String path = document.getString("commentUserUid") + "/" + "displayPic.jpeg";
                                commentProfilePath.add(path);
                                Log.d("comments", document.getString("comment"));
                            }
                            adapter = new CommentAdapter(CommentsActivity.this, comments, commentProfilePath, commentUsername);
                            recyclerComment.setAdapter(adapter);
                        }
                    }
                });
    }

    private void getUsername() {
        db_users.collection("users")
                .document(currUid)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            currUsername = document.getString("username");
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
                                deletePhotoDocument = document.getId();
                                Log.d("caption", document.getId() + " => " + document.getData());
                            }
                        }
                    }
                });
    }

    public void addComment() {
        timestampComment = new SimpleDateFormat("yyyyMMddHHmmss", Locale.CHINA).format(new Date());
        Map<String, Object> comment = new HashMap<>();
        //comment.put("timestampPhoto", timestampPhoto);
        comment.put("timestampComment", timestampComment);
        comment.put("commentUserUid", currUid);
        comment.put("comment", newComment);
        comment.put("username", currUsername);
        db_users.collection(timestampPhoto)
                .add(comment);
        //
        int index = comments.size();
        comments.add(index, newComment);
        String path = currUid + "/" + "displayPic.jpeg";
        commentProfilePath.add(index, path);
        commentUsername.add(index, currUsername);
        adapter.notifyItemInserted(index);
    }

    private void deletePhotosFirestore() {
        Log.d("photo id",  deletePhotoDocument);
        db_users.collection("photos").document(deletePhotoDocument)
                .delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d("photo firestore", "Photo firestore successfully deleted!");
                    }
                });
    }

    private void deletePhotoStorage() {
        pathReference = storageRef.child(clickPath);
        pathReference.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.d("photo storage", "Photo storage successfully deleted!");
                //onBackPressed();
                startActivity(new Intent(CommentsActivity.this, ProfileActivity.class));
            }
        });
    }
    /*
    * TODO: delete documents-onbackpressed norefresh
     */
    private void deleteCommentsFirestore() {
        db_users.collection(timestampPhoto)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            db_users.collection(timestampPhoto).document(document.getId()).delete();
                        }
                        Log.d("photo comments", "Photo comments successfully deleted!");
                    }
                });
    }

    private void initializeViews() {
        clickPhoto = findViewById(R.id.click_photo);
        captionTxt = findViewById(R.id.caption_txt);
        commentTxt = findViewById(R.id.comment_txt);
        commentPostBtn = findViewById(R.id.comment_post_btn);
        topAppBar = findViewById(R.id.top_app_bar);
        recyclerComment = findViewById(R.id.recycler_comment);
    }
}