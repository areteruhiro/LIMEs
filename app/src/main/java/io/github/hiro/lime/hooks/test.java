package io.github.hiro.lime.hooks;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewStub;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;


import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import dalvik.system.DexFile;
import io.github.hiro.lime.LimeOptions;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class test implements IHook {
    private boolean isButtonAdded = false; // ボタンが追加されたかどうかを追跡するフラグ
    @Override
    public void hook(LimeOptions limeOptions, XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        String packageName = loadPackageParam.packageName;
        XposedHelpers.findAndHookMethod(
                View.class,
                "onAttachedToWindow",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        View view = (View) param.thisObject;
                        String className = view.getClass().getName();
                        if (!"jp.naver.line.android.common.view.header.HeaderButton".equals(className)) return;

                        XposedBridge.log("[HeaderButton] Attached: " + className);
                        if (view instanceof ViewGroup) {
                            analyzeHeaderButtonChildren((ViewGroup) view);
                        }
                    }

                    private void analyzeHeaderButtonChildren(ViewGroup headerButton) {
                        for (int i = 0; i < headerButton.getChildCount(); i++) {
                            View child = headerButton.getChildAt(i);
                            int id = child.getId();
                            String idName = "(no id)";
                            try {
                                if (id != View.NO_ID) {
                                    idName = child.getContext().getResources().getResourceName(id);
                                }
                            } catch (Resources.NotFoundException ignored) {}

                            XposedBridge.log("[HeaderButton] Child: id=" + idName + ", class=" + child.getClass().getName());

                            if (child instanceof ImageView) {
                                Drawable drawable = ((ImageView) child).getDrawable();
                                if (drawable != null) {
                                    XposedBridge.log("[HeaderButton] → Drawable class: " + drawable.getClass().getName());
                                }

                                try {
                                    Field mResourceField = ImageView.class.getDeclaredField("mResource");
                                    mResourceField.setAccessible(true);
                                    int resId = mResourceField.getInt(child);
                                    if (resId != 0) {
                                        String resName = child.getContext().getResources().getResourceName(resId);
                                        XposedBridge.log("[HeaderButton] → Image Resource: " + resName);
                                    }
                                } catch (Throwable ignored) {}
                            }

                            if (child instanceof TextView) {
                                CharSequence text = ((TextView) child).getText();
                                XposedBridge.log("[HeaderButton] → Text: " + text);
                            }

                            // ネストされたViewも探索
                            if (child instanceof ViewGroup) {
                                analyzeHeaderButtonChildren((ViewGroup) child);
                            }
                        }
                    }

                }
        );


        XposedHelpers.findAndHookMethod(
                View.class,
                "performClick",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        View view = (View) param.thisObject;
                        int id = view.getId();
                        String resourceName = "(no id)";
                        try {
                            if (id != View.NO_ID) {
                                resourceName = view.getContext().getResources().getResourceName(id);
                            }
                        } catch (Resources.NotFoundException e) {
                            resourceName = "(unknown id " + id + ")";
                        }

                        String viewClass = view.getClass().getName();
                        String parentClass = view.getParent() != null ? view.getParent().getClass().getName() : "null";

                        XposedBridge.log("[Click::performClick] View clicked:");
                        XposedBridge.log("  - Resource ID: " + id + ", Name: " + resourceName);
                        XposedBridge.log("  - View Class: " + viewClass);
                        XposedBridge.log("  - Parent Class: " + parentClass);
                    }
                }
        );


        XposedHelpers.findAndHookMethod(
                View.class,
                "setOnClickListener",
                View.OnClickListener.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        View.OnClickListener originalListener = (View.OnClickListener) param.args[0];

                        if (originalListener == null) return;
                        View.OnClickListener wrappedListener = new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                try {
                                    Context context = v.getContext();
                                    int id = v.getId();
                                    String resourceName = "(no id)";
                                    if (id != View.NO_ID) {
                                        try {
                                            resourceName = context.getResources().getResourceName(id);
                                        } catch (Resources.NotFoundException e) {
                                            resourceName = "(unknown id " + id + ")";
                                        }
                                    }

                                    String viewClass = v.getClass().getName();
                                    String parentClass = (v.getParent() != null) ? v.getParent().getClass().getName() : "null";

                                    XposedBridge.log("[Click] View clicked:");
                                    XposedBridge.log("  - Resource ID: " + id + ", Name: " + resourceName);
                                    XposedBridge.log("  - View Class: " + viewClass);
                                    XposedBridge.log("  - Parent Class: " + parentClass);

                                } catch (Throwable t) {
                                    XposedBridge.log("Error logging click info: " + Log.getStackTraceString(t));
                                }
                                originalListener.onClick(v);
                            }
                        };
                        param.args[0] = wrappedListener;
                    }
                }
        );


