package io.github.hiro.lime.hooks;

import android.app.AndroidAppHelper;
import android.app.Application;
import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.hiro.lime.LimeOptions;

public class RingTone implements IHook {
    private android.media.Ringtone ringtone = null;
    private boolean isPlaying = false;
    MediaPlayer mediaPlayer = null;
    @Override
    public void hook(LimeOptions limeOptions, XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        if (!limeOptions.callTone.checked) return;

        XposedHelpers.findAndHookMethod(Application.class, "onCreate", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Application appContext = (Application) param.thisObject;
                if (appContext == null) {
                    return;
                }


                Context moduleContext = AndroidAppHelper.currentApplication().createPackageContext(
                        "io.github.hiro.lime", Context.CONTEXT_IGNORE_SECURITY);

                String resourceNameA = "dial_tone";
                int resourceIdA = moduleContext.getResources().getIdentifier(resourceNameA, "raw", "io.github.hiro.lime");

                File ringtoneDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "LimeBackup");
                if (!ringtoneDir.exists()) {
                    ringtoneDir.mkdirs();
                }
                File destFileA = new File(ringtoneDir, resourceNameA + ".wav");

                if (!destFileA.exists()) {
                    try (InputStream in = moduleContext.getResources().openRawResource(resourceIdA);
                         OutputStream out = new FileOutputStream(destFileA)) {
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = in.read(buffer)) > 0) {
                            out.write(buffer, 0, length);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                XposedBridge.hookAllMethods(
                        loadPackageParam.classLoader.loadClass(Constants.RESPONSE_HOOK.className),
                        Constants.RESPONSE_HOOK.methodName,
                        new XC_MethodHook() {

                            @Override
                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                String paramValue = param.args[1].toString();
                                Context context = AndroidAppHelper.currentApplication().getApplicationContext();
                                Context moduleContext = AndroidAppHelper.currentApplication().createPackageContext(
                                        "io.github.hiro.lime", Context.CONTEXT_IGNORE_SECURITY);

                                String resourceName = "ringtone";
                                int resourceId = moduleContext.getResources().getIdentifier(resourceName, "raw", "io.github.hiro.lime");

                                File ringtoneDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "LimeBackup");
                                if (!ringtoneDir.exists()) {
                                    ringtoneDir.mkdirs();
                                }
                                File destFile = new File(ringtoneDir, resourceName + ".wav");

                                if (!destFile.exists()) {
                                    try (InputStream in = moduleContext.getResources().openRawResource(resourceId);
                                         OutputStream out = new FileOutputStream(destFile)) {
                                        byte[] buffer = new byte[1024];
                                        int length;
                                        while ((length = in.read(buffer)) > 0) {
                                            out.write(buffer, 0, length);
                                        }
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }

                                if (paramValue.contains("type:NOTIFIED_RECEIVED_CALL,")) {
                                    XposedBridge.log(paramValue);
                                    if (context != null) {
                                        // MediaPlayerが初期化されているか確認
                                        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                                            Log.d("Xposed", "MediaPlayer is already playing. Not starting new playback.");
                                            return; // すでに再生中の場合は新しい再生を開始しない
                                        }
                                        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                                            return;
                                        }
                                        Uri ringtoneUri = Uri.fromFile(destFile); 
                                        mediaPlayer = MediaPlayer.create(context, ringtoneUri);
                                        mediaPlayer.setLooping(true); 

                                        if (mediaPlayer != null) {
                                            mediaPlayer.start();
                                            isPlaying = true;
                                        }
                                    }
                                }
                            }
                        });

                Class<?> targetClass = loadPackageParam.classLoader.loadClass("com.linecorp.andromeda.audio.AudioManager");
                Method[] methods = targetClass.getDeclaredMethods();

                for (Method method : methods) {
                    XposedBridge.hookMethod(method, new XC_MethodHook() {

                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            String methodName = method.getName();
                            Context context = AndroidAppHelper.currentApplication().getApplicationContext();

                            if (methodName.equals("getVoiceComplexityLevel")) {
                                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                                    return;

                                }
                                XposedBridge.log("getVoiceComplexityLevel");

                                File destFile = new File(ringtoneDir, "ringtone" + ".wav");
                                Uri ringtoneUri = Uri.fromFile(destFile);
                                mediaPlayer = MediaPlayer.create(context, ringtoneUri);
                                mediaPlayer.setLooping(true);
                                mediaPlayer.start();
                                isPlaying = true;
                                return;
                            }
                            if (method.getName().equals("setServerConfig")) {
                                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                                    mediaPlayer.stop();
                                    mediaPlayer.release(); 
                                    mediaPlayer = null; 
                                }
                            }

                            if (method.getName().equals("stop")) {
                                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                                    mediaPlayer.stop();
                                    mediaPlayer.release(); 
                                    mediaPlayer = null; 
                                }
                            }

                            if( param.args != null && param.args.length > 0) {
                                Object arg0 = param.args[0];
                            if (method.getName().equals("ACTIVATED") )
                                if ("ACTIVATED".equals(arg0)) {
                                    if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                                        mediaPlayer.stop();
                                        mediaPlayer.release(); 
                                        mediaPlayer = null; 
                                    }
                                }
                            }
                            if (method.getName().equals("processToneEvent")) {

                                if (limeOptions.DialTone.checked) {
                                    Log.d("Xposed", "MuteTone is enabled. Suppressing tone event.");
                                    param.setResult(null);
                                    return;
                                }
                                if (limeOptions.MuteTone.checked) {
                                    if (method.getName().equals("setTonePlayer")) {
                                        param.setResult(null);
                                    }
                                }


                                // 音楽が再生中の場合、MediaPlayerの再生を抑制
                                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                                    Log.d("Xposed", "音楽が再生中のため、MediaPlayerの再生を抑制します。");
                                    return; // ここで何もしないことで再生を抑制
                                }



                                if( param.args != null && param.args.length > 0) {
                                    Object arg0 = param.args[0];
                                    if (arg0.toString().contains("START")) {
                                        if (appContext != null) {
                                            if (mediaPlayer != null) {
                                                if (mediaPlayer.isPlaying()) {
                                                    Log.d("Xposed", "MediaPlayer is already playing. Stopping playback.");
                                                    mediaPlayer.stop();
                                                }
                                                mediaPlayer.release(); 
                                                mediaPlayer = null; 
                                            }
                                        }

                                        Uri ringtoneUriA = Uri.fromFile(destFileA);
                                        mediaPlayer = MediaPlayer.create(appContext, ringtoneUriA);
                                        mediaPlayer.setLooping(true);

                                        if (mediaPlayer != null) {
                                            Log.d("Xposed", "Playing media.");
                                            mediaPlayer.start();
                                        }
                                    }
                                }
                            }


//
//                            // 引数の値を取得してログに出力
//                            StringBuilder argsLog = new StringBuilder("Method: " + methodName + ", Arguments: ");
//                            for (Object arg : param.args) {
//                                argsLog.append(arg).append(", ");
//                            }
//
//                            // 最後のカンマとスペースを削除
//                            if (argsLog.length() > 0) {
//                                argsLog.setLength(argsLog.length() - 2);
//                            }
//
//                            // XposedBridge.logを使用してログ出力
//                            XposedBridge.log(argsLog.toString());
                        }

                    });


                }
            }
        });
    }
}