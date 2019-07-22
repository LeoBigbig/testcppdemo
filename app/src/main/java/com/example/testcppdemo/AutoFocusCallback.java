/*
 * Copyright (C) 2010 ZXing authors
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

package com.example.testcppdemo;

import android.hardware.Camera;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * 自动聚焦接口
 */
final class AutoFocusCallback implements Camera.AutoFocusCallback {

  private static final String TAG = AutoFocusCallback.class.getSimpleName();

  private static final long AUTOFOCUS_INTERVAL_MS = 1000L;//自动聚焦的时间间隔，每隔一秒聚焦一次

  private Handler autoFocusHandler;
  private int autoFocusMessage;
  /**
   * CameraActivity的handler绑定
   */
  void setHandler(Handler autoFocusHandler, int autoFocusMessage) {
    this.autoFocusHandler = autoFocusHandler;
    this.autoFocusMessage = autoFocusMessage;
  }

  /**
   * 接口回调方法，每隔一秒聚焦
   */
  public void onAutoFocus(boolean success, Camera camera) {
    Log.i(TAG, "onAutoFocus: ");
    if (autoFocusHandler != null) {
      Message message = autoFocusHandler.obtainMessage(autoFocusMessage, success);
      autoFocusHandler.sendMessageDelayed(message, AUTOFOCUS_INTERVAL_MS);
      autoFocusHandler = null;
    } else {
      Log.d(TAG, "Got auto-focus callback, but no handler for it");
    }
  }

}
