package io.github.hiro.lime.hooks;

import android.content.res.Resources;
import android.os.Handler;
import android.os.Looper;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.hiro.lime.LimeOptions;
public class RemoveVoiceRecord implements IHook {

    @Override
    public void hook(LimeOptions limeOptions, XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        if (!limeOptions.RemoveVoiceRecord.checked) return;
        final boolean[] shouldProceed = {true};
        final boolean[] isDelayActive = {false};

        XposedHelpers.findAndHookMethod(
                "android.content.res.Resources",
                loadPackageParam.classLoader,
                "getString",
                int.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        int resourceId = (int) param.args[0];
                        Resources resources = (Resources) param.thisObject;

                        try {

                            String resourceName = resources.getResourceName(resourceId);
                            String entryName = resourceName.substring(resourceName.lastIndexOf('/') + 1);
                            if ("chathistory_attach_dialog_label_file".equals(entryName)) {
                                shouldProceed[0] = false;
                                isDelayActive[0] = true;
                                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                    shouldProceed[0] = true;
                                    isDelayActive[0] = false;
                                }, 1000);
                            } else if ("access_chat_button_voicemessage".equals(entryName)) {
                                if (!isDelayActive[0]) {
                                    shouldProceed[0] = true;
                                }
                            }
                        } catch (Resources.NotFoundException ignored) {
                        }
                    }
                }
        );
        XposedBridge.hookAllMethods(
                loadPackageParam.classLoader.loadClass(Constants.RemoveVoiceRecord_Hook_a.className),
                "run",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {

                        if (shouldProceed[0]) {
                            param.setResult(null);
                        }
                    }
                }
        );
    }
}