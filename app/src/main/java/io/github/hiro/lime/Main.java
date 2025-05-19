package io.github.hiro.lime;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AndroidAppHelper;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.XModuleResources;

import android.graphics.Color;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.provider.DocumentsContract;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LayoutInflated;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.hiro.lime.hooks.*;

public class Main implements IXposedHookLoadPackage, IXposedHookInitPackageResources, IXposedHookZygoteInit {

    public static String modulePath;
    public static CustomPreferences customPreferences;
    public static LimeOptions limeOptions = new LimeOptions();
    private static Context context; // Static context to be shared
    static final IHook[] hooks = {
            new OutputResponse(),
            new ModifyRequest(),
            new CheckHookTargetVersion(),
            new SpoofAndroidId(),
            new SpoofUserAgent(),
            new AddRegistrationOptions(),
            new EmbedOptions(),
            new RemoveIcons(),
            new RemoveIconLabels(),
            new RemoveAds(),
            new RemoveFlexibleContents(),
            new RemoveReplyMute(),
            new RedirectWebView(),
            new PreventMarkAsRead(),
            new PreventUnsendMessage(),
            new SendMuteMessage(),
            new KeepUnread(),
            new ModifyResponse(),
            new OutputRequest(),
            new ChatList(),
            new UnsentRec(),
            new RingTone(),
            new ReadChecker(),
            new DarkColor(),
            new KeepUnreadLSpatch(),
            new AutomaticBackup(),
            new RemoveProfileNotification(),
            new Disabled_Group_notification(),
            new PhotoAddNotification(),
            new RemoveVoiceRecord(),
            new AgeCheckSkip(),
            new CallOpenApplication(),
            new SettingCrash(),
            new BlockCheck(),
            new AutoUpdate(),
            new Removebutton(),
            new PhotoSave(),
            new ReactionList(),
            new WhiteToDark(),
            new DisableSilentMessage()
    };


    @Override
    public void handleLoadPackage(@NonNull XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals(Constants.PACKAGE_NAME)) return;

        context = getTargetAppContext(lpparam);
        if (context == null) {
            XposedBridge.log("Lime: Context acquisition failed");
            return;
        }

