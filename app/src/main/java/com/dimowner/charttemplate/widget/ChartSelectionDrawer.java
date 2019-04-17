package com.dimowner.charttemplate.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

import com.dimowner.charttemplate.R;
import com.dimowner.charttemplate.model.ChartData;
import com.dimowner.charttemplate.util.AndroidUtils;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

//import timber.log.Timber;

public class ChartSelectionDrawer {

	private final float DENSITY;
	private final int PADD_XNORMAL;
	private final int PADD_NORMAL;
	private final int PADD_SMALL;
	private final int PADD_XSMALL;
	private final int PADD_TINY;
	private final int ARROW_LENGTH;
	private final int BASE_LINE_Y;
	private final int CIRCLE_SIZE;
	private final int RADIUS;

	{
		DENSITY = AndroidUtils.dpToPx(1);
		PADD_NORMAL = (int) (16*DENSITY);
		PADD_XNORMAL = (int) (12*DENSITY);
		PADD_SMALL = (int) (8*DENSITY);
		PADD_XSMALL = (int) (10*DENSITY);
		PADD_TINY = (int) (4*DENSITY);
		ARROW_LENGTH = (int) (4.5*DENSITY);
		BASE_LINE_Y = (int) (32*DENSITY);
		CIRCLE_SIZE = (int) (4.5f*DENSITY);
		RADIUS = (int) (6*DENSITY);
	}

	DecimalFormat format;

	private TextPaint selectedDatePaint;
	private TextPaint selectedNamePaint;
	private TextPaint selectedValuePaint;
	private TextPaint percentsPaint;
	private Paint panelPaint;
	private Paint scrubblerPaint;
	private Paint circlePaint;
	private Paint shadowPaint;
	private Paint arrowPaint;

	private int overlayColor;
	private int panelColor;

	private float[] selectedValues;
	private String[] formattedValues;
	private String[] formattedPrecents;

	private float selectedDateHeight = 0;
	private float selectedDateWidth = 0;
	private float selectedNameHeight = 0;
	private float maxRowWidth = 0;
	private float percentWidth = 0;

	private float selectionX;
	private float scrollPos;
	private int selectionIndex;
	private String selectionDate;

	private RectF sizeRect;
	private Rect tempRect;
	private float tempWidth = 0;
	private float STEP = 10;
	private boolean show = false;

//	private View view;

	private ValueAnimator alphaAnimator;
	private DecelerateInterpolator decelerateInterpolator = new DecelerateInterpolator();
	private AccelerateInterpolator accelerateInterpolator= new AccelerateInterpolator();

	private boolean showAnimation = false;
	private int alpha = 255;

	private InvalidateIlstener invalidateIlstener;

	public void setInvalidateIlstener(InvalidateIlstener invalidateIlstener) {
		this.invalidateIlstener = invalidateIlstener;
	}

	public interface InvalidateIlstener {
		void onInvalidate();
	}

//	private Typeface boldTypeface;
//	private Typeface normalTypeface;

	ValueAnimator.AnimatorUpdateListener alphaValueAnimator = new ValueAnimator.AnimatorUpdateListener() {
		@Override
		public void onAnimationUpdate(ValueAnimator animation) {
			float val = (float) animation.getAnimatedValue();
			alpha = (int) val;

			selectedDatePaint.setAlpha(alpha);
			selectedNamePaint.setAlpha(alpha);
			selectedValuePaint.setAlpha(alpha);
			panelPaint.setAlpha(alpha);
//			scrubblerPaint.setAlpha(alpha);
//			circlePaint.setAlpha(alpha);
			shadowPaint.setAlpha(alpha);
//			selectedDatePaint.setAlpha((int)val);
			if (!showAnimation && val == 0) {
				show = false;
				selectionX = -1;
			}
//			TODO:  fix this NULL
//			if (view != null) {
//				view.invalidate();
//			}
			if (invalidateIlstener != null) {
				invalidateIlstener.onInvalidate();
			}
		}
	};

//	public void setView(View view) {
//		this.view = view;
//	}

	public void setLinesCount(int count) {
		selectedValues = new float[count];
		formattedValues = new String[count];
		formattedPrecents = new String[count];
		for (int i = 0; i < count; i++) {
			selectedValues[i] = 0;
			formattedValues[i] = "";
			formattedPrecents[i] = "";
		}
	}

