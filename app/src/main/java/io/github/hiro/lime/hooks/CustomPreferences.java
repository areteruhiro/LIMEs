package io.github.hiro.lime.hooks;

import android.app.AndroidAppHelper;
import android.content.Context;
import android.content.pm.PackageManager;
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

    public CustomPreferences() throws PackageManager.NameNotFoundException {
        Context moduleContext = AndroidAppHelper.currentApplication().createPackageContext(
                "io.github.hiro.lime", Context.CONTEXT_IGNORE_SECURITY);
        File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), SETTINGS_DIR);
        if (!dir.exists() && !dir.mkdirs()) {
            // 最初のディレクトリの作成に失敗した場合
            dir = new File(Environment.getExternalStorageDirectory(), "Android/data/jp.naver.line.android/");
            if (!dir.exists() && !dir.mkdirs()) {
                // 次のディレクトリの作成に失敗した場合
                dir = moduleContext.getFilesDir(); // アプリの内部ストレージを使用
            }
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