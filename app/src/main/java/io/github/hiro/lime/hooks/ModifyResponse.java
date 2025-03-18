package io.github.hiro.lime.hooks;

import android.app.AndroidAppHelper;
import android.os.Build;
import android.util.Base64;
import android.content.Context; // Explicit import for Android Context

import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.hiro.lime.LimeOptions;

public class ModifyResponse implements IHook {
    @Override
    public void hook(LimeOptions limeOptions, XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {

        Context context = getTargetAppContext(loadPackageParam);

        XposedBridge.hookAllMethods(
                loadPackageParam.classLoader.loadClass(Constants.RESPONSE_HOOK.className),
                Constants.RESPONSE_HOOK.methodName,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {

                        CustomPreferences customPreferences = new CustomPreferences(context);

                        final String script = new String(Base64.decode(
                                customPreferences.getSetting("encoded_js_modify_response", ""),
                                Base64.NO_WRAP
                        ));


                        org.mozilla.javascript.Context rhinoContext = org.mozilla.javascript.Context.enter();
                        rhinoContext.setOptimizationLevel(-1);
                        try {
                            Scriptable scope = rhinoContext.initStandardObjects();

                            Object jsData = org.mozilla.javascript.Context.javaToJS(
                                    new Communication(Communication.Type.RESPONSE, param.args[0].toString(), param.args[1]),
                                    scope
                            );

                            ScriptableObject.putProperty(scope, "data", jsData);
                            ScriptableObject.putProperty(scope, "console",
                                    org.mozilla.javascript.Context.javaToJS(new Console(), scope)
                            );

                            rhinoContext.evaluateString(scope, script, "Script", 1, null);
                        } catch (Exception e) {
                            XposedBridge.log(e.toString());
                        } finally {
                            org.mozilla.javascript.Context.exit();
                        }
                    }
                }
        );
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
}