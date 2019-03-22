/*
 * Copyright 2019 Dmitriy Ponomarenko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dimowner.charttemplate.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

import com.dimowner.charttemplate.model.ChartData;
import com.dimowner.charttemplate.util.AndroidUtils;

public class ChartScrollView extends View {

	private static final int ANIMATION_DURATION = 80; //mills

	private final float DENSITY;
	private final float SELECTION;

	{
		DENSITY = AndroidUtils.dpToPx(1);
		SELECTION = 5*DENSITY;
	}

	private ChartData data;
	private boolean[] linesVisibility;
	private boolean[] linesCalculated;
	private Path path;
	private float STEP = 10;

	private Paint[] linePaints;

	private float WIDTH = 1;
	private float HEIGHT = 1;
	private int maxValueY = 0;
	private float valueScaleY = 0;

	private ValueAnimator animator;
	private ValueAnimator alphaAnimator;

	public ChartScrollView(Context context) {
		super(context);
		init(context);
	}

	public ChartScrollView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public ChartScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(context);
	}

	private void init(Context context) {
		setFocusable(false);
		path = new Path();
	}

	private Paint createLinePaint(int color) {
		Paint lp = new Paint();
		lp.setAntiAlias(true);
		lp.setDither(false);
		lp.setStyle(Paint.Style.STROKE);
		lp.setStrokeWidth(1.2f*DENSITY);
		lp.setStrokeJoin(Paint.Join.ROUND);
		lp.setStrokeCap(Paint.Cap.ROUND);
		lp.setColor(color);
		return lp;
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		WIDTH = getWidth();
		HEIGHT = getHeight();
		valueScaleY = (HEIGHT-SELECTION)/maxValueY;

		if (data != null) {
			STEP = (WIDTH/data.getLength());
		}
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (data != null) {
			for (int i = 0; i < data.getNames().length; i++) {
				if (linesVisibility[i]) {
					drawChart(canvas, data.getValues(i), i);
				}
			}
		}
	}

	private void drawChart(Canvas canvas, int[] values, int index) {
		path.rewind();
		float x = 0;
		for (int i = 0; i < values.length; i+=2) {
			if (x == 0) {
				path.moveTo(x, HEIGHT - values[i] * valueScaleY);
			} else {
				path.lineTo(x, HEIGHT - values[i] * valueScaleY);
			}

			if (x - STEP > WIDTH) {
				break;
			}
			x += 2*STEP;
		}
		canvas.drawPath(path, linePaints[index]);
	}

	public void hideLine(String name) {
		final int pos = findLinePosition(name);
		if (pos >= 0) {
			linesCalculated[pos] = false;
			alphaAnimator(linePaints[pos].getAlpha(), 0, pos, false);
		}
		calculateMaxValue(false);
		invalidate();
	}

	public void showLine(String name) {
		final int pos = findLinePosition(name);
		if (pos >= 0) {
			linesVisibility[pos] = true;
			linesCalculated[pos] = true;
			alphaAnimator(linePaints[pos].getAlpha(), 255f, pos, true);
		}
		calculateMaxValue(false);
		invalidate();
	}

	public void setData(ChartData d) {
		this.data = d;
		if (data != null) {
			//Init lines visibility state, all visible by default.
			linesVisibility = new boolean[data.getLinesCount()];
			linesCalculated = new boolean[data.getLinesCount()];
			linePaints = new Paint[data.getLinesCount()];
			for (int i = 0; i < linesVisibility.length; i++) {
				linesVisibility[i] = true;
				linesCalculated[i] = true;
				linePaints[i] = createLinePaint(data.getColorsInts()[i]);
			}
			calculateMaxValue(true);
			if (WIDTH > 1 && data.getLength() > 0) {
				STEP = (WIDTH / data.getLength());
			}
		}
		invalidate();
	}

	private void calculateMaxValue(final boolean invalidate) {
		int prev = maxValueY;
		maxValueY = 0;
		for (int j = 0; j < data.getLinesCount(); j++) {
			if (linesCalculated[j]) {
				for (int i = 0; i < data.getLength(); i++) {
					if (data.getValues(j)[i] > maxValueY) {
						maxValueY = data.getValues(j)[i];
					}
				}
			}
		}
		valueScaleY = (HEIGHT-SELECTION)/maxValueY;
		if (prev != maxValueY) {
			animation(prev, maxValueY, invalidate);
		}
	}

	private void animation(final float start, final float end, final boolean invalidate) {
		if (animator != null && animator.isStarted()) {
			animator.cancel();
		}
		animator = ValueAnimator.ofFloat(0.0f, 1.0f);
		animator.setInterpolator(new AccelerateDecelerateInterpolator());
		animator.setDuration(ANIMATION_DURATION);
		animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator animation) {
				maxValueY = (int) (start+(end-start)*(Float) animation.getAnimatedValue());
				valueScaleY = (HEIGHT-SELECTION)/ maxValueY;
				if (invalidate) {
					invalidate();
				}
			}
		});
		animator.start();
	}

	private void alphaAnimator(float start, final float end, final int index, final boolean show) {
		if (alphaAnimator != null && alphaAnimator.isRunning()) {
			alphaAnimator.cancel();
		}
		alphaAnimator = ValueAnimator.ofFloat(start, end);
		if (show) {
			alphaAnimator.setInterpolator(new DecelerateInterpolator());
		} else {
			alphaAnimator.setInterpolator(new AccelerateInterpolator());
		}
		alphaAnimator.setDuration(ANIMATION_DURATION);
		alphaAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator animation) {
				float val = (float) animation.getAnimatedValue();
				linePaints[index].setAlpha((int)val);
				if (val == end) {
					linesVisibility[index] = show;
				}
				invalidate();
			}
		});
		alphaAnimator.start();
	}

	private int findLinePosition(String name) {
		for (int i = 0; i < data.getLinesCount(); i++) {
			if (data.getNames()[i].equalsIgnoreCase(name)) {
				return i;
			}
		}
		return -1;
	}

	@Override
	public Parcelable onSaveInstanceState() {
		Parcelable superState = super.onSaveInstanceState();
		SavedState ss = new SavedState(superState);

		ss.maxValueY = maxValueY;
		ss.linesVisibility = linesVisibility;
		ss.linesCalculated = linesCalculated;
		ss.data = data;
		return ss;
	}

	@Override
	public void onRestoreInstanceState(Parcelable state) {
		SavedState ss = (SavedState) state;
		super.onRestoreInstanceState(ss.getSuperState());
		maxValueY = ss.maxValueY;
		linesVisibility = ss.linesVisibility;
		linesCalculated = ss.linesCalculated;
		data = ss.data;
		if (data != null) {
			linePaints = new Paint[data.getLinesCount()];
			for (int i = 0; i < data.getLinesCount(); i++) {
				linePaints[i] = createLinePaint(data.getColorsInts()[i]);
			}
		}
	}

	static class SavedState extends View.BaseSavedState {
		SavedState(Parcelable superState) {
			super(superState);
		}

		private SavedState(Parcel in) {
			super(in);
			maxValueY = in.readInt();
			in.readBooleanArray(linesVisibility);
			in.readBooleanArray(linesCalculated);
			data = in.readParcelable(ChartData.class.getClassLoader());
		}

		@Override
		public void writeToParcel(Parcel out, int flags) {
			super.writeToParcel(out, flags);
			out.writeInt(maxValueY);
			out.writeBooleanArray(linesVisibility);
			out.writeBooleanArray(linesCalculated);
			out.writeParcelable(data, Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
		}

		private ChartData data;
		private int maxValueY;
		private boolean[] linesVisibility;
		private boolean[] linesCalculated;

		public static final Parcelable.Creator<SavedState> CREATOR =
				new Parcelable.Creator<SavedState>() {
					@Override
					public SavedState createFromParcel(Parcel in) {
						return new SavedState(in);
					}

					@Override
					public SavedState[] newArray(int size) {
						return new SavedState[size];
					}
				};
	}
}
