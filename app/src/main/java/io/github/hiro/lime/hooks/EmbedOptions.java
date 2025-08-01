package io.github.hiro.lime.hooks;


import static io.github.hiro.lime.Main.limeOptions;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AndroidAppHelper;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.text.InputType;
import android.text.method.ScrollingMovementMethod;
import android.util.Base64;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.documentfile.provider.DocumentFile;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.hiro.lime.BuildConfig;
import io.github.hiro.lime.LimeOptions;
import io.github.hiro.lime.R;
import io.github.hiro.lime.Utils;

public class EmbedOptions implements IHook {
    @Override
    public void hook(LimeOptions limeOptions, XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        if (limeOptions.removeOption.checked) return;


        XposedBridge.hookAllMethods(
                loadPackageParam.classLoader.loadClass("com.linecorp.line.settings.lab.LineUserLabSettingsFragment"),
                "onViewCreated",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        Context contextV = getTargetAppContext(loadPackageParam);
                        Context moduleContext = AndroidAppHelper.currentApplication().createPackageContext(
                                "io.github.hiro.lime", Context.CONTEXT_IGNORE_SECURITY);
                        String backupUri = loadBackupUri(contextV);
                        if (backupUri == null) {
                            XposedBridge.log("Lime: Settings URI not configured");
                            return;
                        }

                        Uri treeUri = Uri.parse(backupUri);


                        if (limeOptions.NewOption.checked) {

                            try {
                                PackageManager pm = contextV.getPackageManager();
                                String versionName = "";
                                try {
                                    versionName = pm.getPackageInfo(loadPackageParam.packageName, 0).versionName;
                                } catch (PackageManager.NameNotFoundException e) {
                                    XposedBridge.log("Lime: Package info error: " + e.getMessage());
                                }

                                Set<String> checkedOptions = new HashSet<>();
                                DocumentPreferences docPrefs = new DocumentPreferences(contextV, treeUri);
                                for (LimeOptions.Option option : limeOptions.options) {
                                    if (!checkedOptions.contains(option.name)) {
                                        option.checked = Boolean.parseBoolean(
                                                docPrefs.getSetting(option.name, String.valueOf(option.checked))
                                        );
                                        checkedOptions.add(option.name);
                                    }
                                }
                                ViewGroup viewGroup = ((ViewGroup) param.args[0]);
                                Context context = viewGroup.getContext();
                                Utils.addModuleAssetPath(context);

                                FrameLayout rootLayout = new FrameLayout(context);
                                rootLayout.setLayoutParams(new ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT));

                                Button button = new Button(context);
                                button.setText(R.string.app_name);

                                FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                                        FrameLayout.LayoutParams.WRAP_CONTENT,
                                        FrameLayout.LayoutParams.WRAP_CONTENT);
                                layoutParams.gravity = Gravity.TOP | Gravity.END;
                                layoutParams.rightMargin = Utils.dpToPx(10, context);

                                int statusBarHeight = getStatusBarHeight(context);

                                String versionNameStr = String.valueOf(versionName);
                                String majorVersionStr = versionNameStr.split("\\.")[0]; // Extract the major version number
                                int versionNameInt = Integer.parseInt(majorVersionStr); // Convert the major version to an integer

                                if (versionNameInt >= 15) {
                                    layoutParams.topMargin = statusBarHeight; // Set margin to status bar height
                                } else {
                                    layoutParams.topMargin = Utils.dpToPx(5, context); // Set margin to 5dp
                                }
                                button.setLayoutParams(layoutParams);

                                button.setOnClickListener(view -> {
                                    // カテゴリ一覧画面を生成

                                    if (backupUri == null) {
                                        Toast.makeText(context, "Please select settings folder first", Toast.LENGTH_LONG).show();
                                        return;
                                    }
                                    ScrollView categoryLayout = createCategoryListLayout(context, Arrays.asList(limeOptions.options), docPrefs, moduleContext, loadPackageParam);
                                    showView(rootLayout, categoryLayout);
                                });
                                rootLayout.addView(button);
                                viewGroup.addView(rootLayout);
                            } catch (Exception e) {
                                new Handler(Looper.getMainLooper()).post(() -> {
                                    if (moduleContext != null) {
                                        Toast.makeText(
                                                moduleContext,
                                                moduleContext.getString(R.string.Error_Create_setting_Button)
                                                        + moduleContext.getString(R.string.save_failed),
                                                Toast.LENGTH_LONG
                                        ).show();
                                    }
                                });
                            }


                        } else {
                            DocumentPreferences docPrefs = new DocumentPreferences(contextV, treeUri);
                            try {
                                PackageManager pm = contextV.getPackageManager();
                                String versionName = "";
                                try {
                                    versionName = pm.getPackageInfo(loadPackageParam.packageName, 0).versionName;
                                } catch (PackageManager.NameNotFoundException e) {
                                    e.printStackTrace();
                                }


                                Set<String> checkedOptions = new HashSet<>();

                                for (LimeOptions.Option option : limeOptions.options) {
                                    if (!checkedOptions.contains(option.name)) {
                                        // DocumentPreferencesから設定を読み込む
                                        option.checked = Boolean.parseBoolean(
                                                docPrefs.getSetting(option.name, String.valueOf(option.checked))
                                        );
                                        checkedOptions.add(option.name);
                                    }
                                }
                                ViewGroup viewGroup = ((ViewGroup) param.args[0]);
                                Context context = viewGroup.getContext();
                                Utils.addModuleAssetPath(context);

                                LinearLayout layout = new LinearLayout(context);
                                layout.setLayoutParams(new LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.MATCH_PARENT,
                                        LinearLayout.LayoutParams.MATCH_PARENT));
                                layout.setOrientation(LinearLayout.VERTICAL);
                                layout.setPadding(Utils.dpToPx(20, context), Utils.dpToPx(20, context), Utils.dpToPx(20, context), Utils.dpToPx(20, context));
                                Switch switchRedirectWebView = null;
                                Switch photoAddNotificationView = null;
                                Switch ReadCheckerView = null;
                                Switch preventUnsendMessageView = null;
                                Switch DarkModeView = null;
                                List<Switch> webViewChildSwitches = new ArrayList<>();
                                List<Switch> photoNotificationChildSwitches = new ArrayList<>();
                                List<Switch> ReadCheckerSwitches = new ArrayList<>();
                                List<Switch> preventUnsendMessageSwitches = new ArrayList<>();
                                List<Switch> DarkModeSwitches = new ArrayList<>();
                                for (LimeOptions.Option option : limeOptions.options) {
                                    final String name = option.name;

                                    Switch switchView = new Switch(context);
                                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                            LinearLayout.LayoutParams.WRAP_CONTENT,
                                            LinearLayout.LayoutParams.WRAP_CONTENT
                                    );
                                    params.topMargin = Utils.dpToPx(20, context);
                                    switchView.setLayoutParams(params);
                                    switchView.setText(option.id);
                                    switchView.setChecked(option.checked);
                                    switchView.setTextColor(Color.WHITE);
                                    switch (name) {
                                        case "redirect_webview":
                                            switchRedirectWebView = switchView;
                                            switchRedirectWebView.setOnCheckedChangeListener((buttonView, isChecked) -> {
                                                for (Switch child : webViewChildSwitches) {
                                                    child.setEnabled(isChecked);
                                                    if (!isChecked) child.setChecked(false);
                                                }
                                            });
                                            break;
                                        case "PhotoAddNotification":
                                            photoAddNotificationView = switchView;
                                            photoAddNotificationView.setOnCheckedChangeListener((buttonView, isChecked) -> {
                                                for (Switch child : photoNotificationChildSwitches) {
                                                    child.setEnabled(isChecked);
                                                    if (!isChecked) child.setChecked(false);
                                                }
                                            });
                                            break;
                                        case "ReadChecker":
                                            ReadCheckerView = switchView;
                                            ReadCheckerView.setOnCheckedChangeListener((buttonView, isChecked) -> {
                                                for (Switch child : ReadCheckerSwitches) {
                                                    child.setEnabled(isChecked);
                                                    if (!isChecked) child.setChecked(false);
                                                }
                                            });
                                            break;
                                        case "prevent_unsend_message":
                                            preventUnsendMessageView = switchView;
                                            preventUnsendMessageView.setOnCheckedChangeListener((buttonView, isChecked) -> {
                                                for (Switch child : preventUnsendMessageSwitches) {
                                                    child.setEnabled(isChecked);
                                                    if (!isChecked) child.setChecked(false);
                                                }
                                            });
                                            break;

                                        case "DarkColor":
                                            DarkModeView = switchView;
                                            DarkModeView.setOnCheckedChangeListener((buttonView, isChecked) -> {
                                                for (Switch child : DarkModeSwitches) {
                                                    child.setEnabled(isChecked);
                                                    if (!isChecked) child.setChecked(false);
                                                }
                                            });
                                            break;

                                        case "open_in_browser":
                                            webViewChildSwitches.add(switchView);
                                            switchView.setEnabled(switchRedirectWebView != null && switchRedirectWebView.isChecked());
                                            break;

                                        case "hide_canceled_message":
                                            preventUnsendMessageSwitches.add(switchView);
                                            switchView.setEnabled(preventUnsendMessageView != null && preventUnsendMessageView.isChecked());
                                            break;

                                        case "GroupNotification":
                                        case "CansellNotification":
                                        case "AddCopyAction":
                                        case "original_ID":
                                        case "DisableSilentMessage":
                                            photoNotificationChildSwitches.add(switchView);
                                            switchView.setEnabled(photoAddNotificationView != null && photoAddNotificationView.isChecked());
                                            break;

                                        case "MySendMessage":
                                        case "ReadCheckerChatdataDelete":
                                            ReadCheckerSwitches.add(switchView);
                                            switchView.setEnabled(ReadCheckerView != null && ReadCheckerView.isChecked());
                                            break;

                                        case "DarkModSync":
                                            DarkModeSwitches.add(switchView);
                                            switchView.setEnabled(DarkModeView != null && DarkModeView.isChecked());
                                            break;
                                    }

                                    layout.addView(switchView);
                                }

                                if (switchRedirectWebView != null) {
                                    switchRedirectWebView.setChecked(switchRedirectWebView.isChecked());
                                }
                                if (photoAddNotificationView != null) {
                                    photoAddNotificationView.setChecked(photoAddNotificationView.isChecked());
                                }

                                {
                                    final String script = new String(Base64.decode(docPrefs.getSetting("encoded_js_modify_request", ""), Base64.NO_WRAP));
                                    LinearLayout layoutModifyRequest = new LinearLayout(context);
                                    layoutModifyRequest.setLayoutParams(new LinearLayout.LayoutParams(
                                            LinearLayout.LayoutParams.MATCH_PARENT,
                                            LinearLayout.LayoutParams.MATCH_PARENT));
                                    layoutModifyRequest.setOrientation(LinearLayout.VERTICAL);
                                    layoutModifyRequest.setPadding(Utils.dpToPx(20, context), Utils.dpToPx(20, context), Utils.dpToPx(20, context), Utils.dpToPx(20, context));

                                    EditText editText = new EditText(context);
                                    editText.setLayoutParams(new LinearLayout.LayoutParams(
                                            LinearLayout.LayoutParams.MATCH_PARENT,
                                            LinearLayout.LayoutParams.WRAP_CONTENT));
                                    editText.setTypeface(Typeface.MONOSPACE);
                                    editText.setInputType(InputType.TYPE_CLASS_TEXT |
                                            InputType.TYPE_TEXT_FLAG_MULTI_LINE |
                                            InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS |
                                            InputType.TYPE_TEXT_FLAG_IME_MULTI_LINE);
                                    editText.setMovementMethod(new ScrollingMovementMethod());
                                    editText.setTextIsSelectable(true);
                                    editText.setHorizontallyScrolling(true);
                                    editText.setVerticalScrollBarEnabled(true);
                                    editText.setHorizontalScrollBarEnabled(true);
                                    editText.setText(script);

                                    layoutModifyRequest.addView(editText);

                                    LinearLayout buttonLayout = new LinearLayout(context);
                                    LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                                            LinearLayout.LayoutParams.MATCH_PARENT,
                                            LinearLayout.LayoutParams.WRAP_CONTENT);
                                    buttonParams.topMargin = Utils.dpToPx(10, context);
                                    buttonLayout.setLayoutParams(buttonParams);
                                    buttonLayout.setOrientation(LinearLayout.HORIZONTAL);

                                    Button copyButton = new Button(context);
                                    copyButton.setText(R.string.button_copy);
                                    copyButton.setOnClickListener(v -> {
                                        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                                        ClipData clip = ClipData.newPlainText("", editText.getText().toString());
                                        clipboard.setPrimaryClip(clip);
                                    });

                                    buttonLayout.addView(copyButton);

                                    Button pasteButton = new Button(context);
                                    pasteButton.setText(R.string.button_paste);
                                    pasteButton.setOnClickListener(v -> {
                                        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                                        if (clipboard != null && clipboard.hasPrimaryClip()) {
                                            ClipData clip = clipboard.getPrimaryClip();
                                            if (clip != null && clip.getItemCount() > 0) {
                                                CharSequence pasteData = clip.getItemAt(0).getText();
                                                editText.setText(pasteData);
                                            }
                                        }
                                    });

