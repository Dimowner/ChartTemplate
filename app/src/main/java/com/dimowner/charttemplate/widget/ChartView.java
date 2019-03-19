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

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import com.dimowner.charttemplate.R;
import com.dimowner.charttemplate.model.ChartData;
import com.dimowner.charttemplate.util.AndroidUtils;
import com.dimowner.charttemplate.util.TimeUtils;

import java.util.Date;

public class ChartView extends View {

	private static final float DENSITY = AndroidUtils.dpToPx(1);
	private static float STEP = 25*DENSITY;
	private static int PADDING_NORMAL = (int) (16*DENSITY);
	private static int PADDING_SMALL = (int) (8*DENSITY);
	private static int PADDING_TINY = (int) (4*DENSITY);
	private static int GRID_LINES_COUNT = 6;
	private static int TEXT_SPACE = (int) (65*DENSITY);
	private static int BASE_LINE_Y = (int) (32*DENSITY);
	private static int CIRCLE_SIZE = (int) (5*DENSITY);
	private static int SHADOW_SIZE = (int) (1.5f*DENSITY);

	private ChartData data;

	private Path chartPath;
	private RectF rect;

	private Date date;
	private boolean[] linesVisibility;

	private String dateText;

	private TextPaint textPaint;
	private Paint gridPaint;
	private Paint baselinePaint;
	private Paint linePaint;
	private Paint scrubblerPaint;
	private Paint circlePaint;
	private TextPaint selectedDatePaint;
	private TextPaint selectedNamePaint;
	private TextPaint selectedValuePaint;
	private Paint panelPaint;
	private String selectionDate;

	private float scrollPos;

	private float selectionX = -1;
	private int selectionIndex = -1;
	private float[] selectedValues;
	private float screenShift = 0;
	private float selectedDateHeight = 0;
	private float selectedNameHeight = 0;
	private float selectedValueHeight = 0;
	private float selectedItemWidth = 0;

	private float WIDTH = 0;
	private float HEIGHT = 0;
	private int maxValue = 0;
	private float valueScale = 0;

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

		rect = new RectF();
		chartPath = new Path();
		date = new Date();
		dateText = null;
		selectionDate = "";

		scrollPos = -1;

		int gridColor;
		int gridBaseLineColor;
		int gridTextColor;
		int panelTextColor;
		int panelColor;
		int scrubblerColor;

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

		linePaint = new Paint();
		linePaint.setAntiAlias(true);
		linePaint.setDither(false);
		linePaint.setStyle(Paint.Style.STROKE);
		linePaint.setStrokeWidth(2.2f*DENSITY);
		linePaint.setStrokeJoin(Paint.Join.ROUND);
		linePaint.setStrokeCap(Paint.Cap.ROUND);
//		linePaint.setPathEffect(new CornerPathEffect(AndroidUtils.dpToPx(8)));
		linePaint.setColor(context.getResources().getColor(R.color.md_yellow_A700));

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

		panelPaint = new Paint();
		panelPaint.setStyle(Paint.Style.FILL);
		panelPaint.setColor(panelColor);
		panelPaint.setAntiAlias(true);
		panelPaint.setShadowLayer(SHADOW_SIZE, 0, 0, context.getResources().getColor(R.color.shadow));
//		TODO: investigate how it influences on performance. Find way to increase performance.
//		setLayerType(LAYER_TYPE_SOFTWARE, panelPaint);

		circlePaint = new Paint();
		circlePaint.setStyle(Paint.Style.FILL);
		circlePaint.setColor(panelColor);

		selectedDatePaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
		selectedDatePaint.setColor(panelTextColor);
		selectedDatePaint.setTextAlign(Paint.Align.LEFT);
		selectedDatePaint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
		selectedDatePaint.setTextSize(context.getResources().getDimension(R.dimen.text_medium));

		selectedNamePaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
		selectedNamePaint.setTextAlign(Paint.Align.LEFT);
		selectedNamePaint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
		selectedNamePaint.setTextSize(context.getResources().getDimension(R.dimen.text_large));

		selectedValuePaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
		selectedValuePaint.setTextAlign(Paint.Align.LEFT);
		selectedValuePaint.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
		selectedValuePaint.setTextSize(context.getResources().getDimension(R.dimen.text_normal));

		scrubblerPaint = new Paint();
		scrubblerPaint.setAntiAlias(false);
		scrubblerPaint.setDither(false);
		scrubblerPaint.setStyle(Paint.Style.STROKE);
		scrubblerPaint.setColor(scrubblerColor);
		scrubblerPaint.setStrokeWidth(1.5f*DENSITY);

		setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent motionEvent) {
				switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {
					case MotionEvent.ACTION_DOWN:
						break;
					case MotionEvent.ACTION_MOVE:
						selectionX = motionEvent.getX();
						if (selectionX > WIDTH) {
							selectionX = WIDTH;
						}
						if (selectionX < 0) {
							selectionX = -1;
						}
						calculatePanelSize();
						if (onMoveEventsListener != null) {
							onMoveEventsListener.onMoveEvent();
						}
						invalidate();
						break;
					case MotionEvent.ACTION_UP:
						selectionX = -1;
						invalidate();
						performClick();
						break;
				}
				return true;
			}
		});
	}

	private void calculatePanelSize() {
		calculateSelectedItemIndex();

		Rect r = new Rect();
		float width = 0;
		float valuesWidth = 0;
		selectedItemWidth = 0;
		String v;
		//Calculate intermediate Y val for each line. It will be used for drawing line circles.
		for (int i = 0; i < data.getLinesCount(); i++) {
			if (linesVisibility[i]) {
				if (selectionIndex + 1 < data.getLength()) {
					selectedValues[i] = calculateValY(
							scrollPos + selectionX,  //X
							selectionIndex * STEP, //X1
							(selectionIndex + 1) * STEP, //X2
							data.getValues(i)[selectionIndex], //Y1
							data.getValues(i)[selectionIndex + 1] //Y2
					);
				}
				selectedNamePaint.getTextBounds(data.getNames()[i], 0, data.getNames()[i].length(), r);
				width += r.width();

				selectedItemWidth = selectedItemWidth < r.width() ? r.width() : selectedItemWidth;

				if (selectedNameHeight == 0) selectedNameHeight = r.height();
				v = String.valueOf((data.getValues(i)[selectionIndex]));
				selectedValuePaint.getTextBounds(v, 0, v.length(), r);
				valuesWidth += r.width();

				selectedItemWidth = selectedItemWidth < r.width() ? r.width() : selectedItemWidth;
				if (selectedValueHeight == 0) selectedValueHeight = r.height();
			}
		}

		//Calculate date size
		date.setTime(data.getTime()[selectionIndex]);
		selectionDate = TimeUtils.formatDate(date);
		selectedDatePaint.getTextBounds(selectionDate, 0, selectionDate.length(), r);

		if (selectedDateHeight == 0) {
			selectedDateHeight = r.height();
		}

		if (width < valuesWidth) {
			width = valuesWidth;
		}
		if (r.width() > width) {
			width = r.width();
		}

		//Set panel size.
		rect.left = selectionX - PADDING_NORMAL;
		rect.right = selectionX + width+data.getLinesCount()*PADDING_NORMAL;
		rect.top = PADDING_NORMAL;
		rect.bottom = 3.5f*PADDING_NORMAL + selectedDateHeight + selectedNameHeight + selectedValueHeight;

		//Set Panel edges
		if (rect.right > WIDTH-PADDING_SMALL) {
			float w = rect.width();
			rect.right = WIDTH-PADDING_SMALL;
			rect.left = WIDTH-PADDING_SMALL-w;
		}
		if (rect.left < PADDING_SMALL) {
			float w = rect.width();
			rect.left = PADDING_SMALL;
			rect.right = PADDING_SMALL+w;
		}
	}

	private void calculateSelectedItemIndex() {
		selectionIndex = (int)((scrollPos + selectionX)/STEP);
		if (selectionIndex >= data.getLength()-1) {
			selectionIndex = data.getLength()-1;
		}
	}

	/**
	 * Calculate intermediate values Y between two X time coordinates using equation:
	 * (x - x2)/(x2 - x1) = (y - y1)/(y2 - y1) then
	 *  y = (x - x2)*(y2 - y1)/(x2 - x1) + y2
	 */
	private float calculateValY(float x, float x1, float x2, float y1, float y2) {
		return  (x - x2)*(y2 - y1)/(x2 - x1) + y2;
	}

	public void scrollPos(float x, float size) {
		STEP = WIDTH/size;
		scrollPos = (x*STEP);
		screenShift = -scrollPos;
		invalidate();
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		WIDTH = getWidth();
		HEIGHT = getHeight();
		valueScale = (HEIGHT-BASE_LINE_Y-PADDING_SMALL)/maxValue;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		if (data != null) {
			textPaint.setTextAlign(Paint.Align.LEFT);
			drawGrid(canvas);

			//Draw scrubbler
			if (selectionX >= 0) {
				canvas.drawLine(selectionX, 0, selectionX, HEIGHT, scrubblerPaint);
			}

			if (data.getLinesCount() > 0) {
				drawTimeline(canvas, data.getValues(0).length);
			}

			//Draw charts
			textPaint.setTextAlign(Paint.Align.CENTER);
			for (int i = 0; i < data.getNames().length; i++) {
				if (linesVisibility[i]) {
					drawChart(canvas, data.getValues(i), data.getColorsInts()[i]);
				}
			}

			if (selectionX >= 0) {
				//Draw circles on charts
				for (int i = 0; i < data.getNames().length; i++) {
					if (linesVisibility[i]) {
						linePaint.setColor(data.getColorsInts()[i]);
						canvas.drawCircle(selectionX,
								HEIGHT - BASE_LINE_Y - selectedValues[i] * valueScale,
								CIRCLE_SIZE, circlePaint);
						canvas.drawCircle(selectionX,
								HEIGHT - BASE_LINE_Y - selectedValues[i] * valueScale,
								CIRCLE_SIZE, linePaint);
					}
				}
				//Draw selection panel
				canvas.drawRoundRect(rect, PADDING_TINY, PADDING_TINY, panelPaint);
				baselinePaint.setAntiAlias(true);
				canvas.drawRoundRect(rect, PADDING_TINY, PADDING_TINY, baselinePaint);
				//Draw date on panel
				canvas.drawText(selectionDate, rect.left+PADDING_NORMAL,
						rect.top+selectedDateHeight+PADDING_SMALL, selectedDatePaint);
				//Draw names and values on panel
				for (int i = 0; i < data.getNames().length; i++) {
					if (linesVisibility[i]) {
						selectedNamePaint.setColor(data.getColorsInts()[i]);
						canvas.drawText(data.getNames()[i],
								rect.left+PADDING_NORMAL + selectedItemWidth*i + PADDING_NORMAL*i,
								rect.top+selectedDateHeight+selectedNameHeight+3*PADDING_SMALL, selectedNamePaint);
						selectedValuePaint.setColor(data.getColorsInts()[i]);
						canvas.drawText(String.valueOf((data.getValues(i)[selectionIndex])),
								rect.left+PADDING_NORMAL + selectedItemWidth*i+ PADDING_NORMAL*i,
								rect.top+selectedDateHeight+selectedNameHeight+selectedValueHeight+4*PADDING_SMALL,
								selectedValuePaint);
					}
				}
			}
		}
	}

	private void drawGrid(Canvas canvas) {
		int gridValueText = (maxValue / GRID_LINES_COUNT);
		int gridStep = (int) (gridValueText * valueScale);
		for (int i = 0; i < GRID_LINES_COUNT; i++) {
			if (i == 0) {
				canvas.drawLine(0, HEIGHT - BASE_LINE_Y - gridStep * i,
						WIDTH, HEIGHT - BASE_LINE_Y - gridStep * i, baselinePaint);
			} else {
				canvas.drawLine(0, HEIGHT - BASE_LINE_Y - gridStep * i,
						WIDTH, HEIGHT - BASE_LINE_Y - gridStep * i, gridPaint);
			}
			if (i > 0) {
				canvas.drawText(Integer.toString((gridValueText * i)),
						0, HEIGHT - BASE_LINE_Y - gridStep * i - PADDING_TINY, textPaint);
			}
		}
	}

	private void drawChart(Canvas canvas, int[] values, int color) {
		linePaint.setColor(color);

		chartPath.reset();
		float start = screenShift <= 0 ? -screenShift / STEP : 0;
		float offset = screenShift % STEP;

		float pos = 0;
		for (int i = (int) start; i < values.length; i++) {
			//Draw chart
			if (pos == 0) {
				chartPath.moveTo(pos + offset, HEIGHT - BASE_LINE_Y - values[i] * valueScale);
			} else {
				chartPath.lineTo(pos + offset, HEIGHT - BASE_LINE_Y - values[i] * valueScale);
			}

			if (pos - STEP > WIDTH) {
				break;
			}
			pos += STEP;
		}
		canvas.drawPath(chartPath, linePaint);
	}

	private void drawTimeline(Canvas canvas, int length) {

		float start = screenShift <= 0 ? -screenShift / STEP : 0;
		float offset = screenShift % STEP;

		float pos = 0;
		for (int i = (int) start; i < length; i++) {
			date.setTime(data.getTime()[i]);
			dateText = TimeUtils.formatDate(date);
			if (TEXT_SPACE < STEP) {
				canvas.drawText(dateText, pos + offset, HEIGHT - PADDING_NORMAL, textPaint);
			} else if (i % (Math.ceil(TEXT_SPACE / STEP)) == 0) {
				canvas.drawText(dateText, pos + offset, HEIGHT - PADDING_NORMAL, textPaint);
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
			linesVisibility[pos] = false;
		}
		invalidate();
	}

	public void showLine(String name) {
		int pos = findLinePosition(name);
		if (pos >= 0) {
			linesVisibility[pos] = true;
		}
		invalidate();
	}

	public void setData(ChartData d) {
		this.data = d;
		if (data != null) {
			maxValue = 0;
			for (int i = 0; i < data.getLength(); i++) {
				if (data.getValues(0)[i] > maxValue) {
					maxValue = data.getValues(0)[i];
				}
			}
			//Init lines visibility state, all visible by default.
			linesVisibility = new boolean[data.getLinesCount()];
			selectedValues = new float[data.getLinesCount()];
			for (int i = 0; i < linesVisibility.length; i++) {
				linesVisibility[i] = true;
				selectedValues[i] = 0;
			}
		}
		valueScale = (HEIGHT-BASE_LINE_Y-PADDING_SMALL)/maxValue;
		selectionX = -1;
		invalidate();
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

	public interface OnMoveEventsListener {
		void onMoveEvent();
	}
}
