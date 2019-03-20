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
import android.graphics.Path;
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
		TEXT_SPACE = (int) (65*DENSITY);
		BASE_LINE_Y = (int) (32*DENSITY);
	}

	private float STEP = 25*DENSITY;

	private ChartData data;

	private Path chartPath;

	private Date date;
	private boolean[] linesVisibility;
	private boolean[] linesCalculated;

	private String dateText;

	private TextPaint textPaint;
	private Paint gridPaint;
	private Paint baselinePaint;
	private Paint[] linePaints;

	private ValueAnimator animator;
	private ValueAnimator alphaAnimator;

	private float scrollPos;
	private float screenShift = 0;

	private ChartSelectionDrawer selectionDrawer;

	private float WIDTH = 0;
	private float HEIGHT = 0;
	private int maxValueY = 0;
	private float valueScaleY = 0;

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

		chartPath = new Path();
		date = new Date();
		dateText = null;
		scrollPos = -1;

		int gridColor;
		int gridBaseLineColor;
		int gridTextColor;
		int panelTextColor;
		int panelColor;
		int scrubblerColor;
		int shadowColor;

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

		selectionDrawer = new ChartSelectionDrawer(getContext(), panelTextColor,
					panelColor, scrubblerColor, shadowColor);

		gridPaint = new Paint();
		gridPaint.setAntiAlias(false);
		gridPaint.setDither(false);
		gridPaint.setStyle(Paint.Style.STROKE);
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

		setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent motionEvent) {
				switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {
					case MotionEvent.ACTION_DOWN:
						selectionDrawer.reset();
						break;
					case MotionEvent.ACTION_MOVE:
						float selectionX = motionEvent.getX();
						if (selectionX > WIDTH) {
							selectionX = WIDTH;
						}
						if (selectionX < 0) {
							selectionX = -1;
						}
						selectionDrawer.setSelectionX(selectionX);
						selectionDrawer.calculatePanelSize(data, STEP, linesCalculated, scrollPos, WIDTH);
						if (onMoveEventsListener != null) {
							onMoveEventsListener.onMoveEvent();
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
		Paint lp = new Paint();
		lp.setAntiAlias(true);
		lp.setDither(false);
		lp.setStyle(Paint.Style.STROKE);
		lp.setStrokeWidth(2.2f*DENSITY);
		lp.setStrokeJoin(Paint.Join.ROUND);
		lp.setStrokeCap(Paint.Cap.ROUND);
		lp.setColor(color);
		return lp;
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
				valueScaleY = (HEIGHT-BASE_LINE_Y-PADD_SMALL)/ maxValueY;
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

	public void scrollPos(float x, float size) {
		STEP = WIDTH/size;
		scrollPos = (x*STEP);
		screenShift = -scrollPos;
//		calculateMaxValue();
		invalidate();
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		WIDTH = getWidth();
		HEIGHT = getHeight();
		valueScaleY = (HEIGHT-BASE_LINE_Y-PADD_SMALL)/ maxValueY;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (data != null) {
			textPaint.setTextAlign(Paint.Align.LEFT);
			drawGrid(canvas);

			if (data.getLinesCount() > 0) {
				drawTimeline(canvas, data.getValues(0).length);
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
		int gridValueText = (maxValueY / GRID_LINES_COUNT);
		int gridStep = (int) (gridValueText * valueScaleY);
		for (int i = 0; i < GRID_LINES_COUNT; i++) {
			if (i == 0) {
				canvas.drawLine(0, HEIGHT - BASE_LINE_Y - gridStep * i,
						WIDTH, HEIGHT - BASE_LINE_Y - gridStep * i, baselinePaint);
			} else {
				canvas.drawLine(0, HEIGHT - BASE_LINE_Y - gridStep * i,
						WIDTH, HEIGHT - BASE_LINE_Y - gridStep * i, gridPaint);
			}
			canvas.drawText(Integer.toString((gridValueText * i)),
					0, HEIGHT - BASE_LINE_Y - gridStep * i - PADD_TINY, textPaint);
		}
	}

	private void drawChart(Canvas canvas, int[] values, int index) {
		chartPath.reset();
		float start = screenShift <= 0 ? -screenShift / STEP : 0;
		float offset = screenShift % STEP;

		float pos = 0;
		for (int i = (int) start; i < values.length; i++) {
			//Draw chart
			if (pos == 0) {
				chartPath.moveTo(pos + offset, HEIGHT - BASE_LINE_Y - values[i] * valueScaleY);
			} else {
				chartPath.lineTo(pos + offset, HEIGHT - BASE_LINE_Y - values[i] * valueScaleY);
			}

			if (pos - STEP > WIDTH) {
				break;
			}
			pos += STEP;
		}
		canvas.drawPath(chartPath, linePaints[index]);
	}

	private void drawTimeline(Canvas canvas, int length) {

		float start = screenShift <= 0 ? -screenShift / STEP : 0;
		float offset = screenShift % STEP;

		float pos = 0;
		for (int i = (int) start; i < length; i++) {
			date.setTime(data.getTime()[i]);
			dateText = TimeUtils.formatDate(date);
			if (TEXT_SPACE < STEP) {
				canvas.drawText(dateText, pos + offset, HEIGHT - PADD_NORMAL, textPaint);
			} else if (i % (Math.ceil(TEXT_SPACE / STEP)) == 0) {
				canvas.drawText(dateText, pos + offset, HEIGHT - PADD_NORMAL, textPaint);
			}

			if (pos - STEP > WIDTH) {
				break;
			}
			pos += STEP;
		}
	}

	public void hideLine(String name) {
		int pos = findLinePosition(name);
		if (pos >= 0) {
			linesCalculated[pos] = false;
			alphaAnimator(linePaints[pos].getAlpha(), 0, pos, false);
		}
		calculateMaxValue(false);
	}

	public void showLine(String name) {
		int pos = findLinePosition(name);
		if (pos >= 0) {
			linesVisibility[pos] = true;
			linesCalculated[pos] = true;
			alphaAnimator(linePaints[pos].getAlpha(), 255, pos, true);
		}
		calculateMaxValue(false);
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
			calculateMaxValue(true);
		}
		selectionDrawer.setSelectionX(-1);
	}

	private void calculateMaxValue(boolean invalidate) {
		int prev = maxValueY;
		maxValueY = 0;
//		for (int i = (int)(scrollPos/STEP); i < (int)((scrollPos+WIDTH)/STEP); i++) {
		for (int j = 0; j < data.getLinesCount(); j++) {
			for (int i = 0; i < data.getLength(); i++) {
				if (linesCalculated[j]) {
					if (data.getValues(j)[i] > maxValueY) {
						maxValueY = data.getValues(j)[i];
					}
				}
			}
		}
		maxValueY = (int) adjustToGrid((float) maxValueY, GRID_LINES_COUNT);
		valueScaleY = (HEIGHT-BASE_LINE_Y-PADD_SMALL)/ maxValueY;
		if (prev != maxValueY) {
			animation(prev, maxValueY, invalidate);
		}
	}

	private float adjustToGrid(float val, int scale) {
		int amp = 1;
		while (val > scale*100) {
			val = (int)Math.floor(val/(scale*10));
			amp *=(scale*10);
		}
		if (val > (scale*10)) {
			val = (float) Math.ceil(val/scale);
			amp *=scale;
		}
		return val*amp;
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
		ss.screenShift = screenShift;
		ss.valueScaleY = valueScaleY;
		ss.STEP = STEP;
		ss.maxValueY = maxValueY;
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
		screenShift = ss.screenShift;
		valueScaleY = ss.valueScaleY;
		STEP = ss.STEP;
		maxValueY = ss.maxValueY;
		data = ss.data;

		selectionDrawer.setLinesCount(data.getLinesCount());
		linePaints = new Paint[data.getLinesCount()];
		for (int i = 0; i < data.getLinesCount(); i++) {
			linePaints[i] = createLinePaint(data.getColorsInts()[i]);
		}
	}

	public interface OnMoveEventsListener {
		void onMoveEvent();
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
			maxValueY = in.readInt();
			data = in.readParcelable(ChartData.class.getClassLoader());
		}

		@Override
		public void writeToParcel(Parcel out, int flags) {
			super.writeToParcel(out, flags);
			out.writeBooleanArray(linesVisibility);
			out.writeBooleanArray(linesCalculated);
			out.writeFloatArray(new float[] {scrollPos, selectionX, screenShift, valueScaleY, STEP});
			out.writeInt(maxValueY);
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
		int maxValueY;

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
