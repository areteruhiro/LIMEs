package io.github.hiro.lime.hooks;

import android.content.Context;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.hiro.lime.LimeOptions;

public class SpoofUserAgent implements IHook {
    private volatile boolean hasLoggedSpoofedUserAgent = false;

    @Override
    public void hook(LimeOptions limeOptions, XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        XposedHelpers.findAndHookMethod(
                loadPackageParam.classLoader.loadClass(Constants.USER_AGENT_HOOK.className),
                Constants.USER_AGENT_HOOK.methodName,
                Context.class, // 第1引数がContextであることを明示
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        // ターゲットアプリのContextをパラメータから取得
                        Context context = (Context) param.args[0];

                        try {
                            CustomPreferences customPrefs = new CustomPreferences(context);
                            boolean isSecondaryEnabled = Boolean.parseBoolean(
                                    customPrefs.getSetting("android_secondary", "false")
                            );

                            if (!isSecondaryEnabled) return;

                            String device = customPrefs.getSetting("device_name", "ANDROID");
                            String androidVersion = customPrefs.getSetting("android_version", "14.16.0");
                            String osName = customPrefs.getSetting("os_name", "Android OS");
                            String osVersion = customPrefs.getSetting("os_version", "14");

                            String spoofedUserAgent = String.format("%s\t%s\t%s\t%s",
                                    device, androidVersion, osName, osVersion);

                            param.setResult(spoofedUserAgent);
                            logUserAgentOnce(spoofedUserAgent);

                        } catch (Exception ignored) {
                        }
                    }
                }
        );
    }

    private synchronized void logUserAgentOnce(String userAgent) {
        if (!hasLoggedSpoofedUserAgent) {
            XposedBridge.log("Lime: Spoofed User-Agent - " + userAgent);
            hasLoggedSpoofedUserAgent = true;
        }
    }
}