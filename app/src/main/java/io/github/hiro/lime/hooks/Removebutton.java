package io.github.hiro.lime.hooks;

import static io.github.hiro.lime.Main.limeOptions;

import android.content.Context;
import android.content.res.Resources;
import android.view.View;
import android.view.ViewGroup;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.hiro.lime.LimeOptions;
public class Removebutton implements IHook {
    private static final String[] TARGET_RESOURCES = {
            "chat_ui_call_header_starter_photobooth_button",
            "chat_ui_group_call_header_starter_voice_button",
            "chat_ui_group_call_header_starter_video_button",
            "chat_ui_singlecall_layer_video_button",
            "chat_ui_send_button_image"
    };

    @Override
    public void hook(LimeOptions limeOptions, XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        XposedHelpers.findAndHookMethod("android.view.View", loadPackageParam.classLoader, "onAttachedToWindow", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                final View view = (View) param.thisObject;
                if (view == null) return;

                final Context context = view.getContext();
                if (context == null) return;
                final Resources res = context.getResources();
                if (res == null) return;

                final String packageName = context.getPackageName();

                view.post(() -> {
                    try {
                        if (view.getParent() == null) return;
                        for (String resName : TARGET_RESOURCES) {
                            boolean isOptionEnabled = isOptionEnabled(limeOptions, resName);
                            if (!isOptionEnabled) continue;
                            int targetId = res.getIdentifier(resName, "id", packageName);
                            if (targetId != View.NO_ID && view.getId() == targetId) {
                                optimizeViewLayout(view);
                                break;
                            }
                        }
                    } catch (Exception e) {
                        XposedBridge.log("View processing error: " + e);
                    }
                });
            }
        });
    }

    private boolean isOptionEnabled(LimeOptions options, String resName) {
        switch (resName) {
            case "chat_ui_call_header_starter_photobooth_button":
                return limeOptions.photoboothButtonOption.checked;
            case "chat_ui_group_call_header_starter_voice_button":
                return limeOptions.voiceButtonOption.checked;
            case "chat_ui_group_call_header_starter_video_button":
                return limeOptions.videoButtonOption.checked;
            case "chat_ui_singlecall_layer_video_button":
                return limeOptions.videoSingleButtonOption.checked;
//            case "chat_ui_send_button_image":
//                return limeOptions.sendButtonImageOption.checked;
            default:
                return false;
        }
    }

    private void optimizeViewLayout(final View view) {
        // 既存の実装を保持
        try {
            view.setVisibility(View.GONE);
            view.animate().cancel();

            ViewGroup.LayoutParams params = view.getLayoutParams();
            if (params != null) {
                params.width = 1;
                params.height = 1;
                if (params instanceof ViewGroup.MarginLayoutParams) {
                    ((ViewGroup.MarginLayoutParams) params).setMargins(0, 0, 0, 0);
                }
                view.setLayoutParams(params);
            }
            if (view.getParent() instanceof ViewGroup) {
                ((ViewGroup) view.getParent()).requestLayout();
            }
            view.setClickable(false);
            view.setFocusable(false);
            view.setEnabled(false);
        } catch (Exception e) {
            XposedBridge.log("Layout optimization error: " + e);
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
