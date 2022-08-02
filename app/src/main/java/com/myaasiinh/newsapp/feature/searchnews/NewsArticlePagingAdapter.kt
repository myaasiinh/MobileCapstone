package com.myaasiinh.newsapp.feature.searchnews

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import com.myaasiinh.newsapp.databinding.ItemNewsArticleBinding
import com.myaasiinh.newsapp.shared.NewsArticleComparator
import com.myaasiinh.newsapp.shared.NewsArticleViewHolder

class NewsArticlePagingAdapter(
    private val onItemClick: (com.myaasiinh.core.data.NewsArticle) -> Unit,
    private val onBookmarkClick: (com.myaasiinh.core.data.NewsArticle) -> Unit
) : PagingDataAdapter<com.myaasiinh.core.data.NewsArticle, NewsArticleViewHolder>(NewsArticleComparator()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsArticleViewHolder {
        val binding =
            ItemNewsArticleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NewsArticleViewHolder(
            binding, onItemClick = { position ->
                val article = getItem(position)
                if (article != null) {
                    onItemClick(article)
                }
            },
            onBookmarkClick = { position ->
                val article = getItem(position)
                if (article != null) {
                    onBookmarkClick(article)
                }
            }
        )
    }

    override fun onBindViewHolder(holder: NewsArticleViewHolder, position: Int) {
        val currentItem = getItem(position)
        if (currentItem != null) {
            holder.bind(currentItem)
        }
    }
}