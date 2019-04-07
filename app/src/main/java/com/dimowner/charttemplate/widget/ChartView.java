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
import android.graphics.Typeface;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

import com.dimowner.charttemplate.R;
import com.dimowner.charttemplate.model.ChartData;
import com.dimowner.charttemplate.util.AndroidUtils;
import com.dimowner.charttemplate.util.TimeUtils;

import java.util.Date;

import timber.log.Timber;

public class ChartView extends View {

	private final float DENSITY;
	private final int PADD_NORMAL;
	private final int PADD_SMALL;
	private final int PADD_TINY;
	private final int TEXT_SPACE;
	private final int BASE_LINE_Y;
	private static final int GRID_LINES_COUNT = 6;
	private static final int ANIMATION_DURATION = 150; //mills

	{
		DENSITY = AndroidUtils.dpToPx(1);
		PADD_NORMAL = (int) (16*DENSITY);
		PADD_SMALL = (int) (8*DENSITY);
		PADD_TINY = (int) (4*DENSITY);
		TEXT_SPACE = (int) (56*DENSITY);
		BASE_LINE_Y = (int) (32*DENSITY);
	}

	private float STEP = 10*DENSITY;

	private ChartData data;

//	private Path chartPath;
	private float chartArray[];

	private Date date;
	private boolean[] linesVisibility;
	private boolean[] linesCalculated;

	private String dateText;

	private TextPaint textPaint;
	private TextPaint timelineTextPaint;
	private Paint gridPaint;
	private Paint baselinePaint;
	private Paint[] linePaints;

	private ValueAnimator alphaAnimator;
	private ValueAnimator heightAnimator;

	private float scrollPos;
	private float scrollIndex;
//	private float screenShift = 0;

	private ChartSelectionDrawer selectionDrawer;

	private float WIDTH = 1;
	private float HEIGHT = 1;
	private int maxValueVisible = 0;
	private int maxValueCalculated = 0;
	private int[] maxValuesLine;
	private float valueScaleY = 1;
	private boolean skipNextInvalidation = false;

	private OnMoveEventsListener onMoveEventsListener;

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

//		chartPath = new Path();
		date = new Date();
		dateText = null;
		scrollPos = -1;
		scrollIndex = 0;

		int gridColor;
		int gridBaseLineColor;
		int gridTextColor;
		int panelTextColor;
		int panelColor;
		int scrubblerColor;
		int shadowColor;

		Resources res = context.getResources();
//		TypedValue typedValue = new TypedValue();
//		Resources.Theme theme = context.getTheme();
//		if (theme.resolveAttribute(R.attr.gridColor, typedValue, true)) {
//			gridColor = typedValue.data;
//		} else {
			gridColor = res.getColor(R.color.grid_color2);
//		}
//		if (theme.resolveAttribute(R.attr.gridBaseLineColor, typedValue, true)) {
//			gridBaseLineColor = typedValue.data;
//		} else {
			gridBaseLineColor = res.getColor(R.color.grid_base_line);
//		}
//		if (theme.resolveAttribute(R.attr.gridTextColor, typedValue, true)) {
//			gridTextColor = typedValue.data;
//		} else {
			gridTextColor = res.getColor(R.color.text_color);
//		}
//		if (theme.resolveAttribute(R.attr.panelColor, typedValue, true)) {
//			panelColor = typedValue.data;
//		} else {
			panelColor = res.getColor(R.color.panel_background);
//		}
//		if (theme.resolveAttribute(R.attr.panelTextColor, typedValue, true)) {
//			panelTextColor = typedValue.data;
//		} else {
			panelTextColor = res.getColor(R.color.panel_text_night);
//		}
//		if (theme.resolveAttribute(R.attr.scrubblerColor, typedValue, true)) {
//			scrubblerColor = typedValue.data;
//		} else {
			scrubblerColor = res.getColor(R.color.scrubbler_color);
//		}
//		if (theme.resolveAttribute(R.attr.shadowColor, typedValue, true)) {
//			shadowColor = typedValue.data;
//		} else {
			shadowColor = res.getColor(R.color.shadow_color);
//		}

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
		textPaint.setTextSize(context.getResources().getDimension(R.dimen.text_normal));

		timelineTextPaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
		timelineTextPaint.setColor(gridTextColor);
		timelineTextPaint.setTextAlign(Paint.Align.LEFT);
		timelineTextPaint.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
		timelineTextPaint.setTextSize(context.getResources().getDimension(R.dimen.text_normal));

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
						if (Math.abs(motionEvent.getY() - startY) < 60*DENSITY) {
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
				skipNextInvalidation = true;
				invalidate();
			}
		});
		alphaAnimator.start();
	}

	private void heightAnimator(final float diff) {
		if (heightAnimator != null && heightAnimator.isRunning()) {
			heightAnimator.cancel();
		}
		heightAnimator = ValueAnimator.ofFloat(diff, 0);
		heightAnimator.setInterpolator(new DecelerateInterpolator());

		heightAnimator.setDuration(220);
		heightAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator animation) {
				float val = (float) animation.getAnimatedValue();
				maxValueVisible = maxValueCalculated -(int)val;
				valueScaleY = (HEIGHT-BASE_LINE_Y-PADD_SMALL)/ maxValueVisible;
				if (skipNextInvalidation) {
					skipNextInvalidation = false;
				} else {
					invalidate();
				}
			}
		});
		heightAnimator.start();
	}

	public void scrollPos(float x, float size) {
		if (x >= 0) {
			STEP = WIDTH / size;
			scrollPos = (x * STEP);
			scrollIndex = x;
//			screenShift = -scrollPos;
			calculateMaxValue2(false);
			skipNextInvalidation = true;
			invalidate();
		}
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		WIDTH = getWidth();
		HEIGHT = getHeight();
		if (maxValueVisible > 0) {
			valueScaleY = (HEIGHT - BASE_LINE_Y - PADD_SMALL) / maxValueVisible;
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

			//Draw selection panel with scrubbler
			selectionDrawer.draw(canvas, data, linesVisibility, HEIGHT, linePaints, valueScaleY);
		}
	}

	private void drawGrid(Canvas canvas) {
		int gridValueText = (maxValueVisible / GRID_LINES_COUNT);
		int gridStep = (int) (gridValueText * valueScaleY);
		for (int i = 0; i < GRID_LINES_COUNT; i++) {
			if (i == 0) {
				canvas.drawLine(0, HEIGHT - BASE_LINE_Y - gridStep * i,
						WIDTH, HEIGHT - BASE_LINE_Y - gridStep * i, baselinePaint);
			} else {
				canvas.drawLine(0, HEIGHT - BASE_LINE_Y - gridStep * i,
						WIDTH, HEIGHT - BASE_LINE_Y - gridStep * i, gridPaint);
			}
//			canvas.drawText(AndroidUtils.formatValue(gridValueText * i),
//					0, HEIGHT - BASE_LINE_Y - gridStep * i - PADD_TINY, textPaint);
			canvas.drawText(Integer.toString((gridValueText * i)),
					0, HEIGHT - BASE_LINE_Y - gridStep * i - PADD_TINY, textPaint);
		}
	}

	private void drawChart(Canvas canvas, int[] values, int index) {
//		chartPath.rewind();
//		float start = screenShift <= 0 ? -screenShift / STEP : 0;
		float offset = -scrollPos % STEP;

		float pos = 0;
//		chartArray = new float[4* (int)Math.ceil((WIDTH/STEP))];
		int skip = (int)(-scrollPos <= 0 ? scrollIndex : 0);
		int k = skip;
		for (int i = skip; i < values.length; i++) {
			//Draw chart
//			if (pos == 0) {
//				chartPath.moveTo(pos + offset, HEIGHT - BASE_LINE_Y - values[i] * valueScaleY);
//			} else {
//				chartPath.lineTo(pos + offset, HEIGHT - BASE_LINE_Y - values[i] * valueScaleY);
//			}

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
				if (pos - STEP > WIDTH) {
					if (pos/STEP < 150) {
						Timber.v("ROUND");
						linePaints[index].setStrokeCap(Paint.Cap.ROUND);
					} else {
						Timber.v("BUTT");
						linePaints[index].setStrokeCap(Paint.Cap.BUTT);
					}
					break;
				}
			}
			pos += STEP;
		}
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
				date.setTime(data.getTime()[i * count]);
				dateText = String.valueOf(date.getTime()/100000000);//TimeUtils.formatDate(date);

				if (count*STEP > TEXT_SPACE && count*STEP < TEXT_SPACE*1.18f && (i)%2!=0) {
					timelineTextPaint.setAlpha((int)(255/(TEXT_SPACE*0.18f)*(count*STEP-TEXT_SPACE)));
				} else {
					timelineTextPaint.setAlpha(255);
				}
				canvas.drawText(dateText, pos - scrollPos, HEIGHT - PADD_NORMAL, timelineTextPaint);
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
		int prev = maxValueCalculated;
		maxValueCalculated = 0;
		for (int i = (int)(scrollPos/STEP); i < (int)((scrollPos+WIDTH)/STEP); i++) {
			if (i >= 0 && i < maxValuesLine.length && maxValuesLine[i] > maxValueCalculated) {
				maxValueCalculated = maxValuesLine[i];
			}
		}

		if (adjust) {
			maxValueCalculated = (int) adjustToGrid((float) maxValueCalculated, GRID_LINES_COUNT);
		}
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

	@Override
	public Parcelable onSaveInstanceState() {
		Parcelable superState = super.onSaveInstanceState();
		SavedState ss = new SavedState(superState);

		ss.linesVisibility = linesVisibility;
		ss.linesCalculated = linesCalculated;
		ss.scrollPos = scrollPos;
		ss.selectionX = selectionDrawer.getSelectionX();
//		ss.screenShift = screenShift;
		ss.valueScaleY = valueScaleY;
		ss.STEP = STEP;
		ss.maxValueVisible = maxValueVisible;
		ss.maxValueCalculated = maxValueCalculated;
		ss.maxValuesLine = maxValuesLine;
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
		selectionDrawer.setSelectionX(ss.selectionX);
//		screenShift = ss.screenShift;
		valueScaleY = ss.valueScaleY;
		STEP = ss.STEP;
		maxValueVisible = ss.maxValueVisible;
		maxValueCalculated = ss.maxValueCalculated;
		maxValuesLine = ss.maxValuesLine;
		data = ss.data;

		if (data != null) {
			selectionDrawer.setLinesCount(data.getLinesCount());
			linePaints = new Paint[data.getLinesCount()];
			for (int i = 0; i < data.getLinesCount(); i++) {
				linePaints[i] = createLinePaint(data.getColorsInts()[i]);
			}
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
			float[] floats = new float[5];
			in.readFloatArray(floats);
			scrollPos = floats[0];
			selectionX = floats[1];
			screenShift = floats[2];
			valueScaleY = floats[3];
			STEP = floats[4];
			maxValueVisible = in.readInt();
			maxValueCalculated = in.readInt();
			in.readIntArray(maxValuesLine);
			data = in.readParcelable(ChartData.class.getClassLoader());
		}

		@Override
		public void writeToParcel(Parcel out, int flags) {
			super.writeToParcel(out, flags);
			out.writeBooleanArray(linesVisibility);
			out.writeBooleanArray(linesCalculated);
			out.writeFloatArray(new float[] {scrollPos, selectionX, screenShift, valueScaleY, STEP});
			out.writeInt(maxValueVisible);
			out.writeInt(maxValueCalculated);
			out.writeIntArray(maxValuesLine);
			out.writeParcelable(data, Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
		}

		ChartData data;
		float STEP;
		boolean[] linesVisibility;
		boolean[] linesCalculated;
		float scrollPos;
		float selectionX;
		float screenShift;
		float valueScaleY;
		int maxValueVisible;
		int maxValueCalculated = 0;
		int[] maxValuesLine;

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
