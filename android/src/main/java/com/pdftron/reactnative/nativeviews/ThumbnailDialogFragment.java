package com.pdftron.reactnative.nativeviews;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.net.Uri;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Page;
import com.pdftron.pdf.controls.AddPageDialogFragment;
import com.pdftron.pdf.controls.OutlineDialogFragment;
import com.pdftron.pdf.controls.ThumbnailsViewAdapter;
import com.pdftron.pdf.controls.ThumbnailsViewFilterMode;
import com.pdftron.pdf.dialog.pagelabel.PageLabelDialog;
import com.pdftron.pdf.dialog.pagelabel.PageLabelSetting;
import com.pdftron.pdf.dialog.pagelabel.PageLabelSettingViewModel;
import com.pdftron.pdf.dialog.pagelabel.PageLabelUtils;
import com.pdftron.pdf.tools.ToolManager;
import com.pdftron.pdf.tools.UndoRedoManager;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.AnalyticsParam;
import com.pdftron.pdf.utils.AnnotUtils;
import com.pdftron.pdf.utils.BookmarkManager;
import com.pdftron.pdf.utils.CommonToast;
import com.pdftron.pdf.utils.Event;
import com.pdftron.pdf.utils.PdfViewCtrlSettingsManager;
import com.pdftron.pdf.utils.ToolbarActionMode;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.utils.ViewerUtils;
import com.pdftron.pdf.widget.recyclerview.ItemClickHelper;
import com.pdftron.pdf.widget.recyclerview.ItemSelectionHelper;
import com.pdftron.pdf.widget.recyclerview.SimpleRecyclerView;
import com.pdftron.reactnative.R;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import co.paulburke.android.itemtouchhelperdemo.helper.SimpleItemTouchHelperCallback;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class ThumbnailDialogFragment extends OutlineDialogFragment implements ThumbnailsViewAdapter.EditPagesListener {
    private static final String BUNDLE_READ_ONLY_DOC = "read_only_doc";
    private static final String BUNDLE_EDIT_MODE = "edit_mode";
    private static final String BUNDLE_OUTPUT_FILE_URI = "output_file_uri";
    public static final int FILTER_MODE_NORMAL = 0;
    public static final int FILTER_MODE_ANNOTATED = 1;
    public static final int FILTER_MODE_BOOKMARKED = 2;
    FloatingActionMenu mFabMenu;
    private Uri mOutputFileUri;
    private boolean mIsReadOnly;
    private boolean mIsReadOnlySave;
    private Integer mInitSelectedItem;
    private PDFViewCtrl mPdfViewCtrl;
    private Toolbar mToolbar;
    private Toolbar mCabToolbar;
    private SimpleRecyclerView mRecyclerView;
    private ThumbnailsViewAdapter mAdapter;
    private ProgressBar mProgressBarView;
    private ItemSelectionHelper mItemSelectionHelper;
    private ItemTouchHelper mItemTouchHelper;
    private ToolbarActionMode mActionMode;
    private MenuItem mMenuItemUndo;
    private MenuItem mMenuItemRedo;
    private MenuItem mMenuItemRotate;
    private MenuItem mMenuItemDelete;
    private MenuItem mMenuItemDuplicate;
    private MenuItem mMenuItemExport;
    private MenuItem mMenuItemPageLabel;
    private MenuItem mMenuItemEdit;
    private MenuItem mMenuItemFilter;
    private MenuItem mMenuItemFilterAll;
    private MenuItem mMenuItemFilterAnnotated;
    private MenuItem mMenuItemFilterBookmarked;
    private int mSpanCount;
    private String mTitle = "";
    private boolean mHasEventAction;
    boolean mAddDocPagesDelay;
    int mPositionDelay;
    ThumbnailsViewAdapter.DocumentFormat mDocumentFormatDelay;
    Object mDataDelay;
    private ThumbnailDialogFragment.OnThumbnailsViewDialogDismissListener mOnThumbnailsViewDialogDismissListener;
    private ThumbnailDialogFragment.OnThumbnailsEditAttemptWhileReadOnlyListener mOnThumbnailsEditAttemptWhileReadOnlyListener;
    private ThumbnailDialogFragment.OnExportThumbnailsListener mOnExportThumbnailsListener;
    private final CompositeDisposable mDisposables = new CompositeDisposable();
    private ThumbnailsViewFilterMode mFilterMode;
    private boolean mStartInEdit;
    private ToolbarActionMode.Callback mActionModeCallback = new ToolbarActionMode.Callback() {
        public boolean onCreateActionMode(ToolbarActionMode mode, Menu menu) {
            mode.inflateMenu(R.menu.cab_controls_fragment_thumbnails_view);
            ThumbnailDialogFragment.this.mMenuItemUndo = menu.findItem(R.id.controls_thumbnails_view_action_undo);
            ThumbnailDialogFragment.this.mMenuItemRedo = menu.findItem(R.id.controls_thumbnails_view_action_redo);
            ThumbnailDialogFragment.this.mMenuItemRotate =menu.findItem(R.id.controls_thumbnails_view_action_rotate);
            ThumbnailDialogFragment.this.mMenuItemDelete =menu.findItem(R.id.controls_thumbnails_view_action_delete);
            ThumbnailDialogFragment.this.mMenuItemDuplicate =menu.findItem(R.id.controls_thumbnails_view_action_duplicate);
            ThumbnailDialogFragment.this.mMenuItemExport = menu.findItem(R.id.controls_thumbnails_view_action_export);
            ThumbnailDialogFragment.this.mMenuItemPageLabel =menu.findItem(R.id.controls_thumbnails_view_action_page_label);
            return true;
        }

        public boolean onPrepareActionMode(ToolbarActionMode mode, Menu menu) {
            boolean isEnabled = ThumbnailDialogFragment.this.mItemSelectionHelper.getCheckedItemCount() > 0;
            if (ThumbnailDialogFragment.this.mMenuItemRotate != null) {
                ThumbnailDialogFragment.this.mMenuItemRotate.setEnabled(isEnabled);
                if (ThumbnailDialogFragment.this.mMenuItemRotate.getIcon() != null) {
                    ThumbnailDialogFragment.this.mMenuItemRotate.getIcon().setAlpha(isEnabled ? 255 : 150);
                }
            }

            if (ThumbnailDialogFragment.this.mMenuItemDelete != null) {
                ThumbnailDialogFragment.this.mMenuItemDelete.setEnabled(isEnabled);
                if (ThumbnailDialogFragment.this.mMenuItemDelete.getIcon() != null) {
                    ThumbnailDialogFragment.this.mMenuItemDelete.getIcon().setAlpha(isEnabled ? 255 : 150);
                }
            }

            if (ThumbnailDialogFragment.this.mMenuItemDuplicate != null) {
                ThumbnailDialogFragment.this.mMenuItemDuplicate.setEnabled(isEnabled);
                if (ThumbnailDialogFragment.this.mMenuItemDuplicate.getIcon() != null) {
                    ThumbnailDialogFragment.this.mMenuItemDuplicate.getIcon().setAlpha(isEnabled ? 255 : 150);
                }
            }

            if (ThumbnailDialogFragment.this.mMenuItemExport != null) {
                ThumbnailDialogFragment.this.mMenuItemExport.setEnabled(isEnabled);
                if (ThumbnailDialogFragment.this.mMenuItemExport.getIcon() != null) {
                    ThumbnailDialogFragment.this.mMenuItemExport.getIcon().setAlpha(isEnabled ? 255 : 150);
                }

                ThumbnailDialogFragment.this.mMenuItemExport.setVisible(ThumbnailDialogFragment.this.mOnExportThumbnailsListener != null);
            }

            if (ThumbnailDialogFragment.this.mMenuItemPageLabel != null) {
                ThumbnailDialogFragment.this.mMenuItemPageLabel.setEnabled(isEnabled);
                if (ThumbnailDialogFragment.this.mMenuItemPageLabel.getIcon() != null) {
                    ThumbnailDialogFragment.this.mMenuItemPageLabel.getIcon().setAlpha(isEnabled ? 255 : 150);
                }
            }

            if (!Utils.isTablet(ThumbnailDialogFragment.this.getContext()) && ThumbnailDialogFragment.this.getResources().getConfiguration().orientation != 2) {
                mode.setTitle(Utils.getLocaleDigits(Integer.toString(ThumbnailDialogFragment.this.mItemSelectionHelper.getCheckedItemCount())));
            } else {
                mode.setTitle(ThumbnailDialogFragment.this.getString(R.string.controls_thumbnails_view_selected, new Object[]{Utils.getLocaleDigits(Integer.toString(ThumbnailDialogFragment.this.mItemSelectionHelper.getCheckedItemCount()))}));
            }

            ThumbnailDialogFragment.this.updateUndoRedoIcons();
            return true;
        }

        public boolean onActionItemClicked(ToolbarActionMode mode, MenuItem item) {
            if (ThumbnailDialogFragment.this.mPdfViewCtrl == null) {
                throw new NullPointerException("setPdfViewCtrl() must be called with a valid PDFViewCtrl");
            } else {
                SparseBooleanArray selectedItemsx;
                int toPage;
                int numPages;
                if (item.getItemId() == R.id.controls_thumbnails_view_action_rotate) {
                    if (ThumbnailDialogFragment.this.mIsReadOnly) {
                        if (ThumbnailDialogFragment.this.mOnThumbnailsEditAttemptWhileReadOnlyListener != null) {
                            ThumbnailDialogFragment.this.mOnThumbnailsEditAttemptWhileReadOnlyListener.onThumbnailsEditAttemptWhileReadOnly();
                        }

                        return true;
                    }

                    selectedItemsx = ThumbnailDialogFragment.this.mItemSelectionHelper.getCheckedItemPositions();
                    List<Integer> pageListx = new ArrayList();

                    for(toPage = 0; toPage < selectedItemsx.size(); ++toPage) {
                        if (selectedItemsx.valueAt(toPage)) {
                            numPages = selectedItemsx.keyAt(toPage);
                            ThumbnailDialogFragment.this.mAdapter.rotateDocPage(numPages + 1);
                            pageListx.add(numPages + 1);
                        }
                    }

                    ThumbnailDialogFragment.this.manageRotatePages(pageListx);
                    ThumbnailDialogFragment.this.mHasEventAction = true;
                    AnalyticsHandlerAdapter.getInstance().sendEvent(29, AnalyticsParam.thumbnailsViewCountParam(2, selectedItemsx.size()));
                } else {
                    int page;
                    ArrayList pageListxx;
                    SparseBooleanArray selectedItems;
                    if (item.getItemId() == R.id.controls_thumbnails_view_action_delete) {
                        if (ThumbnailDialogFragment.this.mIsReadOnly) {
                            if (ThumbnailDialogFragment.this.mOnThumbnailsEditAttemptWhileReadOnlyListener != null) {
                                ThumbnailDialogFragment.this.mOnThumbnailsEditAttemptWhileReadOnlyListener.onThumbnailsEditAttemptWhileReadOnly();
                            }

                            return true;
                        }

                        pageListxx = new ArrayList();
                        selectedItems = ThumbnailDialogFragment.this.mItemSelectionHelper.getCheckedItemPositions();
                        boolean shouldUnlockRead = false;

                        try {
                            ThumbnailDialogFragment.this.mPdfViewCtrl.docLockRead();
                            shouldUnlockRead = true;
                            toPage = ThumbnailDialogFragment.this.mPdfViewCtrl.getDoc().getPageCount();
                        } catch (Exception var17) {
                            AnalyticsHandlerAdapter.getInstance().sendException(var17);
                            boolean var27 = true;
                            return var27;
                        } finally {
                            if (shouldUnlockRead) {
                                ThumbnailDialogFragment.this.mPdfViewCtrl.docUnlockRead();
                            }

                        }

                        if (selectedItems.size() >= toPage) {
                            CommonToast.showText(ThumbnailDialogFragment.this.getContext(), R.string.controls_thumbnails_view_delete_msg_all_pages);
                            ThumbnailDialogFragment.this.clearSelectedList();
                            return true;
                        }

                        for(page = 0; page < selectedItems.size(); ++page) {
                            if (selectedItems.valueAt(page)) {
                                pageListxx.add(selectedItems.keyAt(page) + 1);
                            }
                        }

                        Collections.sort(pageListxx, Collections.reverseOrder());
                        page = pageListxx.size();

                        for(int i = 0; i < page; ++i) {
                            ThumbnailDialogFragment.this.mAdapter.removeDocPage((Integer)pageListxx.get(i));
                        }

                        ThumbnailDialogFragment.this.clearSelectedList();
                        ThumbnailDialogFragment.this.manageDeletePages(pageListxx);
                        ThumbnailDialogFragment.this.mHasEventAction = true;
                        AnalyticsHandlerAdapter.getInstance().sendEvent(29, AnalyticsParam.thumbnailsViewCountParam(3, selectedItems.size()));
                    } else if (item.getItemId() == R.id.controls_thumbnails_view_action_duplicate) {
                        if (ThumbnailDialogFragment.this.mAdapter != null) {
                            pageListxx = new ArrayList();
                            selectedItems = ThumbnailDialogFragment.this.mItemSelectionHelper.getCheckedItemPositions();

                            for(toPage = 0; toPage < selectedItems.size(); ++toPage) {
                                if (selectedItems.valueAt(toPage)) {
                                    pageListxx.add(selectedItems.keyAt(toPage) + 1);
                                }
                            }

                            ThumbnailDialogFragment.this.mAdapter.duplicateDocPages(pageListxx);
                            ThumbnailDialogFragment.this.mHasEventAction = true;
                            AnalyticsHandlerAdapter.getInstance().sendEvent(29, AnalyticsParam.thumbnailsViewCountParam(1, selectedItems.size()));
                        }
                    } else if (item.getItemId() == R.id.controls_thumbnails_view_action_export) {
                        if (ThumbnailDialogFragment.this.mOnExportThumbnailsListener != null) {
                            selectedItemsx = ThumbnailDialogFragment.this.mItemSelectionHelper.getCheckedItemPositions();
                            ThumbnailDialogFragment.this.mOnExportThumbnailsListener.onExportThumbnails(selectedItemsx);
                            ThumbnailDialogFragment.this.mHasEventAction = true;
                            AnalyticsHandlerAdapter.getInstance().sendEvent(29, AnalyticsParam.thumbnailsViewCountParam(4, selectedItemsx.size()));
                        }
                    } else if (item.getItemId() != R.id.controls_thumbnails_view_action_page_label) {
                        ToolManager toolManager;
                        String redoInfo;
                        List pageList;
                        if (item.getItemId() == R.id.controls_thumbnails_view_action_undo) {
                            toolManager = (ToolManager)ThumbnailDialogFragment.this.mPdfViewCtrl.getToolManager();
                            if (toolManager != null && toolManager.getUndoRedoManger() != null) {
                                redoInfo = toolManager.getUndoRedoManger().undo(3, true);
                                ThumbnailDialogFragment.this.updateUndoRedoIcons();
                                if (!Utils.isNullOrEmpty(redoInfo)) {
                                    try {
                                        if (UndoRedoManager.isDeletePagesAction(ThumbnailDialogFragment.this.getContext(), redoInfo)) {
                                            pageList = UndoRedoManager.getPageList(redoInfo);
                                            if (pageList.size() != 0) {
                                                ThumbnailDialogFragment.this.mAdapter.updateAfterAddition(pageList);
                                            }
                                        } else if (UndoRedoManager.isAddPagesAction(ThumbnailDialogFragment.this.getContext(), redoInfo)) {
                                            pageList = UndoRedoManager.getPageList(redoInfo);
                                            if (pageList.size() != 0) {
                                                ThumbnailDialogFragment.this.mAdapter.updateAfterDeletion(pageList);
                                            }
                                        } else if (UndoRedoManager.isRotatePagesAction(ThumbnailDialogFragment.this.getContext(), redoInfo)) {
                                            pageList = UndoRedoManager.getPageList(redoInfo);
                                            if (pageList.size() != 0) {
                                                ThumbnailDialogFragment.this.mAdapter.updateAfterRotation(pageList);
                                            }
                                        } else if (UndoRedoManager.isMovePageAction(ThumbnailDialogFragment.this.getContext(), redoInfo)) {
                                            ThumbnailDialogFragment.this.mAdapter.updateAfterMove(UndoRedoManager.getPageTo(redoInfo), UndoRedoManager.getPageFrom(redoInfo));
                                        } else if (UndoRedoManager.isEditPageLabelsAction(ThumbnailDialogFragment.this.getContext(), redoInfo)) {
                                            ThumbnailDialogFragment.this.mAdapter.updateAfterPageLabelEdit();
                                        }
                                    } catch (Exception var16) {
                                        AnalyticsHandlerAdapter.getInstance().sendException(var16);
                                    }
                                }
                            }
                        } else if (item.getItemId() == R.id.controls_thumbnails_view_action_redo) {
                            toolManager = (ToolManager)ThumbnailDialogFragment.this.mPdfViewCtrl.getToolManager();
                            if (toolManager != null && toolManager.getUndoRedoManger() != null) {
                                redoInfo = toolManager.getUndoRedoManger().redo(3, true);
                                ThumbnailDialogFragment.this.updateUndoRedoIcons();
                                if (!Utils.isNullOrEmpty(redoInfo)) {
                                    try {
                                        if (UndoRedoManager.isDeletePagesAction(ThumbnailDialogFragment.this.getContext(), redoInfo)) {
                                            pageList = UndoRedoManager.getPageList(redoInfo);
                                            if (pageList.size() != 0) {
                                                ThumbnailDialogFragment.this.mAdapter.updateAfterDeletion(pageList);
                                            }
                                        } else if (UndoRedoManager.isAddPagesAction(ThumbnailDialogFragment.this.getContext(), redoInfo)) {
                                            pageList = UndoRedoManager.getPageList(redoInfo);
                                            if (pageList.size() != 0) {
                                                ThumbnailDialogFragment.this.mAdapter.updateAfterAddition(pageList);
                                            }
                                        } else if (UndoRedoManager.isRotatePagesAction(ThumbnailDialogFragment.this.getContext(), redoInfo)) {
                                            pageList = UndoRedoManager.getPageList(redoInfo);
                                            if (pageList.size() != 0) {
                                                ThumbnailDialogFragment.this.mAdapter.updateAfterRotation(pageList);
                                            }
                                        } else if (UndoRedoManager.isMovePageAction(ThumbnailDialogFragment.this.getContext(), redoInfo)) {
                                            ThumbnailDialogFragment.this.mAdapter.updateAfterMove(UndoRedoManager.getPageFrom(redoInfo), UndoRedoManager.getPageTo(redoInfo));
                                        } else if (UndoRedoManager.isEditPageLabelsAction(ThumbnailDialogFragment.this.getContext(), redoInfo)) {
                                            ThumbnailDialogFragment.this.mAdapter.updateAfterPageLabelEdit();
                                        }
                                    } catch (Exception var15) {
                                        AnalyticsHandlerAdapter.getInstance().sendException(var15);
                                    }
                                }
                            }
                        }
                    } else {
                        if (ThumbnailDialogFragment.this.mAdapter == null) {
                            return true;
                        }

                        selectedItemsx = ThumbnailDialogFragment.this.mItemSelectionHelper.getCheckedItemPositions();
                        int fromPage = 2147483647;
                        toPage = -1;
                        numPages = 0;

                        while(true) {
                            if (numPages >= selectedItemsx.size()) {
                                numPages = ThumbnailDialogFragment.this.mPdfViewCtrl.getPageCount();
                                if (fromPage < 1 || toPage < 1 || toPage < fromPage || fromPage > numPages) {
                                    CommonToast.showText(ThumbnailDialogFragment.this.getContext(), ThumbnailDialogFragment.this.getString(R.string.page_label_failed), 1);
                                    return true;
                                }

                                FragmentActivity activity = ThumbnailDialogFragment.this.getActivity();
                                FragmentManager fragManager = ThumbnailDialogFragment.this.getFragmentManager();
                                if (fragManager != null && activity != null) {
                                    String prefix = PageLabelUtils.getPageLabelPrefix(ThumbnailDialogFragment.this.mPdfViewCtrl, fromPage);
                                    PageLabelDialog dialog = PageLabelDialog.newInstance(fromPage, toPage, numPages, prefix);
                                    dialog.setStyle(STYLE_NO_TITLE, R.style.CustomAppTheme);
                                    dialog.show(fragManager, PageLabelDialog.TAG);
                                }
                                break;
                            }

                            if (selectedItemsx.valueAt(numPages)) {
                                page = selectedItemsx.keyAt(numPages) + 1;
                                toPage = page > toPage ? page : toPage;
                                fromPage = page < fromPage ? page : fromPage;
                            }

                            ++numPages;
                        }
                    }
                }

                return true;
            }
        }

        public void onDestroyActionMode(ToolbarActionMode mode) {
            ThumbnailDialogFragment.this.mActionMode = null;
            ThumbnailDialogFragment.this.clearSelectedList();
        }
    };

    public ThumbnailDialogFragment() {
    }

    public static ThumbnailDialogFragment newInstance() {
        return newInstance(false);
    }

    public static ThumbnailDialogFragment newInstance(boolean readOnly) {
        return newInstance(readOnly, false);
    }

    public static ThumbnailDialogFragment newInstance(boolean readOnly, boolean editMode) {
        ThumbnailDialogFragment fragment = new ThumbnailDialogFragment();
        Bundle args = new Bundle();
        args.putBoolean("read_only_doc", readOnly);
        args.putBoolean("edit_mode", editMode);
        fragment.setArguments(args);
        return fragment;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            this.mOutputFileUri = (Uri)savedInstanceState.getParcelable("output_file_uri");
        }

        int defaultFilterMode = 0;
        Context context = this.getContext();
        if (context != null) {
            defaultFilterMode = PdfViewCtrlSettingsManager.getThumbListFilterMode(context, defaultFilterMode);
        }

        if (this.getArguments() != null && this.getArguments().getBoolean("edit_mode", false)) {
            this.mStartInEdit = true;
            defaultFilterMode = 0;
        }

        this.mFilterMode = (ThumbnailsViewFilterMode) ViewModelProviders.of(this, new ThumbnailsViewFilterMode.Factory(defaultFilterMode)).get(ThumbnailsViewFilterMode.class);
    }

    @Nullable
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.controls_fragment_thumbnails_view, container, false);
    }

    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (this.mPdfViewCtrl != null) {
            if (Utils.isNullOrEmpty(this.mTitle)) {
                this.mTitle = this.getString(R.string.controls_thumbnails_view_description);
            }

            int viewWidth = this.getDisplayWidth();
            int thumbSize = this.getResources().getDimensionPixelSize(R.dimen.controls_thumbnails_view_image_width);
            int thumbSpacing = this.getResources().getDimensionPixelSize(R.dimen.controls_thumbnails_view_grid_spacing);
            this.mSpanCount = (int)Math.floor((double)(viewWidth / (thumbSize + thumbSpacing)));
            this.mToolbar = (Toolbar)view.findViewById(R.id.controls_thumbnails_view_toolbar);
            this.mToolbar.setVisibility(View.GONE);
            this.mToolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
            this.mCabToolbar = (Toolbar)view.findViewById(R.id.controls_thumbnails_view_cab);
            this.mCabToolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
            this.mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (!ThumbnailDialogFragment.this.onBackPressed()) {
                        ThumbnailDialogFragment.this.dismiss();
                    }

                }
            });
            if (this.getArguments() != null) {
                Bundle args = this.getArguments();
                this.mIsReadOnly = args.getBoolean("read_only_doc", false);
                this.mIsReadOnlySave = this.mIsReadOnly;
            }

            this.mToolbar.inflateMenu(R.menu.controls_fragment_thumbnail_browser_toolbar);
            this.mMenuItemEdit = this.mToolbar.getMenu().findItem(R.id.controls_action_edit);
            if (this.mMenuItemEdit != null) {
                this.mMenuItemEdit.setVisible(!this.mIsReadOnly);
            }

            this.mMenuItemFilter = this.mToolbar.getMenu().findItem(R.id.action_filter);
            this.mMenuItemFilterAll = this.mToolbar.getMenu().findItem(R.id.menu_filter_all);
            this.mMenuItemFilterAnnotated = this.mToolbar.getMenu().findItem(R.id.menu_filter_annotated);
            this.mMenuItemFilterBookmarked = this.mToolbar.getMenu().findItem(R.id.menu_filter_bookmarked);
            this.mToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                public boolean onMenuItemClick(MenuItem item) {
                    if (item.getItemId() == R.id.controls_action_edit) {
                        ThumbnailDialogFragment.this.mActionMode = new ToolbarActionMode(ThumbnailDialogFragment.this.getActivity(), ThumbnailDialogFragment.this.mCabToolbar);
                        ThumbnailDialogFragment.this.mActionMode.startActionMode(ThumbnailDialogFragment.this.mActionModeCallback);
                        return true;
                    } else {
                        if (item.getItemId() == R.id.action_filter) {
                            if (ThumbnailDialogFragment.this.mMenuItemFilterAll != null && ThumbnailDialogFragment.this.mMenuItemFilterAnnotated != null && ThumbnailDialogFragment.this.mMenuItemFilterBookmarked != null) {
                                Integer mode = ThumbnailDialogFragment.this.mFilterMode.getFilterMode();
                                if (mode != null) {
                                    switch(mode) {
                                        case 0:
                                            ThumbnailDialogFragment.this.mMenuItemFilterAll.setChecked(true);
                                            break;
                                        case 1:
                                            ThumbnailDialogFragment.this.mMenuItemFilterAnnotated.setChecked(true);
                                            break;
                                        case 2:
                                            ThumbnailDialogFragment.this.mMenuItemFilterBookmarked.setChecked(true);
                                    }
                                }
                            }
                        } else {
                            if (item.getItemId() == R.id.menu_filter_all) {
                                ThumbnailDialogFragment.this.mFilterMode.publishFilterTypeChange(0);
                                return true;
                            }

                            if (item.getItemId() == R.id.menu_filter_annotated) {
                                ThumbnailDialogFragment.this.mFilterMode.publishFilterTypeChange(1);
                                return true;
                            }

                            if (item.getItemId() == R.id.menu_filter_bookmarked) {
                                ThumbnailDialogFragment.this.mFilterMode.publishFilterTypeChange(2);
                                return true;
                            }
                        }

                        return false;
                    }
                }
            });
            this.mToolbar.setTitle(this.mTitle);
            this.mProgressBarView = (ProgressBar)view.findViewById(R.id.progress_bar_view);
            this.mProgressBarView.setVisibility(View.GONE);
            this.mRecyclerView = (SimpleRecyclerView)view.findViewById(R.id.controls_thumbnails_view_recycler_view);
            this.mRecyclerView.initView(this.mSpanCount, this.getResources().getDimensionPixelSize(R.dimen.controls_thumbnails_view_grid_spacing));
            this.mRecyclerView.setItemViewCacheSize(this.mSpanCount * 2);
            if (this.mPdfViewCtrl != null && this.mPdfViewCtrl.getToolManager() != null && ((ToolManager)this.mPdfViewCtrl.getToolManager()).isNightMode()) {
                this.mRecyclerView.setBackgroundColor(this.getResources().getColor(R.color.controls_thumbnails_view_bg_dark));
            }

            try {
                this.mRecyclerView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    public void onGlobalLayout() {
                        if (ThumbnailDialogFragment.this.mRecyclerView != null) {
                            try {
                                ThumbnailDialogFragment.this.mRecyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                            } catch (Exception var2) {
                            }

                            if (ThumbnailDialogFragment.this.mAdapter != null) {
                                ThumbnailDialogFragment.this.mAdapter.updateMainViewWidth(ThumbnailDialogFragment.this.getMainViewWidth());
                                ThumbnailDialogFragment.this.updateSpanCount(ThumbnailDialogFragment.this.mSpanCount);
                            }
                        }
                    }
                });
            } catch (Exception var12) {
                AnalyticsHandlerAdapter.getInstance().sendException(var12);
            }

            ItemClickHelper itemClickHelper = new ItemClickHelper();
            itemClickHelper.attachToRecyclerView(this.mRecyclerView);
            this.mItemSelectionHelper = new ItemSelectionHelper();
            this.mItemSelectionHelper.attachToRecyclerView(this.mRecyclerView);
            this.mItemSelectionHelper.setChoiceMode(2);
            this.mAdapter = new ThumbnailsViewAdapter(this.getActivity(), this, this.getFragmentManager(), this.mPdfViewCtrl, (List)null, this.mSpanCount, this.mItemSelectionHelper);
            this.mAdapter.registerAdapterDataObserver(this.mItemSelectionHelper.getDataObserver());
            this.mAdapter.updateMainViewWidth(this.getMainViewWidth());
            this.mRecyclerView.setAdapter(this.mAdapter);
            this.mFilterMode.observeFilterTypeChanges(this.getViewLifecycleOwner(), new Observer<Integer>() {
                public void onChanged(Integer mode) {
                    if (mode != null) {
                        ThumbnailDialogFragment.this.populateThumbList(mode);
                        ThumbnailDialogFragment.this.updateSharedPrefs(mode);
                        switch(mode) {
                            case 0:
                                ThumbnailDialogFragment.this.mToolbar.setTitle(ThumbnailDialogFragment.this.mTitle);
                                ThumbnailDialogFragment.this.mIsReadOnly = ThumbnailDialogFragment.this.mIsReadOnlySave;
                                break;
                            case 1:
                                ThumbnailDialogFragment.this.mToolbar.setTitle(String.format("%s (%s)", ThumbnailDialogFragment.this.mTitle, ThumbnailDialogFragment.this.getResources().getString(R.string.action_filter_thumbnails_annotated)));
                                ThumbnailDialogFragment.this.mIsReadOnly = true;
                                break;
                            case 2:
                                ThumbnailDialogFragment.this.mToolbar.setTitle(String.format("%s (%s)", ThumbnailDialogFragment.this.mTitle, ThumbnailDialogFragment.this.getResources().getString(R.string.action_filter_thumbnails_bookmarked)));
                                ThumbnailDialogFragment.this.mIsReadOnly = true;
                        }

                        ThumbnailDialogFragment.this.updateReadOnlyUI();
                    }

                }
            });
            this.mItemTouchHelper = new ItemTouchHelper(new SimpleItemTouchHelperCallback(this.mAdapter, this.mSpanCount, false, false));
            this.mItemTouchHelper.attachToRecyclerView(this.mRecyclerView);
            itemClickHelper.setOnItemClickListener(new ItemClickHelper.OnItemClickListener() {
                public void onItemClick(RecyclerView recyclerView, View v, int position, long id) {
                    if (ThumbnailDialogFragment.this.mActionMode == null) {
                        int page = ThumbnailDialogFragment.this.mAdapter.getItem(position);
                        ThumbnailDialogFragment.this.mAdapter.setCurrentPage(page);
                        ThumbnailDialogFragment.this.mHasEventAction = true;
                        AnalyticsHandlerAdapter.getInstance().sendEvent(30, AnalyticsParam.viewerNavigateByParam(4));
                        ThumbnailDialogFragment.this.dismiss();
                    } else {
                        ThumbnailDialogFragment.this.mItemSelectionHelper.setItemChecked(position, !ThumbnailDialogFragment.this.mItemSelectionHelper.isItemChecked(position));
                        ThumbnailDialogFragment.this.mActionMode.invalidate();
                    }

                }
            });
            itemClickHelper.setOnItemLongClickListener(new ItemClickHelper.OnItemLongClickListener() {
                public boolean onItemLongClick(RecyclerView recyclerView, View v, final int position, long id) {
                    if (ThumbnailDialogFragment.this.mIsReadOnly) {
                        return true;
                    } else {
                        if (ThumbnailDialogFragment.this.mActionMode == null) {
                            ThumbnailDialogFragment.this.mItemSelectionHelper.setItemChecked(position, true);
                            ThumbnailDialogFragment.this.mActionMode = new ToolbarActionMode(ThumbnailDialogFragment.this.getActivity(), ThumbnailDialogFragment.this.mCabToolbar);
                            ThumbnailDialogFragment.this.mActionMode.startActionMode(ThumbnailDialogFragment.this.mActionModeCallback);
                        } else {
                            if (ThumbnailDialogFragment.this.mIsReadOnly) {
                                if (ThumbnailDialogFragment.this.mOnThumbnailsEditAttemptWhileReadOnlyListener != null) {
                                    ThumbnailDialogFragment.this.mOnThumbnailsEditAttemptWhileReadOnlyListener.onThumbnailsEditAttemptWhileReadOnly();
                                }

                                return true;
                            }

                            ThumbnailDialogFragment.this.mRecyclerView.post(new Runnable() {
                                public void run() {
                                    RecyclerView.ViewHolder holder = ThumbnailDialogFragment.this.mRecyclerView.findViewHolderForAdapterPosition(position);
                                    ThumbnailDialogFragment.this.mItemTouchHelper.startDrag(holder);
                                }
                            });
                        }

                        return true;
                    }
                }
            });
