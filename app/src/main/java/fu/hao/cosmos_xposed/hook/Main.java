package fu.hao.cosmos_xposed.hook;

import android.app.Application;
import android.app.PendingIntent;
import android.os.Build;
import android.os.Environment;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import fu.hao.cosmos_xposed.MainApplication;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

/**
 * Description:
 *
 * @author Hao Fu(haofu AT ucdavis.edu)
 * @since 10/13/2016
 */
public class Main implements IXposedHookLoadPackage {
    private final String TAG = this.getClass().getName();

    private static List<String> PscoutMethod;
    private static Set<XMethod> PscoutXMethod;

    static {
        PscoutMethod = new ArrayList<>();
        PscoutXMethod = new HashSet<>();
    }

    public static Set<XMethod> getPscoutXMethod() {
        return PscoutXMethod;
    }

    public XMethod signature2Class(String sootSignature) throws ClassNotFoundException {
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
            default:
                if (typeName.contains("[]")) {
                    typeName = typeName.replace("[]", "");
                    return Class.forName("[L" + typeName + ";");
                } else {
                    return Class.forName(typeName);
                }
        }
    }

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
                    PscoutMethod.add(mLine);
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

        for (String sensSignature : PscoutMethod) {
            try {
                XMethod xMethod = signature2Class(sensSignature);
                if (xMethod != null) {
                    Log.w(TAG, xMethod.getMethodName() + ": " + xMethod.getDeclaredClass());
                    if (xMethod.getParamTypes() != null) {
                        for (Class paramType : xMethod.getParamTypes()) {
                            Log.w(TAG, "paramType" + paramType);
                        }
                    }
                    PscoutXMethod.add(xMethod);
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 包加载时候的回调, which is the entry method of the hook system
     */
    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        Log.v(TAG, "Package checking: " + lpparam.packageName);
        // 将包名不是 edu.ucdavis.test的应用剔除掉
        //if (!lpparam.packageName.equals("edu.ucavis.test")) {
        //return;
        //}

        if (getPscoutXMethod() == null) {
            readSensDefFile();
        }

        XposedBridge.log("Loaded app: " + lpparam.packageName);
        Log.w(TAG, "Staring hooking " + lpparam.packageName);

        String self = Main.class.getPackage().getName();
        if (lpparam.packageName.equals(self)) {
            return;
        }

        for (XMethod xMethod : getPscoutXMethod()) {
            Log.w(TAG, xMethod.getMethodName() + " " + xMethod.getDeclaredClass());
            for (Class paramType : xMethod.getParamTypes()) {
                Log.w(TAG, "paramType" + paramType);
            }
            findAndHookMethod(xMethod.getDeclaredClass(), xMethod.getMethodName(), xMethod.getParamTypes(),
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            XposedBridge.log("开始劫持了~");
                            Log.w(TAG, "Hooking method " + param.method);
                            param.args[0] = "10086";
                        }

                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            XposedBridge.log("劫持结束了~");
                            XposedBridge.log("参数1 = " + param.args[0]);

                            Log.w(TAG, "End hooking method " + param.method);
                        }
                    });
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
