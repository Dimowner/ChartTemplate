package com.dimowner.charttemplate.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.TextPaint;

import com.dimowner.charttemplate.R;
import com.dimowner.charttemplate.model.ChartData;
import com.dimowner.charttemplate.util.AndroidUtils;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class ChartSelectionDrawer {

	private final float DENSITY;
	private final int PADD_XNORMAL;
	private final int PADD_NORMAL;
	private final int PADD_SMALL;
	private final int PADD_TINY;
	private final int BASE_LINE_Y;
	private final int CIRCLE_SIZE;
	private final int RADIUS;

	{
		DENSITY = AndroidUtils.dpToPx(1);
		PADD_NORMAL = (int) (16*DENSITY);
		PADD_XNORMAL = (int) (12*DENSITY);
		PADD_SMALL = (int) (8*DENSITY);
		PADD_TINY = (int) (4*DENSITY);
		BASE_LINE_Y = (int) (32*DENSITY);
		CIRCLE_SIZE = (int) (5*DENSITY);
		RADIUS = (int) (6*DENSITY);
	}

	DecimalFormat format;

	private TextPaint selectedDatePaint;
	private TextPaint selectedNamePaint;
	private TextPaint selectedValuePaint;
	private Paint panelPaint;
	private Paint scrubblerPaint;
	private Paint circlePaint;
	private Paint shadowPaint;

	private int overlayColor;
	private int panelColor;

	private float[] selectedValues;
	private String[] formattedValues;

	private float selectedDateHeight = 0;
	private float selectedDateWidth = 0;
	private float selectedNameHeight = 0;
	private float maxRowWidth = 0;

	private float selectionX;
	private float scrollPos;
	private int selectionIndex;
	private String selectionDate;

	private RectF sizeRect;
	private Rect tempRect;
	private float tempWidth = 0;

	public void setLinesCount(int count) {
		selectedValues = new float[count];
		formattedValues = new String[count];
		for (int i = 0; i < count; i++) {
			selectedValues[i] = 0;
		}
	}

	public ChartSelectionDrawer(Context context, int panelTextColor, int panelColor,
										 int scrubblerColor, int shadowColor, int windowBgColor, int overlayColor) {

		this.panelColor = panelColor;
		this.overlayColor = overlayColor;
		sizeRect = new RectF();
		tempRect = new Rect();
		selectionDate = "";
		selectionX = -1;
		selectionIndex = -1;

		selectedDatePaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
		selectedDatePaint.setColor(panelTextColor);
		selectedDatePaint.setTextAlign(Paint.Align.LEFT);
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

		panelPaint = new Paint();
		panelPaint.setStyle(Paint.Style.FILL);
		panelPaint.setColor(panelColor);
		panelPaint.setAntiAlias(true);
//		panelPaint.setShadowLayer(SHADOW_SIZE, 0, 0, context.getResources().getColor(R.color.shadow));

		circlePaint = new Paint();
		circlePaint.setStyle(Paint.Style.FILL);
		circlePaint.setColor(panelColor);

		scrubblerPaint = new Paint();
		scrubblerPaint.setAntiAlias(false);
		scrubblerPaint.setDither(false);
		scrubblerPaint.setStyle(Paint.Style.STROKE);
		scrubblerPaint.setColor(scrubblerColor);
		scrubblerPaint.setStrokeWidth(1.2f*DENSITY);

		shadowPaint = new Paint();
		shadowPaint.setAntiAlias(true);
		shadowPaint.setDither(false);
		shadowPaint.setStyle(Paint.Style.STROKE);
		shadowPaint.setColor(shadowColor);
		shadowPaint.setStrokeWidth(DENSITY);

		DecimalFormatSymbols formatSymbols = new DecimalFormatSymbols(Locale.getDefault());
      formatSymbols.setDecimalSeparator('.');
      formatSymbols.setGroupingSeparator(' ');
		format = new DecimalFormat("###,###.#", formatSymbols);
	}

	public void drawBarOverlay(Canvas canvas, int type, float STEP, float H1, float WIDTH, float HEIGHT) {
		if (selectionX >= 0 && type == ChartData.TYPE_BAR) {
			float x = selectionIndex*STEP-scrollPos;
			float half = (STEP-1)/2;
			panelPaint.setColor(overlayColor);
			canvas.drawRect(-PADD_NORMAL, H1, x-half, HEIGHT - H1 + PADD_XNORMAL, panelPaint);
			canvas.drawRect(x+half, H1, WIDTH + PADD_NORMAL, HEIGHT - H1 + PADD_XNORMAL, panelPaint);
		}
	}

	public void draw(Canvas canvas, ChartData data, boolean[] linesVisibility, float HEIGHT,
						  Paint[] linePaints, float valueScaleY) {
		if (selectionX >= 0) {
			if (data.getType(0) == ChartData.TYPE_LINE || data.getType(0) == ChartData.TYPE_AREA) {
				//Draw scrubbler
				canvas.drawLine(selectionX, BASE_LINE_Y + PADD_XNORMAL, selectionX, HEIGHT - BASE_LINE_Y, scrubblerPaint);
			}
			//Draw circles on charts
			for (int i = 0; i < data.getLinesCount(); i++) {
				if (data.getType(i) == ChartData.TYPE_LINE) {
					if (linesVisibility[i]) {
						canvas.drawCircle(selectionX,
								HEIGHT - BASE_LINE_Y - selectedValues[i] * valueScaleY,
								CIRCLE_SIZE, circlePaint);
						canvas.drawCircle(selectionX,
								HEIGHT - BASE_LINE_Y - selectedValues[i] * valueScaleY,
								CIRCLE_SIZE, linePaints[i]);
					}
				}
			}
			panelPaint.setColor(panelColor);
			//Draw selection panel
			canvas.drawRoundRect(sizeRect, RADIUS, RADIUS, panelPaint);
			canvas.drawRoundRect(sizeRect, RADIUS, RADIUS, shadowPaint);
			//Draw date on panel
			canvas.drawText(selectionDate, sizeRect.left+ PADD_XNORMAL,
					sizeRect.top+selectedDateHeight+ PADD_XNORMAL, selectedDatePaint);
			int count = 0;
			//Draw names and values on panel
			for (int i = 0; i < data.getLinesCount(); i++) {
				if (linesVisibility[i]) {
					canvas.drawText(data.getNames()[i],
							sizeRect.left+ PADD_XNORMAL,
							sizeRect.top+selectedDateHeight + PADD_SMALL+2* PADD_XNORMAL +PADD_TINY + selectedNameHeight*count, selectedNamePaint);
					selectedValuePaint.setColor(data.getColorsInts()[i]);
//					canvas.drawText(String.valueOf(((int)selectedValues[i])),
					canvas.drawText(formattedValues[i],
							sizeRect.right- PADD_XNORMAL,
							sizeRect.top+selectedDateHeight + PADD_SMALL+2* PADD_XNORMAL +PADD_TINY + selectedNameHeight*count,
							selectedValuePaint);
					count++;
				}
			}
		}
	}

	public void reset() {
		selectedDateHeight = 0;
		selectedDateWidth = 0;
		selectedNameHeight = 0;
		maxRowWidth = 0;
	}

	public void calculatePanelSize(ChartData data, float STEP, boolean[] linesCalculated,
											  float scrollPos, float WIDTH, boolean isYscale, int yIndex, float yScale) {
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
				selectedNamePaint.getTextBounds(data.getNames()[i], 0, data.getNames()[i].length(), tempRect);
				tempWidth = tempRect.width();
				if (selectedNameHeight < tempRect.height()+PADD_SMALL) selectedNameHeight = tempRect.height()+PADD_SMALL;

//				val = String.valueOf((data.getValues(i)[selectionIndex]));
				if (isYscale && i == yIndex) {
					formattedValues[i] = format.format(data.getValues(i)[selectionIndex]/yScale);
				} else {
					formattedValues[i] = format.format(data.getValues(i)[selectionIndex]);
				}

				//Value height and width
//				selectedValuePaint.getTextBounds(val, 0, val.length(), tempRect);
				selectedValuePaint.getTextBounds(formattedValues[i], 0, formattedValues[i].length(), tempRect);
				maxRowWidth = maxRowWidth < tempWidth + tempRect.width() ? tempWidth + tempRect.width() : maxRowWidth;
			}
		}

		//Calculate date sizes
