package fu.hao.cosmos_xposed.ml;

import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;

import cc.mallet.classify.Classifier;

/**
 * Description:
 *
 * @author Hao Fu(haofu AT ucdavis.edu)
 * @since 2/28/2017
 */
public class MalletUtils {
    public static final String MODEL_FILE_PATH = Environment.getExternalStorageDirectory().getAbsolutePath()
            + "/COSMOS/trained.model";

    public static Classifier loadClassifier() throws IOException, ClassNotFoundException {
        return loadClassifier(new File(MODEL_FILE_PATH));
    }

    public static Classifier loadClassifier(File serializedFile)
            throws  IOException, ClassNotFoundException {

        // The standard way to save classifiers and Mallet data
        //  for repeated use is through Java serialization.
        // Here we load a serialized classifier from a file.

        Classifier classifier;

        ObjectInputStream ois =
                new ObjectInputStream (new FileInputStream(serializedFile));
        classifier = (Classifier) ois.readObject();
        ois.close();

        return classifier;
    }
}
