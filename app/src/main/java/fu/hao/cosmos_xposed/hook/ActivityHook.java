package fu.hao.cosmos_xposed.hook;

import android.app.Activity;
import android.util.Log;

import de.robv.android.xposed.XC_MethodHook;

/**
 * Description:
 *
 * @author Hao Fu(haofu AT ucdavis.edu)
 * @since 4/6/2017
 */
public class ActivityHook extends XC_MethodHook{
    private static String TAG = ActivityHook.class.getName();

    /* Assure latest read of write */
    private static volatile Activity _currentActivity = null;

    public static Activity getCurrentActivity() {
        return _currentActivity;
    }

    @Override
    protected void afterHookedMethod(XC_MethodHook.MethodHookParam param)
            throws Throwable {
        _currentActivity = (Activity) param.getResult();
        Log.w(TAG, _currentActivity.toString());
    }
}
