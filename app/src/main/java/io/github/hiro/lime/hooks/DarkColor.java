package io.github.hiro.lime.hooks;

import static io.github.hiro.lime.Main.limeOptions;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.TextView;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.hiro.lime.LimeOptions;

public class DarkColor implements IHook {
    @Override
    public void hook(LimeOptions limeOptions, XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        if (!limeOptions.DarkColor.checked) return;
        XposedHelpers.findAndHookMethod("android.view.View", loadPackageParam.classLoader, "onAttachedToWindow", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                View view = (View) param.thisObject;
                checkAndChangeBackgroundColor(view);
                checkAndChangeTextColor(view);
            }
        });
        XposedHelpers.findAndHookMethod("android.view.View", loadPackageParam.classLoader, "setBackgroundColor", int.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                View view = (View) param.thisObject;
                checkAndChangeBackgroundColor(view);
                checkAndChangeTextColor(view);
            }
        });
    }
    private void checkAndChangeTextColor(View view) {
        try {
            if (limeOptions.DarkModSync.checked) {
                if (!isDarkModeEnabled(view)) return;
            }
            if (view instanceof TextView) {
                TextView textView = (TextView) view;
                int currentTextColor = textView.getCurrentTextColor();
                String resourceName = getViewResourceName(view); // リソース名を取得
                // voipを含む場合は変更しない
                if (resourceName.contains("voip")) {
                    // XposedBridge.log("Skipping background Color Change for Resource Name: " + resourceName);
                    return;
                }

                if (currentTextColor == Color.parseColor("#111111")) {
                    textView.setTextColor(Color.parseColor("#FFFFFF"));
//XposedBridge.log("Changed Text Color of Resource Name: " + resourceName + " to #FFFFFF");
                } else {
//XposedBridge.log("Text Color of Resource Name: " + resourceName + " is not #111111 (Current: " + (currentTextColor) + ")");
                }}
        } catch (Resources.NotFoundException ignored) {
        }
    }
    private void checkAndChangeBackgroundColor(View view) {
        try {
            if (limeOptions.DarkModSync.checked) {
                if (!isDarkModeEnabled(view)) return;
            }
            String resourceName = getViewResourceName(view);
          //  //XposedBridge.log("Resource Name: " + resourceName);
            XposedBridge.log("Resource Name: " + resourceName);

            // voipを含む場合は変更しない
            if (resourceName.contains("voip")) {
               // XposedBridge.log("Skipping background Color Change for Resource Name: " + resourceName);
                return;
            }

            Drawable background = view.getBackground();

            if (background != null) {
               // //XposedBridge.log("Background Class Name: " + background.getClass().getName());
                if (background instanceof ColorDrawable) {
                    int currentColor = ((ColorDrawable) background).getColor();
                    if (currentColor == Color.parseColor("#111111") ||
                            currentColor == Color.parseColor("#1A1A1A") ||
                            currentColor == Color.parseColor("#FFFFFF")) {
                        ((ColorDrawable) background).setColor(Color.parseColor("#000000"));
    ////XposedBridge.log("Changed Background Color of Resource Name: " + resourceName + " to #000000");
                    } else {
    ////XposedBridge.log("Background Color of Resource Name: " + resourceName + " is not #111111, #1A1A1A, or #FFFFFF (Current: " + convertToHexColor(currentColor) + ")");
                    }
                } else if (background instanceof BitmapDrawable) {
////XposedBridge.log("BitmapDrawable background, cannot change color directly.");
                } else {
////XposedBridge.log("Unknown background type for Resource Name: " + resourceName + ", Class Name: " + background.getClass().getName());
                }
            } else {
              //  //XposedBridge.log("Background is null for Resource Name: " + resourceName);
            }
        } catch (Resources.NotFoundException ignored) {
       //     //XposedBridge.log("Resource name not found for View ID: " + view.getId());
        }
    }

    private String getViewResourceName(View view) {
        int viewId = view.getId();
        if (viewId != View.NO_ID) { // IDが無効でない場合にのみ処理
            try {
                return view.getResources().getResourceEntryName(viewId);
            } catch (Resources.NotFoundException e) {
                //XposedBridge("Resource not found for View ID: " + viewId);
                return "unknown";
            }
        }
        return "no_id";
    }
    private boolean isDarkModeEnabled(View view) {
        Configuration configuration = view.getContext().getResources().getConfiguration();
        int currentNightMode = configuration.uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return currentNightMode == Configuration.UI_MODE_NIGHT_YES;
    }

}






