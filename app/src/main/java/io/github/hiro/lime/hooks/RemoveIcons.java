package io.github.hiro.lime.hooks;

import static io.github.hiro.lime.Main.limeOptions;

import android.app.Activity;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.Toolbar;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
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

                        if (limeOptions.removeVoom.checked) {
                            if (!limeOptions.DarkColor.checked) {
                                hideViewWithSpacer(activity, "bnb_timeline", "bnb_timeline_spacer");
                            }
                            if (!limeOptions.distributeEvenly.checked) {
                                if (limeOptions.DarkColor.checked) {
                                    int timelineId = activity.getResources().getIdentifier(
                                            "bnb_timeline",
                                            "id",
                                            activity.getPackageName()
                                    );

                                    View timelineView = activity.findViewById(timelineId);
                                    if (timelineView != null) {
                                        timelineView.setEnabled(false);
                                        timelineView.setClickable(false);
                                        timelineView.setFocusable(false);
                                        timelineView.setBackgroundColor(Color.BLACK);
                                        if (timelineView instanceof ViewGroup) {
                                            ViewGroup group = (ViewGroup) timelineView;
                                            for (int i = 0; i < group.getChildCount(); i++) {
                                                View child = group.getChildAt(i);
                                                child.setBackgroundColor(Color.BLACK);
                                                child.setEnabled(false);
                                            }
                                        }
                                        int spacerId = activity.getResources().getIdentifier(
                                                "bnb_timeline_spacer",
                                                "id",
                                                activity.getPackageName()
                                        );
                                        View spacerView = activity.findViewById(spacerId);
                                        if (spacerView != null) {
                                            spacerView.setVisibility(View.VISIBLE);
                                        }
                                    }
                                }
                            }
                        }
                        if (limeOptions.removeWallet.checked) {
                            if (!limeOptions.DarkColor.checked) {
                                hideViewWithSpacer(activity, "bnb_wallet", "bnb_wallet_spacer");
                            }
                            if (!limeOptions.distributeEvenly.checked) {
                                if (limeOptions.DarkColor.checked) {
                                    int bnb_walletid = activity.getResources().getIdentifier(
                                            "bnb_wallet",
                                            "id",
                                            activity.getPackageName()
                                    );

                                    View bnb_wallet = activity.findViewById(bnb_walletid);
                                    if (bnb_wallet != null) {
                                        bnb_wallet.setEnabled(false);
                                        bnb_wallet.setClickable(false);
                                        bnb_wallet.setFocusable(false);
                                        bnb_wallet.setBackgroundColor(Color.BLACK);
                                        if (bnb_wallet instanceof ViewGroup) {
                                            ViewGroup group = (ViewGroup) bnb_wallet;
                                            for (int i = 0; i < group.getChildCount(); i++) {
                                                View child = group.getChildAt(i);
                                                child.setBackgroundColor(Color.BLACK);
                                                child.setEnabled(false);
                                            }
                                        }
                                        int spacerId = activity.getResources().getIdentifier(
                                                "bnb_wallet_spacer",
                                                "id",
                                                activity.getPackageName()
                                        );
                                        View spacerView = activity.findViewById(spacerId);
                                        if (spacerView != null) {
                                            spacerView.setVisibility(View.VISIBLE);
                                        }
                                    }
                                }
                            }
                        }

                        if (limeOptions.removeNewsOrCall.checked) {

                            if (!limeOptions.DarkColor.checked) {
                                hideViewWithSpacer(activity, "bnb_news", "bnb_news_spacer");
                            }

                            if (limeOptions.DarkColor.checked) {
                                if (!limeOptions.PureDarkCall.checked) {
                                    if (!limeOptions.distributeEvenly.checked) {
                                        // bnb_news の処理
                                        int bnb_newsId = activity.getResources().getIdentifier(
                                                "bnb_news",
                                                "id",
                                                activity.getPackageName()
                                        );

                                        View bnb_news = activity.findViewById(bnb_newsId);
                                        if (bnb_news != null) {
                                            bnb_news.setEnabled(false);
                                            bnb_news.setClickable(false);
                                            bnb_news.setFocusable(false);
                                            bnb_news.setBackgroundColor(Color.BLACK);
                                            if (bnb_news instanceof ViewGroup) {
                                                ViewGroup group = (ViewGroup) bnb_news;
                                                for (int i = 0; i < group.getChildCount(); i++) {
                                                    View child = group.getChildAt(i);
                                                    child.setBackgroundColor(Color.BLACK);
                                                    child.setEnabled(false);
                                                }
                                            }
                                            int newsSpacerId = activity.getResources().getIdentifier(
                                                    "bnb_news_spacer",
                                                    "id",
                                                    activity.getPackageName()
                                            );
                                            View newsSpacerView = activity.findViewById(newsSpacerId);
                                            if (newsSpacerView != null) {
                                                newsSpacerView.setVisibility(View.VISIBLE);
                                            }
                                        }
                                    }
                                }
                                if (limeOptions.removeNewsOrCall.checked) {
                                    if (!limeOptions.DarkColor.checked) {
                                        hideViewWithSpacer(activity, "bnb_call", "bnb_call_spacer");
                                    }

                                    if (limeOptions.DarkColor.checked) {
                                        if (limeOptions.PureDarkCall.checked) {
                                            if (!limeOptions.distributeEvenly.checked) {
                                                int bnb_callId = activity.getResources().getIdentifier(
                                                        "bnb_call",
                                                        "id",
                                                        activity.getPackageName()
                                                );

                                                View bnb_call = activity.findViewById(bnb_callId);
                                                if (bnb_call != null) {
                                                    bnb_call.setEnabled(false);
                                                    bnb_call.setClickable(false);
                                                    bnb_call.setFocusable(false);
                                                    bnb_call.setBackgroundColor(Color.BLACK);
                                                    if (bnb_call instanceof ViewGroup) {
                                                        ViewGroup group = (ViewGroup) bnb_call;
                                                        for (int i = 0; i < group.getChildCount(); i++) {
                                                            View child = group.getChildAt(i);
                                                            child.setBackgroundColor(Color.BLACK);
                                                            child.setEnabled(false);
                                                        }
                                                    }
                                                    int callSpacerId = activity.getResources().getIdentifier(
                                                            "bnb_call_spacer",
                                                            "id",
                                                            activity.getPackageName()
                                                    );
                                                    View callSpacerView = activity.findViewById(callSpacerId);
                                                    if (callSpacerView != null) {
                                                        callSpacerView.setVisibility(View.VISIBLE);
                                                    }
                                                }
                                            }
                                        }

                                    }
                                }

                            }
                        }
                        if (limeOptions.DarkColor.checked) {
                            applyDarkTheme(activity);
                        }
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
    private void applyDarkTheme(Activity activity) {
        try {
            int mainContainerId = activity.getResources().getIdentifier(
                    "main_container",
                    "id",
                    activity.getPackageName()
            );
            View mainContainer = activity.findViewById(mainContainerId);
            mainContainer.setBackgroundColor(0xFF000000);
            int toolbarId = activity.getResources().getIdentifier(
                    "main_toolbar",
                    "id",
                    activity.getPackageName()
            );
            Toolbar toolbar = activity.findViewById(toolbarId);
            if (toolbar != null) {
                toolbar.setTitleTextColor(0xFFFFFFFF);
                toolbar.setSubtitleTextColor(0xFFFFFFFF);
            }
        } catch (Exception e) {
            XposedBridge.log("Dark theme apply failed: " + e);
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
}
