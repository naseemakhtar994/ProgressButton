package com.nihaskalam.progressbuttonlibrary;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.util.Property;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

class CircularAnimatedDrawable extends Drawable implements Animatable {

    private static final Interpolator ANGLE_INTERPOLATOR = new LinearInterpolator();
    private static final Interpolator SWEEP_INTERPOLATOR = new AccelerateDecelerateInterpolator();
    private static final int ANGLE_ANIMATOR_DURATION = 1500;
    private static final int SWEEP_ANIMATOR_DURATION = 800;
    public static final int MIN_SWEEP_ANGLE = 30;
    private static final float ANGLE_MULTIPLIER = 3.6f;
    public static final int DURATION_INSTANT = 1;
    private final RectF fBounds = new RectF();

    private ObjectAnimator mObjectAnimatorSweep;
    //    private ObjectAnimator mObjectAnimatorAngle;
    private boolean mModeAppearing;
    private Paint mPaint;
    private float mCurrentGlobalAngleOffset;
    private float mCurrentGlobalAngle;
    private float mCurrentSweepAngle;
    private float mBorderWidth;
    private boolean mRunning;
    private boolean mIndeterminateProgressMode;
    private OnAnimationEndListener mListener;
    private int mCustomSweepDuration;
    //    private OnAnimationTimeUpdateListener onAnimationTimeUpdateListener;
    private float cancelButtonSpokeLength;
    private float maxAngle = 360f;
    private float minAngle = 0f;
    private boolean customProgressMode = false;
    private float customProgress = -1;
    private OnAnimationUpdateListener onAnimationUpdateListener;

    public CircularAnimatedDrawable(int color, float borderWidth, boolean indeterminateProgressMode) {
        mBorderWidth = borderWidth;
        mIndeterminateProgressMode = indeterminateProgressMode;
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(borderWidth);
        mPaint.setColor(color);
    }

    @Override
    public void draw(Canvas canvas) {
//        float startAngle = mCurrentGlobalAngle - mCurrentGlobalAngleOffset;
//        float sweepAngle = mCurrentSweepAngle;
//        if (!mModeAppearing) {
//            startAngle = startAngle + sweepAngle;
//            sweepAngle = 360 - sweepAngle - MIN_SWEEP_ANGLE;
//        } else {
//            sweepAngle += MIN_SWEEP_ANGLE;
//        }
//        sweepAngle = 360 - startAngle;

//        mPaint.setColor(color);
        canvas.drawArc(fBounds, 270, mCurrentSweepAngle, false, mPaint);
        cancelButtonSpokeLength = (int) fBounds.width() / 3;
        canvas.drawLine(fBounds.left + cancelButtonSpokeLength, fBounds.bottom - cancelButtonSpokeLength, fBounds.right - cancelButtonSpokeLength, fBounds.top + cancelButtonSpokeLength, mPaint);
        canvas.drawLine(fBounds.left + cancelButtonSpokeLength, fBounds.top + cancelButtonSpokeLength, fBounds.right - cancelButtonSpokeLength, fBounds.bottom - cancelButtonSpokeLength, mPaint);
    }

    @Override
    public void setAlpha(int alpha) {
        mPaint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        mPaint.setColorFilter(cf);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSPARENT;
    }

    private void toggleAppearingMode() {
        mModeAppearing = !mModeAppearing;
        if (mModeAppearing) {
            mCurrentGlobalAngleOffset = (mCurrentGlobalAngleOffset + MIN_SWEEP_ANGLE * 2) % 360;
        }
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        fBounds.left = bounds.left + mBorderWidth / 2f + .5f;
        fBounds.right = bounds.right - mBorderWidth / 2f - .5f;
        fBounds.top = bounds.top + mBorderWidth / 2f + .5f;
        fBounds.bottom = bounds.bottom - mBorderWidth / 2f - .5f;
    }

    private Property<CircularAnimatedDrawable, Float> mAngleProperty =
            new Property<CircularAnimatedDrawable, Float>(Float.class, "angle") {
                @Override
                public Float get(CircularAnimatedDrawable object) {
                    return object.getCurrentGlobalAngle();
                }

                @Override
                public void set(CircularAnimatedDrawable object, Float value) {
                    object.setCurrentGlobalAngle(value);
                }
            };

    private Property<CircularAnimatedDrawable, Float> mSweepProperty
            = new Property<CircularAnimatedDrawable, Float>(Float.class, "outerCircleRadiusProgress") {
        @Override
        public Float get(CircularAnimatedDrawable object) {
            return object.getCurrentSweepAngle();
        }

        @Override
        public void set(CircularAnimatedDrawable object, Float value) {
            if (onAnimationUpdateListener != null)
                onAnimationUpdateListener.onAnimationValueUpdate(value);
            object.setCurrentSweepAngle(value);
        }
    };

