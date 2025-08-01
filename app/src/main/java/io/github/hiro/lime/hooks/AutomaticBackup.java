package io.github.hiro.lime.hooks;

import android.app.Activity;
import android.app.AndroidAppHelper;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Toast;

import androidx.documentfile.provider.DocumentFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.hiro.lime.LimeOptions;
import io.github.hiro.lime.R;

public class AutomaticBackup implements IHook {
    @Override
    public void hook(LimeOptions limeOptions, XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {

        XposedHelpers.findAndHookMethod("jp.naver.line.android.activity.schemeservice.LineSchemeServiceActivity",
                loadPackageParam.classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Context moduleContext = AndroidAppHelper.currentApplication().createPackageContext(
                                "io.github.hiro.lime", Context.CONTEXT_IGNORE_SECURITY);
                        Intent intent = ((Activity) param.thisObject).getIntent();
                        handleIntent(intent, param.thisObject, moduleContext);
                    }
                });
    }

    private void handleIntent(Intent intent, Object activity, Context moduleContext) {
        if (intent != null) {
            String text = intent.getStringExtra(Intent.EXTRA_TEXT);

            if (moduleContext.getResources().getString(R.string.Talk_Back_up).equals(text)) {
                backupChatHistory(((Activity) activity).getApplicationContext(), moduleContext);
            }
            if (moduleContext.getResources().getString(R.string.Talk_Picture_Back_up).equals(text)) {
                backupChatsFolder(((Activity) activity).getApplicationContext(), moduleContext);
            }

            if (moduleContext.getResources().getString(R.string.BackUp_Stat).equals(text)) {
                backupChatHistory(((Activity) activity).getApplicationContext(), moduleContext);
                backupChatsFolder(((Activity) activity).getApplicationContext(), moduleContext);
            }
        }
    }


    private void backupChatHistory(Context appCtx, Context moduleCtx) {

        File original = appCtx.getDatabasePath("naver_line");
        String backupUriS = loadBackupUri(appCtx);

        if (backupUriS == null) {
            showToast(appCtx, moduleCtx.getString(
                    R.string.Talk_Auto_Back_up_Error_No_URI));
            return;
        }

        try {
            Uri treeUri = Uri.parse(backupUriS);
            DocumentFile rootDir = DocumentFile.fromTreeUri(appCtx, treeUri);

            if (rootDir == null || !rootDir.exists()) {
                showToast(appCtx, moduleCtx.getString(
                        R.string.Talk_Auto_Back_up_Error_No_Access));
                return;
            }

            String fileName = "naver_line_backup.db";
            DocumentFile exist = rootDir.findFile(fileName);
            if (exist != null) exist.delete();

            DocumentFile newFile = rootDir.createFile(
                    "application/x-sqlite3", fileName);
            if (newFile == null) {
                showToast(appCtx, moduleCtx.getString(
                        R.string.Talk_Auto_Back_up_Error_Create_File));
                return;
            }

            try (InputStream in = new FileInputStream(original);
                 OutputStream out = appCtx.getContentResolver()
                         .openOutputStream(newFile.getUri())) {

                byte[] buf = new byte[4096];
                int len;
                while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
            }

            showToast(appCtx, moduleCtx.getString(
                    R.string.Talk_Auto_Back_up_Success));

        } catch (FileNotFoundException e) {
            XposedBridge.log("Lime-Backup DB: File not found → " + e);
            showToast(appCtx, moduleCtx.getString(
                    R.string.Talk_Auto_Back_up_Error_Create_File));
        } catch (SecurityException e) {
            XposedBridge.log("Lime-Backup DB: SAF permission error → " + e);
            showToast(appCtx, moduleCtx.getString(
                    R.string.Talk_Auto_Back_up_Error_No_Access));
        } catch (IOException e) {
            XposedBridge.log("Lime-Backup DB: I/O error → " + e);
            showToast(appCtx, moduleCtx.getString(
                    R.string.Talk_Auto_Back_up_Error));
        }
    }

    private void backupChatsFolder(Context appCtx, Context moduleCtx) {

        File srcChats = new File(Environment.getExternalStorageDirectory(),
                "Android/data/jp.naver.line.android/files/chats");
        String backupUriS = loadBackupUri(appCtx);

        if (backupUriS == null) {
            showToast(appCtx, moduleCtx.getString(
                    R.string.Talk_Picture_Back_up_Error_No_URI));
            return;
        }

        try {
            Uri treeUri = Uri.parse(backupUriS);
            DocumentFile rootDir = DocumentFile.fromTreeUri(appCtx, treeUri);

            if (rootDir == null || !rootDir.exists()) {
                showToast(appCtx, moduleCtx.getString(
                        R.string.Talk_Picture_Back_up_Error_No_Access));
                return;
            }

            DocumentFile chatsDir = rootDir.findFile("chats_backup");
            if (chatsDir != null) chatsDir.delete();        // 上書き用に削除
            chatsDir = rootDir.createDirectory("chats_backup");
            if (chatsDir == null) {
                showToast(appCtx, moduleCtx.getString(
                        R.string.Talk_Picture_Back_up_Error_Create_Dir));
                return;
            }

            copyDirectoryToDocumentFile(appCtx, srcChats, chatsDir);

            showToast(appCtx, moduleCtx.getString(
                    R.string.Talk_Picture_Back_up_Success));

        } catch (SecurityException e) {
            XposedBridge.log("Lime-Backup Chats: SAF permission error → " + e);
            showToast(appCtx, moduleCtx.getString(
                    R.string.Talk_Picture_Back_up_Error_No_Access));
        } catch (IOException | NullPointerException e) {
            // NullPointerException は SAF が null を返した時を想定
            XposedBridge.log("Lime-Backup Chats: error → " + e);
            showToast(appCtx, moduleCtx.getString(
                    R.string.Talk_Picture_Back_up_Error));
        }
    }

    /* ─────────────── ③ 再帰コピー (DocumentFile 版) ─────────────── */
    private void copyDirectoryToDocumentFile(Context ctx,
                                             File srcDir,
                                             DocumentFile destDir) throws IOException {

        File[] children = srcDir.listFiles();
        if (children == null) return;

        for (File child : children) {
            if (child.isDirectory()) {

                DocumentFile newDir = destDir.createDirectory(child.getName());
                if (newDir != null) {
                    copyDirectoryToDocumentFile(ctx, child, newDir);
                }

            } else {
                DocumentFile newFile = destDir.createFile(
                        getMimeType(child.getName()), child.getName());
                if (newFile == null) continue;

                try (InputStream in = new FileInputStream(child);
                     OutputStream out = ctx.getContentResolver()
                             .openOutputStream(newFile.getUri())) {

                    byte[] buf = new byte[4096];
                    int len;
                    while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
                }
            }
        }
    }

    private String loadBackupUri(Context context) {
        File settingsFile = new File(context.getFilesDir(), "LimeBackup/backup_uri.txt");
        if (!settingsFile.exists()) return null;

        try (BufferedReader br = new BufferedReader(new FileReader(settingsFile))) {
            return br.readLine();
        } catch (IOException e) {
            XposedBridge.log("Lime: Error reading backup URI: " + e.getMessage());
            return null;
        }
    }


    private String getMimeType(String fileName) {
        String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        switch (extension) {
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "mp4":
                return "video/mp4";
            case "db":
                return "application/x-sqlite3";
            default:
                return "application/octet-stream";
        }
    }

    private void copyDirectory(File sourceDir, File destDir) throws IOException {
        if (!sourceDir.exists()) {
            throw new IOException("Source directory does not exist: " + sourceDir.getAbsolutePath());
        }

        if (!destDir.exists()) {
            destDir.mkdirs();
        }
        File[] files = sourceDir.listFiles();
        if (files != null) {
            for (File file : files) {
                File destFile = new File(destDir, file.getName());
                if (file.isDirectory()) {
                    copyDirectory(file, destFile);
                } else {
                    copyFile(file, destFile);
                }
            }
        }
    }
    private void copyFile(File sourceFile, File destFile) throws IOException {
        if (destFile.exists()) {
            destFile.delete();
        }
        try (FileChannel sourceChannel = new FileInputStream(sourceFile).getChannel();
             FileChannel destChannel = new FileOutputStream(destFile).getChannel()) {
            destChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
        }
    }
    private void showToast(final Context context, final String message) {
        new android.os.Handler(context.getMainLooper()).post(() ->
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        );
    }
}
