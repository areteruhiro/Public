package io.test.hiro.NoAD;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class Main implements IXposedHookLoadPackage {
    static final List<String> adClassNames = new ArrayList<>();
    private static final String FILE_NAME = "log_settings.txt";
    private static final String DIRECTORY_NAME = "NoAd Module";
    private static final String AD_CLASSES_KEY = "ad_classes";
    private static final String PREF_NAME = "ad_prefs";
    private static boolean hasToastShown = false; // トースト表示済みフラグ
    @Override
    public void handleLoadPackage(@NonNull XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        loadAdClassesFromPreferences();
        XSharedPreferences preferences = new XSharedPreferences("io.test.hiro.NoAD", "Ad_Name");

        String packageName = loadPackageParam.packageName;  // 正しいパッケージ名を取得
        if ("io.test.hiro.NoAD".equals(packageName)) {

            return; // ここで処理を抜ける
        }

        preferences.makeWorldReadable(); // モジュールがアクセスできるように設定
        XposedHelpers.findAndHookMethod(
                ViewGroup.class,
                "addView",
                View.class,
                ViewGroup.LayoutParams.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        View view = (View) param.args[0];
                        Context context = view.getContext();

                            // トーストをまだ表示していない場合のみ表示
                            if (!hasToastShown) {
                                Toast.makeText(context, "Loading" + adClassNames, Toast.LENGTH_SHORT).show();
                                hasToastShown = true; // フラグを立てる


                        }
                        String className = view.getClass().getName();
                        if ("com.google.android.webview".equals(packageName) || "com.google.android.gms".equals(packageName)) {
                            return;
                        }

// 広告ビューかどうかを判定
                        boolean isAdView = className.contains("Ad")
                                || className.contains("com.five_corp.ad.internal.view.d")
                                || className.equals("com.five_corp.ad.internal.view.m")
                                || className.contains("com.mbridge.msdk.videocommon.view.MyImageView")
                                || className.contains("com.mbridge.msdk.nativex.view.WindVaneWebViewForNV")
                                || adClassNames.stream().anyMatch(className::contains); // Check against dynamic list

                        if (!isAdView) {
                            Resources resources = context.getResources();
                            String resourceName = getResourceName(view, resources);
                            isAdView = resourceName != null && (resourceName.contains("Ad")
                                    || resourceName.contains("adaptive_banner_container")
                                    || resourceName.contains("footer_banner_ad_container")
                                    || resourceName.contains("ad_label")
                                    || resourceName.contains("layoutMediaContainer")
                                    || resourceName.contains("adg_container")
                                    || resourceName.contains("rect_banner_ad_container")
                                    || resourceName.contains("ad_container")
                                    || resourceName.contains("textAdLabel")
                                    || resourceName.contains("mbridge")
                                    || resourceName.contains("buttonRemoveAd")
                                    || adClassNames.stream().anyMatch(resourceName::contains) // Check against dynamic list
                            );
                        }


                        // 広告ビューの場合は非表示
                        if (isAdView) {
                            if (view.getVisibility() != View.GONE) {
                                view.setVisibility(View.GONE);
                            }
                        }

                        // ログ書き込み処理
                        if (isLoggingEnabled()) {
                            // 特定のパッケージの場合、処理をスキップ
                            if ("com.google.android.webview".equals(packageName) || "com.google.android.gms".equals(packageName)) {
                                return; // 処理を終了
                            }
                            Resources resources = context.getResources();
                            String resourceName = getResourceName(view, resources);

                            if ((resourceName != null && !resourceName.isEmpty()) || className != null) {
                                File backupDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "NoAd Module");
                                if (!backupDir.exists() && !backupDir.mkdirs()) {
                                    XposedBridge.log("Failed to create backup directory");
                                    return;
                                }

                                File logFile = new File(backupDir, packageName + "_ad_log.txt");
                                StringBuilder logEntryBuilder = new StringBuilder();

                                if (resourceName != null) {
                                    logEntryBuilder.append("resourceName: ").append(resourceName).append("\n");
                                }
                                logEntryBuilder.append("className: ").append(className).append("\n");

                                String logEntry = logEntryBuilder.toString();


                                if (logFile.exists()) {
                                    // ファイルを逐次的に読み取り、同じログがあるかチェック
                                    try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
                                        String line;
                                        while ((line = reader.readLine()) != null) {
                                            if (line.contains(logEntry)) {
                                                return; // ログが既に存在していればスキップ
                                            }
                                        }
                                    } catch (IOException e) {
                                        XposedBridge.log("Error reading log file: " + e.getMessage());
                                    }
                                }

                                // ログファイルに追記
                                try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
                                    writer.append(logEntry);
                                    writer.flush();
                                } catch (IOException e) {
                                    XposedBridge.log("Error writing to log file: " + e.getMessage());
                                }
                            }
                            XposedBridge.log(resourceName);
                            XposedBridge.log(className);
                        }
                    }
                }
        );

        XposedBridge.hookAllMethods(
                View.class,
                "onAttachedToWindow",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        View view = (View) param.thisObject;
                        String className = view.getClass().getName();
                        if (adClassNames.contains(className)) {
                            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
                            if (layoutParams != null) {
                                layoutParams.height = 0;
                                view.setLayoutParams(layoutParams);
                            }
                        }
                    if (className.contains("Ad")||className.contains("ads")) {
                            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
                            if (layoutParams != null) {
                                layoutParams.height = 0;
                                view.setLayoutParams(layoutParams);
                            }
                        }

                    }
                }
        );

        for (String adClassName : adClassNames) {
            try {
                XposedBridge.hookAllConstructors(
                        loadPackageParam.classLoader.loadClass(adClassName),
                        new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                View view = (View) param.thisObject;
                                view.setVisibility(View.GONE);
                                view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                                    @Override
                                    public void onGlobalLayout() {
                                        if (view.getVisibility() != View.GONE) {
                                            view.setVisibility(View.GONE);
                                        }
                                    }
                                });


                            }
                        }
                );
            } catch (ClassNotFoundException e) {
                //XposedBridge.log("Class not found: " + adClassName);
            }
        }

    }
    // リソース名を取得するヘルパーメソッド
    private String getResourceName(View view, Resources resources) {
        try {
            int resourceId = view.getId();
            if (resourceId != View.NO_ID) {
                return resources.getResourceEntryName(resourceId); // リソース名を返す
            }
        } catch (Resources.NotFoundException e) {
            //XposedBridge.log("Resource not found: " + e.getMessage());
        }
        return "";
    }

    // ログ状態を確認するメソッド
    private boolean isLoggingEnabled() {
        File backupDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), DIRECTORY_NAME);
        if (!backupDir.exists()) {
            Log.w("LoggingCheck", "Directory does not exist: " + backupDir.getAbsolutePath());
            return false;
        }

        File logFile = new File(backupDir, FILE_NAME);
        if (!logFile.exists()) {
            Log.w("LoggingCheck", "Log file does not exist: " + logFile.getAbsolutePath());
            return false;
        }

        if ("log_settings.txt".equals(logFile.getName())) {
            Log.i("LoggingCheck", "Log file is log_settings.txt, returning false.");
            return true;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(logFile)))) {
            String line = reader.readLine();
            boolean isEnabled = Boolean.parseBoolean(line);
            Log.i("LoggingCheck", "Read log status: " + isEnabled);
            return isEnabled;
        } catch (IOException e) {
            Log.e("LoggingCheck", "Error reading log file", e);
            return false;
        }
    }

    private List<String> loadAdClassesFromPreferences() {
        XSharedPreferences preferences = new XSharedPreferences("io.test.hiro.NoAD", "ad_prefs");

        preferences.makeWorldReadable();

        preferences.reload();

        String adClasses = preferences.getString("ad_classes", ""); // デフォルトは空文字
        if (!adClasses.isEmpty()) {
            String[] adClassArray = adClasses.split(","); // カンマ区切りで分割
            for (String adClass : adClassArray) {
                adClass = adClass.trim();
                if (!adClassNames.contains(adClass)) {
                    adClassNames.add(adClass);
                }
            }
        }

        // ログに出力して確認
 XposedBridge.log("Loaded adClassNames: " + adClassNames);

        return adClassNames;
    }


}
