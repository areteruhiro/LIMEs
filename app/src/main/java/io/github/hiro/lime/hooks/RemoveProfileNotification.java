package io.github.hiro.lime.hooks;

import android.content.res.Resources;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.hiro.lime.LimeOptions;

public class RemoveProfileNotification implements IHook {
    private static boolean isHandlingHook = false;
    @Override
    public void hook(LimeOptions limeOptions, XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        if (!limeOptions.RemoveNotification.checked) return;
        XposedHelpers.findAndHookMethod(
                "android.content.res.Resources",
                loadPackageParam.classLoader,
                "getString",
                int.class,
                new XC_MethodHook() {

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (isHandlingHook) {
                            return;
                        }
                        int resourceId = (int) param.args[0];
                        Resources resources = (Resources) param.thisObject;
                        try {
                            isHandlingHook = true;

                            String resourceName;
                            try {
                                resourceName = resources.getResourceName(resourceId);
                            } catch (Resources.NotFoundException e) {
                                return;
                            }
                            String entryName = resourceName.substring(resourceName.lastIndexOf('/') + 1);
                            if ("line_home_header_recentlyupdatedsection".equals(entryName)) {
                                param.setResult(""); // 空文字列を返す
                            }
                        } finally {
                            isHandlingHook = false;
                        }
                    }
                }
        );
    }
}