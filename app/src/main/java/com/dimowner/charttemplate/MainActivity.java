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
import com.dimowner.charttemplate.widget.ChartScrollView;
import com.dimowner.charttemplate.widget.ChartView;
import com.dimowner.charttemplate.widget.CheckersView;
import com.dimowner.charttemplate.widget.OnCheckListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

public class MainActivity extends Activity implements View.OnClickListener {

	private int activeItem = 4;

	private ChartView chartView;
	private ChartScrollView chartScrollView;
	private CheckersView checkersView;
	private ScrollView scrollView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		AndroidUtils.update(getApplicationContext());
		if (CTApplication.isNightMode()) {
			setTheme(R.style.AppTheme_Night);
		} else {
			setTheme(R.style.AppTheme);
		}
		super.onCreate(savedInstanceState);
		ScrollView.LayoutParams params = new ScrollView.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.MATCH_PARENT);
		setContentView(generateLayout(), params);

		if (savedInstanceState == null) {
			Thread thread = new Thread(new Runnable() {
				@Override
				public void run() {
					final ChartData d = readDemoData(activeItem);
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							setData(d);
						}
					});
				}
			});
			thread.start();
		}

		chartView.setOnMoveEventsListener(new ChartView.OnMoveEventsListener() {
			@Override
			public void onMoveEvent() {
				scrollView.requestDisallowInterceptTouchEvent(true);
			}
		});
		chartScrollView.setOnScrollListener(new ChartScrollView.OnScrollListener() {
			@Override
			public void onScroll(float x, float size) {
				chartView.scrollPos(x, size);
				scrollView.requestDisallowInterceptTouchEvent(true);
			}
		});

		checkersView.setOnCheckListener(new OnCheckListener() {
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
		});
	}

	private View generateLayout() {
		float DENSITY = AndroidUtils.dpToPx(1);
		int PADD_NORMAL = (int) (16*DENSITY);
		int PADD_SMALL = (int) (8*DENSITY);
		int PADD_TINY = (int) (4*DENSITY);
		int TOOLBAR_HEIGHT = (int) (56*DENSITY);

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
		FrameLayout.LayoutParams toolbarLp = new FrameLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, TOOLBAR_HEIGHT);
		toolbar.setLayoutParams(toolbarLp);
		toolbar.setBackgroundResource(R.color.primary);

		Typeface typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL);

		//Title
		final TextView title = new TextView(getApplicationContext());
		FrameLayout.LayoutParams titleLp = new FrameLayout.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.WRAP_CONTENT);
		titleLp.setMargins((int)(68*DENSITY), 0, 0, 0);
		titleLp.gravity = Gravity.START | Gravity.CENTER_VERTICAL;
		title.setLayoutParams(titleLp);
		title.setTypeface(typeface);
		title.setTextColor(res.getColor(R.color.white));
		title.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
		title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
		title.setText(R.string.statistics);

		//Button Next
		TextView btnNext = new TextView(getApplicationContext());
		FrameLayout.LayoutParams nextLp = new FrameLayout.LayoutParams(
				TOOLBAR_HEIGHT, TOOLBAR_HEIGHT);
		nextLp.gravity = Gravity.START | Gravity.CENTER_VERTICAL;
		btnNext.setLayoutParams(nextLp);
		btnNext.setTextColor(res.getColor(R.color.white));
		btnNext.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
		btnNext.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
		btnNext.setText(R.string.next);
		btnNext.setId(R.id.btn_next);
		btnNext.setGravity(Gravity.CENTER);
		btnNext.setOnClickListener(this);

		//Button Theme
		ImageButton btnTheme = new ImageButton(getApplicationContext());
		FrameLayout.LayoutParams themeLp = new FrameLayout.LayoutParams(
				TOOLBAR_HEIGHT, TOOLBAR_HEIGHT);

		themeLp.gravity = Gravity.END | Gravity.CENTER_VERTICAL;
		btnTheme.setLayoutParams(themeLp);
		btnTheme.setImageResource(R.drawable.moon);
		btnTheme.setId(R.id.btn_theme);
		btnNext.setGravity(Gravity.CENTER);
		btnTheme.setOnClickListener(this);

		toolbar.addView(title);
		toolbar.addView(btnNext);
		toolbar.addView(btnTheme);

		//Followers
		final TextView txtFollowers = new TextView(getApplicationContext());
		LinearLayout.LayoutParams followersLp = new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.WRAP_CONTENT);
		followersLp.setMargins(PADD_NORMAL, PADD_NORMAL, PADD_NORMAL, PADD_NORMAL);

		txtFollowers.setLayoutParams(followersLp);
		txtFollowers.setTextColor(res.getColor(R.color.text_blue));
		txtFollowers.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
		txtFollowers.setText(R.string.followers);
		txtFollowers.setTypeface(typeface);
		txtFollowers.setGravity(Gravity.CENTER);

		//CharView
		chartView = new ChartView(this);
		LinearLayout.LayoutParams chartLp = new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, (int)(300*DENSITY));
		chartLp.setMargins(PADD_NORMAL, 0, PADD_NORMAL, 0);
		chartView.setLayoutParams(chartLp);

		//CharScrollView
		chartScrollView = new ChartScrollView(this);
		LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, (int)(50*DENSITY));
		scrollLp.setMargins(PADD_NORMAL, PADD_TINY, PADD_NORMAL, PADD_SMALL);
		chartScrollView.setLayoutParams(scrollLp);

		//CheckersView
		checkersView = new CheckersView(this);
		LinearLayout.LayoutParams checkersLp = new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		checkersView.setLayoutParams(checkersLp);

		container.addView(toolbar);
		container.addView(txtFollowers);
		container.addView(chartView);
		container.addView(chartScrollView);
		container.addView(checkersView);
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
		btnNext.setBackgroundResource(bg);
		btnTheme.setBackgroundResource(bg);
		toolbar.setBackgroundColor(a.getColor(1, 0));
		container.setBackgroundColor(a.getColor(2, 0));
		a.recycle();

		return scrollView;
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		AndroidUtils.update(getApplicationContext());
		super.onConfigurationChanged(newConfig);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt("active_item", activeItem);

		outState.putParcelable("chart_view", chartView.onSaveInstanceState());
		outState.putParcelable("chart_scroll_view", chartScrollView.onSaveInstanceState());
		outState.putParcelable("checkers_view", checkersView.onSaveInstanceState());
	}

	@Override
	protected void onRestoreInstanceState(Bundle state) {
		super.onRestoreInstanceState(state);
		activeItem = state.getInt("active_item", 4);

		chartView.onRestoreInstanceState(state.getParcelable("chart_view"));
		chartScrollView.onRestoreInstanceState(state.getParcelable("chart_scroll_view"));
		checkersView.onRestoreInstanceState(state.getParcelable("checkers_view"));
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
			Timber.e(ex);
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
		if (v.getId() == R.id.btn_next) {
			int prev = activeItem;
			activeItem--;
			if (activeItem < 0) {
				activeItem = 4;
			}
			if (activeItem != prev) {
				setData(toChartData(CTApplication.getData()[activeItem]));
			}
		} else if (v.getId() == R.id.btn_theme) {
			CTApplication.setNightMode(!CTApplication.isNightMode());
			recreate();
		}
	}
}
