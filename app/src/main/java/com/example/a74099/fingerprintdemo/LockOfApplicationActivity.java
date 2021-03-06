package com.example.a74099.fingerprintdemo;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.a74099.fingerprintdemo.core.FingerprintCore;

/**
 * Created by 74099 on 2018/6/28.
 */

public class LockOfApplicationActivity extends AppCompatActivity implements View.OnClickListener {
    private LinearLayout ll_back;
    private ImageView img_fingerprint;
    private TextView tv_pw, tv_cancel;

    private FingerprintCore mFingerprintCore;

    private KeyguardLockScreenManager mKeyguardLockScreenManager;

    private Toast mToast;

    private Handler mHandler = new Handler(Looper.getMainLooper());
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lock);
        ll_back = findViewById(R.id.ll_back);
        img_fingerprint = findViewById(R.id.img_fingerprint);
        tv_pw = findViewById(R.id.tv_pw);
        tv_cancel = findViewById(R.id.tv_cancel);

        ll_back.setOnClickListener(this);
        tv_pw.setOnClickListener(this);
        tv_cancel.setOnClickListener(this);

    }

    @Override
    protected void onResume() {
        super.onResume();
        initFingerprintCore();
        startFingerprint();
    }

    private void initFingerprintCore() {
        mFingerprintCore = new FingerprintCore(this);
        mFingerprintCore.setFingerprintManager(mresultlistener);
        mKeyguardLockScreenManager = new KeyguardLockScreenManager(this);
    }
    /**
     * 开始识别指纹
     */
    private void startFingerprint() {
        if (mFingerprintCore.isSupport()) {
            if (!mFingerprintCore.isHardwareDetected()) {
                toastTipMsg(R.string.fingerprint_recognition_not_enrolled);
                FingerprintUtil.openFingerPrintSettingPage(this);
                return;
            }
            toastTipMsg(R.string.fingerprint_recognition_tip);

            img_fingerprint.setBackgroundResource(R.drawable.fingerprint);
            if (mFingerprintCore.isAuthenticating()) {
                toastTipMsg(R.string.fingerprint_recognition_authenticating);
            } else {
                mFingerprintCore.startAuthenticate();
            }
        } else {
            toastTipMsg(R.string.fingerprint_recognition_not_support);
        }
    }

    private void toastTipMsg(int messageId) {
        if (mToast == null) {
            mToast = Toast.makeText(this, messageId, Toast.LENGTH_SHORT);
        }
        mToast.setText(messageId);
        mToast.cancel();
        mHandler.removeCallbacks(mShowToastRunnable);
        mHandler.postDelayed(mShowToastRunnable, 0);
    }

    private Runnable mShowToastRunnable = new Runnable() {
        @Override
        public void run() {
            mToast.show();
        }
    };


    private FingerprintCore.IFingerprintResultListener mresultlistener = new FingerprintCore.IFingerprintResultListener() {
        @Override
        public void onAuthenticateSuccess() {
            toastTipMsg(R.string.fingerprint_recognition_success);
            resetGuideViewState();
            finish();
        }

        @Override
        public void onAuthenticateFailed(int helpId) {
            toastTipMsg(R.string.fingerprint_recognition_failed);
        }

        @Override
        public void onAuthenticateError(int errMsgId) {
            resetGuideViewState();
            toastTipMsg(R.string.fingerprint_recognition_error);
        }

        @Override
        public void onStartAuthenticateResult(boolean isSuccess) {

        }
    };

    private void resetGuideViewState() {
        img_fingerprint.setBackgroundResource(R.drawable.deblocking);
    }
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_cancel:
                finish();
                break;
            case R.id.tv_pw:
                break;
            case R.id.ll_back:
                finish();
                break;

        }
    }

    @Override
    protected void onDestroy() {
        if (mFingerprintCore != null) {
            mFingerprintCore.onDestroy();
            mFingerprintCore = null;
        }
        if (mKeyguardLockScreenManager != null) {
            mKeyguardLockScreenManager.onDestroy();
            mKeyguardLockScreenManager = null;
        }
        mresultlistener = null;
        mShowToastRunnable = null;
        mToast = null;
        super.onDestroy();
    }
}
