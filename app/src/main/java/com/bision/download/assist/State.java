package com.bision.download.assist;

import android.support.annotation.IntDef;

/**
 * Created by oeager on 2015/9/15.
 */
public class State {

    /**
     * 每一个下载任务=>一个Request
     * Request生命周期状态说明:
     * 1.Request的初始状态为IDLE，在重置后也将变为IDLE;
     * 2.Request的数据初始化完成后,状态将更改为Initialized，这里只作下载链接设置完成的判定;
     * 3.Request在加入下载队列中并处于候列时，状态为Waiting;
     * 4.Request被从下载队列中取出并开始建立网络连接时(还在连接中),状态更改为Preparing;
     * 5.连接成功后，并开始读出下载数据，状态更改为Started;
     * 6.只有状态处于preparing,started状态时才可以执行暂停操作，此时状态更改为Stop;
     * 7.只有处理Stop状态时，才可以执行恢复下载操作，此时状态更改为started;
     * 8.下载过程抛出错误，变更状态为Error
     * 9，下载完成，状态变更为End
     */

    public final static int IDLE= 0;

    public final static int INITIALIZED= 1;

    public final static int WAITING = 2;

    public final static int PREPARING= 3;

    public final static int STARTED= 4;

    public final static int STOPPED= 5;

    public final static int END= 7;

    public final static int ERROR=8;

    @IntDef({IDLE,INITIALIZED,WAITING,PREPARING,STARTED,STOPPED,END,ERROR})
    public @interface RequestState{}



}
