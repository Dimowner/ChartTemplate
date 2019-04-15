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

import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.dimowner.charttemplate.model.ChartData;
import com.dimowner.charttemplate.model.Data;
import com.dimowner.charttemplate.util.AndroidUtils;
import com.dimowner.charttemplate.util.TimeUtils;
import com.dimowner.charttemplate.widget.ChartScrollOverlayView;
import com.dimowner.charttemplate.widget.ChartView;
import com.dimowner.charttemplate.widget.ItemView;
import com.google.gson.Gson;

import java.io.IOException;

import timber.log.Timber;

public class MainActivity extends Activity implements View.OnClickListener,
			ChartView.OnMoveEventsListener, ChartScrollOverlayView.OnScrollListener {
//		, OnCheckListener {

//	private final float DENSITY;
//	private final int PADD_NORMAL;
//	private final int PADD_TINY;
//	private final int TOOLBAR_SIZE ;
//
//	{
//		DENSITY = AndroidUtils.dpToPx(1);
//		PADD_NORMAL = (int) (16* DENSITY);
//		PADD_TINY = (int) (4* DENSITY);
//		TOOLBAR_SIZE = (int) (56* DENSITY);
//	}

//	private ItemView itemView;
//	private ItemView itemView2;
//	private ItemView itemView3;
//	private ItemView itemView4;
//	private ItemView itemView5;
//	private ScrollView scrollView;
//	private ChartView chartView;
//	private ChartScrollView chartScrollView;
//	private ChartScrollOverlayView chartScrollOverlayView;
//	private CheckersView checkersView;
	private ItemView itemView;
//	private TextView btnNext;
//	private int activeItem = 0;
//
//	private ImageButton btnNightMode;
//	private TextView txtTitle;
//	private FrameLayout toolbar;

	private Gson gson = new Gson();

	private RecyclerView recyclerView;
	private LinearLayoutManager layoutManager;
	private ItemsAdapter adapter;
	private Parcelable listSaveState;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		AndroidUtils.update(getApplicationContext());
		if (CTApplication.isNightMode()) {
			setTheme(R.style.AppTheme_Night);
		} else {
			setTheme(R.style.AppTheme);
		}
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_activity);

//		Timber.v("Time 1 = " + TimeUtils.formatDate(new Date())
//				+ " 2 = " + TimeUtils.formatDateWeek(new Date())
//				+  " 3 = " + TimeUtils.formatDateLong(new Date()));
//		ScrollView.LayoutParams params = new ScrollView.LayoutParams(
//				LinearLayout.LayoutParams.MATCH_PARENT,
//				LinearLayout.LayoutParams.MATCH_PARENT);
//		setContentView(generateLayout(), params);
//		chartView = findViewById(R.id.chartView);
//		chartScrollView = findViewById(R.id.chartScrollView);
//		chartScrollOverlayView = findViewById(R.id.chartScrollOverlayView);
//		chartView.setOnMoveEventsListener(this);
//		chartScrollOverlayView.setOnScrollListener(this);
//		checkersView = findViewById(R.id.checkersView);
//		checkersView.setOnCheckListener(this);

		itemView = findViewById(R.id.itemView);
//		itemView.setOnMoveEventsListener(this);
//		itemView.setOnScrollListener(this);
//
//		btnNext = findViewById(R.id.btnNext);
//		btnNext.setOnClickListener(this);
//		scrollView = findViewById(R.id.scrollView);
//		toolbar = findViewById(R.id.toolbar);
		ImageButton btnNightMode = findViewById(R.id.btnNightMode);
		btnNightMode.setOnClickListener(this);

		TextView txtTitle = findViewById(R.id.txtTitle);
