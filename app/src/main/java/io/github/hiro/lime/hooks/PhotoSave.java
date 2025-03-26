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
import android.media.ExifInterface;
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.hiro.lime.LimeOptions;

public class PhotoSave implements IHook {
    private SQLiteDatabase db3 = null;
    private SQLiteDatabase db4 = null;

    private volatile boolean isDh1Invoked = false;
    private volatile boolean isDh1Invoked2 = false;

    private long currentTimeMillisValue = 0;
    private String currentCreatedTime = "";
    private String currentContentType = "";



    private volatile boolean album1 = false;
    private volatile boolean album2 = false;
    private volatile boolean album3 = false;
    private volatile boolean album4 = false;
    private volatile boolean album5 = false;
    private long currentAlbumTime = 0;
    private String currentAlbumFormattedTime = "";

    private String currentOid = "";

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


//        XposedBridge.hookAllMethods(
//                java.lang.System.class,
//                "currentTimeMillis",
//                new XC_MethodHook() {
//
//                    @Override
//                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//                        // 元の戻り値を取得
//                        long originalResult = (long) param.getResult();
//
//                        // スタックトレースを取得
//                        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
//
//                        // ログに出力
//                        StringBuilder sb = new StringBuilder();
//                        sb.append("System.currentTimeMillis() returned: ").append(originalResult).append("\n");
//                        sb.append("Stack trace:\n");
//
//                        // スタックトレースをフォーマット (上位10フレームまで)
//                        for (int i = 0; i < Math.min(stackTrace.length, 80); i++) {
//                            StackTraceElement element = stackTrace[i];
//                            sb.append("  at ")
//                                    .append(element.getClassName())
//                                    .append(".")
//                                    .append(element.getMethodName())
//                                    .append("(")
//                                    .append(element.getFileName())
//                                    .append(":")
//                                    .append(element.getLineNumber())
//                                    .append(")\n");
//                        }
//
//                        XposedBridge.log(sb.toString());
//                    }
//                }
//        );
    }




    private void SaveCatch(XC_LoadPackage.LoadPackageParam loadPackageParam, SQLiteDatabase db3, SQLiteDatabase db4, Context appContext, Context moduleContext) throws ClassNotFoundException {

// チャット内
        XposedBridge.hookAllMethods(
                System.class,
                "currentTimeMillis",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (isDh1Invoked) {
                            long currentTime = (long) param.getResult();
                            XposedBridge.log("[Dh1.p0] System.currentTimeMillis() returned: " + currentTime);

                            // 新しいタスクを作成してキューに追加
                            synchronized (fileTasks) {
                                fileTasks.add(new ImageFileTask(
                                        currentTime,
                                        currentCreatedTime,
                                        currentContentType
                                ));
                            }

                            isDh1Invoked = false;
                            isDh1Invoked2 = true;
                            handleFileRename(); // 即時処理を開始
                        }
                    }
                }
        );

        XposedBridge.hookAllMethods(
                loadPackageParam.classLoader.loadClass("Dh1.p0"),
                "invokeSuspend",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (appContext == null) return;

                        Object result = param.getResult();
                        if (result == null) {
                            XposedBridge.log("[Dh1.p0] Result is null");
                            return;
                        }

                        String resultStr = result.toString();
                        if (resultStr.startsWith("ChatHistoryMessageData(")) {
                            // 必要なフィールドを抽出
                            String serverMessageId = extractField(resultStr, "serverMessageId=");
                            String chatId = extractField(resultStr, "chatId=");
                            currentContentType = extractField(resultStr, "contentType=");
                            currentCreatedTime = extractField(resultStr, "createdTimeMillis=");

                            String logMessage = String.format(
                                    "[Message]\nServerMsgID: %s\nChatID: %s\nType: %s\nTime: %s",
                                    serverMessageId, chatId, currentContentType, formatTimestamp(currentCreatedTime)
                            );
                            XposedBridge.log(logMessage);

                            isDh1Invoked = true;
                        }

                    }

                }

        );

        // Ec1.Uのフック
        XposedBridge.hookAllMethods(
                loadPackageParam.classLoader.loadClass("Ec1.U"),
                "invokeSuspend",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (isDh1Invoked2 && "IMAGE".equals(currentContentType)) {
                            isDh1Invoked2 = false; // フラグをリセット
                            handleFileRename();
                        }
                    }
                }
        );

        // ProgressWheelのフック
        XposedBridge.hookAllMethods(
                loadPackageParam.classLoader.loadClass("com.todddavies.components.progressbar.ProgressWheel"),
                "setText",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (isDh1Invoked2 && "IMAGE".equals(currentContentType)) {
                            isDh1Invoked2 = false; // フラグをリセット
                            handleFileRename();
                        }
                    }
                }
        );

