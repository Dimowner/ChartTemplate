package com.dimowner.charttemplate.model;

import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;

public class ChartData implements Parcelable {

	private long[] time;
	private int[][] columns;
	private String[] names;
	private String[] types;
	private String[] colors;
	private int[] colorsInts;

	public ChartData(long[] time, int[][] columns, String[] names, String[] types, String[] colors) {
		this.time = time;
		this.columns = columns;
		this.names = names;
		this.types = types;
		this.colors = colors;
		this.colorsInts = parseColor(colors);
	}

	//----- START Parcelable implementation ----------
	private ChartData(Parcel in) {
		in.readIntArray(colorsInts);
		in.readLongArray(time);
		in.readStringArray(names);
		in.readStringArray(types);
		in.readStringArray(colors);
		int size = in.readInt();
		columns = new int[size][];
		for (int i = 0; i < size; i++) {
			in.readIntArray(columns[i]);
		}
	}

	public int describeContents() {
		return 0;
	}

	public void writeToParcel(Parcel out, int flags) {
		out.writeIntArray(colorsInts);
		out.writeLongArray(time);
		out.writeStringArray(names);
		out.writeStringArray(types);
		out.writeStringArray(colors);
		out.writeInt(columns.length);
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

	public int getLength() {
		return time.length;
	}

	public int getLinesCount() {
		return names.length;
	}
}
