package io.github.hiro.lime.hooks;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.AndroidAppHelper;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.text.InputType;
import android.text.method.ScrollingMovementMethod;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.hiro.lime.BuildConfig;
import io.github.hiro.lime.LimeOptions;
import io.github.hiro.lime.Main;
import io.github.hiro.lime.R;
import io.github.hiro.lime.Utils;

public class EmbedOptions implements IHook {
    @Override
    public void hook(LimeOptions limeOptions, XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        if (limeOptions.removeOption.checked) return;
        XposedBridge.hookAllMethods(
                loadPackageParam.classLoader.loadClass("com.linecorp.line.settings.main.LineUserMainSettingsFragment"),
                "onViewCreated",
                new XC_MethodHook() {

                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        Context contextV = (Context) XposedHelpers.callMethod(XposedHelpers.callStaticMethod(
                                XposedHelpers.findClass("android.app.ActivityThread", null),
                                "currentActivityThread"
                        ), "getSystemContext");
                        try {




                            PackageManager pm = contextV.getPackageManager();
                            String versionName = ""; // 初期化
                            try {
                                versionName = pm.getPackageInfo(loadPackageParam.packageName, 0).versionName;
                            } catch (PackageManager.NameNotFoundException e) {
                                e.printStackTrace();
                            }

                            CustomPreferences customPreferences = new CustomPreferences();

                            for (LimeOptions.Option option : limeOptions.options) {
                                option.checked = Boolean.parseBoolean(customPreferences.getSetting(option.name, String.valueOf(option.checked)));
                            }
                            Context moduleContext = AndroidAppHelper.currentApplication().createPackageContext(
                                    "io.github.hiro.lime", Context.CONTEXT_IGNORE_SECURITY);
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
                                copyButton.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                                        ClipData clip = ClipData.newPlainText("", editText.getText().toString());
                                        clipboard.setPrimaryClip(clip);
                                    }
                                });

                                buttonLayout.addView(copyButton);

                                Button pasteButton = new Button(context);
                                pasteButton.setText(R.string.button_paste);
                                pasteButton.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                                        if (clipboard != null && clipboard.hasPrimaryClip()) {
                                            ClipData clip = clipboard.getPrimaryClip();
                                            if (clip != null && clip.getItemCount() > 0) {
                                                CharSequence pasteData = clip.getItemAt(0).getText();
                                                editText.setText(pasteData);
                                            }
                                        }
                                    }
                                });

                                buttonLayout.addView(pasteButton);
                                layoutModifyRequest.addView(buttonLayout);
                                ScrollView scrollView = new ScrollView(context);
                                scrollView.addView(layoutModifyRequest);
                                AlertDialog.Builder builder = new AlertDialog.Builder(context)
                                        .setTitle(R.string.modify_request);
                                builder.setView(scrollView);
                                Button backupButton = new Button(context);
                                buttonParams.topMargin = Utils.dpToPx(20, context);
                                backupButton.setLayoutParams(buttonParams);
                                backupButton.setText(moduleContext.getResources().getString(R.string.Back_Up));
                                backupButton.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        backupChatHistory(context, moduleContext);
                                    }
                                });
                                layout.addView(backupButton);
                                Button restoreButton = new Button(context);
                                restoreButton.setLayoutParams(buttonParams);
                                restoreButton.setText(moduleContext.getResources().getString(R.string.Restore));
                                restoreButton.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        restoreChatHistory(context, moduleContext);
                                    }
                                });
                                layout.addView(restoreButton);
                                Button backupfolderButton = new Button(context);
                                backupfolderButton.setLayoutParams(buttonParams);
                                backupfolderButton.setText(moduleContext.getResources().getString(R.string.Talk_Picture_Back_up));
                                backupfolderButton.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        backupChatsFolder(context, moduleContext);
                                    }
                                });
                                layout.addView(backupfolderButton);
                                Button restorefolderButton = new Button(context);
                                restorefolderButton.setLayoutParams(buttonParams);
                                restorefolderButton.setText(moduleContext.getResources().getString(R.string.Picure_Restore));
                                restorefolderButton.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        restoreChatsFolder(context, moduleContext);
                                    }
                                });
                                layout.addView(restorefolderButton);
                                if (limeOptions.MuteGroup.checked) {
                                    Button MuteGroups_Button = new Button(context);
                                    MuteGroups_Button.setLayoutParams(buttonParams);
                                    MuteGroups_Button.setText(moduleContext.getResources().getString(R.string.Mute_Group));
                                    MuteGroups_Button.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            MuteGroups_Button(context, moduleContext);
                                        }
                                    });
                                    layout.addView(MuteGroups_Button);
                                }

                                Button KeepUnread_Button = new Button(context);
                                KeepUnread_Button.setLayoutParams(buttonParams);
                                KeepUnread_Button.setText(moduleContext.getResources().getString(R.string.edit_margin_settings));
                                KeepUnread_Button.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        KeepUnread_Button(context, moduleContext);
                                    }
                                });
                                layout.addView(KeepUnread_Button);

                                if (limeOptions.preventUnsendMessage.checked) {
                                    Button canceled_message_Button = new Button(context);
                                    canceled_message_Button.setLayoutParams(buttonParams);
                                    canceled_message_Button.setText(moduleContext.getResources().getString(R.string.canceled_message));
                                    canceled_message_Button.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            Cancel_Message_Button(context, moduleContext);
                                        }
                                    });
                                    layout.addView(canceled_message_Button);
                                }
                                if (limeOptions.CansellNotification.checked) {

                                    Button CansellNotification_Button = new Button(context);
                                    CansellNotification_Button.setLayoutParams(buttonParams);
                                    CansellNotification_Button.setText(moduleContext.getResources().getString(R.string.CansellNotification));
                                    CansellNotification_Button.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            CansellNotification(context, moduleContext);
                                        }
                                    });
                                    layout.addView(CansellNotification_Button);
                                }

                                builder.setPositiveButton(R.string.positive_button, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        String code = editText.getText().toString();
                                        if (!code.equals(script)) {
                                            // CustomPreferencesのインスタンスを作成
                                            CustomPreferences customPreferences = null;
                                            try {
                                                customPreferences = new CustomPreferences();
                                            } catch (PackageManager.NameNotFoundException e) {
                                                throw new RuntimeException(e);
                                            }
                                            // Base64エンコードして設定を保存
                                            String encodedCode = Base64.encodeToString(code.getBytes(), Base64.NO_WRAP);
                                            customPreferences.saveSetting("encoded_js_modify_request", encodedCode);

                                            Toast.makeText(context.getApplicationContext(), context.getString(R.string.restarting), Toast.LENGTH_SHORT).show();
                                            Process.killProcess(Process.myPid());
                                            context.startActivity(new Intent().setClassName(Constants.PACKAGE_NAME, "jp.naver.line.android.activity.SplashActivity"));
                                        }
                                    }
                                });

                                builder.setNegativeButton(R.string.negative_button, null);

                                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                                    @Override
                                    public void onDismiss(DialogInterface dialog) {
                                        editText.setText(script);
                                    }
                                });

                                AlertDialog dialog = builder.create();
                                Button button = new Button(context);
                                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.WRAP_CONTENT,
                                        LinearLayout.LayoutParams.WRAP_CONTENT);
                                params.topMargin = Utils.dpToPx(20, context);
                                button.setLayoutParams(params);
                                button.setText(R.string.modify_request);
                                button.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        dialog.show();
                                    }
                                });
                                layout.addView(button);
                            }

                            {
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
                                copyButton.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                                        ClipData clip = ClipData.newPlainText("", editText.getText().toString());
                                        clipboard.setPrimaryClip(clip);
                                    }
                                });

                                buttonLayout.addView(copyButton);

                                Button pasteButton = new Button(context);
                                pasteButton.setText(R.string.button_paste);
                                pasteButton.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                                        if (clipboard != null && clipboard.hasPrimaryClip()) {
                                            ClipData clip = clipboard.getPrimaryClip();
                                            if (clip != null && clip.getItemCount() > 0) {
                                                CharSequence pasteData = clip.getItemAt(0).getText();
                                                editText.setText(pasteData);
                                            }
                                        }
                                    }
                                });


                                buttonLayout.addView(pasteButton);
                                layoutModifyResponse.addView(buttonLayout);
                                ScrollView scrollView = new ScrollView(context);
                                scrollView.addView(layoutModifyResponse);
                                AlertDialog.Builder builder = new AlertDialog.Builder(context)
                                        .setTitle(R.string.modify_response);
                                builder.setView(scrollView);
                                builder.setPositiveButton(R.string.positive_button, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        String code = editText.getText().toString();
                                        if (!code.equals(script)) {
                                            customPreferences.saveSetting("encoded_js_modify_response", Base64.encodeToString(code.getBytes(), Base64.NO_WRAP));
                                            Toast.makeText(context.getApplicationContext(), context.getString(R.string.restarting), Toast.LENGTH_SHORT).show();
                                            Process.killProcess(Process.myPid());
                                            context.startActivity(new Intent().setClassName(Constants.PACKAGE_NAME, "jp.naver.line.android.activity.SplashActivity"));
                                        }
                                    }
                                });

                                builder.setNegativeButton(R.string.negative_button, null);

                                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                                    @Override
                                    public void onDismiss(DialogInterface dialog) {
                                        editText.setText(script);
                                    }
                                });
                                AlertDialog dialog = builder.create();
                                Button button = new Button(context);
                                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.WRAP_CONTENT,
                                        LinearLayout.LayoutParams.WRAP_CONTENT);
                                params.topMargin = Utils.dpToPx(20, context);
                                button.setLayoutParams(params);
                                button.setText(R.string.modify_response);
                                button.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        dialog.show();
                                    }
                                });

                                layout.addView(button);
                            }
                            String LIMEs_versionName = BuildConfig.VERSION_NAME; // versionNameを取得
                            ScrollView scrollView = new ScrollView(context);
                            scrollView.addView(layout);
                            AlertDialog.Builder builder = new AlertDialog.Builder(context)
                                    .setTitle("LIMEs" + " (" + LIMEs_versionName + ")");
                            builder.setView(scrollView);
                            builder.setPositiveButton(R.string.positive_button, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
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
                                        if (!customPreferences.saveSetting(limeOptions.options[i].name, String.valueOf(isChecked))) {
                                            saveSuccess = false;
                                        }
                                    }

                                    if (!saveSuccess) {
                                        Toast.makeText(context, context.getString(R.string.save_failed), Toast.LENGTH_LONG).show();
                                    }
                                    else if (optionChanged) {
                                        Toast.makeText(context.getApplicationContext(), context.getString(R.string.restarting), Toast.LENGTH_SHORT).show();
                                        Process.killProcess(Process.myPid());
                                        context.startActivity(new Intent().setClassName(Constants.PACKAGE_NAME, "jp.naver.line.android.activity.SplashActivity"));
                                    }
                                }
                            });

                            builder.setNegativeButton(R.string.negative_button, null);

                            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                                @Override
                                public void onDismiss(DialogInterface dialog) {
                                    for (int i = 0; i < limeOptions.options.length; ++i) {
                                        Switch switchView = (Switch) layout.getChildAt(i);
                                        switchView.setChecked(limeOptions.options[i].checked);
                                    }
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
                                layoutParams.topMargin = (int) (statusBarHeight); // Set margin to status bar height
                            } else {
                                layoutParams.topMargin = Utils.dpToPx(5, context); // Set margin to 5dp
                            }
                            button.setLayoutParams(layoutParams);
                            button.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    dialog.show();
                                }
                            });

                            FrameLayout frameLayout = new FrameLayout(context);
                            frameLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                            frameLayout.addView(button);
                            viewGroup.addView(frameLayout);

                    } catch (Exception ignored) {
                        // エラー発生時の処理
                        try {


                            // UIスレッドでToastを表示
                            new Handler(Looper.getMainLooper()).post(() -> {
                                Toast.makeText(contextV,
                                        contextV.getString(R.string.Error_Create_setting_Button) ,
                                        Toast.LENGTH_LONG).show();
                            });
                        } catch (Throwable t) {
                            XposedBridge.log("Toast表示に失敗: " + t);
                        }
                    }
                }
                });

}


    public void CansellNotification(Context context, Context moduleContext) {
        // フォルダのパスを設定
        File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "LimeBackup/Setting");

        // 最初のディレクトリを確認
        if (!dir.exists()) {
            // 次のディレクトリを確認
            dir = new File(Environment.getExternalStorageDirectory(), "Android/data/jp.naver.line.android/LimeBackup/Setting");

            if (!dir.exists()) {
                // 内部ストレージを確認
                dir = new File(context.getFilesDir(), "LimeBackup/Setting");
            }

            // 最終的にディレクトリを作成
            if (!dir.exists() && !dir.mkdirs()) {
                Toast.makeText(context, "Failed to create directory", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // ファイルのパスを設定
        File file = new File(dir, "Notification_Setting.txt");

        // 現在のペアを読み取る
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

        // 入力欄を表示するダイアログを作成
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getString(R.string.NotiFication_Setting));

        // レイアウトを作成
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);

        // グループ名の入力欄
        final EditText groupNameInput = new EditText(context);
        groupNameInput.setHint(context.getString(R.string.GroupName));
        layout.addView(groupNameInput);

        // ユーザー名の入力欄
        final EditText userNameInput = new EditText(context);
        userNameInput.setHint(context.getString(R.string.User_name));
        layout.addView(userNameInput);

        // 追加ボタン
        Button addButton = new Button(context);
        addButton.setText(context.getString(R.string.Add));
        layout.addView(addButton);

        // 追加ボタンのクリックリスナー
        addButton.setOnClickListener(v -> {
            String groupName = groupNameInput.getText().toString();
            String userName = userNameInput.getText().toString();
            String newPair = ": " + groupName + ","+ context.getString(R.string.User_name)+": " + userName;

            // 重複チェック
            if (existingPairs.contains(newPair)) {
                Toast.makeText(context, context.getString(R.string.Aleady_Pair), Toast.LENGTH_SHORT).show();
                return;
            }

            // ファイルに保存
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

        // 現在のペアを表示し、削除できるダイアログ
        builder.setNeutralButton( context.getString(R.string.Registering_Pair), (dialog, which) -> {
            AlertDialog.Builder pairsBuilder = new AlertDialog.Builder(context);
            pairsBuilder.setTitle(context.getString(R.string.Registering_Pair));

            // レイアウトを作成
            LinearLayout pairsLayout = new LinearLayout(context);
            pairsLayout.setOrientation(LinearLayout.VERTICAL);

            for (String pair : existingPairs) {
                // 各ペアの表示用レイアウト
                LinearLayout pairLayout = new LinearLayout(context);
                pairLayout.setOrientation(LinearLayout.HORIZONTAL);

                // ペアのテキストビュー
                TextView pairTextView = new TextView(context);
                pairTextView.setText(pair);
                pairLayout.addView(pairTextView);

                // 削除ボタン
                Button deleteButton = new Button(context);
                deleteButton.setText(context.getString(R.string.Delete_Pair));
                deleteButton.setOnClickListener(v -> {
                    existingPairs.remove(pair);
                    // ファイルを更新
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
                    // ダイアログを再表示
                    pairsBuilder.setMessage(getCurrentPairsMessage(existingPairs,context));
                });
                pairLayout.addView(deleteButton);
                pairsLayout.addView(pairLayout);
            }

            pairsBuilder.setView(pairsLayout);
            pairsBuilder.setPositiveButton( context.getString(R.string.Close), null);
            pairsBuilder.show();
        });

        // キャンセルボタンの設定
        builder.setNegativeButton( context.getString(R.string.Cansel), (dialog, which) -> dialog.cancel());

        // ダイアログを表示
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
// フォルダのパスを設定
        File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "LimeBackup/Setting");

// 最初のディレクトリを確認
        if (!dir.exists()) {
            // 次のディレクトリを確認
            dir = new File(Environment.getExternalStorageDirectory(), "Android/data/jp.naver.line.android/LimeBackup/Setting");

            if (!dir.exists()) {
                // 内部ストレージを確認
                dir = new File(context.getFilesDir(), "LimeBackup/Setting");
            }

            // 最終的にディレクトリを作成
            if (!dir.exists() && !dir.mkdirs()) {
                Toast.makeText(context, "Failed to create directory", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // ファイルのパスを設定
        File file = new File(dir, "canceled_message.txt");

        // ファイルが存在しない場合、デフォルトのメッセージを設定
        if (!file.exists()) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                String defaultMessage = moduleContext.getResources().getString(R.string.canceled_message_txt);
                writer.write(defaultMessage);
            } catch (IOException e) {
                Toast.makeText(context, "Failed to create file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // ファイルの内容を読み取る
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

        // EditText の設定
        final EditText editText = new EditText(context);
        editText.setText(fileContent.toString().trim()); // 不要な改行を削除
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        editText.setMinLines(10);
        editText.setGravity(Gravity.TOP);

        // Save ボタンの設定
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

        // レイアウトの設定
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(editText);
        layout.addView(saveButton);

        // AlertDialog の設定
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
        File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "LimeBackup");
        if (!dir.exists() && !dir.mkdirs()) {
            dir = new File(Environment.getExternalStorageDirectory(), "Android/data/jp.naver.line.android/");
            if (!dir.exists() && !dir.mkdirs()) {
                dir = moduleContext.getFilesDir();
            }
        }
        File file = new File(dir, "margin_settings.txt");

        // 初期値
        float keep_unread_horizontalMarginFactor = 0.5f;
        int keep_unread_verticalMarginDp = 15;
        float read_button_horizontalMarginFactor = 0.6f;
        int read_button_verticalMarginDp = 60;
        float read_checker_horizontalMarginFactor = 0.5f;
        int read_checker_verticalMarginDp = 60;
        float keep_unread_size = 60.0f;
        float chat_unread_size = 30.0f;
        float chat_read_check_size = 80.0f;

        // ファイルの内容を読み込む
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
            // ファイルが存在しない場合は初期値で作成
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

        // レイアウト設定
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(16, 16, 16, 16);

        // 各入力フィールドの作成
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
                // 各入力フィールドの値を取得
                String keepUnreadSizeStr = keepUnreadSizeInput.getText().toString().trim();
                String horizontalMarginStr = horizontalInput.getText().toString().trim();
                String verticalMarginStr = verticalInput.getText().toString().trim();
                String chatUnreadSizeStr = ChatUnreadSizeInput.getText().toString().trim();
                String chatReadCheckSizeStr = ChatReadCheckSizeInput.getText().toString().trim();
                String readButtonHorizontalMarginStr = readButtonHorizontalInput.getText().toString().trim();
                String readButtonVerticalMarginStr = readButtonVerticalInput.getText().toString().trim();
                String readCheckerHorizontalMarginStr = readCheckerHorizontalInput.getText().toString().trim();
                String readCheckerVerticalMarginStr = readCheckerVerticalInput.getText().toString().trim();

                // 入力値の検証
                if (keepUnreadSizeStr.isEmpty() || horizontalMarginStr.isEmpty() || verticalMarginStr.isEmpty() ||
                        chatUnreadSizeStr.isEmpty() || chatReadCheckSizeStr.isEmpty() ||
                        readButtonHorizontalMarginStr.isEmpty() || readButtonVerticalMarginStr.isEmpty() ||
                        readCheckerHorizontalMarginStr.isEmpty() || readCheckerVerticalMarginStr.isEmpty()) {
                    Toast.makeText(context, "All fields must be filled!", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 数値に変換
                float newKeepUnreadSize = Float.parseFloat(keepUnreadSizeStr); // 修正
                float newKeepUnreadHorizontalMarginFactor = Float.parseFloat(horizontalMarginStr);
                int newKeepUnreadVerticalMarginDp = Integer.parseInt(verticalMarginStr);
                float newChatUnreadSize = Float.parseFloat(chatUnreadSizeStr); // 修正
                float newChatReadCheckSize = Float.parseFloat(chatReadCheckSizeStr); // 修正
                float newReadButtonHorizontalMarginFactor = Float.parseFloat(readButtonHorizontalMarginStr);
                int newReadButtonVerticalMarginDp = Integer.parseInt(readButtonVerticalMarginStr);
                float newReadCheckerHorizontalMarginFactor = Float.parseFloat(readCheckerHorizontalMarginStr);
                int newReadCheckerVerticalMarginDp = Integer.parseInt(readCheckerVerticalMarginStr);
                // ファイルに保存
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
            // 確認ダイアログを表示
            new AlertDialog.Builder(context)
                    .setMessage(moduleContext.getResources().getString(R.string.really_delete))
                    .setPositiveButton(moduleContext.getResources().getString(R.string.yes), (dialog, which) -> {
                        try {


                            // ファイルが存在する場合、削除する
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



        // レイアウトを構築
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
        builder.setView(scrollView); // ScrollView をダイアログのビューとして設定
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

        // ファイルが存在する場合、内容を読み込む
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


    private void backupChatHistory(Context appContext,Context moduleContext) {
        File originalDbFile = appContext.getDatabasePath("naver_line");
        File backupDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "LimeBackup");
        if (!backupDir.exists()) {
            if (!backupDir.mkdirs()) {
                return;}}

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String backupFileNameWithTimestamp = "naver_line_backup_" + timeStamp + ".db";
        String backupFileNameFixed = "naver_line_backup.db";
        File backupFileWithTimestamp = new File(backupDir, backupFileNameWithTimestamp);
        File backupFileFixed = new File(backupDir, backupFileNameFixed);

        try (FileChannel source = new FileInputStream(originalDbFile).getChannel()) {
            try (FileChannel destinationWithTimestamp = new FileOutputStream(backupFileWithTimestamp).getChannel()) {
                destinationWithTimestamp.transferFrom(source, 0, source.size());
            }
            source.position(0);
            try (FileChannel destinationFixed = new FileOutputStream(backupFileFixed).getChannel()) {
                destinationFixed.transferFrom(source, 0, source.size());
            }
            Toast.makeText(appContext,moduleContext.getResources().getString(R.string.Talk_Back_up_Success), Toast.LENGTH_SHORT).show();
        } catch (IOException ignored) {
            Toast.makeText(appContext,moduleContext.getResources().getString(R.string.Talk_Back_up_Error), Toast.LENGTH_SHORT).show();
        }
    }
    private void restoreChatHistory(Context context,Context moduleContext) {
        File backupDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "LimeBackup");
        File backupDbFile = new File(backupDir, "naver_line_backup.db");

        if (!backupDbFile.exists()) {
            Toast.makeText(context,moduleContext.getResources().getString(R.string.Backup_file_not_found), Toast.LENGTH_SHORT).show();
            return;
        }
        SQLiteDatabase backupDb = null;
        SQLiteDatabase originalDb = null;
        try {
            backupDb = SQLiteDatabase.openDatabase(backupDbFile.getPath(), null, SQLiteDatabase.OPEN_READONLY);
            originalDb = context.openOrCreateDatabase("naver_line", Context.MODE_PRIVATE, null);
            Cursor cursor = backupDb.rawQuery("SELECT * FROM chat_history", null);
            if (cursor.moveToFirst()) {
                do {
                    String serverId = cursor.getString(cursor.getColumnIndexOrThrow("server_id"));
                    Integer type = cursor.isNull(cursor.getColumnIndexOrThrow("type")) ? null : cursor.getInt(cursor.getColumnIndexOrThrow("type"));
                    String chatId = cursor.getString(cursor.getColumnIndexOrThrow("chat_id"));
                    String fromMid = cursor.getString(cursor.getColumnIndexOrThrow("from_mid"));
                    String content = cursor.getString(cursor.getColumnIndexOrThrow("content"));
                    String createdTime = cursor.getString(cursor.getColumnIndexOrThrow("created_time"));
                    String deliveredTime = cursor.getString(cursor.getColumnIndexOrThrow("delivered_time"));
                    Integer status = cursor.isNull(cursor.getColumnIndexOrThrow("status")) ? null : cursor.getInt(cursor.getColumnIndexOrThrow("status"));
                    Integer sentCount = cursor.isNull(cursor.getColumnIndexOrThrow("sent_count")) ? null : cursor.getInt(cursor.getColumnIndexOrThrow("sent_count"));
                    Integer readCount = cursor.isNull(cursor.getColumnIndexOrThrow("read_count")) ? null : cursor.getInt(cursor.getColumnIndexOrThrow("read_count"));
                    String locationName = cursor.getString(cursor.getColumnIndexOrThrow("location_name"));
                    String locationAddress = cursor.getString(cursor.getColumnIndexOrThrow("location_address"));
                    String locationPhone = cursor.getString(cursor.getColumnIndexOrThrow("location_phone"));
                    Integer locationLatitude = cursor.isNull(cursor.getColumnIndexOrThrow("location_latitude")) ? null : cursor.getInt(cursor.getColumnIndexOrThrow("location_latitude"));
                    Integer locationLongitude = cursor.isNull(cursor.getColumnIndexOrThrow("location_longitude")) ? null : cursor.getInt(cursor.getColumnIndexOrThrow("location_longitude"));
                    Integer attachmentImage = cursor.isNull(cursor.getColumnIndexOrThrow("attachement_image")) ? null : cursor.getInt(cursor.getColumnIndexOrThrow("attachement_image"));
                    Integer attachmentImageHeight = cursor.isNull(cursor.getColumnIndexOrThrow("attachement_image_height")) ? null : cursor.getInt(cursor.getColumnIndexOrThrow("attachement_image_height"));
                    Integer attachmentImageWidth = cursor.isNull(cursor.getColumnIndexOrThrow("attachement_image_width")) ? null : cursor.getInt(cursor.getColumnIndexOrThrow("attachement_image_width"));
                    Integer attachmentImageSize = cursor.isNull(cursor.getColumnIndexOrThrow("attachement_image_size")) ? null : cursor.getInt(cursor.getColumnIndexOrThrow("attachement_image_size"));
                    Integer attachmentType = cursor.isNull(cursor.getColumnIndexOrThrow("attachement_type")) ? null : cursor.getInt(cursor.getColumnIndexOrThrow("attachement_type"));
                    String attachmentLocalUri = cursor.getString(cursor.getColumnIndexOrThrow("attachement_local_uri"));
                    String parameter = cursor.getString(cursor.getColumnIndexOrThrow("parameter"));
                    byte[] chunks = cursor.getBlob(cursor.getColumnIndexOrThrow("chunks"));

                    if (serverId == null) {continue;}
                    Cursor existingCursor = originalDb.rawQuery("SELECT 1 FROM chat_history WHERE server_id = ?", new String[]{serverId});
                    boolean recordExists = existingCursor.moveToFirst();
                    existingCursor.close();

                    if (recordExists) {continue;}

                    ContentValues values = new ContentValues();
                    values.put("server_id", serverId);
                    values.put("type", type);
                    values.put("chat_id", chatId);
                    values.put("from_mid", fromMid);
                    values.put("content", content);
                    values.put("created_time", createdTime);
                    values.put("delivered_time", deliveredTime);
                    values.put("status", status);
                    values.put("sent_count", sentCount);
                    values.put("read_count", readCount);
                    values.put("location_name", locationName);
                    values.put("location_address", locationAddress);
                    values.put("location_phone", locationPhone);
                    values.put("location_latitude", locationLatitude);
                    values.put("location_longitude", locationLongitude);
                    values.put("attachement_image", attachmentImage);
                    values.put("attachement_image_height", attachmentImageHeight);
                    values.put("attachement_image_width", attachmentImageWidth);
                    values.put("attachement_image_size", attachmentImageSize);
                    values.put("attachement_type", attachmentType);
                    values.put("attachement_local_uri", attachmentLocalUri);
                    values.put("parameter", parameter);
                    values.put("chunks", chunks);

                    originalDb.insertWithOnConflict("chat_history", null, values, SQLiteDatabase.CONFLICT_IGNORE);
                } while (cursor.moveToNext());
            }
            cursor.close();
            Toast.makeText(context,moduleContext.getResources().getString(R.string.Restore_Success), Toast.LENGTH_SHORT).show();
            restoreChat(context,moduleContext);
        } catch (Exception ignored) {
            Toast.makeText(context,moduleContext.getResources().getString(R.string.Restore_Error), Toast.LENGTH_SHORT).show();
        }

    }
    private void restoreChat(Context context,Context moduleContext) {
        File backupDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "LimeBackup");
        File backupDbFile = new File(backupDir, "naver_line_backup.db");

        if (!backupDbFile.exists()) {
            File alternativeDir = new File(Environment.getExternalStorageDirectory(), "Android/data/jp.naver.line.android/");
            File alternativeDbFile = new File(alternativeDir, "naver_line_backup.db");

            if (!alternativeDbFile.exists()) {
                Toast.makeText(context, moduleContext.getResources().getString(R.string.Backup_file_not_found), Toast.LENGTH_SHORT).show();
                return;
            } else {
                backupDbFile = alternativeDbFile;
            }
        }
        SQLiteDatabase backupDb = null;
        SQLiteDatabase originalDb = null;
        try {
            backupDb = SQLiteDatabase.openDatabase(backupDbFile.getPath(), null, SQLiteDatabase.OPEN_READONLY);
            originalDb = context.openOrCreateDatabase("naver_line", Context.MODE_PRIVATE, null);

            Cursor cursor = backupDb.rawQuery("SELECT * FROM chat", null);
            if (cursor.moveToFirst()) {
                do {
                    String chatId = cursor.getString(cursor.getColumnIndexOrThrow("chat_id"));
                    String chatName = cursor.getString(cursor.getColumnIndexOrThrow("chat_name"));
                    String ownerMid = cursor.getString(cursor.getColumnIndexOrThrow("owner_mid"));
                    String lastFromMid = cursor.getString(cursor.getColumnIndexOrThrow("last_from_mid"));
                    String lastMessage = cursor.getString(cursor.getColumnIndexOrThrow("last_message"));
                    String lastCreatedTime = cursor.getString(cursor.getColumnIndexOrThrow("last_created_time"));
                    Integer messageCount = cursor.isNull(cursor.getColumnIndexOrThrow("message_count")) ? null : cursor.getInt(cursor.getColumnIndexOrThrow("message_count"));
                    Integer readMessageCount = cursor.isNull(cursor.getColumnIndexOrThrow("read_message_count")) ? null : cursor.getInt(cursor.getColumnIndexOrThrow("read_message_count"));
                    Integer latestMentionedPosition = cursor.isNull(cursor.getColumnIndexOrThrow("latest_mentioned_position")) ? null : cursor.getInt(cursor.getColumnIndexOrThrow("latest_mentioned_position"));
                    Integer type = cursor.isNull(cursor.getColumnIndexOrThrow("type")) ? null : cursor.getInt(cursor.getColumnIndexOrThrow("type"));
                    Integer isNotification = cursor.isNull(cursor.getColumnIndexOrThrow("is_notification")) ? null : cursor.getInt(cursor.getColumnIndexOrThrow("is_notification"));
                    String skinKey = cursor.getString(cursor.getColumnIndexOrThrow("skin_key"));
                    String inputText = cursor.getString(cursor.getColumnIndexOrThrow("input_text"));
                    String inputTextMetadata = cursor.getString(cursor.getColumnIndexOrThrow("input_text_metadata"));
                    Integer hideMember = cursor.isNull(cursor.getColumnIndexOrThrow("hide_member")) ? null : cursor.getInt(cursor.getColumnIndexOrThrow("hide_member"));
                    Integer pTimer = cursor.isNull(cursor.getColumnIndexOrThrow("p_timer")) ? null : cursor.getInt(cursor.getColumnIndexOrThrow("p_timer"));
                    String lastMessageDisplayTime = cursor.getString(cursor.getColumnIndexOrThrow("last_message_display_time"));
                    String midP = cursor.getString(cursor.getColumnIndexOrThrow("mid_p"));
                    Integer isArchived = cursor.isNull(cursor.getColumnIndexOrThrow("is_archived")) ? null : cursor.getInt(cursor.getColumnIndexOrThrow("is_archived"));
                    String readUp = cursor.getString(cursor.getColumnIndexOrThrow("read_up"));
                    Integer isGroupCalling = cursor.isNull(cursor.getColumnIndexOrThrow("is_groupcalling")) ? null : cursor.getInt(cursor.getColumnIndexOrThrow("is_groupcalling"));
                    Integer latestAnnouncementSeq = cursor.isNull(cursor.getColumnIndexOrThrow("latest_announcement_seq")) ? null : cursor.getInt(cursor.getColumnIndexOrThrow("latest_announcement_seq"));
                    Integer announcementViewStatus = cursor.isNull(cursor.getColumnIndexOrThrow("announcement_view_status")) ? null : cursor.getInt(cursor.getColumnIndexOrThrow("announcement_view_status"));
                    String lastMessageMetaData = cursor.getString(cursor.getColumnIndexOrThrow("last_message_meta_data"));
                    String chatRoomBgmData = cursor.getString(cursor.getColumnIndexOrThrow("chat_room_bgm_data"));
                    Integer chatRoomBgmChecked = cursor.isNull(cursor.getColumnIndexOrThrow("chat_room_bgm_checked")) ? null : cursor.getInt(cursor.getColumnIndexOrThrow("chat_room_bgm_checked"));
                    Integer chatRoomShouldShowBgmBadge = cursor.isNull(cursor.getColumnIndexOrThrow("chat_room_should_show_bgm_badge")) ? null : cursor.getInt(cursor.getColumnIndexOrThrow("chat_room_should_show_bgm_badge"));
                    String unreadTypeAndCount = cursor.getString(cursor.getColumnIndexOrThrow("unread_type_and_count"));

                    if (chatId == null) {continue;}


                    ContentValues values = new ContentValues();
                    values.put("chat_id", chatId);
                    values.put("chat_name", chatName);
                    values.put("owner_mid", ownerMid);
                    values.put("last_from_mid", lastFromMid);
                    values.put("last_message", lastMessage);
                    values.put("last_created_time", lastCreatedTime);
                    values.put("message_count", messageCount);
                    values.put("read_message_count", readMessageCount);
                    values.put("latest_mentioned_position", latestMentionedPosition);
                    values.put("type", type);
                    values.put("is_notification", isNotification);
                    values.put("skin_key", skinKey);
                    values.put("input_text", inputText);
                    values.put("input_text_metadata", inputTextMetadata);
                    values.put("hide_member", hideMember);
                    values.put("p_timer", pTimer);
                    values.put("last_message_display_time", lastMessageDisplayTime);
                    values.put("mid_p", midP);
                    values.put("is_archived", isArchived);
                    values.put("read_up", readUp);
                    values.put("is_groupcalling", isGroupCalling);
                    values.put("latest_announcement_seq", latestAnnouncementSeq);
                    values.put("announcement_view_status", announcementViewStatus);
                    values.put("last_message_meta_data", lastMessageMetaData);
                    values.put("chat_room_bgm_data", chatRoomBgmData);
                    values.put("chat_room_bgm_checked", chatRoomBgmChecked);
                    values.put("chat_room_should_show_bgm_badge", chatRoomShouldShowBgmBadge);
                    values.put("unread_type_and_count", unreadTypeAndCount);

                    originalDb.insertWithOnConflict("chat", null, values, SQLiteDatabase.CONFLICT_IGNORE);
                } while (cursor.moveToNext());
            }
            cursor.close();

            Toast.makeText(context,moduleContext.getResources().getString(R.string.Restore_Chat_Table_Success), Toast.LENGTH_SHORT).show();
        } catch (Exception ignored) {
            Toast.makeText(context,moduleContext.getResources().getString(R.string.Restore_Chat_Table_Error), Toast.LENGTH_SHORT).show();

        } finally {
            if (backupDb != null) {
                backupDb.close();
            }
            if (originalDb != null) {
                originalDb.close();
            }
        }
    }

    private void backupChatsFolder(Context context,Context moduleContext) {
        File originalChatsDir = new File(Environment.getExternalStorageDirectory(), "Android/data/jp.naver.line.android/files/chats");
        File backupDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "LimeBackup");

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
        File backupDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "LimeBackup/chats_backup");
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

}