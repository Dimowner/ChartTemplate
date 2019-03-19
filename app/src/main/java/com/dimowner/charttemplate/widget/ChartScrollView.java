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
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import com.dimowner.charttemplate.R;
import com.dimowner.charttemplate.model.ChartData;
import com.dimowner.charttemplate.util.AndroidUtils;

public class ChartScrollView extends View {

	private final static float DENSITY = AndroidUtils.dpToPx(1);
	private final static int CURSOR_UNSELECTED = 2000;
	private final static int CURSOR_LEFT = 2001;
	private final static int CURSOR_CENTER = 2002;
	private final static int CURSOR_RIGHT = 2003;

	private final static float SMALLEST_SELECTION_WIDTH = 80*DENSITY;
	private final static int PADDING_SMALL = (int) (8*DENSITY);
	private final static float SELECTION = 5*DENSITY;
	private final static float SELECTION_HALF = SELECTION/2;

	private float selectionWidth = SMALLEST_SELECTION_WIDTH;

	private ChartData data;
	private boolean[] linesVisibility;
	private Path path;
	private float STEP = 1;

	private Paint linePaint;
	private Paint overlayPaint;
	private Paint selectionPaint;

	private float scrollX = -1;

	private float WIDTH = 0;
	private float HEIGHT = 0;
	private int maxValueY = 0;
	private float valueScaleY = 0;

	private OnScrollListener onScrollListener;

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

