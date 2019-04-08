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
import android.graphics.RectF;
import android.graphics.Region;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import com.dimowner.charttemplate.R;
import com.dimowner.charttemplate.util.AndroidUtils;

import timber.log.Timber;

public class ChartScrollOverlayView extends View {

	private static final int CURSOR_UNSELECTED = 2000;
	private static final int CURSOR_LEFT = 2001;
	private static final int CURSOR_CENTER = 2002;
	private static final int CURSOR_RIGHT = 2003;

	private final float SMALLEST_SELECTION_WIDTH;
	private final int PADD_DOUBLE;
	private final int LINE_HEIGHT;
	private final int LINE_WIDTH;
	private final float SELECTION;
	private final float SELECTION_HALF;

	Region.Op op = Region.Op.UNION;

	{
		float DENSITY = AndroidUtils.dpToPx(1);
		SMALLEST_SELECTION_WIDTH = 45*DENSITY;
		PADD_DOUBLE = (int) (16*DENSITY);
		LINE_HEIGHT = (int) (6*DENSITY);
		LINE_WIDTH = (int) (1.5*DENSITY);
		SELECTION = 11*DENSITY;
		SELECTION_HALF = SELECTION/2;
	}

	private float selectionWidth = (int)(1.3*SMALLEST_SELECTION_WIDTH);

	private int dataLength = 0;

	private Path path;
	private RectF rect;
	private float STEP = 10;

	private Paint overlayPaint;
	private Paint selectionPaint;

	private float scrollX = -1;

	private float WIDTH = 1;
	private float HEIGHT = 1;

	private float prevScroll = 0;
	private float prevWidth = 0;
	private int colorWhite;
	private int selectionColor;

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
		rect = new RectF();

		int overlayColor;
		Resources res = context.getResources();
		TypedValue typedValue = new TypedValue();
		Resources.Theme theme = context.getTheme();
		colorWhite = res.getColor(R.color.white);
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
//		selectionPaint.setAntiAlias(false);
//		selectionPaint.setDither(false);
		selectionPaint.setStyle(Paint.Style.FILL);
//		selectionPaint.setStrokeWidth(SELECTION);
		selectionPaint.setColor(selectionColor);

		overlayPaint = new Paint();
		overlayPaint.setAntiAlias(false);
		overlayPaint.setDither(false);
		overlayPaint.setStyle(Paint.Style.FILL);
		overlayPaint.setColor(overlayColor);

