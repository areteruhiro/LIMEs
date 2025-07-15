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

    static  HookTarget REQUEST_HOOK = new HookTarget("org.apache.thrift.n", "b");
    static  HookTarget RESPONSE_HOOK = new HookTarget("org.apache.thrift.n", "a");
    //BackEventCompat
    static HookTarget  RemoveVoiceRecord_Hook_a = new HookTarget("q.j", "run");


    public static HookTarget ChatRestore = new HookTarget("", "onActivityResult");


    static HookTarget  PhotoSave = new HookTarget("", "");
    static HookTarget  PhotoSave1 = new HookTarget("", "");
    static HookTarget  PhotoSave2 = new HookTarget("", "");
    static HookTarget  PhotoSave3 = new HookTarget("", "");
    static HookTarget  Video = new HookTarget("", "");


    static HookTarget  ReactionList = new HookTarget("", "");

    static HookTarget  WhiteToDark0 = new HookTarget("", "");



    public static void initializeHooks(LoadPackageParam loadPackageParam) {
        Context context = (Context) XposedHelpers.callMethod(XposedHelpers.callStaticMethod(
                XposedHelpers.findClass("android.app.ActivityThread", null),
                "currentActivityThread"
        ), "getSystemContext");

        PackageManager pm = context.getPackageManager();
        String versionName = ""; // 初期化
        String versionNameStr = String.valueOf(versionName);
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

            REQUEST_HOOK = new HookTarget("org.apache.thrift.l", "b");
            RESPONSE_HOOK = new HookTarget("org.apache.thrift.l", "a");
            RemoveVoiceRecord_Hook_a = new HookTarget("af0.e", "run");
            ChatRestore = new HookTarget("androidx.fragment.app.r", "onActivityResult");
        } else if (versionName.equals("14.21.1")) {
            USER_AGENT_HOOK = new HookTarget("vf1.c", "j");
            WEBVIEW_CLIENT_HOOK = new HookTarget("pN0.l", "onPageFinished");
            MARK_AS_READ_HOOK = new HookTarget("xN.b$d", "run");
            MUTE_MESSAGE_HOOK = new HookTarget("ne1.b", "H");
            Archive = new HookTarget("tB.N", "invokeSuspend");
            REQUEST_HOOK = new HookTarget("org.apache.thrift.l", "b");
            RESPONSE_HOOK = new HookTarget("org.apache.thrift.l", "a");
            RemoveVoiceRecord_Hook_a = new HookTarget("q.j", "run");
            ChatRestore = new HookTarget("androidx.fragment.app.o", "onActivityResult");
        } else if (versionName.equals("15.0.0")) {
            USER_AGENT_HOOK = new HookTarget("Sg1.c", "j");
            WEBVIEW_CLIENT_HOOK = new HookTarget("FO0.l", "onPageFinished");
            MUTE_MESSAGE_HOOK = new HookTarget("Lf1.b", "I");
            MARK_AS_READ_HOOK = new HookTarget("KO.d$d", "run");
            Archive = new HookTarget("tB.P", "invokeSuspend");
            REQUEST_HOOK = new HookTarget("org.apache.thrift.l", "b");
            RESPONSE_HOOK = new HookTarget("org.apache.thrift.l", "a");
            RemoveVoiceRecord_Hook_a = new HookTarget("q.j", "run");
            ChatRestore = new HookTarget("androidx.fragment.app.o", "onActivityResult");
        } else if (isVersionInRange(versionName, "15.1.0", "15.2.0")) {
            USER_AGENT_HOOK = new HookTarget("qi1.c", "j");
            WEBVIEW_CLIENT_HOOK = new HookTarget("VP0.k", "onPageFinished");
            MUTE_MESSAGE_HOOK = new HookTarget("jh1.b", "I");
            MARK_AS_READ_HOOK = new HookTarget("nP.d$d", "run");
            Archive = new HookTarget("LB.U", "invokeSuspend");
            REQUEST_HOOK = new HookTarget("org.apache.thrift.n", "b");
            RESPONSE_HOOK = new HookTarget("org.apache.thrift.n", "a");
            RemoveVoiceRecord_Hook_a = new HookTarget("q.j", "run");
            ChatRestore = new HookTarget("androidx.fragment.app.o", "onActivityResult");
//        null cannot be cast to non-null type androidx.activity.result.ActivityResultCallback<O of androidx.activity.result.ActivityResultRegistry.dispatchResult>

        } else if (isVersionInRange(versionName, "15.2.0", "15.2.1")) {

            USER_AGENT_HOOK = new HookTarget("Mi1.c", "j");
            WEBVIEW_CLIENT_HOOK = new HookTarget("sQ0.l", "onPageFinished");
            MUTE_MESSAGE_HOOK = new HookTarget("Fh1.b", "I");
            MARK_AS_READ_HOOK = new HookTarget("GP.e$d", "run");
            Archive = new HookTarget("aC.P", "invokeSuspend");
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


            ReactionList = new HookTarget("Iy.l", "");

            //TIMELINE_FEED_TAB_LAST_PAUSED_TIME_MILLIS
            WhiteToDark0 = new HookTarget("Xv0.m$b", "");


        } else if (isVersionInRange(versionName, "15.5.0", "15.6.0")) {

            XposedBridge.log("15.5.1-15.5.4 Patched ");

            USER_AGENT_HOOK = new HookTarget("ej1.c", "j");
            WEBVIEW_CLIENT_HOOK = new HookTarget("FS0.l", "onPageFinished");
            MUTE_MESSAGE_HOOK = new HookTarget("Xh1.b", "I");
            MARK_AS_READ_HOOK = new HookTarget("mQ.c$d", "run");
            Archive = new HookTarget("JC.Y", "invokeSuspend");

            REQUEST_HOOK = new HookTarget("org.apache.thrift.l", "b");
            RESPONSE_HOOK = new HookTarget("org.apache.thrift.l", "a");

            RemoveVoiceRecord_Hook_a = new HookTarget("h.j", "run");

            ChatRestore = new HookTarget("androidx.fragment.app.n", "onActivityResult");
            PhotoSave = new HookTarget("Cg1.s0", "");
            PhotoSave1 = new HookTarget("Ib1.L", "");
            PhotoSave2 = new HookTarget("jR.g", "");
            PhotoSave3 = new HookTarget("gm.y", "");
            ReactionList = new HookTarget("Ky.m", "");

            Video = new HookTarget("YP.I", "");

        } else if (isVersionInRange(versionName, "15.6.0", "15.7.0")) {

            XposedBridge.log("15.6.0 Patched ");
            USER_AGENT_HOOK = new HookTarget("xj1.c", "j");
            WEBVIEW_CLIENT_HOOK = new HookTarget("bT0.l", "onPageFinished");
            MUTE_MESSAGE_HOOK = new HookTarget("qi1.b", "I");
            MARK_AS_READ_HOOK = new HookTarget("vQ.c$d", "run");
            Archive = new HookTarget("JC.b0", "invokeSuspend");

            REQUEST_HOOK = new HookTarget("org.apache.thrift.m", "b");
            RESPONSE_HOOK = new HookTarget("org.apache.thrift.m", "a");

            RemoveVoiceRecord_Hook_a = new HookTarget("h.j", "run");

            ChatRestore = new HookTarget("androidx.fragment.app.m", "onActivityResult");
//jp.naver.gallery.viewer.SaveSingleMediaToDeviceViewModel
            //getAllChatIds
            PhotoSave = new HookTarget("Vg1.l0", "");

//jp.naver.gallery.viewer.SaveSingleMediaToDeviceViewModel
            //VideoPlaybackSyncEvent(localMessageId
            PhotoSave1 = new HookTarget("hc1.C", "");

            //DIRECTORY_PICTURES
            PhotoSave2 = new HookTarget("sR.g", "");

//com.linecorp.line.album.ui.viewmodel.AlbumViewModel$downloadPhotoDirectly$1
            //createAlbum
            PhotoSave3 = new HookTarget("gm.A", "");

            Video = new HookTarget("hQ.J", "");
            //reactionListModel
            ReactionList = new HookTarget("Iy.m", "");

        } else if (isVersionInRange(versionName, "15.7.0", "15.8.0")) {

            XposedBridge.log("15.7.0 Patched ");

            USER_AGENT_HOOK = new HookTarget("Si1.c", "j");
            WEBVIEW_CLIENT_HOOK = new HookTarget("CS0.m", "onPageFinished");
            MUTE_MESSAGE_HOOK = new HookTarget("Lh1.b", "I");
            MARK_AS_READ_HOOK = new HookTarget("dR.d$d", "run");
            Archive = new HookTarget("jD.S", "invokeSuspend");

            REQUEST_HOOK = new HookTarget("org.apache.thrift.l", "b");
            RESPONSE_HOOK = new HookTarget("org.apache.thrift.l", "a");

            RemoveVoiceRecord_Hook_a = new HookTarget("h.j", "run");

            ChatRestore = new HookTarget("androidx.fragment.app.n", "onActivityResult");
//jp.naver.gallery.viewer.SaveSingleMediaToDeviceViewModel
            //getAllChatIds
            PhotoSave = new HookTarget("rg1.n0", "");

//jp.naver.gallery.viewer.SaveSingleMediaToDeviceViewModel
            //VideoPlaybackSyncEvent(localMessageId
            PhotoSave1 = new HookTarget("Db1.U", "");

            //DIRECTORY_PICTURES
            PhotoSave2 = new HookTarget("aS.g", "");

//com.linecorp.line.album.ui.viewmodel.AlbumViewModel$downloadPhotoDirectly$1
            //createAlbum
            PhotoSave3 = new HookTarget("zm.C", "");

            //reactionListModel
            ReactionList = new HookTarget("iz.j", "");
//skipMemoryCache
            Video = new HookTarget("PQ.J", "");

        } else if (isVersionInRange(versionName, "15.9.0", "15.10.0")) {

            XposedBridge.log("15.9.0 Patched ");

            USER_AGENT_HOOK = new HookTarget("Bk1.d", "j");
            WEBVIEW_CLIENT_HOOK = new HookTarget("ZT0.l", "onPageFinished");
            MUTE_MESSAGE_HOOK = new HookTarget("uj1.b", "I");
            MARK_AS_READ_HOOK = new HookTarget("dS.e$d", "run");
            String ChatListClassName;
            if (versionName.equals("15.9.2")) {
                ChatListClassName = "LD.S";
            } else if (versionName.equals("15.9.3")) {
                ChatListClassName = "LD.Q";
                XposedBridge.log("15.9.3 Patched ");
            } else {

                return;
            }
            Archive = new HookTarget(ChatListClassName, "invokeSuspend");
            REQUEST_HOOK = new HookTarget("org.apache.thrift.l", "b");
            RESPONSE_HOOK = new HookTarget("org.apache.thrift.l", "a");
//%s failed: out of sequence response: expected %d but got %d

            RemoveVoiceRecord_Hook_a = new HookTarget("g.l", "run");

//jp.naver.gallery.viewer.SaveSingleMediaToDeviceViewModel
            //getAllChatIds
            PhotoSave = new HookTarget("ai1.p0", "");

//jp.naver.gallery.viewer.SaveSingleMediaToDeviceViewModel
            //VideoPlaybackSyncEvent(localMessageId
            PhotoSave1 = new HookTarget("ld1.F", "");

            //DIRECTORY_PICTURES
            PhotoSave2 = new HookTarget("aT.g", "");

//com.linecorp.line.album.ui.viewmodel.AlbumViewModel$downloadPhotoDirectly$1
            //createAlbum
            PhotoSave3 = new HookTarget("Nm.L", "");

            //reactionListModel
            String ReactionListClassName;
            if (versionName.equals("15.9.3")) {
                ReactionListClassName = "Jz.m";
                XposedBridge.log("15.9.3 Reaction ");
            } else {

                ReactionListClassName = "Jz.l";
                XposedBridge.log("15.9.0 Patched ");
            }
            ReactionList = new HookTarget(ReactionListClassName, "");


//skipMemoryCache
            Video = new HookTarget("PR.I", "");
            String chatRestoreClassName;
            if (versionName.equals("15.9.2")) {
                chatRestoreClassName = "androidx.fragment.app.t";
            } else if (versionName.equals("15.9.3")) {
                chatRestoreClassName = "androidx.fragment.app.r";
                XposedBridge.log("15.9.3 Patched ");
            } else {

                chatRestoreClassName = "androidx.fragment.app.r";
                XposedBridge.log("15.9.0 Patched ");
            }
            ChatRestore = new HookTarget(chatRestoreClassName, "onActivityResult");
        } else if (isVersionInRange(versionName, "15.11.0", "15.12.0")) {

            XposedBridge.log("15.11.0 Patched ");

            USER_AGENT_HOOK = new HookTarget("pp1.c", "j");
            WEBVIEW_CLIENT_HOOK = new HookTarget("VY0.m", "onPageFinished");


            MUTE_MESSAGE_HOOK = new HookTarget("io1.b", "I");
            MARK_AS_READ_HOOK = new HookTarget("xS.d$d", "run");
            Archive = new HookTarget("YD.Q", "invokeSuspend");

            REQUEST_HOOK = new HookTarget("org.apache.thrift.m", "b");
            RESPONSE_HOOK = new HookTarget("org.apache.thrift.m", "a");

            RemoveVoiceRecord_Hook_a = new HookTarget("g.l", "run");

            ChatRestore = new HookTarget("androidx.fragment.app.n", "onActivityResult");
//jp.naver.gallery.viewer.SaveSingleMediaToDeviceViewModel
            //getAllChatIds
            PhotoSave = new HookTarget("Om1.s0", "");

//jp.naver.gallery.viewer.SaveSingleMediaToDeviceViewModel
            //VideoPlaybackSyncEvent(localMessageId
            PhotoSave1 = new HookTarget("bi1.G", "");

            //DIRECTORY_PICTURES
            PhotoSave2 = new HookTarget("uT.g", "");

//com.linecorp.line.album.ui.viewmodel.AlbumViewModel$downloadPhotoDirectly$1
            //createAlbum
            PhotoSave3 = new HookTarget("Vm.I", "");

            //reactionListModel
            ReactionList = new HookTarget("Vz.k", "");
//skipMemoryCache
            Video = new HookTarget("jS.D", "");
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