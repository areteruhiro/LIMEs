package io.github.hiro.lime.hooks;

import android.content.Context;
import android.content.pm.PackageManager;
import android.widget.Toast;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.hiro.lime.LimeOptions;
import io.github.hiro.lime.R;
import io.github.hiro.lime.Utils;

public class CheckHookTargetVersion implements IHook {
    @Override
    public void hook(LimeOptions limeOptions, XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        if (limeOptions.stopVersionCheck.checked) return;
        XposedBridge.hookAllMethods(
                loadPackageParam.classLoader.loadClass("jp.naver.line.android.activity.SplashActivity"),
                "onCreate",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        Context context = (Context) param.thisObject;
                        PackageManager pm = context.getPackageManager();

                        String versionName = pm.getPackageInfo(loadPackageParam.packageName, 0).versionName;
                        String versionNameStr = String.valueOf(versionName);
                        if (
!versionNameStr.equals("14.19.1")&&
!versionNameStr.equals("14.21.1")&&
!versionNameStr.equals("15.0.0")&&

!(isVersionInRange(versionName, "15.1.0", "15.2.0"))&&
 !(isVersionInRange(versionName, "15.3.0", "15.4.0")&&
!(isVersionInRange(versionName, "15.4.0", "15.5.0")

 )))


                        {
                            Utils.addModuleAssetPath(context);
                            Toast.makeText(context.getApplicationContext(), context.getString(R.string.incompatible_version), Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }
    // バージョン比較用のメソッド
    private static boolean isVersionInRange(String versionName, String minVersion, String maxVersion) {
        try {
            // バージョン文字列を数値に変換
            int[] currentVersion = parseVersion(versionName);
            int[] minVersionArray = parseVersion(minVersion);
            int[] maxVersionArray = parseVersion(maxVersion);

            // バージョンが minVersion 以上かどうかをチェック
            boolean isGreaterOrEqualMin = compareVersions(currentVersion, minVersionArray) >= 0;

            // バージョンが maxVersion 未満かどうかをチェック
            boolean isLessThanMax = compareVersions(currentVersion, maxVersionArray) < 0;

            // 両方の条件を満たすかどうかを返す
            return isGreaterOrEqualMin && isLessThanMax;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // バージョン文字列を数値配列に変換
    private static int[] parseVersion(String version) {
        String[] parts = version.split("\\.");
        int[] versionArray = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            versionArray[i] = Integer.parseInt(parts[i]);
        }
        return versionArray;
    }

    // バージョン比較用のメソッド
    private static int compareVersions(int[] version1, int[] version2) {
        for (int i = 0; i < Math.min(version1.length, version2.length); i++) {
            if (version1[i] < version2[i]) return -1;
            if (version1[i] > version2[i]) return 1;
        }
        return 0;
    }
}
