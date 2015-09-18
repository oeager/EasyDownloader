package com.bision.download.core;

import android.content.Context;
import android.util.Log;

import com.bision.download.assist.DBHelper;
import com.bision.download.assist.State;
import com.bision.download.bean.ChunkInfo;
import com.bision.download.bean.Counter;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by oeager on 2015/9/15.
 */
public class DownloadManager {


    private AtomicInteger mSerialNumber = new AtomicInteger();

    private final AtomicInteger executeRequestCount = new AtomicInteger(0);

    private final Map<String, Queue<Request>> mWaitingRequest = new HashMap<>();

    private final Set<Request> mCurrentRequest = new HashSet<>();

    private Queue<Request> mRequestQueue = new PriorityQueue<>();

    private ExecutorService mService;

    private Downloader downloader;

    private DownloadDispatcher dispatcher;

    private final int maxParallelRequestCount;

    private DBHelper dbHelper;

    private final static String TAG = "DownloadManager";

    public DownloadManager(Context mContext) {
        this(mContext, 5, 3);
    }

    public DownloadManager(Context mContext, int maxConnections, int maxParallelRequestCount) {
        mService = Executors.newFixedThreadPool(maxConnections);
        downloader = new HttpDefaultDownloader();
        dispatcher = new DownloadDispatcher(this);
        this.maxParallelRequestCount = maxParallelRequestCount;
        dbHelper = new DBHelper(mContext);

    }

    public Request enqueue(Request request) {
        boolean isShouldModifyQueue;
        synchronized (mCurrentRequest) {
            isShouldModifyQueue = mCurrentRequest.add(request);
            if (isShouldModifyQueue) {
                request.setId(mSerialNumber.incrementAndGet());
            }
        }
        if (!isShouldModifyQueue) {
            return request;
        }
        String linkUrl = request.getLinkUrl();
        synchronized (mWaitingRequest) {

            if (mWaitingRequest.containsKey(linkUrl)) {
                Queue<Request> stageRequest = mWaitingRequest.get(linkUrl);
                if (stageRequest == null) {
                    stageRequest = new LinkedList<>();
                }
                stageRequest.add(request);
                mWaitingRequest.put(linkUrl, stageRequest);
            } else {
                mWaitingRequest.put(linkUrl, null);
                mRequestQueue.add(request);
            }
            int state = request.getState();
            if(state==State.INITIALIZED){
                dispatcher.dispatchOnWaiting(request);
            }else if (state==State.STOPPED){
                //do nothing
            }else {
                throw  new IllegalArgumentException("bad request");
            }

            submit();
        }

        return request;
    }

    void submit() {
        final Request r = mRequestQueue.poll();
        if (r == null) {
            return;
        }
        final int state = r.getState();
        if (state != State.WAITING&&state!=State.STOPPED) {
            finish(r,true);
            return;
        }
        if (executeRequestCount.get() >= maxParallelRequestCount) {
            return;
        }
        executeRequestCount.incrementAndGet();
        mService.execute(new Runnable() {
            @Override
            public void run() {
                if(state==State.WAITING){
                    dispatcher.dispatchPreparing(r);
                }
                String filePath = r.getFilePath();
                File f = new File(filePath);
                if (f.exists()) {
                    dispatcher.dispatchStart(r);
                    dispatcher.dispatchEnd(r);
                    return;
                }
                try {
                    int contentLength = downloader.getContentLength(r.getLinkUrl());
//                    Log.e(TAG,"contentLength is "+ contentLength);
                    if(r.supportAutoChunk()){
                        r.setChunkCount(autoChunkCount(contentLength));
                    }
                    Counter counter = new Counter();
                    counter.contentLength = contentLength;
                    List<ChunkInfo> chunks = getRecordChunks(r);
                    if (chunks == null) {//从未有匹配断点记录或主动不断点
                        dbHelper.deleteOldData(r.getLinkUrl(), r.getFilePath());
                        String tempFilePath = r.getTempFilePath();
                        RandomAccessFile raf = new RandomAccessFile(tempFilePath, "rwd");
                        raf.setLength(contentLength);
                        raf.close();
                        chunks = makeChunks(contentLength, r.getChunkCount());
                        if (r.isStartByBreakPoint()) {
                            int requestId = dbHelper.insertRequestInfo(r);
                            dbHelper.insertChunkInfo(requestId, chunks);
                        }
                    }
//                    else{
//                        Log.e(TAG,"找到历史记录，断点开始下载");
//                    }
                    r.setCounter(counter);
                    if(r.getState()==State.PREPARING){
                        dispatcher.dispatchStart(r);
                    }else{
                        dispatcher.dispatchResume(r);
                    }

                    for (ChunkInfo chunk : chunks) {
                        counter.loadAmount += chunk.amount;
                        mService.execute(new DownloadRunnable(downloader, r, chunk, dispatcher));
                    }
                } catch (IOException e) {
                    dispatcher.dispatchError(r, e);
                }

            }
        });

    }

