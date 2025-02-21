package io.github.hiro.lime;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.InputType;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.Manifest;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import io.github.hiro.lime.hooks.CustomPreferences;

public class MainActivity extends Activity {
    public LimeOptions limeOptions = new LimeOptions();
    private static final int REQUEST_CODE = 100;

    @Deprecated
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            } else {
                try {
                    initializeApp();
                } catch (PackageManager.NameNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_CODE
                );
            } else {
                try {
                    initializeApp();
                } catch (PackageManager.NameNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                try {
                    initializeApp();
                } catch (PackageManager.NameNotFoundException e) {
                    throw new RuntimeException(e);
                }
            } else {
                Toast.makeText(this, "ストレージアクセス権限が必要です", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private void initializeApp() throws PackageManager.NameNotFoundException {
        CustomPreferences customPrefs = new CustomPreferences();
        Set<String> addedOptionNames = new LinkedHashSet<>();
        List<LimeOptions.Option> uniqueOptions = new ArrayList<>();
        for (LimeOptions.Option option : limeOptions.options) {
            if (!addedOptionNames.contains(option.name)) {
                uniqueOptions.add(option);
                addedOptionNames.add(option.name);
            }
        }
        limeOptions.options = uniqueOptions.toArray(new LimeOptions.Option[0]);

        for (LimeOptions.Option option : limeOptions.options) {
            String savedValue = customPrefs.getSetting(option.name, String.valueOf(option.checked));
            option.checked = savedValue != null ? Boolean.parseBoolean(savedValue) : option.checked;
        }

        ScrollView mainScrollView = new ScrollView(this);
        mainScrollView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        int padding = Utils.dpToPx(20, this);
        mainLayout.setPadding(padding, padding, padding, padding);

        Switch switchRedirectWebView = null;
        boolean isAndroid15OrHigher = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE;
        boolean firstOptionHandled = false;

        for (LimeOptions.Option option : limeOptions.options) {
            final String name = option.name;
            if (isAndroid15OrHigher && !firstOptionHandled) {
                addAndroid15FirstOption(mainLayout, option);
                firstOptionHandled = true;
            }

            Switch switchView = createSwitchView(option);
            configureSwitchLayout(switchView, firstOptionHandled);

            handleDependencies(switchView, name, switchRedirectWebView);

            mainLayout.addView(switchView);
            if (isAndroid15OrHigher && !firstOptionHandled) {
                mainLayout.addView(createDummyView());
            }

            if ("redirect_webview".equals(name)) {
                switchRedirectWebView = switchView;
            }
        }
        addModifySection(mainLayout, customPrefs, "request", R.string.modify_request, "encoded_js_modify_request");
        addModifySection(mainLayout, customPrefs, "response", R.string.modify_response, "encoded_js_modify_response");

        mainScrollView.addView(mainLayout);
        ViewGroup rootView = findViewById(android.R.id.content);
        rootView.addView(mainScrollView);

    }
    private Switch createSwitchView(LimeOptions.Option option) {
        Switch switchView = new Switch(this);
        try {
            switchView.setText(getString(option.id));
        } catch (Resources.NotFoundException e) {
            switchView.setText(option.name);
        }
        return switchView;
    }
    private void handleDependencies(Switch currentSwitch, String optionName, Switch redirectWebViewSwitch) {
        if ("open_in_browser".equals(optionName) && redirectWebViewSwitch != null) {
            redirectWebViewSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                currentSwitch.setEnabled(isChecked);
                if (!isChecked) {
                    currentSwitch.setChecked(false);
                }
            });

            currentSwitch.setEnabled(redirectWebViewSwitch.isChecked());
            if (!redirectWebViewSwitch.isChecked()) {
                currentSwitch.setChecked(false);
            }
        }

    }

    private void configureSwitchLayout(Switch switchView, boolean isFirst) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );

        params.topMargin = isFirst ? Utils.dpToPx(2, this) : Utils.dpToPx(20, this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            params.topMargin = Utils.dpToPx(1, this);
            switchView.setElevation(Utils.dpToPx(2, this));
        }

        switchView.setLayoutParams(params);
        switchView.setVisibility(View.VISIBLE);
    }

    private void addAndroid15FirstOption(LinearLayout layout, LimeOptions.Option option) {
        Switch mainSwitch = createSwitchView(option);
        Switch dummySwitch = createSwitchView(option);
        dummySwitch.setVisibility(View.INVISIBLE);
        dummySwitch.setEnabled(false);
        LinearLayout.LayoutParams dummyParams = new LinearLayout.LayoutParams(0, 0);
        dummySwitch.setLayoutParams(dummyParams);
        layout.addView(mainSwitch);
        layout.addView(dummySwitch);
    }

    private View createDummyView() {
        View dummy = new View(this);
        dummy.setLayoutParams(new LinearLayout.LayoutParams(0, 0));
        return dummy;
    }

    private void debugLayout(ScrollView scrollView, LinearLayout layout) {
        scrollView.post(() -> {
            for (int i = 0; i < layout.getChildCount(); i++) {
                View child = layout.getChildAt(i);

            }
        });
    }
    private void addModifySection(LinearLayout mainLayout, CustomPreferences prefs, String type, int titleRes, String prefKey) {
        Context context = this;

        Button button = new Button(context);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = Utils.dpToPx(20, context);
        button.setLayoutParams(params);
        button.setText(titleRes);

        button.setOnClickListener(v -> showModifyDialog(prefs, context, titleRes, prefKey));
        mainLayout.addView(button);
    }

    private void showModifyDialog(CustomPreferences prefs, Context context, int titleRes, String prefKey) {
        LinearLayout dialogLayout = new LinearLayout(context);
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        int padding = Utils.dpToPx(20, context);
        dialogLayout.setPadding(padding, padding, padding, padding);

        EditText editText = new EditText(context);
        editText.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                Utils.dpToPx(150, context)));
        editText.setTypeface(Typeface.MONOSPACE);
        editText.setInputType(InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        editText.setText(new String(Base64.decode(prefs.getSetting(prefKey, ""), Base64.NO_WRAP)));
        LinearLayout buttonLayout = new LinearLayout(context);
        buttonLayout.setOrientation(LinearLayout.HORIZONTAL);

        Button copyButton = new Button(context);
        copyButton.setText(R.string.button_copy);
        copyButton.setOnClickListener(v -> copyToClipboard(editText.getText().toString()));

        Button pasteButton = new Button(context);
        pasteButton.setText(R.string.button_paste);
        pasteButton.setOnClickListener(v -> editText.setText(getClipboardText()));

        buttonLayout.addView(copyButton);
        buttonLayout.addView(pasteButton);

        dialogLayout.addView(editText);
        dialogLayout.addView(buttonLayout);

        new AlertDialog.Builder(context)
                .setTitle(titleRes)
                .setView(dialogLayout)
                .setPositiveButton(R.string.positive_button, (dialog, which) -> {
                    String encoded = Base64.encodeToString(editText.getText().toString().getBytes(), Base64.NO_WRAP);
                    prefs.saveSetting(prefKey, encoded);
                })
                .setNegativeButton(R.string.negative_button, null)
                .setOnDismissListener(dialog -> {
                    editText.setText(new String(Base64.decode(prefs.getSetting(prefKey, ""), Base64.NO_WRAP)));
                })
                .show();
    }

    private void copyToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (clipboard != null) {
            ClipData clip = ClipData.newPlainText("Copied Text", text);
            clipboard.setPrimaryClip(clip);
        }
    }

    private String getClipboardText() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (clipboard != null && clipboard.hasPrimaryClip()) {
            ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
            return item.getText().toString();
        }
        return "";
    }
    private void showModuleNotEnabledAlert() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.module_not_enabled_title))
                .setMessage(getString(R.string.module_not_enabled_text))
                .setPositiveButton(getString(R.string.positive_button), (dialog, which) -> finishAndRemoveTask())
                .setCancelable(false)
                .show();
    }
}