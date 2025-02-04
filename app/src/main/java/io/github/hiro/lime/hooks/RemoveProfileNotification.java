package io.github.hiro.lime.hooks;

import static io.github.hiro.lime.Main.limeOptions;

import android.app.Application;
import android.app.Notification;
import android.content.Context;
import android.content.pm.PackageManager;
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
    @Override
    public void hook(LimeOptions limeOptions, XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        if (!limeOptions.RemoveNotification.checked) return;
        Class<?> notificationManagerClass = XposedHelpers.findClass(
                "android.app.NotificationManager", loadPackageParam.classLoader
        );
//        XposedHelpers.findAndHookMethod(notificationManagerClass, "notify",
//                String.class, int.class, Notification.class, new XC_MethodHook() {
//                    @Override
//                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
//
//                        Notification notification = (Notification) param.args[2];
//                        String tag = (String) param.args[0];
//                        int ids = (int) param.args[1];
//                       // logAllNotificationDetails(tag, ids, notification, notification.tickerText != null ? notification.tickerText.toString() : null);
//
//                        if (Objects.equals(notification.category, "call")) {
//                            if (notification.extras != null && "UNKNOWN".equals(notification.extras.getString("android.title"))) {
//
//
//                            }
//
//
//                            return;
//
//                        }
//                    }
//                });
        XposedBridge.hookAllMethods(Application.class, "onCreate", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Application appContext = (Application) param.thisObject;
                if (appContext == null) {
                    return;
                }
                Context moduleContext;
                try {
                    moduleContext = appContext.createPackageContext(
                            "io.github.hiro.lime", Context.CONTEXT_IGNORE_SECURITY);
                } catch (PackageManager.NameNotFoundException ignored) {
                    return;
                }
                File dbFile1 = appContext.getDatabasePath("naver_line");
                File dbFile2 = appContext.getDatabasePath("contact");
                if (dbFile1.exists() && dbFile2.exists()) {

                    SQLiteDatabase.OpenParams.Builder builder1 = new SQLiteDatabase.OpenParams.Builder();
                    builder1.addOpenFlags(SQLiteDatabase.OPEN_READWRITE);
                    SQLiteDatabase.OpenParams dbParams1 = builder1.build();

                    SQLiteDatabase.OpenParams.Builder builder2 = new SQLiteDatabase.OpenParams.Builder();
                    builder2.addOpenFlags(SQLiteDatabase.OPEN_READWRITE);
                    SQLiteDatabase.OpenParams dbParams2 = builder2.build();

                    SQLiteDatabase db1 = SQLiteDatabase.openDatabase(dbFile1, dbParams1);
                    SQLiteDatabase db2 = SQLiteDatabase.openDatabase(dbFile2, dbParams2);

                    RemoveProfileNotifications(loadPackageParam, appContext, db1, db2, moduleContext);
                }
            }

        });
    }
    private void RemoveProfileNotifications(XC_LoadPackage.LoadPackageParam loadPackageParam, Context context, SQLiteDatabase db1, SQLiteDatabase db2, Context moduleContext) throws ClassNotFoundException {
        XposedBridge.hookAllMethods(
                loadPackageParam.classLoader.loadClass(Constants.RESPONSE_HOOK.className),
                Constants.RESPONSE_HOOK.methodName,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!"sync".equals(param.args[0].toString())) return;

                        try {
                            String paramValue = param.args[1].toString();
                            String[] operations = paramValue.split("Operation\\(");
                            for (String operation : operations) {
                                if (operation.trim().isEmpty()) continue;

                                String type = null;
                                String param1 = null;
                                String param2 = null;
                                String[] parts = operation.split(",");
                                for (String part : parts) {
                                    part = part.trim();
                                    if (part.startsWith("type:")) {
                                        type = part.substring("type:".length()).trim();
                                    } else if (part.startsWith("param1:")) {
                                        param1 = part.substring("param1:".length()).trim();
                                    } else if (part.startsWith("param2:")) {
                                        param2 = part.substring("param2:".length()).trim();
                                    }
                                }
                                if ("NOTIFIED_UPDATE_PROFILE".equals(type)) {
                                    XposedBridge.log(paramValue);
                                    Cursor cursor = db2.rawQuery("SELECT mid, contact_type, profile_updated_time_millis, profile_name FROM contacts WHERE mid=?", new String[]{param1});

                                    Object operationObject = param.args[1].getClass().getDeclaredField("a").get(param.args[1]);
                                    if (operationObject != null) {
                                        Field operationResponseField = operationObject.getClass().getSuperclass().getDeclaredField("value_");
                                        operationResponseField.setAccessible(true);
                                        Object operationResponse = operationResponseField.get(operationObject);
                                        if (operationResponse != null) {
                                            ArrayList<?> operationList = (ArrayList<?>) operationResponse.getClass().getDeclaredField("a").get(operationResponse);
                                            Map<String, String> profileMap = new HashMap<>(); // midとprofile_nameのマップ

                                            for (Object op : operationList) {
                                                Field typeField = op.getClass().getDeclaredField("c");
                                                typeField.setAccessible(true);
                                                Object typeFieldValue = typeField.get(op);
                                                if ("NOTIFIED_UPDATE_PROFILE".equals(typeFieldValue.toString())) {
                                                    typeField.set(op, typeFieldValue.getClass().getMethod("valueOf", String.class).invoke(typeFieldValue, "DUMMY"));
                                                }

                                                if ("8".contains(param2)) {
                                                    if (cursor != null) {
                                                        try {
                                                            if (cursor.moveToFirst()) {
                                                                String mid = cursor.getString(cursor.getColumnIndex("mid"));
                                                                String profileName = cursor.getString(cursor.getColumnIndex("profile_name"));

                                                                // midとprofile_nameをマップに追加
                                                                profileMap.put(mid, profileName);
                                                                saveProfilesToFile(profileMap);
                                                                // データを削除
                                                                int rowsDeleted = db2.delete("contacts", "mid=?", new String[]{mid});
                                                            }
                                                        } finally {
                                                            cursor.close();
                                                        }
                                                    }
                                                }
                                            }

                                        }
                                    }

                            }
                            }
                        } catch (Exception e) {
                            XposedBridge.log("RemoveProfileNotification: 予期しないエラー - " + e.getMessage());
                        }
                    }
                });



    }

    private void saveProfilesToFile(Map<String, String> profileMap) {
        File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "LimeBackup");
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File file = new File(dir, "profiles.txt");
        Map<String, String> existingProfiles = new HashMap<>();

        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(",");
                    if (parts.length == 2) {
                        existingProfiles.put(parts[0], parts[1]);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        for (Map.Entry<String, String> entry : profileMap.entrySet()) {
            String mid = entry.getKey();
            String profileName = entry.getValue();
            existingProfiles.put(mid, profileName);
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            for (Map.Entry<String, String> entry : existingProfiles.entrySet()) {
                String mid = entry.getKey();
                String profileName = entry.getValue();
                writer.write(mid + "," + profileName);
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
//    private void logAllNotificationDetails(String method, int ids, Notification notification, String tag) {
//        XposedBridge.log(method + " called. ID: " + ids + (tag != null ? ", Tag: " + tag : ""));
//        XposedBridge.log("Notification Icon: " + notification.icon);
//        XposedBridge.log("Notification When: " + notification.when);
//        XposedBridge.log("Notification Flags: " + notification.flags);
//        XposedBridge.log("Notification Priority: " + notification.priority);
//        XposedBridge.log("Notification Category: " + notification.category);
//        if (notification.extras != null) {
//            Bundle extras = notification.extras;
//            XposedBridge.log("Notification Extras:");
//            for (String key : extras.keySet()) {
//                Object value = extras.get(key);
//                XposedBridge.log("  " + key + ": " + (value != null ? value.toString() : "null"));
//            }
//        } else {
//            XposedBridge.log("Notification has no extras.");
//        }
//
//        if (notification.actions != null) {
//            XposedBridge.log("Notification Actions:");
//            for (int i = 0; i < notification.actions.length; i++) {
//                Notification.Action action = notification.actions[i];
//                XposedBridge.log("  Action " + i + ": " +
//                        "Title=" + action.title +
//                        ", Intent=" + action.actionIntent);
//            }
//        } else {
//            //XposedBridge.log("No actions found.");
//        }
//
//        // その他の情報
//        XposedBridge.log("Notification Visibility: " + notification.visibility);
//        XposedBridge.log("Notification Color: " + notification.color);
//        XposedBridge.log("Notification Group: " + notification.getGroup());
//        XposedBridge.log("Notification SortKey: " + notification.getSortKey());
//        XposedBridge.log("Notification Sound: " + notification.sound);
//        XposedBridge.log("Notification Vibrate: " + (notification.vibrate != null ? "Yes" : "No"));
//    }
}

