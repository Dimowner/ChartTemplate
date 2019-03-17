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
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

public class MainActivity extends Activity implements View.OnClickListener {

	private Data[] dataArray = null;
	private int activeItem = 0;

	private ChartView chartView;
	private ChartScrollView chartScrollView;
	private CheckersView checkersView;
	private Button btnNext;
	private Button btnPrev;

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

		chartView = findViewById(R.id.chartView);
		chartScrollView = findViewById(R.id.chartScrollView);
		checkersView = findViewById(R.id.checkersView);

		setData(readDemoData(activeItem));

		chartScrollView.setOnScrollListener(new ChartScrollView.OnScrollListener() {
			@Override
			public void onScroll(float x, float size) {
				chartView.scrollPos(x, size);
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
		btnPrev = findViewById(R.id.btnPrev);
		if (activeItem == 0) {
			btnPrev.setVisibility(View.GONE);
		}
		if (activeItem == dataArray.length-1) {
			btnNext.setVisibility(View.GONE);
		}
		btnNext.setOnClickListener(this);
		btnPrev.setOnClickListener(this);
	}

	public void setData(ChartData d) {
		chartView.setData(d);
		chartScrollView.setData(d);
		checkersView.setData(d.getNames(), d.getColors());
	}

	public ChartData readDemoData(int pos) {
		if (dataArray == null) {
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

					names = jsonToMap(joItem.getJSONObject(NAMES));
					types = jsonToMap(joItem.getJSONObject(TYPES));
					colors = jsonToMap(joItem.getJSONObject(COLORS));

					JSONArray colArray = joItem.getJSONArray(COLUMNS);
					List<Object> list = toList(colArray);
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

//				Gson gson = new Gson();
//				DataArray array = gson.fromJson(json, DataArray.class);
//				dataArray = array.getDataArray();
				dataArray = dataValues;
			} catch (IOException | ClassCastException | JSONException ex) {
				Timber.e(ex);
				return null;
			}
		}
		return toChartData(dataArray[pos]);
	}

	public static Map<String, String> jsonToMap(JSONObject json) throws JSONException {
		Map<String, String> retMap = new HashMap<>();

		if(json != JSONObject.NULL) {
			retMap = toMap(json);
		}
		return retMap;
	}

	public static Map<String, String> toMap(JSONObject object) throws JSONException {
		Map<String, String> map = new HashMap<>();

		Iterator<String> keysItr = object.keys();
		while(keysItr.hasNext()) {
			String key = keysItr.next();
			String value = (String) object.get(key);
			map.put(key, value);
		}
		return map;
	}

	public static List<Object> toList(JSONArray array) throws JSONException {
		List<Object> list = new ArrayList<>();
		for(int i = 0; i < array.length(); i++) {
			Object value = array.get(i);
			if(value instanceof JSONArray) {
				value = toList((JSONArray) value);
			}

			else if(value instanceof JSONObject) {
				value = toMap((JSONObject) value);
			}
			list.add(value);
		}
		return list;
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
			activeItem++;
			if (activeItem >= dataArray.length-1) {
				activeItem = dataArray.length-1;
				btnNext.setVisibility(View.GONE);
			}
			if (btnPrev.getVisibility() == View.GONE) {
				btnPrev.setVisibility(View.VISIBLE);
			}
			if (activeItem != prev) {
				setData(toChartData(dataArray[activeItem]));
			}
		} else if (v.getId() == R.id.btnPrev) {
			int prev = activeItem;
			activeItem--;
			if (activeItem <= 0) {
				activeItem = 0;
				btnPrev.setVisibility(View.GONE);
			}
			if (btnNext.getVisibility() == View.GONE) {
				btnNext.setVisibility(View.VISIBLE);
			}
			if (activeItem != prev) {
				setData(toChartData(dataArray[activeItem]));
			}
		}
		Timber.v("Active item change = %s", activeItem);
	}
}
