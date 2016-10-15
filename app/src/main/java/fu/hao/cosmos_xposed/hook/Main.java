package fu.hao.cosmos_xposed.hook;

import android.app.Application;
import android.app.PendingIntent;
import android.os.Build;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

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
    private final String TAG = this.getClass().getSimpleName();

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

        XposedBridge.log("Loaded app: " + lpparam.packageName);
        Log.w(TAG, "Staring hooking " + lpparam.packageName);

        String self = Main.class.getPackage().getName();
        if (lpparam.packageName.equals(self)) {
            return;
        }

        for (XMethod xMethod : MainApplication.getPscoutXMethod()) {
            findAndHookMethod(xMethod.getDeclaredClass(), xMethod.getMethodName(), xMethod.getParamTypes(),
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            XposedBridge.log("开始劫持了~");
                            Log.w(TAG, "Staring hooking!");
                            param.args[0] = "10086";
                        }

                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            XposedBridge.log("劫持结束了~");
                            XposedBridge.log("参数1 = " + param.args[0]);
                        }
                    });
        }
    }


}
