package io.github.hiro.lime.hooks;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.hiro.lime.LimeOptions;

public class BlockTracking implements IHook {
    @Override
    public void hook(LimeOptions limeOptions, XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        if (!limeOptions.blockTracking.checked) return;
/*
        XposedBridge.hookAllMethods(
                loadPackageParam.classLoader.loadClass(Constants.REQUEST_HOOK.className),
                Constants.REQUEST_HOOK.methodName,
@@ -37,5 +39,58 @@ protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    }
                }
        );
*/
        String[] classesToHook = {
                "me1.Za", "me1.Fb", "me1.Hc", "me1.Hb",
                "me1.hd", "me1.jd",  "me1.Lb", "me1.Nb"
        };
        String[] methodsToHook = {"read", "write"};
        for (String className : classesToHook) {
            for (String methodName : methodsToHook) {
                XposedBridge.hookAllMethods(
                        loadPackageParam.classLoader.loadClass(className),
                        methodName,
                        new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                param.setResult(null);
                            }
                        }
                );
            }
        }
        // Hook additional specific methods in 'me1.d8'
        String d8Class = "me1.d8";
        String[] d8Methods = { "L", "M"};
        for (String methodName : d8Methods) {
            XposedBridge.hookAllMethods(
                    loadPackageParam.classLoader.loadClass(d8Class),
                    methodName,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            param.setResult(null);
                        }
                    }
            );
        }
         XposedBridge.hookAllMethods(
                loadPackageParam.classLoader.loadClass("jp.naver.line.android.thrift.client.impl.LegacyTalkServiceClientImpl"),
                "F2",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        param.setResult(null);
                    }
                }
        );
    }
}