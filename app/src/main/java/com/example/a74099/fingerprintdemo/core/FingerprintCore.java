package com.example.a74099.fingerprintdemo.core;

import android.annotation.TargetApi;
import android.content.Context;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.RequiresApi;
import android.util.Log;


import java.lang.ref.WeakReference;

/**
 * 指纹核心
 * Created by 74099 on 2018/6/28.
 */
@TargetApi(Build.VERSION_CODES.M)
public class FingerprintCore {
    private static final int NONE = 0;
    private static final int CANCEL = 1;
    private static final int AUTHENTICATING = 2;
    private int mState = NONE;
    /**
     * FingerprintManager
     * 协调对指纹硬件的访问的类。
     */
    private FingerprintManager mFingerprintManager;
    /**
     * WeakReference
     * 弱参考对象，不妨碍他们的对象被定型，定型，然后再回收。
     * 弱引用通常用于实现规范化映射。
     * 假设垃圾收集器在某个时间点确定一个对象是
     * <a href="package-summary.html#reachability">弱到达</a>。
     * 那时它会原子地清除对该对象的所有弱引用，
     * 以及通过一系列强和软引用可访问该对象的任何其他弱可访问对象的所有弱引用。
     * 同时它将宣布所有以前弱可达的物体可以定型。
     * 在同一时间或稍后的时间，它将排队那些在参考队列中注册的新清除的弱引用。
     */
    private WeakReference<IFingerprintResultListener> mFpResultListener;
    /**
     * 提供取消正在进行的操作的能力。
     */
    private CancellationSignal mCancellationSignal;

    private CryptoObjectCreator mCryptoObjectCreator;

    private FingerprintManager.AuthenticationCallback mAuthenticationCallback;

    /**
     * 失败时间
     */
    private int mFailedTimes = 0;
    /**
     * 是否支持指纹解锁
     */
    private boolean isSupport = false;

    private Handler mHandler = new Handler(Looper.getMainLooper());

    /**
     * 指纹识别回调接口
     */
    public interface IFingerprintResultListener {
        /**
         * 指纹识别成功
         */
        void onAuthenticateSuccess();

        /**
         * 指纹识别失败
         */
        void onAuthenticateFailed(int helpId);

        /**
         * 指纹识别发生错误-不可短暂恢复
         */
        void onAuthenticateError(int errMsgId);

        /**
         * 开始指纹识别监听成功
         */
        void onStartAuthenticateResult(boolean isSuccess);
    }

