package io.github.hiro.lime.hooks;

import android.content.res.Resources;
import android.text.SpannedString;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.hiro.lime.LimeOptions;
public class SettingCrash implements IHook {

    @Override
    public void hook(LimeOptions limeOptions, XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        try {

            Class<?> te0jClass = XposedHelpers.findClass(Constants.SettingCrash_Hook_Sub.className, lpparam.classLoader);
            Class<?> integerClass = Integer.class;

            XposedHelpers.findAndHookMethod(
                    Constants.SettingCrash_Hook.className,
                    lpparam.classLoader,
                    Constants.SettingCrash_Hook.methodName,
                    te0jClass,
                    integerClass,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            Object arg1 = param.args[0];
                            Object arg2 = param.args[1];
//                            XposedBridge.log("Arg1 type: " + arg1.getClass().getName());
//                            XposedBridge.log("Arg2 type: " + arg2.getClass().getName());
                        }
                    }
            );

            XposedHelpers.findAndHookMethod(
                    Resources.class,
                    "getText",
                    int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            Object result = param.getResult();
                            if (result instanceof String) {
                                param.setResult(new SpannedString((String) result));
                            }
                        }
                    }
            );

        } catch (Throwable t) {
            XposedBridge.log("LIME RESOLVED ERROR: " + Log.getStackTraceString(t));
        }
    }
}