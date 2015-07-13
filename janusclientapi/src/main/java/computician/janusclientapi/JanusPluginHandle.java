package computician.janusclientapi;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.*;

import java.math.BigInteger;

/**
 * Created by ben.trent on 6/25/2015.
 */
public class JanusPluginHandle {

    private boolean started = false;
    private MediaStream myStream = null;
    private MediaStream remoteStream = null;
    private SessionDescription mySdp = null;
    private PeerConnection pc = null;
    private DataChannel dataChannel = null;
    private boolean trickle = true;
    private boolean iceDone = false;
    private boolean sdpSent = false;

    private final String VIDEO_TRACK_ID = "JANUS_VIDEO_TRACK";
    private final String AUDIO_TRACK_ID = "JANUS_AUDIO_TRACK";
    private final String LOCAL_MEDIA_ID = "JANUS_MEDIA_STREAM";

    private class WebRtcObserver implements SdpObserver, PeerConnection.Observer
    {
        private final IPluginHandleWebRTCCallbacks webRtcCallbacks;
        public WebRtcObserver(IPluginHandleWebRTCCallbacks callbacks)
        {
            this.webRtcCallbacks = callbacks;
        }

        @Override
        public void onSetSuccess()
        {
            if(mySdp == null){
                createSdpInternal(webRtcCallbacks, false);
            }
        }

        @Override
        public void onSetFailure(String error)
        {
            //todo JS api does not account for this
            webRtcCallbacks.onCallbackError(error);
        }

        @Override
        public void onCreateSuccess(SessionDescription sdp)
        {
            onLocalSdp(sdp, webRtcCallbacks);
        }

        @Override
        public void onCreateFailure(String error)
        {
            webRtcCallbacks.onCallbackError(error);
        }

        @Override
        public void onSignalingChange(PeerConnection.SignalingState state)
        {
            switch(state)
            {
                case STABLE:
                    break;
                case HAVE_LOCAL_OFFER:;
                    break;
                case HAVE_LOCAL_PRANSWER:
                    break;
                case HAVE_REMOTE_OFFER:
                    break;
                case HAVE_REMOTE_PRANSWER:
                    break;
                case CLOSED:
                    break;
            }
        }

        @Override
        public void onIceConnectionChange(PeerConnection.IceConnectionState state)
        {
            switch(state)
            {
                case DISCONNECTED:
                    break;
                case CONNECTED:
                    break;
                case NEW:
                    break;
                case CHECKING:
                    break;
                case CLOSED:
                    break;
                case FAILED:
                    break;
                default:
                    break;
            }
        }

        @Override
        public void onIceGatheringChange(PeerConnection.IceGatheringState state)
        {
            switch(state)
            {
                case NEW:
                    break;
                case GATHERING:
                    break;
                case COMPLETE:
                    break;
                default:
                    break;
            }
        }

        @Override
        public void onIceCandidate(IceCandidate candidate)
        {
            if(!trickle && candidate == null){
                sendSdp(webRtcCallbacks);
            } else {
                sendTrickleCandidate(candidate);
            }
        }

        @Override
        public void onAddStream(MediaStream stream)
        {
            remoteStream = stream;
            onRemoteStream(stream);
        }

        @Override
        public void onRemoveStream(MediaStream stream)
        {
            //TODO
        }

        @Override
        public void onDataChannel(DataChannel channel)
        {
            //TODO
        }

        @Override
        public void onRenegotiationNeeded()
        {
            //TODO
        }

    }

    private PeerConnectionFactory sessionFactory = null;
    private final JanusServer server;
    public final JanusSupportedPluginPackages plugin;
    public final BigInteger id;
    private final IJanusPluginCallbacks callbacks;
    public JanusPluginHandle(JanusServer server, JanusSupportedPluginPackages plugin, BigInteger handle_id, IJanusPluginCallbacks callbacks)
    {
        this.server = server;
        this.plugin = plugin;
        id = handle_id;
        this.callbacks = callbacks;
        sessionFactory = new PeerConnectionFactory();
    }

    public void onMessage(String msg)
    {
        try
        {
            JSONObject obj = new JSONObject(msg);
            callbacks.onMessage(obj, null);
        }
        catch(JSONException ex)
        {

        }
    }

    public void onMessage(JSONObject msg, JSONObject jsep)
    {
        callbacks.onMessage(msg, jsep);
    }

    public void onLocalStream(MediaStream stream)
    {
        callbacks.onLocalStream(stream);
    }

    public void onRemoteStream(MediaStream stream)
    {
        callbacks.onRemoteStream(stream);
    }

    public void onDataOpen(Object data)
    {
        callbacks.onDataOpen(data);
    }

