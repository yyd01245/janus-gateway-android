package computician.janusclient;

import android.content.Context;
import android.opengl.EGLContext;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoRendererGui;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import computician.janusclientapi.IJanusGatewayCallbacks;
import computician.janusclientapi.IJanusPluginCallbacks;
import computician.janusclientapi.IPluginHandleWebRTCCallbacks;
import computician.janusclientapi.JanusMediaConstraints;
import computician.janusclientapi.JanusPluginHandle;
import computician.janusclientapi.JanusServer;
import computician.janusclientapi.JanusSupportedPluginPackages;
import computician.janusclientapi.PluginHandleSendMessageCallbacks;
import computician.janusclientapi.PluginHandleWebRTCCallbacks;

/**
 * Created by yandong.yan on 12/29/2016.
 */

//TODO create message classes unique to this plugin

public class BroadCast {
    public static final String REQUEST = "request";
    public static final String MESSAGE = "message";
    private  String JANUS_URI = "ws://10.0.3.115:8188";
    private JanusPluginHandle handle = null;
    private final VideoRenderer.Callbacks localRender, remoteRender;
    private final JanusServer janusServer;
    private BigInteger myid;
    final private String user_name = "android";
//    public void SetURI(String ip,String port){
//        JANUS_URI = "ws://"+ip+":"+port;
//    }

    public class JanusGlobalCallbacks implements IJanusGatewayCallbacks {

        @Override
        public void onSuccess() {
            janusServer.Attach(new JanusPluginCallbacks());
        }

        @Override
        public void onDestroy() {
        }

        @Override
        public String getServerUri() {
            return JANUS_URI;
        }

        @Override
        public List<PeerConnection.IceServer> getIceServers() {
            return new ArrayList<PeerConnection.IceServer>();
        }

        @Override
        public Boolean getIpv6Support() {
            return Boolean.FALSE;
        }

        @Override
        public Integer getMaxPollEvents() {
            return 0;
        }

        @Override
        public void onCallbackError(String error) {

        }
    }

    public class JanusPluginCallbacks implements IJanusPluginCallbacks {

        private void registerToJanus() {
            if(handle != null) {
                JSONObject obj = new JSONObject();
                JSONObject msg = new JSONObject();
                try {
                    obj.put(REQUEST, "register");
                    obj.put("client_id", 9999);
                    msg.put(MESSAGE, obj);
                } catch (Exception ex) {

                }
                handle.sendMessage(new PluginHandleSendMessageCallbacks(msg));
            }
        }
        private void joinToJanus() {
            if(handle != null) {
                JSONObject obj = new JSONObject();
                JSONObject msg = new JSONObject();
                try {
                    obj.put(REQUEST, "join");
                    obj.put("ptype","publisher");
                    obj.put("id", myid);
                    obj.put("key","");
                    msg.put(MESSAGE, obj);
                } catch (Exception ex) {

                }
                handle.sendMessage(new PluginHandleSendMessageCallbacks(msg));
            }
        }
        private void publishOwnFeed() {
            if(handle != null) {
                handle.createOffer(new IPluginHandleWebRTCCallbacks() {
                    @Override
                    public void onSuccess(JSONObject obj) {
                        try
                        {
                            JSONObject msg = new JSONObject();
                            JSONObject body = new JSONObject();
                            body.put(REQUEST, "configure");
                            body.put("audio", true);
                            body.put("video", true);
                            msg.put(MESSAGE, body);
                            msg.put("jsep", obj);
                            handle.sendMessage(new PluginHandleSendMessageCallbacks(msg));
                        }catch (Exception ex) {

                        }
                    }

                    @Override
                    public JSONObject getJsep() {
                        return null;
                    }

                    @Override
                    public JanusMediaConstraints getMedia() {
                        JanusMediaConstraints cons = new JanusMediaConstraints();
                        cons.setRecvAudio(false);
                        cons.setRecvVideo(false);
                        cons.setSendAudio(true);
                        return cons;
                    }

                    @Override
                    public Boolean getTrickle() {
                        return true;
                    }

                    @Override
                    public void onCallbackError(String error) {

                    }
                });
            }
        }
//        private void newRemoteFeed(BigInteger id) { //todo attach the plugin as a listener
//
//        }


        @Override
        public void success(JanusPluginHandle pluginHandle) {
            BroadCast.this.handle = pluginHandle;

            registerToJanus();

        }

        @Override
        public void onMessage(JSONObject msg, final JSONObject jsepLocal) {
            try
            {
                String event = msg.getString("broadcast");
                if(event.equals("registered")){
                    myid = new BigInteger(msg.getString("client_id"));
                    joinToJanus();
                } else if(event.equals("joined")) {

                    publishOwnFeed();

                } else if(event.equals("destroyed")) {

                } else if(event.equals("attached")) {

                } else if(event.equals("event")) {
                    if(msg.has("configure")) {
                        String result = msg.getString("configure");

                    } else if(msg.has("leaving")) {

                    } else if(msg.has("unpublished")) {

                    } else {
                        //todo error
                    }
                }
                if(jsepLocal != null) {
                    handle.handleRemoteJsep(new PluginHandleWebRTCCallbacks(null, jsepLocal, false));
                }
            }
            catch (Exception ex)
            {

            }

            if(jsepLocal != null)
            {
                handle.handleRemoteJsep(new IPluginHandleWebRTCCallbacks() {
                    final JSONObject myJsep = jsepLocal;
                    @Override
                    public void onSuccess(JSONObject obj) {

                    }

                    @Override
                    public JSONObject getJsep() {
                        return myJsep;
                    }

                    @Override
                    public JanusMediaConstraints getMedia() {
                        return null;
                    }

                    @Override
                    public Boolean getTrickle() {
                        return Boolean.FALSE;
                    }

                    @Override
                    public void onCallbackError(String error) {

                    }
                });
            }
        }

        @Override
        public void onLocalStream(MediaStream stream) {
            stream.videoTracks.get(0).addRenderer(new VideoRenderer(localRender));
            VideoRendererGui.update(localRender, 0, 0, 25, 25, VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, false);
        }

        @Override
        public void onRemoteStream(MediaStream stream) {
            stream.videoTracks.get(0).setEnabled(true);
            if(stream.videoTracks.get(0).enabled())
                Log.d("JANUSCLIENT", "video tracks enabled");
            stream.videoTracks.get(0).addRenderer(new VideoRenderer(remoteRender));
            VideoRendererGui.update(remoteRender, 0, 0, 25, 25, VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, true);
            VideoRendererGui.update(localRender, 72, 72, 25, 25, VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, false);
        }

        @Override
        public void onDataOpen(Object data) {

        }

        @Override
        public void onData(Object data) {

        }

        @Override
        public void onCleanup() {

        }

        @Override
        public JanusSupportedPluginPackages getPlugin() {
            return JanusSupportedPluginPackages.JANUS_ECHO_BROADCAST;
        }

        @Override
        public void onCallbackError(String error) {

        }

        @Override
        public void onDetached() {

        }

    }

    public BroadCast(VideoRenderer.Callbacks localRender, VideoRenderer.Callbacks remoteRender, String uri) {
        this.localRender = localRender;
        this.JANUS_URI = uri;
        this.remoteRender = remoteRender;
        janusServer = new JanusServer(new JanusGlobalCallbacks());
    }

    public boolean initializeMediaContext(Context context, boolean audio, boolean video, boolean videoHwAcceleration, EGLContext eglContext){
        return janusServer.initializeMediaContext(context, audio, video, videoHwAcceleration, eglContext);
    }

    public void Start() {
        janusServer.Connect();
    }
}
