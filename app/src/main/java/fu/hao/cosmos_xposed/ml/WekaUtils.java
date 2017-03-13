package fu.hao.cosmos_xposed.ml;

import android.content.res.AssetManager;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;
import weka.classifiers.Classifier;
import weka.classifiers.meta.FilteredClassifier;
import weka.classifiers.trees.J48;
import weka.classifiers.trees.RandomForest;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SerializationHelper;
import weka.core.converters.ArffLoader;
import weka.core.converters.ArffSaver;
import weka.core.converters.ConverterUtils.DataSource;
import weka.core.stemmers.SnowballStemmer;
import weka.core.stopwords.WordsFromFile;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;
import weka.filters.unsupervised.attribute.StringToWordVector;
import org.tartarus.snowball.ext.porterStemmer;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import weka.classifiers.Evaluation;
import java.util.Random;

/**
 * Description:
 *
 * @author Hao Fu(haofu@ucdavis.edu)
 * @since 3/1/2017
 */
public class WekaUtils {
    private static String TAG = WekaUtils.class.getName();

    private class LabelledDoc {
        private String label;
        private String doc = null;

        LabelledDoc(String label, String doc) {
            this.label = label;
            doc = doc.replace(",", "");
            this.doc = doc;
        }

        public String getLabel() {
            return label;
        }

        public String getDoc() {
            return doc;
        }
    }

    public static Instances loadArff() throws Exception {
        DataSource source = new DataSource("D:\\workspace\\COSPOS_MINING\\output\\gnd\\Test\\train_data.arff");
        Instances data = source.getDataSet();
        // setting class attribute if the data format does not provide this information
        // For example, the XRFF format saves the class attribute information as well
        if (data.classIndex() == -1)
            data.setClassIndex(data.numAttributes() - 1);
        return data;
    }

    public static Classifier getPureClassifier() {
        return new RandomForest();
    }

    public static FilteredClassifier buildClassifier(Instances data, boolean rmFirst) throws Exception {
        FilteredClassifier fc = new FilteredClassifier();
        if (rmFirst) {
            // filter
            Remove rm = new Remove();
            rm.setAttributeIndices("1");  // remove 1st attribute
            fc.setFilter(rm);
        }
        // classifier
        // J48 j48 = new J48();
        // j48.setUnpruned(true);        // using an unpruned J48

        RandomForest classifier = new RandomForest();
        System.err.println("Parameters");
        for (int i = 0; i < classifier.getOptions().length; i++) {
            System.err.println(classifier.getOptions()[i]);
        }
        // meta-classifier
        fc.setClassifier(classifier); //j48);
        // train and make predictions
        fc.buildClassifier(data);

        weka.core.SerializationHelper.write("weka.model", fc);

        return fc;
    }

    public static StringToWordVector getWordFilter(Instances input, boolean useIdf) throws Exception {
        StringToWordVector filter = new StringToWordVector();
        filter.setInputFormat(input);
        //filter.setWordsToKeep(1000000);
        if (useIdf) {
            filter.setIDFTransform(true);
        }
        //filter.setTFTransform(true);
        filter.setLowerCaseTokens(true);
        filter.setOutputWordCounts(true);

        //WordsFromFile stopwords = new WordsFromFile();
        //stopwords.setStopwords(new File("data/stopwords.txt"));
        //filter.setStopwordsHandler(stopwords);
        SnowballStemmer stemmer = new SnowballStemmer();
        porterStemmer porterStemmer = new porterStemmer();
        filter.setStemmer(stemmer);

        weka.core.SerializationHelper.write("weka.filter", filter);
        return filter;
    }

    /**
     * Creates an ARFF file represented by Instances
     *
     * @param docs list of docs return Instances which includes the list of
     *             docs
     */
    public static Instances createArff(List<LabelledDoc> docs, List<String> labels) throws FileNotFoundException {
        ArrayList<Attribute> atts = new ArrayList<>();
        ArrayList<String> classVal = new ArrayList<>();
        for (String label : labels) {
            classVal.add(label);
        }


        Attribute attribute1 = new Attribute("text", (ArrayList<String>) null);
        Attribute attribute2 = new Attribute("text_label", classVal); // Do not use common words for this attribute

        atts.add(attribute1);
        atts.add(attribute2);

        //build training data
        Instances data = new Instances("docs", atts, 1);
        DenseInstance instance;

        for (LabelledDoc labelledDoc : docs) {
            instance = new DenseInstance(2);
            instance.setValue((Attribute) atts.get(0), labelledDoc.getDoc());
            instance.setValue((Attribute) atts.get(1), labelledDoc.getLabel());
            data.add(instance);
        }
        data.setClassIndex(data.numAttributes() - 1);

        System.out.println("--------------------------------------------------");
        System.out.println("Create ARFF file:");
        System.out.println(data.toString());
        System.out.println("--------------------------------------------------");

        PrintWriter out = new PrintWriter("data.arff");
        out.print(data.toString());
        out.close();
        return data;
    }

