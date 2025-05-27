package io.github.hiro.lime.hooks;

import android.app.Activity;
import android.app.AndroidAppHelper;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.hiro.lime.LimeOptions;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class AutoUpdate implements IHook {

    private final OkHttpClient client = new OkHttpClient();
    private final String repoOwner = "areteruhiro";
    private final String repoName = "LIMEs";
    interface UpdateCheckCallback {
        void onUpdateAvailable(String latestVersion, List<String> apkUrls);
        void onUpToDate();
        void onError(String message);
    }
    @Override
    public void hook(LimeOptions limeOptions, XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        XposedHelpers.findAndHookMethod(
                "com.linecorp.line.chatlist.view.fragment.ChatListPageFragment",
                loadPackageParam.classLoader,
                "onCreateView",
                LayoutInflater.class,
                ViewGroup.class,
                Bundle.class,
                new XC_MethodHook() {

                    boolean isUpdateChecked = false;

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {

                        if (!limeOptions.AutoUpDateCheck.checked) return;
                        try {
                            if (isUpdateChecked) return;

                            Class<?> fragmentClass = loadPackageParam.classLoader.loadClass(
                                    "com.linecorp.line.chatlist.view.fragment.ChatListPageFragment"
                            );
                            Object fragment = fragmentClass.cast(param.thisObject);


                            Method getContextMethod = fragmentClass.getMethod("getContext");
                            Context context = (Context) getContextMethod.invoke(fragment);
                            if (context == null) return;

                            Method getActivityMethod = fragmentClass.getMethod("getActivity");
                            Activity activity = (Activity) getActivityMethod.invoke(fragment);
                            if (activity == null) return;

                            View rootView = (View) param.getResult();
                            if (!(rootView instanceof ViewGroup)) return;
                            ViewGroup rootContainer = (ViewGroup) rootView;

                            fetchLatestReleaseVersion(activity, new UpdateCheckCallback() {
                                @Override
                                public void onUpdateAvailable(String latestVersion, List<String> apkUrls) {
                                    createDownloadButtons(activity, rootContainer, apkUrls);
                                    isUpdateChecked = true;
                                }
                                @Override
                                public void onUpToDate() {
                                    showToast(activity, "Already up-to-date");
                                    isUpdateChecked = true;
                                }

                                @Override
                                public void onError(String message) {
                                    showErrorToast(activity, message);
                                    isUpdateChecked = true;
                                }
                            });

                        } catch (Exception e) {
                            XposedBridge.log("[AutoUpdate] Hook error: " + e.getMessage());
                        }
                    }
                }
        );
    }

    private void fetchLatestReleaseVersion(Context context, UpdateCheckCallback callback) {
        String apiUrl = String.format(
                "https://api.github.com/repos/%s/%s/releases/latest",
                repoOwner,
                repoName
        );

        Request request = new Request.Builder()
                .url(apiUrl)
                .header("Accept", "application/vnd.github.v3+json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (!response.isSuccessful()) {
                        callback.onError("HTTP " + response.code());
                        return;
                    }

                    JSONObject json = new JSONObject(response.body().string());
                    String latestVersion = json.getString("tag_name");
                    List<String> apkUrls = parseApkUrls(json);

                    if (apkUrls.isEmpty()) {
                        callback.onError("No APK files found");
                        return;
                    }

                    String currentVersion = getCurrentVersion(context);
                    if (isNewerVersion(latestVersion, currentVersion)) {
                        callback.onUpdateAvailable(latestVersion, apkUrls);
                    } else {
                        callback.onUpToDate();
                    }

                } catch (Exception e) {
                    callback.onError("Parse error: " + e.getMessage());
                }
            }
        });
    }

    private List<String> parseApkUrls(JSONObject releaseJson) throws Exception {
        List<String> urls = new ArrayList<>();
        JSONArray assets = releaseJson.getJSONArray("assets");

        for (int i = 0; i < assets.length(); i++) {
            JSONObject asset = assets.getJSONObject(i);
            String name = asset.getString("name");
            if (name.endsWith(".apk")) {
                urls.add(asset.getString("browser_download_url"));
            }
        }
        return urls;
    }
    private void createDownloadButtons(Context context, ViewGroup rootView, List<String> apkUrls) {
        if (context instanceof Activity) {
            ((Activity) context).runOnUiThread(() -> {
                try {
                    LinearLayout mainContainer = new LinearLayout(context);
                    mainContainer.setOrientation(LinearLayout.VERTICAL);
                    mainContainer.setLayoutParams(new ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    ));
                    Button parentButton = new Button(context);
                    setupParentButtonStyle(parentButton, context);
                    parentButton.setBackgroundColor(Color.TRANSPARENT);
                    parentButton.setText("更新有");
                    mainContainer.addView(parentButton);
                    LinearLayout subContainer = new LinearLayout(context);
                    subContainer.setOrientation(LinearLayout.VERTICAL);
                    subContainer.setVisibility(View.GONE);
                    ScrollView scrollView = new ScrollView(context);
                    LinearLayout buttonLayout = new LinearLayout(context);
                    buttonLayout.setOrientation(LinearLayout.VERTICAL);
                    buttonLayout.setPadding(
                            dpToPx(16, context),
                            dpToPx(16, context),
                            dpToPx(16, context),
                            dpToPx(16, context)
                    );


                    for (String url : apkUrls) {
                        Button btn = new Button(context);
                        btn.setText(getFileNameFromUrl(url));
                        btn.setOnClickListener(v -> openDownloadLink(context, url));
                        setupButtonStyle(btn, context);
                        buttonLayout.addView(btn);
                    }

                    LinearLayout controlLayout = new LinearLayout(context);
                    controlLayout.setOrientation(LinearLayout.HORIZONTAL);
                    controlLayout.setGravity(Gravity.CENTER);

                    Button cancelButton = new Button(context);
                    setupControlButtonStyle(cancelButton, context);
                    cancelButton.setText("非表示");
                    cancelButton.setOnClickListener(v -> {
                        mainContainer.removeAllViews();
                        rootView.removeView(mainContainer);
                    });

                    Button hideButton = new Button(context);
                    setupControlButtonStyle(hideButton, context);
                    hideButton.setText("キャンセル");
                    hideButton.setOnClickListener(v -> subContainer.setVisibility(View.GONE));

                    controlLayout.addView(cancelButton);
                    controlLayout.addView(hideButton);
                    scrollView.addView(buttonLayout);
                    subContainer.addView(scrollView);
                    subContainer.addView(controlLayout);
                    mainContainer.addView(subContainer);
                    parentButton.setOnClickListener(v -> {
                        if (subContainer.getVisibility() == View.VISIBLE) {
                            subContainer.setVisibility(View.GONE);
                        } else {
                            subContainer.setVisibility(View.VISIBLE);
                        }
                    });

                    rootView.addView(mainContainer);

                } catch (Exception e) {
                    XposedBridge.log("[AutoUpdate] UI error: " + e.getMessage());
                }
            });
        }
    }

    private void setupParentButtonStyle(Button button, Context context) {
        button.setBackgroundColor(Color.parseColor("#4CAF50"));
        button.setTextColor(Color.WHITE);
        button.setAllCaps(false);
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        button.setPadding(
                dpToPx(24, context),
                dpToPx(12, context),
                dpToPx(24, context),
                dpToPx(12, context)
        );
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, dpToPx(8, context));
        button.setLayoutParams(params);
    }

    private void setupControlButtonStyle(Button button, Context context) {
        button.setBackgroundColor(Color.parseColor("#9E9E9E"));
        button.setTextColor(Color.WHITE);
        button.setAllCaps(false);
        button.setPadding(
                dpToPx(16, context),
                dpToPx(8, context),
                dpToPx(16, context),
                dpToPx(8, context)
        );
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(dpToPx(8, context), 0, dpToPx(8, context), 0);
        button.setLayoutParams(params);
    }

    private void setupButtonStyle(Button button, Context context) {
        button.setBackgroundColor(Color.parseColor("#2196F3"));
        button.setTextColor(Color.WHITE);
        button.setAllCaps(false);
        button.setPadding(
                dpToPx(16, context),
                dpToPx(12, context),
                dpToPx(16, context),
                dpToPx(12, context)
        );
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, dpToPx(8, context));
        button.setLayoutParams(params);
    }

    private String getFileNameFromUrl(String url) {
        return url.substring(url.lastIndexOf('/') + 1);
    }

    private void openDownloadLink(Context context, String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            if (intent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(intent);
            } else {
                showToast(context, "No browser app found");
            }
        } catch (Exception e) {
            XposedBridge.log("[AutoUpdate] Intent error: " + e.getMessage());
        }
    }

    private boolean isNewerVersion(String latest, String current) {
        String[] latestParts = latest.replaceAll("[^\\d.]", "").split("\\.");
        String[] currentParts = current.replaceAll("[^\\d.]", "").split("\\.");

        for (int i = 0; i < Math.max(latestParts.length, currentParts.length); i++) {
            int l = i < latestParts.length ? parseInt(latestParts[i]) : 0;
            int c = i < currentParts.length ? parseInt(currentParts[i]) : 0;

            if (l > c) return true;
            if (l < c) return false;
        }
        return false;
    }

    private int parseInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String getCurrentVersion(Context context) {
        try {
            PackageInfo pInfo = context.getPackageManager()
                    .getPackageInfo("io.github.hiro.lime", 0);
            return pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "0.0.0";
        }
    }

    private int dpToPx(int dp, Context context) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                context.getResources().getDisplayMetrics()
        );
    }

    private void showToast(Context context, String message) {
        new Handler(Looper.getMainLooper()).post(() -> {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show();
        });
    }

    private void showErrorToast(Context context, String message) {
        showToast(context, "Error: " + message);
    }
}