package android.myapplication;

import android.app.DownloadManager;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class MyService extends Service {
    private DownloadBinder mBinder = new DownloadBinder();
    public MyService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return mBinder;
    }

    @Override
    public void onCreate(){
        super.onCreate();
        Log.d("MyService","服务启动了");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("MyService","服务被销毁了");
    }
    class DownloadBinder extends Binder {
        public void startDownload(){
            Log.d("MyService","开始下载");
        }
        public void stopDownload(){
            Log.d("MyService","停止下载");
        }
    }
}
