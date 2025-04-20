package io.github.hiro.lime.hooks;

import android.app.AndroidAppHelper;
import android.app.Application;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;

import java.io.File;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.hiro.lime.LimeOptions;
import io.github.hiro.lime.R;

public class BlockCheck implements IHook {

    @Override
    public void hook(LimeOptions limeOptions, XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        if (!limeOptions.BlockCheck.checked) return;
        XposedBridge.hookAllMethods(Application.class, "onCreate", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Application appContext = (Application) param.thisObject;
                if (appContext == null) {
                    return;
                }

                File dbFile1 = appContext.getDatabasePath("events");
                File dbFile2 = appContext.getDatabasePath("contact");
                if (dbFile1.exists() && dbFile2.exists()) {

                    SQLiteDatabase.OpenParams.Builder builder1 = null;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                        builder1 = new SQLiteDatabase.OpenParams.Builder();

                        builder1.addOpenFlags(SQLiteDatabase.OPEN_READWRITE);
                        SQLiteDatabase.OpenParams dbParams1 = builder1.build();

                        SQLiteDatabase.OpenParams.Builder builder2 = new SQLiteDatabase.OpenParams.Builder();
                        builder2.addOpenFlags(SQLiteDatabase.OPEN_READWRITE);
                        SQLiteDatabase.OpenParams dbParams2 = builder2.build();
                        SQLiteDatabase db2 = SQLiteDatabase.openDatabase(dbFile2, dbParams2);


                        XposedBridge.hookAllMethods(
                                loadPackageParam.classLoader.loadClass(Constants.RESPONSE_HOOK.className),
                                Constants.RESPONSE_HOOK.methodName,

                                new XC_MethodHook() {

                                    @Override
                                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                        String paramValue = param.args[1].toString();
                                        if (paramValue.contains("sync_result(")) {
                                            Context moduleContext = AndroidAppHelper.currentApplication().createPackageContext(
                                                    "io.github.hiro.lime", Context.CONTEXT_IGNORE_SECURITY);


                                            String operationsPart = paramValue.split("operations:\\[")[1].split("\\]")[0];
                                            String[] operations = operationsPart.split("\\)\\s*,?");
                                            for (String op : operations) {
                                                if (op.trim().isEmpty()) continue;
                                                String operation = op.replaceFirst("^Operation\\(", "").trim();
                                                operation = operation.replaceAll("\\)$", "");
                                                String revision = null;
                                                String createdTime = null;
                                                String type = null;
                                                String reqSeq = null;
                                                String param1 = null;
                                                String param2 = null;
                                                String[] parts = operation.split(",\\s*(?=revision:|createdTime:|type:|reqSeq:|param1:|param2:)");
                                                for (String part : parts) {
                                                    part = part.trim();
                                                    if (part.startsWith("revision:")) {
                                                        revision = part.substring("revision:".length()).trim();
                                                    } else if (part.startsWith("createdTime:")) {
                                                        createdTime = part.substring("createdTime:".length()).trim();
                                                    } else if (part.startsWith("type:")) {
                                                        type = part.substring("type:".length()).trim();
                                                    } else if (part.startsWith("reqSeq:")) {
                                                        reqSeq = part.substring("reqSeq:".length()).trim();
                                                    } else if (part.startsWith("param1:")) {
                                                        param1 = part.substring("param1:".length()).trim();
                                                    } else if (part.startsWith("param2:")) {
                                                        param2 = part.substring("param2:".length()).trim();
                                                        int endIndex = param2.indexOf(']');
                                                        if (endIndex != -1) {
                                                            param2 = param2.substring(0, endIndex + 1);
                                                        }
                                                    }
                                                }
                                                if ("NOTIFIED_CONTACT_CALENDAR_EVENT".equals(type)) {
                                                    XposedBridge.log("Revision: " + revision);
//                                                XposedBridge.log("Created Time: " + createdTime);
//                                                XposedBridge.log("Request Sequence: " + reqSeq);
//                                                XposedBridge.log("Param1: " + param1);
//                                                XposedBridge.log("Param2: " + param2);
                                                    if (param2 != null && param2.contains("\"HIDE\"")) {
                                                        if (param1 != null) {
                                                            String talkName = queryDatabase(db2, param1);
                                                            if (talkName != null) {
                                                                String customMessage = talkName + "[" + moduleContext.getResources().getString(R.string.UserBlocked) + "]";
                                                                updateProfileName(db2, param1, customMessage);
                                                                XposedBridge.log("Updated profile name to: " + customMessage);
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                });

                    }
                }
            }
        });
    }
    private void updateProfileName(SQLiteDatabase db, String mid, String newProfileName) {
        if (db == null) {
            return;
        }

        ContentValues values = new ContentValues();
        values.put("overridden_name", newProfileName); // Set the new profile name

        // Update the database
        int rowsAffected = db.update("contacts", values, "mid = ?", new String[]{mid});

        // Log the result
        if (rowsAffected > 0) {
            //XposedBridge.log("Successfully updated overridden_name for mid: " + mid);
        } else {
            //XposedBridge.log("No record found to update for mid: " + mid);
        }
    }
    private String queryDatabase(SQLiteDatabase db, String... selectionArgs) {
        if (db == null) {
            return null;
        }
        Cursor cursor = db.rawQuery("SELECT profile_name FROM contacts WHERE mid=?", selectionArgs);
        String result = null;
        if (cursor.moveToFirst()) {
            result = cursor.getString(0);
        }
        cursor.close();
        return result;
    }
}