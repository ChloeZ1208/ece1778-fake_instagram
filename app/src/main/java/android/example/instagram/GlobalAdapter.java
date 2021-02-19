package android.example.instagram;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.List;

public class GlobalAdapter extends RecyclerView.Adapter<GlobalAdapter.ViewHolder> {

    List<String> photo_paths;
    Context ctx;

    StorageReference storageRef = FirebaseStorage.getInstance().getReference();;
    StorageReference pathReference;


    public GlobalAdapter(Context ctx, List<String> photos) {
        this.photo_paths = photos;
        this.ctx = ctx;
    }

    @NonNull
    @Override
    public GlobalAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(ctx).inflate(R.layout.photo_view, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        pathReference = storageRef.child(photo_paths.get(position));
        pathReference.getBytes(1024 * 1024).addOnSuccessListener(new OnSuccessListener<byte[]>() {
            @Override
            public void onSuccess(byte[] bytes) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                holder.photo_view_item.setImageBitmap(bitmap);
                Log.d("storage", photo_paths.get(position));
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Log.w("Downloaded photos", "Error occurred during downloading.");
            }
        });
    }

    @Override
    public int getItemCount() {
        //Log.d("Global_adapter", String.valueOf(photo_paths.size()));
        return photo_paths.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView photo_view_item;

        public ViewHolder(View itemView) {
            super(itemView);
            photo_view_item = itemView.findViewById(R.id.photoView);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Get the position of the item that was clicked.
                    int mPosition = getLayoutPosition();
                    Intent intent = new Intent(ctx,  CommentsActivity.class);
                    intent.putExtra("click_path", photo_paths.get(mPosition));
                    ctx.startActivity(intent);
                }
            });

        }
    }
}
