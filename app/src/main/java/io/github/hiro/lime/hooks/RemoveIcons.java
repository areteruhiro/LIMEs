package io.github.hiro.lime.hooks;

import static io.github.hiro.lime.Main.limeOptions;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.hiro.lime.LimeOptions;

public class RemoveIcons implements IHook {
    @Override
    public void hook(LimeOptions limeOptions, XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        XposedHelpers.findAndHookMethod(
                loadPackageParam.classLoader.loadClass("jp.naver.line.android.activity.main.MainActivity"),
                "onResume",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        Activity activity = (Activity) param.thisObject;

                        boolean isDarkMode = isDarkModeEnabled(activity);

                        handleRemoveVoom(activity, isDarkMode);

                        handleRemoveWallet(activity, isDarkMode);

                        handleRemoveNewsOrCall(activity, isDarkMode);

                        if (limeOptions.extendClickableArea.checked) {
                            int mainTabContainerResId = activity.getResources().getIdentifier("main_tab_container", "id", activity.getPackageName());
                            ViewGroup mainTabContainer = activity.findViewById(mainTabContainerResId);
                            for (int i = 2; i < mainTabContainer.getChildCount(); i += 2) {
                                ViewGroup icon = (ViewGroup) mainTabContainer.getChildAt(i);
                                ViewGroup.LayoutParams layoutParams = icon.getLayoutParams();
                                layoutParams.width = 0;
                                icon.setLayoutParams(layoutParams);

                                View clickableArea = icon.getChildAt(icon.getChildCount() - 1);
                                layoutParams = clickableArea.getLayoutParams();
                                layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
                                clickableArea.setLayoutParams(layoutParams);
                            }
                        }
                    }
                }
        );
    }

    private void handleRemoveVoom(Activity activity, boolean isDarkMode) {
        if (limeOptions.removeVoom.checked) {
            if (!limeOptions.distributeEvenly.checked && limeOptions.DarkColor.checked) {
                if (isDarkMode) {
                    setViewProperties(activity, "bnb_timeline", Color.BLACK);
                    setViewProperties(activity, "bnb_timeline_spacer", Color.BLACK);
                }
                } else {
                int timelineResId = activity.getResources().getIdentifier("bnb_timeline", "id", activity.getPackageName());
                activity.findViewById(timelineResId).setVisibility(View.GONE);
                if (limeOptions.distributeEvenly.checked) {
                    int timelineSpacerResId = activity.getResources().getIdentifier("bnb_timeline_spacer", "id", activity.getPackageName());
                    activity.findViewById(timelineSpacerResId).setVisibility(View.GONE);
                }
            }

        }
    }

    private void handleRemoveWallet(Activity activity, boolean isDarkMode) {
        if (limeOptions.removeWallet.checked) {
            if (!limeOptions.distributeEvenly.checked && limeOptions.DarkColor.checked) {
                if (limeOptions.DarkModSync.checked ) {
                    if (isDarkMode) {
                        setViewProperties(activity, "bnb_wallet", Color.BLACK);
                        setViewProperties(activity, "bnb_wallet_spacer", Color.BLACK);
                    }
                }
            }else {
                int walletResId = activity.getResources().getIdentifier("bnb_wallet", "id", activity.getPackageName());
                activity.findViewById(walletResId).setVisibility(View.GONE);
                if (limeOptions.distributeEvenly.checked) {
                    int walletSpacerResId = activity.getResources().getIdentifier("bnb_wallet_spacer", "id", activity.getPackageName());
                    activity.findViewById(walletSpacerResId).setVisibility(View.GONE);
                }
            }

        }
    }

    private void handleRemoveNewsOrCall(Activity activity, boolean isDarkMode) {
        if (limeOptions.removeNewsOrCall.checked) {
            if (!limeOptions.distributeEvenly.checked && limeOptions.DarkColor.checked) {
                if (limeOptions.DarkModSync.checked ) {
                    if (isDarkMode) {


                        setViewProperties(activity, "bnb_news", Color.BLACK);
                        setViewProperties(activity, "bnb_news_spacer", Color.BLACK);
                    }
                }
            }else {
                int newsResId = activity.getResources().getIdentifier("bnb_news", "id", activity.getPackageName());
                activity.findViewById(newsResId).setVisibility(View.GONE);

                if (limeOptions.distributeEvenly.checked) {
                    int newsSpacerResId = activity.getResources().getIdentifier("bnb_news_spacer", "id", activity.getPackageName());
                    activity.findViewById(newsSpacerResId).setVisibility(View.GONE);
                }
            }
        }


    }

    private void setViewProperties(Activity activity, String viewId, int backgroundColor) {
        int id = activity.getResources().getIdentifier(viewId, "id", activity.getPackageName());
        View view = activity.findViewById(id);
        if (view != null) {
            view.setEnabled(false);
            view.setClickable(false);
            view.setFocusable(false);
            view.setBackgroundColor(backgroundColor);
            if (view instanceof ViewGroup) {
                ViewGroup group = (ViewGroup) view;
                for (int i = 0; i < group.getChildCount(); i++) {
                    View child = group.getChildAt(i);
                    child.setBackgroundColor(backgroundColor);
                    child.setEnabled(false);
                }
            }
            int spacerId = activity.getResources().getIdentifier(viewId + "_spacer", "id", activity.getPackageName());
            View spacerView = activity.findViewById(spacerId);
            if (spacerView != null) {
                spacerView.setVisibility(View.VISIBLE);
            }
        }
    }

    private void hideViewWithSpacer(Activity activity, String mainId, String spacerId) {
        int resId = activity.getResources().getIdentifier(mainId, "id", activity.getPackageName());
        activity.findViewById(resId).setVisibility(View.GONE);
        if (limeOptions.distributeEvenly.checked) {
            int walletSpacerResId = activity.getResources().getIdentifier("bnb_wallet_spacer", "id", activity.getPackageName());
            activity.findViewById(walletSpacerResId).setVisibility(View.GONE);
            int newsSpacerResId = activity.getResources().getIdentifier("bnb_news_spacer", "id", activity.getPackageName());
            activity.findViewById(newsSpacerResId).setVisibility(View.GONE);

                int timelineSpacerResId = activity.getResources().getIdentifier("bnb_timeline_spacer", "id", activity.getPackageName());
                activity.findViewById(timelineSpacerResId).setVisibility(View.GONE);

        }
    }

    private void adjustParentLayout(Activity activity) {
        int containerId = activity.getResources().getIdentifier("main_tab_container", "id", activity.getPackageName());
        ViewGroup container = activity.findViewById(containerId);

        int visibleCount = 0;
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            if (child.getVisibility() == View.VISIBLE) {
                visibleCount++;
                setLayoutWeight(child, 1);
            }
        }

        if (container instanceof LinearLayout) {
            ((LinearLayout) container).setWeightSum(visibleCount);
        }
    }

    private void setLayoutWeight(View view, float weight) {
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (params instanceof LinearLayout.LayoutParams) {
            LinearLayout.LayoutParams llParams = (LinearLayout.LayoutParams) params;
            llParams.weight = weight;
            llParams.width = 0;
            view.setLayoutParams(llParams);
        }
    }

    private void extendClickableArea(Activity activity) {
        int containerId = activity.getResources().getIdentifier("main_tab_container", "id", activity.getPackageName());
        ViewGroup container = activity.findViewById(containerId);

        for (int i = 2; i < container.getChildCount(); i += 2) {
            ViewGroup tab = (ViewGroup) container.getChildAt(i);
            ViewGroup.LayoutParams tabParams = tab.getLayoutParams();
            tabParams.width = 0;
            tab.setLayoutParams(tabParams);
            View clickArea = tab.getChildAt(tab.getChildCount() - 1);
            ViewGroup.LayoutParams clickParams = clickArea.getLayoutParams();
            clickParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
            clickArea.setLayoutParams(clickParams);
        }
    }

    private boolean isDarkModeEnabled(Activity activity) {
        Configuration configuration = activity.getResources().getConfiguration();
        int currentNightMode = configuration.uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return currentNightMode == Configuration.UI_MODE_NIGHT_YES;
    }
}