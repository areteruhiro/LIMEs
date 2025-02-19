package io.github.hiro.lime.hooks;

import android.content.Context;
import android.content.res.Resources;
import net.sqlcipher.database.SQLiteDatabase;

import android.database.MatrixCursor;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannedString;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.hiro.lime.LimeOptions;
public class SettingCrash implements IHook {
    @Override
    public void hook(LimeOptions limeOptions, XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!limeOptions.SettingClick.checked) return;
        XposedHelpers.findAndHookMethod("android.content.ContentProvider",
                lpparam.classLoader,
                "query",
                Uri.class,
                String[].class,
                String.class,
                String[].class,
                String.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        Uri uri = (Uri) param.args[0];
                        if (uri.toString().contains("database_version")) {
                            Bundle extras = new Bundle();
                            extras.putInt("user_version", 156);
                            param.setResult(new MatrixCursor(new String[0], 0).getExtras());
                        }
                    }
                });



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