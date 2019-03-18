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

import android.content.Context;
import android.content.res.Resources;

import java.io.IOException;
import java.io.InputStream;

/**
 * Android related utilities methods.
 */
public class AndroidUtils {

	private AndroidUtils() {}

	/**
	 * Convert density independent pixels value (dip) into pixels value (px).
	 * @param dp Value needed to convert
	 * @return Converted value in pixels.
	 */
	public static float dpToPx(int dp) {
		return dpToPx((float) dp);
	}

	/**
	 * Convert density independent pixels value (dip) into pixels value (px).
	 * @param dp Value needed to convert
	 * @return Converted value in pixels.
	 */
	public static float dpToPx(float dp) {
		return (dp * Resources.getSystem().getDisplayMetrics().density);
	}

	public static String readAsset(Context context, String name) throws IOException {
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
}
