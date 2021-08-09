package com.pdftron.reactnative.nativeviews;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.controls.AnnotationDialogFragment;
import com.pdftron.pdf.controls.PdfViewCtrlTabFragment;
import com.pdftron.pdf.controls.PdfViewCtrlTabHostFragment;
import com.pdftron.pdf.dialog.BookmarksDialogFragment;
import com.pdftron.pdf.dialog.ViewModePickerDialogFragment;
import com.pdftron.pdf.dialog.annotlist.AnnotationListSortOrder;
import com.pdftron.pdf.tools.ToolManager;
import com.pdftron.pdf.utils.DialogFragmentTab;
import com.pdftron.pdf.utils.PdfViewCtrlSettingsManager;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.reactnative.R;
import com.pdftron.pdf.tools.R.drawable;
import com.pdftron.pdf.tools.R.string;
import com.pdftron.pdf.tools.R.menu;

import java.util.ArrayList;

public class CustomPdfViewCtrlTabHostFragment extends PdfViewCtrlTabHostFragment implements CustomViewModePickerDialogFragment.CustomViewModePickerDialogFragmentListener, CustomBookmarkDialogFragment.ThumbnailClickListener {
    private final String TAG = "ahihi-" + CustomPdfViewCtrlTabHostFragment.class.getSimpleName();
    protected ImageButton mBtnBookmark;
    protected boolean mShowCustomizeTool = false;
    private BookmarksDialogFragment.BookmarksDialogListener mBookmarksDialogListener = null;

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
    }

    @Override
    public boolean showAnnotationToolbar(int mode, ToolManager.ToolMode toolMode) {
        Log.d(TAG, "showAnnotationToolbar: ");
        return super.showAnnotationToolbar(mode, toolMode);
    }

    @Override
    protected DialogFragmentTab createAnnotationDialogTab() {
        PdfViewCtrlTabFragment currentFragment = this.getCurrentPdfViewCtrlFragment();
        Context context = this.getContext();
        if (currentFragment != null && context != null) {
            Bundle bundle = new Bundle();
            boolean readonly = currentFragment.isTabReadOnly();
            if (!readonly && this.mViewerConfig != null && !this.mViewerConfig.annotationsListEditingEnabled()) {
                readonly = true;
            }

            bundle.putBoolean("is_read_only", true);
            bundle.putBoolean("is_right-to-left", currentFragment.isRtlMode());
            bundle.putInt("sort_mode_as_int", PdfViewCtrlSettingsManager.getAnnotListSortOrder(context, AnnotationListSortOrder.DATE_ASCENDING));
            return new DialogFragmentTab(AnnotationDialogFragment.class, "tab-annotation", Utils.getDrawable(context, R.drawable.ic_crayon), (String)null, this.getString(string.bookmark_dialog_fragment_annotation_tab_title), bundle, menu.fragment_annotlist_sort);
        } else {
            return null;
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

    @Override
    protected DialogFragmentTab createOutlineDialogTab() {
        DialogFragmentTab dialogTab = super.createOutlineDialogTab();
        dialogTab.toolbarTitle = "Table of Contents";
        return dialogTab;
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
                return new DialogFragmentTab(ThumbnailDialogFragment.class, "tab-outline", Utils.getDrawable(this.getContext(), R.drawable.toolbar_page), null, "Thumbnails", bundle);
            }
        }
    }

    @Override
    protected BookmarksDialogFragment createBookmarkDialogFragmentInstance() {
        BookmarksDialogFragment.DialogMode mode = this.canOpenNavigationListAsSideSheet() ? BookmarksDialogFragment.DialogMode.SHEET : BookmarksDialogFragment.DialogMode.DIALOG;
        return BookmarksDialogFragment.newInstance(mode);
    }

    protected BookmarksDialogFragment createCustomBookmarkDialogFragmentInstance() {
        // CustomBookmarkDialogFragment.DialogMode mode = this.canOpenNavigationListAsSideSheet() ? CustomBookmarkDialogFragment.DialogMode.SHEET : CustomBookmarkDialogFragment.DialogMode.DIALOG;
        CustomBookmarkDialogFragment.DialogMode mode = CustomBookmarkDialogFragment.DialogMode.DIALOG;
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
                    this.mBookmarksDialog.setPdfViewCtrl(pdfViewCtrl).setDialogFragmentTabs(this.getBookmarksDialogTabs(), 0).setCurrentBookmark(this.mCurrentBookmark);
                    this.mBookmarksDialog.setBookmarksDialogListener(this);
                    this.mBookmarksDialog.setBookmarksTabsListener(this);
                    ((CustomBookmarkDialogFragment) this.mBookmarksDialog).setThumbnailClickListener(this);
                    this.mBookmarksDialog.setStyle(DialogFragment.STYLE_NO_TITLE, R.style.AppTheme);
                    FragmentManager fragmentManager = this.getFragmentManager();

                    // Show fullscreen dialog
                    if (fragmentManager != null) {
                        this.mBookmarksDialog.show(fragmentManager, "bookmarks_dialog");
                    }

//                    if (this.canOpenNavigationListAsSideSheet()) {
//                        int topMargin = this.getToolbarHeight();
//                        if (this.mViewerConfig != null && !this.mViewerConfig.isAutoHideToolbarEnabled()) {
//                            topMargin = 0;
//                        }
//
//                        currentFragment.openNavigationList(this.mBookmarksDialog, topMargin, this.mSystemWindowInsetBottom);
//                        this.mBookmarksDialog = null;
//                    } else {
//                        FragmentManager fragmentManager = this.getFragmentManager();
//                        if (fragmentManager != null) {
//                            this.mBookmarksDialog.show(fragmentManager, "bookmarks_dialog");
//                        }
//                    }

                    this.stopHideToolbarsTimer();
                }
            }
        }
    }

    @Override
    public void onBookmarksDialogWillDismiss(int tabIndex) {
        super.onBookmarksDialogWillDismiss(tabIndex);
        if (this.mBookmarksDialogListener != null) {
            this.mBookmarksDialogListener.onBookmarksDialogWillDismiss(tabIndex);
        }
    }

    @Override
    public void onBookmarksDialogDismissed(int tabIndex) {
        super.onBookmarksDialogDismissed(tabIndex);
        if (this.mBookmarksDialogListener != null) {
            this.mBookmarksDialogListener.onBookmarksDialogDismissed(tabIndex);
        }
    }

    public CustomPdfViewCtrlTabHostFragment useCustomizeTool(boolean mShowCustomizeTool) {
        this.mShowCustomizeTool = mShowCustomizeTool;
        return this;
    }

    public CustomPdfViewCtrlTabHostFragment setBookmarkDialogListener(BookmarksDialogFragment.BookmarksDialogListener listener) {
        this.mBookmarksDialogListener = listener;
        return this;
    }

    @Override
    protected void onViewModeOptionSelected() {
        PdfViewCtrlTabFragment currentFragment = this.getCurrentPdfViewCtrlFragment();
        if (currentFragment != null) {
            currentFragment.updateCurrentPageInfo();
            PDFViewCtrl.PagePresentationMode currentViewMode = PDFViewCtrl.PagePresentationMode.SINGLE_CONT;
            PDFViewCtrl pdfViewCtrl = currentFragment.getPDFViewCtrl();
            if (pdfViewCtrl != null) {
                currentViewMode = pdfViewCtrl.getPagePresentationMode();
            }

            if (currentViewMode == PDFViewCtrl.PagePresentationMode.SINGLE_VERT) {
                currentViewMode = PDFViewCtrl.PagePresentationMode.SINGLE_CONT;
            } else if (currentViewMode == PDFViewCtrl.PagePresentationMode.FACING_VERT) {
                currentViewMode = PDFViewCtrl.PagePresentationMode.FACING_CONT;
            } else if (currentViewMode == PDFViewCtrl.PagePresentationMode.FACING_COVER_VERT) {
                currentViewMode = PDFViewCtrl.PagePresentationMode.FACING_COVER_CONT;
            }

            boolean isRtlMode = currentFragment.isRtlMode();
            boolean isReflowMode = currentFragment.isReflowMode();
            int reflowTextSize = currentFragment.getReflowTextSize();
            ArrayList<Integer> hiddenViewModeItems = new ArrayList();
            if (this.mViewerConfig != null && !this.mViewerConfig.isShowCropOption()) {
                hiddenViewModeItems.add(ViewModePickerDialogFragment.ViewModePickerItems.ITEM_ID_USERCROP.getValue());
            }

            if (this.mViewerConfig != null && this.mViewerConfig.getHideViewModeIds() != null) {
                int[] var8 = this.mViewerConfig.getHideViewModeIds();
                int var9 = var8.length;

                for (int var10 = 0; var10 < var9; ++var10) {
                    int item = var8[var10];
                    hiddenViewModeItems.add(item);
                }
            }

            CustomViewModePickerDialogFragment dialog = CustomViewModePickerDialogFragment.newInstance(currentViewMode, isRtlMode, isReflowMode, reflowTextSize, hiddenViewModeItems);
            dialog.setViewModePickerDialogFragmentListener(this);
            dialog.setStyle(DialogFragment.STYLE_NORMAL, R.style.AppTheme);
            FragmentManager fragmentManager = this.getFragmentManager();
            if (fragmentManager != null) {
                dialog.show(fragmentManager, "view_mode_picker");
            }

            this.stopHideToolbarsTimer();
        }
    }

    @Override
    public void onThumbnailClickListener(int page) {
        this.getCurrentPdfViewCtrlFragment().setCurrentPageHelper(page, true);
        Log.d(TAG, "onThumbnailClickListener: " + page);
    }

    @Override
    public void onTabSingleTapConfirmed() {
        PdfViewCtrlTabFragment currentFragment = this.getCurrentPdfViewCtrlFragment();
        if (currentFragment != null) {
            if (!currentFragment.isAnnotationMode() && !this.mIsSearchMode) {
                if (this.mAppBarLayout.getVisibility() == View.VISIBLE) {
                    this.hideUI();
                } else {
                    this.showUI();
                }
            }

        }
    }

    @Override
    public void hideUI() {
        if (this.mViewerConfig != null && !this.mViewerConfig.isAutoHideToolbarEnabled()) {
            this.setThumbSliderVisibility(false, true);
        } else {
            Activity activity = this.getActivity();
            PdfViewCtrlTabFragment currentFragment = this.getCurrentPdfViewCtrlFragment();
            if (activity == null || currentFragment == null) {
                return;
            }

            boolean canHideToolbars = currentFragment.onHideToolbars();
            boolean canEnterFullscreenMode = currentFragment.onEnterFullscreenMode();
            boolean isThumbSliderVisible = currentFragment.isThumbSliderVisible();
            boolean isAnnotationMode = currentFragment.isAnnotationMode();

            if (canHideToolbars) {
                this.setToolbarsVisible(false);
            }

            if (isThumbSliderVisible && canHideToolbars && canEnterFullscreenMode || !isThumbSliderVisible && canEnterFullscreenMode) {
                if (isAnnotationMode) {
                    this.showSystemStatusBar();
                } else {
                    this.hideSystemUI();
                }
            }

            if (!this.isInFullScreenMode() && currentFragment.isNavigationListShowing()) {
                View view = this.getView();
                if (view != null) {
                    ViewCompat.requestApplyInsets(view);
                }
            }
        }
    }
}
