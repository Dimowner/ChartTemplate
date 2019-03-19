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
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
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
	private TextView btnNext;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		if (CTApplication.isNightMode()) {
			setTheme(R.style.AppTheme_Night);
		} else {
			setTheme(R.style.AppTheme);
		}
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		ImageButton btnNightMode = findViewById(R.id.btnNightMode);
		btnNightMode.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				CTApplication.setNightMode(!CTApplication.isNightMode());
				recreate();
			}
		});

		final ScrollView scrollView = findViewById(R.id.scrollView);
		chartView = findViewById(R.id.chartView);
		chartScrollView = findViewById(R.id.chartScrollView);
		checkersView = findViewById(R.id.checkersView);

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

		btnNext = findViewById(R.id.btnNext);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			//Apply better looking ripple for buttons on Android higher than API 21.
			int[] attrs = new int[]{android.R.attr.selectableItemBackgroundBorderless};
			TypedArray typedArray = getApplicationContext().obtainStyledAttributes(attrs);
			int bg = typedArray.getResourceId(0, 0);
			btnNext.setBackgroundResource(bg);
			btnNightMode.setBackgroundResource(bg);
			typedArray.recycle();
		}
		btnNext.setOnClickListener(this);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt("active_item", activeItem);
	}

	@Override
	protected void onRestoreInstanceState(Bundle state) {
		super.onRestoreInstanceState(state);
		activeItem = state.getInt("active_item", 4);
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
		if (v.getId() == R.id.btnNext) {
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
}
