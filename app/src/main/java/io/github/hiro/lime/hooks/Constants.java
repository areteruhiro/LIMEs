package io.github.hiro.lime.hooks;

import android.content.Context;
import android.content.pm.PackageManager;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class Constants {
    public static  String PACKAGE_NAME = "jp.naver.line.android";
    public static  String MODULE_NAME = "io.github.hiro.lime";

    //TRADITIONAL_CHINESE
    static  HookTarget USER_AGENT_HOOK = new  HookTarget("qi1.c", "j");
    //HANDLED_AND_RETURN_TRUE
    static  HookTarget WEBVIEW_CLIENT_HOOK = new HookTarget("VP0.k", "onPageFinished");
    //NOTIFICATION_DISABLED
    static  HookTarget MUTE_MESSAGE_HOOK = new HookTarget("jh1.b", "I");
    //PROCESSING
    static  HookTarget MARK_AS_READ_HOOK = new HookTarget("nP.d$d", "run");

    //ChatListViewModel
    static  HookTarget Archive = new HookTarget("LB.W", "invokeSuspend");
    //StreamingFetchOperationHandler
    static  HookTarget NOTIFICATION_READ_HOOK = new HookTarget("Ki1.b", "invokeSuspend");
    static  HookTarget REQUEST_HOOK = new HookTarget("org.apache.thrift.n", "b");
    static  HookTarget RESPONSE_HOOK = new HookTarget("org.apache.thrift.n", "a");
    //BackEventCompat
    static HookTarget  RemoveVoiceRecord_Hook_a = new HookTarget("q.j", "run");


    static HookTarget  ChatRestore = new HookTarget("", "onActivityResult");


    static HookTarget  PhotoSave = new HookTarget("", "");
    static HookTarget  PhotoSave1 = new HookTarget("", "");
    static HookTarget  PhotoSave2 = new HookTarget("", "");
    static HookTarget  PhotoSave3 = new HookTarget("", "");


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

            ChatRestore = new HookTarget("androidx.fragment.app.r", "onActivityResult");

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

            ChatRestore = new HookTarget("androidx.fragment.app.o", "onActivityResult");
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

            ChatRestore = new HookTarget("androidx.fragment.app.o", "onActivityResult");

//            RemoveVoiceRecord_Hook_b = new HookTarget("uk1.e$a", "run");
//            RemoveVoiceRecord_Hook_c = new HookTarget("C30.f", "run");
        } else if (isVersionInRange(versionName, "15.1.0", "15.2.0")) {
            USER_AGENT_HOOK = new HookTarget("qi1.c", "j");
            WEBVIEW_CLIENT_HOOK = new HookTarget("VP0.k", "onPageFinished");
            MUTE_MESSAGE_HOOK = new HookTarget("jh1.b", "I");
            MARK_AS_READ_HOOK = new HookTarget("nP.d$d", "run");
            Archive = new HookTarget("LB.U", "invokeSuspend");
            NOTIFICATION_READ_HOOK = new HookTarget("Ki1.b", "invokeSuspend");
            REQUEST_HOOK = new HookTarget("org.apache.thrift.n", "b");
            RESPONSE_HOOK = new HookTarget("org.apache.thrift.n", "a");
            RemoveVoiceRecord_Hook_a = new HookTarget("q.j", "run");

            ChatRestore = new HookTarget("androidx.fragment.app.o", "onActivityResult");

//        null cannot be cast to non-null type androidx.activity.result.ActivityResultCallback<O of androidx.activity.result.ActivityResultRegistry.dispatchResult>
//            RemoveVoiceRecord_Hook_c = new HookTarget("C30.f", "run");

        } else if (isVersionInRange(versionName, "15.2.0", "15.2.1")) {

        USER_AGENT_HOOK = new HookTarget("Mi1.c", "j");
        WEBVIEW_CLIENT_HOOK = new HookTarget("sQ0.l", "onPageFinished");
        MUTE_MESSAGE_HOOK = new HookTarget("Fh1.b", "I");
        MARK_AS_READ_HOOK = new HookTarget("GP.e$d", "run");
        Archive = new HookTarget("aC.P", "invokeSuspend");
        NOTIFICATION_READ_HOOK = new HookTarget("gj1.b", "invokeSuspend");
        REQUEST_HOOK = new HookTarget("org.apache.thrift.l", "b");
        RESPONSE_HOOK = new HookTarget("org.apache.thrift.l", "a");
        RemoveVoiceRecord_Hook_a = new HookTarget("q.j", "run");

        ChatRestore = new HookTarget("androidx.fragment.app.p", "onActivityResult");

    } else if (isVersionInRange(versionName, "15.3.0", "15.4.0")) {

        USER_AGENT_HOOK = new HookTarget("ek1.c", "j");
        WEBVIEW_CLIENT_HOOK = new HookTarget("CR0.m", "onPageFinished");
        MUTE_MESSAGE_HOOK = new HookTarget("Xi1.b", "I");
        MARK_AS_READ_HOOK = new HookTarget("aQ.c$d", "run");
        Archive = new HookTarget("tC.S", "invokeSuspend");
        NOTIFICATION_READ_HOOK = new HookTarget("yk1.b", "invokeSuspend");
        REQUEST_HOOK = new HookTarget("org.apache.thrift.l", "b");
        RESPONSE_HOOK = new HookTarget("org.apache.thrift.l", "a");
        RemoveVoiceRecord_Hook_a = new HookTarget("q.j", "run");

        ChatRestore = new HookTarget("androidx.fragment.app.n", "onActivityResult");
        PhotoSave = new HookTarget("Dh1.p0", "");
        PhotoSave1 = new HookTarget("Ec1.U", "");
        PhotoSave2 = new HookTarget("XQ.g", "");
        PhotoSave3 = new HookTarget("lm.K$b", "");




        } else if (isVersionInRange(versionName, "15.4.0", "15.4.1")) {

            XposedBridge.log("15.4.0 Patched ");

            USER_AGENT_HOOK = new HookTarget("Rj1.c", "j");
            WEBVIEW_CLIENT_HOOK = new HookTarget("jS0.l", "onPageFinished");
            MUTE_MESSAGE_HOOK = new HookTarget("Ki1.b", "I");
            MARK_AS_READ_HOOK = new HookTarget("pQ.d$d", "run");
            Archive = new HookTarget("GC.Z", "invokeSuspend");
            NOTIFICATION_READ_HOOK = new HookTarget("lk1.b", "invokeSuspend");
            REQUEST_HOOK = new HookTarget("org.apache.thrift.l", "b");
            RESPONSE_HOOK = new HookTarget("org.apache.thrift.l", "a");
            RemoveVoiceRecord_Hook_a = new HookTarget("h.i", "run");

            ChatRestore = new HookTarget("androidx.fragment.app.p", "onActivityResult");
//jp.naver.gallery.viewer.SaveSingleMediaToDeviceViewModel
            PhotoSave = new HookTarget("qh1.i0", "");

//jp.naver.gallery.viewer.SaveSingleMediaToDeviceViewModel
            PhotoSave1 = new HookTarget("rc1.C", "");

 //DIRECTORY_PICTURES
            PhotoSave2 = new HookTarget("mR.g", "");

//com.linecorp.line.album.ui.viewmodel.AlbumViewModel$downloadPhotoDirectly$1
            PhotoSave3 = new HookTarget("gm.J$b", "");



        } else if (isVersionInRange(versionName, "15.4.1", "15.5.0")) {

            XposedBridge.log("15.4.1 Patched ");
            USER_AGENT_HOOK = new HookTarget("Rj1.c", "j");
            WEBVIEW_CLIENT_HOOK = new HookTarget("jS0.l", "onPageFinished");
            MUTE_MESSAGE_HOOK = new HookTarget("Ki1.b", "I");
            MARK_AS_READ_HOOK = new HookTarget("pQ.c$d", "run");
            Archive = new HookTarget("GC.a0", "invokeSuspend");
            NOTIFICATION_READ_HOOK = new HookTarget("lk1.b", "invokeSuspend");
            REQUEST_HOOK = new HookTarget("org.apache.thrift.l", "b");
            RESPONSE_HOOK = new HookTarget("org.apache.thrift.l", "a");
            RemoveVoiceRecord_Hook_a = new HookTarget("h.j", "run");

            ChatRestore = new HookTarget("androidx.fragment.app.n", "onActivityResult");
//jp.naver.gallery.viewer.SaveSingleMediaToDeviceViewModel
            //getAllChatIds
            PhotoSave = new HookTarget("qh1.k0", "");

//jp.naver.gallery.viewer.SaveSingleMediaToDeviceViewModel
            //VideoPlaybackSyncEvent(localMessageId
            PhotoSave1 = new HookTarget("rc1.D", "");

            //DIRECTORY_PICTURES
            PhotoSave2 = new HookTarget("mR.g", "");

//com.linecorp.line.album.ui.viewmodel.AlbumViewModel$downloadPhotoDirectly$1
            //createAlbum
            PhotoSave3 = new HookTarget("gm.K", "");


        } else if (isVersionInRange(versionName, "15.5.0", "15.6.0")) {

            XposedBridge.log("15.5.1-15.5.4 Patched ");

                     /*
            TRADITIONAL_CHINESE
            static  HookTarget USER_AGENT_HOOK = new HookTarget("ek1.c", "j");
            HANDLED_AND_RETURN_TRUE
            static     WEBVIEW_CLIENT_HOOK = new HookTarget("CR0.m", "onPageFinished");
            NOTIFICATION_DISABLED
            static  HookTarget MUTE_MESSAGE_HOOK = new HookTarget("Xi1.b", "I");
            PROCESSING
            static  HookTarget   MARK_AS_READ_HOOK = new HookTarget("aQ.c$d", "run");

        ChatListViewModel
            static  HookTarget Archive = new HookTarget("tC.S", "invokeSuspend");
          StreamingFetchOperationHandler
            static  HookTargetNOTIFICATION_READ_HOOK = new HookTarget("yk1.b", "invokeSuspend");

            //%s failed: out of sequence response: expected %d but got %d
            static  HookTarget   REQUEST_HOOK = new HookTarget("org.apache.thrift.l", "b");
            static  HookTarget RESPONSE_HOOK = new HookTarget("org.apache.thrift.l", "a");
          */

            //BackEventCompat
            USER_AGENT_HOOK = new HookTarget("ej1.c", "j");
            WEBVIEW_CLIENT_HOOK = new HookTarget("FS0.l", "onPageFinished");
            MUTE_MESSAGE_HOOK = new HookTarget("Xh1.b", "I");
            MARK_AS_READ_HOOK = new HookTarget("mQ.c$d", "run");
            Archive = new HookTarget("JC.Y", "invokeSuspend");

            NOTIFICATION_READ_HOOK = new HookTarget("yj1.b", "invokeSuspend");
            REQUEST_HOOK = new HookTarget("org.apache.thrift.l", "b");
            RESPONSE_HOOK = new HookTarget("org.apache.thrift.l", "a");

            RemoveVoiceRecord_Hook_a = new HookTarget("h.j", "run");

            ChatRestore = new HookTarget("androidx.fragment.app.n", "onActivityResult");
//jp.naver.gallery.viewer.SaveSingleMediaToDeviceViewModel
            //getAllChatIds
            PhotoSave = new HookTarget("Cg1.s0", "");

//jp.naver.gallery.viewer.SaveSingleMediaToDeviceViewModel
            //VideoPlaybackSyncEvent(localMessageId
            PhotoSave1 = new HookTarget("Ib1.L", "");

            //DIRECTORY_PICTURES
            PhotoSave2 = new HookTarget("jR.g", "");

//com.linecorp.line.album.ui.viewmodel.AlbumViewModel$downloadPhotoDirectly$1
            //createAlbum
            PhotoSave3 = new HookTarget("gm.y", "");



        }





    }

    private static boolean isVersionInRange(String versionName, String minVersion, String maxVersion) {
        try {

            int[] currentVersion = parseVersion(versionName);
            int[] minVersionArray = parseVersion(minVersion);
            int[] maxVersionArray = parseVersion(maxVersion);

            boolean isGreaterOrEqualMin = compareVersions(currentVersion, minVersionArray) >= 0;

            boolean isLessThanMax = compareVersions(currentVersion, maxVersionArray) < 0;

            return isGreaterOrEqualMin && isLessThanMax;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static int[] parseVersion(String version) {
        String[] parts = version.split("\\.");
        int[] versionArray = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            versionArray[i] = Integer.parseInt(parts[i]);
        }
        return versionArray;
    }

    private static int compareVersions(int[] version1, int[] version2) {
        for (int i = 0; i < Math.min(version1.length, version2.length); i++) {
            if (version1[i] < version2[i]) return -1;
            if (version1[i] > version2[i]) return 1;
        }
        return 0;
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