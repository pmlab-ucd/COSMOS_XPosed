package fu.hao.cosmos_xposed.ml;

import android.content.ContentResolver;
import android.util.Log;

import fu.hao.cosmos_xposed.utils.MyContentProvider;
import weka.attributeSelection.*;
import weka.classifiers.Classifier;
import weka.classifiers.UpdateableClassifier;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.bayes.NaiveBayesMultinomial;
import weka.classifiers.bayes.NaiveBayesMultinomialUpdateable;
import weka.classifiers.functions.SGD;
import weka.classifiers.functions.SMO;
import weka.classifiers.lazy.IBk;
import weka.classifiers.lazy.KStar;
import weka.classifiers.lazy.LWL;
import weka.classifiers.meta.AttributeSelectedClassifier;
import weka.classifiers.meta.FilteredClassifier;
import weka.classifiers.trees.HoeffdingTree;
import weka.classifiers.trees.J48;
import weka.classifiers.trees.RandomForest;
import weka.core.*;
import weka.core.converters.ArffLoader;
import weka.core.converters.ArffSaver;
import weka.core.converters.ConverterUtils.DataSource;
import weka.core.pmml.Array;
import weka.core.stemmers.SnowballStemmer;
import weka.core.stopwords.WordsFromFile;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;
import weka.filters.unsupervised.attribute.StringToWordVector;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

import weka.classifiers.Evaluation;

import javax.xml.parsers.FactoryConfigurationError;

/**
 * Description:
 *
 * @author Hao Fu(haofu@ucdavis.edu)
 * @since 3/1/2017
 */
public class WekaUtils {
    private static String TAG = WekaUtils.class.getName();

    private static Classifier wekaModel;
    private static StringToWordVector stringToWordVector;

    public static List<String> LABELS = new ArrayList<>();

    static {
        // FIXME
        LABELS.add("T");
        LABELS.add("D");
        LABELS.add("F");
    }

    public static void init(ContentResolver contentResolver) throws Exception {
        if (stringToWordVector == null) {
            InputStream inputStream = contentResolver.openInputStream(MyContentProvider.STR2VEC_CONTENT_URI);
            WekaUtils.setStringToWordVector(WekaUtils.loadStr2WordVec(inputStream));
        }

        if (wekaModel == null) {
            InputStream inputStream = contentResolver.openInputStream(MyContentProvider.MODEL_CONTENT_URI);
            WekaUtils.setWekaModel(WekaUtils.loadClassifier(inputStream));
        }
    }

    public static Instances labelledDocs2Instances(List<fu.hao.cosmos_xposed.ml.LabelledDoc> docs, List<String> labels) {
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

        for (fu.hao.cosmos_xposed.ml.LabelledDoc labelledDoc : docs) {
            instance = new DenseInstance(2);
            instance.setValue((Attribute) atts.get(0), labelledDoc.getDoc());
            instance.setValue((Attribute) atts.get(1), labelledDoc.getLabel());
            data.add(instance);
        }
        data.setClassIndex(data.numAttributes() - 1);

        return data;
    }

    /**
     * Textual Docs to numerical instances
     *
     * @param instances
     * @param stringToWordVector
     * @return
     */
    public static Instances nominal2Numerical(Instances instances, StringToWordVector
            stringToWordVector) throws Exception {
        return Filter.useFilter(instances, stringToWordVector);
    }

    public static void setStringToWordVector(StringToWordVector stringToWordVector) {
        WekaUtils.stringToWordVector = stringToWordVector;
    }

    public static StringToWordVector getStringToWordVector() {
        return stringToWordVector;
    }

    public static void setWekaModel(Classifier classifier) {
        WekaUtils.wekaModel = classifier;
    }

    public static Classifier getWekaModel() throws Exception {
        return wekaModel;
    }

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

