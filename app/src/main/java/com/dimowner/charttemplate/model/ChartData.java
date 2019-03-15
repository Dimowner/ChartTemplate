package com.dimowner.charttemplate.model;

import java.util.Arrays;

public class ChartData {

	private long[] time;
	private int[][] columns;
	private String[] names;
	private String[] types;
	private String[] colors;

	public ChartData(long[] time, int[][] columns, String[] names, String[] types, String[] colors) {
		this.time = time;
		this.columns = columns;
		this.names = names;
		this.types = types;
		this.colors = colors;
	}

	public long[] getTime() {
		return time;
	}

	public int[][] getColumns() {
		return columns;
	}

	public String[] getNames() {
		return names;
	}

	public String[] getTypes() {
		return types;
	}

	public String[] getColors() {
		return colors;
	}

	public int[] getValues(int p) {
		return columns[p];
	}

	public int getLength() {
		return time.length;
	}

	@Override
	public String toString() {
		return "ChartData{" +
				"time=" + Arrays.toString(time) +
				", columns=" + Arrays.toString(columns) +
				", names=" + Arrays.toString(names) +
				", types=" + Arrays.toString(types) +
				", colors=" + Arrays.toString(colors) +
				'}';
	}
}
