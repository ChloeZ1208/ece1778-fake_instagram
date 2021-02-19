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
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.List;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.ViewHolder> {

    List<String> comments;
    List<String> displayPicPath;
    List<String> username;
    Context ctx;

    StorageReference storageRef = FirebaseStorage.getInstance().getReference();;
    StorageReference pathReference;


    public CommentAdapter(Context ctx, List<String> comments, List<String> displayPicPath, List<String> username) {
        this.comments = comments;
        this.displayPicPath = displayPicPath;
        this.username = username;
        this.ctx = ctx;
    }

    @NonNull
    @Override
    public CommentAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(ctx).inflate(R.layout.comment_view, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        /*
        * TODO: 1. storage-displayPicPath-profile pic
        *       2. set comment
        *       3. set username
         */
        pathReference = storageRef.child(displayPicPath.get(position));
        pathReference.getBytes(1024 * 1024)
                .addOnSuccessListener(new OnSuccessListener<byte[]>() {
                    @Override
                    public void onSuccess(byte[] bytes) {
                        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        holder.profilePic.setImageBitmap(bitmap);
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Log.w("profile pic", "Error occurred during downloading.");
            }
        });
        holder.userName.setText(username.get(position));
        holder.comment.setText(comments.get(position));
    }

    @Override
    public int getItemCount() {
        Log.d("adapter", String.valueOf(comments.size()));
        return comments.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView profilePic;
        TextView userName;
        TextView comment;

        public ViewHolder(View itemView) {
            super(itemView);
            profilePic = itemView.findViewById(R.id.comment_user_profile);
            userName = itemView.findViewById(R.id.comment_username);
            comment = itemView.findViewById(R.id.comment_user_comment);
        }
    }
}
