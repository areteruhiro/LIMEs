package io.github.hiro.lime.hooks;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static io.github.hiro.lime.Main.limeOptions;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.hiro.lime.LimeOptions;

public class WhiteToDark implements IHook {
    @Override
    public void hook(LimeOptions limeOptions, XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        if (!limeOptions.WhiteToDark.checked) return;
        final int REPLACE_COLOR = Color.parseColor("#000000"); // 灰色
        Context context = (Context) XposedHelpers.callMethod(XposedHelpers.callStaticMethod(
                XposedHelpers.findClass("android.app.ActivityThread", null),
                "currentActivityThread"
        ), "getSystemContext");

        PackageManager pm = context.getPackageManager();
        String versionName = "";
        try {
            versionName = pm.getPackageInfo(loadPackageParam.packageName, 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        if (isVersionInRange(versionName, "15.5.1", "15.6.0")){


        XposedHelpers.findAndHookMethod(
                "Zv0.e",
                loadPackageParam.classLoader,
                "p",
                XposedHelpers.findClass("Xv0.m$b", loadPackageParam.classLoader),  // 引数の enum 型
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        Class<?> modeClass = XposedHelpers.findClass("Xv0.m$b", loadPackageParam.classLoader);

                        //XposedBridge.log("[ThemeHook] before: original param = " + param.args[0]);

                        Object darkEnum = Enum.valueOf((Class<Enum>) modeClass, "DARK");
                        param.args[0] = darkEnum;

                        XposedBridge.log("[ThemeHook] forced param = " + param.args[0]);
                    }

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        XposedBridge.log("[ThemeHook] method Zv0.e.p() was called successfully");
                    }
                }
        );
        XposedBridge.hookAllMethods(
                loadPackageParam.classLoader.loadClass("le1.h"), // 対象のクラスを指定
                "c",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {

                        Class<?> modeClass = XposedHelpers.findClass("Xv0.m$b", loadPackageParam.classLoader);

                       // XposedBridge.log("[ThemeHook] before: original param = " + param.args[0]);

                        Object darkEnum = Enum.valueOf((Class<Enum>) modeClass, "DARK");
                        param.args[0] = darkEnum;

                        //XposedBridge.log("[ThemeHook] forced param = " + param.args[0]);
                    }
                }
        );



        XposedHelpers.findAndHookMethod("Zv0.e", loadPackageParam.classLoader, "y", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Class<?> modeClass = XposedHelpers.findClass("Xv0.m$b", loadPackageParam.classLoader);
                Object darkEnum = Enum.valueOf((Class<Enum>) modeClass, "DARK");
                param.setResult(darkEnum);
            }
        });
