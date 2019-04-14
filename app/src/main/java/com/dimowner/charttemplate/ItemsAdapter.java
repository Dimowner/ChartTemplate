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

import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.TextView;

import com.dimowner.charttemplate.model.ChartData;
import com.dimowner.charttemplate.widget.ItemView;

import java.util.LinkedList;
import java.util.List;

import timber.log.Timber;

public class ItemsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

	private ChartData[] data;

	private List<ItemViewHolder> holders;

	private Bundle viewsState;

	private ItemClickListener itemClickListener;

	public ItemsAdapter() {
		data = new ChartData[0];
		holders = new LinkedList<>();
		viewsState = new Bundle();
	}

	@NonNull
	@Override
	public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int type) {
		View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.list_item, viewGroup, false);
		return new ItemViewHolder(v);
	}

	@Override
	public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder viewHolder, final int p) {
		final ItemViewHolder holder = (ItemViewHolder) viewHolder;
		holder.itemView.setData(data[p]);
		holder.txtTitle.setText("Chart " + data[p].getChartNum());
		if (viewsState.containsKey(holder.getKey())) {
			Timber.v("onBindViewHolder restore key = "+holder.getKey());
			holder.restoreState(viewsState.getParcelable(holder.getKey()));
		}
	}

	@Override
	public void onViewAttachedToWindow(@NonNull RecyclerView.ViewHolder holder) {
		super.onViewAttachedToWindow(holder);
		holders.add((ItemViewHolder) holder);
		Timber.v("onViewAttachedToWindow size = " + holders.size());
	}

	@Override
	public void onViewDetachedFromWindow(@NonNull RecyclerView.ViewHolder holder) {
		super.onViewDetachedFromWindow(holder);
		ItemViewHolder h = (ItemViewHolder) holder;
		holders.remove(h);
		Timber.v("onViewDetachedFromWindow size = " + holders.size());
		Timber.v("onViewDetachedFromWindow saveState key = " + h.getKey());
		if (viewsState.containsKey(h.getKey())) {
			viewsState.remove(h.getKey());
		}
		viewsState.putParcelable(h.getKey(), h.saveState());
	}

	@Override
	public int getItemCount() {
		return data.length;
	}

	public void setData(ChartData[] data) {
		this.data = data;
		notifyDataSetChanged();
		holders.clear();
	}

	public void setItemClickListener(ItemClickListener itemClickListener) {
		this.itemClickListener = itemClickListener;
	}

	public interface ItemClickListener{
		void onItemClick(View view, long id, String path, int position);
	}

	public Bundle onSaveState() {
		for (int i = 0; i < holders.size(); i++) {
			if (viewsState.containsKey(holders.get(i).getKey())) {
				viewsState.remove(holders.get(i).getKey());
			}
			viewsState.putParcelable(holders.get(i).getKey(), holders.get(i).saveState());
		}
		Timber.v("onSaveState size = " + holders.size());
		Timber.v("ADAPTER onSaveState bundle size = " + viewsState.size());
		return viewsState;
	}

	public void onRestoreState(Bundle state) {
		Timber.v("onRestoreState bundlee size = " + state.size() );
		viewsState = state;
	}

	public class ItemViewHolder extends RecyclerView.ViewHolder {

		TextView txtTitle;
		ItemView itemView;
		View view;

		ItemViewHolder(View itemView) {
			super(itemView);
			this.view = itemView;
			this.txtTitle = itemView.findViewById(R.id.txtTitle);
			this.itemView = itemView.findViewById(R.id.itemView);
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
