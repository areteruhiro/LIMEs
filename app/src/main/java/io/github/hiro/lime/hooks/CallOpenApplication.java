package io.github.hiro.lime.hooks;



import android.app.Activity;
import androidx.fragment.app.Fragment; // AndroidXのFragmentをインポート

import android.content.Context;
import android.content.Intent;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.hiro.lime.LimeOptions;

public class CallOpenApplication implements IHook {
    @Override
    public void hook(LimeOptions limeOptions, XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        if (!limeOptions.CallOpenApplication.checked) return;
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
    private void addButton(Activity activity) {

        Button button = new Button(activity);
        button.setText("LINE");
        button.setBackgroundResource(0);
        // button.setBackgroundColor(Color.TRANSPARENT);

        // button.setTextColor(Color.WHITE);

        button.setPadding(0, 0, 0, 0);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );

        params.gravity = Gravity.BOTTOM | Gravity.END;
        params.setMargins(0, 0, 16, 16);
        button.setLayoutParams(params);

        button.setOnClickListener(v -> {
            Intent intent = activity.getPackageManager().getLaunchIntentForPackage("jp.naver.line.android");
            if (intent != null) {
                activity.startActivity(intent);
            } else {
                Toast.makeText(activity, "アプリが見つかりません", Toast.LENGTH_SHORT).show();
            }
        });
        ViewGroup layout = activity.findViewById(android.R.id.content);
        layout.addView(button);
    }
    }