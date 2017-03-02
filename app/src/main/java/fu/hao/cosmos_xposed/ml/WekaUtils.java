package fu.hao.cosmos_xposed.ml;

import android.content.res.AssetManager;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import fu.hao.cosmos_xposed.MainApplication;
import weka.classifiers.meta.FilteredClassifier;
import weka.classifiers.trees.J48;
import weka.classifiers.trees.RandomForest;
import weka.core.Instances;
import weka.core.SerializationHelper;
import weka.core.converters.ConverterUtils;
import weka.filters.unsupervised.attribute.Remove;

/**
 * Description:
 *
 * @author Hao Fu(haofu AT ucdavis.edu)
 * @since 2/26/2017
 */
public class WekaUtils {
    private static String TAG = WekaUtils.class.getName();
    private RandomForest randomForest = null;

    public static final String MODEL_FILE_PATH = Environment.getExternalStorageDirectory().getAbsolutePath()
            + "/COSMOS/weka.model";

    public WekaUtils() {
    }

    public void predict() {
        Log.i(TAG, Integer.toString(randomForest.getMaxDepth()));
    }

    public static Instances loadArff() throws Exception {
        ConverterUtils.DataSource source = new ConverterUtils.DataSource("D:\\workspace\\COSPOS_MINING\\output\\gnd\\Test\\train_data.arff");
        Instances data = source.getDataSet();
        // setting class attribute if the data format does not provide this information
        // For example, the XRFF format saves the class attribute information as well
        if (data.classIndex() == -1)
            data.setClassIndex(data.numAttributes() - 1);
        return data;
    }

    public static FilteredClassifier buildClassifier(Instances data) throws Exception {
        // filter
        Remove rm = new Remove();
        rm.setAttributeIndices("1");  // remove 1st attribute
        // classifier
        J48 j48 = new J48();
        j48.setUnpruned(true);        // using an unpruned J48
        // meta-classifier
        FilteredClassifier fc = new FilteredClassifier();
        fc.setFilter(rm);
        fc.setClassifier(j48);
        // train and make predictions
        fc.buildClassifier(data);

        weka.core.SerializationHelper.write("weka.model", fc);

        return fc;
    }

    public static FilteredClassifier loadClassifier(AssetManager assetManager, String modelPath) throws Exception {
        Log.i(TAG, "loading model ...");
        return loadClassifier(assetManager.open(modelPath));
    }

    public static FilteredClassifier loadClassifier(InputStream fileInputStream) throws Exception {
        FilteredClassifier filteredClassifier = null;

            filteredClassifier = (FilteredClassifier)
                    weka.core.SerializationHelper.read(fileInputStream);
            Log.w(TAG, filteredClassifier.getBatchSize());

        return filteredClassifier;
    }

    public static FilteredClassifier loadClassifier(String filePath) throws Exception {
            FileInputStream fileInputStream = new FileInputStream(MODEL_FILE_PATH);
            return (FilteredClassifier)SerializationHelper.read(fileInputStream);
    }
}
