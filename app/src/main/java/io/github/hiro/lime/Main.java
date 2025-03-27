package io.github.hiro.lime;

import android.app.AndroidAppHelper;
import android.content.Context;
import android.content.res.XModuleResources;

import android.os.Build;
import android.view.View;

import androidx.annotation.NonNull;
import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
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
            new BlockCheck(),
            new AutoUpdate(),
            new Removebutton()
            ,new PhotoSave()
    };




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

        context = getTargetAppContext(lpparam);

        if (context == null) {
           // XposedBridge.log("Lime: Context acquisition failed after all methods");
            return;
        }
        try {
            customPreferences = new CustomPreferences(context);
            createDefaultSettings();
        } catch (Exception e) {
     XposedBridge.log("Lime: Preferences init failed: " + e);
            return;
        }

        Constants.initializeHooks(lpparam);

        loadSettings();
        for (IHook hook : hooks) {
            hook.hook(limeOptions, lpparam);
        }
    }

    private void loadSettings() {
        for (LimeOptions.Option option : limeOptions.options) {
            option.checked = Boolean.parseBoolean(
                    customPreferences.getSetting(option.name, String.valueOf(option.checked))
            );
            String defaultValue = String.valueOf(option.checked);
            String currentValue = customPreferences.getSetting(option.name, defaultValue);

           // XposedBridge.log("Lime: Loaded setting - "+ option.name+ " | Value: " + currentValue+ " | Default: " + defaultValue);
        }
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
                loadSettings(); 
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
