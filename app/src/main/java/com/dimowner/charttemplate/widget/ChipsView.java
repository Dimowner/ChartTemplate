/*
 * Copyright 2019 Dmitriy Ponomarenko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain prevDegree copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dimowner.charttemplate.widget;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.dimowner.charttemplate.R;
import com.dimowner.charttemplate.util.AndroidUtils;

import java.util.ArrayList;
import java.util.List;

public class ChipsView extends LinearLayout {

	private final float DENSITY;
	private final int PADD_TINY;
	private final int PADD_SMALL;
	private final int PADD_NORMAL;
	private final float RAD;
	private final int BORDER;
	private final int NO_ICON_PADD;
	private final int CHIP_HEIGHT;

//	private final float SELECTION;

	{
		DENSITY = AndroidUtils.dpToPx(1);
//		SELECTION = 5*DENSITY;
		PADD_TINY = (int)(4*DENSITY);
		PADD_SMALL = (int)(8*DENSITY);
		PADD_NORMAL = (int)(16*DENSITY);
		RAD = 24*DENSITY;
		BORDER = (int)(2*DENSITY);
		NO_ICON_PADD = (int)(28*DENSITY);
		CHIP_HEIGHT = (int)(42*DENSITY);
	}

	private FrameLayout container;
	private boolean[] chipState;
	private String[] names;
	private int[] colors;
//	private int gridTextColor;
	private List<TextView> views;
	private List<Integer> chipsWidth;
	private int totalWidth = 0;
	private float chipsHeight = 0;
	private float shift = 0;
	private float WIDTH = 1;

	private OnCheckListener listener;

	public ChipsView(Context context) {
		super(context);
		init(context);
	}

	public ChipsView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public ChipsView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(context);
	}

	private void init(Context context) {
		chipsWidth = new ArrayList<>();
		views = new ArrayList<>();
//		TypedValue typedValue = new TypedValue();
//		if (context.getTheme().resolveAttribute(R.attr.gridTextColor, typedValue, true)) {
//			gridTextColor = typedValue.data;
//		} else {
//			gridTextColor = context.getResources().getColor(R.color.text_color);
//		}

		container = new FrameLayout(context);
		LinearLayout.LayoutParams containerLp = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);
		containerLp.gravity = Gravity.START;
//		containerLp.topMargin = PADD_NORMAL;
//		containerLp.bottomMargin = PADD_SMALL+PADD_TINY;
		container.setLayoutParams(containerLp);
		this.addView(container);
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		WIDTH = getWidth();
	}

	public TextView createChipView(int id, final Context context, final String name, final int color, boolean checked) {
		final TextView textView = new TextView(context);
		FrameLayout.LayoutParams  lp = new FrameLayout.LayoutParams(
				FrameLayout.LayoutParams .WRAP_CONTENT, CHIP_HEIGHT);
		textView.setLayoutParams(lp);
		textView.setGravity(Gravity.START);
		if (checked) {
			textView.setCompoundDrawablesWithIntrinsicBounds(
					ContextCompat.getDrawable(context, R.drawable.ic_check), null, null, null);
			textView.setCompoundDrawablePadding(PADD_SMALL);
			setChecked(textView, color);
//			textView.setTextColor(textColor);
			textView.setTextColor(ContextCompat.getColor(context, R.color.white));
			textView.setPadding(PADD_SMALL, PADD_SMALL, PADD_NORMAL, PADD_SMALL);
		} else {
			setUnchecked(textView, color);
			textView.setTextColor(color);
//			textView.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
			textView.setPadding(NO_ICON_PADD, PADD_SMALL, NO_ICON_PADD, PADD_SMALL);
		}

		textView.setText(name);
		textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
//		textView.getPaint().getTextBounds();
//		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(textView.getLayoutParams());
//		if (container.getChildCount() == 0) {
//			params.setMargins(0, PADD_NORMAL, 0, PADD_NORMAL);
//		} else {
//			params.setMargins(PADD_SMALL, PADD_NORMAL, 0, PADD_NORMAL);
//		}
//		textView.setLayoutParams(params);

		textView.setId(id);

		textView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (chipState[v.getId()]) {
//					textView.setCompoundDrawablesWithIntrinsicBounds(
//							ContextCompat.getDrawable(context, R.drawable.ic_circle), null, null, null);
//					textView.getCompoundDrawables()[0].mutate();
//					DrawableCompat.setTint(textView.getCompoundDrawables()[0], color);
					setUnchecked(textView, color);
					textView.setTextColor(color);
					textView.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
					textView.setPadding(NO_ICON_PADD, PADD_SMALL, NO_ICON_PADD, PADD_SMALL);
					if (listener != null) {
						listener.onCheck(v.getId(), name, false);
					}
				} else {
//					textView.setCompoundDrawablesWithIntrinsicBounds(
//							ContextCompat.getDrawable(context, R.drawable.ic_check), null, null, null);
//					textView.getCompoundDrawables()[0].mutate();
//					DrawableCompat.setTint(textView.getCompoundDrawables()[0], color);
					textView.setCompoundDrawablesWithIntrinsicBounds(
							ContextCompat.getDrawable(context, R.drawable.ic_check), null, null, null);
					textView.setCompoundDrawablePadding(PADD_SMALL);
					setChecked(textView, color);
					textView.setPadding(PADD_SMALL, PADD_SMALL, PADD_NORMAL, PADD_SMALL);
					textView.setTextColor(ContextCompat.getColor(getContext(), R.color.white));
					if (listener != null) {
						listener.onCheck(v.getId(), name, true);
					}
				}
				chipState[v.getId()] = !chipState[v.getId()];
			}
		});
		return textView;
	}

	public void setChecked(View v, int backgroundColor) {
		GradientDrawable shape = new GradientDrawable();
		shape.setShape(GradientDrawable.RECTANGLE);
		shape.setCornerRadii(new float[] {RAD, RAD, RAD, RAD, RAD, RAD, RAD, RAD});
		shape.setColor(backgroundColor);
		shape.setStroke(BORDER, backgroundColor);
		v.setBackground(shape);
	}

	public void setUnchecked(View v, int borderColor) {
		GradientDrawable shape = new GradientDrawable();
		shape.setShape(GradientDrawable.RECTANGLE);
		shape.setCornerRadii(new float[] {RAD, RAD, RAD, RAD, RAD, RAD, RAD, RAD});
		shape.setColor(ContextCompat.getColor(getContext(), R.color.transparent));
		shape.setStroke(BORDER, borderColor);
		v.setBackground(shape);
	}

	public void setData(String[] names, int[] colors) {
		if (names != null && colors != null) {
			if (WIDTH <= 1) {
				WIDTH = AndroidUtils.getScreenWidth(getContext())-2*PADD_NORMAL;
			}
			this.names = names;
			this.colors = colors;
			chipState = new boolean[names.length];
			if (names.length > 1) {
				views.clear();
				chipsWidth.clear();
				totalWidth = 0;
				chipsHeight = 0;
				shift = 0;
				container.removeAllViews();
				for (int i = 0; i < names.length; i++) {
					chipState[i] = true;
					views.add(createChipView(i, getContext(), names[i], colors[i], true));
					container.addView(views.get(i));
					updatePositions(i);
				}
//				container.requestLayout();
			} else {
				clearChips();
			}
		}
	}

	public void clearChips() {
		views.clear();
		chipsWidth.clear();
		totalWidth = 0;
		chipsHeight = 0;
		shift = 0;
		container.removeAllViews();
		ViewGroup.LayoutParams lp = container.getLayoutParams();
		lp.height = PADD_NORMAL;
		container.setLayoutParams(lp);
	}

	private void updatePositions(int i) {
		//Read view sizes;
		Rect rect = new Rect();
		views.get(i).getPaint().getTextBounds(names[i], 0, names[i].length(), rect);
		int w = 2*NO_ICON_PADD + rect.width() + PADD_SMALL;
		totalWidth +=w;
		chipsWidth.add(w);
		if (chipsHeight == 0) {
			chipsHeight = 2*PADD_NORMAL + rect.height();
		}
		float x = totalWidth%WIDTH;
		float y = (PADD_TINY+chipsHeight)*(totalWidth/(int)WIDTH)+PADD_NORMAL;
		if (x-w < 0) { shift = Math.abs(x-w);}
		views.get(i).setTranslationX(x-w+shift);
		views.get(i).setTranslationY(y);
		ViewGroup.LayoutParams lp = container.getLayoutParams();
		lp.height = (int)(y+chipsHeight + PADD_NORMAL);
		container.setLayoutParams(lp);
	}

	public void setOnChipCheckListener(OnCheckListener l) {
		this.listener = l;
	}

	@Override
	public Parcelable onSaveInstanceState() {
		Parcelable superState = super.onSaveInstanceState();
		SavedState ss = new SavedState(superState);

		ss.WIDTH = WIDTH;
		ss.chipState = chipState;
		ss.names = names;
		ss.colors = colors;
		return ss;
	}

	@Override
	public void onRestoreInstanceState(Parcelable state) {
		SavedState ss = (SavedState) state;
		super.onRestoreInstanceState(ss.getSuperState());
		WIDTH = ss.WIDTH;
		chipState = ss.chipState;
		names = ss.names;
		colors = ss.colors;
		container.removeAllViews();
		if (names != null && colors != null) {
			if (names.length > 1) {
				views.clear();
				chipsWidth.clear();
				totalWidth = 0;
				chipsHeight = 0;
				shift = 0;
				for (int i = 0; i < names.length; i++) {
					views.add(createChipView(i, getContext(), names[i], colors[i], chipState[i]));
					container.addView(views.get(i));
					updatePositions(i);
				}
			} else {
				clearChips();
			}
		}
	}

	static class SavedState extends View.BaseSavedState {
		SavedState(Parcelable superState) {
			super(superState);
		}

		private SavedState(Parcel in) {
			super(in);
			WIDTH = in.readFloat();
			in.readBooleanArray(chipState);
			in.writeStringArray(names);
			in.writeIntArray(colors);
		}

		@Override
		public void writeToParcel(Parcel out, int flags) {
			super.writeToParcel(out, flags);
			out.writeFloat(WIDTH);
			out.writeBooleanArray(chipState);
			out.writeStringArray(names);
			out.writeIntArray(colors);
		}

		boolean[] chipState;
		String[] names;
		int[] colors;
		float WIDTH;


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
