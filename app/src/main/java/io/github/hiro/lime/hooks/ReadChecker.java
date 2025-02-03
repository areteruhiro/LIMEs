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
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabaseLockedException;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
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
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
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

    private static final int MAX_RETRY_COUNT = 3; // 最大リトライ回数
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
                    initializeLimeDatabase(appContext);
                    catchNotification(loadPackageParam, db3, db4, appContext, moduleContext);
                }
            }
        });


        Class<?> chatHistoryRequestClass = XposedHelpers.findClass("com.linecorp.line.chat.request.ChatHistoryRequest", loadPackageParam.classLoader);
        XposedHelpers.findAndHookMethod(chatHistoryRequestClass, "getChatId", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
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
            Context moduleContext;


            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (moduleContext == null) {
                    try {
                        Context systemContext = (Context) XposedHelpers.callMethod(param.thisObject, "getApplicationContext");
                        moduleContext = systemContext.createPackageContext("io.github.hiro.lime", Context.CONTEXT_IGNORE_SECURITY);
                    } catch (Exception e) {
                        //XposedBridge.log("Failed to get module context: " + e.getMessage());
                    }
                }
            }


            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (moduleContext == null) {
                    //XposedBridge.log("Module context is null. Skipping hook.");
                    return;
                }


                if (shouldHookOnCreate && currentGroupId != null) {

                    Activity activity = (Activity) param.thisObject;


                    addButton(activity, moduleContext);
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


    private boolean isNoGroup(String groupId) {
        if (limeDatabase == null) {
            //XposedBridge.log("Database is not initialized.");
            return true;
        }
        String query = "SELECT group_name FROM read_message WHERE group_id = ?";
        Cursor cursor = limeDatabase.rawQuery(query, new String[]{groupId});
        boolean noGroup = true;
        if (cursor.moveToFirst()) {
            String groupName = cursor.getString(cursor.getColumnIndexOrThrow("group_name"));
            noGroup = groupName == null || groupName.isEmpty();
        }


        cursor.close();
        return noGroup;
    }

    private void addButton(Activity activity, Context moduleContext) {
        // ファイルパスを取得
        File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "LimeBackup");
        File file = new File(dir, "margin_settings.txt");

        // デフォルト値
        float readCheckerHorizontalMarginFactor = 0.5f; // デフォルト値
        int readCheckerVerticalMarginDp = 100; // デフォルト値
        float readCheckerSizeDp = 60; // デフォルト値

        // ファイルの内容を読み込む
        if (!file.exists()) {
            // 次のディレクトリを確認
            dir = new File(Environment.getExternalStorageDirectory(), "Android/data/jp.naver.line.android/");
            file = new File(dir, "margin_settings.txt");

        }

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
        File imageFile = new File(dir, imageName);

        // 画像ファイルが存在しない場合、コピーを試みる
        if (!imageFile.exists()) {
            // 最初のディレクトリにコピーを試みる
            if (!copyImageFile(moduleContext, imageName, imageFile)) {
                // 次のディレクトリにコピーを試みる
                File fallbackDir = new File(Environment.getExternalStorageDirectory(), "Android/data/jp.naver.line.android/");
                if (!fallbackDir.exists()) {
                    fallbackDir.mkdirs();
                }
                imageFile = new File(fallbackDir, imageName);
                if (!copyImageFile(moduleContext, imageName, imageFile)) {
                    // 内部ストレージにコピーを試みる
                    File internalDir = new File(moduleContext.getFilesDir(), "backup");
                    if (!internalDir.exists()) {
                        internalDir.mkdirs();
                    }
                    imageFile = new File(internalDir, imageName);
                    copyImageFile(moduleContext, imageName, imageFile);
                }
            }
        }

        // 画像ファイルが存在する場合、Drawableを設定
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

        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentGroupId != null) {
                    showDataForGroupId(activity, currentGroupId, moduleContext);
                }
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
            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (currentGroupId != null) {
                        new AlertDialog.Builder(activity)
                                .setTitle(moduleContext.getResources().getString(R.string.check))
                                .setMessage(moduleContext.getResources().getString(R.string.really_delete))
                                .setPositiveButton(moduleContext.getResources().getString(R.string.yes), (confirmDialog, confirmWhich) -> deleteGroupData(currentGroupId, activity, moduleContext))
                                .setNegativeButton(moduleContext.getResources().getString(R.string.no), null)
                                .show();
                    }
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
        if (limeDatabase == null) {
            return;
        }
