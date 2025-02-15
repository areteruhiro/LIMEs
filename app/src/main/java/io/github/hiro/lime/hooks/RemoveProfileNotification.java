package io.github.hiro.lime.hooks;

import static io.github.hiro.lime.Main.limeOptions;

import android.app.Application;
import android.app.Notification;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Environment;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.hiro.lime.LimeOptions;

public class RemoveProfileNotification implements IHook {
    private static boolean isHandlingHook = false;
    @Override
    public void hook(LimeOptions limeOptions, XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        if (!limeOptions.RemoveNotification.checked) return;
        XposedHelpers.findAndHookMethod(
                "android.content.res.Resources",
                loadPackageParam.classLoader,
                "getString",
                int.class,
                new XC_MethodHook() {

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (isHandlingHook) {
                            return;
                        }
                        int resourceId = (int) param.args[0];
                        Resources resources = (Resources) param.thisObject;
                        try {
                            isHandlingHook = true;

                            String resourceName;
                            try {
                                resourceName = resources.getResourceName(resourceId);
                            } catch (Resources.NotFoundException e) {
                                return;
                            }
                            String entryName = resourceName.substring(resourceName.lastIndexOf('/') + 1);
                            if ("line_home_header_recentlyupdatedsection".equals(entryName)) {
                                param.setResult(""); // 空文字列を返す
                            }
                        } finally {
                            isHandlingHook = false;
                        }
                    }
                }
        );
    }
}