//		TextView txtChartTitle = findViewById(R.id.txtChartTitle);
//		if (CTApplication.isNightMode()) {
		if (CTApplication.isNightMode()) {
			int color = getResources().getColor(R.color.white);
			txtTitle.setTextColor(color);
//			txtChartTitle.setTextColor(color);
//			btnNext.setTextColor(color);
			btnNightMode.setImageResource(R.drawable.moon_light7);
		} else {
			int color = getResources().getColor(R.color.black);
			txtTitle.setTextColor(color);
//			txtChartTitle.setTextColor(color);
//			btnNext.setTextColor(color);
			btnNightMode.setImageResource(R.drawable.moon7);
			AndroidUtils.statusBarLightMode(this);
		}

		recyclerView = findViewById(R.id.recyclerView);
		layoutManager = new LinearLayoutManager(getApplicationContext());
		recyclerView.setLayoutManager(layoutManager);
		adapter = new ItemsAdapter();
		recyclerView.setAdapter(adapter);
		adapter.setOnDetailsListener(new ChartView.OnDetailsListener() {
			@Override
			public void showDetails(final int num, long time) {
//				Timber.v("showDetails num = " + num + " time = " + time);
				loadChartAsync(num, time, new OnLoadCharListener() {
					@Override
					public void onLoadChart(ChartData chart) {
						if (chart != null) {
//							Timber.v("onLoadChart chart = " + chart.getChartNum());
							adapter.setItem(num-1, chart);
						}
					}
				});
			}

			@Override
			public void hideDetails(int num) {
//				Timber.v("hideDetails num = " + num);
				adapter.setItem(num-1, CTApplication.getChartData()[num-1]);
			}
		});

		if (savedInstanceState == null || CTApplication.getChartData() == null) {
			Thread thread = new Thread(new Runnable() {
				@Override
				public void run() {
//					final ChartData d = readDemoData(activeItem);
					final ChartData d = readDemoData(0);
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
//							setData(CTApplication.getChartData()[1]);
//							itemView.setData(d);
//							chartView.setData(d);
//							chartScrollView.setData(d);
//							chartScrollOverlayView.setData(d.getLength());
//							checkersView.setData(d.getNames(), d.getColorsInts());
//							itemView2.setData(toChartData(CTApplication.getData()[0]));
//							itemView3.setData(toChartData(CTApplication.getData()[1]));
//							itemView4.setData(toChartData(CTApplication.getData()[2]));
//							itemView5.setData(toChartData(CTApplication.getData()[3]));
							adapter.setData(CTApplication.getChartData());
//							if (state != null) {
//								adapter.onRestoreState(state);
//							}
						}
					});
				}
			});
			thread.start();
		} else {
//			if (listSaveState != null) {
//				adapter.setData(CTApplication.getChartData());
//				layoutManager.onRestoreInstanceState(listSaveState);
//			} else {
//				adapter.setData(CTApplication.getChartData());
//			}
		}
	}

	public void setData(ChartData d) {
		itemView.setData(d);
//		if (chartView != null) {
//			chartView.setData(d);
//		}
//		if (chartScrollView != null) {
//			chartScrollView.setData(d);
//		}
//		if (checkersView != null) {
//			checkersView.setData(d.getNames(), d.getColorsInts());
//		}
//		chartScrollOverlayView.setData(d.getLength());
	}
