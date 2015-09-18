package com.bision.download.assist;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by Oeager on 2015/9/15.
 */
public interface Priority {
    public static final int LOW = 0x0;
    public static final int NORMAL = 0x1;
    public static final int HIGH = 0x2;
    public static final int IMMEDIATE = 0x3;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({LOW,NORMAL,HIGH,IMMEDIATE})
    public @interface Sequence {

    }
}
