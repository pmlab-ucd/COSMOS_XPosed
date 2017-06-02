package fu.hao.cosmos_xposed.hook;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AndroidAppHelper;
import android.app.Application;
import android.app.Instrumentation;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.InstrumentationInfo;
import android.database.Cursor;
import android.graphics.Color;
import android.location.LocationManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import org.w3c.dom.NodeList;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import fu.hao.cosmos_xposed.MainApplication;
import fu.hao.cosmos_xposed.MainService;
import fu.hao.cosmos_xposed.accessibility.LayoutData;
import fu.hao.cosmos_xposed.accessibility.UIAccessibilityService;
import fu.hao.cosmos_xposed.ml.WekaUtils;
import fu.hao.cosmos_xposed.utils.MyContentProvider;
import fu.hao.cosmos_xposed.utils.XMLParser;
import weka.classifiers.evaluation.output.prediction.Null;
import weka.classifiers.trees.j48.ClassifierSplitModel;

import static de.robv.android.xposed.XposedHelpers.findAndHookConstructor;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static fu.hao.cosmos_xposed.hook.Utils.readMethods;
import static fu.hao.cosmos_xposed.utils.MyContentProvider.INSTANCE_INDEX;
import static fu.hao.cosmos_xposed.utils.MyContentProvider.INSTANCE_LABEL;
import static fu.hao.cosmos_xposed.utils.MyContentProvider.LAYOUT_CONTENT_URI;
import static fu.hao.cosmos_xposed.utils.MyContentProvider.NEW_INSTANCE_CONTENT_URI;
import static fu.hao.cosmos_xposed.utils.MyContentProvider.PREDICTIONS_DATA;
import static fu.hao.cosmos_xposed.utils.MyContentProvider.PREDICTIONS_INDEX;
import static fu.hao.cosmos_xposed.utils.MyContentProvider.PREDICTION_RES_URI;

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

    private void hookEvent(final XC_LoadPackage.LoadPackageParam lpparam) {
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

                    Activity activity = ActivityHook.getCurrentActivity();
                    /*if (activity != null) {
                        try {
                            final ViewGroup viewGroup = (ViewGroup) ((ViewGroup) activity
                                    .findViewById(android.R.id.content)).getChildAt(0);
                            ActivityHook.checkView(viewGroup);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        Log.w(TAG, "Current Activity is missing!");
                    }*/
                }
            };
            findAndHookMethod(xMethod.getDeclaredClass(), xMethod.getMethodName(), argus);
        }
    }

    public String getViewHierWithUIAutomator(final XC_LoadPackage.LoadPackageParam lpparam,
                                             Application application) throws Exception {
        String texts = "";
        if (UIAccessibilityService.toXml) {
            ContentResolver cr = application.getContentResolver();
            Cursor cursor = cr.query(LAYOUT_CONTENT_URI, null, null, null, null);
            if (cursor == null) {
                Log.e(TAG, "Cannot get the cursor!");
                return "";
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
                return "";
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
                return "";
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
                return "";
            }

            StringBuilder stringBuilder = new StringBuilder();
            for (String text : layoutData.getTexts()) {
                stringBuilder.append(text + ";");
            }
            texts = stringBuilder.toString();
        }

        return texts;
    }

    public String getTexts(Activity activity) {
        StringBuilder stringBuilder = new StringBuilder();
        for (String text : ActivityHook.toLayoutXML(activity).getTexts()) {
            stringBuilder.append(text + ";");
        }
        return stringBuilder.toString();
    }


    /**
     * 包加载时候的回调, which is the entry method of the hook system
     */
    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        Log.v(TAG, "Package checking: " + lpparam.packageName);
        // 将包名不是 edu.ucdavis.test的应用剔除掉, for debugging
        if (!(lpparam.packageName.contains("fu.hao") || lpparam.packageName.contains("jp.snowlife01")
                || lpparam.packageName.contains("net.sourceforge") || lpparam.packageName.contains("com.jessdev")
                || lpparam.packageName.contains("com.yahoo"))) {
            return;
        }

        Log.w(TAG, "Package checking: " + lpparam.packageName);

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

        //findAndHookConstructor(Thread.class, Runnable.class, new ThreadHook(true));
        //findAndHookConstructor(Thread.class, new ThreadHook(false));
        //findAndHookMethod("fu.hao.testthread.MainActivity$ConnectionThread2", lpparam.classLoader, "run", new ThreadHook(false));

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
                    Application application = Utils.getApplication();
                    Activity activity = ActivityHook.getCurrentActivity();
                    if (activity != null) {
                        final ViewGroup viewGroup = (ViewGroup) ((ViewGroup) activity
                                .findViewById(android.R.id.content)).getChildAt(0);
                        for (int i = 0; i < viewGroup.getChildCount(); i++) {
                            Log.w(TAG, viewGroup.getChildAt(i).getClass().getName());
                        }
                    } else {
                        Log.w(TAG, "Current Activity is missing!");
                    }

                    for (StackTraceElement stackTraceElement : Thread.currentThread().getStackTrace()) {
                        Log.w(TAG, "stackTraceElem: " + stackTraceElement.getMethodName() + ", "
                                + stackTraceElement.getClassName() + ", " + stackTraceElement.getFileName()
                                + ", " + stackTraceElement.getLineNumber());

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

                    //ActivityManager am = (ActivityManager) application.getSystemService(Context.ACTIVITY_SERVICE);
                    //ComponentName cn = am.getRunningTasks(1).get(0).topActivity;
                    String texts = getTexts(ActivityHook.getCurrentActivity());

                    Log.w(TAG, "Texts: " + texts);
                    if (texts.length() < 1) {
                        return;
                    }

                    //MainApplication.getFileExternally(WekaUtils.MODEL_FILE_PATH));
                    Intent intent = new Intent();
                    intent.setComponent((new ComponentName("fu.hao.cosmos_xposed", "fu.hao.cosmos_xposed.MainService")));
                    String index = String.valueOf((int)(Math.random() * 500)); // String.valueOf(texts.hashCode()); //
                    intent.putExtra("index", index);
                    intent.putExtra("texts", texts);
                    application.startService(intent);
                    Log.w(TAG, "Staring MainService");

                    String res = Utils.checkResults(PREDICTION_RES_URI, index, PREDICTIONS_INDEX, PREDICTIONS_DATA, 10, 10);
                    Log.w(TAG, "Res: " + res);


                    if (res.equals("1")) {
                        param.setResult(null);
                        Log.w(TAG, "Blocked!");
                    } else {
                        final String instanceTexts = texts;
                        final String fIndex = index;
                        ActivityHook.getCurrentActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // FIXME Event
                                ActivityHook.show(ActivityHook.getCurrentActivity(), sensitiveView,
                                lpparam.packageName, null, instanceTexts, fIndex);
                            }
                        });
                        res = Utils.checkResults(NEW_INSTANCE_CONTENT_URI, fIndex, INSTANCE_INDEX, INSTANCE_LABEL, 100, 500);
                        Log.w(TAG, "User Decision: " + res);
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


}
