package io.github.hiro.lime;

public class LimeOptions {

    public class Option {
        public final String name;
        public int id;
        public boolean checked;

        public Option(String name, int id, boolean checked) {
            this.name = name;
            this.id = id;
            this.checked = checked;
        }
    }
    public Option removeOption = new Option("removeOption", R.string.switch_unembed_options, false);

    public Option removeVoom = new Option("remove_voom", R.string.switch_remove_voom, true);
    public Option removeWallet = new Option("remove_wallet", R.string.switch_remove_wallet, true);
    public Option removeNewsOrCall = new Option("remove_news_or_call", R.string.switch_remove_news_or_call, true);
    public Option distributeEvenly = new Option("distribute_evenly", R.string.switch_distribute_evenly, true);
    public Option extendClickableArea = new Option("extend_clickable_area", R.string.switch_extend_clickable_area, true);
    public Option removeIconLabels = new Option("remove_icon_labels", R.string.switch_remove_icon_labels, true);
    public Option removeAds = new Option("remove_ads", R.string.switch_remove_ads, true);
    public Option removeRecommendation = new Option("remove_recommendation", R.string.switch_remove_recommendation, true);
    public Option removePremiumRecommendation = new Option("remove_premium_recommendation", R.string.switch_remove_premium_recommendation, true);
    public Option removeServiceLabels = new Option("remove_service_labels", R.string.switch_remove_service_labels, false);


    public Option removeSearchBar = new Option("removeSearchBar", R.string.removeSearchBar, false);
    public Option removeNaviAlbum = new Option("removeNaviAlbum", R.string.removeNaviAlbum, false);
    public Option removeNaviOpenchat = new Option("removeNaviOpenchat", R.string.removeNaviOpenchat, false);


    public Option removeReplyMute = new Option("remove_reply_mute", R.string.switch_remove_reply_mute, true);
    public Option redirectWebView = new Option("redirect_webview", R.string.switch_redirect_webview, true);
    public Option openInBrowser = new Option("open_in_browser", R.string.switch_open_in_browser, false);
    public Option preventMarkAsRead = new Option("prevent_mark_as_read", R.string.switch_prevent_mark_as_read, false);
    public Option preventUnsendMessage = new Option("prevent_unsend_message", R.string.switch_prevent_unsend_message, false);
    public Option sendMuteMessage = new Option("mute_message", R.string.switch_send_mute_message, false);

    public Option removeKeepUnread = new Option("remove_keep_unread", R.string.switch_remove_keep_unread, false);
    public Option KeepUnreadLSpatch = new Option("Keep_UnreadLSpatch", R.string.switch_KeepUnreadLSpatch, false);
    public Option blockTracking = new Option("block_tracking", R.string.switch_block_tracking, false);
    public Option CansellNotification = new Option("CansellNotification", R.string.CansellNotification, false);
    public Option BlockUpdateProfileNotification = new Option("BlockUpdateProfileNotification", R.string.switch_BlockUpdateProfileNotification, false);

    public Option stopVersionCheck = new Option("stop_version_check", R.string.switch_stop_version_check, false);
    public Option outputCommunication = new Option("output_communication", R.string.switch_output_communication, false);
    public Option Archived = new Option("Archived_message", R.string.switch_archived, false);
    public Option removeAllServices = new Option("remove_Services", R.string.RemoveService, false);
    public Option callTone = new Option("callTone", R.string.callTone, false);
    public Option MuteTone = new Option("MuteTone", R.string.MuteTone, false);
    public Option DialTone = new Option("DialTone", R.string.DialTone, false);

    public Option ReadChecker = new Option("ReadChecker", R.string.ReadChecker, false);
    public Option ReadCheckerChatdataDelete = new Option("ReadCheckerChatdataDelete", R.string.ReadCheckerChatdataDelete, false);
    public Option MySendMessage = new Option("MySendMessage", R.string.MySendMessage, false);

