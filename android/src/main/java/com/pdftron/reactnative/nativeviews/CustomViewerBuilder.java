package com.pdftron.reactnative.nativeviews;

import android.net.Uri;
import android.os.Parcel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pdftron.pdf.config.GenericViewerBuilder;

import java.io.File;

public class CustomViewerBuilder extends GenericViewerBuilder<CustomPdfViewCtrlTabHostFragment, RNPdfViewCtrlTabFragment, CustomViewerBuilder> {
    public static final Creator<CustomViewerBuilder> CREATOR = new Creator<CustomViewerBuilder>() {
        public CustomViewerBuilder createFromParcel(Parcel source) {
            return new CustomViewerBuilder(source);
        }

        public CustomViewerBuilder[] newArray(int size) {
            return new CustomViewerBuilder[size];
        }
    };

    private CustomViewerBuilder() {
    }

    protected Class<RNPdfViewCtrlTabFragment> useDefaultTabFragmentClass() {
        return RNPdfViewCtrlTabFragment.class;
    }

    protected Class<CustomPdfViewCtrlTabHostFragment> useDefaultTabHostFragmentClass() {
        return CustomPdfViewCtrlTabHostFragment.class;
    }

    @NonNull
    protected CustomViewerBuilder useBuilder() {
        return this;
    }

    public static CustomViewerBuilder withUri(@Nullable Uri file, @Nullable String password) {
        CustomViewerBuilder builder = new CustomViewerBuilder();
        builder.mFile = file;
        builder.mPassword = password;
        return builder;
    }

    public static CustomViewerBuilder withUri(@Nullable Uri file) {
        return withUri(file, (String)null);
    }

    public static CustomViewerBuilder withFile(@Nullable File file, @Nullable String password) {
        return withUri(file != null ? Uri.fromFile(file) : null, password);
    }

    public static CustomViewerBuilder withFile(@Nullable File file) {
        return withUri(file != null ? Uri.fromFile(file) : null, (String)null);
    }

    protected CustomViewerBuilder(Parcel in) {
        super(in);
    }
}
