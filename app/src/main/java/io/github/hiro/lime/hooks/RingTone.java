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
                                        // MediaPlayerが初期化されているか確認
                                        if (mediaPlayer != null) {
                                            // MediaPlayerが再生中か確認
                                            if (mediaPlayer.isPlaying()) {
                                                return; // 再生中の場合は何もしない
                                            } else {
                                                mediaPlayer.release(); // 再生中でない場合は解放
                                                mediaPlayer = null; // MediaPlayerのインスタンスをnullに設定
                                            }
                                        }

                                        Uri ringtoneUri = Uri.fromFile(destFile); // コピーしたファイルのURIを取得
                                        mediaPlayer = MediaPlayer.create(context, ringtoneUri);
                                        mediaPlayer.setLooping(true); // 繰り返し再生を設定

                                        if (mediaPlayer != null) {
                                            mediaPlayer.start();
                                            isPlaying = true;

                                            mediaPlayer.setOnCompletionListener(mp -> {
                                                mp.seekTo(0);
                                                mp.start();
                                            });
                                        }
                                    }
                                }

                                if (paramValue.contains("RESULT=REJECTED,")) {
                                    if (mediaPlayer != null) {
                                        try {
                                            if (mediaPlayer.isPlaying()) {
                                                mediaPlayer.stop();
                                            }
                                        } catch (IllegalStateException ignored) {
                                        }
                                        try {
                                            mediaPlayer.release();
                                        } catch (IllegalStateException ignored) {

                                        } finally {
                                            mediaPlayer = null;
                                            isPlaying = false;
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

                            if (method.getName().equals("setServerConfig")) {
                                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                                    mediaPlayer.stop();
                                    mediaPlayer.release(); // MediaPlayerを解放
                                    mediaPlayer = null; // MediaPlayerのインスタンスをnullに設定
                                }
                            }

                            if (method.getName().equals("stop")) {
                                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                                    mediaPlayer.stop();
                                    mediaPlayer.release(); // MediaPlayerを解放
                                    mediaPlayer = null; // MediaPlayerのインスタンスをnullに設定
                                }
                            }

                            if (method.getName().equals("processToneEvent")) {
                                Object arg0 = param.args[0];
                                if (limeOptions.DialTone.checked) {
                                    Log.d("Xposed", "MuteTone is enabled. Suppressing tone event.");
                                    param.setResult(null);
                                    return;
                                }

                                if (arg0.toString().contains("START")) {
                                    if (appContext != null) {
                                        // MediaPlayerが初期化されており、再生中の場合はスキップ
                                        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                                            Log.d("Xposed", "MediaPlayer is already playing. Skipping playback.");
                                            return; // 再生中の場合は何もしない
                                        }
                                        Context moduleContext = AndroidAppHelper.currentApplication().createPackageContext(
                                                "io.github.hiro.lime", Context.CONTEXT_IGNORE_SECURITY);

                                        String resourceNameA = "dial_tone";
                                        int resourceId = moduleContext.getResources().getIdentifier(resourceNameA, "raw", "io.github.hiro.lime");

                                        File ringtoneDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "LimeBackup");
                                        if (!ringtoneDir.exists()) {
                                            ringtoneDir.mkdirs(); // ディレクトリが存在しない場合は作成
                                        }
                                        File destFile = new File(ringtoneDir, resourceNameA + ".wav");

                                        // リソースをストリームとして読み込み、ファイルに書き込む
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

                                        Uri ringtoneUri = Uri.fromFile(destFile); // コピーしたファイルのURIを取得
                                        mediaPlayer = MediaPlayer.create(appContext, ringtoneUri);
                                        mediaPlayer.setLooping(true); // 繰り返し再生を設定

                                        if (mediaPlayer != null) {
                                            mediaPlayer.start();
                                        } else {
                                            return;
                                        }
                                    } else {
                                        return;
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
                                    if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                                        mediaPlayer.stop();
                                        mediaPlayer.release(); // MediaPlayerを解放
                                        mediaPlayer = null; // MediaPlayerのインスタンスをnullに設定
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