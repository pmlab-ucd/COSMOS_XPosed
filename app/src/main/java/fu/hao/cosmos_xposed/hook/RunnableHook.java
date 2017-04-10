package fu.hao.cosmos_xposed.hook;

import android.app.Activity;
import android.util.Log;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

/**
 * Description:
 *
 * @author Hao Fu(haofu AT ucdavis.edu)
 * @since 4/6/2017
 */
public class RunnableHook extends XC_MethodHook {
    private static String TAG = RunnableHook.class.getName();

    /* Assure latest read of write */
    private static volatile Runnable _runnable = null;

    public static Runnable getRunnable() {
        return _runnable;
    }

    @Override
    protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param)
            throws Throwable {
        _runnable = (Runnable) param.thisObject;
        Log.v(TAG, _runnable.toString());
    }
}
