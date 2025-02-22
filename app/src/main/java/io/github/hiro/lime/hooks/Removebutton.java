package io.github.hiro.lime.hooks;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

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

            "chat_ui_group_call_header_starter_voice_button",
            "chat_ui_group_call_header_starter_voice_button_text",
            "chat_ui_group_call_header_starter_voice_button_icon",

            "chat_ui_group_call_header_starter_video_button",
            "chat_ui_group_call_header_starter_video_button_text",
            "chat_ui_group_call_header_starter_video_button_icon",

            "chat_ui_singlecall_layer_video_button",
            "chat_ui_send_button_image",


            "top_space_for_photobooth",
            "chat_ui_photo_booth_root",

            "chat_ui_call_header_starter_photobooth_button",

            "chat_ui_group_call_header_starter_photobooth_new_badge_icon",
            "chat_ui_group_call_header_starter_photobooth_new_badge_background",
            "chat_ui_group_call_header_starter_photobooth_button_text",
            "chat_ui_group_call_header_starter_photobooth_button_icon",
            "bottom_action_photo_booth",
    };

    @Override
    public void hook(LimeOptions limeOptions, XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        XposedHelpers.findAndHookMethod("android.view.LayoutInflater", lpparam.classLoader,
                "inflate", int.class, ViewGroup.class, boolean.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        handleView((View) param.getResult(), limeOptions);
                    }
                });

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

        if (!initialized) {
            synchronized (this) {
                if (!initialized) {
                    initialize(view.getContext(), options);
                    initialized = true;
                }
            }
        }

        if (targetIds.isEmpty() || !targetIds.contains(view.getId())) return;
        applyDefaultOptimization(view);
        adjustParentLayout(view);
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
                    }
                }
            }
        } catch (Exception ignored) {
            XposedBridge.log("LIME: Initialization failed - ");
        }
    }
    private void adjustParentLayout(View view) {
        ViewGroup parent = (ViewGroup) view.getParent();
        if (parent != null) {
            ViewGroup.LayoutParams params = parent.getLayoutParams();
            if (params instanceof LinearLayout.LayoutParams) {
                LinearLayout.LayoutParams linearParams = (LinearLayout.LayoutParams) params;
                linearParams.weight = 0;
                parent.setLayoutParams(linearParams);
            }
        }
    }
    private boolean isOptionEnabled(LimeOptions options, String resName) {
        switch (resName) {
            case "chat_ui_call_header_starter_photobooth_button":
            case "chat_ui_group_call_header_starter_photobooth_new_badge_icon":
            case "chat_ui_group_call_header_starter_photobooth_new_badge_background":
            case "chat_ui_group_call_header_starter_photobooth_new_badge_text":
            case "chat_ui_group_call_header_starter_photobooth_button_text":
            case "chat_ui_group_call_header_starter_photobooth_button_icon":
            case "bottom_action_photo_booth":

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




    private void applyDefaultOptimization(View view) {
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (params == null) return;
        params.width = 1;
        params.height = 1;
        if (params instanceof ViewGroup.MarginLayoutParams) {
            ((ViewGroup.MarginLayoutParams) params).setMargins(0, 0, 0, 0);
        }

        view.setLayoutParams(params);
    }

    private int convertDpToPixel(float dp, Context context) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }
}


