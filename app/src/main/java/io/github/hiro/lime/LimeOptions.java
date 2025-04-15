package io.github.hiro.lime;

import android.content.Context;

public class LimeOptions {

    public class Option {
        public final String name;
        public int id;
        public boolean checked;
        public OptionCategory category;

        public Option(String name, int id, boolean checked, OptionCategory category) {
            this.name = name;
            this.id = id;
            this.checked = checked;
            this.category = category;
        }
    }
    public enum OptionCategory {
        GENERAL(R.string.general),
        Ad(R.string.Ad),
        NOTIFICATIONS(R.string.notifications),
        Theme(R.string.Theme),
        CHAT(R.string.chat),
        CALL(R.string.call),
        OTHER(R.string.other);

        private final int resourceId;

        OptionCategory(int resourceId) {
            this.resourceId = resourceId;
        }

        public int getResourceId() {
            return resourceId;
        }

        public String getName(Context context) {
            return context.getString(resourceId);
        }
    }

        public Option removeAllServices = new Option("remove_Services", R.string.RemoveService, false,OptionCategory.GENERAL);
        public Option removeOption = new Option("removeOption", R.string.switch_unembed_options, false, OptionCategory.GENERAL);
        public Option removeVoom = new Option("remove_voom", R.string.switch_remove_voom, true, OptionCategory.GENERAL);
        public Option removeWallet = new Option("remove_wallet", R.string.switch_remove_wallet, true, OptionCategory.GENERAL);
        public Option removeNewsOrCall = new Option("remove_news_or_call", R.string.switch_remove_news_or_call, true, OptionCategory.GENERAL);
        public Option distributeEvenly = new Option("distribute_evenly", R.string.switch_distribute_evenly, true, OptionCategory.GENERAL);
        public Option extendClickableArea = new Option("extend_clickable_area", R.string.switch_extend_clickable_area, true, OptionCategory.GENERAL);
        public Option removeIconLabels = new Option("remove_icon_labels", R.string.switch_remove_icon_labels, true, OptionCategory.GENERAL);
        public Option removeServiceLabels = new Option("remove_service_labels", R.string.switch_remove_service_labels, false, OptionCategory.GENERAL);
        public Option removeSearchBar = new Option("removeSearchBar", R.string.removeSearchBar, false, OptionCategory.GENERAL);
        public Option removeNaviAlbum = new Option("removeNaviAlbum", R.string.removeNaviAlbum, false, OptionCategory.GENERAL);
        public Option removeNaviOpenchat = new Option("removeNaviOpenchat", R.string.removeNaviOpenchat, false, OptionCategory.GENERAL);

        public Option redirectWebView = new Option("redirect_webview", R.string.switch_redirect_webview, true, OptionCategory.GENERAL);
         public Option openInBrowser = new Option("open_in_browser", R.string.switch_open_in_browser, false, OptionCategory.GENERAL);
        public Option RemoveNotification = new Option("RemoveProfileNotification", R.string.removeNotification, false, OptionCategory.GENERAL);

        public Option removeAds = new Option("remove_ads", R.string.switch_remove_ads, true, OptionCategory.Ad);
        public Option removeRecommendation = new Option("remove_recommendation", R.string.switch_remove_recommendation, true, OptionCategory.Ad);
        public Option removePremiumRecommendation = new Option("remove_premium_recommendation", R.string.switch_remove_premium_recommendation, true, OptionCategory.Ad);
        public Option blockTracking = new Option("block_tracking", R.string.switch_block_tracking, false, OptionCategory.Ad);

        public Option preventMarkAsRead = new Option("prevent_mark_as_read", R.string.switch_prevent_mark_as_read, false, OptionCategory.CHAT);
        public Option preventUnsendMessage = new Option("prevent_unsend_message", R.string.switch_prevent_unsend_message, false, OptionCategory.CHAT);
        public Option sendMuteMessage = new Option("mute_message", R.string.switch_send_mute_message, false, OptionCategory.CHAT);
        public Option removeKeepUnread = new Option("remove_keep_unread", R.string.switch_remove_keep_unread, false, OptionCategory.CHAT);
        public Option KeepUnreadLSpatch = new Option("Keep_UnreadLSpatch", R.string.switch_KeepUnreadLSpatch, false, OptionCategory.CHAT);
        public Option Archived = new Option("Archived_message", R.string.switch_archived, false, OptionCategory.CHAT);
        public Option ReadChecker = new Option("ReadChecker", R.string.ReadChecker, false, OptionCategory.CHAT);
        public Option ReadCheckerChatdataDelete = new Option("ReadCheckerChatdataDelete", R.string.ReadCheckerChatdataDelete, false, OptionCategory.CHAT);
        public Option MySendMessage = new Option("MySendMessage", R.string.MySendMessage, false, OptionCategory.CHAT);
        public Option ReactionCount = new Option("ReactionCount", R.string.ReactionCount, false, OptionCategory.CHAT);