//        XposedHelpers.findAndHookMethod(
//                "Xv0.m$a", loadPackageParam.classLoader,   // フックするクラスとそのクラスローダ
//                "a", Context.class,   // フックするメソッドとその引数
//                new XC_MethodHook() {   // メソッドフックの定義
//                    @Override
//                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//                        Object result = param.getResult();
//                       // XposedBridge.log("Xv0.m 実装クラス: " + result.getClass().getName());
//                    }
//                }
//        );


        }

        XposedHelpers.findAndHookMethod(
                "android.view.View",
                loadPackageParam.classLoader,
                "setPressed",
                boolean.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        View view = (View) param.thisObject;
                        boolean isPressed = (boolean) param.args[0];

                        if (isPressed) {
                            Drawable background = view.getBackground();
                            if (background != null) {
                                handleBackground(background, view);
                            }
                            Drawable foreground = null;
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                foreground = view.getForeground();
                            }
                            if (foreground != null) {
                                XposedBridge.log("[Foreground Type] " + foreground.getClass().getSimpleName());
                            }
                        }
                    }

                    private void handleBackground(Drawable background, View view) {
                        if (background instanceof StateListDrawable) {
                            StateListDrawable sl = (StateListDrawable) background;
                            for (int i = 0; i < sl.getStateCount(); i++) {
                                Drawable stateDrawable = sl.getStateDrawable(i);
                                if (stateDrawable instanceof ColorDrawable) {
                                    int stateColor = ((ColorDrawable) stateDrawable).getColor();
                                    ((ColorDrawable) stateDrawable).setColor(REPLACE_COLOR);
                                }
                            }
                        }
                    }
                }
        );


        findAndHookMethod(
                "android.app.Activity",
                loadPackageParam.classLoader,
                "onResume",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        Activity activity = (Activity) param.thisObject;
                        View rootView = activity.getWindow().getDecorView(); // DecorViewを取得
                        View view = rootView;

                        if (limeOptions.DarkModSync.checked) {
                            if (!isDarkModeEnabled(view)) return;
                        }
                        traverseViewsAndLog((ViewGroup) rootView, activity);
                    }
                }
        );

        findAndHookMethod("android.view.View", loadPackageParam.classLoader, "onAttachedToWindow", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {

                applyDarkThemeRecursive((View) param.thisObject);
            }
        });

        findAndHookMethod("android.view.View", loadPackageParam.classLoader, "setBackground", Drawable.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                View view = (View) param.thisObject;
                checkAndChangeBackgroundColor(view);
                //checkAndChangeTextColor(view);
            }
        });

        findAndHookMethod("android.view.View", loadPackageParam.classLoader, "setBackgroundResource", int.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                View view = (View) param.thisObject;
                checkAndChangeBackgroundColor(view);
               // checkAndChangeTextColor(view);
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
                                        LayerDrawable layerDrawable;
                                        if (limeOptions.removeIconLabels.checked) {
                                            layerDrawable = new LayerDrawable(new Drawable[]{
                                                    createTransparentRect(height * 0.1f),
                                                    createBlackRect(height * 0.9f)
                                            });
                                        } else {
                                            // 元のグラデーション
                                            layerDrawable = new LayerDrawable(new Drawable[]{
                                                    createTransparentRect(height * 0.3f),
                                                    createBlackRect(height * 0.8f)
                                            });
                                            layerDrawable.setLayerInset(1, 0, (int) (height * 0.2f), 0, 0);
                                        }

                                        child.setBackground(layerDrawable);
                                        child.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                                    }
                                };
                        child.getViewTreeObserver().addOnGlobalLayoutListener(bgListener);
                        if (!limeOptions.removeIconLabels.checked) {
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
                            child.getViewTreeObserver().addOnGlobalLayoutListener(heightListener);

                            if (child instanceof TextView && !resName.endsWith("_spacer")) {
                                ((TextView) child).setTextColor(Color.BLACK);
                            }
                        }else{

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
                            child.getViewTreeObserver().addOnGlobalLayoutListener(heightListener);

                            if (child instanceof TextView && !resName.endsWith("_spacer")) {
                                ((TextView) child).setTextColor(Color.BLACK);
                            }
                        }
                    }
                } catch (Resources.NotFoundException ignored) {
                }
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




    private void applyDarkThemeRecursive(View view) {
        String logPrefix = "[DarkTheme]";
        String resName = getViewResourceName(view);

        if (limeOptions.DarkModSync.checked) {
            if (!isDarkModeEnabled(view)) return;
        }

        String contentDescription = String.valueOf(view.getContentDescription());
        if ("no_id".equals(resName) && "null".equals(contentDescription)) {
//            XposedBridge.log(String.format("%s Skipping no_id|null view: %s",
//                    logPrefix,
//                    view.getClass().getSimpleName()));
            return;
        }
        if (view instanceof TextView) {
            TextView tv = (TextView) view;
            String textContent = tv.getText().toString();
//
//            XposedBridge.log(String.format("%s [TextContent] resName=%s, text=\"%s\"",
//                    logPrefix,
//                    resName,
//                    textContent));
        }

        if (resName.contains("floating_toolbar_menu_item_text")) {
            if (view instanceof TextView) {
                ((TextView) view).setTextColor(Color.WHITE);
            }
            return;
        }
      //  XposedBridge.log(resName);

        if (resName.contains("common_popup_item_title") ||
                resName.contains("setting_icon") ||
                resName.contains("setting_title") ||
                resName.contains("setting_description") ||
                resName.contains("status_message") ||
                resName.contains("main_tab_search_bar_hint_text") ||
                resName.contains("name")

        ) {
            XposedBridge.log(String.format("%s [WHITELIST] resName=%s, ViewType=%s",
                    logPrefix,
                    resName,
                    view.getClass().getName()));

            if (view instanceof TextView) {
                TextView tv = (TextView) view;
                int currentColor = tv.getCurrentTextColor();
                XposedBridge.log(String.format("%s [Before] Color=#%s",
                        logPrefix,
                        Integer.toHexString(currentColor)));
                tv.setTextColor(Color.WHITE);
                XposedBridge.log(String.format("%s [After] Color=#%s",
                        logPrefix,
                        Integer.toHexString(tv.getCurrentTextColor())));
            }
            else {
        //  view.setBackgroundColor(Color.WHITE);
            }
            return;
        }
//        if (
//             resName.contains("chat_ui_message_edit") ||
//                resName.contains("chat_ui_message_edit_text_background") ||
//                resName.contains("chat_ui_input_keyboard_switch_layout") ||
//                resName.contains("sticker_sticon_input_selection_view_root") ||
//                resName.contains("chathistory_stt_input_viewstub") ||
//                        resName.contains("chathistory_message_suggestion_input_viewstub") ||
//                        resName.contains("chat_ui_edge_to_edge_main_content_area") ||
//                     resName.contains("chat_ui_main_content_area") ||
//                     resName.contains("chat_ui_text_keyboard_button") ||
//
//                     resName.contains("chat_ui_main_content_container") ||
//                     resName.contains("chat_ui_voice_input_card_alert_viewstub") ||
//                     resName.contains("chat_ui_input_refine_message_button") ||
//
//                resName.contains("chat_ui_row_editmode_clicktarget_overlay")){
//            view.setBackgroundColor(Color.BLACK);
//
//        }
//        if ( resName.contains("edit_message_view") ){
//            view.setBackgroundColor(Color.WHITE);
//
//        }

        if (
            resName.contains("bnb_home_v2") ||
                    resName.contains("bnb_chat") ||
                    resName.contains("bnb_news") ||
                    resName.contains("bnb_wallet") ||
                    resName.contains("bnb_portal") ||
                    resName.contains("bnb_button_text") ) {

            if (view instanceof TextView) {
                TextView tv = (TextView) view;
                tv.setTextColor(Color.WHITE);
            }

        }
        // タイトル系のテキストを処理
        if (resName.contains("title")) {
            if (view instanceof TextView) {
                TextView tv = (TextView) view;
                String textContent = tv.getText().toString();

                // 特定のテキスト内容に一致する場合のみ色変更
                if (textContent.contains("プロフィール表示を設定") ||
                        textContent.contains("お気に入りに追加") ||
                        textContent.contains("Set profile to display") ||
                        textContent.contains("Add to favorites") ||
                        textContent.contains("設定個人檔案的顯示") ||
                        textContent.contains("加到我的最愛")) {

                    tv.setTextColor(Color.WHITE);
                }
            }
            return;
        }

        // ViewGroupの子要素を再帰処理
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                View child = viewGroup.getChildAt(i);
                applyDarkThemeRecursive(child);
            }
        }

        // ポップアップ背景の処理
        String parentHierarchy = getParentHierarchy(view);
        if (parentHierarchy.contains("PopupBackgroundView")) {
            GradientDrawable roundedBg = new GradientDrawable();
            roundedBg.setShape(GradientDrawable.RECTANGLE);
            roundedBg.setCornerRadius(20f);
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
                        ((TextView) child).setTextColor(Color.WHITE);
                    }
                }
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
                    textView.setTextColor(Color.parseColor("#000000"));
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
            //  XposedBridge.log("Resource Name: " + resourceName);

            // floating_toolbarの強制処理（最優先）
            if (resourceName.contains("floating_toolbar")) {
                if (view.getBackground() instanceof ColorDrawable) {
                    ((ColorDrawable) view.getBackground()).setColor(Color.BLACK);
XposedBridge.log("[FloatingToolbar] Forced Black Background: " + resourceName);
                } else if (view.getBackground() != null) {
                    view.setBackgroundColor(Color.BLACK);
XposedBridge.log("[FloatingToolbar] Override Background: " + resourceName);
                }
                return;
            }

            if (resourceName.contains("floating_toolbar")) {
                if (view.getBackground() instanceof ColorDrawable) {
                    ((ColorDrawable) view.getBackground()).setColor(Color.BLACK);
                    XposedBridge.log("[FloatingToolbar] Forced Black Background: " + resourceName);
                } else if (view.getBackground() != null) {
                    view.setBackgroundColor(Color.BLACK);
                    XposedBridge.log("[FloatingToolbar] Override Background: " + resourceName);
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
                       // XposedBridge.log("Changed Background Color of Resource Name: " + resourceName + " to #000000");
                    } else {
                     //   XposedBridge.log("Background Color of Resource Name: " + resourceName );
                    }
                } else if (background instanceof BitmapDrawable) {
//XposedBridge.log("BitmapDrawable background, cannot change color directly.");
                } else {
//XposedBridge.log("Unknown background type for Resource Name: " + resourceName + ", Class Name: " + background.getClass().getName());
                }
            } else {
         //    XposedBridge.log("Background is null for Resource Name: " + resourceName);
            }
        } catch (Resources.NotFoundException ignored) {
       //   XposedBridge.log("Resource name not found for View ID: " + view.getId());
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

    static boolean isVersionInRange(String versionName, String minVersion, String maxVersion) {
        try {
            int[] currentVersion = parseVersion(versionName);
            int[] minVersionArray = parseVersion(minVersion);
            int[] maxVersionArray = parseVersion(maxVersion);

            boolean isGreaterOrEqualMin = compareVersions(currentVersion, minVersionArray) >= 0;

            boolean isLessThanMax = compareVersions(currentVersion, maxVersionArray) < 0;
            return isGreaterOrEqualMin && isLessThanMax;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static int[] parseVersion(String version) {
        String[] parts = version.split("\\.");
        int[] versionArray = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            versionArray[i] = Integer.parseInt(parts[i]);
        }
        return versionArray;
    }


    private static int compareVersions(int[] version1, int[] version2) {
        for (int i = 0; i < Math.min(version1.length, version2.length); i++) {
            if (version1[i] < version2[i]) return -1;
            if (version1[i] > version2[i]) return 1;
        }
        return 0;
    }
}
