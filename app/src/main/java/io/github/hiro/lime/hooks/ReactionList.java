package io.github.hiro.lime.hooks;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClassIfExists;
import static io.github.hiro.lime.Main.limeOptions;
import static io.github.hiro.lime.Utils.dpToPx;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.hiro.lime.LimeOptions;

public class ReactionList implements IHook {
    private static final int CLOSE_BUTTON_ID = 0x7f0b087f;
    private final String[] REACTION_TYPES = {"NICE", "LOVE", "FUN", "AMAZING", "SAD", "OMG"};
    private final Map<String, Integer> reactionCounts = new HashMap<>();
    private ImageView reactionGrid;
    @Override
    public void hook(LimeOptions options, XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        if (!limeOptions.ReactionCount.checked) return;
        Context context = (Context) XposedHelpers.callMethod(XposedHelpers.callStaticMethod(
                XposedHelpers.findClass("android.app.ActivityThread", null),
                "currentActivityThread"
        ), "getSystemContext");

        PackageManager pm = context.getPackageManager();
        String versionName = ""; // 初期化
        try {
            versionName = pm.getPackageInfo(loadPackageParam.packageName, 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        if (!isVersionInRange(versionName, "15.4.1", "15.5.0"))return;
        XposedBridge.hookAllMethods(
                loadPackageParam.classLoader.loadClass("Iy.l"),
                "invokeSuspend",
                new XC_MethodHook() {


                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        try {

                            logMethodArguments(param);

                            Object result = param.getResult();
                            if (result instanceof List) {
                                countReactions((List<?>) result);
                                logReactionCounts();
                                updateReactionGrid();
                            }
                        } catch (Exception e) {
                            XposedBridge.log("[ERROR] Reaction counting failed: " + e);
                        }
                    }

                    private void logMethodArguments(MethodHookParam param) {
                        StringBuilder log = new StringBuilder("Method invoked with args: ");
                        for (int i = 0; i < param.args.length; i++) {
                            log.append("\n  Arg[").append(i).append("]: ")
                                    .append(param.args[i] != null ? param.args[i].toString() : "null");
                        }
                        XposedBridge.log(log.toString());
                    }

                    private void countReactions(List<?> entries) {

                        for (String type : REACTION_TYPES) {
                            reactionCounts.put(type, 0);
                        }

                        for (Object entry : entries) {
                            try {
                                String entryStr = entry.toString();
                                XposedBridge.log("Parsing entry: " + entryStr);

                                int typeIndex = entryStr.indexOf("reactionType=");
                                if (typeIndex != -1) {
                                    parseReactionType(entryStr.substring(typeIndex));
                                }
                                else {
                                    parsePredefinedReaction(entryStr);
                                }
                            } catch (Exception e) {
                                XposedBridge.log("[WARN] Failed to parse entry: " + e);
                            }
                        }
                    }

                    private void parseReactionType(String typeStr) {
                        String[] parts = typeStr.split("[=,)]");
                        if (parts.length >= 2) {
                            String type = parts[1].trim();
                            incrementCount(type);
                        }
                    }

                    private void parsePredefinedReaction(String str) {
                        Pattern pattern = Pattern.compile("PredefinedReaction\\(reactionType=([A-Z]+)");
                        Matcher matcher = pattern.matcher(str);
                        if (matcher.find()) {
                            incrementCount(matcher.group(1));
                        }
                    }

                    private void incrementCount(String type) {
                        if (Arrays.asList(REACTION_TYPES).contains(type)) {
                            reactionCounts.put(type, reactionCounts.getOrDefault(type, 0) + 1);
                        }
                    }

                    private void logReactionCounts() {
                        StringBuilder log = new StringBuilder("[RESULT] Reaction Counts:\n");
                        reactionCounts.forEach((type, count) -> {
                            if (count > 0) log.append("  ").append(type).append(": ").append(count).append("\n");
                        });
                        XposedBridge.log(log.toString());
                    }
                }

        );


        findAndHookMethod(
                ViewGroup.class,
                "onViewAdded",
                View.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        try {
                            View addedView = (View) param.args[0];

                            if (addedView.getId() == CLOSE_BUTTON_ID) {
                                Context context = addedView.getContext();
                                int marginLeftPx = dpToPx(16, context);

                                if (addedView.getParent() instanceof ViewGroup) {
                                    ViewGroup parent = (ViewGroup) addedView.getParent();

                                    createReactionIcons(context, parent, marginLeftPx);
                                }
                            }
                        } catch (Throwable t) {
                            XposedBridge.log("[ERROR] " + Log.getStackTraceString(t));
                        }
                    }
                }
        );
    }
    private void updateReactionGrid() {
        if (reactionGrid != null) {
            reactionGrid.post(() -> {
                ViewGroup parent = (ViewGroup) reactionGrid.getParent();
                if (parent != null) {
                    parent.removeView(reactionGrid);
                }

                createReactionIcons(
                        reactionGrid.getContext(),
                        parent,
                        dpToPx(16, reactionGrid.getContext())
                );
            });
        }
    }

    private static final Map<String, String> REACTION_IMAGE_NAMES = new HashMap<String, String>() {{
        put("NICE", "chat_ui_nice_reaction_square");
        put("LOVE", "chat_ui_love_reaction_square");
        put("FUN", "chat_ui_fun_reaction_square");
        put("AMAZING", "chat_ui_envy_reaction_square");
        put("SAD", "chat_ui_sad_reaction_square");
        put("OMG", "chat_ui_omg_reaction_square");
    }};
    private void createReactionIcons(Context context, ViewGroup parent, int marginLeftPx) {
        try {
            Context moduleContext = context.createPackageContext("io.github.hiro.lime", 0);
            GridLayout grid = new GridLayout(context);
            grid.setColumnCount(3);
            grid.setRowCount(2);
            grid.setPadding(dpToPx(4, context), 0, dpToPx(4, context), 0);

            for (String type : REACTION_TYPES) {
                int count = reactionCounts.getOrDefault(type, 0);
                if (count > 0) {
                    String resourceName = REACTION_IMAGE_NAMES.get(type);
                    int resourceId = moduleContext.getResources().getIdentifier(
                            resourceName,
                            "raw",
                            "io.github.hiro.lime"
                    );

                    if (resourceId != 0) {

                        int imageSize = dpToPx(20, context);

                        InputStream is = moduleContext.getResources().openRawResource(resourceId);
                        Bitmap rawBitmap = BitmapFactory.decodeStream(is);
                        Bitmap scaledBitmap = Bitmap.createScaledBitmap(
                                rawBitmap,
                                imageSize,
                                imageSize,
                                true
                        );
                        is.close();
                        LinearLayout container = new LinearLayout(context);
                        container.setOrientation(LinearLayout.HORIZONTAL);
                        container.setGravity(Gravity.CENTER_VERTICAL);

                        ImageView iv = new ImageView(context);
                        iv.setImageBitmap(scaledBitmap);
                        LinearLayout.LayoutParams ivParams = new LinearLayout.LayoutParams(
                                imageSize,
                                imageSize
                        );
                        ivParams.setMargins(0, 0, dpToPx(4, context), 0);
                        iv.setLayoutParams(ivParams);

                        TextView tv = createCountTextView(context, count);

                        container.addView(iv);
                        container.addView(tv);

                        GridLayout.LayoutParams gridParams = new GridLayout.LayoutParams();
                        gridParams.setMargins(
                                dpToPx(2, context),
                                dpToPx(2, context),
                                dpToPx(2, context),
                                dpToPx(2, context)
                        );
                        container.setLayoutParams(gridParams);

                        grid.addView(container);
                    }
                }
            }

            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(marginLeftPx, 0, 0, 0);
            params.gravity = Gravity.CENTER_VERTICAL | Gravity.END;
            parent.addView(grid, params);

        } catch (Exception e) {
            XposedBridge.log("[ERROR] Image processing failed: " + e.getMessage());
        }
    }

    private TextView createCountTextView(Context context, int count) {
        TextView tv = new TextView(context);
        tv.setText(String.valueOf(count));
        tv.setTextColor(Color.parseColor("#FF4081"));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tv.setTypeface(Typeface.DEFAULT_BOLD);

        tv.setShadowLayer(
                dpToPx(1, context),
                dpToPx((int) 0.5f, context),
                dpToPx((int) 0.5f, context),
                Color.BLACK
        );

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.gravity = Gravity.CENTER_VERTICAL;

        tv.setLayoutParams(params);

        return tv;
    }

    private static boolean isVersionInRange(String versionName, String minVersion, String maxVersion) {
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