package io.github.hiro.lime.hooks;

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

import androidx.core.content.ContextCompat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Objects;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.hiro.lime.LimeOptions;

public class PhotoAddNotification implements IHook {

    private static final int MAX_RETRIES = 20;
    private static final String COPY_ACTION = "io.github.hiro.lime.COPY_TEXT";
    private static final String TAG = "LimeModule";
    private boolean isReceiverRegistered = false;
    private static final long RETRY_DELAY = 1000;
    private static boolean isHandlingNotification = false;

    @Override
    public void hook(LimeOptions limeOptions, XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        if (!limeOptions.PhotoAddNotification.checked) return;

        XposedHelpers.findAndHookMethod(Application.class, "onCreate", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                Application appContext = (Application) param.thisObject;

                if (appContext == null) {
                    ////XposedBridge.log("Application context is null!");
                    return;
                }

                File dbFile1 = appContext.getDatabasePath("naver_line");
                File dbFile2 = appContext.getDatabasePath("contact");

                if (dbFile1.exists() && dbFile2.exists()) {
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

                    SQLiteDatabase db1 = null;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                        db1 = SQLiteDatabase.openDatabase(dbFile1, dbParams1);
                    }
                    SQLiteDatabase db2 = null;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                        db2 = SQLiteDatabase.openDatabase(dbFile2, dbParams2);
                    }

                    hookNotificationMethods(loadPackageParam, appContext, db1, db2);
                }
            }
        });
    }

    private void hookNotificationMethods(XC_LoadPackage.LoadPackageParam loadPackageParam,
                                         Context context, SQLiteDatabase db1, SQLiteDatabase db2) {
        Class<?> notificationManagerClass = XposedHelpers.findClass(
                "android.app.NotificationManager", loadPackageParam.classLoader
        );
        XposedHelpers.findAndHookMethod(notificationManagerClass, "notify",
                String.class, int.class, Notification.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {

                        Notification notification = (Notification) param.args[2];
                        String tag = (String) param.args[0];
                        int ids = (int) param.args[1];

                        if (limeOptions.CansellNotification.checked) {
                            // Notification Extrasを取得
                            Bundle extras = notification.extras;
                            String userName = extras.getString("android.title");
                            String groupName = extras.getString("android.subText");

                            if (isMatchingSetting(userName, groupName)) {

                            } else {

                                param.setResult(null);
                                return;
                            }
                        }

                        if (Objects.equals(notification.category, "call")) {
                            return;
                        }

                        if (limeOptions.GroupNotification.checked) {
                            if (!(param.args[0] == null)) {
                                handleNotificationHook(context, db1, db2, param, notification, true, loadPackageParam);
                            }
                        } else {
                            if (param.args[0] == null) {
                                param.setResult(null);
                                return;
                            }
                            //logAllNotificationDetails(tag, ids, notification, notification.tickerText != null ? notification.tickerText.toString() : null);
                            handleNotificationHook(context, db1, db2, param, notification, true, loadPackageParam);
                        }
                    }
                });

    }


    private void handleNotificationHook(Context context, SQLiteDatabase db1, SQLiteDatabase db2, XC_MethodHook.MethodHookParam param, Notification notification, boolean hasTag, XC_LoadPackage.LoadPackageParam loadPackageParam) {
        if (isHandlingNotification) {
            return;
        }

        isHandlingNotification = true;


        try {
            Notification originalNotification = hasTag ? (Notification) param.args[2] : (Notification) param.args[1];
            String title = getNotificationTitle(originalNotification);


            String originalText = getNotificationText(originalNotification);
            Notification newNotification = originalNotification;


            if (title == null) {
                return;
            }

            if (notification.extras != null) {
                Bundle extras = notification.extras;
                //XposedBridge.log("Notification Extras:");
//                for (String key : extras.keySet()) {
//                    Object value = extras.get(key);
//                    //XposedBridge.log("  " + key + ": " + (value != null ? value.toString() : "null"));
//                }
                if (extras.containsKey("line.sticker.url")) {
                    String stickerUrl = extras.getString("line.sticker.url");
                    if (stickerUrl != null) {
                        Bitmap stickerBitmap = downloadBitmapFromUrl(stickerUrl);
                        if (stickerBitmap != null) {
                            newNotification = createNotificationWithImageFromBitmap(context, originalNotification, stickerBitmap, originalText);
                        }
                    }
                }
            }

            if (originalText != null && (originalText.contains("写真を送信しました")
                    || originalText.contains("sent a photo") || originalText.contains("傳送了照片"))) {

                Bundle extras = notification.extras;

                if (extras.containsKey("line.message.id")) {
                    String TalkId = extras.getString("line.message.id");

                    int retryCount = 0;
                    boolean foundValidData = false;
                    //XposedBridge.log(TalkId);
                    while (retryCount < MAX_RETRIES && !foundValidData) {
                        if (TalkId != null) {
                            String chatId = queryDatabase(db1, "SELECT chat_id FROM chat_history WHERE server_id =?", TalkId);
                            String id = queryDatabase(db1, "SELECT id FROM chat_history WHERE server_id =?", TalkId);

                            if (chatId != null && id != null) {
                                foundValidData = true;
                                // XposedBridge.log("Found Chat ID: " + chatId + ", Message ID: " + id);

                                File latestFile = getFileWithId(chatId, id);
                                if (latestFile == null) {
                                    // XposedBridge.log("No file found for Chat ID: " + chatId + " and ID: " + id);
                                    return;
                                }

                                Bitmap bitmap = loadBitmapFromFile(latestFile);
                                if (bitmap == null) {
                                    // XposedBridge.log("Failed to load bitmap from file: " + latestFile.getAbsolutePath());
                                    return;
                                }


                                newNotification = createNotificationWithImageFromFile(context, originalNotification, latestFile, originalText);
                                // XposedBridge.log("Created new notification with image.");
                                if (hasTag) {
                                    param.args[2] = newNotification;
                                } else {
                                    param.args[1] = newNotification;
                                }

                            } else {
                                // Log and retry if either chatId or id is null
                                // XposedBridge.log("Chat ID or Message ID is null, retrying...");
                                retryCount++;
                                try {
                                    Thread.sleep(RETRY_DELAY); // Wait before retrying
                                } catch (InterruptedException e) {
                                    // XposedBridge.log("Retry interrupted.");
                                }
                            }
                        } else {
                            //XposedBridge.log("TalkId is null, retrying...");
                            retryCount++;
                            try {
                                Thread.sleep(RETRY_DELAY); // Wait before retrying
                            } catch (InterruptedException e) {
                                //XposedBridge.log("Retry interrupted.");
                            }
                        }
                    }

                    if (!foundValidData) {
                        //XposedBridge.log("Failed to retrieve valid Chat ID and Message ID after " + MAX_RETRIES + " retries.");
                    }
                }
            }
            if (limeOptions.AddCopyAction.checked) {
                if (!isReceiverRegistered) {
                    //Log.d(TAG, "Attempting to register receiver");
                    registerReceiver(loadPackageParam);
                    isReceiverRegistered = true;
                }

                CharSequence message = extractMessageContent(originalNotification);
                //Log.d(TAG, "Extracted message: " + (message != null ? message : "NULL"));

                if (message != null) {
                    PendingIntent pendingIntent = createPendingIntent(context, message);
                    if (pendingIntent == null) {
                        Log.e(TAG, "Failed to create PendingIntent");
                        return;
                    }

                    newNotification = addCopyAction(context, newNotification, originalText, pendingIntent);
                    param.args[2] = newNotification;
                    //Log.d(TAG, "Notification modified successfully");
                }
            }
            int randomNotificationId = (int) System.currentTimeMillis();
            if (limeOptions.original_ID.checked) {
                int originalId;
                String tag = null;
                if (hasTag) {
                    tag = (String) param.args[0];
                    originalId = (Integer) param.args[1];
                } else {
                    originalId = (Integer) param.args[0];
                }


                NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                if (notificationManager != null) {
                    if (hasTag) {
                        notificationManager.notify(tag, originalId, newNotification);
                        param.setResult(null);
                    } else {
                        notificationManager.notify(originalId, newNotification);
                        param.setResult(null);
                    }
                }
            } else {
                NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                if (notificationManager != null) {
                    if (hasTag) {
                        String tag = (String) param.args[0];
                        notificationManager.notify(tag, randomNotificationId, newNotification);
                        param.setResult(null);
                    } else {
                        notificationManager.notify(randomNotificationId, newNotification);
                        param.setResult(null);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Critical error in notification processing", e);
        } finally {
            param.setResult(null);
            isHandlingNotification = false;
        }
    }


    private Bitmap loadBitmapFromFile(File file) {
        if (!file.exists()) {
            return null;
        }
        return BitmapFactory.decodeFile(file.getAbsolutePath());
    }

    private Notification createNotificationWithImageFromFile(Context context, Notification original, File imageFile, String originalText) {
        Bitmap bitmap = loadBitmapFromFile(imageFile);
        if (bitmap == null) {
            return original;
        }
        Notification.Builder builder = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder = Notification.Builder.recoverBuilder(context, original)
                    .setStyle(new Notification.BigPictureStyle()
                            .bigPicture(bitmap)
                            .bigLargeIcon(bitmap)
                            .setSummaryText(originalText));
        }
        return builder.build();

    }


    private Bitmap downloadBitmapFromUrl(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            return BitmapFactory.decodeStream(input);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private Notification createNotificationWithImageFromBitmap(Context context, Notification original, Bitmap bitmap, String originalText) {
        Notification.BigPictureStyle bigPictureStyle = new Notification.BigPictureStyle()
                .bigPicture(bitmap)
                .bigLargeIcon(bitmap)
                .setSummaryText(originalText);

        Notification.Builder builder = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder = Notification.Builder.recoverBuilder(context, original)
                    .setStyle(bigPictureStyle);
        }

        return builder.build();
    }

    private File getFileWithId(String chatId, String id) {

        // XposedBridge.log("getFileWithId: Searching for file with id: " + id + " in chat: " + chatId);

        File messagesDir = new File(Environment.getExternalStorageDirectory(),
                "/Android/data/jp.naver.line.android/files/chats/" + chatId + "/messages");

        if (!messagesDir.exists() || !messagesDir.isDirectory()) {
            //XposedBridge.log("getFileWithId: Messages directory does not exist or is not a directory.");
            return null;
        }


        long waitTimeMillis = 1000; // デフォルトの待機時間（1秒）

        //XposedBridge.log("getFileWithId: Wait time read from file: " + waitTimeMillis);
        // 待機
        try {
            // XposedBridge.log("getFileWithId: Sleeping for " + waitTimeMillis + " milliseconds.");
            Thread.sleep(waitTimeMillis);
        } catch (InterruptedException e) {
            //XposedBridge.log("getFileWithId: Thread interrupted during sleep.");
            return null;
        }
        File[] files = messagesDir.listFiles((dir, name) -> !name.endsWith(".downloading"));
        if (files == null || files.length == 0) {
            // XposedBridge.log("getFileWithId: No files found in the messages directory.");
            return null;
        }

        // Iterate through the files and find the one containing the id
        for (File file : files) {
            // XposedBridge.log("getFileWithId: Checking file: " + file.getName());
            if (file.getName().contains(id)) {
                // XposedBridge.log("getFileWithId: Found file containing id: " + file.getName());
                return file; // Return the first file containing the id
            }
        }
        // XposedBridge.log("getFileWithId: No file with the specified id found.");
        return null; // No file with the specified id found
    }


    private String queryDatabase(SQLiteDatabase db, String query, String... selectionArgs) {
        Cursor cursor = db.rawQuery(query, selectionArgs);
        String result = null;
        if (cursor.moveToFirst()) {
            result = cursor.getString(0);
        }
        cursor.close();
        return result;
    }

    private String getNotificationTitle(Notification notification) {
        if (notification.extras != null) {
            return notification.extras.getString(Notification.EXTRA_TITLE);
        }
        return null;
    }

    private String getNotificationText(Notification notification) {
        if (notification.extras != null) {
            return notification.extras.getString(Notification.EXTRA_TEXT);
        }
        return null;
    }

    private boolean isMatchingSetting(String userName, String groupName) {
        // Notification_Setting.txtから設定を読み込む
        File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "LimeBackup/Setting");
        File file = new File(dir, "Notification_Setting.txt");

        if (!file.exists()) {
            return false; // ファイルが存在しない場合は一致しない
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // 各行を解析
                String[] parts = line.split(", ");
                if (parts.length == 2) {
                    String storedGroupName = parts[0].replace("グループ名: ", "").trim();
                    String storedUserName = parts[1].replace("ユーザー名: ", "").trim();

                    // 一致するか確認
                    if (storedGroupName.equals(groupName) && storedUserName.equals(userName)) {
                        return true; // 一致する場合
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            // エラーハンドリング
        }

        return false; // 一致しない場合
    }

    private CharSequence extractMessageContent(Notification notification) {
        try {
            Bundle extras = notification.extras;
            //Log.d(TAG, "Notification extras keys: " + extras.keySet().toString());
            CharSequence message = extras.getCharSequence("android.line.metadata.MESSAGE_CONTENT");
            if (message != null) {
                //Log.d(TAG, "Found LINE specific message content");
                return message;
            }
            Parcelable[] messages = extras.getParcelableArray("android.line.metadata.MESSAGES");
            if (messages != null && messages.length > 0) {
                //Log.d(TAG, "Processing group notification with " + messages.length + " messages");
                Bundle firstMessage = (Bundle) messages[0];
                return firstMessage.getCharSequence("content");
            }
            return extras.getCharSequence(Notification.EXTRA_TEXT);
        } catch (Exception e) {
            Log.e(TAG, "Error extracting message content", e);
            return null;
        }
    }

    private Context resolveContext(XC_MethodHook.MethodHookParam param) {
        try {
            Context context = AndroidAppHelper.currentApplication();
            if (context == null) {
                //Log.d(TAG, "Falling back to alternative context resolution");
                context = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
            }
            return context;
        } catch (Exception e) {
            Log.e(TAG, "Context resolution failed", e);
            return null;
        }
    }

    private PendingIntent createPendingIntent(Context context, CharSequence message) {
        try {
            Intent copyIntent = new Intent(COPY_ACTION);
            copyIntent.putExtra("text_to_copy", message.toString());
            copyIntent.setPackage(context.getPackageName());

            //Log.d(TAG, "Creating PendingIntent with flags: "
            // + "FLAG_UPDATE_CURRENT|FLAG_IMMUTABLE");

            return PendingIntent.getBroadcast(
                    context,
                    0,
                    copyIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
        } catch (Exception e) {
            Log.e(TAG, "PendingIntent creation failed", e);
            return null;
        }
    }

    private Notification addCopyAction(Context context, Notification original, String text, PendingIntent pendingIntent) {
        try {
            Notification.Builder builder = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                builder = Notification.Builder.recoverBuilder(context, original)
                        .addAction(new Notification.Action.Builder(
                                null,
                                "Copy Text",
                                pendingIntent
                        ).build());
            }

            return builder.build();
        } catch (Exception e) {
            Log.e("CopyFeature", "Failed to add copy action", e);
            return original;
        }
    }

    private void registerReceiver(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Context context = AndroidAppHelper.currentApplication();
            if (context == null) {
                Log.e(TAG, "Context is null during receiver registration");
                return;
            }

            IntentFilter filter = new IntentFilter(COPY_ACTION);
            filter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);

            //Log.d(TAG, "Registering receiver with filter: " + COPY_ACTION);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(
                        new CopyTextReceiver(),
                        filter,
                        Context.RECEIVER_NOT_EXPORTED
                );
            } else {
                ContextCompat.registerReceiver(context, new CopyTextReceiver(), filter, ContextCompat.RECEIVER_NOT_EXPORTED);
            }
            //Log.d(TAG, "Receiver registration successful");
        } catch (Exception e) {
            Log.e(TAG, "Receiver registration failed", e);
        }
    }

    public static class CopyTextReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            //Log.d(TAG, "=== BroadcastReceiver triggered ===");
            //Log.d(TAG, "Action: " + intent.getAction());
            //Log.d(TAG, "Package: " + intent.getPackage());
            //Log.d(TAG, "Extras: " + intent.getExtras());

            try {
                if (!COPY_ACTION.equals(intent.getAction())) {
                    //Log.d(TAG, "Ignoring unrelated action");
                    return;
                }

                final String textToCopy = intent.getStringExtra("text_to_copy");
                if (textToCopy == null || textToCopy.isEmpty()) {
                    Log.e(TAG, "Empty text received");
                    return;
                }

                //Log.d(TAG, "Attempting to copy text: " + textToCopy);

                new Handler(Looper.getMainLooper()).post(() -> {
                    try {
                        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                        if (clipboard == null) {
                            Log.e(TAG, "Clipboard service unavailable");
                            return;
                        }

                        //Log.d(TAG, "Clipboard service obtained");

                        ClipData clip = ClipData.newPlainText("LINE Message", textToCopy);
                        clipboard.setPrimaryClip(clip);

                        //Log.d(TAG, "Clipboard operation completed");
                        Toast.makeText(context, "✓ Copied to clipboard", Toast.LENGTH_SHORT).show();

                    } catch (SecurityException e) {
                        Log.e(TAG, "Security exception: " + e.getMessage());
                    } catch (Exception e) {
                        Log.e(TAG, "Unexpected error in UI thread", e);
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Global error in receiver", e);
            }
        }

    }
    private void logAllNotificationDetails(String method, int ids, Notification notification, String tag) {
        // XposedBridge.log(method + " called. ID: " + ids + (tag != null ? ", Tag: " + tag : ""));
        // XposedBridge.log("Notification Icon: " + notification.icon);
        // XposedBridge.log("Notification When: " + notification.when);
        // XposedBridge.log("Notification Flags: " + notification.flags);
        // XposedBridge.log("Notification Priority: " + notification.priority);
        // XposedBridge.log("Notification Category: " + notification.category);
        if (notification.extras != null) {
            Bundle extras = notification.extras;
            // XposedBridge.log("Notification Extras:");
            for (String key : extras.keySet()) {
                Object value = extras.get(key);
                // XposedBridge.log("  " + key + ": " + (value != null ? value.toString() : "null"));
            }
        } else {
            // XposedBridge.log("Notification has no extras.");
        }

        if (notification.actions != null) {
            // XposedBridge.log("Notification Actions:");
            for (int i = 0; i < notification.actions.length; i++) {
                Notification.Action action = notification.actions[i];
                // XposedBridge.log("  Action " + i + ": " +
//                        "Title=" + action.title +
//                        ", Intent=" + action.actionIntent);
            }
        } else {
            //XposedBridge.log("No actions found.");
        }

        // その他の情報
        // XposedBridge.log("Notification Visibility: " + notification.visibility);
        // XposedBridge.log("Notification Color: " + notification.color);
        // XposedBridge.log("Notification Group: " + notification.getGroup());
        // XposedBridge.log("Notification SortKey: " + notification.getSortKey());
        // XposedBridge.log("Notification Sound: " + notification.sound);
        // XposedBridge.log("Notification Vibrate: " + (notification.vibrate != null ? "Yes" : "No"));
    }
}