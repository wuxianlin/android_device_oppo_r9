/*
 * Copyright (C) 2015 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cyanogenmod.settings.device;

import android.app.ActivityManagerNative;
import android.app.KeyguardManager;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.TorchManager;
import android.media.session.MediaSessionLegacyHelper;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.WindowManagerGlobal;

import com.android.internal.os.DeviceKeyHandler;
import com.android.internal.util.ArrayUtils;

public class KeyHandler implements DeviceKeyHandler {

    private static final String TAG = KeyHandler.class.getSimpleName();
    private OppoScreenOffGestureUtil mGestureUtil = null;

    private static final String ACTION_DISMISS_KEYGUARD =
            "com.android.keyguard.action.DISMISS_KEYGUARD_SECURELY";

    private static final String KEY_GESTURE_HAPTIC_FEEDBACK =
            "touchscreen_gesture_haptic_feedback";

    private final Context mContext;
    private final PowerManager mPowerManager;
    private KeyguardManager mKeyguardManager;
    private TorchManager mTorchManager;
    WakeLock mGestureWakeLock;
    private Vibrator mVibrator;

    private static final int GESTURE_WAKELOCK_DURATION = 3000;

private int mBlackKeySettingState;

    public KeyHandler(Context context) {
        mContext = context;
        mGestureUtil = new OppoScreenOffGestureUtil(context);
        mPowerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        mGestureWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "GestureWakeLock");
        mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (mVibrator == null || !mVibrator.hasVibrator()) {
            mVibrator = null;
        }
    }

    public boolean handleKeyEvent(KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_UP) {
            return false;
        }
        mBlackKeySettingState = Settings.System.getInt(mContext.getContentResolver(), "blackscreen_gestrue_enable", 0);
        int keyCode = event.getKeyCode();
        if (mGestureUtil.isScreenoffGestureKey(keyCode)) {
            mGestureUtil.updateGestureInfo();
            int gestureType = mGestureUtil.mGestureType;
            Log.i(TAG, "The gesture is  : " + gestureType);
            if (gestureType == 1 & getOffset(mBlackKeySettingState,7)==1) {
                if(!mPowerManager.isScreenOn()){
                    mPowerManager.wakeUpWithProximityCheck(SystemClock.uptimeMillis());
                    doHapticFeedback();
                }
            } else if (gestureType == 2 & getOffset(mBlackKeySettingState,0)==1) {
                ensureTorchManager();
                mGestureWakeLock.acquire(GESTURE_WAKELOCK_DURATION);
                mTorchManager.toggleTorch();
                doHapticFeedback();
            } else if (gestureType == 3) {
                Log.i(TAG, "The gesture is ^ : " + gestureType);
            } else if (gestureType == 4 & getOffset(mBlackKeySettingState,3)==1) {
                dispatchMediaKeyWithWakeLockToMediaSession(KeyEvent.KEYCODE_MEDIA_NEXT);
                Log.i(TAG, "The gesture is > : " + gestureType);
                doHapticFeedback();
            } else if (gestureType == 5 & getOffset(mBlackKeySettingState,4)==1) {
                dispatchMediaKeyWithWakeLockToMediaSession(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
                Log.i(TAG, "The gesture is < : " + gestureType);
                doHapticFeedback();
            } else if (gestureType == 6 & getOffset(mBlackKeySettingState,6)==1) {
                ensureKeyguardManager();
                final String action;
                mGestureWakeLock.acquire(GESTURE_WAKELOCK_DURATION);
                if (mKeyguardManager.isKeyguardSecure() && mKeyguardManager.isKeyguardLocked()) {
                    action = MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE;
                } else {
                    mContext.sendBroadcastAsUser(new Intent(ACTION_DISMISS_KEYGUARD),
                            UserHandle.CURRENT);
                    action = MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA;
                }
                mPowerManager.wakeUp(SystemClock.uptimeMillis());
                Intent intent = new Intent(action, null);
                startActivitySafely(intent);
                doHapticFeedback();
            } else if (gestureType == 7 & getOffset(mBlackKeySettingState,1)==1) {
                dispatchMediaKeyWithWakeLockToMediaSession(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
                Log.i(TAG, "The gesture is || : " + gestureType);
                doHapticFeedback();
            } else if (gestureType == 8) {
                Log.i(TAG, "The gesture is -> : " + gestureType);
            } else if (gestureType == 9) {
                Log.i(TAG, "The gesture is <- : " + gestureType);
            } else if (gestureType == 10) {
                Log.i(TAG, "The gesture is |v : " + gestureType);
            } else if (gestureType == 11) {
                Log.i(TAG, "The gesture is |^ : " + gestureType);
            } else if (gestureType == 12) {
                Log.i(TAG, "The gesture is M : " + gestureType);
            } else if (gestureType == 13) {
                Log.i(TAG, "The gesture is W : " + gestureType);
            } else {
                Log.i(TAG, "Screen off gesture received. But can't be recognized!");
            }
            return true;
        }
        return false;
    }

    public static int getOffset(int num, int index) {
        return ((1 << index) & num) >> index;
    }

    private void ensureKeyguardManager() {
        if (mKeyguardManager == null) {
            mKeyguardManager =
                    (KeyguardManager) mContext.getSystemService(Context.KEYGUARD_SERVICE);
        }
    }

    private void ensureTorchManager() {
        if (mTorchManager == null) {
            mTorchManager = (TorchManager) mContext.getSystemService(Context.TORCH_SERVICE);
        }
    }

    private void dispatchMediaKeyWithWakeLockToMediaSession(int keycode) {
        MediaSessionLegacyHelper helper = MediaSessionLegacyHelper.getHelper(mContext);
        if (helper != null) {
            KeyEvent event = new KeyEvent(SystemClock.uptimeMillis(),
                    SystemClock.uptimeMillis(), KeyEvent.ACTION_DOWN, keycode, 0);
            helper.sendMediaButtonEvent(event, true);
            event = KeyEvent.changeAction(event, KeyEvent.ACTION_UP);
            helper.sendMediaButtonEvent(event, true);
        } else {
            Log.w(TAG, "Unable to send media key event");
        }
    }

    private void startActivitySafely(Intent intent) {
        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_SINGLE_TOP
                | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        try {
            UserHandle user = new UserHandle(UserHandle.USER_CURRENT);
            mContext.startActivityAsUser(intent, null, user);
        } catch (ActivityNotFoundException e) {
            // Ignore
        }
    }

    private void doHapticFeedback() {
        if (mVibrator == null) {
            return;
        }
        boolean enabled = Settings.System.getInt(mContext.getContentResolver(),
                KEY_GESTURE_HAPTIC_FEEDBACK, 1) != 0;
        if (enabled) {
            mVibrator.vibrate(50);
        }
    }
}
