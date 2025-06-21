
package io.github.hiro.lime.hooks;


import android.app.AndroidAppHelper;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;

import androidx.documentfile.provider.DocumentFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.hiro.lime.LimeOptions;


public class KeepUnread implements IHook {
    static boolean keepUnread = false;


    @Override
    public void hook(LimeOptions limeOptions, XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        if (limeOptions.removeKeepUnread.checked) return;


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
                        Context moduleContext = AndroidAppHelper.currentApplication().createPackageContext(
                                "io.github.hiro.lime", Context.CONTEXT_IGNORE_SECURITY);

                        RelativeLayout layout = new RelativeLayout(context);
                        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                        layout.setLayoutParams(layoutParams);

                        keepUnread = readStateFromFile(context);
                        ImageView imageView = new ImageView(context);
                        updateSwitchImage(imageView, keepUnread, moduleContext, context);

                        Resources resources = context.getResources();
                        Configuration configuration = resources.getConfiguration();
                        int smallestWidthDp = configuration.smallestScreenWidthDp;
                        float density = resources.getDisplayMetrics().density;

                        float keep_unread_horizontalMarginFactor = getkeep_unread_horizontalMarginFactor(context);
                        int keep_unread_verticalMarginDp = getkeep_unread_verticalMarginDp(context);

                        int horizontalMarginPx = (int) (smallestWidthDp * keep_unread_horizontalMarginFactor * density);
                        int verticalMarginPx = (int) (keep_unread_verticalMarginDp * density);

                        RelativeLayout.LayoutParams imageParams = new RelativeLayout.LayoutParams(
                                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                        imageParams.setMargins(horizontalMarginPx, verticalMarginPx, 0, 0);

                        imageView.setOnClickListener(v -> {
                            keepUnread = !keepUnread;
                            updateSwitchImage(imageView, keepUnread, moduleContext, context);
                            saveStateToFile(context, keepUnread);
                        });

                        layout.addView(imageView, imageParams);

                        if (rootView instanceof ViewGroup) {
                            ViewGroup rootViewGroup = (ViewGroup) rootView;
                            boolean added = false;
                            for (int i = 0; i < rootViewGroup.getChildCount(); i++) {
                                View child = rootViewGroup.getChildAt(i);
                                if (child instanceof ListView) {
                                    ListView listView = (ListView) child;
                                    listView.addFooterView(layout);
                                    added = true;
                                    break;
                                }
                            }

                            if (!added) {
                                rootViewGroup.addView(layout);
                            }
                        }
                    }

                    private Map<String, String> readSettingsFromExternalFile(Context context) {
                        String fileName = "margin_settings.txt";
                        File dir = new File(context.getFilesDir(), "LimeBackup/Setting");
                        File file = new File(dir, fileName);
                        Map<String, String> settings = new HashMap<>();

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
                                XposedBridge.log("Lime: Error reading margin settings: " + e.getMessage());
                            }
                        } else {
                            XposedBridge.log("Lime: Margin settings file not found at " + file.getAbsolutePath());
                            // デフォルト値を設定
                            settings.put("keep_unread_horizontalMarginFactor", "0.5");
                            settings.put("keep_unread_verticalMarginDp", "50");
                            settings.put("keep_unread_size", "80");
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
                            return Integer.parseInt(settings.getOrDefault("keep_unread_verticalMarginDp", "50"));
                        } catch (NumberFormatException e) {
                            return 50;
                        }
                    }

                    private void updateSwitchImage(ImageView imageView, boolean isOn, Context moduleContext, Context context) {
                        String imageName = isOn ? "keep_switch_on.png" : "keep_switch_off.png";

                        // URIから画像を読み込む
                        Drawable drawable = loadImageFromUri(context, imageName);

                        // URIから読み込めなかった場合はアプリ内リソースを使用
                        if (drawable == null) {
                            int resId = moduleContext.getResources().getIdentifier(
                                    imageName.replace(".png", ""), "drawable", "io.github.hiro.lime");
                            if (resId != 0) {
                                drawable = moduleContext.getResources().getDrawable(resId);
                            }
                        }

                        if (drawable != null) {
                            Map<String, String> settings = readSettingsFromExternalFile(context);
                            float sizeInDp = Float.parseFloat(settings.getOrDefault("keep_unread_size", "80"));
                            int sizeInPx = dpToPx(context, sizeInDp);
                            drawable = scaleDrawable(drawable, sizeInPx, sizeInPx);
                            imageView.setImageDrawable(drawable);
                        }
                    }

                    private Drawable loadImageFromUri(Context context, String imageName) {
                        String backupUri = loadBackupUri(context);
                        if (backupUri != null) {
                            try {
                                Uri treeUri = Uri.parse(backupUri);
                                DocumentFile dir = DocumentFile.fromTreeUri(context, treeUri);
                                if (dir != null) {
                                    DocumentFile imageFile = dir.findFile(imageName);
                                    if (imageFile != null && imageFile.exists()) {
                                        try (InputStream inputStream = context.getContentResolver().openInputStream(imageFile.getUri())) {
                                            return Drawable.createFromStream(inputStream, null);
                                        } catch (IOException e) {
                                            XposedBridge.log("Lime: Error loading image from URI: " + e.getMessage());
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                XposedBridge.log("Lime: Error accessing image URI: " + e.getMessage());
                            }
                        }
                        return null;
                    }

                    private int dpToPx(Context context, float dp) {
                        float density = context.getResources().getDisplayMetrics().density;
                        return Math.round(dp * density);
                    }

                    private Drawable scaleDrawable(Drawable drawable, int width, int height) {
                        Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
                        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
                        return new BitmapDrawable(scaledBitmap);
                    }

                    private boolean readStateFromFile(Context context) {
                        String filename = "keep_unread_state.txt";
                        try (FileInputStream fis = context.openFileInput(filename)) {
                            int c;
                            StringBuilder sb = new StringBuilder();
                            while ((c = fis.read()) != -1) {
                                sb.append((char) c);
                            }
                            return "1".equals(sb.toString());
                        } catch (IOException ignored) {
                            return false;
                        }
                    }

                    private void saveStateToFile(Context context, boolean state) {
                        String filename = "keep_unread_state.txt";
                        try (FileOutputStream fos = context.openFileOutput(filename, Context.MODE_PRIVATE)) {
                            fos.write((state ? "1" : "0").getBytes());
                        } catch (IOException ignored) {
                        }
                    }
                }
        );
    }

    private String loadBackupUri(Context context) {
        if (context == null) {
            XposedBridge.log("Lime: Context is null in loadBackupUri");
            return null;
        }

        File settingsFile = new File(context.getFilesDir(), "LimeBackup/backup_uri.txt");
        if (!settingsFile.exists()) return null;

        try (FileInputStream fis = new FileInputStream(settingsFile);
             InputStreamReader isr = new InputStreamReader(fis);
             BufferedReader br = new BufferedReader(isr)) {
            return br.readLine();
        } catch (IOException e) {
            XposedBridge.log("Lime URI Load Error: " + e.getMessage());
            return null;
        }
    }
    private void saveStateToFile(Context context, boolean state) {
        String filename = "keep_unread_state.txt";
        try (FileOutputStream fos = context.openFileOutput(filename, Context.MODE_PRIVATE)
        ) {
            fos.write((state ? "1" : "0").getBytes());
        } catch (IOException ignored) {
        }
    }
}