	public ChartSelectionDrawer(Context context, int panelTextColor, int arrowColor, int panelColor,
										 int scrubblerColor, int shadowColor, int windowBgColor, int overlayColor) {

		this.panelColor = panelColor;
		this.overlayColor = overlayColor;
		sizeRect = new RectF();
		tempRect = new Rect();
		selectionDate = "";
		selectionX = -1;
		selectionIndex = -1;

//		normalTypeface = Typeface.create("sans-serif", Typeface.NORMAL);
//		boldTypeface = Typeface.create("sans-serif", Typeface.BOLD);

		selectedDatePaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
		selectedDatePaint.setColor(panelTextColor);
		selectedDatePaint.setTextAlign(Paint.Align.LEFT);
//		selectedDatePaint.setAlpha(50);
		selectedDatePaint.setTypeface(Typeface.create("sans-serif-sans-serif-thin", Typeface.BOLD));
		selectedDatePaint.setTextSize(context.getResources().getDimension(R.dimen.text_xnormal));

		selectedNamePaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
		selectedNamePaint.setTextAlign(Paint.Align.LEFT);
		selectedNamePaint.setColor(panelTextColor);
		selectedNamePaint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
		selectedNamePaint.setTextSize(context.getResources().getDimension(R.dimen.text_normal));

		selectedValuePaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
		selectedValuePaint.setTextAlign(Paint.Align.RIGHT);
		selectedValuePaint.setColor(panelTextColor);
		selectedValuePaint.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
		selectedValuePaint.setTextSize(context.getResources().getDimension(R.dimen.text_normal));

		percentsPaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
		percentsPaint.setTextAlign(Paint.Align.LEFT);
		percentsPaint.setColor(panelTextColor);
		percentsPaint.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
		percentsPaint.setTextSize(context.getResources().getDimension(R.dimen.text_normal));

		panelPaint = new Paint();
		panelPaint.setStyle(Paint.Style.FILL);
		panelPaint.setColor(panelColor);
		panelPaint.setAntiAlias(true);
//		panelPaint.setShadowLayer(SHADOW_SIZE, 0, 0, context.getResources().getColor(R.color.shadow));

		circlePaint = new Paint();
		circlePaint.setStyle(Paint.Style.FILL);
		circlePaint.setColor(windowBgColor);

		scrubblerPaint = new Paint();
		scrubblerPaint.setAntiAlias(false);
		scrubblerPaint.setStyle(Paint.Style.STROKE);
		scrubblerPaint.setColor(scrubblerColor);
		scrubblerPaint.setStrokeWidth(1.2f*DENSITY);

		shadowPaint = new Paint();
		shadowPaint.setAntiAlias(true);
		shadowPaint.setStyle(Paint.Style.STROKE);
		shadowPaint.setColor(shadowColor);
		shadowPaint.setStrokeWidth(DENSITY);

		arrowPaint = new Paint();
		arrowPaint.setAntiAlias(true);
		arrowPaint.setStyle(Paint.Style.STROKE);
		arrowPaint.setColor(arrowColor);
		arrowPaint.setStrokeWidth(1.6f*DENSITY);
		arrowPaint.setStrokeCap(Paint.Cap.ROUND);

		DecimalFormatSymbols formatSymbols = new DecimalFormatSymbols(Locale.getDefault());
      formatSymbols.setDecimalSeparator('.');
      formatSymbols.setGroupingSeparator(' ');
		format = new DecimalFormat("###,###.#", formatSymbols);
	}

	public void drawBarOverlay(Canvas canvas, int type, float STEP, float H1, float WIDTH, float HEIGHT) {
		if (show && type == ChartData.TYPE_BAR) {
			float x = selectionIndex*STEP-scrollPos;
			float half = (STEP-1)/2;
			panelPaint.setColor(overlayColor);
			panelPaint.setAlpha(alpha > 127 ? 127 : alpha);
			canvas.drawRect(-PADD_NORMAL, 0, x-half, H1 + PADD_XNORMAL, panelPaint);
			canvas.drawRect(x+half, 0, WIDTH + PADD_NORMAL, H1 + PADD_XNORMAL, panelPaint);
		}
	}

