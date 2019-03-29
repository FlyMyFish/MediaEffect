package com.zhang.shichen.mediaeffect;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * @author shichen 754314442@qq.com
 * Created by shichen on 2018/8/10.
 */
public class WaveSurfaceView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    private final String TAG="WaveSurfaceView";
    private SurfaceHolder mHolder;
    private Canvas mCanvas;//绘图的画布
    private boolean mIsDrawing;//控制绘画线程的标志位

    public WaveSurfaceView(Context context) {
        this(context, null);
    }

    Paint mForePaint;

    private void initView() {
        mForePaint = new Paint();
        mForePaint.setAntiAlias(true);
        mForePaint.setStyle(Paint.Style.STROKE);
        mForePaint.setStrokeWidth(2.0f);
        mForePaint.setColor(0xffffffff);
        mHolder = getHolder();//获取SurfaceHolder对象
        mHolder.addCallback(this);//注册SurfaceHolder的回调方法
        setFocusable(true);
        setFocusableInTouchMode(true);
        this.setKeepScreenOn(true);
    }

    public WaveSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    byte[] mBytes;

    public void updateWave(byte[] wave) {
        mBytes = wave;
        Log.e(TAG,"updateWave:"+wave.length);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        mIsDrawing = true;
        new Thread(this).start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        mIsDrawing = false;
    }

    /**
     * 每30帧刷新一次屏幕
     **/
    public static final int TIME_IN_FRAME = 30;

    @Override
    public void run() {
        long startTime = System.currentTimeMillis();
        while (mIsDrawing) {
            draw();
//             x+=1;
//             y=(int)(100*Math.sin(x*2*Math.PI/180)+400);
//             mPath.lineTo(x,y);
        }

        /**取得更新结束的时间**/
        long endTime = System.currentTimeMillis();

        /**计算出一次更新的毫秒数**/
        int diffTime = (int) (endTime - startTime);

        /**确保每次更新时间为30帧**/
        while (diffTime <= TIME_IN_FRAME) {
            diffTime = (int) (System.currentTimeMillis() - startTime);
            /**线程等待**/
            Thread.yield();
        }

    }

    float[] mPoints;
    Rect mRect = new Rect();

    //绘图操作
    private void draw() {
        try {
            mCanvas = mHolder.lockCanvas();
            if (mBytes==null){
                return;
            }
            Log.e(TAG,String.valueOf(mBytes.length));
            //mBytes就是采集来的数据 这里是个大小为1024的数组，里面的数据都是byts类型，所以大小为-127到128
            if (mBytes == null) {
                return;
            }

            if (mPoints == null || mPoints.length < mBytes.length * 4) {
                //mPoints主要用来存储要画直线的4个坐标（每个点两个坐标，所以一条直线需要两个点，也就是4个坐标）
                mPoints = new float[mBytes.length * 4];
            }

            mRect.set(0, 0, getWidth(), getHeight());
            //xOrdinate是x轴的总刻度，因为一次会传输过来1024个数据，每两个数据要画成一条直线，
            // 所以x轴我们分成1023段。你要是觉的太多了，也可以像我一样除以2，看自己需求了。
            int xOrdinate = (mBytes.length - 1) / 2;
            //以下的for循环将利用mBytes[i] mBytes[i+1] 这两个数据去生成4个坐标值，从而在刻画成两个坐标，来画线条
            for (int i = 0; i < xOrdinate; i++) {
                //第i个点在总横轴上的坐标，
                mPoints[i * 4] = mRect.width() * i / xOrdinate;
                //第i个点的在总纵轴上的坐标。他在画线上以总纵轴的1/2为基准线（mRect.height() / 2），所有的点或正或负以此线为基础标记。
                //((byte) (mBytes[i] + 128))这个一直没有理解，如果+128是为了将数据全部换算为正整数，那么强转为byte后不又变回-127到128了么？？
                // 要是谁知道原因可以留言告诉我.....
                //(mRect.height() / 2) / 128就是将二分之一的总长度换算成128个刻度，因为我们的数据是byte类型，所以刻画成128个刻度正好
                Log.e(TAG,"mBytes["+i+"]="+mBytes[i]);
                Log.e(TAG,"mBytes["+i+"]+128="+(mBytes[i] + 128));
                Log.e(TAG,"(byte)(mBytes["+i+"]+128)="+(byte) (mBytes[i] + 128));
                Log.e(TAG,"mBytes["+i+"]Y="+((byte) (mBytes[i] + 128)) * (mRect.height() / 2) / 128);
                //因为java的byte类型只占一个字节，即2的8次方，表示的范围是0~127，128溢出了当做0，129为1，以此类推。
                // 所以 257转换为byte后的值为：257-128-128=1
                // 323转换为byte后：323-128-128=67
                mPoints[i * 4 + 1] = mRect.height() / 2 + ((byte) (mBytes[i] + 128)) * (mRect.height() / 2) / 128;
                //以下就是刻画第i+1个数据了，原理和刻画第i个一样
                mPoints[i * 4 + 2] = mRect.width() * (i + 1) / xOrdinate;
                mPoints[i * 4 + 3] = mRect.height() / 2 + ((byte) (mBytes[i + 1] + 128)) * (mRect.height() / 2) / 128;
            }

            //循环结束后，就得到了这一次波形的所有刻画坐标，直接画在画布上就好了
            mCanvas.drawColor(0xff000000);
            mCanvas.drawLines(mPoints, mForePaint);
            // draw sth绘制过程
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (mCanvas != null)
                mHolder.unlockCanvasAndPost(mCanvas);//保证每次都将绘图的内容提交
        }
    }

}
