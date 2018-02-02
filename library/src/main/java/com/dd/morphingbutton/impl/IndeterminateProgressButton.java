package com.dd.morphingbutton.impl;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AnimationUtils;

import com.dd.morphingbutton.MorphingAnimation;
import com.dd.morphingbutton.MorphingButton;
import com.dd.morphingbutton.R;

public class IndeterminateProgressButton extends MorphingButton {

	private int[] mColors;
	private int mProgressCornerRadius;
	private ProgressBar mProgressBar;

	private boolean mIsRunning;

	public IndeterminateProgressButton(Context context) {
		super(context);
		init(context);
	}

	public IndeterminateProgressButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public IndeterminateProgressButton(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(context);
	}

	private void init(Context context) {
		mColors = new int[]{ContextCompat.getColor(context, R.color.mb_gray),
				ContextCompat.getColor(context, R.color.mb_blue)};

		if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN_MR2) {
			// clipPath only available on hardware for 18+
			setLayerType(LAYER_TYPE_SOFTWARE, null);
		}
	}

	@Override
	protected void onDraw(@NonNull Canvas canvas) {
		super.onDraw(canvas);

		if (!mAnimationInProgress && mIsRunning) {
			if (mProgressBar == null) {
				mProgressBar = new ProgressBar(this);
				setupProgressBarBounds();
				mProgressBar.setColorScheme(mColors);
				mProgressBar.start();
			}
			mProgressBar.draw(canvas);
		}
	}

	@Override
	public void morph(@NonNull Params params) {
		mIsRunning = false;
		super.morph(params);
	}

	public void morphToProgress(int backgroundColor, int progressCornerRadius, int width,
	                            int height, int duration, @NonNull @ColorInt int... colors) {
		mProgressCornerRadius = progressCornerRadius;
		mColors = colors;

		Params longRoundedSquare =
				Params.create().duration(duration).cornerRadius(mProgressCornerRadius).width(width)
						.height(height).color(backgroundColor).colorPressed(backgroundColor)
						.animationListener(new MorphingAnimation.Listener() {

							@Override
							public void onAnimationEnd() {
								mIsRunning = true;
								invalidate();
							}
						});
		morph(longRoundedSquare);
	}

	private void setupProgressBarBounds() {
		double indicatorHeight = getHeight();
		int bottom = (int) (getMeasuredHeight() - indicatorHeight);
		mProgressBar.setBounds(0, bottom, getMeasuredWidth(), getMeasuredHeight(),
				mProgressCornerRadius);
	}

	public static class ProgressBar {

		// Default progress animation colors are grays.
		private final static int COLOR1 = 0xB3000000;
		private final static int COLOR2 = 0x80000000;
		private final static int COLOR3 = 0x4d000000;
		private final static int COLOR4 = 0x1a000000;

		// The duration of the animation cycle.
		private static final int ANIMATION_DURATION_MS = 2000;

		// The duration of the animation to clear the bar.
		private static final int FINISH_ANIMATION_DURATION_MS = 1000;

		// Interpolator for varying the speed of the animation.
		private static final android.view.animation.Interpolator INTERPOLATOR =
				new AccelerateDecelerateInterpolator();

		private final Paint mPaint = new Paint();
		private final RectF mClipRect = new RectF();
		private float mTriggerPercentage;
		private long mStartTime;
		private long mFinishTime;
		private boolean mRunning;

		// Colors used when rendering the animation,
		private int[] mColors;
		private int mCornerRadius;
		private View mParent;

		private RectF mBounds = new RectF();

		private ProgressBar(View parent) {
			mParent = parent;
			mColors = new int[]{COLOR1, COLOR2, COLOR3, COLOR4};
		}

		/**
		 * Set the four colors used in the progress animation. The first color will
		 * also be the color of the bar that grows in response to a user swipe
		 * gesture.
		 *
		 * @param colors Integer representation of a color.
		 */
		void setColorScheme(@NonNull @ColorInt int... colors) {
			mColors = colors;
		}

		/**
		 * Start showing the progress animation.
		 */
		void start() {
			if (!mRunning) {
				mTriggerPercentage = 0;
				mStartTime = AnimationUtils.currentAnimationTimeMillis();
				mRunning = true;
				mParent.postInvalidate();
			}
		}

		void draw(Canvas canvas) {
			Path clipPath = new Path();
			clipPath.addRoundRect(mBounds, mCornerRadius, mCornerRadius, Path.Direction.CW);

			final int width = (int) mBounds.width();
			final int height = (int) mBounds.height();
			final int cx = width / 2;
			final int cy = height / 2;
			boolean drawTriggerWhileFinishing = false;
			int restoreCount = canvas.save();
			canvas.clipPath(clipPath);

			if (mRunning || (mFinishTime > 0)) {
				long now = AnimationUtils.currentAnimationTimeMillis();
				long elapsed = (now - mStartTime) % ANIMATION_DURATION_MS;
				long iterations = (now - mStartTime) / ANIMATION_DURATION_MS;
				float rawProgress = (elapsed / (ANIMATION_DURATION_MS / 100f));

				// If we're not running anymore, that means we're running through
				// the finish animation.
				if (!mRunning) {
					// If the finish animation is done, don't draw anything, and
					// don't repost.
					if ((now - mFinishTime) >= FINISH_ANIMATION_DURATION_MS) {
						mFinishTime = 0;
						return;
					}

					// Otherwise, use a 0 opacity alpha layer to clear the animation
					// from the inside out. This layer will prevent the circles from
					// drawing within its bounds.
					long finishElapsed = (now - mFinishTime) % FINISH_ANIMATION_DURATION_MS;
					float finishProgress = (finishElapsed / (FINISH_ANIMATION_DURATION_MS / 100f));
					float pct = (finishProgress / 100f);
					// Radius of the circle is half of the screen.
					float clearRadius = width / 2 * INTERPOLATOR.getInterpolation(pct);
					mClipRect.set(cx - clearRadius, 0, cx + clearRadius, height);
					canvas.saveLayerAlpha(mClipRect, 0, 0);
					// Only draw the trigger if there is a space in the center of
					// this refreshing view that needs to be filled in by the
					// trigger. If the progress view is just still animating, let it
					// continue animating.
					drawTriggerWhileFinishing = true;
				}

				// First fill in with the last color that would have finished drawing.
				if (iterations == 0) {
					canvas.drawColor(mColors[0]);
				} else {
					if (rawProgress >= 0 && rawProgress < 25) {
						canvas.drawColor(mColors[3 % mColors.length]);
					} else if (rawProgress >= 25 && rawProgress < 50) {
						canvas.drawColor(mColors[0]);
					} else if (rawProgress >= 50 && rawProgress < 75) {
						canvas.drawColor(mColors[1 % mColors.length]);
					} else {
						canvas.drawColor(mColors[2 % mColors.length]);
					}
				}

				// Then draw up to 4 overlapping concentric circles of varying radii, based on how far
				// along we are in the cycle.
				// progress 0-50 draw mColor2
				// progress 25-75 draw mColor3
				// progress 50-100 draw mColor4
				// progress 75 (wrap to 25) draw mColor1
				if ((rawProgress >= 0 && rawProgress <= 25)) {
					float pct = (((rawProgress + 25) * 2) / 100f);
					drawCircle(canvas, cx, cy, mColors[0], pct);
				}
				if (rawProgress >= 0 && rawProgress <= 50) {
					float pct = ((rawProgress * 2) / 100f);
					drawCircle(canvas, cx, cy, mColors[1 % mColors.length], pct);
				}
				if (rawProgress >= 25 && rawProgress <= 75) {
					float pct = (((rawProgress - 25) * 2) / 100f);
					drawCircle(canvas, cx, cy, mColors[2 % mColors.length], pct);
				}
				if (rawProgress >= 50 && rawProgress <= 100) {
					float pct = (((rawProgress - 50) * 2) / 100f);
					drawCircle(canvas, cx, cy, mColors[3 % mColors.length], pct);
				}
				if ((rawProgress >= 75 && rawProgress <= 100)) {
					float pct = (((rawProgress - 75) * 2) / 100f);
					drawCircle(canvas, cx, cy, mColors[0], pct);
				}
				if (mTriggerPercentage > 0 && drawTriggerWhileFinishing) {
					// There is some portion of trigger to draw. Restore the canvas,
					// then draw the trigger. Otherwise, the trigger does not appear
					// until after the bar has finished animating and appears to
					// just jump in at a larger width than expected.
					canvas.restoreToCount(restoreCount);
					restoreCount = canvas.save();
					canvas.clipPath(clipPath);
					drawTrigger(canvas, cx, cy);
				}
				// Keep running until we finish out the last cycle.
				ViewCompat.postInvalidateOnAnimation(mParent);
			} else {
				// Otherwise if we're in the middle of a trigger, draw that.
				if (mTriggerPercentage > 0 && mTriggerPercentage <= 1.0) {
					drawTrigger(canvas, cx, cy);
				}
			}
			canvas.restoreToCount(restoreCount);
		}

		private void drawTrigger(Canvas canvas, int cx, int cy) {
			mPaint.setColor(mColors[0]);
			canvas.drawCircle(cx, cy, cx * mTriggerPercentage, mPaint);
		}

		/**
		 * Draws a circle centered in the view.
		 *
		 * @param canvas the canvas to draw on
		 * @param cx     the center x coordinate
		 * @param cy     the center y coordinate
		 * @param color  the color to draw
		 * @param pct    the percentage of the view that the circle should cover
		 */
		private void drawCircle(Canvas canvas, float cx, float cy, int color, float pct) {
			mPaint.setColor(color);
			canvas.save();
			canvas.translate(cx, cy);
			float radiusScale = INTERPOLATOR.getInterpolation(pct);
			canvas.scale(radiusScale, radiusScale);
			canvas.drawCircle(0, 0, cx, mPaint);
			canvas.restore();
		}

		/**
		 * Set the drawing bounds of this SwipeProgressBar.
		 */
		void setBounds(int left, int top, int right, int bottom, int cornerRadius) {
			mBounds.left = left;
			mBounds.top = top;
			mBounds.right = right;
			mBounds.bottom = bottom;
			mCornerRadius = cornerRadius;
		}
	}

}
