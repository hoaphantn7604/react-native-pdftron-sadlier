package com.pdftron.reactnative.nativeviews;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.tabs.TabLayout;
import com.pdftron.pdf.dialog.BookmarksDialogFragment;
import com.pdftron.pdf.utils.DialogFragmentTab;

import java.util.ArrayList;

public class CustomBookmarkDialogFragment extends BookmarksDialogFragment {
    private static final String TAG = "ahihi-"+CustomBookmarkDialogFragment.class.getSimpleName();

    private boolean useTrickToolbarTitle = false;

    CustomBookmarkDialogFragment(){
        super();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated: 1");
        if(this.useTrickToolbarTitle) {
            this.mToolbar.setTitle("Thumbnails");
        }
    }

    public static BookmarksDialogFragment newInstance(BookmarksDialogFragment.DialogMode mode) {
        Bundle args = new Bundle();
        args.putString("BookmarksDialogFragment_mode", mode != null ? mode.name() : BookmarksDialogFragment.DialogMode.DIALOG.name());
        CustomBookmarkDialogFragment fragment = new CustomBookmarkDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public BookmarksDialogFragment setDialogFragmentTabs(@NonNull ArrayList<DialogFragmentTab> dialogFragmentTabs, int initialTabIndex) {
        if(dialogFragmentTabs.size() == 4 && initialTabIndex == 1){
            this.useTrickToolbarTitle = true;
        }else{
            this.useTrickToolbarTitle = false;
        }
        Log.d(TAG, "setDialogFragmentTabs: 2");
       return super.setDialogFragmentTabs(dialogFragmentTabs,initialTabIndex);
    }

    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        super.onTabSelected(tab);

       int pos = tab.getPosition();
       int total = this.mTabLayout.getTabCount();
       if(pos == 1 && total == 4){
           this.mToolbar.setTitle("Thumbnails");
       }
    }
}