    public FingerprintCore(Context context) {
        mFingerprintManager = getFingerprintManager(context);
        /**
         * 获取设备是否支持指纹识别
         */
        isSupport = (mFingerprintManager != null && isHardwareDetected());
        Log.i("TAG", "fingerprint isSupport: " + isSupport);
        initCryptoObject();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void initCryptoObject() {
        try {
            mCryptoObjectCreator = new CryptoObjectCreator(new CryptoObjectCreator.ICryptoObjectCreateListener() {
                @Override
                public void onDataPrepared(FingerprintManager.CryptoObject cryptoObject) {
                    // 如果需要一开始就进行指纹识别，可以在秘钥数据创建之后就启动指纹认证
//                     startAuthenticate(cryptoObject);
                }
            });
        } catch (Throwable e) {
            Log.i("TAG", "create cryptoObject failed!");
        }
    }

    /**
     * 是否有指纹识别硬件支持
     *
     * @return
     */
    public boolean isHardwareDetected() {
        try {
            return mFingerprintManager.isHardwareDetected();
        } catch (SecurityException e) {
        } catch (Throwable e) {
        }
        return false;
    }

    /**
     * 初始化指纹管理者
     *
     * @param context
     * @return
     */
    public static FingerprintManager getFingerprintManager(Context context) {
        FingerprintManager fingerprintManager = null;
        try {
            /**
             * 获取系统的指纹服务
             */
            fingerprintManager = (FingerprintManager) context.getSystemService(Context.FINGERPRINT_SERVICE);
        } catch (Throwable e) {
            Log.i("TAG", "have not class FingerprintManager");
        }
        return fingerprintManager;
    }


    public void setFingerprintManager(IFingerprintResultListener fingerprintResultListener) {
        mFpResultListener = new WeakReference<IFingerprintResultListener>(fingerprintResultListener);
    }

    public void startAuthenticate() {
        startAuthenticate(mCryptoObjectCreator.getCryptoObject());
    }

    public boolean isAuthenticating() {
        return mState == AUTHENTICATING;
    }

    private void startAuthenticate(FingerprintManager.CryptoObject cryptoObject) {
        prepareData();
        mState = AUTHENTICATING;
        try {
            mFingerprintManager.authenticate(cryptoObject, mCancellationSignal, 0, mAuthenticationCallback, null);
            notifyStartAuthenticateResult(true, "");
        } catch (SecurityException e) {
            try {
                mFingerprintManager.authenticate(null, mCancellationSignal, 0, mAuthenticationCallback, null);
                notifyStartAuthenticateResult(true, "");
            } catch (SecurityException e2) {
                notifyStartAuthenticateResult(false, Log.getStackTraceString(e2));
            } catch (Throwable throwable) {

            }
        } catch (Throwable throwable) {

        }
    }

    private void notifyStartAuthenticateResult(boolean isSuccess, String exceptionMsg) {
        if (isSuccess) {
            Log.i("TAG", "start authenticate...");
            if (mFpResultListener.get() != null) {
                mFpResultListener.get().onStartAuthenticateResult(true);
            }
        } else {
            Log.i("TAG", "startListening, Exception" + exceptionMsg);
            if (mFpResultListener.get() != null) {
                mFpResultListener.get().onStartAuthenticateResult(false);
            }
        }
    }

    private void notifyAuthenticationSucceeded() {
        Log.i("TAG", "onAuthenticationSucceeded");
        mFailedTimes = 0;
        if (null != mFpResultListener && null != mFpResultListener.get()) {
            mFpResultListener.get().onAuthenticateSuccess();
        }
    }

    private void notifyAuthenticationError(int errMsgId, CharSequence errString) {
        Log.i("TAG", "onAuthenticationError, errId:" + errMsgId + ", err:" + errString + ", retry after 30 seconds");
        if (null != mFpResultListener && null != mFpResultListener.get()) {
            mFpResultListener.get().onAuthenticateError(errMsgId);
        }
    }

    private void notifyAuthenticationFailed(int msgId, String errString) {
        Log.i("TAG", "onAuthenticationFailed, msdId: " + msgId + " errString: " + errString);
        if (null != mFpResultListener && null != mFpResultListener.get()) {
            mFpResultListener.get().onAuthenticateFailed(msgId);
        }
    }


    private void prepareData() {
        if (mCancellationSignal == null) {
            mCancellationSignal = new CancellationSignal();
        }
        if (mAuthenticationCallback == null) {
            mAuthenticationCallback = new FingerprintManager.AuthenticationCallback() {
                @Override
                public void onAuthenticationError(int errMsgId, CharSequence errString) {
                    // 多次指纹密码验证错误后，进入此方法；并且，不能短时间内调用指纹验证,一般间隔从几秒到几十秒不等
                    // 这种情况不建议重试，建议提示用户用其他的方式解锁或者认证
                    mState = NONE;
                    notifyAuthenticationError(errMsgId, errString);
                }

                @Override
                public void onAuthenticationHelp(int helpMsgId, CharSequence helpString) {
                    mState = NONE;
                    // 建议根据参数helpString返回值，并且仅针对特定的机型做处理，并不能保证所有厂商返回的状态一致
                    notifyAuthenticationFailed(helpMsgId, helpString.toString());
                    onFailedRetry(helpMsgId, helpString.toString());
                }

                @Override
                public void onAuthenticationFailed() {
                    mState = NONE;
                    notifyAuthenticationFailed(0, "onAuthenticationFailed");
                    onFailedRetry(-1, "onAuthenticationFailed");
                }

                @Override
                public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult result) {
                    mState = NONE;
                    notifyAuthenticationSucceeded();
                }
            };
        }
    }



    private void onFailedRetry(int msgId, String helpString) {
        mFailedTimes++;
        Log.i("TAG","on failed retry time " + mFailedTimes);
        if (mFailedTimes > 5) { // 每个验证流程最多重试5次，这个根据使用场景而定，验证成功时清0
            Log.i("TAG","on failed retry time more than 5 times");
            return;
        }
        Log.i("TAG","onFailedRetry: msgId " + msgId + " helpString: " + helpString);
        cancelAuthenticate();
        mHandler.removeCallbacks(mFailedRetryRunnable);
        mHandler.postDelayed(mFailedRetryRunnable, 300); // 每次重试间隔一会儿再启动
    }

    private Runnable mFailedRetryRunnable = new Runnable() {
        @Override
        public void run() {
            startAuthenticate(mCryptoObjectCreator.getCryptoObject());
        }
    };

    public void cancelAuthenticate() {
        if (mCancellationSignal != null && mState != CANCEL) {
            Log.i("TAG","cancelAuthenticate...");
            mState = CANCEL;
            mCancellationSignal.cancel();
            mCancellationSignal = null;
        }
    }

    public boolean isSupport() {
        return isSupport;
    }


    public void onDestroy() {
        cancelAuthenticate();
        mHandler = null;
        mAuthenticationCallback = null;
        mFpResultListener = null;
        mCancellationSignal = null;
        mFingerprintManager = null;
        if (mCryptoObjectCreator != null) {
            mCryptoObjectCreator.onDestroy();
            mCryptoObjectCreator = null;
        }
    }
}
