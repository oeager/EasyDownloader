package com.bision.download.listener;

import com.bision.download.core.Request;

/**
 * Created by Hello on 2015/9/15.
 */
public interface DownloadListener {

    void onWaiting(Request request);

    void onPreparing(Request request);

    void onStarted(Request request);

    void onStop(Request request);

    void onResume(Request request);

    void onLoading(Request request, int currentSize, int totalSize);

    void onEnd(Request request);

    void onError(Request request, String errorInfo);
}
