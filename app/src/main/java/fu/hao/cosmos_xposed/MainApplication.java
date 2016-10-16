package fu.hao.cosmos_xposed;

import android.app.Application;
import android.content.res.AssetManager;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import fu.hao.cosmos_xposed.hook.XMethod;

/**
 * Description:
 *
 * @author Hao Fu(haofu AT ucdavis.edu)
 * @since 10/14/2016
 */
public class MainApplication extends Application {
    private final String TAG = this.getClass().getName();

    public static final String SENS_DEF_FILE = "test.txt";
    public static final String SENS_DEF_FILE_PATH =
            Environment.getExternalStorageDirectory().getAbsolutePath() + "/COSMOS/" + SENS_DEF_FILE;

    private boolean copyAsset(AssetManager assetManager,
                                     String fromAssetPath, String toPath) {
        InputStream in = null;
        OutputStream out = null;
        try {
            in = assetManager.open(fromAssetPath);
            File newFile = new File(toPath);
            if (newFile.exists()) {
                return false;
            }
            if (!newFile.getParentFile().exists()) {
                newFile.getParentFile().mkdirs();
            }
            newFile.createNewFile();
            out = new FileOutputStream(toPath);
            copyFile(in, out);
            in.close();
            in = null;
            out.flush();
            out.close();
            out = null;
            Log.w(TAG, "Copy " + fromAssetPath + " to " + toPath);
            return true;
        } catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while((read = in.read(buffer)) != -1){
            out.write(buffer, 0, read);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.e(TAG, "COSMOS start!");
        Log.w(TAG, "Copying asset ");
        //copyAsset(getAssets(), SENS_DEF_FILE, SENS_DEF_FILE_PATH);
    }
}
