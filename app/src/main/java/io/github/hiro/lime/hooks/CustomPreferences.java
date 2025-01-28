package io.github.hiro.lime.hooks;

import android.os.Environment;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class CustomPreferences {

    private static final String SETTINGS_DIR = "LimeBackup/Setting";
    private static final String SETTINGS_FILE = "settings.properties";

    private final File settingsFile;

    public CustomPreferences() {
        File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), SETTINGS_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        settingsFile = new File(dir, SETTINGS_FILE);
    }

    public void saveSetting(String key, String value) {
        Properties properties = new Properties();
        try (FileInputStream fis = new FileInputStream(settingsFile)) {
            properties.load(fis);
        } catch (IOException e) {
            // ファイルが存在しない場合、新規作成する
        }

        try (FileOutputStream fos = new FileOutputStream(settingsFile)) {
            properties.setProperty(key, value);
            properties.store(fos, null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getSetting(String key, String defaultValue) {
        Properties properties = new Properties();
        try (FileInputStream fis = new FileInputStream(settingsFile)) {
            properties.load(fis);
            return properties.getProperty(key, defaultValue);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return defaultValue;
    }
}