    public void onData(Object data)
    {
        callbacks.onData(data);
    }

    public void onCleanup()
    {
        callbacks.onCleanup();
    }

    public void onDetached()
    {
        callbacks.onDetached();
    }

    public void sendMessage(IPluginHandleSendMessageCallbacks obj)
    {
        server.sendMessage(TransactionType.plugin_handle_message, id, obj, plugin);
    }

    private void streamsDone(IPluginHandleWebRTCCallbacks webRTCCallbacks)
    {
        MediaConstraints pc_cons = new MediaConstraints();
        pc_cons.optional.add(new MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"));
        if(webRTCCallbacks.getMedia().getRecvAudio())
            pc_cons.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        if(webRTCCallbacks.getMedia().getRecvVideo())
            pc_cons.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
        pc = sessionFactory.createPeerConnection(server.iceServers, pc_cons, new WebRtcObserver(webRTCCallbacks));
        if(myStream != null)
            pc.addStream(myStream);
        if(webRTCCallbacks.getJsep() == null)
        {
            createSdpInternal(webRTCCallbacks, true);
        } else {
            try
            {
                JSONObject obj = webRTCCallbacks.getJsep();
                String sdp = obj.getString("sdp");
                SessionDescription.Type type = SessionDescription.Type.fromCanonicalForm(obj.getString("type"));
                SessionDescription sessionDescription = new SessionDescription(type, sdp);
                pc.setRemoteDescription(new WebRtcObserver(webRTCCallbacks), sessionDescription);
            }
            catch(Exception ex)
            {
                webRTCCallbacks.onCallbackError(ex.getMessage());
            }
        }
    }

    public void createOffer(IPluginHandleWebRTCCallbacks webrtcCallbacks)
    {
        prepareWebRtc(webrtcCallbacks);
    }

    public void createAnswer(IPluginHandleWebRTCCallbacks webrtcCallbacks)
    {
        prepareWebRtc(webrtcCallbacks);
    }

    private void prepareWebRtc(IPluginHandleWebRTCCallbacks callbacks)
    {
        if(pc != null)
        {
            if(callbacks.getJsep() == null)
            {
                createSdpInternal(callbacks, true);
            }
            else
            {
                try
                {
                    JSONObject jsep = callbacks.getJsep();
                    String sdpString = jsep.getString("sdp");
                    SessionDescription.Type type = SessionDescription.Type.fromCanonicalForm(jsep.getString("type"));
                    SessionDescription sdp = new SessionDescription(type, sdpString);
                    pc.setRemoteDescription(new WebRtcObserver(callbacks), sdp);
                }
                catch(JSONException ex)
                {

                }
            }
        }
        else
        {
            trickle = callbacks.getTrickle();
            AudioTrack audioTrack = null;
            VideoTrack videoTrack = null;
            MediaStream stream = null;
            if(callbacks.getMedia().getSendAudio())
            {
                AudioSource source = sessionFactory.createAudioSource(new MediaConstraints());
                audioTrack = sessionFactory.createAudioTrack(AUDIO_TRACK_ID, source);
            }
            if(callbacks.getMedia().getSendVideo())
            {
                VideoCapturerAndroid capturer = null;
                switch(callbacks.getMedia().getCamera())
                {
                    case back:
                        capturer = VideoCapturerAndroid.create(VideoCapturerAndroid.getNameOfBackFacingDevice());
                        break;
                    case front:
                        capturer = VideoCapturerAndroid.create(VideoCapturerAndroid.getNameOfFrontFacingDevice());
                        break;
                }
                MediaConstraints constraints = new MediaConstraints();
                JanusMediaConstraints.JanusVideo videoConstraints = callbacks.getMedia().getVideo();
                constraints.mandatory.add(new MediaConstraints.KeyValuePair("maxHeight", Integer.toString(videoConstraints.getMaxHeight())));
                constraints.optional.add(new MediaConstraints.KeyValuePair("minHeight", Integer.toString(videoConstraints.getMinHeight())));
                constraints.mandatory.add(new MediaConstraints.KeyValuePair("maxWidth", Integer.toString(videoConstraints.getMaxWidth())));
                constraints.optional.add(new MediaConstraints.KeyValuePair("minWidth", Integer.toString(videoConstraints.getMinWidth())));
                constraints.mandatory.add(new MediaConstraints.KeyValuePair("maxFrameRate", Integer.toString(videoConstraints.getMaxFramerate())));
                constraints.optional.add(new MediaConstraints.KeyValuePair("minFrameRate", Integer.toString(videoConstraints.getMinFramerate())));
                VideoSource source = sessionFactory.createVideoSource(capturer, constraints);
                videoTrack = sessionFactory.createVideoTrack(VIDEO_TRACK_ID, source);
            }
            if(audioTrack != null || videoTrack != null)
            {
                stream = sessionFactory.createLocalMediaStream(LOCAL_MEDIA_ID);
                if(audioTrack != null)
                    stream.addTrack(audioTrack);
                if(videoTrack != null)
                    stream.addTrack(videoTrack);
            }
            myStream = stream;
            if(stream != null)
                onLocalStream(stream);
            streamsDone(callbacks);
        }
    }

    private void createSdpInternal(IPluginHandleWebRTCCallbacks callbacks, Boolean isOffer)
    {
        MediaConstraints pc_cons = new MediaConstraints();
        pc_cons.optional.add(new MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"));
        if(callbacks.getMedia().getRecvAudio())
            pc_cons.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        if(callbacks.getMedia().getRecvVideo())
            pc_cons.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
        if(isOffer)
        {
            pc.createOffer(new WebRtcObserver(callbacks), pc_cons);
        }
        else
        {
            pc.createAnswer(new WebRtcObserver(callbacks), pc_cons);
        }
    }

    public void handleRemoteJsep(IPluginHandleWebRTCCallbacks webrtcCallbacks)
    {
        if(sessionFactory == null) {
            webrtcCallbacks.onCallbackError("WebRtc PeerFactory is not initialized. Please call initializeMediaContext");
            return;
        }
        JSONObject jsep = webrtcCallbacks.getJsep();
        if(jsep != null)
        {
            if(pc == null)
            {
                callbacks.onCallbackError("No peerconnection created, if this is an answer please use createAnswer");
                return;
            }
            try
            {
                String sdpString = jsep.getString("sdp");
                SessionDescription.Type type = SessionDescription.Type.fromCanonicalForm(jsep.getString("type"));
                SessionDescription sdp = new SessionDescription(type, sdpString);
                pc.setRemoteDescription(new WebRtcObserver(webrtcCallbacks), sdp);
            }
            catch (JSONException ex)
            {
                webrtcCallbacks.onCallbackError(ex.getMessage());
            }
        }
    }

    public void hangUp()
    {
        if(remoteStream != null)
        {
            remoteStream.dispose();
            remoteStream = null;
        }
        if(myStream != null)
        {
            myStream.dispose();
            myStream = null;
        }
        if(pc != null && pc.signalingState() != PeerConnection.SignalingState.CLOSED)
            pc.close();
        pc = null;
        started = false;
        mySdp = null;
        if(dataChannel != null)
            dataChannel.close();
        dataChannel = null;
        trickle = true;
        iceDone = false;
        sdpSent = false;
    }

    public void detach()
    {
        hangUp();
        JSONObject obj = new JSONObject();
        server.sendMessage(obj, JanusMessageType.detach, id);
    }

    public void onLocalSdp(SessionDescription sdp, IPluginHandleWebRTCCallbacks callbacks)
    {
        if(pc != null)
        {
            if(mySdp == null) {
                mySdp = sdp;
                pc.setLocalDescription(new WebRtcObserver(callbacks), sdp);
            }
            if(!iceDone && !trickle)
                return;
            if(sdpSent)
                return;

            try
            {
                sdpSent = true;
                JSONObject obj = new JSONObject();
                obj.put("sdp", mySdp.description);
                obj.put("type", mySdp.type.canonicalForm());
                callbacks.onSuccess(obj);
            }
            catch(JSONException ex)
            {
                callbacks.onCallbackError(ex.getMessage());
            }
        }
    }

    private void sendTrickleCandidate(IceCandidate candidate)
    {
        try
        {
            JSONObject message = new JSONObject();
            JSONObject cand = new JSONObject();
            if(candidate == null)
                cand.put("completed", true);
            else
            {
                cand.put("candidate", candidate.sdp);
                cand.put("sdpMid", candidate.sdpMid);
                cand.put("sdpMLineIndex", candidate.sdpMLineIndex);
            }
            message.put("candidate", cand);

            server.sendMessage(message, JanusMessageType.trickle, id);
        }
        catch(JSONException ex)
        {

        }
    }

    private void sendSdp(IPluginHandleWebRTCCallbacks callbacks)
    {
        if(mySdp != null)
        {
            mySdp = pc.getLocalDescription();
            if(!sdpSent)
            {
                sdpSent = true;
                try
                {
                    JSONObject obj = new JSONObject();
                    obj.put("sdp", mySdp.description);
                    obj.put("type", mySdp.type.canonicalForm());
                    callbacks.onSuccess(obj);
                }
                catch(JSONException ex)
                {
                    callbacks.onCallbackError(ex.getMessage());
                }
            }
        }
    }
}