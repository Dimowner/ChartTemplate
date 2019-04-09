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

package com.dimowner.charttemplate.model;

import java.util.Iterator;
import java.util.Map;

public class Data {

	public static final String TIME_ARRAY_NAME = "x";

	private Object[][] columns;

	private Map<String, String> types;
	private Map<String, String> names;
	private Map<String, String> colors;
	private boolean y_scaled;

	public Data(Object[][] columns, Map<String, String> types, Map<String, String> names,
					Map<String, String> colors, boolean y_scaled) {
		this.columns = columns;
		this.types = types;
		this.names = names;
		this.colors = colors;
		this.y_scaled = y_scaled;
	}

	public Object[][] getColumns() {
		return columns;
	}

	public Map<String, String> getTypes() {
		return types;
	}

	public Map<String, String> getNames() {
		return names;
	}

	public Map<String, String> getColors() {
		return colors;
	}

	public long[] getTimeArray() throws ClassCastException {
		if (columns != null) {
			int pos = -1;
			for (int i = 0; i < columns.length; i++) {
				if (columns[i].length > 0 && columns[i][0].equals(TIME_ARRAY_NAME)) {
					pos = i;
				}
			}
			if (pos >= 0 && columns[pos].length > 1) {
				long array[] = new long[columns[pos].length - 1];
				for (int i = 1; i < columns[pos].length; i++) {
					if (columns[pos][i] instanceof Double) {
						array[i - 1] = ((Double) columns[pos][i]).longValue();
					} else {
						array[i - 1] = ((long) columns[pos][i]);
					}
				}
				return array;
			}
		}
		return new long[0];
	}

	public int getColumnCount() {
		if (names != null) {
			return names.size();
		}
		return 0;
	}

	public String[] getColumnsKeys() {
		if (names != null) {
			Iterator<String> iterator = names.keySet().iterator();
			String[] keys = new String[names.keySet().size()];
			int i = 0;
			while (iterator.hasNext()) {
				keys[i] = iterator.next();
				i++;
			}
			return keys;
		} else {
			return new String[0];
		}
	}

	public String getColor(String key) {
		if (colors != null) {
			return colors.get(key);
		}
		return "";
	}

	public String getName(String key) {
		if (names != null) {
			return names.get(key);
		}
		return "";
	}

	public String getType(String key) {
		if (types != null) {
			return types.get(key);
		}
		return "";
	}

	public int[] getValues(String key) throws ClassCastException {
		if (columns != null) {
			int pos = -1;
			for (int i = 0; i < columns.length; i++) {
				if (columns[i].length > 0 && columns[i][0].equals(key)) {
					pos = i;
				}
			}
			if (pos >= 0 && columns[pos].length > 1) {
				int array[] = new int[columns[pos].length - 1];
				for (int i = 1; i < columns[pos].length; i++) {
					if (columns[pos][i] instanceof Double) {
						array[i - 1] = ((Double) columns[pos][i]).intValue();
					} else {
						array[i - 1] = ((int) columns[pos][i]);
					}
				}
				return array;
			}
		}
		return new int[0];
	}

	public int getDataLength() {
		if (columns != null && columns.length > 0 && columns[0].length > 0) {
			return columns[0].length-1;
		}
		return 0;
	}

	public boolean isYscaled() {
		return y_scaled;
	}
}