	public void draw(Canvas canvas, ChartData data, boolean[] linesVisibility, float HEIGHT, int[] linesAlpha,
						  Paint linePaint, float valueScaleY, float minValVisible) {
		if (show) {
			if (data.getType(0) == ChartData.TYPE_LINE || data.getType(0) == ChartData.TYPE_AREA) {
				//Draw scrubbler
				canvas.drawLine(selectionX, BASE_LINE_Y + PADD_XNORMAL, selectionX, HEIGHT - BASE_LINE_Y, scrubblerPaint);
			}
			//Draw circles on charts
			for (int i = 0; i < data.getLinesCount(); i++) {
				if (data.getType(i) == ChartData.TYPE_LINE) {
					if (linesVisibility[i]) {
						canvas.drawCircle(selectionX,
								HEIGHT - BASE_LINE_Y - (selectedValues[i]-minValVisible) * valueScaleY,
								CIRCLE_SIZE, circlePaint);
						linePaint.setColor(data.getColor(i));
						canvas.drawCircle(selectionX,
								HEIGHT - BASE_LINE_Y - (selectedValues[i]-minValVisible) * valueScaleY,
								CIRCLE_SIZE, linePaint);
					}
				}
			}
			panelPaint.setColor(panelColor);
			panelPaint.setAlpha(alpha);
			//Draw selection panel
			canvas.drawRoundRect(sizeRect, RADIUS, RADIUS, panelPaint);
			canvas.drawRoundRect(sizeRect, RADIUS, RADIUS, shadowPaint);

			//Draw arrow icon.
			canvas.drawLine(sizeRect.right - PADD_XNORMAL, sizeRect.top + selectedDateHeight/2+ PADD_XNORMAL,
					sizeRect.right - PADD_XNORMAL-ARROW_LENGTH, sizeRect.top + selectedDateHeight/2 + PADD_XNORMAL-ARROW_LENGTH,
					arrowPaint);
			canvas.drawLine(sizeRect.right - PADD_XNORMAL, sizeRect.top + selectedDateHeight/2+ PADD_XNORMAL,
					sizeRect.right - PADD_XNORMAL-ARROW_LENGTH, sizeRect.top + selectedDateHeight/2+ARROW_LENGTH + PADD_XNORMAL,
					arrowPaint);

			//Draw date on panel
			canvas.drawText(selectionDate, sizeRect.left+ PADD_XNORMAL,
					sizeRect.top+selectedDateHeight+ PADD_XNORMAL, selectedDatePaint);
			int count = 0;
			//Draw names and values on panel
			for (int i = 0; i < data.getLinesCount(); i++) {
				if (linesVisibility[i]) {
					if (data.isPercentage()) {
						//Draw percents
						canvas.drawText(formattedPrecents[i],
								sizeRect.left + PADD_XNORMAL,
								sizeRect.top + selectedDateHeight + PADD_XSMALL + 2 * PADD_XNORMAL + PADD_TINY + selectedNameHeight * count, percentsPaint);
						//Draw names
						canvas.drawText(data.getName(i),
								sizeRect.left + PADD_XNORMAL + percentWidth + PADD_SMALL,
								sizeRect.top + selectedDateHeight + PADD_XSMALL + 2 * PADD_XNORMAL + PADD_TINY + selectedNameHeight * count, selectedNamePaint);
					} else {
						//Draw names
						canvas.drawText(data.getName(i),
								sizeRect.left + PADD_XNORMAL,
								sizeRect.top + selectedDateHeight + PADD_SMALL + 2 * PADD_XNORMAL + PADD_TINY + selectedNameHeight * count, selectedNamePaint);
					}
					selectedValuePaint.setColor(data.getColor(i));
					selectedValuePaint.setAlpha(alpha);
//					canvas.drawText(String.valueOf(((int)selectedValues[i])),
					//Draw values
					canvas.drawText(formattedValues[i],
							sizeRect.right - PADD_XNORMAL,
							sizeRect.top+selectedDateHeight + PADD_SMALL+2* PADD_XNORMAL +PADD_TINY + selectedNameHeight*count,
							selectedValuePaint);
					count++;
				}
			}
		}
	}

	public boolean checkCoordinateInPanel(float x, float y) {
		return x > sizeRect.left && x < sizeRect.right && y > sizeRect.top && y < sizeRect.bottom;
	}

	public void reset() {
		selectedDateHeight = 0;
		selectedDateWidth = 0;
		selectedNameHeight = 0;
		maxRowWidth = 0;
		percentWidth = 0;
	}

