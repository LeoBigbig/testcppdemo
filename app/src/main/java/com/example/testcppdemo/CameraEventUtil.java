package com.example.testcppdemo;

import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * 处理屏幕点击 对相机预览的影响
 */

public class CameraEventUtil {
    private String TAG = "CameraEventUtil";
    private float oldDist = 1f;
    private DrawFocusRect drawFocusRect;//聚焦框

    /**
     * 点击屏幕显示聚焦框
     * @param event 触摸事件
     * @param mCamera Camera对象
     * @param surfaceViewHeight surfaceview高
     * @param surfaceViewWidth surfaceview宽
     * @param frameLayout surfaceview的父布局
     */
    public boolean touchFocusAndZoom(MotionEvent event, Camera mCamera, int surfaceViewWidth, int surfaceViewHeight, final FrameLayout frameLayout){
        if (event.getPointerCount() == 1) {
            handleFocusMetering(event, mCamera,surfaceViewWidth,surfaceViewHeight);

            if (drawFocusRect!=null) frameLayout.removeView(drawFocusRect);//界面上只显示一个聚焦框，移除历史框
            //父布局添加聚焦框
            drawFocusRect = new DrawFocusRect(frameLayout.getContext(), Color.GREEN);
            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(200,200);
            layoutParams.leftMargin = (int) (event.getX()-100);
            layoutParams.topMargin = (int) (event.getY() - 100);
            frameLayout.addView(drawFocusRect,layoutParams);
            //显示2秒后，消失
            new android.os.Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (drawFocusRect!=null)frameLayout.removeView(drawFocusRect);
                }
            },2000);
        } else {
            //多指触摸情况下为缩放相机
            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_POINTER_DOWN:
                    oldDist = getFingerSpacing(event);
                    break;
                case MotionEvent.ACTION_MOVE:
                    float newDist = getFingerSpacing(event);//计算两指间距
                    if (newDist > oldDist) {
                        handleZoom(true, mCamera);//间距增大，则放大相机
                    } else if (newDist < oldDist) {
                        handleZoom(false, mCamera);//反之
                    }
                    oldDist = newDist;
                    break;
            }
        }
        return true;
    }
    /**
     * 计算两指间距
     * @param event 触摸事件
     * @param camera Camera对象
     * @param surfaceViewWidth surfaceview宽
     * @param surfaceViewHeight surfaceview高
     */
    private void handleFocusMetering(MotionEvent event, Camera camera, int surfaceViewWidth, int surfaceViewHeight) {
        int viewWidth = surfaceViewWidth;
        int viewHeight = surfaceViewHeight;
        //计算触摸区域
        Rect focusRect = calculateTapArea(event.getX(), event.getY(), 1f, viewWidth, viewHeight);
        Rect meteringRect = calculateTapArea(event.getX(), event.getY(), 1.5f, viewWidth, viewHeight);

        camera.cancelAutoFocus();//关闭自动聚焦
        //判断相机是否支持focus area
        Camera.Parameters params = camera.getParameters();
        if (params.getMaxNumFocusAreas() > 0) {
            List<Camera.Area> focusAreas = new ArrayList<>();
            focusAreas.add(new Camera.Area(focusRect, 800));
            params.setFocusAreas(focusAreas);
        } else {
            Log.i(TAG, "focus areas not supported");
        }
        //判断相机是否支持metering area
        if (params.getMaxNumMeteringAreas() > 0) {
            List<Camera.Area> meteringAreas = new ArrayList<>();
            meteringAreas.add(new Camera.Area(meteringRect, 800));
            params.setMeteringAreas(meteringAreas);
        } else {
            Log.i(TAG, "metering areas not supported");
        }
        /** focus area和 metering area不支持的话，无法手动局部聚焦 */
        final String currentFocusMode = params.getFocusMode();
        params.setFocusMode(Camera.Parameters.FOCUS_MODE_MACRO);//设置近场聚焦模式，局部聚焦
        camera.setParameters(params);
        //开启相机聚焦
        camera.autoFocus(new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean success, Camera camera) {
                Camera.Parameters params = camera.getParameters();
                params.setFocusMode(currentFocusMode);
                camera.setParameters(params);
            }
        });
    }
    /**
     * 获取两指间距
     */
    private static float getFingerSpacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }
    /**
     * 计算触摸有效区域
     */
    private static Rect calculateTapArea(float x, float y, float coefficient, int width, int height) {
        float focusAreaSize = 300;
        int areaSize = Float.valueOf(focusAreaSize * coefficient).intValue();
        int centerX = (int) (x / width * 2000 - 1000);
        int centerY = (int) (y / height * 2000 - 1000);

        int halfAreaSize = areaSize / 2;
        RectF rectF = new RectF(clamp(centerX - halfAreaSize, -1000, 1000)
                , clamp(centerY - halfAreaSize, -1000, 1000)
                , clamp(centerX + halfAreaSize, -1000, 1000)
                , clamp(centerY + halfAreaSize, -1000, 1000));
        return new Rect(Math.round(rectF.left), Math.round(rectF.top), Math.round(rectF.right), Math.round(rectF.bottom));
    }
    /**
     * 防止坐标越界
     */
    private static int clamp(int x, int min, int max) {
        if (x > max) {
            return max;
        }
        if (x < min) {
            return min;
        }
        return x;
    }
    /**
     * 相机缩放
     */
    private void handleZoom(boolean isZoomIn, Camera camera) {
        Camera.Parameters params = camera.getParameters();
        if (params.isZoomSupported()) {
            int maxZoom = params.getMaxZoom();
            int zoom = params.getZoom();
            if (isZoomIn && zoom < maxZoom) {
                zoom+=2;
            } else if (zoom > 0) {
                zoom-=2;
            }
            params.setZoom(zoom);
            camera.setParameters(params);
        } else {
            Log.i(TAG, "zoom not supported");
        }
    }
}
