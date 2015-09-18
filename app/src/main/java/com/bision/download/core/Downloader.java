package com.bision.download.core;

import com.bision.download.bean.ChunkInfo;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Hello on 2015/9/15.
 */
public interface Downloader {

    InputStream load(String linkUrl,ChunkInfo chunk) throws IOException;

    int getContentLength(String url)throws  IOException;
}
