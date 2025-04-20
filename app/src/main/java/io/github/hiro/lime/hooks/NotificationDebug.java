package io.github.hiro.lime.hooks;


import static de.robv.android.xposed.XposedBridge.hookMethod;
import static io.github.hiro.lime.Main.limeOptions;

import android.app.AndroidAppHelper;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.service.notification.StatusBarNotification;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Random;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.hiro.lime.LimeOptions;

public class NotificationDebug implements IHook {
    private static final String TAG = "NotificationDebug";
    private Context context;

    @Override
    public void hook(LimeOptions limeOptions, XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {


        Class<?> clazz = loadPackageParam.classLoader.loadClass("MX.f");
        XposedBridge.hookAllConstructors(clazz, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                // 1. コンストラクタ引数のログ出力
                StringBuilder argsLog = new StringBuilder("MX.f コンストラクタ引数:\n");
                for (int i = 0; i < param.args.length; i++) {
                    argsLog.append("  arg[").append(i).append("]: ").append(param.args[i]).append("\n");
                }
                XposedBridge.log(argsLog.toString());

                if (param.args.length > 20) {
                    XposedBridge.log("[NOTIFICATION_ID] arg[20]: " + param.args[20]);
                }

                // 3. スタックトレースの全情報を出力
                StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
                StringBuilder stackLog = new StringBuilder("MX.f 呼び出しスタックトレース:\n");
                for (int i = 0; i < stackTrace.length; i++) {
                    stackLog.append("  [").append(i).append("] ").append(stackTrace[i]).append("\n");
                }
                XposedBridge.log(stackLog.toString());
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                int notificationId = XposedHelpers.getIntField(param.thisObject, "v");
                XposedBridge.log("生成された notificationId: " + notificationId);
            }
        });
// MX.fの全コンストラクタをフック
        XposedBridge.hookAllConstructors(clazz, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                // コンストラクタの引数数をログ出力
                XposedBridge.log("MX.f constructor called with " + param.args.length + " args");

                // 各引数の型をチェック
                for (int i = 0; i < param.args.length; i++) {
                    XposedBridge.log("  arg[" + i + "] type: " + (param.args[i] != null ? param.args[i].getClass().getName() : "null"));
                }
            }
        });
//        XposedBridge.hookAllConstructors(clazz, new XC_MethodHook() {
//            @Override
//            protected void beforeHookedMethod(MethodHookParam param) {
//                try {
//                    // コンストラクタの引数数で処理を分岐
//                    if (param.args.length == 24) { // パターンB
//                        // notificationIdがarg[20]の場合
//                        if (param.args.length > 20 && param.args[20] instanceof Integer) {
//                            param.args[20] = generateNewNotificationId(); // 新しいID生成
//                        }
//                    } else if (param.args.length == 23) { // パターンA
//                        // 異なる引数位置に対応
//                        if (param.args.length > 19 && param.args[19] instanceof Integer) {
//                            param.args[19] = generateNewNotificationId();
//                        }
//                    }
//                } catch (Exception e) {
//                    XposedBridge.log("Error modifying notificationId: " + e);
//                }
//            }
//
//            private int generateNewNotificationId() {
//                // 新しいID生成ロジック（例：ランダム値）
//                return new Random().nextInt(Integer.MAX_VALUE);
//            }
//        });
        XposedBridge.hookAllConstructors(clazz, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                try {
                    // コンストラクタ引数の詳細ログ
                    StringBuilder log = new StringBuilder("MX.f Constructor Args:\n");
                    for (int i = 0; i < param.args.length; i++) {
                        Object arg = param.args[i];
                        log.append(String.format("[%02d] %-20s: %s\n",
                                i,
                                (arg != null) ? arg.getClass().getSimpleName() : "null",
                                arg));
                    }
                    XposedBridge.log(log.toString());

                    // Notification IDの詳細解析
                    if (param.args.length > 20 && param.args[20] instanceof Integer) {
                        int notificationId = (Integer) param.args[20];
                        XposedBridge.log(String.format(
                                "Detected Notification ID: %,d (Hex: 0x%08X)",
                                notificationId,
                                notificationId
                        ));

                        // ハッシュ生成の検証
                        String chatId = (String) param.args[0];
                        String messageId = (String) param.args[6];
                        String seed = chatId + "|" + messageId;
                        int calculatedHash = seed.hashCode();

                        XposedBridge.log(String.format(
                                "Hash Validation:\n" +
                                        " - Chat ID:    %s\n" +
                                        " - Message ID: %s\n" +
                                        " - Seed:       %s\n" +
                                        " - Actual ID:  %,d\n" +
                                        " - Calculated: %,d\n" +
                                        " - Match:      %b",
                                chatId, messageId, seed,
                                notificationId, calculatedHash,
                                notificationId == calculatedHash
                        ));
                    }
                } catch (Exception e) {
                    XposedBridge.log("MX.f hook error: " + e);
                }
            }
        });
        XposedBridge.hookAllMethods(XposedHelpers.findClass("java.lang.String", loadPackageParam.classLoader),
                "hashCode",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        String str = (String) param.thisObject;
                        if (str.contains("|") && str.length() > 50) {
                            XposedBridge.log("Potential seed: " + str);
                        }
                    }
                }
        );

    }
    }