    public static Instances loadArff(String filePath) throws Exception {
        DataSource source = new DataSource(filePath);
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

        //RandomForest classifier = new RandomForest();
        HoeffdingTree classifier = new HoeffdingTree();
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


    private static AttributeSelection getAttributeSelector(
            Instances trainingData, int number) throws Exception {
        AttributeSelection selector = new AttributeSelection();
        InfoGainAttributeEval evaluator = new InfoGainAttributeEval();
        Ranker ranker = new Ranker();
        ranker.setNumToSelect(Math.min(number, trainingData.numAttributes() - 1));
        selector.setEvaluator(evaluator);
        selector.setSearch(ranker);
        selector.SelectAttributes(trainingData);
        return selector;
    }


    public static Classifier buildClassifier(Instances data, String modelName,
                                             boolean updateable) throws Exception {


        // classifier
        // J48 j48 = new J48();
        // j48.setUnpruned(true);        // using an unpruned J48

        //RandomForest classifier = new RandomForest();
        //NaiveBayes classifier = new NaiveBayes();
        //HoeffdingTree classifier = new HoeffdingTree();
        Classifier classifier;
        if (updateable) {
            SGD sgd = new SGD();
            //sgd.setLossFunction(new SelectedTag(SGD.LOGLOSS, SGD.TAGS_SELECTION));
            //sgd.setLossFunction(new SelectedTag(SGD.HINGE, SGD.TAGS_SELECTION));
            classifier = sgd; //new KStar(); //SGD(); //IBk(); //; //NaiveBayesMultinomial(); //LWL();//;//; //SGD(); //HoeffdingTree();
            //classifier = new HoeffdingTree();//NaiveBayesMultinomialUpdateable();
        } else {
            classifier = new SMO();
        }


        // System.err.println("Parameters");
        //for (int i = 0; i < classifier.getOptions().length; i++) {
        //  System.err.println(classifier.getOptions()[i]);
        //}
        // train and make predictions
        classifier.buildClassifier(data);

        weka.core.SerializationHelper.write(modelName + ".model", classifier);

        return classifier;
    }

    public static StringToWordVector getWordFilter(Instances input, boolean useIdf) throws Exception {
        StringToWordVector filter = new StringToWordVector();
        filter.setInputFormat(input);
        filter.setWordsToKeep(10000);
        if (useIdf) {
            filter.setIDFTransform(true);
        }
        //filter.setTFTransform(true);
        //filter.setLowerCaseTokens(true);
        //filter.setOutputWordCounts(false);

        //WordsFromFile stopwords = new WordsFromFile();
        //stopwords.setStopwords(new File("data/stopwords.txt"));
        //filter.setStopwordsHandler(stopwords);
        //SnowballStemmer stemmer = new SnowballStemmer();
        //filter.setStemmer(stemmer);

        return filter;
    }

    /**
     * Method: docs2Instances
     * Description:
     *
     * @param docs   Documents
     * @param labels Pre-defined labels
     * @return weka.core.Instances
     * @author Hao Fu(haofu AT ucdavis.edu)
     * @since 3/5/2017 2:24 PM
     */
    public static Instances docs2Instances(List<LabelledDoc> docs, List<String> labels) throws FileNotFoundException {
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
        data.setClassIndex(1);

        return data;
    }


    public static Instance doc2Instance(LabelledDoc doc, List<String> labels) throws Exception {
        ArrayList<LabelledDoc> docs = new ArrayList<>();
        docs.add(doc);

        return docs2Instances(docs, labels).get(0);
    }

    public static Instance genInstanceForUpdateable(LabelledDoc doc, List<String> labels, StringToWordVector stringToWordVector) throws Exception {
        ArrayList<LabelledDoc> docs = new ArrayList<>();
        docs.add(doc);
        return Filter.useFilter(docs2Instances(docs, labels), stringToWordVector).get(0);

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

    public static String predict(String doc, StringToWordVector stringToWordVector, Classifier classifier, Attribute classAttribute)
            throws Exception {
        List<String> docs = new ArrayList<>();
        docs.add(doc);
        return predict(docs, stringToWordVector, classifier, classAttribute).get(0);
    }

    public static List<String> predict(List<String> docs, StringToWordVector stringToWordVector,
                                       Classifier classifier, Attribute classAttribute) throws Exception {
        Instances unlabelledInstances = docs2Instances(docs);
        unlabelledInstances = Filter.useFilter(unlabelledInstances, stringToWordVector);
        List<String> results = new ArrayList<>();
        for (Instance instance : unlabelledInstances) {
            Double clsLabel = classifier.classifyInstance(instance);

            if (classAttribute != null && classAttribute.numValues() > 0) {
                results.add(classAttribute.value(clsLabel.intValue()));
                System.out.println("Predicted: " + classAttribute.value(clsLabel.intValue()) + ", " + clsLabel);
            } else {
                results.add(clsLabel.toString());
                System.out.println("Predicted: " + clsLabel);
            }

            //get the predicted probabilities
            double[] prediction = classifier.distributionForInstance(instance);

            //output predictions
            for (int i = 0; i < prediction.length; i++) {
                Log.w(TAG, "Probability of class " + i +
                        " : " + Double.toString(prediction[i]));
            }

        }

        return results;
    }

    public static Instances createArff(Instances data, String filePath) throws Exception {
        //System.out.println("--------------------------------------------------");
        System.out.println("Create ARFF file:" + filePath);
        //System.out.println(data.toString());
        //System.out.println("--------------------------------------------------");
        //System.out.println(data.numAttributes());
        /*
        PrintWriter out = new PrintWriter("data.arff");
        out.print(data.toString());
        out.close();*/
        ArffSaver saver = new ArffSaver();
        saver.setInstances(data);
        saver.setFile(new File(filePath));
        //saver.setDestination(new File(filePath));   // **not** necessary in 3.5.4 and later
        saver.writeBatch();
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
    public static double crossValidation(Instances data, Classifier classifier, int fold) throws Exception {
        Evaluation eval = new Evaluation(data);
        System.out.println(eval.getHeader().numAttributes());
        eval.crossValidateModel(classifier, data, fold, new Random(10));
        System.out.println(eval.toSummaryString("\nResults\n======\n", false));
        System.out.println(eval.toClassDetailsString());
        System.out.println(eval.toMatrixString());
        return eval.weightedFMeasure();
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

    public static Classifier loadClassifier(InputStream fileInputStream) throws Exception {
        return (Classifier)
                weka.core.SerializationHelper.read(fileInputStream);
    }

    public static Classifier loadClassifier(File file) throws Exception {
        FileInputStream fileInputStream = new FileInputStream(file);
        return (Classifier) SerializationHelper.read(fileInputStream);
    }

    public static StringToWordVector loadStr2WordVec(File file) throws Exception {
        FileInputStream fileInputStream = new FileInputStream(file);
        return loadStr2WordVec(fileInputStream);
    }

    public static StringToWordVector loadStr2WordVec(InputStream fileInputStream) throws Exception {
        return (StringToWordVector) SerializationHelper.read(fileInputStream);
    }


    public static Instances readArff(String filePath, int classIndex) throws Exception {
        DataSource source = new DataSource(filePath);
        Instances data = source.getDataSet();
        data.setClassIndex(classIndex);
        // setting class attribute if the data format does not provide this information
        // For example, the XRFF format saves the class attribute information as well
        //if (data.classIndex() == -1) {
        //data.setClassIndex(data.numAttributes() - 1);
        //}

        return data;
    }




}


