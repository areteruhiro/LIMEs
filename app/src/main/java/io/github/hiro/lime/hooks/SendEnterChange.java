package io.github.hiro.lime.hooks;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.hiro.lime.LimeOptions;

    public class SendEnterChange implements IHook {
        @Override
        public void hook(LimeOptions limeOptions, XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
            if (limeOptions.SendEnterChange.checked) {

                ClassLoader cl = loadPackageParam.classLoader;
                final Class<?> keyEnumCls = XposedHelpers.findClass(
                        "jp.naver.line.android.db.generalkv.dao.a", cl);
                final Class<?> kvDaoCls = XposedHelpers.findClass(
                        "jp.naver.line.android.db.generalkv.dao.c", cl);
                final Object chatroomEnterSendKey =
                        XposedHelpers.getStaticObjectField(keyEnumCls, "CHATROOM_ENTER_SEND");

                XposedHelpers.findAndHookMethod(
                        "com.linecorp.line.chatlist.view.fragment.ChatListFragment",
                        cl,
                        "onCreateView",
                        LayoutInflater.class, ViewGroup.class, Bundle.class,
                        new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) {
                                View rootView = (View) param.getResult();
                                if (!(rootView instanceof ViewGroup)) return;
                                ViewGroup root = (ViewGroup) rootView;
                                Context ctx = root.getContext();

                                String tag = "xposed_enter_send_toggle_btn";
                                if (root.findViewWithTag(tag) != null) return;

                                boolean cur = (Boolean) XposedHelpers.callStaticMethod(
                                        kvDaoCls, "b", chatroomEnterSendKey);

                                Button btn = new Button(ctx);
                                btn.setTag(tag);
                                btn.setText("Enter" + (cur ? "ON" : "OFF"));
                                final String PREF = "enter_send_pref";
                                final String KEY_X = "btn_x";
                                final String KEY_Y = "btn_y";
                                SharedPreferences sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
                                float savedX = sp.getFloat(KEY_X, -1f);
                                float savedY = sp.getFloat(KEY_Y, -1f);
                                root.addView(btn);
                                root.post(() -> {
                                    if (savedX >= 0 && savedY >= 0) {
                                        btn.setX(savedX);
                                        btn.setY(savedY);
                                    } else {

                                        int pw = root.getWidth();
                                        int ph = root.getHeight();
                                        int cx = pw / 2 - btn.getWidth() / 2;
                                        float dp100 = TypedValue.applyDimension(
                                                TypedValue.COMPLEX_UNIT_DIP, 100,
                                                ctx.getResources().getDisplayMetrics());
                                        int cy = ph / 2 - btn.getHeight() / 2 - (int) dp100;
                                        btn.setX(cx);
                                        btn.setY(cy);
                                    }
                                });
                                btn.setOnTouchListener(new View.OnTouchListener() {
                                    private final Handler h = new Handler();
                                    private boolean dragging = false;
                                    private float offsetX, offsetY;
                                    private final Runnable startDrag = () -> dragging = true;

                                    @Override
                                    public boolean onTouch(View v, MotionEvent e) {
                                        switch (e.getActionMasked()) {
                                            case MotionEvent.ACTION_DOWN:
                                                h.postDelayed(startDrag, 500);
                                                offsetX = e.getRawX() - v.getX();
                                                offsetY = e.getRawY() - v.getY();
                                                return false;
                                            case MotionEvent.ACTION_MOVE:
                                                if (dragging) {
                                                    v.setX(e.getRawX() - offsetX);
                                                    v.setY(e.getRawY() - offsetY);
                                                    return true;
                                                }
                                                return false;

                                            case MotionEvent.ACTION_UP:
                                            case MotionEvent.ACTION_CANCEL:
                                                h.removeCallbacks(startDrag);
                                                if (dragging) {
                                                    dragging = false;
                                                    sp.edit()
                                                            .putFloat(KEY_X, v.getX())
                                                            .putFloat(KEY_Y, v.getY())
                                                            .apply();
                                                }
                                                return false;
                                        }
                                        return false;
                                    }
                                });
                                btn.setOnClickListener(v -> {
                                    try {

                                        boolean before = (Boolean) XposedHelpers.callStaticMethod(
                                                kvDaoCls, "b", chatroomEnterSendKey);
                                        boolean toggled = !before;
                                        XposedHelpers.callStaticMethod(kvDaoCls, "l",
                                                chatroomEnterSendKey, toggled);
                                        XposedBridge.log("Enter-Send DB: " + before + " → " + toggled);

                                        int newOpt = toggled ? 4 : 1;
                                        propagateImeOptionsToAll(ctx, newOpt);

                                        btn.setText("Enter " + (toggled ? "ON" : "OFF"));
                                        Toast.makeText(ctx,
                                                "Enter " + (toggled ? "ON" : "OFF"),
                                                Toast.LENGTH_SHORT).show();

                                    } catch (Throwable t) {
                                        XposedBridge.log(" Toggle failed: " + Log.getStackTraceString(t));
                                        Toast.makeText(ctx, "Toggle failed", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        });



            }
        }
        void propagateImeOptionsToAll (Context ctx,int imeOpt){
            try {
                Object wmGlobal = XposedHelpers.callStaticMethod(
                        XposedHelpers.findClass("android.view.WindowManagerGlobal",
                                ctx.getClassLoader()),
                        "getInstance");
                Object mViews = XposedHelpers.getObjectField(wmGlobal, "mViews");
                if (mViews instanceof java.util.List<?>) {
                    for (Object rootObj : (java.util.List<?>) mViews) {
                        if (rootObj instanceof View)
                            updateImeOptionsRec((View) rootObj, imeOpt);
                    }
                }
            } catch (Throwable e) {
                XposedBridge.log("propagateImeOptionsToAll error: " + Log.getStackTraceString(e));
            }
        }

        void updateImeOptionsRec (View v,int imeOpt){
            if (v.getClass().getName()
                    .equals("com.linecorp.line.chat.ui.resources.message.input.ChatHistoryEditText")) {
                try {
                    XposedHelpers.callMethod(v, "setImeOptions", imeOpt);
                } catch (Throwable ignore) {
                }
            }
            if (v instanceof ViewGroup) {
                ViewGroup vg = (ViewGroup) v;
                for (int i = 0; i < vg.getChildCount(); i++)
                    updateImeOptionsRec(vg.getChildAt(i), imeOpt);
            }
        }
}