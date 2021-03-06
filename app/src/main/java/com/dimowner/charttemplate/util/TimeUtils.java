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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class TimeUtils {

	/** Date format: 16 May */
	private static SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM", Locale.getDefault());

	/** Date format: Sat, 12 May 2019 */
	private static SimpleDateFormat dateFormatWeek = new SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault());

	/** Date format: 10 April 2019 */
	private static SimpleDateFormat dateFormatLong = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());

	/** Time format: 15:30 */
	private static SimpleDateFormat timeFormat24H = new SimpleDateFormat("HH:mm", Locale.getDefault());

	private TimeUtils() {
	}

	public static String formatDate(Date date) {
		if (date == null) {
			return "Wrong date!";
		}
		return dateFormat.format(date);
	}

	public static String formatDateWeek(Date date) {
		if (date == null) {
			return "Wrong date!";
		}
		return dateFormatWeek.format(date);
	}

	public static String formatDateLong(Date date) {
		if (date == null) {
			return "Wrong date!";
		}
		return dateFormatLong.format(date);
	}

	public static String formatTime(Date date) {
		if (date == null) {
			return "Wrong date!";
		}
		return timeFormat24H.format(date);
	}

	public static String getMonthYear(long time) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(time);
		int year = calendar.get(Calendar.YEAR);
		int month = calendar.get(Calendar.MONTH)+1;
		if (month < 10) {
			return year + "-0"+month;
		}
		return year + "-"+month;
	}

	public static String getDayOfMonth(long time) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(time);
		int day = calendar.get(Calendar.DAY_OF_MONTH);
		if (day < 10) {
			return "0"+day;
		}
		return String.valueOf(day);
	}

	public static boolean isDiffSorterThan2Days(long start, long end) {
		return  (float) (end - start)/ (24 * 60 * 60 * 1000*2) < 1; //2 days period mills
	}
}
