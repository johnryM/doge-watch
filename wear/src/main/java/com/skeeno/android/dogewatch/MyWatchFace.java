/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.skeeno.android.dogewatch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.view.SurfaceHolder;

import com.skeeno.android.dogewatch.Utils.WatchUtils;

import java.lang.ref.WeakReference;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Analog watch face with a ticking second hand. In ambient mode, the second hand isn't shown. On
 * devices with low-bit ambient mode, the hands are drawn without anti-aliasing in ambient mode.
 */
public class MyWatchFace extends CanvasWatchFaceService {
    /**
     * Update rate in milliseconds for interactive mode. We update once a second to advance the
     * second hand.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;

    private WatchUtils watchUtils = new WatchUtils();

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private static class EngineHandler extends Handler {
        private final WeakReference<MyWatchFace.Engine> mWeakReference;

        public EngineHandler(MyWatchFace.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            MyWatchFace.Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }

    private class Engine extends CanvasWatchFaceService.Engine {
        final Handler mUpdateTimeHandler = new EngineHandler(this);
        boolean mRegisteredTimeZoneReceiver = false;
        Paint mBackgroundPaint;
        Paint mHandPaint;
        Paint mBigTickPaint;
        Paint mSmallTickPaint;
        Paint mDigitalTimePaint;
        Paint mFuzzyTimePaint;
        Paint mDatePaint;
        Paint mWowPaint;
        Typeface mTypeface;
        boolean mAmbient;
        Time mTime;
        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mTime.clear(intent.getStringExtra("time-zone"));
                mTime.setToNow();
            }
        };
        int mTapCount;

        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        boolean mLowBitAmbient;

        Bitmap mBackgroundBitmap;
        Bitmap mScaledBackgroundBitmap;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(MyWatchFace.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .setAcceptsTapEvents(true)
                    .build());

            Resources resources = MyWatchFace.this.getResources();

            //load image
            Drawable backgroundDrawable = resources.getDrawable(R.drawable.doge_face, null);
            mBackgroundBitmap = ((BitmapDrawable) backgroundDrawable).getBitmap();

            //comic sans typeface
            mTypeface = Typeface.createFromAsset(getBaseContext().getAssets(), "fonts/comic.ttf");

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(resources.getColor(R.color.background));

            mHandPaint = new Paint();
            mHandPaint.setColor(Color.DKGRAY);
            mHandPaint.setStrokeWidth(resources.getDimension(R.dimen.analog_hand_stroke));
            mHandPaint.setAntiAlias(true);
            mHandPaint.setStrokeCap(Paint.Cap.ROUND);

            mBigTickPaint = new Paint();
            mBigTickPaint.setColor(Color.BLACK);
            mBigTickPaint.setStrokeWidth(resources.getDimension(R.dimen.analog_hand_big_tick));
            mBigTickPaint.setAntiAlias(true);
            mBigTickPaint.setStrokeCap(Paint.Cap.ROUND);

            mSmallTickPaint = new Paint();
            mSmallTickPaint.setColor(Color.BLACK);
            mSmallTickPaint.setStrokeWidth(resources.getDimension(R.dimen.analog_hand_small_tick));
            mSmallTickPaint.setAntiAlias(true);
            mSmallTickPaint.setStrokeCap(Paint.Cap.ROUND);

            mDigitalTimePaint = new Paint();
            mDigitalTimePaint.setARGB(255, 134, 225, 0);
            mDigitalTimePaint.setStrokeWidth(5.f);
            mDigitalTimePaint.setTextSize(28);
            mDigitalTimePaint.setTypeface(mTypeface);
            mDigitalTimePaint.setStyle(Paint.Style.FILL);
            mDigitalTimePaint.setAntiAlias(true);

            mFuzzyTimePaint = new Paint();
            mFuzzyTimePaint.setARGB(255, 6, 215, 255);
            mFuzzyTimePaint.setStrokeWidth(5.f);
            mFuzzyTimePaint.setTextSize(24);
            mFuzzyTimePaint.setTypeface(mTypeface);
            mFuzzyTimePaint.setStyle(Paint.Style.FILL);
            mFuzzyTimePaint.setAntiAlias(true);

            mDatePaint = new Paint();
            mDatePaint.setARGB(255, 255, 17, 0);
            mDatePaint.setStrokeWidth(5.f);
            mDatePaint.setTextSize(24);
            mDatePaint.setTypeface(mTypeface);
            mDatePaint.setStyle(Paint.Style.FILL);
            mDatePaint.setAntiAlias(true);

            mWowPaint = new Paint();
            mWowPaint.setARGB(255, 255, 255, 255);
            mWowPaint.setStrokeWidth(5.f);
            mWowPaint.setTextSize(30);
            mWowPaint.setTypeface(mTypeface);
            mWowPaint.setStyle(Paint.Style.FILL);
            mWowPaint.setAntiAlias(true);
            mWowPaint.setTextAlign(Paint.Align.CENTER);

            mTime = new Time();
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (mAmbient != inAmbientMode) {
                mAmbient = inAmbientMode;
                if (mLowBitAmbient) {
                    mHandPaint.setAntiAlias(!inAmbientMode);
                }
                invalidate();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        /**
         * Captures tap event (and tap type) and toggles the background color if the user finishes
         * a tap.
         */
        @Override
        public void onTapCommand(int tapType, int x, int y, long eventTime) {
            Resources resources = MyWatchFace.this.getResources();
            switch (tapType) {
                case TAP_TYPE_TOUCH:
                    // The user has started touching the screen.
                    break;
                case TAP_TYPE_TOUCH_CANCEL:
                    // The user has started a different gesture or otherwise cancelled the tap.
                    break;
                case TAP_TYPE_TAP:
                    // The user has completed the tap gesture.
                    mTapCount++;
                    mBackgroundPaint.setColor(resources.getColor(mTapCount % 2 == 0 ?
                            R.color.background : R.color.background2));
                    break;
            }
            invalidate();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            mTime.setToNow();

            // Find the center. Ignore the window insets so that, on round watches with a
            // "chin", the watch face is centered on the entire screen, not just the usable
            // portion.
            float centerX = bounds.width() / 2f;
            float centerY = bounds.height() / 2f;

            //draw digital watchface when ambient or analogue w/ face on active
            if (isInAmbientMode()) {
                canvas.drawColor(Color.BLACK);
            } else {
                canvas.drawBitmap(mScaledBackgroundBitmap, 0, 0, null);
                drawWatchTicks(centerX, centerY, canvas);

                float secRot = mTime.second / 30f * (float) Math.PI;
                int minutes = mTime.minute;
                float minRot = minutes / 30f * (float) Math.PI;
                float hrRot = ((mTime.hour + (minutes / 60f)) / 6f) * (float) Math.PI;

                float secLength = centerX - 20;
                float minLength = centerX - 40;
                float hrLength = centerX - 80;

                if (!mAmbient) {
                    float secX = (float) Math.sin(secRot) * secLength;
                    float secY = (float) -Math.cos(secRot) * secLength;
                    canvas.drawLine(centerX, centerY, centerX + secX, centerY + secY, mHandPaint);
                }

                float minX = (float) Math.sin(minRot) * minLength;
                float minY = (float) -Math.cos(minRot) * minLength;
                canvas.drawLine(centerX, centerY, centerX + minX, centerY + minY, mHandPaint);

                float hrX = (float) Math.sin(hrRot) * hrLength;
                float hrY = (float) -Math.cos(hrRot) * hrLength;
                canvas.drawLine(centerX, centerY, centerX + hrX, centerY + hrY, mHandPaint);
            }

            //fuzzy time
            String fuzzyTime = watchUtils.getFuzzTimeString(getBaseContext(), mTime);
            float ftWidth = mDigitalTimePaint.measureText(fuzzyTime);
            float ftx = (bounds.width() - ftWidth) / 2 - 50;
            float fty = bounds.height() / 2 - 80;
            canvas.drawText(fuzzyTime, ftx, fty, mFuzzyTimePaint);

            // digital time
            String digitalTime = "So " + watchUtils.getFormattedDigitalTime(mTime);
            float dtWidth = mDigitalTimePaint.measureText(digitalTime);
            float dtx = (bounds.width() - dtWidth) / 2 + 80;
            float dty = bounds.height() / 2 - 30;
            canvas.drawText(digitalTime, dtx, dty, mDigitalTimePaint);

            // date
            String dateString = "much " + watchUtils.getFormattedDate(mTime);
            float dsWidth = mDigitalTimePaint.measureText(digitalTime);
            float dsx = (bounds.width() - dsWidth) / 2 - 50;
            float dsy = bounds.height() / 2 + 65;
            canvas.drawText(dateString, dsx, dsy, mDatePaint);

            // wow
            String wowString = getString(R.string.wow_string);
            float wowWidth = mDigitalTimePaint.measureText(digitalTime);
            float wowx = bounds.width() / 2;
            float wowy = bounds.height() / 2 + 120;
            canvas.drawText(wowString, wowx, wowy, mWowPaint);
        }