    public Option AgeCheckSkip = new Option("AgeCheckSkip", R.string.AgeCheckSkip, false);
    public Option hide_canceled_message = new Option("hide_canceled_message", R.string.hide_canceled_message, false);
    public Option RemoveNotification = new Option("RemoveProfileNotification", R.string.removeNotification, false);
    public Option DarkColor = new Option("DarkColor", R.string.DarkColor, false);
    public Option NoMuteMessage = new Option("NoMuteMessage", R.string.NoMuteMessage, false);
    public Option MuteGroup = new Option("Disabled_Group_notification", R.string.MuteGroup, false);
    public Option PhotoAddNotification = new Option("PhotoAddNotification", R.string.PhotoAddNotification, false);
    public Option GroupNotification = new Option("GroupNotification", R.string.GroupNotification, false);
    public Option RemoveVoiceRecord = new Option("RemoveVoiceRecord", R.string.RemoveVoiceRecord, false);
    public Option AddCopyAction = new Option("AddCopyAction", R.string.AddCopyAction, false);
    public Option CallOpenApplication = new Option("CallOpenApplication", R.string.CallOpenApplication, true);
    public Option DarkModSync = new Option("DarkModSync", R.string.DarkkModSync, true);

    public Option PureDarkCall = new Option("PureDarkCall", R.string.PureDarkCall, false);

    public Option BlockCheck = new Option("BlockCheck", R.string.BlockCheck, true);
    public Option SettingClick = new Option("SettingClick", R.string.SettingClick, false);


    public Option photoboothButtonOption = new Option("photoboothButtonOption", R.string.photoboothButtonOption, true);
    public Option voiceButtonOption = new Option("voiceButtonOption", R.string.voiceButtonOption, false);
    public Option videoButtonOption = new Option("videoButtonOption", R.string.videoButtonOption, true);
    public Option videoSingleButtonOption = new Option("videoSingleButtonOption", R.string.videoSingleButtonOption, true);

    public Option AutoUpDateCheck = new Option("AutoUpDateCheck", R.string.AutoUpDateCheck, false);

    public Option ringtonevolume = new Option("ringtonevolume", R.string.ringtonevolume, false);

    public Option PinList = new Option("ringtonevolume", R.string.PinList, false);

    public Option SpoofAndroidId = new Option("SpoofAndroidId", R.string.SpoofAndroidId, false);
    public Option SpoofUserAgent = new Option("SpoofUserAgent", R.string.SpoofUserAgent, false);


    public Option UnsendFix = new Option("UnsendFix", R.string.UnsendFix, false);
    public Option[] options = {
            removeOption,
            removeVoom,
            removeWallet,
            removeNewsOrCall,
            distributeEvenly,
            extendClickableArea,
            removeIconLabels,
            removeAds,
            removeRecommendation,
            removePremiumRecommendation,
            removeAllServices,
            removeServiceLabels,
            RemoveNotification,
            removeNaviOpenchat,
            removeNaviAlbum,
            removeSearchBar,
            removeReplyMute,
            redirectWebView,
            openInBrowser,
            preventMarkAsRead,
            preventUnsendMessage,
            sendMuteMessage,
            Archived,
            ReadChecker,MySendMessage,ReadCheckerChatdataDelete,
            removeKeepUnread,
            KeepUnreadLSpatch,
            blockTracking,
            stopVersionCheck,
            outputCommunication,
            callTone,ringtonevolume,
            MuteTone,
            DialTone,
            DarkColor,DarkModSync,
            MuteGroup,
            PhotoAddNotification,GroupNotification,CansellNotification,AddCopyAction,
            RemoveVoiceRecord,
            AgeCheckSkip,
            CallOpenApplication,
            BlockCheck,SettingClick,
            photoboothButtonOption,voiceButtonOption,videoButtonOption,videoSingleButtonOption
            ,AutoUpDateCheck,PinList,

    };

}
