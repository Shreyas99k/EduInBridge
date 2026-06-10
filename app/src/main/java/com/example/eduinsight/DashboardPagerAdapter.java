package com.example.eduinsight;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class DashboardPagerAdapter extends FragmentStateAdapter {
    public DashboardPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 0) {
            return new ChatFragment(); // Active Chat Selection
        } else {
            return new MentorshipHistoryFragment(); // Recent Chats List
        }
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}
