package com.dimowner.charttemplate.model;

import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;

import com.dimowner.charttemplate.util.TimeUtils;

import java.util.Date;

public class ChartData implements Parcelable {

	public static final int TYPE_LINE = 1;
	public static final int TYPE_BAR = 2;
	public static final int TYPE_AREA = 3;

	private int chartNum;
	private long[] time;
	private String[] times;
	private String[] timesShort;
	private String[] timesLong;
	private int[][] columns;
	private String[] names;
	private String[] types;
	private int[] typesInt;
	private String[] colors;
	private int[] colorsInts;
	private boolean yScaled;
	private boolean percentage;
	private boolean stacked;

	public ChartData(int chartNum, long[] time, int[][] columns, String[] names, String[] types, String[] colors,
						  boolean yScaled, boolean percentage, boolean stacked) {
		this.chartNum = chartNum;
		this.time = time;
		this.columns = columns;
		this.names = names;
		this.types = types;
		this.colors = colors;
		this.colorsInts = parseColor(colors);
		times = new String[time.length];
		timesShort = new String[time.length];
		timesLong = new String[time.length];
		this.yScaled = yScaled;
		this.percentage = percentage;
		this.stacked = stacked;
		Date date = new Date();
		for (int i = 0; i < time.length; i++) {
			date.setTime(time[i]);
			times[i] = TimeUtils.formatDateWeek(date);
			timesShort[i] = TimeUtils.formatDate(date);
			timesLong[i] = TimeUtils.formatDateLong(date);
		}
		typesInt = new int[types.length];
		for (int i = 0; i < types.length; i++) {
			if (types[i].equalsIgnoreCase("line")) {
				typesInt[i] = TYPE_LINE;
			} else if (types[i].equalsIgnoreCase("bar")) {
				typesInt[i] = TYPE_BAR;
			} else if (types[i].equalsIgnoreCase("area")) {
				typesInt[i] = TYPE_AREA;
			}
		}
	}

	//----- START Parcelable implementation ----------
	private ChartData(Parcel in) {
		in.readStringArray(times);
		in.readStringArray(timesShort);
		in.readStringArray(timesLong);
		in.readIntArray(colorsInts);
		in.readLongArray(time);
		in.readStringArray(names);
		in.readStringArray(types);
		in.readStringArray(colors);
		in.readIntArray(typesInt);
		chartNum = in.readInt();
		int size = in.readInt();
		columns = new int[size][];
		for (int i = 0; i < size; i++) {
			in.readIntArray(columns[i]);
		}
		boolean[] bools = new boolean[3];
		in.readBooleanArray(bools);
		yScaled = bools[0];
		percentage = bools[1];
		stacked = bools[2];
	}

	public int describeContents() {
		return 0;
	}

	public void writeToParcel(Parcel out, int flags) {
		out.writeStringArray(times);
		out.writeStringArray(timesShort);
		out.writeStringArray(timesLong);
		out.writeIntArray(colorsInts);
		out.writeLongArray(time);
		out.writeStringArray(names);
		out.writeStringArray(types);
		out.writeStringArray(colors);
		out.writeIntArray(typesInt);
		out.writeInt(chartNum);
		out.writeInt(columns.length);

		out.writeBooleanArray(new boolean[] {yScaled, percentage, stacked});
		for (int i = 0; i < columns.length; i++) {
			out.writeIntArray(columns[i]);
		}
	}

	public static final Parcelable.Creator<ChartData> CREATOR
			= new Parcelable.Creator<ChartData>() {
		public ChartData createFromParcel(Parcel in) {
			return new ChartData(in);
		}

		public ChartData[] newArray(int size) {
			return new ChartData[size];
		}
	};
	//----- END Parcelable implementation ----------

	private int[] parseColor(String[] s) {
		int[] c = new int[s.length];
		for (int i = 0; i < s.length; i++) {
			c[i] = Color.parseColor(s[i]);
		}
		return c;
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

	public int[] getColorsInts() {
		return colorsInts;
	}

	public int[] getValues(int p) {
		return columns[p];
	}

	public String[] getTimes() {
		return times;
	}

	public String[] getTimesShort() {
		return timesShort;
	}

	public String[] getTimesLong() {
		return timesLong;
	}

	public boolean isYscaled() {
		return yScaled;
	}

	public boolean isPercentage() {
		return percentage;
	}

	public boolean isStacked() {
		return stacked;
	}

	public int getLength() {
		return times.length;
	}

	public int getLinesCount() {
		return names.length;
	}

	public int getType(int index) {
		return typesInt[index];
	}

	public int getVal(int lineIndex, int valIndex) {
		return columns[lineIndex][valIndex];
	}

	public int getChartNum() {
		return chartNum;
	}

	public void setData(int val, int lineIndex, int valIndex) {
		columns[lineIndex][valIndex] = val;
	}
}