//
//	private View generateLayout() {
//
//		scrollView = new ScrollView(getApplicationContext());
//		Resources res = getResources();
//
//		//Container
//		LinearLayout container = new LinearLayout(getApplicationContext());
//		LinearLayout.LayoutParams containerLp = new LinearLayout.LayoutParams(
//				LinearLayout.LayoutParams.MATCH_PARENT,
//				LinearLayout.LayoutParams.WRAP_CONTENT);
//		container.setLayoutParams(containerLp);
//		container.setOrientation(LinearLayout.VERTICAL);
//		container.setBackgroundColor(res.getColor(R.color.view_background));
//
//		//Toolbar
//		FrameLayout toolbar = new FrameLayout(getApplicationContext());
//		LinearLayout.LayoutParams toolbarLp = new LinearLayout.LayoutParams(
//				LinearLayout.LayoutParams.MATCH_PARENT, TOOLBAR_SIZE);
//		toolbar.setLayoutParams(toolbarLp);
//		toolbar.setBackgroundResource(R.color.primary);
//
//		Typeface typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL);
//
//		//Title
//		final TextView title = new TextView(getApplicationContext());
//		FrameLayout.LayoutParams titleLp = new FrameLayout.LayoutParams(
//				ViewGroup.LayoutParams.WRAP_CONTENT,
//				ViewGroup.LayoutParams.WRAP_CONTENT);
//		titleLp.setMargins(PADD_NORMAL, 0, 0, 0);
//		titleLp.gravity = Gravity.START | Gravity.CENTER_VERTICAL;
//		title.setLayoutParams(titleLp);
//		title.setTypeface(typeface);
//		title.setTextColor(res.getColor(R.color.white));
//		title.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
//		title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
//		title.setText(R.string.statistics);
//
//		//Button Theme
//		ImageButton btnTheme = new ImageButton(getApplicationContext());
//		FrameLayout.LayoutParams themeLp = new FrameLayout.LayoutParams(
//				TOOLBAR_SIZE, TOOLBAR_SIZE);
//
//		themeLp.gravity = Gravity.END | Gravity.CENTER_VERTICAL;
//		btnTheme.setLayoutParams(themeLp);
//		btnTheme.setImageResource(R.drawable.moon);
//		btnTheme.setId(R.id.btn_theme);
//		btnTheme.setOnClickListener(this);
//
//		toolbar.addView(title);
////		toolbar.addView(btnNext);
//		toolbar.addView(btnTheme);
//
//		//Followers
//		final TextView txtFollowers = new TextView(getApplicationContext());
//		LinearLayout.LayoutParams followersLp = new LinearLayout.LayoutParams(
//				ViewGroup.LayoutParams.WRAP_CONTENT,
//				ViewGroup.LayoutParams.WRAP_CONTENT);
//		followersLp.setMargins(PADD_NORMAL, PADD_NORMAL, PADD_NORMAL, PADD_TINY);
//
//		txtFollowers.setLayoutParams(followersLp);
//		txtFollowers.setTextColor(res.getColor(R.color.text_blue));
//		txtFollowers.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
//		txtFollowers.setText(R.string.followers);
//		txtFollowers.setTypeface(typeface);
//		txtFollowers.setGravity(Gravity.CENTER);
//
////		itemView = createItemView();
////		itemView2 = createItemView();
////		itemView3 = createItemView();
////		itemView4 = createItemView();
////		itemView5 = createItemView();
//
//		container.addView(toolbar);
//		container.addView(txtFollowers);
////		container.addView(itemView);
////		container.addView(itemView2);
////		container.addView(itemView3);
////		container.addView(itemView4);
////		container.addView(itemView5);
//		scrollView.addView(container);
//
//		//Set theme colors.
//		int[] attrs = new int[]{
//				android.R.attr.selectableItemBackground,
//				R.attr.primaryColor,
//				R.attr.viewBackground
//		};
//
//		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//			//Apply better looking ripple for buttons on Android higher than API 21.
//			attrs[0] = android.R.attr.selectableItemBackgroundBorderless;
//			toolbar.setElevation(PADD_TINY);
//			container.setElevation(DENSITY);
//		}
//
//		TypedArray a = this.getTheme().obtainStyledAttributes(attrs);
//		int bg = a.getResourceId(0, 0);
//		btnTheme.setBackgroundResource(bg);
//		toolbar.setBackgroundColor(a.getColor(1, 0));
//		container.setBackgroundColor(a.getColor(2, 0));
//		a.recycle();
//
//		return scrollView;
//	}
//
//	private ItemView createItemView() {
//		ItemView v = new ItemView(this);
//		LinearLayout.LayoutParams itemLp = new LinearLayout.LayoutParams(
//				ViewGroup.LayoutParams.MATCH_PARENT,
//				ViewGroup.LayoutParams.WRAP_CONTENT);
//		itemLp.bottomMargin = PADD_NORMAL;
//		v.setLayoutParams(itemLp);
//		v.setOnMoveEventsListener(this);
//		v.setOnScrollListener(this);
//		return v;
//	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		AndroidUtils.update(getApplicationContext());
		super.onConfigurationChanged(newConfig);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putParcelable("item_view1", itemView.onSaveInstanceState());
