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
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import com.dimowner.charttemplate.R;
import com.dimowner.charttemplate.util.AndroidUtils;

public class CheckersView extends LinearLayout {

	private final int PADD_NORMAL;
	private final int PADD_XSMALL;
	private final int START_INSET;
	private final int DIVIDER_HEIGHT;

	{
		float DENSITY = AndroidUtils.dpToPx(1);
		PADD_NORMAL = (int) (16* DENSITY);
		PADD_XSMALL = (int) (10* DENSITY);
		START_INSET = (int) (56* DENSITY);
		DIVIDER_HEIGHT = (int) DENSITY;
	}

	private LinearLayout container;
	private boolean[] checkerState;
	private int textColor;
	private int dividerColor;
	private String[] names;
	private int[] colors;

	private OnCheckListener listener;

	public CheckersView(Context context) {
		super(context);
		init(context);
	}

	public CheckersView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public CheckersView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(context);
	}

	private void init(Context context) {
		Resources res = context.getResources();
//		TypedValue typedValue = new TypedValue();
//		Resources.Theme theme = context.getTheme();
//		if (theme.resolveAttribute(R.attr.textCheckerColor, typedValue, true)) {
//			textColor = typedValue.data;
//		} else {
			textColor = res.getColor(R.color.text_dark);
//		}
//		if (theme.resolveAttribute(R.attr.gridColor, typedValue, true)) {
//			dividerColor = typedValue.data;
//		} else {
			dividerColor = res.getColor(R.color.grid_color2);
//		}

		container = new LinearLayout(context);
		LinearLayout.LayoutParams containerLp = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);
		container.setLayoutParams(containerLp);
		container.setOrientation(LinearLayout.VERTICAL);
		this.addView(container);
	}

	public void setData(String[] names, int[] colors) {
		if (names != null && colors != null) {
			this.names = names;
			this.colors = colors;
			checkerState = new boolean[names.length];
			container.removeAllViews();
			for (int i = 0; i < names.length; i++) {
				checkerState[i] = true;
				container.addView(createCheckerView(i, names[i], colors[i], i));
				if (i < names.length-1) {
					container.addView(createDivider());
				}
			}
		}
	}

	public CheckBox createCheckerView(int id, final String name, final int color, int index) {
		final CheckBox checkBox = new CheckBox(getContext());
		checkBox.setTextColor(textColor);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			ColorStateList colorStateList = new ColorStateList(
					new int[][]{
							new int[]{-android.R.attr.state_checked}, // unchecked
							new int[]{android.R.attr.state_checked}  // checked
					},
					new int[]{
							color,
							color
					}
			);
			checkBox.setButtonTintList(colorStateList);
		}

		checkBox.setText(name);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		params.setMargins(PADD_XSMALL, PADD_XSMALL, PADD_NORMAL, PADD_XSMALL);
		checkBox.setLayoutParams(params);
		checkBox.setId(id);
		checkBox.setChecked(checkerState[index]);
		checkBox.setSaveEnabled(false);
		checkBox.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
		checkBox.setPadding(checkBox.getPaddingLeft()+PADD_NORMAL, checkBox.getPaddingTop(),
				checkBox.getPaddingRight(), checkBox.getPaddingBottom());

		checkBox.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (checkerState[v.getId()]) {
					if (listener != null) {
						listener.onCheck(v.getId(), name, false);
					}
				} else {
					if (listener != null) {
						listener.onCheck(v.getId(), name, true);
					}
				}
				checkerState[v.getId()] = !checkerState[v.getId()];
			}
		});
		return checkBox;
	}

	private View createDivider() {
		View divider = new View(getContext());
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		params.height = DIVIDER_HEIGHT;
		params.setMargins(START_INSET, 0, 0, 0);
		divider.setLayoutParams(params);
		divider.setBackgroundColor(dividerColor);
		return divider;
	}

	public void setOnCheckListener(OnCheckListener l) {
		this.listener = l;
	}

	@Override
	public Parcelable onSaveInstanceState() {
		Parcelable superState = super.onSaveInstanceState();
		SavedState ss = new SavedState(superState);

		ss.checkerState = checkerState;
		ss.names = names;
		ss.colors = colors;
		return ss;
	}

	@Override
	public void onRestoreInstanceState(Parcelable state) {
		SavedState ss = (SavedState) state;
		super.onRestoreInstanceState(ss.getSuperState());
		checkerState = ss.checkerState;
		names = ss.names;
		colors = ss.colors;
		container.removeAllViews();
		if (names != null && colors != null) {
			for (int i = 0; i < names.length; i++) {
				container.addView(createCheckerView(i, names[i], colors[i], i));
				if (i < names.length - 1) {
					container.addView(createDivider());
				}
			}
		}
	}

	static class SavedState extends View.BaseSavedState {
		SavedState(Parcelable superState) {
			super(superState);
		}

		private SavedState(Parcel in) {
			super(in);
			in.readBooleanArray(checkerState);
			in.writeStringArray(names);
			in.writeIntArray(colors);
		}

		@Override
		public void writeToParcel(Parcel out, int flags) {
			super.writeToParcel(out, flags);
			out.writeBooleanArray(checkerState);
			out.writeStringArray(names);
			out.writeIntArray(colors);
		}

		boolean[] checkerState;
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
