package com.example.musicparty.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicparty.R;
import com.example.musicparty.model.Track;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TrackAdapter extends ListAdapter<Track, TrackAdapter.TrackViewHolder> {

    public interface OnTrackClickListener {
        void onTrackClick(Track track);
    }

    private static final DiffUtil.ItemCallback<Track> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<>() {
                @Override
                public boolean areItemsTheSame(@NonNull Track oldItem, @NonNull Track newItem) {
                    return oldItem.matches(newItem);
                }

                @Override
                public boolean areContentsTheSame(@NonNull Track oldItem, @NonNull Track newItem) {
                    return Objects.equals(oldItem.getTitle(), newItem.getTitle())
                            && Objects.equals(oldItem.getArtist(), newItem.getArtist())
                            && oldItem.getDurationMs() == newItem.getDurationMs();
                }
            };

    private OnTrackClickListener listener;

    public TrackAdapter() {
        super(DIFF_CALLBACK);
    }

    public void setTracks(List<Track> items) {
        submitList(items == null ? new ArrayList<>() : new ArrayList<>(items));
    }

    public void setOnTrackClickListener(OnTrackClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public TrackViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_track, parent, false);
        return new TrackViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TrackViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    public class TrackViewHolder extends RecyclerView.ViewHolder {
        private final TextView titleText;
        private final TextView subtitleText;

        TrackViewHolder(@NonNull View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.trackTitleText);
            subtitleText = itemView.findViewById(R.id.trackSubtitleText);
        }

        void bind(Track track) {
            titleText.setText(track.getDisplayTitle(itemView.getContext()));
            subtitleText.setText(
                    track.getDisplayArtist(itemView.getContext()) + " • " + track.getFormattedDuration()
            );
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTrackClick(track);
                }
            });
        }
    }
}
