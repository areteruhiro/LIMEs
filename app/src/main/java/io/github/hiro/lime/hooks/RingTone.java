package io.github.hiro.lime.hooks;

import android.app.AndroidAppHelper;
import android.app.Application;
import android.content.Context;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Environment;

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


                                File ringtoneDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_RINGTONES), "LimeBackup");
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
                                        if (ringtone != null && ringtone.isPlaying()) {
                                            //Log.d("Xposed", "Ringtone is already playing. Skipping playback.");
                                            return; // 再生中の場合は何もしない
                                        }
                                        Uri ringtoneUri = Uri.fromFile(destFile); // コピーしたファイルのURIを取得
                                        ringtone = RingtoneManager.getRingtone(context, ringtoneUri);
                                        ringtone.play();
                                        isPlaying = true;
                                    }
                                }

                                if (paramValue.contains("RESULT=REJECTED,") || paramValue.contains("RESULT=REJECTED,")) {
                                    if (ringtone != null && ringtone.isPlaying()) {
                                        ringtone.stop();
                                        isPlaying = false;
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
                                if (ringtone != null && ringtone.isPlaying()) {
                                    ringtone.stop();
                                    isPlaying = false;
                                }
                            }


                            if (method.getName().equals("stop")) {
                                if (ringtone != null && ringtone.isPlaying()) {
                                    ringtone.stop();
                                    isPlaying = false;
                                }
                            }


                            if (method.getName().equals("processToneEvent")) {
                                Object arg0 = param.args[0];
                                if (limeOptions.DialTone.checked) {
                                    //Log.d("Xposed", "MuteTone is enabled. Suppressing tone event.");
                                    param.setResult(null);
                                    return;
                                }

                                if (arg0.toString().contains("START")) {
                                    if (appContext != null) {
                                        // ringtone が初期化されており、再生中の場合はスキップ
                                        if (ringtone != null && ringtone.isPlaying()) {
                                            //Log.d("Xposed", "Ringtone is already playing. Skipping playback.");
                                            return; // 再生中の場合は何もしない
                                        }
                                        Context moduleContext = AndroidAppHelper.currentApplication().createPackageContext(
                                                "io.github.hiro.lime", Context.CONTEXT_IGNORE_SECURITY);

                                        String resourceName = "dial_tone";
                                        int resourceId = moduleContext.getResources().getIdentifier(resourceName, "raw", "io.github.hiro.lime");


                                        File ringtoneDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_RINGTONES), "LimeBackup");
                                        if (!ringtoneDir.exists()) {
                                            ringtoneDir.mkdirs(); // ディレクトリが存在しない場合は作成
                                        }
                                        File destFile = new File(ringtoneDir, resourceName + ".wav");

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
                                        ringtone = RingtoneManager.getRingtone(appContext, ringtoneUri);

                                        if (ringtone != null) {
                                            //Log.d("Xposed", "Playing ringtone.");
                                            ringtone.play();
                                            isPlaying = true;
                                        } else {
                                            //Log.d("Xposed", "Ringtone is null. Cannot play ringtone.");
                                            return;
                                        }
                                    } else {
                                        //Log.d("Xposed", "appContext is null. Cannot play ringtone.");
                                        return;
                                    }
                                } else {
                                    //Log.d("Xposed", "Argument is not 'START'. Actual value: " + arg0);
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
                                    if (ringtone != null && ringtone.isPlaying()) {
                                        ringtone.stop();
                                        isPlaying = false;
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