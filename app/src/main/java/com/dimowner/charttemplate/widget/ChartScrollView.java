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
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

import com.dimowner.charttemplate.ColorMap;
import com.dimowner.charttemplate.R;
import com.dimowner.charttemplate.model.ChartData;
import com.dimowner.charttemplate.util.AndroidUtils;

public class ChartScrollView extends View {

	private static final int ANIMATION_DURATION = 100; //mills

	private final float DENSITY;
	private final float PADD_TINY;
	private final float PADD_NORMAL;
	private final float SELECTION_HALF;
	private final float BORDER;

	{
		DENSITY = AndroidUtils.dpToPx(1);
		PADD_TINY = 3*DENSITY;
		BORDER = 4*DENSITY;
		PADD_NORMAL = 16*DENSITY;
		SELECTION_HALF = 8*DENSITY;
	}

	private ChartData data;
	private boolean[] linesVisibility;
	private boolean[] linesCalculated;
	private float chartArray[];
	private float chartArray2[];
	private float STEP = 10;

	private Paint[] linePaints;
	private Paint borderPaint;

	private float WIDTH = 1;
	private float HEIGHT = 1;
	private float H1 = 1;
	private int maxValueY = 0;
	private float valueScaleY = 0;

	private boolean isAnimating = false;
	private float scaleKoef = 1;
	private int amnimItemIndex = -1;
	private float[] sumVals;

	private ValueAnimator animator;
	private ValueAnimator alphaAnimator;
	private DecelerateInterpolator decelerateInterpolator = new DecelerateInterpolator();
	private AccelerateInterpolator accelerateInterpolator= new AccelerateInterpolator();

	private boolean invalidate = false;
	private float start = 0;
	private float end = 0;

	ValueAnimator.AnimatorUpdateListener heightValueAnimator = new ValueAnimator.AnimatorUpdateListener() {
		@Override
		public void onAnimationUpdate(ValueAnimator animation) {
			maxValueY = (int) (start+(end-start)*(Float) animation.getAnimatedValue());
			valueScaleY = (HEIGHT-2*PADD_TINY)/ maxValueY;
			if (invalidate) {
				invalidate();
			}
		}
	};

	private boolean show = false;
	private int index = 0;
	private float end2 = 0;

	ValueAnimator.AnimatorUpdateListener alphaValueAnimator = new ValueAnimator.AnimatorUpdateListener() {
		@Override
		public void onAnimationUpdate(ValueAnimator animation) {
			float val = (float) animation.getAnimatedValue();
			scaleKoef = Math.abs(val/255);
			linePaints[index].setAlpha((int)val);
			if (val == end2) {
				linesVisibility[index] = show;
				isAnimating = false;
				amnimItemIndex = -1;
			}
			if (data.isPercentage()) {
				calculateSumsLine();
			}
			invalidate();
		}
	};


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

