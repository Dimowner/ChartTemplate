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

	public Data(Object[][] columns, Map<String, String> types, Map<String, String> names, Map<String, String> colors) {
		this.columns = columns;
		this.types = types;
		this.names = names;
		this.colors = colors;
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
		int pos = -1;
		for (int i = 0; i < columns.length; i++) {
			if (columns[i].length > 0 && columns[i][0].equals(TIME_ARRAY_NAME)) {
				pos = i;
			}
		}
		if (pos >= 0 && columns[pos].length > 1) {
			long array[] = new long[columns[pos].length - 1];
			for (int i = 1; i < columns[pos].length; i++) {
				array[i - 1] = ((Double) columns[pos][i]).longValue();
			}
			return array;
		}
		return new long[0];
	}

	public int getColumnCount() {
		return names.size();
	}

	public String[] getColumnsKeys() {
		Iterator<String> iterator = names.keySet().iterator();
		String[] keys = new String[names.keySet().size()];
		int i = 0;
		while (iterator.hasNext()) {
			keys[i] = iterator.next();
			i++;
		}
		return keys;
	}

	public String getColor(String key) {
		return colors.get(key);
	}

	public String getName(String key) {
		return names.get(key);
	}

	public String getType(String key) {
		return types.get(key);
	}

	public int[] getValues(String key) throws ClassCastException {
		int pos = -1;
		for (int i = 0; i < columns.length; i++) {
			if (columns[i].length > 0 && columns[i][0].equals(key)) {
				pos = i;
			}
		}
		if (pos >= 0 && columns[pos].length > 1) {
			int array[] = new int [columns[pos].length - 1];
			for (int i = 1; i < columns[pos].length; i++) {
				array[i - 1] = ((Double) columns[pos][i]).intValue();
			}
			return array;
		}
		return new int[0];
	}
}