    public void initAnimations() {
        setupAnimations(customProgressMode ? minAngle : maxAngle);
    }

    private void setupAnimations(float angleToDraw) {
//        mObjectAnimatorAngle = ObjectAnimator.ofFloat(this, mAngleProperty, 360f);
//        mObjectAnimatorAngle.setInterpolator(ANGLE_INTERPOLATOR);
//        mObjectAnimatorAngle.setDuration(ANGLE_ANIMATOR_DURATION);
//        mObjectAnimatorAngle.setRepeatMode(ValueAnimator.RESTART);
//        mObjectAnimatorAngle.setRepeatCount(ValueAnimator.INFINITE);

        mObjectAnimatorSweep = ObjectAnimator.ofFloat(this, mSweepProperty, angleToDraw);// - MIN_SWEEP_ANGLE * 2);
        mObjectAnimatorSweep.setInterpolator(SWEEP_INTERPOLATOR);
        if (mIndeterminateProgressMode) {
            mObjectAnimatorSweep.setDuration(SWEEP_ANIMATOR_DURATION);
            mObjectAnimatorSweep.setRepeatMode(ValueAnimator.RESTART);
            mObjectAnimatorSweep.setRepeatCount(ValueAnimator.INFINITE);
        } else {
            mObjectAnimatorSweep.setDuration(mCustomSweepDuration == -1 ? SWEEP_ANIMATOR_DURATION : mCustomSweepDuration);
        }
        mObjectAnimatorSweep.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (customProgressMode) {
                    if (customProgress != 360) {
                        return;
                    }
                }
                if (mListener != null) {
                    mListener.onAnimationEnd();
//                    mCurrentSweepAngle = 0;
//                    invalidateSelf();
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {
                toggleAppearingMode();
            }
        });
        mObjectAnimatorSweep.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                if (customProgressMode) {
                    return;
                }
                if (onAnimationUpdateListener != null)
                    onAnimationUpdateListener.onAnimationTimeUpdate((int) valueAnimator.getCurrentPlayTime(), mCustomSweepDuration);

            }
        });
    }

    @Override
    public void start() {
        if (isRunning()) {
            return;
        }
        mRunning = true;
//        mObjectAnimatorAngle.start();
        mObjectAnimatorSweep.start();
        invalidateSelf();
    }

    @Override
    public void stop() {
        if (!isRunning()) {
            return;
        }
        mRunning = false;
//        mObjectAnimatorAngle.cancel();
        mObjectAnimatorSweep.cancel();
        invalidateSelf();
    }

    @Override
    public boolean isRunning() {
        return mRunning;
    }

    public void setCurrentGlobalAngle(float currentGlobalAngle) {
        mCurrentGlobalAngle = currentGlobalAngle;
        invalidateSelf();
    }

    public float getCurrentGlobalAngle() {
        return mCurrentGlobalAngle;
    }

    public void setCurrentSweepAngle(float currentSweepAngle) {
        mCurrentSweepAngle = currentSweepAngle;
        invalidateSelf();
    }

    public void setCurrentSweepAngleAndTimeRemaining(float currentSweepAngle, int timeRemaining) {
        mCurrentSweepAngle = currentSweepAngle;
        mCustomSweepDuration = timeRemaining;
        stop();
        initAnimations();
        start();
    }

    public float getCurrentSweepAngle() {
        return mCurrentSweepAngle;
    }

    public void setListener(OnAnimationEndListener listener) {
        mListener = listener;
    }

//    public void setOnAnimationTimeUpdateListener(OnAnimationTimeUpdateListener onAnimationTimeUpdateListener) {
//        this.onAnimationTimeUpdateListener = onAnimationTimeUpdateListener;
//    }

    public void drawProgress(int angle) {
        stop();
        customProgress = angle * ANGLE_MULTIPLIER > maxAngle ? angle * ANGLE_MULTIPLIER - maxAngle : angle * ANGLE_MULTIPLIER;
        setupAnimations(customProgress);
        start();
    }

    public void drawProgress(float angle) {
        stop();
        customProgress = angle;
        mCustomSweepDuration = DURATION_INSTANT;
        setupAnimations(customProgress);
        start();
        mCustomSweepDuration = -1;
    }


    public void setCustomProgressMode(boolean customProgressMode) {
        this.customProgressMode = customProgressMode;
    }

    public void setmCustomSweepDuration(int mCustomSweepDuration) {
        this.mCustomSweepDuration = mCustomSweepDuration;
    }

    public void setOnAnimationUpdateListener(OnAnimationUpdateListener onAnimationUpdateListener) {
        this.onAnimationUpdateListener = onAnimationUpdateListener;
    }
}