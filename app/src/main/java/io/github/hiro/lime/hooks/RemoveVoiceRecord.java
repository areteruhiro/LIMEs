package io.github.hiro.lime.hooks;

import android.content.Context;
import android.content.pm.PackageManager;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.hiro.lime.BuildConfig;
import io.github.hiro.lime.LimeOptions;
public class RemoveVoiceRecord implements IHook {
    private boolean isXg1EaRunCalled = false;
    private String versionCodeStr; // フィールドとして定義

    @Override
    public void hook(LimeOptions limeOptions, XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        if (!limeOptions.RemoveVoiceRecord.checked) return;

        XposedBridge.hookAllMethods(
                loadPackageParam.classLoader.loadClass(Constants.RemoveVoiceRecord_Hook_a.className),
                "run",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (isXg1EaRunCalled) {
                            return;
                        }
                        param.setResult(null);

                        Context context = (Context) XposedHelpers.callMethod(XposedHelpers.callStaticMethod(
                                XposedHelpers.findClass("android.app.ActivityThread", null),
                                "currentActivityThread"
                        ), "getSystemContext");
                        PackageManager pm = context.getPackageManager();
                        long versionCode = pm.getPackageInfo(loadPackageParam.packageName, 0).getLongVersionCode();
                        versionCodeStr = String.valueOf(versionCode);
                    }
                }
        );


        if (BuildConfig.HOOK_TARGET_VERSION.equals(versionCodeStr)) {
            XposedBridge.hookAllMethods(
                    loadPackageParam.classLoader.loadClass(Constants.RemoveVoiceRecord_Hook_b.className),
                    "run",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            isXg1EaRunCalled = false;
                        }
                    }
            );

            XposedBridge.hookAllMethods(
                    loadPackageParam.classLoader.loadClass(Constants.RemoveVoiceRecord_Hook_c.className),
                    "run",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {

                                isXg1EaRunCalled = true;
                            }

                    }
            );
        }
    }
}