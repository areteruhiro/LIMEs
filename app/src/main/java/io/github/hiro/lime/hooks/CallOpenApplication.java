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
                // 任意のオブジェクトからActivityを取得
                Activity activity = null;
                try {
                    Object fragment = param.thisObject;
                    activity = (Activity) XposedHelpers.callMethod(fragment, "getActivity");
                } catch (Throwable t) {
                    // 代替方法でContextを取得
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
        // ボタンを作成
        Button button = new Button(activity);
        button.setText("LINE");

        // 背景を透明に設定
        button.setBackgroundResource(0); // または
        // button.setBackgroundColor(Color.TRANSPARENT);

        // テキスト色を設定（必要に応じて）
        // button.setTextColor(Color.WHITE);

        // パディングを0に設定（必要に応じて）
        button.setPadding(0, 0, 0, 0);

        // レイアウトパラメータ設定
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );

        params.gravity = Gravity.BOTTOM | Gravity.END;
        params.setMargins(0, 0, 16, 16);
        button.setLayoutParams(params);

        // クリックリスナー
        button.setOnClickListener(v -> {
            Intent intent = activity.getPackageManager().getLaunchIntentForPackage("jp.naver.line.android");
            if (intent != null) {
                activity.startActivity(intent);
            } else {
                Toast.makeText(activity, "アプリが見つかりません", Toast.LENGTH_SHORT).show();
            }
        });

        // レイアウトに追加
        ViewGroup layout = activity.findViewById(android.R.id.content);
        layout.addView(button);
    }
    }