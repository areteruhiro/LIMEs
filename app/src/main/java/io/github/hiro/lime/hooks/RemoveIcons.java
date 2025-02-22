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

                        if (limeOptions.distributeEvenly.checked) {
                            adjustParentLayout(activity);
                        }

                        if (limeOptions.extendClickableArea.checked) {
                            extendClickableArea(activity);
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
                } else {
                    hideViewWithSpacer(activity, "bnb_timeline", "bnb_timeline_spacer");
                }
            }
        }
    }

    private void handleRemoveWallet(Activity activity, boolean isDarkMode) {
        if (limeOptions.removeWallet.checked) {
            if (!limeOptions.distributeEvenly.checked && limeOptions.DarkColor.checked) {
                if (limeOptions.DarkModSync.checked && !isDarkMode) {
                    hideViewWithSpacer(activity, "bnb_wallet", "bnb_wallet_spacer");
                    hideViewWithSpacer(activity, "bnb_call", "bnb_call_spacer");

                    return; // 処理を終了
                }
                setViewProperties(activity, "bnb_wallet", Color.BLACK);
                setViewProperties(activity, "bnb_wallet_spacer", Color.BLACK);
            }
        }
    }

    private void handleRemoveNewsOrCall(Activity activity, boolean isDarkMode) {
        if (limeOptions.removeNewsOrCall.checked) {
            if (!limeOptions.distributeEvenly.checked && limeOptions.DarkColor.checked) {
                if (limeOptions.DarkModSync.checked && !isDarkMode) {
                    hideViewWithSpacer(activity, "bnb_news", "bnb_news_spacer");
                    return;
                }
                setViewProperties(activity, "bnb_news", Color.BLACK);
                setViewProperties(activity, "bnb_news_spacer", Color.BLACK);
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
        } else {
            hideViewWithSpacer(activity, viewId, viewId + "_spacer");
        }
    }

    private void hideViewWithSpacer(Activity activity, String mainId, String spacerId) {
        int resId = activity.getResources().getIdentifier(mainId, "id", activity.getPackageName());
        activity.findViewById(resId).setVisibility(View.GONE);
        if (limeOptions.distributeEvenly.checked) {
            int spacerResId = activity.getResources().getIdentifier(spacerId, "id", activity.getPackageName());
            activity.findViewById(spacerResId).setVisibility(View.GONE);
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