                                    buttonLayout.addView(pasteButton);
                                    layoutModifyRequest.addView(buttonLayout);
                                    ScrollView scrollView = new ScrollView(context);
                                    scrollView.addView(layoutModifyRequest);
                                    AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.Theme_AppCompat_Dialog)
                                            .setTitle(R.string.modify_request);
                                    builder.setView(scrollView);
                                    Button backupButton = new Button(context);
                                    buttonParams.topMargin = Utils.dpToPx(20, context);
                                    backupButton.setLayoutParams(buttonParams);
                                    backupButton.setText(moduleContext.getResources().getString(R.string.Back_Up));
                                    backupButton.setOnClickListener(v -> backupChatHistory(context, moduleContext));
                                    layout.addView(backupButton);


                                    Button restoreButton = new Button(context);
                                    restoreButton.setLayoutParams(buttonParams);
                                    restoreButton.setText(moduleContext.getResources().getString(R.string.Restore));
                                    restoreButton.setOnClickListener(v -> showFilePickerChatHistory(context, moduleContext));
                                    layout.addView(restoreButton);

                                    Button restoreChatListButton = new Button(context);
                                    restoreChatListButton.setLayoutParams(buttonParams);
                                    restoreChatListButton.setText(moduleContext.getResources().getString(R.string.restoreChatListButton));
                                    restoreChatListButton.setOnClickListener(v -> showFilePickerChatlist(context, moduleContext));
                                    layout.addView(restoreChatListButton);

                                    Button backupfolderButton = new Button(context);
                                    backupfolderButton.setLayoutParams(buttonParams);
                                    backupfolderButton.setText(moduleContext.getResources().getString(R.string.Talk_Picture_Back_up));
                                    backupfolderButton.setOnClickListener(v -> backupChatsFolder(context, moduleContext));
                                    layout.addView(backupfolderButton);

                                    Button restorefolderButton = new Button(context);
                                    restorefolderButton.setLayoutParams(buttonParams);
                                    restorefolderButton.setText(moduleContext.getResources().getString(R.string.Picure_Restore));
                                    restorefolderButton.setOnClickListener(v -> restoreChatsFolder(context, moduleContext));
                                    layout.addView(restorefolderButton);
                                    if (limeOptions.MuteGroup.checked) {
                                        Button MuteGroups_Button = new Button(context);
                                        MuteGroups_Button.setLayoutParams(buttonParams);
                                        MuteGroups_Button.setText(moduleContext.getResources().getString(R.string.Mute_Group));
                                        MuteGroups_Button.setOnClickListener(v -> MuteGroups_Button(context, moduleContext));
                                        layout.addView(MuteGroups_Button);
                                    }

                                    Button KeepUnread_Button = new Button(context);
                                    KeepUnread_Button.setLayoutParams(buttonParams);
                                    KeepUnread_Button.setText(moduleContext.getResources().getString(R.string.edit_margin_settings));
                                    KeepUnread_Button.setOnClickListener(v -> KeepUnread_Button(context, moduleContext));
                                    layout.addView(KeepUnread_Button);

                                    if (limeOptions.preventUnsendMessage.checked) {
                                        Button canceled_message_Button = new Button(context);
                                        canceled_message_Button.setLayoutParams(buttonParams);
                                        canceled_message_Button.setText(moduleContext.getResources().getString(R.string.canceled_message));
                                        canceled_message_Button.setOnClickListener(v -> Cancel_Message_Button(context, moduleContext));
                                        layout.addView(canceled_message_Button);
                                    }
                                    if (limeOptions.CansellNotification.checked) {

                                        Button CansellNotification_Button = new Button(context);
                                        CansellNotification_Button.setLayoutParams(buttonParams);
                                        CansellNotification_Button.setText(moduleContext.getResources().getString(R.string.CansellNotification));
                                        CansellNotification_Button.setOnClickListener(v -> CansellNotification(context, moduleContext));
                                        layout.addView(CansellNotification_Button);
                                    }

                                    if (limeOptions.BlockCheck.checked) {

                                        Button BlockCheck_Button = new Button(context);
                                        BlockCheck_Button.setLayoutParams(buttonParams);
                                        BlockCheck_Button.setText(moduleContext.getResources().getString(R.string.BlockCheck));
                                        BlockCheck_Button.setOnClickListener(v -> Blocklist(context, moduleContext));
                                        layout.addView(BlockCheck_Button);
                                    }
                                    if (limeOptions.preventUnsendMessage.checked) {
                                        Button hide_canceled_message_Button = new Button(context);
                                        hide_canceled_message_Button.setLayoutParams(buttonParams);
                                        hide_canceled_message_Button.setText(moduleContext.getResources().getString(R.string.hide_canceled_message));
                                        hide_canceled_message_Button.setOnClickListener(v -> new AlertDialog.Builder(context, R.style.Theme_AppCompat_Dialog)
                                                .setTitle(moduleContext.getResources().getString(R.string.HideSetting))
                                                .setMessage(moduleContext.getResources().getString(R.string.HideSetting_selection))
                                                .setPositiveButton(moduleContext.getResources().getString(R.string.Hide), (dialog, which) -> updateMessagesVisibility(context, true, moduleContext))
                                                .setNegativeButton(moduleContext.getResources().getString(R.string.Show), (dialog, which) -> updateMessagesVisibility(context, false, moduleContext))
                                                .setIcon(android.R.drawable.ic_dialog_info)
                                                .show());
                                        layout.addView(hide_canceled_message_Button);
                                    }

                                    if (limeOptions.PinList.checked) {

                                        Button PinList_Button = new Button(context);
                                        PinList_Button.setLayoutParams(buttonParams);
                                        PinList_Button.setText(moduleContext.getResources().getString(R.string.PinList));
                                        PinList_Button.setOnClickListener(v -> PinListButton(context, moduleContext));
                                        layout.addView(PinList_Button);
                                    }
                                    Button resetButton = new Button(context);
                                    resetButton.setLayoutParams(buttonParams);
                                    resetButton.setText(moduleContext.getResources().getString(R.string.ResetButton));

                                    resetButton.setOnClickListener(v -> {
                                        try {

                                            File settingsDir  = new File(context.getFilesDir(), "LimeBackup");
                                            File settingsFile = new File(settingsDir, "backup_uri.txt");

                                            String toastMsg;
                                            if (settingsFile.exists()) {
                                                boolean deleted = settingsFile.delete();
                                                toastMsg = deleted
                                                        ? " Reset URI"
                                                        : "Error";

                                                Toast.makeText(context.getApplicationContext(), context.getString(R.string.restarting), Toast.LENGTH_SHORT).show();
                                                context.startActivity(new Intent().setClassName(Constants.PACKAGE_NAME, "jp.naver.line.android.activity.SplashActivity"));
                                                Process.killProcess(Process.myPid());
                                            } else {
                                                toastMsg = "リセット対象の URI はありません";
                                            }

                                            Toast.makeText(moduleContext, toastMsg, Toast.LENGTH_SHORT).show();
                                            XposedBridge.log("Lime: ResetButton pressed – " + toastMsg);
                                        } catch (Throwable t) {
                                            XposedBridge.log(t);
                                            Toast.makeText(moduleContext,
                                                    "エラー: " + t.getMessage(),
                                                    Toast.LENGTH_LONG).show();
                                        }
                                    });

                                    layout.addView(resetButton);

                                    builder.setPositiveButton(R.string.positive_button, (dialog, which) -> {
                                        String code = editText.getText().toString();
                                        if (!code.equals(script)) {
                                            try {

                                                if (backupUri == null) {
                                                    Toast.makeText(contextV, "Settings URI not configured", Toast.LENGTH_LONG).show();
                                                    return;
                                                }
                                                // Base64エンコードして設定を保存
                                                String encodedCode = Base64.encodeToString(code.getBytes(), Base64.NO_WRAP);
                                                docPrefs.saveSetting("encoded_js_modify_request", encodedCode);

                                                // アプリを再起動
                                                Toast.makeText(contextV.getApplicationContext(),
                                                        contextV.getString(R.string.restarting),
                                                        Toast.LENGTH_SHORT).show();
                                                Process.killProcess(Process.myPid());
                                                contextV.startActivity(new Intent()
                                                        .setClassName(Constants.PACKAGE_NAME,
                                                                "jp.naver.line.android.activity.SplashActivity"));

                                            } catch (IOException e) {
                                                Toast.makeText(contextV,
                                                        "Failed to save settings: " + e.getMessage(),
                                                        Toast.LENGTH_LONG).show();
                                                XposedBridge.log("Save modify request error: " + e.getMessage());
                                            }
                                        }
                                    });

                                    builder.setNegativeButton(R.string.negative_button, null);

                                    builder.setOnDismissListener(dialog -> editText.setText(script));

                                    AlertDialog dialog = builder.create();
                                    Button button = new Button(context);
                                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                            LinearLayout.LayoutParams.WRAP_CONTENT,
                                            LinearLayout.LayoutParams.WRAP_CONTENT);
                                    params.topMargin = Utils.dpToPx(20, context);
                                    button.setLayoutParams(params);
                                    button.setText(R.string.modify_request);
                                    button.setOnClickListener(view -> dialog.show());
                                    layout.addView(button);
                                }

                                {
                                    final String script = new String(Base64.decode(docPrefs.getSetting("encoded_js_modify_response", ""), Base64.NO_WRAP));
                                    LinearLayout layoutModifyResponse = new LinearLayout(context);
                                    layoutModifyResponse.setLayoutParams(new LinearLayout.LayoutParams(
                                            LinearLayout.LayoutParams.MATCH_PARENT,
                                            LinearLayout.LayoutParams.MATCH_PARENT));
                                    layoutModifyResponse.setOrientation(LinearLayout.VERTICAL);
                                    layoutModifyResponse.setPadding(Utils.dpToPx(20, context), Utils.dpToPx(20, context), Utils.dpToPx(20, context), Utils.dpToPx(20, context));
                                    EditText editText = new EditText(context);
                                    editText.setLayoutParams(new LinearLayout.LayoutParams(
                                            LinearLayout.LayoutParams.MATCH_PARENT,
                                            LinearLayout.LayoutParams.WRAP_CONTENT));
                                    editText.setTypeface(Typeface.MONOSPACE);
                                    editText.setInputType(InputType.TYPE_CLASS_TEXT |
                                            InputType.TYPE_TEXT_FLAG_MULTI_LINE |
                                            InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS |
                                            InputType.TYPE_TEXT_FLAG_IME_MULTI_LINE);
                                    editText.setMovementMethod(new ScrollingMovementMethod());
                                    editText.setTextIsSelectable(true);
                                    editText.setHorizontallyScrolling(true);
                                    editText.setVerticalScrollBarEnabled(true);
                                    editText.setHorizontalScrollBarEnabled(true);
                                    editText.setText(script);
                                    layoutModifyResponse.addView(editText);
                                    LinearLayout buttonLayout = new LinearLayout(context);
                                    LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                                            LinearLayout.LayoutParams.MATCH_PARENT,
                                            LinearLayout.LayoutParams.WRAP_CONTENT);
                                    buttonParams.topMargin = Utils.dpToPx(10, context);
                                    buttonLayout.setLayoutParams(buttonParams);
                                    buttonLayout.setOrientation(LinearLayout.HORIZONTAL);

                                    Button copyButton = new Button(context);
                                    copyButton.setText(R.string.button_copy);
                                    copyButton.setOnClickListener(v -> {
                                        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                                        ClipData clip = ClipData.newPlainText("", editText.getText().toString());
                                        clipboard.setPrimaryClip(clip);
                                    });

                                    buttonLayout.addView(copyButton);

                                    Button pasteButton = new Button(context);
                                    pasteButton.setText(R.string.button_paste);
                                    pasteButton.setOnClickListener(v -> {
                                        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                                        if (clipboard != null && clipboard.hasPrimaryClip()) {
                                            ClipData clip = clipboard.getPrimaryClip();
                                            if (clip != null && clip.getItemCount() > 0) {
                                                CharSequence pasteData = clip.getItemAt(0).getText();
                                                editText.setText(pasteData);
                                            }
                                        }
                                    });

                                    buttonLayout.addView(pasteButton);
                                    layoutModifyResponse.addView(buttonLayout);
                                    ScrollView scrollView = new ScrollView(context);
                                    scrollView.addView(layoutModifyResponse);
                                    AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.Theme_AppCompat_Dialog)
                                            .setTitle(R.string.modify_response);
                                    builder.setView(scrollView);
                                    builder.setPositiveButton(R.string.positive_button, (dialog, which) -> {
                                        String code = editText.getText().toString();
                                        if (!code.equals(script)) {
                                            try {
                                                docPrefs.saveSetting("encoded_js_modify_response", Base64.encodeToString(code.getBytes(), Base64.NO_WRAP));
                                            } catch (IOException e) {
                                                throw new RuntimeException(e);
                                            }
                                            Toast.makeText(context.getApplicationContext(), context.getString(R.string.restarting), Toast.LENGTH_SHORT).show();
                                            Process.killProcess(Process.myPid());
                                            context.startActivity(new Intent().setClassName(Constants.PACKAGE_NAME, "jp.naver.line.android.activity.SplashActivity"));
                                        }
                                    });

                                    builder.setNegativeButton(R.string.negative_button, null);

                                    builder.setOnDismissListener(dialog -> editText.setText(script));
                                    AlertDialog dialog = builder.create();
                                    Button button = new Button(context);
                                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                            LinearLayout.LayoutParams.WRAP_CONTENT,
                                            LinearLayout.LayoutParams.WRAP_CONTENT);
                                    params.topMargin = Utils.dpToPx(20, context);
                                    button.setLayoutParams(params);
                                    button.setText(R.string.modify_response);
                                    button.setOnClickListener(view -> dialog.show());

                                    layout.addView(button);
                                }
                                String LIMEs_versionName = BuildConfig.VERSION_NAME; // versionNameを取得
                                ScrollView scrollView = new ScrollView(context);
                                scrollView.addView(layout);
                                AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.Theme_AppCompat_Dialog)
                                        .setTitle("LIMEs" + " (" + LIMEs_versionName + ")");
                                builder.setView(scrollView);
                                builder.setPositiveButton(R.string.positive_button, (dialog, which) -> {
                                    boolean optionChanged = false;
                                    boolean saveSuccess = true; // 保存成功フラグ

                                    for (int i = 0; i < limeOptions.options.length; ++i) {
                                        Switch switchView = (Switch) layout.getChildAt(i);
                                        boolean isChecked = switchView.isChecked();

                                        // 変更があったかチェック
                                        if (limeOptions.options[i].checked != isChecked) {
                                            optionChanged = true;
                                        }

                                        // 保存処理の結果をチェック
                                        try {
                                            if (!docPrefs.saveSetting(limeOptions.options[i].name, String.valueOf(isChecked))) {
                                                saveSuccess = false;
                                            }
                                        } catch (IOException e) {
                                            throw new RuntimeException(e);
                                        }
                                    }

                                    if (!saveSuccess) {
                                        Toast.makeText(context, context.getString(R.string.save_failed), Toast.LENGTH_LONG).show();
                                    } else if (optionChanged) {
                                        Toast.makeText(context.getApplicationContext(), context.getString(R.string.restarting), Toast.LENGTH_SHORT).show();
                                        Process.killProcess(Process.myPid());
                                        context.startActivity(new Intent().setClassName(Constants.PACKAGE_NAME, "jp.naver.line.android.activity.SplashActivity"));
                                    }
                                });

                                builder.setNegativeButton(R.string.negative_button, null);

                                builder.setOnDismissListener(dialog -> {
                                    for (int i = 0; i < limeOptions.options.length; ++i) {
                                        Switch switchView = (Switch) layout.getChildAt(i);
                                        switchView.setChecked(limeOptions.options[i].checked);
                                    }
                                });

                                AlertDialog dialog = builder.create();
                                Button button = new Button(context);
                                button.setText(R.string.app_name);

                                FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                                        FrameLayout.LayoutParams.WRAP_CONTENT,
                                        FrameLayout.LayoutParams.WRAP_CONTENT);
                                layoutParams.gravity = Gravity.TOP | Gravity.END;
                                layoutParams.rightMargin = Utils.dpToPx(10, context);

                                // ステータスバーの高さを取得
                                int statusBarHeight = getStatusBarHeight(context);

                                // ステータスバーの高さに係数（0.1）を掛けて調整

                                String versionNameStr = String.valueOf(versionName);
                                String majorVersionStr = versionNameStr.split("\\.")[0]; // Extract the major version number
                                int versionNameInt = Integer.parseInt(majorVersionStr); // Convert the major version to an integer

                                if (versionNameInt >= 15) {
                                    layoutParams.topMargin = statusBarHeight; // Set margin to status bar height
                                } else {
                                    layoutParams.topMargin = Utils.dpToPx(5, context); // Set margin to 5dp
                                }
                                button.setLayoutParams(layoutParams);
                                button.setOnClickListener(view -> dialog.show());

                                FrameLayout frameLayout = new FrameLayout(context);
                                frameLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                                frameLayout.addView(button);
                                viewGroup.addView(frameLayout);

                            } catch (Exception e) {
                                // エラー情報を詳細にログ出力
                                // XposedBridge.log("設定作成エラー: " + e.getClass().getSimpleName() + " - " + e.getMessage());


                                try {
                                    // UIスレッドでToastを表示
                                    new Handler(Looper.getMainLooper()).post(() -> {
                                        if (moduleContext != null) {
                                            Toast.makeText(
                                                    moduleContext,
                                                    moduleContext.getString(R.string.Error_Create_setting_Button)
                                                            + "\nError: " + e.getClass().getSimpleName(),
                                                    Toast.LENGTH_LONG
                                            ).show();
                                        } else {
                                            // XposedBridge.log("Toast表示失敗: moduleContextがnullです");
                                        }
                                    });
                                } catch (Throwable t) {
                                    // XposedBridge.log("Toast表示中に例外発生: " + t);
                                    t.printStackTrace();
                                }
                            }
                        }
                    }
                    });

// ベースクラス名を定義
        String baseName = "androidx.fragment.app.";
        List<String> validClasses = new ArrayList<>();

