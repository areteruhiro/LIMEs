package io.github.hiro.lime.hooks;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.hiro.lime.LimeOptions;
import io.github.hiro.lime.R;
import io.github.hiro.lime.Utils;

public class AddRegistrationOptions implements IHook {

    private Switch switchAndroidSecondary;

    @Override
    public void hook(LimeOptions limeOptions, XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {

        XposedBridge.hookAllMethods(
                loadPackageParam.classLoader.loadClass("com.linecorp.registration.ui.fragment.WelcomeFragment"),
                "onViewCreated",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Context context = (Context) XposedHelpers.callMethod(XposedHelpers.callStaticMethod(
                                XposedHelpers.findClass("android.app.ActivityThread", null),
                                "currentActivityThread"
                        ), "getSystemContext");
                        ViewGroup viewGroup = (ViewGroup) ((ViewGroup) param.args[0]).getChildAt(0);
                        Activity activity = (Activity) viewGroup.getContext();
                        Utils.addModuleAssetPath(activity);

                        FrameLayout frameLayout = new FrameLayout(activity);
                        frameLayout.setLayoutParams(new ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT));

                        LinearLayout linearLayout = new LinearLayout(activity);
                        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.WRAP_CONTENT,
                                FrameLayout.LayoutParams.WRAP_CONTENT);
                        layoutParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
                        layoutParams.topMargin = Utils.dpToPx(60, activity);
                        linearLayout.setLayoutParams(layoutParams);
                        linearLayout.setOrientation(LinearLayout.VERTICAL);

                        // Android ID偽装スイッチ
                        Switch switchSpoofAndroidId = new Switch(activity);
                        switchSpoofAndroidId.setText(R.string.switch_spoof_android_id);
                        try {
                            switchSpoofAndroidId.setChecked(Boolean.parseBoolean(
                                    new CustomPreferences(context).getSetting("spoof_android_id", "false")));
                        } catch (PackageManager.NameNotFoundException e) {
                            handlePreferencesError(activity, e);
                        }
                        switchSpoofAndroidId.setOnCheckedChangeListener((buttonView, isChecked) -> {
                            if (isChecked) {
                                showSpoofAndroidIdDialog(activity,context);
                            } else {
                                try {
                                    new CustomPreferences(context).saveSetting("spoof_android_id", "false");
                                    showRefreshToast(activity);
                                } catch (PackageManager.NameNotFoundException e) {
                                    handlePreferencesError(activity, e);
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        });

                        // Android Secondaryスイッチ
                        switchAndroidSecondary = new Switch(activity);
                        switchAndroidSecondary.setText(R.string.switch_android_secondary);
                        try {
                            switchAndroidSecondary.setChecked(Boolean.parseBoolean(
                                    new CustomPreferences(context).getSetting("android_secondary", "false")));
                        } catch (PackageManager.NameNotFoundException e) {
                            handlePreferencesError(activity, e);
                        }
                        switchAndroidSecondary.setOnCheckedChangeListener((buttonView, isChecked) -> {
                            if (isChecked) {
                                showSpoofVersionIdDialog(activity,context);
                            } else {
                                try {
                                    new CustomPreferences(context).saveSetting("android_secondary", "false");
                                } catch (PackageManager.NameNotFoundException e) {
                                    handlePreferencesError(activity, e);
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        });

                        linearLayout.addView(switchSpoofAndroidId);
                        linearLayout.addView(switchAndroidSecondary);
                        frameLayout.addView(linearLayout);
                        viewGroup.addView(frameLayout);
                    }
                }
        );
    }

    private void showSpoofVersionIdDialog(Activity activity,Context context) {

        AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                .setTitle(R.string.options_title);

        LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(Utils.dpToPx(20, activity), Utils.dpToPx(20, activity), Utils.dpToPx(20, activity), Utils.dpToPx(20, activity));

        TextView textView = new TextView(activity);
        textView.setText(R.string.spoof_version_id_risk);
        layout.addView(textView);

        EditText editTextDeviceName = new EditText(activity);
        editTextDeviceName.setHint(R.string.spoof_device_name);
        try {
            editTextDeviceName.setText(new CustomPreferences(context).getSetting("device_name", "ANDROID"));
        } catch (PackageManager.NameNotFoundException e) {
            handlePreferencesError(activity, e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        layout.addView(editTextDeviceName);

        EditText editTextOsName = new EditText(activity);
        editTextOsName.setHint(R.string.spoof_os_name);
        try {
            editTextOsName.setText(new CustomPreferences(context).getSetting("os_name", "Android OS"));
        } catch (PackageManager.NameNotFoundException e) {
            handlePreferencesError(activity, e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        layout.addView(editTextOsName);

        EditText editTextOsVersion = new EditText(activity);
        editTextOsVersion.setHint(R.string.spoof_os_version);
        try {
            editTextOsVersion.setText(new CustomPreferences(context).getSetting("os_version", "14"));
        } catch (PackageManager.NameNotFoundException e) {
            handlePreferencesError(activity, e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        layout.addView(editTextOsVersion);

        EditText editTextAndroidVersion = new EditText(activity);
        editTextAndroidVersion.setHint(R.string.spoof_android_version);
        try {
            editTextAndroidVersion.setText(new CustomPreferences(context).getSetting("android_version", "14.16.0"));
        } catch (PackageManager.NameNotFoundException e) {
            handlePreferencesError(activity, e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        layout.addView(editTextAndroidVersion);

        builder.setView(layout);
        builder.setPositiveButton(R.string.positive_button, (dialog, which) -> {
            try {
                CustomPreferences prefs = new CustomPreferences(context);
                prefs.saveSetting("android_secondary", "true");
                prefs.saveSetting("device_name", editTextDeviceName.getText().toString());
                prefs.saveSetting("os_name", editTextOsName.getText().toString());
                prefs.saveSetting("os_version", editTextOsVersion.getText().toString());
                prefs.saveSetting("android_version", editTextAndroidVersion.getText().toString());
                switchAndroidSecondary.setChecked(true);
                showRefreshToast(activity);
            } catch (PackageManager.NameNotFoundException e) {
                handlePreferencesError(activity, e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        builder.setNegativeButton(R.string.negative_button, null);
        builder.show();
    }

    private void showSpoofAndroidIdDialog(Activity activity,Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                .setTitle(R.string.options_title);

        TextView textView = new TextView(activity);
        textView.setText(R.string.spoof_android_id_risk);
        textView.setPadding(Utils.dpToPx(20, activity), Utils.dpToPx(20, activity), Utils.dpToPx(20, activity), Utils.dpToPx(20, activity));
        builder.setView(textView);

        builder.setPositiveButton(R.string.positive_button, (dialog, which) -> {
            try {
                new CustomPreferences(context).saveSetting("spoof_android_id", "true");
                showRefreshToast(activity);
            } catch (PackageManager.NameNotFoundException e) {
                handlePreferencesError(activity, e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        builder.setNegativeButton(R.string.negative_button, null);
        builder.show();
    }

    private void handlePreferencesError(Activity activity, Exception e) {
        XposedBridge.log(e);
        Toast.makeText(activity, "Failed to access settings", Toast.LENGTH_LONG).show();
    }

    private void showRefreshToast(Activity activity) {
        Toast.makeText(activity.getApplicationContext(), activity.getString(R.string.need_refresh), Toast.LENGTH_SHORT).show();
        activity.finish();
    }
}