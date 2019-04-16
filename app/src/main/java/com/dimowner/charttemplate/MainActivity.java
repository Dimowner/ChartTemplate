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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

public class MainActivity extends Activity implements View.OnClickListener {
//			ChartView.OnMoveEventsListener,
//		ChartScrollOverlayView.OnScrollListener {

	private Gson gson = new Gson();

	private RecyclerView recyclerView;
	private LinearLayoutManager layoutManager;
	private ItemsAdapter adapter;
	private Parcelable listSaveState;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		AndroidUtils.update(getApplicationContext());
//		if (CTApplication.isNightMode()) {
		if (ColorMap.isNightTheme()) {
			setTheme(R.style.AppTheme_Night);
		} else {
			setTheme(R.style.AppTheme);
		}
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_activity);

		ImageButton btnNightMode = findViewById(R.id.btnNightMode);
		btnNightMode.setOnClickListener(this);

		TextView txtTitle = findViewById(R.id.txtTitle);
//		if (CTApplication.isNightMode()) {
//		if (ColorMap.isNightTheme()) {
//			int color = getResources().getColor(R.color.white);
//			txtTitle.setTextColor(color);
//			btnNightMode.setImageResource(R.drawable.moon_light7);
//		} else {
//			int color = getResources().getColor(R.color.black);
//			txtTitle.setTextColor(color);
//			btnNightMode.setImageResource(R.drawable.moon7);
//			AndroidUtils.statusBarLightMode(this);
//		}
		txtTitle.setTextColor(getResources().getColor(ColorMap.getTittleColor()));
		btnNightMode.setImageResource(ColorMap.getMoonIcon());
		findViewById(R.id.toolbar).setBackgroundColor(getResources().getColor(ColorMap.getViewBackground()));

		recyclerView = findViewById(R.id.recyclerView);
		layoutManager = new LinearLayoutManager(getApplicationContext());
		recyclerView.setLayoutManager(layoutManager);
		adapter = new ItemsAdapter();
		recyclerView.setAdapter(adapter);
		adapter.setOnDetailsListener(new ChartView.OnDetailsListener() {
			@Override
			public void showDetails(final int num, long time) {
				if (num == 5) {
					ChartData c = CTApplication.getChartData()[4];
					c.setDetailsMode(true);
					adapter.setItem(num - 1, c);
				} else {
					loadChartAsync(num, time, new OnLoadCharListener() {
						@Override
						public void onLoadChart(ChartData chart) {
							if (chart != null) {
								adapter.setItem(num - 1, chart);
							}
						}
					});
				}
			}

			@Override
			public void hideDetails(int num) {
				if (num == 5) {
					ChartData c = CTApplication.getChartData()[4];
					c.setDetailsMode(false);
					adapter.setItem(num - 1, c);
				}
				adapter.setItem(num-1, CTApplication.getChartData()[num-1]);
			}
		});

		if (savedInstanceState == null || CTApplication.getChartData() == null) {
//			try {
//				ArrayList<Data1> dat = Data1.convertJsonArrayToList(new JSONArray(AndroidUtils.readData(getApplicationContext())));
//				Timber.v("data loaded = " + dat.size());
//			} catch (JSONException e){
//				Timber.e(e);
//			}

			Thread thread = new Thread(new Runnable() {
				@Override
				public void run() {
					readDemoData();
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							adapter.setData(CTApplication.getChartData());
						}
					});
				}
			});
			thread.start();
		}
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
//		if (outState != null) {
			listSaveState = layoutManager.onSaveInstanceState();
			outState.putParcelable("recycler_state", listSaveState);
			outState.putBundle("adapter", adapter.onSaveState());
