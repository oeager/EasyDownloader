package com.bision.download.core;

import com.bision.download.bean.ChunkInfo;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Hello on 2015/9/15.
 */
public class HttpDefaultDownloader implements Downloader {

    @Override
    public InputStream load(String linkUrl, ChunkInfo chunk) throws IOException {
        URL u = new URL(linkUrl);
        HttpURLConnection conn = (HttpURLConnection) u.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(15000);
        //设置范围数据

        conn.setRequestProperty("Range", "bytes=" + (chunk.startPos + chunk.amount) + "-" + chunk.endPos);

        return conn.getInputStream();
    }

    @Override
    public int getContentLength(String linkUrl) throws IOException {
        URL url = new URL(linkUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(15000);
        conn.connect();
        return conn.getContentLength();
    }
}