        @Override
        public void onSurfaceChanged( SurfaceHolder holder, int format, int width, int height) {
            if (mScaledBackgroundBitmap == null
                    || mScaledBackgroundBitmap.getWidth() != width
                    || mScaledBackgroundBitmap.getHeight() != height) {
                mScaledBackgroundBitmap = Bitmap.createScaledBitmap(mBackgroundBitmap,
                        width, height, true);
            }
            super.onSurfaceChanged(holder, format, width, height);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();

                // Update time zone in case it changed while we weren't visible.
                mTime.clear(TimeZone.getDefault().getID());
                mTime.setToNow();
            } else {
                unregisterReceiver();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            MyWatchFace.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            MyWatchFace.this.unregisterReceiver(mTimeZoneReceiver);
        }

        /**
         * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer should
         * only run when we're visible and in interactive mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        private void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS
                        - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }

        private void drawWatchTicks(float centerX, float centerY, Canvas canvas) {
            double sinVal;
            double cosVal;
            double angle;
            float length1;
            float length2;
            float x1, y1, x2, y2;

            length1 = centerX - 25;
            length2 = centerX;
            for (int i = 0; i < 60; i++) {
                angle = (i * Math.PI * 2 / 60);
                sinVal = Math.sin(angle);
                cosVal = Math.cos(angle);
                float len = (i % 5 == 0) ? length1 : (length1 + 15);
                x1 = (float)(sinVal * len);
                y1 = (float)(-cosVal * len);
                x2 = (float)(sinVal * length2);
                y2 = (float)(-cosVal * length2);

                if (i % 5 == 0) {
                    canvas.drawLine(centerX + x1, centerY + y1, centerX + x2, centerY + y2, mBigTickPaint);
                } else {
                    canvas.drawLine(centerX + x1, centerY + y1, centerX + x2, centerY + y2, mSmallTickPaint);
                }
            }
        }


    }
}