		int viewBackground = getResources().getColor(ColorMap.getViewBackground());
//		TypedValue typedValue = new TypedValue();
//		if (context.getTheme().resolveAttribute(R.attr.viewBackground, typedValue, true)) {
//			viewBackground = typedValue.data;
//		} else {
//			viewBackground = context.getResources().getColor(R.color.view_background);
//		}
		borderPaint = new Paint();
		borderPaint.setStyle(Paint.Style.STROKE);
		borderPaint.setStrokeWidth(BORDER);
		borderPaint.setColor(viewBackground);
		borderPaint.setAntiAlias(true);
	}

	private Paint createLinePaint(int color) {
		Paint lp = new Paint(1);
//		lp.setAntiAlias(true);
//		lp.setDither(false);
		lp.setStyle(Paint.Style.STROKE);
		lp.setStrokeWidth(1.0f*DENSITY);
//		lp.setStrokeJoin(Paint.Join.ROUND);
//		lp.setStrokeCap(Paint.Cap.ROUND);
		lp.setStrokeCap(Paint.Cap.SQUARE);
		lp.setColor(color);
		return lp;
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		WIDTH = getWidth();
		HEIGHT = getHeight();
//		H1 = HEIGHT-PADD_TINY;
		H1 = HEIGHT-1.5f*DENSITY;
		valueScaleY = (HEIGHT-2*PADD_TINY)/maxValueY;

		if (data != null) {
			STEP = (WIDTH/data.getLength());
		}
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
//		Timber.v("onDraw sel = " + maxValueY);
//		if (path.isEmpty()) {
//			path.addRoundRect(0, 1.5f*DENSITY, WIDTH, HEIGHT-1.5f*DENSITY, SELECTION_HALF, SELECTION_HALF, Path.Direction.CW);
//		}
//		canvas.clipPath(path);
		if (data != null) {
			for (int i = 0; i < data.getNames().length; i++) {
				if (linesVisibility[i]) {
					if (data.getType(i) == ChartData.TYPE_LINE) {
						drawChart(canvas, data.getValues(i), i);
					} else if (data.getType(i) == ChartData.TYPE_BAR) {
						drawBars(canvas, data.getValues(i), i);
					} else if (data.getType(i) == ChartData.TYPE_AREA) {
//						drawBars(canvas, data.getValues(i), i);
						drawAreaPercentage(canvas, data.getValues(i), i);
					} else {
						drawChart(canvas, data.getValues(i), i);
					}
				}
			}
			//Draw round borders.
			canvas.drawRoundRect(-DENSITY, -0.5f*DENSITY, WIDTH, HEIGHT+DENSITY, SELECTION_HALF, SELECTION_HALF, borderPaint);
//			canvas.drawRoundRect(rectF, SELECTION_HALF, SELECTION_HALF, borderPaint);
		}
	}

	private void drawChart(Canvas canvas, int[] values, int index) {
//		path.rewind();
		float x = 0;
		int k = 0;
//		chartArray = new float[4* (int)Math.ceil((WIDTH/STEP))];
//		Log.v("LOG", "drawChart size = " + chartArray.length + " WIDTH = " + WIDTH + " STEP = " + STEP);
		for (int i = 0; i < values.length; i+=2) {
//			if (x == 0) {
//				path.moveTo(x, HEIGHT - values[i] * valueScale);
//			} else {
//				path.lineTo(x, HEIGHT - values[i] * valueScale);
//			}
			chartArray[k] = x; //x
			chartArray[k+1] = H1 - values[i] * valueScaleY; //y
			chartArray[k + 2] = x + 2*STEP; //x
			if (i+2 < values.length) {
				chartArray[k + 3] = H1 - values[i + 2] * valueScaleY; //y
			} else {
				chartArray[k + 3] = H1 - values[i] * valueScaleY; //y
			}
//			if (x - STEP > WIDTH) {
//				break;
//			}
			x += 2*STEP;
			k +=4;
		}
//		canvas.drawPath(path, linePaints[index]);
//		linePaints[index].setStrokeCap(Paint.Cap.SQUARE);
		canvas.drawLines(chartArray, linePaints[index]);
	}


	private void drawBars(Canvas canvas, int[] values, int index) {
		float pos = 0;
//		int skip = (int) scrollStartIndex -(int)(PADD_NORMAL/STEP);
//		if (skip < 0) {skip = 0;}
//		pos +=skip*STEP;
		float H3 = (H1-1.5f*DENSITY)/100;
		int k = 0;
		//TODO: Draw every second bar.
		linePaints[index].setStrokeWidth(3*STEP+1);
		if (data.isStacked()) {
			int j;
			int sum=0;
			for (int i = 0; i < values.length; i+=3) {
				if (k < chartArray.length) {
					for (j = 0; j <= index; j++) {
						if (linesCalculated[j] && j != amnimItemIndex) { //
							sum += data.getVal(j, i);
						}
					}
					if (isAnimating && amnimItemIndex <= index) {
						sum += scaleKoef*data.getVal(amnimItemIndex, i);
					}
					if (data.isPercentage()) {
						chartArray[k] = pos; //x1
						if (index == amnimItemIndex) {
							chartArray[k + 1] = H1 - H3*(sum - values[i] * scaleKoef)/(sumVals[i]);
						} else {
							chartArray[k + 1] = H1 - H3*(sum - values[i])/(sumVals[i]);
						}
						chartArray[k + 2] = pos; //x2
						chartArray[k + 3] = H1 - H3*sum/(sumVals[i]); //y2
					} else {
						chartArray[k] = pos; //x1
						//					chartArray[k + 1] = H1 - data.getValues(index-1)[i] * valueScale; //y1
						if (index == amnimItemIndex) {
							chartArray[k + 1] = H1 - (sum - values[i] * scaleKoef) * valueScaleY; //y1
						} else {
							chartArray[k + 1] = H1 - (sum - values[i]) * valueScaleY; //y1
						}
						chartArray[k + 2] = pos; //x2
						//					chartArray[k + 3] = H1 - values[i] * valueScale; //y2
						chartArray[k + 3] = H1 - sum * valueScaleY; //y2
					}
					k += 4;
					if (pos - STEP > WIDTH + PADD_NORMAL) {
						break;
					}
					sum = 0;
				}
				pos += 3*STEP;
			}
		} else {
			for (int i = 0; i < values.length; i+=3) {
				if (k < chartArray.length) {
					chartArray[k] = pos; //x1
					chartArray[k + 1] = H1; //y1
					chartArray[k + 2] = pos; //x2
					chartArray[k + 3] = H1 - values[i] * valueScaleY; //y2
					k += 4;
					if (pos - STEP > WIDTH + PADD_NORMAL) {
						break;
					}
				}
				pos += 3*STEP;
			}
		}
		canvas.drawLines(chartArray, linePaints[index]);
	}

	private void drawAreaPercentage(Canvas canvas, int[] values, int index) {
		int scale = 6;
		float pos = 0;
//		int skip = 0;
//		if (skip < 0) {skip = 0;}
//		skip -= skip%scale;
		pos +=0;
		int k = 0;
		int j;
		int sum=0;
		int sum2=0;
		float H3 = H1/100;
		for (int i = 0; i < values.length; i+=scale) {
			if (k < chartArray.length) {
				for (j = 0; j <= index; j++) {
					if (linesCalculated[j] && j != amnimItemIndex) { //
						sum += data.getVal(j, i);
						if (i+scale < values.length) {
							sum2 += data.getVal(j, i + scale);
						}
					}
				}
				if (isAnimating && amnimItemIndex <= index) {
					sum += scaleKoef*data.getVal(amnimItemIndex, i);
					if (i+scale < values.length) {
						sum2 += scaleKoef * data.getVal(amnimItemIndex, i+scale);
					}
				}
				chartArray[k] = pos; //x1
				if (index == amnimItemIndex) {
					chartArray[k + 1] = H1 - H3*(sum - values[i] * scaleKoef)/(sumVals[i]);
				} else {
					chartArray[k + 1] = H1 - H3*(sum - values[i])/(sumVals[i]);
				}
				chartArray[k + 2] = pos; //x2
				chartArray[k + 3] = H1 - H3*sum/(sumVals[i]); //y2

				///Draw lines
				chartArray2[k] = pos-1; //x1
				if (index == amnimItemIndex) {
					chartArray2[k + 1] = H1 - H3*(sum - values[i] * scaleKoef)/(sumVals[i]);
				} else {
					chartArray2[k + 1] = H1 - H3*(sum - values[i])/(sumVals[i]);
				}
				if (i + scale < values.length) {
					chartArray2[k + 2] = pos + STEP*scale; //x2
					if (index == amnimItemIndex) {
						chartArray2[k + 3] = H1 - H3 * (sum2 - values[i+scale]* scaleKoef) / (sumVals[i + scale]);
					} else {
						chartArray2[k + 3] = H1 - H3 * (sum2 - values[i+scale]) / (sumVals[i + scale]);
					}
				} else {
					chartArray2[k + 2] = pos+STEP*scale/2; //x2
					if (index == amnimItemIndex) {
						chartArray2[k + 3] = H1 - H3 * (sum - values[i]*scaleKoef) / (sumVals[i]); //y2
					} else {
						chartArray2[k + 3] = H1 - H3 * (sum - values[i]) / (sumVals[i]); //y2
					}
				}
				k += 4;
				if (pos - STEP*scale > WIDTH + PADD_NORMAL) {
					break;
				}
				sum = 0;
				sum2 = 0;
			}
			pos += scale*STEP;
		}

		linePaints[index].setStrokeWidth(scale*STEP+1);
		linePaints[index].setTextAlign(Paint.Align.RIGHT);
		linePaints[index].setStrokeCap(Paint.Cap.BUTT);
		canvas.drawLines(chartArray, 0, k, linePaints[index]);
		if (data.isPercentage() && index > 0 && !isBottomLine(index)) {
			linePaints[index].setStrokeWidth(STEP*scale);
			linePaints[index].setStrokeCap(Paint.Cap.BUTT);
			canvas.drawLines(chartArray2, 0, k, linePaints[index]);
			linePaints[index].setStrokeCap(Paint.Cap.ROUND);
			canvas.drawPoints(chartArray2, 0, k-1, linePaints[index]);
		}
	}

	private boolean isBottomLine(int index) {
		for (int i = 0; i < index; i++) {
			if (linesVisibility[i]) {
				return false;
			}
		}
		return true;
	}

	public void hideLine(String name) {
		isAnimating = true;
		final int pos = findLinePosition(name);
		if (pos >= 0) {
			amnimItemIndex = pos;
			linesCalculated[pos] = false;
			alphaAnimator(linePaints[pos].getAlpha(), 0, pos, false);
		}
		calculateMaxValue(false, true);
		invalidate();
	}

	public void showLine(String name) {
		isAnimating = true;
		final int pos = findLinePosition(name);
		if (pos >= 0) {
			amnimItemIndex = pos;
			linesVisibility[pos] = true;
			linesCalculated[pos] = true;
			alphaAnimator(linePaints[pos].getAlpha(), 255f, pos, true);
		}
		calculateMaxValue(false, true);
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
			calculateSumsLine();
			calculateMaxValue(true, false);
			if (WIDTH > 1 && data.getLength() > 0) {
				STEP = (WIDTH / data.getLength());
			}
			chartArray = new float[data.getLength() * 4];
			if (data.isPercentage()) {
				chartArray2 = new float[data.getLength() * 4];
			} else {
				chartArray2 = null;
			}
		}
		invalidate();
	}

	private void calculateSumsLine() {
		sumVals = new float[data.getLength()];
		int sum = 0;
		for (int i = 0; i < data.getLength(); i++) {
			for (int j = 0; j < data.getLinesCount(); j++) {
				if (linesVisibility[j]) {
					if (isAnimating && j == amnimItemIndex) {
						sum += data.getValues(j)[i]*scaleKoef;
					} else {
						sum += data.getValues(j)[i];
					}
				}
			}
			sumVals[i] = (float)sum/100;
			sum = 0;
		}
	}

	private void calculateMaxValue(final boolean invalidate, boolean animate) {
		int prev = maxValueY;
		maxValueY = 0;
		int sum=0;
		for (int j = 0; j < data.getLinesCount(); j++) {
			if (linesCalculated[j]) {
				for (int i = 0; i < data.getLength(); i++) {
					if (!data.isStacked()) {
						if (data.getValues(j)[i] > maxValueY) {
							maxValueY = data.getValues(j)[i];
						}
					} else {
//						if (i >= 0 && i < maxValuesLine.length) {
							for (j = 0; j < data.getLinesCount(); j++) {
								if (linesCalculated[j]) {
									sum += data.getVal(j, i);
								}
							}
							if (sum > maxValueY) {
								maxValueY = sum;
							}
							sum =0;
//						}
					}
				}
			}
		}
		valueScaleY = (HEIGHT-2*PADD_TINY)/maxValueY;
		if (prev != maxValueY) {
			if (animate) {
				heightAnimation(prev, maxValueY, invalidate);
			} else {
//				maxValueY = (int) (start+(end-start)*(Float) animation.getAnimatedValue());
				valueScaleY = (HEIGHT-2*PADD_TINY)/ maxValueY;
			}
		}
	}

	private void heightAnimation(final float start, final float end, final boolean invalidate) {
		this.start = start;
		this.end = end;
		this.invalidate = invalidate;
		if (animator != null && animator.isStarted()) {
			animator.cancel();
		}
		animator = ValueAnimator.ofFloat(0.0f, 1.0f);
		animator.setInterpolator(decelerateInterpolator);
		animator.setDuration(ANIMATION_DURATION);
		animator.addUpdateListener(heightValueAnimator);
//		animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
//			@Override
//			public void onAnimationUpdate(ValueAnimator animation) {
//				maxValueY = (int) (start+(end-start)*(Float) animation.getAnimatedValue());
//				valueScale = (HEIGHT-2*PADD_TINY)/ maxValueY;
//				if (invalidate) {
//					invalidate();
//				}
//			}
//		});
		animator.start();
	}

	private void alphaAnimator(float start, final float end, final int index, final boolean show) {
		this.end2 = end;
		this.index = index;
		this.show = show;
		if (alphaAnimator != null && alphaAnimator.isStarted()) {
			alphaAnimator.cancel();
		}
		alphaAnimator = ValueAnimator.ofFloat(start, end);
		if (show) {
			alphaAnimator.setInterpolator(decelerateInterpolator);
		} else {
			alphaAnimator.setInterpolator(accelerateInterpolator);
		}
		alphaAnimator.setDuration(ANIMATION_DURATION);
		alphaAnimator.addUpdateListener(alphaValueAnimator);
//		alphaAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
//			@Override
//			public void onAnimationUpdate(ValueAnimator animation) {
//				float val = (float) animation.getAnimatedValue();
//				linePaints[index].setAlpha((int)val);
//				if (val == end) {
//					linesVisibility[index] = show;
//				}
//				invalidate();
//			}
//		});
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
		ss.sumVals = sumVals;
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
		sumVals = ss.sumVals;
		data = ss.data;
		if (data != null) {
			linePaints = new Paint[data.getLinesCount()];
			for (int i = 0; i < data.getLinesCount(); i++) {
				linePaints[i] = createLinePaint(data.getColorsInts()[i]);
			}
			chartArray = new float[data.getLength() * 4];
			if (data.isPercentage()) {
				chartArray2 = new float[data.getLength() * 4];
			} else {
				chartArray2 = null;
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
			valueScaleY = in.readFloat();
			in.readBooleanArray(linesVisibility);
			in.readBooleanArray(linesCalculated);
			in.readFloatArray(sumVals);
			data = in.readParcelable(ChartData.class.getClassLoader());
		}

		@Override
		public void writeToParcel(Parcel out, int flags) {
			super.writeToParcel(out, flags);
			out.writeInt(maxValueY);
			out.writeFloat(valueScaleY);
			out.writeBooleanArray(linesVisibility);
			out.writeBooleanArray(linesCalculated);
			out.writeFloatArray(sumVals);
			out.writeParcelable(data, Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
		}

		private ChartData data;
		private int maxValueY;
		private float valueScaleY = 0;
		private boolean[] linesVisibility;
		private boolean[] linesCalculated;
		private float[] sumVals;

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
