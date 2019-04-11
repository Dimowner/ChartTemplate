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

import android.app.Application;
import android.content.res.Configuration;

import com.dimowner.charttemplate.model.ChartData;
import com.dimowner.charttemplate.util.AndroidUtils;

import timber.log.Timber;

public class CTApplication extends Application {

	private static boolean isNightMode = false;

	private static ChartData[] chartData;

	@Override
	public void onCreate() {
		if (BuildConfig.DEBUG) {
			//Timber initialization
			Timber.plant(new Timber.DebugTree() {
				@Override
				protected String createStackElementTag(StackTraceElement element) {
					return super.createStackElementTag(element) + ":" + element.getLineNumber();
				}
			});
		}
		super.onCreate();
		AndroidUtils.update(getApplicationContext());
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		AndroidUtils.update(getApplicationContext());
	}

	public static boolean isNightMode() {
		return isNightMode;
	}

	public static void setNightMode(boolean b) {
		isNightMode = b;
	}

	public static ChartData[] getChartData() {
		return chartData;
	}

	public static void setChartData(ChartData[] chartData) {
		CTApplication.chartData = chartData;
	}
}
