package fu.hao.cosmos_xposed.hook;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AndroidAppHelper;
import android.app.Application;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.location.LocationManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import org.w3c.dom.NodeList;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import fu.hao.cosmos_xposed.MainApplication;
import fu.hao.cosmos_xposed.accessibility.LayoutData;
import fu.hao.cosmos_xposed.accessibility.UIAccessibilityService;
import fu.hao.cosmos_xposed.ml.WekaUtils;
import fu.hao.cosmos_xposed.utils.MyContentProvider;
import fu.hao.cosmos_xposed.utils.XMLParser;
import weka.classifiers.evaluation.output.prediction.Null;
import weka.classifiers.trees.j48.ClassifierSplitModel;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static fu.hao.cosmos_xposed.utils.MyContentProvider.LAYOUT_CONTENT_URI;

/**
 * Description:
 *
 * @author Hao Fu(haofu AT ucdavis.edu)
 * @since 10/13/2016
 */
public class Main implements IXposedHookLoadPackage {
    private final String TAG = this.getClass().getName();

    private static Set<XMethod> PscoutXMethod;
    private static Set<XMethod> EVENT_XMETHODS;
    private View sensitiveView = null;
    private Member eventTriggered = null; // The most recent event.

    static {
        PscoutXMethod = new HashSet<>();
        EVENT_XMETHODS = new HashSet<>();
    }

    public static Set<XMethod> getPscoutXMethod() {
        return PscoutXMethod;
    }

    public static Set<XMethod> getEventXMethods() {
        return EVENT_XMETHODS;
    }

