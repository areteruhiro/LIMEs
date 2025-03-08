package io.github.hiro.lime.hooks;

import android.app.AndroidAppHelper;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Environment;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
public class CustomPreferences {
    private static final String SETTINGS_DIR = "LimeBackup/Setting";
    private static final String SETTINGS_FILE = "settings.properties";
    private final File settingsFile;
    private File settingsDir = null;

    public CustomPreferences() throws PackageManager.NameNotFoundException {
        File settingsDir1;
        File baseDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        settingsDir1 = new File(baseDir, SETTINGS_DIR);

        // 初期ディレクトリ作成試行
        if (!settingsDir1.exists() && !settingsDir1.mkdirs()) {
            // フォールバックディレクトリ
            File fallbackDir = new File(
                    Environment.getExternalStorageDirectory(),
                    "Android/data/jp.naver.line.android/"
            );
            settingsDir1 = new File(fallbackDir, SETTINGS_DIR);
            if (!settingsDir.exists() && !settingsDir.mkdirs()) {
                throw new PackageManager.NameNotFoundException(
                        "Directory creation failed: " + settingsDir.getAbsolutePath()
                );
            }
        }
        settingsDir = settingsDir1;
        settingsFile = new File(settingsDir, SETTINGS_FILE);
    }

    public boolean saveSetting(String key, String value) {
        Properties properties = new Properties();
        boolean success = false;

        if (settingsFile.exists()) {
            try (FileInputStream fis = new FileInputStream(settingsFile)) {
                properties.load(fis);
            } catch (IOException ignored) {}
        }

        properties.setProperty(key, value);
        for (int attempt = 0; attempt < 2; attempt++) {
            try (FileOutputStream fos = new FileOutputStream(settingsFile)) {
                properties.store(fos, "Updated: " + new Date());
                success = true;
                break;
            } catch (IOException e) {
                handleSaveError(e);
                if (attempt == 0) prepareRetryEnvironment();
            }
        }
        return success;
    }

    private void handleSaveError(IOException e) {
        e.printStackTrace();
        if (settingsFile.exists()) {
            boolean deleted = settingsFile.delete();
            System.out.println("File deletion " + (deleted ? "successful" : "failed"));
        }
    }

    private void prepareRetryEnvironment() {
        // ディレクトリ再作成試行
        if (!settingsDir.exists()) {
            boolean dirCreated = settingsDir.mkdirs();
            System.out.println("Directory recreated: " + dirCreated);
        }
    }

    public String getSetting(String key, String defaultValue) {
        if (!settingsFile.exists()) return defaultValue;

        Properties properties = new Properties();
        try (FileInputStream fis = new FileInputStream(settingsFile)) {
            properties.load(fis);
            return properties.getProperty(key, defaultValue);
        } catch (IOException e) {
            e.printStackTrace();
            return defaultValue;
        }
    }
}