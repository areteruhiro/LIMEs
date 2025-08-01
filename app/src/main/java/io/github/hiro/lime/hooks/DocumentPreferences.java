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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Properties;

import io.github.hiro.lime.LimeOptions;

public class DocumentPreferences {
    private static final String TAG = "DocumentPreferences";
    private static final String SETTINGS_FILE_NAME = "settings.properties";
    private static final String MIME_TYPE = "application/x-java-properties";
    private static final String SETTINGS_DIR = "LimeBackup/Setting";

    private final Context mContext;
    private final Uri mTreeUri;
    private DocumentFile mSettingsFile;
    private File mInternalSettingsFile;

    public DocumentPreferences(@NonNull Context context, @NonNull Uri treeUri) {
        this.mContext = context;
        this.mTreeUri = treeUri;

        initializeInternalStorage();
        initializeUriStorage();
    }

    private void initializeInternalStorage() {
        try {
            File internalDir = new File(mContext.getFilesDir(), SETTINGS_DIR);
            if (!internalDir.exists() && !internalDir.mkdirs()) {
                logError("Failed to create internal directory");
                return;
            }

            mInternalSettingsFile = new File(internalDir, SETTINGS_FILE_NAME);
            if (!mInternalSettingsFile.exists()) {
                logDebug("Internal settings file not found, will create when needed");
            } else {
                logInfo("Internal settings file found: " + mInternalSettingsFile.getAbsolutePath());
            }
        } catch (SecurityException e) {
            logError("SecurityException in initializeInternalStorage: " + e.getMessage(), e);
        } catch (Exception e) {
            logError("Unexpected error in initializeInternalStorage: " + e.getMessage(), e);
        }
    }

