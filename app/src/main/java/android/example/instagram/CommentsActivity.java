package android.example.instagram;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class CommentsActivity extends AppCompatActivity {

    private ImageView full_screen_image;
    private Button close_btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comments);

        full_screen_image = findViewById(R.id.fullScreenImage);
        close_btn = findViewById(R.id.closeBtn);

        Intent intent = getIntent();
        String clickPath = intent.getStringExtra("click_path");
        StorageReference storageRef = FirebaseStorage.getInstance().getReference();;
        StorageReference pathReference;
        pathReference = storageRef.child(clickPath);
        pathReference.getBytes(1024 * 1024).addOnSuccessListener(new OnSuccessListener<byte[]>() {
            @Override
            public void onSuccess(byte[] bytes) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                full_screen_image.setImageBitmap(bitmap);
            }
        });

        close_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(CommentsActivity.this, ProfileActivity.class));
            }
        });
    }
}