//		outState.putParcelable("item_view2", itemView2.onSaveInstanceState());
//		outState.putParcelable("item_view3", itemView3.onSaveInstanceState());
//		outState.putParcelable("item_view4", itemView4.onSaveInstanceState());
//		outState.putParcelable("item_view5", itemView5.onSaveInstanceState());
		if (outState != null) {
			listSaveState = layoutManager.onSaveInstanceState();
			outState.putParcelable("recycler_state", listSaveState);
			outState.putBundle("adapter", adapter.onSaveState());
		}
	}

	@Override
	protected void onRestoreInstanceState(Bundle state) {
		super.onRestoreInstanceState(state);
		itemView.onRestoreInstanceState(state.getParcelable("item_view1"));
//		itemView2.onRestoreInstanceState(state.getParcelable("item_view2"));
//		itemView3.onRestoreInstanceState(state.getParcelable("item_view3"));
//		itemView4.onRestoreInstanceState(state.getParcelable("item_view4"));
//		itemView5.onRestoreInstanceState(state.getParcelable("item_view5"));
		if (state != null) {
			listSaveState = state.getParcelable("recycler_state");
			adapter.setData(CTApplication.getChartData());
			layoutManager.onRestoreInstanceState(listSaveState);
			adapter.onRestoreState(state.getBundle("adapter"));
		}
	}

	/**
	 * Load chart data
	 * if time <= 0 load overview chart with selected num
	 */
	public ChartData loadChart(int chartNum, long time) {
		try {
			boolean detailsMode;
			String location;
			if (time > 0) {
				location = "contest/" + chartNum + "/" + TimeUtils.getMonthYear(time) + "/"
						+ TimeUtils.getDayOfMonth(time) + ".json";
//				Timber.v("loadChart: " + location);
				detailsMode = true;
			} else {
				location = "contest/" + chartNum + "/overview.json";
				detailsMode = false;
			}

			String json1 = AndroidUtils.readAsset(getApplicationContext(), location);
			return toChartData(detailsMode, chartNum, gson.fromJson(json1, Data.class));
		} catch (IOException | ClassCastException ex) {
			Timber.e(ex);
		}
		return null;
	}

	public void loadChartAsync(final int num, final long time, final OnLoadCharListener listener) {
		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				final ChartData data = loadChart(num, time);
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if (listener != null) {
							listener.onLoadChart(data);
						}
					}
				});
			}
		});
		thread.start();
	}

	public ChartData readDemoData(int pos) {
		try {
//			String DATA_ARRAY = "dataArray";
//			String COLUMNS = "columns";
//			String TYPES = "types";
//			String COLORS = "colors";
//			String NAMES = "names";

//			String json = AndroidUtils.readAsset(getApplicationContext(), "telegram_chart_data.json");
			String json1 = AndroidUtils.readAsset(getApplicationContext(), "contest/1/overview.json");
			String json2 = AndroidUtils.readAsset(getApplicationContext(), "contest/2/overview.json");
			String json3 = AndroidUtils.readAsset(getApplicationContext(), "contest/3/overview.json");
			String json4 = AndroidUtils.readAsset(getApplicationContext(), "contest/4/overview.json");
			String json5 = AndroidUtils.readAsset(getApplicationContext(), "contest/5/overview.json");
//			Timber.v("json = %s", json);
//			JSONObject jo = new JSONObject(json);
//			JSONArray joArray = jo.getJSONArray(DATA_ARRAY);
//			Data[] dataValues = new Data[joArray.length()];
//			for (int i = 0; i < joArray.length(); i++) {
//				Object[][] columns;
//				Map<String, String> types;
//				Map<String, String> names;
//				Map<String, String> colors;
//				JSONObject joItem = (JSONObject) joArray.get(i);
//
//				names = AndroidUtils.jsonToMap(joItem.getJSONObject(NAMES));
//				types = AndroidUtils.jsonToMap(joItem.getJSONObject(TYPES));
//				colors = AndroidUtils.jsonToMap(joItem.getJSONObject(COLORS));
//
//				JSONArray colArray = joItem.getJSONArray(COLUMNS);
//				List<Object> list = AndroidUtils.toList(colArray);
//				columns = new Object[list.size()][];
//
//				for (int j = 0; j < list.size(); j++) {
//					List<Object> l2 = (List<Object>) list.get(j);
//					Object[] a = new Object[l2.size()];
//					for (int k = 0; k < l2.size(); k++) {
//						a[k] = l2.get(k);
//					}
//					columns[j] = a;
//				}
//				dataValues[i] = new Data(columns, types, names, colors);
//			}

			Gson gson = new Gson();
//			DataArray data = gson.fromJson(json, DataArray.class);
			Data data1 = gson.fromJson(json1, Data.class);
			Data data2 = gson.fromJson(json2, Data.class);
			Data data3 = gson.fromJson(json3, Data.class);
			Data data4 = gson.fromJson(json4, Data.class);
			Data data5 = gson.fromJson(json5, Data.class);
//			dataArray = array.getDataArray();
			ChartData[] chartData = new ChartData[] {toChartData(false, 1, data1), toChartData(false, 2, data2),
					toChartData(false, 3, data3), toChartData(false, 4, data4), toChartData(false, 5, data5)};
			CTApplication.setChartData(chartData);
//			CTApplication.setData(new Data[]{data1, data2, data3, data4, data5});
//			CTApplication.setData(dataValues);
//		} catch (IOException | ClassCastException | JSONException ex) {
		} catch (IOException | ClassCastException ex) {
			Timber.e(ex);
			return null;
		}
//		Timber.v("DATA = %s", CTApplication.getData()[0].getColumnCount() + " length = " + CTApplication.getData()[0].getDataLength());
//		return toChartData(CTApplication.getData()[pos]);
		return CTApplication.getChartData()[pos];
	}

	private ChartData toChartData(boolean detailsMode, int chartNum, Data d) {
		if (d != null) {
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
			return new ChartData(detailsMode, chartNum, d.getTimeArray(), vals, names, types, colors,
					d.isYscaled(), d.isPercentage(), d.isStacked());
		}
		return null;
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.btnNightMode) {
			CTApplication.setNightMode(!CTApplication.isNightMode());
			recreate();
//			if (ColorMap.isNightTheme()) {
//				ColorMap.setDayTheme();
//				updateTheme(true);
//			} else {
//				ColorMap.setNightTheme();
//				updateTheme(false);
//			}
		}
