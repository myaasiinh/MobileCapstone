package com.myaasiinh.newsapp.feature.searchnews

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import com.myaasiinh.core.util.onQueryTextSubmit
import com.myaasiinh.core.util.showIfOrInvisible
import com.myaasiinh.core.util.showSnackbar
import com.myaasiinh.newsapp.MainActivity
import com.myaasiinh.newsapp.R
import com.myaasiinh.newsapp.WebViewActivity
import com.myaasiinh.newsapp.databinding.FragmentSearchNewsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter

@AndroidEntryPoint
class SearchNewsFragment : Fragment(R.layout.fragment_search_news),
    MainActivity.OnBottomNavigationFragmentReselectedListener {
    private val viewModel: SearchNewsViewModel by viewModels()

    private var currentBinding: FragmentSearchNewsBinding? = null
    private val binding get() = currentBinding!!
    private lateinit var newsArticleAdapter: NewsArticlePagingAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        currentBinding = FragmentSearchNewsBinding.bind(view)

        newsArticleAdapter = NewsArticlePagingAdapter(
            onItemClick = { article ->
                val intent = Intent(requireContext(), WebViewActivity::class.java).putExtra(
                    "url",
                    article.url
                )
                requireActivity().startActivity(intent)
            },
            onBookmarkClick = { article ->
                viewModel.onBookmarkClick(article)
            }
        )

        binding.apply {
            recyclerView.apply {
                adapter = newsArticleAdapter.withLoadStateFooter(
                    NewsArticleLoadStateAdapter(newsArticleAdapter::retry) // function reference
                )
                layoutManager = LinearLayoutManager(requireContext())
                setHasFixedSize(true)
                itemAnimator?.changeDuration = 0
            }

            viewLifecycleOwner.lifecycleScope.launchWhenStarted {
                viewModel.searchResult.collectLatest { data ->

                    newsArticleAdapter.submitData(data)
                }
            }

            viewLifecycleOwner.lifecycleScope.launchWhenStarted {
                viewModel.hasCurrentQuery.collect { hasCurrentQuery ->
                    textViewInstructions.isVisible = !hasCurrentQuery
                    swipeRefreshLayout.isEnabled = hasCurrentQuery

                    if (!hasCurrentQuery) {
                        recyclerView.isVisible = false
                    }
                }
            }

            viewLifecycleOwner.lifecycleScope.launchWhenStarted {
                newsArticleAdapter.loadStateFlow
                    .distinctUntilChangedBy { it.source.refresh }
                    .filter { it.source.refresh is LoadState.NotLoading }
                    .collect {
                        if (viewModel.pendingScrollToTopAfterNewQuery) {
                            recyclerView.scrollToPosition(0)
                            viewModel.pendingScrollToTopAfterNewQuery = false
                        }
                        if (viewModel.pendingScrollToTopAfterRefresh &&
                            it.mediator?.refresh is LoadState.NotLoading
                        ) {
                            recyclerView.scrollToPosition(0)
                            viewModel.pendingScrollToTopAfterRefresh = false
                        }
                    }
            }

            viewLifecycleOwner.lifecycleScope.launchWhenStarted {
                newsArticleAdapter.loadStateFlow.collect { loadState ->
                    when (val refresh = loadState.mediator?.refresh) {
                        is LoadState.Loading -> {
                            textViewError.isVisible = false
                            buttonRetry.isVisible = false
                            swipeRefreshLayout.isRefreshing = true
                            textViewNoResults.isVisible = false
                            recyclerView.showIfOrInvisible {
                                !viewModel.newQueryInProgress && newsArticleAdapter.itemCount > 0
                            }

                            viewModel.refreshInProgress = true
                            viewModel.pendingScrollToTopAfterRefresh = true
                        }
                        is LoadState.NotLoading -> {
                            textViewError.isVisible = false
                            buttonRetry.isVisible = false
                            swipeRefreshLayout.isRefreshing = false
                            recyclerView.isVisible = newsArticleAdapter.itemCount > 0

                            val noResults =
                                newsArticleAdapter.itemCount < 1 && loadState.append.endOfPaginationReached &&
                                        loadState.source.append.endOfPaginationReached

                            textViewNoResults.isVisible = noResults

                            viewModel.refreshInProgress = false
                            viewModel.newQueryInProgress = false
                        }
                        is LoadState.Error -> {
                            swipeRefreshLayout.isRefreshing = false
                            textViewNoResults.isVisible = false
                            recyclerView.isVisible = newsArticleAdapter.itemCount > 0

                            val noCachedResult =
                                newsArticleAdapter.itemCount < 1 && loadState.source.append.endOfPaginationReached

                            textViewError.isVisible = noCachedResult
                            buttonRetry.isVisible = noCachedResult

                            val errorMessage = getString(
                                R.string.could_not_load_search_results,
                                refresh.error.localizedMessage ?: getString(
                                    R.string.unknown_error_occurred
                                )
                            )

                            textViewError.text = errorMessage
                            if (viewModel.refreshInProgress) {
                                showSnackbar(errorMessage)
                            }
                            viewModel.refreshInProgress = false
                            viewModel.newQueryInProgress = false
                            viewModel.pendingScrollToTopAfterRefresh = false
                        }
                        null -> {
                            // do nothing
                        }
                    }
                }
            }

            swipeRefreshLayout.setOnRefreshListener {
                newsArticleAdapter.refresh()
            }

            buttonRetry.setOnClickListener {
                newsArticleAdapter.retry()
            }
        }
        categoriesNewsQuerySearchListeners()
        setHasOptionsMenu(true)
    }

    private fun categoriesNewsQuerySearchListeners() {
        binding.apply {
            queryTechnology.setOnClickListener {
                viewModel.onSearchQuerySubmit(queryTechnology.text.toString())
                resetChipChecked()
                queryTechnology.isChecked = true
            }

            queryIndia.setOnClickListener {
                viewModel.onSearchQuerySubmit(queryIndia.text.toString())
                resetChipChecked()
                queryIndia.isChecked = true
            }

            querySports.setOnClickListener {
                viewModel.onSearchQuerySubmit(querySports.text.toString())
                resetChipChecked()
                querySports.isChecked = true
            }

            queryBusiness.setOnClickListener {
                viewModel.onSearchQuerySubmit(queryBusiness.text.toString())
                resetChipChecked()
                queryBusiness.isChecked = true
            }

            queryEntertainment.setOnClickListener {
                viewModel.onSearchQuerySubmit(queryEntertainment.text.toString())
                resetChipChecked()
                queryEntertainment.isChecked = true
            }

            queryScience.setOnClickListener {
                viewModel.onSearchQuerySubmit(queryScience.text.toString())
                resetChipChecked()
                queryScience.isChecked = true
            }

            queryHealth.setOnClickListener {
                viewModel.onSearchQuerySubmit(queryHealth.text.toString())
                resetChipChecked()
                queryHealth.isChecked = true
            }
        }
    }

    private fun resetChipChecked() {
        binding.apply {
            queryTechnology.isChecked = false
            queryIndia.isChecked = false
            querySports.isChecked = false
            queryBusiness.isChecked = false
            queryEntertainment.isChecked = false
            queryScience.isChecked = false
            queryHealth.isChecked = false
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_search_news, menu)
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem?.actionView as SearchView
        searchView.queryHint = getString(R.string.search_query_hint)

        searchView.onQueryTextSubmit { query ->
            viewModel.onSearchQuerySubmit(query)
            searchView.clearFocus()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            R.id.action_refresh -> {
                newsArticleAdapter.refresh()
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerView.adapter = null // avoid memory leak
        currentBinding = null // avoid memory leak
    }

    override fun onBottomNavigationFragmentReselected() {
        binding.recyclerView.scrollToPosition(0)
    }
}