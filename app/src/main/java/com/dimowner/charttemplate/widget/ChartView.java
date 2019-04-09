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
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
//import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

import com.dimowner.charttemplate.R;
import com.dimowner.charttemplate.model.ChartData;
import com.dimowner.charttemplate.util.AndroidUtils;
import java.text.DecimalFormat;

public class ChartView extends View {

	private final float DENSITY;
	private final int PADD_NORMAL;
	private final int PADD_SMALL;
	private final int PADD_TINY;
	private final int TEXT_SPACE;
	private final int BASE_LINE_Y;
	private static final int GRID_LINES_COUNT = 6;
	private static final int ANIMATION_DURATION = 220; //mills

	{
		DENSITY = AndroidUtils.dpToPx(1);
		PADD_NORMAL = (int) (16*DENSITY);
		PADD_SMALL = (int) (8*DENSITY);
		PADD_TINY = (int) (4*DENSITY);
		TEXT_SPACE = (int) (56*DENSITY);
		BASE_LINE_Y = (int) (32*DENSITY);
	}

	private float STEP = 10*DENSITY;

	private DecimalFormat format = new DecimalFormat("#,##0.#");
	private StringBuilder stringBuilder = new StringBuilder();

	private ChartData data;

	private float chartArray[];

	private boolean[] linesVisibility;
	private boolean[] linesCalculated;

	private TextPaint textPaint;
	private TextPaint dateRangePaint;
	private TextPaint timelineTextPaint;
	private Paint gridPaint;
	private Paint baselinePaint;
	private Paint[] linePaints;

	private ValueAnimator alphaAnimator;
	private ValueAnimator heightAnimator;
	private DecelerateInterpolator decelerateInterpolator = new DecelerateInterpolator();
	private AccelerateInterpolator accelerateInterpolator= new AccelerateInterpolator();

	private float scrollPos;
	private float scrollIndex;

	private ChartSelectionDrawer selectionDrawer;

	private float WIDTH = 1;
	private float HEIGHT = 1;
	private float maxValueVisible = 0;
	private float maxValueCalculated = 0;
	private int[] maxValuesLine;
	private float valueScaleY = 1;
	private float gridScaleY = 1;
	private int gridCount = GRID_LINES_COUNT;
	private float gridStep = 1;
	private float gridValueStep = 1;
	private boolean skipNextInvalidation = false;
	private String dateRange;
	private float dateRangeHeight;
	private Rect rect;

	private OnMoveEventsListener onMoveEventsListener;

	ValueAnimator.AnimatorUpdateListener heightValueAnimator = new ValueAnimator.AnimatorUpdateListener() {
		@Override
		public void onAnimationUpdate(ValueAnimator animation) {
			float val = (float) animation.getAnimatedValue();
			maxValueVisible = maxValueCalculated -(int)val;
			valueScaleY = (HEIGHT-2*BASE_LINE_Y-PADD_NORMAL)/ maxValueVisible;
			if (skipNextInvalidation) {
				skipNextInvalidation = false;
			} else {
				invalidate();
			}
			if (gridStep < 45*DENSITY) {
				gridValueStep *=2;
			}
			if (gridStep > 90*DENSITY) {
				gridValueStep /=2;
			}
			gridStep = gridValueStep*valueScaleY;
			if (gridStep < 40*DENSITY) { gridStep = 40*DENSITY;}
			updateGrid();
		}
	};

	private boolean show = false;
	private int index = 0;
	private float end2 = 0;

	ValueAnimator.AnimatorUpdateListener alphaValueAnimator = new ValueAnimator.AnimatorUpdateListener() {
		@Override
		public void onAnimationUpdate(ValueAnimator animation) {
			float val = (float) animation.getAnimatedValue();
			linePaints[index].setAlpha((int)val);
			if (val == end2) {
				linesVisibility[index] = show;
			}
			skipNextInvalidation = true;
			invalidate();
		}
	};

	public ChartView(Context context) {
		super(context);
		init(context);
	}

