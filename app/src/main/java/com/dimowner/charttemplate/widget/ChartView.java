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
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import com.dimowner.charttemplate.AppConstants;
import com.dimowner.charttemplate.R;
import com.dimowner.charttemplate.model.Data2;
import com.dimowner.charttemplate.util.AndroidUtils;
import com.dimowner.charttemplate.util.TimeUtils;

import timber.log.Timber;

public class ChartView extends View {

	private static final float PADD = 15.0f;
	private static final int END_PADD = 30;
	private static int STEP = (int) AndroidUtils.dpToPx(AppConstants.DEFAULT_STEP);
	private static int PADDING_NORMAL = (int) AndroidUtils.dpToPx(16);

	private Data2 data;

	private Paint gridPaint;
	private TextPaint textPaint;

	private Paint linePaint;

	private long playProgressPx;

	private boolean showRecording = false;

	private float textHeight;
	private float inset;

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

		linePaint = new Paint();
		linePaint.setAntiAlias(false);
		linePaint.setStyle(Paint.Style.STROKE);
		linePaint.setStrokeWidth(AndroidUtils.dpToPx(2));
		linePaint.setColor(context.getResources().getColor(R.color.md_yellow_A700));

		gridPaint = new Paint();
		gridPaint.setColor(context.getResources().getColor(R.color.md_grey_100));
		gridPaint.setStrokeWidth(AndroidUtils.dpToPx(1)/2);

		textHeight = context.getResources().getDimension(R.dimen.text_normal);
		inset = textHeight + PADD;
		textPaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
		textPaint.setColor(context.getResources().getColor(R.color.md_grey_100));
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
								if (shift <= -data.getLength() * STEP - END_PADD + WIDTH) {
									shift = -data.getLength() * STEP - END_PADD + WIDTH;
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

			//==================Draw grid ==============
			int lineCount = 6;
			int gridValue = (maxValue / lineCount);
			int lineStep = (int) (gridValue * valueScale);
			for (int i = 0; i < lineCount; i++) {
				canvas.drawLine(0, HEIGHT - lineStep * i, WIDTH, HEIGHT - lineStep * i, gridPaint);
				if (i > 0) {
					canvas.drawText(Integer.toString((gridValue * i)), PADDING_NORMAL, HEIGHT - lineStep * i - PADD, textPaint);
				}
			}

			for (int i = 0; i < data.getNames().length; i++) {
				drawChart(canvas, data.getValues(i), data.getColors()[i]);
			}
		}
	}

	private void drawChart(Canvas canvas, int[] vals, String color) {
		Timber.v("drawPath2 screenShift = " + screenShift + " height = " + HEIGHT + " maxVal = " + maxValue * valueScale
				+ " valScale = " + valueScale);
		linePaint.setColor(Color.parseColor(color));
		int bottomPadd = 0;
		int pos = 0;
		Path path2 = new Path();
		int start = screenShift <= 0 ? -screenShift / STEP : 0;
		int offset = screenShift % STEP;

		int w = 150;//TODO: textWidth to DP
		for (int i = start; i < vals.length; i++) {
			if (pos == 0) {
				path2.moveTo(pos + offset, HEIGHT - bottomPadd - vals[i] * valueScale);
			} else {
				path2.lineTo(pos + offset, HEIGHT - bottomPadd - vals[i] * valueScale);
			}

			String text = TimeUtils.formatDate(data.getTime()[i]);

			Timber.v("textWidth = " + w + " step = " + STEP + " t/s = " + w / (float) STEP
					+ " i = " + i % (Math.ceil(w / (float) STEP)) + " round = " + Math.ceil(w / (float) STEP));
			if (w < STEP) {
				canvas.drawText(text, pos + offset, HEIGHT - PADD, textPaint);
			} else if (i % (Math.ceil(w / (float) STEP)) == 0) {
				canvas.drawText(text, pos + offset, HEIGHT - PADD, textPaint);
			}

			if (pos - STEP > WIDTH) {
				break;
			}
			pos += STEP;
		}
		canvas.drawPath(path2, linePaint);
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

	public void setData(Data2 d) {
		this.data = d;
		if (data != null) {
			for (int i = 0; i < data.getLength(); i++) {
				if (data.getValues(0)[i] > maxValue) {
					maxValue = data.getValues(0)[i];
				}
			}
			Timber.v("maxValue = " + maxValue);
		}
		linePaint.setColor(Color.parseColor(data.getColors()[0]));
		invalidate();
	}

	public void setOnSeekListener(OnSeekListener onSeekListener) {
		this.onSeekListener = onSeekListener;
	}

	public interface OnSeekListener {
		void onSeek(int px);
		void onSeeking(int px);
	}
}
