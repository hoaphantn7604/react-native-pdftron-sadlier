package com.pdftron.reactnative.nativeviews;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.PDFViewCtrl.PagePresentationMode;
import com.pdftron.pdf.controls.ReflowPagerAdapter;
import com.pdftron.pdf.dialog.CustomColorModeDialogFragment;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.AnalyticsParam;
import com.pdftron.pdf.utils.CommonToast;
import com.pdftron.pdf.utils.PdfViewCtrlSettingsManager;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.widget.InertSwitch;
import com.pdftron.reactnative.R;
import com.pdftron.reactnative.R.id;
import com.pdftron.reactnative.R.layout;
import com.pdftron.reactnative.R.string;
import com.pdftron.reactnative.R.drawable;
import com.pdftron.reactnative.R.color;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public class CustomViewModePickerDialogFragment extends DialogFragment {
    protected static final String BUNDLE_CURRENT_VIEW_MODE = "current_view_mode";
    protected static final String BUNDLE_CURRENT_RTL_MODE = "current_rtl_mode";
    protected static final String BUNDLE_CURRENT_REFLOW_MODE = "current_reflow_mode";
    protected static final String BUNDLE_CURRENT_REFLOW_TEXT_SIZE = "current_reflow_text_size";
    protected static final String BUNDLE_HIDDEN_ITEMS = "disabled_view_mode_items";
    protected static final String BUNDLE_ACTION = "action";
    protected static final int ITEM_ID_CONTINUOUS = 100;
    protected static final int ITEM_ID_TEXT_SIZE = 101;
    protected static final int ITEM_ID_ROTATION = 103;
    protected static final int ITEM_ID_USERCROP = 105;
    protected static final int ITEM_ID_RTLMODE = 106;
    protected static final int ITEM_ID_BLANK = 107;
    protected static final int ITEM_ID_COLORMODE = 108;
    protected static final int ITEM_ID_REFLOW = 109;
    protected static final int ITEM_ID_FACING_COVER = 110;
    protected static final String KEY_ITEM_ICON = "item_view_mode_picker_list_icon";
    protected static final String KEY_ITEM_TEXT = "item_view_mode_picker_list_text";
    protected static final String KEY_ITEM_ID = "item_view_mode_picker_list_id";
    protected static final String KEY_ITEM_CONTROL = "item_view_mode_picker_list_control";
    protected static final int CONTROL_TYPE_RADIO = 0;
    protected static final int CONTROL_TYPE_SWITCH = 1;
    protected static final int CONTROL_TYPE_SIZE = 2;
    protected static final int CONTROL_TYPE_NONE = 3;
    private boolean mHasEventAction;
    protected RelativeLayout mColorModeLayout;
    protected LinearLayout mViewModeLayout;
    protected ListView mOptionListView;
    protected CustomViewModePickerDialogFragment.SeparatedListAdapter mListAdapter;
    protected PagePresentationMode mCurrentViewMode;
    protected boolean mIsRtlMode;
    protected boolean mIsReflowMode;
    protected int mReflowTextSize;
    protected List<Map<String, Object>> mViewModeOptionsList;
    protected CustomViewModePickerDialogFragment.CustomViewModePickerDialogFragmentListener mViewModePickerDialogListener;
    @NonNull
    protected List<Integer> mHiddenItems;

    public CustomViewModePickerDialogFragment() {
    }

    public static CustomViewModePickerDialogFragment newInstance(PDFViewCtrl.PagePresentationMode currentViewMode, boolean isRTLMode, boolean isReflowMode, int reflowTextSize, @Nullable ArrayList<Integer> hiddenItems) {
        CustomViewModePickerDialogFragment f = new CustomViewModePickerDialogFragment();
        Bundle args = new Bundle();
        args.putInt("current_view_mode", currentViewMode.getValue());
        args.putBoolean("current_rtl_mode", isRTLMode);
        args.putBoolean("current_reflow_mode", isReflowMode);
        args.putInt("current_reflow_text_size", reflowTextSize);
        if (hiddenItems != null) {
            args.putIntegerArrayList("disabled_view_mode_items", hiddenItems);
        }

        f.setArguments(args);
        return f;
    }

    public static CustomViewModePickerDialogFragment newInstance(PagePresentationMode currentViewMode, boolean isRTLMode, boolean isReflowMode, int reflowTextSize) {
        return newInstance(currentViewMode, isRTLMode, isReflowMode, reflowTextSize, (ArrayList)null);
    }

    protected void populateOptionsList() {
        Context context = this.getContext();
        if (context != null) {
            this.mViewModeOptionsList = new ArrayList();
            Resources res = this.getResources();
            this.addItem(this.mViewModeOptionsList, this.createItem(100, res.getDrawable(drawable.ic_view_mode_continuous_black_24dp), this.getString(string.pref_viewmode_scrolling_direction), 1));
            this.addItem(this.mViewModeOptionsList, this.createItem(101, res.getDrawable(drawable.ic_font_size_black_24dp), this.getString(string.pref_viewmode_reflow_text_size), 2));
            this.addItem(this.mViewModeOptionsList, this.createItem(108, (Drawable)null, (String)null, 0));
            if (PdfViewCtrlSettingsManager.hasRtlModeOption(context)) {
                this.addItem(this.mViewModeOptionsList, this.createItem(106, res.getDrawable(drawable.rtl), this.getString(string.pref_viewmode_rtl_mode), 1));
            }

            if (this.mViewModeOptionsList.size() > 0) {
                this.addItem(this.mViewModeOptionsList, this.createItem(107, (Drawable)null, (String)null, 3));
                this.mListAdapter.addSection((String)null, new CustomViewModePickerDialogFragment.CustomViewModeEntryAdapter(this.getActivity(), this.mViewModeOptionsList));
            }

            List<Map<String, Object>> list = new ArrayList();
            this.addItem(list, this.createItem(103, res.getDrawable(drawable.ic_rotate_right_black_24dp), "Rotate Pages", 3));
            this.addItem(list, this.createItem(105, res.getDrawable(drawable.user_crop), this.getString(string.pref_viewmode_user_crop), 3));
            if (list.size() > 0) {
                this.mListAdapter.addSection(this.getString(string.pref_viewmode_actions), new CustomViewModeEntryAdapter(this.getActivity(), list));
            }

        }
    }

    private void addItem(List<Map<String, Object>> list, Map<String, Object> item) {
        int id = (Integer)item.get("item_view_mode_picker_list_id");
        if (!this.mHiddenItems.contains(id)) {
            list.add(item);
        }

    }

    protected Map<String, Object> createItem(int id, Drawable drawable, String title, int controlType) {
        Map<String, Object> item = new HashMap();
        item.put("item_view_mode_picker_list_id", id);
        item.put("item_view_mode_picker_list_icon", drawable);
        item.put("item_view_mode_picker_list_text", title);
        item.put("item_view_mode_picker_list_control", controlType);
        return item;
    }

    public void setViewModePickerDialogFragmentListener(CustomViewModePickerDialogFragmentListener listener) {
        this.mViewModePickerDialogListener = listener;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (this.getArguments() != null) {
            int mode = this.getArguments().getInt("current_view_mode", PagePresentationMode.SINGLE.getValue());
            this.mCurrentViewMode = PagePresentationMode.valueOf(mode);
            this.mIsRtlMode = this.getArguments().getBoolean("current_rtl_mode", false);
            this.mIsReflowMode = this.getArguments().getBoolean("current_reflow_mode", false);
            this.mReflowTextSize = this.getArguments().getInt("current_reflow_text_size", 100);
            this.mHasEventAction = this.getArguments().getBoolean("action", false);
            this.mHiddenItems = this.getArguments().getIntegerArrayList("disabled_view_mode_items");
            if (this.mHiddenItems == null) {
                this.mHiddenItems = new ArrayList();
            }

        }
    }

    public void onStart() {
        super.onStart();
        AnalyticsHandlerAdapter.getInstance().sendTimedEvent(24);
    }

    public void onStop() {
        super.onStop();
        AnalyticsHandlerAdapter.getInstance().endTimedEvent(24);
    }

    public void onDismiss(DialogInterface dialog) {
        if (this.mViewModePickerDialogListener != null) {
            this.mViewModePickerDialogListener.onViewModePickerDialogFragmentDismiss();
        }

        super.onDismiss(dialog);
        AnalyticsHandlerAdapter.getInstance().sendEvent(25, AnalyticsParam.noActionParam(this.mHasEventAction));
    }

    public void setListViewHeightBasedOnChildren(ListView listView, LinearLayout layout) {
        if (this.mListAdapter != null) {
            int totalHeight = 0;

            for(int i = 0; i < this.mListAdapter.getCount(); ++i) {
                View listItem = this.mListAdapter.getView(i, (View)null, listView);
                listItem.setLayoutParams(new ViewGroup.LayoutParams(0, 0));
                listItem.measure(0, 0);
                totalHeight += listItem.getMeasuredHeight();
            }

            ViewGroup.LayoutParams params = listView.getLayoutParams();
            params.height = totalHeight + listView.getDividerHeight() * (this.mListAdapter.getCount() - 1) + 10;
            listView.setLayoutParams(params);
            listView.requestLayout();
            ViewGroup.LayoutParams params2 = layout.getLayoutParams();
            params2.height = totalHeight + listView.getDividerHeight() * (this.mListAdapter.getCount() - 1) + 10;
            layout.setLayoutParams(params2);
            layout.requestLayout();
        }
    }

    @NonNull
    @SuppressLint({"InflateParams"})
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity());
        LayoutInflater inflater = this.getActivity().getLayoutInflater();
        View view = inflater.inflate(layout.custom_fragment_view_mode_picker_dialog, (ViewGroup)null);
        builder.setView(view);
        this.mViewModeLayout = (TableLayout)view.findViewById(id.fragment_view_mode_button_table_layout);
        View reflowItem = this.mViewModeLayout.findViewById(id.fragment_view_mode_button_reflow);
        View facingCover = this.mViewModeLayout.findViewById(id.fragment_view_mode_button_cover);
        View facing = this.mViewModeLayout.findViewById(id.fragment_view_mode_button_facing);

