package io.github.hiro.lime.hooks;


import static io.github.hiro.lime.Main.limeOptions;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AndroidAppHelper;
import android.app.Application;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabaseLockedException;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.hiro.lime.LimeOptions;
import io.github.hiro.lime.R;
public class ReadChecker implements IHook {
    private SQLiteDatabase limeDatabase;
    private SQLiteDatabase db3 = null;
    private SQLiteDatabase db4 = null;
    private boolean shouldHookOnCreate = false;
    private String currentGroupId = null;

    @Override
    public void hook(LimeOptions limeOptions, XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        if (!limeOptions.ReadChecker.checked) return;
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


                    Context moduleContext = AndroidAppHelper.currentApplication().createPackageContext(
                            "io.github.hiro.lime", Context.CONTEXT_IGNORE_SECURITY);
                    initializeLimeDatabase(appContext);
                    catchNotification(loadPackageParam, db3, db4, appContext, moduleContext);
                }
            }
        });


        Class<?> chatHistoryRequestClass = XposedHelpers.findClass("com.linecorp.line.chat.request.ChatHistoryRequest", loadPackageParam.classLoader);
        XposedHelpers.findAndHookMethod(chatHistoryRequestClass, "getChatId", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                String chatId = (String) param.getResult();
                //XposedBridge.log(chatId);
                if (isGroupExists(chatId)) {
                    shouldHookOnCreate = true;
                    currentGroupId = chatId;
                } else {
                    shouldHookOnCreate = false;
                    currentGroupId = null;
                }
            }
        });
        Class<?> chatHistoryActivityClass = XposedHelpers.findClass("jp.naver.line.android.activity.chathistory.ChatHistoryActivity", loadPackageParam.classLoader);
        XposedHelpers.findAndHookMethod(chatHistoryActivityClass, "onCreate", Bundle.class, new XC_MethodHook() {
            private Context moduleContext;
            private Context appContext;
            private Context activityContext;

            @Override
            protected void beforeHookedMethod(MethodHookParam param) {

                activityContext = (Context) param.thisObject;
                appContext = activityContext.getApplicationContext();
                if (moduleContext == null) {
                    try {
                        moduleContext = appContext.createPackageContext(
                                "io.github.hiro.lime",
                                Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY
                        );
                    } catch (PackageManager.NameNotFoundException e) {
                        XposedBridge.log("Failed to create module context: " + e.getMessage());
                        return;
                    }
                }
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                if (moduleContext == null || activityContext == null) {
                    return;
                }
                if (shouldHookOnCreate && currentGroupId != null) {
                    Activity activity = (Activity) param.thisObject;
                    addButton(activity, activityContext, moduleContext);
                }
            }
        });

    }


    private boolean isGroupExists(String groupId) {
        if (limeDatabase == null) {
            //XposedBridge.log("Database is not initialized.");
            return false;
        }
        String query = "SELECT 1 FROM read_message WHERE group_id = ?";
        Cursor cursor = limeDatabase.rawQuery(query, new String[]{groupId});
        boolean exists = cursor.moveToFirst();
        cursor.close();
        return exists;
    }


    private void addButton(Activity activity, Context context,Context moduleContext) {

        File dir = new File(context.getFilesDir(), "LimeBackup/Setting");
        File dir2 = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "LimeBackup");

        File file = new File(dir, "margin_settings.txt");

        float readCheckerHorizontalMarginFactor = 0.5f;
        int readCheckerVerticalMarginDp = 100;
        float readCheckerSizeDp = 60;
        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split("=", 2);
                    if (parts.length == 2) {
                        if (parts[0].trim().equals("Read_checker_horizontalMarginFactor")) {
                            readCheckerHorizontalMarginFactor = Float.parseFloat(parts[1].trim());
                        } else if (parts[0].trim().equals("Read_checker_verticalMarginDp")) {
                            readCheckerVerticalMarginDp = Integer.parseInt(parts[1].trim());
                        } else if (parts[0].trim().equals("chat_read_check_size")) {
                            readCheckerSizeDp = Float.parseFloat(parts[1].trim());
                        }
                    }
                }
            } catch (IOException | NumberFormatException ignored) {
            }
        }

        ImageView imageButton = new ImageView(activity);
        String imageName = "read_checker.png";
        File imageFile = new File(dir2, imageName);

        if (!imageFile.exists()) {
            if (!copyImageFile(moduleContext, imageName, imageFile)) {
                    copyImageFile(moduleContext, imageName, imageFile);
                }

        }

        if (imageFile.exists()) {
            Drawable drawable = Drawable.createFromPath(imageFile.getAbsolutePath());
            if (drawable != null) {
                int sizeInPx = dpToPx(moduleContext, readCheckerSizeDp);
                drawable = scaleDrawable(drawable, sizeInPx, sizeInPx);
                imageButton.setImageDrawable(drawable);
            }
        }

        FrameLayout.LayoutParams frameParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );

        int horizontalMarginPx = (int) (readCheckerHorizontalMarginFactor * activity.getResources().getDisplayMetrics().widthPixels);
        int verticalMarginPx = (int) (readCheckerVerticalMarginDp * activity.getResources().getDisplayMetrics().density);
        frameParams.setMargins(horizontalMarginPx, verticalMarginPx, 0, 0);

        imageButton.setLayoutParams(frameParams);

        imageButton.setOnClickListener(v -> {
            if (currentGroupId != null) {
                showDataForGroupId(activity, currentGroupId, moduleContext);
            }
        });

        if (limeOptions.ReadCheckerChatdataDelete.checked) {
            Button deleteButton = new Button(activity);
            deleteButton.setText(moduleContext.getResources().getString(R.string.Delete));
            deleteButton.setBackgroundColor(Color.RED);
            deleteButton.setTextColor(Color.WHITE);
            FrameLayout.LayoutParams deleteButtonParams = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
            );
            deleteButtonParams.setMargins(horizontalMarginPx + dpToPx(moduleContext, readCheckerSizeDp) + 20, verticalMarginPx, 0, 0);
            deleteButton.setLayoutParams(deleteButtonParams);
            deleteButton.setOnClickListener(v -> {
                if (currentGroupId != null) {
                    new AlertDialog.Builder(activity)
                            .setTitle(moduleContext.getResources().getString(R.string.check))
                            .setMessage(moduleContext.getResources().getString(R.string.really_delete))
                            .setPositiveButton(moduleContext.getResources().getString(R.string.yes), (confirmDialog, confirmWhich) -> deleteGroupData(currentGroupId, activity, moduleContext))
                            .setNegativeButton(moduleContext.getResources().getString(R.string.no), null)
                            .show();
                }
            });

            ViewGroup layout = activity.findViewById(android.R.id.content);
            layout.addView(deleteButton);
        }

        ViewGroup layout = activity.findViewById(android.R.id.content);
        layout.addView(imageButton);
    }

    private boolean copyImageFile(Context moduleContext, String imageName, File destinationFile) {
        try (InputStream in = moduleContext.getResources().openRawResource(
                moduleContext.getResources().getIdentifier(imageName.replace(".png", ""), "drawable", "io.github.hiro.lime"));
             OutputStream out = new FileOutputStream(destinationFile)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private int dpToPx(@NonNull Context context, float dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private Drawable scaleDrawable(Drawable drawable, int width, int height) {
        Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
        return new BitmapDrawable(scaledBitmap);
    }


    private void showDataForGroupId(Activity activity, String groupId, Context moduleContext) {
        final String TAG = "MyXposedModule";

        if (limeDatabase == null) {
            XposedBridge.log(TAG + " - limeDatabase is null");
            return;
        }

        try {
            XposedBridge.log(TAG + " - Starting transaction for groupId: " + groupId);
            limeDatabase.beginTransaction();

// NULLグループIDの処理
            try (Cursor nullGroupIdCursor = limeDatabase.rawQuery(
                    "SELECT server_id, user_name FROM read_message WHERE group_id = 'null'", null)) {

                XposedBridge.log(TAG + " - Processing null groupIds...");
                while (nullGroupIdCursor.moveToNext()) {
                    String serverId = nullGroupIdCursor.getString(0);
                    String userName = nullGroupIdCursor.getString(1);
                    XposedBridge.log(TAG + " - Processing serverId: " + serverId + ", user: " + userName);

                    String chatId = queryDatabase(db3, "SELECT chat_id FROM chat_history WHERE server_id=?", serverId);
                    XposedBridge.log(TAG + " - Retrieved chatId: " + chatId);

                    if (chatId != null && !"null".equals(chatId)) {
                        try {
                            // グループIDを更新
                            limeDatabase.execSQL("UPDATE read_message SET group_id=? WHERE server_id=?",
                                    new String[]{chatId, serverId});
                            XposedBridge.log(TAG + " - Updated group_id for serverId: " + serverId);

                            // コンテンツ情報を取得
                            String contentRetry = queryDatabaseWithRetry(db3,
                                    "SELECT content FROM chat_history WHERE server_id=?", serverId);
                            contentRetry = (contentRetry != null && !"null".equals(contentRetry)) ? contentRetry : "null";
                            String mediaDescription = getMediaDescription(serverId, contentRetry, moduleContext);
                            String finalContent = "null".equals(contentRetry) ? mediaDescription : contentRetry;

                            // 関連レコード処理を実行（新しいグループIDで）
                            processRelatedRecords(chatId, serverId, finalContent);

                            processSameGroupId(serverId, chatId, userName);
                            updateSendUser(serverId);

                        } catch (SQLException e) {
                            XposedBridge.log(TAG + " - UPDATE failed for serverId: " + serverId);
                            XposedBridge.log(e);
                        }
                    }
                }
            } catch (SQLException e) {
                XposedBridge.log(TAG + " - Error processing null groupIds");
                XposedBridge.log(e);
            }

            // 指定groupIdの処理
            try (Cursor groupCursor = limeDatabase.rawQuery(
                    "SELECT server_id FROM read_message WHERE group_id = ?", new String[]{groupId})) {

                XposedBridge.log(TAG + " - Processing groupId: " + groupId);
                while (groupCursor.moveToNext()) {
                    String serverId = groupCursor.getString(0);
                    XposedBridge.log(TAG + " - Processing serverId: " + serverId);

                    String contentRetry = queryDatabaseWithRetry(db3,
                            "SELECT content FROM chat_history WHERE server_id=?", serverId);
                    XposedBridge.log(TAG + " - Initial content: " + contentRetry);

                    contentRetry = (contentRetry != null && !"null".equals(contentRetry)) ? contentRetry : "null";
                    String mediaDescription = getMediaDescription(serverId, contentRetry, moduleContext);
                    String finalContent = "null".equals(contentRetry) ? mediaDescription : contentRetry;

                    XposedBridge.log(TAG + " - Final content: " + finalContent);

                    try {
                        updateReadMessage(serverId, finalContent);

                    } catch (SQLException e) {
                        XposedBridge.log(TAG + " - Error updating records for serverId: " + serverId);
                        XposedBridge.log(e);
                    }
                }
            } catch (SQLException e) {
                XposedBridge.log(TAG + " - Error processing groupId: " + groupId);
                XposedBridge.log(e);
            }

            // ダイアログ表示
            try {
                showDialog(activity, moduleContext, groupId);
                limeDatabase.setTransactionSuccessful();
                XposedBridge.log(TAG + " - Transaction completed successfully for groupId: " + groupId);
            } catch (Exception e) {
                XposedBridge.log(TAG + " - Error showing dialog");
                XposedBridge.log(e);
            }

        } catch (SQLException | Resources.NotFoundException e) {
            XposedBridge.log(TAG + " - Critical error in transaction");
            XposedBridge.log(e);
            throw new RuntimeException(e);
        } finally {
            try {
                limeDatabase.endTransaction();
                XposedBridge.log(TAG + " - Transaction ended");
            } catch (SQLException e) {
                XposedBridge.log(TAG + " - Error ending transaction");
                XposedBridge.log(e);
            }
        }
    }
    private void processSameGroupId(String serverId, String chatId, String userName) {
        try (Cursor sameGroupIdCursor = limeDatabase.rawQuery(
                "SELECT server_id, user_name FROM read_message WHERE group_id=? AND server_id != ?",
                new String[]{chatId, serverId}
        )) {
            while (sameGroupIdCursor.moveToNext()) {
                String otherServerId = sameGroupIdCursor.getString(0);
                String otherUserName = sameGroupIdCursor.getString(1);
                if (!userName.equals(otherUserName)) {
                    limeDatabase.execSQL(
                            "INSERT INTO read_message (server_id, group_id, user_name) VALUES (?, ?, ?)",
                            new String[]{otherServerId, chatId, userName}
                    );
                }
            }
        }
    }

    private void updateSendUser (String serverId) {
        try (Cursor nullSendUserCursor = limeDatabase.rawQuery(
                "SELECT server_id FROM read_message WHERE Send_User = 'null' OR Send_User IS NULL", null)) {
            while (nullSendUserCursor.moveToNext()) {
                String sendUser  = queryDatabaseWithRetry(db3, "SELECT from_mid FROM chat_history WHERE server_id=?", serverId);
                sendUser  = (sendUser  != null && !sendUser .isEmpty() && !sendUser .equals("null")) ? sendUser  : "null";
                limeDatabase.execSQL("UPDATE read_message SET Send_User = ? WHERE server_id = ?", new String[]{sendUser , serverId});
            }
        }
    }

    private String getMediaDescription(String serverId, String contentRetry, Context moduleContext) {
        String media = "null";
        if ("null".equals(contentRetry)) {
            media = queryDatabaseWithRetry(db3, "SELECT parameter FROM chat_history WHERE server_id=?", serverId);
            media = (media != null) ? media : "null";
        }

        if (!"null".equals(media)) {
            if (media.contains("IMAGE")) {
                return moduleContext.getResources().getString(R.string.picture);
            } else if (media.contains("video")) {
                return moduleContext.getResources().getString(R.string.video);
            } else if (media.contains("STKPKGID")) {
                return moduleContext.getResources ().getString(R.string.sticker);
            } else if (media.contains("FILE")) {
                return moduleContext.getResources().getString(R.string.file);
            } else if (media.contains("LOCATION")) {
                return moduleContext.getResources().getString(R.string.location);
            }
        }
        return "null";
    }

    private void updateReadMessage(String serverId, String finalContent) {
        String timeEpochStr = queryDatabase(db3, "SELECT created_time FROM chat_history WHERE server_id=?", serverId);
        String timeFormatted = (timeEpochStr != null && !"null".equals(timeEpochStr)) ? formatMessageTime(timeEpochStr) : "";

        limeDatabase.execSQL("UPDATE read_message SET content = ? WHERE server_id = ?", new String[]{finalContent, serverId});
        limeDatabase.execSQL("UPDATE read_message SET created_time = ? WHERE server_id = ?", new String[]{timeFormatted, serverId});
    }

    private void showDialog(Activity activity, Context moduleContext, String groupId) {
        String query = limeOptions.MySendMessage.checked
                ? "SELECT ID, server_id, content, created_time FROM read_message WHERE group_id=? AND Send_User = 'null' ORDER BY ID ASC"
                : "SELECT ID, server_id, content, created_time FROM read_message WHERE group_id=? ORDER BY ID ASC";

        try (Cursor cursor = limeDatabase.rawQuery(query, new String[]{groupId})) {
            List<DataItem> dataItems = new ArrayList<>();

            while (cursor.moveToNext()) {
                int id = cursor.getInt(0);
                String serverId = cursor.getString(1);
                String content = cursor.getString(2);
                String timeFormatted = cursor.getString(3);
                List<String> user_nameList = getuser_namesForServerId(serverId, db3);

                DataItem existingItem = null;
                for (DataItem item : dataItems) {
                    if (item.serverId.equals(serverId)) {
                        existingItem = item;
                        break;
                    }
                }

                if (existingItem == null) {
                    DataItem newItem = new DataItem(serverId, content, timeFormatted);
                    newItem.id = id;
                    newItem.user_names.addAll(user_nameList);
                    dataItems.add(newItem);
                } else {
                    existingItem.user_names.addAll(user_nameList);
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Collections.sort(dataItems, Comparator.comparingInt(item -> item.id));
            }

            StringBuilder resultBuilder = new StringBuilder();
            for (DataItem item : dataItems) {
                resultBuilder.append("Content: ")
                        .append(item.content != null ? item.content : "Media")
                        .append("\n");
                resultBuilder.append("Created Time: ")
                        .append(item.timeFormatted)
                        .append("\n");

                if (!item.user_names.isEmpty()) {
                    resultBuilder.append(moduleContext.getResources().getString(R.string.Reader))
                            .append(" (").append(item.user_names.size()).append("):\n");
                    for (String user_name : item.user_names) {
                        resultBuilder.append(user_name).append("\n");
                    }
                } else {
                    resultBuilder.append("No talk names found.\n");
                }
                resultBuilder.append("\n");
            }

            TextView textView = new TextView(activity);
            textView.setText(resultBuilder.toString());
            textView.setPadding(20, 20, 20, 20);

            ScrollView scrollView = new ScrollView(activity);
            scrollView.addView(textView);
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle("READ Data");
            builder.setView(scrollView);
            builder.setPositiveButton("OK", null);
            builder.setNegativeButton(moduleContext.getResources().getString(R.string.Delete), (dialog, which) -> {
                new AlertDialog.Builder(activity)
                        .setTitle(moduleContext.getResources().getString(R.string.check))
                        .setMessage(moduleContext.getResources().getString(R.string.really_delete))
                        .setPositiveButton(moduleContext.getResources().getString(R.string.yes),
                                (confirmDialog, confirmWhich) -> deleteGroupData(groupId, activity, moduleContext))
                        .setNegativeButton(moduleContext.getResources().getString(R.string.no), null)
                        .show();
            });

            AlertDialog dialog = builder.create();
            dialog.show();
            scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
        }
    }
    private List<String> getuser_namesForServerId(String serverId, SQLiteDatabase db3) {
        processAndRemoveDuplicates(serverId, db3);
        if (limeDatabase == null) {
            return Collections.emptyList();
        }

        String query = "SELECT user_name, ID, Sent_User FROM read_message WHERE server_id=? ORDER BY CAST(ID AS INTEGER) ASC";
        Cursor cursor = limeDatabase.rawQuery(query, new String[]{serverId});
        List<String> userNames = new ArrayList<>();
        Set<String> uniqueBaseNames = new HashSet<>();

        while (cursor.moveToNext()) {
            String userNameStr = cursor.getString(0);
            int id = cursor.getInt(1);
            String SentUser = cursor.getString(2);

            if (userNameStr != null) {
                String trimmed = userNameStr.trim();
                String processedName = processUserName(trimmed, SentUser, db4);

                String baseName = processedName.replaceAll("\\s*\\[.*", "").trim();
                String normalized = baseName.toLowerCase();

                if (!uniqueBaseNames.contains(normalized)) {
                    userNames.add(processedName);
                    uniqueBaseNames.add(normalized);
                    XposedBridge.log("追加されたuser_name: " + processedName);
                } else {
                    XposedBridge.log("重複スキップ: " + processedName);

                }
            }
        }
        cursor.close();
        return userNames;
    }
    private void processAndRemoveDuplicates(String serverId, SQLiteDatabase db3) {
        if (limeDatabase == null) return;

        limeDatabase.beginTransaction();
        try {

            List<Integer> idsToDelete = new ArrayList<>();

            String query = "SELECT ID, user_name, Sent_User FROM read_message WHERE server_id=? ORDER BY CAST(ID AS INTEGER) ASC";
            Cursor cursor = limeDatabase.rawQuery(query, new String[]{serverId});

            Set<String> uniqueBaseNames = new HashSet<>();

            while (cursor.moveToNext()) {
                int currentId = cursor.getInt(0);
                String userNameStr = cursor.getString(1);
                String sentUser = cursor.getString(2);

                if (userNameStr != null) {
                    String processedName = processUserName(userNameStr.trim(), sentUser, db4);
                    String baseName = processedName.replaceAll("\\s*\\[.*", "").trim().toLowerCase();

                    if (uniqueBaseNames.contains(baseName)) {
                        idsToDelete.add(currentId);
                        XposedBridge.log("削除対象ID: " + currentId + " - " + processedName);
                    } else {
                        uniqueBaseNames.add(baseName);
                    }
                }
            }
            cursor.close();

            // 重複レコードの削除処理
            if (!idsToDelete.isEmpty()) {
                String deleteQuery = "DELETE FROM read_message WHERE ID IN (" +
                        TextUtils.join(",", Collections.nCopies(idsToDelete.size(), "?")) + ")";

                String[] deleteParams = new String[idsToDelete.size()];
                for (int i = 0; i < idsToDelete.size(); i++) {
                    deleteParams[i] = String.valueOf(idsToDelete.get(i));
                }

                limeDatabase.execSQL(deleteQuery, deleteParams);
                XposedBridge.log("削除されたレコード数: " + idsToDelete.size());
            }

            limeDatabase.setTransactionSuccessful();
        } catch (SQLException e) {
            XposedBridge.log("データベースエラー: " + e.getMessage());
        } finally {
            limeDatabase.endTransaction();
        }
    }
    private String processUserName(String trimmed, String sentUser, SQLiteDatabase db) {
        if (trimmed.startsWith("-")) {
            int bracketIndex = trimmed.indexOf('[');
            if (bracketIndex != -1) {
                String namePart = trimmed.substring(1, bracketIndex).trim();
                if (namePart.equalsIgnoreCase("null") && sentUser != null) {
                    String newName = queryDatabase(db, "SELECT profile_name FROM contacts WHERE mid=?", sentUser);
                    if (newName != null && !newName.equalsIgnoreCase("null")) {
                        return "-" + newName + " [" + trimmed.substring(bracketIndex + 1);
                    }
                }
            }
        }
        return trimmed;
    }



    private void processRelatedRecords(String groupId, String currentServerId, String finalContent) {
        final String TAG = "MyXposedModule";

        if (limeDatabase == null) {
            XposedBridge.log(TAG + " - limeDatabase is null");
            return;
        }

        // トランザクションは呼び出し元で管理するため、ここでは開始しない
        try (Cursor cursor = limeDatabase.rawQuery(
                "SELECT server_id, user_name, Sent_User, Send_User, group_name, content, created_time " +
                        "FROM read_message WHERE group_id = ? AND server_id != ?",
                new String[]{groupId, currentServerId})) {

            String currentTrimmedName = extractTrimmedName(finalContent);
            XposedBridge.log(TAG + " - Processing groupId: " + groupId +
                    ", currentServerId: " + currentServerId +
                    ", currentTrimmedName: " + currentTrimmedName);

            int insertedCount = 0;
            while (cursor.moveToNext()) {
                String targetServerId = cursor.getString(0);
                String targetUserName = cursor.getString(1);
                String sentUser = cursor.getString(2);
                String sendUser = cursor.getString(3);
                String groupName = cursor.getString(4);
                String content = cursor.getString(5);
                String createdTime = cursor.getString(6);

                String targetTrimmedName = extractTrimmedName(targetUserName);
                boolean isDuplicate = isDuplicateRecord(targetServerId, sentUser);

                XposedBridge.log(TAG + " - Checking record: " + targetServerId +
                        ", targetTrimmedName: " + targetTrimmedName +
                        ", isDuplicate: " + isDuplicate);

                if (currentTrimmedName != null && targetTrimmedName != null &&
                        !currentTrimmedName.equals(targetTrimmedName) &&
                        !isDuplicate) {

                    ContentValues values = new ContentValues();
                    values.put("group_id", groupId);
                    values.put("server_id", targetServerId);
                    values.put("Sent_User", sentUser);
                    values.put("Send_User", sendUser);
                    values.put("group_name", groupName);
                    values.put("content", content);
                    values.put("user_name", targetUserName);
                    values.put("created_time", createdTime);

                    long rowId = limeDatabase.insert("read_message", null, values);
                    if (rowId != -1) {
                        insertedCount++;
                        XposedBridge.log(TAG + " - Inserted new record: " + targetServerId);
                    } else {
                        XposedBridge.log(TAG + " - Failed to insert: " + targetServerId);
                    }
                }
            }
            XposedBridge.log(TAG + " - Total inserted records: " + insertedCount);
        } catch (SQLException e) {
            XposedBridge.log(TAG + " - Database error in processRelatedRecords");
            XposedBridge.log(e);
            throw new RuntimeException("Database error", e);
        }
    }
    private boolean isDuplicateRecord(String serverId, String sentUser) {
        // nullチェックを追加
        if (serverId == null || sentUser == null) {
            XposedBridge.log("isDuplicateRecord: Invalid parameters - serverId: " + serverId + ", sentUser: " + sentUser);
            return false; // または適切なデフォルト値を返す
        }

        try (Cursor cursor = limeDatabase.rawQuery(
                "SELECT COUNT(*) FROM read_message WHERE server_id = ? AND Sent_User = ?",
                new String[]{serverId, sentUser}
        )) {
            return cursor.moveToFirst() && cursor.getInt(0) > 0;
        } catch (SQLException e) {
            XposedBridge.log("Database error in isDuplicateRecord: " + e.getMessage());
            return false;
        }
    }
    private String extractTrimmedName(String formattedName) {
        if (formattedName == null) return null;
        Pattern pattern = Pattern.compile("-(.*?)\\s\\[");
        Matcher matcher = pattern.matcher(formattedName);
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    private void fetchDataAndSave(SQLiteDatabase db3, SQLiteDatabase db4, String paramValue, Context context, Context moduleContext) {
        String serverId = null;
        String SentUser = null;

        try {
            serverId = extractServerId(paramValue, context);
            SentUser = extractSentUser(paramValue);
            if (serverId == null || SentUser == null) return;

            String SendUser = queryDatabaseWithRetry(db3, "SELECT from_mid FROM chat_history WHERE server_id=?", serverId);
            SendUser = SendUser != null ? SendUser : "null";

            String groupId = queryDatabaseWithRetry(db3, "SELECT chat_id FROM chat_history WHERE server_id=?", serverId);
            groupId = groupId != null ? groupId : "null";

            String groupName = queryDatabaseWithRetry(db3, "SELECT name FROM groups WHERE id=?", groupId);
            groupName = groupName != null ? groupName : "null";

            String content = queryDatabase(db3, "SELECT content FROM chat_history WHERE server_id=?", serverId);
            content = content != null ? content : "null";

            String name = queryDatabase(db4, "SELECT profile_name FROM contacts WHERE mid=?", SentUser);
            name = name != null ? name : "null";

            String timeEpochStr = queryDatabase(db3, "SELECT created_time FROM chat_history WHERE server_id=?", serverId);
            timeEpochStr = timeEpochStr != null ? timeEpochStr : "null";

            String media = queryDatabase(db3, "SELECT parameter FROM chat_history WHERE server_id=?", serverId);
            media = media != null ? media : "null";

            String mediaDescription = "";
            boolean mediaError = false;
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
                mediaError = true;
            }

            if (mediaError) {
                mediaDescription = "null";
                createErrorFile(context, serverId, media); // エラーファイル作成
            }

            String finalContent = determineFinalContent(content, mediaDescription);
            String timeFormatted = formatMessageTime(timeEpochStr);
            saveData(SendUser, groupId, serverId, SentUser, groupName, finalContent, name, timeFormatted, context);

        } catch (Resources.NotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void deleteGroupData(String groupId, Activity activity, Context moduleContext) {
        if (limeDatabase == null) {
            return;
        }
        String deleteAllQuery = "DELETE FROM read_message";
        limeDatabase.execSQL(deleteAllQuery);

        Toast.makeText(activity, moduleContext.getResources().getString(R.string.Reader_Data_Delete_Success), Toast.LENGTH_SHORT).show();
    }

    private int countNewlines(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (char c : text.toCharArray()) {
            if (c == '\n') {
                count++;
            }
        }
        return count;
    }



    private void catchNotification(XC_LoadPackage.LoadPackageParam loadPackageParam, SQLiteDatabase db3, SQLiteDatabase db4, Context appContext, Context moduleContext) {
        try {
            XposedBridge.hookAllMethods(
                    loadPackageParam.classLoader.loadClass(Constants.NOTIFICATION_READ_HOOK.className),
                    "invokeSuspend",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            String paramValue = param.args[0].toString();
                            if (appContext == null) {

                                return;
                            }
                            Context moduleContext;
                            try {
                                moduleContext = appContext.createPackageContext(
                                        "io.github.hiro.lime", Context.CONTEXT_IGNORE_SECURITY);
                            } catch (PackageManager.NameNotFoundException e) {
                                //("Failed to create package context: " + e.getMessage());
                                return;
                            }


                            if (paramValue != null && paramValue.contains("type:NOTIFIED_READ_MESSAGE")) {
                                List<String> messages = extractMessages(paramValue);
                                for (String message : messages) {
                                    fetchDataAndSave(db3, db4, message, appContext, moduleContext);
                                }
                            }
                        }
                    }
            );
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }


    private List<String> extractMessages(String paramValue) {
        List<String> messages = new ArrayList<>();
        Pattern pattern = Pattern.compile("type:NOTIFIED_READ_MESSAGE.*?(?=type:|$)");
        Matcher matcher = pattern.matcher(paramValue);


        while (matcher.find()) {
            messages.add(matcher.group().trim());
        }


        return messages;
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

    private static class DataItem {
        int id;
        String serverId;
        String content;
        String timeFormatted;
        Set<String> user_names = new LinkedHashSet<>();

        DataItem(String serverId, String content, String timeFormatted) {
            this.serverId = serverId;
            this.content = content;
            this.timeFormatted = timeFormatted;
        }
    }
    private void createErrorFile(Context context, String serverId, String mediaValue) {
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File limeBackupDir = new File(downloadsDir, "LimeBackup");
        if (!limeBackupDir.exists()) {
            limeBackupDir.mkdirs();
        }
        File errorFile = new File(limeBackupDir, "no_get.txt");
        try (FileWriter fw = new FileWriter(errorFile, true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter pw = new PrintWriter(bw)) {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
            String errorMsg = String.format(Locale.getDefault(), "[%s] ServerID: %s, Media Value: %s", timestamp, serverId, mediaValue);
            pw.println(errorMsg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String queryDatabaseWithRetry(SQLiteDatabase db, String query, String... params) {
        final int RETRY_DELAY_MS = 100;

        while (true) {
            try {
                return queryDatabase(db, query, params);
            } catch (SQLiteDatabaseLockedException e) {

                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Thread interrupted while waiting for database", ie);
                }
            }
        }
    }


    private String formatMessageTime(String timeEpochStr) {
        if (timeEpochStr == null || timeEpochStr.trim().isEmpty()) {
            return "null";
        }

        try {
            long timeEpoch = Long.parseLong(timeEpochStr); // 数値に変換
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            return sdf.format(new Date(timeEpoch)); // フォーマットして返す
        } catch (NumberFormatException e) {
            return "null";
        }
    }


    private String extractSentUser(String paramValue) {
        Pattern pattern = Pattern.compile("param2:([a-zA-Z0-9]+)");
        Matcher matcher = pattern.matcher(paramValue);
        return matcher.find() ? matcher.group(1) : null;
    }


    private String extractServerId(String paramValue, Context context) {
        Pattern pattern = Pattern.compile("param3:([0-9]+)");
        Matcher matcher = pattern.matcher(paramValue);
        //(paramValue);
        if (matcher.find()) {
            return matcher.group(1);


        } else {
            ;
            return null;
        }
    }


    private String queryDatabase(SQLiteDatabase db, String query, String... selectionArgs) {
        if (db == null) {
            //("Database is not initialized.");
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


    private void initializeLimeDatabase(Context context) {
        File oldDbFile = new File(context.getFilesDir(), "checked_data.db");
        if (oldDbFile.exists()) {
            boolean deleted = oldDbFile.delete();
            if (deleted) {
                //XposedBridge.log("Old database file lime_data.db deleted.");
            } else {
                //XposedBridge.log("Failed to delete old database file lime_data.db.");
            }
        }
        File dbFile = new File(context.getFilesDir(), "lime_checked_data.db");
        limeDatabase = SQLiteDatabase.openOrCreateDatabase(dbFile, null);

        String createGroupTable = "CREATE TABLE IF NOT EXISTS read_message (" +
                "ID INTEGER PRIMARY KEY AUTOINCREMENT, " +  // 新しいIDカラム
                "group_id TEXT NOT NULL, " +
                "server_id TEXT NOT NULL, " +
                "Sent_User TEXT, " +
                "Send_User TEXT, " +
                "group_name TEXT, " +
                "content TEXT, " +
                "user_name TEXT, " +
                "created_time TEXT" +
                ");";
        limeDatabase.execSQL(createGroupTable);
        //("Database initialized and read_message table created with ID column.");
    }

    private String getCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }


    private void saveData(String SendUser, String groupId, String serverId, String SentUser,
                          String groupName, String finalContent, String name,
                          String timeFormatted, Context context) {
        final String currentTime = getCurrentTime();

        final String safeName = (name != null && !name.equals("null")) ? name : "Unknown";
        final String formattedUserName = "-" + safeName + " [" + currentTime + "]";

        try (Cursor cursor = limeDatabase.rawQuery(
                "SELECT COUNT(*) FROM read_message WHERE server_id=? AND user_name=?",
                new String[]{serverId, formattedUserName} // 検索条件もformattedUserNameに変更
        )) {
            if (cursor.moveToFirst() && cursor.getInt(0) == 0) {
                insertNewRecord(
                        SendUser,
                        groupId,
                        serverId,
                        SentUser,
                        groupName,
                        finalContent,
                        formattedUserName, // フォーマット済みの値を渡す
                        timeFormatted
                );
            }
        } catch (SQLException ignored) {
        }
    }

    private void insertNewRecord(String SendUser, String groupId, String serverId,
                                 String SentUser, String groupName, String finalContent,
                                 String formattedUserName, String timeFormatted) {
        try {
            limeDatabase.beginTransaction();
            insertRecord(SendUser, groupId, serverId, SentUser, groupName,
                    finalContent, formattedUserName, timeFormatted);
            copyRelatedRecords(groupId, serverId, SentUser, formattedUserName,timeFormatted);

            limeDatabase.setTransactionSuccessful();
        } finally {
            limeDatabase.endTransaction();
        }
    }

    private void copyRelatedRecords(String groupId, String sourceServerId,
                                    String SentUser, String user_name,String timeFormatted) {
        final String selectQuery =
                "SELECT server_id, Sent_User, Send_User, group_name, content, created_time " +
                        "FROM read_message " +
                        "WHERE group_id = ? AND server_id != ?";

        try (Cursor cursor = limeDatabase.rawQuery(selectQuery, new String[]{groupId, sourceServerId})) {
            while (cursor.moveToNext()) {
                final String otherServerId = cursor.getString(0);
                final String otherSentUser = cursor.getString(1);
                final String otherSendUser = cursor.getString(2);
                final String otherGroupName = cursor.getString(3);
                final String otherContent = cursor.getString(4);
                final String otherTime = cursor.getString(5);

                if (!SentUser.equals(otherSentUser)) {
                    String originalName = extractOriginalName(user_name);
                    if (!isRecordExists(otherServerId, user_name)) {
                        if (originalName != null && !isRecordExists(otherServerId, originalName)) {
                            insertRecord(
                                    otherSendUser,
                                    groupId,
                                    otherServerId,
                                    SentUser,
                                    otherGroupName,
                                    otherContent,
                                    user_name,
                                    otherTime
                            );
                        }
                    }
                }
            }
        }
    }

    private String extractOriginalName(String formattedUserName) {
        if (formattedUserName == null || formattedUserName.isEmpty()) {
            return null;
        }
        Pattern pattern = Pattern.compile("-(.*?)\\s*\\[");
        Matcher matcher = pattern.matcher(formattedUserName);
        return matcher.find() ? matcher.group(1) : null;
    }

    private boolean isRecordExists(String serverId, String originalName) {
        if (originalName == null) return false;

        final String checkQuery =
                "SELECT COUNT(*) FROM read_message " +
                        "WHERE server_id = ? AND user_name LIKE ? ESCAPE '!'";

        String escapedName = escapeForLike(originalName);
        String namePattern = "%-" + escapedName + "%";

        try (Cursor cursor = limeDatabase.rawQuery(checkQuery, new String[]{serverId, namePattern})) {
            return cursor.moveToFirst() && cursor.getInt(0) > 0;
        }
    }

    private String escapeForLike(String value) {
        return value.replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_")
                .replace("[", "![");
    }
    private void insertRecord(String SendUser, String groupId, String serverId,
                              String SentUser, String groupName, String finalContent,
                              String user_name, String timeFormatted) {
        final String insertQuery =
                "INSERT OR IGNORE INTO read_message(" +
                        "    group_id, server_id, Sent_User, Send_User, " +  // SendUser → Send_Userに修正
                        "    group_name, content, user_name, created_time" +
                        ") VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try {
            limeDatabase.execSQL(insertQuery, new Object[]{
                    groupId,
                    serverId,
                    SentUser,
                    SendUser,  // この値はSend_Userカラムに挿入される
                    groupName,
                    finalContent,
                    user_name,
                    timeFormatted
            });
        } catch (SQLException e) {
            XposedBridge.log("Database insert error: " + e.getMessage());
            XposedBridge.log(e);
            XposedBridge.log("Failed data: " +
                    "group=" + groupName + ", " +
                    "sender=" + SendUser + ", " +
                    "content=" + finalContent);
        }
    }
}