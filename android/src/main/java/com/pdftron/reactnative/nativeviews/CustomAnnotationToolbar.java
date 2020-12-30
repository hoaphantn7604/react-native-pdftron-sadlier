package com.pdftron.reactnative.nativeviews;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pdftron.pdf.controls.AnnotationToolbar;
import com.pdftron.reactnative.R;

public class CustomAnnotationToolbar extends AnnotationToolbar {
    private static final String TAG = "ahihi-" + CustomAnnotationToolbar.class.getSimpleName();

    public CustomAnnotationToolbar(@NonNull Context context) {
        super(context);
    }

    public CustomAnnotationToolbar(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomAnnotationToolbar(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public CustomAnnotationToolbar(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public void updateButtonsVisibility() {
        Integer[] listAnnotationToolVisible = new Integer[]{
                R.id.controls_annotation_toolbar_tool_stickynote,
                R.id.controls_annotation_toolbar_tool_line,
                R.id.controls_annotation_toolbar_tool_arrow,
                R.id.controls_annotation_toolbar_tool_rectangle,
                R.id.controls_annotation_toolbar_tool_oval,
                R.id.controls_annotation_toolbar_tool_eraser,
                R.id.controls_annotation_toolbar_tool_free_highlighter,
                R.id.controls_annotation_toolbar_tool_polyline,
                R.id.controls_annotation_toolbar_tool_polygon,
                R.id.controls_annotation_toolbar_tool_freetext,
                R.id.controls_annotation_toolbar_tool_pan,
                R.id.controls_annotation_toolbar_btn_close,
                R.id.controls_annotation_toolbar_tool_freehand
//                R.id.controls_annotation_toolbar_btn_more,
//                R.id.controls_annotation_toolbar_tool_ruler,
//                R.id.controls_annotation_toolbar_tool_perimeter_measure,
//                R.id.controls_annotation_toolbar_tool_area_measure,
//                R.id.controls_annotation_toolbar_tool_cloud,
//                R.id.controls_annotation_toolbar_tool_sound,
//                R.id.controls_annotation_toolbar_tool_callout,
//                R.id.controls_annotation_toolbar_tool_text_underline,
//                R.id.controls_annotation_toolbar_state_edit,
//                R.id.controls_annotation_toolbar_tool_text_highlight,
//                R.id.controls_annotation_toolbar_tool_text_squiggly,
//                R.id.controls_annotation_toolbar_tool_text_strikeout,
//                R.id.controls_annotation_toolbar_tool_multi_select,
//                R.id.controls_annotation_toolbar_tool_stamp,
//                R.id.controls_annotation_toolbar_tool_image_stamper,
//                R.id.controls_annotation_toolbar_tool_rubber_stamper,
//                R.id.controls_annotation_toolbar_tool_stamp,
        };


        for (View mButton : this.mButtons) {
            int viewId = mButton.getId();
            boolean isVisible = false;
            for (Integer visibleViewId : listAnnotationToolVisible) {
                if (visibleViewId.equals(viewId)) {
                    isVisible = true;
                    break;
                }
            }
            mButton.setVisibility(isVisible ? VISIBLE : GONE);
        }
    }
}
