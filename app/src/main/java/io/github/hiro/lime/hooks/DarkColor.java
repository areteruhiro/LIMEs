package io.github.hiro.lime.hooks;

import static io.github.hiro.lime.Main.limeOptions;

import android.app.Activity;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.hiro.lime.LimeOptions;

public class DarkColor implements IHook {
    @Override
    public void hook(LimeOptions limeOptions, XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        if (!limeOptions.DarkColor.checked) return;


            XposedHelpers.findAndHookMethod(
                    "android.app.Activity",
                    loadPackageParam.classLoader,
                    "onResume",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            Activity activity = (Activity) param.thisObject;
                            View rootView = activity.getWindow().getDecorView();
                            traverseViewsAndLog((ViewGroup) rootView, activity);
                        }
                    }
            );
//            XposedHelpers.findAndHookMethod(
//                    "jp.naver.line.android.activity.main.MainActivity",
//                    loadPackageParam.classLoader,
//                    "onResume",
//                    new XC_MethodHook() {
//                        @Override
//                        protected void afterHookedMethod(MethodHookParam param) {
//                            Activity activity = (Activity) param.thisObject;
//                            View rootView = activity.getWindow().getDecorView();
//                            if (limeOptions.DarkModSync.checked) {
//                                if (!isDarkModeEnabled(rootView)) return;
//                            }
//                            traverseViewsAndLog((ViewGroup) rootView, activity);
//
//                        }
//                    }
//            );


        XposedHelpers.findAndHookMethod("android.view.View", loadPackageParam.classLoader, "onAttachedToWindow", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {

                applyDarkThemeRecursive((View) param.thisObject);
            }
        });

        XposedHelpers.findAndHookMethod("android.view.View", loadPackageParam.classLoader, "setBackground", Drawable.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                View view = (View) param.thisObject;
                checkAndChangeBackgroundColor(view);
            }
        });

        XposedHelpers.findAndHookMethod("android.view.View", loadPackageParam.classLoader, "setBackgroundResource", int.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                View view = (View) param.thisObject;
                checkAndChangeBackgroundColor(view);
            }
        });

        XposedHelpers.findAndHookMethod("android.view.View", loadPackageParam.classLoader, "setBackgroundColor", int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                int color = (int) param.args[0];
                if (isTargetColor(color, "#111111")) {
                    param.args[0] = Color.parseColor("#000000");
                }
            }
        });
    }
    private boolean isTargetColor(int color, String target) {
        int targetColor = Color.parseColor(target);
        return (color & 0x00FFFFFF) == (targetColor & 0x00FFFFFF);
    }
    private void traverseViewsAndLog(ViewGroup viewGroup, Activity activity) {
        final Set<String> gradientTargets = new HashSet<>(Arrays.asList(
                "bnb_chat",
                "bnb_home_v2"
        ));

        final Set<String> otherTargets = new HashSet<>(Arrays.asList(
                "bnb_wallet", "bnb_news", "bnb_call", "bnb_timeline",
                "bnb_wallet_spacer", "bnb_news_spacer", "bnb_call_spacer",
                "bnb_timeline_spacer", "bnb_chat_spacer", "main_tab_container",
                "bnb_background_image", "bnb_portal_spacer"
        ));

        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            View child = viewGroup.getChildAt(i);
            int resId = child.getId();

            if (resId != View.NO_ID) {
                try {
                    String resName = activity.getResources().getResourceEntryName(resId);
                    if (gradientTargets.contains(resName) || otherTargets.contains(resName)) {
                        ViewTreeObserver.OnGlobalLayoutListener bgListener =
                                new ViewTreeObserver.OnGlobalLayoutListener() {
                                    @Override
                                    public void onGlobalLayout() {
                                        if (!child.getViewTreeObserver().isAlive()) return;
                                        int height = child.getHeight();
                                        if (height <= 0) return;

                                        LayerDrawable layerDrawable = new LayerDrawable(new Drawable[]{
                                                createTransparentRect(height * 0.2f),
                                                createBlackRect(height * 0.8f)
                                        });
                                        layerDrawable.setLayerInset(1, 0, (int)(height * 0.2f), 0, 0);
                                        child.setBackground(layerDrawable);
                                        child.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                                    }
                                };
                        ViewTreeObserver.OnGlobalLayoutListener heightListener =
                                new ViewTreeObserver.OnGlobalLayoutListener() {
                                    @Override
                                    public void onGlobalLayout() {
                                        if (!child.getViewTreeObserver().isAlive()) return;

                                        if (child.getVisibility() == View.VISIBLE) {
                                            ViewGroup.LayoutParams params = child.getLayoutParams();
                                            params.height = (int) TypedValue.applyDimension(
                                                    TypedValue.COMPLEX_UNIT_DIP,
                                                    80,
                                                    activity.getResources().getDisplayMetrics()
                                            );
                                            child.setLayoutParams(params);
                                        }
                                        child.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                                    }
                                };
                        child.getViewTreeObserver().addOnGlobalLayoutListener(bgListener);
                        child.getViewTreeObserver().addOnGlobalLayoutListener(heightListener);

                        if (child instanceof TextView && !resName.endsWith("_spacer")) {
                            ((TextView) child).setTextColor(Color.BLACK);
                        }
                    }
                } catch (Resources.NotFoundException ignored) {}
            }

            if (child instanceof ViewGroup) {
                traverseViewsAndLog((ViewGroup) child, activity);
            }
        }
    }

    private Drawable createTransparentRect(float height) {
        GradientDrawable transparent = new GradientDrawable();
        transparent.setShape(GradientDrawable.RECTANGLE);
        transparent.setColor(Color.TRANSPARENT);
        transparent.setSize(ViewGroup.LayoutParams.MATCH_PARENT, (int)height);
        return transparent;
    }

    private Drawable createBlackRect(float height) {
        GradientDrawable black = new GradientDrawable();
        black.setShape(GradientDrawable.RECTANGLE);
        black.setColor(Color.BLACK);
        black.setSize(ViewGroup.LayoutParams.MATCH_PARENT, (int)height);
        return black;
    }


    private void traverseViewsAndLogG(ViewGroup viewGroup, Activity activity) {
        Random random = new Random(); // ランダムカラー生成用

        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            View child = viewGroup.getChildAt(i);

            int resId = child.getId();
            if (resId != View.NO_ID) {
                try {

                    String resName = activity.getResources().getResourceEntryName(resId);
                    String resType = activity.getResources().getResourceTypeName(resId);
                    String fullResName = activity.getPackageName() + ":" + resType + "/" + resName;
                    int randomColor = Color.argb(
                            255,
                            random.nextInt(256),
                            random.nextInt(256),
                            random.nextInt(256)
                    );

                    XposedBridge.log(String.format(
                            "Changed Resource: %s → #%08X",
                            fullResName,
                            randomColor
                    ));
                    child.setBackgroundColor(randomColor);
                    if (child instanceof TextView) {
                        ((TextView) child).setTextColor(randomColor);
                    }

                } catch (Resources.NotFoundException e) {
                    XposedBridge.log("Resource not found for ID: 0x" + Integer.toHexString(resId));
                }
            }

            if (child instanceof ViewGroup) {
                traverseViewsAndLogG((ViewGroup) child, activity);
            }
        }
    }

    private void applyDarkThemeRecursive(View view) {
        String logPrefix = "[DarkTheme]";
        String resName = getViewResourceName(view);
        String viewInfo = String.format("%s|%s|%s",
                view.getClass().getSimpleName(),
                resName,
                view.getContentDescription()
        );
        if (limeOptions.DarkModSync.checked) {
            if (!isDarkModeEnabled(view)) return;
        }
        String contentDescription = String.valueOf(view.getContentDescription()); // null安全な変換

        if ("no_id".equals(resName) && "null".contentEquals(contentDescription)) {
//                XposedBridge.log(String.format("%s Skipping no_id|null view: %s",
//                        logPrefix,
//                        view.getClass().getSimpleName()));
            return;
        }
        if (resName.contains("floating_toolbar_menu_item_text")) {
            if (view instanceof TextView) {
                TextView tv = (TextView) view;
                tv.setTextColor(Color.WHITE);
//                    XposedBridge.log(String.format("%s FloatingToolbar text white: %s",
//                            logPrefix, viewInfo));
            }
            return;
        }
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                View child = viewGroup.getChildAt(i);
                applyDarkThemeRecursive(child);
            }
        }

        String parentHierarchy = getParentHierarchy(view);
        if (parentHierarchy.contains("PopupBackgroundView")) {
            GradientDrawable roundedBg = new GradientDrawable();
            roundedBg.setShape(GradientDrawable.RECTANGLE);
            roundedBg.setCornerRadius(20f); // 角丸の半径（単位：ピクセル）
            roundedBg.setColor(Color.BLACK);
            view.setBackground(roundedBg);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                view.setClipToOutline(true);
            }
            if (view instanceof ViewGroup) {
                ViewGroup container = (ViewGroup) view;
                for (int i = 0; i < container.getChildCount(); i++) {
                    View child = container.getChildAt(i);
                    if (child instanceof TextView) {
                        TextView tv = (TextView) child;
                        tv.setTextColor(Color.WHITE);
                    }
                }
            }
            if (view instanceof TextView) {
                ((TextView) view).setTextColor(Color.WHITE);
            }
            if (view instanceof ImageView) {
                ((ImageView) view).setColorFilter(Color.WHITE);
            }

        }


    }
    private String getParentHierarchy(View view) {
        StringBuilder sb = new StringBuilder();
        View current = view;
        while (current.getParent() instanceof View) {
            current = (View) current.getParent();
            sb.insert(0, current.getClass().getSimpleName() + " > ");
        }
        return sb.toString();
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
//            XposedBridge.log("Resource Name: " + resourceName);

            // floating_toolbarの強制処理（最優先）
            if (resourceName.contains("floating_toolbar")) {
                if (view.getBackground() instanceof ColorDrawable) {
                    ((ColorDrawable) view.getBackground()).setColor(Color.BLACK);
//                    XposedBridge.log("[FloatingToolbar] Forced Black Background: " + resourceName);
                } else if (view.getBackground() != null) {
                    view.setBackgroundColor(Color.BLACK);
//                    XposedBridge.log("[FloatingToolbar] Override Background: " + resourceName);
                }
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
        try {
            int viewId = view.getId();
            if (viewId == View.NO_ID) return "no_id";

            String resName;
            try {
                resName = view.getResources().getResourceEntryName(viewId);
            } catch (Resources.NotFoundException e) {
                String pkgName = view.getResources().getResourcePackageName(viewId);
                String typeName = view.getResources().getResourceTypeName(viewId);
                String entryName = view.getResources().getResourceEntryName(viewId);
                resName = pkgName + ":" + typeName + "/" + entryName;
            }
            return resName;
        } catch (Exception ignored) {
            return "unknown";
        }
    }
    private boolean isDarkModeEnabled(View view) {
        Configuration configuration = view.getContext().getResources().getConfiguration();
        int currentNightMode = configuration.uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return currentNightMode == Configuration.UI_MODE_NIGHT_YES;
    }

}