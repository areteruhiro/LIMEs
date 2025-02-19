package io.github.hiro.lime.hooks;

import android.content.Context;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.hiro.lime.LimeOptions;

public class SpoofUserAgent implements IHook {
    private boolean hasLoggedSpoofedUserAgent = false;

    @Override
    public void hook(LimeOptions limeOptions, XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        // CustomPreferences を初期化
        io.github.hiro.lime.hooks.CustomPreferences customPreferences = new io.github.hiro.lime.hooks.CustomPreferences();


        // 設定を確認
        if (!Boolean.parseBoolean(customPreferences.getSetting("android_secondary", "false"))) {
            return; // 設定が無効な場合は何もしない
        }

        // ターゲットメソッドをフック
        XposedHelpers.findAndHookMethod(
                loadPackageParam.classLoader.loadClass(Constants.USER_AGENT_HOOK.className),
                Constants.USER_AGENT_HOOK.methodName,
                Context.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {

                        String device = customPreferences.getSetting("device_name", "ANDROID");
                        String androidVersion = customPreferences.getSetting("android_version", "14.16.0");
                        String osName = customPreferences.getSetting("os_name", "Android OS");
                        String osVersion = customPreferences.getSetting("os_version", "14");

                        String spoofedUserAgent = device + "\t" + androidVersion + "\t" + osName + "\t" + osVersion;
                        param.setResult(spoofedUserAgent);

                        if (!hasLoggedSpoofedUserAgent) {
                            XposedBridge.log("Spoofed User-Agent: " + spoofedUserAgent);
                            hasLoggedSpoofedUserAgent = true;
                        }
                    }
                }
        );
    }
}