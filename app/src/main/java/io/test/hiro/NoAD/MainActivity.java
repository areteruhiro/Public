package io.test.hiro.NoAD;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;


import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;





public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_PERMISSION_CODE = 1;
    private static final String PREF_NAME = "ad_prefs";  // SharedPreferencesファイル名
    private static final String LOGGING_ENABLED_KEY = "logging_enabled";  // ログ状態を保存するキー
    private Switch logSwitch;
    private EditText adClassesEditText;
    private Button saveButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        logSwitch = findViewById(R.id.logSwitch);
        adClassesEditText = findViewById(R.id.adClassesEditText); // 入力欄
        saveButton = findViewById(R.id.saveButton); // 保存ボタン

        // 設定ディレクトリとログファイルを指定
        File backupDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "NoAd Module");
        File logFile = new File(backupDir, "log_settings.txt");

        // 起動時にlog_settings.txtの存在を確認してスイッチの状態を設定
        logSwitch.setChecked(logFile.exists());

        String savedAdClasses = loadAdClassesFromPreferences(); // SharedPreferencesから読み込む
        adClassesEditText.setText(savedAdClasses); // 入力欄に設定

// スイッチの状態が変更されたときの処理
        logSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
                // 外部ストレージの管理権限がない場合はリクエストを通知
                Toast.makeText(this, "Storage permission is required to create log files.", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
                // スイッチを戻す
                logSwitch.setChecked(false);
                return;
            }

            if (isChecked) {
                // フォルダが存在しない場合は作成
                if (!backupDir.exists() && !backupDir.mkdirs()) {
                    Toast.makeText(this, "Failed to create directory", Toast.LENGTH_SHORT).show();
                    return;
                }

                // ファイルを作成する
                try (FileWriter writer = new FileWriter(logFile)) {
                    writer.write("Logging enabled");
                    Toast.makeText(this, "Log ON", Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Failed to create log file", Toast.LENGTH_SHORT).show();
                }
            } else {
                // ファイルを削除する
                if (logFile.exists() && logFile.delete()) {
                    Toast.makeText(this, "Log OFF", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "No log file to delete", Toast.LENGTH_SHORT).show();
                }
            }
        });



        saveButton.setOnClickListener(v -> {
            // Ad classes を保存
            String adClasses = adClassesEditText.getText().toString();
            saveAdClassesToPreferences(adClasses); // SharedPreferencesに保存
            Toast.makeText(this, "Ad classes saved!", Toast.LENGTH_SHORT).show();

            restartApp();
        });

    }
    private void restartApp() {
        Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent); // アプリのメイン画面を起動
        }
        // 現在のプロセスを終了
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    private void saveAdClassesToPreferences(String adClasses) {
        SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("ad_classes", adClasses);
        editor.commit(); // 同期保存
    }


    // SharedPreferencesからad_classesを読み込む
    private String loadAdClassesFromPreferences() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);  // MODE_PRIVATEは推奨
        return sharedPreferences.getString("ad_classes", "");  // デフォルトは空文字
    }

    // SharedPreferencesにad_classesを保存



}
