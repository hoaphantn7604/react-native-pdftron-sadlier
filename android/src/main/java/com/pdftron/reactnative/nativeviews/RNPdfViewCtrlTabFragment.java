package com.pdftron.reactnative.nativeviews;

import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.PointF;

import androidx.fragment.app.FragmentActivity;

import com.pdftron.pdf.controls.PdfViewCtrlTabFragment;
import com.pdftron.pdf.tools.ToolManager;
import com.pdftron.pdf.utils.ViewerUtils;

import org.apache.commons.io.FilenameUtils;

import java.io.File;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class RNPdfViewCtrlTabFragment extends PdfViewCtrlTabFragment {

    @Override
    protected void initLayout() {
        super.initLayout();
        this.mDownloadDocumentDialog.setMessage("Opening book...");
    }

    @Override
    protected void openConvertibleFormats(String tag) {
        // super.openConvertibleFormats(tag);
        Activity activity = this.getActivity();
        if (activity != null) {
            final ProgressDialog progressDialog = new ProgressDialog(activity);
            this.mDisposables.add(this.convertToPdf(tag).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).doOnSubscribe(new Consumer<Disposable>() {
                public void accept(Disposable disposable) throws Exception {
                    progressDialog.setMessage("Opening book...");
                    progressDialog.setCancelable(false);
                    progressDialog.setProgressStyle(0);
                    progressDialog.setIndeterminate(true);
                    progressDialog.show();
                }
            }).subscribe(filePath -> {
                progressDialog.dismiss();
                RNPdfViewCtrlTabFragment.this.mCurrentFile = new File(filePath);
                String oldTabTag = RNPdfViewCtrlTabFragment.this.mTabTag;
                int oldTabSource = RNPdfViewCtrlTabFragment.this.mTabSource;
                RNPdfViewCtrlTabFragment.this.mTabTag = RNPdfViewCtrlTabFragment.this.mCurrentFile.getAbsolutePath();
                RNPdfViewCtrlTabFragment.this.mTabSource = 2;
                RNPdfViewCtrlTabFragment.this.mTabTitle = FilenameUtils.removeExtension((new File(RNPdfViewCtrlTabFragment.this.mTabTag)).getName());
                if ((!RNPdfViewCtrlTabFragment.this.mTabTag.equals(oldTabTag) || RNPdfViewCtrlTabFragment.this.mTabSource != oldTabSource) && RNPdfViewCtrlTabFragment.this.mTabListener != null) {
                    RNPdfViewCtrlTabFragment.this.mTabListener.onTabIdentityChanged(oldTabTag, RNPdfViewCtrlTabFragment.this.mTabTag, RNPdfViewCtrlTabFragment.this.mTabTitle, RNPdfViewCtrlTabFragment.this.mFileExtension, RNPdfViewCtrlTabFragment.this.mTabSource);
                }

                RNPdfViewCtrlTabFragment.this.mToolManager.setReadOnly(false);
                RNPdfViewCtrlTabFragment.this.openLocalFile(filePath);
            }, throwable -> {
                progressDialog.dismiss();
                RNPdfViewCtrlTabFragment.this.handleOpeningDocumentFailed(1);
            }));
        }
    }

    @Override
    public void imageStamperSelected(PointF targetPoint) {
        // in react native, intent must be sent from the activity
        // to be able to receive by the activity
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return;
        }
        this.mImageCreationMode = ToolManager.ToolMode.STAMPER;
        this.mAnnotTargetPoint = targetPoint;
        this.mOutputFileUri = ViewerUtils.openImageIntent(activity);
    }

    @Override
    public void imageSignatureSelected(PointF targetPoint, int targetPage, Long widget) {
        // in react native, intent must be sent from the activity
        // to be able to receive by the activity
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return;
        }
        mImageCreationMode = ToolManager.ToolMode.SIGNATURE;
        mAnnotTargetPoint = targetPoint;
        mAnnotTargetPage = targetPage;
        mTargetWidget = widget;
        mOutputFileUri = ViewerUtils.openImageIntent(activity);
    }

    @Override
    public void attachFileSelected(PointF targetPoint) {
        // in react native, intent must be sent from the activity
        // to be able to receive by the activity
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return;
        }
        mAnnotTargetPoint = targetPoint;
        ViewerUtils.openFileIntent(activity);
    }
}
