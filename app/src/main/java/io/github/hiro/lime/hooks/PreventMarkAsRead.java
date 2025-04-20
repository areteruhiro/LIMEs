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
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
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
                            //XposedBridge.log("Failed to get module context: " + e.getMessage());
                        }
                    }
                }


                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (moduleContext == null) {
                        //XposedBridge.log("Module context is null. Skipping hook.");
                        return;
                    }
                    Activity activity = (Activity) param.thisObject;
                    addButton(activity, moduleContext);
                }
                private void addButton(Activity activity, Context moduleContext) {
                    Map<String, String> settings = readSettingsFromExternalFile(moduleContext);

                    float horizontalMarginFactor = 0.5f;
                    int verticalMarginDp = 15;

                    if (settings.containsKey("Read_buttom_Chat_horizontalMarginFactor")) {
                        horizontalMarginFactor = Float.parseFloat(settings.get("Read_buttom_Chat_horizontalMarginFactor"));
                    }
                    if (settings.containsKey("Read_buttom_Chat_verticalMarginDp")) {
                        verticalMarginDp = Integer.parseInt(settings.get("Read_buttom_Chat_verticalMarginDp"));
                    }

                    ImageView imageView = new ImageView(activity);
                    updateSwitchImage(imageView, isSendChatCheckedEnabled, moduleContext);

                    // chat_unread_size の値を取得
                    float chatUnreadSizeDp = 30; // デフォルト値
                    if (settings.containsKey("chat_unread_size")) {
                        chatUnreadSizeDp = Float.parseFloat(settings.get("chat_unread_size"));
                    }

                    // DP値をピクセル値に変換
                    int sizeInPx = dpToPx(moduleContext, chatUnreadSizeDp);

                    // FrameLayout.LayoutParams の幅と高さを動的に設定
                    FrameLayout.LayoutParams frameParams = new FrameLayout.LayoutParams(sizeInPx, sizeInPx);

                    int horizontalMarginPx = (int) (horizontalMarginFactor * activity.getResources().getDisplayMetrics().widthPixels);
                    int verticalMarginPx = (int) (verticalMarginDp * activity.getResources().getDisplayMetrics().density);
                    frameParams.setMargins(horizontalMarginPx, verticalMarginPx, 0, 0);
                    imageView.setLayoutParams(frameParams);

                    imageView.setOnClickListener(v -> {
                        isSendChatCheckedEnabled = !isSendChatCheckedEnabled;
                        updateSwitchImage(imageView, isSendChatCheckedEnabled, moduleContext);
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

                    // ファイルが存在する場合、内容を読み込む
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
                            e.printStackTrace();
                        }
                    }
                    return settings;
                }

                private void updateSwitchImage(ImageView imageView, boolean isOn, Context moduleContext) {
                    // ファイルパスを取得
                    File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "LimeBackup");
                    File file = new File(dir, "margin_settings.txt");

                    // デフォルト値
                    float chatUnreadSizeDp = 30; // デフォルト値

                    // ファイルの内容を読み込む
                    if (!file.exists()) {
                        // 次のディレクトリを確認
                        dir = new File(Environment.getExternalStorageDirectory(), "Android/data/jp.naver.line.android/");
                        file = new File(dir, "margin_settings.txt");

                        // それでも存在しない場合、内部ストレージを確認
                        if (!file.exists()) {
                            file = new File(moduleContext.getFilesDir(), "margin_settings.txt");
                        }
                    }

                    if (file.exists()) {
                        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                String[] parts = line.split("=", 2);
                                if (parts.length == 2) {
                                    if (parts[0].trim().equals("chat_unread_size")) {
                                        chatUnreadSizeDp = Float.parseFloat(parts[1].trim());
                                    }
                                }
                            }
                        } catch (IOException | NumberFormatException ignored) {
                            // エラーが発生した場合はデフォルト値を使用
                        }
                    }

                    // 画像のファイル名を決定（ON/OFF状態に応じて）
                    String imageName = isOn ? "read_switch_on.png" : "read_switch_off.png";

                    // 画像ファイルのパスを指定
                    File imageFile = new File(dir, imageName);

                    // 画像ファイルが存在しない場合、リソースからコピーして保存
                    if (!imageFile.exists()) {
                        // 最初のディレクトリにコピーを試みる
                        if (!copyImageFile(moduleContext, imageName, imageFile)) {
                            // 次のディレクトリにコピーを試みる
                            File fallbackDir = new File(Environment.getExternalStorageDirectory(), "Android/data/jp.naver.line.android/");
                            if (!fallbackDir.exists()) {
                                fallbackDir.mkdirs();
                            }
                            imageFile = new File(fallbackDir, imageName);
                            if (!copyImageFile(moduleContext, imageName, imageFile)) {
                                // 内部ストレージにコピーを試みる
                                File internalDir = new File(moduleContext.getFilesDir(), "backup");
                                if (!internalDir.exists()) {
                                    internalDir.mkdirs();
                                }
                                imageFile = new File(internalDir, imageName);
                                copyImageFile(moduleContext, imageName, imageFile);
                            }
                        }
                    }

                    // 画像ファイルが存在する場合、ImageViewに設定
                    if (imageFile.exists()) {
                        Drawable drawable = Drawable.createFromPath(imageFile.getAbsolutePath());
                        if (drawable != null) {
                            // DP値をピクセル値に変換
                            int sizeInPx = dpToPx(moduleContext, chatUnreadSizeDp);
                            // 画像を指定したサイズにスケーリング
                            drawable = scaleDrawable(drawable, sizeInPx, sizeInPx);
                            // ImageViewにスケーリングされた画像を設定
                            imageView.setImageDrawable(drawable);
                        }
                    }
                }

                private boolean copyImageFile(Context moduleContext, String imageName, File destinationFile) {
                    try (InputStream in = moduleContext.getResources().openRawResource(
                            moduleContext.getResources().getIdentifier(imageName.replace(".png", ""), "drawable", "io.github.hiro.lime"));
                         OutputStream out = new FileOutputStream(destinationFile)) {
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = in.read(buffer)) > 0) {
                            out.write(buffer, 0, length);
                        }
                        return true; // コピー成功
                    } catch (IOException e) {
                        e.printStackTrace();
                        return false; // コピー失敗
                    }
                }
                // DP値をピクセル値に変換するメソッド
                private int dpToPx(@NonNull Context context, float dp) {
                    float density = context.getResources().getDisplayMetrics().density;
                    return Math.round(dp * density);
                }

                private Drawable scaleDrawable(Drawable drawable, int width, int height) {
                    Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
                    Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
                    return new BitmapDrawable(scaledBitmap);
                }

                private void send_chat_checked_state (Context context,boolean state){
                    String filename = "send_chat_checked_state.txt";
                    try (FileOutputStream fos = context.openFileOutput(filename, Context.MODE_PRIVATE)) {
                        fos.write((state ? "1" : "0").getBytes());
                    } catch (IOException ignored) {

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
    }
