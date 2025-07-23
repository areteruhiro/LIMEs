package io.github.hiro.lime.hooks;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.hiro.lime.LimeOptions;

    public class SendEnterChange implements IHook {
        private static volatile Object savedWInstance = null;

        @Override
        public void hook(LimeOptions limeOptions, XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
            ClassLoader cl = loadPackageParam.classLoader;
            if (limeOptions.SendEnterChange.checked) {
                Class<?> wCls = XposedHelpers.findClass("ly.w", cl);
                XposedBridge.hookAllConstructors(wCls, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (savedWInstance == null) {
                            savedWInstance = param.thisObject;

                        }
                    }
                });


                XposedHelpers.findAndHookMethod(
                        "jp.naver.line.android.activity.chathistory.ChatHistoryActivity",
                        cl,
                        "onCreate",
                        Bundle.class,
                        new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) {
                                Activity act = (Activity) param.thisObject;
                                FrameLayout root = act.findViewById(android.R.id.content);

                                Button btn = new Button(act);
                                btn.setText("EnterSend");
                                FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                                        ViewGroup.LayoutParams.WRAP_CONTENT,
                                        ViewGroup.LayoutParams.WRAP_CONTENT);
                                lp.setMargins(350, 150, 0, 0);
                                root.addView(btn, lp);

                                btn.setOnClickListener(v -> {
                                    if (savedWInstance == null) {
                                        Toast.makeText(act, "ly.w not yet initialized", Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                    try {

                                        Class<?> keyEnumClass = XposedHelpers.findClass(
                                                "jp.naver.line.android.db.generalkv.dao.a", cl);
                                        Class<?> kvDaoClass = XposedHelpers.findClass(
                                                "jp.naver.line.android.db.generalkv.dao.c", cl);
                                        Object key = XposedHelpers.getStaticObjectField(keyEnumClass, "CHATROOM_ENTER_SEND");
                                        Boolean cur = (Boolean) XposedHelpers.callStaticMethod(kvDaoClass, "b", key);
                                        Boolean tog = !cur;
                                        XposedHelpers.callStaticMethod(kvDaoClass, "l", key, tog);
                                        XposedHelpers.callMethod(savedWInstance, "G");
                                        Toast.makeText(act, ""+tog, Toast.LENGTH_SHORT).show();
                                    } catch (Throwable t) {
                                        Toast.makeText(act, "Toggle failed", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        }
                );

            }
        }
    }
    