//		else if (v.getId() == R.id.btnNext) {
//			int prev = activeItem;
//			activeItem--;
//			if (activeItem < 0) {
//				activeItem = 4;
//			}
//			if (activeItem != prev) {
////				setData(toChartData(CTApplication.getData()[activeItem]));
//				setData(CTApplication.getChartData()[activeItem]);
//			}
//		}
	}

//	private void updateTheme(boolean night) {
//		AndroidUtils.statusBarLightMode(this);
//		Resources res = getResources();
//		txtTitle.setTextColor(res.getColor(ColorMap.getTittleColor()));
//		btnNightMode.setImageResource(ColorMap.getMoonIcon());
//		toolbar.setBackgroundColor(res.getColor(ColorMap.getPrimaryColor()));
//	}

	@Override
	public void disallowTouchEvent() {
//		scrollView.requestDisallowInterceptTouchEvent(true);
//		recyclerView.requestDisallowInterceptTouchEvent(true);
	}

	@Override
	public void allowTouchEvent() {
//		scrollView.requestDisallowInterceptTouchEvent(false);
//		recyclerView.requestDisallowInterceptTouchEvent(false);
	}

	@Override
	public void onScroll(float x, float size) {
//		chartView.scrollPos(x, size);
//		scrollView.requestDisallowInterceptTouchEvent(true);
//		recyclerView.requestDisallowInterceptTouchEvent(true);
	}

//	@Override
//	public void onCheck(int id, String name, boolean checked) {
//		if (checked) {
//			chartView.showLine(name);
//			chartScrollView.showLine(name);
//		} else {
//			chartView.hideLine(name);
//			chartScrollView.hideLine(name);
//		}
//	}

	public interface OnLoadCharListener {
		void onLoadChart(ChartData chart);
	}
}