//アルバムの処理


 ////////////////////////////////////////////////////
        //albumオリジナルファイル名
        XposedBridge.hookAllMethods(
                loadPackageParam.classLoader.loadClass("XQ.g"),
                "a",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {

                        album1 = true; // フラグをリセット
                    }
                }
        );
/*
        XposedBridge.hookAllMethods(
                System.class,
                "currentTimeMillis",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (album1) {
                            long currentTime = (long) param.getResult();
                            XposedBridge.log("(album) " + currentTime);
                            album1 = false;

                        }
                    }
                }
        );
        */

//////////////////////////////////////////////

//album単体
        XposedHelpers.findAndHookMethod(
                "com.linecorp.line.album.data.model.AlbumPhotoModel",
                loadPackageParam.classLoader,
                "toString",
                new XC_MethodHook() {

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        String modelInfo = (String) param.getResult();
                        XposedBridge.log("[AlbumPhotoModel] " + modelInfo);

                        // 必要なフィールドを抽出
                        String mid = extractField(modelInfo, "mid=");
                        String createdTime = extractField(modelInfo, "createdTime=");
                        String resourceType = extractField(modelInfo, "resourceType=");
                        currentOid = extractField(modelInfo, "oid=");

                        XposedBridge.log("Extracted fields - mid: " + mid + ", createdTime: " + createdTime + ", resourceType: " + resourceType);

                        // 画像の場合のみ処理
                        if ("IMAGE".equals(resourceType)) {
                            try {
                                long timeMillis = Long.parseLong(createdTime);
                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                                currentAlbumFormattedTime = sdf.format(new Date(timeMillis));
                                album1 = true; // currentTimeMillisを取得するフラグ

                                XposedBridge.log("Preparing to process image - Owner: " + mid + ", Time: " + currentAlbumFormattedTime);
                            } catch (Exception e) {
                                XposedBridge.log("Error parsing time: " + e.getMessage());
                            }
                        }
                    }
                }
        );

        XposedBridge.hookAllMethods(
                System.class,
                "currentTimeMillis",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (album1) {
                            currentAlbumTime = (long) param.getResult();
                            XposedBridge.log("(album) CurrentTimeMillis: " + currentAlbumTime);
                            album1 = false;
                            album3 =true;
                        }

                        if (album4) {
                            currentAlbumTime = (long) param.getResult();
                            XposedBridge.log("(album) All　album: " + currentAlbumTime);
                            album4 = false;
                            album5 = true;
                        }

                    }
                }
        );

        XposedBridge.hookAllMethods(
                loadPackageParam.classLoader.loadClass("lm.K$b"),
                "invokeSuspend",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        // kotlin.Unitチェック
                        boolean containsKotlinUnit = false;
                        StringBuilder argsString = new StringBuilder("Args: ");

                        for (int i = 0; i < param.args.length; i++) {
                            Object arg = param.args[i];
                            String argStr = arg != null ? arg.toString() : "null";
                            argsString.append("Arg[").append(i).append("]: ").append(argStr).append(", ");

                            if (argStr.contains("kotlin.Unit")) {
                                containsKotlinUnit = true;
                            }
                        }

                        XposedBridge.log("[lm.K$b] " + argsString);

                        // album2モード判定
                        album2 = containsKotlinUnit;

                        if (album2 && currentAlbumTime != 0) {
                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                final int MAX_RETRIES = 30; // 最大30回試行
                                final long RETRY_INTERVAL = 5000; // 500ミリ秒間隔
                                int retryCount = 0;

                                @Override
                                public void run() {
                                    try {
                                        File lineDir = new File(Environment.getExternalStoragePublicDirectory(
                                                Environment.DIRECTORY_PICTURES), "LINE");

                                        // ディレクトリが存在しない場合は作成
                                        if (!lineDir.exists() && !lineDir.mkdirs()) {
                                            XposedBridge.log("Failed to create LINE directory");
                                            return;
                                        }

                                        String tempFileName = currentAlbumTime + ".jpg";
                                        String baseName = currentAlbumFormattedTime.replace(" ", "_").replace(":", "-");
                                        String newFileName = baseName + ".jpg";

                                        File tempFile = new File(lineDir, tempFileName);
                                        File newFile = new File(lineDir, newFileName);

                                        if (tempFile.exists()) {
                                            // 重複処理
                                            int counter = 1;
                                            while (newFile.exists()) {
                                                newFileName = baseName + "_" + counter + ".jpg";
                                                newFile = new File(lineDir, newFileName);
                                                counter++;
                                            }

                                            if (tempFile.renameTo(newFile)) {
                                                XposedBridge.log("Successfully renamed: " + tempFileName + " -> " + newFileName);
                                            } else {
                                                tryRetry("Failed to rename file");
                                            }
                                        } else {
                                            tryRetry("Temp file not found: " + tempFileName + " (attempt " + (retryCount + 1) + ")");
                                        }
                                    } catch (Exception e) {
                                        tryRetry("Error: " + e.getMessage());
                                    }
                                }

                                private void tryRetry(String errorMessage) {
                                    XposedBridge.log(errorMessage);
                                    if (retryCount < MAX_RETRIES) {
                                        retryCount++;
                                        new Handler(Looper.getMainLooper()).postDelayed(this, RETRY_INTERVAL);
                                    } else {
                                        XposedBridge.log("Max retries reached for: " + currentAlbumTime + ".jpg");
                                    }
                                }
                            });
                        }
                    }
                }
        );



        XposedBridge.hookAllMethods(
                loadPackageParam.classLoader.loadClass("nl.c"),
                "invokeSuspend",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        StringBuilder argsString = new StringBuilder("Args: ");
                        for (int i = 0; i < param.args.length; i++) {
                            Object arg = param.args[i];
                            argsString.append("Arg[").append(i).append("]: ")
                                    .append(arg != null ? arg.toString() : "null")
                                    .append(", ");
                        }

                        if (album3) {
                            album3 = false;
                            Object result = param.getResult();
                            String resultStr = result != null ? result.toString() : "null";
                            XposedBridge.log("[nl.c] " + resultStr);

                            String oid2 = extractField(resultStr, "oid=");
                            XposedBridge.log("OID : " + oid2);
                            if (currentOid.equals(oid2)) {
                                album4 = true;
                                XposedBridge.log("OID matched: " + currentOid);
                            }
                        }
                    }
                }
        );

        XposedBridge.hookAllMethods(
                loadPackageParam.classLoader.loadClass("kl.h"),
                "invokeSuspend",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (album5) {
                            album5 = false;
                            XposedBridge.log("[kl.h] Starting file processing");

                            // ファイル処理を開始
                            processAlbumImageFile();
                        }
                    }
                }
        );
    }


    private void processAlbumImageFile() {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            final int MAX_RETRIES = 30;
            final long RETRY_INTERVAL = 500;
            int retryCount = 0;

            @Override
            public void run() {
                try {
                    File lineDir = new File(Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_PICTURES), "LINE");

                    if (!lineDir.exists() && !lineDir.mkdirs()) {
                        tryRetry("Failed to create directory");
                        return;
                    }

                    String tempFileName = currentAlbumTime + ".jpg";
                    String baseName = currentAlbumFormattedTime.replace(" ", "_").replace(":", "-");
                    String newFileName = baseName + ".jpg";

                    File tempFile = new File(lineDir, tempFileName);
                    File newFile = new File(lineDir, newFileName);

                    if (tempFile.exists()) {
                        // 重複処理
                        int counter = 1;
                        while (newFile.exists()) {
                            newFileName = baseName + "_" + counter + ".jpg";
                            newFile = new File(lineDir, newFileName);
                            counter++;
                        }

                        if (tempFile.renameTo(newFile)) {
                            XposedBridge.log("Successfully renamed: " + tempFileName + " -> " + newFileName);
                        } else {
                            tryRetry("Failed to rename file");
                        }
                    } else {
                        tryRetry("File not found: " + tempFileName + " (attempt " + (retryCount + 1) + ")");
                    }
                } catch (Exception e) {
                    tryRetry("Error: " + e.getMessage());
                }
            }

            private void tryRetry(String message) {
                XposedBridge.log(message);
                if (retryCount < MAX_RETRIES) {
                    retryCount++;
                    new Handler(Looper.getMainLooper()).postDelayed(this, RETRY_INTERVAL);
                } else {
                    XposedBridge.log("Max retries reached for: " + currentAlbumTime + ".jpg");
                }
            }
        });
    }


    private static class ImageFileTask {
        final long tempFileTime;
        final String createdTime;
        final String contentType;
        boolean processed;
        int retryCount = 0;

        ImageFileTask(long tempFileTime, String createdTime, String contentType) {
            this.tempFileTime = tempFileTime;
            this.createdTime = createdTime;
            this.contentType = contentType;
        }
    }



    private final ConcurrentLinkedQueue<ImageFileTask> fileTasks = new ConcurrentLinkedQueue<>();

    private void handleFileRename() {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            final int MAX_RETRIES = 3;
            final long RETRY_INTERVAL = 300;

            @Override
            public void run() {
                synchronized (fileTasks) {
                    Iterator<ImageFileTask> iterator = fileTasks.iterator();
                    while (iterator.hasNext()) {
                        ImageFileTask task = iterator.next();

                        if (task.processed) {
                            iterator.remove();
                            continue;
                        }

                        try {
                            String tempFileName = task.tempFileTime + ".jpg";
                            String newFileName = formatForFilename(task.createdTime) + ".jpg";
                            File lineDir = new File(Environment.getExternalStoragePublicDirectory(
                                    Environment.DIRECTORY_PICTURES), "LINE");

                            if (!lineDir.exists()) {
                                lineDir.mkdirs();
                            }

                            File tempFile = new File(lineDir, tempFileName);

                            if (tempFile.exists()) {
                                // 重複処理
                                int counter = 1;
                                File newFile = new File(lineDir, newFileName);
                                while (newFile.exists()) {
                                    newFileName = formatForFilename(task.createdTime) + "_" + counter + ".jpg";
                                    newFile = new File(lineDir, newFileName);
                                    counter++;
                                }

                                if (tempFile.renameTo(newFile)) {
                                    XposedBridge.log("Successfully renamed: " + tempFileName + " -> " + newFileName);
                                    task.processed = true;
                                }
                            } else if (task.retryCount < MAX_RETRIES) {
                                task.retryCount++;
                                XposedBridge.log("Retrying (" + task.retryCount + "/" + MAX_RETRIES + "): " + tempFileName);
                            } else {
                                XposedBridge.log("Max retries reached for: " + tempFileName);
                                task.processed = true;
                            }
                        } catch (Exception e) {
                            XposedBridge.log("Error processing file: " + e.getMessage());
                        }
                    }
                }

                // 未処理のタスクがあれば再試行
                if (!fileTasks.isEmpty()) {
                    new Handler(Looper.getMainLooper()).postDelayed(this, RETRY_INTERVAL);
                }
            }
        });
    }

    private String formatForFilename(String millisStr) {
        try {
            long millis = Long.parseLong(millisStr);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm", Locale.getDefault());
            return sdf.format(new Date(millis));
        } catch (Exception e) {
            return millisStr;
        }
    }

    private String extractField(String input, String fieldName) {
        try {
            int start = input.indexOf(fieldName);
            if (start == -1) return "N/A";

            start += fieldName.length();
            int end = input.indexOf(",", start);
            if (end == -1) end = input.indexOf(")", start);
            if (end == -1) return "N/A";

            return input.substring(start, end).trim();
        } catch (Exception e) {
            return "Error";
        }
    }

    private String formatTimestamp(String millisStr) {
        try {
            long millis = Long.parseLong(millisStr);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            return sdf.format(new Date(millis));
        } catch (Exception e) {
            return millisStr + " (raw)";
        }
    }
}




