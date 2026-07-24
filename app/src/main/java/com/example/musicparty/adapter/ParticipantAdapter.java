package com.example.musicparty.adapter;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicparty.R;
import com.example.musicparty.model.Participant;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ParticipantAdapter
        extends ListAdapter<Participant, ParticipantAdapter.ParticipantViewHolder> {

    private static final DiffUtil.ItemCallback<Participant> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<>() {
                @Override
                public boolean areItemsTheSame(
                        @NonNull Participant oldItem,
                        @NonNull Participant newItem
                ) {
                    return Objects.equals(oldItem.getId(), newItem.getId());
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull Participant oldItem,
                        @NonNull Participant newItem
                ) {
                    return Objects.equals(oldItem.getName(), newItem.getName())
                            && oldItem.isHost() == newItem.isHost();
                }
            };

    public ParticipantAdapter() {
        super(DIFF_CALLBACK);
    }

    public void setParticipants(List<Participant> items) {
        submitList(items == null ? new ArrayList<>() : new ArrayList<>(items));
    }

    @NonNull
    @Override
    public ParticipantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_participant, parent, false);
        return new ParticipantViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ParticipantViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    private static String getInitial(String name) {
        String trimmed = name.trim();
        if (trimmed.isEmpty()) {
            return "?";
        }
        return trimmed.substring(0, 1).toUpperCase();
    }

    public static class ParticipantViewHolder extends RecyclerView.ViewHolder {
        private final TextView avatarText;
        private final TextView nameText;
        private final TextView roleText;

        ParticipantViewHolder(@NonNull View itemView) {
            super(itemView);
            avatarText = itemView.findViewById(R.id.participantAvatarText);
            nameText = itemView.findViewById(R.id.participantNameText);
            roleText = itemView.findViewById(R.id.participantRoleText);
        }

        void bind(Participant participant) {
            String name = participant.getName();
            if (TextUtils.isEmpty(name)) {
                name = itemView.getContext().getString(R.string.default_participant_name);
            }
            avatarText.setText(getInitial(name));
            nameText.setText(name);
            roleText.setText(participant.isHost()
                    ? itemView.getContext().getString(R.string.role_host)
                    : itemView.getContext().getString(R.string.role_guest));
        }
    }
}