// SQLクエリの初期化
        String query;
        // group_id が null のレコードを探し、chat_history から chat_id を取得して更新する
        Cursor nullGroupIdCursor = limeDatabase.rawQuery("SELECT server_id, user_name FROM read_message WHERE group_id = 'null'", null);
        while (nullGroupIdCursor.moveToNext()) {
            String serverId = nullGroupIdCursor.getString(0);
            String userName = nullGroupIdCursor.getString(1); // 更新したレコードの user_name
            String chatId = queryDatabase(db3, "SELECT chat_id FROM chat_history WHERE server_id=?", serverId);

            if (chatId != null && !"null".equals(chatId)) {
                // group_id を更新
                limeDatabase.execSQL("UPDATE read_message SET group_id=? WHERE server_id=?", new String[]{chatId, serverId});

                // 同じ group_id を持つ他の server_id を検索
                Cursor sameGroupIdCursor = limeDatabase.rawQuery(
                        "SELECT server_id, user_name FROM read_message WHERE group_id=? AND server_id != ?",
                        new String[]{chatId, serverId}
                );

                while (sameGroupIdCursor.moveToNext()) {
                    String otherServerId = sameGroupIdCursor.getString(0);
                    String otherUserName = sameGroupIdCursor.getString(1);

                    if (!userName.equals(otherUserName)) {
                        // 新しいレコードを作成
                        limeDatabase.execSQL(
                                "INSERT INTO read_message (server_id, group_id, user_name) VALUES (?, ?, ?)",
                                new String[]{otherServerId, chatId, userName}
                        );
                    }
                }
                sameGroupIdCursor.close();
            }
        }
        nullGroupIdCursor.close();

        if (limeOptions.MySendMessage.checked) {
            // Send_User が (null) のメッセージのみを取得するクエリ
            query = "SELECT server_id, content, created_time FROM read_message WHERE group_id=? AND Send_User = 'null' ORDER BY created_time ASC";
        } else {
            // 通常のクエリ
            query = "SELECT server_id, content, created_time FROM read_message WHERE group_id=? ORDER BY created_time ASC";
        }

        Cursor cursor = limeDatabase.rawQuery(query, new String[]{groupId});

        Map<String, DataItem> dataItemMap = new HashMap<>();

        while (cursor.moveToNext()) {
            String serverId = cursor.getString(0);
            String content = cursor.getString(1);
            String timeFormatted = cursor.getString(2);

            // created_time が "null" の場合、chat_history テーブルから取得してフォーマットする
            if ("null".equals(timeFormatted)) {
                String timeEpochStr = queryDatabase(db3, "SELECT created_time FROM chat_history WHERE server_id=?", serverId);
                if (timeEpochStr != null && !"null".equals(timeEpochStr)) {
                    timeFormatted = formatMessageTime(timeEpochStr);
                }
            }

            List<String> user_nameList = getuser_namesForServerId(serverId, db3);

            if (dataItemMap.containsKey(serverId)) {
                DataItem existingItem = dataItemMap.get(serverId);
                for (String user_name : user_nameList) {
                    if (!existingItem.user_names.contains(user_name)) { // 重複排除
                        existingItem.user_names.add(user_name);
                    }
                }
            } else {
                DataItem dataItem = new DataItem(serverId, content, timeFormatted);
                dataItem.user_names.addAll(user_nameList);
                dataItemMap.put(serverId, dataItem);
            }
        }
        cursor.close();

        List<DataItem> sortedDataItems = new ArrayList<>(dataItemMap.values());
        Collections.sort(sortedDataItems, Comparator.comparing(item -> item.timeFormatted));

        StringBuilder resultBuilder = new StringBuilder();
        for (DataItem item : sortedDataItems) {
            resultBuilder.append("Content: ").append(item.content != null ? item.content : "Media").append("\n");
            resultBuilder.append("Created Time: ").append(item.timeFormatted).append("\n");

            if (!item.user_names.isEmpty()) {
                int newlineCount = 0;
                for (String user_name : item.user_names) {
                    newlineCount += countNewlines(user_name);
                }
                resultBuilder.append(moduleContext.getResources().getString(R.string.Reader))
                        .append(" (").append(item.user_names.size() + newlineCount).append("):\n");
                for (String user_name : item.user_names) {
                    resultBuilder.append("").append(user_name).append("\n");
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
                    .setPositiveButton(moduleContext.getResources().getString(R.string.yes), (confirmDialog, confirmWhich) -> deleteGroupData(groupId, activity, moduleContext))
                    .setNegativeButton(moduleContext.getResources().getString(R.string.no), null)
                    .show();
        });

        AlertDialog dialog = builder.create();
        dialog.show();
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
    }

    private List<String> getuser_namesForServerId(String serverId, SQLiteDatabase db3) {
        if (limeDatabase == null) {
            //("limeDatabaseがnullです。");
            return Collections.emptyList();
        }

        // ID カラムで昇順ソートするクエリ
        String query = "SELECT user_name, ID, Sent_User FROM read_message WHERE server_id=? ORDER BY CAST(ID AS INTEGER) ASC";
        Cursor cursor = limeDatabase.rawQuery(query, new String[]{serverId});
        List<String> userNames = new ArrayList<>();
        Set<String> uniqueUserNames = new HashSet<>(); // 重複排除用の Set

        while (cursor.moveToNext()) {
            String userNameStr = cursor.getString(0);
            int id = cursor.getInt(1); // ID を取得
            String SentUser = cursor.getString(2); // Sent_User を取得

            if (userNameStr != null) {
                // //("取得したuser_name: " + userNameStr);
                //XposedBridge.log("取得したSent_User: " + SentUser);

                // user_name の値をトリミングして "null" かどうかを確認
                String trimmedUserName = userNameStr.trim();
                if (trimmedUserName.startsWith("-")) {
                    // "-ユーザー名 [時間]" の形式からユーザー名部分を抽出
                    int bracketIndex = trimmedUserName.indexOf('[');
                    if (bracketIndex != -1) {
                        String userNamePart = trimmedUserName.substring(1, bracketIndex).trim();
                        ////("抽出したユーザー名部分: " + userNamePart);

                        if (userNamePart.equals("null")) {
                            // SentUser を mid として使用し、contacts テーブルからユーザー名を再取得
                            if (SentUser != null) {
                                String newUserName = queryDatabase(db4, "SELECT profile_name FROM contacts WHERE mid=?", SentUser);
                                //XposedBridge.log("再取得したnewUserName: " + newUserName);

                                if (newUserName != null && !newUserName.equals("null")) {
                                    // 新しいユーザー名を反映
                                    userNameStr = "-" + newUserName + " [" + trimmedUserName.substring(bracketIndex + 1);
                                    //XposedBridge.log("更新後のuser_name: " + userNameStr);
                                }
                            } else {
                                //XposedBridge.log("SentUserがnullです。");
                            }
                        }
                    }
                }

                // 重複排除
                if (!uniqueUserNames.contains(userNameStr)) {
                    userNames.add(userNameStr);
                    uniqueUserNames.add(userNameStr);
                }
            }
        }
        cursor.close();
        ////("最終的なuserNames: " + userNames);
        return userNames;
    }


    private void deleteGroupData(String groupId, Activity activity, Context moduleContext) {
        if (limeDatabase == null) {
            return;
        }


        String deleteQuery = "DELETE FROM read_message WHERE group_id=?";
        limeDatabase.execSQL(deleteQuery, new String[]{groupId});
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


    private static class DataItem {
        String serverId;
        String content;
        String timeFormatted;
        List<String> user_names; // Set から List に変更

        DataItem(String serverId, String content, String timeFormatted) {
            this.serverId = serverId;
            this.content = content;
            this.timeFormatted = timeFormatted;
            this.user_names = new ArrayList<>(); // HashSet から ArrayList に変更
        }
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

    private void fetchDataAndSave(SQLiteDatabase db3, SQLiteDatabase db4, String paramValue, Context context, Context moduleContext) {
        //("fetchDataAndSave");

        String serverId = null;
        String SentUser = null;

        try {
            serverId = extractServerId(paramValue, context);
            SentUser = extractSentUser(paramValue);
            if (serverId == null || SentUser == null) {
                return;
            }

            // データベースが利用可能になるまで待機して再試行
            String SendUser = queryDatabaseWithRetry(db3, "SELECT from_mid FROM chat_history WHERE server_id=?", serverId);
            if (SendUser == null) {
                SendUser = "null";
            }

            String groupId = queryDatabaseWithRetry(db3, "SELECT chat_id FROM chat_history WHERE server_id=?", serverId);
            if (groupId == null) {
                groupId = "null";
            }

            String groupName = queryDatabaseWithRetry(db3, "SELECT name FROM groups WHERE id=?", groupId);
            if (groupName == null) {
                groupName = "null";
            }

            String content = queryDatabaseWithRetry(db3, "SELECT content FROM chat_history WHERE server_id=?", serverId);
            if (content == null) {
                content = "null";
            }

            String user_name = queryDatabaseWithRetry(db4, "SELECT profile_name FROM contacts WHERE mid=?", SentUser);
            if (user_name == null) {
                user_name = "null";
            }

            String timeEpochStr = queryDatabaseWithRetry(db3, "SELECT created_time FROM chat_history WHERE server_id=?", serverId);
            if (timeEpochStr == null) {
                timeEpochStr = "null";
            }

            String timeFormatted = formatMessageTime(timeEpochStr);

            String media = queryDatabaseWithRetry(db3, "SELECT attachement_type FROM chat_history WHERE server_id=?", serverId);
            String mediaDescription = "";

            switch (media) {
                case "7":
                    mediaDescription = moduleContext.getResources().getString(R.string.sticker);
                    break;
                case "1":
                    mediaDescription = moduleContext.getResources().getString(R.string.picture);
                    break;
                case "2":
                    mediaDescription = moduleContext.getResources().getString(R.string.video);
                    break;
                default:
                    mediaDescription = "";
                    break;
            }

            String finalContent = (content != null && !content.isEmpty() && !content.equals("null"))
                    ? content
                    : (!mediaDescription.isEmpty() ? mediaDescription : "No content:" + serverId);

            //("セーブメゾットに渡したよ" + serverId + ", Sent_User: " + SentUser);
            saveData(SendUser, groupId, serverId, SentUser, groupName, finalContent, user_name, timeFormatted, context);

        } catch (Resources.NotFoundException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            // 割り込みが発生した場合の処理
            Thread.currentThread().interrupt(); // 割り込み状態を再設定
            //  System.out.println("Database operation was interrupted");
        }
    }

    private String queryDatabaseWithRetry(SQLiteDatabase db, String query, String... params) throws InterruptedException {
        final int RETRY_DELAY_MS = 100;

        while (true) {
            try {
                // 割り込みが発生したかどうかをチェック
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException("Thread was interrupted");
                }

                return queryDatabase(db, query, params);
            } catch (SQLiteDatabaseLockedException e) {
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    // 割り込みが発生した場合、スレッドの割り込み状態を再設定し、例外をスロー
                    Thread.currentThread().interrupt();
                    throw ie;
                }
            }
        }
    }


    private String formatMessageTime(String timeEpochStr) {
        if (timeEpochStr == null || timeEpochStr.trim().isEmpty()) {
            return "null"; // null または空文字列の場合 "null" を返す
        }

        try {
            long timeEpoch = Long.parseLong(timeEpochStr); // 数値に変換
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            return sdf.format(new Date(timeEpoch)); // フォーマットして返す
        } catch (NumberFormatException e) {
            // 数値として不正な形式の場合
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


        } else {;
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

        // ID カラムを追加し、自動インクリメントさせる
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


    private void saveData(String SendUser, String groupId, String serverId, String SentUser, String groupName, String content, String user_name, String timeFormatted, Context context) {
        // nullの場合に"null"を代入
        SendUser = (SendUser == null) ? "null" : SendUser;
        groupId = (groupId == null) ? "null" : groupId;
        serverId = (serverId == null) ? "null" : serverId;
        SentUser = (SentUser == null) ? "null" : SentUser;
        groupName = (groupName == null) ? "null" : groupName;
        content = (content == null) ? "null" : content;
        user_name = (user_name == null) ? "null" : user_name;
        timeFormatted = (timeFormatted == null) ? "null" : timeFormatted;

        //("セーブメゾットまで処理されたよ: serverId=" + serverId + ", Sent_User=" + SentUser);

        Cursor cursor = null;
        try {
            // クエリの修正: Sent_Userもバインドする
            String checkQuery = "SELECT COUNT(*), user_name FROM read_message WHERE server_id=? AND Sent_User=?";
            cursor = limeDatabase.rawQuery(checkQuery, new String[]{serverId, SentUser});

            if (cursor.moveToFirst()) {
                int count = cursor.getInt(0);
                String existingUserName = cursor.getString(1);
                String currentTime = getCurrentTime();

                if (count > 0) {
                    // 既存のレコードがある場合
                    if (!existingUserName.contains(user_name)) {
                        // user_nameが既存のレコードに含まれていない場合、更新する
                        String updatedUserName = existingUserName + (existingUserName.isEmpty() ? "" : "\n") + "-" + user_name + " [" + currentTime + "]";
                        ContentValues values = new ContentValues();
                        values.put("user_name", updatedUserName);
                        limeDatabase.update("read_message", values, "server_id=? AND Sent_User=?", new String[]{serverId, SentUser});
                        //("User name updated for server_id: " + serverId + ", Sent_User: " + SentUser);
                    }
                } else {
                    // 新しいレコードを挿入する
                    //("insertNewRecordにデータを渡したよ");
                    insertNewRecord(SendUser, groupId, serverId, SentUser, groupName, content, "-" + user_name + " [" + currentTime + "]", timeFormatted);
                }
            }
        } catch (Exception e) {
            // 例外が発生した場合にログを出力
            //("saveDataでエラーが発生しました: " + e.getMessage());
        } finally {
            // カーソルを閉じる
            if (cursor != null) {
                cursor.close();
            }
        }
    }


    private String getCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }

    private void insertNewRecord(String SendUser, String groupId, String serverId, String SentUser, String groupName, String content, String user_name, String timeFormatted) {
        //("Attempting to insert new record: server_id=" + serverId + ", Sent_User=" + SentUser);
        //("Inserting values: groupId=" + groupId + ", serverId=" + serverId + ", SentUser=" + SentUser + ", SendUser=" + SendUser + ", groupName=" + groupName + ", content=" + content + ", user_name=" + user_name + ", timeFormatted=" + timeFormatted);

        // 新しいレコードを挿入
        insertRecord(SendUser, groupId, serverId, SentUser, groupName, content, user_name, timeFormatted);

        // 同じ group_id を持つ他の server_id を検索
        String selectQuery = "SELECT server_id, Sent_User, Send_User, group_name, content, created_time FROM read_message WHERE group_id = ? AND server_id != ?";
        Cursor cursor = null;

        try {
            cursor = limeDatabase.rawQuery(selectQuery, new String[]{groupId, serverId});

            // 検索結果をループ処理
            while (cursor.moveToNext()) {
                String otherServerId = cursor.getString(0);
                String otherSentUser = cursor.getString(1);
                String otherSendUser = cursor.getString(2);
                String otherGroupName = cursor.getString(3);
                String otherContent = cursor.getString(4);
                String othertimeFormatted = cursor.getString(5);

                // Sent_User が存在しない場合のみ処理
                if (!otherSentUser.equals(SentUser)) {
                    // user_name のみ変更して新しいレコードを挿入
                    insertRecord(otherSendUser, groupId, otherServerId, SentUser, otherGroupName, otherContent, user_name, othertimeFormatted);
                    //("Copied record inserted: server_id=" + otherServerId + ", Sent_User=" + SentUser);
                }
            }
        } catch (Exception e) {
            //("Error during copying records: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private void insertRecord(String SendUser, String groupId, String serverId, String SentUser, String groupName, String content, String user_name, String timeFormatted) {
        // Send_User が null の場合に "(null)" として扱う
        String sendUserValue = (SendUser == null) ? "(null)" : SendUser;

        // 重複チェック（group_id, server_id, Sent_User の組み合わせでチェック）
        String checkQuery = "SELECT COUNT(*) FROM read_message WHERE group_id = ? AND server_id = ? AND Sent_User = ?";
        Cursor cursor = limeDatabase.rawQuery(checkQuery, new String[]{groupId, serverId, SentUser});

        if (cursor != null && cursor.moveToFirst() && cursor.getInt(0) == 0) {
            // 重複がない場合のみ挿入
            String insertQuery = "INSERT INTO read_message(group_id, server_id, Sent_User, Send_User, group_name, content, user_name, created_time)" +
                    " VALUES(?, ?, ?, ?, ?, ?, ?, ?);";

            try {
                limeDatabase.beginTransaction();
                limeDatabase.execSQL(insertQuery, new Object[]{groupId, serverId, SentUser, sendUserValue, groupName, content, user_name, timeFormatted});
                limeDatabase.setTransactionSuccessful();
                //("New record inserted successfully: server_id=" + serverId + ", Sent_User=" + SentUser);
            } catch (Exception e) {
                //("Error inserting new record: " + e.getMessage());
                e.printStackTrace();
            } finally {
                limeDatabase.endTransaction();
            }
        } else {
            //XposedBridge.log("Record already exists, skipping insertion: server_id=" + serverId + ", Sent_User=" + SentUser);
        }

        if (cursor != null) {
            cursor.close();
        }
    }
}