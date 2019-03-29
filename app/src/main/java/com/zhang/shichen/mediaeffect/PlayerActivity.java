package com.zhang.shichen.mediaeffect;

import android.media.MediaPlayer;
import android.media.audiofx.Visualizer;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import java.io.IOException;
import java.security.Permission;
import java.util.Timer;
import java.util.TimerTask;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.support.v4.content.PermissionChecker.PERMISSION_DENIED;

/**
 * @author shichen 754314442@qq.com
 * Created by shichen on 2018/8/10.
 */
public class PlayerActivity extends AppCompatActivity {
    private final String TAG = "PlayerActivity";
    MediaPlayer mediaPlayer;
    Visualizer visualizer;
    WaveSurfaceView waveSurfaceView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        setupToolBar();
        waveSurfaceView = findViewById(R.id.wave_surface);
        tvCurrentProgress = findViewById(R.id.tv_current_progress);
        Uri musicUri=Uri.parse("http://ws.stream.qqmusic.qq.com/C400003lghpv0jfFXG.m4a?fromtag=0&guid=126548448&vkey=472787275A9A37B34CF63C656B46A5A163E891A62244EB8C7A9DE4D7801F4E9A0B71FDDADF36F92591C3728E3ACFF6305ACF500A9349EBF2");
        mediaPlayer = MediaPlayer.create(this, musicUri);
        // 顺带再说一个bug 如果你得到的错误代码是 -1 那么基本上的原因是你忘记了声明权限
        // <uses-permission android:name="android.permission.RECORD_AUDIO"/>
        /*if (ContextCompat.checkSelfPermission(this, RECORD_AUDIO) != PERMISSION_DENIED) {
            initVisualizer();
        } else {
            ActivityCompat.requestPermissions(this, permission, 0x01);
        }*/
        /*
         在使用start()播放流媒体之前，需要装载流媒体资源。这里最好使用
         prepareAsync()用异步的方式装载流媒体资源。因为流媒体资源的装载
         是会消耗系统资源的，在一些硬件不理想的设备上，如果使用prepare()
         同步的方式装载资源，可能会造成UI界面的卡顿，这是非常影响用于体验
         的。因为推荐使用异步装载的方式，为了避免还没有装载完成就调用
         start()而报错的问题，需要绑定MediaPlayer.setOnPreparedListener()事件，
         它将在异步装载完成之后回调。
         mediaPlayer.prepareAsync();
         mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
        @Override public void onPrepared(MediaPlayer mediaPlayer) {
        // 装载完毕回调
        mediaPlayer.start();
        }
        });
         */
        //mediaPlayer.start();
        mTimer = new Timer();
        mTimerTask = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvCurrentProgress.setText(String.valueOf(mediaPlayer.getCurrentPosition()));
                    }
                });
            }
        };
    }

    String[] permission = {RECORD_AUDIO};
    private Timer mTimer;
    private TimerTask mTimerTask;

    private void initVisualizer() {
        //使用音乐的sessionId来实例化这个类
        visualizer = new Visualizer(mediaPlayer.getAudioSessionId());
        //设置每次捕获频谱的大小，音乐在播放中的时候采集的数据的大小或者说是采集的精度吧，
        // 我的理解，而且getCaptureSizeRange()所返回的数组里面就两个值 .文档里说
        // 数组[0]是最小值（128），数组[1]是最大值（1024）。
        visualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);
        //接下来就好理解了设置一个监听器来监听不断而来的所采集的数据。一共有4个参数，
        // 第一个是监听者，第二个单位是毫赫兹，表示的是采集的频率，第三个是是否采集波形，第四个是是否采集频率
        visualizer.setDataCaptureListener(new Visualizer.OnDataCaptureListener() {
            @Override
            public void onWaveFormDataCapture(Visualizer visualizer, byte[] waveform, int i) {
                //这个回调应该采集的是波形数据
                waveSurfaceView.updateWave(waveform);
            }

            @Override
            public void onFftDataCapture(Visualizer visualizer, byte[] fft, int samplingRate) {
                //这个回调应该采集的是快速傅里叶变换有关的数据，没试过，回头有空了再试试
                Log.e(TAG, "fft.length()=" + fft.length);
               /* byte[] model = new byte[fft.length / 2 + 1];
                model[0] = (byte) Math.abs(fft[1]);
                int j = 1;

                for (int i = 2; i < 18; ) {
                    model[j] = (byte) Math.hypot(fft[i], fft[i + 1]);
                    i += 2;
                    j++;
                }*/

            }
        }, Visualizer.getMaxCaptureRate() / 2, true, false);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 0x01) {
            if (grantResults[0] == PERMISSION_DENIED) {
                finish();
            } else {
                initVisualizer();
            }
        }
    }

    private void setupToolBar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_player, menu);
        return true;
    }

    TextView tvCurrentProgress;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.previous:
                break;
            case R.id.play:
                if (mediaPlayer != null) {
                    mTimer.schedule(mTimerTask, 0, 10);
                    if (ContextCompat.checkSelfPermission(PlayerActivity.this, RECORD_AUDIO) != PERMISSION_DENIED) {
                        initVisualizer();
                    } else {
                        ActivityCompat.requestPermissions(PlayerActivity.this, permission, 0x01);
                    }
                    mediaPlayer.start();
                    //这个方法的主要作用是为了控制何时去采集频谱数据，
                    // 你应该只是愿意采集你所关心的音乐数据，而不关心声音输出器中任何的声音。
                    // 而且对mVisualizer的许多设置必须在setEnable之前完成。并且结束功能后，
                    // 要记得setEnable(false)
                    // 如果你见到了以下这个错误,那基本上就是因为没有及时setEnable（false）,导致setCaptureSize()这个方法出错。
                    // E/AndroidRuntime(22259): Caused by: java.lang.IllegalStateException: setCaptureSize() called in wrong state: 2
                    visualizer.setEnabled(true);
                }
                break;
            case R.id.pause:
                if (mediaPlayer != null) {
                    mediaPlayer.pause();
                }
                break;
            case R.id.next:
                break;
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        /*使用完MediaPlayer需要回收资源。MediaPlayer是很消耗系统资源的，
        所以在使用完MediaPlayer，不要等待系统自动回收，最好是主动回收资源。*/
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (visualizer != null) {
            visualizer.setEnabled(false);
        }
        mTimerTask.cancel();
        mTimer.cancel();
    }
}
