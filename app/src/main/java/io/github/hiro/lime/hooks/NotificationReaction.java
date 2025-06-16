package io.github.hiro.lime.hooks;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

import android.app.Activity;
import android.app.AndroidAppHelper;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.hiro.lime.LimeOptions;
import io.github.hiro.lime.R;

public class NotificationReaction implements IHook {
    private SQLiteDatabase db3 = null;
    private SQLiteDatabase db4 = null;
    private Context context;
    private static final Set<String> sentNotifications = Collections.synchronizedSet(new HashSet<>());

    @Override
    public void hook(LimeOptions limeOptions, XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        if (!limeOptions.NotificationReaction.checked) return;
        XposedHelpers.findAndHookMethod(Application.class, "onCreate", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                context = (Application) param.thisObject;
                Application appContext = (Application) param.thisObject;
                File dbFile3 = appContext.getDatabasePath("naver_line");
                File dbFile4 = appContext.getDatabasePath("contact");
                if (dbFile3.exists() && dbFile4.exists()) {
                    SQLiteDatabase.OpenParams.Builder builder1 = null;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                        builder1 = new SQLiteDatabase.OpenParams.Builder();
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                        builder1.addOpenFlags(SQLiteDatabase.OPEN_READWRITE);
                    }
                    SQLiteDatabase.OpenParams dbParams1 = null;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                        dbParams1 = builder1.build();
                    }


                    SQLiteDatabase.OpenParams.Builder builder2 = null;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                        builder2 = new SQLiteDatabase.OpenParams.Builder();
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                        builder2.addOpenFlags(SQLiteDatabase.OPEN_READWRITE);
                    }
                    SQLiteDatabase.OpenParams dbParams2 = null;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                        dbParams2 = builder2.build();
                    }


                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                        db3 = SQLiteDatabase.openDatabase(dbFile3, dbParams1);
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                        db4 = SQLiteDatabase.openDatabase(dbFile4, dbParams2);
                    }

                }
            }
        });

        XposedBridge.hookAllMethods(
                loadPackageParam.classLoader.loadClass(Constants.RESPONSE_HOOK.className),
                Constants.RESPONSE_HOOK.methodName,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        String paramValue = param.args[1].toString();
                     //   XposedBridge.log(paramValue);
                        if (paramValue.contains("type:NOTIFIED_SEND_REACTION,")) {
                            Class<?> GetHook = XposedHelpers.findClass("com.linecorp.line.fullsync.c", loadPackageParam.classLoader);

                            XposedBridge.hookAllMethods(GetHook, "invokeSuspend", new XC_MethodHook() {
                                @Override
                                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                    String[] operations = paramValue.split("Operation\\(");
                                    for (String operation : operations) {
                                        if (operation.trim().isEmpty()) continue;

                                        String revision = null;
                                        String createdTime = null;
                                        String type = null;
                                        String serverId = null;
                                        String reactionJson = null;
                                        String chatMid = null;
                                        String param3 = null;

                                        String[] parts = operation.split(",");
                                        for (String part : parts) {
                                            part = part.trim();
                                            if (part.startsWith("revision:")) {
                                                revision = part.substring("revision:".length()).trim();
                                            } else if (part.startsWith("createdTime:")) {
                                                createdTime = part.substring("createdTime:".length()).trim();
                                            } else if (part.startsWith("type:")) {
                                                type = part.substring("type:".length()).trim();
                                            } else if (part.startsWith("param1:")) {
                                                serverId = part.substring("param1:".length()).trim();
                                            } else if (part.startsWith("param3:")) {

                                                param3 = part.substring("param3:".length()).trim();
                                                param3 = param3.replaceAll("\\)\\]$", "");
                                            } else if (part.startsWith("param2:")) {
                                                reactionJson = part.substring("param2:".length()).trim();
                                                if (reactionJson.contains("chatMid")) {
                                                    int start = reactionJson.indexOf("chatMid\":\"") + 10;
                                                    int end = reactionJson.indexOf("\"", start);
                                                    chatMid = reactionJson.substring(start, end);
                                                }
                                            }


                                        }

                                        if ("NOTIFIED_SEND_REACTION".equals(type)) {
                                            String content = queryDatabase(db3,
                                                    "SELECT content FROM chat_history WHERE server_id=?",
                                                    serverId);

                                            String media = queryDatabase(db3, "SELECT parameter FROM chat_history WHERE server_id=?", serverId);
                                            media = media != null ? media : "null";


                                            String SendUser = queryDatabase(db3, "SELECT from_mid FROM chat_history WHERE server_id=?", serverId);
                                            SendUser = SendUser != null ? SendUser : "null";

                                            String name = queryDatabase(db4, "SELECT profile_name FROM contacts WHERE mid=?", param3);
                                            name = name != null ? name : "null";

                                            Context moduleContext = AndroidAppHelper.currentApplication().createPackageContext(
                                                    "io.github.hiro.lime", Context.CONTEXT_IGNORE_SECURITY);
                                            XposedBridge.log(chatMid);
                                            String talkName = queryDatabase(db4, "SELECT profile_name FROM contacts WHERE mid=?", chatMid);
                                            if (Objects.equals(talkName, "null")) {
                                                talkName = queryDatabase(db3, "SELECT name FROM groups WHERE id=?", chatMid);
                                            }

                                            XposedBridge.log(talkName);
                                            String mediaDescription = "";
                                            if (media != null) {
                                                if (media.contains("IMAGE")) {
                                                    mediaDescription = moduleContext.getResources().getString(R.string.picture);
                                                } else if (media.contains("video")) {
                                                    mediaDescription = moduleContext.getResources().getString(R.string.video);
                                                } else if (media.contains("STKPKGID")) {
                                                    mediaDescription = moduleContext.getResources().getString(R.string.sticker);
                                                } else if (media.contains("FILE")) {
                                                    mediaDescription = moduleContext.getResources().getString(R.string.file);
                                                } else if (media.contains("LOCATION")) {
                                                    mediaDescription = moduleContext.getResources().getString(R.string.location);
                                                }
                                            } else {
                                                mediaDescription = "null";
                                            }
                                            String finalContent = determineFinalContent(content, mediaDescription);
            //                                XposedBridge.log(content);

                                            String notificationKey = serverId + "|" + param3 + "|" + finalContent + "|" + talkName;

                                            if (!sentNotifications.contains(notificationKey)) {
                                                sentNotifications.add(notificationKey);

                                                generateCustomNotification(
                                                        context,
                                                        name,
                                                        finalContent,
                                                        talkName,
                                                        createdTime
                                                );
                                            }
                                        }
                                    }

                                }
                            });


                        }
                    }
                });
    }
    private void generateCustomNotification(Context context, String name, String content, String talkName, String createdTime) {
        try {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                        "xposed_channel",
                        "Xposed Notifications",
                        NotificationManager.IMPORTANCE_DEFAULT
                );
                nm.createNotificationChannel(channel);
            }
            String formattedTime = "Unknown Time";
            try {
                long timeMillis = Long.parseLong(createdTime);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                formattedTime = sdf.format(new Date(timeMillis));
            } catch (NumberFormatException e) {
                XposedBridge.log("Failed to parse createdTime: " + createdTime);
            }
            String notificationTitle = talkName + " (" + formattedTime + ")";

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "xposed_channel")
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(notificationTitle)
                    .setContentText(name + " reacted to " + content)
                    .setAutoCancel(true);

            nm.notify((int) System.currentTimeMillis(), builder.build());

        } catch (Exception e) {
            XposedBridge.log("Notification error: " + e.getMessage());
        }
    }
    private String determineFinalContent(String content, String mediaDescription) {
        String result;
        if (content != null && !content.isEmpty() && !content.equals("null")) {
            result = content;
        } else {
            result = mediaDescription;
        }

        if (result == null || result.isEmpty() || result.equals("null") || result.equals("NoGetError")) {
            return "null";
        } else {
            return result;
        }
    }
    private String queryDatabase(SQLiteDatabase db, String query, String... selectionArgs) {
        if (db == null || !db.isOpen()) {
            XposedBridge.log("Database not initialized");
            return "null";
        }

        Cursor cursor = null;
        try {
            db.beginTransaction();
            cursor = db.rawQuery(query, selectionArgs);

            String result = "null";
            if (cursor != null && cursor.moveToFirst()) {
                result = cursor.getString(0);
                XposedBridge.log("Query success: " + result);
            }

            db.setTransactionSuccessful();
            return result;
        } catch (Exception e) {
            XposedBridge.log("Query error: " + e.getMessage());
            return "null";
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.endTransaction();
        }
    }
}