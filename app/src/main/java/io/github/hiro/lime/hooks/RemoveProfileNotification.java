package io.github.hiro.lime.hooks;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.hiro.lime.LimeOptions;

public class RemoveProfileNotification implements IHook {
    @Override
    public void hook(LimeOptions limeOptions, XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        if (!limeOptions.RemoveNotification.checked) return;

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
                                String[] parts = operation.split(",");
                                for (String part : parts) {
                                    part = part.trim();
                                    if (part.startsWith("type:")) {
                                        type = part.substring("type:".length()).trim();
                                    } else if (part.startsWith("param1:")) {
                                        param1 = part.substring("param1:".length()).trim();
                                    }
                                }
                                if ("NOTIFIED_UPDATE_PROFILE".equals(type)) {
                                    XposedBridge.log("取得したparam1: " + param1);
                                    Cursor cursor = db2.rawQuery("SELECT mid, contact_type, profile_updated_time_millis, profile_name FROM contacts WHERE mid=?", new String[]{param1});
                                    if (cursor != null) {
                                        try {
                                            if (cursor.moveToFirst()) {
                                                String mid = cursor.getString(cursor.getColumnIndex("mid"));
                                                int rowsDeleted = db2.delete("contacts", "mid=?", new String[]{mid});
                                            }
                                        } finally {
                                            cursor.close();
                                        }
                                    }
                                    Object operationObject = param.args[1].getClass().getDeclaredField("a").get(param.args[1]);
                                    if (operationObject != null) {
                                        Field operationResponseField = operationObject.getClass().getSuperclass().getDeclaredField("value_");
                                        operationResponseField.setAccessible(true);
                                        Object operationResponse = operationResponseField.get(operationObject);
                                        if (operationResponse != null) {
                                            ArrayList<?> operationList = (ArrayList<?>) operationResponse.getClass().getDeclaredField("a").get(operationResponse);
                                            for (Object op : operationList) {
                                                Field typeField = op.getClass().getDeclaredField("c");
                                                typeField.setAccessible(true);
                                                Object typeFieldValue = typeField.get(op);
                                                if ("NOTIFIED_UPDATE_PROFILE".equals(typeFieldValue.toString())) {
                                                    typeField.set(op, typeFieldValue.getClass().getMethod("valueOf", String.class).invoke(typeFieldValue, "DUMMY"));
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
}

