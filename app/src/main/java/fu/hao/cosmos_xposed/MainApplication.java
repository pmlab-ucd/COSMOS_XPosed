package fu.hao.cosmos_xposed;

import android.app.Application;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import fu.hao.cosmos_xposed.hook.XMethod;

/**
 * Description:
 *
 * @author Hao Fu(haofu AT ucdavis.edu)
 * @since 10/14/2016
 */
public class MainApplication extends Application {
    private final String TAG = this.getClass().getSimpleName();

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
            case "int": return Integer.TYPE;
            case "int[]": int[] integers = new int[1];
                return integers.getClass();
            case "short": return Short.TYPE;
            case "short[]": short[] shorts = new short[1];
                return shorts.getClass();
            case "float": return Float.TYPE;
            case "float[]": float[] floats = new float[1];
                return floats.getClass();
            case "double": return Double.TYPE;
            case "double[]": double[] doubles = new double[1];
                return doubles.getClass();
            case "char": return Character.TYPE;
            case "char[]": char[] chars = new char[1];
                return chars.getClass();
            case "byte": return Byte.TYPE;
            case "byte[]": byte[] bytes = new byte[1];
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

    public Class<?> getArrayClass(Class<?> componentType) throws ClassNotFoundException{
        ClassLoader classLoader = componentType.getClassLoader();
        String name;
        if(componentType.isArray()){
            // just add a leading "["
            name = "["+componentType.getName();
        }else if(componentType == boolean.class){
            name = "[Z";
        }else if(componentType == byte.class){
            name = "[B";
        }else if(componentType == char.class){
            name = "[C";
        }else if(componentType == double.class){
            name = "[D";
        }else if(componentType == float.class){
            name = "[F";
        }else if(componentType == int.class){
            name = "[I";
        }else if(componentType == long.class){
            name = "[J";
        }else if(componentType == short.class){
            name = "[S";
        }else{
            // must be an object non-array class
            name = "[L"+componentType.getName()+";";
        }
        return classLoader != null ? classLoader.loadClass(name) : Class.forName(name);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(
                    new InputStreamReader(getAssets().open("test.txt")));

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
}
