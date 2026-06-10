package com.example.eduinsight;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class MyDoubtsPagerAdapter extends FragmentStateAdapter {

    public MyDoubtsPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 0) {
            return DoubtListFragment.newInstance("pending");
        } else {
            return DoubtListFragment.newInstance("solved");
        }
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}
