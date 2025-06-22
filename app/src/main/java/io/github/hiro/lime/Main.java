package io.github.hiro.lime;

import android.app.Activity;
import android.app.AndroidAppHelper;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.XModuleResources;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.provider.DocumentsContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

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
    private static final String TARGET_PACKAGE = "jp.naver.line.android";
    private static boolean isContextInitialized = false;
    public static String modulePath;
    public static LimeOptions limeOptions = new LimeOptions();
    public static CustomPreferences customPreferences;
    private static Context context; // Static context to be shared
    private static Context mContext; // staticに変更して共有
    static final IHook[] hooks = {
            new InstallModule(),
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
            new DisableSilentMessage(),
            new NotificationReaction(),
    };

    private boolean isSettingsLoaded = false; // 設定ロード状態を追跡

    private synchronized void ensureContextAndLoad(XC_LoadPackage.LoadPackageParam lpparam) {
        if (mContext == null) {
            mContext = AndroidAppHelper.currentApplication();
            if (mContext == null) {
                mContext = getTargetAppContext(lpparam);
                XposedBridge.log("Lime: [CONTEXT] Fallback context created");
            }
        }

        if (!isSettingsLoaded && mContext != null) {
            limeOptions = new LimeOptions();
            loadSettings(mContext);
            isSettingsLoaded = true;
            logCurrentSettings();
        }
    }

    private void logCurrentSettings() {
        for (LimeOptions.Option option : limeOptions.options) {

        }
    }

    @Override
    public void handleLoadPackage(@NonNull XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals(Constants.PACKAGE_NAME)) return;
        ensureContextAndLoad(lpparam);
        if (mContext == null) {
            XposedBridge.log("Lime: Context acquisition failed in handleLoadPackage");
            return;
        }

        try {
            String backupUri = loadBackupUri(mContext);
            if (backupUri != null) {
                Uri treeUri = Uri.parse(backupUri);
                DocumentPreferences docPrefs = new DocumentPreferences(mContext, treeUri);
                docPrefs.loadSettings(limeOptions);
                XposedBridge.log("Lime: Settings loaded from DocumentPreferences");
            } else {
                setupUriConfiguration(lpparam);
                XposedBridge.log("Lime: Settings URI not configured");
            }
        } catch (Exception e) {
            XposedBridge.log("Lime: Error loading settings: " + e.getMessage());
            setupUriConfiguration(lpparam);
        }

        Constants.initializeHooks(lpparam);
        for (IHook hook : hooks) {
            hook.hook(limeOptions, lpparam);
        }
    }

    private Button createConfigButton(Context context, XC_MethodHook.MethodHookParam param) {
        Button button = new Button(context);
        button.setText("Setting Directory");
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
            prefs.loadSettings(limeOptions);
            Toast.makeText(context, "設定を正常に読み込みました", Toast.LENGTH_LONG).show();
            android.os.Process.killProcess(Process.myPid());
            context.startActivity(new Intent().setClassName(Constants.PACKAGE_NAME, "jp.naver.line.android.activity.SplashActivity"));

        } catch (Exception e) {
            Toast.makeText(context, "設定の読み込みに失敗: " + e.getMessage(), Toast.LENGTH_LONG).show();
            XposedBridge.log("Lime Settings Error: " + e.getMessage());
        }
    }

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

                        String baseName = "androidx.fragment.app."; //
                        List<String> validClasses = new ArrayList<>();

                        for (char c = 'a'; c <= 'z'; c++) {
                            String className = baseName + c;
                            try {

                                Class<?> clazz = Class.forName(className, false, lpparam.classLoader);

                                try {
                                    clazz.getDeclaredMethod("onActivityResult", int.class, int.class, Intent.class);
                                    validClasses.add(className);
                                    XposedBridge.log("Found valid fragment class: " + className);
                                } catch (NoSuchMethodException ignored) {

                                }
                            } catch (ClassNotFoundException ignored) {
                            }
                        }

                        if (validClasses.isEmpty()) {
                            XposedBridge.log("No valid fragment class found with onActivityResult method");
                            return;
                        }
                        for (String fragmentClass : validClasses) {
                            try {
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
                                XposedBridge.log("Successfully hooked onActivityResult in: " + fragmentClass);
                            } catch (Throwable t) {
                                XposedBridge.log("Failed to hook onActivityResult in " + fragmentClass + ": " + t.getMessage());
                            }
                        }
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
        // contextがnullでないことを確認
        if (context == null) {
            XposedBridge.log("Lime: Context is null in saveBackupUri");
            return;
        }

        File settingsDir = new File(context.getFilesDir(), "LimeBackup");
        if (!settingsDir.exists()) {
            if (!settingsDir.mkdirs()) {
                XposedBridge.log("Lime: Failed to create LimeBackup directory");
                return;
            }
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
        // contextがnullでないことを確認
        if (context == null) {
            XposedBridge.log("Lime: Context is null in loadBackupUri");
            return null;
        }

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
        String backupUri = loadBackupUri(mContext);
        Uri treeUri = Uri.parse(backupUri);
        DocumentPreferences prefs = new DocumentPreferences(mContext, treeUri);
        try {
            for (LimeOptions.Option option : limeOptions.options) {
                String value = prefs.getSetting(option.name, null);
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
        public SettingsLoadException(String message) {
            super(message);
        }

        public SettingsLoadException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private void showToast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    private Context getTargetAppContext(XC_LoadPackage.LoadPackageParam lpparam) {
        Context context = null;

        // 方法1: AndroidAppHelperを使用 (最優先)
        try {
            context = AndroidAppHelper.currentApplication();
            if (context != null) {
                XposedBridge.log("Lime: Got target context via AndroidAppHelper: " + context.getPackageName());
                return context; // ターゲットアプリのコンテキストを直接返す
            }
        } catch (Throwable t) {
            XposedBridge.log("Lime: AndroidAppHelper failed: " + t.toString());
        }

        // 方法2: ActivityThreadからアプリケーションコンテキストを取得
        try {
            Class<?> activityThreadClass = XposedHelpers.findClass("android.app.ActivityThread", lpparam.classLoader);
            Object activityThread = XposedHelpers.callStaticMethod(activityThreadClass, "currentActivityThread");

            // 優先: getApplication() で直接アプリケーションコンテキスト取得
            try {
                context = (Context) XposedHelpers.callMethod(activityThread, "getApplication");
                if (context != null) {
                    XposedBridge.log("Lime: Got target context via ActivityThread.getApplication()");
                    return context; // ターゲットアプリのコンテキストを直接返す
                }
            } catch (Throwable t) {
                XposedBridge.log("Lime: getApplication() failed: " + t.toString());
            }

            // 代替: mInitialApplication フィールドから取得
            try {
                context = (Context) XposedHelpers.getObjectField(activityThread, "mInitialApplication");
                if (context != null) {
                    XposedBridge.log("Lime: Got target context via mInitialApplication");
                    return context;
                }
            } catch (Throwable t) {
                XposedBridge.log("Lime: mInitialApplication failed: " + t.toString());
            }

            // 最終手段: ロードされたAPKからコンテキスト作成
            try {
                Object loadedApk = XposedHelpers.getObjectField(activityThread, "mBoundApplication");
                Object appInfo = XposedHelpers.getObjectField(loadedApk, "info");

                // ContextImpl.createAppContext を直接呼び出す
                Class<?> contextImplClass = XposedHelpers.findClass("android.app.ContextImpl", lpparam.classLoader);
                context = (Context) XposedHelpers.callStaticMethod(
                        contextImplClass,
                        "createAppContext",
                        activityThread,
                        appInfo
                );

                if (context != null) {
                    XposedBridge.log("Lime: Created target context via ContextImpl.createAppContext");
                    return context;
                }
            } catch (Throwable t) {
                XposedBridge.log("Lime: ContextImpl.createAppContext failed: " + t.toString());
            }
        } catch (Throwable t) {
            XposedBridge.log("Lime: ActivityThread method failed: " + t.toString());
        }

        // 方法3: フォールバック (システムコンテキストは使用しない)
        XposedBridge.log("Lime: All context acquisition methods failed");
        return null;
    }

    /**
     * リソースフック用のコンテキスト取得 (デバッグログ強化版)
     */

    @Override
    public void initZygote(@NonNull StartupParam startupParam) {
        modulePath = startupParam.modulePath;
    }

    private Context getReliableContext(XC_InitPackageResources.InitPackageResourcesParam resparam) {
        // 方法1: キャッシュされたコンテキストがあるか確認
        if (mContext != null && !isContextInvalid(mContext)) {
            XposedBridge.log("Lime: [RES] Using cached context");
            return mContext;
        }

        // 方法2: AndroidAppHelperから取得
        try {
            Context context = AndroidAppHelper.currentApplication();
            if (context != null && isValidTargetContext(context)) {
                mContext = context;
                XposedBridge.log("Lime: [RES] Context obtained from AndroidAppHelper");
                return context;
            }
        } catch (Throwable t) {
            XposedBridge.log("Lime: [RES] AndroidAppHelper error: " + t.getMessage());
        }

        // 方法3: ActivityThreadから取得
        try {
            Class<?> activityThreadClass = XposedHelpers.findClass("android.app.ActivityThread", null);
            Object activityThread = XposedHelpers.callStaticMethod(activityThreadClass, "currentActivityThread");

            // 3.1: getApplication()を試す
            try {
                Context appContext = (Context) XposedHelpers.callMethod(activityThread, "getApplication");
                if (appContext != null && isValidTargetContext(appContext)) {
                    mContext = appContext;
                    XposedBridge.log("Lime: [RES] Context obtained from ActivityThread.getApplication()");
                    return appContext;
                }
            } catch (Throwable t) {
                XposedBridge.log("Lime: [RES] getApplication() failed: " + t.getMessage());
            }

            // 3.2: getSystemContext()を試す
            try {
                Context systemContext = (Context) XposedHelpers.callMethod(activityThread, "getSystemContext");
                if (systemContext != null) {
                    Context packageContext = systemContext.createPackageContext(
                            TARGET_PACKAGE,
                            Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY
                    );
                    if (packageContext != null && isValidTargetContext(packageContext)) {
                        mContext = packageContext;
                        XposedBridge.log("Lime: [RES] Context obtained from ActivityThread.getSystemContext()");
                        return packageContext;
                    }
                }
            } catch (Throwable t) {
                XposedBridge.log("Lime: [RES] getSystemContext() failed: " + t.getMessage());
            }

            // 3.3: mInitialApplicationから取得
            try {
                Object initialApp = XposedHelpers.getObjectField(activityThread, "mInitialApplication");
                if (initialApp instanceof Context && isValidTargetContext((Context) initialApp)) {
                    mContext = (Context) initialApp;
                    XposedBridge.log("Lime: [RES] Context obtained from mInitialApplication");
                    return (Context) initialApp;
                }
            } catch (Throwable t) {
                XposedBridge.log("Lime: [RES] mInitialApplication failed: " + t.getMessage());
            }
        } catch (Throwable t) {
            XposedBridge.log("Lime: [RES] ActivityThread error: " + t.getMessage());
        }

        // 方法4: Resourcesから逆探査
        try {
            Object resourcesImpl = XposedHelpers.getObjectField(resparam.res, "mResourcesImpl");
            Object assets = XposedHelpers.getObjectField(resourcesImpl, "mAssets");
            Context context = (Context) XposedHelpers.getObjectField(assets, "mContext");
            if (context != null && isValidTargetContext(context)) {
                mContext = context;
                XposedBridge.log("Lime: [RES] Context obtained from Resources");
                return context;
            }
        } catch (Throwable t) {
            XposedBridge.log("Lime: [RES] Resources extraction error: " + t.getMessage());
        }

        // 方法5: ContextImplを直接作成
        try {
            Class<?> contextImplClass = Class.forName("android.app.ContextImpl");
            Method createMethod = contextImplClass.getMethod("createPackageContext", String.class, int.class);

            Context context = (Context) createMethod.invoke(
                    null,
                    TARGET_PACKAGE,
                    Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY
            );

            if (context != null && isValidTargetContext(context)) {
                mContext = context;
                XposedBridge.log("Lime: [RES] Context created via ContextImpl");
                return context;
            }
        } catch (Throwable t) {
            XposedBridge.log("Lime: [RES] ContextImpl creation error: " + t.getMessage());
        }

        // 方法6: 対象アプリのクラスローダーを使用
        try {
            ClassLoader targetClassLoader = findTargetClassLoader();
            if (targetClassLoader != null) {
                Context context = (Context) XposedHelpers.callStaticMethod(
                        XposedHelpers.findClass("android.app.AppGlobals", targetClassLoader),
                        "getInitialApplication"
                );

                if (context != null && isValidTargetContext(context)) {
                    mContext = context;
                    XposedBridge.log("Lime: [RES] Context obtained via target classloader");
                    return context;
                }
            }
        } catch (Throwable t) {
            XposedBridge.log("Lime: [RES] Target classloader method failed: " + t.getMessage());
        }

        XposedBridge.log("Lime: [RES] WARNING: All context acquisition methods failed!");
        return null;

    }

    /**
     * コンテキストが有効かどうかをチェック
     */
    private boolean isValidTargetContext(Context context) {
        try {
            return context != null &&
                    TARGET_PACKAGE.equals(context.getPackageName()) &&
                    context.getResources() != null;
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * コンテキストが無効かどうかをチェック
     */
    private boolean isContextInvalid(Context context) {
        try {
            return context == null ||
                    context.getPackageName() == null ||
                    context.getResources() == null;
        } catch (Throwable t) {
            return true;
        }
    }

    /**
     * 対象アプリのクラスローダーを検索
     */
    private ClassLoader findTargetClassLoader() {
        try {
            // 方法1: 現在のスレッドのコンテキストクラスローダー
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if (cl != null && cl.toString().contains(TARGET_PACKAGE)) {
                return cl;
            }

            // 方法2: ActivityThreadから取得
            Object activityThread = XposedHelpers.callStaticMethod(
                    XposedHelpers.findClass("android.app.ActivityThread", null),
                    "currentActivityThread"
            );

            Object boundApplication = XposedHelpers.getObjectField(activityThread, "mBoundApplication");
            Object loadedApk = XposedHelpers.getObjectField(boundApplication, "info");
            return (ClassLoader) XposedHelpers.getObjectField(loadedApk, "mClassLoader");
        } catch (Throwable t) {
            XposedBridge.log("Lime: [RES] Failed to find target classloader: " + t.getMessage());
            return null;
        }
    }

    private Context getApplicationContext(ClassLoader classLoader) {
        try {
            Object activityThread = XposedHelpers.callStaticMethod(
                    XposedHelpers.findClass("android.app.ActivityThread", classLoader),
                    "currentActivityThread"
            );

            return (Context) XposedHelpers.callMethod(activityThread, "getApplication");
        } catch (Throwable t) {
            XposedBridge.log("Lime: [LOAD] getApplicationContext failed: " + t.getMessage());
            return null;
        }
    }

    private synchronized Context initializeContext(Object resparam) {
        if (isContextInitialized) return null;

        try {
            // 方法1: AndroidAppHelperから取得
            mContext = AndroidAppHelper.currentApplication();
            if (mContext != null && TARGET_PACKAGE.equals(mContext.getPackageName())) {
                isContextInitialized = true;
                XposedBridge.log("Lime: Context initialized via AndroidAppHelper");
                return null;
            }

// 方法2: ActivityThreadから取得
            try {
                // ClassLoaderを安全に取得する
                ClassLoader classLoader = null;

                // 方法2: デフォルトのClassLoaderを使用
                if (classLoader == null) {
                    classLoader = XposedBridge.BOOTCLASSLOADER;
                }

                // ActivityThreadを取得
                Object activityThread = XposedHelpers.callStaticMethod(
                        XposedHelpers.findClass("android.app.ActivityThread", classLoader),
                        "currentActivityThread"
                );

                // getSystemContext()を試す
                Context systemContext = (Context) XposedHelpers.callMethod(activityThread, "getSystemContext");
                if (systemContext != null) {
                    try {
                        Context packageContext = systemContext.createPackageContext(
                                TARGET_PACKAGE,
                                Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY
                        );
                        if (packageContext != null && isValidTargetContext(packageContext)) {
                            mContext = packageContext;
                            XposedBridge.log("Lime: Got context via ActivityThread.getSystemContext()");
                            return packageContext;
                        }
                    } catch (PackageManager.NameNotFoundException e) {
                        XposedBridge.log("Lime: Package not found: " + e.getMessage());
                    }
                }
            } catch (Throwable t) {
                XposedBridge.log("Lime: ActivityThread method failed: " + Log.getStackTraceString(t));
            }

            isContextInitialized = true;
            XposedBridge.log("Lime: Context initialized via ContextImpl.createAppContext");

        } catch (Throwable t) {
            XposedBridge.log("Lime: Context initialization failed: " + Log.getStackTraceString(t));
        }
        return null;
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
    public void handleInitPackageResources(@NonNull XC_InitPackageResources.InitPackageResourcesParam resparam) throws Throwable {
        if (!resparam.packageName.equals(Constants.PACKAGE_NAME)) return;
        if (customPreferences == null) {
            try {
                context = getTargetAppContextForResources(resparam);
                customPreferences = new CustomPreferences(context);
                for (LimeOptions.Option option : limeOptions.options) {
                    String value = customPreferences.getSetting(option.name, null);
                    if (value == null) {
                        throw new SettingsLoadException("Setting " + option.name + " not found");
                    }
                    option.checked = Boolean.parseBoolean(value);
                }
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