//        XposedHelpers.findAndHookMethod(
//                Activity.class,
//                "onResume",
//                new XC_MethodHook() {
//                    @Override
//                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//                        final Activity activity = (Activity) param.thisObject;
//
//                        // 画面描画が安定するのを待ってから再帰走査
//                        activity.getWindow().getDecorView().postDelayed(() -> {
//                            View rootView = activity.getWindow().getDecorView().getRootView();
//                            XposedBridge.log("[ViewScan] Dumping view tree for: " + activity.getClass().getName());
//                            scanViewHierarchy(rootView);  // ここでView再帰スキャン
//                        }, 1500);  // 1.5秒後（必要なら調整）
//                    }
//                }
//        );



//        hookFragmentOnCreateView(loadPackageParam.classLoader);
        //hookChatHistoryActivity(loadPackageParam.classLoader); // ChatHistoryActivityのフック
        //hookLongClickListeners(loadPackageParam.classLoader); // 長押しリスナーのフック

//        // System.currentTimeMillis() をフック
//        XposedBridge.hookAllMethods(
//                java.lang.System.class,
//                "currentTimeMillis",
//                new XC_MethodHook() {
//                    @Override
//                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
//                        // フックが呼ばれたことをログ出力
//                        XposedBridge.log("System.currentTimeMillis() called");
//                    }
//
//                    @Override
//                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//                        // 元の戻り値を取得
//                        long originalResult = (long) param.getResult();
//
//                        // スタックトレースを取得
//                        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
//
//                        // ログに出力
//                        StringBuilder sb = new StringBuilder();
//                        sb.append("System.currentTimeMillis() returned: ").append(originalResult).append("\n");
//                        sb.append("Stack trace:\n");
//
//                        // スタックトレースをフォーマット (上位10フレームまで)
//                        for (int i = 0; i < Math.min(stackTrace.length, 80); i++) {
//                            StackTraceElement element = stackTrace[i];
//                            sb.append("  at ")
//                                    .append(element.getClassName())
//                                    .append(".")
//                                    .append(element.getMethodName())
//                                    .append("(")
//                                    .append(element.getFileName())
//                                    .append(":")
//                                    .append(element.getLineNumber())
//                                    .append(")\n");
//                        }
//
//                        XposedBridge.log(sb.toString());
//                    }
//                }
//        );



        Class<?> targetClass = loadPackageParam.classLoader.loadClass("jp.naver.gallery.viewer.g");
        Method[] methods = targetClass.getDeclaredMethods();

        for (Method method : methods) {
            XposedBridge.hookMethod(method, new XC_MethodHook() {

                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            Object arg0 = param.args[0];
                            String methodName = method.getName();

                            XposedBridge.log("TEST"+methodName+arg0);
                        }
                    }
            );
        }
    }

    private void logStackTrace(String methodName) {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        StringBuilder sb = new StringBuilder();
        sb.append("Stack trace for MP.h.").append(methodName).append(":\n");

        for (int i = 3; i < Math.min(stackTrace.length, 10); i++) {
            StackTraceElement element = stackTrace[i];
            sb.append("  at ")
                    .append(element.getClassName())
                    .append(".")
                    .append(element.getMethodName())
                    .append("(")
                    .append(element.getFileName())
                    .append(":")
                    .append(element.getLineNumber())
                    .append(")\n");
        }

        XposedBridge.log(sb.toString());
    }
    private void hookAllClassesInPackage(ClassLoader classLoader, XC_LoadPackage.LoadPackageParam loadPackageParam) {
        try {
            String apkPath = loadPackageParam.appInfo.sourceDir;
            if (apkPath == null) {
                XposedBridge.log("Could not get APK path.");
                return;
            }

            DexFile dexFile = new DexFile(new File(apkPath));
            Enumeration<String> classNames = dexFile.entries();
            while (classNames.hasMoreElements()) {
                String className = classNames.nextElement();
                //  if (className.startsWith("com.linecorp.line") || className.startsWith("jp.naver.line.android")) {
                try {
                    Class<?> clazz = Class.forName(className, false, classLoader);
                    hookAllMethods(clazz);
                } catch (ClassNotFoundException e) {
                    XposedBridge.log("Class not found: " + className);
                } catch (Throwable e) {
                    XposedBridge.log("Error loading class " + className + ": " + e.getMessage());
                }
                //  }
            }
        } catch (Throwable e) {
            XposedBridge.log("Error while hooking classes: " + e.getMessage());
        }
    }

    private void hookAllClasses(ClassLoader classLoader, XC_LoadPackage.LoadPackageParam loadPackageParam) {
        try {
            String apkPath = loadPackageParam.appInfo.sourceDir;
            if (apkPath == null) {
                XposedBridge.log("Could not get APK path.");
                return;
            }

            DexFile dexFile = new DexFile(new File(apkPath));
            Enumeration<String> classNames = dexFile.entries();
            while (classNames.hasMoreElements()) {
                String className = classNames.nextElement();
                try {
                    Class<?> clazz = Class.forName(className, false, classLoader);
                    hookAllMethods(clazz);
                } catch (ClassNotFoundException e) {
                    XposedBridge.log("Class not found: " + className);
                } catch (Throwable e) {
                    XposedBridge.log("Error loading class " + className + ": " + e.getMessage());
                }
            }
        } catch (Throwable e) {
            XposedBridge.log("Error while hooking classes: " + e.getMessage());
        }
    }


    private void hookFragmentOnCreateView(ClassLoader classLoader) {
        try {
            Class<?> fragmentClass = Class.forName("androidx.fragment.app.Fragment", false, classLoader);
            Method onCreateViewMethod = fragmentClass.getDeclaredMethod("onCreateView", LayoutInflater.class, ViewGroup.class, Bundle.class);

            XposedBridge.hookMethod(onCreateViewMethod, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    XposedBridge.log("Before calling: " + fragmentClass.getName() + ".onCreateView");
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    ViewGroup rootView = (ViewGroup) param.getResult(); // ルートのViewGroupを取得
                    Context context = rootView.getContext();

                    // IDによるビューの取得
                    int messageContainerId = getIdByName(context, "message_context_menu_content_container");
                    View messageContainer = rootView.findViewById(messageContainerId);

                    if (messageContainer != null) {
                        XposedBridge.log("messageContainer found: " + messageContainer.toString());
                    } else {
                        XposedBridge.log("messageContainer not found");
                    }
                    rootView.setOnHierarchyChangeListener(new ViewGroup.OnHierarchyChangeListener() {
                        @Override
                        public void onChildViewAdded(View parent, View child) {
                            XposedBridge.log("Child view added: " + child.toString());
                        }

                        @Override
                        public void onChildViewRemoved(View parent, View child) {
                            XposedBridge.log("Child view removed: " + child.toString());
                        }
                    });
                }
            });
        } catch (ClassNotFoundException e) {
            XposedBridge.log("Class not found: androidx.fragment.app.Fragment");
        } catch (NoSuchMethodException e) {
            XposedBridge.log("Method not found: onCreateView in Fragment");
        } catch (Throwable e) {
            XposedBridge.log("Error hooking onCreateView: " + e.getMessage());
        }
    }


    private void hookOnViewAdded(ClassLoader classLoader) {
        try {
            Class<?> constraintLayoutClass = Class.forName("androidx.constraintlayout.widget.ConstraintLayout", false, classLoader);
            Method onViewAddedMethod = constraintLayoutClass.getDeclaredMethod("onViewAdded", View.class);

            findAndHookMethod(
                    View.class,
                    "onAttachedToWindow",
                    new XC_MethodHook() {
                        View view;
                        @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    View addedView = (View) param.args[0];
                    XposedBridge.log("Called: " + constraintLayoutClass.getName() + ".onViewAdded");

                    // 追加されたビューの ID を取得
                    int addedViewId = addedView.getId();

                    // 親ビューを取得
                    ViewGroup parent = (ViewGroup) param.thisObject;

                    // 追加されたビューのリソース名を取得
                    String resourceName;
                    try {
                        resourceName = parent.getContext().getResources().getResourceEntryName(addedViewId);
                    } catch (Resources.NotFoundException e) {
                        resourceName = "Resource name not found (ID: " + addedViewId + ")";
                    }

                    XposedBridge.log("Called: " + resourceName);

                    // スタックトレースをログに出力
                    StringBuilder stackTrace = new StringBuilder("\nStack Trace:\n");
                    for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
                        stackTrace.append("  at ")
                                .append(element.getClassName())
                                .append(".")
                                .append(element.getMethodName())
                                .append("(")
                                .append(element.getFileName())
                                .append(":")
                                .append(element.getLineNumber())
                                .append(")\n");
                    }
                    XposedBridge.log(stackTrace.toString());
                }

                private void createAndAddButton(ConstraintLayout parent, View referenceView) {
                    // ボタンを作成
                    Button newButton = new Button(parent.getContext());
                    newButton.setText("新しいボタン");

                    // ボタンの ID を設定
                    newButton.setId(View.generateViewId());

                    // ボタンのレイアウトパラメータを設定
                    ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(
                            ConstraintLayout.LayoutParams.WRAP_CONTENT,
                            ConstraintLayout.LayoutParams.WRAP_CONTENT
                    );

                    // ボタンを親ビューに追加
                    parent.addView(newButton, params);

                    // 制約を設定
                    ConstraintSet constraintSet = new ConstraintSet();
                    constraintSet.clone(parent);

                    // ボタンに対する制約を設定
                    constraintSet.connect(newButton.getId(), ConstraintSet.TOP, referenceView.getId(), ConstraintSet.BOTTOM); // 上に参照ビューを設定
                    constraintSet.connect(newButton.getId(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START); // 左端に設定
                    constraintSet.connect(newButton.getId(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END); // 右端に設定
                    constraintSet.setHorizontalBias(newButton.getId(), 0.5f); // 中央に配置

                    // 制約を適用
                    constraintSet.applyTo(parent);
                }

                private void createAndAddButton(ViewGroup parent) {
                    // ボタンを作成
                    Button newButton = new Button(parent.getContext());
                    newButton.setText("新しいボタン"); // ボタンのテキストを設定

                    // ボタンのレイアウトパラメータを設定
                    ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(
                            ConstraintLayout.LayoutParams.WRAP_CONTENT,
                            ConstraintLayout.LayoutParams.WRAP_CONTENT
                    );

                    // 親の一番右に追加するために、適切な位置を設定
                    params.startToEnd = parent.getChildAt(parent.getChildCount() - 1).getId(); // 最後のビューの右側に配置
                    params.topToTop = ConstraintLayout.LayoutParams.PARENT_ID; // 上部を親の上部に固定

                    newButton.setLayoutParams(params);

                    // 親ビューにボタンを追加
                    parent.addView(newButton);
                }


            });
        } catch (ClassNotFoundException e) {
            XposedBridge.log("Class not found: androidx.constraintlayout.widget.ConstraintLayout");
        } catch (NoSuchMethodException e) {
            XposedBridge.log("Method not found: onViewAdded in ConstraintLayout");
        } catch (Throwable e) {
            XposedBridge.log("Error hooking onViewAdded: " + e.getMessage());
        }
    }



    private int getIdByName(Context context, String resourceName) {
        return context.getResources().getIdentifier(resourceName, "id", context.getPackageName());
    }

    private void hookAllMethods(Class<?> clazz) {
        // クラス内のすべてのメソッドを取得
        Method[] methods = clazz.getDeclaredMethods();

        for (Method method : methods) {
            // 抽象メソッドをスキップ
            if (java.lang.reflect.Modifier.isAbstract(method.getModifiers())) {
                continue;
            }

            // 対象メソッドが特定のビュー関連メソッドであるか確認
            if (

                    !"invokeSuspend".equals(method.getName())&&
//                    !"run".equals(method.getName())
//                    !"setOnTouchListener".equals(method.getName()) &&
//                    !"setVisibility".equals(method.getName()) &&
//                    !"setAlpha".equals(method.getName()) &&
//                    !"setEnabled".equals(method.getName()) &&
//                            !"getString".equals(method.getName()) &&
                    !"onCreate".equals(method.getName())&&
//                    !"setFocusable".equals(method.getName()) &&
//                    !"setOnClickListener".equals(method.getName()) &&
//                    !"setBackgroundColor".equals(method.getName()) &&
//                    !"setPadding".equals(method.getName()) &&
//                    !"setLayoutParams".equals(method.getName()) &&
//                    !"requestLayout".equals(method.getName()) &&
//                    !"invalidate".equals(method.getName()) &&
//                    !"setText".equals(method.getName()) &&  // 新しく追加されたメソッド
//                    !"setTextColor".equals(method.getName()) &&  // 新しく追加されたメソッド
//                    !"setHint".equals(method.getName()) &&  // 新しく追加されたメソッド
//                    !"setHintTextColor".equals(method.getName()) &&  // 新しく追加されたメソッド
//                    !"onStart".equals(method.getName()) &&
                    !"onViewCreated".equals(method.getName()) &&
                    !"onViewAdded".equals(method.getName())
//                    !"setState".equals(method.getName())
            )
            {   // PendingIntent method
                continue;
            }

            method.setAccessible(true); // アクセス可能に設定

            try {
                // メソッドをフックする
                XposedBridge.hookMethod(method, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        StringBuilder argsString = new StringBuilder("Args: ");

                        // 引数が複数の場合、すべてを追加
                        for (int i = 0; i < param.args.length; i++) {
                            Object arg = param.args[i];
                            argsString.append("Arg[").append(i).append("]: ")
                                    .append(arg != null ? arg.toString() : "null")
                                    .append(", ");
                        }

// メソッドに応じたログ出力
                        if ("invokeSuspend".equals(method.getName())) {
                            XposedBridge.log("Before calling invokeSuspend in class: " + clazz.getName() + " with args: " + argsString);
                            StringBuilder stackTrace = new StringBuilder("\nStack Trace:\n");
                            for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
                                stackTrace.append("  at ")
                                        .append(element.getClassName())
                                        .append(".")
                                        .append(element.getMethodName())
                                        .append("(")
                                        .append(element.getFileName())
                                        .append(":")
                                        .append(element.getLineNumber())
                                        .append(")\n");
                            }

//                        } else if ("run".equals(method.getName())) {
//                          XposedBridge.log("Before calling run in class: " + clazz.getName() + " with args: " + argsString);

//                            // ログ出力（引数 + スタックトレース）
//                            XposedBridge.log("Before calling onViewAdded in class: "
//                                    + clazz.getName()
//                                    + " with args: " + argsString
//                                    + stackTrace.toString());
//                        } else if ("onCreate".equals(method.getName())) {
//                            XposedBridge.log("Before calling onCreate in class: " + clazz.getName() + " with args: " + argsString);
//                        } else if ("setAlpha".equals(method.getName())) {
//                            XposedBridge.log("Before calling setAlpha in class: " + clazz.getName() + " with args: " + argsString);
//                        } else if ("setEnabled".equals(method.getName())) {
//                            XposedBridge.log("Before calling setEnabled in class: " + clazz.getName() + " with args: " + argsString);
//                        } else if ("setFocusable".equals(method.getName())) {
//                            XposedBridge.log("Before calling setFocusable in class: " + clazz.getName() + " with args: " + argsString);
//                        } else if ("setOnClickListener".equals(method.getName())) {
//                            XposedBridge.log("Before calling setOnClickListener in class: " + clazz.getName() + " with args: " + argsString);
//                        } else if ("setBackgroundColor".equals(method.getName())) {
//                            XposedBridge.log("Before calling setBackgroundColor in class: " + clazz.getName() + " with args: " + argsString);
//                        } else if ("setPadding".equals(method.getName())) {
//                            XposedBridge.log("Before calling setPadding in class: " + clazz.getName() + " with args: " + argsString);
//                        } else if ("setLayoutParams".equals(method.getName())) {
//                            XposedBridge.log("Before calling setLayoutParams in class: " + clazz.getName() + " with args: " + argsString);
//                        } else if ("requestLayout".equals(method.getName())) {
//                            XposedBridge.log("Before calling requestLayout in class: " + clazz.getName() + " with args: " + argsString);
//                        } else if ("invalidate".equals(method.getName())) {
//                            XposedBridge.log("Before calling invalidate in class: " + clazz.getName() + " with args: " + argsString);
//                        } else if ("setText".equals(method.getName())) {
//                            XposedBridge.log("Before calling setText in class: " + clazz.getName() + " with args: " + argsString);
//                        } else if ("setTextColor".equals(method.getName())) {
//                            XposedBridge.log("Before calling setTextColor in class: " + clazz.getName() + " with args: " + argsString);
//                        } else if ("setHint".equals(method.getName())) {
//                            XposedBridge.log("Before calling setHint in class: " + clazz.getName() + " with args: " + argsString);
//                        } else if ("setHintTextColor".equals(method.getName())) {
//                            XposedBridge.log("Before calling setHintTextColor in class: " + clazz.getName() + " with args: " + argsString);
//                        } else if ("setCompoundDrawables".equals(method.getName())) {
//                            XposedBridge.log("Before calling setCompoundDrawables in class: " + clazz.getName() + " with args: " + argsString);
//                        } else if ("onStart".equals(method.getName())) {
//                            XposedBridge.log("Before calling onStart in class: " + clazz.getName() + " with args: " + argsString);
//                        } else if ("getActivity".equals(method.getName())) {
//                            XposedBridge.log("Before calling getActivity in class: " + clazz.getName() + " with args: " + argsString);
//                        } else if ("getString".equals(method.getName())) {
//                            // スタックトレースの取得
//                            StringBuilder stackTrace = new StringBuilder("\nStack Trace:\n");
//                            for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
//                                stackTrace.append("  at ")
//                                        .append(element.getClassName())
//                                        .append(".")
//                                        .append(element.getMethodName())
//                                        .append("(")
//                                        .append(element.getFileName())
//                                        .append(":")
//                                        .append(element.getLineNumber())
//                                        .append(")\n");
//                            }
//
//                            // ログ出力（引数 + スタックトレース）
//                            XposedBridge.log("Before calling onViewAdded in class: "
//                                    + clazz.getName()
//                                    + " with args: " + argsString
//                                    + stackTrace.toString());

//                        } else if ("getService".equals(method.getName())) {
//                            XposedBridge.log("Before calling getService in class: " + clazz.getName() + " with args: " + argsString);
//                        } else if ("setState".equals(method.getName())) {
//                            XposedBridge.log("Before setState invoke in class: " + clazz.getName() + " with args: " + argsString);
//                        }
                        }
                    }
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Object result = param.getResult();
                        if ("invokeSuspend".equals(method.getName())) {
                            XposedBridge.log("after calling invokeSuspend in class: " + clazz.getName() + (result != null ? result.toString() : "null"));
//                        } else if ("run".equals(method.getName())) {
//                            XposedBridge.log("After calling run in class: " + clazz.getName() + " with result: " + (result != null ? result.toString() : "null"));
//                        } else if ("setAlpha".equals(method.getName())) {
////                            XposedBridge.log("After calling setAlpha in class: " + clazz.getName() + " with result: " + (result != null ? result.toString() : "null"));
//                        } else if ("setEnabled".equals(method.getName())) {
//                            XposedBridge.log("After calling setEnabled in class: " + clazz.getName() + " with result: " + (result != null ? result.toString() : "null"));
//                        } else if ("onCreate".equals(method.getName())) {
//                            XposedBridge.log("After calling onCreate in class: " + clazz.getName() + " with result: " + (result != null ? result.toString() : "null"));
//                        } else if ("onViewCreated".equals(method.getName())) {
//                            XposedBridge.log("After calling onViewCreated in class: " + clazz.getName() + " with result: " + (result != null ? result.toString() : "null"));
//
//                        } else if ("setFocusable".equals(method.getName())) {
//                            XposedBridge.log("After calling setFocusable in class: " + clazz.getName() + " with result: " + (result != null ? result.toString() : "null"));
//                        } else if ("setOnClickListener".equals(method.getName())) {
//                            XposedBridge.log("After calling setOnClickListener in class: " + clazz.getName() + " with result: " + (result != null ? result.toString() : "null"));
//                        } else if ("setBackgroundColor".equals(method.getName())) {
//                            XposedBridge.log("After calling setBackgroundColor in class: " + clazz.getName() + " with result: " + (result != null ? result.toString() : "null"));
//                        } else if ("setPadding".equals(method.getName())) {
//                            XposedBridge.log("After calling setPadding in class: " + clazz.getName() + " with result: " + (result != null ? result.toString() : "null"));
//                        } else if ("setLayoutParams".equals(method.getName())) {
//                            XposedBridge.log("After calling setLayoutParams in class: " + clazz.getName() + " with result: " + (result != null ? result.toString() : "null"));
//                        } else if ("requestLayout".equals(method.getName())) {
//                            XposedBridge.log("After calling requestLayout in class: " + clazz.getName() + " with result: " + (result != null ? result.toString() : "null"));
//                        } else if ("invalidate".equals(method.getName())) {
//                            XposedBridge.log("After calling invalidate in class: " + clazz.getName() + " with result: " + (result != null ? result.toString() : "null"));
//                        } else if ("setText".equals(method.getName())) {
//                            XposedBridge.log("After calling setText in class: " + clazz.getName() + " with result: " + (result != null ? result.toString() : "null"));
//                        } else if ("setTextColor".equals(method.getName())) {
//                            XposedBridge.log("After calling setTextColor in class: " + clazz.getName() + " with result: " + (result != null ? result.toString() : "null"));
//                        } else if ("setHint".equals(method.getName())) {
//                            XposedBridge.log("After calling setHint in class: " + clazz.getName() + " with result: " + (result != null ? result.toString() : "null"));
//                        } else if ("setHintTextColor".equals(method.getName())) {
//                            XposedBridge.log("After calling setHintTextColor in class: " + clazz.getName() + " with result: " + (result != null ? result.toString() : "null"));
//                        } else if ("setCompoundDrawables".equals(method.getName())) {
//                            XposedBridge.log("After calling setCompoundDrawables in class: " + clazz.getName() + " with result: " + (result != null ? result.toString() : "null"));
//                        } else if ("onStart".equals(method.getName())) {
//                            XposedBridge.log("Before calling onStart in class: " + clazz.getName() + " with result: " + (result != null ? result.toString() : "null"));
//                        } else if ("getActivity".equals(method.getName())) {
//                            XposedBridge.log("After calling getActivity in class: " + clazz.getName() + " with result: " + (result != null ? result.toString() : "null"));
//                        } else if ("onViewAdded".equals(method.getName())) {
//                            XposedBridge.log("After calling onViewAdded in class: " + clazz.getName() + " with result: " + (result != null ? result.toString() : "null"));
//                        } else if ("getService".equals(method.getName())) {
//                            XposedBridge.log("After calling getService in class: " + clazz.getName() + " with result: " + (result != null ? result.toString() : "null"));
//                        } else if ("setState".equals(method.getName())) {
//                            XposedBridge.log("setState " + clazz.getName() + " with result: " + (result != null ? result.toString() : "null"));
//                        }
                        }
                    }

                });
            } catch (IllegalArgumentException e) {
                XposedBridge.log("Error hooking method " + method.getName() + " in class " + clazz.getName() + " : " + e.getMessage());
            } catch (Throwable e) {
                XposedBridge.log("Unexpected error hooking method " + method.getName() + " in class " + clazz.getName() + " : " + Log.getStackTraceString(e));
            }
        }
    }

    private boolean isViewCreationMethod(Method method) {
        // View作成に関連するメソッドを検出
        String methodName = method.getName().toLowerCase();

        return methodName.contains("inflate") || methodName.contains("new") || methodName.contains("create");
    }
    private void traverseAllViews(ViewGroup parent) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);

            // ViewStub の処理
            if (child instanceof ViewStub) {
                ViewStub stub = (ViewStub) child;
                int layoutRes = stub.getLayoutResource();

                if (layoutRes == 0) {
                    Log.w("Xposed", "ViewStub skipped (no layoutResource): " + stub);
                    continue;
                }

                try {
                    View inflated = stub.inflate();
                    Log.i("Xposed", "ViewStub inflated: " + inflated);
                    if (inflated instanceof ViewGroup) {
                        traverseAllViews((ViewGroup) inflated);
                    }
                    continue;
                } catch (Exception e) {
                    Log.e("Xposed", "ViewStub inflate failed: " + e.getMessage(), e);
                    continue;
                }
            }

            // View のログ出力
            String idName = "NO_ID";
            try {
                int id = child.getId();
                if (id != View.NO_ID) {
                    idName = child.getResources().getResourceEntryName(id);
                }
            } catch (Exception ignored) {}

            Log.i("Xposed", "View: " + child.getClass().getName() + " (ID: " + idName + ")");

            // 子が ViewGroup の場合は再帰
            if (child instanceof ViewGroup) {
                traverseAllViews((ViewGroup) child);
            }
        }
    }

    private void dumpViewHierarchy(View root) {
        dumpViewHierarchyRecursive(root, 0);
    }

    private void dumpViewHierarchyRecursive(View view, int depth) {
        StringBuilder indent = new StringBuilder();
        for (int i = 0; i < depth; i++) indent.append("  ");  // インデント生成

        int id = view.getId();
        String idName = "(no id)";
        try {
            if (id != View.NO_ID) {
                idName = view.getContext().getResources().getResourceName(id);
            }
        } catch (Resources.NotFoundException ignored) {}

        String viewClass = view.getClass().getName();
        String text = null;
        if (view instanceof TextView) {
            text = ((TextView) view).getText().toString();
        }

        XposedBridge.log(indent + "View: " + viewClass + ", ID: " + idName + (text != null ? ", Text: \"" + text + "\"" : ""));

        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                dumpViewHierarchyRecursive(vg.getChildAt(i), depth + 1);
            }
        }
    }
    private static class MyFactory2 implements LayoutInflater.Factory2 {
        private final LayoutInflater.Factory2 original;

        MyFactory2(LayoutInflater.Factory2 original) {
            this.original = original;
        }

        @Override
        public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
            View view = null;
            try {
                if (original != null) {
                    view = original.onCreateView(parent, name, context, attrs);
                }
            } catch (Exception e) {
                XposedBridge.log("MyFactory2: error from original factory: " + e);
            }

            if (view != null) {
                try {
                    int id = view.getId();
                    String idName = "(no id)";
                    if (id != View.NO_ID) {
                        idName = context.getResources().getResourceName(id);
                    }

                    // ログ出力（全Viewをログしたい場合）
                    String text = null;
                    if (view instanceof TextView) {
                        text = ((TextView) view).getText().toString();
                    }

                    XposedBridge.log("[MyFactory2] View created: class=" + view.getClass().getName()
                            + ", id=" + idName + (text != null ? ", text=\"" + text + "\"" : ""));

                    // 非表示にしたいView ID一覧
                    Set<String> idsToHide = new HashSet<>(Arrays.asList(
                            "jp.naver.line.android:id/home_bigbanner_ad_video_play_btn",
                            "jp.naver.line.android:id/home_bigbanner_ad_video_replay",
                            "jp.naver.line.android:id/home_bigbanner_ad_video_see_detail"
                    ));

                    // 条件一致した場合、親を非表示に
                    if (idsToHide.contains(idName)) {
                        View target = view;
                        for (int i = 0; i < 3; i++) {  // 最大3階層上まで探索
                            if (target.getParent() instanceof View) {
                                target = (View) target.getParent();
                            } else {
                                break;
                            }
                        }

                        XposedBridge.log("[MyFactory2] => Hiding parent of matched view: class=" + target.getClass().getName());

                        ViewGroup.LayoutParams params = target.getLayoutParams();
                        if (params != null) {
                            params.height = 0;
                            target.setLayoutParams(params);
                        }
                        target.setVisibility(View.GONE);
                    }

                    // テキスト内容での非表示条件
                    if (text != null && (text.contains("呼出音") || text.contains("ゆるかわ"))) {
                        ViewGroup.LayoutParams params = view.getLayoutParams();
                        if (params != null) {
                            params.height = 0;
                            view.setLayoutParams(params);
                        }
                        view.setVisibility(View.GONE);
                        XposedBridge.log("[MyFactory2] => Hiding view with text match: " + text);
                    }

                } catch (Throwable t) {
                    XposedBridge.log("MyFactory2: Exception during processing: " + t);
                }
            }

            return view;
        }

        @Override
        public View onCreateView(String name, Context context, AttributeSet attrs) {
            return onCreateView(null, name, context, attrs);
        }

    }

    private void scanViewHierarchy(View view) {
        try {
            int id = view.getId();
            String idName = "(no id)";
            if (id != View.NO_ID) {
                idName = view.getContext().getResources().getResourceName(id);
            }

            String text = null;
            if (view instanceof TextView) {
                text = ((TextView) view).getText().toString();
            }

            XposedBridge.log("[ViewScan] Class=" + view.getClass().getName()
                    + ", id=" + idName + (text != null ? ", text=\"" + text + "\"" : ""));

            // IDによる判定
            Set<String> idsToHide = new HashSet<>(Arrays.asList(
                    "jp.naver.line.android:id/home_bigbanner_ad_video_play_btn",
                    "jp.naver.line.android:id/home_bigbanner_ad_video_replay",
                    "jp.naver.line.android:id/home_bigbanner_ad_video_see_detail",
            "com.linecorp.line.home.ui.profile.HomeProfileWithPremiumBadgeView"
            ));
            if (idsToHide.contains(idName)) {
                hideParent(view, 3);  // 最大3階層まで遡って親を非表示
            }

            // テキストによる判定
            if (text != null && (text.contains("呼出音") || text.contains("ゆるかわ"))) {
                hideParent(view, 3);
            }

            if (view instanceof ViewGroup) {
                ViewGroup vg = (ViewGroup) view;
                for (int i = 0; i < vg.getChildCount(); i++) {
                    scanViewHierarchy(vg.getChildAt(i));
                }
            }
        } catch (Throwable t) {
            XposedBridge.log("scanViewHierarchy: " + t);
        }
    }

    private void hideParent(View view, int maxDepth) {
        View target = view;
        for (int i = 0; i < maxDepth; i++) {
            ViewParent parent = target.getParent();
            if (parent instanceof View) {
                target = (View) parent;
            } else {
                break;
            }
        }

        try {
            ViewGroup.LayoutParams params = target.getLayoutParams();
            if (params != null) {
                params.height = 0;
                target.setLayoutParams(params);
            }
            target.setVisibility(View.GONE);
            XposedBridge.log("[ViewScan] => Hiding parent view: " + target.getClass().getName());
        } catch (Throwable e) {
            XposedBridge.log("hideParent error: " + e);
        }
    }

}