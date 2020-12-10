package com.pdftron.reactnative.nativeviews;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.controls.PdfViewCtrlTabFragment;
import com.pdftron.pdf.controls.PdfViewCtrlTabHostFragment;
import com.pdftron.pdf.dialog.BookmarksDialogFragment;
import com.pdftron.pdf.utils.DialogFragmentTab;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.reactnative.R;

import java.util.ArrayList;

public class CustomPdfViewCtrlTabHostFragment extends PdfViewCtrlTabHostFragment {
    private final String TAG = "ahihi-" + CustomPdfViewCtrlTabHostFragment.class.getSimpleName();
    protected ImageButton mBtnBookmark;
    protected MenuItem mMenuBookmark;
    protected boolean mShowCustomizeTool = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: ");
        return inflater.inflate(R.layout.custom_controls_fragment_tabbed_pdfviewctrl, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.mBtnBookmark = view.findViewById(R.id.btnBookmark);
        this.mBtnBookmark.setVisibility(this.mShowCustomizeTool ? View.VISIBLE : View.GONE);
        this.mBtnBookmark.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: ");
                PdfViewCtrlTabFragment fragment = CustomPdfViewCtrlTabHostFragment.this.getCurrentPdfViewCtrlFragment();
                if (fragment instanceof RNPdfViewCtrlTabFragment) {
                    ((RNPdfViewCtrlTabFragment) fragment).openNavigationUIControl();
                }
            }
        });

        Activity activity = this.getActivity();
        if (activity != null) {
            this.onCreateOptionsMenu(this.mToolbar.getMenu(), new MenuInflater(activity));
        }

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    protected void initOptionsMenu(Menu menu) {
        super.initOptionsMenu(menu);
        Activity activity = this.getActivity();
        if (activity != null && this.mShowCustomizeTool) {
            this.mMenuBookmark = menu.getItem(0);
            this.mMenuBookmark.setVisible(this.mShowCustomizeTool);
        }
    }

    @Override
    protected ArrayList<DialogFragmentTab> getBookmarksDialogTabs() {
        int numOfTabs = this.mShowCustomizeTool ? 4 : 3;
        DialogFragmentTab userBookmarkTab = this.createUserBookmarkDialogTab();
        DialogFragmentTab outlineTab = this.createOutlineDialogTab();
        DialogFragmentTab annotationTab = this.createAnnotationDialogTab();

        ArrayList<DialogFragmentTab> dialogFragmentTabs = new ArrayList(numOfTabs);
        boolean canAdd;

        if (outlineTab != null) {
            canAdd = this.mViewerConfig == null || this.mViewerConfig.isShowOutlineList();
            if (canAdd) {
                dialogFragmentTabs.add(outlineTab);
            }
        }

        if (this.mShowCustomizeTool) {
            DialogFragmentTab thumbnailTab = this.createThumbnailDialogTab();
            if (thumbnailTab != null) {
                canAdd = this.mViewerConfig == null || this.mViewerConfig.isShowAnnotationsList();
                if (canAdd) {
                    dialogFragmentTabs.add(thumbnailTab);
                }
            }
        }

        if (annotationTab != null) {
            canAdd = this.mViewerConfig == null || this.mViewerConfig.isShowAnnotationsList();
            if (canAdd) {
                dialogFragmentTabs.add(annotationTab);
            }
        }

        if (userBookmarkTab != null) {
            canAdd = this.mViewerConfig == null || this.mViewerConfig.isShowUserBookmarksList();
            if (canAdd) {
                dialogFragmentTabs.add(userBookmarkTab);
            }
        }

        return dialogFragmentTabs;
    }

    private DialogFragmentTab createThumbnailDialogTab() {
        PdfViewCtrlTabFragment currentFragment = this.getCurrentPdfViewCtrlFragment();
        if (currentFragment == null) {
            return null;
        } else {
            PDFViewCtrl pdfViewCtrl = currentFragment.getPDFViewCtrl();
            if (pdfViewCtrl == null) {
                return null;
            } else {
                Bundle bundle = new Bundle();
                boolean readonly = currentFragment.isTabReadOnly();
                if (!readonly && this.mViewerConfig != null && !this.mViewerConfig.isUserBookmarksListEditingEnabled()) {
                    readonly = true;
                }

                bundle.putBoolean("is_read_only", false);
                return new DialogFragmentTab(ThumbnailDialogFragment.class, "tab-outline", Utils.getDrawable(this.getContext(), R.drawable.toolbar_page), (String) null, "Thumbnails", bundle);
            }
        }
    }

    @Override
    protected BookmarksDialogFragment createBookmarkDialogFragmentInstance() {
        BookmarksDialogFragment.DialogMode mode = this.canOpenNavigationListAsSideSheet() ? BookmarksDialogFragment.DialogMode.SHEET : BookmarksDialogFragment.DialogMode.DIALOG;
        return BookmarksDialogFragment.newInstance(mode);
    }

    protected BookmarksDialogFragment createCustomBookmarkDialogFragmentInstance() {
        CustomBookmarkDialogFragment.DialogMode mode = this.canOpenNavigationListAsSideSheet() ? CustomBookmarkDialogFragment.DialogMode.SHEET : CustomBookmarkDialogFragment.DialogMode.DIALOG;
        return CustomBookmarkDialogFragment.newInstance(mode);
    }

    @Override
    public void onOutlineOptionSelected(int initialTabIndex) {
        Log.d(TAG, "onOutlineOptionSelected: " + initialTabIndex);
        PdfViewCtrlTabFragment currentFragment = this.getCurrentPdfViewCtrlFragment();
        if (currentFragment != null) {
            PDFViewCtrl pdfViewCtrl = currentFragment.getPDFViewCtrl();
            if (pdfViewCtrl != null) {
                currentFragment.updateCurrentPageInfo();
                if (currentFragment.isNavigationListShowing()) {
                    currentFragment.closeNavigationList();
                } else {
                    if (this.mBookmarksDialog != null) {
                        this.mBookmarksDialog.dismiss();
                    }

                    // this.mBookmarksDialog = this.createBookmarkDialogFragmentInstance();
                    this.mBookmarksDialog = this.createCustomBookmarkDialogFragmentInstance();
                    this.mBookmarksDialog.setPdfViewCtrl(pdfViewCtrl).setDialogFragmentTabs(this.getBookmarksDialogTabs(), initialTabIndex).setCurrentBookmark(this.mCurrentBookmark);
                    this.mBookmarksDialog.setBookmarksDialogListener(this);
                    this.mBookmarksDialog.setBookmarksTabsListener(this);
                    this.mBookmarksDialog.setStyle(DialogFragment.STYLE_NO_TITLE, R.style.CustomAppTheme);
                    if (this.canOpenNavigationListAsSideSheet()) {
                        int topMargin = this.getToolbarHeight();
                        if (this.mViewerConfig != null && !this.mViewerConfig.isAutoHideToolbarEnabled()) {
                            topMargin = 0;
                        }

                        currentFragment.openNavigationList(this.mBookmarksDialog, topMargin, this.mSystemWindowInsetBottom);
                        this.mBookmarksDialog = null;
                    } else {
                        FragmentManager fragmentManager = this.getFragmentManager();
                        if (fragmentManager != null) {
                            this.mBookmarksDialog.show(fragmentManager, "bookmarks_dialog");
                        }
                    }

                    this.stopHideToolbarsTimer();
                }
            }
        }
    }

    public PdfViewCtrlTabHostFragment useCustomizeTool(boolean mShowCustomizeTool) {
        this.mShowCustomizeTool = mShowCustomizeTool;
        return this;
    }
}
