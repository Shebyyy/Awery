package com.mrboomdev.awery.ui.fragments;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ConcatAdapter;

import com.mrboomdev.awery.AweryApp;
import com.mrboomdev.awery.catalog.anilist.AnilistApi;
import com.mrboomdev.awery.catalog.anilist.query.AnilistQuery;
import com.mrboomdev.awery.catalog.anilist.query.AnilistTagsQuery;
import com.mrboomdev.awery.catalog.anilist.query.AnilistTrendingQuery;
import com.mrboomdev.awery.ui.adapter.MediaCategoriesAdapter;
import com.mrboomdev.awery.ui.adapter.MediaPagerAdapter;
import com.mrboomdev.awery.util.ObservableList;

import java.util.List;

public class AnimeFragment extends MediaCatalogFragment {
	private final MediaPagerAdapter pagerAdapter = new MediaPagerAdapter();
	private final MediaCategoriesAdapter categoriesAdapter = new MediaCategoriesAdapter();

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		var config = new ConcatAdapter.Config.Builder()
				.setIsolateViewTypes(true)
				.setStableIdMode(ConcatAdapter.Config.StableIdMode.ISOLATED_STABLE_IDS)
				.build();

		var concatAdapter = new ConcatAdapter(config, pagerAdapter, categoriesAdapter);
		getBinding().catalogCategories.setHasFixedSize(true);
		getBinding().catalogCategories.setAdapter(concatAdapter);

		loadData();
		getBinding().swipeRefresher.setOnRefreshListener(this::loadData);
	}

	@SuppressLint("NotifyDataSetChanged")
	private void loadData() {
		AnilistTrendingQuery.getAnime().executeQuery(items -> {
			requireActivity().runOnUiThread(() -> pagerAdapter.setItems(items));
		}).catchExceptions(e -> {
			AweryApp.toast("Failed to load data", 1);
			e.printStackTrace();
		}).onFinally(() -> getBinding().swipeRefresher.setRefreshing(false));

		var cats = ObservableList.of(
				new MediaCategoriesAdapter.Category("Trending"),
				new MediaCategoriesAdapter.Category("Recommended"),
				new MediaCategoriesAdapter.Category("Popular")
		);

		categoriesAdapter.setCategories(cats);

		AnilistTrendingQuery.getAnime().executeQuery(items -> requireActivity().runOnUiThread(() -> {
			cats.get(0).setItems(items);
		})).catchExceptions(e -> {
			AweryApp.toast("Failed to load data", 1);
			e.printStackTrace();
		}).onFinally(() -> getBinding().swipeRefresher.setRefreshing(false));

		AnilistTrendingQuery.getAnime().executeQuery(items -> requireActivity().runOnUiThread(() -> {
			cats.get(1).setItems(items);
		})).catchExceptions(e -> {
			AweryApp.toast("Failed to load data", 1);
			e.printStackTrace();
		}).onFinally(() -> getBinding().swipeRefresher.setRefreshing(false));

		AnilistTrendingQuery.getAnime().executeQuery(items -> requireActivity().runOnUiThread(() -> {
			cats.get(2).setItems(items);
		})).catchExceptions(e -> {
			AweryApp.toast("Failed to load data", 1);
			e.printStackTrace();
		}).onFinally(() -> getBinding().swipeRefresher.setRefreshing(false));
	}
}