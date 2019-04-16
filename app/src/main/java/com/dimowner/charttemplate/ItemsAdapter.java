/*
 * Copyright 2019 Dmitriy Ponomarenko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dimowner.charttemplate;

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.TextView;

import com.dimowner.charttemplate.model.ChartData;
import com.dimowner.charttemplate.util.AndroidUtils;
import com.dimowner.charttemplate.widget.ChartView;
import com.dimowner.charttemplate.widget.ItemView;

import java.util.LinkedList;
import java.util.List;

public class ItemsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

	private ChartData[] data;

	private List<ItemViewHolder> holders;

	private Bundle viewsState;

	private ItemClickListener itemClickListener;

	private ChartView.OnDetailsListener onDetailsListener;

	private int zoomOutColor;
//	private int zoomOutColorNight;
	private int titleColor;
//	private int titleColorNight;

	public ItemsAdapter() {
		data = new ChartData[0];
		holders = new LinkedList<>();
		viewsState = new Bundle();
	}

	@NonNull
	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int type) {
		Resources res = viewGroup.getContext().getResources();
		zoomOutColor = res.getColor(ColorMap.getZoomOutColor());
//		zoomOutColorNight = res.getColor(R.color.zoom_out_color_night);
		titleColor = res.getColor(ColorMap.getTittleColor());
//		titleColorNight = res.getColor(R.color.white);
		View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.list_item, viewGroup, false);
		return new ItemViewHolder(v);
	}

	@Override
	public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder viewHolder, final int p) {
		final ItemViewHolder holder = (ItemViewHolder) viewHolder;
		holder.itemView.setData(data[p]);
		if (data[p].isDetailsMode()) {
			holder.txtTitle.setText("Zoom Out");
			holder.txtTitle.setTextColor(zoomOutColor);
			holder.txtTitle.setCompoundDrawablesWithIntrinsicBounds(ColorMap.getZoomOutIcon(), 0, 0, 0);
//			if (CTApplication.isNightMode()) {
//			if (ColorMap.isNightTheme()) {
//				holder.txtTitle.setTextColor(zoomOutColorNight);
//				holder.txtTitle.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_zoom_out_night, 0, 0, 0);
//			} else {
//				holder.txtTitle.setTextColor(zoomOutColor);
//				holder.txtTitle.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_zoom_out, 0, 0, 0);
//			}
			holder.txtTitle.setCompoundDrawablePadding((int)AndroidUtils.dpToPx(6));
		} else {
			holder.txtTitle.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
			holder.txtTitle.setText("Chart " + data[p].getChartNum());
//			if (CTApplication.isNightMode()) {
//			if (ColorMap.isNightTheme()) {
//				holder.txtTitle.setTextColor(titleColorNight);
//			} else {
//				holder.txtTitle.setTextColor(titleColor);
//			}
			holder.txtTitle.setTextColor(titleColor);
		}
		holder.txtTitle.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (onDetailsListener != null && data[p].isDetailsMode()) {
					onDetailsListener.hideDetails(data[p].getChartNum());
				}
			}
		});
		if (viewsState.containsKey(holder.getKey())) {
			holder.restoreState(viewsState.getParcelable(holder.getKey()));
		}
	}

	@Override
	public void onViewAttachedToWindow(@NonNull RecyclerView.ViewHolder holder) {
		super.onViewAttachedToWindow(holder);
		holders.add((ItemViewHolder) holder);
		((ItemViewHolder)holder).itemView.setOnDetailsListener(onDetailsListener);
	}

	@Override
	public void onViewDetachedFromWindow(@NonNull RecyclerView.ViewHolder holder) {
		super.onViewDetachedFromWindow(holder);
		ItemViewHolder h = (ItemViewHolder) holder;
		h.itemView.setOnDetailsListener(null);
		holders.remove(h);
		if (viewsState.containsKey(h.getKey())) {
			viewsState.remove(h.getKey());
		}
		if (!h.getKey().contains("Zoom Out")) {
			viewsState.putParcelable(h.getKey(), h.saveState());
		}
	}

	@Override
	public int getItemCount() {
		return data.length;
	}

	public void setData(ChartData[] d) {
		this.data = new ChartData[d.length];
		for (int i = 0; i < d.length; i++) {
			data[i] = d[i];
		}
		notifyDataSetChanged();
		holders.clear();
	}

	public void setItem(int pos, ChartData item) {
		if (pos < data.length) {
			data[pos] = item;
			notifyItemChanged(pos);
			String key = "Chart " + item.getChartNum();
			if (viewsState.containsKey(key)) {
				viewsState.remove(key);
			}
		}
	}

	public void setItemClickListener(ItemClickListener itemClickListener) {
		this.itemClickListener = itemClickListener;
	}

	public void setOnDetailsListener(ChartView.OnDetailsListener onDetailsListener) {
		this.onDetailsListener = onDetailsListener;
	}

	public interface ItemClickListener{
		void onItemClick(View view, long id, String path, int position);
	}

	public Bundle onSaveState() {
		for (int i = 0; i < holders.size(); i++) {
			if (viewsState.containsKey(holders.get(i).getKey())) {
				viewsState.remove(holders.get(i).getKey());
			}
			if (!holders.get(i).getKey().contains("Zoom Out")) {
//				Timber.v("putparcelable key = " + holders.get(i).getKey());
				viewsState.putParcelable(holders.get(i).getKey(), holders.get(i).saveState());
			}
		}
//		Timber.v("onSaveState size = " + holders.size());
//		Timber.v("ADAPTER onSaveState bundle size = " + viewsState.size());
		return viewsState;
	}

	public void onRestoreState(Bundle state) {
//		Timber.v("onRestoreState bundlee size = " + state.size() );
		viewsState = state;
	}

	public class ItemViewHolder extends RecyclerView.ViewHolder {

		TextView txtTitle;
		ItemView itemView;
		View view;

		ItemViewHolder(View view) {
			super(view);
			this.view = view;
			this.txtTitle = view.findViewById(R.id.txtTitle);
			this.itemView = view.findViewById(R.id.itemView);

			Context ctx = view.getContext();
			this.itemView.findViewById(R.id.itemView).setBackgroundColor(ctx.getResources().getColor(ColorMap.getViewBackground()));

		}

		String getKey() {
			return txtTitle.getText().toString();
		}

		Parcelable saveState() {
//			Timber.v("saveState");
			return itemView.onSaveInstanceState();
		}

		void restoreState(Parcelable state) {
//			Timber.v("restoreState");
			itemView.onRestoreInstanceState(state);
		}
	}
}
