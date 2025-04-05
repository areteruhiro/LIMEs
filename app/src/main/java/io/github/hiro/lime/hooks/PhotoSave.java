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
    private SQLiteDatabase db = null;
    private SQLiteDatabase dbContact = null;

    private volatile boolean isDh1Invoked = false;
    private volatile boolean isDh1Invoked2 = false;

    private long currentTimeMillisValue = 0;
    private String currentCreatedTime = "";
    private String currentContentType = "";
    private String serverMessageId = "";
    private String chatid = "";
    private String senderMid = "";
    private String mid = "";
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
        if (!limeOptions.PhotoSave.checked) return;

        Context context = (Context) XposedHelpers.callMethod(XposedHelpers.callStaticMethod(
                XposedHelpers.findClass("android.app.ActivityThread", null),
                "currentActivityThread"
        ), "getSystemContext");

        PackageManager pm = context.getPackageManager();
        String versionName = ""; // 初期化
        try {
            versionName = pm.getPackageInfo(loadPackageParam.packageName, 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        if (!isVersionInRange(versionName, "15.3.0", "15.5.0"))return;
        XposedHelpers.findAndHookMethod(Application.class, "onCreate", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Application appContext = (Application) param.thisObject;


                if (appContext == null) {
                    return;
                }
                File dbFile = appContext.getDatabasePath("naver_line");
                File dbFileContact = appContext.getDatabasePath("contact");
                if (dbFile.exists() && dbFileContact.exists()) {
                    SQLiteDatabase.OpenParams.Builder builder1 = new SQLiteDatabase.OpenParams.Builder();
                    builder1.addOpenFlags(SQLiteDatabase.OPEN_READWRITE);
                    SQLiteDatabase.OpenParams dbParams1 = builder1.build();


                    SQLiteDatabase.OpenParams.Builder builder2 = new SQLiteDatabase.OpenParams.Builder();
                    builder2.addOpenFlags(SQLiteDatabase.OPEN_READWRITE);
                    SQLiteDatabase.OpenParams dbParams2 = builder2.build();


                    db = SQLiteDatabase.openDatabase(dbFile, dbParams1);
                    dbContact = SQLiteDatabase.openDatabase(dbFileContact, dbParams2);


                    Context moduleContext = AndroidAppHelper.currentApplication().createPackageContext(
                            "io.github.hiro.lime", Context.CONTEXT_IGNORE_SECURITY);
                    SaveCatch(loadPackageParam, db, dbContact, appContext, moduleContext);
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




    private void SaveCatch(XC_LoadPackage.LoadPackageParam loadPackageParam, SQLiteDatabase db, SQLiteDatabase dbContact, Context appContext, Context moduleContext) throws ClassNotFoundException {

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

                            synchronized (fileTasks) {
                                fileTasks.add(new ImageFileTask(
                                        currentTime,
                                        currentCreatedTime,
                                        currentContentType,
                                        serverMessageId,
                                        chatid,
                                        senderMid
                                ));
                            }

                            isDh1Invoked = false;
                            isDh1Invoked2 = true;
                            //handleFileRename(db,dbContact); // 即時処理を開始
                        }
                    }
                }
        );

        XposedBridge.hookAllMethods(
                loadPackageParam.classLoader.loadClass(Constants.PhotoSave.className),
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

                            XposedBridge.log(resultStr);
                            serverMessageId = extractField(resultStr, "serverMessageId=");
                            chatid = extractField(resultStr, "chatId=");
                            currentContentType = extractField(resultStr, "contentType=");
                            currentCreatedTime = extractField(resultStr, "createdTimeMillis=");
                            senderMid =extractField(resultStr, "senderMid=");
                            String logMessage = String.format(
                                    "[Message]\nServerMsgID: %s\nChatID: %s\nType: %s\nTime: %s",
                                    serverMessageId, chatid, currentContentType, formatTimestamp(currentCreatedTime)
                            );
                            XposedBridge.log(logMessage);

                            isDh1Invoked = true;
                        }

                    }

                }

        );

        // Ec1.Uのフック
        XposedBridge.hookAllMethods(
                loadPackageParam.classLoader.loadClass(Constants.PhotoSave1.className),
                "invokeSuspend",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (isDh1Invoked2 && "IMAGE".equals(currentContentType)) {
                            isDh1Invoked2 = false; // フラグをリセット
                            handleFileRename(db,dbContact);
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
                            handleFileRename(db,dbContact);
                        }
                    }
                }
        );

//アルバムの処理

