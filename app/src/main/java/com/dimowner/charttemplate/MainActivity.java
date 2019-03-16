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
import android.widget.ImageButton;
import android.widget.SeekBar;

import com.dimowner.charttemplate.model.ChartData;
import com.dimowner.charttemplate.model.Data;
import com.dimowner.charttemplate.model.DataArray;
import com.dimowner.charttemplate.util.AndroidUtils;
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

public class MainActivity extends Activity {

	private int dataLength;

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

		SeekBar seekBarScale = findViewById(R.id.seekBarScale);
		SeekBar seekBarScroll = findViewById(R.id.seekBarScroll);
		final ChartView chartView = findViewById(R.id.chartView);
		CheckersView checkersView = findViewById(R.id.checkersView);

		final ChartData data = readDemoData();
		chartView.setData(data);
		checkersView.setData(data.getNames(), data.getColors());
		dataLength = data.getLength();

		seekBarScale.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if (fromUser) {
					if (progress < 20) {
						progress = 20;
					}
					ChartView.setStep(progress);
					chartView.invalidate();

				}
			}
			@Override public void onStartTrackingTouch(SeekBar seekBar) { }
			@Override public void onStopTrackingTouch(SeekBar seekBar) { }
		});

		seekBarScroll.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if (fromUser) {
					int val = progress * dataLength*ChartView.getStep() / 1000;
					chartView.seekPx(val);
				}
			}
			@Override public void onStartTrackingTouch(SeekBar seekBar) { }
			@Override public void onStopTrackingTouch(SeekBar seekBar) { }
		});

		checkersView.setOnChipCheckListener(new OnCheckListener() {
			@Override
			public void onChipCheck(int id, String name, boolean checked) {
				if (checked) {
					chartView.showLine(name);
				} else {
					chartView.hideLine(name);
				}
			}
		});
	}

	public ChartData readDemoData() {
		try {
			String DATA_ARRAY = "dataArray";
			String COLUMNS = "columns";
			String TYPES = "types";
			String COLORS = "colors";
			String NAMES = "names";

			JSONObject jo = new JSONObject(AndroidUtils.readJsonAsset(getApplicationContext()));
			JSONArray joArray = jo.getJSONArray(DATA_ARRAY);
			Data[] dataValues = new Data[joArray.length()];
			for (int i = 0; i < joArray.length(); i++) {
				Object[][] columns;
				Map<String, String> types;
				Map<String, String> names;
				Map<String, String> colors;
				JSONObject joItem = (JSONObject)joArray.get(i);

				names = jsonToMap(joItem.getJSONObject(NAMES));
				types = jsonToMap(joItem.getJSONObject(TYPES));
				colors = jsonToMap(joItem.getJSONObject(COLORS));

				JSONArray colArray = joItem.getJSONArray(COLUMNS);
				List<Object> list = toList(colArray);
				columns = new Object[list.size()][];

				for (int j = 0; j < list.size(); j++) {
					List<Object> l2 = (List<Object>)list.get(j);
//						Timber.v("L2 size = %s", l2.size());
					Object[] a = new Object[l2.size()];
					for (int k = 0; k < l2.size(); k++) {
						a[k] = l2.get(k);
					}
					columns[j] = a;
//					Timber.v("subArray size = " + a.length + " j = " + j);
				}
//				Timber.v("Column size = %s", columns.length);
				dataValues[i] = new Data(columns, types, names, colors);
			}

//			Gson gson = new Gson();
//			DataArray dataArray = gson.fromJson(json, DataArray.class);
			DataArray dataArray = new DataArray(dataValues);
			return toChartData(dataArray.getDataArray()[0]);
		} catch (IOException | ClassCastException | JSONException ex) {
			Timber.e(ex);
			return null;
		}
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
		String[] names2 = new String[keys.length];
		String[] types2 = new String[keys.length];
		String[] colors2 = new String[keys.length];
		for (int i = 0; i < keys.length; i++) {
			vals[i] = d.getValues(keys[i]);
			names2[i] = d.getName(keys[i]);
			types2[i] = d.getType(keys[i]);
			colors2[i] = d.getColor(keys[i]);
		}
		return new ChartData(d.getTimeArray(), vals, names2, types2, colors2);
	}
}
