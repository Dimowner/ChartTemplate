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
import android.graphics.Color;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.dimowner.charttemplate.CTApplication;
import com.dimowner.charttemplate.R;
import com.dimowner.charttemplate.util.AndroidUtils;

public class ChipsView extends LinearLayout {

	//TODO: Use attributes
	private static int PADD_SMALL = (int) AndroidUtils.dpToPx(8);
	private static int PADD_NORMAL = (int) AndroidUtils.dpToPx(16);

	private LinearLayout container;
	private boolean[] chipState;
	private int gridTextColor;

	private OnChipCheckListener listener;

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
		if (context.getTheme().resolveAttribute(R.attr.gridTextColor, typedValue, true)) {
			gridTextColor = typedValue.data;
		} else {
			gridTextColor = context.getResources().getColor(R.color.text_color);
		}

		//TODO: consider adding HorizontalScrollView as root view.
		container = new LinearLayout(context);
		container.setOrientation(LinearLayout.HORIZONTAL);
		this.addView(container);
	}

	public TextView createChipView(int id, final Context context, final String name, final String color, int textColor) {
		final TextView textView = new TextView(context);
		ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.WRAP_CONTENT);
		textView.setLayoutParams(lp);
		textView.setCompoundDrawablesWithIntrinsicBounds(
				ContextCompat.getDrawable(context, R.drawable.ic_check_circle), null, null, null);
		textView.getCompoundDrawables()[0].mutate();
		DrawableCompat.setTint(textView.getCompoundDrawables()[0], Color.parseColor(color));
		if (CTApplication.isNightMode()) {
			textView.setBackgroundResource(R.drawable.bg_chip_night);
		} else {
			textView.setBackgroundResource(R.drawable.bg_chip);
		}
		textView.setGravity(Gravity.CENTER);
		textView.setTextColor(textColor);

		textView.setText(name);
		ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(textView.getLayoutParams());
		params.setMargins(PADD_NORMAL, PADD_NORMAL, 0, PADD_NORMAL);
		textView.setLayoutParams(params);
		textView.setPadding(PADD_SMALL, PADD_SMALL, PADD_NORMAL, PADD_SMALL);
		textView.setCompoundDrawablePadding(PADD_SMALL);
		textView.setId(id);

		textView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (chipState[v.getId()]) {
					//TODO: need to be optimized.
					textView.setCompoundDrawablesWithIntrinsicBounds(
							ContextCompat.getDrawable(context, R.drawable.ic_circle), null, null, null);
					textView.getCompoundDrawables()[0].mutate();
					DrawableCompat.setTint(textView.getCompoundDrawables()[0], Color.parseColor(color));
					if (listener != null) {
						listener.onChipCheck(v.getId(), name, false);
					}
				} else {
					textView.setCompoundDrawablesWithIntrinsicBounds(
							ContextCompat.getDrawable(context, R.drawable.ic_check_circle), null, null, null);
					textView.getCompoundDrawables()[0].mutate();
					DrawableCompat.setTint(textView.getCompoundDrawables()[0], Color.parseColor(color));
					if (listener != null) {
						listener.onChipCheck(v.getId(), name, true);
					}
				}
				chipState[v.getId()] = !chipState[v.getId()];
			}
		});
		return textView;
	}

	public void setData(String[] names, String[] colors) {
		if (names != null && colors != null) {
			chipState = new boolean[names.length];
			container.removeAllViews();
			for (int i = 0; i < names.length; i++) {
				chipState[i] = true;
				container.addView(createChipView(i, getContext(), names[i], colors[i], gridTextColor));
			}
		}
	}

	public void setOnChipCheckListener(OnChipCheckListener l) {
		this.listener = l;
	}

	public interface OnChipCheckListener {
		void onChipCheck(int id, String name, boolean checked);
	}
}
