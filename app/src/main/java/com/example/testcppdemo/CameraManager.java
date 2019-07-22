/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.testcppdemo;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;

import java.io.IOException;

/**
 * 相机管理者
 */
public final class CameraManager {

    private static final String TAG = CameraManager.class.getSimpleName();

    private static final int MIN_FRAME_WIDTH = 240;
    private static final int MIN_FRAME_HEIGHT = 240;
    private static final int MAX_FRAME_WIDTH = 480;
    private static final int MAX_FRAME_HEIGHT = 360;

    private static CameraManager cameraManager;

    static final int SDK_INT; // Later we can use Build.VERSION.SDK_INT

    static {
        int sdkInt;
        try {
            sdkInt = Integer.parseInt(Build.VERSION.SDK);
        } catch (NumberFormatException nfe) {
            // Just to be safe
            sdkInt = 10000;
        }
        SDK_INT = sdkInt;
    }

    private final Context context;
    private final CameraConfigurationManager configManager;
    private Camera camera;

    private boolean initialized;//是否初始化
    private boolean previewing;//是否在预览
    private final boolean useOneShotPreviewCallback;
    /**
     * Preview frames are delivered here, which we pass on to the registered handler. Make sure to
     * clear the handler so it will only receive one message.
     */
    private final PreviewCallback previewCallback;
    /**
     * Autofocus callbacks arrive here, and are dispatched to the Handler which requested them.
     */
    private final AutoFocusCallback autoFocusCallback;

    /**
     * Initializes this static object with the Context of the calling Activity.
     *
     * @param context The Activity which wants to use the camera.
     */
    public static void init(Context context) {
        if (cameraManager == null) {
            cameraManager = new CameraManager(context);
        }
    }

    /**
     * Gets the CameraManager singleton instance.
     *
     * @return A reference to the CameraManager singleton.
     */
    public static CameraManager get() {
        return cameraManager;
    }

    private CameraManager(Context context) {

        this.context = context;
        this.configManager = new CameraConfigurationManager(context);

        // Camera.setOneShotPreviewCallback() has a race condition in Cupcake, so we use the older
        // Camera.setPreviewCallback() on 1.5 and earlier. For Donut and later, we need to use
        // the more efficient one shot callback, as the older one can swamp the system and cause it
        // to run out of memory. We can't use SDK_INT because it was introduced in the Donut SDK.
        //useOneShotPreviewCallback = Integer.parseInt(Build.VERSION.SDK) > Build.VERSION_CODES.CUPCAKE;
        useOneShotPreviewCallback = Integer.parseInt(Build.VERSION.SDK) > 3; // 3 = Cupcake
        previewCallback = new PreviewCallback(configManager, useOneShotPreviewCallback);
        autoFocusCallback = new AutoFocusCallback();
    }

    public void requestAutoFocus(Handler handler, int message) {
        if (camera != null && previewing) {
            autoFocusCallback.setHandler(handler, message);
            //Log.d(TAG, "Requesting auto-focus callback");
            camera.autoFocus(autoFocusCallback);
        }
    }

    /**
     * Opens the camera driver and initializes the hardware parameters.
     *
     * @param holder The surface object which the camera will draw preview frames into.
     * @throws IOException Indicates the camera driver failed to open.
     */
    public void openDriver(SurfaceHolder holder) throws IOException {
        if (camera == null) {
            camera = Camera.open();
            if (camera == null) {
                throw new IOException();
            }
            camera.setPreviewDisplay(holder);//在SurfaceView创建好后，通过Camera类的setPreviewDisplay()方法，将SurfaceHolder传入Camera。

            if (!initialized) {
                initialized = true;
                configManager.initFromCameraParameters(camera);//计算了屏幕分辨率和当前最适合的相机像素
            }
            configManager.setDesiredCameraParameters(camera);//读取配置并设置相机参数

        }
    }

    public void openDriver(SurfaceHolder holder, int cameraId) throws IOException {
        if (camera == null) {
            camera = Camera.open(cameraId);
            if (camera == null) {
                throw new IOException();
            }
            camera.setPreviewDisplay(holder);//在SurfaceView创建好后，通过Camera类的setPreviewDisplay()方法，将SurfaceHolder传入Camera。

            if (!initialized) {
                initialized = true;
                configManager.initFromCameraParameters(camera);//计算了屏幕分辨率和当前最适合的相机像素
            }
            configManager.setDesiredCameraParameters(camera, (Activity) context, cameraId);//读取配置并设置相机参数

        }
    }

    /**
     * Closes the camera driver if still in use.
     */
    public void closeDriver() {
        if (camera != null) {
            camera.release();
            camera = null;
        }
    }

    /**
     * Asks the camera hardware to begin drawing preview frames to the screen.
     * 相机预览
     */
    public void startPreview() {
        if (camera != null && !previewing) {
            camera.startPreview();
            previewing = true;
        }
    }

    /**
     * Tells the camera to stop drawing preview frames.
     */
    public void stopPreview() {
        if (camera != null && previewing) {
            if (!useOneShotPreviewCallback) {
                camera.setPreviewCallback(null);
            }
            camera.stopPreview();
            previewCallback.setHandler(null, 0);
            autoFocusCallback.setHandler(null, 0);
            previewing = false;
        }
    }

    /**
     * A single preview frame will be returned to the handler supplied. The data will arrive as byte[]
     * in the message.obj field, with width and height encoded as message.arg1 and message.arg2,
     * respectively.
     *
     * @param handler The handler to send the message to.
     * @param message The what field of the message to be sent.
     */
    public void requestPreviewFrame(Handler handler, int message) {
        if (camera != null && previewing) {
            previewCallback.setHandler(handler, message);
            Log.i(TAG, "requestPreviewFrame: useOneShotPreviewCallback " + useOneShotPreviewCallback);
            if (useOneShotPreviewCallback) {
                camera.setOneShotPreviewCallback(previewCallback);// 绑定相机回调函数，当预览界面准备就绪后会回调Camera.PreviewCallback.onPreviewFrame
            } else {
                camera.setPreviewCallback(previewCallback);
            }
        }
    }

    public Camera getCamera() {
        return camera;
    }

    public CameraConfigurationManager getConfigManager() {
        return configManager;
    }

    public Context getContext() {
        return context;
    }

}
