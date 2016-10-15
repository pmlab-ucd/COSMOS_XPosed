package fu.hao.cosmos_xposed.hook;

import android.util.Log;

/**
 * Description:
 *
 * @author Hao Fu(haofu AT ucdavis.edu)
 * @since 10/14/2016
 */
public class XMethod {
    private final String TAG = this.getClass().getSimpleName();

    private Class declaredClass;
    private String methodName;
    private Class[] paramTypes;

    public void setDeclaredClass(Class declaredClass) {
        Log.d(TAG, "setDeclaringClass " + declaredClass);
        this.declaredClass = declaredClass;
    }

    public Class getDeclaredClass() {
        return declaredClass;
    }

    public void setMethodName(String methodName) {
        Log.d(TAG, "seMethodName " + methodName);
        this.methodName = methodName;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setParamTypes(Class[] paramTypes) {
        this.paramTypes = paramTypes;
    }

    public Class[] getParamTypes() {
        return paramTypes;
    }

}
