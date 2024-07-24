package com.deneb.org;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class MediaAdapter extends RecyclerView.Adapter<MediaAdapter.MediaViewHolder> {

    private Context context;
    private List<Uri> mediaUriList;

    public MediaAdapter(Context context, List<Uri> mediaUriList) {
        this.context = context;
        this.mediaUriList = mediaUriList;
    }

    @NonNull
    @Override
    public MediaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_media, parent, false);
        return new MediaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MediaViewHolder holder, int position) {
        Uri mediaUri = mediaUriList.get(position);

        // Aquí debes cargar la imagen o video en el ImageView
        Glide.with(context)
                .load(mediaUri)
                .into(holder.mediaImageView);

        // Configura la descripción accesible (si es necesario)
        holder.mediaImageView.setContentDescription("Descripción de archivo multimedia");
    }

    @Override
    public int getItemCount() {
        return mediaUriList.size();
    }

    public static class MediaViewHolder extends RecyclerView.ViewHolder {
        ImageView mediaImageView;

        public MediaViewHolder(@NonNull View itemView) {
            super(itemView);
            mediaImageView = itemView.findViewById(R.id.mediaImageView);
        }
    }
}
