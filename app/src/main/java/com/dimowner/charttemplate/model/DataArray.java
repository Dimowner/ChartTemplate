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

import java.util.Arrays;

public class DataArray {
	private Data[] dataArray;

	public DataArray(Data[] dataArray) {
		this.dataArray = dataArray;
	}

	public void setDataArray(Data[] dataArray) {
		this.dataArray = dataArray;
	}

	public Data[] getDataArray() {
		return dataArray;
	}

	@Override
	public String toString() {
		return "DataArray{" +
				"dataArray=" + Arrays.toString(dataArray) +
				'}';
	}
}
