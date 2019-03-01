package georgesbriand.artpub;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class MainActivity extends AppCompatActivity implements View.OnTouchListener, CameraBridgeViewBase.CvCameraViewListener2 {
    private static final String  TAG              = "OCVSample::Activity";


    private Mat                  mRgba;
    private CameraBridgeViewBase mOpenCvCameraView;
    private Scalar scalarRed = new Scalar(255, 0, 0,.8);
       private Mat bw;
    private Mat downscaled;
    private Mat upscaled;
    private Mat contourImage;
    private Mat hierarchyOutputVector;
    private MatOfPoint2f approxCurve;

    public int resoAndroidWidth ;
    public int resoAndoidLength;
    private static final int FRAME_SIZE_WIDTH = 640;
    /**
     * frame size height
     */
    private static final int FRAME_SIZE_HEIGHT = 480;
    /**
     * whether or not to use a fixed frame size -> results usually in higher FPS
     * 640 x 480
     */
    private static final boolean FIXED_FRAME_SIZE = true;

    ImageView imageview;
    Size text;
    Rect r;
    Point pt;
    float ratioHeight;
    float ratioWidth;
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    downscaled = new Mat();
                    upscaled = new Mat();
                    bw = new Mat();
                    contourImage = new Mat();
                    hierarchyOutputVector = new Mat();
                    approxCurve = new MatOfPoint2f();
                    mOpenCvCameraView.enableView();
                    mOpenCvCameraView.setOnTouchListener(MainActivity.this);


                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };



    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        if (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA}, 50);
        }
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);
        imageview = new ImageView(MainActivity.this);
        imageview.setImageResource(R.drawable.index);
        LayoutParams params= new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.BELOW, R.id.opencv_screen);

        DisplayMetrics displaymetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        resoAndoidLength = displaymetrics.heightPixels;
        resoAndroidWidth = displaymetrics.widthPixels;

        imageview.setLayoutParams(params);

        imageview.setVisibility(View.VISIBLE);

        imageview.setEnabled(false);
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.opencv_screen);
        mOpenCvCameraView.setMaxFrameSize(1280, 720);

        //if (FIXED_FRAME_SIZE) {
        //    mOpenCvCameraView.setMaxFrameSize(FRAME_SIZE_WIDTH, FRAME_SIZE_HEIGHT);
        //}
        //mOpenCvCameraView.
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        mOpenCvCameraView.getMeasuredHeight();



    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
    }

    public void onCameraViewStopped() {
        mRgba.release();
    }

    public boolean onTouch(View v, MotionEvent event) {
        int y = (int) event.getY();
        int x = (int)event.getX();
        Toast toast = (Toast) Toast.makeText(MainActivity.this, "x = " +x + " y = " + y, Toast.LENGTH_SHORT);
        toast.show();
        return false; // don't need subsequent touch events
    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat src =inputFrame.rgba();
        int height = src.height();
        int width = src.width();
        ratioHeight = resoAndoidLength / height;
        ratioWidth = resoAndroidWidth / width;
        //new code
        Mat gray = src;
        Mat dst = src;
        Imgproc.pyrDown(gray, downscaled, new Size(gray.cols()/2, gray.rows()/2));
        Imgproc.pyrUp(downscaled, upscaled, gray.size());
        Imgproc.Canny(upscaled, bw, 0, 255);
        Imgproc.dilate(bw, bw, new Mat(), new Point(-1, 1), 1);

        // find contours and store them all as a list
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        contourImage = bw.clone();
        Imgproc.findContours(
                contourImage,
                contours,
                hierarchyOutputVector,
                Imgproc.RETR_EXTERNAL,
                Imgproc.CHAIN_APPROX_SIMPLE
        );

        // loop over all found contours
        for (MatOfPoint cnt : contours) {
            MatOfPoint2f curve = new MatOfPoint2f(cnt.toArray());

            // approximates a polygonal curve with the specified precision
            Imgproc.approxPolyDP(
                    curve,
                    approxCurve,
                    0.02 * Imgproc.arcLength(curve, true),
                    true
            );

            int numberVertices = (int) approxCurve.total();
            double contourArea = Imgproc.contourArea(cnt);

            Log.d(TAG, "vertices:" + numberVertices);

            // ignore to small areas
            if (Math.abs(contourArea) < 100
                // || !Imgproc.isContourConvex(
                    ) {
                continue;
            }
            if (numberVertices >= 4 && numberVertices <= 6) {

                List<Double> cos = new ArrayList<>();
                for (int j = 2; j < numberVertices + 1; j++) {
                    cos.add(
                            angle(
                                    approxCurve.toArray()[j % numberVertices],
                                    approxCurve.toArray()[j - 2],
                                    approxCurve.toArray()[j - 1]
                            )
                    );
                }
                Collections.sort(cos);

                double mincos = cos.get(0);
                double maxcos = cos.get(cos.size() - 1);

                // rectangle detection
                if (numberVertices == 4
                        && mincos >= -0.1 && maxcos <= 0.3
                        ) {
                    setLabel(dst, "RECT", cnt);

                }

            }
        }










        return  src;

    }


    private double angle(Point p1, Point p2, Point p0) {
        double dx1 = p1.x - p0.x;
        double dy1 = p1.y - p0.y;
        double dx2 = p2.x - p0.x;
        double dy2 = p2.y - p0.y;
        return (dx1 * dx2 + dy1 * dy2)
                / Math.sqrt((dx1 * dx1 + dy1 * dy1) * (dx2 * dx2 + dy2 * dy2)
                + 1e-10);
    }

        @SuppressLint("ResourceType")
        private void setLabel(Mat im, String label, MatOfPoint contour) {

            r = Imgproc.boundingRect(contour);

            Log.d(TAG,"r x = " + r.x);
            Log.d(TAG,"r y = " + r.y);
            int xtl = (int) r.tl().x;
            int ytl = (int)r.tl().y;
            int xbrtl = (int)r.br().x;
            int ybrtl = (int)r.br().y;


        /*
        Imgproc.rectangle(
                im,
                new Point(r.x + 0, r.y + baseline[0]),
                new Point(r.x + text.width, r.y -text.height),
                new Scalar(255,255,255),
                -1
                );*/


            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    View v = getWindow().findViewById(Window.ID_ANDROID_CONTENT);
                    int viewHeight = v.getHeight();
                    int viewWidth = v.getWidth();
                    double scaleH = viewHeight * 1.0 / mOpenCvCameraView.getWidth();  // size is the camera preview size (or the cv::Mat size)
                    double scaleW = viewWidth * 1.0 / mOpenCvCameraView.getHeight();
                    ConstraintLayout relativelayout = (ConstraintLayout)findViewById(R.id.main);
                    relativelayout.removeView(imageview);
                    float xLayout = findViewById(R.id.opencv_screen).getScaleX();
                    float yLayout = findViewById(R.id.opencv_screen).getScaleY();

                    Toast toast = (Toast) Toast.makeText(MainActivity.this, "r.x = " +r.x + " r.y = " + r.y,  Toast.LENGTH_SHORT);

                    toast.show();

                    LayoutParams layoutParams;
                    layoutParams = new LayoutParams((int)(r.width), (int) ((r.height)));
                    //layoutParams.leftMargin = findViewById(R.id.opencv_screen).getLeft();
                    //layoutParams.topMargin = findViewById(R.id.opencv_screen).getTop();
                    layoutParams.addRule(RelativeLayout.BELOW, R.id.opencv_screen);
                    imageview.setLayoutParams(layoutParams);

                    imageview.setX ((float) (r.x * 1.43));
                    imageview.setY((int)(r.y *1.23));
                    Log.d(TAG, "w" + imageview.getWidth());
                    Log.d(TAG, "h" + imageview.getHeight());
                    imageview.setEnabled(true);
                    relativelayout.addView(imageview);
                    //LinearLayout.LayoutParams params =  new LinearLayout
                    //        .LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    //imageview.setLayoutParams(params);




                }
            });

            Imgproc.rectangle(im, r.tl(), r.br(), scalarRed, 8);
            //Log.d(TAG, "w" + pt.x);
            //Log.d(TAG, "h" + pt.y);
            //Imgproc.putText(im, label, pt, fontface, scale, scalarRed, thickness);

        }



}

