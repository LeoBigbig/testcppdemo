package com.example.testcppdemo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import org.opencv.core.Mat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends Activity implements SurfaceHolder.Callback, View.OnClickListener {
    private SurfaceView surfaceView;
    private SurfaceHolder mHolder;
    private int mCameraId = 0; // 1前置摄像头自拍，0后置
    private Context mContext;

    //屏幕宽高
    private int screenWidth;
    private int screenHeight;
    private LinearLayout home_custom_top_relative;
    private RelativeLayout homecamera_bottom_relative;
    private ImageView flash_light;

    //闪光灯模式 0:关闭 1: 开启 2: 自动
    private int light_num = 0;
    //延迟时间
    private int delay_time;
    private int delay_time_temp;
    private boolean isview = false;
    private boolean is_camera_delay;
    private ImageView camera_frontback;
    private ImageView camera_close;
    private ImageView img_camera;
    private int picHeight;
    private int captureMills = 0;//延时拍照，默认不延时
    private final String tag = "CameraActivity";
    private boolean canFinish = true;
    private boolean hasSurface;//是否显示扫描画面
    private CameraEventUtil cameraEventUtil;

    protected static final String FINISH_INTENT = "finish";
    private Receiver receiver;
    private static String setPath;//接收传递过来的图片存储路径



    private String img_path;


    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    public static void turnToCameraActivity(Activity context,String path) {
        setPath = path;
        Intent intent = new Intent(context, MainActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);





        mContext = this;
        Intent intent = getIntent();
        captureMills = intent.getIntExtra("time", 0);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);//活动界面为全屏

        initView();//控件初始化
        initData();//获取屏幕尺寸


        CameraManager.init(this);//相机管理者初始化
        cameraEventUtil = new CameraEventUtil();



        mHolder = surfaceView.getHolder();
        if (hasSurface) {
            //surfaceHolder配置完之后，只需开启CameraManager的驱动即可
            initCamera(mHolder, mCameraId);
        } else {
            //最初surfaceHolder配置并监听CallBack接口
            mHolder.addCallback(this);
            mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }


        //
        Mat mImg = new Mat();
        mImg.release();
//




        //设置广播监听，拍照显示活动可退回当前活动
        IntentFilter intentFilter = new IntentFilter(FINISH_INTENT);
        receiver = new Receiver();
        registerReceiver(receiver,intentFilter);



    }

    private void startAutoCapture() {
        //自动拍照，留给有需要的人添加
    }

    /**
     * 控件初始化
     */
    private void initView() {
        final FrameLayout frameLayout = (FrameLayout) findViewById(R.id.framelayout);

        surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        /*surfaceView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                autoFocusSeconds = false;
                mHandler.removeMessages(11);//防止过度点击，移除点击触发的历史信息
                mHandler.sendEmptyMessageDelayed(11, 5000);
                return cameraEventUtil.touchFocusAndZoom(event, CameraManager.get().getCamera(), surfaceView.getWidth(), surfaceView.getHeight(), frameLayout);
            }
        });*/
/*
        img_camera = (ImageView) findViewById(R.id.img_camera);
        img_camera.setOnClickListener(this);

        //关闭相机界面按钮
        camera_close = (ImageView) findViewById(R.id.camera_close);
        camera_close.setOnClickListener(this);

        //top 的view
        home_custom_top_relative = (LinearLayout) findViewById(R.id.home_custom_top_relative);
        home_custom_top_relative.setAlpha(0.5f);

        //前后摄像头切换
        camera_frontback = (ImageView) findViewById(R.id.camera_frontback);
        camera_frontback.setOnClickListener(this);


        //闪光灯
        flash_light = (ImageView) findViewById(R.id.flash_light);
        flash_light.setOnClickListener(this);

        homecamera_bottom_relative = (RelativeLayout) findViewById(R.id.homecamera_bottom_relative);

        */
    }
    /**
     * 屏幕尺寸
     */
    private void initData() {
        DisplayMetrics dm = mContext.getResources().getDisplayMetrics();
        screenWidth = dm.widthPixels;
        screenHeight = dm.heightPixels;

    }

    public boolean autoFocusSeconds = true;//相机自动聚焦标志位。true可自动聚焦，false 关闭自动聚焦

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            int what = msg.what;
            switch (what) {
                case 9:
                    //可发送消息，不通过点击拍照，执行拍照动作
                    try {
                        if (delay_time == 0) {
                            captrue();
                            is_camera_delay = false;
                        }
                    } catch (Exception e) {
                        return;
                    }

                    break;

                case -9:
                    //留有延时自动拍照，当前未使用
                    is_camera_delay = false;
                    break;
//                case UIConstants.CAPTURE_NOW:
//                    img_camera.performClick();
//                    img_camera.setVisibility(View.INVISIBLE);
//                    break;
                case 13:
                    decode((byte[]) msg.obj, msg.arg1, msg.arg2);//拍照后解析图像数据
                    break;

                case 11:
                    //当关闭自动聚焦后，可发送消息重新开启自动聚焦
                    autoFocusSeconds = true;
                    what = 15;//执行下面持续对焦
                    Log.i(tag, "autoFocusSeconds: " + autoFocusSeconds);

                case 15:
                    if (autoFocusSeconds) {
                        CameraManager.get().requestAutoFocus(this, 15);//自动对焦
                    }
                    break;
            }


        }
    };

    /**
     * 控件点击事件处理
     */
    @Override
    public void onClick(View v) {

        /*

        if (v.getId() == R.id.img_camera) {//点击拍照button，进行拍照
            if (isview) {
                if (delay_time == 0) {
                    captrue();
                }
                isview = false;
            }
        } else if (v.getId() == R.id.camera_frontback) {
            switchCamera();//点击 切换摄像头 button， 切换摄像头
        } else if (v.getId() == R.id.camera_close) {
            if (is_camera_delay) {//延时情况下处理，当前未延时
                Toast.makeText(CameraActivity.this, "正在拍照请稍后...", Toast.LENGTH_SHORT).show();
                return;
            }
            finish();
        } else if (v.getId() == R.id.flash_light) {//闪光灯 开启 关闭 自动 等情况
            if (mCameraId == 1) {
                //前置
                Toast.makeText(mContext, "请切换为后置摄像头开启闪光灯", Toast.LENGTH_LONG).show();
                return;
            }
            Camera.Parameters parameters = CameraManager.get().getCamera().getParameters();
            if (light_num == 0) {
                //打开
                light_num = 1;
                flash_light.setImageResource(R.drawable.btn_camera_flash_on);
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);//开启

                CameraManager.get().getCamera().setParameters(parameters);
            } else if (light_num == 1) {
                //自动
                light_num = 2;
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
                CameraManager.get().getCamera().setParameters(parameters);
                flash_light.setImageResource(R.drawable.btn_camera_flash_auto);
            } else if (light_num == 2) {
                //关闭
                light_num = 0;
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                CameraManager.get().getCamera().setParameters(parameters);
                flash_light.setImageResource(R.drawable.btn_camera_flash_off);
            }
        }


*/
    }
    /**
     * 切换摄像头
     */
    public void switchCamera() {
        CameraManager.get().stopPreview();
        CameraManager.get().closeDriver();
        mCameraId = (mCameraId + 1) % CameraManager.get().getCamera().getNumberOfCameras();
        Log.i("CameraId", String.valueOf(mCameraId));
        if (mHolder != null) {
            initCamera(mHolder, mCameraId);
        }
    }

    /**
     * 初始化相机及预览
     */
    @Override
    protected void onResume() {
        super.onResume();

        mHolder = surfaceView.getHolder();
        if (hasSurface) {
            //surfaceHolder配置完之后，只需开启CameraManager的驱动即可
            initCamera(mHolder, mCameraId);
        } else {
            //最初surfaceHolder配置并监听CallBack接口
            mHolder.addCallback(this);
            mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }

        if (captureMills != 0) {
            startAutoCapture();
        }
    }

    /**
     * 停止预览，回收相机资源
     */
    @Override
    protected void onPause() {
        super.onPause();
        CameraManager.get().stopPreview();
        CameraManager.get().closeDriver();
        if (!hasSurface) {

            SurfaceHolder surfaceHolder = surfaceView.getHolder();
            surfaceHolder.removeCallback(this);
        }
    }
    /**
     * 初始化相机和相关操作
     */
    private void initCamera(SurfaceHolder surfaceHolder, int cameraId) {
        try {
            CameraManager.get().openDriver(surfaceHolder, cameraId);
            CameraManager.get().startPreview();//摄像头开启预览
            CameraManager.get().requestAutoFocus(mHandler, 15);

            isview = true;
        } catch (IOException ioe) {
            return;
        } catch (RuntimeException e) {
            return;
        }
    }

