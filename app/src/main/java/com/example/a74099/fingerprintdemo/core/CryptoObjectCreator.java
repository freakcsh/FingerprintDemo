package com.example.a74099.fingerprintdemo.core;

import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

/**
 * 加密对象创建者
 * Created by 74099 on 2018/6/28.
 */

public class CryptoObjectCreator {
    private static final String KEY_NAME = "crypto_object_fingerprint_key";
    /**
     * CryptoObject
     * FingerprintManager支持的加密对象的包装类。
     * 目前该框架支持{@link Signature}，{@link Cipher}和{@link Mac}对象。
     */
    private FingerprintManager.CryptoObject mCryptoObject;
    /**
     * 密钥存储提供者
     */
    private KeyStore mKeyStore;
    /**
     * 密钥发生器
     */
    private KeyGenerator mKeyGenerator;
    /**
     * 加密
     */
    private Cipher mCipher;

    public interface ICryptoObjectCreateListener {
        void onDataPrepared(FingerprintManager.CryptoObject cryptoObject);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public CryptoObjectCreator(ICryptoObjectCreateListener createListener) {
        mKeyStore = providesKeystore();
        mKeyGenerator = providesKeyGenerator();
        mCipher = providesCipher(mKeyStore);
        if (mKeyStore != null && mKeyGenerator != null && mCipher != null) {
            mCryptoObject = new FingerprintManager.CryptoObject(mCipher);
        }
        prepareData(createListener);
    }

    private void prepareData(final ICryptoObjectCreateListener createListener) {
        new Thread("FingerprintLogic:InitThread") {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void run() {
                try {
                    if (mCryptoObject != null) {
                        createKey();
                        // Set up the crypto object for later. The object will be authenticated by use
                        // of the fingerprint.
                        /**
                         * 为以后设置crypto对象。该物体将通过使用指纹认证。
                         */
                        if (!initCipher()) {
                            Log.i("TAG","Failed to init Cipher.");
                        }
                    }
                } catch (Exception e) {
                    Log.i("TAG"," Failed to init Cipher, e:" + Log.getStackTraceString(e));
                }
                if (createListener != null) {
                    createListener.onDataPrepared(mCryptoObject);
                }
            }
        }.start();
    }

    /**
     * 在Android密钥存储库中创建一个对称密钥，该密钥只能在用户通过指纹验证后使用。
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void createKey() {
        // The enrolling flow for fingerprint. This is where you ask the user to set up fingerprint
        // for your flow. Use of keys is necessary if you need to know if the set of
        // enrolled fingerprints has changed.
        /**
         * 指纹注册流程。这是您要求用户为您的流设置指纹的地方。如果您需要知道登记的指纹是否已经更改，则需要使用密钥。
         */
        try {
            mKeyStore.load(null);
            // Set the alias of the entry in Android KeyStore where the key will appear
            // and the constrains (purposes) in the constructor of the Builder
            /**
             * 在Android KeyStore中设置条目的别名，其中显示密钥，构建器的构造函数中设置约束(目的)
             */
            mKeyGenerator.init(new KeyGenParameterSpec.Builder(KEY_NAME,
                    KeyProperties.PURPOSE_ENCRYPT |
                            KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    // Require the user to authenticate with a fingerprint to authorize every use
                    // of the key
                    /**
                     * 要求用户使用指纹进行身份验证，以授权每次使用密钥
                     */
                    .setUserAuthenticationRequired(true)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .build());
            mKeyGenerator.generateKey();
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException
                | CertificateException | IOException e) {
            Log.i("TAG"," Failed to createKey, e:" + Log.getStackTraceString(e));
            throw new RuntimeException(e);
        }
    }

    /**
     * 使用{@link #createKey()}方法中创建的键初始化{@link Cipher}实例。
     * @return @return {@code true}如果初始化成功，{@code false}如果锁定屏幕在密钥生成后已被禁用或重置，或者在生成密钥后注册了一个指纹。
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    private boolean initCipher() {
        try {
            mKeyStore.load(null);
            SecretKey key = (SecretKey) mKeyStore.getKey(KEY_NAME, null);
            mCipher.init(Cipher.ENCRYPT_MODE, key);
            return true;
        } catch (KeyPermanentlyInvalidatedException e) {
            Log.i("TAG"," Failed to initCipher, e:" + Log.getStackTraceString(e));
            return false;
        } catch (KeyStoreException | CertificateException | UnrecoverableKeyException | IOException
                | NoSuchAlgorithmException | InvalidKeyException e) {
            Log.i("TAG"," Failed to initCipher, e :" + Log.getStackTraceString(e));
            throw new RuntimeException("Failed to init Cipher", e);
        }
    }
    /**
     * 进行加密
     *
     * @param keyStore
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    private Cipher providesCipher(KeyStore keyStore) {
        try {
            return Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/"
                    + KeyProperties.BLOCK_MODE_CBC + "/"
                    + KeyProperties.ENCRYPTION_PADDING_PKCS7);
        } catch (Throwable e) {
            return null;
        }
    }

    /**
     * 密钥发生器初始化
     *
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    private KeyGenerator providesKeyGenerator() {
        try {
            return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        } catch (Throwable e) {
            return null;
        }
    }

    /***
     * 密钥存储提供者初始化
     * @return
     */
    private KeyStore providesKeystore() {
        try {
            return KeyStore.getInstance("AndroidKeyStore");
        } catch (KeyStoreException e) {
            e.printStackTrace();
            return null;
        }
    }

    public FingerprintManager.CryptoObject getCryptoObject() {
        return mCryptoObject;
    }

    public void onDestroy() {
        mCipher = null;
        mCryptoObject = null;
        mCipher = null;
        mKeyStore = null;
    }

}
