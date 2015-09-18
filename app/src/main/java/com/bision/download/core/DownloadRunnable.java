package com.bision.download.core;

import com.bision.download.assist.State;
import com.bision.download.bean.ChunkInfo;
import com.bision.download.bean.Counter;

import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;

/**
 * Created by Hello on 2015/9/16.
 */
public class DownloadRunnable implements Runnable {

    private final Request request;

    private final Downloader downloader;

    private final ChunkInfo chunk;

    private final DownloadDispatcher dispatcher;


    public DownloadRunnable (Downloader downloader,Request request,ChunkInfo chunk,DownloadDispatcher dispatcher){
        this.downloader = downloader;
        this.request = request;
        this.chunk = chunk;
        this.dispatcher = dispatcher;
    }

    @Override
    public void run() {

        try {

            InputStream is = downloader.load(request.getLinkUrl(), chunk);

            String tempFilePath = request.getTempFilePath();

            RandomAccessFile raf = new RandomAccessFile(tempFilePath, "rwd");

            raf.seek(chunk.startPos + chunk.amount);
            byte[] buffer = new byte[4096];
            int len;

            while (request.getState()==State.STARTED&&(len = is.read(buffer))!=-1){
                // 写入本地文件
                raf.write(buffer, 0, len);
                // 信息入库
                // 更新进度
               dispatcher.dispatchLoadingProcess(request, chunk.id, len);
            }
            raf.close();
            is.close();
            if(request.getState()==State.STARTED){//单线程下载完成
                Counter counter = request.getCounter();
                if(counter.contentLength==counter.loadAmount){
                    File tempFile = new File(request.getTempFilePath());
                    File file = new File(request.getFilePath());
                    if(tempFile.renameTo(file)){
                        dispatcher.dispatchEnd(request);
                    }else{
                        dispatcher.dispatchError(request,new RuntimeException("rename file fail"));
                    }

                }
            }


        }catch (Exception e){
            dispatcher.dispatchError(request,e);
        }

    }
}