// a-zの全文字をループしてクラス存在チェック
        for (char c = 'a'; c <= 'z'; c++) {
            String className = baseName + c;
            try {
                // クラスの存在確認
                Class<?> clazz = loadPackageParam.classLoader.loadClass(className);

                // onActivityResultメソッドの存在確認
                try {
                    clazz.getDeclaredMethod("onActivityResult", int.class, int.class, Intent.class);
                    validClasses.add(className);
                    XposedBridge.log("Found valid fragment class: " + className);
                } catch (NoSuchMethodException ignored) {
                    // メソッドがない場合はスキップ
                }
            } catch (ClassNotFoundException ignored) {
                // クラスがない場合はスキップ
            }
        }

        if (validClasses.isEmpty()) {
            XposedBridge.log("No valid fragment class found with onActivityResult method");
        } else {
            // 全ての有効なクラスに対してフックを試みる
            for (String fragmentClass : validClasses) {
                try {
                    XposedHelpers.findAndHookMethod(
                            fragmentClass,
                            loadPackageParam.classLoader,
                            "onActivityResult",
                            int.class, int.class, Intent.class,
                            new XC_MethodHook() {
                                @Override
                                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                    Context moduleContext = AndroidAppHelper.currentApplication().createPackageContext(
                                            "io.github.hiro.lime", Context.CONTEXT_IGNORE_SECURITY);
                                    int requestCode = (int) param.args[0];
                                    int resultCode = (int) param.args[1];
                                    Intent data = (Intent) param.args[2];

                                    if (requestCode == PICK_FILE_REQUEST_CODE
                                            && resultCode == Activity.RESULT_OK
                                            && data != null) {

                                        Context context = (Context) param.thisObject;
                                        Uri uri = data.getData();

                                        new Thread(() -> {
                                            File tempFile = null;
                                            try {
                                                tempFile = File.createTempFile("restore", ".db", context.getCacheDir());
                                                tempFile.setReadable(true, false);

                                                try (InputStream is = context.getContentResolver().openInputStream(uri);
                                                     OutputStream os = new FileOutputStream(tempFile)) {

                                                    byte[] buffer = new byte[8192];
                                                    int length;
                                                    while ((length = is.read(buffer)) > 0) {
                                                        os.write(buffer, 0, length);
                                                    }
                                                    os.flush();

                                                    if (tempFile.length() == 0) {
                                                        throw new IOException("Copied file is empty");
                                                    }

                                                    File finalTempFile = tempFile;
                                                    new Handler(Looper.getMainLooper()).post(() -> {
                                                        restoreChatHistory(context, moduleContext, finalTempFile);
                                                    });
                                                }
                                            } catch (Exception e) {
                                                Log.e("FileCopy", "Error copying file", e);
                                                new Handler(Looper.getMainLooper()).post(() ->
                                                        Toast.makeText(context,
                                                                moduleContext.getString(R.string.file_copy_error) + ": " + e.getMessage(),
                                                                Toast.LENGTH_LONG).show());

                                                if (tempFile != null && tempFile.exists()) {
                                                    tempFile.delete();
                                                }
                                            }
                                        }).start();
                                    }
                                    if (requestCode == PICK_FILE_REQUEST_CODE2
                                            && resultCode == Activity.RESULT_OK
                                            && data != null) {

                                        Context context = (Context) param.thisObject;
                                        Uri uri = data.getData();

                                        new Thread(() -> {
                                            File tempFile = null;
                                            try {
                                                tempFile = File.createTempFile("restore", ".db", context.getCacheDir());
                                                tempFile.setReadable(true, false);

                                                try (InputStream is = context.getContentResolver().openInputStream(uri);
                                                     OutputStream os = new FileOutputStream(tempFile)) {

                                                    byte[] buffer = new byte[8192];
                                                    int length;
                                                    while ((length = is.read(buffer)) > 0) {
                                                        os.write(buffer, 0, length);
                                                    }
                                                    os.flush();
                                                    if (tempFile.length() == 0) {
                                                        throw new IOException("Copied file is empty");
                                                    }

                                                    File finalTempFile = tempFile;
                                                    new Handler(Looper.getMainLooper()).post(() -> {
                                                        restoreChatList(context, moduleContext, finalTempFile);
                                                    });
                                                }
                                            } catch (Exception e) {
                                                Log.e("FileCopy", "Error copying file", e);
                                                new Handler(Looper.getMainLooper()).post(() ->
                                                        Toast.makeText(context,
                                                                moduleContext.getString(R.string.file_copy_error) + ": " + e.getMessage(),
                                                                Toast.LENGTH_LONG).show());

                                                if (tempFile != null && tempFile.exists()) {
                                                    tempFile.delete();
                                                }
                                            }
                                        }).start();
                                    }
                                }
                            }
                    );
                    XposedBridge.log("Successfully hooked onActivityResult in: " + fragmentClass);
                } catch (Throwable t) {
                    XposedBridge.log("Failed to hook onActivityResult in " + fragmentClass + ": " + t.getMessage());
                }
            }
        }
    }


    private void showView(ViewGroup parent, View view) {

        ViewGroup currentParent = (ViewGroup) view.getParent();
        if (currentParent != null) {

            currentParent.removeView(view);
        }
        parent.removeAllViews();
        parent.addView(view);
    }

    private ScrollView createCategoryListLayout(Context context, List<LimeOptions.Option> options, DocumentPreferences docPrefs, Context moduleContext, XC_LoadPackage.LoadPackageParam loadPackageParam) {
        // ScrollView を親レイアウトとして作成
        ScrollView scrollView = new ScrollView(context);
        scrollView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        // スクロール対象の LinearLayout
        LinearLayout layout = new LinearLayout(context);
        layout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(Utils.dpToPx(20, context), Utils.dpToPx(20, context), Utils.dpToPx(20, context), Utils.dpToPx(20, context));
        layout.setBackgroundColor(Color.BLACK);
        scrollView.setFillViewport(true);

        // バージョン情報表示
        String LIMEs_versionName = BuildConfig.VERSION_NAME;
        TextView versionTextView = new TextView(context);
        versionTextView.setText("LIMEs" + " (" + LIMEs_versionName + ")");
        versionTextView.setTextSize(16);
        versionTextView.setTextColor(Color.WHITE);
        versionTextView.setGravity(Gravity.CENTER);
        versionTextView.setPadding(0, Utils.dpToPx(10, context), 0, Utils.dpToPx(10, context));
        layout.addView(versionTextView);

        // カテゴリごとにオプションを分類
        Map<LimeOptions.OptionCategory, List<LimeOptions.Option>> categorizedOptions = new LinkedHashMap<>();
        List<LimeOptions.OptionCategory> categoryOrder = Arrays.asList(
                LimeOptions.OptionCategory.GENERAL,
                LimeOptions.OptionCategory.NOTIFICATIONS,
                LimeOptions.OptionCategory.CHAT,
                LimeOptions.OptionCategory.Ad,
                LimeOptions.OptionCategory.CALL,
                LimeOptions.OptionCategory.Theme,
                LimeOptions.OptionCategory.OTHER
        );

        // カテゴリ初期化
        for (LimeOptions.OptionCategory category : categoryOrder) {
            categorizedOptions.put(category, new ArrayList<>());
        }

        // オプション分類
        for (LimeOptions.Option option : options) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                categorizedOptions
                        .computeIfAbsent(option.category, k -> new ArrayList<>())
                        .add(option);
            }
        }

        // カテゴリ表示
        for (Map.Entry<LimeOptions.OptionCategory, List<LimeOptions.Option>> entry : categorizedOptions.entrySet()) {
            LimeOptions.OptionCategory category = entry.getKey();
            List<LimeOptions.Option> optionsInCategory = entry.getValue();

            TextView categoryTitle = new TextView(context);
            categoryTitle.setText(category.getName(context));
            categoryTitle.setTextSize(18);
            categoryTitle.setPadding(0, Utils.dpToPx(10, context), 0, Utils.dpToPx(5, context));
            categoryTitle.setTextColor(Color.WHITE);
            categoryTitle.setClickable(true);
            categoryTitle.setOnClickListener(v -> {
                LinearLayout optionsLayout = createOptionsLayout(context, optionsInCategory, docPrefs, moduleContext, loadPackageParam);
                showView((ViewGroup) layout.getParent(), optionsLayout);
            });

            layout.addView(categoryTitle);
        }

        // 追加ボタン
        addAdditionalButtons(context, layout, docPrefs, moduleContext);

        // 保存ボタン
        Button saveButton = new Button(context);
        saveButton.setText(moduleContext.getString(R.string.Restart));
        saveButton.setTextColor(Color.WHITE);
        saveButton.setBackgroundColor(Color.DKGRAY);
        saveButton.setOnClickListener(v -> {
            boolean saveSuccess = true;

            try {
                for (LimeOptions.Option option : limeOptions.options) {
                    docPrefs.saveSetting(option.name, String.valueOf(option.checked));
                }
            } catch (IOException e) {
                saveSuccess = false;
                XposedBridge.log("Lime: Save failed: " + e.getMessage());
            }

            if (!saveSuccess) {
                Toast.makeText(context, context.getString(R.string.save_failed), Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(context.getApplicationContext(), context.getString(R.string.restarting), Toast.LENGTH_SHORT).show();
                context.startActivity(new Intent().setClassName(Constants.PACKAGE_NAME, "jp.naver.line.android.activity.SplashActivity"));
                Process.killProcess(Process.myPid());
            }
        });
        layout.addView(saveButton);

        // 戻るボタン
        Button hideButton = new Button(context);
        hideButton.setText(moduleContext.getString(R.string.back));
        hideButton.setTextColor(Color.WHITE);
        hideButton.setBackgroundColor(Color.DKGRAY);
        hideButton.setOnClickListener(v -> {
            showView((ViewGroup) layout.getParent(), createButtonLayout(context, docPrefs, moduleContext, loadPackageParam, loadPackageParam));
        });
        layout.addView(hideButton);

        scrollView.addView(layout);
        return scrollView;
    }

    private LinearLayout createOptionsLayout(Context context, List<LimeOptions.Option> options, DocumentPreferences docPrefs, Context moduleContext, XC_LoadPackage.LoadPackageParam loadPackageParam) {
        LinearLayout layout = new LinearLayout(context);
        layout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(Utils.dpToPx(20, context), Utils.dpToPx(20, context), Utils.dpToPx(20, context), Utils.dpToPx(20, context));
        layout.setBackgroundColor(Color.BLACK);
        layout.setClickable(true);
        layout.setFocusable(true);

        String LIMEs_versionName = BuildConfig.VERSION_NAME;
        TextView versionTextView = new TextView(context);
        versionTextView.setText("LIMEs" + " (" + LIMEs_versionName + ")");
        versionTextView.setTextSize(16);
        versionTextView.setTextColor(Color.WHITE);
        versionTextView.setGravity(Gravity.CENTER);
        versionTextView.setPadding(0, Utils.dpToPx(10, context), 0, Utils.dpToPx(10, context));
        layout.addView(versionTextView);

        Switch switchRedirectWebView = null;
        Switch photoAddNotificationView = null;
        Switch ReadCheckerView = null;
        Switch preventUnsendMessageView = null;
        Switch DarkModeView = null;
        List<Switch> webViewChildSwitches = new ArrayList<>();
        List<Switch> photoNotificationChildSwitches = new ArrayList<>();
        List<Switch> ReadCheckerSwitches = new ArrayList<>();
        List<Switch> preventUnsendMessageSwitches = new ArrayList<>();
        List<Switch> DarkModeSwitches = new ArrayList<>();

        for (LimeOptions.Option option : options) {
            Switch switchView = new Switch(context);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.topMargin = Utils.dpToPx(10, context);
            switchView.setLayoutParams(params);
            switchView.setText(option.id);
            switchView.setTextColor(Color.WHITE);
            switchView.setChecked(option.checked);

            switchView.setOnCheckedChangeListener((buttonView, isChecked) -> {
                option.checked = isChecked;
                try {
                    docPrefs.saveSetting(option.name, String.valueOf(isChecked));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            switch (option.name) {
                case "redirect_webview":
                    switchRedirectWebView = switchView;
                    switchRedirectWebView.setOnCheckedChangeListener((buttonView, isChecked) -> {
                        for (Switch child : webViewChildSwitches) {
                            child.setEnabled(isChecked);
                            if (!isChecked) {
                                child.setChecked(false);
                                // 子スイッチの状態を保存
                                String childName = getOptionNameFromSwitch(child, options);
                                if (childName != null) {
                                    try {
                                        docPrefs.saveSetting(childName, "false");
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                }
                            }
                        }
                        // 親スイッチの状態を保存
                        try {
                            docPrefs.saveSetting("redirect_webview", String.valueOf(isChecked));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                    break;

                case "PhotoAddNotification":
                    photoAddNotificationView = switchView;
                    photoAddNotificationView.setOnCheckedChangeListener((buttonView, isChecked) -> {
                        for (Switch child : photoNotificationChildSwitches) {
                            child.setEnabled(isChecked);
                            if (!isChecked) {
                                child.setChecked(false);
                                String childName = getOptionNameFromSwitch(child, options);
                                if (childName != null) {
                                    try {
                                        docPrefs.saveSetting(childName, "false");
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                }
                            }
                        }
                        try {
                            docPrefs.saveSetting("PhotoAddNotification", String.valueOf(isChecked));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                    break;

                case "ReadChecker":
                    ReadCheckerView = switchView;
                    ReadCheckerView.setOnCheckedChangeListener((buttonView, isChecked) -> {
                        for (Switch child : ReadCheckerSwitches) {
                            child.setEnabled(isChecked);
                            if (!isChecked) {
                                child.setChecked(false);
                                String childName = getOptionNameFromSwitch(child, options);
                                if (childName != null) {
                                    try {
                                        docPrefs.saveSetting(childName, "false");
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                }
                            }
                        }
                        try {
                            docPrefs.saveSetting("ReadChecker", String.valueOf(isChecked));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                    break;

                case "prevent_unsend_message":
                    preventUnsendMessageView = switchView;
                    preventUnsendMessageView.setOnCheckedChangeListener((buttonView, isChecked) -> {
                        for (Switch child : preventUnsendMessageSwitches) {
                            child.setEnabled(isChecked);
                            if (!isChecked) {
                                child.setChecked(false);
                                String childName = getOptionNameFromSwitch(child, options);
                                if (childName != null) {
                                    try {
                                        docPrefs.saveSetting(childName, "false");
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                }
                            }
                        }
                        try {
                            docPrefs.saveSetting("prevent_unsend_message", String.valueOf(isChecked));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                    break;

                case "DarkColor":
                    DarkModeView = switchView;
                    DarkModeView.setOnCheckedChangeListener((buttonView, isChecked) -> {
                        for (Switch child : DarkModeSwitches) {
                            child.setEnabled(isChecked);
                            if (!isChecked) {
                                child.setChecked(false);
                                String childName = getOptionNameFromSwitch(child, options);
                                if (childName != null) {
                                    try {
                                        docPrefs.saveSetting(childName, "false");
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                }
                            }
                        }
                        try {
                            docPrefs.saveSetting("DarkColor", String.valueOf(isChecked));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                    break;

                case "open_in_browser":
                    webViewChildSwitches.add(switchView);
                    switchView.setEnabled(switchRedirectWebView != null && switchRedirectWebView.isChecked());
                    break;

                case "hide_canceled_message":
                    preventUnsendMessageSwitches.add(switchView);
                    switchView.setEnabled(preventUnsendMessageView != null && preventUnsendMessageView.isChecked());
                    break;

                case "GroupNotification":
                case "CansellNotification":
                case "AddCopyAction":
                case "DisableSilentMessage":
                    photoNotificationChildSwitches.add(switchView);
                    switchView.setEnabled(photoAddNotificationView != null && photoAddNotificationView.isChecked());
                    break;

                case "MySendMessage":
                case "ReadCheckerChatdataDelete":
                    ReadCheckerSwitches.add(switchView);
                    switchView.setEnabled(ReadCheckerView != null && ReadCheckerView.isChecked());
                    break;

                case "DarkModSync":
                    DarkModeSwitches.add(switchView);
                    switchView.setEnabled(DarkModeView != null && DarkModeView.isChecked());
                    break;
            }

            layout.addView(switchView);
        }

        Button saveButton = new Button(context);
        saveButton.setText(moduleContext.getString(R.string.Restart));
        saveButton.setTextColor(Color.WHITE);
        saveButton.setBackgroundColor(Color.DKGRAY);
        saveButton.setOnClickListener(v -> {
            Toast.makeText(context, context.getString(R.string.restarting), Toast.LENGTH_SHORT).show();
            context.startActivity(new Intent().setClassName(Constants.PACKAGE_NAME, "jp.naver.line.android.activity.SplashActivity"));
            Process.killProcess(Process.myPid());
        });
        layout.addView(saveButton);

        // 戻るボタン
        Button backButton = new Button(context);
        backButton.setText(moduleContext.getString(R.string.back));
        backButton.setTextColor(Color.WHITE);
        backButton.setBackgroundColor(Color.DKGRAY);
        backButton.setOnClickListener(v -> {
            ScrollView categoryLayout = createCategoryListLayout(context, Arrays.asList(limeOptions.options), docPrefs, moduleContext, loadPackageParam);
            showView((ViewGroup) layout.getParent(), categoryLayout);
        });
        layout.addView(backButton);

        // スクロールビューでラップ
        ScrollView scrollView = new ScrollView(context);
        scrollView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        scrollView.addView(layout);

        return layout;
    }

    private String getOptionNameFromSwitch(Switch switchView, List<LimeOptions.Option> options) {
        try {
            int switchTextId = Integer.parseInt(switchView.getText().toString());

            for (LimeOptions.Option option : options) {
                if (option.id == switchTextId) {
                    return option.name;
                }
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        return null;
    }
    private View createButtonLayout(Context context, DocumentPreferences docPrefs, Context moduleContext, XC_LoadPackage.LoadPackageParam loadPackageParam, XC_LoadPackage.LoadPackageParam viewGroup) {
        PackageManager pm = context.getPackageManager();
        String versionName = "";
        try {
            versionName = pm.getPackageInfo(loadPackageParam.packageName, 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        FrameLayout rootLayout = new FrameLayout(context);
        rootLayout.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));


        Button button = new Button(context);
        button.setText(R.string.app_name);

        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.gravity = Gravity.TOP | Gravity.END;
        layoutParams.rightMargin = Utils.dpToPx(10, context);


        int statusBarHeight = getStatusBarHeight(context);

        String versionNameStr = String.valueOf(versionName);
        String majorVersionStr = versionNameStr.split("\\.")[0];
        int versionNameInt = Integer.parseInt(majorVersionStr);

        if (versionNameInt >= 15) {
            layoutParams.topMargin = statusBarHeight;
        } else {
            layoutParams.topMargin = Utils.dpToPx(5, context);
        }
        button.setLayoutParams(layoutParams);

        button.setOnClickListener(view -> {

            ScrollView categoryLayout = createCategoryListLayout(context, Arrays.asList(limeOptions.options), docPrefs, moduleContext, loadPackageParam);
            showView(rootLayout, categoryLayout);
        });


        rootLayout.addView(button);

        return rootLayout;
    }

    private void addAdditionalButtons(Context context, LinearLayout layout, DocumentPreferences docPrefs, Context moduleContext) {
        // 復元ボタン
        Button restoreButton = new Button(context);
        restoreButton.setText(moduleContext.getResources().getString(R.string.Restore));
        restoreButton.setOnClickListener(v -> showFilePickerChatHistory(context, moduleContext));
        layout.addView(restoreButton);

        // チャットリスト復元ボタン
        Button restoreChatListButton = new Button(context);
        restoreChatListButton.setText(moduleContext.getResources().getString(R.string.restoreChatListButton));
        restoreChatListButton.setOnClickListener(v -> showFilePickerChatlist(context, moduleContext));
        layout.addView(restoreChatListButton);

        // バックアップボタン
        Button backupButton = new Button(context);
        backupButton.setText(moduleContext.getResources().getString(R.string.Back_Up));
        backupButton.setOnClickListener(v -> backupChatHistory(context, moduleContext));
        layout.addView(backupButton);

        // トーク画像バックアップボタン
        Button backupfolderButton = new Button(context);
        backupfolderButton.setText(moduleContext.getResources().getString(R.string.Talk_Picture_Back_up));
        backupfolderButton.setOnClickListener(v -> backupChatsFolder(context, moduleContext));
        layout.addView(backupfolderButton);

        // 画像復元ボタン
        Button restorefolderButton = new Button(context);
        restorefolderButton.setText(moduleContext.getResources().getString(R.string.Picure_Restore));
        restorefolderButton.setOnClickListener(v -> restoreChatsFolder(context, moduleContext));
        layout.addView(restorefolderButton);

        Button GetMidIdButton = new Button(context);
        GetMidIdButton.setText(moduleContext.getResources().getString(R.string.GetMidIdButton));
        GetMidIdButton.setOnClickListener(v -> GetMidId(context, moduleContext));
        layout.addView(GetMidIdButton);

        // グループミュートボタン（条件付き）
        if (limeOptions.MuteGroup.checked) {
            Button muteGroupsButton = new Button(context);
            muteGroupsButton.setText(moduleContext.getResources().getString(R.string.Mute_Group));
            muteGroupsButton.setOnClickListener(v -> MuteGroups_Button(context, moduleContext));
            layout.addView(muteGroupsButton);
        }

        // 未読維持設定ボタン
        Button keepUnreadButton = new Button(context);
        keepUnreadButton.setText(moduleContext.getResources().getString(R.string.edit_margin_settings));
        keepUnreadButton.setOnClickListener(v -> KeepUnread_Button(context, moduleContext));
        layout.addView(keepUnreadButton);

        // 取消メッセージボタン（条件付き）
        if (limeOptions.preventUnsendMessage.checked) {
            Button canceledMessageButton = new Button(context);
            canceledMessageButton.setText(moduleContext.getResources().getString(R.string.canceled_message));
            canceledMessageButton.setOnClickListener(v -> Cancel_Message_Button(context, moduleContext));
            layout.addView(canceledMessageButton);
        }

        // 通知キャンセルボタン（条件付き）
        if (limeOptions.CansellNotification.checked) {
            Button cansellNotificationButton = new Button(context);
            cansellNotificationButton.setText(moduleContext.getResources().getString(R.string.CansellNotification));
            cansellNotificationButton.setOnClickListener(v -> CansellNotification(context, moduleContext));
            layout.addView(cansellNotificationButton);
        }

        // ブロックチェックボタン（条件付き）
        if (limeOptions.BlockCheck.checked) {
            Button blockCheckButton = new Button(context);
            blockCheckButton.setText(moduleContext.getResources().getString(R.string.BlockCheck));
            blockCheckButton.setOnClickListener(v -> Blocklist(context, moduleContext));
            layout.addView(blockCheckButton);
        }

        // 取消メッセージ非表示ボタン（条件付き）
        if (limeOptions.preventUnsendMessage.checked) {
            Button hideCanceledMessageButton = new Button(context);
            hideCanceledMessageButton.setText(moduleContext.getResources().getString(R.string.hide_canceled_message));
            hideCanceledMessageButton.setOnClickListener(v -> {
                new AlertDialog.Builder(context)
                        .setTitle(moduleContext.getResources().getString(R.string.HideSetting))
                        .setMessage(moduleContext.getResources().getString(R.string.HideSetting_selection))
                        .setPositiveButton(moduleContext.getResources().getString(R.string.Hide), (dialog, which) -> {
                            updateMessagesVisibility(context, true, moduleContext);
                        })
                        .setNegativeButton(moduleContext.getResources().getString(R.string.Show), (dialog, which) -> {
                            updateMessagesVisibility(context, false, moduleContext);
                        })
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .show();
            });
            layout.addView(hideCanceledMessageButton);
        }

        // ピン留めリストボタン（条件付き）
        if (limeOptions.PinList.checked) {
            Button pinListButton = new Button(context);
            pinListButton.setText(moduleContext.getResources().getString(R.string.PinList));
            pinListButton.setOnClickListener(v -> PinListButton(context, moduleContext));
            layout.addView(pinListButton);
        }

        // リクエスト修正ボタン（DocumentPreferencesを使用）
        Button modifyRequestButton = new Button(context);
        modifyRequestButton.setText(R.string.modify_request);
        modifyRequestButton.setOnClickListener(v -> {
            try {
                String encodedJs = docPrefs.getSetting("encoded_js_modify_request", "");
                showModifyDialog(
                        context,
                        moduleContext.getString(R.string.modify_request),
                        encodedJs,
                        (content) -> {
                            try {
                                docPrefs.saveSetting("encoded_js_modify_request",
                                        Base64.encodeToString(content.getBytes(), Base64.NO_WRAP));
                            } catch (IOException e) {
                                Toast.makeText(context, "Failed to save request modification", Toast.LENGTH_SHORT).show();
                                XposedBridge.log("Save modify request error: " + e.getMessage());
                            }
                        },
                        moduleContext
                );
            } catch (Exception e) {
                Toast.makeText(context, "Error loading request settings", Toast.LENGTH_SHORT).show();
                XposedBridge.log("Load modify request error: " + e.getMessage());
            }
        });
        layout.addView(modifyRequestButton);

        // レスポンス修正ボタン（DocumentPreferencesを使用）
        Button modifyResponseButton = new Button(context);
        modifyResponseButton.setText(R.string.modify_response);
        modifyResponseButton.setOnClickListener(v -> {
            try {
                String encodedJs = docPrefs.getSetting("encoded_js_modify_response", "");
                showModifyDialog(
                        context,
                        moduleContext.getString(R.string.modify_response),
                        encodedJs,
                        (content) -> {
                            try {
                                docPrefs.saveSetting("encoded_js_modify_response",
                                        Base64.encodeToString(content.getBytes(), Base64.NO_WRAP));
                            } catch (IOException e) {
                                Toast.makeText(context, "Failed to save response modification", Toast.LENGTH_SHORT).show();
                                XposedBridge.log("Save modify response error: " + e.getMessage());
                            }
                        },
                        moduleContext
                );
            } catch (Exception e) {
                Toast.makeText(context, "Error loading response settings", Toast.LENGTH_SHORT).show();
                XposedBridge.log("Load modify response error: " + e.getMessage());
            }
        });
        layout.addView(modifyResponseButton);
    }

    // 修正ダイアログ表示用ヘルパーメソッド
    private void showModifyDialog(Context context, String title, String encodedContent,
                                  ContentSaver contentSaver, Context moduleContext) {

        String decodedContent = new String(Base64.decode(encodedContent, Base64.NO_WRAP));

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(Utils.dpToPx(20, context), Utils.dpToPx(20, context),
                Utils.dpToPx(20, context), Utils.dpToPx(20, context));

        EditText editText = new EditText(context);
        editText.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        editText.setText(decodedContent);
        layout.addView(editText);

        new AlertDialog.Builder(context)
                .setTitle(title)
                .setView(layout)
                .setPositiveButton(moduleContext.getString(R.string.positive_button), (dialog, which) -> {
                    contentSaver.saveContent(editText.getText().toString());
                })
                .setNegativeButton(moduleContext.getString(R.string.negative_button), null)
                .show();
    }

    interface ContentSaver {
        void saveContent(String content);
    }

    private void showModifyRequestDialog(Context context, CustomPreferences customPreferences, Context moduleContext) {
        final String script = new String(Base64.decode(customPreferences.getSetting("encoded_js_modify_request", ""), Base64.NO_WRAP));
        LinearLayout layoutModifyRequest = new LinearLayout(context);
        layoutModifyRequest.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));
        layoutModifyRequest.setOrientation(LinearLayout.VERTICAL);
        layoutModifyRequest.setPadding(Utils.dpToPx(20, context), Utils.dpToPx(20, context), Utils.dpToPx(20, context), Utils.dpToPx(20, context));

        EditText editText = new EditText(context);
        editText.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        editText.setTypeface(Typeface.MONOSPACE);
        editText.setInputType(InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_FLAG_MULTI_LINE |
                InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS |
                InputType.TYPE_TEXT_FLAG_IME_MULTI_LINE);
        editText.setMovementMethod(new ScrollingMovementMethod());
        editText.setTextIsSelectable(true);
        editText.setHorizontallyScrolling(true);
        editText.setVerticalScrollBarEnabled(true);
        editText.setHorizontalScrollBarEnabled(true);
        editText.setText(script);

        layoutModifyRequest.addView(editText);

        LinearLayout buttonLayout = new LinearLayout(context);
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        buttonParams.topMargin = Utils.dpToPx(10, context);
        buttonLayout.setLayoutParams(buttonParams);
        buttonLayout.setOrientation(LinearLayout.HORIZONTAL);

        Button copyButton = new Button(context);
        copyButton.setText(R.string.button_copy);
        copyButton.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("", editText.getText().toString());
            clipboard.setPrimaryClip(clip);
        });
        buttonLayout.addView(copyButton);

        Button pasteButton = new Button(context);
        pasteButton.setText(R.string.button_paste);
        pasteButton.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null && clipboard.hasPrimaryClip()) {
                ClipData clip = clipboard.getPrimaryClip();
                if (clip != null && clip.getItemCount() > 0) {
                    CharSequence pasteData = clip.getItemAt(0).getText();
                    editText.setText(pasteData);
                }
            }
        });
        buttonLayout.addView(pasteButton);

        layoutModifyRequest.addView(buttonLayout);
        ScrollView scrollView = new ScrollView(context);
        scrollView.addView(layoutModifyRequest);
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(R.string.modify_request)
                .setView(scrollView)
                .setPositiveButton(R.string.positive_button, (dialog, which) -> {
                    String code = editText.getText().toString();
                    if (!code.equals(script)) {
                        customPreferences.saveSetting("encoded_js_modify_request", Base64.encodeToString(code.getBytes(), Base64.NO_WRAP));
                        Toast.makeText(context.getApplicationContext(), context.getString(R.string.restarting), Toast.LENGTH_SHORT).show();
                        Process.killProcess(Process.myPid());
                        context.startActivity(new Intent().setClassName(Constants.PACKAGE_NAME, "jp.naver.line.android.activity.SplashActivity"));
                    }
                })
                .setNegativeButton(R.string.negative_button, null)
                .setOnDismissListener(dialog -> editText.setText(script));

        AlertDialog dialog = builder.create();
        dialog.show();
    }


    private void showModifyResponseDialog(Context context, CustomPreferences customPreferences, Context moduleContext) {
        final String script = new String(Base64.decode(customPreferences.getSetting("encoded_js_modify_response", ""), Base64.NO_WRAP));
        LinearLayout layoutModifyResponse = new LinearLayout(context);
        layoutModifyResponse.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));
        layoutModifyResponse.setOrientation(LinearLayout.VERTICAL);
        layoutModifyResponse.setPadding(Utils.dpToPx(20, context), Utils.dpToPx(20, context), Utils.dpToPx(20, context), Utils.dpToPx(20, context));

        EditText editText = new EditText(context);
        editText.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        editText.setTypeface(Typeface.MONOSPACE);
        editText.setInputType(InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_FLAG_MULTI_LINE |
                InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS |
                InputType.TYPE_TEXT_FLAG_IME_MULTI_LINE);
        editText.setMovementMethod(new ScrollingMovementMethod());
        editText.setTextIsSelectable(true);
        editText.setHorizontallyScrolling(true);
        editText.setVerticalScrollBarEnabled(true);
        editText.setHorizontalScrollBarEnabled(true);
        editText.setText(script);

        layoutModifyResponse.addView(editText);

        LinearLayout buttonLayout = new LinearLayout(context);
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        buttonParams.topMargin = Utils.dpToPx(10, context);
        buttonLayout.setLayoutParams(buttonParams);
        buttonLayout.setOrientation(LinearLayout.HORIZONTAL);

        Button copyButton = new Button(context);
        copyButton.setText(R.string.button_copy);
        copyButton.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("", editText.getText().toString());
            clipboard.setPrimaryClip(clip);
        });
        buttonLayout.addView(copyButton);

        Button pasteButton = new Button(context);
        pasteButton.setText(R.string.button_paste);
        pasteButton.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null && clipboard.hasPrimaryClip()) {
                ClipData clip = clipboard.getPrimaryClip();
                if (clip != null && clip.getItemCount() > 0) {
                    CharSequence pasteData = clip.getItemAt(0).getText();
                    editText.setText(pasteData);
                }
            }
        });
        buttonLayout.addView(pasteButton);

        layoutModifyResponse.addView(buttonLayout);
        ScrollView scrollView = new ScrollView(context);
        scrollView.addView(layoutModifyResponse);
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(R.string.modify_response)
                .setView(scrollView)
                .setPositiveButton(R.string.positive_button, (dialog, which) -> {
                    String code = editText.getText().toString();
                    if (!code.equals(script)) {
                        customPreferences.saveSetting("encoded_js_modify_response", Base64.encodeToString(code.getBytes(), Base64.NO_WRAP));
                        Toast.makeText(context.getApplicationContext(), context.getString(R.string.restarting), Toast.LENGTH_SHORT).show();
                        Process.killProcess(Process.myPid());
                        context.startActivity(new Intent().setClassName(Constants.PACKAGE_NAME, "jp.naver.line.android.activity.SplashActivity"));
                    }
                })
                .setNegativeButton(R.string.negative_button, null)
                .setOnDismissListener(dialog -> editText.setText(script));

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void CansellNotification(Context context, Context moduleContext) {
        File dir = new File(context.getFilesDir(), "LimeBackup/Setting");

        if (!dir.exists()) {

            dir = new File(Environment.getExternalStorageDirectory(), "Android/data/jp.naver.line.android/LimeBackup/Setting");

            if (!dir.exists()) {

                dir = new File(context.getFilesDir(), "LimeBackup/Setting");
            }

            if (!dir.exists() && !dir.mkdirs()) {
                Toast.makeText(context, "Failed to create directory", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        File file = new File(dir, "Notification_Setting.txt");

        List<String> existingPairs = new ArrayList<>();
        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    existingPairs.add(line.trim());
                }
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(context, "ファイルの読み取りに失敗しました", Toast.LENGTH_SHORT).show();
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getString(R.string.NotiFication_Setting));

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);

        final EditText groupNameInput = new EditText(context);
        groupNameInput.setHint(context.getString(R.string.GroupName));
        layout.addView(groupNameInput);

        final EditText userNameInput = new EditText(context);
        userNameInput.setHint(context.getString(R.string.User_name));
        layout.addView(userNameInput);

        Button addButton = new Button(context);
        addButton.setText(context.getString(R.string.Add));
        layout.addView(addButton);

        addButton.setOnClickListener(v -> {
            String groupName = groupNameInput.getText().toString();
            String userName = userNameInput.getText().toString();
            String newPair = ": " + groupName + ","+ context.getString(R.string.User_name)+": " + userName;

            if (existingPairs.contains(newPair)) {
                Toast.makeText(context, context.getString(R.string.Aleady_Pair), Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                FileWriter writer = new FileWriter(file, true); // trueを指定して追記モードにする
                writer.write(newPair + "\n");
                writer.close();
                existingPairs.add(newPair);
                Toast.makeText(context, context.getString(R.string.Add_Pair), Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(context, context.getString(R.string.Add_Error_Pair), Toast.LENGTH_SHORT).show();
            }
        });

        builder.setView(layout);

        builder.setNeutralButton( context.getString(R.string.Registering_Pair), (dialog, which) -> {
            AlertDialog.Builder pairsBuilder = new AlertDialog.Builder(context);
            pairsBuilder.setTitle(context.getString(R.string.Registering_Pair));

            LinearLayout pairsLayout = new LinearLayout(context);
            pairsLayout.setOrientation(LinearLayout.VERTICAL);

            for (String pair : existingPairs) {

                LinearLayout pairLayout = new LinearLayout(context);
                pairLayout.setOrientation(LinearLayout.HORIZONTAL);


                TextView pairTextView = new TextView(context);
                pairTextView.setText(pair);
                pairLayout.addView(pairTextView);

                Button deleteButton = new Button(context);
                deleteButton.setText(context.getString(R.string.Delete_Pair));
                deleteButton.setOnClickListener(v -> {
                    existingPairs.remove(pair);

                    try {
                        FileWriter writer = new FileWriter(file);
                        for (String remainingPair : existingPairs) {
                            writer.write(remainingPair + "\n");
                        }
                        writer.close();
                        Toast.makeText(context,  context.getString(R.string.Deleted_Pair), Toast.LENGTH_SHORT).show();
                        pairsBuilder.setMessage( context.getString(R.string.Deleted_Pair));
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(context,  context.getString(R.string.Error_Deleted_Pair), Toast.LENGTH_SHORT).show();
                    }
                    pairsBuilder.setMessage(getCurrentPairsMessage(existingPairs,context));
                });
                pairLayout.addView(deleteButton);
                pairsLayout.addView(pairLayout);
            }

            pairsBuilder.setView(pairsLayout);
            pairsBuilder.setPositiveButton( context.getString(R.string.Close), null);
            pairsBuilder.show();
        });


        builder.setNegativeButton( context.getString(R.string.Cansel), (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private String getCurrentPairsMessage(List<String> pairs,Context context) {
        StringBuilder message = new StringBuilder();
        if (pairs.isEmpty()) {
            message.append( context.getString(R.string.No_Pair));
        } else {
            for (String pair : pairs) {
                message.append(pair).append("\n");
            }
        }
        return message.toString();
    }


    private void Cancel_Message_Button(Context context, Context moduleContext) {

        File dir = new File(context.getFilesDir(), "LimeBackup/Setting");


        if (!dir.exists() && !dir.mkdirs()) {
            Toast.makeText(context, "Failed to create directory", Toast.LENGTH_SHORT).show();
            return;
        }


        File file = new File(dir, "canceled_message.txt");

        if (!file.exists()) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                String defaultMessage = moduleContext.getResources().getString(R.string.canceled_message_txt);
                writer.write(defaultMessage);
            } catch (IOException e) {
                Toast.makeText(context, "Failed to create file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }
        }

        StringBuilder fileContent = new StringBuilder();
        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    fileContent.append(line).append("\n");
                }
            } catch (IOException e) {
                Toast.makeText(context, "Failed to read file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }
        }

        final EditText editText = new EditText(context);
        editText.setText(fileContent.toString().trim());
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        editText.setMinLines(10);
        editText.setGravity(Gravity.TOP);

        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        buttonParams.setMargins(16, 16, 16, 16);

        Button saveButton = new Button(context);
        saveButton.setText(moduleContext.getResources().getString(R.string.options_title)); // リソースからテキストを取得
        saveButton.setLayoutParams(buttonParams);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                    writer.write(editText.getText().toString());
                    Toast.makeText(context, "Saved successfully", Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    Toast.makeText(context, "Failed to save file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(editText);
        layout.addView(saveButton);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(moduleContext.getResources().getString(R.string.canceled_message)); // リソースからタイトルを取得
        builder.setView(layout);
        builder.setNegativeButton(moduleContext.getResources().getString(R.string.cancel), null); // リソースからキャンセルボタンのテキストを取得
        builder.show();
    }
    public int getStatusBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }
    private void KeepUnread_Button(Context context, Context moduleContext) {
        File dir = new File(context.getFilesDir(), "LimeBackup/Setting");

        File file = new File(dir, "margin_settings.txt");

        float keep_unread_horizontalMarginFactor = 0.5f;
        int keep_unread_verticalMarginDp = 50;
        float read_button_horizontalMarginFactor = 0.6f;
        int read_button_verticalMarginDp = 60;
        float read_checker_horizontalMarginFactor = 0.5f;
        int read_checker_verticalMarginDp = 60;
        float keep_unread_size = 80.0f;
        float chat_unread_size = 30.0f;
        float chat_read_check_size = 80.0f;

        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split("=", 2);
                    if (parts.length == 2) {
                        switch (parts[0].trim()) {
                            case "keep_unread_horizontalMarginFactor":
                                keep_unread_horizontalMarginFactor = Float.parseFloat(parts[1].trim());
                                break;
                            case "keep_unread_verticalMarginDp":
                                keep_unread_verticalMarginDp = Integer.parseInt(parts[1].trim());
                                break;
                            case "Read_buttom_Chat_horizontalMarginFactor":
                                read_button_horizontalMarginFactor = Float.parseFloat(parts[1].trim());
                                break;
                            case "Read_buttom_Chat_verticalMarginDp":
                                read_button_verticalMarginDp = Integer.parseInt(parts[1].trim());
                                break;
                            case "Read_checker_horizontalMarginFactor":
                                read_checker_horizontalMarginFactor = Float.parseFloat(parts[1].trim());
                                break;
                            case "Read_checker_verticalMarginDp":
                                read_checker_verticalMarginDp = Integer.parseInt(parts[1].trim());
                                break;
                            case "keep_unread_size":
                                keep_unread_size = Float.parseFloat(parts[1].trim());
                                break;
                            case "chat_unread_size":
                                chat_unread_size = Float.parseFloat(parts[1].trim());
                                break; // 追加
                            case "chat_read_check_size":
                                chat_read_check_size = Float.parseFloat(parts[1].trim());
                                break; // 追加
                        }
                    }
                }
            } catch (IOException | NumberFormatException ignored) {

            }
        } else {

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                String defaultSettings = "keep_unread_horizontalMarginFactor=0.5\n" +
                        "keep_unread_verticalMarginDp=15\n" +
                        "Read_buttom_Chat_horizontalMarginFactor=0.6\n" +
                        "Read_buttom_Chat_verticalMarginDp=60\n" +
                        "Read_checker_horizontalMarginFactor=0.5\n" +
                        "Read_checker_verticalMarginDp=60\n" +
                        "keep_unread_size=60\n" +
                        "chat_unread_size=30\n" +
                        "chat_read_check_size=60\n";
                writer.write(defaultSettings);
                writer.flush(); // 追加
            } catch (IOException ignored) {

                return;
            }
        }

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(16, 16, 16, 16);

        TextView keepUnreadSizeLabel = new TextView(context);
        keepUnreadSizeLabel.setText(moduleContext.getResources().getString(R.string.keep_unread_size));
        keepUnreadSizeLabel.setLayoutParams(layoutParams);

        final EditText keepUnreadSizeInput = new EditText(context);
        keepUnreadSizeInput.setText(String.valueOf(keep_unread_size));
        keepUnreadSizeInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        keepUnreadSizeInput.setLayoutParams(layoutParams);

        TextView horizontalLabel = new TextView(context);
        horizontalLabel.setText(moduleContext.getResources().getString(R.string.keep_unread_horizontalMarginFactor));
        horizontalLabel.setLayoutParams(layoutParams);

        final EditText horizontalInput = new EditText(context);
        horizontalInput.setText(String.valueOf(keep_unread_horizontalMarginFactor));
        horizontalInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        horizontalInput.setLayoutParams(layoutParams);

        TextView verticalLabel = new TextView(context);
        verticalLabel.setText(moduleContext.getResources().getString(R.string.keep_unread_vertical));
        verticalLabel.setLayoutParams(layoutParams);

        final EditText verticalInput = new EditText(context);
        verticalInput.setText(String.valueOf(keep_unread_verticalMarginDp));
        verticalInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        verticalInput.setLayoutParams(layoutParams);

        TextView ChatUnreadLabel = new TextView(context);
        ChatUnreadLabel.setText(moduleContext.getResources().getString(R.string.chat_unread_size));
        ChatUnreadLabel.setLayoutParams(layoutParams);

        final EditText ChatUnreadSizeInput = new EditText(context);
        ChatUnreadSizeInput.setText(String.valueOf(chat_unread_size));
        ChatUnreadSizeInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        ChatUnreadSizeInput.setLayoutParams(layoutParams);

        TextView ChatReadCheckSizeLabel = new TextView(context);
        ChatReadCheckSizeLabel.setText(moduleContext.getResources().getString(R.string.chat_read_check_size));
        ChatReadCheckSizeLabel.setLayoutParams(layoutParams);

        final EditText ChatReadCheckSizeInput = new EditText(context);
        ChatReadCheckSizeInput.setText(String.valueOf(chat_read_check_size));
        ChatReadCheckSizeInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        ChatReadCheckSizeInput.setLayoutParams(layoutParams);

        TextView readButtonHorizontalLabel = new TextView(context);
        readButtonHorizontalLabel.setText(moduleContext.getResources().getString(R.string.Read_buttom_Chat_horizontalMarginFactor));
        readButtonHorizontalLabel.setLayoutParams(layoutParams);

        final EditText readButtonHorizontalInput = new EditText(context);
        readButtonHorizontalInput.setText(String.valueOf(read_button_horizontalMarginFactor));
        readButtonHorizontalInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        readButtonHorizontalInput.setLayoutParams(layoutParams);

        TextView readButtonVerticalLabel = new TextView(context);
        readButtonVerticalLabel.setText(moduleContext.getResources().getString(R.string.Read_buttom_Chat_verticalMarginDp));
        readButtonVerticalLabel.setLayoutParams(layoutParams);

        final EditText readButtonVerticalInput = new EditText(context);
        readButtonVerticalInput.setText(String.valueOf(read_button_verticalMarginDp));
        readButtonVerticalInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        readButtonVerticalInput.setLayoutParams(layoutParams);

        TextView readCheckerHorizontalLabel = new TextView(context);
        readCheckerHorizontalLabel.setText(moduleContext.getResources().getString(R.string.Read_checker_horizontalMarginFactor));
        readCheckerHorizontalLabel.setLayoutParams(layoutParams);

        final EditText readCheckerHorizontalInput = new EditText(context);
        readCheckerHorizontalInput.setText(String.valueOf(read_checker_horizontalMarginFactor));
        readCheckerHorizontalInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        readCheckerHorizontalInput.setLayoutParams(layoutParams);

        TextView readCheckerVerticalLabel = new TextView(context);
        readCheckerVerticalLabel.setText(moduleContext.getResources().getString(R.string.Read_checker_verticalMarginDp));
        readCheckerVerticalLabel.setLayoutParams(layoutParams);

        final EditText readCheckerVerticalInput = new EditText(context);
        readCheckerVerticalInput.setText(String.valueOf(read_checker_verticalMarginDp));
        readCheckerVerticalInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        readCheckerVerticalInput.setLayoutParams(layoutParams);

        Button saveButton = new Button(context);
        saveButton.setText("Save");
        saveButton.setLayoutParams(layoutParams);
        saveButton.setOnClickListener(v -> {
            try {

                String keepUnreadSizeStr = keepUnreadSizeInput.getText().toString().trim();
                String horizontalMarginStr = horizontalInput.getText().toString().trim();
                String verticalMarginStr = verticalInput.getText().toString().trim();
                String chatUnreadSizeStr = ChatUnreadSizeInput.getText().toString().trim();
                String chatReadCheckSizeStr = ChatReadCheckSizeInput.getText().toString().trim();
                String readButtonHorizontalMarginStr = readButtonHorizontalInput.getText().toString().trim();
                String readButtonVerticalMarginStr = readButtonVerticalInput.getText().toString().trim();
                String readCheckerHorizontalMarginStr = readCheckerHorizontalInput.getText().toString().trim();
                String readCheckerVerticalMarginStr = readCheckerVerticalInput.getText().toString().trim();

                if (keepUnreadSizeStr.isEmpty() || horizontalMarginStr.isEmpty() || verticalMarginStr.isEmpty() ||
                        chatUnreadSizeStr.isEmpty() || chatReadCheckSizeStr.isEmpty() ||
                        readButtonHorizontalMarginStr.isEmpty() || readButtonVerticalMarginStr.isEmpty() ||
                        readCheckerHorizontalMarginStr.isEmpty() || readCheckerVerticalMarginStr.isEmpty()) {
                    Toast.makeText(context, "All fields must be filled!", Toast.LENGTH_SHORT).show();
                    return;
                }

                float newKeepUnreadSize = Float.parseFloat(keepUnreadSizeStr); // 修正
                float newKeepUnreadHorizontalMarginFactor = Float.parseFloat(horizontalMarginStr);
                int newKeepUnreadVerticalMarginDp = Integer.parseInt(verticalMarginStr);
                float newChatUnreadSize = Float.parseFloat(chatUnreadSizeStr); // 修正
                float newChatReadCheckSize = Float.parseFloat(chatReadCheckSizeStr); // 修正
                float newReadButtonHorizontalMarginFactor = Float.parseFloat(readButtonHorizontalMarginStr);
                int newReadButtonVerticalMarginDp = Integer.parseInt(readButtonVerticalMarginStr);
                float newReadCheckerHorizontalMarginFactor = Float.parseFloat(readCheckerHorizontalMarginStr);
                int newReadCheckerVerticalMarginDp = Integer.parseInt(readCheckerVerticalMarginStr);

                try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                    writer.write("keep_unread_size=" + newKeepUnreadSize + "\n");
                    writer.write("keep_unread_horizontalMarginFactor=" + newKeepUnreadHorizontalMarginFactor + "\n");
                    writer.write("keep_unread_verticalMarginDp=" + newKeepUnreadVerticalMarginDp + "\n");
                    writer.write("chat_unread_size=" + newChatUnreadSize + "\n");
                    writer.write("chat_read_check_size=" + newChatReadCheckSize + "\n");
                    writer.write("Read_buttom_Chat_horizontalMarginFactor=" + newReadButtonHorizontalMarginFactor + "\n");
                    writer.write("Read_buttom_Chat_verticalMarginDp=" + newReadButtonVerticalMarginDp + "\n");
                    writer.write("Read_checker_horizontalMarginFactor=" + newReadCheckerHorizontalMarginFactor + "\n");
                    writer.write("Read_checker_verticalMarginDp=" + newReadCheckerVerticalMarginDp + "\n");
                    writer.flush();
                    Toast.makeText(context, "Settings saved!", Toast.LENGTH_SHORT).show();
                }
            } catch (NumberFormatException ignored) {
                Toast.makeText(context, "Invalid input format! Please check your inputs.", Toast.LENGTH_SHORT).show();

            } catch (IOException ignored) {
                Toast.makeText(context, "Failed to save settings.", Toast.LENGTH_SHORT).show();
            }

        });

        Button resetButton = new Button(context);
        resetButton.setText("Reset");
        resetButton.setLayoutParams(layoutParams);
        resetButton.setOnClickListener(v -> {

            new AlertDialog.Builder(context)
                    .setMessage(moduleContext.getResources().getString(R.string.really_delete))
                    .setPositiveButton(moduleContext.getResources().getString(R.string.yes), (dialog, which) -> {
                        try {

                            if (file.exists()) {
                                if (file.delete()) {
                                    Toast.makeText(context.getApplicationContext(), moduleContext.getResources().getString(R.string.file_content_deleted), Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(context.getApplicationContext(),moduleContext.getResources().getString(R.string.file_delete_failed), Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Toast.makeText(context.getApplicationContext(), moduleContext.getResources().getString(R.string.file_not_found), Toast.LENGTH_SHORT).show();
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(context.getApplicationContext(), moduleContext.getResources().getString(R.string.file_not_found), Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton(moduleContext.getResources().getString(R.string.no), null)
                    .show();
        });


        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(keepUnreadSizeLabel);
        layout.addView(keepUnreadSizeInput);
        layout.addView(horizontalLabel);
        layout.addView(horizontalInput);
        layout.addView(verticalLabel);
        layout.addView(verticalInput);
        layout.addView(ChatUnreadLabel);
        layout.addView(ChatUnreadSizeInput);
        layout.addView(readButtonHorizontalLabel);
        layout.addView(readButtonHorizontalInput);
        layout.addView(readButtonVerticalLabel);
        layout.addView(readButtonVerticalInput);
        layout.addView(ChatReadCheckSizeLabel);
        layout.addView(ChatReadCheckSizeInput);
        layout.addView(readCheckerHorizontalLabel);
        layout.addView(readCheckerHorizontalInput);
        layout.addView(readCheckerVerticalLabel);
        layout.addView(readCheckerVerticalInput);
        layout.addView(saveButton);
        layout.addView(resetButton);
        ScrollView scrollView = new ScrollView(context);
        scrollView.addView(layout);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(moduleContext.getResources().getString(R.string.edit_margin_settings));
        builder.setView(scrollView);
        builder.setNegativeButton(moduleContext.getResources().getString(R.string.cancel), null);
        if (context instanceof Activity && !((Activity) context).isFinishing()) {
            builder.show();
        }
    }

    private void MuteGroups_Button(Context context, Context moduleContext) {
        File dir = context.getFilesDir();
        File file = new File(dir, "Notification.txt");
        StringBuilder fileContent = new StringBuilder();

        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                dir = new File(Environment.getExternalStorageDirectory(), "Android/data/jp.naver.line.android/");
            }
        }

        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    fileContent.append(line).append("\n");
                }
            } catch (IOException e) {
                Log.e("MuteGroups_Button", "Error reading file", e);
            }
        }

        final EditText editText = new EditText(context);
        editText.setText(fileContent.toString());
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        editText.setMinLines(10);
        editText.setGravity(Gravity.TOP);
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        buttonParams.setMargins(16, 16, 16, 16);
        Button saveButton = new Button(context);
        saveButton.setText("Save");
        saveButton.setLayoutParams(buttonParams);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                    writer.write(editText.getText().toString());
                } catch (IOException ignored) {
                }
            }
        });

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(editText);
        layout.addView(saveButton);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        builder.setTitle(moduleContext.getResources().getString(R.string.Mute_Group));
        builder.setView(layout);
        builder.setNegativeButton(moduleContext.getResources().getString(R.string.cancel), null);
        builder.show();
    }


    private void backupChatHistory(Context appContext, Context moduleContext) {
        File originalDbFile = appContext.getDatabasePath("naver_line");

        // 1. 内部ストレージへのバックアップ（従来通り）
        File backupDir = new File(appContext.getFilesDir(), "LimeBackup");
        if (!backupDir.exists()) {
            if (!backupDir.mkdirs()) {
                return;
            }
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String backupFileNameWithTimestamp = "naver_line_backup_" + timeStamp + ".db";
        String backupFileNameFixed = "naver_line_backup.db";
        File backupFileWithTimestamp = new File(backupDir, backupFileNameWithTimestamp);
        File backupFileFixed = new File(backupDir, backupFileNameFixed);

        try (FileChannel source = new FileInputStream(originalDbFile).getChannel()) {
            // 内部ストレージにバックアップ（従来の処理）
            try (FileChannel destinationWithTimestamp = new FileOutputStream(backupFileWithTimestamp).getChannel()) {
                destinationWithTimestamp.transferFrom(source, 0, source.size());
            }
            source.position(0);
            try (FileChannel destinationFixed = new FileOutputStream(backupFileFixed).getChannel()) {
                destinationFixed.transferFrom(source, 0, source.size());
            }

            // 2. URIが設定されていれば、そちらにもバックアップ（新規追加）
            String backupUri = loadBackupUri(appContext);
            if (backupUri != null) {
                Uri treeUri = Uri.parse(backupUri);
                DocumentFile dir = DocumentFile.fromTreeUri(appContext, treeUri);
                if (dir != null) {
                    // タイムスタンプ付きファイルをURIにコピー
                    copyFileToUri(appContext, backupFileWithTimestamp, dir, backupFileNameWithTimestamp);
                    // 固定名ファイルをURIにコピー
                    copyFileToUri(appContext, backupFileFixed, dir, backupFileNameFixed);
                }
            }

            Toast.makeText(appContext, moduleContext.getResources().getString(R.string.Talk_Back_up_Success), Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(appContext, moduleContext.getResources().getString(R.string.Talk_Back_up_Error), Toast.LENGTH_SHORT).show();
        }
    }

    private void copyFileToUri(Context context, File sourceFile, DocumentFile destinationDir, String destinationFileName) {
        try {
            DocumentFile existingFile = destinationDir.findFile(destinationFileName);
            if (existingFile != null) {
                existingFile.delete();
            }

            DocumentFile newFile = destinationDir.createFile("application/x-sqlite3", destinationFileName);
            if (newFile == null) return;

            try (InputStream in = new FileInputStream(sourceFile);
                 OutputStream out = context.getContentResolver().openOutputStream(newFile.getUri())) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = in.read(buffer)) > 0) {
                    out.write(buffer, 0, length);
                }
            }
        } catch (IOException e) {
            XposedBridge.log("Lime: Error copying DB to URI: " + e.getMessage());
        }
    }


    private void updateMessagesVisibility(Context context, boolean hide,Context moduleContext) {
        SQLiteDatabase db1 = null;
        try {
            db1 = context.openOrCreateDatabase("naver_line", Context.MODE_PRIVATE, null);

            if (hide) {
                db1.execSQL(
                        "UPDATE chat_history " +
                                "SET chat_id = '/' || chat_id " +
                                "WHERE parameter = 'LIMEsUnsend' " +
                                "AND chat_id NOT LIKE '/%'"
                );
                Toast.makeText(context, moduleContext.getResources().getString(R.string.Hiden_setting), Toast.LENGTH_SHORT).show();
            } else {
                db1.execSQL(
                        "UPDATE chat_history " +
                                "SET chat_id = LTRIM(chat_id, '/') " +
                                "WHERE parameter = 'LIMEsUnsend' " +
                                "AND chat_id LIKE '/%'"
                );
                Toast.makeText(context,  moduleContext.getResources().getString(R.string.Show_setting), Toast.LENGTH_SHORT).show();
            }
        } catch (SQLException e) {
            Log.e("DatabaseError", "Update failed: " + e.getMessage());
            Toast.makeText(context, moduleContext.getResources().getString(R.string.Setting_Error), Toast.LENGTH_SHORT).show();
        } finally {
            if (db1 != null) {
                db1.close();
            }
        }
    }


    private static class UserEntry {
        String userId;
        String userName;
        transient EditText inputView;

        UserEntry(String userId, String userName) {
            this.userId = userId;
            this.userName = userName;
        }
    }

    private void PinListButton(Context context, Context moduleContext) {
        List<UserEntry> userEntries = new ArrayList<>();
        Map<String, String> existingSettings = loadExistingSettings(context);

        try (SQLiteDatabase chatListDb = context.openOrCreateDatabase("naver_line", Context.MODE_PRIVATE, null);
             SQLiteDatabase profileDb = context.openOrCreateDatabase("contact", Context.MODE_PRIVATE, null);
             Cursor chatCursor = chatListDb.rawQuery("SELECT chat_id FROM chat WHERE is_archived = 0", null)) {

            if (chatCursor != null && chatCursor.getColumnIndex("chat_id") != -1) {
                while (chatCursor.moveToNext()) {
                    String chatId = chatCursor.getString(chatCursor.getColumnIndex("chat_id"));
                    String profileName = getProfileNameFromContacts(profileDb, chatId);
                    if ("Unknown".equals(profileName)) {
                        profileName = getGroupNameFromGroups(chatListDb, chatId);
                    }
                    userEntries.add(new UserEntry(chatId, profileName));
                }
            }

        } catch (SQLiteException e) {
           // XposedBridge.log("SQL Error: " + e.getMessage());
            Toast.makeText(context, "データ取得エラー", Toast.LENGTH_SHORT).show();
            return;
        }

        ScrollView scrollView = new ScrollView(context);
        LinearLayout mainLayout = new LinearLayout(context);
        mainLayout.setOrientation(LinearLayout.VERTICAL);

        int padding = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                16,
                context.getResources().getDisplayMetrics()
        );

        LinearLayout contentLayout = new LinearLayout(context);
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        contentLayout.setPadding(padding, padding, padding, padding);

        for (UserEntry entry : userEntries) {
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));

            TextView userNameView = new TextView(context);
            userNameView.setText(entry.userName);
            LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
            );
            textParams.setMargins(0, 0, padding, 0);
            userNameView.setLayoutParams(textParams);

            EditText inputNumber = new EditText(context);
            inputNumber.setInputType(InputType.TYPE_CLASS_NUMBER);
            inputNumber.setLayoutParams(new LinearLayout.LayoutParams(
                    (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 120, context.getResources().getDisplayMetrics()),
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));

            if (existingSettings.containsKey(entry.userId)) {
                inputNumber.setText(existingSettings.get(entry.userId));
            }

            entry.inputView = inputNumber;
            row.addView(userNameView);
            row.addView(inputNumber);
            contentLayout.addView(row);
        }

        scrollView.addView(contentLayout);
        mainLayout.addView(scrollView);

        new AlertDialog.Builder(context)
                .setTitle(moduleContext.getString(R.string.UserSet))
                .setView(mainLayout)
                .setPositiveButton(moduleContext.getString(R.string.save), (dialog, which) -> saveUserData(context, userEntries,moduleContext))
                .setNegativeButton(moduleContext.getString(R.string.cancel), null)
                .show();
    }

    private Map<String, String> loadExistingSettings(Context context) {
        Map<String, String> settingsMap = new HashMap<>();
        File settingFile = new File(
                context.getFilesDir(),
                "LimeBackup/Setting/ChatList.txt"
        );

        if (settingFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(settingFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(",", 2);
                    if (parts.length == 2) {
                        settingsMap.put(parts[0].trim(), parts[1].trim());
                    }
                }
            } catch (IOException e) {
               // XposedBridge.log("設定読み込みエラー: " + e.getMessage());
            }
        }
        return settingsMap;
    }

    private void saveUserData(Context context, List<UserEntry> entries,Context moduleContext) {
        File outputDir = new File(
                context.getFilesDir(),
                "LimeBackup/Setting"
        );

        if (!outputDir.exists() && !outputDir.mkdirs()) {
            Toast.makeText(context, "ディレクトリ作成失敗", Toast.LENGTH_SHORT).show();
            return;
        }

        File outputFile = new File(outputDir, "ChatList.txt");
        Map<String, String> newSettings = new HashMap<>();

        for (UserEntry entry : entries) {
            String value = entry.inputView.getText().toString().trim();
            if (!value.isEmpty()) {
                newSettings.put(entry.userId, value);
            }
        }

        try (FileWriter writer = new FileWriter(outputFile)) {
            for (Map.Entry<String, String> entry : newSettings.entrySet()) {
                writer.write(entry.getKey() + "," + entry.getValue() + "\n");
            }
            Toast.makeText(context, moduleContext.getString(R.string.SettingSave) + outputFile.getPath(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(context, moduleContext.getString(R.string.file_save_failed)+ e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String getProfileNameFromContacts(SQLiteDatabase db, String contactMid) {
        try (Cursor cursor = db.rawQuery("SELECT profile_name FROM contacts WHERE mid=?", new String[]{contactMid})) {
            return cursor.moveToFirst() ? cursor.getString(0) : "Unknown";
        } catch (SQLiteException e) {
           // XposedBridge.log("プロファイル取得エラー: " + e.getMessage());
            return "Unknown";
        }
    }

    private String getGroupNameFromGroups(SQLiteDatabase db, String groupId) {
        try (Cursor cursor = db.rawQuery("SELECT name FROM groups WHERE id=?", new String[]{groupId})) {
            return cursor.moveToFirst() ? cursor.getString(0) : "Unknown";
        } catch (SQLiteException e) {
           // XposedBridge.log("グループ名取得エラー: " + e.getMessage());
            return "Unknown";
        }
    }

    private void Blocklist(Context context, Context moduleContext) {
        new ProfileManager().showProfileManagement(context,moduleContext);
    }
    private static class ProfileInfo {
        final String contactMid;
        final String profileName;

        ProfileInfo(String contactMid, String profileName) {
            this.contactMid = contactMid;
            this.profileName = profileName;
        }
    }

    public class ProfileManager {
        private HiddenProfileManager hiddenManager;
        private AlertDialog currentDialog;
        private Context moduleContext;

        public void showProfileManagement(Context context, Context moduleContext) {
            this.moduleContext = moduleContext;
            hiddenManager = new HiddenProfileManager(context);

            new AsyncTask<Void, Void, List<ProfileInfo>>() {
                @Override
                protected List<ProfileInfo> doInBackground(Void... voids) {
                    return loadProfiles(context);
                }

                @Override
                protected void onPostExecute(List<ProfileInfo> profiles) {
                    showManagementDialog(context, profiles, moduleContext);
                }
            }.execute();

        }

        private List<ProfileInfo> loadProfiles(Context context) {
            Set<String> hidden = hiddenManager.getHiddenProfiles();
            List<ProfileInfo> profiles = new ArrayList<>();

            try (SQLiteDatabase blockListDb = context.openOrCreateDatabase("events", Context.MODE_PRIVATE, null);
                 SQLiteDatabase contactDb = context.openOrCreateDatabase("contact", Context.MODE_PRIVATE, null);
                 Cursor cursor = blockListDb.rawQuery(
                         "SELECT contact_mid, year, month, day FROM contact_calendar_event", null)) {

                while (cursor.moveToNext()) {
                    if (isNullDate(cursor)) {
                        String contactMid = cursor.getString(cursor.getColumnIndex("contact_mid"));
                        if (!hidden.contains(contactMid)) {
                            String profileName = getProfileName(contactDb, contactMid);
                            profiles.add(new ProfileInfo(contactMid, profileName));
                        }
                    }
                }
            } catch (Exception e) {
               // XposedBridge.log( moduleContext.getResources().getString(R.string.Block_Profile_Reload) + e.getMessage());
            }
            return profiles;
        }

        private boolean isNullDate(Cursor cursor) {
            return cursor.isNull(cursor.getColumnIndex("year")) &&
                    cursor.isNull(cursor.getColumnIndex("month")) &&
                    cursor.isNull(cursor.getColumnIndex("day"));
        }



        private void showManagementDialog(Context context, List<ProfileInfo> profiles, Context moduleContext) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context)
                    .setTitle(moduleContext.getResources().getString(R.string.Block_list))
                    .setNeutralButton(moduleContext.getResources().getString(R.string.Name_Reset), (dialog, which) -> showResetConfirmation(context, profiles,moduleContext))
                    .setPositiveButton(moduleContext.getResources().getString(R.string.Redisplay), (dialog, which) -> showRestoreDialog(context))
                    .setNegativeButton(moduleContext.getResources().getString(R.string.Close), null);

            ScrollView scrollView = new ScrollView(context);
            LinearLayout container = new LinearLayout(context);
            container.setOrientation(LinearLayout.VERTICAL);
            int padding = dpToPx(context, 16);
            container.setPadding(padding, padding, padding, padding);
            scrollView.addView(container);

            if (profiles.isEmpty()) {

                TextView emptyView = new TextView(context);
                emptyView.setText(moduleContext.getResources().getString(R.string.No_Profiles));
                emptyView.setGravity(Gravity.CENTER);
                emptyView.setTextSize(16);
                container.addView(emptyView, new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                ));
            } else {

                for (ProfileInfo profile : profiles) {
                    container.addView(createProfileItem(context, profile, container));
                }
            }

            builder.setView(scrollView);
            currentDialog = builder.show();

            Window window = currentDialog.getWindow();
            if (window != null) {
                window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, 800);
            }
        }

        private int dpToPx(Context context, int dp) {
            return (int) (dp * context.getResources().getDisplayMetrics().density);
        }
        private void showResetConfirmation(Context context, List<ProfileInfo> profiles,Context moduleContext) {
            new AlertDialog.Builder(context)
                    .setTitle(moduleContext.getResources().getString(R.string.really_delete))
                    .setMessage(moduleContext.getResources().getString(R.string.really_delete))
                    .setPositiveButton(moduleContext.getResources().getString(R.string.ok), (d, w) -> performResetOperation(context, profiles))
                    .setNegativeButton(moduleContext.getResources().getString(R.string.cancel), null)
                    .show();
        }
        private LinearLayout createProfileItem(Context context, ProfileInfo profile, ViewGroup parent) {
            LinearLayout layout = new LinearLayout(context);
            layout.setOrientation(LinearLayout.HORIZONTAL);
            layout.setPadding(32, 16, 32, 16);

            TextView tv = new TextView(context);
            tv.setText(profile.profileName);
            tv.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            tv.setTextSize(16);

            Button hideBtn = new Button(context);
                hideBtn.setText(moduleContext.getResources().getString(R.string.Hide));
            hideBtn.setBackgroundColor(Color.parseColor("#FF9800"));
            hideBtn.setTextColor(Color.WHITE);
            hideBtn.setOnClickListener(v -> {
                hiddenManager.addHiddenProfile(profile.contactMid);
                parent.removeView(layout);
                    Toast.makeText(context, profile.profileName + moduleContext.getResources().getString(R.string.user_hide), Toast.LENGTH_SHORT).show();
            });

            layout.addView(tv);
            layout.addView(hideBtn);
            return layout;
        }

        @SuppressLint("StaticFieldLeak")
        private void showRestoreDialog(Context context) {
            Set<String> hidden = hiddenManager.getHiddenProfiles();
            if (hidden.isEmpty()) {
                Toast.makeText(context, moduleContext.getResources().getString(R.string.no_hidden_profiles), Toast.LENGTH_SHORT).show();
                return;
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(context)
                    .setTitle(moduleContext.getResources().getString(R.string.Unhide_hidden_profiles));

            ScrollView scrollView = new ScrollView(context);
            LinearLayout container = new LinearLayout(context);
            container.setOrientation(LinearLayout.VERTICAL);
            scrollView.addView(container);

            new AsyncTask<Void, Void, List<ProfileInfo>>() {
                @Override
                protected List<ProfileInfo> doInBackground(Void... voids) {
                    return loadHiddenProfiles(context, hidden);
                }

                @Override
                protected void onPostExecute(List<ProfileInfo> hiddenProfiles) {
                    for (ProfileInfo profile : hiddenProfiles) {
                        LinearLayout itemLayout = new LinearLayout(context);
                        itemLayout.setOrientation(LinearLayout.HORIZONTAL);
                        itemLayout.setPadding(32, 16, 32, 16);

                        TextView tv = new TextView(context);
                        tv.setText(profile.profileName);
                        tv.setLayoutParams(new LinearLayout.LayoutParams(
                                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

                        Button restoreBtn = new Button(context);
                        restoreBtn.setText(moduleContext.getResources().getString(R.string.Redisplay));
                        restoreBtn.setBackgroundColor(Color.parseColor("#4CAF50"));
                        restoreBtn.setTextColor(Color.WHITE);
                        restoreBtn.setOnClickListener(v -> {
                            hiddenManager.removeHiddenProfile(profile.contactMid);
                            container.removeView(itemLayout);
                            Toast.makeText(context, profile.profileName + moduleContext.getResources().getString(R.string.redisplayed_Profile), Toast.LENGTH_SHORT).show();
                        });

                        itemLayout.addView(tv);
                        itemLayout.addView(restoreBtn);
                        container.addView(itemLayout);
                    }
                }
            }.execute();

            builder.setView(scrollView);
            builder.setNegativeButton(moduleContext.getResources().getString(R.string.Return), null);
            builder.show();
        }
        private List<ProfileInfo> loadHiddenProfiles(Context context, Set<String> hiddenMids) {
            List<ProfileInfo> profiles = new ArrayList<>();

            try (SQLiteDatabase contactDb = context.openOrCreateDatabase("contact", Context.MODE_PRIVATE, null)) {
                for (String contactMid : hiddenMids) {
                    String profileName = getProfileName(contactDb, contactMid);
                    profiles.add(new ProfileInfo(contactMid, profileName));
                }
            } catch (Exception e) {
               // XposedBridge.log("非表示プロファイル読込エラー: " + e.getMessage());
            }
            return profiles;
        }

        private void performResetOperation(Context context, List<ProfileInfo> profiles) {
            new Thread(() -> {
                SQLiteDatabase db = null;
                try {
                    db = context.openOrCreateDatabase("contact", Context.MODE_PRIVATE, null);
                    final String targetPhrase = "[" + moduleContext.getResources().getString(R.string.UserBlocked) + "]";

                    // 既存の値を取得
                    List<String> originalValues = new ArrayList<>();
                    try (Cursor cursor = db.rawQuery(
                            "SELECT overridden_name FROM contacts WHERE overridden_name LIKE ?",
                            new String[]{"%"+targetPhrase+"%"}
                    )) {
                        while (cursor.moveToNext()) {
                            originalValues.add(cursor.getString(0));
                        }
                    }

                    int affectedRows = 0;
                    if (!originalValues.isEmpty()) {
                        db.beginTransaction();
                        try {
                            ContentValues values = new ContentValues();
                            for (String original : originalValues) {
                                String updatedValue = original.replace(targetPhrase, "");

                                if (!original.equals(updatedValue)) {
                                    values.clear();
                                    values.put("overridden_name", updatedValue);

                                    affectedRows += db.update(
                                            "contacts",
                                            values,
                                            "overridden_name = ?",
                                            new String[]{original}
                                    );
                                }
                            }
                            db.setTransactionSuccessful();
                        } finally {
                            db.endTransaction();
                        }
                    }
                    final String message = affectedRows > 0 ?
                            affectedRows + moduleContext.getResources().getString(R.string.reset_name) :
                            moduleContext.getResources().getString(R.string.no_reset_name);

                    new Handler(Looper.getMainLooper()).post(() -> {
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                        refreshProfileList(context, profiles, moduleContext);
                    });

                } catch (Exception e) {
                    new Handler(Looper.getMainLooper()).post(() ->
                            Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
                } finally {
                    if (db != null) {
                        db.close();
                    }
                }
            }).start();
        }

        private void refreshProfileList(Context context, List<ProfileInfo> profiles,Context moduleContext) {
            List<ProfileInfo> newList = loadProfiles(context);
            profiles.clear();
            profiles.addAll(newList);
                showManagementDialog(context, profiles, moduleContext);

        }
    }

    private String getProfileName(SQLiteDatabase db, String contactMid) {
        try (Cursor cursor = db.rawQuery(
                "SELECT profile_name FROM contacts WHERE mid=?", new String[]{contactMid})) {
            return cursor.moveToFirst() ? cursor.getString(0) : "Unknown";
        }
    }
    private static class HiddenProfileManager {
        private static final String HIDDEN_FILE = "hidden_profiles.txt";
        private final File storageFile;

        public HiddenProfileManager(Context context) {
            File downloadsDir = context.getFilesDir();
            File limeDir = new File(downloadsDir, "LimeBackup/Setting");

            if (!limeDir.exists() && !limeDir.mkdirs()) {
                throw new RuntimeException("ディレクトリ作成に失敗: " + limeDir.getAbsolutePath());
            }

            storageFile = new File(limeDir, HIDDEN_FILE);
            if (!storageFile.exists()) {
                try {
                    storageFile.createNewFile();
                } catch (IOException e) {
                   // XposedBridge.log("ファイル作成エラー: " + e.getMessage());
                }
            }
        }

        public void addHiddenProfile(String contactMid) {
            new Thread(() -> {
                try (FileWriter fw = new FileWriter(storageFile, true);
                     BufferedWriter bw = new BufferedWriter(fw)) {
                    bw.write(contactMid);
                    bw.newLine();
                } catch (IOException e) {
                   // XposedBridge.log("非設定追加エラー: " + e.getMessage());
                }
            }).start();
        }

        public Set<String> getHiddenProfiles() {
            Set<String> hidden = Collections.synchronizedSet(new HashSet<>());
            if (storageFile.exists()) {
                try (BufferedReader br = new BufferedReader(new FileReader(storageFile))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        hidden.add(line.trim());
                    }
                } catch (IOException e) {
                   // XposedBridge.log("非表示リスト読込エラー: " + e.getMessage());
                }
            }
            return hidden;
        }

        public void removeHiddenProfile(String contactMid) {
            new Thread(() -> {
                Set<String> current = getHiddenProfiles();
                if (current.remove(contactMid)) {
                    try (FileWriter fw = new FileWriter(storageFile);
                         BufferedWriter bw = new BufferedWriter(fw)) {
                        for (String mid : current) {
                            bw.write(mid);
                            bw.newLine();
                        }
                    } catch (IOException e) {
                       // XposedBridge.log("非表示解除エラー: " + e.getMessage());
                    }
                }
            }).start();
        }
    }


    private static final int PICK_FILE_REQUEST_CODE = 1001;

    private void showFilePickerChatHistory(Context context, Context moduleContext) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {

            if (context instanceof Activity) {
                ((Activity) context).startActivityForResult(Intent.createChooser(intent, "Select a file to restore"), PICK_FILE_REQUEST_CODE);
            } else {
                Toast.makeText(context, "Context is not an Activity", Toast.LENGTH_SHORT).show();
            }
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(context, "Please install a File Manager.", Toast.LENGTH_SHORT).show();
        }
    }

    private static final int PICK_FILE_REQUEST_CODE2 = 1002;

    private void showFilePickerChatlist(Context context, Context moduleContext) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            if (context instanceof Activity) {
                ((Activity) context).startActivityForResult(Intent.createChooser(intent, "Select a file to restore"), PICK_FILE_REQUEST_CODE2);
            } else {
                Toast.makeText(context, "Context is not an Activity", Toast.LENGTH_SHORT).show();
            }
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(context, "Please install a File Manager.", Toast.LENGTH_SHORT).show();
        }
    }

    private void restoreChatHistory(Context context, Context moduleContext, File finalTempFile) {
        new AsyncTask<Void, Integer, Boolean>() {
            private ProgressDialog progressDialog;
            private int totalRecords = 0;
            private int processedRecords = 0;
            private String errorMessage = null;
            private Exception exception = null;

            @Override
            protected void onPreExecute() {
                progressDialog = new ProgressDialog(context);
                progressDialog.setTitle(moduleContext.getString(R.string.restoring_chat_history));
                progressDialog.setMessage(moduleContext.getString(R.string.preparing));
                progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                progressDialog.setCancelable(false);
                progressDialog.setMax(100);
                progressDialog.setProgress(0);
                progressDialog.show();
            }

            @Override
            protected Boolean doInBackground(Void... voids) {
                final int BATCH_SIZE = 50;
                if (finalTempFile == null) {
                    errorMessage = "Backup file path is null";
                    return false;
                }

                File backupDbFile = finalTempFile;
                if (!backupDbFile.exists()) {
                    errorMessage = "Backup file not found at: " + backupDbFile.getAbsolutePath();
                    showToast(context, moduleContext, R.string.Delete_Cache);
                    return false;
                }

                SQLiteDatabase backupDb = null;
                SQLiteDatabase originalDb = null;
                Cursor countCursor = null;
                Cursor dataCursor = null;

                try {

                    backupDb = SQLiteDatabase.openDatabase(
                            backupDbFile.getAbsolutePath(),
                            null,
                            SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS
                    );

                    try {
                        countCursor = backupDb.rawQuery("SELECT COUNT(*) FROM chat_history", null);
                        if (countCursor == null || !countCursor.moveToFirst()) {
                            errorMessage = "Failed to get record count";
                            return false;
                        }
                        totalRecords = countCursor.getInt(0);
                        publishProgress(0);
                    } catch (SQLiteException e) {
                        errorMessage = "Error counting records";
                        exception = e;
                        return false;
                    }

                    if (totalRecords == 0) {
                        return true;
                    }

                    try {
                        originalDb = context.openOrCreateDatabase(
                                "naver_line",
                                Context.MODE_PRIVATE,
                                null
                        );
                    } catch (SQLiteException e) {
                        errorMessage = "Failed to open target database";
                        exception = e;
                        return false;
                    }

                    try {
                        dataCursor = backupDb.rawQuery("SELECT * FROM chat_history", null);
                        if (dataCursor == null || !dataCursor.moveToFirst()) {
                            errorMessage = "No data to restore";
                            return false;
                        }

                        originalDb.beginTransaction();

                        do {
                            if (isCancelled()) {
                                errorMessage = "Restoration cancelled";
                                return false;
                            }

                            try {
                                String serverId = dataCursor.getString(
                                        dataCursor.getColumnIndexOrThrow("server_id")
                                );

                                if (serverId == null) continue;

                                if (!isRecordExists(originalDb, "chat_history", "server_id", serverId)) {
                                    ContentValues values = extractChatHistoryValues(dataCursor);
                                    long rowId = originalDb.insertWithOnConflict(
                                            "chat_history",
                                            null,
                                            values,
                                            SQLiteDatabase.CONFLICT_IGNORE
                                    );

                                    if (rowId == -1) {
                                        Log.w("Restore", "Failed to insert record: " + serverId);
                                    }
                                }

                                processedRecords++;


                                if (processedRecords % Math.max(1, totalRecords / 100) == 0 ||
                                        processedRecords % BATCH_SIZE == 0) {
                                    int progress = (int) ((float) processedRecords / totalRecords * 100);
                                    publishProgress(progress);
                                }
                                if (processedRecords % BATCH_SIZE == 0) {
                                    originalDb.setTransactionSuccessful();
                                    originalDb.endTransaction();
                                    originalDb.beginTransaction();
                                }
                            } catch (SQLiteException e) {
                                Log.e("RestoreRecord", "Error restoring record " + processedRecords, e);
                                continue;
                            }

                        } while (dataCursor.moveToNext());

                        originalDb.setTransactionSuccessful();
                        return true;

                    } catch (Exception e) {
                        errorMessage = "Error during data restoration";
                        exception = e;
                        return false;
                    }

                } catch (Exception e) {
                    errorMessage = "Unexpected error during restoration";
                    exception = e;
                    return false;
                } finally {

                    if (originalDb != null) {
                        try {
                            originalDb.endTransaction();
                            originalDb.close();
                        } catch (Exception e) {
                            Log.e("CloseDB", "Error closing original DB", e);
                        }
                    }
                    closeQuietly(countCursor);
                    closeQuietly(dataCursor);
                    if (backupDb != null) {
                        try {
                            backupDb.close();
                        } catch (Exception e) {
                            Log.e("CloseDB", "Error closing backup DB", e);
                        }
                    }
                }
            }

            @Override
            protected void onProgressUpdate(Integer... progress) {
                if (progressDialog != null && progress.length > 0) {
                    int percent = progress[0];
                    progressDialog.setProgress(percent);
                    progressDialog.setMessage(
                            String.format(
                                    moduleContext.getString(R.string.progress_message),
                                    processedRecords,
                                    totalRecords,
                                    percent
                            )
                    );
                }
            }

            @Override
            protected void onPostExecute(Boolean success) {
                if (progressDialog != null) {
                    progressDialog.dismiss();
                }

                if (success) {
                    showToast(context, moduleContext, R.string.Restore_Success);
                } else {
                    Log.e("RestoreError", errorMessage, exception);
                    String userMessage = moduleContext.getString(R.string.Restore_Error);
                    if (exception instanceof SQLiteException) {
                        userMessage += ": Database error";
                    } else if (exception != null) {
                        userMessage += ": System error";
                    }

                    showToast(context, moduleContext, Integer.parseInt(userMessage));
                }
            }

            @Override
            protected void onCancelled() {
                if (progressDialog != null) {
                    progressDialog.dismiss();
                }
                showToast(context, moduleContext, R.string.Restore_Cancelled);
            }

            private void closeQuietly(Closeable closeable) {
                if (closeable != null) {
                    try {
                        closeable.close();
                    } catch (IOException e) {
                        Log.e("Close", "Error closing resource", e);
                    }
                }
            }
        }.execute();
    }
    private void restoreChatList(Context context, Context moduleContext, File finalTempFile) {
        new AsyncTask<Void, Integer, Boolean>() {
            private ProgressDialog progressDialog;
            private int totalRecords = 0;
            private int processedRecords = 0;

            @Override
            protected void onPreExecute() {
                XposedBridge.log("[Restore] Starting chat list restoration process");
                progressDialog = new ProgressDialog(context);
                progressDialog.setTitle(moduleContext.getString(R.string.restoring_chat));
                progressDialog.setMessage(moduleContext.getString(R.string.preparing));
                progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                progressDialog.setCancelable(false);
                progressDialog.setMax(100);
                progressDialog.setProgress(0);
                progressDialog.show();
            }

            @Override
            protected Boolean doInBackground(Void... voids) {
                final int BATCH_SIZE = 500;
                XposedBridge.log("[Restore] Batch size set to: " + BATCH_SIZE);

                if (finalTempFile == null || !finalTempFile.exists()) {
                    XposedBridge.log("[Restore] ERROR: Backup file not found or null");
                    return false;
                }

                SQLiteDatabase backupDb = null;
                SQLiteDatabase originalDb = null;
                Cursor cursor = null;
                Cursor countCursor = null;

                try {
                    if (!finalTempFile.canRead()) {
                        XposedBridge.log("[Restore] ERROR: No read permission for backup file");
                        return false;
                    }

                    XposedBridge.log("[Restore] Opening backup database: " + finalTempFile.getAbsolutePath());
                    backupDb = SQLiteDatabase.openDatabase(
                            finalTempFile.getAbsolutePath(),
                            null,
                            SQLiteDatabase.OPEN_READONLY
                    );

                    countCursor = backupDb.rawQuery("SELECT COUNT(*) FROM chat", null);
                    if (countCursor != null && countCursor.moveToFirst()) {
                        totalRecords = countCursor.getInt(0);
                        XposedBridge.log("[Restore] Total records to process: " + totalRecords);
                        publishProgress(0);
                    }

                    originalDb = context.openOrCreateDatabase(
                            "naver_line",
                            Context.MODE_PRIVATE,
                            null
                    );
                    XposedBridge.log("[Restore] Original database opened");

                    originalDb.beginTransaction();
                    cursor = backupDb.rawQuery("SELECT * FROM chat", null);

                    if (cursor != null && cursor.moveToFirst()) {
                        XposedBridge.log("[Restore] Starting to process records");
                        do {
                            if (isCancelled()) {
                                XposedBridge.log("[Restore] Operation cancelled by user");
                                return false;
                            }

                            String chatId = cursor.getString(
                                    cursor.getColumnIndexOrThrow("chat_id")
                            );
                            if (chatId == null) {
                                XposedBridge.log("[Restore] WARNING: Found null chat_id, skipping");
                                continue;
                            }

                            ContentValues values = extractChatValues(cursor);
                            long rowId = originalDb.insertWithOnConflict(
                                    "chat",
                                    null,
                                    values,
                                    SQLiteDatabase.CONFLICT_IGNORE
                            );

                            if (rowId == -1) {
                                XposedBridge.log("[Restore] WARNING: Conflict detected for chat_id: " + chatId);
                            }

                            processedRecords++;
                            if (processedRecords % Math.max(1, totalRecords / 100) == 0 ||
                                    processedRecords % BATCH_SIZE == 0) {
                                int progress = (int) ((float) processedRecords / totalRecords * 100);
                                publishProgress(progress);
                            }
                            if (processedRecords % BATCH_SIZE == 0) {
                                originalDb.setTransactionSuccessful();
                                originalDb.endTransaction();
                                XposedBridge.log("[Restore] Committed batch of " + BATCH_SIZE + " records");
                                originalDb.beginTransaction();
                            }

                        } while (cursor.moveToNext());
                    }

                    originalDb.setTransactionSuccessful();
                    XposedBridge.log("[Restore] Successfully processed all records");
                    return true;

                } catch (Exception e) {
                    XposedBridge.log("[Restore] ERROR: Exception during restoration: " + Log.getStackTraceString(e));
                    return false;
                } finally {
                    if (originalDb != null) {
                        try {
                            originalDb.endTransaction();
                            originalDb.close();
                            XposedBridge.log("[Restore] Original database closed");
                        } catch (Exception e) {
                            XposedBridge.log("[Restore] ERROR closing original DB: " + Log.getStackTraceString(e));
                        }
                    }
                    closeQuietly(cursor);
                    closeQuietly(countCursor);
                    if (backupDb != null) {
                        try {
                            backupDb.close();
                            XposedBridge.log("[Restore] Backup database closed");
                        } catch (Exception e) {
                            XposedBridge.log("[Restore] ERROR closing backup DB: " + Log.getStackTraceString(e));
                        }
                    }

                    if (finalTempFile != null && finalTempFile.exists()) {
                        boolean deleted = finalTempFile.delete();
                        XposedBridge.log("[Restore] Temp file deletion " + (deleted ? "successful" : "failed"));
                    }
                }
            }

            @Override
            protected void onProgressUpdate(Integer... progress) {
                if (progressDialog != null && progress.length > 0) {
                    int percent = progress[0];
                    progressDialog.setProgress(percent);
                    progressDialog.setMessage(
                            String.format(
                                    "%s: %d/%d (%d%%)",
                                    moduleContext.getString(R.string.processing),
                                    processedRecords,
                                    totalRecords,
                                    percent
                            )
                    );
                    if (percent % 10 == 0) {
                        XposedBridge.log("[Restore] Progress: " + percent + "% (" + processedRecords + "/" + totalRecords + ")");
                    }
                }
            }

            @Override
            protected void onPostExecute(Boolean success) {
                if (progressDialog != null) {
                    progressDialog.dismiss();
                }

                if (success) {
                    XposedBridge.log("[Restore] SUCCESS: Chat table restored successfully");
                    showToast(context, moduleContext, R.string.Restore_Chat_Table_Success);
                } else {
                    XposedBridge.log("[Restore] FAILED: Chat table restoration failed");
                    showToast(context, moduleContext, R.string.Restore_Chat_Table_Error);
                }
            }

            @Override
            protected void onCancelled() {
                XposedBridge.log("[Restore] CANCELLED: Operation cancelled by user");
                if (progressDialog != null) {
                    progressDialog.dismiss();
                }
                showToast(context, moduleContext, R.string.Restore_Cancelled);
            }

            private void closeQuietly(Closeable closeable) {
                if (closeable != null) {
                    try {
                        closeable.close();
                    } catch (IOException e) {
                        XposedBridge.log("[Restore] ERROR closing resource: " + Log.getStackTraceString(e));
                    }
                }
            }
        }.execute();
    }
    private ContentValues extractChatHistoryValues(Cursor cursor) {
        ContentValues values = new ContentValues();
        values.put("server_id", cursor.getString(cursor.getColumnIndexOrThrow("server_id")));
        values.put("type", getNullableInt(cursor, "type"));
        values.put("chat_id", cursor.getString(cursor.getColumnIndexOrThrow("chat_id")));
        values.put("from_mid", cursor.getString(cursor.getColumnIndexOrThrow("from_mid")));
        values.put("content", cursor.getString(cursor.getColumnIndexOrThrow("content")));
        values.put("created_time", cursor.getString(cursor.getColumnIndexOrThrow("created_time")));
        values.put("delivered_time", cursor.getString(cursor.getColumnIndexOrThrow("delivered_time")));
        values.put("status", getNullableInt(cursor, "status"));
        values.put("sent_count", getNullableInt(cursor, "sent_count"));
        values.put("read_count", getNullableInt(cursor, "read_count"));
        values.put("location_name", cursor.getString(cursor.getColumnIndexOrThrow("location_name")));
        values.put("location_address", cursor.getString(cursor.getColumnIndexOrThrow("location_address")));
        values.put("location_phone", cursor.getString(cursor.getColumnIndexOrThrow("location_phone")));
        values.put("location_latitude", getNullableInt(cursor, "location_latitude"));
        values.put("location_longitude", getNullableInt(cursor, "location_longitude"));
        values.put("attachement_image", getNullableInt(cursor, "attachement_image"));
        values.put("attachement_image_height", getNullableInt(cursor, "attachement_image_height"));
        values.put("attachement_image_width", getNullableInt(cursor, "attachement_image_width"));
        values.put("attachement_image_size", getNullableInt(cursor, "attachement_image_size"));
        values.put("attachement_type", getNullableInt(cursor, "attachement_type"));
        values.put("attachement_local_uri", cursor.getString(cursor.getColumnIndexOrThrow("attachement_local_uri")));
        values.put("parameter", cursor.getString(cursor.getColumnIndexOrThrow("parameter")));
        values.put("chunks", cursor.getBlob(cursor.getColumnIndexOrThrow("chunks")));
        return values;
    }

    private ContentValues extractChatValues(Cursor cursor) {
        ContentValues values = new ContentValues();
        values.put("chat_id", cursor.getString(cursor.getColumnIndexOrThrow("chat_id")));
        values.put("chat_name", cursor.getString(cursor.getColumnIndexOrThrow("chat_name")));
        values.put("owner_mid", cursor.getString(cursor.getColumnIndexOrThrow("owner_mid")));
        values.put("last_from_mid", cursor.getString(cursor.getColumnIndexOrThrow("last_from_mid")));
        values.put("last_message", cursor.getString(cursor.getColumnIndexOrThrow("last_message")));
        values.put("last_created_time", cursor.getString(cursor.getColumnIndexOrThrow("last_created_time")));
        values.put("message_count", getNullableInt(cursor, "message_count"));
        values.put("read_message_count", getNullableInt(cursor, "read_message_count"));
        values.put("latest_mentioned_position", getNullableInt(cursor, "latest_mentioned_position"));
        values.put("type", getNullableInt(cursor, "type"));
        values.put("is_notification", getNullableInt(cursor, "is_notification"));
        values.put("skin_key", cursor.getString(cursor.getColumnIndexOrThrow("skin_key")));
        values.put("input_text", cursor.getString(cursor.getColumnIndexOrThrow("input_text")));
        values.put("input_text_metadata", cursor.getString(cursor.getColumnIndexOrThrow("input_text_metadata")));
        values.put("hide_member", getNullableInt(cursor, "hide_member"));
        values.put("p_timer", getNullableInt(cursor, "p_timer"));
        values.put("last_message_display_time", cursor.getString(cursor.getColumnIndexOrThrow("last_message_display_time")));
        values.put("mid_p", cursor.getString(cursor.getColumnIndexOrThrow("mid_p")));
        values.put("is_archived", getNullableInt(cursor, "is_archived"));
        values.put("read_up", cursor.getString(cursor.getColumnIndexOrThrow("read_up")));
        values.put("is_groupcalling", getNullableInt(cursor, "is_groupcalling"));
        values.put("latest_announcement_seq", getNullableInt(cursor, "latest_announcement_seq"));
        values.put("announcement_view_status", getNullableInt(cursor, "announcement_view_status"));
        values.put("last_message_meta_data", cursor.getString(cursor.getColumnIndexOrThrow("last_message_meta_data")));
        values.put("chat_room_bgm_data", cursor.getString(cursor.getColumnIndexOrThrow("chat_room_bgm_data")));
        values.put("chat_room_bgm_checked", getNullableInt(cursor, "chat_room_bgm_checked"));
        values.put("chat_room_should_show_bgm_badge", getNullableInt(cursor, "chat_room_should_show_bgm_badge"));
        values.put("unread_type_and_count", cursor.getString(cursor.getColumnIndexOrThrow("unread_type_and_count")));
        return values;
    }

    private Integer getNullableInt(Cursor cursor, String columnName) {
        int columnIndex = cursor.getColumnIndexOrThrow(columnName);
        return cursor.isNull(columnIndex) ? null : cursor.getInt(columnIndex);
    }

    private boolean isRecordExists(SQLiteDatabase db, String table, String column, String value) {
        try (Cursor cursor = db.rawQuery("SELECT 1 FROM " + table + " WHERE " + column + " = ?", new String[]{value})) {
            return cursor != null && cursor.moveToFirst();
        }
    }

    private void showToast(Context context, Context moduleContext, int resId) {
        Toast.makeText(context, moduleContext.getResources().getString(resId), Toast.LENGTH_SHORT).show();
    }

    private void backupChatsFolder(Context context,Context moduleContext) {
        File originalChatsDir = new File(Environment.getExternalStorageDirectory(), "Android/data/jp.naver.line.android/files/chats");
        File backupDir = new File(context.getFilesDir(), "LimeBackup");

        if (!backupDir.exists() && !backupDir.mkdirs()) {
            backupDir = new File(Environment.getExternalStorageDirectory(), "Android/data/jp.naver.line.android/backup");
            if (!backupDir.exists() && !backupDir.mkdirs()) {
                Toast.makeText(context, "Failed to create backup directory", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        File backupChatsDir = new File(backupDir, "chats_backup");
        if (!backupChatsDir.exists() && !backupChatsDir.mkdirs()) {
            Toast.makeText(context, "Failed to create chats_backup directory", Toast.LENGTH_SHORT).show();
            return;
        }


        try {
            copyDirectory(originalChatsDir, backupChatsDir);
            Toast.makeText(context,moduleContext.getResources().getString(R.string.BackUp_Chat_Photo_Success), Toast.LENGTH_SHORT).show();
        } catch (IOException ignored) {
            Toast.makeText(context,moduleContext.getResources().getString(R.string.BackUp_Chat_Photo_Error), Toast.LENGTH_SHORT).show();
        }
    }

    private void copyDirectory(File sourceDir, File destDir) throws IOException {
        if (!sourceDir.exists()) {return;}

        if (!destDir.exists()) {
            destDir.mkdirs();
        }

        File[] files = sourceDir.listFiles();
        if (files != null) {
            for (File file : files) {
                File destFile = new File(destDir, file.getName());
                if (file.isDirectory()) {
                    copyDirectory(file, destFile);
                } else {
                    copyFile(file, destFile);
                }
            }
        }
    }
    private void copyFile(File sourceFile, File destFile) throws IOException {
        if (destFile.exists()) {
            destFile.delete();
        }

        try (FileChannel sourceChannel = new FileInputStream(sourceFile).getChannel();
             FileChannel destChannel = new FileOutputStream(destFile).getChannel()) {
            destChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
        }
    }


    private void restoreChatsFolder(Context context, Context moduleContext) {
        File backupDir = new File(context.getFilesDir(), "LimeBackup/chats_backup");
        File originalChatsDir = new File(Environment.getExternalStorageDirectory(), "Android/data/jp.naver.line.android/files/chats");
        if (!backupDir.exists()) {
            File alternativeBackupDir = new File(Environment.getExternalStorageDirectory(), "Android/data/jp.naver.line.android/backup/chats_backup");
            if (!alternativeBackupDir.exists()) {
                Toast.makeText(context, moduleContext.getResources().getString(R.string.Restore_Chat_Photo_Not_Folder), Toast.LENGTH_SHORT).show();
                return;
            } else {
                backupDir = alternativeBackupDir;
            }
        }
        if (!originalChatsDir.exists() && !originalChatsDir.mkdirs()) {
            Toast.makeText(context, moduleContext.getResources().getString(R.string.Restore_Create_Failed_Chat_Photo_Folder), Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            copyDirectory(backupDir, originalChatsDir);
            Toast.makeText(context, moduleContext.getResources().getString(R.string.Restore_Chat_Photo_Success), Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(context, moduleContext.getResources().getString(R.string.Restore_Chat_Photo_Error), Toast.LENGTH_SHORT).show();
        }
    }

    private Context getTargetAppContext(XC_LoadPackage.LoadPackageParam lpparam) {
        Context contextV = null;

        
        try {
            contextV = AndroidAppHelper.currentApplication();
            if (contextV != null) {
               // XposedBridge.log("Lime: Got context via AndroidAppHelper: " + contextV.getPackageName());
                return contextV;
            }
        } catch (Throwable t) {
           // XposedBridge.log("Lime: AndroidAppHelper failed: " + t.toString());
        }

        
        try {
            Class<?> activityThreadClass = XposedHelpers.findClass("android.app.ActivityThread", lpparam.classLoader);
            Object activityThread = XposedHelpers.callStaticMethod(activityThreadClass, "currentActivityThread");
            Object loadedApk = XposedHelpers.getObjectField(activityThread, "mBoundApplication");
            Object appInfo = XposedHelpers.getObjectField(loadedApk, "info");
            contextV = (Context) XposedHelpers.callMethod(activityThread, "getSystemContext");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
               // XposedBridge.log("Lime: Context via ActivityThread: "+ contextV.getPackageName() + " | DataDir: " + contextV.getDataDir());
            }
            return contextV;
        } catch (Throwable t) {
           // XposedBridge.log("Lime: ActivityThread method failed: " + t.toString());
        }

        
        try {
            Context systemContext = (Context) XposedHelpers.callStaticMethod(
                    XposedHelpers.findClass("android.app.ContextImpl", lpparam.classLoader),
                    "createSystemContext",
                    XposedHelpers.callStaticMethod(
                            XposedHelpers.findClass("android.app.ActivityThread", lpparam.classLoader),
                            "currentActivityThread"
                    )
            );

            contextV = systemContext.createPackageContext(
                    Constants.PACKAGE_NAME,
                    Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY
            );

           // XposedBridge.log("Lime: Fallback context created: "+ (contextV != null ? contextV.getPackageName() : "null"));
            return contextV;
        } catch (Throwable t) {
           // XposedBridge.log("Lime: Fallback context failed: " + t.toString());
        }

        return null;
    }


    private void GetMidId(Context context, Context moduleContext) {
        SQLiteDatabase profileDb = context.openOrCreateDatabase("contact", Context.MODE_PRIVATE, null);
        String backupUri = loadBackupUri(context);
        if (backupUri == null) {
            XposedBridge.log("Lime Backup: No backup URI found");
            return;
        }

        try {
            Uri treeUri = Uri.parse(backupUri);
            DocumentFile pickedDir = DocumentFile.fromTreeUri(context, treeUri);

            if (pickedDir == null || !pickedDir.exists()) {
                XposedBridge.log("Lime Backup: Directory does not exist or access denied");
                return;
            }
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
            String fileName = "contacts_" + sdf.format(new Date()) + ".csv";

            DocumentFile file = pickedDir.createFile("text/csv", fileName);
            if (file == null) {
                XposedBridge.log("Lime Backup: Failed to create file");
                return;
            }

            OutputStream outputStream = context.getContentResolver().openOutputStream(file.getUri());
            if (outputStream == null) {
                XposedBridge.log("Lime Backup: Failed to open output stream");
                return;
            }

            OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);

            writer.write("mid,profile_name\n");
            writer.flush();

            Cursor cursor = null;
            try {
                cursor = profileDb.rawQuery("SELECT mid, profile_name FROM contact", null);
                if (cursor != null && cursor.moveToFirst()) {
                    do {
                        String mid = cursor.getString(0);
                        String profileName = cursor.getString(1);

                        // CSV行を構築（値にカンマや改行が含まれる場合はダブルクォートで囲む）
                        String line = "\"" + (mid != null ? mid.replace("\"", "\"\"") : "") + "\"," +
                                "\"" + (profileName != null ? profileName.replace("\"", "\"\"") : "") + "\"\n";

                        // 1行ずつ書き込み
                        writer.write(line);
                        writer.flush();

                    } while (cursor.moveToNext());
                }
                XposedBridge.log("Lime Backup: CSV exported successfully to " + file.getUri());
            } catch (Exception e) {
                XposedBridge.log("Lime Database Error: " + e.getMessage());
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
                writer.close();
                outputStream.close();
                profileDb.close();
                Toast.makeText(context, "Save Mid ID", Toast.LENGTH_LONG).show();

            }
        } catch (IOException e) {
            XposedBridge.log("Lime CSV Export Error: " + e.getMessage());
            Toast.makeText(context, "Error", Toast.LENGTH_LONG).show();
        }
    }
    private String loadBackupUri(Context context) {
        File settingsFile = new File(context.getFilesDir(), "LimeBackup/backup_uri.txt");
        if (!settingsFile.exists()) return null;

        try (FileInputStream fis = new FileInputStream(settingsFile);
             InputStreamReader isr = new InputStreamReader(fis);
             BufferedReader br = new BufferedReader(isr)) {
            return br.readLine();
        } catch (IOException e) {
            XposedBridge.log("Lime URI Load Error: " + e.getMessage());
            return null;
        }
    }
}