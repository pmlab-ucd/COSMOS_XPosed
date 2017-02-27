package fu.hao.cosmos_xposed.ml;

import android.os.Environment;
import android.util.Log;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

import fu.hao.cosmos_xposed.MainApplication;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.classifiers.trees.RandomForest;
import weka.core.SerializationHelper;

/**
 * Description:
 *
 * @author Hao Fu(haofu AT ucdavis.edu)
 * @since 2/26/2017
 */
public class Classifier {
    private static String TAG = Classifier.class.getName();
    private RandomForest randomForest = null;

    public static final String MODEL_FILE_PATH = Environment.getExternalStorageDirectory().getAbsolutePath()
            + "/COSMOS/test.model";

    public Classifier() {
        try {
            Log.i(TAG, MainApplication.readFromFileExternally("COSMOS/test.model"));
            Log.i(TAG, "done!");
            FileInputStream fileInputStream = new FileInputStream(MODEL_FILE_PATH);
            randomForest = (RandomForest) SerializationHelper.read(fileInputStream);
            Log.i(TAG, "Model is loaded successfully");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void predict() {
        Log.i(TAG, Integer.toString(randomForest.getMaxDepth()));
    }
}
