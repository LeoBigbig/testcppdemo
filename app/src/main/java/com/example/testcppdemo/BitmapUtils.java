package com.example.testcppdemo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Bitmap处理工具
 */
public class BitmapUtils {
    private BitmapUtils() {
    }

    /**
     * 保存图片为JPEG
     *
     * @param bitmap
     * @param path
     */
    public static boolean saveJPGE_After(Bitmap bitmap, String path, int quality) {
        File file = new File(path);
        makeDir(file);
        try {
            FileOutputStream out = new FileOutputStream(file);
            if (bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)) {
                out.flush();
                out.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * 保存图片为JPEG
     *
     * @param bitmap
     * @param path
     */
    public static void saveJPGE_After(Context context, Bitmap bitmap, String path, int quality) {
        File file = new File(path);
        makeDir(file);
        try {
            FileOutputStream out = new FileOutputStream(file);
            if (bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)) {
                out.flush();
                out.close();
            }
            updateResources(context, file.getPath());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static FileOutputStream getFos(String path) throws Exception {
        File file = new File(path);
        makeDir(file);
        return new FileOutputStream(file);

    }
    /**
     * 创建文件目录
     */
    private static void makeDir(File file) {
        File tempPath = new File(file.getParent());
        if (!tempPath.exists()) {
            tempPath.mkdirs();
        }
    }

    /**
     * 将手机中的文件转换为Bitmap类型
     *
     * @param f
     * @return
     */
    public static Bitmap getBitemapFromFile(File f) {
        if (!f.exists())
            return null;
        try {
            return BitmapFactory.decodeFile(f.getAbsolutePath());
        } catch (Exception ex) {
            return null;
        }
    }
    /**
     * 发布媒体广播，开启系统扫描
     */
    public static void updateResources(Context context, String path) {
//        MediaScannerConnection.scanFile(context, new String[]{path}, null, null);//发生context内存泄漏
        new SingleMediaScanner(context, path, null);
    }
}