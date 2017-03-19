package fu.hao.cosmos_xposed.ml;

import android.app.AndroidAppHelper;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import fu.hao.cosmos_xposed.utils.MyContentProvider;
import weka.core.Instances;

import static fu.hao.cosmos_xposed.utils.MyContentProvider.LAYOUT_CONTENT_URI;
import static fu.hao.cosmos_xposed.utils.MyContentProvider.NEW_INSTANCE_CONTENT_URI;

/**
 * Description:
 *
 * @author Hao Fu(haofu AT ucdavis.edu)
 * @since 3/17/2017
 */
public class SelfAdaptiveLearning {
    public static String TAG = SelfAdaptiveLearning.class.getSimpleName();

    private static Context context = AndroidAppHelper.currentApplication();

    public static LabelledDocs getLabelledDocs() throws Exception {
        InputStream inputStream = context.getContentResolver().openInputStream(
                MyContentProvider.NEW_INSTANCE_CONTENT_URI);
        ObjectInputStream is = new ObjectInputStream(inputStream);
        LabelledDocs labelledDocs = (LabelledDocs) is.readObject();
        is.close();

        return labelledDocs;
    }


    /**
     * Whether perform SelfAdaptiveLearning
     * @return
     */
    public static boolean doIt() {
        Calendar calendar = Calendar.getInstance();
        int hours = calendar.get(Calendar.HOUR_OF_DAY);

        if (hours >= 2 && hours <= 5) {
            return true;
        }

        return false;
    }

    public void storeNewInstance(String label, String doc) {
        ContentValues values = new ContentValues();
        values.put(MyContentProvider.INSTANCE_DATA, doc);
        values.put(MyContentProvider.INSTANCE_LABEL, label);
        Uri uri = context.getContentResolver().insert(MyContentProvider.NEW_INSTANCE_CONTENT_URI, values);
        Log.i(TAG, "New labelled instance stored: " + uri);

        /*
        LabelledDoc labelledDoc = new LabelledDoc(label, doc);

        File cacheDir = context.getCacheDir();
        File outFile = new File(cacheDir, "newLabelled.data");

        try {
            FileOutputStream fileOutputStream = new FileOutputStream(outFile);
            ObjectOutputStream os = new ObjectOutputStream(fileOutputStream);
            os.writeObject(labelledDoc);
            os.close();
            Log.i(TAG, "newLabelled.data saved.");
        } catch (IOException e) {
            e.printStackTrace();
        } */
    }

    public void exportInstances() {
        Cursor cursor = context.getContentResolver().query(NEW_INSTANCE_CONTENT_URI, null, null, null,
                null);
        List<String> docs = new ArrayList<>();
        do {
            String extracted = cursor.getString(cursor.getColumnIndex(MyContentProvider.NAME));
            String doc =
            docs.add());
        } while (cursor.moveToNext());
        Instances instances = WekaUtils.docs2Instances(docs);

    }


}