package io.github.hiro.lime.hooks;


import static io.github.hiro.lime.Main.limeOptions;

import android.app.AndroidAppHelper;
import android.app.Application;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import java.io.File;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.hiro.lime.LimeOptions;

public class AgeCheckSkip implements IHook {

    @Override
    public void hook(LimeOptions limeOptions, XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        if (!limeOptions.AgeCheckSkip.checked) return;
        XposedHelpers.findAndHookMethod(Application.class, "onCreate", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Application appContext = (Application) param.thisObject;

                if (appContext == null) {
                    return;
                }

                File dbFile3 = appContext.getDatabasePath("line_general_key_value");

                if (dbFile3.exists()) {
                    SQLiteDatabase.OpenParams.Builder builder1 = new SQLiteDatabase.OpenParams.Builder();
                    builder1.addOpenFlags(SQLiteDatabase.OPEN_READWRITE);
                    SQLiteDatabase.OpenParams dbParams1 = builder1.build();
                    SQLiteDatabase db3 = SQLiteDatabase.openDatabase(dbFile3, dbParams1);

                    // 既に "AGE_VERIFICATION_RESULT" が存在するか確認
                    String query = "SELECT * FROM key_value_text WHERE key = ?";
                    Cursor cursor = db3.rawQuery(query, new String[]{"AGE_VERIFICATION_RESULT"});

                    if (cursor.getCount() == 0) {
                        // 存在しない場合のみ挿入
                        ContentValues values = new ContentValues();
                        values.put("key", "AGE_VERIFICATION_RESULT");
                        values.put("value", "2");

                        long newRowId = db3.insert("key_value_text", null, values);

                        if (newRowId == -1) {
                            XposedBridge.log("データの挿入に失敗しました。");
                        } else {
                            XposedBridge.log("データが正常に挿入されました。新しい行ID: " + newRowId);
                        }
                    } else {
                       return;
                    }

                    cursor.close(); // カーソルを閉じる
                    db3.close(); // データベースを閉じる

                }
            }
        });

    }
}
