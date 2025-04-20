package io.github.hiro.lime.hooks;

import android.app.AndroidAppHelper;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.hiro.lime.LimeOptions;

public class SpoofAndroidId implements IHook {

    @Override
    public void hook(LimeOptions limeOptions, XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        Context context = getTargetAppContext(loadPackageParam);
        XposedHelpers.findAndHookMethod(
                Settings.Secure.class,
                "getString",
                ContentResolver.class,
                String.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (Settings.Secure.ANDROID_ID.equals(param.args[1])) {
                            try {
                                CustomPreferences customPrefs = new CustomPreferences(context);
                                boolean isSpoofEnabled = Boolean.parseBoolean(
                                        customPrefs.getSetting("spoof_android_id", "false"));

                                if (isSpoofEnabled) {
                                    param.setResult("0000000000000000");
                                    XposedBridge.log("Lime: Android ID spoofing activated");
                                }
                            } catch (PackageManager.NameNotFoundException ignored) {
                                        }
                        }
                    }
                }
        );


    }
    private Context getTargetAppContext(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        Context context;

        
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
            Class<?> activityThreadClass = XposedHelpers.findClass("android.app.ActivityThread", loadPackageParam.classLoader);
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
                    XposedHelpers.findClass("android.app.ContextImpl", loadPackageParam.classLoader),
                    "createSystemContext",
                    XposedHelpers.callStaticMethod(
                            XposedHelpers.findClass("android.app.ActivityThread", loadPackageParam.classLoader),
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
}