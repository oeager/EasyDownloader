package com.bision.download.core;

import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import com.bision.download.assist.Priority;
import com.bision.download.assist.State;
import com.bision.download.bean.Counter;
import com.bision.download.listener.DownloadListener;

import java.io.File;

/**
 * Created by oeager on 2015/9/15.
 */
public final class Request implements Comparable<Request> {

    private int id;

    private String linkUrl;

    private String fileName;

    private String tempName;

    private boolean startByBreakPoint = false;

    private boolean supportAutoChunk = false;

    private int chunkCount;

    private Counter counter;

    private Object tag;

    private DownloadListener listener;

    private
    @Priority.Sequence
    int priority;

    private
    @State.RequestState
    int state;

    private String savePath;

    public Request(){
        state = State.IDLE;
        savePath = Environment.getExternalStorageDirectory().getPath()+ File.separator;
        priority = Priority.LOW;
        chunkCount = 1;
    }


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getLinkUrl() {
        return linkUrl;
    }

    public void setLinkUrl(String linkUrl) {
        this.linkUrl = linkUrl;
        if(TextUtils.isEmpty(fileName)){
            fileName = getFileName(linkUrl);
            tempName = getTempFileName(fileName);
        }

        state = State.INITIALIZED;
    }

    public int getChunkCount() {
        return chunkCount;
    }

    public void setChunkCount(int chunkCount) {
        this.chunkCount = chunkCount;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        if(fileName==null||fileName.isEmpty()){
            throw new IllegalArgumentException("fileName can not be null");
        }
        this.fileName = fileName;
    }

    public String getTempName() {
        return tempName;
    }

    public
    @Priority.Sequence
    int getPriority() {
        return priority;
    }

    public void setPriority(@Priority.Sequence int priority) {
        this.priority = priority;
    }

    public
    @State.RequestState
    int getState() {
        return state;
    }

    protected void setState(@State.RequestState int state) {
        this.state = state;
    }

    public String getSavePath() {
        return savePath;
    }

    public void setSavePath(String savePath) {
        if(TextUtils.isEmpty(savePath)){
            throw new IllegalArgumentException("savePath can not be null");
        }
        File f = new File(savePath);
        if(!f.exists()){
            f.mkdirs();
        }
        if(!f.exists()){
            throw new IllegalArgumentException("bad savePath :"+savePath);
        }
        this.savePath = savePath;
    }

    public DownloadListener getListener() {
        return listener;
    }

    public void setListener(DownloadListener listener) {
        this.listener = listener;
    }

    public Counter getCounter() {
        return counter;
    }

    protected void setCounter(Counter counter) {
        this.counter = counter;
    }

    public void setSupportAutoChunk(){
        supportAutoChunk = true;
    }

    public boolean supportAutoChunk(){
        Log.w("Request","note:the chunkCount which you set up will be replaced");
        return supportAutoChunk;
    }
    @Override
    public int compareTo(Request another) {
        int left = this.getPriority();
        int right = another.getPriority();
        return left == right ? this.getId() - another.getId() : right
                - left;
    }

    public void openBreakPointLoad(){
        startByBreakPoint = true;
    }

    public boolean isStartByBreakPoint(){
        return startByBreakPoint;
    }


    public String getFilePath() {
        return this.savePath + this.fileName;
    }

    public Object getTag() {
        return tag;
    }

    public void setTag(Object tag) {
        this.tag = tag;
    }

    public String getTempFilePath() {
        return this.savePath + this.tempName;
    }

    private static String getTempFileName(String fileName) {
        int lastIndex = fileName.lastIndexOf(".");
        if (lastIndex != -1) {
            String tempName = fileName.substring(0, lastIndex);
            return tempName + "_temp";
        } else {
            return fileName + "_temp";
        }
    }


    public static String getFileName(String url) {
        int lastIndex = url.lastIndexOf("/");
        if (lastIndex != -1) {
            String fileName = url.substring(lastIndex + 1);
            if (TextUtils.isEmpty(fileName)) {
                return "empty";
            }
            return fileName;
        }
        return "empty";
    }

    public void reset(){
        if(TextUtils.isEmpty(linkUrl)){
            state = State.IDLE;
        }else{
            state = State.INITIALIZED;
        }
    }
}
