package io.github.hiro.lime.hooks;

import static de.robv.android.xposed.XposedBridge.log;

import android.app.AndroidAppHelper;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.hiro.lime.LimeOptions;
import io.github.hiro.lime.R;

public class DisableSilentMessage implements IHook {
    private SQLiteDatabase db3 = null;


    private static  Context context; // Static context to be shared
    @Override
    public void hook(LimeOptions limeOptions, XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        if (!limeOptions.DisableSilentMessage.checked) return;
        XposedHelpers.findAndHookMethod(Application.class, "onCreate", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                context = (Application) param.thisObject;
                Application appContext = (Application) param.thisObject;
                initDatabase(appContext);
            }
        });

        XposedBridge.hookAllMethods(
                loadPackageParam.classLoader.loadClass(Constants.RESPONSE_HOOK.className),
                Constants.RESPONSE_HOOK.methodName,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!"sync".equals(param.args[0].toString())) return;

                            try {
                            Object wrapper = param.args[1].getClass().getDeclaredField("a").get(param.args[1]);
                            Field operationResponseField = wrapper.getClass().getSuperclass().getDeclaredField("value_");
                            operationResponseField.setAccessible(true);
                            Object operationResponse = operationResponseField.get(wrapper);
                            if (operationResponse == null) return;

                            ArrayList<?> operations = (ArrayList<?>) operationResponse.getClass().getDeclaredField("a").get(operationResponse);
                            if (operations == null) return;

                            for (Object operation : operations) {
                                Field[] fields = operation.getClass().getDeclaredFields();
                                for (Field field : fields) {
                                    field.setAccessible(true);
                                    try {
                                        Object value = field.get(operation);
                                      //  XposedBridge.log("Field [" + field.getName() + "] = " + (value != null ? value.toString() : "null"));
                                        try {
                                            Method valueOfMethod = operation.getClass().getMethod("valueOf");
                                            Object valueOfResult = valueOfMethod.invoke(operation);
                                            XposedBridge.log("valueOf() result = " + (valueOfResult != null ? valueOfResult.toString() : "null"));
                                        } catch (NoSuchMethodException e) {
                                        }
                                    } catch (Exception e) {
                                     //   XposedBridge.log("Error accessing field " + field.getName() + ": " + e.getMessage());
                                    }
                                }
                                Field typeField = operation.getClass().getDeclaredField("c");
                                typeField.setAccessible(true);
                                Object type = typeField.get(operation);

                          if ("RECEIVE_MESSAGE".equals(type.toString())) {
                              Field fieldF = operation.getClass().getDeclaredField("f");

                              fieldF.setAccessible(true);
                              fieldF.set(operation, null);
                              Object message = operation.getClass().getDeclaredField("j").get(operation);
                              if (message == null) continue;

                              Field metadataField = message.getClass().getDeclaredField("k");
                              metadataField.setAccessible(true);
                              Map<String, String> contentMetadata = (Map<String, String>) metadataField.get(message);
                              String serverId = "";
                              String alertStatus = "";
                              Field[] messageFields = message.getClass().getDeclaredFields();
                              for (Field field : messageFields) {
                                  field.setAccessible(true);
                                  Object value = field.get(message);

                                  if ("d".equals(field.getName())) {
                                      String rawValue = String.valueOf(value).trim();
                                      serverId = rawValue.replaceAll("[^0-9]", "");
                                  }
                                  if ("k".equals(field.getName())) {
                                      alertStatus = String.valueOf(value);
                                  }
                              }
                              if ("{NOTIFICATION_DISABLED=true, app_extension_type=null, e2eeVersion=2}".contains(alertStatus)) {

                                  if (contentMetadata != null) {
                                      contentMetadata.remove("NOTIFICATION_DISABLED");
                                      contentMetadata.remove("app_extension_type");
                                      metadataField.set(message, contentMetadata);


                                      String content = queryDatabase(db3,
                                              "SELECT content FROM chat_history WHERE server_id=?",
                                              serverId);

                                      String media = queryDatabase(db3, "SELECT parameter FROM chat_history WHERE server_id=?", serverId);
                                      media = media != null ? media : "null";
                                      Context moduleContext = AndroidAppHelper.currentApplication().createPackageContext(
                                              "io.github.hiro.lime", Context.CONTEXT_IGNORE_SECURITY);

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
                                      XposedBridge.log(content);
                                      generateCustomNotification(
                                              context,
                                              "サイレントメッセージが送信されました",
                                              "",
                                              serverId
                                      );
                                  }
                              }

                          }
                                }
                        } catch (Exception e) {
                            XposedBridge.log("Error in hook: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                }
        );




    }


    private synchronized void initDatabase(Context context) {
        try {
            File dbFile = new File(context.getDatabasePath("naver_line").getPath());
            if (dbFile.exists()) {
                db3 = SQLiteDatabase.openDatabase(
                        dbFile.getPath(),
                        null,
                        SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS
                );
                XposedBridge.log("Database initialized successfully");
            } else {
                XposedBridge.log("Database file not found");
            }
        } catch (SQLiteException e) {
            XposedBridge.log("DB init error: " + e.getMessage());
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

    private void generateCustomNotification(Context context, String title, String content, String serverId) {
    try {
        NotificationManager nm = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "xposed_channel",
                    "Xposed Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            nm.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "xposed_channel")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(content)
                .setAutoCancel(true);

        nm.notify((int) System.currentTimeMillis(), builder.build());

    } catch (Exception e) {
        XposedBridge.log("Notification error: " + e.getMessage());
    }

    }

}
