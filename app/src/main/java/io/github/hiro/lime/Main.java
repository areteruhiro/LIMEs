package io.github.hiro.lime;

import android.content.res.XModuleResources;

import android.view.View;

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

public class Main implements IXposedHookLoadPackage, IXposedHookInitPackageResources, IXposedHookZygoteInit {

    public static String modulePath;
    public static CustomPreferences customPreferences;
    public static XSharedPreferences xModulePrefs;
    public static XSharedPreferences xPackagePrefs;
    public static XSharedPreferences xPrefs;
    public static LimeOptions limeOptions = new LimeOptions();

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
            new Archived(),
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
    };

    @Override
    public void initZygote(@NonNull StartupParam startupParam) {
        modulePath = startupParam.modulePath;
    }

    private void initializePreferences() {
        if (customPreferences == null) {
            try {
                customPreferences = new CustomPreferences();
                createDefaultSettings();
            } catch (Exception e) {
                XposedBridge.log("Failed to initialize preferences: " + e);
            }
        }
    }

    private void createDefaultSettings() {
        for (LimeOptions.Option option : limeOptions.options) {
            String currentValue = customPreferences.getSetting(option.name, null);
            if (currentValue == null) {
                customPreferences.saveSetting(option.name, String.valueOf(option.checked));
            }
        }
    }

    @Override
    public void handleLoadPackage(@NonNull XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals(Constants.PACKAGE_NAME)) return;
        Constants.initializeHooks(lpparam);
        initializePreferences();

        xModulePrefs = new XSharedPreferences(Constants.MODULE_NAME, "options");
        xModulePrefs.reload();

        xPrefs = xModulePrefs;
        XposedBridge.log("Using module preferences");

        for (LimeOptions.Option option : limeOptions.options) {
            option.checked = Boolean.parseBoolean(
                    customPreferences.getSetting(option.name, String.valueOf(option.checked))
            );
        }

        for (IHook hook : hooks) {
            hook.hook(limeOptions, lpparam);
        }
    }
    @Override
    public void handleInitPackageResources(@NonNull XC_InitPackageResources.InitPackageResourcesParam resparam) throws Throwable {
        if (!resparam.packageName.equals(Constants.PACKAGE_NAME)) return;

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
        if (limeOptions.removeNaviAlbum.checked) {
            resparam.res.setReplacement(Constants.PACKAGE_NAME, "drawable", "navi_top_albums", xModuleResources.fwd(R.drawable.empty_drawable));
                 }
        if (limeOptions.removeNaviOpenchat.checked) {
            resparam.res.setReplacement(Constants.PACKAGE_NAME, "drawable", "navi_top_openchat", xModuleResources.fwd(R.drawable.empty_drawable));
        }
        if (limeOptions.RemoveVoiceRecord.checked) {
            resparam.res.setReplacement(Constants.PACKAGE_NAME, "drawable", "chat_ui_input_ic_voice_normal", xModuleResources.fwd(R.drawable.empty_drawable));
            resparam.res.setReplacement(Constants.PACKAGE_NAME, "drawable", "chat_ui_input_ic_voice_pressed", xModuleResources.fwd(R.drawable.empty_drawable));
            resparam.res.setReplacement(Constants.PACKAGE_NAME, "drawable", "chat_ui_ic_alert_x", xModuleResources.fwd(R.drawable.empty_drawable));
            resparam.res.setReplacement(Constants.PACKAGE_NAME, "drawable", "chat_ui_ic_alert_overlay_x", xModuleResources.fwd(R.drawable.empty_drawable));
            resparam.res.setReplacement(Constants.PACKAGE_NAME, "drawable", "chat_ui_chatroom_layer_x", xModuleResources.fwd(R.drawable.empty_drawable));
            resparam.res.setReplacement(Constants.PACKAGE_NAME, "drawable", "chatroom_layer_x", xModuleResources.fwd(R.drawable.empty_drawable));
            resparam.res.setReplacement(Constants.PACKAGE_NAME, "drawable", "chatroom_layer_x_oa", xModuleResources.fwd(R.drawable.empty_drawable));

        }
        if (limeOptions.removeServiceLabels.checked) {
            resparam.res.setReplacement(Constants.PACKAGE_NAME, "dimen", "home_tab_v3_service_icon_size", xModuleResources.fwd(R.dimen.home_tab_v3_service_icon_size));
        }
    }
}