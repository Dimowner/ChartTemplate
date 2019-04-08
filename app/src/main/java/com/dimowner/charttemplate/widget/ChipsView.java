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
import android.graphics.drawable.GradientDrawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.dimowner.charttemplate.R;
import com.dimowner.charttemplate.util.AndroidUtils;

public class ChipsView extends LinearLayout {

	private final float DENSITY;
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
		PADD_SMALL = (int)(8*DENSITY);
		PADD_NORMAL = (int)(16*DENSITY);
		RAD = 24*DENSITY;
		BORDER = (int)(2*DENSITY);
		NO_ICON_PADD = (int)(28*DENSITY);
		CHIP_HEIGHT = (int)(42*DENSITY);
	}

	private LinearLayout container;
	private boolean[] chipState;
	private String[] names;
	private int[] colors;
//	private int gridTextColor;

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
		TypedValue typedValue = new TypedValue();
//		if (context.getTheme().resolveAttribute(R.attr.gridTextColor, typedValue, true)) {
//			gridTextColor = typedValue.data;
//		} else {
//			gridTextColor = context.getResources().getColor(R.color.text_color);
//		}

		//TODO: Add child positioning in layout
		container = new LinearLayout(context);
		LinearLayout.LayoutParams containerLp = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);
		container.setLayoutParams(containerLp);
		container.setOrientation(LinearLayout.HORIZONTAL);
		this.addView(container);
	}

	public TextView createChipView(int id, final Context context, final String name, final int color, boolean checked) {
		final TextView textView = new TextView(context);
		ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT, CHIP_HEIGHT);
		textView.setLayoutParams(lp);
		textView.setGravity(Gravity.CENTER);
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
		ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(textView.getLayoutParams());
		params.setMargins(PADD_NORMAL, PADD_NORMAL, 0, PADD_NORMAL);
		textView.setLayoutParams(params);

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
			this.names = names;
			this.colors = colors;
			chipState = new boolean[names.length];
			container.removeAllViews();
			for (int i = 0; i < names.length; i++) {
				chipState[i] = true;
				container.addView(createChipView(i, getContext(), names[i], colors[i], true));
			}
		}
	}

	public void setOnChipCheckListener(OnCheckListener l) {
		this.listener = l;
	}

	@Override
	public Parcelable onSaveInstanceState() {
		Parcelable superState = super.onSaveInstanceState();
		SavedState ss = new SavedState(superState);

		ss.chipState = chipState;
		ss.names = names;
		ss.colors = colors;
		return ss;
	}

	@Override
	public void onRestoreInstanceState(Parcelable state) {
		SavedState ss = (SavedState) state;
		super.onRestoreInstanceState(ss.getSuperState());
		chipState = ss.chipState;
		names = ss.names;
		colors = ss.colors;
		container.removeAllViews();
		if (names != null && colors != null) {
//			for (int i = 0; i < names.length; i++) {
//				container.addView(createChipView(i, names[i], colors[i]));
//				if (i < names.length - 1) {
//					container.addView(createDivider());
//				}
//			}
			for (int i = 0; i < names.length; i++) {
//				chipState[i] = true;
				container.addView(createChipView(i, getContext(), names[i], colors[i], chipState[i]));
			}
		}
	}

	static class SavedState extends View.BaseSavedState {
		SavedState(Parcelable superState) {
			super(superState);
		}

		private SavedState(Parcel in) {
			super(in);
			in.readBooleanArray(chipState);
			in.writeStringArray(names);
			in.writeIntArray(colors);
		}

		@Override
		public void writeToParcel(Parcel out, int flags) {
			super.writeToParcel(out, flags);
			out.writeBooleanArray(chipState);
			out.writeStringArray(names);
			out.writeIntArray(colors);
		}

		boolean[] chipState;
		String[] names;
		int[] colors;


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