	public void calculatePanelSize(ChartData data, float STEP, boolean[] linesCalculated,
											 float scrollPos, float WIDTH, boolean isYscale, int yIndex,
											 float yScale, float[] sumVals) {
		this.STEP = STEP;
		this.scrollPos = scrollPos;
		selectionIndex = (int)((scrollPos + selectionX)/STEP);
		if (selectionIndex >= data.getLength()-1) {
			selectionIndex = data.getLength()-1;
		}

		int visibleLinesCount = 0;
		for (int i = 0; i < data.getLinesCount(); i++) {
			if (linesCalculated[i]) {
				visibleLinesCount++;
				if (selectionIndex + 1 < data.getLength()) {
					//Interpolate intermediate Y val for each line.
					selectedValues[i] = calculateValY(
							scrollPos + selectionX,  //X
							selectionIndex * STEP, //X1
							(selectionIndex + 1) * STEP, //X2
							data.getValues(i)[selectionIndex], //Y1
							data.getValues(i)[selectionIndex + 1] //Y2
					);
				}
				//Name height and width
				selectedNamePaint.getTextBounds(data.getName(i), 0, data.getName(i).length(), tempRect);
				tempWidth = tempRect.width();
				if (selectedNameHeight < tempRect.height()+PADD_XSMALL) selectedNameHeight = tempRect.height()+PADD_XSMALL;

//				val = String.valueOf((data.getValues(i)[selectionIndex]));
				if (data.isPercentage()) {
					formattedPrecents[i] = format.format(data.getValues(i)[selectionIndex]/sumVals[selectionIndex])+"%";
					selectedValuePaint.getTextBounds(formattedPrecents[i], 0, formattedPrecents[i].length(), tempRect);
					percentWidth = percentWidth < tempRect.width() ? tempRect.width() : percentWidth;
				} else {
					percentWidth = 0;
				}
				if (isYscale && i == yIndex) {
					formattedValues[i] = format.format(data.getValues(i)[selectionIndex]/yScale);
				} else {
					formattedValues[i] = format.format(data.getValues(i)[selectionIndex]);
				}

				//Value height and width
//				selectedValuePaint.getTextBounds(val, 0, val.length(), tempRect);
				selectedValuePaint.getTextBounds(formattedValues[i], 0, formattedValues[i].length(), tempRect);
				maxRowWidth = maxRowWidth < tempWidth + tempRect.width() + percentWidth
						? tempWidth + tempRect.width() + percentWidth : maxRowWidth;
			}
		}

		//Calculate date sizes
//		date.setTime(data.getTimes()[selectionIndex]);
		selectionDate = data.getTime(selectionIndex);//String.valueOf(date.getTime()/1000000);//TimeUtils.formatDateWeek(date);
		selectedDatePaint.getTextBounds(selectionDate, 0, selectionDate.length(), tempRect);

		if (selectedDateHeight < tempRect.height()) {
			selectedDateHeight = tempRect.height();
		}
		if (selectedDateWidth < tempRect.width()) {
			selectedDateWidth = tempRect.width();
		}

		float width = 2*PADD_XNORMAL + selectedDateWidth + 24*DENSITY; //30dp is space for arrow icon
		if (width < maxRowWidth+4* PADD_XNORMAL) {
			width = maxRowWidth+4* PADD_XNORMAL;
		}

		sizeRect.left = selectionX - width - PADD_SMALL - STEP;
		sizeRect.right = selectionX - PADD_SMALL - STEP;
		sizeRect.top = BASE_LINE_Y + 2* PADD_XNORMAL;
		sizeRect.bottom = BASE_LINE_Y + 4.5f * PADD_XNORMAL + selectedDateHeight + visibleLinesCount*selectedNameHeight;

		//Set Panel edges
		if (sizeRect.right > WIDTH - PADD_TINY) {
			float w = sizeRect.width();
			sizeRect.right = WIDTH - PADD_TINY;
			sizeRect.left = WIDTH - PADD_TINY - w;
		}
		if (sizeRect.left < 0) {
			float w = sizeRect.width();
			sizeRect.left = 0;
			sizeRect.right = 0 + w;
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

	public void setSelectionX(float selectionX) {
		this.selectionX = selectionX;
	}

	public boolean isShowPanel() {
		return show;
	}

	public float getSelectionX() {
		return selectionX;
	}

	public int getSelectionIndex() {
		return selectionIndex;
	}

	public void setScrollPos(float index, float ST) {
		float i = selectionX/STEP;
		selectionX = i*ST;

		float dx = index - (selectionIndex-i);
		selectionX -= dx*ST;
		sizeRect.left = selectionX - sizeRect.width() - PADD_XNORMAL;
		sizeRect.right = selectionX - PADD_XNORMAL;
		this.scrollPos = index*ST;
		STEP = ST;
	}

	public void showPanel() {
		this.show = true;
		alphaAnimator(0, 255, true);
	}

	public void hidePanel() {
		alphaAnimator(255, 0, false);
	}

	private void alphaAnimator(float start, final float end, boolean show) {
		showAnimation = show;
		if (alphaAnimator != null && alphaAnimator.isStarted()) {
			alphaAnimator.cancel();
		}
		alphaAnimator = ValueAnimator.ofFloat(start, end);
		if (show) {
			alphaAnimator.setInterpolator(decelerateInterpolator);
		} else {
			alphaAnimator.setInterpolator(accelerateInterpolator);
		}
		alphaAnimator.setDuration(150);
		alphaAnimator.addUpdateListener(alphaValueAnimator);
		alphaAnimator.start();
	}
}
