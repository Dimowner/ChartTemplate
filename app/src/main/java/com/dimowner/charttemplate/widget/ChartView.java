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
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;

import com.dimowner.charttemplate.ColorMap;
import com.dimowner.charttemplate.R;
import com.dimowner.charttemplate.model.ChartData;
import com.dimowner.charttemplate.util.AndroidUtils;
import com.dimowner.charttemplate.util.TimeUtils;

import java.text.DecimalFormat;

import timber.log.Timber;

public class ChartView extends View {

	private final float DENSITY;
	private final int PADD_NORMAL;
	private final int PADD_SMALL;
	private final int PADD_TINY;
	private final int TEXT_SPACE;
	private final int BASE_LINE_Y;
	private final int MIN_GRID_STEP;
	private final int MAX_GRID_STEP;
	private final int DATE_RANGE_PADD;
	private final int RADIUS;
	private final int LINE_WIDTH;
	private static final int GRID_LINES_COUNT = 6;
	private static final int ANIMATION_DURATION = 220; //mills

	{
		DENSITY = AndroidUtils.dpToPx(1);
		PADD_NORMAL = (int) (16*DENSITY);
		PADD_SMALL = (int) (8*DENSITY);
		PADD_TINY = (int) (4*DENSITY);
		TEXT_SPACE = (int) (56*DENSITY);
		BASE_LINE_Y = (int) (32*DENSITY);
		MIN_GRID_STEP = (int) (44*DENSITY);
		MAX_GRID_STEP = (int) (90*DENSITY);
		RADIUS = (int) (120*DENSITY);
		LINE_WIDTH = (int) (2*DENSITY);
		DATE_RANGE_PADD = (int) (21*DENSITY);
	}

	private static final int START_ANGLE = 45;
	private float STEP = 10*DENSITY;
	/** Chart view height except bottom timeline height */
	private float H1 = 0;
	/** Chart view height except bottom timeline and top texts heights*/
	private float H2 = 0;
	/** 1% of the H2 var. */
	private float H3 = 0;
	private float HEIGHT_PADDS = 2*BASE_LINE_Y+PADD_NORMAL;

	private DecimalFormat format = new DecimalFormat("#,##0.#");
	private StringBuilder stringBuilder = new StringBuilder();

	private ChartData data;

	private float chartArray[];
	private float chartArray2[];

	private boolean[] linesVisibility;
	private boolean[] linesCalculated;

	private TextPaint dateRangePaint;
	private TextPaint timelineTextPaint;
	private TextPaint percentPaint;
	private Paint gridPaint;
	private Paint[] linePaints;

	private ValueAnimator alphaAnimator;
	private ValueAnimator heightAnimator;
	private ValueAnimator minHeightAnimator;
	private ValueAnimator moveAnimator;
	private DecelerateInterpolator decelerateInterpolator = new DecelerateInterpolator();
	private LinearInterpolator linearInterpolator = new LinearInterpolator();
	private AccelerateInterpolator accelerateInterpolator= new AccelerateInterpolator();

	private float scrollPos;
	private float scrollStartIndex;
	private float indexesWidth;

	private ChartSelectionDrawer selectionDrawer;

	private float WIDTH = 1;
	private float HEIGHT = 1;

	private float valueScale = 1;
	private float maxValueVisible = 0;
	private float maxValueCalculated = 0;

	private float minValueVisible = 0;
	private float minValueCalculated = 0;

	//Y scaled line values;
	private float yScale = 1;
	private int yIndex = 0;
	private boolean isYscaled = false;

	private int[] maxValuesLine;
	private int[] minValuesLine;
	private float gridScale = 1;
	private int gridCount = GRID_LINES_COUNT;
	private float gridStep = 1;
	private float gridValueStep = 1;
	private boolean skipNextInvalidation = false;
	private String dateRange = "";
	private float dateRangeHeight;
	private Rect rect;
	private int gridTextColor;
	private int barOverlayColor;
	private final String minus = " - ";

	private boolean isAnimating = false;
	private float scaleKoef = 1;
	private int amnimItemIndex = -1;
	private float[] sumVals;
	private boolean isFirst = true;
	private boolean isMove = false;
	private int scale = 1;
	private boolean isDetailsMode = false;

	private float[] arcSums;
	private int totalAdcSum;
	private float prevArc;

	public boolean isMoveAnimation = false;
	private float moveVal = 0;
	private float moveValX= 0;
	private float moveValY= 0;
	private int moveIndex = -1;

//	private OnMoveEventsListener onMoveEventsListener;
	private GestureDetector gestureDetector;

	public OnDetailsListener onDetailsListener;

	ValueAnimator.AnimatorUpdateListener heightValueAnimator = new ValueAnimator.AnimatorUpdateListener() {
		@Override
		public void onAnimationUpdate(ValueAnimator animation) {
			maxValueVisible = maxValueCalculated - (float)animation.getAnimatedValue();
			updateValueScale();
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
			gridStep = gridValueStep * valueScale;
			if (gridStep < 40*DENSITY) { gridStep = 40*DENSITY;}
			updateGrid();
		}
	};