    private void initializeUriStorage() {
        try {
            logUriDetails("Initializing URI storage");

            try {
                mContext.getContentResolver().takePersistableUriPermission(
                        mTreeUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION |
                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                );
            } catch (SecurityException e) {
                logError("SecurityException when taking permissions: " + e.getMessage(), e);
                return;
            }

            logDebug("Creating DocumentFile from URI");
            DocumentFile dir = DocumentFile.fromTreeUri(mContext, mTreeUri);

            if (dir == null) {
                logError("Failed to resolve directory from URI: " + mTreeUri);
                return;
            }

            if (!dir.exists()) {
                logWarning("Directory does not exist, attempting to create: " + mTreeUri);
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

            mSettingsFile = dir.findFile(SETTINGS_FILE_NAME);
            if (mSettingsFile == null) {
                logDebug("URI settings file not found, will create when needed");
            } else {
                logInfo("URI settings file found: " + mSettingsFile.getUri());
            }
        } catch (SecurityException e) {
            logError("SecurityException in initializeUriStorage: " + e.getMessage(), e);
        } catch (Exception e) {
            logError("Unexpected error in initializeUriStorage: " + e.getMessage(), e);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private Uri createDirectorySafely(DocumentFile parentDir) {
        try {
            String displayName = getDisplayNameFromUri(mTreeUri);
            logDebug("Creating directory with name: " + displayName);

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
        // まず内部ストレージに保存
        boolean internalSuccess = saveToInternalStorage(key, value);

        boolean uriSuccess = saveToUriStorage(key, value);

        return internalSuccess && uriSuccess;
    }

    private boolean saveToInternalStorage(String key, String value) {
        if (mInternalSettingsFile == null) {
            logError("Internal storage not initialized");
            return false;
        }

        try {
            Properties properties = new Properties();
            if (mInternalSettingsFile.exists()) {
                try (InputStream is = new FileInputStream(mInternalSettingsFile)) {
                    properties.load(is);
                }
            }

            properties.setProperty(key, value);

            try (OutputStream os = new FileOutputStream(mInternalSettingsFile)) {
                properties.store(os, "Updated: " + new Date());
                logInfo("Setting saved to internal storage: " + key + " = " + value);
                return true;
            }
        } catch (IOException e) {
            logError("Failed to save setting to internal storage: " + key, e);
            return false;
        }
    }

    private boolean saveToUriStorage(String key, String value) throws IOException {
        try {
            ensureUriSettingsFile();

            Properties properties = new Properties();
            if (mSettingsFile != null && mSettingsFile.exists()) {
                try (InputStream is = mContext.getContentResolver().openInputStream(mSettingsFile.getUri())) {
                    properties.load(is);
                }
            }

            properties.setProperty(key, value);

            if (mSettingsFile != null) {
                try (OutputStream os = mContext.getContentResolver().openOutputStream(mSettingsFile.getUri())) {
                    properties.store(os, "Updated");
                    logInfo("Setting saved to URI storage: " + key + " = " + value);
                    return true;
                }
            }
            return false;
        } catch (IOException e) {
            logError("Failed to save setting to URI storage: " + key, e);
            throw e;
        } catch (SecurityException e) {
            logError("SecurityException when saving to URI storage: " + e.getMessage(), e);
            throw new IOException("Permission denied", e);
        }
    }

    public void loadSettings(LimeOptions options) throws IOException {
        // まず内部ストレージから読み込み
        loadFromInternalStorage(options);

        // URIストレージから読み込み (内部ストレージの設定を上書き)
        loadFromUriStorage(options);
    }

    private void loadFromInternalStorage(LimeOptions options) {
        if (mInternalSettingsFile == null || !mInternalSettingsFile.exists()) {
            logDebug("Internal settings file not available for loading");
            return;
        }

        try (InputStream is = new FileInputStream(mInternalSettingsFile)) {
            Properties properties = new Properties();
            properties.load(is);
            applyPropertiesToOptions(properties, options);
            logInfo("Settings loaded from internal storage");
        } catch (IOException e) {
            logError("Failed to load settings from internal storage", e);
        }
    }

    private void loadFromUriStorage(LimeOptions options) throws IOException {
        try {
            ensureUriSettingsFile();

            if (mSettingsFile == null || !mSettingsFile.exists()) {
                logDebug("URI settings file not available for loading");
                return;
            }

            try (InputStream is = mContext.getContentResolver().openInputStream(mSettingsFile.getUri())) {
                Properties properties = new Properties();
                properties.load(is);
                applyPropertiesToOptions(properties, options);
                logInfo("Settings loaded from URI storage");
            }
        } catch (IOException e) {
            logError("Failed to load settings from URI storage", e);
            throw e;
        } catch (SecurityException e) {
            logError("SecurityException when loading from URI storage: " + e.getMessage(), e);
            throw new IOException("Permission denied", e);
        }
    }

    private void applyPropertiesToOptions(Properties properties, LimeOptions options) {
        for (LimeOptions.Option option : options.options) {
            String value = properties.getProperty(option.name);
            if (value != null) {
                option.checked = Boolean.parseBoolean(value);
            }
        }
    }

    public String getSetting(String key, String defaultValue) {
        String value = getFromInternalStorage(key);
        if (value != null) {
            return value;
        }
        value = getFromUriStorage(key);
        if (value != null) {
            return value;
        }

        return defaultValue;
    }

    private String getFromInternalStorage(String key) {
        if (mInternalSettingsFile == null || !mInternalSettingsFile.exists()) {
            return null;
        }

        try (InputStream is = new FileInputStream(mInternalSettingsFile)) {
            Properties properties = new Properties();
            properties.load(is);
            return properties.getProperty(key);
        } catch (IOException e) {
            logError("Failed to get setting from internal storage: " + key, e);
            return null;
        }
    }

    private String getFromUriStorage(String key) {
        try {
            if (mSettingsFile == null || !mSettingsFile.exists()) {
                return null;
            }

            try (InputStream is = mContext.getContentResolver().openInputStream(mSettingsFile.getUri())) {
                Properties properties = new Properties();
                properties.load(is);
                return properties.getProperty(key);
            }
        } catch (Exception e) {
            logError("Failed to get setting from URI storage: " + key, e);
            return null;
        }
    }

    private void ensureUriSettingsFile() throws IOException {
        if (mSettingsFile != null && mSettingsFile.exists()) {
            return;
        }

        try {
            logDebug("Ensuring URI settings file exists");

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

            Properties properties = new Properties();
            try (OutputStream os = mContext.getContentResolver().openOutputStream(mSettingsFile.getUri())) {
                properties.store(os, "Initial Settings");
                logInfo("URI settings file created successfully: " + mSettingsFile.getUri());
            }
        } catch (SecurityException e) {
            logError("SecurityException in ensureUriSettingsFile: " + e.getMessage(), e);
            throw new IOException("Permission denied", e);
        }
    }

    // ロギングとヘルパーメソッド
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
        // 内部ストレージまたはURIストレージが利用可能か
        boolean internalAccessible = mInternalSettingsFile != null && mInternalSettingsFile.exists();
        boolean uriAccessible = false;

        try {
            DocumentFile dir = DocumentFile.fromTreeUri(mContext, mTreeUri);
            uriAccessible = dir != null && dir.exists();
        } catch (Exception e) {
            logError("Error checking URI accessibility", e);
        }

        return internalAccessible || uriAccessible;
    }
}