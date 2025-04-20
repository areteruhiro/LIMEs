package io.github.hiro.lime.hooks;

import android.content.Context;

import android.text.SpannedString;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.hiro.lime.LimeOptions;
public class SettingCrash implements IHook {
    @Override
    public void hook(LimeOptions limeOptions, XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {

        if (!limeOptions.SettingClick.checked) return;
        XposedHelpers.findAndHookMethod(
                Context.class,
                "getText",
                int.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (param.getResult() instanceof String) {
                            param.setResult(SpannedString.valueOf((String) param.getResult()));
                        }
                    }
                }
        );

    }

}