    List<ChunkInfo> makeChunks(int fileLength, int executeThreadCount) {
// 进算下载块数
        int block = fileLength / executeThreadCount;
        int startPos = 0;
        int endPos = 0;
        ChunkInfo info;
        List<ChunkInfo> chunks = new ArrayList<ChunkInfo>();
        for (int i = 0; i < executeThreadCount; i++) {
            info = new ChunkInfo();
            // 计算每个线程的开始和结束位置
            startPos = i * block;
            if (i == (executeThreadCount - 1)) {
                endPos = fileLength - 1;
            } else {
                endPos = (i + 1) * block - 1;
            }

            // 添加下载信息
            info.startPos = startPos;
            info.endPos = endPos;
            info.amount = 0;
            info.index = i;
            chunks.add(info);
        }

        return chunks;
    }

    List<ChunkInfo> getRecordChunks(Request request) {
        if (!request.isStartByBreakPoint()) {
//            Log.e(TAG,"不需要支持断点，重新下载");
            return null;
        }
        String tempFile = request.getTempFilePath();
        File f = new File(tempFile);
        if(!f.exists()){
//            Log.e(TAG,"未发现临时下载文件，重新下载");
            return null;
        }
        int recordId = dbHelper.getMatchRequestId(request);
        if (recordId == -1) {
//            Log.e(TAG,"未发现匹配历史记录，重新下载");
            return null;
        }

        List<ChunkInfo> chunks = dbHelper.getChunksById(recordId);
        return chunks;
    }

    int autoChunkCount(int length){
        int mb = 1024*1024;
        if(length<=5*mb){
            return 1;
        }else if (length>5*mb&&length<10*mb){
            return 2;
        }else {
            return 3;
        }
    }

    public void updateChunkInfo(long chunkId, int newReadByte) {
        dbHelper.updateChunk(chunkId, newReadByte);
    }

    public void finish(Request r,boolean deleteRecords) {
        synchronized (mCurrentRequest) {
            mCurrentRequest.remove(r);
        }
        executeRequestCount.decrementAndGet();
        if(deleteRecords){
            dbHelper.deleteOldData(r.getLinkUrl(),r.getFilePath());
        }
        synchronized (mWaitingRequest) {
            String linkUrl = r.getLinkUrl();
            Queue<Request> waitingTasks = mWaitingRequest.remove(linkUrl);
            if (waitingTasks != null) {
                mRequestQueue.addAll(waitingTasks);
            }
        }
        submit();
    }

    public synchronized void shutDown() {
        mWaitingRequest.clear();
        mCurrentRequest.clear();
        mRequestQueue.clear();
        mService.shutdown();
    }

    public void stop(Request request){
        synchronized (request){
            dispatcher.dispatchStopped(request);
        }
    }

    public void resume(Request request){
        enqueue(request);
    }


}
