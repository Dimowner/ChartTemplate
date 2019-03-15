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
import android.widget.SeekBar;

import com.dimowner.charttemplate.model.ChartData;
import com.dimowner.charttemplate.model.Data;
import com.dimowner.charttemplate.model.DataArray;
import com.dimowner.charttemplate.widget.ChartView;
import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import timber.log.Timber;

public class MainActivity extends Activity {

	private ChartData data = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		SeekBar seekBarScale = findViewById(R.id.seekBarScale);
		SeekBar seekBarScroll = findViewById(R.id.seekBarScroll);
		final ChartView chartView = findViewById(R.id.chartView);
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
					int val = progress * data.getLength()*ChartView.getStep() / 1000;
					chartView.seekPx(val);
				}
			}
			@Override public void onStartTrackingTouch(SeekBar seekBar) { }
			@Override public void onStopTrackingTouch(SeekBar seekBar) { }
		});
		readDemoData();
		chartView.setData(data);
	}

	public void readDemoData() {
		String json;
		try {
			InputStream is = getAssets().open("telegram_chart_data.json");
			int size = is.available();
			byte[] buffer = new byte[size];
			is.read(buffer);
			is.close();
			json = new String(buffer, "UTF-8");

			Gson gson = new Gson();
			DataArray dataArray = gson.fromJson(json, DataArray.class);
			if (dataArray != null) {
				Timber.v("READ DATA = %s", dataArray.toString());
				Timber.v("Test color y0 = %s", dataArray.getDataArray()[0].getColors().get("y0"));
				Timber.v("Test names y0 = %s", dataArray.getDataArray()[0].getNames().get("y0"));
				Timber.v("Test types y0 = %s", dataArray.getDataArray()[0].getTypes().get("y0"));
				Timber.v("Test types x = %s", dataArray.getDataArray()[0].getTypes().get("x"));
				Timber.v("Test column x = %s", (String) dataArray.getDataArray()[0].getColumns()[0][0]);

				Timber.v("TimeLine = %s", Arrays.toString(dataArray.getDataArray()[0].getTimeArray()));
				Timber.v("ColumnCount = %s", dataArray.getDataArray()[0].getColumnCount());
				Timber.v("ColumnKeys = %s", Arrays.toString(dataArray.getDataArray()[0].getColumnsKeys()));

				String key0 = dataArray.getDataArray()[0].getColumnsKeys()[0];
				Timber.v("Color0 = " + dataArray.getDataArray()[0].getColor(key0)
						+ ", Name = " + dataArray.getDataArray()[0].getName(key0)
						+ ", Type = " + dataArray.getDataArray()[0].getType(key0));
				Timber.v("Values = %s", Arrays.toString(dataArray.getDataArray()[0].getValues(key0)));

				Data d = dataArray.getDataArray()[0];
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

				data = new ChartData(d.getTimeArray(), vals, names, types, colors);
				Timber.v("Data = %s", data.toString());
			}
		} catch (IOException | ClassCastException ex) {
			Timber.e(ex);
		}
	}
}