		setOnTouchListener(new OnTouchListener() {
//			TODO: When selection resized to smallest size on right side move selection to the left.

			float moveStartX = 0;
			float startSelectionWidth = 0;
			float offset = 0;
			int selectionState = 0;

			@Override
			public boolean onTouch(View v, MotionEvent motionEvent) {
				switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {
					case MotionEvent.ACTION_DOWN:
						moveStartX = motionEvent.getX();
						offset = moveStartX - scrollX;
						selectionState = checkSelectionState(moveStartX);
						startSelectionWidth = selectionWidth;
						break;
					case MotionEvent.ACTION_MOVE:
						switch (selectionState) {
							case CURSOR_CENTER:
								scrollX = moveStartX + motionEvent.getX() - moveStartX - offset;
								//Set scroll edges.
								if (scrollX + selectionWidth > WIDTH-SELECTION) {
									scrollX = WIDTH - selectionWidth-SELECTION;
								} else if (scrollX < SELECTION) {
									scrollX = SELECTION;
								}

//								if (onScrollListener != null && scrollX != prevScroll && selectionWidth != prevWidth) {
//									onScrollListener.onScroll(scrollX/STEP, selectionWidth/STEP);
//								}
								onScroll(scrollX-SELECTION, selectionWidth+2*SELECTION);
//								invalidate();
								break;
							case CURSOR_LEFT:
								float prevScroll = scrollX;
								float prevWidth = selectionWidth;
								scrollX = moveStartX + motionEvent.getX() - moveStartX - offset;
								selectionWidth += prevScroll-scrollX;
								//Set scroll edges.
								if (selectionWidth < SMALLEST_SELECTION_WIDTH) {
									selectionWidth = SMALLEST_SELECTION_WIDTH;
								}
								if (selectionWidth + PADD_DOUBLE > WIDTH-SELECTION) {
									selectionWidth = WIDTH - PADD_DOUBLE-SELECTION;
									scrollX = prevScroll;
								}
								if (scrollX + selectionWidth > WIDTH-SELECTION) {
									scrollX = WIDTH - selectionWidth-SELECTION;
								}
								if (scrollX < SELECTION) {
									scrollX = SELECTION;
									selectionWidth = prevWidth;
								}
								if (selectionWidth + scrollX > WIDTH) {
									selectionWidth = WIDTH - scrollX;
								}
//								if (onScrollListener != null) {
//									onScrollListener.onScroll(scrollX/STEP, selectionWidth/STEP);
//								}
								onScroll(scrollX-SELECTION, selectionWidth+2*SELECTION);
//								invalidate();
								break;
							case CURSOR_RIGHT:
								selectionWidth = (startSelectionWidth + motionEvent.getX() - moveStartX);
								//Set scroll edges.
								if (selectionWidth < SMALLEST_SELECTION_WIDTH) {
									selectionWidth = SMALLEST_SELECTION_WIDTH;
								}
								if (selectionWidth + PADD_DOUBLE > WIDTH-SELECTION) {
									selectionWidth = WIDTH - PADD_DOUBLE-SELECTION;
								}
								if (selectionWidth + scrollX > WIDTH-SELECTION) {
									selectionWidth = WIDTH - scrollX-SELECTION;
								}
//								if (onScrollListener != null) {
//									onScrollListener.onScroll(scrollX/STEP, selectionWidth/STEP);
//								}
								onScroll(scrollX-SELECTION, selectionWidth+2*SELECTION);
//								invalidate();
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

	private void onScroll(float scroll, float width) {
		if (onScrollListener != null && (scroll != prevScroll || width != prevWidth)) {
			Timber.v("onScroll Scroll = "+ scroll + " width = " + width + " x = " + scroll/STEP + " w = " + width/STEP);
			onScrollListener.onScroll(scroll/STEP, width/STEP);
			prevScroll = scroll;
			prevWidth = width;
			invalidate();
		}
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

		if (scrollX < 0) {
			scrollX = WIDTH - selectionWidth-SELECTION;
		}

		if (b && dataLength > 0) {
//			onScrollListener.onScroll(scrollX / STEP, selectionWidth / STEP);
//			prevScroll = scrollX;
//			prevWidth = selectionWidth;
			onScroll(scrollX-SELECTION, selectionWidth+3*SELECTION);
		}
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		selectionPaint.setColor(selectionColor);
		rect.left = 0;
		rect.top = 0;
		rect.bottom = HEIGHT;
		rect.right = scrollX - SELECTION;
		canvas.drawRect(rect, overlayPaint);
		rect.left = scrollX + selectionWidth + SELECTION;
		rect.top = 0;
		rect.bottom = HEIGHT;
		rect.right = WIDTH;
		canvas.drawRect(rect, overlayPaint);

		rect.left = scrollX - SELECTION;
		rect.top = 0;
		rect.bottom = HEIGHT;
		rect.right = scrollX;// + SELECTION_HALF;
		canvas.clipRect(rect);
		rect.right = scrollX + SELECTION_HALF;
		canvas.drawRoundRect(rect, SELECTION_HALF, SELECTION_HALF, selectionPaint);

		rect.left = scrollX + selectionWidth;
		rect.top = 0;
		rect.bottom = HEIGHT;
		rect.right = scrollX + selectionWidth + SELECTION;
		canvas.clipRect(rect,op);
		rect.left = scrollX + selectionWidth - SELECTION_HALF;
		canvas.drawRoundRect(rect, SELECTION_HALF, SELECTION_HALF, selectionPaint);

		selectionPaint.setColor(colorWhite);
		rect.left = scrollX + selectionWidth + SELECTION_HALF - LINE_WIDTH;
		rect.top = HEIGHT/2-LINE_HEIGHT;
		rect.bottom = HEIGHT/2+LINE_HEIGHT;
		rect.right = scrollX + selectionWidth + SELECTION_HALF + LINE_WIDTH;
		canvas.drawRoundRect(rect, LINE_WIDTH, LINE_WIDTH, selectionPaint);

		rect.left = scrollX - SELECTION_HALF - LINE_WIDTH;
		rect.top = HEIGHT/2-LINE_HEIGHT;
		rect.bottom = HEIGHT/2+LINE_HEIGHT;
		rect.right = scrollX - SELECTION_HALF + LINE_WIDTH;
		canvas.drawRoundRect(rect, LINE_WIDTH, LINE_WIDTH, selectionPaint);
	}

	public void setData(int length) {
		if (length > 0) {
			dataLength = length;

			if (WIDTH > 1) {
				STEP = (WIDTH / dataLength);
				if (onScrollListener != null) {
					onScrollListener.onScroll((scrollX-SELECTION)/ STEP, (selectionWidth+2*SELECTION) / STEP);
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
