package fu.hao.cosmos_xposed;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import java.util.List;

import fu.hao.cosmos_xposed.ml.WekaUtils;

public class MainService extends Service {
    final String TAG = MainService.class.getName();

    public MainService() throws Exception {
        Log.w(TAG, "MainServiceStarted");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        /*
            Prediction
         */
        Log.w(TAG, "Main Service Start!");
        if(intent != null) {
            //int position = intent.getIntExtra("position", 0);
            try {
                WekaUtils.init(getContentResolver());
                // MainApplication.getFileExternally(WekaUtils.STRING_VEC_FILTER_PATH));
                String texts = intent.getStringExtra("texts");
                if (texts != null && !texts.isEmpty()) {
                    Log.w(TAG, texts);
                    String res = WekaUtils.predict(texts, WekaUtils.getStringToWordVector(),
                            WekaUtils.getWekaModel(), null);
                    Log.w(TAG, "Predicted as " + res);
                }
            } catch (Exception e) {
                if (e.getMessage() != null) {
                    Log.e(TAG, e.getMessage());
                }
                e.printStackTrace();
            }
        }

        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }


}
