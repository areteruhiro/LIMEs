package io.github.hiro.lime.hooks;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Environment;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;

public class CustomPreferences {
    private static final String SETTINGS_DIR = "LimeBackup/Setting";
    private static final String SETTINGS_FILE = "settings.properties";

    private final File settingsFileInternal; // Only used in Xposed context
    private final File settingsFileExternal;
    private final boolean isXposedContext;

    public CustomPreferences(Context context) throws PackageManager.NameNotFoundException, IOException {
        File settingsFileExternal1;
        if (context != null) {
            this.isXposedContext = true;

            File internalDir = new File(context.getFilesDir(), SETTINGS_DIR);
            if (!internalDir.exists() && !internalDir.mkdirs()) {
                throw new IOException("Failed to create internal directory: "
                        + internalDir.getAbsolutePath()
                        + " | Permission: "
                        + (internalDir.getParentFile().canWrite() ? "granted" : "denied"));
            }
            settingsFileInternal = new File(internalDir, SETTINGS_FILE);

            File externalBaseDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File externalDir = new File(externalBaseDir, SETTINGS_DIR);
            if (!externalDir.exists() && !externalDir.mkdirs()) {
                throw new IOException("Failed to create external directory: "
                        + externalDir.getAbsolutePath()
                        + " | Storage state: "
                        + Environment.getExternalStorageState());
            }
            settingsFileExternal1 = new File(externalDir, SETTINGS_FILE);

            if (!settingsFileInternal.exists() && settingsFileExternal1.exists()) {
                copyFile(settingsFileExternal1, settingsFileInternal);
            } else if (!settingsFileExternal1.exists() && settingsFileInternal.exists()) {
                copyFile(settingsFileInternal, settingsFileExternal1);
            }

        } else {
            this.isXposedContext = false;

            File externalBaseDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
            File externalDir = new File(externalBaseDir, SETTINGS_DIR);
            settingsFileExternal1 = new File(externalDir, SETTINGS_FILE);

            // ファイルが存在しない場合、作成しない
            if (!settingsFileExternal1.exists()) {
                settingsFileExternal1 = null; // ファイルを作成しないことを示す
            }

            settingsFileInternal = null;
        }

        settingsFileExternal = settingsFileExternal1;
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

                if (!canAccessExternalStorage()) {

                    if (settingsFileExternal.exists()) {
                        if (!settingsFileExternal.delete()) {

                            File renamedFile = new File(settingsFileExternal.getParent(), "old_" + settingsFileExternal.getName());
                            if (!settingsFileExternal.renameTo(renamedFile)) {
                                System.out.println("Failed to delete or rename external file");
                            }
                        }
                    }

                    copyFile(settingsFileInternal, settingsFileExternal);
                } else {
                    copyFile(settingsFileInternal, settingsFileExternal);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 外部ストレージにアクセスできるかどうかを確認するメソッド
     */
    private boolean canAccessExternalStorage() {
        String state = Environment.getExternalStorageState();
        if (!Environment.MEDIA_MOUNTED.equals(state)) {

            return false;
        }

        File externalDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (!externalDir.exists() && !externalDir.mkdirs()) {

            return false;
        }

        File testFile = new File(externalDir, "test.tmp");
        try {
            if (!testFile.createNewFile()) {
                return false;
            }
            testFile.delete();
            return true;
        } catch (IOException e) {
            return false;
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
            boolean successExternal = settingsFileExternal != null ? saveToFile(settingsFileExternal, key, value, true) : true;
            return successInternal && successExternal;
        } else {
            return settingsFileExternal != null ? saveToFile(settingsFileExternal, key, value, false) : false;
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
        if (targetFile == null || !targetFile.exists()) {
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