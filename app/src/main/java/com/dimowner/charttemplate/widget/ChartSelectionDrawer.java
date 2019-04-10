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
import com.dimowner.charttemplate.util.TimeUtils;

import java.util.Date;

public class ChartSelectionDrawer {

	private final float DENSITY;
	private final int PADD_NORMAL;
	private final int PADD_SMALL;
	private final int PADD_TINY;
	private final int BASE_LINE_Y;
	private final int CIRCLE_SIZE;

	{
		DENSITY = AndroidUtils.dpToPx(1);
		PADD_NORMAL = (int) (16*DENSITY);
		PADD_SMALL = (int) (8*DENSITY);
		PADD_TINY = (int) (4*DENSITY);
		BASE_LINE_Y = (int) (32*DENSITY);
		CIRCLE_SIZE = (int) (5*DENSITY);
	}

	private TextPaint selectedDatePaint;
	private TextPaint selectedNamePaint;
	private TextPaint selectedValuePaint;
	private Paint panelPaint;
	private Paint scrubblerPaint;
	private Paint circlePaint;
	private Paint shadowPaint;

	private float[] selectedValues;

	private float selectedDateHeight = 0;
	private float selectedDateWidth = 0;
	private float selectedNameHeight = 0;
	private float selectedValueHeight = 0;
	private float selectedItemWidth = 0;

	private float selectionX;
	private int selectionIndex;
	private String selectionDate;
//	private Date date;

	private RectF sizeRect;
	private Rect tempRect;

	public void setLinesCount(int count) {
		selectedValues = new float[count];
		for (int i = 0; i < count; i++) {
			selectedValues[i] = 0;
		}
	}

	public ChartSelectionDrawer(Context context, int panelTextColor, int panelColor,
										 int scrubblerColor, int shadowColor, int windowBgColor) {
		sizeRect = new RectF();
		tempRect = new Rect();
//		date = new Date();
		selectionDate = "";
		selectionX = -1;
		selectionIndex = -1;

		selectedDatePaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
		selectedDatePaint.setColor(panelTextColor);
		selectedDatePaint.setTextAlign(Paint.Align.LEFT);
		selectedDatePaint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
		selectedDatePaint.setTextSize(context.getResources().getDimension(R.dimen.text_medium));

		selectedNamePaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
		selectedNamePaint.setTextAlign(Paint.Align.LEFT);
		selectedNamePaint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
		selectedNamePaint.setTextSize(context.getResources().getDimension(R.dimen.text_xmedium));

		selectedValuePaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
		selectedValuePaint.setTextAlign(Paint.Align.LEFT);
		selectedValuePaint.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
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
	}

	public void draw(Canvas canvas, ChartData data, boolean[] linesVisibility, float HEIGHT,
						  Paint[] linePaints, float valueScaleY) {
		if (selectionX >= 0) {
			//Draw scrubbler
			canvas.drawLine(selectionX, BASE_LINE_Y+PADD_NORMAL, selectionX, HEIGHT - BASE_LINE_Y, scrubblerPaint);

			//Draw circles on charts
			for (int i = 0; i < data.getNames().length; i++) {
				if (linesVisibility[i]) {
					canvas.drawCircle(selectionX,
							HEIGHT - BASE_LINE_Y - selectedValues[i] * valueScaleY,
							CIRCLE_SIZE, circlePaint);
					canvas.drawCircle(selectionX,
							HEIGHT - BASE_LINE_Y - selectedValues[i] * valueScaleY,
							CIRCLE_SIZE, linePaints[i]);
				}
			}
			//Draw selection panel
			canvas.drawRoundRect(sizeRect, PADD_TINY, PADD_TINY, panelPaint);
			canvas.drawRoundRect(sizeRect, PADD_TINY, PADD_TINY, shadowPaint);
			//Draw date on panel
			canvas.drawText(selectionDate, sizeRect.left+PADD_NORMAL,
					sizeRect.top+selectedDateHeight+PADD_SMALL, selectedDatePaint);
			int count = 0;
			//Draw names and values on panel
			for (int i = 0; i < data.getNames().length; i++) {
				if (linesVisibility[i]) {
					selectedNamePaint.setColor(data.getColorsInts()[i]);
					canvas.drawText(data.getNames()[i],
							sizeRect.left+ PADD_NORMAL + selectedItemWidth*count + PADD_NORMAL*count,
							sizeRect.top+selectedDateHeight+selectedNameHeight+3*PADD_SMALL, selectedNamePaint);
					selectedValuePaint.setColor(data.getColorsInts()[i]);
					canvas.drawText(String.valueOf(((int)selectedValues[i])),
							sizeRect.left+ PADD_NORMAL + selectedItemWidth*count+ PADD_NORMAL*count,
							sizeRect.top+selectedDateHeight+selectedNameHeight+selectedValueHeight+4*PADD_SMALL,
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
		selectedValueHeight = 0;
		selectedItemWidth = 0;
	}

	public void calculatePanelSize(ChartData data, float STEP, boolean[] linesCalculated,
											  float scrollPos, float WIDTH) {

		selectionIndex = (int)((scrollPos + selectionX)/STEP);
		if (selectionIndex >= data.getLength()-1) {
			selectionIndex = data.getLength()-1;
		}

		String val;
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
				selectedItemWidth = selectedItemWidth < tempRect.width() ? tempRect.width() : selectedItemWidth;
				if (selectedNameHeight < tempRect.height()) selectedNameHeight = tempRect.height();

				val = String.valueOf((data.getValues(i)[selectionIndex]));

				//Value height and width
				selectedValuePaint.getTextBounds(val, 0, val.length(), tempRect);
				selectedItemWidth = selectedItemWidth < tempRect.width() ? tempRect.width() : selectedItemWidth;
				if (selectedValueHeight < tempRect.height()) selectedValueHeight = tempRect.height();
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

		//Calculate result width
		float width = selectedItemWidth*visibleLinesCount + PADD_NORMAL*visibleLinesCount-PADD_SMALL;
		if (selectedDateWidth+PADD_NORMAL > width) {
			width = selectedDateWidth+PADD_NORMAL;
		}

		//Set panel size.
		sizeRect.left = selectionX - width - 2*PADD_NORMAL;
		sizeRect.right = selectionX - PADD_NORMAL;
		sizeRect.top = BASE_LINE_Y + 2*PADD_NORMAL;
		sizeRect.bottom = BASE_LINE_Y + 4.5f * PADD_NORMAL + selectedDateHeight + selectedNameHeight + selectedValueHeight;

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