//////////////////////////////////////////////
        XposedBridge.hookAllMethods(
                loadPackageParam.classLoader.loadClass(Constants.PhotoSave2.className),
                "a",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {

                        album1 = true; // フラグをリセット
                    }
                }
        );
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
                       mid = extractField(modelInfo, "mid=");
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
                loadPackageParam.classLoader.loadClass(Constants.PhotoSave3.className),
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


                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                final int MAX_RETRIES = 5; // 最大30回試行
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
                                        String SenderNameAlbum = queryDatabase(dbContact, "SELECT profile_name FROM contacts WHERE mid=?", mid);
                                        String tempFileName = currentAlbumTime + ".jpg";
                                        String baseName = currentAlbumFormattedTime.replace(" ", "_").replace(":", "-");
                                        String newFileName = SenderNameAlbum + "-album-"+ baseName + ".jpg";

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
                                           // tryRetry("Temp file not found: " + tempFileName + " (attempt " + (retryCount + 1) + ")");
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
        );



//        XposedBridge.hookAllMethods(
//                loadPackageParam.classLoader.loadClass("nl.c"),
//                "invokeSuspend",
//                new XC_MethodHook() {
//                    @Override
//                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//                        StringBuilder argsString = new StringBuilder("Args: ");
//                        for (int i = 0; i < param.args.length; i++) {
//                            Object arg = param.args[i];
//                            argsString.append("Arg[").append(i).append("]: ")
//                                    .append(arg != null ? arg.toString() : "null")
//                                    .append(", ");
//                        }
//                        Object result = param.getResult();
//                        String resultStr = result != null ? result.toString() : "null";
//                        XposedBridge.log(resultStr);
//                        if (album3) {
//                            album3 = false;
//
//
//
//                            String oid2 = extractField(resultStr, "oid=");
//
//                            if (currentOid.equals(oid2)) {
//                                album4 = true;
//
//                            }
//                        }
//                    }
//                }
//        );

//        XposedBridge.hookAllMethods(
//                loadPackageParam.classLoader.loadClass("kl.h"),
//                "invokeSuspend",
//                new XC_MethodHook() {
//                    @Override
//                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
//                        Object result = param.getResult();
//                        XposedBridge.log("kl.h :: " + result);
//                        processAlbumImageFile();
//
//                    }
//                }
//        );
    }


    private String queryDatabase(SQLiteDatabase db, String query, String... selectionArgs) {
        if (db == null) {
            return null;
        }
        Cursor cursor = db.rawQuery(query, selectionArgs);
        String result = null;
        if (cursor.moveToFirst()) {
            result = cursor.getString(0);
        }
        cursor.close();
        return result;
    }

    private static class ImageFileTask {
        final long tempFileTime;
        final String createdTime;
        final String contentType;
        final String severid;
        final  String chatid;
        final  String senderMid;

        boolean processed;
        int retryCount = 0;

        ImageFileTask(long tempFileTime, String createdTime, String contentType, String Serverid,String chatid,String senderMid) {
            this.tempFileTime = tempFileTime;
            this.createdTime = createdTime;
            this.contentType = contentType;
            this.severid = Serverid;
            this.chatid = chatid;
            this.senderMid = senderMid;
        }
    }



    private final ConcurrentLinkedQueue<ImageFileTask> fileTasks = new ConcurrentLinkedQueue<>();

    private void handleFileRename(SQLiteDatabase db,SQLiteDatabase dbContact) {
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
                        String talkName = queryDatabase(dbContact, "SELECT profile_name FROM contacts WHERE mid=?", chatid);
                        String groupName = queryDatabase(db, "SELECT name FROM groups WHERE id=?", chatid);
                        String name = (groupName != null ? groupName : (talkName != null ? talkName : "No Name" + ":" + ":" + "talkId" + chatid));

                        String SenderName = queryDatabase(dbContact, "SELECT profile_name FROM contacts WHERE mid=?", senderMid);
                        SenderName = SenderName != null ? SenderName : "Self";
                        try {
                            String tempFileName = task.tempFileTime + ".jpg";
                            String newFileName = SenderName +"-"+ formatForFilename(task.createdTime) +"-"+name+  ".jpg";
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
                                    newFileName = SenderName +"-"+formatForFilename(task.createdTime) + "_" + name+ counter + ".jpg";
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



private static boolean isVersionInRange(String versionName, String minVersion, String maxVersion) {
    try {
        int[] currentVersion = parseVersion(versionName);
        int[] minVersionArray = parseVersion(minVersion);
        int[] maxVersionArray = parseVersion(maxVersion);

        boolean isGreaterOrEqualMin = compareVersions(currentVersion, minVersionArray) >= 0;

        boolean isLessThanMax = compareVersions(currentVersion, maxVersionArray) < 0;
        return isGreaterOrEqualMin && isLessThanMax;
    } catch (Exception e) {
        e.printStackTrace();
        return false;
    }
}

private static int[] parseVersion(String version) {
    String[] parts = version.split("\\.");
    int[] versionArray = new int[parts.length];
    for (int i = 0; i < parts.length; i++) {
        versionArray[i] = Integer.parseInt(parts[i]);
    }
    return versionArray;
}


private static int compareVersions(int[] version1, int[] version2) {
    for (int i = 0; i < Math.min(version1.length, version2.length); i++) {
        if (version1[i] < version2[i]) return -1;
        if (version1[i] > version2[i]) return 1;
    }
    return 0;
}
}
