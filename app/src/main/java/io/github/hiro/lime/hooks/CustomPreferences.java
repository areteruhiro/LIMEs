package io.github.hiro.lime.hooks;

import android.app.AndroidAppHelper;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Environment;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

public class CustomPreferences {
    private static final String SETTINGS_DIR = "LimeBackup/Setting";
    private static final String SETTINGS_FILE = "settings.properties";
    private final File settingsFile;
    public CustomPreferences() throws PackageManager.NameNotFoundException {
        File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), SETTINGS_DIR);
        if (!dir.exists() && !dir.mkdirs()) {
            dir = new File(Environment.getExternalStorageDirectory(), "Android/data/jp.naver.line.android/");
        }
        settingsFile = new File(dir, SETTINGS_FILE);
    }
    public boolean saveSetting(String key, String value) {
        Properties properties = new Properties();
        boolean success = false;

        try (FileInputStream fis = new FileInputStream(settingsFile)) {
            properties.load(fis);
        } catch (IOException ignored) {
        }

        try (FileOutputStream fos = new FileOutputStream(settingsFile)) {
            properties.setProperty(key, value);
            properties.store(fos, null);
            success = true;
        } catch (IOException e) {
            try {
                if (settingsFile.exists()) {
                    settingsFile.delete();
                }
                properties.setProperty(key, value);
                try (FileOutputStream retryFos = new FileOutputStream(settingsFile)) {
                    properties.store(retryFos, null);
                    success = true;
                }
            } catch (IOException ex) {
                ex.printStackTrace();
                success = false;
            }
        }
        return success;
    }

    public String getSetting(String key, String defaultValue) {
        Properties properties = new Properties();
        Set<String> keys = new HashSet<>();

        try (FileInputStream fis = new FileInputStream(settingsFile)) {
            properties.load(fis);
            for (String propertyKey : properties.stringPropertyNames()) {
                if (propertyKey.equals(key)) {
                    if (keys.contains(key)) {
                        return properties.getProperty(key, defaultValue);
                    }
                    keys.add(key);
                }
            }
            return properties.getProperty(key, defaultValue);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return defaultValue;
    }
}