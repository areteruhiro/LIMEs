package io.github.hiro.lime.hooks;

import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.provider.Settings;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.hiro.lime.LimeOptions;

public class SpoofAndroidId implements IHook {

    @Override
    public void hook(LimeOptions limeOptions, XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {

        XposedHelpers.findAndHookMethod(
                Settings.Secure.class,
                "getString",
                ContentResolver.class,
                String.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (Settings.Secure.ANDROID_ID.equals(param.args[1])) {
                            try {
                                CustomPreferences customPrefs = new CustomPreferences();
                                boolean isSpoofEnabled = Boolean.parseBoolean(
                                        customPrefs.getSetting("spoof_android_id", "false"));

                                if (isSpoofEnabled) {
                                    param.setResult("0000000000000000");
                                    XposedBridge.log("Lime: Android ID spoofing activated");
                                }
                            } catch (PackageManager.NameNotFoundException e) {
                                        }
                        }
                    }
                }
        );
    }
}