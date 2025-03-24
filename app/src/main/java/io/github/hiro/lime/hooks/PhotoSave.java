package io.github.hiro.lime.hooks;


import static android.content.ContentValues.TAG;
import static io.github.hiro.lime.Main.limeOptions;

import android.app.AndroidAppHelper;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.hiro.lime.LimeOptions;

public class PhotoSave implements IHook {

    private SQLiteDatabase db3 = null;
    private SQLiteDatabase db4 = null;

    @Override
    public void hook(LimeOptions limeOptions, XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {

        XposedHelpers.findAndHookMethod(Application.class, "onCreate", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Application appContext = (Application) param.thisObject;


                if (appContext == null) {
                    return;
                }
                File dbFile3 = appContext.getDatabasePath("naver_line");
                File dbFile4 = appContext.getDatabasePath("contact");
                if (dbFile3.exists() && dbFile4.exists()) {
                    SQLiteDatabase.OpenParams.Builder builder1 = new SQLiteDatabase.OpenParams.Builder();
                    builder1.addOpenFlags(SQLiteDatabase.OPEN_READWRITE);
                    SQLiteDatabase.OpenParams dbParams1 = builder1.build();


                    SQLiteDatabase.OpenParams.Builder builder2 = new SQLiteDatabase.OpenParams.Builder();
                    builder2.addOpenFlags(SQLiteDatabase.OPEN_READWRITE);
                    SQLiteDatabase.OpenParams dbParams2 = builder2.build();


                    db3 = SQLiteDatabase.openDatabase(dbFile3, dbParams1);
                    db4 = SQLiteDatabase.openDatabase(dbFile4, dbParams2);


                    Context moduleContext = AndroidAppHelper.currentApplication().createPackageContext(
                            "io.github.hiro.lime", Context.CONTEXT_IGNORE_SECURITY);
                    SaveCatch(loadPackageParam, db3, db4, appContext, moduleContext);
                }
            }
        });

        XposedHelpers.findAndHookMethod(
                "com.linecorp.line.album.data.model.AlbumPhotoModel",
                loadPackageParam.classLoader,
                "toString", // すべてのプロパティを含むメソッド
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        String modelInfo = (String) param.getResult();
                        XposedBridge.log("[AlbumPhotoModel] " + modelInfo);
                    }
                }
        );
        XposedBridge.hookAllMethods(
                loadPackageParam.classLoader.loadClass("lm.K$b"),
                "invokeSuspend",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        StringBuilder argsString = new StringBuilder("Args: ");

                        // 引数が複数の場合、すべてを追加
                        for (int i = 0; i < param.args.length; i++) {
                            Object arg = param.args[i];
                            argsString.append("Arg[").append(i).append("]: ")
                                    .append(arg != null ? arg.toString() : "null")
                                    .append(", ");
                        }

                        XposedBridge.log("[lm.K$b]"+ argsString);
                    }
                }
        );

    }
    private void dumpAllFields(Object obj) {
        for (Field field : obj.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            try {
                XposedBridge.log(field.getName() + " = " + field.get(obj));
            } catch (IllegalAccessException e) {
                XposedBridge.log("Error accessing field: " + field.getName());
            }
        }
    }
    private volatile boolean isDh1Invoked = false; // フラグ追加
    private volatile boolean isDh1Invoked2 = false; // フラグ追加
    private void SaveCatch(XC_LoadPackage.LoadPackageParam loadPackageParam,
                           SQLiteDatabase db3, SQLiteDatabase db4,
                           Context appContext, Context moduleContext)
            throws ClassNotFoundException {

        // 1. Dh1.p0のinvokeSuspendをフック
        XposedBridge.hookAllMethods(
                loadPackageParam.classLoader.loadClass("Dh1.p0"),
                "invokeSuspend",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        StringBuilder argsString = new StringBuilder("Args: ");

                        // 引数が複数の場合、すべてを追加
                        for (int i = 0; i < param.args.length; i++) {
                            Object arg = param.args[i];
                            argsString.append("Arg[").append(i).append("]: ")
                                    .append(arg != null ? arg.toString() : "null")
                                    .append(", ");
                        }
                        if (appContext == null) return;
                        isDh1Invoked = true; // フラグを立てる
                        Object result = param.getResult();
                        XposedBridge.log("[Dh1.p0]" +(result != null ? result.toString() : "null"));
                    }
                }
        );

        // 2. System.currentTimeMillisをフック（グローバルに1回だけ登録）
        XposedBridge.hookAllMethods(
                System.class,
                "currentTimeMillis",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (isDh1Invoked) {
                            long currentTime = (long) param.getResult();
                            XposedBridge.log("[Dh1.p0] System.currentTimeMillis() returned: " + currentTime);
                            isDh1Invoked = false; // フラグをリセット
                            isDh1Invoked2 = true;
                        }
                    }
                }
        );

        XposedBridge.hookAllMethods(
                loadPackageParam.classLoader.loadClass("Ec1.U"),
                "invokeSuspend",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        StringBuilder argsString = new StringBuilder("Args: ");

                        // 引数が複数の場合、すべてを追加
                        for (int i = 0; i < param.args.length; i++) {
                            Object arg = param.args[i];
                            argsString.append("Arg[").append(i).append("]: ")
                                    .append(arg != null ? arg.toString() : "null")
                                    .append(", ");
                        }

                        if (appContext == null) return;
                        isDh1Invoked2 = true; // フラグを立てる
                        XposedBridge.log("[Dh1.p0]"+ argsString);
                    }
                }
        );
    }

}