package io.github.hiro.lime.hooks;

import android.content.Context;
import android.content.res.Resources;
import android.view.View;
import android.view.ViewGroup;
import java.util.HashSet;
import java.util.Set;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.hiro.lime.LimeOptions;

public class Removebutton implements IHook {
    private Set<Integer> targetIds = new HashSet<>();
    private volatile boolean initialized = false;

    private static final String[] TARGET_RESOURCES = {
            "chat_ui_call_header_starter_photobooth_button",
            "chat_ui_group_call_header_starter_voice_button",
            "chat_ui_group_call_header_starter_video_button",
            "chat_ui_singlecall_layer_video_button",
            "chat_ui_send_button_image"
    };

    @Override
    public void hook(LimeOptions limeOptions, XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        // 1. LayoutInflaterフック（主要な処理）
        XposedHelpers.findAndHookMethod("android.view.LayoutInflater", lpparam.classLoader,
                "inflate", int.class, ViewGroup.class, boolean.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        handleView((View) param.getResult(), limeOptions);
                    }
                });

        // 2. onAttachedToWindowフック（フォールバック）
        XposedHelpers.findAndHookMethod("android.view.View", lpparam.classLoader,
                "onAttachedToWindow", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        handleView((View) param.thisObject, limeOptions);
                    }
                });
    }

    private void handleView(View view, LimeOptions options) {
        if (view == null) return;

        // 初期化チェック（スレッドセーフ）
        if (!initialized) {
            synchronized (this) {
                if (!initialized) {
                    initialize(view.getContext(), options);
                    initialized = true;
                }
            }
        }

        // 高速チェック
        if (targetIds.isEmpty() || !targetIds.contains(view.getId())) return;

        // 最適化処理
        optimizeViewLayout(view);
    }

    private void initialize(Context context, LimeOptions options) {
        try {
            Resources res = context.getPackageManager()
                    .getResourcesForApplication(context.getPackageName());

            for (String resName : TARGET_RESOURCES) {
                if (isOptionEnabled(options, resName)) {
                    int id = res.getIdentifier(resName, "id", context.getPackageName());
                    if (id != View.NO_ID) {
                        targetIds.add(id);
                        XposedBridge.log("LIME: Registered ID - " + resName + " = 0x" + Integer.toHexString(id));
                    }
                }
            }
        } catch (Exception e) {
            XposedBridge.log("LIME: Initialization failed - " + e);
        }
    }

    private boolean isOptionEnabled(LimeOptions options, String resName) {
        switch (resName) {
            case "chat_ui_call_header_starter_photobooth_button":
                return options.photoboothButtonOption.checked;
            case "chat_ui_group_call_header_starter_voice_button":
                return options.voiceButtonOption.checked;
            case "chat_ui_group_call_header_starter_video_button":
                return options.videoButtonOption.checked;
            case "chat_ui_singlecall_layer_video_button":
                return options.videoSingleButtonOption.checked;
            default:
                return false;
        }
    }


    private void optimizeViewLayout(View view) {
        try {
            ViewGroup.LayoutParams params = view.getLayoutParams();
            if (params == null) return;

            params.width = 1;
            params.height = 1;

            if (params instanceof ViewGroup.MarginLayoutParams) {
                ((ViewGroup.MarginLayoutParams) params).setMargins(0, 0, 0, 0);
            }

            view.setLayoutParams(params);
            // XposedBridge.log("LIME: Optimized view - " + view.getClass().getSimpleName());

        } catch (Exception e) {
            //XposedBridge.log("LIME: Optimization error - " + e);

        }
    }
}


    //        XposedHelpers.findAndHookMethod(
//                "android.view.View",
//                loadPackageParam.classLoader,
//                "onAttachedToWindow",
//                new XC_MethodHook() {
//                    @Override
//                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//                        // Viewからリソースを取得
//                        View view = (View) param.thisObject;
//                        Context context = view.getContext();
//                        Resources resources = context.getResources();
//
//                        // リソースIDを取得
//                        int resourceId = view.getId(); // ViewのIDを取得
//
//                        // リソース名をログに出力
//                        if (resourceId != View.NO_ID) {
//                            Log.d("RemovePhotoBooth", "onAttachedToWindow called");
//                            Log.d("RemovePhotoBooth", "Resource package name: " + resources.getResourcePackageName(resourceId));
//                            Log.d("RemovePhotoBooth", "Resource name: " + resources.getResourceName(resourceId));
//                            Log.d("RemovePhotoBooth", "Resource type: " + resources.getResourceTypeName(resourceId));
//                        } else {
//                            Log.d("RemovePhotoBooth", "No resource ID found for the view.");
//                        }
//                    }
//                }
//        );
