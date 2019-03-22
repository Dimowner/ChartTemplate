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
import com.dimowner.charttemplate.util.AndroidUtils;

public class ChartScrollOverlayView extends View {

	private static final int CURSOR_UNSELECTED = 2000;
	private static final int CURSOR_LEFT = 2001;
	private static final int CURSOR_CENTER = 2002;
	private static final int CURSOR_RIGHT = 2003;

	private final float SMALLEST_SELECTION_WIDTH;
	private final int PADD_DOUBLE;
	private final float SELECTION;
	private final float SELECTION_HALF;

	{
		float DENSITY = AndroidUtils.dpToPx(1);
		SMALLEST_SELECTION_WIDTH = 60* DENSITY;
		PADD_DOUBLE = (int) (32* DENSITY);
		SELECTION = 5* DENSITY;
		SELECTION_HALF = SELECTION/2;
	}

	private float selectionWidth = (int)(1.3*SMALLEST_SELECTION_WIDTH);

	private int dataLength = 0;

	private Path path;
	private float STEP = 10;

	private Paint overlayPaint;
	private Paint selectionPaint;

	private float scrollX = 0;

	private float WIDTH = 1;
	private float HEIGHT = 1;

	private OnScrollListener onScrollListener;

	public ChartScrollOverlayView(Context context) {
		super(context);
		init(context);
	}

	public ChartScrollOverlayView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public ChartScrollOverlayView(Context context, AttributeSet attrs, int defStyleAttr) {
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
								scrollX = moveStartX + motionEvent.getX() - moveStartX - offset;
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
								float prevScroll = scrollX;
								scrollX = moveStartX + motionEvent.getX() - moveStartX - offset;
								selectionWidth += prevScroll-scrollX;
								//Set scroll edges.
								if (selectionWidth < SMALLEST_SELECTION_WIDTH) {
									selectionWidth = SMALLEST_SELECTION_WIDTH;
								}
								if (scrollX + selectionWidth > WIDTH) {
									scrollX = WIDTH - selectionWidth;
								}
								if (scrollX < PADD_DOUBLE) {
									scrollX = PADD_DOUBLE;
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
								selectionWidth = (prevSelectionWidth + motionEvent.getX() - moveStartX);
								//Set scroll edges.
								if (selectionWidth < SMALLEST_SELECTION_WIDTH) {
									selectionWidth = SMALLEST_SELECTION_WIDTH;
								}
								if (selectionWidth + scrollX + PADD_DOUBLE > WIDTH) {
									selectionWidth = WIDTH - PADD_DOUBLE - scrollX;
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
		boolean b = WIDTH != getWidth();
		WIDTH = getWidth();
		HEIGHT = getHeight();

		if (dataLength > 0) {
			STEP = (WIDTH/dataLength);
		}

		if (scrollX <= 0) {
			scrollX = WIDTH - selectionWidth;
		}

		if (b && dataLength > 0 && onScrollListener != null) {
			onScrollListener.onScroll(scrollX / STEP, selectionWidth / STEP);
		}
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		//Draw overlay
		path.rewind();
		path.moveTo(0, 0);
		path.lineTo(0, HEIGHT);
		path.lineTo(scrollX - SELECTION_HALF, HEIGHT);
		path.lineTo(scrollX - SELECTION_HALF, 0);
		path.close();
		canvas.drawPath(path, overlayPaint);

		path.rewind();
		path.moveTo(scrollX + selectionWidth + SELECTION_HALF, 0);
		path.lineTo(scrollX + selectionWidth + SELECTION_HALF, HEIGHT);
		path.lineTo(WIDTH, HEIGHT);
		path.lineTo(WIDTH, 0);
		path.close();
		canvas.drawPath(path, overlayPaint);

		//Draw selection borders
		path.rewind();
		path.moveTo(scrollX, 0);
		path.lineTo(scrollX, HEIGHT);
		path.moveTo(scrollX + selectionWidth, HEIGHT);
		path.lineTo(scrollX + selectionWidth, 0);
		selectionPaint.setStrokeWidth(SELECTION);
		canvas.drawPath(path, selectionPaint);

		path.rewind();
		path.moveTo(scrollX + SELECTION_HALF, 0);
		path.lineTo(scrollX + selectionWidth - SELECTION_HALF, 0);
		path.moveTo(scrollX + SELECTION_HALF, HEIGHT);
		path.lineTo(scrollX + selectionWidth - SELECTION_HALF, HEIGHT);
		selectionPaint.setStrokeWidth(SELECTION_HALF);
		canvas.drawPath(path, selectionPaint);

	}

	public void setData(int length) {
		if (length > 0) {
			dataLength = length;

			if (WIDTH > 1) {
				STEP = (WIDTH / dataLength);
				if (onScrollListener != null) {
					onScrollListener.onScroll(scrollX / STEP, selectionWidth / STEP);
				}
			}
			invalidate();
		}
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

		ss.dataLength = dataLength;
		ss.selectionWidth = selectionWidth;
		ss.scrollX = scrollX;
		return ss;
	}

	@Override
	public void onRestoreInstanceState(Parcelable state) {
		SavedState ss = (SavedState) state;
		super.onRestoreInstanceState(ss.getSuperState());

		dataLength = ss.dataLength;
		selectionWidth = ss.selectionWidth;
		scrollX = ss.scrollX;
	}

	static class SavedState extends View.BaseSavedState {
		SavedState(Parcelable superState) {
			super(superState);
		}

		private SavedState(Parcel in) {
			super(in);
			dataLength = in.readInt();
			float[] floats = new float[2];
			in.readFloatArray(floats);
			selectionWidth = floats[0];
			scrollX = floats[1];
		}

		@Override
		public void writeToParcel(Parcel out, int flags) {
			super.writeToParcel(out, flags);
			out.writeInt(dataLength);
			out.writeFloatArray(new float[] {selectionWidth, scrollX});
		}

		private int dataLength;
		private float selectionWidth;
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
