package io.github.hiro.lime;

import android.content.res.XModuleResources;
import android.os.Environment;
import androidx.annotation.NonNull;
import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LayoutInflated;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.hiro.lime.hooks.*;

import java.io.File;

public class Main implements IXposedHookLoadPackage, IXposedHookInitPackageResources, IXposedHookZygoteInit {

    public static String modulePath;
    public static CustomPreferences customPreferences;
    public static XSharedPreferences xModulePrefs;
    public static XSharedPreferences xPackagePrefs;
    public static XSharedPreferences xPrefs;
    public static LimeOptions limeOptions = new LimeOptions();

    static final IHook[] hooks = new IHook[]{
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
            new Archived(),
            new UnsentRec(),
            new RingTone(),
            new ReadChecker(),
            new DarkColor(),
            new KeepUnreadLSpatch(),
            new AutomaticBackup(),
            new RemoveNotification(),
            new Disabled_Group_notification(),
            new PhotoAddNotification(),
            new RemoveVoiceRecord(),
            new AgeCheckSkip()
    };

    @Override
    public void initZygote(@NonNull StartupParam startupParam) throws Throwable {
        modulePath = startupParam.modulePath;
        customPreferences = new CustomPreferences(); // CustomPreferences を初期化
    }

    public void handleLoadPackage(@NonNull XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        if (!loadPackageParam.packageName.equals(Constants.PACKAGE_NAME)) return;
        Constants.initializeHooks(loadPackageParam);

        xModulePrefs = new XSharedPreferences(Constants.MODULE_NAME, "options");
        xPackagePrefs = new XSharedPreferences(Constants.PACKAGE_NAME, Constants.MODULE_NAME + "-options");

        // 設定ファイルを再読み込み
        xModulePrefs.reload();
        xPackagePrefs.reload();

        // unembed_optionsの値をログに出力
        boolean unembedOptions = xModulePrefs.getBoolean("unembed_options", false);
        XposedBridge.log("unembed_options: " + unembedOptions);

        if (unembedOptions) {
            xPrefs = xModulePrefs;
            XposedBridge.log("Using module preferences");

            // xModulePrefsから設定を読み込む
            for (LimeOptions.Option option : limeOptions.options) {
                option.checked = xModulePrefs.getBoolean(option.name, option.checked);
            }
        } else {
            xPrefs = xPackagePrefs;
            XposedBridge.log("Using package preferences");

            // customPreferencesから設定を読み込む
            for (LimeOptions.Option option : limeOptions.options) {
                option.checked = Boolean.parseBoolean(customPreferences.getSetting(option.name, String.valueOf(option.checked)));
            }
        }

        // 各フックを適用
        for (IHook hook : hooks) {
            hook.hook(limeOptions, loadPackageParam);
        }
    }

    @Override
    public void handleInitPackageResources(@NonNull XC_InitPackageResources.InitPackageResourcesParam resparam) throws Throwable {
        if (!resparam.packageName.equals(Constants.PACKAGE_NAME))
            return;

        XModuleResources xModuleResources = XModuleResources.createInstance(modulePath, resparam.res);

        // 既存のリソースフック
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

        if (limeOptions.removeServiceLabels.checked) {
            resparam.res.setReplacement(Constants.PACKAGE_NAME, "dimen", "home_tab_v3_service_icon_size", xModuleResources.fwd(R.dimen.home_tab_v3_service_icon_size));
        }
    }
}