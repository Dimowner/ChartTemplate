/*
 * Copyright 2019 Dmitriy Ponomarenko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dimowner.charttemplate;

import android.app.Activity;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Typeface;

import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.dimowner.charttemplate.model.ChartData;
import com.dimowner.charttemplate.model.Data;
import com.dimowner.charttemplate.util.AndroidUtils;
import com.dimowner.charttemplate.widget.ChartScrollOverlayView;
import com.dimowner.charttemplate.widget.ChartScrollView;
import com.dimowner.charttemplate.widget.ChartView;
import com.dimowner.charttemplate.widget.CheckersView;
import com.dimowner.charttemplate.widget.ItemView;
import com.dimowner.charttemplate.widget.OnCheckListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity implements View.OnClickListener,
			ChartView.OnMoveEventsListener, ChartScrollOverlayView.OnScrollListener, OnCheckListener {

	private final float DENSITY;
	private final int PADD_NORMAL;
	private final int PADD_TINY;
	private final int TOOLBAR_SIZE ;

	{
		DENSITY = AndroidUtils.dpToPx(1);
		PADD_NORMAL = (int) (16* DENSITY);
		PADD_TINY = (int) (4* DENSITY);
		TOOLBAR_SIZE = (int) (56* DENSITY);
	}

//	private ItemView itemView;
//	private ItemView itemView2;
//	private ItemView itemView3;
//	private ItemView itemView4;
//	private ItemView itemView5;
	private ScrollView scrollView;
	private ChartView chartView;
	private ChartScrollView chartScrollView;
	private ChartScrollOverlayView chartScrollOverlayView;
	private CheckersView checkersView;
	private TextView btnNext;
	private int activeItem = 4;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		AndroidUtils.update(getApplicationContext());
//		if (CTApplication.isNightMode()) {
//			setTheme(R.style.AppTheme_Night);
//		} else {
//			setTheme(R.style.AppTheme);
//		}
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_activity);

//		ScrollView.LayoutParams params = new ScrollView.LayoutParams(
//				LinearLayout.LayoutParams.MATCH_PARENT,
//				LinearLayout.LayoutParams.MATCH_PARENT);
//		setContentView(generateLayout(), params);
		chartView = findViewById(R.id.chartView);
		chartScrollView = findViewById(R.id.chartScrollView);
		chartScrollOverlayView = findViewById(R.id.chartScrollOverlayView);
		chartView.setOnMoveEventsListener(this);
		chartScrollOverlayView.setOnScrollListener(this);
		checkersView = findViewById(R.id.checkersView);
		checkersView.setOnCheckListener(this);
		btnNext = findViewById(R.id.btnNext);
		btnNext.setOnClickListener(this);
		scrollView = findViewById(R.id.scrollView);

		if (savedInstanceState == null || CTApplication.getData() == null) {
			Thread thread = new Thread(new Runnable() {
				@Override
				public void run() {
					final ChartData d = readDemoData(4);
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							setData(d);
//							itemView.setData(d);
//							chartView.setData(d);
//							chartScrollView.setData(d);
//							chartScrollOverlayView.setData(d.getLength());
//							checkersView.setData(d.getNames(), d.getColorsInts());
//							itemView2.setData(toChartData(CTApplication.getData()[0]));
//							itemView3.setData(toChartData(CTApplication.getData()[1]));
//							itemView4.setData(toChartData(CTApplication.getData()[2]));
//							itemView5.setData(toChartData(CTApplication.getData()[3]));
						}
					});
				}
			});
			thread.start();
		}
	}

	public void setData(ChartData d) {
		if (chartView != null) {
			chartView.setData(d);
		}
		if (chartScrollView != null) {
			chartScrollView.setData(d);
		}
		if (checkersView != null) {
			checkersView.setData(d.getNames(), d.getColorsInts());
		}
		chartScrollOverlayView.setData(d.getLength());
	}

	private View generateLayout() {

		scrollView = new ScrollView(getApplicationContext());
		Resources res = getResources();

		//Container
		LinearLayout container = new LinearLayout(getApplicationContext());
		LinearLayout.LayoutParams containerLp = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT);
		container.setLayoutParams(containerLp);
		container.setOrientation(LinearLayout.VERTICAL);
		container.setBackgroundColor(res.getColor(R.color.view_background));

		//Toolbar
		FrameLayout toolbar = new FrameLayout(getApplicationContext());
		LinearLayout.LayoutParams toolbarLp = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, TOOLBAR_SIZE);
		toolbar.setLayoutParams(toolbarLp);
		toolbar.setBackgroundResource(R.color.primary);

		Typeface typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL);

		//Title
		final TextView title = new TextView(getApplicationContext());
		FrameLayout.LayoutParams titleLp = new FrameLayout.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.WRAP_CONTENT);
		titleLp.setMargins(PADD_NORMAL, 0, 0, 0);
		titleLp.gravity = Gravity.START | Gravity.CENTER_VERTICAL;
		title.setLayoutParams(titleLp);
		title.setTypeface(typeface);
		title.setTextColor(res.getColor(R.color.white));
		title.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
		title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
		title.setText(R.string.statistics);

		//Button Theme
		ImageButton btnTheme = new ImageButton(getApplicationContext());
		FrameLayout.LayoutParams themeLp = new FrameLayout.LayoutParams(
				TOOLBAR_SIZE, TOOLBAR_SIZE);

		themeLp.gravity = Gravity.END | Gravity.CENTER_VERTICAL;
		btnTheme.setLayoutParams(themeLp);
		btnTheme.setImageResource(R.drawable.moon);
		btnTheme.setId(R.id.btn_theme);
		btnTheme.setOnClickListener(this);

		toolbar.addView(title);
//		toolbar.addView(btnNext);
		toolbar.addView(btnTheme);

		//Followers
		final TextView txtFollowers = new TextView(getApplicationContext());
		LinearLayout.LayoutParams followersLp = new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.WRAP_CONTENT);
		followersLp.setMargins(PADD_NORMAL, PADD_NORMAL, PADD_NORMAL, PADD_TINY);

		txtFollowers.setLayoutParams(followersLp);
		txtFollowers.setTextColor(res.getColor(R.color.text_blue));
		txtFollowers.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
		txtFollowers.setText(R.string.followers);
		txtFollowers.setTypeface(typeface);
		txtFollowers.setGravity(Gravity.CENTER);

//		itemView = createItemView();
//		itemView2 = createItemView();
//		itemView3 = createItemView();
//		itemView4 = createItemView();
//		itemView5 = createItemView();

		container.addView(toolbar);
		container.addView(txtFollowers);
//		container.addView(itemView);
//		container.addView(itemView2);
//		container.addView(itemView3);
//		container.addView(itemView4);
//		container.addView(itemView5);
		scrollView.addView(container);

		//Set theme colors.
		int[] attrs = new int[]{
				android.R.attr.selectableItemBackground,
				R.attr.primaryColor,
				R.attr.viewBackground
		};

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			//Apply better looking ripple for buttons on Android higher than API 21.
			attrs[0] = android.R.attr.selectableItemBackgroundBorderless;
			toolbar.setElevation(PADD_TINY);
			container.setElevation(DENSITY);
		}

		TypedArray a = this.getTheme().obtainStyledAttributes(attrs);
		int bg = a.getResourceId(0, 0);
		btnTheme.setBackgroundResource(bg);
		toolbar.setBackgroundColor(a.getColor(1, 0));
		container.setBackgroundColor(a.getColor(2, 0));
		a.recycle();

		return scrollView;
	}

	private ItemView createItemView() {
		ItemView v = new ItemView(this);
		LinearLayout.LayoutParams itemLp = new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT);
		itemLp.bottomMargin = PADD_NORMAL;
		v.setLayoutParams(itemLp);
		v.setOnMoveEventsListener(this);
		v.setOnScrollListener(this);
		return v;
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		AndroidUtils.update(getApplicationContext());
		super.onConfigurationChanged(newConfig);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
//		outState.putParcelable("item_view1", itemView.onSaveInstanceState());
//		outState.putParcelable("item_view2", itemView2.onSaveInstanceState());
//		outState.putParcelable("item_view3", itemView3.onSaveInstanceState());
//		outState.putParcelable("item_view4", itemView4.onSaveInstanceState());
//		outState.putParcelable("item_view5", itemView5.onSaveInstanceState());
	}

	@Override
	protected void onRestoreInstanceState(Bundle state) {
		super.onRestoreInstanceState(state);
//		itemView.onRestoreInstanceState(state.getParcelable("item_view1"));
//		itemView2.onRestoreInstanceState(state.getParcelable("item_view2"));
//		itemView3.onRestoreInstanceState(state.getParcelable("item_view3"));
//		itemView4.onRestoreInstanceState(state.getParcelable("item_view4"));
//		itemView5.onRestoreInstanceState(state.getParcelable("item_view5"));
	}

	public ChartData readDemoData(int pos) {
		try {
			String DATA_ARRAY = "dataArray";
			String COLUMNS = "columns";
			String TYPES = "types";
			String COLORS = "colors";
			String NAMES = "names";

			JSONObject jo = new JSONObject(
					AndroidUtils.readAsset(getApplicationContext(), "telegram_chart_data.json"));
			JSONArray joArray = jo.getJSONArray(DATA_ARRAY);
			Data[] dataValues = new Data[joArray.length()];
			for (int i = 0; i < joArray.length(); i++) {
				Object[][] columns;
				Map<String, String> types;
				Map<String, String> names;
				Map<String, String> colors;
				JSONObject joItem = (JSONObject) joArray.get(i);

				names = AndroidUtils.jsonToMap(joItem.getJSONObject(NAMES));
				types = AndroidUtils.jsonToMap(joItem.getJSONObject(TYPES));
				colors = AndroidUtils.jsonToMap(joItem.getJSONObject(COLORS));

				JSONArray colArray = joItem.getJSONArray(COLUMNS);
				List<Object> list = AndroidUtils.toList(colArray);
				columns = new Object[list.size()][];

				for (int j = 0; j < list.size(); j++) {
					List<Object> l2 = (List<Object>) list.get(j);
					Object[] a = new Object[l2.size()];
					for (int k = 0; k < l2.size(); k++) {
						a[k] = l2.get(k);
					}
					columns[j] = a;
				}
				dataValues[i] = new Data(columns, types, names, colors);
			}

//			Gson gson = new Gson();
//			DataArray array = gson.fromJson(json, DataArray.class);
//			dataArray = array.getDataArray();
			CTApplication.setData(dataValues);
		} catch (IOException | ClassCastException | JSONException ex) {
//			Timber.e(ex);
			return null;
		}
		return toChartData(CTApplication.getData()[pos]);
	}

	private ChartData toChartData(Data d) {
		String[] keys = d.getColumnsKeys();
		int[][] vals = new int[keys.length][d.getDataLength()];
		String[] names = new String[keys.length];
		String[] types = new String[keys.length];
		String[] colors = new String[keys.length];
		for (int i = 0; i < keys.length; i++) {
			vals[i] = d.getValues(keys[i]);
			names[i] = d.getName(keys[i]);
			types[i] = d.getType(keys[i]);
			colors[i] = d.getColor(keys[i]);
		}
		return new ChartData(d.getTimeArray(), vals, names, types, colors);
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.btn_theme) {
			CTApplication.setNightMode(!CTApplication.isNightMode());
			recreate();
		} else if (v.getId() == R.id.btnNext) {
			int prev = activeItem;
			activeItem--;
			if (activeItem < 0) {
				activeItem = 4;
			}
			if (activeItem != prev) {
				setData(toChartData(CTApplication.getData()[activeItem]));
			}
		}
	}

	@Override
	public void disallowTouchEvent() {
		scrollView.requestDisallowInterceptTouchEvent(true);
	}

	@Override
	public void allowTouchEvent() {
		scrollView.requestDisallowInterceptTouchEvent(false);
	}

	@Override
	public void onScroll(float x, float size) {
		chartView.scrollPos(x, size);
		scrollView.requestDisallowInterceptTouchEvent(true);
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
}
