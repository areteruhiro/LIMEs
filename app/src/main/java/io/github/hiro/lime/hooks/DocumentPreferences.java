package io.github.hiro.lime.hooks;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.documentfile.provider.DocumentFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import io.github.hiro.lime.LimeOptions;

public class DocumentPreferences {
    private static final String TAG = "DocumentPreferences";
    private static final String SETTINGS_FILE_NAME = "settings.properties";
    private static final String MIME_TYPE = "application/x-java-properties";

    private final Context mContext;
    private final Uri mTreeUri;
    private DocumentFile mSettingsFile;

    public DocumentPreferences(@NonNull Context context, @NonNull Uri treeUri) {
        this.mContext = context;
        this.mTreeUri = treeUri;
        initialize();
    }

    private void initialize() {
        try {
            logUriDetails("Initializing DocumentPreferences with URI");

            try {
                mContext.getContentResolver().takePersistableUriPermission(
                        mTreeUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION |
                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                );
            } catch (SecurityException e) {
                logError("SecurityException when taking permissions: " + e.getMessage(), e);
                // 権限が不足している場合はここでリターン
                return;
            }

            logDebug("Creating DocumentFile from URI");
            DocumentFile dir = DocumentFile.fromTreeUri(mContext, mTreeUri);

            if (dir == null) {
                logError("Failed to resolve directory from URI: " + mTreeUri);
                return;
            }

            // ディレクトリの存在チェック
            if (!dir.exists()) {
                logWarning("Directory does not exist, attempting to create: " + mTreeUri);

                // 改良されたディレクトリ作成方法
                Uri createdDirUri = createDirectorySafely(dir);
                if (createdDirUri == null) {
                    logError("Directory creation failed");
                    return;
                }

                dir = DocumentFile.fromTreeUri(mContext, createdDirUri);
                if (dir == null || !dir.exists()) {
                    logError("New directory does not exist after creation");
                    return;
                }

                logInfo("Directory created successfully: " + dir.getUri());
            }

            // 設定ファイルのチェック
            mSettingsFile = dir.findFile(SETTINGS_FILE_NAME);
            if (mSettingsFile == null) {
                logDebug("Settings file not found, will create when needed");
            } else {
                logInfo("Settings file found: " + mSettingsFile.getUri());
            }
        } catch (SecurityException e) {
            logError("SecurityException in initialize: " + e.getMessage(), e);
        } catch (Exception e) {
            logError("Unexpected error in initialize: " + e.getMessage(), e);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private Uri createDirectorySafely(DocumentFile parentDir) {
        try {
            String displayName = getDisplayNameFromUri(mTreeUri);
            logDebug("Creating directory with name: " + displayName);

            // DocumentsContract APIを使用してディレクトリを作成
            Uri createdUri = DocumentsContract.createDocument(
                    mContext.getContentResolver(),
                    parentDir.getUri(),
                    DocumentsContract.Document.MIME_TYPE_DIR,
                    displayName
            );

            if (createdUri == null) {
                logError("DocumentsContract.createDocument returned null");
                return null;
            }

            logDebug("Directory created via DocumentsContract: " + createdUri);
            return createdUri;
        } catch (Exception e) {
            logError("Error creating directory with DocumentsContract: " + e.getMessage(), e);
            return null;
        }
    }
    public boolean saveSetting(String key, String value) throws IOException {
        try {
            ensureSettingsFile();

            Properties properties = new Properties();
            if (mSettingsFile != null && mSettingsFile.exists()) {
                try (InputStream is = mContext.getContentResolver().openInputStream(mSettingsFile.getUri())) {
                    properties.load(is);
                } catch (IOException ignored) {}
            }

            properties.setProperty(key, value);

            try (OutputStream os = mContext.getContentResolver().openOutputStream(mSettingsFile.getUri())) {
                properties.store(os, "Updated");
                logInfo("Setting saved: " + key + " = " + value);
                return true;
            }
        } catch (IOException e) {
            logError("Failed to save setting: " + key, e);
            throw e;
        } catch (SecurityException e) {
            logError("SecurityException when saving setting: " + e.getMessage(), e);
            throw new IOException("Permission denied", e);
        }
    }

    public void loadSettings(LimeOptions options) throws IOException {
        try {
            ensureSettingsFile();

            if (mSettingsFile == null || !mSettingsFile.exists()) {
                logWarning("Settings file not available for loading");
                return;
            }

            try (InputStream is = mContext.getContentResolver().openInputStream(mSettingsFile.getUri())) {
                Properties properties = new Properties();
                properties.load(is);

                for (LimeOptions.Option option : options.options) {
                    String value = properties.getProperty(option.name);
                    if (value != null) {
                        option.checked = Boolean.parseBoolean(value);
                    }
                }
                logInfo("Settings loaded successfully");
            }
        } catch (IOException e) {
            logError("Failed to load settings", e);
            throw e;
        } catch (SecurityException e) {
            logError("SecurityException when loading settings: " + e.getMessage(), e);
            throw new IOException("Permission denied", e);
        }
    }

    public String getSetting(String key, String defaultValue) {
        try {
            ensureSettingsFile();

            if (mSettingsFile == null || !mSettingsFile.exists()) {
                logDebug("Settings file not available for getSetting: " + key);
                return defaultValue;
            }

            try (InputStream is = mContext.getContentResolver().openInputStream(mSettingsFile.getUri())) {
                Properties properties = new Properties();
                properties.load(is);
                return properties.getProperty(key, defaultValue);
            }
        } catch (Exception e) {
            logError("Failed to get setting: " + key, e);
            return defaultValue;
        }
    }

    private void ensureSettingsFile() throws IOException {
        if (mSettingsFile != null && mSettingsFile.exists()) {
            return;
        }

        try {
            logDebug("Ensuring settings file exists");

            DocumentFile dir = DocumentFile.fromTreeUri(mContext, mTreeUri);
            if (dir == null) {
                throw new IOException("Failed to resolve directory from URI");
            }

            if (!dir.exists()) {
                throw new IOException("Directory does not exist: " + mTreeUri);
            }

            logDebug("Creating settings file: " + SETTINGS_FILE_NAME);
            mSettingsFile = dir.createFile(MIME_TYPE, SETTINGS_FILE_NAME);

            if (mSettingsFile == null) {
                throw new IOException("createFile returned null");
            }

            if (!mSettingsFile.exists()) {
                throw new IOException("Settings file does not exist after creation");
            }

            // 新規作成時は空の設定で初期化
            Properties properties = new Properties();
            try (OutputStream os = mContext.getContentResolver().openOutputStream(mSettingsFile.getUri())) {
                properties.store(os, "Initial Settings");
                logInfo("Settings file created successfully: " + mSettingsFile.getUri());
            }
        } catch (SecurityException e) {
            logError("SecurityException in ensureSettingsFile: " + e.getMessage(), e);
            throw new IOException("Permission denied", e);
        }
    }

    // 追加の診断メソッド
    private String getDisplayNameFromUri(Uri uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            String displayName = DocumentsContract.getTreeDocumentId(uri);
            if (displayName != null) {
                return displayName.substring(displayName.lastIndexOf(':') + 1);
            }
        }
        return "LimeSettings";
    }

    private void logUriDetails(String context) {
        logDebug(context + " - URI: " + mTreeUri);
        logDebug("URI Scheme: " + mTreeUri.getScheme());
        logDebug("URI Authority: " + mTreeUri.getAuthority());
        logDebug("URI Path: " + mTreeUri.getPath());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            logDebug("Tree Document ID: " + DocumentsContract.getTreeDocumentId(mTreeUri));
        }
    }