//		date.setTime(data.getTimes()[selectionIndex]);
		selectionDate = data.getTimes()[selectionIndex];//String.valueOf(date.getTime()/1000000);//TimeUtils.formatDateWeek(date);
		selectedDatePaint.getTextBounds(selectionDate, 0, selectionDate.length(), tempRect);

		if (selectedDateHeight < tempRect.height()) {
			selectedDateHeight = tempRect.height();
		}
		if (selectedDateWidth < tempRect.width()) {
			selectedDateWidth = tempRect.width();
		}

		float width = 2*PADD_XNORMAL + selectedDateWidth + 24*DENSITY;
		if (width < maxRowWidth+3* PADD_XNORMAL) {
			width = maxRowWidth+3* PADD_XNORMAL;
		}

		sizeRect.left = selectionX - width - PADD_XNORMAL;
		sizeRect.right = selectionX - PADD_XNORMAL;
		sizeRect.top = BASE_LINE_Y + 2* PADD_XNORMAL;
		sizeRect.bottom = BASE_LINE_Y + 4.5f * PADD_XNORMAL + selectedDateHeight + visibleLinesCount*selectedNameHeight;

		//Set Panel edges
		if (sizeRect.right > WIDTH - PADD_TINY) {
			float w = sizeRect.width();
			sizeRect.right = WIDTH - PADD_TINY;
			sizeRect.left = WIDTH - PADD_TINY - w;
		}
		if (sizeRect.left < PADD_TINY) {
			float w = sizeRect.width();
			sizeRect.left = PADD_TINY;
			sizeRect.right = PADD_TINY + w;
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

	public float getSelectionX() {
		return selectionX;
	}
}
