package io.github.hiro.lime.hooks;

import android.content.Context;
import android.content.pm.PackageManager;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;

import io.github.hiro.lime.R;

public class CustomPreferences {
    private static final String SETTINGS_DIR = "LimeBackup/Setting";
    private static final String SETTINGS_FILE = "settings.properties";

    private final File settingsFileInternal;
    private final boolean isXposedContext;

    public CustomPreferences(Context context) throws PackageManager.NameNotFoundException, IOException {
        if (context != null && !context.getPackageName().equals("android")) {
            this.isXposedContext = true;
            File internalDir = new File(context.getFilesDir(), SETTINGS_DIR);
            if (!internalDir.exists() && !internalDir.mkdirs()) {
                Toast.makeText(
                        context,
                        context.getString(R.string.Error_Create_setting_Button)
                                + "\nError: " + context.getString(R.string.save_failed),
                        Toast.LENGTH_LONG
                ).show();
                throw new IOException("Failed to create internal directory");
            }

            settingsFileInternal = new File(internalDir, SETTINGS_FILE);

            // 設定ファイルが存在しない場合は新規作成
            if (!settingsFileInternal.exists()) {
                try (FileOutputStream fos = new FileOutputStream(settingsFileInternal)) {
                    new Properties().store(fos, "Initial Settings");
                }
            }
        } else {
            this.isXposedContext = false;
            this.settingsFileInternal = null;
            throw new IOException("Non-Xposed context is not supported in internal-only mode");
        }
    }

    public boolean saveSetting(String key, String value) {
        if (!isXposedContext || settingsFileInternal == null) {
            return false;
        }

        return saveToFile(settingsFileInternal, key, value, true);
    }

    private boolean saveToFile(File file, String key, String value, boolean allowRetry) {
        Properties properties = new Properties();

        // 既存の設定を読み込み
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                properties.load(fis);
            } catch (IOException ignored) {}
        }

        // 新しい設定を追加/更新
        properties.setProperty(key, value);

        // 設定を保存
        int maxAttempts = allowRetry ? 2 : 1;
        boolean success = false;

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            try (FileOutputStream fos = new FileOutputStream(file)) {
                properties.store(fos, "Updated: " + new Date());
                success = true;
                break;
            } catch (IOException e) {
                if (attempt == 0) {
                    handleSaveError(e, file);
                    prepareRetryEnvironment(file.getParentFile());
                }
            }
        }
        return success;
    }

    private void handleSaveError(IOException e, File file) {
        e.printStackTrace();
        if (file.exists() && !file.delete()) {
            System.out.println("Failed to delete corrupted file");
        }
    }

    private void prepareRetryEnvironment(File dir) {
        if (!dir.exists() && !dir.mkdirs()) {
            System.out.println("Failed to recreate directory");
        }
    }

    public String getSetting(String key, String defaultValue) {
        if (!isXposedContext || settingsFileInternal == null || !settingsFileInternal.exists()) {
            return defaultValue;
        }

        Properties properties = new Properties();
        try (FileInputStream fis = new FileInputStream(settingsFileInternal)) {
            properties.load(fis);
            return properties.getProperty(key, defaultValue);
        } catch (IOException e) {
            e.printStackTrace();
            return defaultValue;
        }
    }

    public boolean isInitialized() {
        return isXposedContext && settingsFileInternal != null && settingsFileInternal.exists();
    }
}