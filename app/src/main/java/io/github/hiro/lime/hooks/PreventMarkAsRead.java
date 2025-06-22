package io.github.hiro.lime.hooks;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
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
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.hiro.lime.LimeOptions;

public class PreventMarkAsRead implements IHook {
    private boolean isSendChatCheckedEnabled = false; // デフォルト値

    @Override
    public void hook(LimeOptions limeOptions, XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {


        if (limeOptions.preventMarkAsRead.checked ) {
            Class<?> chatHistoryActivityClass = XposedHelpers.findClass("jp.naver.line.android.activity.chathistory.ChatHistoryActivity", loadPackageParam.classLoader);
            XposedHelpers.findAndHookMethod(chatHistoryActivityClass, "onCreate", Bundle.class, new XC_MethodHook() {
                Context moduleContext;

                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (moduleContext == null) {
                        try {
                            Context systemContext = (Context) XposedHelpers.callMethod(param.thisObject, "getApplicationContext");
                            moduleContext = systemContext.createPackageContext("io.github.hiro.lime", Context.CONTEXT_IGNORE_SECURITY);
                        } catch (Exception e) {
                            XposedBridge.log("Lime: Failed to get module context: " + e.getMessage());
                        }
                    }
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Context activityContext = (Context) param.thisObject;
                    Context context = activityContext.getApplicationContext();

                    if (moduleContext == null) {
                        XposedBridge.log("Lime: Module context is null. Skipping hook.");
                        return;
                    }

                    Activity activity = (Activity) param.thisObject;
                    addButton(activity, moduleContext, context);
                }

                private void addButton(Activity activity, Context moduleContext, Context context) {
                    Map<String, String> settings = readSettingsFromExternalFile(context);

                    float horizontalMarginFactor = Float.parseFloat(settings.getOrDefault("Read_buttom_Chat_horizontalMarginFactor", "0.5"));
                    int verticalMarginDp = Integer.parseInt(settings.getOrDefault("Read_buttom_Chat_verticalMarginDp", "15"));
                    float chatUnreadSizeDp = Float.parseFloat(settings.getOrDefault("chat_unread_size", "30"));

                    ImageView imageView = new ImageView(activity);
                    updateSwitchImage(imageView, isSendChatCheckedEnabled, moduleContext, context);

                    int sizeInPx = dpToPx(moduleContext, chatUnreadSizeDp);
                    FrameLayout.LayoutParams frameParams = new FrameLayout.LayoutParams(sizeInPx, sizeInPx);

                    int horizontalMarginPx = (int) (horizontalMarginFactor * activity.getResources().getDisplayMetrics().widthPixels);
                    int verticalMarginPx = (int) (verticalMarginDp * activity.getResources().getDisplayMetrics().density);
                    frameParams.setMargins(horizontalMarginPx, verticalMarginPx, 0, 0);
                    imageView.setLayoutParams(frameParams);

                    imageView.setOnClickListener(v -> {
                        isSendChatCheckedEnabled = !isSendChatCheckedEnabled;
                        updateSwitchImage(imageView, isSendChatCheckedEnabled, moduleContext, context);
                        send_chat_checked_state(moduleContext, isSendChatCheckedEnabled);
                    });

                    ViewGroup layout = activity.findViewById(android.R.id.content);
                    layout.addView(imageView);
                }

                private Map<String, String> readSettingsFromExternalFile(Context context) {
                    String fileName = "margin_settings.txt";
                    File dir = new File(context.getFilesDir(), "LimeBackup/Setting");
                    File file = new File(dir, fileName);
                    Map<String, String> settings = new HashMap<>();

                    // デフォルト値を設定
                    settings.put("Read_buttom_Chat_horizontalMarginFactor", "0.5");
                    settings.put("Read_buttom_Chat_verticalMarginDp", "15");
                    settings.put("chat_unread_size", "30");

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
                    }
                    return settings;
                }

                private void updateSwitchImage(ImageView imageView, boolean isOn, Context moduleContext, Context context) {
                    String imageName = isOn ? "read_switch_on.png" : "read_switch_off.png";

                    // 1. URIから画像を読み込む
                    Drawable drawable = loadImageFromUri(context, imageName);

                    // 2. 画像が存在しない場合はコピーを試みる
                    if (drawable == null) {
                        copyImageToUri(context, moduleContext, imageName);
                        // 再度読み込みを試みる
                        drawable = loadImageFromUri(context, imageName);
                    }

                    // 3. それでも失敗した場合はモジュールリソースを使用
                    if (drawable == null) {
                        int resId = moduleContext.getResources().getIdentifier(
                                imageName.replace(".png", ""), "drawable", "io.github.hiro.lime");
                        if (resId != 0) {
                            drawable = moduleContext.getResources().getDrawable(resId);
                        }
                    }

                    if (drawable != null) {
                        Map<String, String> settings = readSettingsFromExternalFile(context);
                        float sizeInDp = Float.parseFloat(settings.getOrDefault("chat_unread_size", "30"));
                        int sizeInPx = dpToPx(moduleContext, sizeInDp);
                        drawable = scaleDrawable(drawable, sizeInPx, sizeInPx);
                        imageView.setImageDrawable(drawable);
                    }
                }

                private void copyImageToUri(Context context, Context moduleContext, String imageName) {
                    String backupUri = loadBackupUri(context);
                    if (backupUri == null) return;

                    try {
                        Uri treeUri = Uri.parse(backupUri);
                        DocumentFile dir = DocumentFile.fromTreeUri(context, treeUri);
                        if (dir == null) return;

                        // 既にファイルが存在するか確認
                        if (dir.findFile(imageName) != null) return;

                        // モジュールリソースから読み込み
                        int resId = moduleContext.getResources().getIdentifier(
                                imageName.replace(".png", ""), "drawable", "io.github.hiro.lime");
                        if (resId == 0) return;

                        try (InputStream in = moduleContext.getResources().openRawResource(resId);
                             OutputStream out = context.getContentResolver().openOutputStream(
                                     dir.createFile("image/png", imageName).getUri())) {

                            byte[] buffer = new byte[1024];
                            int length;
                            while ((length = in.read(buffer)) > 0) {
                                out.write(buffer, 0, length);
                            }
                        }
                    } catch (Exception e) {
                        XposedBridge.log("Lime: Error copying image to URI: " + e.getMessage());
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

                private void send_chat_checked_state(Context context, boolean state) {
                    String filename = "send_chat_checked_state.txt";
                    try (FileOutputStream fos = context.openFileOutput(filename, Context.MODE_PRIVATE)) {
                        fos.write((state ? "1" : "0").getBytes());
                    } catch (IOException e) {
                        XposedBridge.log("Lime: Error saving chat checked state: " + e.getMessage());
                    }
                }
            });
            XposedHelpers.findAndHookMethod(
                    loadPackageParam.classLoader.loadClass(Constants.MARK_AS_READ_HOOK.className),
                    Constants.MARK_AS_READ_HOOK.methodName,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {

                                param.setResult(null);
                            }

                    }
            );
                XposedBridge.hookAllMethods(
                        loadPackageParam.classLoader.loadClass(Constants.REQUEST_HOOK.className),
                        Constants.REQUEST_HOOK.methodName,
                        new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                if (param.args[0].toString().equals("sendChatChecked")) {
                                    if (!isSendChatCheckedEnabled) { // isSendChatCheckedEnabledがfalseの場合のみnullを設定
                                        param.args[0] = param.args[0].getClass().getMethod("valueOf", String.class).invoke(null, "DUMMY");
                                    }
                                }
                            }
                        }
                );

                XposedBridge.hookAllMethods(
                        loadPackageParam.classLoader.loadClass(Constants.RESPONSE_HOOK.className),
                        Constants.RESPONSE_HOOK.methodName,
                        new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                if (param.args[0] != null && param.args[0].toString().equals("sendChatChecked")) {
                                    if (!isSendChatCheckedEnabled) {
                                        param.args[0] = param.args[0].getClass().getMethod("valueOf", String.class).invoke(null, "DUMMY");
                                    }
                                }
                            }
                        }
                );
            }
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
    }
