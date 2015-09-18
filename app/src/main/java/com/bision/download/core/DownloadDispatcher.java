package com.bision.download.core;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.bision.download.assist.State;
import com.bision.download.bean.Counter;
import com.bision.download.listener.DownloadListener;

/**
 * Created by oeager on 2015/9/16.
 */
public class DownloadDispatcher {


    public final static int DISPATCH_WAITING = 0;

    public final static int DISPATCH_PREPARING = 1;

    public final static int DISPATCH_STARTED = 2;

    public final static int DISPATCH_RESUME = 3;

    public final static int DISPATCH_STOP = 4;

    public final static int DISPATCH_END = 5;

    public final static int DISPATCH_ERROR = 6;

    public final static int DISPATCH_PROCESS = 7;


    private DownloadManager manager;
    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            doMessage(msg);
        }
    };

    public DownloadDispatcher(DownloadManager manager) {
        this.manager = manager;
    }

    public void dispatchOnWaiting(Request r) {
        r.setState(State.WAITING);
        messageWrapperAndSend(DISPATCH_WAITING, r, null);

    }

    public void dispatchPreparing(Request r) {
        r.setState(State.PREPARING);
        messageWrapperAndSend(DISPATCH_PREPARING, r, null);
    }


    public void dispatchStart(Request request) {
        request.setState(State.STARTED);
        messageWrapperAndSend(DISPATCH_STARTED, request, null);

    }

    public void dispatchResume(Request r) {
        r.setState(State.STARTED);
        messageWrapperAndSend(DISPATCH_RESUME, r, null);
    }

    public void dispatchStopped(Request r) {
        r.setState(State.STOPPED);
        manager.finish(r,false);
        messageWrapperAndSend(DISPATCH_STOP, r, null);
    }

    public void dispatchLoadingProcess(Request request, long chunkId, int newReadByte) {
        synchronized (request) {
            Counter counter = request.getCounter();
            counter.loadAmount += newReadByte;

            if(request.isStartByBreakPoint()){
                manager.updateChunkInfo(chunkId,newReadByte);
            }
            messageWrapperAndSend(DISPATCH_PROCESS, request, null);
        }
    }

    public void dispatchError(Request r, Throwable t) {
        Log.e("DownloadLister",t.toString());
        r.setState(State.ERROR);
        manager.finish(r,false);
        Bundle bundle = new Bundle();
        bundle.putString("msg", t.toString());
        messageWrapperAndSend(DISPATCH_ERROR, r, bundle);
    }

    public void dispatchEnd(Request r) {
        r.setState(State.END);
        manager.finish(r,true);
        messageWrapperAndSend(DISPATCH_END, r, null);
    }

    void messageWrapperAndSend(int what, Request request, Bundle bundle) {
        Message msg = mHandler.obtainMessage(what);
        msg.setData(bundle);
        msg.obj = request;
        mHandler.sendMessage(msg);
    }

    static void doMessage(Message msg) {
        Request r = (Request) msg.obj;
        int what = msg.what;
        DownloadListener listener = r.getListener();
        Bundle b = msg.peekData();
        if (listener == null) {
            return;
        }
        switch (what) {
            case DISPATCH_WAITING:
                listener.onWaiting(r);
                break;
            case DISPATCH_PREPARING:
                listener.onPreparing(r);
                break;
            case DISPATCH_STARTED:
                listener.onStarted(r);
                break;
            case DISPATCH_RESUME:
                listener.onResume(r);
                break;
            case DISPATCH_STOP:
                listener.onStop(r);
                break;
            case DISPATCH_ERROR:

                String exception = "unKnown";
                if (b != null) {
                    exception = b.getString("msg");
                }
                listener.onError(r, exception);
                break;
            case DISPATCH_END:
                listener.onEnd(r);
                break;
            case DISPATCH_PROCESS:
                Counter c = r.getCounter();
                listener.onLoading(r, c.loadAmount, c.contentLength);
                break;
            default:
        }
    }
}
