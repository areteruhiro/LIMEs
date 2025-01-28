package io.github.hiro.lime.hooks;

import android.content.ContentResolver;
import android.provider.Settings;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.hiro.lime.LimeOptions;

public class SpoofAndroidId implements IHook {
    @Override
    public void hook(LimeOptions limeOptions, XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        // CustomPreferences を初期化
        io.github.hiro.lime.hooks.CustomPreferences customPreferences = new io.github.hiro.lime.hooks.CustomPreferences();



        // 設定を確認
        if (!Boolean.parseBoolean(customPreferences.getSetting("spoof_android_id", "false"))) {
            return; // 設定が無効な場合は何もしない
        }

        // Settings.Secure.getString メソッドをフック
        XposedHelpers.findAndHookMethod(
                Settings.Secure.class,
                "getString",
                ContentResolver.class,
                String.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        // ANDROID_ID が要求された場合
                        if (param.args[1].toString().equals(Settings.Secure.ANDROID_ID)) {
                            // 偽の ANDROID_ID を返す
                            param.setResult("0000000000000000");
                        }
                    }
                }
        );
    }
}