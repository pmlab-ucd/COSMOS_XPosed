package fu.hao.cosmos_xposed.hook;

import android.util.Log;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import fu.hao.cosmos_xposed.MainApplication;
import fu.hao.cosmos_xposed.ml.ClassifierUtils;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

/**
 * Description:
 *
 * @author Hao Fu(haofu AT ucdavis.edu)
 * @since 10/13/2016
 */
public class Main implements IXposedHookLoadPackage {
    private final String TAG = this.getClass().getName();

    private static Set<XMethod> PscoutXMethod;
    static {
        PscoutXMethod = new HashSet<>();
    }

    public static Set<XMethod> getPscoutXMethod() {
        return PscoutXMethod;
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

    private void readTARGET_METHODS() {
        for (String sensSignature : TargetMethods.TARGET_METHODS) {
            try {
                XMethod xMethod = sootMethodStr2XMethod(sensSignature);
                if (xMethod != null) {
                    Log.w(TAG, xMethod.getMethodName() + ": " + xMethod.getDeclaredClass());
                    if (xMethod.getParamTypes() != null) {
                        for (Object paramType : xMethod.getParamTypes()) {
                            Log.v(TAG, "paramType" + paramType);
                        }
                    }
                    PscoutXMethod.add(xMethod);
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

    /**
     * 包加载时候的回调, which is the entry method of the hook system
     */
    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        Log.w(TAG, "Package checking: " + lpparam.packageName);
        // 将包名不是 edu.ucdavis.test的应用剔除掉
        //if (!lpparam.packageName.equals("edu.ucavis.test")) {
        //return;
        //}

        if (getPscoutXMethod().isEmpty()) {
            Log.w(TAG, "Try to read TARGET_METHODS...");
            readTARGET_METHODS();
        }

        XposedBridge.log("Loaded app: " + lpparam.packageName);
        Log.w(TAG, "Staring hooking " + lpparam.packageName);

        String self = Main.class.getPackage().getName();
        if (lpparam.packageName.equals(self)) {
            return;
        }

        Log.v(TAG, "Try to load target methods...");

        for (XMethod xMethod : getPscoutXMethod()) {
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
                    Log.w(TAG, "Hooking method " + param.method);
                    //param.args[0] = "10086";
                    Log.w(TAG, MainApplication.readFromFileExternally("layout.xml"));
                    ClassifierUtils classifier = new ClassifierUtils();
                    //classifier.predict();
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    XposedBridge.log("劫持结束了~");
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
