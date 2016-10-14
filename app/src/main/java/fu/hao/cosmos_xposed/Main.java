package fu.hao.cosmos_xposed;

import android.util.Log;
import android.widget.TextView;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

/**
 * Description:
 *
 * @author Hao Fu(haofu AT ucdavis.edu)
 * @since 10/13/2016
 */
public class Main implements IXposedHookLoadPackage {
    private final String TAG = this.getClass().getSimpleName();

    /**
     * 包加载时候的回调
     */
    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        // 将包名不是 com.example.login 的应用剔除掉
        //if (!lpparam.packageName.equals("com.example.login"))
           // return;
        XposedBridge.log("Loaded app: " + lpparam.packageName);
        Log.w(TAG, "Staring hooking " + lpparam.packageName);

        findAndHookMethod(TextView.class, "setText", CharSequence.class,
                 new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        XposedBridge.log("开始劫持了~");
                        Log.w(TAG, "Staring hooking!");
                        param.args[0] = "Hooked!";
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        XposedBridge.log("劫持结束了~");
                        XposedBridge.log("参数1 = " + param.args[0]);
                    }
                });
    }
}
