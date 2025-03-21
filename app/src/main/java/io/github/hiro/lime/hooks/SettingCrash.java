package io.github.hiro.lime.hooks;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import net.sqlcipher.database.SQLiteDatabase;

import android.database.MatrixCursor;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.SpannedString;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import dalvik.system.DexFile;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.hiro.lime.LimeOptions;
public class SettingCrash implements IHook {
    @Override
    public void hook(LimeOptions limeOptions, XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {

        if (!limeOptions.SettingClick.checked) return;
        XposedHelpers.findAndHookMethod(
                Context.class,
                "getText",
                int.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (param.getResult() instanceof String) {
                            param.setResult(SpannedString.valueOf((String) param.getResult()));
                        }
                    }
                }
        );

    }

}