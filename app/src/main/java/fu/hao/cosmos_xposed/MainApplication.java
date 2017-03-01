package fu.hao.cosmos_xposed;

import android.app.Application;
import android.content.Context;
import android.content.res.AssetManager;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import fu.hao.cosmos_xposed.ml.MalletUtils;

// import static fu.hao.cosmos_xposed.ml.ClassifierUtils.MODEL_FILE_PATH;

/**
 * Description:
 *
 * @author Hao Fu(haofu AT ucdavis.edu)
 * @since 10/14/2016
 */
public class MainApplication extends Application {
    private static final String TAG = MainApplication.class.getName();

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
            out.flush();
            out.close();
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
        //copyAsset(getAssets(), "weka_models/test.model", MODEL_FILE_PATH);
        copyAsset(getAssets(), "mallet/trained.model", MalletUtils.MODEL_FILE_PATH);
    }

    public static void write2File(String fileName, String data, Context context) {
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput(fileName, Context.MODE_PRIVATE));
            outputStreamWriter.write(data);
            outputStreamWriter.close();
            Log.i(TAG, "Write to " + fileName + " success!");
        }
        catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    public static String readFromFile(String fileName, Context context) {

        String ret = "";

        try {
            InputStream inputStream = context.openFileInput("config.txt");

            if ( inputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ( (receiveString = bufferedReader.readLine()) != null ) {
                    stringBuilder.append(receiveString);
                }

                inputStream.close();
                ret = stringBuilder.toString();
            }
        }
        catch (FileNotFoundException e) {
            Log.e(TAG, "File not found: " + e.toString());
        } catch (IOException e) {
            Log.e(TAG, "Can not read file: " + e.toString());
        }

        return ret;
    }

    /* Checks if external storage is available for read and write */
    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /* Checks if external storage is available to at least read */
    public static boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }

    public static void write2FileExternally(String fileName, String data) {
        if (!isExternalStorageWritable()) {
            Log.e(TAG, "External storage not writeable!");
            return;
        }
        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() +
                File.separator + fileName);
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(file));
            bw.write(data);
            bw.close();
            Log.i(TAG, "Write to " + fileName + " successfully!");
        } catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
            e.printStackTrace();
        }
    }

    public static String readFromFileExternally(String filePath) {
        if (!isExternalStorageReadable()) {
            Log.e(TAG, "External storage not readable!");
            return null;
        }

        StringBuilder text = new StringBuilder();
        try {
            File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() +
                    File.separator + filePath);

            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) != null) {
                text.append(line);
                Log.i("Test", "text : " + text + " : end");
                text.append('\n');
            }

            br.close();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Can not read file: " + e.toString());
        }

        return text.toString();
    }






}
