package com.dd.morphingbutton.impl;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.CircularProgressDrawable;
import android.util.AttributeSet;

import com.dd.morphingbutton.MorphingAnimation;
import com.dd.morphingbutton.MorphingButton;
import com.dd.morphingbutton.R;

public class CircularProgressButton extends MorphingButton {

	private int[] mProgressColors;

	private CircularProgressDrawable mAnimatedDrawable;

	private boolean mIsRunning;

	private float mStrokeWidth;
	private int mPadding;

	public CircularProgressButton(Context context) {
		super(context);
		init(context);
	}

	public CircularProgressButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public CircularProgressButton(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(context);
	}

	private void init(Context context) {
		mProgressColors = new int[]{ContextCompat.getColor(context, R.color.mb_gray)};

		if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN_MR2) {
			// clipPath only available on hardware for 18+
			setLayerType(LAYER_TYPE_SOFTWARE, null);
		}
	}

	@Override
	protected void onDraw(@NonNull Canvas canvas) {
		super.onDraw(canvas);
		if (!mAnimationInProgress && mIsRunning) {
			drawIndeterminateProgress(canvas);
		}
	}

	@Override
	public void morph(@NonNull Params params) {
		mIsRunning = false;
		super.morph(params);
	}

	public void morphToProgress(int backgroundColor, int shapeRadius, float strokeWidth, int padding,
	                            int width, int height, int duration, @NonNull @ColorInt int... progressColors) {
		mStrokeWidth = strokeWidth;
		mPadding = padding;
		if (progressColors.length > 0) {
			mProgressColors = progressColors;
		}

		Params circle =
				Params.create().duration(duration).cornerRadius(shapeRadius).width(width)
						.height(height).color(backgroundColor).colorPressed(backgroundColor)
						.animationListener(new MorphingAnimation.Listener() {

							@Override
							public void onAnimationEnd() {
								mIsRunning = true;
								invalidate();
							}
						});
		morph(circle);
	}

	private void setupProgressBarBounds() {
		int offset = (getWidth() - getHeight()) / 2;
		int left = offset + mPadding;
		int right = getWidth() - offset - mPadding;
		int bottom = getHeight() - mPadding;
		int top = mPadding;
		mAnimatedDrawable.setBounds(left, top, right, bottom);
	}

	public void stopIndeterminateProgress() {
		if (mAnimatedDrawable != null) {
			mAnimatedDrawable.stop();
		}
	}

	private void drawIndeterminateProgress(Canvas canvas) {
		if (mAnimatedDrawable == null) {
			mAnimatedDrawable = new CircularProgressDrawable(getContext());
			setupProgressBarBounds();
			if (mStrokeWidth > 0) {
				mAnimatedDrawable.setStrokeWidth(mStrokeWidth);
			}
			mAnimatedDrawable.setColorSchemeColors(mProgressColors);
			mAnimatedDrawable.setCallback(this);
			mAnimatedDrawable.start();
		} else {
			if (!mAnimatedDrawable.isRunning()) {
				mAnimatedDrawable.start();
			}
			mAnimatedDrawable.draw(canvas);
		}
		postInvalidate();
	}

}
