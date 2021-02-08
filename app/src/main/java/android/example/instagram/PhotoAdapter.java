package android.example.instagram;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

public class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.ViewHolder> {

    List<String> photos;
    String userUID;
    Context ctx;

    StorageReference storageRef = FirebaseStorage.getInstance().getReference();;
    StorageReference pathReference;


    public PhotoAdapter(Context ctx, List<String> photos, String userUID) {
        this.photos = photos;
        this.userUID = userUID;
        this.ctx = ctx;
    }

    @NonNull
    @Override
    public PhotoAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(ctx).inflate(R.layout.photo_view, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String path = userUID + "/" + photos.get(position) + ".jpeg";
        pathReference = storageRef.child(path);
        pathReference.getBytes(1024 * 1024).addOnSuccessListener(new OnSuccessListener<byte[]>() {
            @Override
            public void onSuccess(byte[] bytes) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                holder.photo_view_item.setImageBitmap(bitmap);
                Log.d("storage", path);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Log.w("Uploaded photos", "Error occurred during downloading.");
            }
        });
    }

    @Override
    public int getItemCount() {
        Log.d("adapter", String.valueOf(photos.size()));
        return photos.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        boolean status = false;
        ImageView photo_view_item;

        public ViewHolder(View itemView) {
            super(itemView);
            photo_view_item =itemView.findViewById(R.id.photoView);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    /*
                    * TODO: click-full screen
                     */
                    // Get the position of the item that was clicked.
                    int mPosition = getLayoutPosition();
                    String path = userUID + "/" + photos.get(mPosition) + ".jpeg";
                    Intent intent = new Intent(ctx,  FullscreenActivity.class);
                    intent.putExtra("click_path", path);
                    ctx.startActivity(intent);
                }
            });

        }
    }


}
