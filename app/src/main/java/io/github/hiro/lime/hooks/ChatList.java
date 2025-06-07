package io.github.hiro.lime.hooks;

import static io.github.hiro.lime.Main.limeOptions;

import android.app.AndroidAppHelper;
import android.app.Application;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteStatement;
import android.os.Build;
import android.os.Environment;
import android.util.Pair;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.hiro.lime.LimeOptions;
import io.github.hiro.lime.R;

public class ChatList implements IHook {
    @Override
    public void hook(LimeOptions limeOptions, XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {




        XposedBridge.hookAllMethods(Application.class, "onCreate", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Application appContext = (Application) param.thisObject;

                if (appContext == null) {
                    return;
                }
                Context moduleContext = AndroidAppHelper.currentApplication().createPackageContext(
                        "io.github.hiro.lime", Context.CONTEXT_IGNORE_SECURITY);

                File dbFile = appContext.getDatabasePath("naver_line");
                SQLiteDatabase db = null;
                File dbFile2 = appContext.getDatabasePath("contact");
                SQLiteDatabase db2 = null;

                if (dbFile.exists() && dbFile2.exists()) {
                    SQLiteDatabase.OpenParams.Builder builder = null;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                        builder = new SQLiteDatabase.OpenParams.Builder();


                        builder.addOpenFlags(SQLiteDatabase.OPEN_READWRITE);

                        SQLiteDatabase.OpenParams dbParams = builder.build();


                        SQLiteDatabase.OpenParams.Builder builder2 = new SQLiteDatabase.OpenParams.Builder();
                        builder2.addOpenFlags(SQLiteDatabase.OPEN_READWRITE);
                        SQLiteDatabase.OpenParams dbParams2 = builder2.build();


                        db = SQLiteDatabase.openDatabase(dbFile, dbParams);
                        db2 = SQLiteDatabase.openDatabase(dbFile2, dbParams2);
                        hookSAMethod(loadPackageParam,db,db2);
                        hookMessageDeletion(loadPackageParam, appContext, db, moduleContext);
                    }
                }
            }
        });
    }

    private void hookMessageDeletion(XC_LoadPackage.LoadPackageParam loadPackageParam, Context context, SQLiteDatabase db, Context moduleContext) throws ClassNotFoundException {
        if (!limeOptions.Archived.checked) return;

        XposedBridge.hookAllMethods(
                loadPackageParam.classLoader.loadClass(Constants.REQUEST_HOOK.className),
                Constants.REQUEST_HOOK.methodName,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        String paramValue = param.args[1].toString();
                        if (paramValue.contains("hidden:true")) {
                            String talkId = extractTalkId(paramValue);
                            if (talkId != null) {
                                saveTalkIdToFile(talkId, context);
                                updateArchivedChatsFromFile(db, moduleContext);
                            }
                        }
                        if (paramValue.contains("hidden:false")) {
                            String talkId = extractTalkId(paramValue);
                            if (talkId != null) {

                                deleteTalkIdFromFile(talkId, context);
                                updateArchivedChatsFromFile(db, moduleContext);
                            }
                        }

                    }
                });

    }

    private void deleteTalkIdFromFile(String talkId, Context moduleContext) {
        File dir = moduleContext.getFilesDir();
        File file = new File(dir, "hidelist.txt");

        if (file.exists()) {
            try {
                List<String> lines = new ArrayList<>();
                BufferedReader reader = new BufferedReader(new FileReader(file));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.trim().equals(talkId)) {
                        lines.add(line);
                    }
                }
                reader.close();

                BufferedWriter writer = new BufferedWriter(new FileWriter(file));
                for (String remainingLine : lines) {
                    writer.write(remainingLine);
                    writer.newLine();
                }
                writer.close();
            } catch (IOException ignored) {
            }
        }
    }

    private void hookSAMethod(XC_LoadPackage.LoadPackageParam loadPackageParam,SQLiteDatabase db,SQLiteDatabase db2) {
        Class<?> targetClass = XposedHelpers.findClass(Constants.Archive.className, loadPackageParam.classLoader);

        XposedBridge.hookAllMethods(targetClass, Constants.Archive.methodName, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Context moduleContext = AndroidAppHelper.currentApplication().createPackageContext(
                        "io.github.hiro.lime", Context.CONTEXT_IGNORE_SECURITY);
                Context appContext = AndroidAppHelper.currentApplication();
                if (appContext == null) {
                    return;
                }
                if (limeOptions.Archived.checked) {
                    List<String> chatIds = readChatIdsFromFile(appContext);
                    for (String chatId : chatIds) {
                        if (!chatId.isEmpty()) {
                            updateIsArchived(db, chatId);
                        }
                    }
                }
                if (limeOptions.PinList.checked) {
                    List<Pair<String, Integer>> chatData = PinChatReadFile(appContext);
                    for (Pair<String, Integer> entry : chatData) {
                        if (!entry.first.isEmpty() && entry.second > 0) {
                            PinListUpdate(db,db2, entry.first, entry.second,moduleContext);
                        }

                    }
                    restoreMissingEntries(appContext,db);
                }
            }
        });

    }


    private List<String> readChatIdsFromFile( Context moduleContext) {
        List<String> chatIds = new ArrayList<>();
        File dir = moduleContext.getFilesDir();
        File file = new File(dir, "hidelist.txt");

        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException ignored) {

            }
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                chatIds.add(line.trim());
            }
        } catch (IOException ignored) {
        }
        return chatIds;
    }


    private List<Pair<String, Integer>> PinChatReadFile(Context context) {
        List<Pair<String, Integer>> chatData = new ArrayList<>();
        File dir = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "LimeBackup/Setting"
        );
        File file = new File(dir, "ChatList.txt");

        if (!file.exists()) {
            return chatData;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                String[] parts = line.split(",", 2);
                if (parts.length == 2) {
                    try {
                        String chatId = parts[0].trim();
                        int number = Integer.parseInt(parts[1].trim());
                        chatData.add(new Pair<>(chatId, number));
                    } catch (NumberFormatException e) {
                        // XposedBridge.log("数値変換エラー: " + line);
                    }
                }
            }
        } catch (IOException e) {
            // XposedBridge.log("ファイル読み込みエラー: " + e.getMessage());
        }
        return chatData;
    }


    private void saveTalkIdToFile(String talkId, Context moduleContext) {
        File dir = moduleContext.getFilesDir(); // moduleContextを使用
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File file = new File(dir, "hidelist.txt");

        try {
            if (!file.exists()) {
                file.createNewFile();
            }

            List<String> existingIds = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    existingIds.add(line.trim());
                }
            } catch (IOException ignored) {
            }
            if (!existingIds.contains(talkId.trim())) {
                try (FileWriter writer = new FileWriter(file, true)) {
                    writer.write(talkId + "\n");
                }
            }
        } catch (IOException ignored) {
        }
    }
    private void updateArchivedChatsFromFile(SQLiteDatabase db, Context moduleContext) {
        File dir = moduleContext.getFilesDir();
        File file = new File(dir, "hidelist.txt");

        if (!file.exists()) {
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String chatId;
            while ((chatId = reader.readLine()) != null) {
                chatId = chatId.trim();
                if (!chatId.isEmpty()) {
                    updateIsArchived(db, chatId);
                }
            }
        } catch (IOException ignored) {
        }
    }

    private String extractTalkId(String paramValue) {
        String talkId = null;
        String requestPrefix = "setChatHiddenStatusRequest:SetChatHiddenStatusRequest(reqSeq:0, chatMid:";
        int startIndex = paramValue.indexOf(requestPrefix);

        if (startIndex != -1) {
            int chatMidStartIndex = startIndex + requestPrefix.length();
            int endIndex = paramValue.indexOf(",", chatMidStartIndex);
            if (endIndex == -1) {
                endIndex = paramValue.indexOf(")", chatMidStartIndex);
            }
            if (endIndex != -1) {
                talkId = paramValue.substring(chatMidStartIndex, endIndex).trim();

            }
        }
        if (talkId == null) {
        }
        return talkId;
    }

    private String queryDatabase(SQLiteDatabase db, String query, String... selectionArgs) {
        if (db == null) {

            return null;
        }
        try (Cursor cursor = db.rawQuery(query, selectionArgs)) {
            if (cursor.moveToFirst()) {
                return cursor.getString(0);
            }
        } catch (Exception ignored) {
        }
        return null;
    }
    private void updateDatabase(SQLiteDatabase db, String query, Object... bindArgs) {
        if (db == null) {
            return;
        }
        try {
            db.beginTransaction();
            db.execSQL(query, bindArgs);
            db.setTransactionSuccessful();
        } catch (Exception ignored) {
        } finally {
            db.endTransaction();
        }
    }
    private void updateIsArchived(SQLiteDatabase db, String chatId) {
        String updateQuery = "UPDATE chat SET is_archived = 1 WHERE chat_id = ?";
        updateDatabase(db, updateQuery, chatId);
        String selectQuery = "SELECT is_archived FROM chat WHERE chat_id = ?";
        String result = queryDatabase(db, selectQuery, chatId);
        if (result != null) {
        } else {
        }
    }
    private void PinListUpdate(SQLiteDatabase db, SQLiteDatabase db2, String chatId, int number, Context moduleContext) {
        final long UNIX_MAX = 2147483646999L;
        long newTime = UNIX_MAX - number;

        long originalTime = getOriginalLastCreatedTime(db, chatId);

        if (originalTime == -1 || originalTime == newTime) {
            return;
        }

        saveChangeToFile(chatId, originalTime);

        db.beginTransaction();
        try {
            String updateQuery = "UPDATE chat SET last_created_time = ? WHERE chat_id = ?";
            SQLiteStatement statement = db.compileStatement(updateQuery);
            statement.bindLong(1, newTime);
            statement.bindString(2, chatId);

            statement.executeUpdateDelete();
            db.setTransactionSuccessful();

            // PinListFix(db, db2, moduleContext, chatId);
        } catch (SQLException e) {
            XposedBridge.log("Update error: " + e.getMessage());
        } finally {
            db.endTransaction();
        }
    }

    private void restoreMissingEntries(Context appContext,SQLiteDatabase db) {
        // XposedBridge.log("復元処理開始");

        Map<String, String> chatList = loadChatList(appContext);
        Map<String, String> changeList = loadChangeList(appContext);
        int successCount = 0;
        int failCount = 0;

        try{
            for (Map.Entry<String, String> entry : changeList.entrySet()) {
                String chatId = entry.getKey();
                String originalTime = entry.getValue();

                // XposedBridge.log("処理中: " + chatId + " | 元時間: " + originalTime);

                if (chatList.containsKey(chatId)) {
                    // XposedBridge.log("スキップ: ChatListに存在");
                    continue;
                }

                try {
                    long timeValue = Long.parseLong(originalTime);
                    String updateQuery = "UPDATE chat SET last_created_time = ? WHERE chat_id = ?";
                    SQLiteStatement statement = db.compileStatement(updateQuery);
                    statement.bindLong(1, timeValue);
                    statement.bindString(2, chatId);

                    int result = statement.executeUpdateDelete();
                    if (result > 0) {
                        successCount++;
                        // XposedBridge.log("復元成功: " + chatId);
                    } else {
                        failCount++;
                        // XposedBridge.log("復元失敗: 該当レコードなし");
                    }

                } catch (NumberFormatException e) {
                    // XposedBridge.log("不正な時間形式: " + originalTime);
                    failCount++;
                }
            }
        } catch (SQLException e) {
            // XposedBridge.log("データベースエラー: " );
        }

        // XposedBridge.log("復元処理結果: 成功=" + successCount + ", 失敗=" + failCount);
    }
    private Map<String, String> loadChangeList(Context appContext) {
        Map<String, String> changeMap = new HashMap<>();
        File file = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "LimeBackup/Setting/ChangeList.txt"
        );

        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(",", 2); // 最大2分割
                    if (parts.length == 2) {
                        changeMap.put(parts[0].trim(), parts[1].trim());
                    }
                }
            } catch (IOException e) {
                // XposedBridge.log("ChangeList読み込みエラー: " + e.getMessage());
            }
        }
        return changeMap;
    }
    private Map<String, String> loadChatList(Context appContext) {
        Map<String, String> chatMap = new HashMap<>();
        File file = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "LimeBackup/Setting/ChatList.txt"
        );

        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(",", 2); // chatIdのみ取得
                    if (parts.length >= 1) {
                        chatMap.put(parts[0].trim(), "");
                    }
                }
            } catch (IOException e) {
                // XposedBridge.log("ChatList読み込みエラー: " + e.getMessage());
            }
        }
        return chatMap;
    }
    private long getOriginalLastCreatedTime(SQLiteDatabase db, String chatId) {
        String query = "SELECT last_created_time FROM chat WHERE chat_id = ?";
        try (Cursor cursor = db.rawQuery(query, new String[]{chatId})) {
            if (cursor.moveToFirst()) {
                return cursor.getLong(cursor.getColumnIndex("last_created_time"));
            }
        } catch (SQLiteException e) {
            // XposedBridge.log("値取得エラー: " + e.getMessage());
        }
        return -1; // エラー時は-1を返す
    }

    private void saveChangeToFile(String chatId, long originalTime) { // numberパラメータ削除
        File dir = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "LimeBackup/Setting"
        );

        if (!dir.exists() && !dir.mkdirs()) {
            // XposedBridge.log("ディレクトリ作成失敗");
            return;
        }

        File file = new File(dir, "ChangeList.txt");
        String newEntry = String.format(
                Locale.getDefault(),
                "%s,%d",  // フォーマット変更
                chatId,
                originalTime
        );

        try {
            // 既存のchatIdをチェック
            Set<String> existingChatIds = new HashSet<>();
            if (file.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] parts = line.split(",");
                        if (parts.length > 0) {
                            existingChatIds.add(parts[0].trim());
                        }
                    }
                }
            }

            // chatIdの重複チェック
            if (!existingChatIds.contains(chatId)) {
                try (FileWriter writer = new FileWriter(file, true)) {
                    writer.append(newEntry).append("\n");
                    // XposedBridge.log("変更記録を保存: " + file.getPath());
                }
            } else {
                // XposedBridge.log("重複chatIdのため保存スキップ: " + chatId);
            }

        } catch (IOException e) {
            // XposedBridge.log("ファイル操作エラー: " + e.getMessage());
        }
    }
}