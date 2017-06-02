package fu.hao.cosmos_xposed.hook;

import android.app.Activity;
import android.app.AndroidAppHelper;
import android.app.Application;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;

import fu.hao.cosmos_xposed.MainApplication;
import fu.hao.cosmos_xposed.utils.MyContentProvider;
import weka.filters.unsupervised.attribute.StringToWordVector;

import static fu.hao.cosmos_xposed.utils.MyContentProvider.PREDICTION_RES_URI;

/**
 * Created by majestyhao on 2017/6/1.
 */

public class Utils {
    private static String TAG = Utils.class.getName();
    private static Application application = null;

    public Class<?> getArrayClass(Class<?> componentType) throws ClassNotFoundException {
        ClassLoader classLoader = componentType.getClassLoader();
        String name;
        if (componentType.isArray()) {
            // just add a leading "["
            name = "[" + componentType.getName();
        } else if (componentType == boolean.class) {
            name = "[Z";
        } else if (componentType == byte.class) {
            name = "[B";
        } else if (componentType == char.class) {
            name = "[C";
        } else if (componentType == double.class) {
            name = "[D";
        } else if (componentType == float.class) {
            name = "[F";
        } else if (componentType == int.class) {
            name = "[I";
        } else if (componentType == long.class) {
            name = "[J";
        } else if (componentType == short.class) {
            name = "[S";
        } else {
            // must be an object non-array class
            name = "[L" + componentType.getName() + ";";
        }
        return classLoader != null ? classLoader.loadClass(name) : Class.forName(name);
    }

    public static XMethod sootMethodStr2XMethod(String sootSignature) throws ClassNotFoundException {
        XMethod xMethod = new XMethod();
        sootSignature = sootSignature.replace("<", "");
        sootSignature = sootSignature.replace(">", "");
        sootSignature = sootSignature.replace(")", "");
        String[] splice = sootSignature.split(": ");
        Class declaredClass = typeName2Class(splice[0]);
        xMethod.setDeclaredClass(declaredClass);

        splice = splice[1].split(" ");

        splice = splice[1].split("\\(");

        String methodName = splice[0];
        xMethod.setMethodName(methodName);

        if (splice.length > 1) {
            splice = splice[1].split(",");
            Class[] paramTypes = new Class[splice.length];
            for (int i = 0; i < splice.length; i++) {
                paramTypes[i] = typeName2Class(splice[i]);
            }
            xMethod.setParamTypes(paramTypes);
        }

        return xMethod;
    }

    private static Class typeName2Class(String typeName) throws ClassNotFoundException {
        switch (typeName) {
            case "int":
                return Integer.TYPE;
            case "int[]":
                int[] integers = new int[1];
                return integers.getClass();
            case "short":
                return Short.TYPE;
            case "short[]":
                short[] shorts = new short[1];
                return shorts.getClass();
            case "long":
                return Long.TYPE;
            case "long[]":
                long[] longs = new long[1];
                return longs.getClass();
            case "float":
                return Float.TYPE;
            case "float[]":
                float[] floats = new float[1];
                return floats.getClass();
            case "double":
                return Double.TYPE;
            case "double[]":
                double[] doubles = new double[1];
                return doubles.getClass();
            case "char":
                return Character.TYPE;
            case "char[]":
                char[] chars = new char[1];
                return chars.getClass();
            case "byte":
                return Byte.TYPE;
            case "byte[]":
                byte[] bytes = new byte[1];
                return bytes.getClass();
            case "boolean":
                return Boolean.TYPE;
            case "boolean[]":
                boolean[] booleens = new boolean[1];
                return booleens.getClass();
            default:
                if (typeName.contains("[]")) {
                    typeName = typeName.replace("[]", "");
                    return Class.forName("[L" + typeName + ";");
                } else {
                    return Class.forName(typeName);
                }
        }
    }

    @Deprecated
    public static Activity getActivity() throws Exception {
        Class activityThreadClass = Class.forName("android.app.ActivityThread");
        Object activityThread = activityThreadClass.getMethod("currentActivityThread").invoke(null);
        Field activitiesField = activityThreadClass.getDeclaredField("mActivities");
        activitiesField.setAccessible(true);
        Map activities = (Map) activitiesField.get(activityThread);
        for(Object activityRecord:activities.values()){
            Class activityRecordClass = activityRecord.getClass();
            Field pausedField = activityRecordClass.getDeclaredField("paused");
            pausedField.setAccessible(true);
            if(!pausedField.getBoolean(activityRecord)) {
                Field activityField = activityRecordClass.getDeclaredField("activity");
                activityField.setAccessible(true);
                Activity activity = (Activity) activityField.get(activityRecord);
                return activity;
            }
        }

        return null;
    }

    @Deprecated
    private void readSensDefFile() {
        Log.w(TAG, "Read SENS_DEF_File from " + MainApplication.SENS_DEF_FILE);
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(
                    new InputStreamReader(
                            new FileInputStream(
                                    MainApplication.SENS_DEF_FILE_PATH)));

            // do reading, usually loop until end of file reading
            String mLine;
            while ((mLine = reader.readLine()) != null) {
                //process line
                if (mLine.startsWith("<")) {
                    //PscoutMethod.add(mLine);
                }
            }
        } catch (IOException e) {
            //log the exception
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    //log the exception
                }
            }
        }

    }

    public static void readMethods(Set<String> sootDef, Set<XMethod> xMethods) {
        for (String sensSignature : sootDef) {
            try {
                XMethod xMethod = Utils.sootMethodStr2XMethod(sensSignature);
                if (xMethod != null) {
                    if (xMethod.getParamTypes() != null) {
                        for (Object paramType : xMethod.getParamTypes()) {
                            Log.v(TAG, "paramType" + paramType);
                        }
                    }
                    xMethods.add(xMethod);
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }


    public static Application getApplication() {
        if (application == null) {
            // https://stackoverflow.com/questions/28059264/how-to-get-context-through-hooking-in-android
            application = AndroidAppHelper.currentApplication();
        }
        return application;
    }

    public static String checkResults(Uri uri, String index, String dbIndex, String dbData, int times, int interval) throws InterruptedException {
        String res = "";
        ContentResolver cr = application.getContentResolver();
        Cursor cursor = null;

        String rindex = "";
        for (int i = 0; i < times; i++) {
            if (!rindex.isEmpty()) {
                if (rindex.equals(index)) {
                    break;
                } else {
                    rindex = "";
                    res = "";
                }
            }
            cursor = cr.query(uri, null, null, null, null);
            if (cursor == null) {
                Log.e(TAG, "Cannot get the cursor!");
                return res;
            }
            if (!cursor.moveToFirst()) {
                Log.e(TAG, " no content yet!");
            } else {
                try {
                    do {
                        rindex = rindex + cursor.getString(cursor.getColumnIndex(dbIndex));
                        res = res + cursor.getString(cursor.getColumnIndex(dbData));
                    } while (cursor.moveToNext());
                } catch (IllegalStateException e) {
                    Log.e(TAG, e.getMessage());
                }
            }

            Thread.sleep(interval);
        }

        return res;
    }
}