	ValueAnimator.AnimatorUpdateListener minHeightValueAnimator = new ValueAnimator.AnimatorUpdateListener() {
		@Override
		public void onAnimationUpdate(ValueAnimator animation) {
			minValueVisible = minValueCalculated - (float)animation.getAnimatedValue();
			updateValueScale();
			invalidate();
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
				if (isDetailsMode) {
					calculateArcs();
				} else {
					calculateSumsLine();
				}
			}
			invalidate();
		}
	};

	public ChartView(Context context) {
		super(context);
		init(context, null);
	}

	public ChartView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context, attrs);
	}

	public ChartView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(context, attrs);
	}

	private void init(Context context, AttributeSet attrs) {
		setFocusable(false);

		scrollPos = -1;
		scrollStartIndex = 0;
		rect = new Rect();

		int gridColor;
		int panelTextColor;
		int panelColor;
		int shadowColor;
		int tittleColor;
		int viewBackground;
		int arrowColor;

		Resources res = context.getResources();
//		gridColor = res.getColor(R.color.grid_color2);
//		gridTextColor = res.getColor(R.color.text_color);
//		panelColor = res.getColor(R.color.panel_background);
//		panelTextColor = res.getColor(R.color.panel_text);
//		shadowColor = res.getColor(R.color.shadow_color);
//		tittleColor = res.getColor(R.color.black);
//		viewBackground = res.getColor(R.color.view_background);
//		barOverlayColor = res.getColor(R.color.bar_overlay_color);
//		arrowColor = res.getColor(R.color.arrow_color);

		gridColor = res.getColor(ColorMap.getGridColor());
		gridTextColor = res.getColor(ColorMap.getGridTextColor());
		panelColor = res.getColor(ColorMap.getPanelColor());
		panelTextColor = res.getColor(ColorMap.getPanelTextColor());
		shadowColor = res.getColor(ColorMap.getShadowColor());
		tittleColor = res.getColor(ColorMap.getTittleColor());
		viewBackground = res.getColor(ColorMap.getViewBackground());
		barOverlayColor = res.getColor(ColorMap.getBarOverlayColor());
		arrowColor = res.getColor(ColorMap.getArrowColor());

//		if (attrs != null) {
//			TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.ChartView);
//			if (ta != null) {
//				//Read View custom attributes
//				viewBackground = ta.getColor(R.styleable.ChartView_viewBackground, res.getColor(R.color.view_background));
//				gridColor = ta.getColor(R.styleable.ChartView_gridColor, res.getColor(R.color.grid_color2));
//				panelColor = ta.getColor(R.styleable.ChartView_panelColor, res.getColor(R.color.panel_background));
//				panelTextColor = ta.getColor(R.styleable.ChartView_panelTextColor, res.getColor(R.color.panel_text));
//				shadowColor = ta.getColor(R.styleable.ChartView_shadowColor, res.getColor(R.color.shadow_color));
//				tittleColor = ta.getColor(R.styleable.ChartView_tittleColor, res.getColor(R.color.black));
//				barOverlayColor = ta.getColor(R.styleable.ChartView_barOverlayColor, res.getColor(R.color.bar_overlay_color));
//				arrowColor = ta.getColor(R.styleable.ChartView_arrowColor, res.getColor(R.color.arrow_color));
//				ta.recycle();
//			}
//		} else {
//			Timber.v("attributes is null \\_(-_-)_/");
//			TypedValue typedValue = new TypedValue();
//			Resources.Theme theme = context.getTheme();
//			if (theme.resolveAttribute(R.attr.gridColor, typedValue, true)) {
//				gridColor = typedValue.data;
//			}
//			if (theme.resolveAttribute(R.attr.gridTextColor, typedValue, true)) {
//				gridTextColor = typedValue.data;
//			}
//			if (theme.resolveAttribute(R.attr.panelColor, typedValue, true)) {
//				panelColor = typedValue.data;
//			}
//			if (theme.resolveAttribute(R.attr.panelTextColor, typedValue, true)) {
//				panelTextColor = typedValue.data;
//			}
//			if (theme.resolveAttribute(R.attr.shadowColor, typedValue, true)) {
//				shadowColor = typedValue.data;
//			}
//			if (theme.resolveAttribute(R.attr.tittleColor, typedValue, true)) {
//				tittleColor = typedValue.data;
//			}
//			if (theme.resolveAttribute(R.attr.viewBackground, typedValue, true)) {
//				viewBackground = typedValue.data;
//			}
//			if (theme.resolveAttribute(R.attr.barOverlayColor, typedValue, true)) {
//				barOverlayColor = typedValue.data;
//			}
//			if (theme.resolveAttribute(R.attr.arrowColor, typedValue, true)) {
//				arrowColor = typedValue.data;
//			}
//		}

		selectionDrawer = new ChartSelectionDrawer(getContext(), panelTextColor, arrowColor,
					panelColor, gridColor, shadowColor, viewBackground, barOverlayColor);
		selectionDrawer.setInvalidateIlstener(new ChartSelectionDrawer.InvalidateIlstener() {
			@Override
			public void onInvalidate() {
				invalidate();
			}
		});

		gridPaint = new Paint();
		gridPaint.setAntiAlias(false);
		gridPaint.setDither(false);
		gridPaint.setStyle(Paint.Style.STROKE);
		gridPaint.setStrokeCap(Paint.Cap.SQUARE);
		gridPaint.setColor(gridColor);
		gridPaint.setStrokeWidth(DENSITY);

		dateRangePaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
		dateRangePaint.setColor(tittleColor);
		dateRangePaint.setTextAlign(Paint.Align.RIGHT);
		dateRangePaint.setTypeface(Typeface.create("sans-serif-sans-serif-thin", Typeface.BOLD));
		dateRangePaint.setTextSize(context.getResources().getDimension(R.dimen.text_normal));

		percentPaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
		percentPaint.setColor(Color.WHITE);
		percentPaint.setTextAlign(Paint.Align.CENTER);
		percentPaint.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
		percentPaint.setTextSize(context.getResources().getDimension(R.dimen.text_normal));

		timelineTextPaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
		timelineTextPaint.setColor(gridTextColor);
		timelineTextPaint.setTextAlign(Paint.Align.CENTER);
		timelineTextPaint.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
		timelineTextPaint.setTextSize(context.getResources().getDimension(R.dimen.text_xsmall));

		gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
			@Override
			public boolean onSingleTapUp(MotionEvent e) {
				if (data.isPercentage() && isDetailsMode) {
					int ind = arcIndexForCoordinates(e.getX(), e.getY());
					if (ind != moveIndex || (moveValX == 0 && moveValY == 0)) {
						moveIndex = ind;
						moveAnimation(0, PADD_NORMAL);
					} else {
						moveAnimation(PADD_NORMAL, 0);
					}
				} else
				if (!selectionDrawer.isShowPanel()) {
					selectionDrawer.showPanel();
					selectionDrawer.setSelectionX(e.getX());
					selectionDrawer.calculatePanelSize(data, STEP, linesCalculated, scrollPos, WIDTH,
							isYscaled, yIndex, yScale, sumVals);
				} else
				if (!selectionDrawer.checkCoordinateInPanel(e.getX(), e.getY())) {
					selectionDrawer.hidePanel();
				} else {
					//Open detailed chart;
					if (onDetailsListener != null && !isDetailsMode) {
						onDetailsListener.showDetails(data.getChartNum(), data.getTime()[selectionDrawer.getSelectionIndex()]);
					}
				}
				return super.onSingleTapUp(e);
			}

			@Override
			public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
				isMove = true;
				if (!selectionDrawer.isShowPanel()) {
					selectionDrawer.showPanel();
				}
				return super.onScroll(e1, e2, distanceX, distanceY);
			}

			@Override
			public boolean onDown(MotionEvent e) {
				return true;
			}
		});

		setOnTouchListener(new OnTouchListener() {

			float startY = 0;

			@Override
			public boolean onTouch(View v, MotionEvent motionEvent) {
				switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {
					case MotionEvent.ACTION_DOWN:
//						selectionDrawer.reset();
						startY = motionEvent.getY();
						break;
					case MotionEvent.ACTION_MOVE:
						if (isMove) {
							float selectionX = motionEvent.getX();
							if (selectionX > WIDTH) {
								selectionX = WIDTH;
							}
							if (selectionX < 0) {
								selectionX = -1;
							}
							if (Math.abs(motionEvent.getY() - startY) < 90 * DENSITY) {
								selectionDrawer.setSelectionX(selectionX);
								selectionDrawer.calculatePanelSize(data, STEP, linesCalculated, scrollPos,
										WIDTH, isYscaled, yIndex, yScale, sumVals);
								if (isDetailsMode && !data.isPercentage()) {
									setDateRange((int)((scrollPos+selectionX)/STEP));
								}
//								if (onMoveEventsListener != null) {
//									onMoveEventsListener.disallowTouchEvent();
//								}
								getParent().requestDisallowInterceptTouchEvent(true);
							} else {
//								selectionX = -1;
								selectionDrawer.setSelectionX(selectionX);
//								if (onMoveEventsListener != null) {
//									onMoveEventsListener.allowTouchEvent();
//								}
								getParent().requestDisallowInterceptTouchEvent(false);
							}
							invalidate();
						}
						break;
					case MotionEvent.ACTION_UP:
						if (isMove) {
//							selectionDrawer.setSelectionX(-1);
//							selectionDrawer.hidePanel();
							isMove = false;
						}
						if (isDetailsMode) {
							calculateDateRange();
						}
						invalidate();
						performClick();
						break;
				}
				return gestureDetector.onTouchEvent(motionEvent);
			}
		});
	}

	private Paint createLinePaint(int color, boolean isBars) {
		Paint lp = new Paint();
		lp.setStyle(Paint.Style.STROKE);
		lp.setStrokeWidth(LINE_WIDTH);
		lp.setAntiAlias(true);
//		lp.setStrokeJoin(Paint.Join.ROUND);
		lp.setColor(color);
//		if (isBars) {
			lp.setStrokeCap(Paint.Cap.BUTT);
//		}
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
		heightAnimator.start();
	}

	private void minHeightAnimator(final float diff) {
		if (minHeightAnimator != null && (minHeightAnimator.isStarted())) {
			minHeightAnimator.cancel();
		}
		minHeightAnimator = ValueAnimator.ofFloat(diff, 0);
		minHeightAnimator.setInterpolator(linearInterpolator);
		minHeightAnimator.setDuration(300);
		minHeightAnimator.addUpdateListener(minHeightValueAnimator);
		minHeightAnimator.start();
	}

	private void moveAnimation(final float start, final float end) {
		isMoveAnimation = true;
		if (moveAnimator != null && (moveAnimator.isStarted())) {
			moveAnimator.cancel();
		}
		moveAnimator = ValueAnimator.ofFloat(start, end);
		moveAnimator.setInterpolator(decelerateInterpolator);
		moveAnimator.setDuration(300);
		moveAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator animation) {
				moveVal = (float)animation.getAnimatedValue();
				int prevArc = START_ANGLE;
				for (int i = 0; i < moveIndex; i++) {
					if (linesVisibility[moveIndex]) {
						prevArc += arcSums[i];
					}
				}
				moveValX = (float) Math.cos((prevArc+arcSums[moveIndex]/2)*Math.PI/180)*moveVal;
				moveValY = (float) Math.sin((prevArc+arcSums[moveIndex]/2)*Math.PI/180)*moveVal;

				if (moveVal == end) {
					isMoveAnimation = false;
					if (end == 0) {
						moveIndex = -1;
					}
				}
				invalidate();
			}
		});
		moveAnimator.start();
	}

	private void updateGrid() {
		gridCount = (int)((HEIGHT-BASE_LINE_Y-PADD_TINY)/gridStep);
	}

	public void scrollPos(float x, float size) {
		if (x >= 0) {
			STEP = WIDTH / size;
			scrollPos = (x * STEP);
			scrollStartIndex = x;
			indexesWidth = size;

			if (data.isPercentage()) {
				if (size > 260) {
					scale = 3;
				} else if (size > 130) {
					scale = 2;
				} else {
					scale = 1;
				}
			} else if (data.isStacked()) {
				if (size > 190) {
					scale = 2;
				} else {
					scale = 1;
				}
			}

			selectionDrawer.setScrollPos(scrollStartIndex, STEP);
			calculateDateRange();
			if (!data.isPercentage()) {
				calculateMaxValue2(true, !isFirst);
				isFirst = false;
				updateGrid();
				skipNextInvalidation = true;
			} else {
				calculateArcs();
			}

			invalidate();
		}
	}

	private int idx;
	private void calculateDateRange() {
//		int idx = (int) Math.ceil(scrollStartIndex + indexesWidth) - 1;
		idx = (int) Math.ceil(scrollStartIndex + indexesWidth) - 1;
		if (idx < data.getLength()) {
			if (isDetailsMode) {
				int start = (int) Math.floor(scrollStartIndex);
				if (TimeUtils.isDiffSorterThan2Days(data.getTime()[start], data.getTime()[idx])) {
					dateRange = data.getTimesLong()[start];
				} else {
					dateRange = data.getTimesLong()[start] + minus + data.getTimesLong()[idx];
				}
			} else {
				dateRange = data.getTimesLong()[(int) Math.floor(scrollStartIndex)] + minus + data.getTimesLong()[idx];
			}
			dateRangePaint.getTextBounds(dateRange, 0, dateRange.length(), rect);
		}
		if (dateRangeHeight < rect.height()) {
			dateRangeHeight = rect.height();
		}
		if (dateRange == null) {
			dateRange = "";
		}
	}

	private void setDateRange(int index) {
		if (data.getLength() > index) {
			dateRange = data.getTimesLong()[index];
			dateRangePaint.getTextBounds(dateRange, 0, dateRange.length(), rect);
			if (dateRangeHeight < rect.height()) {
				dateRangeHeight = rect.height();
			}
			if (dateRange == null) {
				dateRange = "";
			}
		}
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		WIDTH = getWidth();
		HEIGHT = getHeight();
		H1 = HEIGHT - BASE_LINE_Y;
		H2 = (HEIGHT-HEIGHT_PADDS);
		H3 = (H2-PADD_NORMAL)/100; //1% of view height
//		gridStep = (HEIGHT/GRID_LINES_COUNT);
		if (maxValueVisible > 0) {
//			valueScale = (HEIGHT - 2*BASE_LINE_Y-PADD_NORMAL) / maxValueVisible;
			updateValueScale();
			gridScale = valueScale;
		}
	}

	private int arcIndexForCoordinates(float x, float y) {
		float angle = (float) Math.atan2((y - WIDTH/2), (x - (HEIGHT-BASE_LINE_Y+PADD_SMALL)/2 + (BASE_LINE_Y+PADD_NORMAL+PADD_SMALL)/2f));

		float angle2 =(float) (angle * 180/Math.PI);
		if (angle2 < 0) {
			angle2 = 180 + 180+angle2;
		}

		float angleToStart  = angle2 - START_ANGLE;
		if (angleToStart < 0) {
			angleToStart = 360-START_ANGLE + START_ANGLE + angleToStart;
		}

		float prev = 0;
		if (arcSums != null && arcSums.length > 0) {
			for (int i = 0; i < arcSums.length; i++) {
				prev += arcSums[i];
				if (angleToStart - prev < 0) {
					return i;
				}
			}
		}
		return -1;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (data != null) {
			//Draw charts
			timelineTextPaint.setTextAlign(Paint.Align.CENTER);
			for (int i = 0; i < data.getLinesCount(); i++) {
				if (linesVisibility[i]) {
					if (data.getType(i) == ChartData.TYPE_LINE) {
						drawChart(canvas, data.getValues(i), i);
					} else if (data.getType(i) == ChartData.TYPE_BAR) {
						drawBars(canvas, data.getValues(i), i);
					} else if (data.getType(i) == ChartData.TYPE_AREA) {
						if (isDetailsMode) {
							drawPercentCircle(canvas, i);
						} else {
							drawAreaPercentage(canvas, data.getValues(i), i);
						}
					} else {
						drawChart(canvas, data.getValues(i), i);
					}
				}
			}
			timelineTextPaint.setTextAlign(Paint.Align.LEFT);
			selectionDrawer.drawBarOverlay(canvas, data.getType(0), STEP, H1, WIDTH, HEIGHT);
			if (data.isPercentage()) {
				if (!isDetailsMode) {
					drawPercentageGrid(canvas);
				}
			} else {
				drawGrid(canvas);
			}

			if (data.getLinesCount() > 0 && !(data.isPercentage() && isDetailsMode)) {
				timelineTextPaint.setTextAlign(Paint.Align.CENTER);
				timelineTextPaint.setColor(gridTextColor);
				drawTimeline(canvas);
				//Draw selection panel with scrubbler
				selectionDrawer.draw(canvas, data, linesVisibility, HEIGHT, linePaints, valueScale, minValueVisible);
			}
			canvas.drawText(dateRange, WIDTH-2, dateRangeHeight+DATE_RANGE_PADD, dateRangePaint);
		}
	}

	private void drawPercentCircle(Canvas canvas, int i) {
		if (i == 0) { prevArc = START_ANGLE;}
		linePaints[i].setStyle(Paint.Style.FILL);
		if (i == moveIndex) {
			canvas.drawArc(
					(WIDTH - (H2)) / 2+ moveValX,
					BASE_LINE_Y + PADD_NORMAL + PADD_SMALL + moveValY,
					(WIDTH - (H2)) / 2 + H2 + moveValX,
					HEIGHT - BASE_LINE_Y + PADD_SMALL + moveValY,
					prevArc, arcSums[i], true, linePaints[i]);
			if (moveValX != 0 || moveValY != 0) {
				canvas.drawText(data.getNames()[i], WIDTH / 2 + moveValX * 6, HEIGHT/2+PADD_NORMAL + moveValY * 6, percentPaint);
			}
		} else {
			canvas.drawArc(
					(WIDTH - (H2)) / 2, BASE_LINE_Y + PADD_NORMAL + PADD_SMALL,
					(WIDTH - (H2)) / 2 + H2, HEIGHT - BASE_LINE_Y + PADD_SMALL,
					prevArc, arcSums[i], true, linePaints[i]);
			canvas.drawText((int) (arcSums[i]/3.6) + "%",
					WIDTH/2+(float) Math.cos((prevArc+arcSums[i]/2)*Math.PI/180)*RADIUS,
					(HEIGHT)/2+(float) Math.sin((prevArc+arcSums[i]/2)*Math.PI/180)*RADIUS+PADD_NORMAL+PADD_TINY, percentPaint);
		}
		prevArc += arcSums[i];
	}

	private void drawGrid(Canvas canvas) {
		if (isYscaled) {
			timelineTextPaint.setColor(data.getColorsInts()[yIndex == 0 ? 1: 0]);
		} else {
			timelineTextPaint.setColor(gridTextColor);
		}
		for (int i = 0; i < gridCount; i++) {
			canvas.drawLine(0, H1 - gridStep * i, WIDTH, H1 - gridStep * i, gridPaint);
			canvas.drawText(formatValue(gridValueStep * i+minValueVisible), 0, H1 - gridStep * i - PADD_TINY, timelineTextPaint);
		}
//		for (float i = 0; i < maxValueVisible*valueScale; i+=gridStep) {
//			canvas.drawLine(0, H1 - i, WIDTH, H1 - i, gridPaint);
//		}
		if (isYscaled) {
			timelineTextPaint.setColor(data.getColorsInts()[yIndex]);
			timelineTextPaint.setTextAlign(Paint.Align.RIGHT);
			timelineTextPaint.setAlpha(linePaints[yIndex].getAlpha());
			for (int i = 0; i < gridCount; i++) {
				canvas.drawText(formatValue((gridValueStep * i+minValueVisible)/yScale),
						WIDTH, H1 - gridStep * i - PADD_TINY, timelineTextPaint);
			}
			timelineTextPaint.setAlpha(255);
		}
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
//		selectionDrawer.setView(null);
	}

	private void drawPercentageGrid(Canvas canvas) {
		gridStep = (H2-PADD_NORMAL)/5;
		gridValueStep = 20;//%
		timelineTextPaint.setAlpha(255);
		for (int i = 0; i <= 5; i++) {
			canvas.drawLine(0, H1 - gridStep * i, WIDTH, H1 - gridStep * i, gridPaint);
			canvas.drawText(String.valueOf(gridValueStep * i), 0, H1 - gridStep * i - PADD_TINY, timelineTextPaint);
		}
	}

	private float chartPos;
	private int chartSkip;
	private int chartK;

	private void drawChart(Canvas canvas, int[] values, int index) {
//		float pos = -scrollPos;
		chartPos = -scrollPos;
//		int skip = (int) scrollStartIndex -(int)(PADD_NORMAL/STEP);
		chartSkip = (int) scrollStartIndex -(int)(PADD_NORMAL/STEP);
		if (chartSkip < 0) {chartSkip = 0;}
		chartPos +=chartSkip*STEP;
//		int k = chartSkip;
		chartK = chartSkip;
		for (int i = chartSkip; i < values.length; i++) {
			if (chartK < chartArray.length) {
				chartArray[chartK] = chartPos; //x1
				chartArray[chartK + 1] = H1 - (values[i]-minValueVisible) * valueScale; //y1
				if (i + 1 < values.length) {
					chartArray[chartK + 2] = chartPos + STEP; //x2
					chartArray[chartK + 3] = H1 - (values[i + 1]-minValueVisible) * valueScale; //y2
				} else {
					chartArray[chartK + 2] = chartPos; //x2
					chartArray[chartK + 3] = H1 - (values[i]-minValueVisible) * valueScale; //y2
				}
				chartK +=4;
				if (chartPos - STEP > WIDTH+PADD_NORMAL) {
					break;
				}
			}
			chartPos += STEP;
		}
		if (chartPos/STEP < 80) {
			linePaints[index].setStrokeCap(Paint.Cap.ROUND);
		} else {
			linePaints[index].setStrokeCap(Paint.Cap.SQUARE);
		}
		canvas.drawLines(chartArray, chartSkip, chartK-chartSkip, linePaints[index]);
	}

	private float barPos;
	private int barSkip;
	private int barK;
	private int barJ;
	private int barSum;
	private void drawBars(Canvas canvas, int[] values, int index) {
//		float pos = -scrollPos;
		barPos = -scrollPos;
//		int skip = (int) scrollStartIndex -(int)(PADD_NORMAL/STEP);
		barSkip = (int) scrollStartIndex -(int)(PADD_NORMAL/STEP);
		if (barSkip < 0) {barSkip = 0;}
		barPos +=barSkip*STEP;
//		int k = barSkip;
		barSkip -= barSkip%scale;
		barK = barSkip;
		linePaints[index].setStrokeWidth(scale*STEP+1);
		if (data.isStacked()) {
//			int j;
//			int sum=0;
			barSum = 0;
			for (int i = barSkip; i < values.length; i+=scale) {
				if (barK < chartArray.length) {
					for (barJ = 0; barJ <= index; barJ++) {
						if (linesCalculated[barJ] && barJ != amnimItemIndex) { //
							barSum += data.getVal(barJ, i);
						}
					}
					if (isAnimating && amnimItemIndex <= index) {
						barSum += scaleKoef*data.getVal(amnimItemIndex, i);
					}
					if (data.isPercentage()) {
						chartArray[barK] = barPos; //x1
						if (index == amnimItemIndex) {
							chartArray[barK + 1] = H1 - H3*(barSum - values[i] * scaleKoef)/(sumVals[i]);
						} else {
							chartArray[barK + 1] = H1 - H3*(barSum - values[i])/(sumVals[i]);
						}
						chartArray[barK + 2] = barPos; //x2
						chartArray[barK + 3] = H1 - H3*barSum/(sumVals[i]); //y2
					} else {
						chartArray[barK] = barPos; //x1
	//					chartArray[k + 1] = H1 - data.getValues(index-1)[i] * valueScale; //y1
						if (index == amnimItemIndex) {
							chartArray[barK + 1] = H1 - (barSum - values[i] * scaleKoef) * valueScale; //y1
						} else {
							chartArray[barK + 1] = H1 - (barSum - values[i]) * valueScale; //y1
						}
						chartArray[barK + 2] = barPos; //x2
	//					chartArray[k + 3] = H1 - values[i] * valueScale; //y2
						chartArray[barK + 3] = H1 - barSum * valueScale; //y2
					}
					barK += 4;
					if (barPos - STEP > WIDTH + PADD_NORMAL) {
						break;
					}
					barSum = 0;
				}
				barPos += scale*STEP;
			}
		} else {
			for (int i = barSkip; i < values.length; i++) {
				if (barK < chartArray.length) {
					chartArray[barK] = barPos; //x1
					chartArray[barK + 1] = H1; //y1
					chartArray[barK + 2] = barPos; //x2
					chartArray[barK + 3] = H1 - values[i] * valueScale; //y2
					barK += 4;
					if (barPos - STEP > WIDTH + PADD_NORMAL) {
						break;
					}
				}
				barPos += STEP;
			}
		}
		canvas.drawLines(chartArray, barSkip, barK-barSkip, linePaints[index]);
	}

	private void drawAreaPercentage(Canvas canvas, int[] values, int index) {
		float pos = -scrollPos;
		int skip = (int) scrollStartIndex -(int)(PADD_NORMAL/STEP);
		if (skip < 0) {skip = 0;}
		skip -= skip%scale;
		pos +=skip*STEP;
		int k = skip;
		int j;
		int sum=0;
		int sum2=0;
		for (int i = skip; i < values.length; i+=scale) {
			if (k < chartArray.length) {
				for (j = 0; j <= index; j++) {
					if (linesCalculated[j] && j != amnimItemIndex) { //
						sum += data.getVal(j, i);
						if (i+scale < values.length) {
							sum2 += data.getVal(j, i + scale);
						}
					}
				}
				if (isAnimating && amnimItemIndex <= index) {
					sum += scaleKoef*data.getVal(amnimItemIndex, i);
					if (i+scale < values.length) {
						sum2 += scaleKoef * data.getVal(amnimItemIndex, i+scale);
					}
				}
				chartArray[k] = pos; //x1
				if (index == amnimItemIndex) {
					chartArray[k + 1] = H1 - H3*(sum - values[i] * scaleKoef)/(sumVals[i]);
				} else {
					chartArray[k + 1] = H1 - H3*(sum - values[i])/(sumVals[i]);
				}
				chartArray[k + 2] = pos; //x2
				chartArray[k + 3] = H1 - H3*sum/(sumVals[i]); //y2

				///Draw lines
				chartArray2[k] = pos-1; //x1
				if (index == amnimItemIndex) {
					chartArray2[k + 1] = H1 - H3*(sum - values[i] * scaleKoef)/(sumVals[i]);
				} else {
					chartArray2[k + 1] = H1 - H3*(sum - values[i])/(sumVals[i]);
				}
				if (i + scale < values.length) {
					chartArray2[k + 2] = pos + STEP*scale; //x2
					if (index == amnimItemIndex) {
						chartArray2[k + 3] = H1 - H3 * (sum2 - values[i+scale]* scaleKoef) / (sumVals[i + scale]);
					} else {
						chartArray2[k + 3] = H1 - H3 * (sum2 - values[i+scale]) / (sumVals[i + scale]);
					}
				} else {
					chartArray2[k + 2] = pos+STEP*scale/2; //x2
					if (index == amnimItemIndex) {
						chartArray2[k + 3] = H1 - H3 * (sum - values[i]*scaleKoef) / (sumVals[i]); //y2
					} else {
						chartArray2[k + 3] = H1 - H3 * (sum - values[i]) / (sumVals[i]); //y2
					}
				}
				k += 4;
				if (pos - STEP*scale > WIDTH + PADD_NORMAL) {
					break;
				}
				sum = 0;
				sum2 = 0;
			}
			pos += scale*STEP;
		}

		linePaints[index].setStrokeWidth(scale*STEP+1);
		linePaints[index].setTextAlign(Paint.Align.RIGHT);
		linePaints[index].setStrokeCap(Paint.Cap.BUTT);
		canvas.drawLines(chartArray, skip, k-skip, linePaints[index]);
		if (data.isPercentage() && index > 0 && !isBottomLine(index)) {
			linePaints[index].setStrokeWidth(STEP*scale);
//			linePaints[index].setStrokeCap(Paint.Cap.BUTT);
			canvas.drawLines(chartArray2, skip, k - skip, linePaints[index]);
			linePaints[index].setStrokeCap(Paint.Cap.ROUND);
			canvas.drawPoints(chartArray2, skip, k - skip-1, linePaints[index]);
		}
	}

	private boolean isBottomLine(int index) {
		for (int i = 0; i < index; i++) {
			if (linesVisibility[i]) {
				return false;
			}
		}
		return true;
	}

	private float timelinePos;
	private int timelineCount;

	private void drawTimeline(Canvas canvas) {
//		float pos = 0;
//		int count = 1;
		timelinePos = 0;
		timelineCount = 1;
		while (timelineCount*STEP < TEXT_SPACE) {
			timelineCount*=2;
		}

		for (int i = 0; i < data.getLength()/timelineCount+1; i++) {
			if (i == 0) {
				timelineTextPaint.setTextAlign(Paint.Align.LEFT);
			} else {
				timelineTextPaint.setTextAlign(Paint.Align.CENTER);
			}
			if (timelinePos-scrollPos+TEXT_SPACE >= 0 && timelinePos-scrollPos < WIDTH && i*timelineCount < data.getLength()) {
				if (timelineCount*STEP > TEXT_SPACE && timelineCount*STEP < TEXT_SPACE*1.18f && (i)%2!=0) {
					timelineTextPaint.setAlpha((int)(255/(TEXT_SPACE*0.18f)*(timelineCount*STEP-TEXT_SPACE)));
				} else {
					timelineTextPaint.setAlpha(255);
				}
				if (isDetailsMode) {
					int start = (int)(scrollPos/STEP);
					if (data.getLength() > start+(int)(indexesWidth)
							&& TimeUtils.isDiffSorterThan2Days(data.getTime()[start], data.getTime()[start+(int)(indexesWidth)])) {
						canvas.drawText(data.getTimes()[i * timelineCount], timelinePos - scrollPos, HEIGHT - PADD_NORMAL, timelineTextPaint);
					} else {
						canvas.drawText(data.getTimesShort()[i * timelineCount], timelinePos - scrollPos, HEIGHT - PADD_NORMAL, timelineTextPaint);
					}
				} else {
					canvas.drawText(data.getTimesShort()[i * timelineCount], timelinePos - scrollPos, HEIGHT - PADD_NORMAL, timelineTextPaint);
				}
			}
			timelinePos += timelineCount*STEP;
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
		calculateMaxValue2(false, true);
		selectionDrawer.hidePanel();
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
		calculateMaxValue2(false, true);
		selectionDrawer.hidePanel();
		updateGrid();
	}

	public void setData(ChartData d) {
		//Recalculate back Y-scaled values.
		if (isYscaled) {
			int[] v = data.getValues(yIndex);
			for (int j = 0; j < v.length; j++) {
				data.setData((int) (v[j]/yScale), yIndex, j);
			}
		}
		this.data = d;
		if (data != null) {
			this.isDetailsMode = data.isDetailsMode();
			selectionDrawer.hidePanel();
			maxValueCalculated = 0;
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

			if (isYscaled) {
				yScale = 1;
				yIndex = 0;
				updateYline();
			}
			calculateMaxValuesLine();
			calculateSumsLine();
			calculateMaxValue2(false, false);

			chartArray = new float[data.getLength() * 4];
			if (data.isPercentage()) {
				chartArray2 = new float[data.getLength() * 4];
			} else {
				chartArray2 = null;
			}
		}
		invalidate();
	}

	private int cType;
	private float prevMax;
	private float prevMin;
	private int end;
	private int calcSum;
	private int calcJ;
	private int calcI;

	//TODO: optimize this method. There max val should not use all values to calculate max.
	private void calculateMaxValue2(boolean linearAnim, boolean animate) {
		//TODO: need better solution for type
//		int type = data.getType(0);
		cType = data.getType(0);
//		float prevMax = maxValueCalculated;
		prevMax = maxValueCalculated;
//		float prevMin = minValueCalculated;
		prevMin = minValueCalculated;
		maxValueCalculated = 0;
		if (cType == ChartData.TYPE_LINE) {
			minValueCalculated = Float.MAX_VALUE;
		} else {
			minValueCalculated = 0;
		}
//		int end = (int) ((scrollPos + WIDTH) / STEP);
		end = (int) ((scrollPos + WIDTH) / STEP);
//		int j;
//		int sum=0;
		calcSum=0;

		for (calcI = (int) (scrollPos / STEP); calcI < end; calcI++) {
			if (!data.isStacked()) {
				if (calcI >= 0 && calcI < maxValuesLine.length && maxValuesLine[calcI] > maxValueCalculated) {
					maxValueCalculated = maxValuesLine[calcI];
				}
				if (cType == ChartData.TYPE_LINE && calcI >= 0 && calcI < minValuesLine.length && minValuesLine[calcI] < minValueCalculated) {
					minValueCalculated = minValuesLine[calcI];
				}
			} else {
				if (calcI >= 0 && calcI < maxValuesLine.length) {
					for (calcJ = 0; calcJ < data.getLinesCount(); calcJ++) {
						if (linesCalculated[calcJ]) {
							calcSum += data.getVal(calcJ, calcI);
						}
					}
					if (calcSum > maxValueCalculated) {
						maxValueCalculated = calcSum;
					}
					calcSum = 0;
				}
			}
		}
		if (prevMax != maxValueCalculated) {
			if (animate) {
				heightAnimator(maxValueCalculated - maxValueVisible, linearAnim);
			} else {
				maxValueCalculated = (int) adjustToGrid(maxValueCalculated, GRID_LINES_COUNT);
			}
		}
		if (prevMin != minValueCalculated) {
			if (animate) {
				minHeightAnimator(minValueCalculated - minValueVisible);
			} else {
				minValueVisible = minValueCalculated;
			}
		}
		if ((prevMax != maxValueCalculated || prevMin != minValueCalculated)&& !animate) {
			gridValueStep = (maxValueCalculated-minValueCalculated) / GRID_LINES_COUNT;
			maxValueVisible = maxValueCalculated;
			updateValueScale();
			gridStep = gridValueStep * valueScale;
			if (gridStep < 40*DENSITY) { gridStep = 40*DENSITY;}
			updateGrid();
			invalidate();
		}
	}

	//TODO: Need optimisation.
	private void calculateArcs() {
		if (data.isPercentage() && isDetailsMode) {
			if (!isMoveAnimation && (moveValY != 0 || moveValX != 0)) {
				moveAnimation(PADD_NORMAL, 0);
			}
			end = (int) ((scrollPos + WIDTH) / STEP);
			totalAdcSum = 0;
			if (arcSums == null || arcSums.length == 0) {
				arcSums = new float[data.getLinesCount()];
			}
			for (int i = 0; i < arcSums.length; i++) {
				arcSums[i] = 0;
			}
			for (calcI = (int) (scrollPos / STEP); calcI < end; calcI++) {
				if (calcI >= 0 && calcI < maxValuesLine.length) {
					for (calcJ = 0; calcJ < data.getLinesCount(); calcJ++) {
						if (linesVisibility[calcJ]) {
							if (isAnimating && calcJ == amnimItemIndex) {
								totalAdcSum += data.getVal(calcJ, calcI) * scaleKoef;
								arcSums[calcJ] += data.getVal(calcJ, calcI) * scaleKoef;
							} else {
								totalAdcSum += data.getVal(calcJ, calcI);
								arcSums[calcJ] += data.getVal(calcJ, calcI);
							}
						}
					}
				}
			}
			if (totalAdcSum > 0) {
				for (int i = 0; i < arcSums.length; i++) {
					arcSums[i] = 360 * arcSums[i] / totalAdcSum;
				}
			}
		}
	}

	private void updateValueScale() {
		if (maxValueVisible - minValueVisible > 0) {
			valueScale = (HEIGHT - HEIGHT_PADDS) / (maxValueVisible - minValueVisible);
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
		minValuesLine = new int[data.getLength()];
		int max;
		int min;
		for (int i = 0; i < data.getLength(); i++) {
			max = 0;
			min = Integer.MAX_VALUE;
			for (int j = 0; j < data.getLinesCount(); j++) {
				if (linesCalculated[j]) {
					if (i < data.getLength() && data.getValues(j)[i] > max) {
						max = data.getValues(j)[i];
					}
					if (i < data.getLength() && data.getValues(j)[i] < min) {
						min = data.getValues(j)[i];
					}
				}
			}
			maxValuesLine[i] = max;
			minValuesLine[i] = min;
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
//		long[] arcSums = new long[vals.length];
//		for (int i = 0; i < vals[0].length; i++) {
//			for (int j = 0; j < vals.length; j++) {
//				arcSums[j] += vals[j][i];
//			}
//		}
//		for (int i = 0; i < arcSums.length; i++) {
//			order.put(arcSums[i], i);
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

//	public void setOnMoveEventsListener(OnMoveEventsListener onMoveEventsListener) {
//		this.onMoveEventsListener = onMoveEventsListener;
//	}

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

	public void setOnDetailsListener(OnDetailsListener onDetailsListener) {
		this.onDetailsListener = onDetailsListener;
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
		ss.minValueVisible= minValueVisible;
		ss.minValueCalculated = minValueCalculated;
		ss.maxValuesLine = maxValuesLine;
		ss.minValuesLine = minValuesLine;
		ss.gridValueStep = gridValueStep;
		ss.dateRange = dateRange;
		ss.gridCount = gridCount;
		ss.gridScale = gridScale;
		ss.gridStep = gridStep;
		ss.dateRangeHeight = dateRangeHeight;
		ss.data = data;
		ss.yScale = yScale;
		ss.yIndex = yIndex;
		ss.scale = scale;
		ss.isYscaled = isYscaled;
		ss.isDetailsMode = isDetailsMode;
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
		minValueVisible = ss.minValueVisible;
		minValueCalculated = ss.minValueCalculated;
		maxValuesLine = ss.maxValuesLine;
		minValuesLine = ss.minValuesLine;
		gridValueStep = ss.gridValueStep;
		dateRangeHeight = ss.dateRangeHeight;
		dateRange = ss.dateRange;
		gridCount = ss.gridCount;
		gridScale = ss.gridScale;
		gridStep = ss.gridStep;
		data = ss.data;
		yScale = ss.yScale;
		yIndex = ss.yIndex;
		scale = ss.scale;
		isYscaled = ss.isYscaled;
		isDetailsMode = ss.isDetailsMode;
		sumVals = ss.sumVals;

		if (data != null) {
			selectionDrawer.setLinesCount(data.getLinesCount());
			linePaints = new Paint[data.getLinesCount()];
			for (int i = 0; i < data.getLinesCount(); i++) {
				linePaints[i] = createLinePaint(data.getColorsInts()[i], data.getType(i) == ChartData.TYPE_BAR);
			}
			chartArray = new float[data.getLength() * 4];
			chartArray2 = new float[data.getLength() * 4];
		}
		selectionDrawer.hidePanel();
	}

//	public interface OnMoveEventsListener {
//		void disallowTouchEvent();
//		void allowTouchEvent();
//	}

	static class SavedState extends View.BaseSavedState {
		SavedState(Parcelable superState) {
			super(superState);
		}

		private SavedState(Parcel in) {
			super(in);
			in.readBooleanArray(linesVisibility);
			in.readBooleanArray(linesCalculated);
			boolean[] bools = new boolean[2];
			in.readBooleanArray(bools);
			isYscaled = bools[0];
			isDetailsMode = bools[1];
			float[] floats = new float[14];
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
			minValueCalculated = floats[12];
			minValueVisible = floats[13];
			gridCount = in.readInt();
			yIndex = in.readInt();
			scale = in.readInt();
			in.readIntArray(maxValuesLine);
			in.readIntArray(minValuesLine);
			in.readFloatArray(sumVals);
			dateRange = in.readString();
			data = in.readParcelable(ChartData.class.getClassLoader());
		}

		@Override
		public void writeToParcel(Parcel out, int flags) {
			super.writeToParcel(out, flags);
			out.writeBooleanArray(linesVisibility);
			out.writeBooleanArray(linesCalculated);
			out.writeBooleanArray(new boolean[] {isYscaled, isDetailsMode});
			out.writeFloatArray(new float[] {scrollPos, scrollIndex, selectionX,
					valueScale, STEP, maxValueVisible, maxValueCalculated, gridValueStep,
					dateRangeHeight, gridScale, gridStep, yScale, minValueCalculated, minValueVisible});
			out.writeInt(gridCount);
			out.writeInt(yIndex);
			out.writeInt(scale);
			out.writeIntArray(maxValuesLine);
			out.writeIntArray(minValuesLine);
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
		float minValueVisible = 0;
		float minValueCalculated = 0;
		int[] maxValuesLine;
		int[] minValuesLine;
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
		boolean isDetailsMode;
		int scale = 1;

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

	public interface OnDetailsListener {
		void showDetails(int num, long time);
		void hideDetails(int num);
	}
}
