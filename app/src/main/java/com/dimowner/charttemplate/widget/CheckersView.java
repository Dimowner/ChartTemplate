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
import android.graphics.Color;
import android.os.Build;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;

import com.dimowner.charttemplate.R;
import com.dimowner.charttemplate.util.AndroidUtils;

public class CheckersView extends LinearLayout {

	private static final float DENSITY = AndroidUtils.dpToPx(1);
	//TODO: Use attributes
	private static int PADD_NORMAL = (int) (16*DENSITY);
	private static int PADD_SMALL = (int) (8*DENSITY);
	private static int START_INSET = (int) (62*DENSITY);
	private static int DIVIDER_HEIGHT = (int) DENSITY;

	private LinearLayout container;
	private boolean[] checkerState;
	private int textColor;
	private int dividerColor;

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
		TypedValue typedValue = new TypedValue();
		Resources.Theme theme = context.getTheme();
		if (theme.resolveAttribute(R.attr.textCheckerColor, typedValue, true)) {
			textColor = typedValue.data;
		} else {
			textColor = res.getColor(R.color.text_dark);
		}
		if (theme.resolveAttribute(R.attr.gridColor, typedValue, true)) {
			dividerColor = typedValue.data;
		} else {
			dividerColor = res.getColor(R.color.grid_color2);
		}

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
			checkerState = new boolean[names.length];
			container.removeAllViews();
			for (int i = 0; i < names.length; i++) {
				checkerState[i] = true;
				container.addView(createCheckerView(i, names[i], colors[i]));
				if (i < names.length-1) {
					container.addView(createDivider());
				}
			}
		}
	}

	public CheckBox createCheckerView(int id, final String name, final int color) {
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
		ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		params.setMargins(PADD_SMALL, PADD_NORMAL, PADD_NORMAL, PADD_NORMAL);
		checkBox.setLayoutParams(params);
		checkBox.setPadding(PADD_NORMAL, 0, 0, 0);
		checkBox.setId(id);
		checkBox.setChecked(true);
		checkBox.setSaveEnabled(false);

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
		ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(
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
}
