package com.example.testcppdemo;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;

import java.io.File;
import java.lang.reflect.Field;

/**
 * 系统媒体扫描器
 */

public class SingleMediaScanner implements MediaScannerConnection.MediaScannerConnectionClient {

    public interface ScanListener{
        public void onScanFinish();
    }

    private MediaScannerConnection mMs;
    private File mFile;
    private String mPath;
    private ScanListener listener;
    private Context mContext;

    public SingleMediaScanner(Context context, String path, ScanListener l) {
        listener = l;
        mPath = path;
        mContext = context;
        mMs = new MediaScannerConnection(mContext, this);
        mMs.connect();
    }

    @Override
    public void onMediaScannerConnected() {
        mMs.scanFile(mPath, null);
    }

    /**
     * 更新系统媒体 图像数据库
     */
    @Override
    public void onScanCompleted(String path, Uri uri) {
        mMs.disconnect();
        mContext = null;
        if (listener !=null)
        listener.onScanFinish();
        try {
            Field mContext = mMs.getClass().getDeclaredField("mContext");
            if (!mContext.isAccessible()) mContext.setAccessible(true);
            mContext.set(mMs,null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
