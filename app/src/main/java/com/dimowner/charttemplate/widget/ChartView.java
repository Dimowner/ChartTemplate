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
import android.view.animation.LinearInterpolator;

import com.dimowner.charttemplate.R;
import com.dimowner.charttemplate.model.ChartData;
import com.dimowner.charttemplate.util.AndroidUtils;
import java.text.DecimalFormat;

public class ChartView extends View {

	private final float DENSITY;
	private final int PADD_NORMAL;
//	private final int PADD_SMALL;
	private final int PADD_TINY;
	private final int TEXT_SPACE;
	private final int BASE_LINE_Y;
	private final int MIN_GRID_STEP;
	private final int MAX_GRID_STEP;
	private final int DATE_RANGE_PADD;
	private static final int GRID_LINES_COUNT = 6;
	private static final int ANIMATION_DURATION = 220; //mills

	{
		DENSITY = AndroidUtils.dpToPx(1);
		PADD_NORMAL = (int) (16*DENSITY);
//		PADD_SMALL = (int) (8*DENSITY);
		PADD_TINY = (int) (4*DENSITY);
		TEXT_SPACE = (int) (56*DENSITY);
		BASE_LINE_Y = (int) (32*DENSITY);
		MIN_GRID_STEP = (int) (44*DENSITY);
		MAX_GRID_STEP = (int) (90*DENSITY);
		DATE_RANGE_PADD = (int) (21*DENSITY);
	}

	private float STEP = 10*DENSITY;
	private float H1 = 0;
	private float H2 = 0;
	private float H3 = 0;
	private float HEIGHT_PADDS = 2*BASE_LINE_Y+PADD_NORMAL;

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
	private Paint[] linePaints;

	private ValueAnimator alphaAnimator;
	private ValueAnimator heightAnimator;
	private DecelerateInterpolator decelerateInterpolator = new DecelerateInterpolator();
	private LinearInterpolator linearInterpolator = new LinearInterpolator();
	private AccelerateInterpolator accelerateInterpolator= new AccelerateInterpolator();

	private float scrollPos;
	private float scrollStartIndex;

	private ChartSelectionDrawer selectionDrawer;

	private float WIDTH = 1;
	private float HEIGHT = 1;

	private float valueScale = 1;
	private float maxValueVisible = 0;
	private float maxValueCalculated = 0;

	//Y scaled line values;
	private float yScale = 1;
	private int yIndex = 0;
	private boolean isYscaled = false;

	private int[] maxValuesLine;
	private float gridScale = 1;
	private int gridCount = GRID_LINES_COUNT;
	private float gridStep = 1;
	private float gridValueStep = 1;
	private boolean skipNextInvalidation = false;
	private String dateRange;
	private float dateRangeHeight;
	private Rect rect;
	private int gridTextColor;
	private final String minus = " - ";

	private boolean isAnimating = false;
	private float scaleKoef = 1;
	private int amnimItemIndex = -1;
	private float[] sumVals;

	private OnMoveEventsListener onMoveEventsListener;

