package io.github.hiro.lime.hooks;


import static io.github.hiro.lime.Main.limeOptions;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AndroidAppHelper;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.util.AttributeSet;
import android.util.Base64;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.util.Supplier;
import androidx.documentfile.provider.DocumentFile;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.hiro.lime.BuildConfig;
import io.github.hiro.lime.LimeOptions;
import io.github.hiro.lime.R;
import io.github.hiro.lime.Utils;

public class EmbedOptions implements IHook {

    private DocumentPreferences sDocPrefs = null;

    private boolean sOptionsLoadedFromPrefs = false;
    private  Activity activity2 = null;
    @Override
    public void hook(LimeOptions limeOptions, XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        final Class<?> headerButtonClass = XposedHelpers.findClass(
                "jp.naver.line.android.common.view.header.HeaderButton",
                loadPackageParam.classLoader
        );

        XposedHelpers.findAndHookConstructor(
                headerButtonClass,
                Context.class,
                AttributeSet.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        final View headerButton = (View) param.thisObject;
                        final Context ctx = headerButton.getContext();

                        final Context moduleContext = AndroidAppHelper.currentApplication()
                                .createPackageContext(Constants.MODULE_NAME, Context.CONTEXT_IGNORE_SECURITY);

                        final String backupUri = loadBackupUri(ctx);
                        if (backupUri == null) {
                            XposedBridge.log("Lime: Settings URI not configured");
                            return;
                        }
                        final Uri treeUri = Uri.parse(backupUri);

                        headerButton.post(() -> {
                            try {
                                int targetId = ctx.getResources().getIdentifier(
                                        "notification_header_button",
                                        "id",
                                        Constants.PACKAGE_NAME
                                );
                                if (targetId == 0) return;

                                if (headerButton.getId() != targetId) return;

                                ViewParent vp = headerButton.getParent();
                                if (!(vp instanceof ViewGroup)) return;
                                ViewGroup parent = (ViewGroup) vp;
                                final String PARENT_TAG = "lime_settings_header_parent";
                                Object parentTag = parent.getTag();
                                if (PARENT_TAG.equals(parentTag)) {
                                    return;
                                }
                                final String CHILD_TAG = "lime_settings_header_button";
                                for (int i = 0; i < parent.getChildCount(); i++) {
                                    View child = parent.getChildAt(i);
                                    Object tag = child.getTag();
                                    if (CHILD_TAG.equals(tag)) {
                                        parent.setTag(PARENT_TAG);
                                        return;
                                    }
                                }

                                Map<String, String> settings = readSettingsFromFile(ctx);
                                float readCheckerSizeDp = 60f;
                                try {
                                    readCheckerSizeDp = Float.parseFloat(
                                            settings.getOrDefault("header_setting_size", "60")
                                    );
                                } catch (Throwable ignore) {
                                }


                                ImageView imageButton = new ImageView(ctx);
                                imageButton.setTag(CHILD_TAG);
                                boolean dark = isDarkMode(ctx);
//                                XposedBridge.log("LIMEs[UI]: isDarkMode = " + dark);
                                String themeSuffix = dark ? "light" : "dark";
                                String imageName = "header_setting_" + themeSuffix + ".png";
//                                XposedBridge.log("LIMEs[UI]: imageName = " + imageName);
                                Drawable drawable = loadImageFromUri(ctx, imageName);

                                if (drawable == null) {
                                    copyImageToUri(ctx, moduleContext, imageName);
                                    drawable = loadImageFromUri(ctx, imageName);
                                }

                                if (drawable == null) {
                                    int resId = moduleContext.getResources().getIdentifier(
                                            imageName.replace(".png", ""), "drawable", Constants.MODULE_NAME);
                                    if (resId != 0) {
                                        drawable = moduleContext.getResources().getDrawable(resId);
                                    }
                                }

                                if (drawable != null) {
                                    int sizeInPx = dpToPx(moduleContext, readCheckerSizeDp);
                                    drawable = scaleDrawable(drawable, sizeInPx, sizeInPx);
                                    imageButton.setImageDrawable(drawable);
                                }

                                ViewGroup.LayoutParams lp = headerButton.getLayoutParams();
                                ViewGroup.LayoutParams newLp;
                                if (lp != null) {
                                    newLp = new ViewGroup.LayoutParams(lp);
                                } else {
                                    newLp = new ViewGroup.LayoutParams(
                                            ViewGroup.LayoutParams.WRAP_CONTENT,
                                            ViewGroup.LayoutParams.WRAP_CONTENT
                                    );
                                }
                                imageButton.setLayoutParams(newLp);

                                View.OnClickListener listener = v -> {
                                    try {
                                        Context contextV = v.getContext();

                                        if (backupUri == null) {
                                            Toast.makeText(
                                                    contextV,
                                                    "Please select settings folder first",
                                                    Toast.LENGTH_LONG
                                            ).show();
                                            return;
                                        }
                                        if (sDocPrefs == null) {
                                            sDocPrefs = new DocumentPreferences(contextV, treeUri);
                                        }

                                        if (!sOptionsLoadedFromPrefs) {
                                            for (LimeOptions.Option option : limeOptions.options) {
                                                option.checked = Boolean.parseBoolean(
                                                        sDocPrefs.getSetting(
                                                                option.name,
                                                                String.valueOf(option.checked)
                                                        )
                                                );
                                            }
                                            sOptionsLoadedFromPrefs = true;
                                        }
                                        Utils.addModuleAssetPath(contextV);

                                        Activity activity = (Activity) contextV;
                                        activity2 = (Activity) contextV;
                                        ViewGroup viewGroup =
                                                activity.findViewById(android.R.id.content);

                                        FrameLayout rootLayout = new FrameLayout(contextV);
                                        rootLayout.setLayoutParams(
                                                new ViewGroup.LayoutParams(
                                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                                        ViewGroup.LayoutParams.MATCH_PARENT
                                                )
                                        );

                                        ScrollView categoryLayout =
                                                createCategoryListLayout(
                                                        contextV,
                                                        Arrays.asList(limeOptions.options),
                                                        sDocPrefs,
                                                        moduleContext,
                                                        loadPackageParam,
                                                        true
                                                );

                                        showView(rootLayout, categoryLayout);
                                        viewGroup.addView(rootLayout);

                                    } catch (Throwable e) {
                                        new Handler(Looper.getMainLooper()).post(() -> {
                                            try {
                                                if (moduleContext != null) {
                                                    Toast.makeText(
                                                            moduleContext,
                                                            moduleContext.getString(
                                                                    R.string.Error_Create_setting_Button
                                                            ) + moduleContext.getString(
                                                                    R.string.save_failed
                                                            ),
                                                            Toast.LENGTH_LONG
                                                    ).show();
                                                }
                                            } catch (Throwable ignore) {
                                            }
                                        });
                                    }
                                };
                                imageButton.setOnClickListener(listener);
                                final String PREF_IMG = "HeaderSetting_button_position";
                                final String KEY_IMG_X = "img_x";
                                final String KEY_IMG_Y = "img_y";
                                SharedPreferences spImg =
                                        ctx.getSharedPreferences(PREF_IMG, Context.MODE_PRIVATE);

                                float savedImgX = spImg.getFloat(KEY_IMG_X, Float.NaN);
                                float savedImgY = spImg.getFloat(KEY_IMG_Y, Float.NaN);

                                if (!Float.isNaN(savedImgX) && !Float.isNaN(savedImgY)) {
                                    imageButton.setX(savedImgX);
                                    imageButton.setY(savedImgY);
                                }

                                imageButton.setOnTouchListener(new View.OnTouchListener() {
                                    private final Handler h = new Handler(Looper.getMainLooper());
                                    private boolean dragging = false;
                                    private float offsetX, offsetY;
                                    private final Runnable startDrag = () -> dragging = true;

                                    private float clamp(float value, float min, float max) {
                                        return Math.max(min, Math.min(value, max));
                                    }

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
                                                    float newX = e.getRawX() - offsetX;
                                                    float newY = e.getRawY() - offsetY;

                                                    int pw = parent.getWidth();
                                                    int ph = parent.getHeight();
                                                    int vw = v.getWidth();
                                                    int vh = v.getHeight();

                                                    newX = clamp(newX, 0, pw - vw);
                                                    newY = clamp(newY, 0, ph - vh);

                                                    v.setX(newX);
                                                    v.setY(newY);
                                                    return true;
                                                }
                                                return false;

                                            case MotionEvent.ACTION_UP:
                                            case MotionEvent.ACTION_CANCEL:
                                                h.removeCallbacks(startDrag);
                                                if (dragging) {
                                                    dragging = false;

                                                    spImg.edit()
                                                            .putFloat(KEY_IMG_X, v.getX())
                                                            .putFloat(KEY_IMG_Y, v.getY())
                                                            .apply();
                                                    return true;
                                                }
                                                return false;
                                        }
                                        return false;
                                    }
                                });
                                int index = parent.indexOfChild(headerButton);
                                if (index < 0) {
                                    parent.addView(imageButton, newLp);
                                } else {
                                    int insertPos = Math.max(0, index);
                                    parent.addView(imageButton, insertPos, newLp);
                                }

                                parent.setTag(PARENT_TAG);

                            } catch (Throwable t) {
                                XposedBridge.log("Lime HeaderButton ctor hook error: " + t);
                            }
                        });

                    }
                }
        );



        if (limeOptions.removeOption.checked) return;


        XposedBridge.hookAllMethods(
                loadPackageParam.classLoader.loadClass("com.linecorp.line.settings.lab.LineUserLabSettingsFragment"),
                "onViewCreated",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        Context contextV = getTargetAppContext(loadPackageParam);
                        Context moduleContext = AndroidAppHelper.currentApplication().createPackageContext(
                                Constants.MODULE_NAME, Context.CONTEXT_IGNORE_SECURITY);
                        String backupUri = loadBackupUri(contextV);
                        if (backupUri == null) {
                            XposedBridge.log("Lime: Settings URI not configured");
                            return;
                        }


                        Uri treeUri = Uri.parse(backupUri);
                        Object fragmentObj = param.thisObject;
                        if (fragmentObj == null) {
                            XposedBridge.log("Lime: fragmentObj is null in onViewCreated");
                            return;
                        }


                        Activity activity2 = null;
                        try {
                            Object actObj = XposedHelpers.callMethod(fragmentObj, "getActivity");
                            if (actObj instanceof Activity) {
                                activity2 = (Activity) actObj;
                            }
                        } catch (Throwable t) {
                            XposedBridge.log("Lime: getActivity call failed: " + t);
                        }

                        if (activity2 == null) {
                            XposedBridge.log("Lime: activity is null in onViewCreated");
                            return;
                        }
                            try {
                                PackageManager pm = contextV.getPackageManager();
                                String versionName = "";
                                try {
                                    versionName = pm.getPackageInfo(loadPackageParam.packageName, 0).versionName;
                                } catch (PackageManager.NameNotFoundException e) {
                                    XposedBridge.log("Lime: Package info error: " + e.getMessage());
                                }

                                Set<String> checkedOptions = new HashSet<>();
                                DocumentPreferences docPrefs = new DocumentPreferences(contextV, treeUri);
                                for (LimeOptions.Option option : limeOptions.options) {
                                    if (!checkedOptions.contains(option.name)) {
                                        option.checked = Boolean.parseBoolean(
                                                docPrefs.getSetting(option.name, String.valueOf(option.checked))
                                        );
                                        checkedOptions.add(option.name);
                                    }
                                }
                                ViewGroup viewGroup = ((ViewGroup) param.args[0]);
                                Context context = viewGroup.getContext();
                                Utils.addModuleAssetPath(context);

                                FrameLayout rootLayout = new FrameLayout(context);
                                rootLayout.setLayoutParams(new ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT));

                                Button button = new Button(context);
                                button.setText(R.string.app_name);

                                FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                                        FrameLayout.LayoutParams.WRAP_CONTENT,
                                        FrameLayout.LayoutParams.WRAP_CONTENT);
                                layoutParams.gravity = Gravity.TOP | Gravity.END;
                                layoutParams.rightMargin = Utils.dpToPx(10, context);

                                int statusBarHeight = getStatusBarHeight(context);

                                String versionNameStr = String.valueOf(versionName);
                                String majorVersionStr = versionNameStr.split("\\.")[0];
                                int versionNameInt = Integer.parseInt(majorVersionStr);

                                if (versionNameInt >= 15) {
                                    layoutParams.topMargin = statusBarHeight;
                                } else {
                                    layoutParams.topMargin = Utils.dpToPx(5, context);
                                }
                                button.setLayoutParams(layoutParams);

                                button.setOnClickListener(view -> {
                                    if (backupUri == null) {
                                        Toast.makeText(context, "Please select settings folder first", Toast.LENGTH_LONG).show();
                                        return;
                                    }
                                    ScrollView categoryLayout = createCategoryListLayout(context, Arrays.asList(limeOptions.options), docPrefs, moduleContext, loadPackageParam, false);
                                    showView(rootLayout, categoryLayout);
                                });
                                rootLayout.addView(button);
                                viewGroup.addView(rootLayout);
                            } catch (Exception e) {
                                new Handler(Looper.getMainLooper()).post(() -> {
                                    if (moduleContext != null) {
                                        Toast.makeText(
                                                moduleContext,
                                                moduleContext.getString(R.string.Error_Create_setting_Button)
                                                        + moduleContext.getString(R.string.save_failed),
                                                Toast.LENGTH_LONG
                                        ).show();
                                    }
                                });
                            }

                    }
                });

        String baseName = "androidx.fragment.app.";
        List<String> validClasses = new ArrayList<>();
        for (char c = 'a'; c <= 'z'; c++) {
            String className = baseName + c;
            try {
                Class<?> clazz = loadPackageParam.classLoader.loadClass(className);

                try {
                    clazz.getDeclaredMethod("onActivityResult", int.class, int.class, Intent.class);
                    validClasses.add(className);
                    XposedBridge.log("Found valid fragment class: " + className);
                } catch (NoSuchMethodException ignored) {}
            } catch (ClassNotFoundException ignored) {}
        }

        if (validClasses.isEmpty()) {
            XposedBridge.log("No valid fragment class found with onActivityResult method");
        } else {
            for (String fragmentClass : validClasses) {
                try {
                    XposedHelpers.findAndHookMethod(
                            fragmentClass,
                            loadPackageParam.classLoader,
                            "onActivityResult",
                            int.class, int.class, Intent.class,
                            new XC_MethodHook() {
                                @Override
                                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                    Context moduleContext = AndroidAppHelper.currentApplication().createPackageContext(
                                            Constants.MODULE_NAME, Context.CONTEXT_IGNORE_SECURITY);
                                    int requestCode = (int) param.args[0];
                                    int resultCode = (int) param.args[1];
                                    Intent data = (Intent) param.args[2];

                                    if (requestCode == PICK_FILE_REQUEST_CODE
                                            && resultCode == Activity.RESULT_OK
                                            && data != null) {

                                        Context context = (Context) param.thisObject;
                                        Uri uri = data.getData();

                                        new Thread(() -> {
                                            File tempFile = null;
                                            try {
                                                tempFile = File.createTempFile("restore", ".db", context.getCacheDir());
                                                tempFile.setReadable(true, false);

                                                try (InputStream is = context.getContentResolver().openInputStream(uri);
                                                     OutputStream os = new FileOutputStream(tempFile)) {

                                                    byte[] buffer = new byte[8192];
                                                    int length;
                                                    while ((length = is.read(buffer)) > 0) {
                                                        os.write(buffer, 0, length);
                                                    }
                                                    os.flush();

                                                    if (tempFile.length() == 0) {
                                                        throw new IOException("Copied file is empty");
                                                    }

                                                    File finalTempFile = tempFile;
                                                    new Handler(Looper.getMainLooper()).post(() -> {
                                                        restoreChatHistory(context, moduleContext, finalTempFile);
                                                    });
                                                }
                                            } catch (Exception e) {
                                                Log.e("FileCopy", "Error copying file", e);
                                                new Handler(Looper.getMainLooper()).post(() ->
                                                        Toast.makeText(context,
                                                                moduleContext.getString(R.string.file_copy_error) + ": " + e.getMessage(),
                                                                Toast.LENGTH_LONG).show());

                                                if (tempFile != null && tempFile.exists()) {
                                                    tempFile.delete();
                                                }
                                            }
                                        }).start();
                                    }
                                    if (requestCode == PICK_FILE_REQUEST_CODE2
                                            && resultCode == Activity.RESULT_OK
                                            && data != null) {

                                        Context context = (Context) param.thisObject;
                                        Uri uri = data.getData();

                                        new Thread(() -> {
                                            File tempFile = null;
                                            try {
                                                tempFile = File.createTempFile("restore", ".db", context.getCacheDir());
                                                tempFile.setReadable(true, false);

                                                try (InputStream is = context.getContentResolver().openInputStream(uri);
                                                     OutputStream os = new FileOutputStream(tempFile)) {

                                                    byte[] buffer = new byte[8192];
                                                    int length;
                                                    while ((length = is.read(buffer)) > 0) {
                                                        os.write(buffer, 0, length);
                                                    }
                                                    os.flush();
                                                    if (tempFile.length() == 0) {
                                                        throw new IOException("Copied file is empty");
                                                    }

                                                    File finalTempFile = tempFile;
                                                    new Handler(Looper.getMainLooper()).post(() -> {
                                                        restoreChatList(context, moduleContext, finalTempFile);
                                                    });
                                                }
                                            } catch (Exception e) {
                                                Log.e("FileCopy", "Error copying file", e);
                                                new Handler(Looper.getMainLooper()).post(() ->
                                                        Toast.makeText(context,
                                                                moduleContext.getString(R.string.file_copy_error) + ": " + e.getMessage(),
                                                                Toast.LENGTH_LONG).show());

                                                if (tempFile != null && tempFile.exists()) {
                                                    tempFile.delete();
                                                }
                                            }
                                        }).start();
                                    }
                                }
                            }
                    );
                    XposedBridge.log("Successfully hooked onActivityResult in: " + fragmentClass);
                } catch (Throwable t) {
                    XposedBridge.log("Failed to hook onActivityResult in " + fragmentClass + ": " + t.getMessage());
                }
            }
        }
    }


    private void showView(ViewGroup parent, View view) {

        ViewGroup currentParent = (ViewGroup) view.getParent();
        if (currentParent != null) {

            currentParent.removeView(view);
        }
        parent.removeAllViews();
        parent.addView(view);
    }

        private ScrollView createCategoryListLayout(
                Context context,
                List<LimeOptions.Option> options,
                DocumentPreferences docPrefs,
                Context moduleContext,
                XC_LoadPackage.LoadPackageParam loadPackageParam,
                boolean launchedFromHeader
        ) {

            boolean dark = isDarkMode(context);
            ThemePalette P = new ThemePalette(dark);

            ScrollView scrollView = new ScrollView(context);
            scrollView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            scrollView.setFillViewport(true);

            LinearLayout layout = new LinearLayout(context);
            layout.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT));
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(
                    Utils.dpToPx(20, context),
                    Utils.dpToPx(20, context),
                    Utils.dpToPx(20, context),
                    Utils.dpToPx(20, context)
            );
            layout.setBackgroundColor(P.bg);

            // ===============================
            // Version Text
            // ===============================
            String version = BuildConfig.VERSION_NAME;
            TextView versionTextView = new TextView(context);
            versionTextView.setText("LIMEs (" + version + ")");
            versionTextView.setTextSize(16);
            versionTextView.setTextColor(P.fg);
            versionTextView.setGravity(Gravity.CENTER);
            versionTextView.setPadding(
                    0,
                    Utils.dpToPx(10, context),
                    0,
                    Utils.dpToPx(10, context)
            );
            layout.addView(versionTextView);

            // ===============================
            // Category 分類
            // ===============================
            Map<LimeOptions.OptionCategory, List<LimeOptions.Option>> categorizedOptions = new LinkedHashMap<>();
            List<LimeOptions.OptionCategory> categoryOrder = Arrays.asList(
                    LimeOptions.OptionCategory.GENERAL,
                    LimeOptions.OptionCategory.NOTIFICATIONS,
                    LimeOptions.OptionCategory.CHAT,
                    LimeOptions.OptionCategory.Ad,
                    LimeOptions.OptionCategory.CALL,
                    LimeOptions.OptionCategory.Theme,
                    LimeOptions.OptionCategory.OTHER
            );

            for (LimeOptions.OptionCategory category : categoryOrder) {
                categorizedOptions.put(category, new ArrayList<>());
            }

            for (LimeOptions.Option option : options) {
                categorizedOptions.computeIfAbsent(option.category, k -> new ArrayList<>()).add(option);
            }

            // ===============================
            // Category Title ＋ Divider
            // ===============================
            for (Map.Entry<LimeOptions.OptionCategory, List<LimeOptions.Option>> entry : categorizedOptions.entrySet()) {
                LimeOptions.OptionCategory category = entry.getKey();
                List<LimeOptions.Option> optionsInCategory = entry.getValue();

                TextView categoryTitle = new TextView(context);
                categoryTitle.setText(category.getName(context));
                categoryTitle.setTextSize(18);
                categoryTitle.setPadding(
                        0,
                        Utils.dpToPx(12, context),
                        0,
                        Utils.dpToPx(6, context)
                );
                categoryTitle.setTextColor(P.fg);
                categoryTitle.setClickable(true);
                categoryTitle.setOnClickListener(v -> {
                    LinearLayout optionsLayout = createOptionsLayout(
                            context,
                            optionsInCategory,
                            docPrefs,
                            moduleContext,
                            loadPackageParam,
                            launchedFromHeader
                    );
                    showView((ViewGroup) layout.getParent(), optionsLayout);
                });

                layout.addView(categoryTitle);

                layout.addView(createDivider(context, P));
            }

            // ===============================
            // Additional Buttons
            // ===============================
            addAdditionalButtons(context, layout, docPrefs, moduleContext);

            // ===============================
            // Save (Restart) Button
            // ===============================
            layout.addView(createDivider(context, P));

            Button saveButton = new Button(context);
            saveButton.setText(moduleContext.getString(R.string.Restart));
            saveButton.setTextColor(P.fg);
            saveButton.setBackgroundColor(P.btnBg);
            saveButton.setOnClickListener(v -> {
                boolean saveSuccess = true;

                try {
                    for (LimeOptions.Option option : limeOptions.options) {
                        docPrefs.saveSetting(option.name, String.valueOf(option.checked));
                    }
                } catch (IOException e) {
                    saveSuccess = false;
                    XposedBridge.log("Lime: Save failed: " + e.getMessage());
                }

                if (!saveSuccess) {
                    Toast.makeText(context, context.getString(R.string.save_failed), Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(
                            context.getApplicationContext(),
                            context.getString(R.string.restarting),
                            Toast.LENGTH_SHORT
                    ).show();
                    context.startActivity(
                            new Intent().setClassName(
                                    Constants.PACKAGE_NAME,
                                    "jp.naver.line.android.activity.SplashActivity"
                            )
                    );
                    Process.killProcess(Process.myPid());
                }
            });
            layout.addView(saveButton);

            // ===============================
            // Back Button
            // ===============================
            layout.addView(createDivider(context, P));

            Button hideButton = new Button(context);
            hideButton.setText(moduleContext.getString(R.string.back));
            hideButton.setTextColor(P.fg);
            hideButton.setBackgroundColor(P.btnBg);

            hideButton.setOnClickListener(v -> {
                if (launchedFromHeader) {
                    // ヘッダーオーバーレイ → 閉じる
                    ViewParent p1 = layout.getParent();
                    if (p1 instanceof View) {
                        View scroll = (View) p1;
                        ViewParent p2 = scroll.getParent();
                        if (p2 instanceof ViewGroup) {
                            ((ViewGroup) p2).removeView((View) p1);
                        }
                    }
                } else {
                    // 通常メニュー → 元のレイアウトに戻る
                    ViewParent parent = layout.getParent();
                    if (parent instanceof ViewGroup) {
                        ViewGroup vg = (ViewGroup) parent;
                        View buttonLayout = createButtonLayout(context, docPrefs, moduleContext, loadPackageParam, true);
                        showView(vg, buttonLayout);
                    }
                }
            });

            layout.addView(hideButton);

            scrollView.addView(layout);
            return scrollView;
        }
    public static class SettingActionButton {
        public final LimeOptions.OptionCategory category;
        public final Supplier<Boolean> visibleCondition;
        public final String title;
        public final View.OnClickListener onClick;

        public SettingActionButton(
                LimeOptions.OptionCategory category,
                Supplier<Boolean> visibleCondition,
                String title,
                View.OnClickListener onClick
        ) {
            this.category = category;
            this.visibleCondition = visibleCondition;
            this.title = title;
            this.onClick = onClick;
        }
    }
    private List<SettingActionButton> getSettingActionButtons(
            Context context,
            Context moduleContext
    ) {
        return Arrays.asList(


                new SettingActionButton(
                        LimeOptions.OptionCategory.CHAT,
                        () -> limeOptions.PhotoSave.checked,
                        "PhotoSave Name",
                        v -> SettingPhotoSave(context, moduleContext)
                )


        );
    }

    private LinearLayout createOptionsLayout(
            Context context,
            List<LimeOptions.Option> options,
            DocumentPreferences docPrefs,
            Context moduleContext,
            XC_LoadPackage.LoadPackageParam loadPackageParam,
            boolean launchedFromHeader
    ) {

        boolean dark = isDarkMode(context);
        ThemePalette P = new ThemePalette(dark);

        LinearLayout layout = new LinearLayout(context);
        layout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(
                Utils.dpToPx(20, context),
                Utils.dpToPx(20, context),
                Utils.dpToPx(20, context),
                Utils.dpToPx(20, context)
        );
        layout.setBackgroundColor(P.bg);
        layout.setClickable(true);
        layout.setFocusable(true);

        // ===============================
        // Version
        // ===============================
        TextView versionTextView = new TextView(context);
        versionTextView.setText("LIMEs (" + BuildConfig.VERSION_NAME + ")");
        versionTextView.setTextSize(16);
        versionTextView.setTextColor(P.fg);
        versionTextView.setGravity(Gravity.CENTER);
        versionTextView.setPadding(0, Utils.dpToPx(10, context), 0, Utils.dpToPx(10, context));
        layout.addView(versionTextView);

        // ===============================
        // 親子 Switch 管理用
        // ===============================
        Switch switchRedirectWebView = null;
        Switch photoAddNotificationView = null;
        Switch readCheckerView = null;
        Switch preventUnsendMessageView = null;
        Switch darkModeView = null;
        Switch preventUnreadView = null;

        List<Switch> webViewChildSwitches = new ArrayList<>();
        List<Switch> photoNotificationChildSwitches = new ArrayList<>();
        List<Switch> readCheckerSwitches = new ArrayList<>();
        List<Switch> preventUnsendMessageSwitches = new ArrayList<>();
        List<Switch> darkModeSwitches = new ArrayList<>();
        List<Switch> preventUnreadSwitches = new ArrayList<>();

        // ===============================
        // Switch 描画
        // ===============================
        for (LimeOptions.Option option : options) {
            Switch sw = new Switch(context);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.topMargin = Utils.dpToPx(10, context);
            sw.setLayoutParams(params);

            sw.setText(option.id);
            sw.setTextColor(P.fg);
            sw.setChecked(option.checked);

            sw.setOnCheckedChangeListener((buttonView, isChecked) -> {
                option.checked = isChecked;
                try {
                    docPrefs.saveSetting(option.name, String.valueOf(isChecked));
                } catch (IOException e) {
                    XposedBridge.log(e);
                }
            });

            switch (option.name) {

                case "PhotoSave":
                    // 親スイッチ（ボタン表示条件に使用）
                    break;

                case "redirect_webview":
                    switchRedirectWebView = sw;
                    break;

                case "open_in_browser":
                    webViewChildSwitches.add(sw);
                    sw.setEnabled(switchRedirectWebView != null && switchRedirectWebView.isChecked());
                    break;

                case "PhotoAddNotification":
                    photoAddNotificationView = sw;
                    break;

                case "GroupNotification":
                case "CansellNotification":
                case "AddCopyAction":
                case "DisableSilentMessage":
                    photoNotificationChildSwitches.add(sw);
                    sw.setEnabled(photoAddNotificationView != null && photoAddNotificationView.isChecked());
                    break;

                case "ReadChecker":
                    readCheckerView = sw;
                    break;

                case "MySendMessage":
                case "ReadCheckerChatdataDelete":
                    readCheckerSwitches.add(sw);
                    sw.setEnabled(readCheckerView != null && readCheckerView.isChecked());
                    break;

                case "prevent_unsend_message":
                    preventUnsendMessageView = sw;
                    break;

                case "hide_canceled_message":
                    preventUnsendMessageSwitches.add(sw);
                    sw.setEnabled(preventUnsendMessageView != null && preventUnsendMessageView.isChecked());
                    break;

                case "DarkColor":
                    darkModeView = sw;
                    break;

                case "DarkModSync":
                    darkModeSwitches.add(sw);
                    sw.setEnabled(darkModeView != null && darkModeView.isChecked());
                    break;

                case "prevent_mark_as_read":
                    preventUnreadView = sw;
                    break;
            }

            layout.addView(sw);
        }

        // ===============================
        // カテゴリ別 設定画面ボタン
        // ===============================
        LimeOptions.OptionCategory currentCategory = options.isEmpty()
                ? null
                : options.get(0).category;

        if (currentCategory != null) {
            for (SettingActionButton btn : getSettingActionButtons(context, moduleContext)) {
                if (btn.category != currentCategory) continue;
                if (btn.visibleCondition != null && !btn.visibleCondition.get()) continue;

                layout.addView(createDivider(context, P));

                Button b = new Button(context);
                b.setText(btn.title);
                b.setTextColor(P.fg);
                b.setBackgroundColor(P.btnBg);
                b.setOnClickListener(btn.onClick);

                layout.addView(b);
            }
        }

        // ===============================
        // Restart
        // ===============================
        layout.addView(createDivider(context, P));

        Button saveButton = new Button(context);
        saveButton.setText(moduleContext.getString(R.string.Restart));
        saveButton.setTextColor(P.fg);
        saveButton.setBackgroundColor(P.btnBg);
        saveButton.setOnClickListener(v -> {
            Toast.makeText(context, context.getString(R.string.restarting), Toast.LENGTH_SHORT).show();
            context.startActivity(new Intent().setClassName(
                    Constants.PACKAGE_NAME,
                    "jp.naver.line.android.activity.SplashActivity"
            ));
            Process.killProcess(Process.myPid());
        });
        layout.addView(saveButton);

        // ===============================
        // Back
        // ===============================
        Button backButton = new Button(context);
        backButton.setText(moduleContext.getString(R.string.back));
        backButton.setTextColor(P.fg);
        backButton.setBackgroundColor(P.btnBg);
        backButton.setOnClickListener(v -> {
            ScrollView categoryLayout = createCategoryListLayout(
                    context,
                    Arrays.asList(limeOptions.options),
                    docPrefs,
                    moduleContext,
                    loadPackageParam,
                    launchedFromHeader
            );
            showView((ViewGroup) layout.getParent(), categoryLayout);
        });
        layout.addView(backButton);

        ScrollView scrollView = new ScrollView(context);
        scrollView.addView(layout);
        return layout;
    }


    private String getOptionNameFromSwitch(Switch switchView, List<LimeOptions.Option> options) {
        try {
            int switchTextId = Integer.parseInt(switchView.getText().toString());

            for (LimeOptions.Option option : options) {
                if (option.id == switchTextId) {
                    return option.name;
                }
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return null;
    }
    private View createButtonLayout(

            Context context,
            DocumentPreferences docPrefs,
            Context moduleContext,
            XC_LoadPackage.LoadPackageParam loadPackageParam,
            boolean launchedFromHeader
    ) {
        PackageManager pm = context.getPackageManager();
        String versionName = "";
        try {
            versionName = pm.getPackageInfo(loadPackageParam.packageName, 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        FrameLayout rootLayout = new FrameLayout(context);
        rootLayout.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        Button button = new Button(context);
        button.setText(R.string.app_name);

        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.gravity = Gravity.TOP | Gravity.END;
        layoutParams.rightMargin = Utils.dpToPx(10, context);

        int statusBarHeight = getStatusBarHeight(context);

        String versionNameStr = String.valueOf(versionName);
        String majorVersionStr = versionNameStr.split("\\.")[0];
        int versionNameInt = Integer.parseInt(majorVersionStr);

        if (versionNameInt >= 15) {
            layoutParams.topMargin = statusBarHeight;
        } else {
            layoutParams.topMargin = Utils.dpToPx(5, context);
        }
        button.setLayoutParams(layoutParams);

        button.setOnClickListener(view -> {
            ScrollView categoryLayout = createCategoryListLayout(
                    context,
                    Arrays.asList(limeOptions.options),
                    docPrefs,
                    moduleContext,
                    loadPackageParam,
                    launchedFromHeader // ★右上ボタン経由
            );
            showView(rootLayout, categoryLayout);
        });

        rootLayout.addView(button);
        return rootLayout;
    }

    private View createDivider(Context context, ThemePalette P) {
        View divider = new View(context);
        divider.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                2
        ));
        divider.setBackgroundColor(P.divider);
        return divider;
    }
    private void addAdditionalButtons(Context context, LinearLayout layout, DocumentPreferences docPrefs, Context moduleContext) {

        boolean dark = isDarkMode(context);
        ThemePalette P = new ThemePalette(dark);

        layout.addView(createDivider(context, P));

        Button resetFcmButton = new Button(context);
        resetFcmButton.setText("FCM AppID リセット");
        resetFcmButton.setTextColor(P.fg);
        resetFcmButton.setBackgroundColor(P.btnBg);

        resetFcmButton.setOnClickListener(v ->
                resetFcmAppId(context)
        );

        layout.addView(resetFcmButton);


        // ============================================================
        // KeepUnread Button
        // ============================================================
        layout.addView(createDivider(context, P));
        Button keepUnreadButton = new Button(context);
        keepUnreadButton.setText(moduleContext.getString(R.string.edit_margin_settings));
        keepUnreadButton.setTextColor(P.fg);
        keepUnreadButton.setBackgroundColor(P.btnBg);
        keepUnreadButton.setOnClickListener(v -> KeepUnread_Button(context, moduleContext));
        layout.addView(keepUnreadButton);

        // ============================================================
        // Restore Chat History
        // ============================================================
            layout.addView(createDivider(context, P));
        Button restoreButton = new Button(context);
        restoreButton.setText(moduleContext.getString(R.string.Restore));
        restoreButton.setTextColor(P.fg);
        restoreButton.setBackgroundColor(P.btnBg);
        restoreButton.setOnClickListener(v -> showFilePickerChatHistory(context, moduleContext));
        layout.addView(restoreButton);

        // ============================================================
        // Restore Chat List
        // ============================================================
            layout.addView(createDivider(context, P));
        Button restoreChatListButton = new Button(context);
        restoreChatListButton.setText(moduleContext.getString(R.string.restoreChatListButton));
        restoreChatListButton.setTextColor(P.fg);
        restoreChatListButton.setBackgroundColor(P.btnBg);
        restoreChatListButton.setOnClickListener(v -> showFilePickerChatlist(context, moduleContext));
        layout.addView(restoreChatListButton);

        // ============================================================
        // Backup Chat History
        // ============================================================
            layout.addView(createDivider(context, P));
        Button backupButton = new Button(context);
        backupButton.setText(moduleContext.getString(R.string.Back_Up));
        backupButton.setTextColor(P.fg);
        backupButton.setBackgroundColor(P.btnBg);
        backupButton.setOnClickListener(v -> backupChatHistory(context, moduleContext));
        layout.addView(backupButton);

        // ============================================================
        // Backup Image Folder
        // ============================================================
            layout.addView(createDivider(context, P));
        Button backupfolderButton = new Button(context);
        backupfolderButton.setText(moduleContext.getString(R.string.Talk_Picture_Back_up));
        backupfolderButton.setTextColor(P.fg);
        backupfolderButton.setBackgroundColor(P.btnBg);
        backupfolderButton.setOnClickListener(v -> backupChatsFolder(context, moduleContext));
        layout.addView(backupfolderButton);

        // ============================================================
        // Restore Image Folder
        // ============================================================
            layout.addView(createDivider(context, P));
        Button restorefolderButton = new Button(context);
        restorefolderButton.setText(moduleContext.getString(R.string.Picure_Restore));
        restorefolderButton.setTextColor(P.fg);
        restorefolderButton.setBackgroundColor(P.btnBg);
        restorefolderButton.setOnClickListener(v -> restoreChatsFolder(context, moduleContext));
        layout.addView(restorefolderButton);

        // ============================================================
        // Get MID/ID Button
        // ============================================================
            layout.addView(createDivider(context, P));
        Button GetMidIdButton = new Button(context);
        GetMidIdButton.setText(moduleContext.getString(R.string.GetMidIdButton));
        GetMidIdButton.setTextColor(P.fg);
        GetMidIdButton.setBackgroundColor(P.btnBg);
        GetMidIdButton.setOnClickListener(v -> GetMidId(context, moduleContext));
        layout.addView(GetMidIdButton);

        // ============================================================
        // Mute Groups
        // ============================================================
        if (limeOptions.MuteGroup.checked) {
                layout.addView(createDivider(context, P));
            Button muteGroupsButton = new Button(context);
            muteGroupsButton.setText(moduleContext.getString(R.string.Mute_Group));
            muteGroupsButton.setTextColor(P.fg);
            muteGroupsButton.setBackgroundColor(P.btnBg);
            muteGroupsButton.setOnClickListener(v -> MuteGroups_Button(context, moduleContext));
            layout.addView(muteGroupsButton);
        }

        // ============================================================
        // Prevent Unsend Message (Canceled Message Edit)
        // ============================================================
        if (limeOptions.preventUnsendMessage.checked) {
                layout.addView(createDivider(context, P));
            Button canceledMessageButton = new Button(context);
            canceledMessageButton.setText(moduleContext.getString(R.string.canceled_message));
            canceledMessageButton.setTextColor(P.fg);
            canceledMessageButton.setBackgroundColor(P.btnBg);
            canceledMessageButton.setOnClickListener(v -> Cancel_Message_Button(context, moduleContext));
            layout.addView(canceledMessageButton);
        }

        // ============================================================
        // Cancel Notification
        // ============================================================
        if (limeOptions.CansellNotification.checked) {
                layout.addView(createDivider(context, P));
            Button cansellNotificationButton = new Button(context);
            cansellNotificationButton.setText(moduleContext.getString(R.string.CansellNotification));
            cansellNotificationButton.setTextColor(P.fg);
            cansellNotificationButton.setBackgroundColor(P.btnBg);
            cansellNotificationButton.setOnClickListener(v -> CansellNotification(context, moduleContext));
            layout.addView(cansellNotificationButton);
        }

        // ============================================================
        // Block Check
        // ============================================================
        if (limeOptions.BlockCheck.checked) {
                layout.addView(createDivider(context, P));
            Button blockCheckButton = new Button(context);
            blockCheckButton.setText(moduleContext.getString(R.string.BlockCheck));
            blockCheckButton.setTextColor(P.fg);
            blockCheckButton.setBackgroundColor(P.btnBg);
            blockCheckButton.setOnClickListener(v -> Blocklist(context, moduleContext));
            layout.addView(blockCheckButton);
        }

        // ============================================================
        // Pin List
        // ============================================================
        if (limeOptions.PinList.checked) {
                layout.addView(createDivider(context, P));
            Button pinListButton = new Button(context);
            pinListButton.setText(moduleContext.getString(R.string.PinList));
            pinListButton.setTextColor(P.fg);
            pinListButton.setBackgroundColor(P.btnBg);
            pinListButton.setOnClickListener(v -> PinListButton(context, moduleContext));
            layout.addView(pinListButton);
        }

        // ============================================================
        // Read Check List (Edit)
        // ============================================================
        if (limeOptions.PreventMarkAsRead_Setting.checked) {
                layout.addView(createDivider(context, P));
            Button showChatCheckListDialog = new Button(context);
            showChatCheckListDialog.setText("Read Check Edit");
            showChatCheckListDialog.setTextColor(P.fg);
            showChatCheckListDialog.setBackgroundColor(P.btnBg);
            showChatCheckListDialog.setOnClickListener(v -> showChatCheckListDialog(activity2, context));
            layout.addView(showChatCheckListDialog);
        }
//        if (limeOptions.PhotoSave.checked) {
//            layout.addView(createDivider(context, P));
//            Button PhotoSave = new Button(context);
//            PhotoSave.setText("PhotoSave Name");
//            PhotoSave.setTextColor(P.fg);
//            PhotoSave.setBackgroundColor(P.btnBg);
//            PhotoSave.setOnClickListener(v -> SettingPhotoSave(activity2, moduleContext));
//            layout.addView(PhotoSave);
//        }

        // ============================================================
        // Modify Request JavaScript
        // ============================================================
            layout.addView(createDivider(context, P));
        Button modifyRequestButton = new Button(context);
        modifyRequestButton.setText(R.string.modify_request);
        modifyRequestButton.setTextColor(P.fg);
        modifyRequestButton.setBackgroundColor(P.btnBg);
        modifyRequestButton.setOnClickListener(v -> {
            try {
                String encodedJs = docPrefs.getSetting("encoded_js_modify_request", "");
                showModifyDialog(
                        context,
                        moduleContext.getString(R.string.modify_request),
                        encodedJs,
                        (content) -> {
                            try {
                                docPrefs.saveSetting("encoded_js_modify_request",
                                        Base64.encodeToString(content.getBytes(), Base64.NO_WRAP));
                            } catch (IOException e) {
                                Toast.makeText(context, "Failed to save request modification", Toast.LENGTH_SHORT).show();
                            }
                        },
                        moduleContext
                );
            } catch (Exception e) {
                Toast.makeText(context, "Error loading request settings", Toast.LENGTH_SHORT).show();
            }
        });
        layout.addView(modifyRequestButton);

        // ============================================================
        // Modify Response JavaScript
        // ============================================================
            layout.addView(createDivider(context, P));
        Button modifyResponseButton = new Button(context);
        modifyResponseButton.setText(R.string.modify_response);
        modifyResponseButton.setTextColor(P.fg);
        modifyResponseButton.setBackgroundColor(P.btnBg);
        modifyResponseButton.setOnClickListener(v -> {
            try {
                String encodedJs = docPrefs.getSetting("encoded_js_modify_response", "");
                showModifyDialog(
                        context,
                        moduleContext.getString(R.string.modify_response),
                        encodedJs,
                        (content) -> {
                            try {
                                docPrefs.saveSetting("encoded_js_modify_response",
                                        Base64.encodeToString(content.getBytes(), Base64.NO_WRAP));
                            } catch (IOException e) {
                                Toast.makeText(context, "Failed to save response modification", Toast.LENGTH_SHORT).show();
                            }
                        },
                        moduleContext
                );
            } catch (Exception e) {
                Toast.makeText(context, "Error loading response settings", Toast.LENGTH_SHORT).show();
            }
        });
        layout.addView(modifyResponseButton);

        // ============================================================
        // Reset URI Button
        // ============================================================
            layout.addView(createDivider(context, P));
        Button resetButton = new Button(context);
        resetButton.setText(moduleContext.getString(R.string.ResetButton));
        resetButton.setTextColor(P.fg);
        resetButton.setBackgroundColor(P.btnBg);
        resetButton.setOnClickListener(v -> {
            try {
                File settingsDir = new File(context.getFilesDir(), "LimeBackup");
                File settingsFile = new File(settingsDir, "backup_uri.txt");

                String toastMsg;
                if (settingsFile.exists()) {
                    boolean deleted = settingsFile.delete();
                    toastMsg = deleted ? " Reset URI" : "Error";

                    Toast.makeText(context.getApplicationContext(),
                            context.getString(R.string.restarting), Toast.LENGTH_SHORT).show();

                    context.startActivity(new Intent().setClassName(Constants.PACKAGE_NAME,
                            "jp.naver.line.android.activity.SplashActivity"));
                    Process.killProcess(Process.myPid());
                } else {
                    toastMsg = "リセット対象の URI はありません";
                }

                Toast.makeText(moduleContext, toastMsg, Toast.LENGTH_SHORT).show();
            } catch (Throwable t) {
                Toast.makeText(moduleContext, "エラー: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
        layout.addView(resetButton);

        // ============================================================
        // Copy File Button
        // ============================================================
            layout.addView(createDivider(context, P));
        Button CopyFileButton = new Button(context);
        CopyFileButton.setText(moduleContext.getString(R.string.CopyFileButton));
        CopyFileButton.setTextColor(P.fg);
        CopyFileButton.setBackgroundColor(P.btnBg);

        String[] resourceNames = {
                "dial_tone.wav",
                "lineapp_endthis_16k.wav",
                "keep_switch_off.png",
                "keep_switch_on.png",
                "reaction.png",
                "read_checker.png",
                "read_switch_off.png",
                "read_switch_on.png",

                "keep_switch_off_light.png",
                "keep_switch_off_dark.png",
                "keep_switch_on_light.png",
                "keep_switch_on_dark.png",
                "reaction_light.png",
                "reaction_dark.png",
                "read_checker_light.png",
                "read_checker_dark.png",
                "read_switch_off_light.png",
                "read_switch_off_dark.png",
                "read_switch_on_light.png",
                "read_switch_on_dark.png",
                "ringtone.wav",
                "header_setting_dark.png"
        };

        int[] resourceIds = {
                R.raw.dial_tone,
                R.raw.ringtone,
                R.raw.reaction, R.raw.read_checker, R.raw.read_switch_off, R.raw.read_switch_on,R.raw.keep_switch_off, R.raw.keep_switch_on,
                R.raw.keep_switch_off_light,
                R.raw.keep_switch_off_dark,
                R.raw.keep_switch_on_light,
                R.raw.keep_switch_on_dark,
                R.raw.reaction_light,
                R.raw.reaction_dark,
                R.raw.read_checker_light,
                R.raw.read_checker_dark,
                R.raw.read_switch_off_light,
                R.raw.read_switch_off_dark,
                R.raw.read_switch_on_light,
                R.raw.read_switch_on_dark,
                R.raw.header_setting_dark,
                R.raw.header_setting_light,
        };

        CopyFileButton.setOnClickListener(v -> {
            try {
                AlertDialog.Builder builderFile = new AlertDialog.Builder(
                        context,
                        dark ? android.R.style.Theme_DeviceDefault_Dialog_NoActionBar
                                : android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar
                );

                builderFile.setTitle("Choice files to copy");
                boolean[] checkedItems = new boolean[resourceNames.length];

                builderFile.setMultiChoiceItems(resourceNames, checkedItems, (dialog, which, isChecked) -> {
                    checkedItems[which] = isChecked;
                });

                builderFile.setPositiveButton("Copy", (dialog, which) -> {
                    String backupUri = loadBackupUri(context);
                    if (backupUri != null) {
                        Uri treeUri = Uri.parse(backupUri);

                        DocumentFile targetDir = DocumentFile.fromTreeUri(context, treeUri);
                        if (targetDir == null || !targetDir.exists()) {
                            Toast.makeText(context, "指定ディレクトリが見つかりません", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        for (int i = 0; i < resourceIds.length; i++) {
                            if (!checkedItems[i]) continue;

                            try (InputStream in = moduleContext.getResources().openRawResource(resourceIds[i])) {

                                DocumentFile existing = targetDir.findFile(resourceNames[i]);
                                if (existing != null) existing.delete();

                                DocumentFile outFile = targetDir.createFile(
                                        getMimeType(resourceNames[i]),
                                        resourceNames[i]
                                );

                                try (OutputStream out = context.getContentResolver().openOutputStream(outFile.getUri())) {
                                    if (out != null) {
                                        byte[] buffer = new byte[4096];
                                        int len;
                                        while ((len = in.read(buffer)) != -1) {
                                            out.write(buffer, 0, len);
                                        }
                                    }
                                }

                            } catch (Exception ignored) {}
                        }
                    }
                    Toast.makeText(context, "success", Toast.LENGTH_SHORT).show();
                });

                builderFile.setNegativeButton("Cancel", null);

                AlertDialog dialog = builderFile.create();
                dialog.show();
                applyDialogPalette(dialog, P);

            } catch (Throwable t) {
                Toast.makeText(moduleContext, "Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        layout.addView(CopyFileButton);

        // ============================================================
        // Wallet Edit Button
        // ============================================================
            layout.addView(createDivider(context, P));
        Button WalletEdit = new Button(context);
        WalletEdit.setText(moduleContext.getString(R.string.WalletEdit_Button));
        WalletEdit.setTextColor(P.fg);
        WalletEdit.setBackgroundColor(P.btnBg);
        WalletEdit.setOnClickListener(v -> WalletEdit_Button(context, moduleContext));
        layout.addView(WalletEdit);

            layout.addView(createDivider(context, P));
    }

    private static class WalletHideItem {
        String contentLine;
        int viewType;
        String entryName;
        CheckBox checkBox;
    }

    public void WalletEdit_Button(Context context, Context moduleContext) {


        boolean dark = isDarkMode(context);
        ThemePalette P = new ThemePalette(dark);

        File dir = new File(context.getFilesDir(), "LimeBackup");
        if (!dir.exists() && !dir.mkdirs()) {
            Toast.makeText(context, "ディレクトリ作成に失敗しました", Toast.LENGTH_SHORT).show();
            return;
        }

        File file = new File(dir, "wallet_hide_layouts.txt");
        if (!file.exists()) {
            try (FileWriter fw = new FileWriter(file)) {
                fw.write("\n");
            } catch (IOException e) {
                Toast.makeText(context, "wallet_hide_layouts.txt 作成失敗", Toast.LENGTH_SHORT).show();
            }
        }

        Map<String, List<WalletHideItem>> groups = new LinkedHashMap<>();
        List<String> otherLines = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String originalLine = line;

                if (line.trim().isEmpty()) {
                    otherLines.add(originalLine);
                    continue;
                }

                boolean isComment = line.trim().startsWith("#");
                String content = line.replaceFirst("^\\s*#", "").trim();

                if (!content.startsWith("WalletViewType=")) {
                    otherLines.add(originalLine);
                    continue;
                }

                String rest = content.substring("WalletViewType=".length()).trim();
                if (rest.isEmpty()) {
                    otherLines.add(originalLine);
                    continue;
                }

                String viewTypeStr;
                String entryName;
                int spaceIdx = rest.indexOf(' ');

                if (spaceIdx > 0) {
                    viewTypeStr = rest.substring(0, spaceIdx).trim();
                    entryName = rest.substring(spaceIdx + 1).trim();
                } else {
                    viewTypeStr = rest;
                    entryName = "";
                }

                int viewType;
                try {
                    viewType = Integer.parseInt(viewTypeStr);
                } catch (NumberFormatException e) {
                    otherLines.add(originalLine);
                    continue;
                }

                WalletHideItem item = new WalletHideItem();
                item.contentLine = "WalletViewType=" + viewTypeStr + (entryName.isEmpty() ? "" : " " + entryName);
                item.viewType = viewType;
                item.entryName = entryName;

                String groupKey = makeGroupKeyFromEntryName(entryName);
                groups.computeIfAbsent(groupKey, k -> new ArrayList<>());
                groups.get(groupKey).add(item);
            }
        } catch (IOException e) {
            Toast.makeText(context, "読み込み失敗", Toast.LENGTH_SHORT).show();
        }

        Map<String, Boolean> hiddenStateMap = new HashMap<>();

        try (BufferedReader br2 = new BufferedReader(new FileReader(file))) { String line;
            while ((line = br2.readLine()) != null) { String trimmed = line.trim(); if (trimmed.isEmpty())
                continue;
                boolean isComment = trimmed.startsWith("#"); String content = line.replaceFirst("^\\s*#", "").trim();
                if (!content.startsWith("WalletViewType="))
                    continue; hiddenStateMap.put(content, !isComment);
            }
        }
        catch (IOException ignored) {}
        int pad = (int) (16 * context.getResources().getDisplayMetrics().density);

        LinearLayout rootLayout = new LinearLayout(context);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setPadding(pad, pad, pad, pad);
        rootLayout.setBackgroundColor(P.bg);

        for (Map.Entry<String, List<WalletHideItem>> entry : groups.entrySet()) {

            String groupKey = entry.getKey();
            List<WalletHideItem> items = entry.getValue();

            LinearLayout headerLayout = new LinearLayout(context);
            headerLayout.setOrientation(LinearLayout.HORIZONTAL);
            headerLayout.setPadding(0, pad / 2, 0, pad / 2);
            headerLayout.setBackgroundColor(P.btnBg);

            TextView headerTitle = new TextView(context);
            headerTitle.setText(groupKey);
            headerTitle.setTextSize(16);
            headerTitle.setTextColor(P.fg);
            headerTitle.setPadding(0, 0, pad, 0);
            headerTitle.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            TextView arrow = new TextView(context);
            arrow.setText("▼");
            arrow.setTextSize(16);
            arrow.setTextColor(P.fg);

            headerLayout.addView(headerTitle);
            headerLayout.addView(arrow);

            LinearLayout childLayout = new LinearLayout(context);
            childLayout.setOrientation(LinearLayout.VERTICAL);
            childLayout.setPadding(pad, 0, 0, 0);
            childLayout.setBackgroundColor(P.bg);
            childLayout.setVisibility(View.VISIBLE);

            headerLayout.setOnClickListener(v -> {
                if (childLayout.getVisibility() == View.VISIBLE) {
                    childLayout.setVisibility(View.GONE);
                    arrow.setText("▶");
                } else {
                    childLayout.setVisibility(View.VISIBLE);
                    arrow.setText("▼");
                }
            });

            for (WalletHideItem item : items) {
                CheckBox cb = new CheckBox(context);

                String label = (!item.entryName.isEmpty())
                        ? (item.entryName + " (" + item.viewType + ")")
                        : ("WalletViewType=" + item.viewType);

                cb.setText(label);
                cb.setTextColor(P.fg);
                cb.setBackgroundColor(P.bg);

                Boolean hidden = hiddenStateMap.get(item.contentLine);
                cb.setChecked(hidden != null && hidden);

                item.checkBox = cb;
                childLayout.addView(cb);
            }

            rootLayout.addView(headerLayout);
            rootLayout.addView(childLayout);
        }

        Button saveButton = new Button(context);
        saveButton.setText("保存");
        saveButton.setTextColor(P.fg);
        saveButton.setBackgroundColor(P.btnBg);

        saveButton.setOnClickListener(v -> {
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, false))) {

                for (String ol : otherLines) {
                    bw.write(ol);
                    bw.newLine();
                }

                for (Map.Entry<String, List<WalletHideItem>> e : groups.entrySet()) {
                    for (WalletHideItem item : e.getValue()) {
                        boolean hide = item.checkBox != null && item.checkBox.isChecked();
                        bw.write((hide ? "" : "# ") + item.contentLine);
                        bw.newLine();
                    }
                }

                Toast.makeText(context, "保存しました", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                Toast.makeText(context, "保存に失敗しました", Toast.LENGTH_SHORT).show();
            }
        });

        rootLayout.addView(saveButton);

        ScrollView scrollView = new ScrollView(context);
        scrollView.setBackgroundColor(P.bg);
        scrollView.addView(rootLayout);

        AlertDialog dialog = new AlertDialog.Builder(
                context,
                dark ? android.R.style.Theme_DeviceDefault_Dialog_NoActionBar
                        : android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar
        )
                .setTitle("Wallet 表示設定")
                .setView(scrollView)
                .setNegativeButton(
                        moduleContext != null ? moduleContext.getString(R.string.cancel) : "閉じる",
                        (d, w) -> d.dismiss()
                )
                .create();

        dialog.show();
        applyDialogPalette(dialog, P);
    }

    private String makeGroupKeyFromEntryName(String entryName) {
        if (entryName == null || entryName.isEmpty()) return "その他";

        String[] parts = entryName.split("_");
        if (parts.length >= 3) {
            return parts[0] + "_" + parts[1] + "_" + parts[2];
        } else if (parts.length == 2) {
            return parts[0] + "_" + parts[1];
        } else {
            return entryName;
        }
    }

    private void showModifyDialog(
            Context context,
            String title,
            String encodedContent,
            ContentSaver contentSaver,
            Context moduleContext
    ) {

        boolean dark = isDarkMode(context);
        ThemePalette P = new ThemePalette(dark);

        String decodedContent = "";
        try {
            decodedContent = new String(Base64.decode(encodedContent, Base64.NO_WRAP));
        } catch (Throwable ignored) {}

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(
                Utils.dpToPx(20, context),
                Utils.dpToPx(20, context),
                Utils.dpToPx(20, context),
                Utils.dpToPx(20, context)
        );
        layout.setBackgroundColor(P.bg);

        EditText editText = new EditText(context);
        editText.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
        ));
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        editText.setText(decodedContent);
        editText.setTextColor(P.fg);
        editText.setBackgroundColor(P.btnBg);
        editText.setPadding(20, 20, 20, 20);
        layout.addView(editText);

        AlertDialog.Builder builder =
                new AlertDialog.Builder(
                        context,
                        dark
                                ? android.R.style.Theme_DeviceDefault_Dialog_NoActionBar
                                : android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar
                );

        builder.setTitle(title);
        builder.setView(layout);

        builder.setPositiveButton(
                moduleContext.getString(R.string.positive_button),
                (dialog, which) -> contentSaver.saveContent(editText.getText().toString())
        );

        builder.setNegativeButton(
                moduleContext.getString(R.string.negative_button),
                null
        );

        AlertDialog dialog = builder.create();
        dialog.show();

        applyDialogPalette(dialog, P);

        try {
            int titleId = context.getResources().getIdentifier("alertTitle", "id", "android");
            TextView titleView = dialog.findViewById(titleId);
            if (titleView != null) titleView.setTextColor(P.fg);
        } catch (Throwable ignored) {}

        try {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(P.fg);
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(P.fg);
        } catch (Throwable ignored) {}
    }


    interface ContentSaver {
        void saveContent(String content);
    }

    private void showModifyRequestDialog(Context context, CustomPreferences customPreferences, Context moduleContext) {
        final String script = new String(Base64.decode(customPreferences.getSetting("encoded_js_modify_request", ""), Base64.NO_WRAP));
        LinearLayout layoutModifyRequest = new LinearLayout(context);
        layoutModifyRequest.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));
        layoutModifyRequest.setOrientation(LinearLayout.VERTICAL);
        layoutModifyRequest.setPadding(Utils.dpToPx(20, context), Utils.dpToPx(20, context), Utils.dpToPx(20, context), Utils.dpToPx(20, context));

        EditText editText = new EditText(context);
        editText.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        editText.setTypeface(Typeface.MONOSPACE);
        editText.setInputType(InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_FLAG_MULTI_LINE |
                InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS |
                InputType.TYPE_TEXT_FLAG_IME_MULTI_LINE);
        editText.setMovementMethod(new ScrollingMovementMethod());
        editText.setTextIsSelectable(true);
        editText.setHorizontallyScrolling(true);
        editText.setVerticalScrollBarEnabled(true);
        editText.setHorizontalScrollBarEnabled(true);
        editText.setText(script);

        layoutModifyRequest.addView(editText);

        LinearLayout buttonLayout = new LinearLayout(context);
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        buttonParams.topMargin = Utils.dpToPx(10, context);
        buttonLayout.setLayoutParams(buttonParams);
        buttonLayout.setOrientation(LinearLayout.HORIZONTAL);

        Button copyButton = new Button(context);
        copyButton.setText(R.string.button_copy);
        copyButton.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("", editText.getText().toString());
            clipboard.setPrimaryClip(clip);
        });
        buttonLayout.addView(copyButton);

        Button pasteButton = new Button(context);
        pasteButton.setText(R.string.button_paste);
        pasteButton.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null && clipboard.hasPrimaryClip()) {
                ClipData clip = clipboard.getPrimaryClip();
                if (clip != null && clip.getItemCount() > 0) {
                    CharSequence pasteData = clip.getItemAt(0).getText();
                    editText.setText(pasteData);
                }
            }
        });
        buttonLayout.addView(pasteButton);

        layoutModifyRequest.addView(buttonLayout);
        ScrollView scrollView = new ScrollView(context);
        scrollView.addView(layoutModifyRequest);
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(R.string.modify_request)
                .setView(scrollView)
                .setPositiveButton(R.string.positive_button, (dialog, which) -> {
                    String code = editText.getText().toString();
                    if (!code.equals(script)) {
                        customPreferences.saveSetting("encoded_js_modify_request", Base64.encodeToString(code.getBytes(), Base64.NO_WRAP));
                        Toast.makeText(context.getApplicationContext(), context.getString(R.string.restarting), Toast.LENGTH_SHORT).show();
                        Process.killProcess(Process.myPid());
                        context.startActivity(new Intent().setClassName(Constants.PACKAGE_NAME, "jp.naver.line.android.activity.SplashActivity"));
                    }
                })
                .setNegativeButton(R.string.negative_button, null)
                .setOnDismissListener(dialog -> editText.setText(script));

        AlertDialog dialog = builder.create();
        dialog.show();
    }


    private void showModifyResponseDialog(Context context, CustomPreferences customPreferences, Context moduleContext) {
        final String script = new String(Base64.decode(customPreferences.getSetting("encoded_js_modify_response", ""), Base64.NO_WRAP));
        LinearLayout layoutModifyResponse = new LinearLayout(context);
        layoutModifyResponse.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));
        layoutModifyResponse.setOrientation(LinearLayout.VERTICAL);
        layoutModifyResponse.setPadding(Utils.dpToPx(20, context), Utils.dpToPx(20, context), Utils.dpToPx(20, context), Utils.dpToPx(20, context));

        EditText editText = new EditText(context);
        editText.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        editText.setTypeface(Typeface.MONOSPACE);
        editText.setInputType(InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_FLAG_MULTI_LINE |
                InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS |
                InputType.TYPE_TEXT_FLAG_IME_MULTI_LINE);
        editText.setMovementMethod(new ScrollingMovementMethod());
        editText.setTextIsSelectable(true);
        editText.setHorizontallyScrolling(true);
        editText.setVerticalScrollBarEnabled(true);
        editText.setHorizontalScrollBarEnabled(true);
        editText.setText(script);

        layoutModifyResponse.addView(editText);

        LinearLayout buttonLayout = new LinearLayout(context);
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        buttonParams.topMargin = Utils.dpToPx(10, context);
        buttonLayout.setLayoutParams(buttonParams);
        buttonLayout.setOrientation(LinearLayout.HORIZONTAL);

        Button copyButton = new Button(context);
        copyButton.setText(R.string.button_copy);
        copyButton.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("", editText.getText().toString());
            clipboard.setPrimaryClip(clip);
        });
        buttonLayout.addView(copyButton);

        Button pasteButton = new Button(context);
        pasteButton.setText(R.string.button_paste);
        pasteButton.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null && clipboard.hasPrimaryClip()) {
                ClipData clip = clipboard.getPrimaryClip();
                if (clip != null && clip.getItemCount() > 0) {
                    CharSequence pasteData = clip.getItemAt(0).getText();
                    editText.setText(pasteData);
                }
            }
        });
        buttonLayout.addView(pasteButton);

        layoutModifyResponse.addView(buttonLayout);
        ScrollView scrollView = new ScrollView(context);
        scrollView.addView(layoutModifyResponse);
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(R.string.modify_response)
                .setView(scrollView)
                .setPositiveButton(R.string.positive_button, (dialog, which) -> {
                    String code = editText.getText().toString();
                    if (!code.equals(script)) {
                        customPreferences.saveSetting("encoded_js_modify_response", Base64.encodeToString(code.getBytes(), Base64.NO_WRAP));
                        Toast.makeText(context.getApplicationContext(), context.getString(R.string.restarting), Toast.LENGTH_SHORT).show();
                        Process.killProcess(Process.myPid());
                        context.startActivity(new Intent().setClassName(Constants.PACKAGE_NAME, "jp.naver.line.android.activity.SplashActivity"));
                    }
                })
                .setNegativeButton(R.string.negative_button, null)
                .setOnDismissListener(dialog -> editText.setText(script));

        AlertDialog dialog = builder.create();
        dialog.show();
    }
    public void CansellNotification(Context context, Context moduleContext) {
        final String TAG = "LimeModule";


        String backupUriStr = loadBackupUri(context);
        if (backupUriStr == null) {
            Toast.makeText(context, "バックアップフォルダURIが未設定です", Toast.LENGTH_SHORT).show();
            XposedBridge.log(TAG + " ⚠ backupUriStr is null");
            return;
        }

        Uri treeUri = Uri.parse(backupUriStr);
        DocumentFile root = DocumentFile.fromTreeUri(context, treeUri);
        if (root == null || !root.exists()) {
            Toast.makeText(context, "バックアップフォルダにアクセスできません", Toast.LENGTH_SHORT).show();
            XposedBridge.log(TAG + " ⚠ root DocumentFile is invalid: " + backupUriStr);
            return;
        }

        // --- Setting フォルダを取得 or 作成 ---
        DocumentFile settingDir = root.findFile("Setting");
        if (settingDir == null || !settingDir.isDirectory()) {
            settingDir = root.createDirectory("Setting");
            if (settingDir == null) {
                Toast.makeText(context, "Settingフォルダの作成に失敗しました", Toast.LENGTH_SHORT).show();
                XposedBridge.log(TAG + " ❌ Failed to create Setting directory");
                return;
            }
        }

        // --- Notification_Setting.txt を取得 or 作成 ---
        DocumentFile settingFile = settingDir.findFile("Notification_Setting.txt");
        if (settingFile == null || !settingFile.isFile()) {
            settingFile = settingDir.createFile("text/plain", "Notification_Setting.txt");
            if (settingFile == null) {
                Toast.makeText(context, "設定ファイルの作成に失敗しました", Toast.LENGTH_SHORT).show();
                XposedBridge.log(TAG + " ❌ Failed to create Notification_Setting.txt");
                return;
            }
        }

        final Uri settingFileUri = settingFile.getUri();

        // --- 既存ペア読み込み ---
        final List<String> existingPairs = new ArrayList<>();
        try (InputStream in = context.getContentResolver().openInputStream(settingFileUri)) {
            if (in != null) {
                try (BufferedReader reader =
                             new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String t = line.trim();
                        if (!t.isEmpty()) {
                            existingPairs.add(t);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "ファイルの読み取りに失敗しました", Toast.LENGTH_SHORT).show();
            XposedBridge.log(TAG + " ❌ Error reading Notification_Setting.txt: " + e);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getString(R.string.NotiFication_Setting));

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);

        final EditText groupNameInput = new EditText(context);
        groupNameInput.setHint(context.getString(R.string.GroupName));
        layout.addView(groupNameInput);

        final EditText userNameInput = new EditText(context);
        userNameInput.setHint(context.getString(R.string.User_name));
        layout.addView(userNameInput);

        Button addButton = new Button(context);
        addButton.setText(context.getString(R.string.Add));
        layout.addView(addButton);

        // ★★★ フォーマットを統一したまま URI 経由で追記する ★★★
        addButton.setOnClickListener(v -> {
            String groupName = groupNameInput.getText().toString();
            String userName = userNameInput.getText().toString();

            String groupLabel = context.getString(R.string.GroupName);
            String userLabel  = context.getString(R.string.User_name);

            // 空グループ名 → "null" で保存したい場合はここで変換してもOK
            if (groupName.isEmpty()) {
                groupName = "null";
            }

            String newPair = groupLabel + ": " + groupName + ", " + userLabel + ": " + userName;

            if (existingPairs.contains(newPair)) {
                Toast.makeText(context, context.getString(R.string.Aleady_Pair), Toast.LENGTH_SHORT).show();
                return;
            }

            try (OutputStream out =
                         context.getContentResolver().openOutputStream(settingFileUri, "wa");
                 BufferedWriter writer =
                         new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {

                writer.write(newPair);
                writer.newLine();
                writer.flush();

                existingPairs.add(newPair);
                Toast.makeText(context, context.getString(R.string.Add_Pair), Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(context, context.getString(R.string.Add_Error_Pair), Toast.LENGTH_SHORT).show();
                XposedBridge.log(TAG + " ❌ Error appending pair: " + e);
            }
        });

        builder.setView(layout);

        // --- 登録済みペア表示＆削除 ---
        builder.setNeutralButton(context.getString(R.string.Registering_Pair), (dialog, which) -> {
            AlertDialog.Builder pairsBuilder = new AlertDialog.Builder(context);
            pairsBuilder.setTitle(context.getString(R.string.Registering_Pair));

            ScrollView scrollView = new ScrollView(context);
            LinearLayout pairsLayout = new LinearLayout(context);
            pairsLayout.setOrientation(LinearLayout.VERTICAL);
            scrollView.addView(pairsLayout);

            // ★ インデックス固定で作る（文字列キャプチャではなく index で扱う）
            for (int i = 0; i < existingPairs.size(); i++) {
                final int index = i;
                final String pairStr = existingPairs.get(i);

                LinearLayout pairLayout = new LinearLayout(context);
                pairLayout.setOrientation(LinearLayout.HORIZONTAL);

                TextView pairTextView = new TextView(context);
                pairTextView.setText(pairStr);
                pairLayout.addView(pairTextView);

                Button deleteButton = new Button(context);
                deleteButton.setText(context.getString(R.string.Delete_Pair));

                deleteButton.setOnClickListener(v -> {

                    if (index >= 0 && index < existingPairs.size()) {
                        existingPairs.remove(index);
                    }
     try (OutputStream out =
                                 context.getContentResolver().openOutputStream(settingFileUri, "rwt");
                         BufferedWriter writer =
                                 new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {

                        for (String remainingPair : existingPairs) {
                            writer.write(remainingPair);
                            writer.newLine();
                        }
                        writer.flush();

                        Toast.makeText(context, context.getString(R.string.Deleted_Pair), Toast.LENGTH_SHORT).show();
                        XposedBridge.log(TAG + " ✅ Deleted pair: " + pairStr);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(context, context.getString(R.string.Error_Deleted_Pair), Toast.LENGTH_SHORT).show();
                        XposedBridge.log(TAG + " ❌ Error rewriting pairs: " + e);
                    }

                    pairsLayout.removeView(pairLayout);
                });


                pairLayout.addView(deleteButton);
                pairsLayout.addView(pairLayout);
            }

            pairsBuilder.setView(scrollView);
            pairsBuilder.setPositiveButton(context.getString(R.string.Close), null);
            pairsBuilder.show();
        });

        builder.setNegativeButton(context.getString(R.string.Cansel), (dialog, which) -> dialog.cancel());

        builder.show();
    }
    public void SettingPhotoSave(Context context, Context moduleContext) {

        boolean dark = isDarkMode(context);
        ThemePalette P = new ThemePalette(dark);

        // ===== SharedPreferences =====
        SharedPreferences prefsNormal =
                context.getSharedPreferences("photosave", Context.MODE_PRIVATE);

        SharedPreferences prefsAlbum =
                context.getSharedPreferences("photosave_album", Context.MODE_PRIVATE);

        String templateNormal =
                prefsNormal.getString(
                        "filename_template",
                        "SenderName-createdTime-Talkname"
                );

        String templateAlbum =
                prefsAlbum.getString(
                        "filename_template",
                        "SenderName-createdTime-Talkname"
                );

        // ===== UI Root =====
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(48, 32, 48, 16);
        root.setBackgroundColor(P.bg);

        // ===== 説明 =====
        TextView desc = new TextView(context);
        desc.setText(
                "保存されるファイル名の形式を指定できます。\n\n" +
                        "使用可能なキーワード（- 区切り）:\n" +
                        "・SenderName\n" +
                        "・createdTime\n" +
                        "・Talkname\n\n" +
                        "例:\nSenderName-createdTime-Talkname"
        );
        desc.setTextColor(P.fg);
        desc.setTextSize(14f);
        root.addView(desc);

        /* =====================================================
         * 通常 PhotoSave
         * ===================================================== */
        TextView labelNormal = new TextView(context);
        labelNormal.setText("\n📷 通常保存");
        labelNormal.setTextColor(P.fg);
        labelNormal.setTextSize(15f);
        root.addView(labelNormal);

        EditText editNormal = new EditText(context);
        editNormal.setText(templateNormal);
        editNormal.setTextColor(P.fg);
        editNormal.setHintTextColor(P.accent);
        editNormal.setHint("SenderName-createdTime-Talkname");
        editNormal.setSingleLine(true);
        editNormal.setBackground(null);
        editNormal.setPadding(16, 24, 16, 24);
        root.addView(editNormal);

        TextView previewNormal = new TextView(context);
        previewNormal.setTextColor(P.accent);
        previewNormal.setTextSize(13f);
        previewNormal.setPadding(0, 16, 0, 0);
        root.addView(previewNormal);

        Runnable updatePreviewNormal = () -> {
            String tpl = editNormal.getText().toString().trim();
            String sample = tpl
                    .replace("SenderName", "Taro")
                    .replace("createdTime", "2025-12-21-01-30")
                    .replace("Talkname", "Family");
            previewNormal.setText("プレビュー:\n" + sample + ".jpg");
        };
        updatePreviewNormal.run();

        editNormal.addTextChangedListener(new SimpleTextWatcher(updatePreviewNormal));

        /* =====================================================
         * Album PhotoSave
         * ===================================================== */
        TextView labelAlbum = new TextView(context);
        labelAlbum.setText("\n🖼 アルバム保存");
        labelAlbum.setTextColor(P.fg);
        labelAlbum.setTextSize(15f);
        root.addView(labelAlbum);

        EditText editAlbum = new EditText(context);
        editAlbum.setText(templateAlbum);
        editAlbum.setTextColor(P.fg);
        editAlbum.setHintTextColor(P.accent);
        editAlbum.setHint("SenderName-createdTime-Talkname");
        editAlbum.setSingleLine(true);
        editAlbum.setBackground(null);
        editAlbum.setPadding(16, 24, 16, 24);
        root.addView(editAlbum);

        TextView previewAlbum = new TextView(context);
        previewAlbum.setTextColor(P.accent);
        previewAlbum.setTextSize(13f);
        previewAlbum.setPadding(0, 16, 0, 0);
        root.addView(previewAlbum);

        Runnable updatePreviewAlbum = () -> {
            String tpl = editAlbum.getText().toString().trim();
            String sample = tpl
                    .replace("SenderName", "Hanako")
                    .replace("createdTime", "2025-12-21-10-05")
                    .replace("Talkname", "album");
            previewAlbum.setText("プレビュー:\n" + sample + ".jpg");
        };
        updatePreviewAlbum.run();

        editAlbum.addTextChangedListener(new SimpleTextWatcher(updatePreviewAlbum));

        // ===== Dialog =====
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("PhotoSave ファイル名設定");
        builder.setView(root);

        builder.setPositiveButton("保存", (d, w) -> {

            String valueNormal = editNormal.getText().toString().trim();
            String valueAlbum = editAlbum.getText().toString().trim();

            if (!valueNormal.isEmpty()) {
                prefsNormal.edit()
                        .putString("filename_template", valueNormal)
                        .commit();
            }

            if (!valueAlbum.isEmpty()) {
                prefsAlbum.edit()
                        .putString("filename_template", valueAlbum)
                        .commit();
            }

            XposedBridge.log("Lime: PhotoSave templates saved"
                    + " normal=" + valueNormal
                    + " album=" + valueAlbum);
        });

        builder.setNegativeButton("キャンセル", null);

        AlertDialog dialog = builder.create();
        dialog.show();

        applyDialogPalette(dialog, P);

        try {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(P.fg);
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(P.fg);
        } catch (Throwable ignored) {}
    }

    private static class SimpleTextWatcher implements TextWatcher {
        private final Runnable onChange;

        SimpleTextWatcher(Runnable r) {
            this.onChange = r;
        }

        @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
        @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
            onChange.run();
        }
        @Override public void afterTextChanged(Editable s) {}
    }

    private void Cancel_Message_Button(Context context, Context moduleContext) {


        boolean dark = isDarkMode(context);
        ThemePalette P = new ThemePalette(dark);

        File dir = new File(context.getFilesDir(), "LimeBackup/Setting");
        if (!dir.exists() && !dir.mkdirs()) {
            Toast.makeText(context, "Failed to create directory", Toast.LENGTH_SHORT).show();
            return;
        }

        File file = new File(dir, "canceled_message.txt");

        if (!file.exists()) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                writer.write(moduleContext.getString(R.string.canceled_message_txt));
            } catch (IOException e) {
                Toast.makeText(context, "Failed to create file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }
        }

        StringBuilder fileContent = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                fileContent.append(line).append("\n");
            }
        } catch (IOException e) {
            Toast.makeText(context, "Failed to read file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }

        final EditText editText = new EditText(context);
        editText.setText(fileContent.toString().trim());
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        editText.setMinLines(10);
        editText.setGravity(Gravity.TOP);
        editText.setTextColor(P.fg);
        editText.setHintTextColor(P.accent);
        editText.setBackgroundColor(P.btnBg);

        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        buttonParams.setMargins(16, 16, 16, 16);


        Button saveButton = new Button(context);
        saveButton.setText(moduleContext.getString(R.string.options_title));
        saveButton.setLayoutParams(buttonParams);
        saveButton.setBackgroundColor(P.btnBg);
        saveButton.setTextColor(P.fg);

        saveButton.setOnClickListener(v -> {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                writer.write(editText.getText().toString());
                Toast.makeText(context, "Saved successfully", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                Toast.makeText(context, "Failed to save file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });


        Button hideCanceledMessageButton = new Button(context);
        hideCanceledMessageButton.setText(moduleContext.getString(R.string.hide_canceled_message));
        hideCanceledMessageButton.setLayoutParams(buttonParams);
        hideCanceledMessageButton.setBackgroundColor(P.btnBg);
        hideCanceledMessageButton.setTextColor(P.fg);

        hideCanceledMessageButton.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle(moduleContext.getString(R.string.HideSetting))
                    .setMessage(moduleContext.getString(R.string.HideSetting_selection))
                    .setPositiveButton(moduleContext.getString(R.string.Hide), (dialog, which) -> {
                        updateMessagesVisibility(context, true, moduleContext);
                    })
                    .setNegativeButton(moduleContext.getString(R.string.Show), (dialog, which) -> {
                        updateMessagesVisibility(context, false, moduleContext);
                    })
                    .setIcon(android.R.drawable.ic_dialog_info)
                    .show();
        });


        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(P.bg);

        layout.addView(editText);
        layout.addView(saveButton);
        layout.addView(hideCanceledMessageButton);


        ScrollView scrollView = new ScrollView(context);
        scrollView.setBackgroundColor(P.bg);
        scrollView.addView(layout);

        AlertDialog dialog = new AlertDialog.Builder(
                context,
                dark
                        ? android.R.style.Theme_DeviceDefault_Dialog_NoActionBar
                        : android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar
        )
                .setTitle(moduleContext.getString(R.string.canceled_message))
                .setView(scrollView)
                .setNegativeButton(moduleContext.getString(R.string.cancel), null)
                .create();

        dialog.show();
        applyDialogPalette(dialog, P);
    }


    public int getStatusBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }
    private void KeepUnread_Button(Context context, Context moduleContext) {

        boolean dark = isDarkMode(context);
        ThemePalette P = new ThemePalette(dark);

        File dir = new File(context.getFilesDir(), "LimeBackup/Setting");
        if (!dir.exists()) dir.mkdirs();
        File file = new File(dir, "margin_settings.txt");

        float keep_unread_size = 100;
        float chat_unread_size = 30.0f;
        float chat_read_check_size = 80.0f;
        float reaction_size = 100.0f;
        float header_setting_size = 60.0f;

        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split("=", 2);
                    if (parts.length == 2) {
                        switch (parts[0].trim()) {
                            case "keep_unread_size":
                                keep_unread_size = Float.parseFloat(parts[1].trim());
                                break;
                            case "chat_unread_size":
                                chat_unread_size = Float.parseFloat(parts[1].trim());
                                break;
                            case "chat_read_check_size":
                                chat_read_check_size = Float.parseFloat(parts[1].trim());
                                break;
                            case "reaction_size":
                                reaction_size = Float.parseFloat(parts[1].trim());
                                break;
                            case "header_setting_size":
                                header_setting_size = Float.parseFloat(parts[1].trim());
                                break;
                        }
                    }
                }
            } catch (IOException | NumberFormatException ignored) {}
        }

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(16, 16, 16, 16);

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(P.bg);

        class Style {
            void tv(TextView tv) {
                tv.setTextColor(P.fg);
                tv.setBackgroundColor(Color.TRANSPARENT);
            }

            void et(EditText et) {
                et.setTextColor(P.fg);
                et.setHintTextColor(P.accent);
                et.setBackgroundColor(P.btnBg);
            }

            void btn(Button b) {
                b.setTextColor(P.fg);
                b.setBackgroundColor(P.btnBg);
            }
        }
        Style style = new Style();

        TextView keepUnreadSizeLabel = new TextView(context);
        keepUnreadSizeLabel.setText(moduleContext.getString(R.string.keep_unread_size));
        keepUnreadSizeLabel.setLayoutParams(layoutParams);
        style.tv(keepUnreadSizeLabel);

        EditText keepUnreadSizeInput = new EditText(context);
        keepUnreadSizeInput.setText(String.valueOf(keep_unread_size));
        keepUnreadSizeInput.setLayoutParams(layoutParams);
        style.et(keepUnreadSizeInput);

        TextView chatUnreadLabel = new TextView(context);
        chatUnreadLabel.setText(moduleContext.getString(R.string.chat_unread_size));
        chatUnreadLabel.setLayoutParams(layoutParams);
        style.tv(chatUnreadLabel);

        EditText chatUnreadSizeInput = new EditText(context);
        chatUnreadSizeInput.setText(String.valueOf(chat_unread_size));
        chatUnreadSizeInput.setLayoutParams(layoutParams);
        style.et(chatUnreadSizeInput);

        TextView chatReadCheckSizeLabel = new TextView(context);
        chatReadCheckSizeLabel.setText(moduleContext.getString(R.string.chat_read_check_size));
        chatReadCheckSizeLabel.setLayoutParams(layoutParams);
        style.tv(chatReadCheckSizeLabel);

        EditText chatReadCheckSizeInput = new EditText(context);
        chatReadCheckSizeInput.setText(String.valueOf(chat_read_check_size));
        chatReadCheckSizeInput.setLayoutParams(layoutParams);
        style.et(chatReadCheckSizeInput);

        TextView reactionSizeLabel = new TextView(context);
        reactionSizeLabel.setText(moduleContext.getString(R.string.reaction_sizeLabel));
        reactionSizeLabel.setLayoutParams(layoutParams);
        style.tv(reactionSizeLabel);

        EditText reactionSizeInput = new EditText(context);
        reactionSizeInput.setText(String.valueOf(reaction_size));
        reactionSizeInput.setLayoutParams(layoutParams);
        style.et(reactionSizeInput);

        TextView header_setting_Label = new TextView(context);
        header_setting_Label.setText(moduleContext.getString(R.string.header_setting_Label));
        header_setting_Label.setLayoutParams(layoutParams);
        style.tv(header_setting_Label);

        EditText header_setting_size_SizeInput = new EditText(context);
        header_setting_size_SizeInput.setText(String.valueOf(header_setting_size));
        header_setting_size_SizeInput.setLayoutParams(layoutParams);
        style.et(header_setting_size_SizeInput);

        class PrefConfig {
            String prefName;
            String label;
            String keyX;
            String keyY;
            PrefConfig(String p, String l, String x, String y) {
                prefName = p; label = l; keyX = x; keyY = y;
            }
        }

        PrefConfig[] configs = new PrefConfig[]{
                new PrefConfig("reaction_button_position", "Reaction Button", "btn_x", "btn_y"),
                new PrefConfig("schedule_btn_pref", "Schedule Button", "btn_x", "btn_y"),
                new PrefConfig("keep_unread_image_position", "Keep Unread Image", "img_x", "img_y"),
                new PrefConfig("keep_unread_switch_position", "Keep Unread Switch", "switch_x", "switch_y"),
                new PrefConfig("chat_unread_button_position", "Chat Unread Button", "img_x", "img_y"),
                new PrefConfig("ReadCheck_button_position", "ReadCheck Button", "img_x", "img_y"),
                new PrefConfig("delete_button_position", "Delete Button", "btn_x", "btn_y"),
                new PrefConfig("enter_send_pref_IncChat", "Enter Send Button", "btn_x", "btn_y"),
                new PrefConfig("HeaderSetting_button_position", "Header Setting Button", "img_x", "img_y")
        };

        Map<String, EditText> inputMap = new HashMap<>();

        for (PrefConfig cfg : configs) {
            LinearLayout sectionLayout = new LinearLayout(context);
            sectionLayout.setOrientation(LinearLayout.VERTICAL);
            sectionLayout.setLayoutParams(layoutParams);
            sectionLayout.setBackgroundColor(P.bg);

            TextView titleView = new TextView(context);
            titleView.setText("▼ " + cfg.label);
            titleView.setTextSize(18);
            titleView.setPadding(16, 16, 16, 16);
            titleView.setBackgroundColor(P.btnBg);
            titleView.setTextColor(P.fg);
            sectionLayout.addView(titleView);

            LinearLayout contentLayout = new LinearLayout(context);
            contentLayout.setOrientation(LinearLayout.VERTICAL);
            contentLayout.setVisibility(View.GONE);
            contentLayout.setBackgroundColor(P.bg);

            SharedPreferences sp = context.getSharedPreferences(cfg.prefName, Context.MODE_PRIVATE);
            float savedX = sp.getFloat(cfg.keyX, -1f);
            float savedY = sp.getFloat(cfg.keyY, -1f);

            TextView labelX = new TextView(context);
            labelX.setText(cfg.label + " X Position");
            labelX.setLayoutParams(layoutParams);
            style.tv(labelX);

            EditText inputX = new EditText(context);
            inputX.setText(String.valueOf(savedX));
            inputX.setLayoutParams(layoutParams);
            style.et(inputX);

            TextView labelY = new TextView(context);
            labelY.setText(cfg.label + " Y Position");
            labelY.setLayoutParams(layoutParams);
            style.tv(labelY);

            EditText inputY = new EditText(context);
            inputY.setText(String.valueOf(savedY));
            inputY.setLayoutParams(layoutParams);
            style.et(inputY);

            switch (cfg.label) {
                case "Reaction Button":
                    contentLayout.addView(reactionSizeLabel);
                    contentLayout.addView(reactionSizeInput);
                    break;
                case "Keep Unread Image":
                    contentLayout.addView(keepUnreadSizeLabel);
                    contentLayout.addView(keepUnreadSizeInput);
                    break;
                case "Chat Unread Button":
                    contentLayout.addView(chatUnreadLabel);
                    contentLayout.addView(chatUnreadSizeInput);
                    break;
                case "ReadCheck Button":
                    contentLayout.addView(chatReadCheckSizeLabel);
                    contentLayout.addView(chatReadCheckSizeInput);
                    break;
                case "Header Setting Button":
                    contentLayout.addView(header_setting_Label);
                    contentLayout.addView(header_setting_size_SizeInput);
                    break;
            }

            contentLayout.addView(labelX);
            contentLayout.addView(inputX);
            contentLayout.addView(labelY);
            contentLayout.addView(inputY);

            titleView.setOnClickListener(v -> {
                if (contentLayout.getVisibility() == View.VISIBLE) {
                    contentLayout.setVisibility(View.GONE);
                    titleView.setText("▼ " + cfg.label);
                } else {
                    contentLayout.setVisibility(View.VISIBLE);
                    titleView.setText("▲ " + cfg.label);
                }
            });

            inputMap.put(cfg.prefName + "_X", inputX);
            inputMap.put(cfg.prefName + "_Y", inputY);

            sectionLayout.addView(contentLayout);
            layout.addView(sectionLayout);
        }

        Button saveButton = new Button(context);
        saveButton.setText("Save");
        saveButton.setLayoutParams(layoutParams);
        style.btn(saveButton);

        saveButton.setOnClickListener(v -> {
            try {
                float newKeepUnreadSize = Float.parseFloat(keepUnreadSizeInput.getText().toString().trim());
                float newChatUnreadSize = Float.parseFloat(chatUnreadSizeInput.getText().toString().trim());
                float newChatReadCheckSize = Float.parseFloat(chatReadCheckSizeInput.getText().toString().trim());
                float newReactionSize = Float.parseFloat(reactionSizeInput.getText().toString().trim());
                float newHeaderSettingSize = Float.parseFloat(header_setting_size_SizeInput.getText().toString().trim());

                try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                    writer.write("keep_unread_size=" + newKeepUnreadSize + "\n");
                    writer.write("chat_unread_size=" + newChatUnreadSize + "\n");
                    writer.write("chat_read_check_size=" + newChatReadCheckSize + "\n");
                    writer.write("reaction_size=" + newReactionSize + "\n");
                    writer.write("header_setting_size=" + newHeaderSettingSize + "\n");
                    writer.flush();
                }

                for (PrefConfig cfg : configs) {
                    EditText inputX = inputMap.get(cfg.prefName + "_X");
                    EditText inputY = inputMap.get(cfg.prefName + "_Y");
                    if (inputX != null && inputY != null) {
                        float newX = Float.parseFloat(inputX.getText().toString().trim());
                        float newY = Float.parseFloat(inputY.getText().toString().trim());
                        SharedPreferences sp = context.getSharedPreferences(cfg.prefName, Context.MODE_PRIVATE);
                        sp.edit().putFloat(cfg.keyX, newX).putFloat(cfg.keyY, newY).apply();
                    }
                }

                Toast.makeText(context, "Settings saved!", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        Button resetButton = new Button(context);
        resetButton.setText("Reset");
        resetButton.setLayoutParams(layoutParams);
        style.btn(resetButton);

        resetButton.setOnClickListener(v -> new AlertDialog.Builder(context)
                .setMessage(moduleContext.getString(R.string.really_delete))
                .setPositiveButton(moduleContext.getString(R.string.yes), (d, w) -> {
                    try {
                        if (file.exists()) file.delete();
                        for (PrefConfig cfg : configs) {
                            SharedPreferences sp = context.getSharedPreferences(cfg.prefName, Context.MODE_PRIVATE);
                            sp.edit().clear().apply();
                        }
                        Toast.makeText(context, "All settings reset!", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(context, "Reset failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(moduleContext.getString(R.string.no), null)
                .show());

        layout.addView(saveButton);
        layout.addView(resetButton);

        ScrollView scrollView = new ScrollView(context);
        scrollView.setBackgroundColor(P.bg);
        scrollView.addView(layout);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(moduleContext.getString(R.string.edit_margin_settings))
                .setView(scrollView)
                .setNegativeButton(moduleContext.getString(R.string.cancel), null)
                .create();

        dialog.show();
        applyDialogPalette(dialog, P);
    }


    private void MuteGroups_Button(Context context, Context moduleContext) {
        final String TAG = "LimeModule";
        String backupUriStr = loadBackupUri(context);
        if (backupUriStr == null) {
            Toast.makeText(context, "バックアップフォルダURIが未設定です", Toast.LENGTH_SHORT).show();
            // XposedBridge.log(TAG + " ⚠ backupUriStr is null in MuteGroups_Button");
            return;
        }

        Uri treeUri = Uri.parse(backupUriStr);
        DocumentFile root = DocumentFile.fromTreeUri(context, treeUri);
        if (root == null || !root.exists()) {
            Toast.makeText(context, "バックアップフォルダにアクセスできません", Toast.LENGTH_SHORT).show();
            // XposedBridge.log(TAG + " ⚠ root DocumentFile is invalid in MuteGroups_Button: " + backupUriStr);
            return;
        }



        DocumentFile settingDir = root.findFile("Setting");
        if (settingDir == null || !settingDir.isDirectory()) {
            settingDir = root.createDirectory("Setting");
            if (settingDir == null) {
                Toast.makeText(context, "Settingフォルダの作成に失敗しました", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        DocumentFile notifFile = settingDir.findFile("Notification.txt");
        if (notifFile == null || !notifFile.isFile()) {
            notifFile = settingDir.createFile("text/plain", "Notification.txt");
            if (notifFile == null) {
                Toast.makeText(context, "Notification.txt の作成に失敗しました", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        Uri notifUri = notifFile.getUri();
        StringBuilder fileContent = new StringBuilder();
        try (InputStream in = context.getContentResolver().openInputStream(notifUri)) {
            if (in != null) {
                try (BufferedReader reader =
                             new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        fileContent.append(line).append("\n");
                    }
                }
            }
        } catch (Exception e) {
            Log.e("MuteGroups_Button", "Error reading Notification.txt", e);
            // XposedBridge.log(TAG + " ❌ Error reading Notification.txt: " + e);
        }

        final EditText editText = new EditText(context);
        editText.setText(fileContent.toString());
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        editText.setMinLines(10);
        editText.setGravity(Gravity.TOP);

        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        buttonParams.setMargins(16, 16, 16, 16);

        Button saveButton = new Button(context);
        saveButton.setText("Save");
        saveButton.setLayoutParams(buttonParams);

        
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try (OutputStream out =
                             context.getContentResolver().openOutputStream(notifUri, "w");
                     BufferedWriter writer =
                             new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {

                    writer.write(editText.getText().toString());
                    writer.flush();
                    Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                }
            }
        });

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(editText);
        layout.addView(saveButton);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(moduleContext.getResources().getString(R.string.Mute_Group));
        builder.setView(layout);
        builder.setNegativeButton(moduleContext.getResources().getString(R.string.cancel), null);
        builder.show();
    }

    private void backupChatHistory(Context appContext, Context moduleContext) {
        File originalDbFile = appContext.getDatabasePath("naver_line");

        File backupDir = new File(appContext.getFilesDir(), "LimeBackup");
        if (!backupDir.exists()) {
            if (!backupDir.mkdirs()) {
                return;
            }
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String backupFileNameWithTimestamp = "naver_line_backup_" + timeStamp + ".db";
        String backupFileNameFixed = "naver_line_backup.db";
        File backupFileWithTimestamp = new File(backupDir, backupFileNameWithTimestamp);
        File backupFileFixed = new File(backupDir, backupFileNameFixed);

        try (FileChannel source = new FileInputStream(originalDbFile).getChannel()) {
            try (FileChannel destinationWithTimestamp = new FileOutputStream(backupFileWithTimestamp).getChannel()) {
                destinationWithTimestamp.transferFrom(source, 0, source.size());
            }
            source.position(0);
            try (FileChannel destinationFixed = new FileOutputStream(backupFileFixed).getChannel()) {
                destinationFixed.transferFrom(source, 0, source.size());
            }

            String backupUri = loadBackupUri(appContext);
            if (backupUri != null) {
                Uri treeUri = Uri.parse(backupUri);
                DocumentFile dir = DocumentFile.fromTreeUri(appContext, treeUri);
                if (dir != null) {
                    copyFileToUri(appContext, backupFileWithTimestamp, dir, backupFileNameWithTimestamp);
                    copyFileToUri(appContext, backupFileFixed, dir, backupFileNameFixed);
                }
            }

            Toast.makeText(appContext, moduleContext.getResources().getString(R.string.Talk_Back_up_Success), Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(appContext, moduleContext.getResources().getString(R.string.Talk_Back_up_Error), Toast.LENGTH_SHORT).show();
        }
    }

    private void copyFileToUri(Context context, File sourceFile, DocumentFile destinationDir, String destinationFileName) {
        try {
            DocumentFile existingFile = destinationDir.findFile(destinationFileName);
            if (existingFile != null) {
                existingFile.delete();
            }

            DocumentFile newFile = destinationDir.createFile("application/x-sqlite3", destinationFileName);
            if (newFile == null) return;

            try (InputStream in = new FileInputStream(sourceFile);
                 OutputStream out = context.getContentResolver().openOutputStream(newFile.getUri())) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = in.read(buffer)) > 0) {
                    out.write(buffer, 0, length);
                }
            }
        } catch (IOException e) {
            XposedBridge.log("Lime: Error copying DB to URI: " + e.getMessage());
        }
    }
    private static void resetFcmAppId(Context context) {
        try {
            boolean deleted = context.deleteSharedPreferences(
                    "com.google.android.gms.appid"
            );
            boolean deleted2 = context.deleteSharedPreferences(
                    "com.google.firebase.messaging"
            );
            XposedBridge.log(
                    "LIMEs[FCM]: delete com.google.android.gms.appid -> " + deleted + deleted2
            );

            Toast.makeText(
                    context,
                    "FCM AppID を削除しました。\n再起動後に再取得されます。",
                    Toast.LENGTH_LONG
            ).show();

        } catch (Throwable t) {
            XposedBridge.log(t);

            Toast.makeText(
                    context,
                    "FCM AppID 削除に失敗しました",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void updateMessagesVisibility(Context context, boolean hide,Context moduleContext) {
        SQLiteDatabase db1 = null;
        try {
            db1 = context.openOrCreateDatabase("naver_line", Context.MODE_PRIVATE, null);

            if (hide) {
                db1.execSQL(
                        "UPDATE chat_history " +
                                "SET chat_id = '/' || chat_id " +
                                "WHERE parameter = 'LIMEsUnsend' " +
                                "AND chat_id NOT LIKE '/%'"
                );
                Toast.makeText(context, moduleContext.getResources().getString(R.string.Hiden_setting), Toast.LENGTH_SHORT).show();
            } else {
                db1.execSQL(
                        "UPDATE chat_history " +
                                "SET chat_id = LTRIM(chat_id, '/') " +
                                "WHERE parameter = 'LIMEsUnsend' " +
                                "AND chat_id LIKE '/%'"
                );
                Toast.makeText(context,  moduleContext.getResources().getString(R.string.Show_setting), Toast.LENGTH_SHORT).show();
            }
        } catch (SQLException e) {
            Log.e("DatabaseError", "Update failed: " + e.getMessage());
            Toast.makeText(context, moduleContext.getResources().getString(R.string.Setting_Error), Toast.LENGTH_SHORT).show();
        } finally {
            if (db1 != null) {
                db1.close();
            }
        }
    }


    private static class UserEntry {
        String userId;
        String userName;
        transient EditText inputView;

        UserEntry(String userId, String userName) {
            this.userId = userId;
            this.userName = userName;
        }
    }


    private void PinListButton(Context context, Context moduleContext) {
        List<UserEntry> userEntries = new ArrayList<>();
        Map<String, String> existingSettings = loadExistingSettings(context);

        try (SQLiteDatabase chatListDb = context.openOrCreateDatabase("naver_line", Context.MODE_PRIVATE, null);
             SQLiteDatabase profileDb = context.openOrCreateDatabase("contact", Context.MODE_PRIVATE, null);
             Cursor chatCursor = chatListDb.rawQuery("SELECT chat_id FROM chat WHERE is_archived = 0", null)) {

            if (chatCursor != null && chatCursor.getColumnIndex("chat_id") != -1) {
                while (chatCursor.moveToNext()) {
                    String chatId = chatCursor.getString(chatCursor.getColumnIndex("chat_id"));
                    String profileName = getProfileNameFromContacts(profileDb, chatId);
                    if ("Unknown".equals(profileName)) {
                        profileName = getGroupNameFromGroups(chatListDb, chatId);
                    }
                    userEntries.add(new UserEntry(chatId, profileName));
                }
            }

        } catch (SQLiteException e) {
            // XposedBridge.log("SQL Error: " + e.getMessage());
            Toast.makeText(context, "データ取得エラー", Toast.LENGTH_SHORT).show();
            return;
        }

        ScrollView scrollView = new ScrollView(context);
        LinearLayout mainLayout = new LinearLayout(context);
        mainLayout.setOrientation(LinearLayout.VERTICAL);

        int padding = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                16,
                context.getResources().getDisplayMetrics()
        );

        LinearLayout contentLayout = new LinearLayout(context);
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        contentLayout.setPadding(padding, padding, padding, padding);

        for (UserEntry entry : userEntries) {
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));

            TextView userNameView = new TextView(context);
            userNameView.setText(entry.userName);
            LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
            );
            textParams.setMargins(0, 0, padding, 0);
            userNameView.setLayoutParams(textParams);

            EditText inputNumber = new EditText(context);
            inputNumber.setInputType(InputType.TYPE_CLASS_NUMBER);
            inputNumber.setLayoutParams(new LinearLayout.LayoutParams(
                    (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 120, context.getResources().getDisplayMetrics()),
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));

            if (existingSettings.containsKey(entry.userId)) {
                inputNumber.setText(existingSettings.get(entry.userId));
            }

            entry.inputView = inputNumber;
            row.addView(userNameView);
            row.addView(inputNumber);
            contentLayout.addView(row);
        }

        scrollView.addView(contentLayout);
        mainLayout.addView(scrollView);

        new AlertDialog.Builder(context)
                .setTitle(moduleContext.getString(R.string.UserSet))
                .setView(mainLayout)
                .setPositiveButton(moduleContext.getString(R.string.save), (dialog, which) -> saveUserData(context, userEntries,moduleContext))
                .setNegativeButton(moduleContext.getString(R.string.cancel), null)
                .show();
    }


    private Map<String, String> loadExistingSettings(Context context) {
        Map<String, String> settingsMap = new HashMap<>();

        String backupUriStr = loadBackupUri(context);
        if (backupUriStr == null) {
            return settingsMap;
        }

        Uri treeUri = Uri.parse(backupUriStr);
        DocumentFile baseDir = DocumentFile.fromTreeUri(context, treeUri);
        if (baseDir == null || !baseDir.exists()) {
            return settingsMap;
        }

        DocumentFile limeBackupDir = baseDir.findFile("LimeBackup");
        if (limeBackupDir == null) return settingsMap;
        DocumentFile settingDir = limeBackupDir.findFile("Setting");
        if (settingDir == null) return settingsMap;

        DocumentFile settingFile = settingDir.findFile("ChatList.txt");
        if (settingFile == null || !settingFile.exists()) {
            return settingsMap;
        }

        try (InputStream is = context.getContentResolver().openInputStream(settingFile.getUri());
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {

            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",", 3);
                if (parts.length == 3) {
                    settingsMap.put(parts[0].trim(), parts[2].trim()); // userId → number
                }
            }
        } catch (IOException e) {
            // XposedBridge.log("設定読み込みエラー: " + e.getMessage());
        }

        return settingsMap;
    }

    private void saveUserData(Context context, List<UserEntry> entries, Context moduleContext) {
        String backupUriStr = loadBackupUri(context);
        if (backupUriStr == null) {
            Toast.makeText(context, "バックアップフォルダが設定されていません", Toast.LENGTH_SHORT).show();
            return;
        }

        Uri treeUri = Uri.parse(backupUriStr);
        DocumentFile baseDir = DocumentFile.fromTreeUri(context, treeUri);
        if (baseDir == null || !baseDir.exists()) {
            Toast.makeText(context, "バックアップフォルダが見つかりません", Toast.LENGTH_SHORT).show();
            return;
        }

        DocumentFile limeBackupDir = baseDir.findFile("LimeBackup");
        if (limeBackupDir == null) {
            limeBackupDir = baseDir.createDirectory("LimeBackup");
        }

        DocumentFile settingDir = limeBackupDir.findFile("Setting");
        if (settingDir == null) {
            settingDir = limeBackupDir.createDirectory("Setting");
        }

        if (settingDir == null) {
            Toast.makeText(context, "Settingディレクトリの作成に失敗しました", Toast.LENGTH_SHORT).show();
            return;
        }

        DocumentFile outputFile = settingDir.findFile("ChatList.txt");
        if (outputFile != null && outputFile.exists()) {
            outputFile.delete();
        }
        outputFile = settingDir.createFile("text/plain", "ChatList.txt");

        try (OutputStream os = context.getContentResolver().openOutputStream(outputFile.getUri());
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os))) {
            for (UserEntry entry : entries) {
                String number = entry.inputView.getText().toString().trim();
                if (!number.isEmpty()) {
                    writer.write(entry.userId + "," + entry.userName + "," + number);
                    writer.newLine();
                }
            }
            Toast.makeText(context, "保存しました", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(context, "保存に失敗しました", Toast.LENGTH_SHORT).show();
        }
    }
    private String getProfileNameFromContacts(SQLiteDatabase db, String contactMid) {
        try (Cursor cursor = db.rawQuery("SELECT profile_name FROM contacts WHERE mid=?", new String[]{contactMid})) {
            return cursor.moveToFirst() ? cursor.getString(0) : "Unknown";
        } catch (SQLiteException e) {
            // XposedBridge.log("プロファイル取得エラー: " + e.getMessage());
            return "Unknown";
        }
    }

    private String getGroupNameFromGroups(SQLiteDatabase db, String groupId) {
        try (Cursor cursor = db.rawQuery("SELECT name FROM groups WHERE id=?", new String[]{groupId})) {
            return cursor.moveToFirst() ? cursor.getString(0) : "Unknown";
        } catch (SQLiteException e) {
            // XposedBridge.log("グループ名取得エラー: " + e.getMessage());
            return "Unknown";
        }
    }

    private static class UserEntry2 {
        String userId;      // chatId
        String userName;    // 表示名
        transient EditText inputView;  
        transient CheckBox checkBox; 

        UserEntry2(String userId, String userName) {
            this.userId = userId;
            this.userName = userName;
        }
    }

    private static final String CHECKED_CHAT_FILE_NAME = "CheckedChats.txt";

    private File getSettingDir(Context context) {
        File base = context.getFilesDir();
        File limeDir = new File(base, "LimeBackup/Setting");
        if (!limeDir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            limeDir.mkdirs();
        }
        return limeDir;
    }

    private Set<String> loadCheckedChatIds(Context context) {
        Set<String> result = new HashSet<>();
        try {
            File dir = getSettingDir(context);
            File file = new File(dir, CHECKED_CHAT_FILE_NAME);
            if (!file.exists()) return result;

            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty()) {
                        result.add(line);
                    }
                }
            }
        } catch (Exception e) {
            XposedBridge.log("Lime: loadCheckedChatIds error: " + e.getMessage());
        }
        return result;
    }

    private void saveCheckedChatIds(Context context, Set<String> checkedIds) {
        try {
            File dir = getSettingDir(context);
            File file = new File(dir, CHECKED_CHAT_FILE_NAME);

            try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, false))) {
                for (String id : checkedIds) {
                    bw.write(id);
                    bw.newLine();
                }
            }
        } catch (Exception e) {
            XposedBridge.log("Lime: saveCheckedChatIds error: " + e.getMessage());
        }
    }

    private List<UserEntry2> buildUserEntries(Context context) {
        List<UserEntry2> userEntries = new ArrayList<>();

        try (SQLiteDatabase chatListDb = context.openOrCreateDatabase("naver_line", Context.MODE_PRIVATE, null);
             SQLiteDatabase profileDb = context.openOrCreateDatabase("contact", Context.MODE_PRIVATE, null);
             Cursor chatCursor = chatListDb.rawQuery("SELECT chat_id FROM chat WHERE is_archived = 0", null)) {

            if (chatCursor != null && chatCursor.getColumnIndex("chat_id") != -1) {
                int col = chatCursor.getColumnIndex("chat_id");
                while (chatCursor.moveToNext()) {
                    String chatId = chatCursor.getString(col);
                    String profileName = getProfileNameFromContacts(profileDb, chatId);
                    if ("Unknown".equals(profileName)) {
                        profileName = getGroupNameFromGroups(chatListDb, chatId);
                    }
                    userEntries.add(new UserEntry2(chatId, profileName));
                }
            }
        } catch (Exception e) {
            XposedBridge.log("Lime: buildUserEntries error: " + e.getMessage());
        }

        return userEntries;
    }
    static final class ThemePalette {
        final int bg;       // 背景
        final int fg;       // 文字
        final int btnBg;    // ボタン背景
        final int divider;  // 仕切り線
        final int accent;   // 強調

        ThemePalette(boolean dark) {
            if (dark) {
                bg = Color.parseColor("#000000"); // 既存のダーク背景
                fg = Color.WHITE;
                btnBg = Color.parseColor("#000000");
                divider = Color.DKGRAY;
                accent = Color.LTGRAY;
            } else {
                bg = Color.WHITE;
                fg = Color.BLACK;
                btnBg = Color.parseColor("#F2F2F7");
                divider = Color.LTGRAY;
                accent = Color.DKGRAY;
            }
        }
    }



    private static void applyDialogPalette(AlertDialog dialog, ThemePalette P) {
        try {
            Window window = dialog.getWindow();
            if (window != null) {

                window.setBackgroundDrawable(new ColorDrawable(P.bg));
                View root = window.findViewById(android.R.id.content);
                if (root != null) {
                    root.setBackgroundColor(P.bg);
                }
            }
        } catch (Throwable t) {
            XposedBridge.log("Lime: applyDialogPalette error: " + t.getMessage());
        }
    }

    static boolean isDarkMode(Context ctx) {
        int m = ctx.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return m == Configuration.UI_MODE_NIGHT_YES;
    }
    private void showChatCheckListDialog(Activity activity2, Context context) {

        boolean dark = isDarkMode(activity2);
        ThemePalette P = new ThemePalette(dark);

        List<UserEntry2> allEntries = buildUserEntries(context);
        Set<String> checkedIdSet = loadCheckedChatIds(context);

        LinearLayout root = new LinearLayout(activity2);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 24, 24, 24);
        if (P != null) root.setBackgroundColor(P.bg);

        EditText searchBox = new EditText(activity2);
        searchBox.setHint("名前 / ID で検索");
        if (P != null) {
            searchBox.setTextColor(P.fg);
            searchBox.setHintTextColor(Color.GRAY);
        }
        root.addView(searchBox,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));

        ScrollView scroll = new ScrollView(activity2);
        if (P != null) scroll.setBackgroundColor(P.bg);
        LinearLayout listContainer = new LinearLayout(activity2);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        if (P != null) listContainer.setBackgroundColor(P.bg);
        scroll.addView(listContainer,
                new ScrollView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(scroll,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        0,
                        1f));

        for (UserEntry2 entry : allEntries) {
            CheckBox cb = new CheckBox(activity2);
            entry.checkBox = cb;
            cb.setText(entry.userName + "  (" + entry.userId + ")");
            cb.setTag(entry);  
            
            cb.setChecked(checkedIdSet.contains(entry.userId));

            if (P != null) {
                cb.setTextColor(P.fg);
            }

            listContainer.addView(cb);

        }

        searchBox.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String q = s.toString().toLowerCase();
                int childCount = listContainer.getChildCount();
                for (int i = 0; i < childCount; i++) {
                    View v = listContainer.getChildAt(i);
                    if (!(v instanceof CheckBox)) continue;
                    CheckBox cb = (CheckBox) v;
                    UserEntry2 entry = (UserEntry2) cb.getTag();
                    String name = entry.userName != null ? entry.userName.toLowerCase() : "";
                    String id = entry.userId != null ? entry.userId.toLowerCase() : "";

                    boolean visible = name.contains(q) || id.contains(q);
                    cb.setVisibility(visible ? View.VISIBLE : View.GONE);
                }
            }
        });

        AlertDialog dialog = new AlertDialog.Builder(activity2,
                dark
                        ? android.R.style.Theme_DeviceDefault_Dialog_NoActionBar
                        : android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar)
                .setTitle("対象トーク選択")
                .setView(root)
                .setPositiveButton("OK", (d, w) -> {

                    Set<String> newChecked = new HashSet<>();
                    int childCount = listContainer.getChildCount();
                    for (int i = 0; i < childCount; i++) {
                        View v = listContainer.getChildAt(i);
                        if (!(v instanceof CheckBox)) continue;
                        CheckBox cb = (CheckBox) v;
                        if (!cb.isChecked()) continue;
                        UserEntry2 entry = (UserEntry2) cb.getTag();
                        if (entry != null && entry.userId != null) {
                            newChecked.add(entry.userId);
                        }
                    }
                    saveCheckedChatIds(context, newChecked);
                    Toast.makeText(activity2, "保存しました (" + newChecked.size() + " 件)", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("キャンセル", null)
                .create();

        dialog.show();
        applyDialogPalette(dialog, P);
    }

    private void Blocklist(Context context, Context moduleContext) {

        boolean dark = isDarkMode(context);
        ThemePalette P = new ThemePalette(dark);

        new ProfileManager(P).showProfileManagement(context, moduleContext);
    }

    private static class ProfileInfo {
        final String contactMid;
        final String profileName;

        ProfileInfo(String contactMid, String profileName) {
            this.contactMid = contactMid;
            this.profileName = profileName;
        }
    }

    public class ProfileManager {
        private HiddenProfileManager hiddenManager;
        private AlertDialog currentDialog;
        private Context moduleContext;
        private final ThemePalette P;

        public ProfileManager(ThemePalette palette) {
            this.P = palette;
        }

        public void showProfileManagement(Context context, Context moduleContext) {
            this.moduleContext = moduleContext;
            hiddenManager = new HiddenProfileManager(context);

            new AsyncTask<Void, Void, List<ProfileInfo>>() {
                @Override
                protected List<ProfileInfo> doInBackground(Void... voids) {
                    return loadProfiles(context);
                }

                @Override
                protected void onPostExecute(List<ProfileInfo> profiles) {
                    showManagementDialog(context, profiles, moduleContext);
                }
            }.execute();
        }

        private List<ProfileInfo> loadProfiles(Context context) {
            Set<String> hidden = hiddenManager.getHiddenProfiles();
            List<ProfileInfo> profiles = new ArrayList<>();

            try (SQLiteDatabase blockListDb = context.openOrCreateDatabase("events", Context.MODE_PRIVATE, null);
                 SQLiteDatabase contactDb = context.openOrCreateDatabase("contact", Context.MODE_PRIVATE, null);
                 Cursor cursor = blockListDb.rawQuery(
                         "SELECT contact_mid, year, month, day FROM contact_calendar_event", null)) {

                while (cursor.moveToNext()) {
                    if (isNullDate(cursor)) {
                        String contactMid = cursor.getString(cursor.getColumnIndex("contact_mid"));
                        if (!hidden.contains(contactMid)) {
                            String profileName = getProfileName(contactDb, contactMid);
                            profiles.add(new ProfileInfo(contactMid, profileName));
                        }
                    }
                }
            } catch (Exception e) {
                // XposedBridge.log( moduleContext.getResources().getString(R.string.Block_Profile_Reload) + e.getMessage());
            }
            return profiles;
        }

        private boolean isNullDate(Cursor cursor) {
            return cursor.isNull(cursor.getColumnIndex("year")) &&
                    cursor.isNull(cursor.getColumnIndex("month")) &&
                    cursor.isNull(cursor.getColumnIndex("day"));
        }



        private void showManagementDialog(Context context, List<ProfileInfo> profiles, Context moduleContext) {
            AlertDialog dialog = new AlertDialog.Builder(
                    context,
                    isDarkMode(context)
                            ? android.R.style.Theme_DeviceDefault_Dialog_NoActionBar
                            : android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar
            )
                    .setTitle(moduleContext.getString(R.string.Block_list))
                    .setNeutralButton(moduleContext.getString(R.string.Name_Reset),
                            (d, w) -> showResetConfirmation(context, profiles, moduleContext))
                    .setPositiveButton(moduleContext.getString(R.string.Redisplay),
                            (d, w) -> showRestoreDialog(context))
                    .setNegativeButton(moduleContext.getString(R.string.Close), null)
                    .create();

            // Scroll + Container
            ScrollView scroll = new ScrollView(context);
            scroll.setBackgroundColor(P.bg);

            LinearLayout container = new LinearLayout(context);
            container.setOrientation(LinearLayout.VERTICAL);
            container.setPadding(32, 32, 32, 32);
            container.setBackgroundColor(P.bg);

            scroll.addView(container);

            if (profiles.isEmpty()) {
                TextView empty = new TextView(context);
                empty.setText(moduleContext.getString(R.string.No_Profiles));
                empty.setTextColor(P.fg);
                empty.setGravity(Gravity.CENTER);
                empty.setTextSize(16);
                container.addView(empty);
            } else {
                for (ProfileInfo profile : profiles) {
                    container.addView(createProfileItem(context, profile, container));
                }
            }

            dialog.setView(scroll);
            dialog.show();

            applyDialogPalette(dialog, P);

            currentDialog = dialog;
        }
        private void showResetConfirmation(Context context, List<ProfileInfo> profiles, Context moduleContext) {
            boolean dark = isDarkMode(context);
            ThemePalette P = new ThemePalette(dark);
            AlertDialog.Builder builder =
                    new AlertDialog.Builder(
                            context,
                            dark
                                    ? android.R.style.Theme_DeviceDefault_Dialog_NoActionBar
                                    : android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar
                    );

            builder.setTitle(moduleContext.getString(R.string.really_delete));
            builder.setMessage(moduleContext.getString(R.string.really_delete));
            builder.setPositiveButton(
                    moduleContext.getString(R.string.ok),
                    (d, w) -> performResetOperation(context, profiles)
            );

            builder.setNegativeButton(
                    moduleContext.getString(R.string.cancel),
                    null
            );

            AlertDialog dialog = builder.create();
            dialog.show();
            applyDialogPalette(dialog, P);
            try {
                int titleId = context.getResources().getIdentifier("alertTitle", "id", "android");
                TextView titleView = dialog.findViewById(titleId);
                if (titleView != null) titleView.setTextColor(P.fg);
            } catch (Throwable ignored) {}
        }

        private LinearLayout createProfileItem(Context context, ProfileInfo profile, ViewGroup parent) {
            LinearLayout layout = new LinearLayout(context);
            layout.setOrientation(LinearLayout.HORIZONTAL);
            layout.setPadding(32, 16, 32, 16);
            layout.setBackgroundColor(P.bg);

            TextView tv = new TextView(context);
            tv.setText(profile.profileName);
            tv.setTextColor(P.fg);
            tv.setTextSize(16);
            tv.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

            Button hideBtn = new Button(context);
            hideBtn.setText(moduleContext.getString(R.string.Hide));
            hideBtn.setTextColor(P.fg);
            hideBtn.setBackgroundColor(P.btnBg);

            hideBtn.setOnClickListener(v -> {
                hiddenManager.addHiddenProfile(profile.contactMid);
                parent.removeView(layout);
                Toast.makeText(context, profile.profileName + moduleContext.getString(R.string.user_hide), Toast.LENGTH_SHORT).show();
            });

            layout.addView(tv);
            layout.addView(hideBtn);

            return layout;
        }

        @SuppressLint("StaticFieldLeak")
        private void showRestoreDialog(Context context) {
            Set<String> hidden = hiddenManager.getHiddenProfiles();
            if (hidden.isEmpty()) {
                Toast.makeText(context, moduleContext.getString(R.string.no_hidden_profiles), Toast.LENGTH_SHORT).show();
                return;
            }

            AlertDialog dialog = new AlertDialog.Builder(
                    context,
                    isDarkMode(context)
                            ? android.R.style.Theme_DeviceDefault_Dialog_NoActionBar
                            : android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar
            )
                    .setTitle(moduleContext.getString(R.string.Unhide_hidden_profiles))
                    .setNegativeButton(moduleContext.getString(R.string.Return), null)
                    .create();

            ScrollView scroll = new ScrollView(context);
            scroll.setBackgroundColor(P.bg);

            LinearLayout container = new LinearLayout(context);
            container.setOrientation(LinearLayout.VERTICAL);
            container.setBackgroundColor(P.bg);
            container.setPadding(32, 32, 32, 32);
            scroll.addView(container);

            new AsyncTask<Void, Void, List<ProfileInfo>>() {
                @Override
                protected List<ProfileInfo> doInBackground(Void... voids) {
                    return loadHiddenProfiles(context, hidden);
                }

                @Override
                protected void onPostExecute(List<ProfileInfo> list) {
                    for (ProfileInfo p : list) {
                        LinearLayout row = new LinearLayout(context);
                        row.setOrientation(LinearLayout.HORIZONTAL);
                        row.setPadding(32, 16, 32, 16);
                        row.setBackgroundColor(P.bg);

                        TextView tv = new TextView(context);
                        tv.setText(p.profileName);
                        tv.setTextColor(P.fg);
                        tv.setLayoutParams(new LinearLayout.LayoutParams(
                                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

                        Button restoreBtn = new Button(context);
                        restoreBtn.setText(moduleContext.getString(R.string.Redisplay));
                        restoreBtn.setTextColor(P.fg);
                        restoreBtn.setBackgroundColor(P.btnBg);

                        restoreBtn.setOnClickListener(v -> {
                            hiddenManager.removeHiddenProfile(p.contactMid);
                            container.removeView(row);
                            Toast.makeText(context, p.profileName + moduleContext.getString(R.string.redisplayed_Profile), Toast.LENGTH_SHORT).show();
                        });

                        row.addView(tv);
                        row.addView(restoreBtn);
                        container.addView(row);
                    }
                }
            }.execute();

            dialog.setView(scroll);
            dialog.show();

            applyDialogPalette(dialog, P);
        }
        private List<ProfileInfo> loadHiddenProfiles(Context context, Set<String> hiddenMids) {
            List<ProfileInfo> profiles = new ArrayList<>();

            try (SQLiteDatabase contactDb = context.openOrCreateDatabase("contact", Context.MODE_PRIVATE, null)) {
                for (String contactMid : hiddenMids) {
                    String profileName = getProfileName(contactDb, contactMid);
                    profiles.add(new ProfileInfo(contactMid, profileName));
                }
            } catch (Exception e) {
                // XposedBridge.log("非表示プロファイル読込エラー: " + e.getMessage());
            }
            return profiles;
        }

        private void performResetOperation(Context context, List<ProfileInfo> profiles) {
            new Thread(() -> {
                SQLiteDatabase db = null;
                try {
                    db = context.openOrCreateDatabase("contact", Context.MODE_PRIVATE, null);
                    final String targetPhrase = "[" + moduleContext.getResources().getString(R.string.UserBlocked) + "]";

                    // 既存の値を取得
                    List<String> originalValues = new ArrayList<>();
                    try (Cursor cursor = db.rawQuery(
                            "SELECT overridden_name FROM contacts WHERE overridden_name LIKE ?",
                            new String[]{"%"+targetPhrase+"%"}
                    )) {
                        while (cursor.moveToNext()) {
                            originalValues.add(cursor.getString(0));
                        }
                    }

                    int affectedRows = 0;
                    if (!originalValues.isEmpty()) {
                        db.beginTransaction();
                        try {
                            ContentValues values = new ContentValues();
                            for (String original : originalValues) {
                                String updatedValue = original.replace(targetPhrase, "");

                                if (!original.equals(updatedValue)) {
                                    values.clear();
                                    values.put("overridden_name", updatedValue);

                                    affectedRows += db.update(
                                            "contacts",
                                            values,
                                            "overridden_name = ?",
                                            new String[]{original}
                                    );
                                }
                            }
                            db.setTransactionSuccessful();
                        } finally {
                            db.endTransaction();
                        }
                    }
                    final String message = affectedRows > 0 ?
                            affectedRows + moduleContext.getResources().getString(R.string.reset_name) :
                            moduleContext.getResources().getString(R.string.no_reset_name);

                    new Handler(Looper.getMainLooper()).post(() -> {
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                        refreshProfileList(context, profiles, moduleContext);
                    });

                } catch (Exception e) {
                    new Handler(Looper.getMainLooper()).post(() ->
                            Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
                } finally {
                    if (db != null) {
                        db.close();
                    }
                }
            }).start();
        }

        private void refreshProfileList(Context context, List<ProfileInfo> profiles,Context moduleContext) {
            List<ProfileInfo> newList = loadProfiles(context);
            profiles.clear();
            profiles.addAll(newList);
            showManagementDialog(context, profiles, moduleContext);

        }
    }

    private String getProfileName(SQLiteDatabase db, String contactMid) {
        try (Cursor cursor = db.rawQuery(
                "SELECT profile_name FROM contacts WHERE mid=?", new String[]{contactMid})) {
            return cursor.moveToFirst() ? cursor.getString(0) : "Unknown";
        }
    }
    private static class HiddenProfileManager {
        private static final String HIDDEN_FILE = "hidden_profiles.txt";
        private final File storageFile;

        public HiddenProfileManager(Context context) {
            File downloadsDir = context.getFilesDir();
            File limeDir = new File(downloadsDir, "LimeBackup/Setting");

            if (!limeDir.exists() && !limeDir.mkdirs()) {
                throw new RuntimeException("ディレクトリ作成に失敗: " + limeDir.getAbsolutePath());
            }

            storageFile = new File(limeDir, HIDDEN_FILE);
            if (!storageFile.exists()) {
                try {
                    storageFile.createNewFile();
                } catch (IOException e) {
                    // XposedBridge.log("ファイル作成エラー: " + e.getMessage());
                }
            }
        }

        public void addHiddenProfile(String contactMid) {
            new Thread(() -> {
                try (FileWriter fw = new FileWriter(storageFile, true);
                     BufferedWriter bw = new BufferedWriter(fw)) {
                    bw.write(contactMid);
                    bw.newLine();
                } catch (IOException e) {
                    // XposedBridge.log("非設定追加エラー: " + e.getMessage());
                }
            }).start();
        }

        public Set<String> getHiddenProfiles() {
            Set<String> hidden = Collections.synchronizedSet(new HashSet<>());
            if (storageFile.exists()) {
                try (BufferedReader br = new BufferedReader(new FileReader(storageFile))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        hidden.add(line.trim());
                    }
                } catch (IOException e) {
                    // XposedBridge.log("非表示リスト読込エラー: " + e.getMessage());
                }
            }
            return hidden;
        }

        public void removeHiddenProfile(String contactMid) {
            new Thread(() -> {
                Set<String> current = getHiddenProfiles();
                if (current.remove(contactMid)) {
                    try (FileWriter fw = new FileWriter(storageFile);
                         BufferedWriter bw = new BufferedWriter(fw)) {
                        for (String mid : current) {
                            bw.write(mid);
                            bw.newLine();
                        }
                    } catch (IOException e) {
                        // XposedBridge.log("非表示解除エラー: " + e.getMessage());
                    }
                }
            }).start();
        }
    }


    private static final int PICK_FILE_REQUEST_CODE = 1001;

    private void showFilePickerChatHistory(Context context, Context moduleContext) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {

            if (context instanceof Activity) {
                ((Activity) context).startActivityForResult(Intent.createChooser(intent, "Select a file to restore"), PICK_FILE_REQUEST_CODE);
            } else {
                Toast.makeText(context, "Context is not an Activity", Toast.LENGTH_SHORT).show();
            }
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(context, "Please install a File Manager.", Toast.LENGTH_SHORT).show();
        }
    }

    private static final int PICK_FILE_REQUEST_CODE2 = 1002;

    private void showFilePickerChatlist(Context context, Context moduleContext) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            if (context instanceof Activity) {
                ((Activity) context).startActivityForResult(Intent.createChooser(intent, "Select a file to restore"), PICK_FILE_REQUEST_CODE2);
            } else {
                Toast.makeText(context, "Context is not an Activity", Toast.LENGTH_SHORT).show();
            }
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(context, "Please install a File Manager.", Toast.LENGTH_SHORT).show();
        }
    }

    private void restoreChatHistory(Context context, Context moduleContext, File finalTempFile) {
        new AsyncTask<Void, Integer, Boolean>() {
            private ProgressDialog progressDialog;
            private int totalRecords = 0;
            private int processedRecords = 0;
            private String errorMessage = null;
            private Exception exception = null;

            @Override
            protected void onPreExecute() {
                progressDialog = new ProgressDialog(context);
                progressDialog.setTitle(moduleContext.getString(R.string.restoring_chat_history));
                progressDialog.setMessage(moduleContext.getString(R.string.preparing));
                progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                progressDialog.setCancelable(false);
                progressDialog.setMax(100);
                progressDialog.setProgress(0);
                progressDialog.show();
            }

            @Override
            protected Boolean doInBackground(Void... voids) {
                final int BATCH_SIZE = 50;
                if (finalTempFile == null) {
                    errorMessage = "Backup file path is null";
                    return false;
                }

                File backupDbFile = finalTempFile;
                if (!backupDbFile.exists()) {
                    errorMessage = "Backup file not found at: " + backupDbFile.getAbsolutePath();
                    showToast(context, moduleContext, R.string.Delete_Cache);
                    return false;
                }

                SQLiteDatabase backupDb = null;
                SQLiteDatabase originalDb = null;
                Cursor countCursor = null;
                Cursor dataCursor = null;

                try {

                    backupDb = SQLiteDatabase.openDatabase(
                            backupDbFile.getAbsolutePath(),
                            null,
                            SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS
                    );

                    try {
                        countCursor = backupDb.rawQuery("SELECT COUNT(*) FROM chat_history", null);
                        if (countCursor == null || !countCursor.moveToFirst()) {
                            errorMessage = "Failed to get record count";
                            return false;
                        }
                        totalRecords = countCursor.getInt(0);
                        publishProgress(0);
                    } catch (SQLiteException e) {
                        errorMessage = "Error counting records";
                        exception = e;
                        return false;
                    }

                    if (totalRecords == 0) {
                        return true;
                    }

                    try {
                        originalDb = context.openOrCreateDatabase(
                                "naver_line",
                                Context.MODE_PRIVATE,
                                null
                        );
                    } catch (SQLiteException e) {
                        errorMessage = "Failed to open target database";
                        exception = e;
                        return false;
                    }

                    try {
                        dataCursor = backupDb.rawQuery("SELECT * FROM chat_history", null);
                        if (dataCursor == null || !dataCursor.moveToFirst()) {
                            errorMessage = "No data to restore";
                            return false;
                        }

                        originalDb.beginTransaction();

                        do {
                            if (isCancelled()) {
                                errorMessage = "Restoration cancelled";
                                return false;
                            }

                            try {
                                String serverId = dataCursor.getString(
                                        dataCursor.getColumnIndexOrThrow("server_id")
                                );

                                if (serverId == null) continue;

                                if (!isRecordExists(originalDb, "chat_history", "server_id", serverId)) {
                                    ContentValues values = extractChatHistoryValues(dataCursor);
                                    long rowId = originalDb.insertWithOnConflict(
                                            "chat_history",
                                            null,
                                            values,
                                            SQLiteDatabase.CONFLICT_IGNORE
                                    );

                                    if (rowId == -1) {
                                        Log.w("Restore", "Failed to insert record: " + serverId);
                                    }
                                }

                                processedRecords++;


                                if (processedRecords % Math.max(1, totalRecords / 100) == 0 ||
                                        processedRecords % BATCH_SIZE == 0) {
                                    int progress = (int) ((float) processedRecords / totalRecords * 100);
                                    publishProgress(progress);
                                }
                                if (processedRecords % BATCH_SIZE == 0) {
                                    originalDb.setTransactionSuccessful();
                                    originalDb.endTransaction();
                                    originalDb.beginTransaction();
                                }
                            } catch (SQLiteException e) {
                                Log.e("RestoreRecord", "Error restoring record " + processedRecords, e);
                                continue;
                            }

                        } while (dataCursor.moveToNext());

                        originalDb.setTransactionSuccessful();
                        return true;

                    } catch (Exception e) {
                        errorMessage = "Error during data restoration";
                        exception = e;
                        return false;
                    }

                } catch (Exception e) {
                    errorMessage = "Unexpected error during restoration";
                    exception = e;
                    return false;
                } finally {

                    if (originalDb != null) {
                        try {
                            originalDb.endTransaction();
                            originalDb.close();
                        } catch (Exception e) {
                            Log.e("CloseDB", "Error closing original DB", e);
                        }
                    }
                    closeQuietly(countCursor);
                    closeQuietly(dataCursor);
                    if (backupDb != null) {
                        try {
                            backupDb.close();
                        } catch (Exception e) {
                            Log.e("CloseDB", "Error closing backup DB", e);
                        }
                    }
                }
            }

            @Override
            protected void onProgressUpdate(Integer... progress) {
                if (progressDialog != null && progress.length > 0) {
                    int percent = progress[0];
                    progressDialog.setProgress(percent);
                    progressDialog.setMessage(
                            String.format(
                                    moduleContext.getString(R.string.progress_message),
                                    processedRecords,
                                    totalRecords,
                                    percent
                            )
                    );
                }
            }

            @Override
            protected void onPostExecute(Boolean success) {
                if (progressDialog != null) {
                    progressDialog.dismiss();
                }

                if (success) {
                    showToast(context, moduleContext, R.string.Restore_Success);
                } else {
                    Log.e("RestoreError", errorMessage, exception);
                    String userMessage = moduleContext.getString(R.string.Restore_Error);
                    if (exception instanceof SQLiteException) {
                        userMessage += ": Database error";
                    } else if (exception != null) {
                        userMessage += ": System error";
                    }

                    showToast(context, moduleContext, Integer.parseInt(userMessage));
                }
            }

            @Override
            protected void onCancelled() {
                if (progressDialog != null) {
                    progressDialog.dismiss();
                }
                showToast(context, moduleContext, R.string.Restore_Cancelled);
            }

            private void closeQuietly(Closeable closeable) {
                if (closeable != null) {
                    try {
                        closeable.close();
                    } catch (IOException e) {
                        Log.e("Close", "Error closing resource", e);
                    }
                }
            }
        }.execute();
    }
    private void restoreChatList(Context context, Context moduleContext, File finalTempFile) {
        new AsyncTask<Void, Integer, Boolean>() {
            private ProgressDialog progressDialog;
            private int totalRecords = 0;
            private int processedRecords = 0;

            @Override
            protected void onPreExecute() {
                XposedBridge.log("[Restore] Starting chat list restoration process");
                progressDialog = new ProgressDialog(context);
                progressDialog.setTitle(moduleContext.getString(R.string.restoring_chat));
                progressDialog.setMessage(moduleContext.getString(R.string.preparing));
                progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                progressDialog.setCancelable(false);
                progressDialog.setMax(100);
                progressDialog.setProgress(0);
                progressDialog.show();
            }

            @Override
            protected Boolean doInBackground(Void... voids) {
                final int BATCH_SIZE = 500;
                XposedBridge.log("[Restore] Batch size set to: " + BATCH_SIZE);

                if (finalTempFile == null || !finalTempFile.exists()) {
                    XposedBridge.log("[Restore] ERROR: Backup file not found or null");
                    return false;
                }

                SQLiteDatabase backupDb = null;
                SQLiteDatabase originalDb = null;
                Cursor cursor = null;
                Cursor countCursor = null;

                try {
                    if (!finalTempFile.canRead()) {
                        XposedBridge.log("[Restore] ERROR: No read permission for backup file");
                        return false;
                    }

                    XposedBridge.log("[Restore] Opening backup database: " + finalTempFile.getAbsolutePath());
                    backupDb = SQLiteDatabase.openDatabase(
                            finalTempFile.getAbsolutePath(),
                            null,
                            SQLiteDatabase.OPEN_READONLY
                    );

                    countCursor = backupDb.rawQuery("SELECT COUNT(*) FROM chat", null);
                    if (countCursor != null && countCursor.moveToFirst()) {
                        totalRecords = countCursor.getInt(0);
                        XposedBridge.log("[Restore] Total records to process: " + totalRecords);
                        publishProgress(0);
                    }

                    originalDb = context.openOrCreateDatabase(
                            "naver_line",
                            Context.MODE_PRIVATE,
                            null
                    );
                    XposedBridge.log("[Restore] Original database opened");

                    originalDb.beginTransaction();
                    cursor = backupDb.rawQuery("SELECT * FROM chat", null);

                    if (cursor != null && cursor.moveToFirst()) {
                        XposedBridge.log("[Restore] Starting to process records");
                        do {
                            if (isCancelled()) {
                                XposedBridge.log("[Restore] Operation cancelled by user");
                                return false;
                            }

                            String chatId = cursor.getString(
                                    cursor.getColumnIndexOrThrow("chat_id")
                            );
                            if (chatId == null) {
                                XposedBridge.log("[Restore] WARNING: Found null chat_id, skipping");
                                continue;
                            }

                            ContentValues values = extractChatValues(cursor);
                            long rowId = originalDb.insertWithOnConflict(
                                    "chat",
                                    null,
                                    values,
                                    SQLiteDatabase.CONFLICT_IGNORE
                            );

                            if (rowId == -1) {
                                XposedBridge.log("[Restore] WARNING: Conflict detected for chat_id: " + chatId);
                            }

                            processedRecords++;
                            if (processedRecords % Math.max(1, totalRecords / 100) == 0 ||
                                    processedRecords % BATCH_SIZE == 0) {
                                int progress = (int) ((float) processedRecords / totalRecords * 100);
                                publishProgress(progress);
                            }
                            if (processedRecords % BATCH_SIZE == 0) {
                                originalDb.setTransactionSuccessful();
                                originalDb.endTransaction();
                                XposedBridge.log("[Restore] Committed batch of " + BATCH_SIZE + " records");
                                originalDb.beginTransaction();
                            }

                        } while (cursor.moveToNext());
                    }

                    originalDb.setTransactionSuccessful();
                    XposedBridge.log("[Restore] Successfully processed all records");
                    return true;

                } catch (Exception e) {
                    XposedBridge.log("[Restore] ERROR: Exception during restoration: " + Log.getStackTraceString(e));
                    return false;
                } finally {
                    if (originalDb != null) {
                        try {
                            originalDb.endTransaction();
                            originalDb.close();
                            XposedBridge.log("[Restore] Original database closed");
                        } catch (Exception e) {
                            XposedBridge.log("[Restore] ERROR closing original DB: " + Log.getStackTraceString(e));
                        }
                    }
                    closeQuietly(cursor);
                    closeQuietly(countCursor);
                    if (backupDb != null) {
                        try {
                            backupDb.close();
                            XposedBridge.log("[Restore] Backup database closed");
                        } catch (Exception e) {
                            XposedBridge.log("[Restore] ERROR closing backup DB: " + Log.getStackTraceString(e));
                        }
                    }

                    if (finalTempFile != null && finalTempFile.exists()) {
                        boolean deleted = finalTempFile.delete();
                        XposedBridge.log("[Restore] Temp file deletion " + (deleted ? "successful" : "failed"));
                    }
                }
            }

            @Override
            protected void onProgressUpdate(Integer... progress) {
                if (progressDialog != null && progress.length > 0) {
                    int percent = progress[0];
                    progressDialog.setProgress(percent);
                    progressDialog.setMessage(
                            String.format(
                                    "%s: %d/%d (%d%%)",
                                    moduleContext.getString(R.string.processing),
                                    processedRecords,
                                    totalRecords,
                                    percent
                            )
                    );
                    if (percent % 10 == 0) {
                        XposedBridge.log("[Restore] Progress: " + percent + "% (" + processedRecords + "/" + totalRecords + ")");
                    }
                }
            }

            @Override
            protected void onPostExecute(Boolean success) {
                if (progressDialog != null) {
                    progressDialog.dismiss();
                }

                if (success) {
                    XposedBridge.log("[Restore] SUCCESS: Chat table restored successfully");
                    showToast(context, moduleContext, R.string.Restore_Chat_Table_Success);
                } else {
                    XposedBridge.log("[Restore] FAILED: Chat table restoration failed");
                    showToast(context, moduleContext, R.string.Restore_Chat_Table_Error);
                }
            }

            @Override
            protected void onCancelled() {
                XposedBridge.log("[Restore] CANCELLED: Operation cancelled by user");
                if (progressDialog != null) {
                    progressDialog.dismiss();
                }
                showToast(context, moduleContext, R.string.Restore_Cancelled);
            }

            private void closeQuietly(Closeable closeable) {
                if (closeable != null) {
                    try {
                        closeable.close();
                    } catch (IOException e) {
                        XposedBridge.log("[Restore] ERROR closing resource: " + Log.getStackTraceString(e));
                    }
                }
            }
        }.execute();
    }
    private ContentValues extractChatHistoryValues(Cursor cursor) {
        ContentValues values = new ContentValues();
        values.put("server_id", cursor.getString(cursor.getColumnIndexOrThrow("server_id")));
        values.put("type", getNullableInt(cursor, "type"));
        values.put("chat_id", cursor.getString(cursor.getColumnIndexOrThrow("chat_id")));
        values.put("from_mid", cursor.getString(cursor.getColumnIndexOrThrow("from_mid")));
        values.put("content", cursor.getString(cursor.getColumnIndexOrThrow("content")));
        values.put("created_time", cursor.getString(cursor.getColumnIndexOrThrow("created_time")));
        values.put("delivered_time", cursor.getString(cursor.getColumnIndexOrThrow("delivered_time")));
        values.put("status", getNullableInt(cursor, "status"));
        values.put("sent_count", getNullableInt(cursor, "sent_count"));
        values.put("read_count", getNullableInt(cursor, "read_count"));
        values.put("location_name", cursor.getString(cursor.getColumnIndexOrThrow("location_name")));
        values.put("location_address", cursor.getString(cursor.getColumnIndexOrThrow("location_address")));
        values.put("location_phone", cursor.getString(cursor.getColumnIndexOrThrow("location_phone")));
        values.put("location_latitude", getNullableInt(cursor, "location_latitude"));
        values.put("location_longitude", getNullableInt(cursor, "location_longitude"));
        values.put("attachement_image", getNullableInt(cursor, "attachement_image"));
        values.put("attachement_image_height", getNullableInt(cursor, "attachement_image_height"));
        values.put("attachement_image_width", getNullableInt(cursor, "attachement_image_width"));
        values.put("attachement_image_size", getNullableInt(cursor, "attachement_image_size"));
        values.put("attachement_type", getNullableInt(cursor, "attachement_type"));
        values.put("attachement_local_uri", cursor.getString(cursor.getColumnIndexOrThrow("attachement_local_uri")));
        values.put("parameter", cursor.getString(cursor.getColumnIndexOrThrow("parameter")));
        values.put("chunks", cursor.getBlob(cursor.getColumnIndexOrThrow("chunks")));
        return values;
    }

    private ContentValues extractChatValues(Cursor cursor) {
        ContentValues values = new ContentValues();
        values.put("chat_id", cursor.getString(cursor.getColumnIndexOrThrow("chat_id")));
        values.put("chat_name", cursor.getString(cursor.getColumnIndexOrThrow("chat_name")));
        values.put("owner_mid", cursor.getString(cursor.getColumnIndexOrThrow("owner_mid")));
        values.put("last_from_mid", cursor.getString(cursor.getColumnIndexOrThrow("last_from_mid")));
        values.put("last_message", cursor.getString(cursor.getColumnIndexOrThrow("last_message")));
        values.put("last_created_time", cursor.getString(cursor.getColumnIndexOrThrow("last_created_time")));
        values.put("message_count", getNullableInt(cursor, "message_count"));
        values.put("read_message_count", getNullableInt(cursor, "read_message_count"));
        values.put("latest_mentioned_position", getNullableInt(cursor, "latest_mentioned_position"));
        values.put("type", getNullableInt(cursor, "type"));
        values.put("is_notification", getNullableInt(cursor, "is_notification"));
        values.put("skin_key", cursor.getString(cursor.getColumnIndexOrThrow("skin_key")));
        values.put("input_text", cursor.getString(cursor.getColumnIndexOrThrow("input_text")));
        values.put("input_text_metadata", cursor.getString(cursor.getColumnIndexOrThrow("input_text_metadata")));
        values.put("hide_member", getNullableInt(cursor, "hide_member"));
        values.put("p_timer", getNullableInt(cursor, "p_timer"));
        values.put("last_message_display_time", cursor.getString(cursor.getColumnIndexOrThrow("last_message_display_time")));
        values.put("mid_p", cursor.getString(cursor.getColumnIndexOrThrow("mid_p")));
        values.put("is_archived", getNullableInt(cursor, "is_archived"));
        values.put("read_up", cursor.getString(cursor.getColumnIndexOrThrow("read_up")));
        values.put("is_groupcalling", getNullableInt(cursor, "is_groupcalling"));
        values.put("latest_announcement_seq", getNullableInt(cursor, "latest_announcement_seq"));
        values.put("announcement_view_status", getNullableInt(cursor, "announcement_view_status"));
        values.put("last_message_meta_data", cursor.getString(cursor.getColumnIndexOrThrow("last_message_meta_data")));
        values.put("chat_room_bgm_data", cursor.getString(cursor.getColumnIndexOrThrow("chat_room_bgm_data")));
        values.put("chat_room_bgm_checked", getNullableInt(cursor, "chat_room_bgm_checked"));
        values.put("chat_room_should_show_bgm_badge", getNullableInt(cursor, "chat_room_should_show_bgm_badge"));
        values.put("unread_type_and_count", cursor.getString(cursor.getColumnIndexOrThrow("unread_type_and_count")));
        return values;
    }

    private Integer getNullableInt(Cursor cursor, String columnName) {
        int columnIndex = cursor.getColumnIndexOrThrow(columnName);
        return cursor.isNull(columnIndex) ? null : cursor.getInt(columnIndex);
    }

    private boolean isRecordExists(SQLiteDatabase db, String table, String column, String value) {
        try (Cursor cursor = db.rawQuery("SELECT 1 FROM " + table + " WHERE " + column + " = ?", new String[]{value})) {
            return cursor != null && cursor.moveToFirst();
        }
    }

    private void showToast(Context context, Context moduleContext, int resId) {
        Toast.makeText(context, moduleContext.getResources().getString(resId), Toast.LENGTH_SHORT).show();
    }

    private void backupChatsFolder(Context appCtx, Context moduleCtx) {

        File srcChats = new File(Environment.getExternalStorageDirectory(),
                "Android/data/"+Constants.PACKAGE_NAME+"/files/chats");
        String backupUriS = loadBackupUri(appCtx);

        if (backupUriS == null) {
            showToast(appCtx, moduleCtx.getString(
                    R.string.Talk_Picture_Back_up_Error_No_URI));
            return;
        }

        try {
            Uri treeUri = Uri.parse(backupUriS);
            DocumentFile rootDir = DocumentFile.fromTreeUri(appCtx, treeUri);

            if (rootDir == null || !rootDir.exists()) {
                showToast(appCtx, moduleCtx.getString(
                        R.string.Talk_Picture_Back_up_Error_No_Access));
                return;
            }

            DocumentFile chatsDir = rootDir.findFile("chats_backup");
            if (chatsDir != null) chatsDir.delete();        // 上書き用に削除
            chatsDir = rootDir.createDirectory("chats_backup");
            if (chatsDir == null) {
                showToast(appCtx, moduleCtx.getString(
                        R.string.Talk_Picture_Back_up_Error_Create_Dir));
                return;
            }

            copyDirectoryToDocumentFile(appCtx, srcChats, chatsDir);

            showToast(appCtx, moduleCtx.getString(
                    R.string.Talk_Picture_Back_up_Success));

        } catch (SecurityException e) {
            XposedBridge.log("Lime-Backup Chats: SAF permission error → " + e);
            showToast(appCtx, moduleCtx.getString(
                    R.string.Talk_Picture_Back_up_Error_No_Access));
        } catch (IOException | NullPointerException e) {
       
            XposedBridge.log("Lime-Backup Chats: error → " + e);
            showToast(appCtx, moduleCtx.getString(
                    R.string.Talk_Picture_Back_up_Error));
        }
    }
    private void showToast(final Context context, final String message) {
        new android.os.Handler(context.getMainLooper()).post(() ->
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        );
    }

    private void copyDirectoryToDocumentFile(Context ctx,
                                             File srcDir,
                                             DocumentFile destDir) throws IOException {

        File[] children = srcDir.listFiles();
        if (children == null) return;

        for (File child : children) {
            if (child.isDirectory()) {

                DocumentFile newDir = destDir.createDirectory(child.getName());
                if (newDir != null) {
                    copyDirectoryToDocumentFile(ctx, child, newDir);
                }

            } else {
                DocumentFile newFile = destDir.createFile(
                        getMimeType(child.getName()), child.getName());
                if (newFile == null) continue;

                try (InputStream in = new FileInputStream(child);
                     OutputStream out = ctx.getContentResolver()
                             .openOutputStream(newFile.getUri())) {

                    byte[] buf = new byte[4096];
                    int len;
                    while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
                }
            }
        }
    }
    private String getMimeType(String fileName) {
        String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        switch (extension) {
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "mp4":
                return "video/mp4";
            case "db":
                return "application/x-sqlite3";
            default:
                return "application/octet-stream";
        }
    }

    private void copyDirectory(File sourceDir, File destDir) throws IOException {
        if (!sourceDir.exists()) {return;}

        if (!destDir.exists()) {
            destDir.mkdirs();
        }

        File[] files = sourceDir.listFiles();
        if (files != null) {
            for (File file : files) {
                File destFile = new File(destDir, file.getName());
                if (file.isDirectory()) {
                    copyDirectory(file, destFile);
                } else {
                    copyFile(file, destFile);
                }
            }
        }
    }
    private void copyFile(File sourceFile, File destFile) throws IOException {
        if (destFile.exists()) {
            destFile.delete();
        }

        try (FileChannel sourceChannel = new FileInputStream(sourceFile).getChannel();
             FileChannel destChannel = new FileOutputStream(destFile).getChannel()) {
            destChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
        }
    }
    private void restoreChatsFolder(Context context, Context moduleContext) {
        String backupUriString = loadBackupUri(context);
        File backupDirFile = null;
        DocumentFile backupDirDoc = null;

        if (backupUriString != null) {
            Uri backupUri = Uri.parse(backupUriString);

            if ("content".equals(backupUri.getScheme())) {
                backupDirDoc = DocumentFile.fromTreeUri(context, backupUri);
            } else if ("file".equals(backupUri.getScheme()) || backupUri.getScheme() == null) {
                backupDirFile = new File(backupUri.getPath());
            }
        }
        if (backupDirDoc == null && backupDirFile == null) {
            File defaultDir = new File(context.getFilesDir(), "LimeBackup/chats_backup");
            if (defaultDir.exists()) {
                backupDirFile = defaultDir;
            } else {
                File alt = new File(
                        Environment.getExternalStorageDirectory(),
                        "Android/data/"+Constants.PACKAGE_NAME+"/backup/chats_backup"
                );
                if (alt.exists()) {
                    backupDirFile = alt;
                }
            }
        }

        if ((backupDirFile != null && (!backupDirFile.exists() || !backupDirFile.isDirectory()))
                || (backupDirDoc  != null && !backupDirDoc.isDirectory())) {
            Toast.makeText(
                    context,
                    moduleContext.getString(R.string.Restore_Chat_Photo_Not_Folder),
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        // ④ 復元先フォルダの準備
        File targetDir = new File(
                Environment.getExternalStorageDirectory(),
                "Android/data/"+Constants.PACKAGE_NAME+"/files/chats"
        );
        if (!targetDir.exists() && !targetDir.mkdirs()) {
            Toast.makeText(
                    context,
                    moduleContext.getString(R.string.Restore_Create_Failed_Chat_Photo_Folder),
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        try {
            if (backupDirFile != null) {
                
                copyDirectory(backupDirFile, targetDir);
            } else {
                copyFromDocumentFile(context, backupDirDoc, targetDir);
            }
            Toast.makeText(
                    context,
                    moduleContext.getString(R.string.Restore_Chat_Photo_Success),
                    Toast.LENGTH_SHORT
            ).show();
        } catch (IOException e) {
            Toast.makeText(
                    context,
                    moduleContext.getString(R.string.Restore_Chat_Photo_Error),
                    Toast.LENGTH_SHORT
            ).show();
            XposedBridge.log("Lime RestoreChats Error: " + e.getMessage());
        }
    }
    private void copyFromDocumentFile(Context context, DocumentFile srcDir, File targetDir) throws IOException {
        for (DocumentFile child : srcDir.listFiles()) {
            if (child.isDirectory()) {
                File sub = new File(targetDir, child.getName());
                if (!sub.exists() && !sub.mkdirs()) {
                    throw new IOException("Failed to create dir: " + sub);
                }
                copyFromDocumentFile(context, child, sub);
            } else if (child.isFile()) {
                try (InputStream in = context.getContentResolver().openInputStream(child.getUri());
                     OutputStream out = new FileOutputStream(new File(targetDir, child.getName()))) {
                    byte[] buf = new byte[8192];
                    int len;
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                }
            }
        }
    }

    private Context getTargetAppContext(XC_LoadPackage.LoadPackageParam lpparam) {
        Context contextV = null;


        try {
            contextV = AndroidAppHelper.currentApplication();
            if (contextV != null) {
                // XposedBridge.log("Lime: Got context via AndroidAppHelper: " + contextV.getPackageName());
                return contextV;
            }
        } catch (Throwable t) {
            // XposedBridge.log("Lime: AndroidAppHelper failed: " + t.toString());
        }


        try {
            Class<?> activityThreadClass = XposedHelpers.findClass("android.app.ActivityThread", lpparam.classLoader);
            Object activityThread = XposedHelpers.callStaticMethod(activityThreadClass, "currentActivityThread");
            Object loadedApk = XposedHelpers.getObjectField(activityThread, "mBoundApplication");
            Object appInfo = XposedHelpers.getObjectField(loadedApk, "info");
            contextV = (Context) XposedHelpers.callMethod(activityThread, "getSystemContext");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // XposedBridge.log("Lime: Context via ActivityThread: "+ contextV.getPackageName() + " | DataDir: " + contextV.getDataDir());
            }
            return contextV;
        } catch (Throwable t) {
            // XposedBridge.log("Lime: ActivityThread method failed: " + t.toString());
        }


        try {
            Context systemContext = (Context) XposedHelpers.callStaticMethod(
                    XposedHelpers.findClass("android.app.ContextImpl", lpparam.classLoader),
                    "createSystemContext",
                    XposedHelpers.callStaticMethod(
                            XposedHelpers.findClass("android.app.ActivityThread", lpparam.classLoader),
                            "currentActivityThread"
                    )
            );

            contextV = systemContext.createPackageContext(
                    Constants.PACKAGE_NAME,
                    Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY
            );

            // XposedBridge.log("Lime: Fallback context created: "+ (contextV != null ? contextV.getPackageName() : "null"));
            return contextV;
        } catch (Throwable t) {
            // XposedBridge.log("Lime: Fallback context failed: " + t.toString());
        }

        return null;
    }


    private void GetMidId(Context context, Context moduleContext) {
        SQLiteDatabase profileDb = context.openOrCreateDatabase("contact", Context.MODE_PRIVATE, null);
        String backupUri = loadBackupUri(context);
        if (backupUri == null) {
            XposedBridge.log("Lime Backup: No backup URI found");
            return;
        }

        try {
            Uri treeUri = Uri.parse(backupUri);
            DocumentFile pickedDir = DocumentFile.fromTreeUri(context, treeUri);

            if (pickedDir == null || !pickedDir.exists()) {
                XposedBridge.log("Lime Backup: Directory does not exist or access denied");
                return;
            }
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
            String fileName = "contacts_" + sdf.format(new Date()) + ".csv";

            DocumentFile file = pickedDir.createFile("text/csv", fileName);
            if (file == null) {
                XposedBridge.log("Lime Backup: Failed to create file");
                return;
            }

            OutputStream outputStream = context.getContentResolver().openOutputStream(file.getUri());
            if (outputStream == null) {
                XposedBridge.log("Lime Backup: Failed to open output stream");
                return;
            }

            OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);

            writer.write("mid,profile_name\n");
            writer.flush();

            Cursor cursor = null;
            try {
                cursor = profileDb.rawQuery("SELECT mid, profile_name FROM contacts", null);
                if (cursor != null && cursor.moveToFirst()) {
                    do {
                        String mid = cursor.getString(0);
                        String profileName = cursor.getString(1);

                        String line = "\"" + (mid != null ? mid.replace("\"", "\"\"") : "") + "\"," +
                                "\"" + (profileName != null ? profileName.replace("\"", "\"\"") : "") + "\"\n";

                        writer.write(line);
                        writer.flush();

                    } while (cursor.moveToNext());
                }
                XposedBridge.log("Lime Backup: CSV exported successfully to " + file.getUri());
            } catch (Exception e) {
                XposedBridge.log("Lime Database Error: " + e.getMessage());
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
                writer.close();
                outputStream.close();
                profileDb.close();
                Toast.makeText(context, "Save Mid ID", Toast.LENGTH_LONG).show();

            }
        } catch (IOException e) {
            XposedBridge.log("Lime CSV Export Error: " + e.getMessage());
            Toast.makeText(context, "Error", Toast.LENGTH_LONG).show();
        }
    }
    private String loadBackupUri(Context context) {
        File settingsFile = new File(context.getFilesDir(), "LimeBackup/backup_uri.txt");
        if (!settingsFile.exists()) return null;

        try (FileInputStream fis = new FileInputStream(settingsFile);
             InputStreamReader isr = new InputStreamReader(fis);
             BufferedReader br = new BufferedReader(isr)) {
            return br.readLine();
        } catch (IOException e) {
            XposedBridge.log("Lime URI Load Error: " + e.getMessage());
            return null;
        }


    }

    private int dpToPx(@NonNull Context context, float dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private Drawable scaleDrawable(Drawable drawable, int width, int height) {
        Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
        return new BitmapDrawable(scaledBitmap);
    }

    private Drawable loadImageFromUri(Context context, String imageName) {
        String backupUri = loadBackupUri(context);
        if (backupUri != null) {
            try {
                Uri treeUri = Uri.parse(backupUri);
                DocumentFile dir = DocumentFile.fromTreeUri(context, treeUri);
                if (dir != null) {
                    DocumentFile imageFile = dir.findFile(imageName);
                    if (imageFile != null && imageFile.exists()) {
                        try (InputStream inputStream = context.getContentResolver().openInputStream(imageFile.getUri())) {
                            return Drawable.createFromStream(inputStream, null);
                        } catch (IOException e) {
                            XposedBridge.log("Lime: Error loading image from URI: " + e.getMessage());
                        }
                    }
                }
            } catch (Exception e) {
                XposedBridge.log("Lime: Error accessing image URI: " + e.getMessage());
            }
        }
        return null;
    }
    private void copyImageToUri(Context context, Context moduleContext, String imageName) {
        String backupUri = loadBackupUri(context);
        if (backupUri == null) return;

        try {
            Uri treeUri = Uri.parse(backupUri);
            DocumentFile dir = DocumentFile.fromTreeUri(context, treeUri);
            if (dir == null) return;
            if (dir.findFile(imageName) != null) return;
            int resId = moduleContext.getResources().getIdentifier(
                    imageName.replace(".png", ""), "drawable", Constants.MODULE_NAME);
            if (resId == 0) return;

            try (InputStream in = moduleContext.getResources().openRawResource(resId);
                 OutputStream out = context.getContentResolver().openOutputStream(
                         dir.createFile("image/png", imageName).getUri())) {

                byte[] buffer = new byte[1024];
                int length;
                while ((length = in.read(buffer)) > 0) {
                    out.write(buffer, 0, length);
                }
            }
        } catch (Exception e) {
            XposedBridge.log("Lime: Error copying image to URI: " + e.getMessage());
        }
    }


    private Map<String, String> readSettingsFromFile(Context context) {
        String fileName = "margin_settings.txt";
        File dir = new File(context.getFilesDir(), "LimeBackup/Setting");
        File file = new File(dir, fileName);
        Map<String, String> settings = new HashMap<>();
        settings.put("header_setting_size", "60");

        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split("=", 2);
                    if (parts.length == 2) {
                        settings.put(parts[0].trim(), parts[1].trim());
                    }
                }
            } catch (IOException e) {
                XposedBridge.log("Lime: Error reading settings file: " + e.getMessage());
            }
        }
        return settings;
    }

}
