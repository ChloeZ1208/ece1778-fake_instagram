package android.example.instagram;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GlobalFeedActivity extends AppCompatActivity {
    private List<String> photo_paths;
    private GlobalAdapter adapter;
    private RecyclerView recyclerFeed;
    private FirebaseFirestore db_photos;
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_global_feed);

        db_photos = FirebaseFirestore.getInstance();

        recyclerFeed = findViewById(R.id.recycler_feed);
        bottomNav = findViewById(R.id.bottom_navigation_global);

        GridLayoutManager gridLayoutManager = new GridLayoutManager(GlobalFeedActivity.this,1,GridLayoutManager.VERTICAL,false);
        recyclerFeed.setLayoutManager(gridLayoutManager);
        downloadPhotos();

        // bottom navigation
        bottomNav.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch(item.getItemId()) {
                    case R.id.profile_page:
                        startActivity(new Intent(GlobalFeedActivity.this, ProfileActivity.class));
                        break;
                    case R.id.global_feed_page:
                        break;
                }
                return false;
            }
        });
    }

    private void downloadPhotos(){
        photo_paths = new ArrayList<>();
        db_photos.collection("photos")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String path = document.getString("uid") + "/" + document.getString("timestamp") + ".jpeg";
                                photo_paths.add(path);
                                Log.d("path", path);
                            }
                            adapter = new GlobalAdapter(GlobalFeedActivity.this, photo_paths);
                            recyclerFeed.setAdapter(adapter);

                        } else {
                            Log.d("Downloaded photos", "Error getting documents: ", task.getException());
                        }
                    }
                });
    }

}