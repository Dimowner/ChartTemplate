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
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import com.dimowner.charttemplate.AppConstants;
import com.dimowner.charttemplate.R;
import com.dimowner.charttemplate.model.ChartData;
import com.dimowner.charttemplate.util.AndroidUtils;
import com.dimowner.charttemplate.util.TimeUtils;

import java.util.Date;

import timber.log.Timber;

public class ChartView extends View {

	private static int STEP = (int) AndroidUtils.dpToPx(AppConstants.DEFAULT_STEP);
	private static int PADDING_SMALL = (int) AndroidUtils.dpToPx(8);
	private static int PADDING_TINY = (int) AndroidUtils.dpToPx(4);
	private static int GRID_LINES_COUNT = 6;
	private static int TEXT_SPACE = (int) AndroidUtils.dpToPx(65);

	private ChartData data;

	private Path chartPath;
	private Date date;
	private boolean[] linesVisibility;

	private String dateText;

	private Paint gridPaint;
	private TextPaint textPaint;

	private Paint linePaint;

	private long playProgressPx;

	private boolean showRecording = false;

	private float textHeight;

	private int prevScreenShift = 0;
	private float startX = 0;
	private int screenShift = 0;

	private int WIDTH = 0;
	private int HEIGHT = 0;
	private int maxValue = 0;
	private float valueScale = 0;

	private OnSeekListener onSeekListener;

	private GestureDetector gestureDetector;

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

		int gridColor;
		int gridBaseLineColor;
		int gridTextColor;

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

		linePaint = new Paint();
		linePaint.setAntiAlias(true);
		linePaint.setDither(false);
		linePaint.setStyle(Paint.Style.STROKE);
		linePaint.setStrokeWidth(AndroidUtils.dpToPx(2.2f));
		linePaint.setStrokeJoin(Paint.Join.ROUND);
		linePaint.setStrokeCap(Paint.Cap.ROUND);
//		linePaint.setPathEffect(new CornerPathEffect(AndroidUtils.dpToPx(8)));
		linePaint.setColor(context.getResources().getColor(R.color.md_yellow_A700));

		gridPaint = new Paint();
		gridPaint.setAntiAlias(false);
		linePaint.setDither(false);
		gridPaint.setStyle(Paint.Style.STROKE);
		gridPaint.setColor(gridColor);
		gridPaint.setStrokeWidth(AndroidUtils.dpToPx(1));

		textHeight = context.getResources().getDimension(R.dimen.text_normal);
		textPaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
		textPaint.setColor(gridTextColor);
		textPaint.setStrokeWidth(AndroidUtils.dpToPx(1));
		textPaint.setTextAlign(Paint.Align.CENTER);
		textPaint.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
		textPaint.setTextSize(textHeight);

		playProgressPx = -1;

		gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
			@Override
			public boolean onDown(MotionEvent e) {
				return true;
			}

			@Override
			public boolean onDoubleTap(MotionEvent e) {
				//TODO: zoom view
				return true;
			}
		});

		setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent motionEvent) {
				if (!showRecording) {
					switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {
						case MotionEvent.ACTION_DOWN:
							startX = motionEvent.getX();
							break;
						case MotionEvent.ACTION_MOVE:
							int shift = (int) (prevScreenShift + (motionEvent.getX()) - startX);
							if (data != null) {
								//Right char move edge
								if (shift <= -data.getLength() * STEP - PADDING_SMALL + WIDTH) {
									shift = -data.getLength() * STEP - PADDING_SMALL + WIDTH;
								}
							}
							//Left chart move edge
							if (shift > 0) {
								shift = 0;
							}
							if (onSeekListener != null) {
								onSeekListener.onSeeking(-screenShift);
							}
							playProgressPx = -shift;
							updateShifts(shift);
							invalidate();
							break;
						case MotionEvent.ACTION_UP:
							if (onSeekListener != null) {
								onSeekListener.onSeek(-screenShift);
							}
							prevScreenShift = screenShift;
							performClick();
							break;
					}
				}
				return gestureDetector.onTouchEvent(motionEvent);
			}
		});
	}

	public void seekPx(int px) {
		playProgressPx = px;
		updateShifts((int)-playProgressPx);
		prevScreenShift = screenShift;
		invalidate();
		if (onSeekListener != null) {
			onSeekListener.onSeeking(-screenShift);
		}
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		WIDTH = getWidth();
		HEIGHT = getHeight();
		valueScale = (float)HEIGHT/(float)maxValue;
		Timber.v("Width = "+ WIDTH + " HEIGHT = " + HEIGHT + " valScale = " + valueScale);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		if (data != null) {
			textPaint.setTextAlign(Paint.Align.LEFT);
			drawGrid(canvas);

			textPaint.setTextAlign(Paint.Align.CENTER);
			for (int i = 0; i < data.getNames().length; i++) {
				if (linesVisibility[i]) {
					drawChart(canvas, data.getValues(i), data.getColors()[i]);
				}
			}
		}
	}

	private void drawGrid(Canvas canvas) {
		int gridValueText = (maxValue / GRID_LINES_COUNT);
		int gridStep = (int) (gridValueText * valueScale);
		for (int i = 0; i < GRID_LINES_COUNT; i++) {
			canvas.drawLine(0, HEIGHT - gridStep * i, WIDTH, HEIGHT - gridStep * i, gridPaint);
			if (i > 0) {
				canvas.drawText(Integer.toString((gridValueText * i)), 0, HEIGHT - gridStep * i - PADDING_TINY, textPaint);
			}
		}
	}

	private void drawChart(Canvas canvas, int[] values, String color) {
		linePaint.setColor(Color.parseColor(color));

		chartPath.reset();
		int start = screenShift <= 0 ? -screenShift / STEP : 0;
		int offset = screenShift % STEP;

		int pos = 0;
		for (int i = start; i < values.length; i++) {
			//Draw chart
			if (pos == 0) {
				chartPath.moveTo(pos + offset, HEIGHT - values[i] * valueScale);
			} else {
				chartPath.lineTo(pos + offset, HEIGHT - values[i] * valueScale);
			}

			//TODO: Fix this!!! draw dates only once.
			//Draw dates
			date.setTime(data.getTime()[i]);
			dateText = TimeUtils.formatDate(date);
			if (TEXT_SPACE < STEP) {
				canvas.drawText(dateText, pos + offset, HEIGHT - PADDING_SMALL, textPaint);
			} else if (i % (Math.ceil(TEXT_SPACE/(float) STEP)) == 0) {
				canvas.drawText(dateText, pos + offset, HEIGHT - PADDING_SMALL, textPaint);
			}

			if (pos - STEP > WIDTH) {
				break;
			}
			pos += STEP;
		}
		canvas.drawPath(chartPath, linePaint);
	}

	private void updateShifts(int px) {
		screenShift = px;
	}

	public static int getStep() {
		return STEP;
	}

	public static void setStep(int s) {
		ChartView.STEP = s;
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
			for (int i = 0; i < data.getLength(); i++) {
				if (data.getValues(0)[i] > maxValue) {
					maxValue = data.getValues(0)[i];
				}
			}
			Timber.v("maxValue = %s", maxValue);
			//Init lines visibility state, all visible by default.
			linesVisibility = new boolean[data.getLinesCount()];
			for (int i = 0; i < linesVisibility.length; i++) {
				linesVisibility[i] = true;
			}
		}
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

	public void setOnSeekListener(OnSeekListener onSeekListener) {
		this.onSeekListener = onSeekListener;
	}

	public interface OnSeekListener {
		void onSeek(int px);
		void onSeeking(int px);
	}
}