	public ChartView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public ChartView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(context);
	}

	private void init(Context context) {
		setFocusable(false);

		scrollPos = -1;
		scrollIndex = 0;
		rect = new Rect();

		int gridColor;
		int gridBaseLineColor;
		int gridTextColor;
		int panelTextColor;
		int panelColor;
		int scrubblerColor;
		int shadowColor;
		int tittleColor;

		Resources res = context.getResources();
		TypedValue typedValue = new TypedValue();
		Resources.Theme theme = context.getTheme();
		if (theme.resolveAttribute(R.attr.gridColor, typedValue, true)) {
			gridColor = typedValue.data;
		} else {
			gridColor = res.getColor(R.color.grid_color2);
		}
		if (theme.resolveAttribute(R.attr.gridBaseLineColor, typedValue, true)) {
			gridBaseLineColor = typedValue.data;
		} else {
			gridBaseLineColor = res.getColor(R.color.grid_base_line);
		}
		if (theme.resolveAttribute(R.attr.gridTextColor, typedValue, true)) {
			gridTextColor = typedValue.data;
		} else {
			gridTextColor = res.getColor(R.color.text_color);
		}
		if (theme.resolveAttribute(R.attr.panelColor, typedValue, true)) {
			panelColor = typedValue.data;
		} else {
			panelColor = res.getColor(R.color.panel_background);
		}
		if (theme.resolveAttribute(R.attr.panelTextColor, typedValue, true)) {
			panelTextColor = typedValue.data;
		} else {
			panelTextColor = res.getColor(R.color.panel_text_night);
		}
		if (theme.resolveAttribute(R.attr.scrubblerColor, typedValue, true)) {
			scrubblerColor = typedValue.data;
		} else {
			scrubblerColor = res.getColor(R.color.scrubbler_color);
		}
		if (theme.resolveAttribute(R.attr.shadowColor, typedValue, true)) {
			shadowColor = typedValue.data;
		} else {
			shadowColor = res.getColor(R.color.shadow_color);
		}
		if (theme.resolveAttribute(R.attr.tittleColor, typedValue, true)) {
			tittleColor = typedValue.data;
		} else {
			tittleColor = res.getColor(R.color.black);
		}

		selectionDrawer = new ChartSelectionDrawer(getContext(), panelTextColor,
					panelColor, scrubblerColor, shadowColor);

		gridPaint = new Paint();
		gridPaint.setAntiAlias(false);
		gridPaint.setDither(false);
		gridPaint.setStyle(Paint.Style.STROKE);
		gridPaint.setStrokeCap(Paint.Cap.SQUARE);
		gridPaint.setColor(gridColor);
		gridPaint.setStrokeWidth(DENSITY);

		baselinePaint = new Paint();
		baselinePaint.setAntiAlias(false);
		baselinePaint.setDither(false);
		baselinePaint.setStyle(Paint.Style.STROKE);
		baselinePaint.setColor(gridBaseLineColor);
		baselinePaint.setStrokeWidth(DENSITY);

		textPaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
		textPaint.setColor(gridTextColor);
		textPaint.setTextAlign(Paint.Align.CENTER);
		textPaint.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
		textPaint.setTextSize(context.getResources().getDimension(R.dimen.text_xsmall));

		dateRangePaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
		dateRangePaint.setColor(tittleColor);
		dateRangePaint.setTextAlign(Paint.Align.RIGHT);
		dateRangePaint.setTypeface(Typeface.create("sans-serif-sans-serif-thin", Typeface.NORMAL));
		dateRangePaint.setTextSize(context.getResources().getDimension(R.dimen.text_normal));

		timelineTextPaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
		timelineTextPaint.setColor(gridTextColor);
		timelineTextPaint.setTextAlign(Paint.Align.LEFT);
		timelineTextPaint.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
		timelineTextPaint.setTextSize(context.getResources().getDimension(R.dimen.text_xsmall));

		setOnTouchListener(new OnTouchListener() {

			float startY = 0;

			@Override
			public boolean onTouch(View v, MotionEvent motionEvent) {
				switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {
					case MotionEvent.ACTION_DOWN:
						selectionDrawer.reset();
						startY = motionEvent.getY();
						break;
					case MotionEvent.ACTION_MOVE:
						float selectionX = motionEvent.getX();
						if (selectionX > WIDTH) {
							selectionX = WIDTH;
						}
						if (selectionX < 0) {
							selectionX = -1;
						}
						if (Math.abs(motionEvent.getY() - startY) < 90*DENSITY) {
							selectionDrawer.setSelectionX(selectionX);
							selectionDrawer.calculatePanelSize(data, STEP, linesCalculated, scrollPos, WIDTH);
							if (onMoveEventsListener != null) {
								onMoveEventsListener.disallowTouchEvent();
							}
						} else {
							selectionX = -1;
							selectionDrawer.setSelectionX(selectionX);
							if (onMoveEventsListener != null) {
								onMoveEventsListener.allowTouchEvent();
							}
						}
						invalidate();
						break;
					case MotionEvent.ACTION_UP:
						selectionDrawer.setSelectionX(-1);
						invalidate();
						performClick();
						break;
				}
				return true;
			}
		});
	}

	private Paint createLinePaint(int color) {
		Paint lp = new Paint(1);
//		lp.setAntiAlias(false);
//		lp.setDither(false);
		lp.setStyle(Paint.Style.STROKE);
//		lp.setStrokeCap(Paint.Cap.ROUND);
		lp.setStrokeWidth(1.6f*DENSITY);
//		lp.setStrokeJoin(Paint.Join.ROUND);
//		lp.setStrokeCap(Paint.Cap.ROUND);
		lp.setColor(color);
		return lp;
	}

	private void alphaAnimator(float start, final float end, final int index, final boolean show) {
		this.show = show;
		this.index = index;
		this.end2 = end;
		if (alphaAnimator != null && alphaAnimator.isRunning()) {
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
//				skipNextInvalidation = true;
//				invalidate();
//			}
//		});
		alphaAnimator.start();
	}

	private void heightAnimator(final float diff) {
		if (heightAnimator != null && heightAnimator.isRunning()) {
			heightAnimator.cancel();
		}
		heightAnimator = ValueAnimator.ofFloat(diff, 0);
		heightAnimator.setInterpolator(decelerateInterpolator);
		heightAnimator.setDuration(ANIMATION_DURATION);
		heightAnimator.addUpdateListener(heightValueAnimator);
//		heightAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
//			@Override
//			public void onAnimationUpdate(ValueAnimator animation) {
//				float val = (float) animation.getAnimatedValue();
//				maxValueVisible = maxValueCalculated -(int)val;
//				valueScaleY = (HEIGHT-2*BASE_LINE_Y-PADD_NORMAL)/ maxValueVisible;
//				if (skipNextInvalidation) {
//					skipNextInvalidation = false;
//				} else {
//					invalidate();
//				}
//				if (val == 0) {
////					Timber.v("Animation END");
////					gridScaleY = valueScaleY;
////					gridValueStep = maxValueCalculated/GRID_LINES_COUNT;
//				}
//				gridStep = gridValueStep*valueScaleY;
//			}
//		});
		heightAnimator.start();
	}

	private void updateGrid() {
		gridCount = (int)((HEIGHT-2*BASE_LINE_Y)/gridStep);
	}

	public void scrollPos(float x, float size) {
		if (x >= 0) {
			STEP = WIDTH / size;
			scrollPos = (x * STEP);
			scrollIndex = x;
//			Timber.v("Times: " + data.getTimesLong()[(int)x] + " - " + data.getTimesLong()[(int)Math.floor(x+size)-1]);
//			screenShift = -scrollPos;
			int idx = (int) Math.ceil(x + size) - 1;
			if (idx < data.getLength()) {
				dateRange = data.getTimes()[(int) Math.floor(x)] + " - " + data.getTimes()[idx];
				dateRangePaint.getTextBounds(dateRange, 0, dateRange.length(), rect);
			}
			if (dateRangeHeight < rect.height()) {
				dateRangeHeight = rect.height();
			}
			calculateMaxValue2(false);
			skipNextInvalidation = true;
			updateGrid();
			invalidate();
		}
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		WIDTH = getWidth();
		HEIGHT = getHeight();
//		gridStep = (HEIGHT/GRID_LINES_COUNT);
		if (maxValueVisible > 0) {
			valueScaleY = (HEIGHT - 2*BASE_LINE_Y-PADD_NORMAL) / maxValueVisible;
			gridScaleY = valueScaleY;
		}
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (data != null) {
			textPaint.setTextAlign(Paint.Align.LEFT);
			drawGrid(canvas);

			if (data.getLinesCount() > 0) {
				drawTimeline(canvas);
			}

			//Draw charts
			textPaint.setTextAlign(Paint.Align.CENTER);
			for (int i = 0; i < data.getNames().length; i++) {
				if (linesVisibility[i]) {
					drawChart(canvas, data.getValues(i), i);
				}
			}
			canvas.drawText(dateRange, WIDTH-2, dateRangeHeight+21*DENSITY, dateRangePaint);
			//Draw selection panel with scrubbler
			selectionDrawer.draw(canvas, data, linesVisibility, HEIGHT, linePaints, valueScaleY);
		}
	}

	private void drawGrid(Canvas canvas) {
//		float gridValueText = (maxValueCalculated / GRID_LINES_COUNT);
//		float gridStep = (gridValueText * valueScaleY);
		//		float gridStep = (HEIGHT/GRID_LINES_COUNT);
//		int gridValueText = (int)(gridStep/valueScaleY);
		for (int i = 0; i < gridCount; i++) {
			if (i == 0) {
				canvas.drawLine(0, HEIGHT - BASE_LINE_Y - gridStep * i,
						WIDTH, HEIGHT - BASE_LINE_Y - gridStep * i, baselinePaint);
			} else {
				canvas.drawLine(0, HEIGHT - BASE_LINE_Y - gridStep * i,
						WIDTH, HEIGHT - BASE_LINE_Y - gridStep * i, gridPaint);
			}
			canvas.drawText(formatValue(gridValueStep * i),
					0, HEIGHT - BASE_LINE_Y - gridStep * i - PADD_TINY, textPaint);
		}
	}

	private void drawChart(Canvas canvas, int[] values, int index) {
		float offset = 0; //Remove this var
		float pos = -scrollPos;
		int skip = (int)scrollIndex-(int)(PADD_NORMAL/STEP);
		if (skip < 0) {skip = 0;}
		pos +=skip*STEP;
		int k = skip;
		for (int i = skip; i < values.length; i++) {
			if (k < chartArray.length) {
				chartArray[k] = pos + offset; //x
				chartArray[k + 1] = HEIGHT - BASE_LINE_Y - values[i] * valueScaleY; //y
				if (i + 1 < values.length) {
					chartArray[k + 2] = pos + offset + STEP; //x
					chartArray[k + 3] = HEIGHT - BASE_LINE_Y - values[i + 1] * valueScaleY; //y
				} else {
					chartArray[k + 2] = pos + offset; //x
					chartArray[k + 3] = HEIGHT - BASE_LINE_Y - values[i] * valueScaleY; //y
				}
				k +=4;
				if (pos - STEP > WIDTH+PADD_NORMAL) {
					if (pos/STEP < 130) {
						linePaints[index].setStrokeCap(Paint.Cap.ROUND);
					} else {
						linePaints[index].setStrokeCap(Paint.Cap.BUTT);
					}
					break;
				}
			}
			pos += STEP;
		}
//		Timber.v("draw scrollPos = " + scrollPos + " scrollInd = " + scrollIndex+ " STEP = " + STEP);
		canvas.drawLines(chartArray, skip, k-skip, linePaints[index]);
	}

	private void drawTimeline(Canvas canvas) {
		float pos = 0;
		int count = 1;
		while (count*STEP < TEXT_SPACE) {
			count *=2;
		}

		for (int i = 0; i < data.getLength()/count+1; i++) {
			if (pos-scrollPos+TEXT_SPACE >= 0 && pos-scrollPos < WIDTH && i*count < data.getLength()) {
				if (count*STEP > TEXT_SPACE && count*STEP < TEXT_SPACE*1.18f && (i)%2!=0) {
					timelineTextPaint.setAlpha((int)(255/(TEXT_SPACE*0.18f)*(count*STEP-TEXT_SPACE)));
				} else {
					timelineTextPaint.setAlpha(255);
				}
				canvas.drawText(data.getTimesShort()[i*count], pos - scrollPos, HEIGHT - PADD_NORMAL, timelineTextPaint);
			}
			pos += count*STEP;
		}
	}

	public void hideLine(String name) {
		int pos = findLinePosition(name);
		if (pos >= 0) {
			linesCalculated[pos] = false;
			alphaAnimator(linePaints[pos].getAlpha(), 0, pos, false);
		}
		calculateMaxValuesLine();
		calculateMaxValue2(true);
//		gridCount = (int)(HEIGHT/gridStep);
		updateGrid();
	}

	public void showLine(String name) {
		int pos = findLinePosition(name);
		if (pos >= 0) {
			linesVisibility[pos] = true;
			linesCalculated[pos] = true;
			alphaAnimator(linePaints[pos].getAlpha(), 255, pos, true);
		}
		calculateMaxValuesLine();
		calculateMaxValue2(true);
//		gridCount = (int)(HEIGHT/gridStep);
		updateGrid();
	}

	public void setData(ChartData d) {
		this.data = d;
		if (data != null) {
			//Init lines visibility state, all visible by default.
			linesVisibility = new boolean[data.getLinesCount()];
			linesCalculated = new boolean[data.getLinesCount()];
			selectionDrawer.setLinesCount(data.getLinesCount());
			linePaints = new Paint[data.getLinesCount()];
			for (int i = 0; i < data.getLinesCount(); i++) {
				linesVisibility[i] = true;
				linesCalculated[i] = true;
				linePaints[i] = createLinePaint(data.getColorsInts()[i]);
			}
			calculateMaxValuesLine();
			calculateMaxValue2(true);
			gridValueStep = maxValueCalculated/GRID_LINES_COUNT;
//			if (HEIGHT > 1) {
				chartArray = new float[data.getLength() * 4];
//				for (int i = 0; i < data.getLength(); i += 2) {
//					chartArray[i] = i;
//					chartArray[i + 1] = HEIGHT - BASE_LINE_Y - data.getValues(0)[(i / 2 + 1)] * valueScaleY;
//				}
//			}
		}
		selectionDrawer.setSelectionX(-1);
		invalidate();
	}

	//TODO: optimize this method. There max val should not use all values to calculate max.
	private void calculateMaxValue2(boolean adjust) {
		float prev = maxValueCalculated;
		maxValueCalculated = 0;
		for (int i = (int)(scrollPos/STEP); i < (int)((scrollPos+WIDTH)/STEP); i++) {
			if (i >= 0 && i < maxValuesLine.length && maxValuesLine[i] > maxValueCalculated) {
				maxValueCalculated = maxValuesLine[i];
			}
		}

//		if (adjust) {
//			maxValueCalculated = (int) adjustToGrid((float) maxValueCalculated, (int)GRID_LINES_COUNT);
//		}
		if (prev != maxValueCalculated) {
			heightAnimator(maxValueCalculated - maxValueVisible);
		}
	}

	private void calculateMaxValuesLine() {
		maxValuesLine = new int[data.getLength()];
		int max;
		for (int i = 0; i < data.getLength(); i++) {
			max = 0;
			for (int j = 0; j < data.getLinesCount(); j++) {
				if (linesCalculated[j]) {
					if (i < data.getLength() && data.getValues(j)[i] > max) {
						max = data.getValues(j)[i];
					}
				}
			}
			maxValuesLine[i] = max;
		}
	}

	private float adjustToGrid(float val, int scale) {
		int amp = 1;
		while (val > scale*100) {
			val = val/(scale*10);
			amp *=(scale*10);
		}
		if (val > (scale*10)) {
			val = (float) Math.ceil(val/scale);
			amp *=scale;
		}
		return (float) Math.ceil(val)*amp;
	}

	private int findLinePosition(String name) {
		for (int i = 0; i < data.getLinesCount(); i++) {
			if (data.getNames()[i].equalsIgnoreCase(name)) {
				return i;
			}
		}
		return -1;
	}

	public void setOnMoveEventsListener(OnMoveEventsListener onMoveEventsListener) {
		this.onMoveEventsListener = onMoveEventsListener;
	}

	private String formatValue(float f) {
		if (f < 1000.0f) {
			return format.format((double) f);
		}
		stringBuilder.setLength(0);
		if (f < 1000000.0f) {
			stringBuilder.append(format.format((double) (f / 1000.0f)));
			stringBuilder.append("K");
			return stringBuilder.toString();
		} else if (f < 1.0E9f) {
			stringBuilder.append(format.format((double) (f / 1000000.0f)));
			stringBuilder.append("M");
			return stringBuilder.toString();
		} else {
			stringBuilder.append(format.format((double) (f / 1.0E9f)));
			stringBuilder.append("B");
			return stringBuilder.toString();
		}
	}

	@Override
	public Parcelable onSaveInstanceState() {
		Parcelable superState = super.onSaveInstanceState();
		SavedState ss = new SavedState(superState);

		ss.linesVisibility = linesVisibility;
		ss.linesCalculated = linesCalculated;
		ss.scrollPos = scrollPos;
		ss.scrollIndex = scrollIndex;
		ss.selectionX = selectionDrawer.getSelectionX();
		ss.valueScaleY = valueScaleY;
		ss.STEP = STEP;
		ss.maxValueVisible = maxValueVisible;
		ss.maxValueCalculated = maxValueCalculated;
		ss.maxValuesLine = maxValuesLine;
		ss.gridValueStep = gridValueStep;
		ss.dateRange = dateRange;
		ss.gridCount = gridCount;
		ss.gridScaleY = gridScaleY;
		ss.gridStep = gridStep;
		ss.dateRangeHeight = dateRangeHeight;
		ss.data = data;
		return ss;
	}

	@Override
	public void onRestoreInstanceState(Parcelable state) {
		SavedState ss = (SavedState) state;
		super.onRestoreInstanceState(ss.getSuperState());

		linesVisibility = ss.linesVisibility;
		linesCalculated = ss.linesCalculated;
		scrollPos = ss.scrollPos;
		scrollIndex = ss.scrollIndex;
		selectionDrawer.setSelectionX(ss.selectionX);
		valueScaleY = ss.valueScaleY;
		STEP = ss.STEP;
		maxValueVisible = ss.maxValueVisible;
		maxValueCalculated = ss.maxValueCalculated;
		maxValuesLine = ss.maxValuesLine;
		gridValueStep = ss.gridValueStep;
		dateRangeHeight = ss.dateRangeHeight;
		dateRange = ss.dateRange;
		gridCount = ss.gridCount;
		gridScaleY = ss.gridScaleY;
		gridStep = ss.gridStep;
		data = ss.data;

		if (data != null) {
			selectionDrawer.setLinesCount(data.getLinesCount());
			linePaints = new Paint[data.getLinesCount()];
			for (int i = 0; i < data.getLinesCount(); i++) {
				linePaints[i] = createLinePaint(data.getColorsInts()[i]);
			}
			chartArray = new float[data.getLength() * 4];
		}
	}

	public interface OnMoveEventsListener {
		void disallowTouchEvent();
		void allowTouchEvent();
	}

	static class SavedState extends View.BaseSavedState {
		SavedState(Parcelable superState) {
			super(superState);
		}

		private SavedState(Parcel in) {
			super(in);
			in.readBooleanArray(linesVisibility);
			in.readBooleanArray(linesCalculated);
			float[] floats = new float[11];
			in.readFloatArray(floats);
			scrollPos = floats[0];
			scrollIndex = floats[1];
			selectionX = floats[2];
			valueScaleY = floats[3];
			STEP = floats[4];
			maxValueVisible = floats[5];
			maxValueCalculated = floats[6];
			gridValueStep = floats[7];
			dateRangeHeight = floats[8];
			gridScaleY = floats[9];
			gridStep = floats[10];
			gridCount = in.readInt();
			in.readIntArray(maxValuesLine);
			dateRange = in.readString();
			data = in.readParcelable(ChartData.class.getClassLoader());
		}

		@Override
		public void writeToParcel(Parcel out, int flags) {
			super.writeToParcel(out, flags);
			out.writeBooleanArray(linesVisibility);
			out.writeBooleanArray(linesCalculated);
			out.writeFloatArray(new float[] {scrollPos, scrollIndex, selectionX,
					valueScaleY, STEP, maxValueVisible, maxValueCalculated, gridValueStep,
					dateRangeHeight, gridScaleY, gridStep});
			out.writeInt(gridCount);
			out.writeIntArray(maxValuesLine);
			out.writeString(dateRange);
			out.writeParcelable(data, Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
		}

		ChartData data;
		float STEP;
		boolean[] linesVisibility;
		boolean[] linesCalculated;
		float scrollPos;
		float scrollIndex;
		float selectionX;
		float valueScaleY;
		float maxValueVisible;
		float maxValueCalculated;
		int[] maxValuesLine;
		float gridValueStep;
		String dateRange;
		float dateRangeHeight;
		float gridScaleY;
		float gridStep;
		int gridCount;

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
