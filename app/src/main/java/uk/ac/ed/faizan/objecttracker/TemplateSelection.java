package uk.ac.ed.faizan.objecttracker;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class TemplateSelection extends View {

	private Paint mRectPaint;

	private int mStartX = 0;
	private int mStartY = 0;
	private int mEndX = 0;
	private int mEndY = 0;
	private boolean mDrawRect = false;
	private OnUpCallback mCallback = null;
	private boolean mClearCanvas = false;

	// mLeft is first x coordinate of rectangle, mTop is first y coordinate.
	private int mLeft = 0;
	private int mRight = 0;
	private int mTop = 0;
	private int mBottom = 0;



	public interface OnUpCallback {
		void onRectFinished(Rect rect);
	}

	public TemplateSelection(final Context context) {
		super(context);
		init();
	}

	public TemplateSelection(final Context context, final AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public TemplateSelection(final Context context, final AttributeSet attrs, final int defStyle) {
		super(context, attrs, defStyle);
		init();
	}


	public int getLeftCoord() {
		return mLeft;
	}

	public int getRightCoord() {
		return mRight;
	}

	public int getTopCoord() {
		return mTop;
	}

	public int getBottomCoord() {
		return mBottom;
	}

	/**
	 * Setter method to set mClearCanvas. If mClearCanvas is true, this indicates that we have selected
	 * the template, so the rectangle used to distinguish template can be overwritten as we no longer
	 * needed it.
	 *
	 * @param clearCanvas true or false depending on whether or not we want to clear canvas.
	 */
	public void setClearCanvas(boolean clearCanvas) {
		mClearCanvas = clearCanvas;
	}


	/**
	 * Sets callback for up
	 *
	 * @param callback {@link OnUpCallback}
	 */
	public void setOnUpCallback(OnUpCallback callback) {
		mCallback = callback;
	}

	/**
	 * Inits internal data
	 */
	private void init() {
		mRectPaint = new Paint();

		mRectPaint.setStyle(Paint.Style.STROKE);
		mRectPaint.setPathEffect(new DashPathEffect(new float[] {10,20}, 0));
		mRectPaint.setStrokeWidth(8);
	}


	@Override
	public boolean onTouchEvent(final MotionEvent event) {

		mRectPaint.setColor(TrackingActivity.overlayColor);

		// TODO: be aware of multi-touches
		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				mDrawRect = false;
				mStartX = (int) event.getX();
				mStartY = (int) event.getY();
				invalidate();
				break;

			case MotionEvent.ACTION_MOVE:
				final int x = (int) event.getX();
				final int y = (int) event.getY();

				if (!mDrawRect || Math.abs(x - mEndX) > 5 || Math.abs(y - mEndY) > 5) {
					mEndX = x;
					mEndY = y;
					invalidate();
				}

				mDrawRect = true;
				break;

			case MotionEvent.ACTION_UP:
				if (mCallback != null) {
					mCallback.onRectFinished(new Rect(Math.min(mStartX, mEndX), Math.min(mStartY, mEndY),
						Math.max(mEndX, mStartX), Math.max(mEndY, mStartY)));
				}
				invalidate();
				break;

			default:
				break;
		}

		return true;
	}

	@Override
	protected void onDraw(final Canvas canvas) {
		super.onDraw(canvas);

		if (mDrawRect && !mClearCanvas) {
			mLeft = Math.min(mStartX, mEndX);
			mTop = Math.min(mStartY, mEndY);
			mRight = Math.max(mEndX, mStartX);
			mBottom = Math.max(mEndY, mStartY);
			canvas.drawRect(mLeft, mTop, mRight, mBottom, mRectPaint);

		} else if (mClearCanvas) {
			canvas.drawColor(0, PorterDuff.Mode.CLEAR);
		}
	}
}