//    private void captrue() {
//        Log.i(tag, "captrue: ");
//
//        CameraManager.get().getCamera().takePicture(null, null, new Camera.PictureCallback() {
//            @Override
//            public void onPictureTaken(byte[] data, Camera camera) {
//                isview = false;
//                canFinish = false;
//                home_custom_top_relative.setVisibility(View.INVISIBLE);
//                //将data 转换为位图 或者你也可以直接保存为文件使用 FileOutputStream
//                //这里我相信大部分都有其他用处把 比如加个水印 后续再讲解
//                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
//                Bitmap saveBitmap = CameraUtil.getInstance().setTakePicktrueOrientation(mCameraId, bitmap);
//
//                screenWidth = CameraManager.get().getConfigManager().getCameraPictureSize().y;
//                picHeight = CameraManager.get().getConfigManager().getCameraPictureSize().x;
//                saveBitmap = Bitmap.createScaledBitmap(saveBitmap, screenWidth, picHeight, true);
//
//                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//19个字符串  index : 0-18
//                Date date = new Date();
//                final String time = sdf.format(date);
//                String img_path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath() +
//                        File.separator + SystemClock.elapsedRealtime() + ".jpeg";
//
//                Log.i(tag,"img_path"+"-"+img_path);
//
//                BitmapUtils.saveJPGE_After(mContext, saveBitmap, img_path, 100);
//
//                setResult(time, img_path);
//            }
//        });
//    }
    /**
     * 执行拍照，实际上是取预览图像帧
     */
    private void captrue() {
        CameraManager.get().requestPreviewFrame(mHandler, 13);
    }
    /**
     * 解析图像帧源数据
     * @param data 摄像头拿到的YUV源图像
     * @param width 摄像头预览尺寸 宽度
     * @param height 摄像头预览尺寸 高度
     */
    private void decode(byte[] data, int width, int height) {
        Log.i(tag, "decode: data " + data.length);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//19个字符串  index : 0-18
        Date date = new Date();
        final String time = sdf.format(date);

        try {
            String img_path = setPath;

            Log.i(tag, "img_path" + "-" + img_path);
            //YUV压缩成JPEG
            YuvImage yuvImage = new YuvImage(data, ImageFormat.NV21, width, height, null);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            yuvImage.compressToJpeg(new Rect(0, 0, width, height), 100, baos);
            //JPEG转成rgb
            BitmapFactory.Options localOptions = new BitmapFactory.Options();
            localOptions.inPreferredConfig = Bitmap.Config.RGB_565;  //构造位图生成的参数，必须为565。类名+enum
            Bitmap bitmap = BitmapFactory.decodeByteArray(baos.toByteArray(), 0, baos.toByteArray().length, localOptions);
            baos.close();
            bitmap = CameraUtil.getInstance().setTakePicktrueOrientation(mCameraId,bitmap);//调整位图转向

            BitmapUtils.saveJPGE_After(mContext, bitmap, img_path, 100);//位图保存成文件

            setResult(time, img_path);//跳转到显示界面
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * 拍完照后显示界面
     */
    private void setResult(String time, String img_path) {

        //ShowPicActivity.turnToShowPicActivity((Activity) mContext,0,0,img_path,time);
//        Intent intent = new Intent();
//        intent.putExtra("IMG_PATH", img_path);
////        intent.putExtra("PIC_WIDTH", screenWidth);
////        intent.putExtra("PIC_HEIGHT", picHeight);
//        intent.putExtra("PIC_TIME", time);

//        setResult(-1, intent);
//        finish();

    }

    /** --------------------------surfaceview 的接口----------------------------------------------- */
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (!hasSurface) {
            hasSurface = true;
            initCamera(holder, mCameraId);//启动摄像头驱动
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

        hasSurface = false;
    }
    /** ------------------------------------------------------------------------- */
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (canFinish) {
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (receiver!=null) unregisterReceiver(receiver);
        mContext = null;

    }
    /**
     * 监听到finish的广播，回收该活动
     */
    private class Receiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            finish();
        }
    }

}