//            this.getDialog().setOnKeyListener(new DialogInterface.OnKeyListener() {
//                public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
//                    if (event.getKeyCode() == 4 && event.getAction() == 1) {
//                        if (ThumbnailDialogFragment.this.onBackPressed()) {
//                            return true;
//                        }
//
//                        dialog.dismiss();
//                    }
//
//                    return false;
//                }
//            });
            this.mFabMenu = (FloatingActionMenu)view.findViewById(R.id.fab_menu);
            this.mFabMenu.setClosedOnTouchOutside(true);
            if (this.mIsReadOnly) {
                this.mFabMenu.setVisibility(View.GONE);
            }
            this.mFabMenu.setVisibility(View.GONE);
            FloatingActionButton pagePdfButton = (FloatingActionButton)this.mFabMenu.findViewById(R.id.page_PDF);
            pagePdfButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    ThumbnailDialogFragment.this.mFabMenu.close(true);
                    if (ThumbnailDialogFragment.this.mIsReadOnly) {
                        if (ThumbnailDialogFragment.this.mOnThumbnailsEditAttemptWhileReadOnlyListener != null) {
                            ThumbnailDialogFragment.this.mOnThumbnailsEditAttemptWhileReadOnlyListener.onThumbnailsEditAttemptWhileReadOnly();
                        }

                    } else {
                        boolean shouldUnlockRead = false;

                        try {
                            ThumbnailDialogFragment.this.mPdfViewCtrl.docLockRead();
                            shouldUnlockRead = true;
                            Page lastPage = ThumbnailDialogFragment.this.mPdfViewCtrl.getDoc().getPage(ThumbnailDialogFragment.this.mPdfViewCtrl.getDoc().getPageCount());
                            AddPageDialogFragment addPageDialogFragment = AddPageDialogFragment.newInstance(lastPage.getPageWidth(), lastPage.getPageHeight()).setInitialPageSize(AddPageDialogFragment.PageSize.Custom);
                            addPageDialogFragment.setOnAddNewPagesListener(new AddPageDialogFragment.OnAddNewPagesListener() {
                                public void onAddNewPages(Page[] pages) {
                                    if (pages != null && pages.length != 0) {
                                        ThumbnailDialogFragment.this.mAdapter.addDocPages(ThumbnailDialogFragment.this.getLastSelectedPage(), ThumbnailsViewAdapter.DocumentFormat.PDF_PAGE, pages);
                                        ThumbnailDialogFragment.this.mHasEventAction = true;
                                        AnalyticsHandlerAdapter.getInstance().sendEvent(29, AnalyticsParam.thumbnailsViewCountParam(5, pages.length));
                                    }
                                }
                            });
                            addPageDialogFragment.show(ThumbnailDialogFragment.this.getActivity().getSupportFragmentManager(), "add_page_dialog");
                        } catch (Exception var8) {
                            AnalyticsHandlerAdapter.getInstance().sendException(var8);
                        } finally {
                            if (shouldUnlockRead) {
                                ThumbnailDialogFragment.this.mPdfViewCtrl.docUnlockRead();
                            }

                        }

                    }
                }
            });
            FloatingActionButton pdfDocButton = (FloatingActionButton)this.mFabMenu.findViewById(R.id.PDF_doc);
            pdfDocButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    ThumbnailDialogFragment.this.mFabMenu.close(true);
                    if (ThumbnailDialogFragment.this.mIsReadOnly) {
                        if (ThumbnailDialogFragment.this.mOnThumbnailsEditAttemptWhileReadOnlyListener != null) {
                            ThumbnailDialogFragment.this.mOnThumbnailsEditAttemptWhileReadOnlyListener.onThumbnailsEditAttemptWhileReadOnly();
                        }

                    } else {
                        ThumbnailDialogFragment.this.launchAndroidFilePicker();
                    }
                }
            });
            FloatingActionButton imagePdfButton = (FloatingActionButton)this.mFabMenu.findViewById(R.id.image_PDF);
            imagePdfButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    ThumbnailDialogFragment.this.mFabMenu.close(true);
                    if (ThumbnailDialogFragment.this.mIsReadOnly) {
                        if (ThumbnailDialogFragment.this.mOnThumbnailsEditAttemptWhileReadOnlyListener != null) {
                            ThumbnailDialogFragment.this.mOnThumbnailsEditAttemptWhileReadOnlyListener.onThumbnailsEditAttemptWhileReadOnly();
                        }

                    } else {
                        ThumbnailDialogFragment.this.mOutputFileUri = ViewerUtils.openImageIntent(ThumbnailDialogFragment.this);
                    }
                }
            });
            FragmentActivity activity = this.getActivity();
            if (activity != null) {
                PageLabelSettingViewModel mPageLabelViewModel = (PageLabelSettingViewModel)ViewModelProviders.of(activity).get(PageLabelSettingViewModel.class);
                mPageLabelViewModel.observeOnComplete(this.getViewLifecycleOwner(), new Observer<Event<PageLabelSetting>>() {
                    public void onChanged(@Nullable Event<PageLabelSetting> pageLabelSettingEvent) {
                        if (pageLabelSettingEvent != null && !pageLabelSettingEvent.hasBeenHandled()) {
                            boolean isSuccessful = PageLabelUtils.setPageLabel(ThumbnailDialogFragment.this.mPdfViewCtrl, (PageLabelSetting)pageLabelSettingEvent.getContentIfNotHandled());
                            if (isSuccessful) {
                                ThumbnailDialogFragment.this.mHasEventAction = true;
                                ThumbnailDialogFragment.this.mAdapter.updateAfterPageLabelEdit();
                                ThumbnailDialogFragment.this.managePageLabelChanged();
                                CommonToast.showText(ThumbnailDialogFragment.this.getContext(), ThumbnailDialogFragment.this.getString(R.string.page_label_success), 1);
                            } else {
                                CommonToast.showText(ThumbnailDialogFragment.this.getContext(), ThumbnailDialogFragment.this.getString(R.string.page_label_failed), 1);
                            }
                        }

                    }
                });
            }
            this.loadAttributes();
        }
    }

    private void loadAttributes() {
        Context context = this.getContext();
        if (null != context) {
            TypedArray a = context.obtainStyledAttributes((AttributeSet)null, R.styleable.ThumbnailBrowser, R.attr.thumbnail_browser, R.style.ThumbnailBrowserStyle);

            try {
                boolean showFilterMenuItem = a.getBoolean(R.styleable.ThumbnailBrowser_showFilterMenuItem, true);
                boolean showAnnotatedMenuItem = a.getBoolean(R.styleable.ThumbnailBrowser_showFilterAnnotated, true);
                boolean showBookmarkedMenuItem = a.getBoolean(R.styleable.ThumbnailBrowser_showFilterBookmarked, true);
                if (!showAnnotatedMenuItem && !showBookmarkedMenuItem) {
                    showFilterMenuItem = false;
                }

                if (this.mMenuItemFilter != null) {
                    this.mMenuItemFilter.setVisible(showFilterMenuItem);
                }

                if (this.mMenuItemFilterAnnotated != null) {
                    this.mMenuItemFilterAnnotated.setVisible(showAnnotatedMenuItem);
                }

                if (this.mMenuItemFilterBookmarked != null) {
                    this.mMenuItemFilterBookmarked.setVisible(showBookmarkedMenuItem);
                }
            } finally {
                a.recycle();
            }

        }
    }

    private void populateThumbList(final int mode) {
        this.mDisposables.add(getPages(this.mPdfViewCtrl, mode).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).doOnSubscribe(new Consumer<Disposable>() {
            public void accept(Disposable disposable) throws Exception {
                ThumbnailDialogFragment.this.mAdapter.clear();
                ThumbnailDialogFragment.this.mAdapter.notifyDataSetChanged();
                ThumbnailDialogFragment.this.mProgressBarView.setVisibility(View.VISIBLE);
                ThumbnailDialogFragment.this.mRecyclerView.setVisibility(View.GONE);
            }
        }).subscribe(new Consumer<List<Integer>>() {
            public void accept(List<Integer> integers) throws Exception {
                ThumbnailDialogFragment.this.mAdapter.addAll(integers);
            }
        }, new Consumer<Throwable>() {
            public void accept(Throwable throwable) throws Exception {
                ThumbnailDialogFragment.this.mProgressBarView.setVisibility(View.GONE);
                CommonToast.showText(ThumbnailDialogFragment.this.getActivity(), R.string.error_generic_message, 0);
                AnalyticsHandlerAdapter.getInstance().sendException(new RuntimeException(throwable));
            }
        }, new Action() {
            public void run() throws Exception {
                ThumbnailDialogFragment.this.mProgressBarView.setVisibility(View.GONE);
                ThumbnailDialogFragment.this.mRecyclerView.setVisibility(View.VISIBLE);
                if (ThumbnailDialogFragment.this.mRecyclerView != null && ThumbnailDialogFragment.this.mAdapter != null && ThumbnailDialogFragment.this.mPdfViewCtrl != null) {
                    int pos = ThumbnailDialogFragment.this.mAdapter.getPositionForPage(ThumbnailDialogFragment.this.mPdfViewCtrl.getCurrentPage());
                    if (pos >= 0 && pos < ThumbnailDialogFragment.this.mAdapter.getItemCount()) {
                        ThumbnailDialogFragment.this.mRecyclerView.scrollToPosition(pos);
                    }
                }

                if (ThumbnailDialogFragment.this.mStartInEdit) {
                    ThumbnailDialogFragment.this.mStartInEdit = false;
                    if (mode == 0) {
                        ThumbnailDialogFragment.this.mActionMode = new ToolbarActionMode(ThumbnailDialogFragment.this.getActivity(), ThumbnailDialogFragment.this.mCabToolbar);
                        ThumbnailDialogFragment.this.mActionMode.startActionMode(ThumbnailDialogFragment.this.mActionModeCallback);
                        if (ThumbnailDialogFragment.this.mInitSelectedItem != null) {
                            ThumbnailDialogFragment.this.mItemSelectionHelper.setItemChecked(ThumbnailDialogFragment.this.mInitSelectedItem, true);
                            ThumbnailDialogFragment.this.mActionMode.invalidate();
                            ThumbnailDialogFragment.this.mInitSelectedItem = null;
                        }
                    }
                }

            }
        }));
    }

    private static Observable<List<Integer>> getPages(final PDFViewCtrl pdfViewCtrl, final int mode) {
        return Observable.create(new ObservableOnSubscribe<List<Integer>>() {
            public void subscribe(ObservableEmitter<List<Integer>> emitter) throws Exception {
                if (pdfViewCtrl == null) {
                    emitter.onComplete();
                } else {
                    boolean shouldUnlockRead = false;

                    try {
                        pdfViewCtrl.docLockRead();
                        shouldUnlockRead = true;
                        ArrayList<Integer> excludeList = new ArrayList();
                        excludeList.add(1);
                        excludeList.add(19);
                        ArrayList<Integer> bookmarkedPages = new ArrayList();
                        if (mode == 2) {
                            try {
                                bookmarkedPages = BookmarkManager.getPdfBookmarkedPageNumbers(pdfViewCtrl.getDoc());
                            } catch (Exception var13) {
                            }
                        }

                        int pageCount = pdfViewCtrl.getDoc().getPageCount();

                        for(int pageNum = 1; pageNum <= pageCount; ++pageNum) {
                            boolean canAdd = true;
                            if (mode == 1) {
                                int annotCount = AnnotUtils.getAnnotationCountOnPage(pdfViewCtrl, pageNum, excludeList);
                                canAdd = annotCount > 0;
                            }

                            if (mode == 2) {
                                canAdd = bookmarkedPages.contains(pageNum);
                            }

                            if (canAdd) {
                                ArrayList<Integer> pages = new ArrayList();
                                pages.add(pageNum);
                                emitter.onNext(pages);
                            }
                        }
                    } catch (Exception var14) {
                        emitter.onError(var14);
                    } finally {
                        if (shouldUnlockRead) {
                            pdfViewCtrl.docUnlockRead();
                        }

                        emitter.onComplete();
                    }

                }
            }
        });
    }

    private void updateSharedPrefs(int filterMode) {
        Context context = this.getContext();
        if (context != null) {
            PdfViewCtrlSettingsManager.updateThumbListFilterMode(context, filterMode);
        }

    }

    private void updateReadOnlyUI() {
        this.finishActionMode();
        if (this.mMenuItemEdit != null) {
            this.mMenuItemEdit.setVisible(!this.mIsReadOnly);
        }

        if (this.mFabMenu != null) {
            this.mFabMenu.setVisibility(View.GONE);
            // this.mFabMenu.setVisibility(this.mIsReadOnly ? View.GONE : View.VISIBLE);
        }

    }

    public void onResume() {
        super.onResume();
        if (this.mPdfViewCtrl != null && this.mPdfViewCtrl.getToolManager() != null && ((ToolManager)this.mPdfViewCtrl.getToolManager()).canResumePdfDocWithoutReloading()) {
            this.addDocPages();
        }

    }

    public void onStart() {
        super.onStart();
        AnalyticsHandlerAdapter.getInstance().sendTimedEvent(27);
    }

    public void onStop() {
        super.onStop();
        AnalyticsHandlerAdapter.getInstance().endTimedEvent(27);
    }

    public void addDocPages() {
        if (this.mAddDocPagesDelay && this.mDataDelay != null) {
            this.mAddDocPagesDelay = false;
            this.mAdapter.addDocPages(this.mPositionDelay, this.mDocumentFormatDelay, this.mDataDelay);
        }

    }

    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (this.mOutputFileUri != null) {
            outState.putParcelable("output_file_uri", this.mOutputFileUri);
        }

    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Activity activity = this.getActivity();
        if (activity != null) {
            if (resultCode == -1) {
                if (requestCode == 10004 || requestCode == 10003) {
                    this.mPositionDelay = this.getLastSelectedPage();
                    if (requestCode == 10004) {
                        this.mDocumentFormatDelay = ThumbnailsViewAdapter.DocumentFormat.PDF_DOC;
                        if (data == null || data.getData() == null) {
                            return;
                        }

                        this.mDataDelay = data.getData();
                    } else {
                        this.mDocumentFormatDelay = ThumbnailsViewAdapter.DocumentFormat.IMAGE;

                        try {
                            Map imageIntent = ViewerUtils.readImageIntent(data, activity, this.mOutputFileUri);
                            if (!ViewerUtils.checkImageIntent(imageIntent)) {
                                Utils.handlePdfFromImageFailed(activity, imageIntent);
                                return;
                            }

                            this.mDataDelay = ViewerUtils.getImageUri(imageIntent);
                            AnalyticsHandlerAdapter.getInstance().sendEvent(29, AnalyticsParam.thumbnailsViewParam(ViewerUtils.isImageFromCamera(imageIntent) ? 8 : 7));
                        } catch (FileNotFoundException var6) {
                        }
                    }

                    if (this.mDataDelay != null) {
                        this.mAddDocPagesDelay = true;
                        this.mHasEventAction = true;
                    }
                }

            }
        }
    }

    public ThumbnailDialogFragment setPdfViewCtrl(@NonNull PDFViewCtrl pdfViewCtrl) {
        this.mPdfViewCtrl = pdfViewCtrl;
        return this;
    }

    public void setOnThumbnailsViewDialogDismissListener(ThumbnailDialogFragment.OnThumbnailsViewDialogDismissListener listener) {
        this.mOnThumbnailsViewDialogDismissListener = listener;
    }

    public void setOnThumbnailsEditAttemptWhileReadOnlyListener(ThumbnailDialogFragment.OnThumbnailsEditAttemptWhileReadOnlyListener listener) {
        this.mOnThumbnailsEditAttemptWhileReadOnlyListener = listener;
    }

    public void setOnExportThumbnailsListener(ThumbnailDialogFragment.OnExportThumbnailsListener listener) {
        this.mOnExportThumbnailsListener = listener;
    }

    public void setItemChecked(int position) {
        this.mInitSelectedItem = position;
    }

    private void launchAndroidFilePicker() {
        Intent intent = new Intent("android.intent.action.GET_CONTENT");
        intent.setType("*/*");
        intent.addCategory("android.intent.category.OPENABLE");
        intent.putExtra("android.content.extra.SHOW_ADVANCED", true);
        intent.putExtra("android.provider.extra.SHOW_ADVANCED", true);
        this.startActivityForResult(intent, 10004);
    }

    private int getLastSelectedPage() {
        int lastSelectedPage = -1;
        if (this.mItemSelectionHelper.getCheckedItemCount() > 0) {
            lastSelectedPage = -2147483648;
            SparseBooleanArray selectedItems = this.mItemSelectionHelper.getCheckedItemPositions();

            for(int i = 0; i < selectedItems.size(); ++i) {
                if (selectedItems.valueAt(i)) {
                    int position = selectedItems.keyAt(i);
                    Integer itemMap = this.mAdapter.getItem(position);
                    if (itemMap != null) {
                        int pageNum = itemMap;
                        if (pageNum > lastSelectedPage) {
                            lastSelectedPage = pageNum;
                        }
                    }
                }
            }
        }

        return lastSelectedPage;
    }

    public void setTitle(String title) {
        this.mTitle = title;
        if (this.mToolbar != null) {
            this.mToolbar.setTitle(title);
        }

    }

    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (this.mAdapter != null) {
            int viewWidth = this.getDisplayWidth();
            int thumbSize = this.getResources().getDimensionPixelSize(R.dimen.controls_thumbnails_view_image_width);
            int thumbSpacing = this.getResources().getDimensionPixelSize(R.dimen.controls_thumbnails_view_grid_spacing);
            this.mSpanCount = (int)Math.floor((double)(viewWidth / (thumbSize + thumbSpacing)));
            this.mAdapter.updateMainViewWidth(viewWidth);
            this.updateSpanCount(this.mSpanCount);
        }

        if (this.mActionMode != null) {
            this.mActionMode.invalidate();
        }

    }

    private int getMainViewWidth() {
        return this.mRecyclerView != null && ViewCompat.isLaidOut(this.mRecyclerView) ? this.mRecyclerView.getMeasuredWidth() : this.getDisplayWidth();
    }

    private int getDisplayWidth() {
        DisplayMetrics metrics = this.getResources().getDisplayMetrics();
        return metrics.widthPixels;
    }

    public void updateSpanCount(int count) {
        this.mSpanCount = count;
        this.mRecyclerView.updateSpanCount(count);
    }

    public ThumbnailsViewAdapter getAdapter() {
        return this.mAdapter;
    }

    private boolean finishActionMode() {
        boolean success = false;
        if (this.mActionMode != null) {
            success = true;
            this.mActionMode.finish();
            this.mActionMode = null;
        }

        this.clearSelectedList();
        return success;
    }

    private void clearSelectedList() {
        if (this.mItemSelectionHelper != null) {
            this.mItemSelectionHelper.clearChoices();
        }

        if (this.mActionMode != null) {
            this.mActionMode.invalidate();
        }

    }

    private boolean onBackPressed() {
        if (!this.isAdded()) {
            return false;
        } else {
            boolean handled = false;
            if (this.mActionMode != null) {
                handled = this.finishActionMode();
            }

            return handled;
        }
    }

    public void onDestroyView() {
        super.onDestroyView();
        this.mDisposables.clear();
    }

    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        if (this.mPdfViewCtrl != null && this.mPdfViewCtrl.getDoc() != null) {
            AnalyticsHandlerAdapter.getInstance().sendEvent(28, AnalyticsParam.noActionParam(this.mHasEventAction));

            try {
                if (this.mAdapter.getDocPagesModified()) {
                    this.mPdfViewCtrl.updatePageLayout();
                }

                this.mPdfViewCtrl.setCurrentPage(this.mAdapter.getCurrentPage());
            } catch (Exception var4) {
                AnalyticsHandlerAdapter.getInstance().sendException(var4);
            }

            this.mAdapter.clearResources();
            this.mAdapter.finish();

            try {
                this.mPdfViewCtrl.cancelAllThumbRequests();
            } catch (Exception var3) {
                AnalyticsHandlerAdapter.getInstance().sendException(var3);
            }

            if (this.mOnThumbnailsViewDialogDismissListener != null) {
                this.mOnThumbnailsViewDialogDismissListener.onThumbnailsViewDialogDismiss(this.mAdapter.getCurrentPage(), this.mAdapter.getDocPagesModified());
            }

        }
    }

    private void manageAddPages(List<Integer> pageList) {
        if (this.mPdfViewCtrl == null) {
            throw new NullPointerException("setPdfViewCtrl() must be called with a valid PDFViewCtrl");
        } else {
            ToolManager toolManager = (ToolManager)this.mPdfViewCtrl.getToolManager();
            if (toolManager != null) {
                toolManager.raisePagesAdded(pageList);
            }

            this.updateUndoRedoIcons();
        }
    }

    private void manageDeletePages(List<Integer> pageList) {
        if (this.mPdfViewCtrl == null) {
            throw new NullPointerException("setPdfViewCtrl() must be called with a valid PDFViewCtrl");
        } else {
            ToolManager toolManager = (ToolManager)this.mPdfViewCtrl.getToolManager();
            if (toolManager != null) {
                toolManager.raisePagesDeleted(pageList);
            }

            this.updateUndoRedoIcons();
        }
    }

    private void manageRotatePages(List<Integer> pageList) {
        if (this.mPdfViewCtrl == null) {
            throw new NullPointerException("setPdfViewCtrl() must be called with a valid PDFViewCtrl");
        } else {
            ToolManager toolManager = (ToolManager)this.mPdfViewCtrl.getToolManager();
            if (toolManager != null) {
                toolManager.raisePagesRotated(pageList);
            }

            this.updateUndoRedoIcons();
        }
    }

    private void manageMovePage(int fromPageNum, int toPageNum) {
        if (this.mPdfViewCtrl == null) {
            throw new NullPointerException("setPdfViewCtrl() must be called with a valid PDFViewCtrl");
        } else {
            ToolManager toolManager = (ToolManager)this.mPdfViewCtrl.getToolManager();
            if (toolManager != null) {
                toolManager.raisePageMoved(fromPageNum, toPageNum);
            }

            this.updateUndoRedoIcons();
        }
    }

    private void managePageLabelChanged() {
        if (this.mPdfViewCtrl == null) {
            throw new NullPointerException("setPdfViewCtrl() must be called with a valid PDFViewCtrl");
        } else {
            ToolManager toolManager = (ToolManager)this.mPdfViewCtrl.getToolManager();
            if (toolManager != null) {
                toolManager.raisePageLabelChangedEvent();
            }

            this.updateUndoRedoIcons();
        }
    }

    public void onPagesAdded(List<Integer> pageList) {
        this.manageAddPages(pageList);
        if (this.mDocumentFormatDelay != null) {
            if (this.mDocumentFormatDelay == ThumbnailsViewAdapter.DocumentFormat.PDF_DOC) {
                AnalyticsHandlerAdapter.getInstance().sendEvent(29, AnalyticsParam.thumbnailsViewCountParam(6, pageList.size()));
            }

            this.mDocumentFormatDelay = null;
        }

    }

    public void onPageMoved(int fromPageNum, int toPageNum) {
        this.manageMovePage(fromPageNum, toPageNum);
        AnalyticsHandlerAdapter.getInstance().sendEvent(29, AnalyticsParam.thumbnailsViewParam(9));
    }

    public void updateUndoRedoIcons() {
        if (this.mPdfViewCtrl == null) {
            throw new NullPointerException("setPdfViewCtrl() must be called with a valid PDFViewCtrl");
        } else {
            if (this.mMenuItemUndo != null && this.mMenuItemRedo != null) {
                boolean undoEnabled = false;
                boolean redoEnabled = false;
                ToolManager toolManager = (ToolManager)this.mPdfViewCtrl.getToolManager();
                if (toolManager != null) {
                    UndoRedoManager undoRedoManager = toolManager.getUndoRedoManger();
                    if (undoRedoManager != null) {
                        undoEnabled = undoRedoManager.isNextUndoEditPageAction();
                        redoEnabled = undoRedoManager.isNextRedoEditPageAction();
                    }
                }

                this.mMenuItemUndo.setEnabled(undoEnabled);
                if (this.mMenuItemUndo.getIcon() != null) {
                    this.mMenuItemUndo.getIcon().setAlpha(undoEnabled ? 255 : 150);
                }

                this.mMenuItemRedo.setEnabled(redoEnabled);
                if (this.mMenuItemRedo.getIcon() != null) {
                    this.mMenuItemRedo.getIcon().setAlpha(redoEnabled ? 255 : 150);
                }
            }

        }
    }

    public interface OnExportThumbnailsListener {
        void onExportThumbnails(SparseBooleanArray var1);
    }

    public interface OnThumbnailsEditAttemptWhileReadOnlyListener {
        void onThumbnailsEditAttemptWhileReadOnly();
    }

    public interface OnThumbnailsViewDialogDismissListener {
        void onThumbnailsViewDialogDismiss(int var1, boolean var2);
    }
}