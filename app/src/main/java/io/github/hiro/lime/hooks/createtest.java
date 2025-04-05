package io.github.hiro.lime.hooks;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ProgressBar;

import androidx.appcompat.widget.AppCompatTextView;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.hiro.lime.LimeOptions;

public class createtest implements IHook {

    @Override
    public void hook(LimeOptions limeOptions, XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        // LiffFragmentクラスを動的に取得
        Class<?> liffFragmentClass = loadPackageParam.classLoader.loadClass(
                "com.linecorp.liff.impl.LiffFragment");

        // コンテキストメニュー処理にフック
        XposedHelpers.findAndHookMethod(liffFragmentClass,
                "onCreateContextMenu",
                ContextMenu.class, View.class, ContextMenu.ContextMenuInfo.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        ContextMenu menu = (ContextMenu) param.args[0];
                        View view = (View) param.args[1];

                        XposedBridge.log("[LIFF] ContextMenu created for view: " +
                                view.getClass().getSimpleName());

                        try {
                            // リフレクションで内部フィールドにアクセス
                            Object dField = XposedHelpers.getObjectField(param.thisObject, "d");
                            Object jField = XposedHelpers.getObjectField(dField, "j");
                            Object fField = XposedHelpers.getObjectField(jField, "f");

                            // WebViewの取得
                            WebView webView = (WebView) XposedHelpers.callMethod(fField, "b");
                            WebView.HitTestResult hitTestResult = webView.getHitTestResult();

                            XposedBridge.log("[LIFF] HitTestResult type: " + hitTestResult.getType());
                            if (hitTestResult.getExtra() != null) {
                                XposedBridge.log("[LIFF] HitTestResult extra: " + hitTestResult.getExtra());
                            }
                        } catch (Exception e) {
                            XposedBridge.log("[LIFF] Error getting WebView info: " + Log.getStackTraceString(e));
                        }
                    }
                });

        // MenuItemClickListenerにもフック
        XposedHelpers.findAndHookMethod("android.view.MenuItem$OnMenuItemClickListener",
                loadPackageParam.classLoader,
                "onMenuItemClick",
                MenuItem.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        MenuItem item = (MenuItem) param.args[0];
                        XposedBridge.log(String.format(
                                "[LIFF] MenuItem clicked: title=%s, id=%d",
                                item.getTitle(), item.getItemId()));
                    }
                });
    }
}