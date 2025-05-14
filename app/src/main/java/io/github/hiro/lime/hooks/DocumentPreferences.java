package io.github.hiro.lime.hooks;

import static io.github.hiro.lime.Main.limeOptions;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import de.robv.android.xposed.XposedBridge;
import io.github.hiro.lime.LimeOptions;

public class DocumentPreferences {
    private static final String SETTINGS_FILE = "settings.properties";
    private final Context context;
    private final Uri treeUri;

    public DocumentPreferences(Context context, Uri treeUri) {
        this.context = context;
        this.treeUri = treeUri;
    }

    public void recreateSettingsFile(Context context) throws IOException {
        final String TAG = "LimeDocPrefs";

        try {
            XposedBridge.log(TAG + ": Starting file recreation process");

            DocumentFile dir = DocumentFile.fromTreeUri(context, treeUri);
            XposedBridge.log(TAG + ": Resolved directory URI: " + treeUri);

            if (dir == null || !dir.exists()) {
                String msg = "Directory not accessible - URI: " + treeUri;
                XposedBridge.log(TAG + ": " + msg);
                throw new IOException(msg);
            }

            DocumentFile existingFile = dir.findFile("settings.properties");
            if (existingFile != null) {
                XposedBridge.log(TAG + ": Found existing file: " + existingFile.getUri());

                if (existingFile.delete()) {
                    XposedBridge.log(TAG + ": Successfully deleted existing file");
                } else {
                    String msg = "Failed to delete file: " + existingFile.getUri();
                    XposedBridge.log(TAG + ": " + msg);
                    throw new IOException(msg);
                }
            } else {
                XposedBridge.log(TAG + ": No existing file to delete");
            }

            XposedBridge.log(TAG + ": Starting copy from internal storage");
            copyFromInternalStorage(context, dir);
            XposedBridge.log(TAG + ": File recreation completed successfully");

        } catch (IOException e) {
            XposedBridge.log(TAG + ": Critical error in recreateSettingsFile: " + e.getClass().getSimpleName());
            XposedBridge.log(TAG + ": Error details: " + e.getMessage());
            XposedBridge.log(TAG + ": Stack trace: " + Log.getStackTraceString(e));
            throw e;
        }
    }

    private void copyFromInternalStorage(Context context, DocumentFile targetDir) throws IOException {
        final String TAG = "LimeDocPrefs-Copy";

        try {
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File limeBackupDir = new File(downloadsDir, "LimeBackup");
            File settingDir = new File(limeBackupDir, "Setting");

            if (!settingDir.exists() && !settingDir.mkdirs()) {
                throw new IOException("Failed to create directory: " + settingDir.getAbsolutePath());
            }

            File internalFile = new File(settingDir, "settings.properties");
            XposedBridge.log(TAG + ": External file path: " + internalFile.getAbsolutePath());

            if (!internalFile.exists()) {
                XposedBridge.log(TAG + ": External file not found, creating default");
                createDefaultSettingsFile(internalFile);
            }

            DocumentFile newFile = targetDir.createFile(
                    "application/x-java-properties",
                    "settings.properties"
            );

            if (newFile == null) {
                throw new IOException("Failed to create new file in target directory");
            }
            XposedBridge.log(TAG + ": Created new file: " + newFile.getUri());

            try (InputStream is = new FileInputStream(internalFile);
                 OutputStream os = context.getContentResolver().openOutputStream(newFile.getUri())) {

                XposedBridge.log(TAG + ": Starting file copy...");
                long startTime = System.currentTimeMillis();

                byte[] buffer = new byte[4096];
                int length;
                long totalBytes = 0;

                while ((length = is.read(buffer)) > 0) {
                    os.write(buffer, 0, length);
                    totalBytes += length;
                }

                long duration = System.currentTimeMillis() - startTime;
                XposedBridge.log(TAG + ": Copy completed - Bytes: " + totalBytes
                        + ", Duration: " + duration + "ms");
            }

        } catch (IOException e) {
            XposedBridge.log(TAG + ": Copy failed: " + e.getClass().getSimpleName());
            XposedBridge.log(TAG + ": Error details: " + e.getMessage());
            XposedBridge.log(TAG + ": Stack trace: " + Log.getStackTraceString(e));
            throw e;
        }
    }

    private void createDefaultSettingsFile(File file) throws IOException {
        final String TAG = "LimeDocPrefs-Default";

        try {
            XposedBridge.log(TAG + ": Creating default settings file: " + file.getAbsolutePath());

            if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
                throw new IOException("Failed to create directory structure");
            }

            try (FileOutputStream fos = new FileOutputStream(file);
                 OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {

                Properties props = new Properties();
                for (LimeOptions.Option option : limeOptions.options) {
                    props.setProperty(option.name, String.valueOf(option.checked));
                    XposedBridge.log(TAG + ": Setting default value - "
                            + option.name + ": " + option.checked);
                }

                props.store(osw, "Default Settings");
                XposedBridge.log(TAG + ": Successfully stored default settings");
            }

            XposedBridge.log(TAG + ": File created successfully - Size: "
                    + file.length() + " bytes");

        } catch (IOException e) {
            XposedBridge.log(TAG + ": Failed to create default file: " + e.getMessage());
            XposedBridge.log(TAG + ": Stack trace: " + Log.getStackTraceString(e));
            throw e;
        }
    }

    public void saveSettings(LimeOptions limeOptions) throws IOException {
        DocumentFile dir = DocumentFile.fromTreeUri(context, treeUri);
        if (dir == null || !dir.exists()) {
            throw new IOException("Directory not accessible");
        }

        DocumentFile file = dir.findFile(SETTINGS_FILE);
        if (file == null) {
            file = dir.createFile("text/plain", SETTINGS_FILE);
        }

        if (file == null) {
            throw new IOException("File creation failed");
        }

        Properties properties = new Properties();
        for (LimeOptions.Option option : limeOptions.options) {
            properties.setProperty(option.name, String.valueOf(option.checked));
        }

        try (OutputStream os = context.getContentResolver().openOutputStream(file.getUri())) {
            properties.store(os, "Lime Backup Settings");
        }
    }

    public void loadSettings(LimeOptions limeOptions) throws IOException {
        DocumentFile dir = DocumentFile.fromTreeUri(context, treeUri);
        if (dir == null || !dir.exists()) {
            throw new IOException("Directory not accessible");
        }

        DocumentFile file = dir.findFile(SETTINGS_FILE);
        if (file == null) {
            createDefaultSettings(dir);
            file = dir.findFile(SETTINGS_FILE);
            if (file == null) {
                throw new IOException("Settings file not found");
            }
        }

        try (InputStream is = context.getContentResolver().openInputStream(file.getUri())) {
            Properties properties = new Properties();
            properties.load(is);

            for (LimeOptions.Option option : limeOptions.options) {
                String value = properties.getProperty(option.name, String.valueOf(option.checked));
                option.checked = Boolean.parseBoolean(value);
            }
        }
    }

    private void createDefaultSettings(DocumentFile dir) throws IOException {
        DocumentFile file = dir.createFile("text/plain", SETTINGS_FILE);
        if (file == null) {
            throw new IOException("Default settings creation failed");
        }

        Properties properties = new Properties();
        for (LimeOptions.Option option : limeOptions.options) {
            properties.setProperty(option.name, String.valueOf(option.checked));
        }

        try (OutputStream os = context.getContentResolver().openOutputStream(file.getUri())) {
            properties.store(os, "Default Settings");
        }
    }
}
