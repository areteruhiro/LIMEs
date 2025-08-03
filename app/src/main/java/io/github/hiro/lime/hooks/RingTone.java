package io.github.hiro.lime.hooks;

import android.app.Activity;
import android.app.AndroidAppHelper;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.documentfile.provider.DocumentFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
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

                Uri ringtoneUri = getRingtoneUri(appContext,moduleContext, "ringtone.wav");
                Uri ringtoneUriA = getRingtoneUri(appContext, moduleContext,"dial_tone.wav");

                if (!limeOptions.StopCallTone.checked) {
                    Class<?> voIPBaseFragmentClass = loadPackageParam.classLoader.loadClass("com.linecorp.voip2.common.base.VoIPBaseFragment");
                    XposedBridge.hookAllMethods(voIPBaseFragmentClass, "onCreate", new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            Activity activity = null;
                            try {
                                Object fragment = param.thisObject;
                                activity = (Activity) XposedHelpers.callMethod(fragment, "getActivity");
                            } catch (Throwable t) {
                                Context context = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                                if (context instanceof Activity) {
                                    activity = (Activity) context;
                                }
                            }

                            if (activity != null) {
                                addButton(activity);
                            }
                        }
                    });
                }

                XposedBridge.hookAllMethods(
                        loadPackageParam.classLoader.loadClass(Constants.RESPONSE_HOOK.className),
                        Constants.RESPONSE_HOOK.methodName,
                        new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                String paramValue = param.args[1].toString();
                                Context context = AndroidAppHelper.currentApplication().getApplicationContext();

                                AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                                if (limeOptions.SilentCheck.checked) {
                                    if (audioManager.getRingerMode() == AudioManager.RINGER_MODE_SILENT) {
                                        return;
                                    }
                                }

                                if (paramValue.contains("type:NOTIFIED_RECEIVED_CALL,")) {
                                    XposedBridge.log(paramValue);
                                    if (context != null) {
                                        if (isPlaying) {
                                            XposedBridge.log("Xposed: Already playing");
                                            return;
                                        }

                                        if (ringtoneUri != null) {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                                ringtone = RingtoneManager.getRingtone(context, ringtoneUri);
                                                if (ringtone != null) {
                                                    ringtone.setLooping(true);
                                                    ringtone.play();
                                                    isPlaying = true;
                                                    XposedBridge.log("Ringtone started playing.");
                                                }
                                            } else {
                                                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                                                    XposedBridge.log("Xposed: MediaPlayer is already playing. Not starting new playback.");
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
                                        } else {
                                            XposedBridge.log("Xposed: Ringtone file not found");
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
                                         
                                            ringtone = RingtoneManager.getRingtone(appContext, ringtoneUriA);

                                            if (ringtone != null) {
                                                ringtone.setLooping(true);
                                                ringtone.play();
                                                isPlaying = true;
                                                XposedBridge.log("Ringtone started playing from processToneEvent.");
                                            }
                                        } else {
                                            if (mediaPlayer != null) {
                                                if (mediaPlayer.isPlaying()) {
                                                    XposedBridge.log("Xposed"+ "MediaPlayer is already playing. Stopping playback.");
                                                    mediaPlayer.stop();
                                                    XposedBridge.log("MediaPlayer stopped before starting new one.");
                                                }
                                                mediaPlayer.release();
                                                mediaPlayer = null;
                                            }
                                            
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
                                    AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                                    if (limeOptions.SilentCheck.checked) {
                                        if (audioManager.getRingerMode() == AudioManager.RINGER_MODE_SILENT) {
                                            return;
                                        }
                                    }
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

                                                
                                                AudioManager am = (AudioManager) appContext.getSystemService(Context.AUDIO_SERVICE);

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
                                    }
                                }

                                if (method.getName().equals("activate")) {
                                    if (appContext != null) {
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

                                        
                                        AudioManager am = (AudioManager) appContext.getSystemService(Context.AUDIO_SERVICE);

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


                        }

                    });
                }
            }

        });

    }
    private Button stopButton;
    private void addButton(Activity activity) {
        boolean isMediaPlaying = mediaPlayer != null && mediaPlayer.isPlaying();

        if (isPlaying || isMediaPlaying) {
            if (stopButton != null) {
                return;
            }
            stopButton = new Button(activity);
            stopButton.setText("STOP");
            stopButton.setBackgroundResource(0);
            stopButton.setPadding(0, 0, 0, 0);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
            );

            params.gravity = Gravity.CENTER | Gravity.END;
            params.setMargins(0, 0, 0, 0);
            stopButton.setLayoutParams(params);

            stopButton.setOnClickListener(v -> {
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
                if (stopButton != null) {
                    ViewGroup layout = activity.findViewById(android.R.id.content);
                    layout.removeView(stopButton);
                    stopButton = null;
                }
            });

            ViewGroup layout = activity.findViewById(android.R.id.content);
            layout.addView(stopButton);
        }
    }
    private Uri getRingtoneUri(Context appContext,Context moduleContext, String fileName) {
        String backupUri = loadBackupUri(appContext);
        XposedBridge.log("Lime: loadBackupUri → " + backupUri);

        if (backupUri != null) {
            try {
                Uri treeUri = Uri.parse(backupUri);
                DocumentFile dir = DocumentFile.fromTreeUri(moduleContext, treeUri);

                if (dir == null) {
                    XposedBridge.log("Lime: DocumentFile.fromTreeUri returned null");
                } else {
                    XposedBridge.log("Lime: SAF directory URI → " + dir.getUri());

                    DocumentFile ringtoneFile = dir.findFile(fileName);
                    if (ringtoneFile == null || !ringtoneFile.exists()) {
                        XposedBridge.log("Lime: ファイル未検出 → コピーを実行");
                        copyRingtoneToUri(moduleContext, dir, fileName);
                        ringtoneFile = dir.findFile(fileName);
                    }

                    if (ringtoneFile != null && ringtoneFile.exists()) {
                        XposedBridge.log("Lime: RingtoneFile URI → " + ringtoneFile.getUri());
                        return ringtoneFile.getUri();
                    } else {
                        XposedBridge.log("Lime: コピー後もファイル未検出");
                    }
                }
            } catch (Exception e) {
                XposedBridge.log("Lime: Error accessing SA Furi or copying: " + Log.getStackTraceString(e));
            }
        }

        // フォールバック：リソースからの取得
        Uri resUri = getRingtoneResourceUri(moduleContext, fileName);
        XposedBridge.log("Lime: Fallback resource URI → " + resUri);
        return resUri;
    }

    private void copyRingtoneToUri(Context moduleContext, DocumentFile dir, String fileName) {
        try {
            XposedBridge.log("Lime: copyRingtoneToUri 開始, dir URI=" + dir.getUri());
            String resourceName = fileName.replace(".wav", "");
            int resourceId = moduleContext.getResources().getIdentifier(
                    resourceName, "raw", "io.github.hiro.lime");
            if (resourceId == 0) {
                XposedBridge.log("Lime: resourceId == 0 for " + resourceName);
                return;
            }

            // SAF 上に新規ファイルを作成
            DocumentFile created = dir.createFile("audio/wav", fileName);
            if (created == null) {
                XposedBridge.log("Lime: dir.createFile returned null");
                return;
            }
            XposedBridge.log("Lime: Created file URI → " + created.getUri());

            try (
                    InputStream  in  = moduleContext.getResources().openRawResource(resourceId);
                    OutputStream out = moduleContext.getContentResolver().openOutputStream(created.getUri())
            ) {
                byte[] buf = new byte[4096];
                int   len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                XposedBridge.log("Lime: copyRingtoneToUri 成功 for " + fileName);
            }
        } catch (Exception e) {
            XposedBridge.log("Lime: Error in copyRingtoneToUri: " + Log.getStackTraceString(e));
        }
    }


    private Uri getRingtoneResourceUri(Context moduleContext, String fileName) {
        String resourceName = fileName.replace(".wav", "");
        int resourceId = moduleContext.getResources().getIdentifier(
                resourceName, "raw", "io.github.hiro.lime");
        if (resourceId != 0) {
            return Uri.parse("android.resource://io.github.hiro.lime/raw/" + resourceName);
        }
        return null;
    }

    private String loadBackupUri(Context context) {
        File settingsFile = new File(context.getFilesDir(), "LimeBackup/backup_uri.txt");
        if (!settingsFile.exists()) return null;

        try (BufferedReader br = new BufferedReader(new FileReader(settingsFile))) {
            return br.readLine();
        } catch (IOException e) {
            XposedBridge.log("Lime: Error reading backup URI: " + e.getMessage());
            return null;
        }
    }
}
