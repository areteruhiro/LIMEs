package io.github.hiro.lime.hooks;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
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

                    int width = 150;
                    int height = 100;
                    FrameLayout.LayoutParams frameParams = new FrameLayout.LayoutParams(width, height);


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
                    File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "LimeBackup");
                    File file = new File(dir, fileName);
                    Map<String, String> settings = new HashMap<>();

                    if (!file.exists()) {
                    }

                    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            String[] parts = line.split("=", 2);
                            if (parts.length == 2) {
                                settings.put(parts[0].trim(), parts[1].trim());
                            }
                        }
                    } catch (IOException e) {

                    }
                    return settings;
                }

                private void updateSwitchImage(ImageView imageView, boolean isOn, Context moduleContext) {
                    String imageName = isOn ? "read_switch_on.png" : "read_switch_off.png"; // 拡張子を追加
                    File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "LimeBackup");

                    if (!dir.exists()) {
                        dir.mkdirs();
                    }

                    File imageFile = new File(dir, imageName);


                    if (!imageFile.exists()) {
                        try (InputStream in = moduleContext.getResources().openRawResource(
                                moduleContext.getResources().getIdentifier(imageName.replace(".png", ""), "drawable", "io.github.hiro.lime"));
                             OutputStream out = new FileOutputStream(imageFile)) {
                            byte[] buffer = new byte[1024];
                            int length;
                            while ((length = in.read(buffer)) > 0) {
                                out.write(buffer, 0, length);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    if (imageFile.exists()) {
                        Drawable drawable = Drawable.createFromPath(imageFile.getAbsolutePath());
                        if (drawable != null) {
                            Map<String, String> settings = readSettingsFromExternalFile(moduleContext);
                            float sizeInDp = Float.parseFloat(settings.getOrDefault("chat_unread_size", "60"));
                            int sizeInPx = dpToPx(moduleContext, sizeInDp);
                            drawable = scaleDrawable(drawable, sizeInPx, sizeInPx);
                            imageView.setImageDrawable(drawable);
                        }
                    }
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

                private void send_chat_checked_state (Context context,boolean state){
                    String filename = "send_chat_checked_state.txt";
                    try (FileOutputStream fos = context.openFileOutput(filename, Context.MODE_PRIVATE)) {
                        fos.write((state ? "1" : "0").getBytes());
                    } catch (IOException ignored) {

                    }
                }
                private boolean readStateFromFile(Context context) {
                    String filename = "send_chat_checked_state.txt";
                    try (FileInputStream fis = context.openFileInput(filename)) {
                        int c;
                        StringBuilder sb = new StringBuilder();
                        while ((c = fis.read()) != -1) {
                            sb.append((char) c);
                        }
                        return "1".equals(sb.toString());
                    } catch (IOException ignored) {
                        return true;
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
                                        param.setResult(null);
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
                                        param.setResult(null);
                                    }
                                }
                            }
                        }
                );
            }
        }
    }
