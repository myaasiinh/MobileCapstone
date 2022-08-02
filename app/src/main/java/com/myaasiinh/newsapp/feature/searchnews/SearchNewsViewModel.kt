package com.myaasiinh.newsapp.feature.searchnews

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchNewsViewModel @Inject constructor(
    private val repository: com.myaasiinh.core.data.NewsRepository,
    state: SavedStateHandle
) : ViewModel() {
    private val currentQuery = state.getLiveData<String?>("currentQuery", null)

    private var refreshOnInit = false

    val hasCurrentQuery = currentQuery.asFlow().map { it != null }

    val searchResult = currentQuery.asFlow().flatMapLatest { query ->
        query?.let {
            repository.getSearchResultPaged(query, refreshOnInit)
        } ?: emptyFlow()
    }.cachedIn(viewModelScope)

    var refreshInProgress = false // boolean to Snackbar in SearchNews Fragment
    var pendingScrollToTopAfterRefresh = false
    var newQueryInProgress = false
    var pendingScrollToTopAfterNewQuery = false

    fun onSearchQuerySubmit(queryString: String) {
        refreshOnInit = true
        currentQuery.value = queryString
        newQueryInProgress = true
        pendingScrollToTopAfterNewQuery = true
    }

    fun onBookmarkClick(article: com.myaasiinh.core.data.NewsArticle) {
        val currentlyBookmarked = article.isBookmarked
        val updatedArticle = article.copy(isBookmarked = !currentlyBookmarked)
        viewModelScope.launch {
            repository.updateArticle(updatedArticle)
        }
    }
}