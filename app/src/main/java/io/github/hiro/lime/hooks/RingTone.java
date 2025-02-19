package io.github.hiro.lime.hooks;

import android.app.AndroidAppHelper;
import android.app.Application;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
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
    private Ringtone ringtone = null;
    private boolean isPlaying = false;
    MediaPlayer mediaPlayer = null;
    @Override
    public void hook(LimeOptions limeOptions, XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        if (!limeOptions.callTone.checked) return;

        XposedHelpers.findAndHookMethod(Application.class, "onCreate", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Application appContext = (Application) param.thisObject;
                if (appContext == null) return;

                Context moduleContext = AndroidAppHelper.currentApplication().createPackageContext(
                        "io.github.hiro.lime", Context.CONTEXT_IGNORE_SECURITY);

                // dial_toneの準備
                String resourceNameA = "dial_tone";
                int resourceIdA = moduleContext.getResources().getIdentifier(resourceNameA, "raw", "io.github.hiro.lime");
                File ringtoneDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "LimeBackup");
                if (!ringtoneDir.exists()) ringtoneDir.mkdirs();
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
                                        if (isPlaying) {
                                           XposedBridge.log("Xposed"+ "Already playing");
                                            return;
                                        }
                                        Uri ringtoneUri = Uri.fromFile(destFile);
                                        // Android P (API 28) 以上の場合
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                            ringtone = RingtoneManager.getRingtone(context, ringtoneUri);
                                            if (ringtone != null) {
                                                ringtone.setLooping(true);
                                                ringtone.play();
                                                isPlaying = true;
                                                XposedBridge.log("Ringtone started playing.");
                                            }
                                        } else {
                                            // Android P 未満の場合は MediaPlayer を使用
                                            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                                               XposedBridge.log("Xposed"+ "MediaPlayer is already playing. Not starting new playback.");
                                                return;
                                            }
                                            mediaPlayer = MediaPlayer.create(context, ringtoneUri);
                                            if (mediaPlayer != null) {
                                                mediaPlayer.setLooping(true);
                                                mediaPlayer.start();
                                                isPlaying = true;
                                                XposedBridge.log("MediaPlayer started playing.");
                                            }
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

                            if (limeOptions.ringtonevolume.checked) {

                                if (methodName.equals("getVoiceComplexityLevel")) {
                                    if (isPlaying) return;

                                    File destFile = new File(ringtoneDir, "ringtone.wav");
                                    Uri ringtoneUri = Uri.fromFile(destFile);

                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                        ringtone = RingtoneManager.getRingtone(context, ringtoneUri);
                                        if (ringtone != null) {
                                            ringtone.setLooping(true);
                                            ringtone.play();
                                            isPlaying = true;
                                            XposedBridge.log("Ringtone started playing from getVoiceComplexityLevel.");
                                            return;
                                        }
                                    }
                                    if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                                        return;
                                    }

                                    mediaPlayer = MediaPlayer.create(context, ringtoneUri);
                                    if (mediaPlayer != null) {
                                        mediaPlayer.setLooping(true);
                                        mediaPlayer.start();
                                        isPlaying = true;
                                        XposedBridge.log("MediaPlayer started playing from getVoiceComplexityLevel.");
                                    }
                                }

                                if (method.getName().equals("setServerConfig") || method.getName().equals("stop")) {
                                    if (ringtone != null && isPlaying) {
                                        ringtone.stop();
                                        ringtone = null;
                                        isPlaying = false;
                                        XposedBridge.log("Ringtone stopped.");
                                    }
                                    if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                                        mediaPlayer.stop();
                                        mediaPlayer.release();
                                        mediaPlayer = null;
                                        XposedBridge.log("MediaPlayer stopped.");
                                    }
                                    if (method.getName().equals("processToneEvent")) {
                                        if (limeOptions.DialTone.checked) {
                                            XposedBridge.log("Xposed" + "Suppressing tone event");
                                            param.setResult(null);
                                            return;
                                        }
                                        if (limeOptions.MuteTone.checked && method.getName().equals("setTonePlayer")) {
                                            param.setResult(null);
                                        }

                                        if (isPlaying) {
                                            XposedBridge.log("Xposed" + "Suppressing playback");
                                            return;
                                        }
                                    }
                                    if (param.args != null && param.args.length > 0) {
                                        Object arg0 = param.args[0];
                                        if (arg0.toString().contains("START")) {
                                            if (appContext != null) {
                                                // Android P (API 28) 以上の場合
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                                    if (ringtone != null && isPlaying) {
                                                        ringtone.stop();
                                                        ringtone = null;
                                                        XposedBridge.log("Ringtone stopped before starting new one.");
                                                    }
                                                    Uri ringtoneUriA = Uri.fromFile(destFileA);
                                                    ringtone = RingtoneManager.getRingtone(appContext, ringtoneUriA);

                                                    if (ringtone != null) {
                                                        ringtone.setLooping(true);
                                                        ringtone.play();
                                                        isPlaying = true;
                                                        XposedBridge.log("Ringtone started playing from processToneEvent.");
                                                    }
                                                } else {
                                                    // Android P 未満の場合は MediaPlayer を使用
                                                    if (mediaPlayer != null) {
                                                        if (mediaPlayer.isPlaying()) {
                                                            XposedBridge.log("Xposed"+ "MediaPlayer is already playing. Stopping playback.");
                                                            mediaPlayer.stop();
                                                            XposedBridge.log("MediaPlayer stopped before starting new one.");
                                                        }
                                                        mediaPlayer.release();
                                                        mediaPlayer = null;
                                                    }

                                                    Uri ringtoneUriA = Uri.fromFile(destFileA);
                                                    mediaPlayer = MediaPlayer.create(appContext, ringtoneUriA);
                                                    if (mediaPlayer != null) {
                                                        mediaPlayer.setLooping(true);
                                                        XposedBridge.log("Xposed"+ "Playing media.");
                                                        mediaPlayer.start();
                                                        XposedBridge.log("MediaPlayer started playing from processToneEvent.");
                                                    }
                                                }
                                            }

                                        }
                                    }
                                }

                                if (method.getName().equals("activate")) {
                                    if (appContext != null) {
                                        // Android P (API 28) 以上の場合
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                            if (ringtone != null && isPlaying) {
                                                ringtone.stop();
                                                ringtone = null;
                                                XposedBridge.log("Ringtone stopped before starting new one.");
                                            }
                                            Uri ringtoneUriA = Uri.fromFile(destFileA);
                                            ringtone = RingtoneManager.getRingtone(appContext, ringtoneUriA);

                                            if (ringtone != null) {
                                                ringtone.setLooping(true);
                                                ringtone.play();
                                                isPlaying = true;
                                                XposedBridge.log("Ringtone started playing from processToneEvent.");
                                            }
                                        } else {
                                            // Android P 未満の場合は MediaPlayer を使用
                                            if (mediaPlayer != null) {
                                                if (mediaPlayer.isPlaying()) {
                                                    XposedBridge.log("Xposed"+ "MediaPlayer is already playing. Stopping playback.");
                                                    mediaPlayer.stop();
                                                    XposedBridge.log("MediaPlayer stopped before starting new one.");
                                                }
                                                mediaPlayer.release();
                                                mediaPlayer = null;
                                            }
                                            Uri ringtoneUriA = Uri.fromFile(destFileA);
                                            mediaPlayer = MediaPlayer.create(appContext, ringtoneUriA);
                                            if (mediaPlayer != null) {
                                                mediaPlayer.setLooping(true);
                                                XposedBridge.log("Xposed"+ "Playing media.");
                                                mediaPlayer.start();
                                                XposedBridge.log("MediaPlayer started playing from processToneEvent.");
                                            }
                                        }
                                    }
                                }
                                if (param.args != null && param.args.length > 0) {
                                    Object arg0 = param.args[0];
                                    if (method.getName().equals("ACTIVATED") && "ACTIVATED".equals(arg0)) {
                                        if (ringtone != null && isPlaying) {
                                            ringtone.stop();
                                            ringtone = null;
                                            isPlaying = false;
                                        }
                                        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                                            mediaPlayer.stop();
                                            mediaPlayer.release();
                                            mediaPlayer = null;

                                        }
                                    }
                                }

                        } else {
                                if (methodName.equals("getVoiceComplexityLevel")) {
                                    if (isPlaying) return;

                                    File destFile = new File(ringtoneDir, "ringtone.wav");
                                    Uri ringtoneUri = Uri.fromFile(destFile);


                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                        ringtone = RingtoneManager.getRingtone(context, ringtoneUri);
                                        if (ringtone != null) {
                                            ringtone.setLooping(true);
                                            ringtone.play();
                                            isPlaying = true;
                                            return;
                                        }
                                    }
                                    if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                                        return;
                                    }

                                    mediaPlayer = MediaPlayer.create(context, ringtoneUri);
                                    if (mediaPlayer != null) {
                                        mediaPlayer.setLooping(true);
                                        mediaPlayer.start();
                                        isPlaying = true;
                                    }
                                }

                                if (method.getName().equals("setServerConfig") || method.getName().equals("stop")) {
                                    if (ringtone != null && isPlaying) {
                                        ringtone.stop();
                                        ringtone = null;
                                        isPlaying = false;
                                    }
                                    if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                                        mediaPlayer.stop();
                                        mediaPlayer.release();
                                        mediaPlayer = null;
                                    }
                                    if (method.getName().equals("processToneEvent")) {
                                        if (limeOptions.DialTone.checked) {
                                            param.setResult(null);
                                            return;
                                        }
                                        if (limeOptions.MuteTone.checked && method.getName().equals("setTonePlayer")) {
                                            param.setResult(null);
                                        }

                                        if (isPlaying) {
                                            return;
                                        }
                                    }
                                    if (param.args != null && param.args.length > 0) {
                                        Object arg0 = param.args[0];
                                        if (arg0.toString().contains("START")) {
                                            if (appContext != null) {

                                                if (ringtone != null && isPlaying) {
                                                    ringtone.stop();
                                                    ringtone = null;
                                                }
                                                if (mediaPlayer != null) {
                                                    if (mediaPlayer.isPlaying()) {
                                                        mediaPlayer.stop();
                                                    }
                                                    mediaPlayer.release();
                                                    mediaPlayer = null;
                                                }

                                                Uri ringtoneUriA = Uri.fromFile(destFileA);
                                                AudioManager am = (AudioManager) appContext.getSystemService(Context.AUDIO_SERVICE);

                                                am.setStreamVolume(
                                                        AudioManager.STREAM_ALARM,
                                                        am.getStreamMaxVolume(AudioManager.STREAM_ALARM),
                                                        AudioManager.FLAG_SHOW_UI
                                                );
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                                    ringtone = RingtoneManager.getRingtone(appContext, ringtoneUriA);
                                                    AudioAttributes attributes = new AudioAttributes.Builder()
                                                            .setUsage(AudioAttributes.USAGE_ALARM)
                                                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                                            .build();
                                                    ringtone.setAudioAttributes(attributes);
                                                    ringtone.setLooping(true);
                                                    ringtone.play();
                                                } else {
                                                    mediaPlayer = new MediaPlayer();
                                                    try {
                                                        mediaPlayer.setDataSource(appContext, ringtoneUriA);
                                                        // ストリームタイプをアラームに設定
                                                        mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
                                                        mediaPlayer.setLooping(true);
                                                        mediaPlayer.prepare();
                                                        mediaPlayer.start();
                                                    } catch (IOException e) {
                                                        if (mediaPlayer != null) {
                                                            mediaPlayer.release();
                                                            mediaPlayer = null;
                                                        }
                                                    }
                                                }
                                                isPlaying = true;

                                            }
                                        }
                                    }
                                }

                                if (method.getName().equals("activate")) {
                                    if (appContext != null) {
                                        // 既存の再生を停止
                                        if (ringtone != null && isPlaying) {
                                            ringtone.stop();
                                            ringtone = null;
                                            XposedBridge.log("Ringtone stopped before starting new one.");
                                        }
                                        if (mediaPlayer != null) {
                                            if (mediaPlayer.isPlaying()) {
                                                mediaPlayer.stop();
                                            }
                                            mediaPlayer.release();
                                            mediaPlayer = null;
                                        }

                                        Uri ringtoneUriA = Uri.fromFile(destFileA);
                                        AudioManager am = (AudioManager) appContext.getSystemService(Context.AUDIO_SERVICE);

                                        am.setStreamVolume(
                                                AudioManager.STREAM_ALARM,
                                                am.getStreamMaxVolume(AudioManager.STREAM_ALARM),
                                                AudioManager.FLAG_SHOW_UI
                                        );

                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                            ringtone = RingtoneManager.getRingtone(appContext, ringtoneUriA);

                                            AudioAttributes attributes = new AudioAttributes.Builder()
                                                    .setUsage(AudioAttributes.USAGE_ALARM)
                                                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                                    .build();
                                            ringtone.setAudioAttributes(attributes);
                                            ringtone.setLooping(true);
                                            ringtone.play();
                                        } else {
                                            mediaPlayer = new MediaPlayer();
                                            try {
                                                mediaPlayer.setDataSource(appContext, ringtoneUriA);
                                                mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
                                                mediaPlayer.setLooping(true);
                                                mediaPlayer.prepare();
                                                mediaPlayer.start();
                                            } catch (IOException e) {
                                                if (mediaPlayer != null) {
                                                    mediaPlayer.release();
                                                    mediaPlayer = null;
                                                }
                                            }
                                        }
                                        isPlaying = true;
                                    }
                                }
                                if (param.args != null && param.args.length > 0) {
                                    Object arg0 = param.args[0];
                                    if (method.getName().equals("ACTIVATED") && "ACTIVATED".equals(arg0)) {
                                        if (ringtone != null && isPlaying) {
                                            ringtone.stop();
                                            ringtone = null;
                                            isPlaying = false;
                                        }
                                        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                                            mediaPlayer.stop();
                                            mediaPlayer.release();
                                            mediaPlayer = null;
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