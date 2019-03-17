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
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import com.dimowner.charttemplate.R;
import com.dimowner.charttemplate.model.ChartData;
import com.dimowner.charttemplate.util.AndroidUtils;

import timber.log.Timber;

public class ChartScrollView extends View {

	private final static float SELECTION = AndroidUtils.dpToPx(5);
	private final static float SELECTION_HALF = SELECTION/2;
	private float selectionX = AndroidUtils.dpToPx(80);

	private ChartData data;
	private boolean[] linesVisibility;
	private Path path;
	private float STEP = 1;

	private Paint linePaint;
	private Paint overlayPaint;
	private Paint selectionPaint;

	private float scrollX;
	private float moveStartX = 0;
	private boolean isInSelection = false;

	private float WIDTH = 0;
	private float HEIGHT = 0;
	private int maxValueY = 0;
	private float valueScaleY = 0;

	private OnScrollListener onScrollListener;

	private GestureDetector gestureDetector;

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

		int selectionColor;
		int overlayColor;
		Resources res = context.getResources();
		TypedValue typedValue = new TypedValue();
		Resources.Theme theme = context.getTheme();
		if (theme.resolveAttribute(R.attr.selectionColor, typedValue, true)) {
			selectionColor = typedValue.data;
		} else {
			selectionColor = res.getColor(R.color.selection_color);
		}
		if (theme.resolveAttribute(R.attr.overlayColor, typedValue, true)) {
			overlayColor = typedValue.data;
		} else {
			overlayColor = res.getColor(R.color.overlay_color);
		}

		linePaint = new Paint();
		linePaint.setAntiAlias(true);
		linePaint.setDither(false);
		linePaint.setStyle(Paint.Style.STROKE);
		linePaint.setStrokeWidth(AndroidUtils.dpToPx(1.2f));
		linePaint.setStrokeJoin(Paint.Join.ROUND);
		linePaint.setStrokeCap(Paint.Cap.ROUND);
		linePaint.setColor(context.getResources().getColor(R.color.md_yellow_A700));

		selectionPaint = new Paint();
		selectionPaint.setAntiAlias(false);
		selectionPaint.setDither(false);
		selectionPaint.setStyle(Paint.Style.STROKE);
		selectionPaint.setStrokeWidth(SELECTION);
		selectionPaint.setColor(selectionColor);

		overlayPaint = new Paint();
		overlayPaint.setAntiAlias(false);
		overlayPaint.setDither(false);
		overlayPaint.setStyle(Paint.Style.FILL);
		overlayPaint.setColor(overlayColor);

		scrollX = -1;

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
				switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {
					case MotionEvent.ACTION_DOWN:
						moveStartX = motionEvent.getX();
						isInSelection = checkInSelection(moveStartX);
						break;
					case MotionEvent.ACTION_MOVE:
						if (isInSelection) {
							float shift = (motionEvent.getX()) - moveStartX;
							scrollX = moveStartX + shift;
							//Set scroll edges.
							if (scrollX + selectionX > WIDTH) {
								scrollX = WIDTH - selectionX;
							} else if (scrollX < 0) {
								scrollX = 0;
							}

							if (onScrollListener != null) {
								onScrollListener.onScrolling((int)(scrollX/STEP));
							}
							invalidate();
						}
						break;
					case MotionEvent.ACTION_UP:
						if (onScrollListener != null) {
							onScrollListener.onScrolled((int)(scrollX/STEP));
						}

						performClick();
						break;
				}
				return gestureDetector.onTouchEvent(motionEvent);
			}
		});
	}

	private boolean checkInSelection(float x) {
		return (x > scrollX && x < scrollX + selectionX);
	}

//	private void updateShifts(float px) {
//		screenShift = px;
//	}

//
//	public void seekPx(int px) {
//		scrollX = px;
//		updateShifts((int)-scrollX);
//		prevScreenShift = screenShift;
//		invalidate();
//		if (onScrollListener != null) {
//			onScrollListener.onScrolling((int)-screenShift);
//		}
//	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		WIDTH = getWidth();
		HEIGHT = getHeight();
		valueScaleY = HEIGHT/(maxValueY + SELECTION);
		Timber.v("Width = "+ WIDTH + " HEIGHT = " + HEIGHT + " valScale = " + valueScaleY);

		if (data != null) {
			STEP = (WIDTH/data.getLength());
		}

		scrollX = WIDTH-selectionX;
		Timber.v("STEP = " + STEP);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (data != null) {
			for (int i = 0; i < data.getNames().length; i++) {
				if (linesVisibility[i]) {
					drawChart(canvas, data.getValues(i), data.getColors()[i]);
				}
			}
		}

		//Draw overlay
		path.reset();
		path.moveTo(0, 0);
		path.lineTo(0, HEIGHT);
		path.lineTo(scrollX - SELECTION_HALF, HEIGHT);
		path.lineTo(scrollX - SELECTION_HALF, 0);
		path.close();
		canvas.drawPath(path, overlayPaint);

		path.reset();
		path.moveTo(scrollX + selectionX + SELECTION_HALF, 0);
		path.lineTo(scrollX + selectionX + SELECTION_HALF, HEIGHT);
		path.lineTo(WIDTH, HEIGHT);
		path.lineTo(WIDTH, 0);
		path.close();
		canvas.drawPath(path, overlayPaint);

		//Draw selection borders
		path.reset();
		path.moveTo(scrollX, 0);
		path.lineTo(scrollX, HEIGHT);
		path.moveTo(scrollX + selectionX, HEIGHT);
		path.lineTo(scrollX + selectionX, 0);
		selectionPaint.setStrokeWidth(SELECTION);
		canvas.drawPath(path, selectionPaint);

		path.reset();
		path.moveTo(scrollX + SELECTION_HALF, 0);
		path.lineTo(scrollX + selectionX - SELECTION_HALF, 0);
		path.moveTo(scrollX + SELECTION_HALF, HEIGHT);
		path.lineTo(scrollX + selectionX - SELECTION_HALF, HEIGHT);
		selectionPaint.setStrokeWidth(SELECTION_HALF);
		canvas.drawPath(path, selectionPaint);

	}

	private void drawChart(Canvas canvas, int[] values, String color) {
		linePaint.setColor(Color.parseColor(color));
		path.reset();
		float x = 0;
		for (int i = 0; i < values.length; i++) {
			if (x == 0) {
				path.moveTo(x, HEIGHT - values[i] * valueScaleY);
			} else {
				path.lineTo(x, HEIGHT - values[i] * valueScaleY);
			}

			if (x - STEP > WIDTH) {
				break;
			}
			x += STEP;
		}
		canvas.drawPath(path, linePaint);
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
				if (data.getValues(0)[i] > maxValueY) {
					maxValueY = data.getValues(0)[i];
				}
			}
			Timber.v("maxValueY = %s", maxValueY);
			//Init lines visibility state, all visible by default.
			linesVisibility = new boolean[data.getLinesCount()];
			for (int i = 0; i < linesVisibility.length; i++) {
				linesVisibility[i] = true;
			}
			if (WIDTH > 0 && data.getLength() > 0) {
				STEP = (WIDTH / data.getLength());
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

	public void setOnScrollListener(OnScrollListener onScrollListener) {
		this.onScrollListener = onScrollListener;
	}
}
