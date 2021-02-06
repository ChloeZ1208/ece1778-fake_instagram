package android.example.instagram;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
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
    LayoutInflater inflater;


    public PhotoAdapter(Context ctx, List<String> photos, String userUID) {
        this.photos = photos;
        this.userUID = userUID;
        this.inflater = LayoutInflater.from(ctx);
    }

    @NonNull
    @Override
    public PhotoAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.photo_view, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StorageReference storageRef = FirebaseStorage.getInstance().getReference();;
        StorageReference pathReference;
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
                    if (!status) {
                        //int mPosition = getLayoutPosition();
                        //byte[] clicked = photo_bytes.get(mPosition);
                        //Bitmap b = BitmapFactory.decodeByteArray(clicked, 0, clicked.length);
                        //photo_view_item.setImageBitmap(b);
                        photo_view_item.setLayoutParams(new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.WRAP_CONTENT, ConstraintLayout.LayoutParams.MATCH_PARENT));
                        status = true;
                    }
                    else if (status) {
                        photo_view_item.setLayoutParams(new LinearLayout.LayoutParams(200, 200));
                        photo_view_item.setAdjustViewBounds(true);
                        status = false;
                    }
                }
            });

        }
    }


}