    private void logDocumentFileDetails(String prefix, DocumentFile file) {
        if (file == null) {
            logDebug(prefix + ": null");
            return;
        }

        logDebug(prefix + " URI: " + file.getUri());
        logDebug(prefix + " Name: " + file.getName());
        logDebug(prefix + " Type: " + file.getType());
        logDebug(prefix + " Length: " + file.length());
        logDebug(prefix + " Last Modified: " + file.lastModified());
        logDebug(prefix + " Can Read: " + file.canRead());
        logDebug(prefix + " Can Write: " + file.canWrite());
    }

    // ロギングヘルパー
    private void logDebug(String message) {
        Log.d(TAG, message);
    }

    private void logInfo(String message) {
        Log.i(TAG, message);
    }

    private void logWarning(String message) {
        Log.w(TAG, message);
    }

    private void logError(String message) {
        Log.e(TAG, message);
    }

    private void logError(String message, Throwable t) {
        Log.e(TAG, message, t);
    }

    public boolean isAccessible() {
        try {
            DocumentFile dir = DocumentFile.fromTreeUri(mContext, mTreeUri);
            if (dir == null) {
                logDebug("isAccessible: DocumentFile.fromTreeUri returned null");
                return false;
            }

            logDocumentFileDetails("Accessibility Check", dir);
            return dir.exists();
        } catch (SecurityException e) {
            logError("SecurityException in isAccessible: " + e.getMessage(), e);
            return false;
        } catch (Exception e) {
            logError("Unexpected error in isAccessible: " + e.getMessage(), e);
            return false;
        }
    }
}