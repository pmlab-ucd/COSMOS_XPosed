package fu.hao.cosmos_xposed.hook;

import android.util.Log;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

/**
 * Description:
 *
 * @author Hao Fu(haofu AT ucdavis.edu)
 * @since 4/6/2017
 */
public class ThreadHook extends XC_MethodHook {

    private boolean hasRunnableArg;

    public ThreadHook(boolean hasRunnableArg) {
        this.hasRunnableArg = hasRunnableArg;
    }

    private static String TAG = ThreadHook.class.getName();

    /* Assure latest read of write */
    private static volatile Runnable _runnable = null;

    public static Runnable getRunnable() {
        return _runnable;
    }

    @Override
    protected void afterHookedMethod(XC_MethodHook.MethodHookParam param)
            throws Throwable {
        if (hasRunnableArg) {
            _runnable = (Runnable) param.args[0];
            Log.v(TAG, _runnable.toString());
            XposedHelpers.findAndHookMethod(_runnable.getClass(), "run", new RunnableHook());
        } else {
            _runnable = (Runnable) param.thisObject;
            if (_runnable instanceof Thread) {
                Thread thread = (Thread) _runnable;
                Log.v(TAG, thread.toString() + ", " + thread.getId());

                for (StackTraceElement stackTraceElement : thread.getStackTrace()) {
                    Log.v(TAG, stackTraceElement.getMethodName() + ", " + stackTraceElement.getClassName());
                }
            }
        }
    }
}
