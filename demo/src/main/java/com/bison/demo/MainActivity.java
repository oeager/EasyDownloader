package com.bison.demo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bision.download.assist.State;
import com.bision.download.core.DownloadManager;
import com.bision.download.core.Request;
import com.bision.download.listener.DownloadListener;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private final DownloadManager dm = new DownloadManager(this,5,3);

    private final static String LINK_URL = "http://a3yd.pc6.com/cx/QQQingLv.pc6.apk";
    Request request;

    private final static String TAG = "DownloadListen";

    private ProgressBar progressBar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        request = new Request();
        request.setLinkUrl(LINK_URL);
        request.setFileName("QQ情侣");
        request.setChunkCount(3);
        request.openBreakPointLoad();
        request.setListener(new DownloadListener() {
            @Override
            public void onWaiting(Request request) {
                progressBar.setIndeterminate(false);
                Log.e(TAG,"------------------------------onWaiting");
            }

            @Override
            public void onPreparing(Request request) {
                progressBar.setIndeterminate(true);
                Log.e(TAG,"------------------------------onPreparing");
            }

            @Override
            public void onStarted(Request request) {
                progressBar.setIndeterminate(false);
                Log.e(TAG,"------------------------------onStarted");
            }

            @Override
            public void onStop(Request request) {
                Log.e(TAG,"------------------------------onStop");
            }

            @Override
            public void onResume(Request request) {
                Log.e(TAG,"------------------------------onResume");
            }

            @Override
            public void onLoading(Request request, int currentSize, int totalSize) {
                Log.e(TAG,String.format(Locale.ENGLISH,"----------onLoading--%1$d----%2$d",currentSize,totalSize));
                final int percent = (int) (((float) currentSize / totalSize) * 100);
                progressBar.setProgress(percent);
            }

            @Override
            public void onEnd(Request request) {
                Log.e(TAG,"------------------------------onEnd");
                AppUtils.install(MainActivity.this,request.getFilePath());

            }

            @Override
            public void onError(Request request, String errorInfo) {
                progressBar.setIndeterminate(false);
                Log.e(TAG,"------------------------------onError:"+errorInfo);
                Toast.makeText(MainActivity.this,"下载出错:"+errorInfo,Toast.LENGTH_SHORT).show();
            }
        });


    }

    public void onAction(View v ){
        if(request.getState()== State.INITIALIZED){
            dm.enqueue(request);
        }else if(request.getState()==State.ERROR){
            request.reset();
            dm.enqueue(request);
        }

    }

    public void stop(View v ){

      dm.stop(request);
    }

    public void go(View v ){

       dm.resume(request);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