    public XMethod sootMethodStr2XMethod(String sootSignature) throws ClassNotFoundException {
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

    private Class typeName2Class(String typeName) throws ClassNotFoundException {
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

    private void readMethods(Set<String> sootDef, Set<XMethod> xMethods) {
        for (String sensSignature : sootDef) {
            try {
                XMethod xMethod = sootMethodStr2XMethod(sensSignature);
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

    /**
     * 包加载时候的回调, which is the entry method of the hook system
     */
    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        Log.w(TAG, "Package checking: " + lpparam.packageName);
        // 将包名不是 edu.ucdavis.test的应用剔除掉, for debugging
        //if (!lpparam.packageName.contains("fu.hao")) {
           // return;
        //}

        if (getPscoutXMethod().isEmpty()) {
            Log.w(TAG, "Try to read TARGET_METHODS...");
            readMethods(TargetMethods.TARGET_METHODS, PscoutXMethod);
            readMethods(TargetMethods.EVENT_METHODS, EVENT_XMETHODS);
        }

        XposedBridge.log("Loaded app: " + lpparam.packageName);
        Log.v(TAG, "Hooking " + lpparam.packageName);

        String self = Main.class.getPackage().getName();
        if (lpparam.packageName.equals(self)) {
            return;
        }

        Log.v(TAG, "Try to load target methods...");

        Class<?> instrumentation = XposedHelpers.findClass(
                "android.app.Instrumentation", lpparam.classLoader);

        Method method = instrumentation.getMethod("newActivity",
                ClassLoader.class, String.class, Intent.class);
        final ActivityHook iHook = new ActivityHook();
        XposedBridge.hookMethod(method, iHook);

        for (final XMethod xMethod : getEventXMethods()) {
            Log.w(TAG, "Loading " + xMethod.getMethodName() + " @ " + xMethod.getDeclaredClass());
            Object[] argus;
            if (xMethod.getParamTypes() == null || xMethod.getParamTypes().length < 1) {
                argus = new Object[1];
            } else {
                int argPosition = xMethod.getParamTypes().length;
                argus = new Object[argPosition + 1];
                for (int i = 0; i < xMethod.getParamTypes().length; i++) {
                    argus[i] = xMethod.getParamTypes()[i];
                }
            }

            argus[argus.length - 1] = new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    Log.w(TAG, "Start Hooking: " + param.method + " with " + param.thisObject +
                            " called by " + lpparam.packageName);
                    eventTriggered = param.method;

                    if (param.thisObject instanceof View) {
                        sensitiveView = (View) param.thisObject;
                    }
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    XposedBridge.log("劫持结束了~" + lpparam.packageName);
                    Log.w(TAG, "End hooking method " + param.method);
                }
            };
            findAndHookMethod(xMethod.getDeclaredClass(), xMethod.getMethodName(), argus);
        }

        for (final XMethod xMethod : getPscoutXMethod()) {
            Log.v(TAG, "Loading " + xMethod.getMethodName() + " @ " + xMethod.getDeclaredClass());
            if (xMethod.getParamTypes() == null) {
                continue;
            }

            for (Object paramType : xMethod.getParamTypes()) {
                Log.v(TAG, "paramType: " + paramType);
            }

            Object[] argus = new Object[xMethod.getParamTypes().length + 1];
            for (int i = 0; i < xMethod.getParamTypes().length; i++) {
                argus[i] = xMethod.getParamTypes()[i];
            }
            argus[xMethod.getParamTypes().length] = new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    XposedBridge.log("开始劫持了~");
                    Log.w(TAG, "Start Hooking: " + param.method + " with " + param.thisObject +
                            " called by " + lpparam.packageName);

                    /*if (!(param.getResult() instanceof Context)) {
                        Log.e(TAG, param.getResult().toString());
                        return;
                    }*/
                    //Context context = (Context) param.getResult();
                    Application application = AndroidAppHelper.currentApplication();
                    Activity activity = ActivityHook.getCurrentActivity();
                    final ViewGroup viewGroup = (ViewGroup) ((ViewGroup) activity
                            .findViewById(android.R.id.content)).getChildAt(0);
                    for(int i = 0; i < viewGroup.getChildCount(); i++) {
                        Log.w(TAG, viewGroup.getChildAt(i).getClass().getName());
                    }

                    //ActivityManager am = (ActivityManager) application.getSystemService(Context.ACTIVITY_SERVICE);
                    //ComponentName cn = am.getRunningTasks(1).get(0).topActivity;
                    String texts = "";
                    if (UIAccessibilityService.toXml) {
                        ContentResolver cr = application.getContentResolver();
                        Cursor cursor = cr.query(LAYOUT_CONTENT_URI, null, null, null, null);
                        if (cursor == null) {
                            Log.e(TAG, "Cannot get the cursor!");
                            return;
                        }
                        String xmlData = ""; //cursor.getColumnIndex(MyContentProvider.name));

                        if (!cursor.moveToFirst()) {
                            Log.e(TAG, xmlData + " no content yet!");
                        } else {
                            do {
                                xmlData = xmlData + cursor.getString(cursor.getColumnIndex(MyContentProvider.LAYOUT_DATA));
                            } while (cursor.moveToNext());
                        }

                        Log.w(TAG, "XMLData: " + xmlData);
                        //param.args[0] = "10086";
                        // File layoutFile = MainApplication.getFileExternally("layout.xml");

                        if (xmlData.length() < 1) {
                            return;
                        }
                        NodeList nodeList = XMLParser.getNodeList(xmlData);//layoutFile);
                        boolean samePkg = false;
                        for (String pkgClass : XMLParser.getPkg(nodeList)) {
                            if (pkgClass.equals(lpparam.packageName)) {
                                samePkg = true;
                                break;
                            }
                        }
                        if (!samePkg) {
                            return;
                        }

                        StringBuilder stringBuilder = new StringBuilder();
                        for (String text : XMLParser.getTexts(nodeList)) {
                            stringBuilder.append(text);
                        }

                        texts = stringBuilder.toString();
                    } else {
                        InputStream inputStream = application.getContentResolver().openInputStream(
                                MyContentProvider.LAYOUT_DATA_CONTENT_URI);
                        ObjectInputStream is = new ObjectInputStream(inputStream);
                        LayoutData layoutData = (LayoutData) is.readObject();
                        is.close();

                        if (!layoutData.getPkg().equals(lpparam.packageName)) {
                            return;
                        }

                        StringBuilder stringBuilder = new StringBuilder();
                        for (String text : layoutData.getTexts()) {
                            stringBuilder.append(text + ";");
                        }
                        texts = stringBuilder.toString();
                    }

                    for (StackTraceElement stackTraceElement : Thread.currentThread().getStackTrace()) {
                        Log.v(TAG, "stackTraceElem: " + stackTraceElement.getMethodName() + ", "
                                + stackTraceElement.getClassName());
                        // Locate entry point
                        if (eventTriggered != null && stackTraceElement.getMethodName().contains(eventTriggered.getName())) {
                            Log.w(TAG, "" + sensitiveView.getResources().getResourceName(sensitiveView.getId()));
                        }
                        //load the class to get the information
                        /*
                        try {
                            Class<?> c = Class.forName(stackTraceElement.getClassName());
                            Method[] methods = c.getMethods();
                            Class<?>[] parameters = methods[0].getParameterTypes();

                            for (Class<?> paramter : parameters) {
                                Log.w(TAG, paramter.getName());
                            }
                        } catch (Exception e) {
                            Log.w(TAG, e.getMessage());
                        }*/
                    }

                    Log.w(TAG, "Texts: " + texts);
                    if (texts.length() < 1) {
                        return;
                    }

                    //MainApplication.getFileExternally(WekaUtils.MODEL_FILE_PATH));
                    List<String> unlabelled = new ArrayList<>();
                    unlabelled.add(texts);

                    WekaUtils.init(application.getContentResolver());
                    // MainApplication.getFileExternally(WekaUtils.STRING_VEC_FILTER_PATH));
                    List<String> res = WekaUtils.predict(unlabelled, WekaUtils.getStringToWordVector(),
                            WekaUtils.getWekaModel(), null);
                    for (String subres : res) {
                        Log.w(TAG, "Predicted as " + subres);
                    }
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    XposedBridge.log("劫持结束了~" + lpparam.packageName);
                    XposedBridge.log("参数1 = " + param.args[0]);

                    Log.w(TAG, "End hooking method " + param.method);
                }
            };

            findAndHookMethod(xMethod.getDeclaredClass(), xMethod.getMethodName(), argus);
        }
    }

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


}
