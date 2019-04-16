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

package com.dimowner.charttemplate.util;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.dimowner.charttemplate.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

public class AndroidUtils {

	private AndroidUtils() {}

	private static float DENSITY = 1;

	public static void update(Context context) {
		DENSITY = context.getResources().getDisplayMetrics().density;
	}

	/**
	 * Convert density independent pixels value (dip) into pixels value (px).
	 * @param dp Value needed to convert
	 * @return Converted value in pixels.
	 */
	public static float dpToPx(int dp) {
		return dp*DENSITY;
	}

	public static float getScreenWidth(Context context) {
		WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		Point size = new Point();
		if (wm != null) {
			wm.getDefaultDisplay().getSize(size);
			return size.x;
		}
		return 0;
	}

	public static String readAsset(Context context, String name) throws FileNotFoundException, IOException {
		InputStream is = context.getAssets().open(name);
		int size = is.available();
		byte[] buffer = new byte[size];
		int i = is.read(buffer);
		is.close();
		if (i > 0) {
			return new String(buffer, "UTF-8");
		} else {
			return "";
		}
	}

	public static String readData(Context context) {
		try {
			InputStream openRawResource = context.getResources().openRawResource(R.raw.telegram_chart_data);
			byte[] bArr = new byte[openRawResource.available()];
			openRawResource.read(bArr);
			openRawResource.close();
			return new String(bArr);
		} catch (IOException e) {
			Timber.e(e);
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

	public static int statusBarLightMode(Activity activity) {
		int result = 0;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
				&& Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
			if (MIUISetStatusBarLightMode(activity, true)) {
				result = 1;
			} else if (flymeSetStatusBarLightMode(activity.getWindow(), true)) {
				result = 2;
			}
//			else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//				activity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
//				result = 3;
//			}
		}
		return result;
	}


	public static boolean flymeSetStatusBarLightMode(Window window, boolean dark) {
		boolean result = false;
		if (window != null) {
			try {
				WindowManager.LayoutParams lp = window.getAttributes();
				Field darkFlag = WindowManager.LayoutParams.class
						.getDeclaredField("MEIZU_FLAG_DARK_STATUS_BAR_ICON");
				Field meizuFlags = WindowManager.LayoutParams.class
						.getDeclaredField("meizuFlags");
				darkFlag.setAccessible(true);
				meizuFlags.setAccessible(true);
				int bit = darkFlag.getInt(null);
				int value = meizuFlags.getInt(lp);
				if (dark) {
					value |= bit;
				} else {
					value &= ~bit;
				}
				meizuFlags.setInt(lp, value);
				window.setAttributes(lp);
				result = true;
			} catch (Exception e) {

			}
		}
		return result;
	}

	public static boolean MIUISetStatusBarLightMode(Activity activity, boolean dark) {
		boolean result = false;
		Window window = activity.getWindow();
		if (window != null) {
			Class clazz = window.getClass();
			try {
				int darkModeFlag = 0;
				Class layoutParams = Class.forName("android.view.MiuiWindowManager$LayoutParams");
				Field field = layoutParams.getField("EXTRA_FLAG_STATUS_BAR_DARK_MODE");
				darkModeFlag = field.getInt(layoutParams);
				Method extraFlagField = clazz.getMethod("setExtraFlags", int.class, int.class);
				if (dark) {
					extraFlagField.invoke(window, darkModeFlag, darkModeFlag);
				} else {
					extraFlagField.invoke(window, 0, darkModeFlag);
				}
				result = true;

				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
					if (dark) {
						activity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
					} else {
						activity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return result;
	}
}