//        facing.setVisibility(View.GONE);
//        reflowItem.setVisibility(View.GONE);

        for(int i = 0; i < this.mViewModeLayout.getChildCount() * 2; ++i) {
            View child = ((TableRow)this.mViewModeLayout.getChildAt(i / 2)).getChildAt(i % 2);
            child.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    int id = v.getId();
                    if (id != R.id.fragment_view_mode_button_reflow || CustomViewModePickerDialogFragment.this.mViewModePickerDialogListener == null || !CustomViewModePickerDialogFragment.this.mViewModePickerDialogListener.checkTabConversionAndAlert(string.cant_reflow_while_converting_message, true)) {
                        CustomViewModePickerDialogFragment.this.mHasEventAction = true;
                        int viewMode = -1;
                        if (id == R.id.fragment_view_mode_button_single) {
                            viewMode = 1;
                        } else if (id == R.id.fragment_view_mode_button_facing) {
                            viewMode = 2;
                        } else if (id == R.id.fragment_view_mode_button_cover) {
                            viewMode = 3;
                        } else if (id == R.id.fragment_view_mode_button_reflow) {
                            viewMode = 4;
                        }

                        if (viewMode != -1) {
                            boolean isCurrent = id == CustomViewModePickerDialogFragment.this.getActiveMode();
                            AnalyticsHandlerAdapter.getInstance().sendEvent(26, AnalyticsParam.viewModeParam(viewMode, isCurrent));
                        }

                        if (id != CustomViewModePickerDialogFragment.this.getActiveMode()) {
                            CustomViewModePickerDialogFragment.this.setActiveMode(v.getId());
                            CustomViewModePickerDialogFragment.this.setViewMode(CustomViewModePickerDialogFragment.this.isContinuousMode());
                            CustomViewModePickerDialogFragment.this.updateDialogLayout();
                        }

                    }
                }
            });
            child.setOnLongClickListener(new View.OnLongClickListener() {
                public boolean onLongClick(View v) {
                    String description = v.getContentDescription().toString();
                    int[] location = new int[2];
                    v.getLocationOnScreen(location);
                    CommonToast.showText(CustomViewModePickerDialogFragment.this.getActivity(), description, 0, 8388659, location[0], location[1] + v.getMeasuredHeight() / 2);
                    return true;
                }
            });
            View img = ((LinearLayout)child).getChildAt(0);
            if (img instanceof ImageView) {
                ImageView imageView = (ImageView)img;
                Drawable icon = imageView.getDrawable();
                if (icon != null && icon.getConstantState() != null) {
                    icon = DrawableCompat.wrap(icon.getConstantState().newDrawable()).mutate();
                    ColorStateList iconTintList = AppCompatResources.getColorStateList(this.getActivity(), color.selector_action_item_icon_color);
                    DrawableCompat.setTintList(icon, iconTintList);
                }

                imageView.setImageDrawable(icon);
            }
        }

        this.mOptionListView = (ListView)view.findViewById(id.fragment_view_mode_picker_dialog_listview);
        this.mOptionListView.setChoiceMode(2);
        this.mOptionListView.setItemsCanFocus(false);
        View v = new View(this.getActivity());
        v.setBackground(this.mOptionListView.getDivider());
        v.setLayoutParams(new android.widget.AbsListView.LayoutParams(-1, this.mOptionListView.getDividerHeight()));
        this.mOptionListView.addHeaderView(v);
        this.mListAdapter = new CustomViewModePickerDialogFragment.SeparatedListAdapter(this.getActivity());
        this.populateOptionsList();
        this.mOptionListView.setAdapter(this.mListAdapter);
        this.setListViewHeightBasedOnChildren(this.mOptionListView, (LinearLayout)view.findViewById(id.scroll_layout));
        this.mOptionListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view1, int position, long id) {
                switch((int)id) {
                    case 100:
                        boolean checked = CustomViewModePickerDialogFragment.this.mOptionListView.isItemChecked(position);
                        CustomViewModePickerDialogFragment.this.setViewMode(checked);
                        CustomViewModePickerDialogFragment.this.mHasEventAction = true;
                        AnalyticsHandlerAdapter.getInstance().sendEvent(26, AnalyticsParam.viewModeParam(checked ? 5 : 6));
                    case 101:
                    case 102:
                    case 104:
                    default:
                        break;
                    case 103:
                        if (!CustomViewModePickerDialogFragment.this.mIsReflowMode) {
                            if (CustomViewModePickerDialogFragment.this.mViewModePickerDialogListener != null) {
                                CustomViewModePickerDialogFragment.this.mViewModePickerDialogListener.onViewModeSelected("rotation");
                            }

                            CustomViewModePickerDialogFragment.this.mHasEventAction = true;
                            AnalyticsHandlerAdapter.getInstance().sendEvent(26, AnalyticsParam.viewModeParam(13));
                        }
                        break;
                    case 105:
                        if (!CustomViewModePickerDialogFragment.this.mIsReflowMode) {
                            if (CustomViewModePickerDialogFragment.this.mViewModePickerDialogListener != null) {
                                CustomViewModePickerDialogFragment.this.mViewModePickerDialogListener.onViewModeSelected("user_crop");
                            }

                            CustomViewModePickerDialogFragment.this.mHasEventAction = true;
                            AnalyticsHandlerAdapter.getInstance().sendEvent(26, AnalyticsParam.viewModeParam(14));
                            CustomViewModePickerDialogFragment.this.dismiss();
                        }
                        break;
                    case 106:
                        CustomViewModePickerDialogFragment.this.mIsRtlMode = !CustomViewModePickerDialogFragment.this.mIsRtlMode;
                        if (CustomViewModePickerDialogFragment.this.mViewModePickerDialogListener != null) {
                            CustomViewModePickerDialogFragment.this.mViewModePickerDialogListener.onViewModeSelected("pref_rtlmode");
                        }

                        CustomViewModePickerDialogFragment.this.mHasEventAction = true;
                        AnalyticsHandlerAdapter.getInstance().sendEvent(26, AnalyticsParam.viewModeParam(CustomViewModePickerDialogFragment.this.mIsRtlMode ? 11 : 12));
                }

            }
        });
        builder.setPositiveButton(this.getResources().getString(string.ok), new android.content.DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int i) {
                CustomViewModePickerDialogFragment.this.dismiss();
            }
        });
        int checkedItem = -1;
        if (this.mIsReflowMode) {
            checkedItem = id.fragment_view_mode_button_reflow;
        } else {
            switch(this.mCurrentViewMode) {
                case SINGLE:
                    checkedItem = id.fragment_view_mode_button_single;
                    break;
                case SINGLE_CONT:
                    checkedItem = id.fragment_view_mode_button_single;
                    break;
                case FACING:
                    checkedItem = id.fragment_view_mode_button_facing;
                    break;
                case FACING_COVER:
                    checkedItem = id.fragment_view_mode_button_cover;
                    break;
                case FACING_CONT:
                    checkedItem = id.fragment_view_mode_button_facing;
                    break;
                case FACING_COVER_CONT:
                    checkedItem = id.fragment_view_mode_button_cover;
            }
        }

        if (this.mIsReflowMode) {
            this.listSwapFirstSecondViewModeOptions();
        }

        this.setActiveMode(checkedItem);
        this.updateDialogLayout();
        final ScrollView sView = (ScrollView)view.findViewById(id.viewMode_scrollView);

        try {
            sView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                public void onGlobalLayout() {
                    try {
                        sView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    } catch (Exception var2) {
                    }

                    sView.fullScroll(33);
                }
            });
        } catch (Exception var13) {
        }

        return builder.create();
    }

    private boolean isContinuousMode() {
        boolean continuous = false;
        switch(this.mCurrentViewMode) {
            case SINGLE:
                continuous = false;
                break;
            case SINGLE_CONT:
                continuous = true;
                break;
            case FACING:
                continuous = false;
                break;
            case FACING_COVER:
                continuous = false;
                break;
            case FACING_CONT:
                continuous = true;
                break;
            case FACING_COVER_CONT:
                continuous = true;
        }

        return continuous;
    }

    private int getActiveMode() {
        for(int i = 0; i < this.mViewModeLayout.getChildCount() * 2; ++i) {
            View view = ((TableRow)this.mViewModeLayout.getChildAt(i / 2)).getChildAt(i % 2);
            if (view.isActivated()) {
                return view.getId();
            }
        }

        return -1;
    }

    private void setActiveMode(int id) {
        for(int i = 0; i < this.mViewModeLayout.getChildCount() * 2; ++i) {
            View view = ((TableRow)this.mViewModeLayout.getChildAt(i / 2)).getChildAt(i % 2);
            view.setActivated(id == view.getId());
        }

    }

    private void setButtonChecked(@IdRes int buttonId, @DrawableRes int iconId, boolean checked) {
        Activity activity = this.getActivity();
        if (this.mColorModeLayout != null && activity != null) {
            try {
                LayerDrawable layerDrawable = (LayerDrawable)AppCompatResources.getDrawable(activity, iconId);
                if (layerDrawable != null) {
                    if (checked) {
                        GradientDrawable shape = (GradientDrawable)((GradientDrawable)layerDrawable.findDrawableByLayerId(id.selectable_shape));
                        if (shape != null) {
                            shape.mutate();
                            int w = (int) Utils.convDp2Pix(activity, 4.0F);
                            shape.setStroke(w, Utils.getAccentColor(activity));
                        }
                    }

                    ((ImageButton)this.mColorModeLayout.findViewById(buttonId)).setImageDrawable(layerDrawable);
                }
            } catch (Exception var8) {
                AnalyticsHandlerAdapter.getInstance().sendException(var8);
            }

        }
    }

    protected void setActiveColorMode(@IdRes int id) {
        Context context = this.getContext();
        if (context != null) {
            if (id == R.id.item_view_mode_picker_customcolor_button) {
                this.dismiss();
                CustomColorModeDialogFragment frag = CustomColorModeDialogFragment.newInstance(PdfViewCtrlSettingsManager.getCustomColorModeBGColor(context), PdfViewCtrlSettingsManager.getCustomColorModeTextColor(context));
                frag.setCustomColorModeSelectedListener(new CustomColorModeDialogFragment.CustomColorModeSelectedListener() {
                    public void onCustomColorModeSelected(int bgColor, int txtColor) {
                        if (CustomViewModePickerDialogFragment.this.mViewModePickerDialogListener != null) {
                            CustomViewModePickerDialogFragment.this.mViewModePickerDialogListener.onCustomColorModeSelected(bgColor, txtColor);
                        }

                    }
                });
                frag.setStyle(STYLE_NORMAL, R.style.CustomAppTheme);
                FragmentManager fragmentManager = this.getFragmentManager();
                if (fragmentManager != null) {
                    frag.show(fragmentManager, "custom_color_mode");
                }
            } else {
                this.setButtonChecked(R.id.item_view_mode_picker_daymode_button, drawable.ic_daymode_icon, id == R.id.item_view_mode_picker_daymode_button);
                this.setButtonChecked(R.id.item_view_mode_picker_nightmode_button, drawable.ic_nightmode_icon, id == R.id.item_view_mode_picker_nightmode_button);
                this.setButtonChecked(R.id.item_view_mode_picker_sepiamode_button, drawable.ic_sepiamode_icon, id == R.id.item_view_mode_picker_sepiamode_button);
            }

        }
    }

    private void updateDialogLayout() {
        int activeId = this.getActiveMode();
        this.mIsReflowMode = activeId == id.fragment_view_mode_button_reflow;
        boolean continuous = this.isContinuousMode();

        for(int i = 0; i < this.mOptionListView.getCount(); ++i) {
            int id = (int)this.mOptionListView.getItemIdAtPosition(i);
            switch(id) {
                case 100:
                    this.mOptionListView.setItemChecked(i, continuous);
                    break;
                case 106:
                    this.mOptionListView.setItemChecked(i, this.mIsRtlMode);
            }
        }

        this.mListAdapter.notifyDataSetChanged();
    }

    private void listSwapFirstSecondViewModeOptions() {
        if (this.mViewModeOptionsList.size() > 1) {
            Map<String, Object> tmp = (Map)this.mViewModeOptionsList.get(0);
            this.mViewModeOptionsList.set(0, this.mViewModeOptionsList.get(1));
            this.mViewModeOptionsList.set(1, tmp);
        }

    }

    private void setViewMode(boolean verticalScrolling) {
        int activeId = this.getActiveMode();
        if (this.mViewModeOptionsList.size() > 0 && activeId != id.fragment_view_mode_button_reflow && (Integer)((Map)this.mViewModeOptionsList.get(0)).get("item_view_mode_picker_list_id") == 101) {
            this.listSwapFirstSecondViewModeOptions();
        }

        if (verticalScrolling) {
            if (activeId == id.fragment_view_mode_button_single) {
                if (this.mViewModePickerDialogListener != null) {
                    this.mViewModePickerDialogListener.onViewModeSelected("continuous");
                }

                this.mCurrentViewMode = PagePresentationMode.SINGLE_CONT;
            } else if (activeId == id.fragment_view_mode_button_facing) {
                if (this.mViewModePickerDialogListener != null) {
                    this.mViewModePickerDialogListener.onViewModeSelected("facing_cont");
                }

                this.mCurrentViewMode = PagePresentationMode.FACING_CONT;
            } else if (activeId == id.fragment_view_mode_button_cover) {
                if (this.mViewModePickerDialogListener != null) {
                    this.mViewModePickerDialogListener.onViewModeSelected("facingcover_cont");
                }

                this.mCurrentViewMode = PagePresentationMode.FACING_COVER_CONT;
            } else if (activeId == id.fragment_view_mode_button_reflow) {
                if (this.mViewModeOptionsList.size() > 0 && (Integer)((Map)this.mViewModeOptionsList.get(0)).get("item_view_mode_picker_list_id") == 100) {
                    this.listSwapFirstSecondViewModeOptions();
                }

                if (this.mViewModePickerDialogListener != null) {
                    this.mViewModePickerDialogListener.onViewModeSelected("pref_reflowmode");
                }
            }
        } else if (activeId == id.fragment_view_mode_button_single) {
            if (this.mViewModePickerDialogListener != null) {
                this.mViewModePickerDialogListener.onViewModeSelected("singlepage");
            }

            this.mCurrentViewMode = PagePresentationMode.SINGLE;
        } else if (activeId == id.fragment_view_mode_button_facing) {
            if (this.mViewModePickerDialogListener != null) {
                this.mViewModePickerDialogListener.onViewModeSelected("facing");
            }

            this.mCurrentViewMode = PagePresentationMode.FACING;
        } else if (activeId == id.fragment_view_mode_button_cover) {
            if (this.mViewModePickerDialogListener != null) {
                this.mViewModePickerDialogListener.onViewModeSelected("facingcover");
            }

            this.mCurrentViewMode = PagePresentationMode.FACING_COVER;
        } else if (activeId == id.fragment_view_mode_button_reflow) {
            if (this.mViewModeOptionsList.size() > 0 && (Integer)((Map)this.mViewModeOptionsList.get(0)).get("item_view_mode_picker_list_id") == 100) {
                this.listSwapFirstSecondViewModeOptions();
            }

            if (this.mViewModePickerDialogListener != null) {
                this.mViewModePickerDialogListener.onViewModeSelected("pref_reflowmode");
            }
        }

    }

    protected class SeparatedListAdapter extends BaseAdapter {
        static final int TYPE_SECTION_HEADER = -1;
        final Map<String, Adapter> mSections = new LinkedHashMap();
        final ArrayAdapter<String> mHeaders;

        SeparatedListAdapter(Context context) {
            this.mHeaders = new ArrayAdapter(context, layout.listview_header_view_mode_picker_list);
        }

        void addSection(String section, Adapter adapter) {
            this.mHeaders.add(section);
            this.mSections.put(section, adapter);
        }

        public Object getItem(int position) {
            int size;
            for(Iterator var2 = this.mSections.keySet().iterator(); var2.hasNext(); position -= size) {
                String section = (String)var2.next();
                Adapter adapter = (Adapter)this.mSections.get(section);
                size = adapter.getCount() + 1;
                if (position == 0) {
                    return section;
                }

                if (position < size) {
                    return adapter.getItem(position - 1);
                }
            }

            return null;
        }

        public int getCount() {
            int total = 0;

            Adapter adapter;
            for(Iterator var2 = this.mSections.values().iterator(); var2.hasNext(); total += adapter.getCount() + 1) {
                adapter = (Adapter)var2.next();
            }

            return total;
        }

        public int getViewTypeCount() {
            int total = 1;

            Adapter adapter;
            for(Iterator var2 = this.mSections.values().iterator(); var2.hasNext(); total += adapter.getViewTypeCount()) {
                adapter = (Adapter)var2.next();
            }

            return total;
        }

        public int getItemViewType(int position) {
            int type = 1;

            Adapter adapter;
            for(Iterator var3 = this.mSections.keySet().iterator(); var3.hasNext(); type += adapter.getViewTypeCount()) {
                String section = (String)var3.next();
                adapter = (Adapter)this.mSections.get(section);
                int size = adapter.getCount() + 1;
                if (position == 0) {
                    return -1;
                }

                if (position < size) {
                    return type + adapter.getItemViewType(position - 1);
                }

                position -= size;
            }

            return -1;
        }

        public boolean isEnabled(int position) {
            return this.getItemViewType(position) != -1;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            if (parent == null) {
                return null;
            } else {
                int sectionNum = 0;

                for(Iterator var5 = this.mSections.keySet().iterator(); var5.hasNext(); ++sectionNum) {
                    String section = (String)var5.next();
                    Adapter adapter = (Adapter)this.mSections.get(section);
                    int size = adapter.getCount() + 1;
                    if (position == 0) {
                        if (!Utils.isNullOrEmpty(section)) {
                            return this.mHeaders.getView(sectionNum, convertView, parent);
                        }

                        return new View(parent.getContext());
                    }

                    if (position < size) {
                        return adapter.getView(position - 1, convertView, parent);
                    }

                    position -= size;
                }

                return null;
            }
        }

        public long getItemId(int position) {
            int id = position;

            int size;
            for(Iterator var3 = this.mSections.keySet().iterator(); var3.hasNext(); id -= size) {
                String section = (String)var3.next();
                Adapter adapter = (Adapter)this.mSections.get(section);
                size = adapter.getCount() + 1;
                if (id == 0) {
                    return (long)position;
                }

                if (id < size) {
                    return adapter.getItemId(id - 1);
                }
            }

            return (long)position;
        }
    }

    protected class CustomViewModeEntryAdapter extends ArrayAdapter<Map<String, Object>> {
        private List<Map<String, Object>> mEntries;
        private ColorStateList mIconTintList;
        private ColorStateList mTextTintList;

        CustomViewModeEntryAdapter(Context context, List<Map<String, Object>> entries) {
            super(context, 0, entries);
            this.mEntries = entries;
            this.mIconTintList = AppCompatResources.getColorStateList(this.getContext(), color.selector_color);
            this.mTextTintList = AppCompatResources.getColorStateList(this.getContext(), color.selector_color);
        }

        public void setEntries(List<Map<String, Object>> list) {
            this.mEntries = list;
        }

        @NonNull
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            Map<String, Object> map = (Map)this.mEntries.get(position);
            int id = (Integer)map.get("item_view_mode_picker_list_id");
            final CustomViewModeEntryAdapter.ViewHolder holder;
            int currentColorMode;
            if (convertView != null && convertView.getTag() != null) {
                holder = (CustomViewModeEntryAdapter.ViewHolder)convertView.getTag();
            } else {
                holder = new CustomViewModeEntryAdapter.ViewHolder();
                if (id != 108) {
                    convertView = LayoutInflater.from(parent.getContext()).inflate(layout.listview_item_view_mode_picker_list, parent, false);
                    holder.icon = (ImageView)convertView.findViewById(R.id.item_view_mode_picker_list_icon);
                    holder.text = (TextView)convertView.findViewById(R.id.item_view_mode_picker_list_text);
                    holder.radioButton = (RadioButton)convertView.findViewById(R.id.item_view_mode_picker_list_radiobutton);
                    holder.switchButton = (InertSwitch)convertView.findViewById(R.id.item_view_mode_picker_list_switch);
                    holder.sizeLayout = (LinearLayout)convertView.findViewById(R.id.item_view_mode_picker_list_size_layout);
                    holder.decButton = (ImageButton)convertView.findViewById(R.id.item_view_mode_picker_list_dec);
                    holder.sizeText = (TextView)convertView.findViewById(R.id.item_view_mode_picker_list_size_text);
                    holder.incButton = (ImageButton)convertView.findViewById(R.id.item_view_mode_picker_list_inc);
                    convertView.setTag(holder);
                } else {
                    convertView = LayoutInflater.from(this.getContext()).inflate(layout.fragment_view_mode_color_mode_row, parent, false);
                    RelativeLayout layoutView = (RelativeLayout)convertView;
                    LinearLayout colorBtnLayout = (LinearLayout)layoutView.findViewById(R.id.item_view_mode_picker_modebtn_layout);

                    for(int i = 0; i < colorBtnLayout.getChildCount(); ++i) {
                        View child = colorBtnLayout.getChildAt(i);
                        if (child instanceof ImageButton) {
                            child.setOnClickListener(new View.OnClickListener() {
                                public void onClick(View v) {
                                    int id1 = v.getId();
                                    CustomViewModePickerDialogFragment.this.setActiveColorMode(id1);
                                    if (CustomViewModePickerDialogFragment.this.mViewModePickerDialogListener != null) {
                                        if (id1 == R.id.item_view_mode_picker_daymode_button) {
                                            CustomViewModePickerDialogFragment.this.mHasEventAction = true;
                                            AnalyticsHandlerAdapter.getInstance().sendEvent(26, AnalyticsParam.viewModeParam(7, PdfViewCtrlSettingsManager.getColorMode(CustomViewModeEntryAdapter.this.getContext()) == 1));
                                            if (CustomViewModePickerDialogFragment.this.mViewModePickerDialogListener.onViewModeColorSelected(1)) {
                                                CustomViewModePickerDialogFragment.this.dismiss();
                                            }
                                        } else if (id1 == R.id.item_view_mode_picker_nightmode_button) {
                                            CustomViewModePickerDialogFragment.this.mHasEventAction = true;
                                            AnalyticsHandlerAdapter.getInstance().sendEvent(26, AnalyticsParam.viewModeParam(8, PdfViewCtrlSettingsManager.getColorMode(CustomViewModeEntryAdapter.this.getContext()) == 3));
                                            if (CustomViewModePickerDialogFragment.this.mViewModePickerDialogListener.onViewModeColorSelected(3)) {
                                                CustomViewModePickerDialogFragment.this.dismiss();
                                            }
                                        } else if (id1 == R.id.item_view_mode_picker_sepiamode_button) {
                                            CustomViewModePickerDialogFragment.this.mHasEventAction = true;
                                            AnalyticsHandlerAdapter.getInstance().sendEvent(26, AnalyticsParam.viewModeParam(9, PdfViewCtrlSettingsManager.getColorMode(CustomViewModeEntryAdapter.this.getContext()) == 2));
                                            if (CustomViewModePickerDialogFragment.this.mViewModePickerDialogListener.onViewModeColorSelected(2)) {
                                                CustomViewModePickerDialogFragment.this.dismiss();
                                            }
                                        } else if (id1 == R.id.item_view_mode_picker_customcolor_button) {
                                            CustomViewModePickerDialogFragment.this.mHasEventAction = true;
                                            AnalyticsHandlerAdapter.getInstance().sendEvent(26, AnalyticsParam.viewModeParam(10, PdfViewCtrlSettingsManager.getColorMode(CustomViewModeEntryAdapter.this.getContext()) == 4));
                                        }
                                    }

                                }
                            });
                        }
                    }

                    this.applyTintList((AppCompatImageView)layoutView.findViewById(R.id.item_view_mode_picker_color_list_icon), this.mIconTintList);
                    CustomViewModePickerDialogFragment.this.mColorModeLayout = layoutView;
                    convertView.setTag(holder);
                }

                try {
                    int res = -1;
                    currentColorMode = PdfViewCtrlSettingsManager.getColorMode(this.getContext());
                    if (currentColorMode == 3) {
                        res = R.id.item_view_mode_picker_nightmode_button;
                    } else if (currentColorMode == 2) {
                        res = R.id.item_view_mode_picker_sepiamode_button;
                    } else if (currentColorMode == 1) {
                        res = R.id.item_view_mode_picker_daymode_button;
                    }

                    if (res != -1) {
                        CustomViewModePickerDialogFragment.this.setActiveColorMode(res);
                    }
                } catch (Exception var11) {
                    AnalyticsHandlerAdapter.getInstance().sendException(var11);
                }
            }

            switch(id) {
                case 100:
                    if (CustomViewModePickerDialogFragment.this.mIsReflowMode) {
                        return new View(this.getContext());
                    }
                    break;
                case 101:
                    if (!CustomViewModePickerDialogFragment.this.mIsReflowMode) {
                        return new View(this.getContext());
                    }
                case 102:
                case 104:
                case 106:
                default:
                    break;
                case 103:
                    holder.icon.setEnabled(!CustomViewModePickerDialogFragment.this.mIsReflowMode);
                    holder.text.setEnabled(!CustomViewModePickerDialogFragment.this.mIsReflowMode);
                    break;
                case 105:
                    holder.icon.setEnabled(!CustomViewModePickerDialogFragment.this.mIsReflowMode);
                    holder.text.setEnabled(!CustomViewModePickerDialogFragment.this.mIsReflowMode);
                    break;
                case 107:
                    return new View(this.getContext());
            }

            if (id == 108) {
                return convertView;
            } else {
                Drawable icon = (Drawable)map.get("item_view_mode_picker_list_icon");
                holder.icon.setImageDrawable(icon);
                this.applyTintList(holder.icon, this.mIconTintList);
                holder.text.setText((String)map.get("item_view_mode_picker_list_text"));
                holder.text.setTextColor(this.mTextTintList);
                currentColorMode = (Integer)map.get("item_view_mode_picker_list_control");
                holder.radioButton.setVisibility(currentColorMode == 0 ? View.VISIBLE : View.GONE);
                holder.switchButton.setVisibility(currentColorMode == 1 ? View.VISIBLE : View.GONE);
                holder.sizeLayout.setVisibility(currentColorMode == 2 ? View.VISIBLE : View.GONE);
                if (currentColorMode == 2) {
                    this.applyTintList(holder.decButton, this.mIconTintList);
                    this.applyTintList(holder.incButton, this.mIconTintList);
                    if (CustomViewModePickerDialogFragment.this.mReflowTextSize == ReflowPagerAdapter.TH_MIN_SCAlE) {
                        holder.decButton.setEnabled(false);
                    }

                    if (CustomViewModePickerDialogFragment.this.mReflowTextSize == ReflowPagerAdapter.TH_MAX_SCAlE) {
                        holder.incButton.setEnabled(false);
                    }

                    holder.decButton.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            if (CustomViewModePickerDialogFragment.this.mViewModePickerDialogListener != null) {
                                CustomViewModePickerDialogFragment.this.mReflowTextSize = CustomViewModePickerDialogFragment.this.mViewModePickerDialogListener.onReflowZoomInOut(false);
                            }

                            CustomViewModePickerDialogFragment.this.mHasEventAction = true;
                            AnalyticsHandlerAdapter.getInstance().sendEvent(26, AnalyticsParam.viewModeParam(16));
                            if (CustomViewModePickerDialogFragment.this.mReflowTextSize == ReflowPagerAdapter.TH_MIN_SCAlE) {
                                holder.decButton.setEnabled(false);
                            } else {
                                holder.incButton.setEnabled(true);
                            }

                            holder.sizeText.setText(String.format(Locale.getDefault(), "%d%%", CustomViewModePickerDialogFragment.this.mReflowTextSize));
                        }
                    });
                    holder.incButton.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            if (CustomViewModePickerDialogFragment.this.mViewModePickerDialogListener != null) {
                                CustomViewModePickerDialogFragment.this.mReflowTextSize = CustomViewModePickerDialogFragment.this.mViewModePickerDialogListener.onReflowZoomInOut(true);
                            }

                            CustomViewModePickerDialogFragment.this.mHasEventAction = true;
                            AnalyticsHandlerAdapter.getInstance().sendEvent(26, AnalyticsParam.viewModeParam(15));
                            if (CustomViewModePickerDialogFragment.this.mReflowTextSize == ReflowPagerAdapter.TH_MAX_SCAlE) {
                                holder.incButton.setEnabled(false);
                            } else {
                                holder.decButton.setEnabled(true);
                            }

                            holder.sizeText.setText(String.format(Locale.getDefault(), "%d%%", CustomViewModePickerDialogFragment.this.mReflowTextSize));
                        }
                    });
                    holder.sizeText.setText(String.format(Locale.getDefault(), "%d%%", CustomViewModePickerDialogFragment.this.mReflowTextSize));
                }

                return convertView;
            }
        }

        public long getItemId(int position) {
            Map<String, Object> map = (Map)this.mEntries.get(position);
            return (long)(Integer)map.get("item_view_mode_picker_list_id");
        }

        private void applyTintList(ImageView imageView, ColorStateList tintList) {
            if (imageView != null) {
                Drawable icon = imageView.getDrawable();
                if (icon != null && icon.getConstantState() != null) {
                    try {
                        icon = DrawableCompat.wrap(icon.getConstantState().newDrawable()).mutate();
                        DrawableCompat.setTintList(icon, tintList);
                    } catch (NullPointerException var5) {
                    }
                }

                imageView.setImageDrawable(icon);
            }
        }

        private class ViewHolder {
            ImageView icon;
            TextView text;
            RadioButton radioButton;
            InertSwitch switchButton;
            LinearLayout sizeLayout;
            ImageButton decButton;
            TextView sizeText;
            ImageButton incButton;

            private ViewHolder() {
            }
        }
    }

    public interface CustomViewModePickerDialogFragmentListener {
        void onViewModeSelected(String var1);

        boolean onViewModeColorSelected(int var1);

        boolean onCustomColorModeSelected(int var1, int var2);

        void onViewModePickerDialogFragmentDismiss();

        int onReflowZoomInOut(boolean var1);

        boolean checkTabConversionAndAlert(int var1, boolean var2);
    }

    public static enum ViewModePickerItems {
        ITEM_ID_CONTINUOUS(100),
        ITEM_ID_TEXT_SIZE(101),
        ITEM_ID_ROTATION(103),
        ITEM_ID_USERCROP(105),
        ITEM_ID_COLORMODE(108),
        ITEM_ID_REFLOW(109),
        ITEM_ID_FACING_COVER(110),
        ITEM_ID_FACING(111);

        final int itemId;

        private ViewModePickerItems(int item) {
            this.itemId = item;
        }

        public int getValue() {
            return this.itemId;
        }
    }
}