        try {
            customPreferences = new CustomPreferences(context);
            if (!tryLoadSettings()) {
                setupUriConfiguration(lpparam);
                XposedBridge.log("エラー");
            }
        } catch (Exception e) {
            setupUriConfiguration(lpparam);
            XposedBridge.log("エラー");
        }
        Constants.initializeHooks(lpparam);
        loadSettings(context);
        for (IHook hook : hooks) {
            hook.hook(limeOptions, lpparam);
        }
    }


    private Button createConfigButton(Context context, XC_MethodHook.MethodHookParam param) {
        Button button = new Button(context);
        button.setText("Open LimeBackup Folder  \n  Download/LimeBackup//Settingの中でこのフォルダを使用するをクリック");
        button.setBackgroundColor(0xFFBB86FC);
        button.setTextColor(Color.WHITE);
        button.setPadding(30, 20, 30, 20);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        params.bottomMargin = 50;
        params.rightMargin = 50;
        button.setLayoutParams(params);

        button.setOnClickListener(v -> launchDocumentTreeIntent(context, param));
        return button;
    }

    private void launchDocumentTreeIntent(Context context, XC_MethodHook.MethodHookParam param) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Uri initialUri = Uri.parse("content://com.android.externalstorage.documents/document/primary:Download/LimeBackup/Setting");
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, initialUri);
        }

        try {
            Activity activity = (Activity) XposedHelpers.callMethod(param.thisObject, "getActivity");
            if (activity != null) {
                activity.startActivityForResult(intent, 12345);
            }
        } catch (Exception e) {
            showToast(context, "Error: " + e.getMessage());
        }
    }
    private void handleUriResult(Object activity, Intent data) {
        Context context = (Context) activity;
        Uri treeUri = data.getData();

        try {
            context.getContentResolver().takePersistableUriPermission(
                    treeUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION |
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            );

            saveBackupUri(context, treeUri);

            DocumentPreferences prefs = new DocumentPreferences(context, treeUri);
            prefs.recreateSettingsFile(context);
            Toast.makeText(context, "設定を正常に読み込みました", Toast.LENGTH_LONG).show();
            android.os.Process.killProcess(Process.myPid());
            context.startActivity(new Intent().setClassName(Constants.PACKAGE_NAME, "jp.naver.line.android.activity.SplashActivity"));

        } catch (Exception e) {
            Toast.makeText(context, "設定の読み込みに失敗: " + e.getMessage(), Toast.LENGTH_LONG).show();
            XposedBridge.log("Lime Settings Error: " + e.getMessage());
        }
    }
    String fragmentClass;
    String versionName;

    private void setupUriConfiguration(XC_LoadPackage.LoadPackageParam lpparam) throws ClassNotFoundException {
        XposedHelpers.findAndHookMethod(
                "com.linecorp.line.chatlist.view.fragment.ChatListPageFragment",
                lpparam.classLoader,
                "onCreateView",
                LayoutInflater.class, ViewGroup.class, Bundle.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        View rootView = (View) param.getResult();
                        Context context = rootView.getContext();
                        String packageName = context.getPackageName();
                        String versionName = context.getPackageManager()
                                .getPackageInfo(packageName, 0)
                                .versionName;

                        String fragmentClass;
                        if (isVersionInRange(versionName, "15.5.0", "15.6.0")) {
                            fragmentClass = "androidx.fragment.app.n";
                        } else if (isVersionInRange(versionName, "15.6.0", "15.7.0")) {
                            fragmentClass = "androidx.fragment.app.m";
                        } else {
                            XposedBridge.log("Unsupported version: " + versionName);
                            return;
                        }

                        XposedHelpers.findAndHookMethod(
                                fragmentClass,
                                lpparam.classLoader,
                                "onActivityResult",
                                int.class, int.class, Intent.class,
                                new XC_MethodHook() {
                                    @Override
                                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                        int requestCode = (int) param.args[0];
                                        int resultCode = (int) param.args[1];
                                        Intent data = (Intent) param.args[2];

                                        if (requestCode == 12345 && resultCode == Activity.RESULT_OK && data != null) {
                                            handleUriResult(param.thisObject, data);
                                        }
                                    }
                                }
                        );

                        new Handler(Looper.getMainLooper()).post(() -> {
                            Button openFolderButton = createConfigButton(context, param);
                            if (rootView instanceof ViewGroup) {
                                ((ViewGroup) rootView).addView(openFolderButton);
                            }
                        });
                    }
                }
        );

    }


    private static boolean isVersionInRange(String versionName, String minVersion, String maxVersion) {
        try {
            int[] currentVersion = parseVersion(versionName);
            int[] minVersionArray = parseVersion(minVersion);
            int[] maxVersionArray = parseVersion(maxVersion);

            boolean isGreaterOrEqualMin = compareVersions(currentVersion, minVersionArray) >= 0;

            boolean isLessThanMax = compareVersions(currentVersion, maxVersionArray) < 0;

            return isGreaterOrEqualMin && isLessThanMax;
        } catch (Exception ignored) {
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
    private void saveBackupUri(Context context, Uri uri) {
        File settingsDir = new File(context.getFilesDir(), "LimeBackup");
        if (!settingsDir.exists()) {
            settingsDir.mkdirs();
        }

        File settingsFile = new File(settingsDir, "backup_uri.txt");
        try (FileOutputStream fos = new FileOutputStream(settingsFile);
             OutputStreamWriter osw = new OutputStreamWriter(fos)) {
            osw.write(uri.toString());
        } catch (IOException e) {
            Toast.makeText(context, "URIの保存に失敗しました: " + e.getMessage(), Toast.LENGTH_LONG).show();
            XposedBridge.log("Lime URI Save Error: " + e.getMessage());
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
    private void loadSettings(Context context) {
        try {

            loadFromCustomPreferences();
            XposedBridge.log("読み込みました");
        } catch (Exception e) {
            XposedBridge.log("CustomPreferences Load Error: " + e.getMessage());
            String backupUri = loadBackupUri(context);
            if (backupUri != null) {
return;
            }
        }
    }

    private void loadFromCustomPreferences() throws SettingsLoadException {
        try {
            for (LimeOptions.Option option : limeOptions.options) {
                String value = customPreferences.getSetting(option.name, null);
                if (value == null) {
                    throw new SettingsLoadException("Setting " + option.name + " not found");
                }
                option.checked = Boolean.parseBoolean(value);
            }
        } catch (Exception e) {
            throw new SettingsLoadException("Failed to load settings", e);
        }
    }
    private static class SettingsLoadException extends Exception {
        public SettingsLoadException(String message) { super(message); }
        public SettingsLoadException(String message, Throwable cause) { super(message, cause); }
    }

    private void showToast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }


    private Context getTargetAppContext(XC_LoadPackage.LoadPackageParam lpparam) {
        Context context = null;


        try {
            context = AndroidAppHelper.currentApplication();
            if (context != null) {
                XposedBridge.log("Lime: Got context via AndroidAppHelper: " + context.getPackageName());
                return context;
            }
        } catch (Throwable t) {
            XposedBridge.log("Lime: AndroidAppHelper failed: " + t.toString());
        }


        try {
            Class<?> activityThreadClass = XposedHelpers.findClass("android.app.ActivityThread", lpparam.classLoader);
            Object activityThread = XposedHelpers.callStaticMethod(activityThreadClass, "currentActivityThread");
            Object loadedApk = XposedHelpers.getObjectField(activityThread, "mBoundApplication");
            Object appInfo = XposedHelpers.getObjectField(loadedApk, "info");
            context = (Context) XposedHelpers.callMethod(activityThread, "getSystemContext");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                XposedBridge.log("Lime: Context via ActivityThread: "
                        + context.getPackageName()
                        + " | DataDir: " + context.getDataDir());
            }
            return context;
        } catch (Throwable t) {
            XposedBridge.log("Lime: ActivityThread method failed: " + t.toString());
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

            context = systemContext.createPackageContext(
                    Constants.PACKAGE_NAME,
                    Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY
            );

            XposedBridge.log("Lime: Fallback context created: "
                    + (context != null ? context.getPackageName() : "null"));
            return context;
        } catch (Throwable t) {
            XposedBridge.log("Lime: Fallback context failed: " + t.toString());
        }

        return null;
    }
    private boolean tryLoadSettings() {
        try {
            loadSettings(context);
            return true;
        } catch ( Throwable t) {

            return false;
        }
    }
    private Context getTargetAppContextForResources(XC_InitPackageResources.InitPackageResourcesParam resparam) {
        try {
            ClassLoader classLoader = resparam.res.getClass().getClassLoader();

            Class<?> activityThreadClass = XposedHelpers.findClass("android.app.ActivityThread", classLoader);
            Object activityThread = XposedHelpers.callStaticMethod(activityThreadClass, "currentActivityThread");

            Context systemContext = (Context) XposedHelpers.callMethod(activityThread, "getSystemContext");

            return systemContext.createPackageContext(
                    Constants.PACKAGE_NAME,
                    Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY
            );

        } catch (Throwable t) {
           // XposedBridge.log("Lime (Resources): Context creation failed: " + t);
            return null;
        }
    }
    @Override
    public void initZygote(@NonNull StartupParam startupParam) {
        modulePath = startupParam.modulePath;
    }
    @Override
    public void handleInitPackageResources(@NonNull XC_InitPackageResources.InitPackageResourcesParam resparam) throws Throwable {
        if (!resparam.packageName.equals(Constants.PACKAGE_NAME)) return;
        if (customPreferences == null) {
            try {
                context = getTargetAppContextForResources(resparam); 
                customPreferences = new CustomPreferences(context);
                loadSettings(context);
            } catch (Exception e) {
               // XposedBridge.log("Lime: Failed to load settings in handleInitPackageResources: " + e);
            }
        }
        XModuleResources xModuleResources = XModuleResources.createInstance(modulePath, resparam.res);

        if (limeOptions.removeIconLabels.checked) {
            resparam.res.setReplacement(Constants.PACKAGE_NAME, "dimen", "main_bnb_button_height", xModuleResources.fwd(R.dimen.main_bnb_button_height));
            resparam.res.setReplacement(Constants.PACKAGE_NAME, "dimen", "main_bnb_button_width", xModuleResources.fwd(R.dimen.main_bnb_button_width));
            resparam.res.hookLayout(Constants.PACKAGE_NAME, "layout", "app_main_bottom_navigation_bar_button", new XC_LayoutInflated() {
                @Override
                public void handleLayoutInflated(XC_LayoutInflated.LayoutInflatedParam liparam) throws Throwable {
                    liparam.view.setTranslationY(xModuleResources.getDimensionPixelSize(R.dimen.gnav_icon_offset));
                }
            });
        }
        if (limeOptions.removeSearchBar.checked) {
            resparam.res.hookLayout(Constants.PACKAGE_NAME, "layout", "main_tab_search_bar", new XC_LayoutInflated() {
                @Override
                public void handleLayoutInflated(XC_LayoutInflated.LayoutInflatedParam liparam) throws Throwable {
                    liparam.view.setVisibility(View.GONE);
                }
            });
        }
        if (limeOptions.RemoveNotification.checked) {
            resparam.res.hookLayout(Constants.PACKAGE_NAME, "layout", "home_list_row_friend_profile_update_carousel", new XC_LayoutInflated() {
                @Override
                public void handleLayoutInflated(XC_LayoutInflated.LayoutInflatedParam liparam) throws Throwable {
                    liparam.view.setVisibility(View.GONE);
                }
            });

            resparam.res.hookLayout(Constants.PACKAGE_NAME, "layout", "home_list_row_friend_profile_update_carousel_item", new XC_LayoutInflated() {
                @Override
                public void handleLayoutInflated(XC_LayoutInflated.LayoutInflatedParam liparam) throws Throwable {
                    liparam.view.setVisibility(View.GONE);
                }
            });
                 }
        if (limeOptions.WhiteToDark.checked) {
            resparam.res.hookLayout(Constants.PACKAGE_NAME, "layout", "main_tab_search_bar", new XC_LayoutInflated() {
                @Override
                public void handleLayoutInflated(XC_LayoutInflated.LayoutInflatedParam liparam) throws Throwable {
                    ViewGroup rootView = (ViewGroup) liparam.view;
                    setAllViewsToBlack(rootView);
                }

                private void setAllViewsToBlack(View view) {
                    if (view instanceof ViewGroup) {
                        ViewGroup viewGroup = (ViewGroup) view;
                        for (int i = 0; i < viewGroup.getChildCount(); i++) {
                            setAllViewsToBlack(viewGroup.getChildAt(i));
                        }
                    }
                    view.setBackgroundColor(Color.BLACK);
                    if (view instanceof TextView) {
                        ((TextView) view).setTextColor(Color.BLACK);
                    }
                }
            });
        }
        if (limeOptions.removeNaviAlbum.checked) {
            resparam.res.setReplacement(Constants.PACKAGE_NAME, "drawable", "navi_top_albums", xModuleResources.fwd(R.drawable.empty_drawable));
            resparam.res.setReplacement(Constants.PACKAGE_NAME, "drawable", "badge_dot_green", xModuleResources.fwd(R.drawable.empty_drawable));

        }

        if (limeOptions.removeNewsOrCall.checked) {

            resparam.res.setReplacement(Constants.PACKAGE_NAME, "drawable", "navi_bottom_news_new", xModuleResources.fwd(R.drawable.empty_drawable));
            resparam.res.setReplacement(Constants.PACKAGE_NAME, "drawable", "navi_bottom_news_new_dark", xModuleResources.fwd(R.drawable.empty_drawable));
        }
        if (limeOptions.removeWallet.checked) {
            resparam.res.setReplacement(Constants.PACKAGE_NAME, "drawable", "navi_bottom_wallet_new", xModuleResources.fwd(R.drawable.empty_drawable));
            resparam.res.setReplacement(Constants.PACKAGE_NAME, "drawable", "navi_bottom_wallet_new_dark", xModuleResources.fwd(R.drawable.empty_drawable));
        }
        if (limeOptions.removeVoom.checked) {
            resparam.res.setReplacement(Constants.PACKAGE_NAME, "drawable", "navi_bottom_voom_new", xModuleResources.fwd(R.drawable.empty_drawable));
            resparam.res.setReplacement(Constants.PACKAGE_NAME, "drawable", "navi_bottom_voom_new_dark", xModuleResources.fwd(R.drawable.empty_drawable));
        }



        if (limeOptions.removeNaviOpenchat.checked) {
            resparam.res.setReplacement(Constants.PACKAGE_NAME, "drawable", "navi_top_openchat", xModuleResources.fwd(R.drawable.empty_drawable));
        }
        resparam.res.setReplacement(Constants.PACKAGE_NAME, "drawable", "freecall_bottom_photobooth", xModuleResources.fwd(R.drawable.empty_drawable));
        if (limeOptions.RemoveVoiceRecord.checked) {
            resparam.res.setReplacement(Constants.PACKAGE_NAME, "drawable", "chat_ui_input_ic_voice_normal", xModuleResources.fwd(R.drawable.empty_drawable));
            resparam.res.setReplacement(Constants.PACKAGE_NAME, "drawable", "chat_ui_input_ic_voice_pressed", xModuleResources.fwd(R.drawable.empty_drawable));
        
        }

        resparam.res.setReplacement(Constants.PACKAGE_NAME, "dimen", "chat_ui_photobooth_floating_btn_height", xModuleResources.fwd(R.dimen.main_bnb_button_width));
        resparam.res.setReplacement(Constants.PACKAGE_NAME, "dimen", "chat_ui_photobooth_top_margin", xModuleResources.fwd(R.dimen.main_bnb_button_width));

        if (limeOptions.removeServiceLabels.checked) {
            resparam.res.setReplacement(Constants.PACKAGE_NAME, "dimen", "home_tab_v3_service_icon_size", xModuleResources.fwd(R.dimen.home_tab_v3_service_icon_size));
        }
    }
}
