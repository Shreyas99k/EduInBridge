package com.example.eduinsight;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import java.util.List;

public class MentorshipDiffCallback extends DiffUtil.Callback {

    private final List<MentorshipMessage> oldList;
    private final List<MentorshipMessage> newList;

    public MentorshipDiffCallback(List<MentorshipMessage> oldList, List<MentorshipMessage> newList) {
        this.oldList = oldList;
        this.newList = newList;
    }

    @Override
    public int getOldListSize() {
        return oldList != null ? oldList.size() : 0;
    }

    @Override
    public int getNewListSize() {
        return newList != null ? newList.size() : 0;
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        return oldList.get(oldItemPosition).getId() == newList.get(newItemPosition).getId();
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        return oldList.get(oldItemPosition).equals(newList.get(newItemPosition));
    }

    @Nullable
    @Override
    public Object getChangePayload(int oldItemPosition, int newItemPosition) {
        // Implement if partial updates are needed
        return super.getChangePayload(oldItemPosition, newItemPosition);
    }
}
