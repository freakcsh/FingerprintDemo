package com.example.a74099.fingerprintdemo;

import android.content.Context;
import android.content.Intent;

/**
 * 指纹设置工具类，将进入设置进行指纹设置
 */

public class FingerprintUtil {

    private static final String ACTION_SETTING = "android.settings.SETTINGS";

    public static void openFingerPrintSettingPage(Context context) {
        Intent intent = new Intent(ACTION_SETTING);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(intent);
        } catch (Exception e) {
        }
    }
}
