package io.github.hiro.lime.hooks;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;

import io.github.hiro.lime.R;

public class CustomPreferences {
    private static final String SETTINGS_DIR = "LimeBackup/Setting";
    private static final String SETTINGS_FILE = "settings.properties";

    private final File settingsFileInternal; // Only used in Xposed context
    private final File settingsFileExternal;
    private final boolean isXposedContext;

    public CustomPreferences(Context context) throws PackageManager.NameNotFoundException, IOException {
        if (context != null) {

            this.isXposedContext = true;

            File internalDir = new File(context.getFilesDir(), SETTINGS_DIR);
            if (!internalDir.exists() && !internalDir.mkdirs()) {

                Toast.makeText(
                        context,
                        context.getString(R.string.Error_Create_setting_Button)
                                + "\nError: " + context.getString(R.string.save_failed),
                        Toast.LENGTH_LONG
                ).show();
                throw new IOException("Failed to create internal directory: "
                        + internalDir.getAbsolutePath()
                        + " | Permission: "
                        + (internalDir.getParentFile().canWrite() ? "granted" : "denied"));
            }
            settingsFileInternal = new File(internalDir, SETTINGS_FILE);

            File externalBaseDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
            File externalDir = new File(externalBaseDir, SETTINGS_DIR);
            if (!externalDir.exists() && !externalDir.mkdirs()) {


                throw new IOException("Failed to create external directory: "
                        + externalDir.getAbsolutePath()
                        + " | Storage state: "
                        + Environment.getExternalStorageState());
            }
            settingsFileExternal = new File(externalDir, SETTINGS_FILE);


            if (!settingsFileInternal.exists() && settingsFileExternal.exists()) {
                copyFile(settingsFileExternal, settingsFileInternal);
            } else if (!settingsFileExternal.exists() && settingsFileInternal.exists()) {
                copyFile(settingsFileInternal, settingsFileExternal);
            }

        } else {

            this.isXposedContext = false;


            File externalBaseDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
            File externalDir = new File(externalBaseDir, SETTINGS_DIR);
            settingsFileExternal = new File(externalDir, SETTINGS_FILE);


            if (!settingsFileExternal.exists()) {
                throw new FileNotFoundException("External settings file not found at: "
                        + settingsFileExternal.getAbsolutePath()
                        + "\nPlease ensure the file exists or run the hooked app first");
            }

            settingsFileInternal = null;

        }

        syncFiles();
    }

    private void syncFiles() {
        if (!isXposedContext || settingsFileInternal == null) return;

        boolean internalExists = settingsFileInternal.exists();
        boolean externalExists = settingsFileExternal.exists();

        try {
            if (externalExists) {
                if (internalExists) {
                    if (filesDiffer(settingsFileInternal, settingsFileExternal)) {
                        copyFile(settingsFileExternal, settingsFileInternal);
                    }
                } else {
                    copyFile(settingsFileExternal, settingsFileInternal);
                }
            } else if (internalExists) {
                copyFile(settingsFileInternal, settingsFileExternal);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean filesDiffer(File file1, File file2) throws IOException {
        if (file1.length() != file2.length()) return true;

        try (FileInputStream fis1 = new FileInputStream(file1);
             FileInputStream fis2 = new FileInputStream(file2)) {
            int byte1, byte2;
            do {
                byte1 = fis1.read();
                byte2 = fis2.read();
                if (byte1 != byte2) return true;
            } while (byte1 != -1);
        }
        return false;
    }

    private void copyFile(File source, File dest) throws IOException {
        try (FileInputStream in = new FileInputStream(source);
             FileOutputStream out = new FileOutputStream(dest)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
        }
    }

    public boolean saveSetting(String key, String value) {
        if (isXposedContext && settingsFileInternal != null) {
            boolean successInternal = saveToFile(settingsFileInternal, key, value, false);
            boolean successExternal = saveToFile(settingsFileExternal, key, value, true);
            return successInternal && successExternal;
        } else {
            return saveToFile(settingsFileExternal, key, value, false);
        }
    }

    private boolean saveToFile(File file, String key, String value, boolean allowRetry) {
        Properties properties = new Properties();
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                properties.load(fis);
            } catch (IOException ignored) {}
        }

        properties.setProperty(key, value);

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
        File targetFile = isXposedContext && settingsFileInternal != null ? settingsFileInternal : settingsFileExternal;
        if (!targetFile.exists()) {
            return defaultValue;
        }

        Properties properties = new Properties();
        try (FileInputStream fis = new FileInputStream(targetFile)) {
            properties.load(fis);
            return properties.getProperty(key, defaultValue);
        } catch (IOException e) {
            e.printStackTrace();
            return defaultValue;
        }
    }
}