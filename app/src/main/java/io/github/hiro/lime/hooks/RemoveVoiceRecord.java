package io.github.hiro.lime.hooks;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Looper;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.hiro.lime.LimeOptions;
public class RemoveVoiceRecord implements IHook {

    @Override
    public void hook(LimeOptions limeOptions, XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        if (!limeOptions.RemoveVoiceRecord.checked) return;
        final boolean[] shouldProceed = {true};
         final boolean[] isDelayActive = {false};
            XposedHelpers.findAndHookMethod(
                    "android.content.res.Resources",
                    loadPackageParam.classLoader,
                    "getString",
                    int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            int resourceId = (int) param.args[0];

                            if (resourceId == 2132085530) {
                                shouldProceed[0] = false;
                                isDelayActive[0] = true;
                               // XposedBridge.log("shouldProceed set to false");

                                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                    shouldProceed[0] = true;
                                    isDelayActive[0] = false;
                                   // XposedBridge.log("shouldProceed reset to true after 5 seconds");
                                }, 1000);

                            } else if (resourceId == 2132083032) {
                                if (!isDelayActive[0]) {
                                    shouldProceed[0] = true;
                                 //   XposedBridge.log("true");
//                                } else {
//                                    XposedBridge.log("trueトリガーを無視（遅延中）");
//                                }
                                }
                            }
                        }
                    }
            );
        XposedBridge.hookAllMethods(
                loadPackageParam.classLoader.loadClass(Constants.RemoveVoiceRecord_Hook_a.className),
                "run",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {

                        if (shouldProceed[0]) {
                            param.setResult(null);
                        }
                    }
                }
        );
    }
}