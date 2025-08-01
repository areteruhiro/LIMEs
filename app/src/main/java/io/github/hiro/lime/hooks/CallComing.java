package io.github.hiro.lime.hooks;

import android.app.AndroidAppHelper;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.hiro.lime.LimeOptions;

public class CallComing implements IHook {
    private boolean isButtonAdded = false; // ボタンが追加されたかどうかを追跡するフラグ
    private static volatile Object sTalkClient;
    static boolean enterSendEnabled = true; // 初期状態
    private static Constructor<?> toggleCtor;
    private static Class<?> toggleCls;
    private static Method invokeMethod;
    private static volatile Object savedWInstance = null;

    @Override
    public void hook(LimeOptions limeOptions, XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        if (!limeOptions.CallComing.checked) return;
        ClassLoader cl = loadPackageParam.classLoader;
        XposedHelpers.findAndHookConstructor(
                "jp.naver.line.android.thrift.client.impl.TalkServiceClientImpl", cl, cl.loadClass("Wp1.k"),
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        sTalkClient = param.thisObject;
                        XposedBridge.log(" Captured TalkServiceClientImpl:" + sTalkClient);
                    }
                }
        );
//        XposedBridge.hookAllMethods(
//                loadPackageParam.classLoader.loadClass(Constants.RESPONSE_HOOK.className),
//                Constants.RESPONSE_HOOK.methodName,
//                new XC_MethodHook() {
//
//                    private final java.util.concurrent.atomic.AtomicBoolean waitNoop = new java.util.concurrent.atomic.AtomicBoolean(false);
//                    @Override
//                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//
//                        String body = param.args[1].toString();
//                        ClassLoader cl = param.thisObject.getClass().getClassLoader();
//
//                        if (body.contains("type:NOTIFIED_RECEIVED_CALL,")) {
//                            waitNoop.set(true);
//                            return;
//                        }
//                        if (waitNoop.get() && body.contains("noop_result") && body.contains("e:null")) {
//                            waitNoop.set(false);
//                            try {
//                                sendUpdate(false /*disableInCall*/, cl);
//                            } catch (Throwable t) {
//                                XposedBridge.log(t);
//                            }
//                            return;
//                        }
//                    }
//                });
                        XposedHelpers.findAndHookMethod(
                                "com.linecorp.voip2.dependency.elsa.photobooth.ElsaPhotoBoothContext$a$b",
                                loadPackageParam.classLoader,
                                "invokeSuspend",
                                Object.class,
                                new XC_MethodHook() {

                                    @Override
                                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {

                                        String  argStr    = String.valueOf(param.args[0]);
                                        boolean isFailure = argStr.contains("Failure(");
                                        new Thread(() -> {
                                            try {
                                                ClassLoader cl = param.thisObject.getClass().getClassLoader();

                                                Class<?> r8Clz   = XposedHelpers.findClass("Fq1.r8", cl);
                                                //以下のまま　CATION_INCOMING_CALL
                                                Object   enumVal = r8Clz.getField("NOTIFICATION_INCOMING_CALL").get(null);
                                                int      rawVal  = (Integer) XposedHelpers.callMethod(enumVal, "getValue");
                                                Object   thriftR8 = XposedHelpers.callStaticMethod(r8Clz, "a", rawVal);

                                                Set<Object> attrs = new HashSet<>();
                                                attrs.add(thriftR8);

                                                //allowUnregistrationSecondaryDeviceで検索
                                                Class<?> q8Clz = XposedHelpers.findClass("Fq1.q8",  cl);
                                                Object   q8    = XposedHelpers.newInstance(q8Clz);

                                                //set(5, true);で検索
                                                XposedHelpers.callMethod(q8, "q0", isFailure);

                                                int seq = XposedHelpers.getIntField(sTalkClient, "p") + 1;
                                                XposedHelpers.setIntField(sTalkClient, "p", seq);
//executeWithoutThrow
                                                //settings
                                                Object resp = XposedHelpers.callMethod(
                                                        sTalkClient, "L0",
                                                        seq, attrs, q8);

                                                XposedBridge.log("updateSettingsAttributes2 resp = " + resp);

                                                String toastMsg ="着信通知 " + (isFailure ? "ON" : "拒否");
                                                showToast(toastMsg);
                                            } catch (Throwable t) {
                                                XposedBridge.log(t);
                                            }
                                        }).start();
                                    }
                                });


    }
//    private static void sendUpdate(boolean disableInCall, ClassLoader cl) throws Throwable {
//
//        Class<?> r8Clz   = XposedHelpers.findClass("Fq1.r8", cl);
//        Object   enumVal = r8Clz.getField("NOTIFICATION_INCOMING_CALL").get(null);
//        int      rawVal  = (Integer) XposedHelpers.callMethod(enumVal, "getValue");
//        Object   thriftR8 = XposedHelpers.callStaticMethod(r8Clz, "a", rawVal);
//
//        Set<Object> attrs = new java.util.HashSet<>();
//        attrs.add(thriftR8);
//        Class<?> q8Clz = XposedHelpers.findClass("Fq1.q8", cl);
//        Object   q8    = XposedHelpers.newInstance(q8Clz);
//        XposedHelpers.callMethod(q8, "q0", disableInCall);
//        int seq = XposedHelpers.getIntField(sTalkClient, "p") + 1;
//        XposedHelpers.setIntField(sTalkClient, "p", seq);
//
//        Object resp = XposedHelpers.callMethod(sTalkClient, "L0", seq, attrs, q8);
//        XposedBridge.log("▶ updateSettingsAttributes2 resp = " + resp);
//        String toastMsg = "着信通知 " + (disableInCall ? "ON" : "拒否");
//        showToast(toastMsg);
//    }

    private static void showToast(String msg) {
        android.content.Context ctx = AndroidAppHelper.currentApplication();
        new Handler(Looper.getMainLooper()).post(() ->
                Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show());
    }


    }

