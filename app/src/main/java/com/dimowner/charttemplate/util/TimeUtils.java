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
import java.util.Date;
import java.util.Locale;

public class TimeUtils {

    /** Date format: May 16, 15:30 */
    private static SimpleDateFormat dateTimeFormat24H = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());

    /** Date format: May 16 */
    private static SimpleDateFormat dateFormat24H = new SimpleDateFormat("MMM dd", Locale.getDefault());

    /** Time format: 22.11.2019 */
    private static SimpleDateFormat dateFormatEU = new SimpleDateFormat("dd.mm.yyyy", Locale.getDefault());

    private TimeUtils() {
    }

    public static String formatDate(long time) {
        if (time <= 0) {
            return "Wrong date!";
        }
        return dateFormat24H.format(new Date(time));
    }

    public static String formatDate(Date date) {
        if (date == null) {
            return "Wrong date!";
        }
        return dateFormat24H.format(date);
    }
}
