package io.github.hiro.lime.hooks;

import static java.lang.Integer.parseInt;

import android.app.AndroidAppHelper;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.hiro.lime.LimeOptions;
import io.github.hiro.lime.R;
import io.github.hiro.lime.Utils;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class InstallModule implements IHook {
    private final OkHttpClient client = new OkHttpClient();
    private final String repoOwner = "areteruhiro";
    private final String repoName = "LIMEs";
    @Override
    public void hook(LimeOptions limeOptions, XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        XposedBridge.hookAllMethods(
                loadPackageParam.classLoader.loadClass("jp.naver.line.android.activity.SplashActivity"),
                "onCreate",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        final Context context = (Context) param.thisObject;

                        try {
                            Context moduleContext = AndroidAppHelper.currentApplication().createPackageContext(
                                    "io.github.hiro.lime", Context.CONTEXT_IGNORE_SECURITY);
                        } catch (Exception e) {
                            fetchLatestReleaseVersion(context, new UpdateCheckCallback() {
                                @Override
                                public void onUpdateAvailable(String latestVersion, List<String> apkUrls) {
                                    processApkUrls(context, apkUrls);
                                }

                                @Override
                                public void onUpToDate(List<String> apkUrls) {
                                    processApkUrls(context, apkUrls);
                                }

                                @Override
                                public void onError(String message) {
                                    showToastOnMainThread(context, "更新チェック失敗: " + message);
                                }

                                private void processApkUrls(final Context context, final List<String> apkUrls) {
                                    new Handler(Looper.getMainLooper()).post(() -> {
                                        List<String> targetApks = new ArrayList<>();

                                        for (String url : apkUrls) {
                                            String fileName = getFileNameFromUrl(url);
                                            if (fileName != null &&
                                                    fileName.startsWith("LIMEs") &&
                                                    fileName.endsWith(".apk")) {
                                                targetApks.add(url);
                                            }
                                        }

                                        if (!targetApks.isEmpty()) {
                                            openDownloadLink(context, targetApks.get(0));
                                            showToastOnMainThread(context, "最新版をダウンロードします");
                                        } else {
                                            showToastOnMainThread(context, "有効なAPKが見つかりません");
                                        }
                                    });
                                }
                            });
                        }
                    }
                }
        );
    }

    private interface UpdateCheckCallback {
        void onUpdateAvailable(String latestVersion, List<String> apkUrls);
        void onUpToDate(List<String> apkUrls);
        void onError(String message);
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
                        callback.onUpToDate(apkUrls); // APKリストを渡す
                    }

                } catch (Exception e) {
                    callback.onError("解析エラー: " + e.getMessage());
                }
            }
        });
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
    private String getCurrentVersion(Context context) {
        try {
            PackageInfo pInfo = context.getPackageManager()
                    .getPackageInfo("io.github.hiro.lime", 0);
            return pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "0.0.0";
        }
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
                showToastOnMainThread(context, "No browser app found");
            }
        } catch (Exception e) {
            XposedBridge.log("[AutoUpdate] Intent error: " + e.getMessage());
        }
    }

    private void showToastOnMainThread(final Context context, final String message) {
        new Handler(Looper.getMainLooper()).post(() -> {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        });
    }

    // ...既存のヘルパーメソッド...
}
