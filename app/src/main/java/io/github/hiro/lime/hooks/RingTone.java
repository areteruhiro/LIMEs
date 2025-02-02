package io.github.hiro.lime.hooks;

import android.app.AndroidAppHelper;
import android.app.Application;
import android.content.Context;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
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

                                if (paramValue.contains("type:NOTIFIED_RECEIVED_CALL,") && !isPlaying) {
                                    if (context != null) {
                                        if (mediaPlayer != null) {
                                            if (mediaPlayer.isPlaying()) {
                                                return;
                                            } else {
                                                mediaPlayer.release();
                                                mediaPlayer = null;
                                            }
                                        }

                                        Uri ringtoneUri = Uri.fromFile(destFile);
                                        mediaPlayer = MediaPlayer.create(context, ringtoneUri);
                                        mediaPlayer.setLooping(true);

                                        if (mediaPlayer != null) {
                                            mediaPlayer.start();
                                            isPlaying = true;

                                            mediaPlayer.setOnCompletionListener(mp -> {
                                                isPlaying = false;
                                                mp.seekTo(0);
                                                mp.start();
                                                isPlaying = true;
                                            });
                                        }
                                    }
                                }
                            }
                        }

                        );


                Class<?> targetClass = loadPackageParam.classLoader.loadClass("com.linecorp.andromeda.audio.AudioManager");
                Method[] methods = targetClass.getDeclaredMethods();

                for (Method method : methods) {
                    XposedBridge.hookMethod(method, new XC_MethodHook() {

                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {

                            if (method.getName().equals("setServerConfig")) {
                                if (mediaPlayer != null) {
                                    if (mediaPlayer.isPlaying()) {
                                        mediaPlayer.stop();
                                    }
                                    mediaPlayer.release();
                                    mediaPlayer = null;
                                }
                            }

                            if (method.getName().equals("stop")) {
                                if (mediaPlayer != null) {
                                    if (mediaPlayer.isPlaying()) {
                                        mediaPlayer.stop();
                                    }
                                    mediaPlayer.release();
                                    mediaPlayer = null;
                                }
                            }

                            if (method.getName().equals("processToneEvent")) {
                                Object arg0 = param.args[0];
                                if (limeOptions.DialTone.checked) {
                                    param.setResult(null);
                                    return;
                                }

                                if (arg0.toString().contains("START")) {
                                    if (appContext != null) {
                                        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
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
                                        File destFile = new File(ringtoneDir, resourceNameA + ".wav");

                                        if (!destFile.exists()) {
                                            try (InputStream in = moduleContext.getResources().openRawResource(resourceIdA);
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

                                        Uri ringtoneUri = Uri.fromFile(destFile);
                                        mediaPlayer = MediaPlayer.create(appContext, ringtoneUri);
                                        mediaPlayer.setLooping(true);

                                        if (mediaPlayer != null) {
                                            mediaPlayer.start();

                                            mediaPlayer.setOnCompletionListener(mp -> {
                                                mp.seekTo(0);
                                                mp.start();
                                            });
                                        }
                                    }
                                }
                            }

                            if (limeOptions.MuteTone.checked) {
                                if (method.getName().equals("setTonePlayer")) {
                                    param.setResult(null);
                                }
                            }

                            if (method.getName().equals("ACTIVATED") && param.args != null && param.args.length > 0) {
                                Object arg0 = param.args[0];
                                if ("ACTIVATED".equals(arg0)) {
                                    if (mediaPlayer != null) {
                                        if (mediaPlayer.isPlaying()) {
                                            mediaPlayer.stop();
                                        }
                                        mediaPlayer.release();
                                        mediaPlayer = null;
                                    }
                                }
                            }
                        }
                    });


                }
            }
        });
    }
}