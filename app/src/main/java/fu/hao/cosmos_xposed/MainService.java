package fu.hao.cosmos_xposed;

import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;
import android.util.Log;

import java.util.List;

import fu.hao.cosmos_xposed.ml.WekaUtils;
import fu.hao.cosmos_xposed.utils.MyContentProvider;

import static fu.hao.cosmos_xposed.utils.MyContentProvider.LAYOUT_CONTENT_URI;
import static fu.hao.cosmos_xposed.utils.MyContentProvider.PREDICTION_RES_URI;

public class MainService extends Service {
    public final String TAG = MainService.class.getName();

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
                String index = intent.getStringExtra("index");
                if (texts != null && !texts.isEmpty()) {
                    Log.w(TAG, texts);
                    String res = WekaUtils.predict(texts, WekaUtils.getStringToWordVector(),
                            WekaUtils.getWekaModel(), null);
                    Log.w(TAG, index + ": Predicted as " + res);

                    ContentValues values = new ContentValues();
                    values.put(MyContentProvider.PREDICTIONS_INDEX, index);
                    values.put(MyContentProvider.PREDICTIONS_DATA, res);
                    getContentResolver().delete(PREDICTION_RES_URI, null, null);
                    Uri uri = getContentResolver().insert(PREDICTION_RES_URI, values);
                    Log.i(TAG, "Prediction res stored: " + uri);
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
