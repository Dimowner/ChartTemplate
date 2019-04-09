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
import android.graphics.Typeface;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import com.dimowner.charttemplate.model.ChartData;
import com.dimowner.charttemplate.util.AndroidUtils;

public class ItemView extends LinearLayout implements
		ChartScrollOverlayView.OnScrollListener, OnCheckListener {

	private final float DENSITY;
	private final int PADD_NORMAL;
	private final int PADD_SMALL;
	private final int PADD_TINY;

	{
		DENSITY = AndroidUtils.dpToPx(1);
		PADD_NORMAL = (int) (16*DENSITY);
		PADD_SMALL = (int) (8*DENSITY);
		PADD_TINY = (int) (4*DENSITY);
	}

	private LinearLayout container;

	private ChartView chartView;
	private ChartScrollView chartScrollView;
	private ChartScrollOverlayView chartScrollOverlayView;
//	private CheckersView checkersView;
	private ChipsView chipsView;
	private ChartScrollOverlayView.OnScrollListener onScrollListener;

	public ItemView(Context context) {
		super(context);
		init(context);
	}

	public ItemView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public ItemView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(context);
	}

	private void init(Context context) {
		this.addView(generateLayout(context));
	}


	private View generateLayout(Context context) {
		container = new LinearLayout(context);
		LinearLayout.LayoutParams containerLp = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);
		container.setLayoutParams(containerLp);
		container.setOrientation(LinearLayout.VERTICAL);
		container.setPadding(PADD_NORMAL, 0, PADD_NORMAL, 0);
		container.setClipChildren(false);
		container.setClipToPadding(false);

		//CharView
		chartView = new ChartView(context);
		LinearLayout.LayoutParams chartLp = new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, (int)(390*DENSITY));
//		chartLp.setMargins(PADD_NORMAL, 0, PADD_NORMAL, 0);
		chartView.setLayoutParams(chartLp);

		FrameLayout scroll = new FrameLayout(context);
		LinearLayout.LayoutParams scrollLp2 = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		scroll.setLayoutParams(scrollLp2);

		//CharScrollView
		chartScrollView = new ChartScrollView(context);
		FrameLayout.LayoutParams scrollLp = new FrameLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, (int)(45*DENSITY));
		scrollLp.setMargins(0, PADD_TINY, 0, PADD_SMALL);
		scrollLp.gravity = Gravity.CENTER;
		chartScrollView.setLayoutParams(scrollLp);

		chartScrollOverlayView = new ChartScrollOverlayView(context);
		FrameLayout.LayoutParams scrollOverlayLp = new FrameLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, (int)(45*DENSITY));
		scrollOverlayLp.setMargins(0, PADD_TINY, 0, PADD_SMALL);
		scrollOverlayLp.gravity = Gravity.CENTER;
		chartScrollOverlayView.setLayoutParams(scrollOverlayLp);
		chartScrollOverlayView.setOnScrollListener(this);
		scroll.addView(chartScrollView);
		scroll.addView(chartScrollOverlayView);

//		//CheckersView
//		checkersView = new CheckersView(context);
//		LinearLayout.LayoutParams checkersLp = new LinearLayout.LayoutParams (
//				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
//		checkersView.setLayoutParams(checkersLp);
//		checkersView.setOnCheckListener(this);
		//CheckersView
		chipsView = new ChipsView(context);
		LinearLayout.LayoutParams checkersLp = new LinearLayout.LayoutParams (
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		chipsView.setLayoutParams(checkersLp);
		chipsView.setOnChipCheckListener(this);

//		container.addView(txtFollowers);
		container.addView(chartView);
		container.addView(scroll);
//		container.addView(checkersView);
		container.addView(chipsView);

		return container;
	}

	public void setData(ChartData d) {
		if (d != null) {
			if (chartView != null) {
				chartView.setData(d);
			}
			if (chartScrollView != null) {
				chartScrollView.setData(d);
			}
			if (chartScrollOverlayView != null) {
				chartScrollOverlayView.setData(d.getLength());
			}
//			if (checkersView != null) {
//				checkersView.setData(d.getNames(), d.getColorsInts());
//			}
			if (chipsView != null) {
				chipsView.setData(d.getNames(), d.getColorsInts());
			}
		}
	}

	public void setOnMoveEventsListener(ChartView.OnMoveEventsListener listener) {
		chartView.setOnMoveEventsListener(listener);

	}

	public void setOnScrollListener(ChartScrollOverlayView.OnScrollListener l) {
		this.onScrollListener = l;
	}

	@Override
	public void onScroll(float x, float size) {
		chartView.scrollPos(x, size);
		if (onScrollListener != null) {
			onScrollListener.onScroll(x, size);
		}
	}

	@Override
	public void onCheck(int id, String name, boolean checked) {
		if (checked) {
			chartView.showLine(name);
			chartScrollView.showLine(name);
		} else {
			chartView.hideLine(name);
			chartScrollView.hideLine(name);
		}
	}

	@Override
	public Parcelable onSaveInstanceState() {
		Parcelable superState = super.onSaveInstanceState();
		SavedState ss = new SavedState(superState);

		ss.chartView = chartView.onSaveInstanceState();
		ss.chartScrollView = chartScrollView.onSaveInstanceState();
		ss.chartScrollOverlayView = chartScrollOverlayView.onSaveInstanceState();
//		ss.checkersView = checkersView.onSaveInstanceState();
		ss.chipsView = chipsView.onSaveInstanceState();

		return ss;
	}

	@Override
	public void onRestoreInstanceState(Parcelable state) {
		SavedState ss = (SavedState) state;
		super.onRestoreInstanceState(ss.getSuperState());

		chartView.onRestoreInstanceState(ss.chartView);
		chartScrollView.onRestoreInstanceState(ss.chartScrollView);
		chartScrollOverlayView.onRestoreInstanceState(ss.chartScrollOverlayView);
//		checkersView.onRestoreInstanceState(ss.checkersView);
		chipsView.onRestoreInstanceState(ss.chipsView);

	}

	static class SavedState extends View.BaseSavedState {
		SavedState(Parcelable superState) {
			super(superState);
		}

		private SavedState(Parcel in) {
			super(in);
			chartView = in.readParcelable(ItemView.class.getClassLoader());
			chartScrollView = in.readParcelable(ItemView.class.getClassLoader());
			chartScrollOverlayView = in.readParcelable(ItemView.class.getClassLoader());
//			checkersView = in.readParcelable(ItemView.class.getClassLoader());
			chipsView = in.readParcelable(ItemView.class.getClassLoader());
		}

		@Override
		public void writeToParcel(Parcel out, int flags) {
			super.writeToParcel(out, flags);
			out.writeParcelable(chartView, Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
			out.writeParcelable(chartScrollView, Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
			out.writeParcelable(chartScrollOverlayView, Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
//			out.writeParcelable(checkersView, Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
			out.writeParcelable(chipsView, Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
		}

		Parcelable chartView;
		Parcelable chartScrollView;
		Parcelable chartScrollOverlayView;
//		Parcelable checkersView;
		Parcelable chipsView;


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
