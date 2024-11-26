package io.test.hiro.NoAD;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
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
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class Main implements IXposedHookLoadPackage {
    private Set<Object> adClassNames = new HashSet<>();
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
                        checkAndChangeBackgroundColor(context,view, packageName);
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
                                || adClassNames.stream().anyMatch(adClass -> {
                            if (adClass instanceof String) {
                                // Check if the class name matches
                                return className.contains((String) adClass);
                            } else if (adClass instanceof Integer) {
                                // Check if the resource ID matches
                                String resourceName = getResourceName(view, context.getResources());
                                return resourceName != null && resourceName.equals(view.getResources().getResourceName((Integer) adClass));
                            }
                            return false;
                        });

// Check if the view's ID matches any ID in adClassNames
                        if (view.getId() != View.NO_ID) {
                            isAdView = isAdView || adClassNames.contains(view.getId());
                        }

// Check resource name for specific keywords if it's not already marked as an ad
                        if (!isAdView) {
                            // リソース名を取得
                            Resources resources = context.getResources();
                            String resourceName = getResourceName(view, resources);

                            // リソース名がnullでないことを確認
                            if (resourceName != null) {
                                // リソース名に広告関連のキーワードが含まれているかチェック
                                isAdView = resourceName.contains("Ad")
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
                                        || adClassNames.stream().anyMatch(adClass -> {
                                    if (adClass instanceof String) {
                                        // クラス名またはリソース名が一致するかをチェック
                                        return resourceName.contains((String) adClass);
                                    } else if (adClass instanceof Integer) {
                                        // リソースIDに一致するリソース名があるかをチェック
                                        String resourceIdName = resources.getResourceName((Integer) adClass);
                                        return resourceName.equals(resourceIdName);
                                    }
                                    return false;
                                });
                            }
                        }



// 広告ビューの場合は非表示
                        if (isAdView) {
                            if (view.getVisibility() != View.GONE) {
                                view.setVisibility(View.GONE);
                            }
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

                        // adClassNames にクラス名が含まれていれば高さを0に設定
                        if (adClassNames.contains(className)) {
                            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
                            if (layoutParams != null) {
                                layoutParams.height = 0;
                                view.setLayoutParams(layoutParams);
                            }
                        }

                        // adClassNames にリソースIDが含まれていれば高さを0に設定
                        if (view.getId() != View.NO_ID && adClassNames.contains(view.getId())) {
                            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
                            if (layoutParams != null) {
                                layoutParams.height = 0;
                                view.setLayoutParams(layoutParams);
                            }
                        }

                        // クラス名やリソース名に "Ad" や "ads" が含まれている場合も高さを0に設定
                        if (className.contains("Ad") || className.contains("ads")) {
                            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
                            if (layoutParams != null) {
                                layoutParams.height = 0;
                                view.setLayoutParams(layoutParams);
                            }
                        }
                    }
                }
        );

        XposedHelpers.findAndHookMethod("android.view.View", loadPackageParam.classLoader, "onAttachedToWindow", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                View view = (View) param.thisObject;
                String packageName = loadPackageParam.packageName; // 正しいパッケージ名を取得
                Context context = view.getContext();
                checkAndChangeBackgroundColor(context,view, packageName);
            }
        });

        for (Object adClass : adClassNames) {
            if (adClass instanceof String) {
                String adClassName = (String) adClass;
                try {
                    Class<?> clazz = loadPackageParam.classLoader.loadClass(adClassName);
                    XposedBridge.hookAllConstructors(
                            clazz,
                            new XC_MethodHook() {
                                @Override
                                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                    // コンストラクタ内でViewのインスタンスが作成された後の処理
                                    if (param.thisObject instanceof View) {
                                        View view = (View) param.thisObject;
                                        // 初期化直後にビューを非表示に設定
                                        view.setVisibility(View.GONE);

                                        // ViewTreeObserverを使用してレイアウト後に再度非表示設定
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
                            }
                    );
                } catch (ClassNotFoundException e) {
                    //XposedBridge.log("Class not found: " + adClassName);
                }
            }
        }



    }

    private Set<String> changedResources = new HashSet<>(); // 変更済みリソースを保存するセット
    private boolean isChangingColor = false;
    private void checkAndChangeBackgroundColor(Context context, View view, String packageName) {
        try {
            if ("com.google.android.webview".equals(packageName) || "com.google.android.gms".equals(packageName)) {
                return; // 特定のパッケージの場合は処理を終了
            }
    if (!isLoggingEnabled()) {
        return; 
    }

            // Prevent recursion
            if (isChangingColor) {
                return;
            }
            isChangingColor = true;

            // リソース名を取得
            String resourceName = getViewResourceName(view);
            if (resourceName == null || resourceName.isEmpty()) {
                //XposedBridge.log("Resource Name is null or empty. Skipping.");
                return; // リソース名が無効の場合はスキップ
            }

            // 既に変更済みのリソースかどうかを確認
            if (changedResources.contains(resourceName)) {
                //XposedBridge.log("Skipping Background Color Change for Resource Name: " + resourceName + " (Already Changed)");
                return; // 変更済みリソースの場合は処理を終了
            }

            // 背景を取得
            Drawable background = view.getBackground();

            // 背景が null でないことを確認
            if (background != null) {
                // 背景のクラス名をログに出力
                //XposedBridge.log("Background Class Name: " + background.getClass().getName());

                if (background instanceof ColorDrawable) {
                    // ランダムな色を生成
                    Random random = new Random();
                    int randomColor = Color.rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256));

                    // ランダムな色を設定
                    ((ColorDrawable) background).setColor(randomColor);

                    // リソース名を変更済みとしてセットに追加
                    changedResources.add(resourceName);

                    String colorCode = String.format("#%06X", (0xFFFFFF & randomColor));

                    // ビューのクラス名を取得
                    String viewClassName = view.getClass().getName();

                    // ログ出力にクラス名を追加
                    //XposedBridge.log("Changed Background Color of Resource Name: " + resourceName + " (Class: " + viewClassName + ") to " + colorCode);

                    // ログ書き込み処理
                    if (isLoggingEnabled()) {
                        writeLogToFile(context, packageName, resourceName, colorCode,viewClassName);
                    }
                } else if (background instanceof BitmapDrawable) {
                    //XposedBridge.log("BitmapDrawable background, cannot change color directly.");
                } else {
                    //XposedBridge.log("Unknown background type for Resource Name: " + resourceName + ", Class Name: " + background.getClass().getName());
                }
            } else {
                //XposedBridge.log("Background is null for Resource Name: " + resourceName);
            }
        } catch (Resources.NotFoundException e) {
            //XposedBridge.log("Resource name not found for View ID: " + view.getId());
        } finally {
            isChangingColor = false; // Reset the flag after the method execution
        }
    }

    private void writeLogToFile(Context context, String packageName, String resourceName, String colorCode, String className) {
        File backupDir = null;
        File logFile = null;

        try {
            // アプリ固有のダウンロードディレクトリを使用
            backupDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), DIRECTORY_NAME);
            if (!backupDir.exists() && !backupDir.mkdirs()) {
                //XposedBridge.log("Failed to create backup directory: " + backupDir.getAbsolutePath());
                return;
            }

            logFile = new File(backupDir, packageName  + "_resource_color_log.txt");

            // クラス名も含めたログエントリを作成
            String logEntry = "Resource Name: " + resourceName + ", Color Code: " + colorCode + ", Class Name: " + className + "\n";

            // ログファイルに追記
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
                writer.append(logEntry);
                writer.flush();
            }
        } catch (IOException e) {
            //XposedBridge.log("Error writing to log file: " + e.getMessage());
            if (logFile != null && logFile.exists()) {
                // エラー発生時にファイルを削除して再作成
                if (logFile.delete()) {
                    //XposedBridge.log("Log file deleted due to error. Attempting to recreate.");
                    try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
                        String logEntry = "Resource Name: " + resourceName + ", Color Code: " + colorCode + ", Class Name: " + className + "\n";
                        writer.append(logEntry);
                        writer.flush();
                    } catch (IOException retryException) {
                        //XposedBridge.log("Error recreating log file: " + retryException.getMessage());
                    }
                } else {
                    //XposedBridge.log("Failed to delete log file after error.");
                }
            }
        }
    }


    private String getViewResourceName(View view) {
        try {
            int id = view.getId();
            if (id != View.NO_ID) {
                // リソース名を取得
                String resourceName = view.getResources().getResourceName(id);
                return resourceName != null ? resourceName : String.valueOf(id);
            }
        } catch (Resources.NotFoundException e) {
            //XposedBridge.log("Resource name not found for View ID: " + view.getId());
        } catch (Exception e) {
            //XposedBridge.log("Unexpected error while retrieving resource name: " + e.getMessage());
        }
        return null; // 解決できなかった場合
    }




    // リソース名を取得するヘルパーメソッド
    private String getResourceName(View view, Resources resources) {
        try {
            int resourceId = view.getId();
            if (resourceId != View.NO_ID) {
                return resources.getResourceEntryName(resourceId); // リソース名を返す
            }
        } catch (Resources.NotFoundException e) {
            ////XposedBridge.log("Resource not found: " + e.getMessage());
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

    private List<Object> loadAdClassesFromPreferences() {
        XSharedPreferences preferences = new XSharedPreferences("io.test.hiro.NoAD", "ad_prefs");

        preferences.makeWorldReadable();

        preferences.reload();

        String adClasses = preferences.getString("ad_classes", ""); // デフォルトは空文字
        if (!adClasses.isEmpty()) {
            String[] adClassArray = adClasses.split(","); // カンマ区切りで分割
            for (String adClass : adClassArray) {
                adClass = adClass.trim();
                try {
                    // リソースIDとして解釈できる場合
                    int resourceId = Integer.parseInt(adClass);
                    if (!adClassNames.contains(resourceId)) {
                        adClassNames.add(resourceId);  // リソースIDとして追加
                    }
                } catch (NumberFormatException e) {
                    // 文字列（クラス名）として追加
                    if (!adClassNames.contains(adClass)) {
                        adClassNames.add(adClass);
                    }
                }
            }
        }

        // ログに出力して確認
        //XposedBridge.log("Loaded adClassNames: " + adClassNames);

        return new ArrayList<>(adClassNames);
    }

}
