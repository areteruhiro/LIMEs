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
                        handleIntent(intent, param.thisObject,moduleContext);
                    }
                });
    }

    private void handleIntent(Intent intent, Object activity,Context moduleContext) {
        if (intent != null) {
            String text = intent.getStringExtra(Intent.EXTRA_TEXT);

            if (moduleContext.getResources().getString(R.string.Talk_Back_up).equals(text)) {
                backupChatHistory(((Activity) activity).getApplicationContext(),moduleContext);
            }
            if (moduleContext.getResources().getString(R.string.Talk_Picture_Back_up).equals(text)) {
                backupChatsFolder(((Activity) activity).getApplicationContext(),moduleContext);
            }

            if (moduleContext.getResources().getString(R.string.BackUp_Stat).equals(text)) {
                backupChatHistory(((Activity) activity).getApplicationContext(),moduleContext);
                backupChatsFolder(((Activity) activity).getApplicationContext(),moduleContext);
            }
        }
    }


    private void backupChatHistory(Context appContext, Context moduleContext) {
        File originalDbFile = appContext.getDatabasePath("naver_line");
        String backupUriStr = loadBackupUri(moduleContext);

        if (backupUriStr == null) {
            showToast(appContext, moduleContext.getResources().getString(R.string.Talk_Auto_Back_up_Error_No_URI));
            return;
        }

        try {
            Uri backupUri = Uri.parse(backupUriStr);
            DocumentFile backupDir = DocumentFile.fromTreeUri(appContext, backupUri);

            if (backupDir == null || !backupDir.exists()) {
                showToast(appContext, moduleContext.getResources().getString(R.string.Talk_Auto_Back_up_Error_No_Access));
                return;
            }
            String backupFileName = "naver_line_backup.db";
            DocumentFile existingFile = backupDir.findFile(backupFileName);

            if (existingFile != null) {
                existingFile.delete();
            }

            DocumentFile newFile = backupDir.createFile("application/x-sqlite3", backupFileName);
            if (newFile == null) {
                showToast(appContext, moduleContext.getResources().getString(R.string.Talk_Auto_Back_up_Error_Create_File));
                return;
            }

            try (InputStream in = new FileInputStream(originalDbFile);
                 OutputStream out = appContext.getContentResolver().openOutputStream(newFile.getUri())) {
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            }

            showToast(appContext, moduleContext.getResources().getString(R.string.Talk_Auto_Back_up_Success));
        } catch (IOException e) {
            showToast(appContext, moduleContext.getResources().getString(R.string.Talk_Auto_Back_up_Error));
        }
    }

    private void backupChatsFolder(Context appContext, Context moduleContext) {
        File originalChatsDir = new File(Environment.getExternalStorageDirectory(), "Android/data/jp.naver.line.android/files/chats");
        String backupUriStr = loadBackupUri(moduleContext);

        if (backupUriStr == null) {
            showToast(appContext, moduleContext.getResources().getString(R.string.Talk_Picture_Back_up_Error_No_URI));
            return;
        }

        try {
            Uri backupUri = Uri.parse(backupUriStr);
            DocumentFile backupDir = DocumentFile.fromTreeUri(appContext, backupUri);

            if (backupDir == null || !backupDir.exists()) {
                showToast(appContext, moduleContext.getResources().getString(R.string.Talk_Picture_Back_up_Error_No_Access));
                return;
            }

            DocumentFile backupChatsDir = backupDir.findFile("chats_backup");
            if (backupChatsDir != null && backupChatsDir.exists()) {
                backupChatsDir.delete();
            }

            backupChatsDir = backupDir.createDirectory("chats_backup");
            if (backupChatsDir == null) {
                showToast(appContext, moduleContext.getResources().getString(R.string.Talk_Picture_Back_up_Error_Create_Dir));
                return;
            }

            copyDirectoryToDocumentFile(appContext, originalChatsDir, backupChatsDir);

            showToast(appContext, moduleContext.getResources().getString(R.string.Talk_Picture_Back_up_Success));
        } catch (IOException e) {
            showToast(appContext, moduleContext.getResources().getString(R.string.Talk_Picture_Back_up_Error));
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


    private void copyDirectoryToDocumentFile(Context context, File srcDir, DocumentFile destDir) throws IOException {
        File[] files = srcDir.listFiles();
        if (files == null) return;

        for (File srcFile : files) {
            if (srcFile.isDirectory()) {
                DocumentFile newDir = destDir.createDirectory(srcFile.getName());
                if (newDir != null) {
                    copyDirectoryToDocumentFile(context, srcFile, newDir);
                }
            } else {
                DocumentFile newFile = destDir.createFile(getMimeType(srcFile.getName()), srcFile.getName());
                if (newFile != null) {
                    try (InputStream in = new FileInputStream(srcFile);
                         OutputStream out = context.getContentResolver().openOutputStream(newFile.getUri())) {
                        byte[] buf = new byte[1024];
                        int len;
                        while ((len = in.read(buf)) > 0) {
                            out.write(buf, 0, len);
                        }
                    }
                }
            }
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