		setOnTouchListener(new OnTouchListener() {
//			TODO: When selection resized to smallest size on right side move selection to the left.

			float moveStartX = 0;
			float prevSelectionWidth = 0;
			float offset = 0;
			int selectionState = 0;

			@Override
			public boolean onTouch(View v, MotionEvent motionEvent) {
				switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {
					case MotionEvent.ACTION_DOWN:
						moveStartX = motionEvent.getX();
						offset = moveStartX - scrollX;
						selectionState = checkSelectionState(moveStartX);
						prevSelectionWidth = selectionWidth;
						break;
					case MotionEvent.ACTION_MOVE:
						switch (selectionState) {
							case CURSOR_CENTER:
								float shift = (motionEvent.getX()) - moveStartX;
								scrollX = moveStartX + shift - offset;
								//Set scroll edges.
								if (scrollX + selectionWidth > WIDTH) {
									scrollX = WIDTH - selectionWidth;
								} else if (scrollX < 0) {
									scrollX = 0;
								}

								if (onScrollListener != null) {
									onScrollListener.onScroll(scrollX/STEP, selectionWidth/STEP);
								}
								invalidate();
								break;
							case CURSOR_LEFT:
								float shift2 = (motionEvent.getX()) - moveStartX;
								float prevScroll = scrollX;
								scrollX = moveStartX + shift2 - offset;
								selectionWidth += prevScroll-scrollX;
								//Set scroll edges.
								if (selectionWidth < SMALLEST_SELECTION_WIDTH) {
									selectionWidth = SMALLEST_SELECTION_WIDTH;
								}
								if (scrollX + selectionWidth > WIDTH) {
									scrollX = WIDTH - selectionWidth;
								}
								if (scrollX < PADDING_SMALL) {
									scrollX = PADDING_SMALL;
								}
								if (selectionWidth + scrollX > WIDTH) {
									selectionWidth = WIDTH - scrollX;
								}

								if (onScrollListener != null) {
									onScrollListener.onScroll(scrollX/STEP, selectionWidth/STEP);
								}
								invalidate();
								break;
							case CURSOR_RIGHT:
								float shift3 = (motionEvent.getX()) - moveStartX;
								selectionWidth = (prevSelectionWidth + shift3);
								//Set scroll edges.
								if (selectionWidth < SMALLEST_SELECTION_WIDTH) {
									selectionWidth = SMALLEST_SELECTION_WIDTH;
								}
								if (selectionWidth + scrollX + PADDING_SMALL > WIDTH) {
									selectionWidth = WIDTH - PADDING_SMALL -scrollX;
								}
								if (onScrollListener != null) {
									onScrollListener.onScroll(scrollX/STEP, selectionWidth/STEP);
								}
								invalidate();
								break;
							case CURSOR_UNSELECTED:
							default:
								//Do nothing
								break;
						}
						break;
					case MotionEvent.ACTION_UP:
						performClick();
						break;
				}
				return true;
			}
		});
	}

	private int checkSelectionState(float x) {
		if (x > scrollX && x < scrollX + selectionWidth) {
			return CURSOR_CENTER;
		} else if (x < scrollX + selectionWidth) {
			return CURSOR_LEFT;
		} else if (x > scrollX) {
			return CURSOR_RIGHT;
		}
		return CURSOR_UNSELECTED;
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		WIDTH = getWidth();
		HEIGHT = getHeight();
		valueScaleY = HEIGHT/(maxValueY + SELECTION);

		if (data != null) {
			STEP = (WIDTH/data.getLength());
		}

		if (scrollX < 0) {
			scrollX = WIDTH - selectionWidth;
		}
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (data != null) {
			for (int i = 0; i < data.getNames().length; i++) {
				if (linesVisibility[i]) {
					drawChart(canvas, data.getValues(i), data.getColorsInts()[i]);
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
		path.moveTo(scrollX + selectionWidth + SELECTION_HALF, 0);
		path.lineTo(scrollX + selectionWidth + SELECTION_HALF, HEIGHT);
		path.lineTo(WIDTH, HEIGHT);
		path.lineTo(WIDTH, 0);
		path.close();
		canvas.drawPath(path, overlayPaint);

		//Draw selection borders
		path.reset();
		path.moveTo(scrollX, 0);
		path.lineTo(scrollX, HEIGHT);
		path.moveTo(scrollX + selectionWidth, HEIGHT);
		path.lineTo(scrollX + selectionWidth, 0);
		selectionPaint.setStrokeWidth(SELECTION);
		canvas.drawPath(path, selectionPaint);

		path.reset();
		path.moveTo(scrollX + SELECTION_HALF, 0);
		path.lineTo(scrollX + selectionWidth - SELECTION_HALF, 0);
		path.moveTo(scrollX + SELECTION_HALF, HEIGHT);
		path.lineTo(scrollX + selectionWidth - SELECTION_HALF, HEIGHT);
		selectionPaint.setStrokeWidth(SELECTION_HALF);
		canvas.drawPath(path, selectionPaint);

	}

	private void drawChart(Canvas canvas, int[] values, int color) {
		linePaint.setColor(color);
		path.reset();
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
		canvas.drawPath(path, linePaint);
	}

	public void hideLine(String name) {
		int pos = findLinePosition(name);
		if (pos >= 0) {
			linesVisibility[pos] = false;
		}
		calculateMaxValue();
		invalidate();
	}

	public void showLine(String name) {
		int pos = findLinePosition(name);
		if (pos >= 0) {
			linesVisibility[pos] = true;
		}
		calculateMaxValue();
		invalidate();
	}

	public void setData(ChartData d) {
		this.data = d;
		if (data != null) {
//			maxValueY = 0;
//			for (int i = 0; i < data.getLength(); i++) {
//				if (data.getValues(0)[i] > maxValueY) {
//					maxValueY = data.getValues(0)[i];
//				}
//			}
			//Init lines visibility state, all visible by default.
			linesVisibility = new boolean[data.getLinesCount()];
			for (int i = 0; i < linesVisibility.length; i++) {
				linesVisibility[i] = true;
			}
			calculateMaxValue();
//			valueScaleY = HEIGHT/(maxValueY + SELECTION);
			if (WIDTH > 0 && data.getLength() > 0) {
				STEP = (WIDTH / data.getLength());
			}
			if (onScrollListener != null) {
				onScrollListener.onScroll(scrollX/STEP, selectionWidth/STEP);
			}
		}
		invalidate();
	}

	private void calculateMaxValue() {
		maxValueY = 0;
//		for (int i = (int)(scrollPos/STEP); i < (int)((scrollPos+WIDTH)/STEP); i++) {
		for (int j = 0; j < data.getLinesCount(); j++) {
			if (linesVisibility[j]) {
				for (int i = 0; i < data.getLength(); i++) {
					if (data.getValues(j)[i] > maxValueY) {
						maxValueY = data.getValues(j)[i];
					}
				}
			}
		}
		valueScaleY = (HEIGHT-SELECTION)/maxValueY;
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

	public interface OnScrollListener {
		void onScroll(float x, float size);
	}

	@Override
	public Parcelable onSaveInstanceState() {
		Parcelable superState = super.onSaveInstanceState();
		SavedState ss = new SavedState(superState);

		ss.maxValueY = maxValueY;
		ss.linesVisibility = linesVisibility;
		ss.selectionWidth = selectionWidth;
		ss.scrollX = scrollX;
		ss.data = data;
		return ss;
	}

	@Override
	public void onRestoreInstanceState(Parcelable state) {
		SavedState ss = (SavedState) state;
		super.onRestoreInstanceState(ss.getSuperState());
		maxValueY = ss.maxValueY;
		linesVisibility = ss.linesVisibility;
		selectionWidth = ss.selectionWidth;
		scrollX = ss.scrollX;
		data = ss.data;
	}

	static class SavedState extends View.BaseSavedState {
		SavedState(Parcelable superState) {
			super(superState);
		}

		private SavedState(Parcel in) {
			super(in);
			maxValueY = in.readInt();
			in.readBooleanArray(linesVisibility);
			float[] floats = new float[2];
			in.readFloatArray(floats);
			selectionWidth = floats[0];
			scrollX = floats[1];
			data = in.readParcelable(ChartData.class.getClassLoader());
		}

		@Override
		public void writeToParcel(Parcel out, int flags) {
			super.writeToParcel(out, flags);
			out.writeInt(maxValueY);
			out.writeBooleanArray(linesVisibility);
			out.writeFloatArray(new float[] {selectionWidth, scrollX});
			out.writeParcelable(data, Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
		}

		private ChartData data;
		private int maxValueY = 0;
		private boolean[] linesVisibility;
		private float selectionWidth = SMALLEST_SELECTION_WIDTH;
		private float scrollX;

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