    /**
     * Method: crossValidation
     * Description: Note that classifier should not be pre-trained
     *
     * @param data
     * @param classifier
     * @return void
     * @throw
     * @author Hao Fu(haofu AT ucdavis.edu)
     * @since 3/4/2017 4:13 PM
     */
    public static void crossValidation(Instances data, Classifier classifier) throws Exception {
        Evaluation eval = new Evaluation(data);
        System.out.println(eval.getHeader().numAttributes());
        System.out.println(eval.numInstances());
        eval.crossValidateModel(classifier, data, 10, new Random(10));
        System.out.println(eval.toSummaryString("\nResults\n======\n", false));
        System.out.println(eval.toClassDetailsString());
        System.out.println(eval.toMatrixString());
    }

    public static String fixEncoding(String latin1) {
        try {
            byte[] bytes = latin1.getBytes("ISO-8859-1");
            if (!validUTF8(bytes))
                return latin1;
            return new String(bytes, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // Impossible, throw unchecked
            throw new IllegalStateException("No Latin1 or UTF-8: " + e.getMessage());
        }

    }

    public static boolean validUTF8(byte[] input) {
        int i = 0;
        // Check for BOM
        if (input.length >= 3 && (input[0] & 0xFF) == 0xEF
                && (input[1] & 0xFF) == 0xBB & (input[2] & 0xFF) == 0xBF) {
            i = 3;
        }

        int end;
        for (int j = input.length; i < j; ++i) {
            int octet = input[i];
            if ((octet & 0x80) == 0) {
                continue; // ASCII
            }

            // Check for UTF-8 leading byte
            if ((octet & 0xE0) == 0xC0) {
                end = i + 1;
            } else if ((octet & 0xF0) == 0xE0) {
                end = i + 2;
            } else if ((octet & 0xF8) == 0xF0) {
                end = i + 3;
            } else {
                // Java only supports BMP so 3 is max
                return false;
            }

            while (i < end) {
                i++;
                try {
                    octet = input[i];
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
                if ((octet & 0xC0) != 0x80) {
                    // Not a valid trailing byte
                    return false;
                }
            }
        }
        return true;
    }

    public static void save2Arff(Instances instances, String fileName) throws IOException {
        // Save instances to arff
        instances.renameAttribute(0, "class");
        for (int i = 1; i < instances.numAttributes(); i++) {
            String name = fixEncoding(instances.attribute(i).name());
            try {
                instances.renameAttribute(i, name);
            } catch (IllegalArgumentException e) {
                instances.renameAttribute(i, "_" + name);
            }
        }
        ArffSaver saver = new ArffSaver();
        saver.setInstances(instances);
        File dataFile = new File(fileName + ".arff");
        saver.setFile(dataFile);
        // saver.setDestination(dataFile);   // **not** necessary in 3.5.4 and later
        saver.writeBatch();
        for (Instance instance : instances) {
            instance.classAttribute();
            System.out.println(instance);
        }
    }

    public static Instances docs2Instances(List<String> docs) {
        ArrayList<Attribute> atts = new ArrayList<>();

        Attribute attribute1 = new Attribute("text", (ArrayList<String>) null);
        //Attribute attribute2 = new Attribute("text_label", classVal); // Do not use common words for this attribute

        atts.add(attribute1);
        //atts.add(attribute2);

        //build training data
        Instances data = new Instances("docs", atts, 1);
        DenseInstance instance;

        for (String doc : docs) {
            instance = new DenseInstance(2);
            instance.setValue((Attribute) atts.get(0), doc);
//            instance.setValue((Attribute)atts.get(1), "?");
            data.add(instance);
        }
        //data.setClassIndex(data.numAttributes() - 1);

        return data;
    }

    public static List<String> predict(List<String> docs, StringToWordVector stringToWordVector,
                                       FilteredClassifier classifier, Attribute classAttibute) throws Exception {
        Instances unlabelledInstances = docs2Instances(docs);
        System.out.println(unlabelledInstances);
        unlabelledInstances = Filter.useFilter(unlabelledInstances, stringToWordVector);
        List<String> results = new ArrayList<>();
        for (Instance instance : unlabelledInstances) {
            Double clsLabel = classifier.classifyInstance(instance);

            if (classAttibute != null && classAttibute.numValues() > 0) {
                results.add(classAttibute.value(clsLabel.intValue()));
                System.out.println("Predicted: " + classAttibute.value(clsLabel.intValue()));
            } else {
                results.add(clsLabel.toString());
                System.out.println("Predicted: " + clsLabel);
            }
        }

        return results;
    }

    public static FilteredClassifier loadClassifier(InputStream fileInputStream) throws Exception {
        FilteredClassifier filteredClassifier = null;

        filteredClassifier = (FilteredClassifier)
                weka.core.SerializationHelper.read(fileInputStream);

        return filteredClassifier;
    }

    public static FilteredClassifier loadClassifier(File file) throws Exception {
        FileInputStream fileInputStream = new FileInputStream(file);
        Log.i(TAG, "Trying to load " + file);
        return (FilteredClassifier) SerializationHelper.read(fileInputStream);
    }

    public static StringToWordVector loadStr2WordVec(File file) throws Exception {
        FileInputStream fileInputStream = new FileInputStream(file);
        return loadStr2WordVec(fileInputStream);
    }

    public static StringToWordVector loadStr2WordVec(InputStream fileInputStream) throws Exception {
        return (StringToWordVector) SerializationHelper.read(fileInputStream);
    }

}