	ValueAnimator.AnimatorUpdateListener heightValueAnimator = new ValueAnimator.AnimatorUpdateListener() {
		@Override
		public void onAnimationUpdate(ValueAnimator animation) {
//			float val = (float) animation.getAnimatedValue();
			maxValueVisible = maxValueCalculated - (float)animation.getAnimatedValue();
			valueScale = (HEIGHT-HEIGHT_PADDS)/ maxValueVisible;
			if (skipNextInvalidation) {
				skipNextInvalidation = false;
			} else {
				invalidate();
			}
			if (gridStep < MIN_GRID_STEP) {
				gridValueStep *=2;
			}
			if (gridStep > MAX_GRID_STEP) {
				gridValueStep /=2;
			}
			gridStep = gridValueStep* valueScale;
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
			scaleKoef = Math.abs(val/255);
			linePaints[index].setAlpha((int) val);
			if (val == end2) {
				linesVisibility[index] = show;
				isAnimating = false;
				amnimItemIndex = -1;
			}
			skipNextInvalidation = true;
			if (data.isPercentage()) {
				calculateSumsLine();
			}
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
		scrollStartIndex = 0;
		rect = new Rect();
//		stackedData = new ArrayList<>();

		int gridColor;
//		int gridBaseLineColor;
//		int gridTextColor;
		int panelTextColor;
		int panelColor;
//		int scrubblerColor;
		int shadowColor;
		int tittleColor;
		int viewBackground;

		Resources res = context.getResources();
		TypedValue typedValue = new TypedValue();
		Resources.Theme theme = context.getTheme();
		if (theme.resolveAttribute(R.attr.gridColor, typedValue, true)) {
			gridColor = typedValue.data;
		} else {
			gridColor = res.getColor(R.color.grid_color2);
		}
//		if (theme.resolveAttribute(R.attr.gridBaseLineColor, typedValue, true)) {
//			gridBaseLineColor = typedValue.data;
//		} else {
//			gridBaseLineColor = res.getColor(R.color.grid_base_line);
//		}
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
//		if (theme.resolveAttribute(R.attr.scrubblerColor, typedValue, true)) {
//			scrubblerColor = typedValue.data;
//		} else {
//			scrubblerColor = res.getColor(R.color.scrubbler_color);
//		}
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
		if (theme.resolveAttribute(R.attr.viewBackground, typedValue, true)) {
			viewBackground = typedValue.data;
		} else {
			viewBackground = res.getColor(R.color.view_background);
		}

		selectionDrawer = new ChartSelectionDrawer(getContext(), panelTextColor,
					panelColor, gridColor, shadowColor, viewBackground);

		gridPaint = new Paint();
		gridPaint.setAntiAlias(false);
		gridPaint.setDither(false);
		gridPaint.setStyle(Paint.Style.STROKE);
		gridPaint.setStrokeCap(Paint.Cap.SQUARE);
		gridPaint.setColor(gridColor);
		gridPaint.setStrokeWidth(DENSITY);

//		baselinePaint = new Paint();
//		baselinePaint.setAntiAlias(false);
//		baselinePaint.setDither(false);
//		baselinePaint.setStyle(Paint.Style.STROKE);
//		baselinePaint.setColor(gridBaseLineColor);
//		baselinePaint.setStrokeWidth(DENSITY);

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
		timelineTextPaint.setTextAlign(Paint.Align.CENTER);
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

	private Paint createLinePaint(int color, boolean isBars) {
		Paint lp = new Paint(1);
//		lp.setAntiAlias(false);
//		lp.setDither(false);
		lp.setStyle(Paint.Style.STROKE);
//		lp.setStrokeCap(Paint.Cap.ROUND);
		lp.setStrokeWidth(1.6f*DENSITY);
		lp.setStrokeJoin(Paint.Join.ROUND);
//		lp.setStrokeCap(Paint.Cap.ROUND);
		lp.setColor(color);
		if (isBars) {
			lp.setStrokeCap(Paint.Cap.BUTT);
		}
		return lp;
	}

	private void alphaAnimator(float start, final float end, final int index, final boolean show) {
		this.show = show;
		this.index = index;
		this.end2 = end;
		if (alphaAnimator != null && alphaAnimator.isStarted()) {
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

	private void heightAnimator(final float diff, boolean isLinear) {
		if (heightAnimator != null && (heightAnimator.isStarted())) {
			heightAnimator.cancel();
		}
		heightAnimator = ValueAnimator.ofFloat(diff, 0);
		if (isLinear) {
			heightAnimator.setInterpolator(linearInterpolator);
			heightAnimator.setDuration(300);
		} else {
			heightAnimator.setInterpolator(decelerateInterpolator);
			heightAnimator.setDuration(ANIMATION_DURATION);
		}
		heightAnimator.addUpdateListener(heightValueAnimator);
//		heightAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
//			@Override
//			public void onAnimationUpdate(ValueAnimator animation) {
//				float val = (float) animation.getAnimatedValue();
//				maxValueVisible = maxValueCalculated -(int)val;
//				valueScale = (HEIGHT-2*BASE_LINE_Y-PADD_NORMAL)/ maxValueVisible;
//				if (skipNextInvalidation) {
//					skipNextInvalidation = false;
//				} else {
//					invalidate();
//				}
//				if (val == 0) {
////					Timber.v("Animation END");
////					gridScale = valueScale;
////					gridValueStep = maxValueCalculated/GRID_LINES_COUNT;
//				}
//				gridStep = gridValueStep*valueScale;
//			}
//		});
		heightAnimator.start();
	}

	private void updateGrid() {
		gridCount = (int)((HEIGHT-BASE_LINE_Y-PADD_TINY)/gridStep);
	}

	public void scrollPos(float x, float size) {
		if (x >= 0) {
			STEP = WIDTH / size;
			scrollPos = (x * STEP);
			scrollStartIndex = x;
			int idx = (int) Math.ceil(x + size) - 1;
			if (idx < data.getLength()) {
				dateRange = data.getTimesLong()[(int) Math.floor(x)] + minus + data.getTimesLong()[idx];
				dateRangePaint.getTextBounds(dateRange, 0, dateRange.length(), rect);
			}
			if (dateRangeHeight < rect.height()) {
				dateRangeHeight = rect.height();
			}
			if (!data.isPercentage()) {
				calculateMaxValue2(false, true);
				updateGrid();
				skipNextInvalidation = true;
			}
			invalidate();
		}
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		WIDTH = getWidth();
		HEIGHT = getHeight();
		H1 = HEIGHT - BASE_LINE_Y;
		H2 = (HEIGHT-HEIGHT_PADDS);
		H3 = H2/100;
//		gridStep = (HEIGHT/GRID_LINES_COUNT);
		if (maxValueVisible > 0) {
			valueScale = (HEIGHT - 2*BASE_LINE_Y-PADD_NORMAL) / maxValueVisible;
			gridScale = valueScale;
		}
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (data != null) {
			//Draw charts
			textPaint.setTextAlign(Paint.Align.CENTER);
			for (int i = 0; i < data.getNames().length; i++) {
				if (linesVisibility[i]) {
					if (data.getType(i) == ChartData.TYPE_LINE) {
						drawChart(canvas, data.getValues(i), i);
					} else if (data.getType(i) == ChartData.TYPE_BAR) {
						drawBars(canvas, data.getValues(i), i);
					} else if (data.getType(i) == ChartData.TYPE_AREA) {
						drawBars(canvas, data.getValues(i), i);
					} else {
						drawChart(canvas, data.getValues(i), i);
					}
				}
			}
			textPaint.setTextAlign(Paint.Align.LEFT);
			if (data.isPercentage()) {
				drawPercentageGrid(canvas);
			} else {
				drawGrid(canvas);
			}

			if (data.getLinesCount() > 0) {
				drawTimeline(canvas);
			}
			canvas.drawText(dateRange, WIDTH-2, dateRangeHeight+DATE_RANGE_PADD, dateRangePaint);
			//Draw selection panel with scrubbler
			selectionDrawer.draw(canvas, data, linesVisibility, HEIGHT, linePaints, valueScale);
		}
	}

	private void drawGrid(Canvas canvas) {
		if (isYscaled) {
			textPaint.setColor(data.getColorsInts()[yIndex == 0 ? 1: 0]);
		} else {
			textPaint.setColor(gridTextColor);
		}
		for (int i = 0; i < gridCount; i++) {
			canvas.drawLine(0, H1 - gridStep * i, WIDTH, H1 - gridStep * i, gridPaint);
			canvas.drawText(formatValue(gridValueStep * i), 0, H1 - gridStep * i - PADD_TINY, textPaint);
		}
		if (isYscaled) {
			textPaint.setColor(data.getColorsInts()[yIndex]);
			textPaint.setTextAlign(Paint.Align.RIGHT);
			for (int i = 0; i < gridCount; i++) {
				canvas.drawText(formatValue((gridValueStep * i)/yScale),
						WIDTH, H1 - gridStep * i - PADD_TINY, textPaint);
			}
		}
	}

	private void drawPercentageGrid(Canvas canvas) {
		gridStep = H2/5;
		gridValueStep = 20;//%
		for (int i = 0; i < 5; i++) {
			canvas.drawLine(0, H1 - gridStep * i, WIDTH, H1 - gridStep * i, gridPaint);
			canvas.drawText(String.valueOf(gridValueStep * i), 0, H1 - gridStep * i - PADD_TINY, textPaint);
		}
	}

	private void drawChart(Canvas canvas, int[] values, int index) {
		float pos = -scrollPos;
		int skip = (int) scrollStartIndex -(int)(PADD_NORMAL/STEP);
		if (skip < 0) {skip = 0;}
		pos +=skip*STEP;
		int k = skip;
		for (int i = skip; i < values.length; i++) {
			if (k < chartArray.length) {
				chartArray[k] = pos; //x1
				chartArray[k + 1] = H1 - values[i] * valueScale; //y1
				if (i + 1 < values.length) {
					chartArray[k + 2] = pos + STEP; //x2
					chartArray[k + 3] = H1 - values[i + 1] * valueScale; //y2
				} else {
					chartArray[k + 2] = pos; //x2
					chartArray[k + 3] = H1 - values[i] * valueScale; //y2
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
		canvas.drawLines(chartArray, skip, k-skip, linePaints[index]);
//		canvas.drawVertices();
	}

	private void drawBars(Canvas canvas, int[] values, int index) {
		float pos = -scrollPos;
		int skip = (int) scrollStartIndex -(int)(PADD_NORMAL/STEP);
		if (skip < 0) {skip = 0;}
		pos +=skip*STEP;
		int k = skip;
		linePaints[index].setStrokeWidth(STEP+1);
		if (data.isStacked()) {
			int j;
			int sum=0;
			for (int i = skip; i < values.length; i++) {
				if (k < chartArray.length) {
					for (j = 0; j <= index; j++) {
						if (linesCalculated[j] && j != amnimItemIndex) { //
							sum += data.getVal(j, i);
						}
					}
					if (isAnimating && amnimItemIndex <= index) {
						sum += scaleKoef*data.getVal(amnimItemIndex, i);
					}
					if (data.isPercentage()) {
						chartArray[k] = pos; //x1
						if (index == amnimItemIndex) {
							chartArray[k + 1] = H1 - H3*(sum - values[i] * scaleKoef)/((float)sumVals[i]);
						} else {
							chartArray[k + 1] = H1 - H3*(sum - values[i])/((float)sumVals[i]);
						}
						chartArray[k + 2] = pos; //x2
						chartArray[k + 3] = H1 - H3*sum/((float)sumVals[i]); //y2
					} else {
						chartArray[k] = pos; //x1
	//					chartArray[k + 1] = H1 - data.getValues(index-1)[i] * valueScale; //y1
						if (index == amnimItemIndex) {
							chartArray[k + 1] = H1 - (sum - values[i] * scaleKoef) * valueScale; //y1
						} else {
							chartArray[k + 1] = H1 - (sum - values[i]) * valueScale; //y1
						}
						chartArray[k + 2] = pos; //x2
	//					chartArray[k + 3] = H1 - values[i] * valueScale; //y2
						chartArray[k + 3] = H1 - sum * valueScale; //y2
					}
					k += 4;
					if (pos - STEP > WIDTH + PADD_NORMAL) {
						break;
					}
					sum = 0;
				}
				pos += STEP;
			}
		} else {
			for (int i = skip; i < values.length; i++) {
				if (k < chartArray.length) {
					chartArray[k] = pos; //x1
					chartArray[k + 1] = H1; //y1
					chartArray[k + 2] = pos; //x2
					chartArray[k + 3] = H1 - values[i] * valueScale; //y2
					k += 4;
					if (pos - STEP > WIDTH + PADD_NORMAL) {
						break;
					}
				}
				pos += STEP;
			}
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
			if (i == 0) {
				timelineTextPaint.setTextAlign(Paint.Align.LEFT);
			} else {
				timelineTextPaint.setTextAlign(Paint.Align.CENTER);
			}
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
		isAnimating = true;
		int pos = findLinePosition(name);
		if (pos >= 0) {
			amnimItemIndex = pos;
			linesCalculated[pos] = false;
			alphaAnimator(linePaints[pos].getAlpha(), 0, pos, false);
		}
		calculateMaxValuesLine();
		calculateMaxValue2(true, false);
//		gridCount = (int)(HEIGHT/gridStep);
		updateGrid();
	}

	public void showLine(String name) {
		isAnimating = true;
		int pos = findLinePosition(name);
		amnimItemIndex = pos;
		if (pos >= 0) {
			linesVisibility[pos] = true;
			linesCalculated[pos] = true;
			alphaAnimator(linePaints[pos].getAlpha(), 255, pos, true);
		}
		calculateMaxValuesLine();
		calculateMaxValue2(true, false);
//		gridCount = (int)(HEIGHT/gridStep);
		updateGrid();
	}

	public void setData(ChartData d) {
		this.data = d;
		if (data != null) {
			isYscaled = data.isYscaled();
			//Init lines visibility state, all visible by default.
			linesVisibility = new boolean[data.getLinesCount()];
			linesCalculated = new boolean[data.getLinesCount()];
			selectionDrawer.setLinesCount(data.getLinesCount());
			linePaints = new Paint[data.getLinesCount()];
			for (int i = 0; i < data.getLinesCount(); i++) {
				linesVisibility[i] = true;
				linesCalculated[i] = true;
				linePaints[i] = createLinePaint(data.getColorsInts()[i], data.getType(i) == ChartData.TYPE_BAR);
			}
//			if (data.isStacked()) {
//				updateStackedData();
//			}
			calculateMaxValuesLine();
			calculateSumsLine();
			calculateMaxValue2(true, false);
			if (isYscaled) {
				updateYline();
			}
			gridValueStep = maxValueCalculated/GRID_LINES_COUNT;
//			if (HEIGHT > 1) {
				chartArray = new float[data.getLength() * 4];
//				for (int i = 0; i < data.getLength(); i += 2) {
//					chartArray[i] = i;
//					chartArray[i + 1] = HEIGHT - BASE_LINE_Y - data.getValues(0)[(i / 2 + 1)] * valueScale;
//				}
//			}
		}
		selectionDrawer.setSelectionX(-1);
		invalidate();
	}

	//TODO: optimize this method. There max val should not use all values to calculate max.
	private void calculateMaxValue2(boolean adjust, boolean linearAnim) {
		float prev = maxValueCalculated;
		maxValueCalculated = 0;
		int end = (int) ((scrollPos + WIDTH) / STEP);
		int j;
		int sum=0;
		for (int i = (int) (scrollPos / STEP); i < end; i++) {
			if (!data.isStacked()) {
				if (i >= 0 && i < maxValuesLine.length && maxValuesLine[i] > maxValueCalculated) {
					maxValueCalculated = maxValuesLine[i];
				}
			} else {
				if (i >= 0 && i < maxValuesLine.length) {
					for (j = 0; j < data.getLinesCount(); j++) {
						if (linesCalculated[j]) {
							sum += data.getVal(j, i);
						}
					}
					if (sum > maxValueCalculated) {
						maxValueCalculated = sum;
					}
					sum =0;
				}
			}
		}
//
//		if (adjust) {
//			maxValueCalculated = (int) adjustToGrid((float) maxValueCalculated, (int)GRID_LINES_COUNT);
//		}
		if (prev != maxValueCalculated) {
			heightAnimator(maxValueCalculated - maxValueVisible, linearAnim);
		}
	}

	private void calculateSumsLine() {
		sumVals = new float[data.getLength()];
		int sum = 0;
		for (int i = 0; i < data.getLength(); i++) {
			for (int j = 0; j < data.getLinesCount(); j++) {
				if (linesVisibility[j]) {
					if (isAnimating && j == amnimItemIndex) {
						sum += data.getValues(j)[i]*scaleKoef;
					} else {
						sum += data.getValues(j)[i];
					}
				}
			}
			sumVals[i] = (float)sum/100;
			sum = 0;
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

	/**
	 * Rescalse samller line to show it full size on Y axis.
	 */
	private void updateYline() {
		int globalMax = 0;
		int yMax = 0;
		int lineMax;
		int prevLineMax = Integer.MAX_VALUE;
		for (int i = 0; i < data.getLinesCount(); i++) {
			lineMax = 0;
			for (int j = 0; j < data.getLength(); j++) {
				if (j < data.getLength() && data.getValues(i)[j] > lineMax) {
					lineMax = data.getValues(i)[j];
				}
			}
			if (lineMax > globalMax) { globalMax = lineMax; }
			if (lineMax < prevLineMax) {
				yIndex = i;
				yMax = lineMax;
				prevLineMax = lineMax;
			}
		}
		yScale = (float)globalMax/(float)yMax;
		int[] v = data.getValues(yIndex);
		for (int j = 0; j < v.length; j++) {
//			v[j] = (int)(v[j]* yScale);
			data.setData((int)(v[j]* yScale), yIndex, j);
		}
	}

//	private void updateStackedData() {
//		int[][] vals = data.getColumns();
//		TreeMap<Long, Integer> order = new TreeMap<>();
//		long[] sums = new long[vals.length];
//		for (int i = 0; i < vals[0].length; i++) {
//			for (int j = 0; j < vals.length; j++) {
//				sums[j] += vals[j][i];
//			}
//		}
//		for (int i = 0; i < sums.length; i++) {
//			order.put(sums[i], i);
//		}
//		stackedData.clear();
//		for (int i = 0; i < vals.length; i++) {
//			stackedData.add(new Integer[vals[0].length]);
//		}
//	}

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
		ss.scrollIndex = scrollStartIndex;
		ss.selectionX = selectionDrawer.getSelectionX();
		ss.valueScale = valueScale;
		ss.STEP = STEP;
		ss.maxValueVisible = maxValueVisible;
		ss.maxValueCalculated = maxValueCalculated;
		ss.maxValuesLine = maxValuesLine;
		ss.gridValueStep = gridValueStep;
		ss.dateRange = dateRange;
		ss.gridCount = gridCount;
		ss.gridScale = gridScale;
		ss.gridStep = gridStep;
		ss.dateRangeHeight = dateRangeHeight;
		ss.data = data;
		ss.yScale = yScale;
		ss.yIndex = yIndex;
		ss.isYscaled = isYscaled;
		ss.sumVals = sumVals;
		return ss;
	}

	@Override
	public void onRestoreInstanceState(Parcelable state) {
		SavedState ss = (SavedState) state;
		super.onRestoreInstanceState(ss.getSuperState());

		linesVisibility = ss.linesVisibility;
		linesCalculated = ss.linesCalculated;
		scrollPos = ss.scrollPos;
		scrollStartIndex = ss.scrollIndex;
		selectionDrawer.setSelectionX(ss.selectionX);
		valueScale = ss.valueScale;
		STEP = ss.STEP;
		maxValueVisible = ss.maxValueVisible;
		maxValueCalculated = ss.maxValueCalculated;
		maxValuesLine = ss.maxValuesLine;
		gridValueStep = ss.gridValueStep;
		dateRangeHeight = ss.dateRangeHeight;
		dateRange = ss.dateRange;
		gridCount = ss.gridCount;
		gridScale = ss.gridScale;
		gridStep = ss.gridStep;
		data = ss.data;
		yScale = ss.yScale;
		yIndex = ss.yIndex;
		isYscaled = ss.isYscaled;
		sumVals = ss.sumVals;

		if (data != null) {
			selectionDrawer.setLinesCount(data.getLinesCount());
			linePaints = new Paint[data.getLinesCount()];
			for (int i = 0; i < data.getLinesCount(); i++) {
				linePaints[i] = createLinePaint(data.getColorsInts()[i], data.getType(i) == ChartData.TYPE_BAR);
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
			boolean[] bools = new boolean[1];
			in.readBooleanArray(bools);
			isYscaled = bools[0];
			float[] floats = new float[12];
			in.readFloatArray(floats);
			scrollPos = floats[0];
			scrollIndex = floats[1];
			selectionX = floats[2];
			valueScale = floats[3];
			STEP = floats[4];
			maxValueVisible = floats[5];
			maxValueCalculated = floats[6];
			gridValueStep = floats[7];
			dateRangeHeight = floats[8];
			gridScale = floats[9];
			gridStep = floats[10];
			yScale = floats[11];
			gridCount = in.readInt();
			yIndex = in.readInt();
			in.readIntArray(maxValuesLine);
			in.readFloatArray(sumVals);
			dateRange = in.readString();
			data = in.readParcelable(ChartData.class.getClassLoader());
		}

		@Override
		public void writeToParcel(Parcel out, int flags) {
			super.writeToParcel(out, flags);
			out.writeBooleanArray(linesVisibility);
			out.writeBooleanArray(linesCalculated);
			out.writeBooleanArray(new boolean[] {isYscaled});
			out.writeFloatArray(new float[] {scrollPos, scrollIndex, selectionX,
					valueScale, STEP, maxValueVisible, maxValueCalculated, gridValueStep,
					dateRangeHeight, gridScale, gridStep, yScale});
			out.writeInt(gridCount);
			out.writeInt(yIndex);
			out.writeIntArray(maxValuesLine);
			out.writeFloatArray(sumVals);
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
		float valueScale;
		float maxValueVisible;
		float maxValueCalculated;
		int[] maxValuesLine;
		float[] sumVals;
		float gridValueStep;
		String dateRange;
		float dateRangeHeight;
		float gridScale;
		float gridStep;
		int gridCount;
		float yScale;
		int yIndex;
		boolean isYscaled;

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
