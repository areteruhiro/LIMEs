package io.github.hiro.lime.hooks;

import android.content.Context;
import android.content.pm.PackageManager;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class Constants {
    public static  String PACKAGE_NAME = "jp.naver.line.android";
    public static  String MODULE_NAME = "io.github.hiro.lime";

    //TRADITIONAL_CHINESE
    static  HookTarget USER_AGENT_HOOK = new HookTarget("Wc1.c", "h");
    //HANDLED_AND_RETURN_TRUE
    static  HookTarget WEBVIEW_CLIENT_HOOK = new HookTarget("OK0.l", "onPageFinished");
    //NOTIFICATION_DISABLED
    static  HookTarget MUTE_MESSAGE_HOOK = new HookTarget("Ob1.b", "H");
    //PROCESSING
    static  HookTarget MARK_AS_READ_HOOK = new HookTarget("WM.c$d", "run");

    //ChatListViewModel
    static  HookTarget Archive = new HookTarget("sB.Q", "invokeSuspend");
    //StreamingFetchOperationHandler
    static  HookTarget NOTIFICATION_READ_HOOK = new HookTarget("qd1.b", "invokeSuspend");
    static  HookTarget REQUEST_HOOK = new HookTarget("org.apache.thrift.l", "b");
    static  HookTarget RESPONSE_HOOK = new HookTarget("org.apache.thrift.l", "a");
    //BackEventCompat
    static HookTarget  RemoveVoiceRecord_Hook_a = new HookTarget("q.j", "run");

    static HookTarget   SettingCrash_Hook = new HookTarget("Zb0.o0", "v0");
    static HookTarget   SettingCrash_Hook_Sub = new HookTarget("Sb0.j", "");
    //有効から無効
//    static HookTarget RemoveVoiceRecord_Hook_b = new HookTarget("xg1.e$a", "run");
////無効から有効
//static HookTarget RemoveVoiceRecord_Hook_c = new HookTarget("TS.f", "run");
    public static void initializeHooks(LoadPackageParam loadPackageParam) {
        Context context = (Context) XposedHelpers.callMethod(XposedHelpers.callStaticMethod(
                XposedHelpers.findClass("android.app.ActivityThread", null),
                "currentActivityThread"
        ), "getSystemContext");

        PackageManager pm = context.getPackageManager();
        String versionName = ""; // 初期化
        try {
            versionName = pm.getPackageInfo(loadPackageParam.packageName, 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        // バージョンネームに応じてHookTargetを変更
        if (versionName.equals("14.19.1")) {
            USER_AGENT_HOOK = new HookTarget("Wc1.c", "h");
            WEBVIEW_CLIENT_HOOK = new HookTarget("OK0.l", "onPageFinished");
            MUTE_MESSAGE_HOOK = new HookTarget("Ob1.b", "H");
            MARK_AS_READ_HOOK = new HookTarget("WM.c$d", "run");
            Archive = new HookTarget("sB.Q", "invokeSuspend");
            NOTIFICATION_READ_HOOK = new HookTarget("qd1.b", "invokeSuspend");
            REQUEST_HOOK = new HookTarget("org.apache.thrift.l", "b");
            RESPONSE_HOOK = new HookTarget("org.apache.thrift.l", "a");
            RemoveVoiceRecord_Hook_a = new HookTarget("af0.e", "run");
            SettingCrash_Hook = new HookTarget("Zb0.o0", "v0");
            SettingCrash_Hook_Sub = new HookTarget("Sb0.j", "");
//            RemoveVoiceRecord_Hook_b = new HookTarget("xg1.e$a", "run");
  //          RemoveVoiceRecord_Hook_c = new HookTarget("TS.f", "run");
        } else if (versionName.equals("14.21.1")) {
            USER_AGENT_HOOK = new HookTarget("vf1.c", "j");
            WEBVIEW_CLIENT_HOOK = new HookTarget("pN0.l", "onPageFinished");
            MARK_AS_READ_HOOK = new HookTarget("xN.b$d", "run");
            MUTE_MESSAGE_HOOK = new HookTarget("ne1.b", "H");
            Archive = new HookTarget("tB.N", "invokeSuspend");
            NOTIFICATION_READ_HOOK = new HookTarget("Pf1.c", "invokeSuspend");
            REQUEST_HOOK = new HookTarget("org.apache.thrift.l", "b");
            RESPONSE_HOOK = new HookTarget("org.apache.thrift.l", "a");
            RemoveVoiceRecord_Hook_a = new HookTarget("q.j", "run");
            SettingCrash_Hook = new HookTarget("Tc0.n0", "C0");
            SettingCrash_Hook_Sub = new HookTarget("Mc0.j", "");
     //       RemoveVoiceRecord_Hook_b = new HookTarget("Fi1.j", "run");
       //     RemoveVoiceRecord_Hook_c = new HookTarget("Fi1.j", "run");
        } else if (versionName.equals("15.0.0")) {
            USER_AGENT_HOOK = new HookTarget("Sg1.c", "j");
            WEBVIEW_CLIENT_HOOK = new HookTarget("FO0.l", "onPageFinished");
            MUTE_MESSAGE_HOOK = new HookTarget("Lf1.b", "I");
            MARK_AS_READ_HOOK = new HookTarget("KO.d$d", "run");
            Archive = new HookTarget("tB.P", "invokeSuspend");
            NOTIFICATION_READ_HOOK = new HookTarget("mh1.b", "invokeSuspend");
            REQUEST_HOOK = new HookTarget("org.apache.thrift.l", "b");
            RESPONSE_HOOK = new HookTarget("org.apache.thrift.l", "a");
            RemoveVoiceRecord_Hook_a = new HookTarget("q.j", "run");
            SettingCrash_Hook = new HookTarget("pe0.p0", "A0");
            SettingCrash_Hook_Sub = new HookTarget("ie0.j", "");
//            RemoveVoiceRecord_Hook_b = new HookTarget("uk1.e$a", "run");
//            RemoveVoiceRecord_Hook_c = new HookTarget("C30.f", "run");
    } else if (versionName.equals("15.1.0")) {
            USER_AGENT_HOOK = new HookTarget("qi1.c", "j");
            WEBVIEW_CLIENT_HOOK = new HookTarget("VP0.k", "onPageFinished");
            MUTE_MESSAGE_HOOK = new HookTarget("jh1.b", "I");
            MARK_AS_READ_HOOK = new HookTarget("nP.d$d", "run");
            Archive = new HookTarget("LB.U", "invokeSuspend");
            NOTIFICATION_READ_HOOK = new HookTarget("Ki1.f", "invokeSuspend");
            REQUEST_HOOK = new HookTarget("org.apache.thrift.n", "b");
            RESPONSE_HOOK = new HookTarget("org.apache.thrift.n", "a");
            RemoveVoiceRecord_Hook_a = new HookTarget("q.j", "run");
             SettingCrash_Hook = new HookTarget("af0.o0", "B0");
            SettingCrash_Hook_Sub = new HookTarget("Te0.j", "");
//        null cannot be cast to non-null type androidx.activity.result.ActivityResultCallback<O of androidx.activity.result.ActivityResultRegistry.dispatchResult>
//            RemoveVoiceRecord_Hook_c = new HookTarget("C30.f", "run");
        } else if (versionName.equals("15.1.1")) {
            USER_AGENT_HOOK = new HookTarget("qi1.c", "j");
            WEBVIEW_CLIENT_HOOK = new HookTarget("VP0.k", "onPageFinished");
            MUTE_MESSAGE_HOOK = new HookTarget("jh1.b", "I");
            MARK_AS_READ_HOOK = new HookTarget("nP.d$d", "run");
            Archive = new HookTarget("LB.U", "invokeSuspend");
            NOTIFICATION_READ_HOOK = new HookTarget("Ki1.f", "invokeSuspend");
            REQUEST_HOOK = new HookTarget("org.apache.thrift.n", "b");
            RESPONSE_HOOK = new HookTarget("org.apache.thrift.n", "a");
            RemoveVoiceRecord_Hook_a = new HookTarget("q.j", "run");
            SettingCrash_Hook = new HookTarget("af0.o0", "B0");
            SettingCrash_Hook_Sub = new HookTarget("Te0.j", "");
//            RemoveVoiceRecord_Hook_b = new HookTarget("uk1.e$a", "run");
//            RemoveVoiceRecord_Hook_c = new HookTarget("C30.f", "run");

    } else if (versionName.equals("15.1.2")) {
        USER_AGENT_HOOK = new HookTarget("qi1.c", "j");
        WEBVIEW_CLIENT_HOOK = new HookTarget("VP0.k", "onPageFinished");
        MUTE_MESSAGE_HOOK = new HookTarget("jh1.b", "I");
        MARK_AS_READ_HOOK = new HookTarget("nP.d$d", "run");
        Archive = new HookTarget("LB.U", "invokeSuspend");
        NOTIFICATION_READ_HOOK = new HookTarget("Ki1.f", "invokeSuspend");
        REQUEST_HOOK = new HookTarget("org.apache.thrift.n", "b");
        RESPONSE_HOOK = new HookTarget("org.apache.thrift.n", "a");
        RemoveVoiceRecord_Hook_a = new HookTarget("q.j", "run");
        SettingCrash_Hook = new HookTarget("af0.o0", "B0");
        SettingCrash_Hook_Sub = new HookTarget("Te0.j", "");
//            RemoveVoiceRecord_Hook_b = new HookTarget("uk1.e$a", "run");
//            RemoveVoiceRecord_Hook_c = new HookTarget("C30.f", "run");
    }

    }

    public static class HookTarget {
        public String className;
        public String methodName;

        public HookTarget(String className, String methodName) {
            this.className = className;
            this.methodName = methodName;
        }
    }
}