//		}
	}

	@Override
	protected void onRestoreInstanceState(Bundle state) {
		super.onRestoreInstanceState(state);
//		if (state != null) {
			listSaveState = state.getParcelable("recycler_state");
			adapter.setData(CTApplication.getChartData());
			layoutManager.onRestoreInstanceState(listSaveState);
			adapter.onRestoreState(state.getBundle("adapter"));
//		}
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

	public void readDemoData() {
		try {
			String DATA_ARRAY = "dataArray";
			String COLUMNS = "columns";
			String TYPES = "types";
			String COLORS = "colors";
			String NAMES = "names";

			String json = AndroidUtils.readAsset(getApplicationContext(), "telegram_chart_data.json");
//			String json = AndroidUtils.readData(getApplicationContext());
//			String json1 = AndroidUtils.readAsset(getApplicationContext(), "contest/1/overview.json");
//			String json2 = AndroidUtils.readAsset(getApplicationContext(), "contest/2/overview.json");
//			String json3 = AndroidUtils.readAsset(getApplicationContext(), "contest/3/overview.json");
//			String json4 = AndroidUtils.readAsset(getApplicationContext(), "contest/4/overview.json");
//			String json5 = AndroidUtils.readAsset(getApplicationContext(), "contest/5/overview.json");
			Timber.v("json = %s", json);
			JSONObject jo = new JSONObject(json);
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
				dataValues[i] = new Data(columns, types, names, colors, false, false, false);
			}

//			Gson gson = new Gson();
//			DataArray data = gson.fromJson(json, DataArray.class);
//			Data data1 = gson.fromJson(json1, Data.class);
//			Data data2 = gson.fromJson(json2, Data.class);
//			Data data3 = gson.fromJson(json3, Data.class);
//			Data data4 = gson.fromJson(json4, Data.class);
//			Data data5 = gson.fromJson(json5, Data.class);
//			dataArray = array.getDataArray();
//			ChartData[] chartData = new ChartData[] {toChartData(false, 1, data1), toChartData(false, 2, data2),
//					toChartData(false, 3, data3), toChartData(false, 4, data4), toChartData(false, 5, data5)};
//			ChartData[] chartData = new ChartData[] {
//					toChartData(false, 1, data.getDataArray()[0]),
//					toChartData(false, 2, data.getDataArray()[1]),
//					toChartData(false, 3, data.getDataArray()[2]),
//					toChartData(false, 4, data.getDataArray()[3]),
//					toChartData(false, 5, data.getDataArray()[4]),
//			};
			ChartData[] chartData = new ChartData[] {
					toChartData(false, 1, dataValues[0]),
					toChartData(false, 2, dataValues[1]),
					toChartData(false, 3, dataValues[2]),
					toChartData(false, 4, dataValues[3]),
					toChartData(false, 5, dataValues[4]),
			};
			CTApplication.setChartData(chartData);
//			CTApplication.setData(new Data[]{data1, data2, data3, data4, data5});
//			CTApplication.setData(dataValues);
		} catch (IOException | ClassCastException | JSONException ex) {
//		} catch (IOException | ClassCastException ex) {
			Timber.e(ex);
		}
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
//			CTApplication.setNightMode(!CTApplication.isNightMode());
			if (ColorMap.isNightTheme()) {
				ColorMap.setDayTheme();
			} else {
				ColorMap.setNightTheme();
			}
			recreate();
		}
	}

//	@Override
//	public void disallowTouchEvent() {
//		scrollView.requestDisallowInterceptTouchEvent(true);
//		recyclerView.requestDisallowInterceptTouchEvent(true);
//	}
//
//	@Override
//	public void allowTouchEvent() {
//		scrollView.requestDisallowInterceptTouchEvent(false);
//		recyclerView.requestDisallowInterceptTouchEvent(false);
//	}
//
//	@Override
//	public void onScroll(float x, float size) {
//		chartView.scrollPos(x, size);
//		scrollView.requestDisallowInterceptTouchEvent(true);
//		recyclerView.requestDisallowInterceptTouchEvent(true);
//	}

	public interface OnLoadCharListener {
		void onLoadChart(ChartData chart);
	}
}
