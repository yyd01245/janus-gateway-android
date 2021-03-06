package computician.janusclient;

import computician.janusclient.util.SystemUiHider;
import computician.janusclientapi.*;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.opengl.EGLContext;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;

import org.webrtc.VideoRenderer;
import org.webrtc.VideoRendererGui;

public class JanusActivity extends Activity {
    private static final boolean AUTO_HIDE = true;

    private GLSurfaceView vsv;
    private VideoRenderer.Callbacks localRender;
    private VideoRenderer.Callbacks remoteRender;
    private EchoTest echoTest;
    private VideoRoomTest videoRoomTest;
    private BroadCast broadcast;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * If set, will toggle the system UI visibility upon interaction. Otherwise,
     * will show the system UI visibility upon interaction.
     */
    private static final boolean TOGGLE_ON_CLICK = true;

    /**
     * The flags to pass to {@link SystemUiHider#getInstance}.
     */
    private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;

    /**
     * The instance of the {@link SystemUiHider} for this activity.
     */
    private SystemUiHider mSystemUiHider;

    private class MyInit implements Runnable {

        public void run() {
            init();
        }

        private void init() {
            try {
                SharedPreferences ps = getSharedPreferences(SettingsActivity.SETTING_CONFIG, MODE_PRIVATE);
                String ip = ps.getString(SettingsActivity.IPTVACCESS_IP, "");
                String port = ps.getString(SettingsActivity.IPTVACCESS_PORT, "");
                String uri  = "ws://"+ip+":"+port;
                EGLContext con = VideoRendererGui.getEGLContext();
//                echoTest = new EchoTest(localRender, remoteRender,uri);
//                echoTest.initializeMediaContext(JanusActivity.this, true, true, true, con);
//                echoTest.Start();
                broadcast = new BroadCast(localRender, remoteRender,uri);
                broadcast.initializeMediaContext(JanusActivity.this, true, true, true, con);
                broadcast.Start();

            } catch (Exception ex) {
                Log.e("computician.janusclient", ex.getMessage());
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        java.lang.System.setProperty("java.net.preferIPv6Addresses", "false");
        java.lang.System.setProperty("java.net.preferIPv4Stack", "true");
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_janus);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        vsv = (GLSurfaceView) findViewById(R.id.glview);
        vsv.setPreserveEGLContextOnPause(true);
        vsv.setKeepScreenOn(true);
        VideoRendererGui.setView(vsv, new MyInit());
//        localRender = VideoRendererGui.create(72, 72, 25, 25, VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, false);
//        remoteRender = VideoRendererGui.create(0, 0, 50, 50, VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, true);
        localRender = VideoRendererGui.create(72, 72, 25, 25, VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, false);
       remoteRender = VideoRendererGui.create(0, 0, 25, 25, VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, true);
    }
//    @Override
//    public boolean onKeyDown(int keyCode, KeyEvent event) {
//        if(keyCode == KeyEvent.KEYCODE_BACK){
//            Intent intent = new Intent();
//            intent.setClass(this, StartActivity.class);
//            startActivity(intent);
//
//            finish();
//        }
//
//        return super.onKeyDown(keyCode, event);
//    }

}