        public Option BlockCheck = new Option("BlockCheck", R.string.BlockCheck, true, OptionCategory.CHAT);

        public Option CansellNotification = new Option("CansellNotification", R.string.CansellNotification, false, OptionCategory.NOTIFICATIONS);
        public Option BlockUpdateProfileNotification = new Option("BlockUpdateProfileNotification", R.string.switch_BlockUpdateProfileNotification, false, OptionCategory.NOTIFICATIONS);

        public Option MuteGroup = new Option("Disabled_Group_notification", R.string.MuteGroup, false, OptionCategory.NOTIFICATIONS);
        public Option PhotoAddNotification = new Option("PhotoAddNotification", R.string.PhotoAddNotification, false, OptionCategory.NOTIFICATIONS);
        public Option GroupNotification = new Option("GroupNotification", R.string.GroupNotification, false, OptionCategory.NOTIFICATIONS);
        public Option AddCopyAction = new Option("AddCopyAction", R.string.AddCopyAction, false, OptionCategory.NOTIFICATIONS);
        public Option removeReplyMute = new Option("remove_reply_mute", R.string.switch_remove_reply_mute, true, OptionCategory.NOTIFICATIONS);
        public Option original_ID = new Option("original_ID", R.string.original_ID, false, OptionCategory.NOTIFICATIONS);


        public Option DarkColor = new Option("DarkColor", R.string.DarkColor, false, OptionCategory.Theme);
        public Option DarkModSync = new Option("DarkModSync", R.string.DarkkModSync, true, OptionCategory.Theme);

        public Option callTone = new Option("callTone", R.string.callTone, false, OptionCategory.CALL);
        public Option MuteTone = new Option("MuteTone", R.string.MuteTone, false, OptionCategory.CALL);
        public Option DialTone = new Option("DialTone", R.string.DialTone, false, OptionCategory.CALL);
        public Option ringtonevolume = new Option("ringtonevolume", R.string.ringtonevolume, false, OptionCategory.CALL);


        public Option RemoveVoiceRecord = new Option("RemoveVoiceRecord", R.string.RemoveVoiceRecord, false, OptionCategory.CHAT);
        public Option CallOpenApplication = new Option("CallOpenApplication", R.string.CallOpenApplication, true, OptionCategory.CALL);
        public Option SettingClick = new Option("SettingClick", R.string.SettingClick, false, OptionCategory.OTHER);
        public Option photoboothButtonOption = new Option("photoboothButtonOption", R.string.photoboothButtonOption, true, OptionCategory.CHAT);
        public Option voiceButtonOption = new Option("voiceButtonOption", R.string.voiceButtonOption, false, OptionCategory.CHAT);
        public Option videoButtonOption = new Option("videoButtonOption", R.string.videoButtonOption, true, OptionCategory.CHAT);
        public Option videoSingleButtonOption = new Option("videoSingleButtonOption", R.string.videoSingleButtonOption, true, OptionCategory.CHAT);

        public Option PinList = new Option("PinList", R.string.PinList, false, OptionCategory.CHAT);

        public Option outputCommunication = new Option("output_communication", R.string.switch_output_communication, false, OptionCategory.OTHER);
        public Option NewOption = new Option("NewOption", R.string.NewOption, false, OptionCategory.OTHER);
        public Option AgeCheckSkip = new Option("AgeCheckSkip", R.string.AgeCheckSkip, false, OptionCategory.OTHER);
        public Option AutoUpDateCheck = new Option("AutoUpDateCheck", R.string.AutoUpDateCheck, false, OptionCategory.OTHER);

        public Option PhotoSave = new Option("PhotoSave", R.string.PhotoSave, false, OptionCategory.CHAT);
        public Option stopVersionCheck = new Option("stop_version_check", R.string.switch_stop_version_check, false, OptionCategory.OTHER);
        public Option WhiteToDark = new Option("WhiteToDark", R.string.WhiteToDark, false, OptionCategory.Theme);


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
                ReadChecker, MySendMessage, ReadCheckerChatdataDelete,
                removeKeepUnread,
                KeepUnreadLSpatch,

                stopVersionCheck,
                outputCommunication,
                callTone, ringtonevolume,
                MuteTone,
                DialTone,
                DarkColor, DarkModSync,
                MuteGroup,
                PhotoAddNotification, GroupNotification, CansellNotification, AddCopyAction, original_ID,
                RemoveVoiceRecord,
                AgeCheckSkip,
                CallOpenApplication,
                BlockCheck, SettingClick,
                photoboothButtonOption, voiceButtonOption, videoButtonOption, videoSingleButtonOption,
                AutoUpDateCheck, PinList,
                NewOption, PhotoSave,
                ReactionCount,WhiteToDark
        };


}
