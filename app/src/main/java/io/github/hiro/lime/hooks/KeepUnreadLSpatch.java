package io.github.hiro.lime.hooks;


import android.content.Context;
import android.graphics.Color;
import android.os.Environment;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Switch;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.hiro.lime.LimeOptions;

public class KeepUnreadLSpatch implements IHook {

    static boolean keepUnread = true;

    @Override
    public void hook(LimeOptions limeOptions, XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        if (!limeOptions.KeepUnreadLSpatch.checked) return;
        XposedHelpers.findAndHookMethod(
                "com.linecorp.line.chatlist.view.fragment.ChatListFragment",
                loadPackageParam.classLoader,
                "onCreateView",
                LayoutInflater.class, ViewGroup.class, android.os.Bundle.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        View rootView = (View) param.getResult();
                        Context context = rootView.getContext();

                        RelativeLayout layout = new RelativeLayout(context);
                        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                        layout.setLayoutParams(layoutParams);

                        Switch switchView = new Switch(context);
                        switchView.setText("");
                        switchView.setTextColor(Color.WHITE);

                        RelativeLayout.LayoutParams switchParams = new RelativeLayout.LayoutParams(
                                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);

                        float horizontalMarginFactor = getkeep_unread_horizontalMarginFactor(context);
                        int verticalMarginDp = getkeep_unread_verticalMarginDp(context);
                        int verticalMarginPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, verticalMarginDp, context.getResources().getDisplayMetrics());
                        layout.post(() -> {
                            int horizontalMarginPx = (int) (layout.getWidth() * horizontalMarginFactor);
                            switchParams.setMargins(horizontalMarginPx, verticalMarginPx, 0, 0);
                            layout.addView(switchView, switchParams);
                        });

                        File backupDir = new File(context.getFilesDir(), "LimeBackup");
                        File logFile = new File(backupDir, "no_read.txt");

                        if (!backupDir.exists()) {
                            backupDir.mkdirs();
                        }

                        keepUnread = logFile.exists();
                        switchView.setChecked(keepUnread);

                        switchView.setOnCheckedChangeListener((buttonView, isChecked) -> {
                            keepUnread = isChecked;
                            if (isChecked) {
                                try {
                                    if (!logFile.exists()) {
                                        logFile.createNewFile();
                                    }
                                } catch (IOException ignored) {
                                    XposedBridge.log("Error creating file: ");
                                }
                            } else {
                                if (logFile.exists()) {
                                    logFile.delete();
                                }
                            }
                        });

                        if (rootView instanceof ViewGroup) {
                            ViewGroup rootViewGroup = (ViewGroup) rootView;
                            if (rootViewGroup.getChildCount() > 0 && rootViewGroup.getChildAt(0) instanceof ListView) {
                                ListView listView = (ListView) rootViewGroup.getChildAt(0);
                                listView.addFooterView(layout);
                            } else {
                                rootViewGroup.addView(layout);
                            }
                        }
                    }
                }
        );
        XposedHelpers.findAndHookMethod(
                loadPackageParam.classLoader.loadClass(Constants.MARK_AS_READ_HOOK.className),
                Constants.MARK_AS_READ_HOOK.methodName,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {

                        if (keepUnread) {
                            param.setResult(null); // 既読処理を無効化
                        }
                    }
                }
        );

    }

    private Map<String, String> readSettingsFromExternalFile(Context context) {
        String fileName = "margin_settings.txt";
        File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "LimeBackup");
        File file = new File(dir, fileName);
        Map<String, String> settings = new HashMap<>();
        if (!file.exists()) {
            dir = new File(Environment.getExternalStorageDirectory(), "Android/data/jp.naver.line.android/");
            file = new File(dir, fileName);
        }
        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split("=", 2);
                    if (parts.length == 2) {
                        settings.put(parts[0].trim(), parts[1].trim());
                    }
                }
            } catch (IOException e) {
                Log.e("FileError", "Error reading file: " + e.getMessage());
            }
        } else {
            Log.e("FileError", "File not found: " + file.getAbsolutePath());
        }

        return settings;
    }
    private float getkeep_unread_horizontalMarginFactor(Context context) {
        Map<String, String> settings = readSettingsFromExternalFile(context);
        try {
            return Float.parseFloat(settings.getOrDefault("keep_unread_horizontalMarginFactor", "0.5"));
        } catch (NumberFormatException e) {
            return 0.5f;
        }
    }

    private int getkeep_unread_verticalMarginDp(Context context) {
        Map<String, String> settings = readSettingsFromExternalFile(context);
        try {
            return Integer.parseInt(settings.getOrDefault("keep_unread_verticalMarginDp", "15"));
        } catch (NumberFormatException e) {
            return 15;
        }
    }
}





