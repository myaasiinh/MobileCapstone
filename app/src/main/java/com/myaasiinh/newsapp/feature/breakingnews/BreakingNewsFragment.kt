package com.myaasiinh.newsapp.feature.breakingnews

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.myaasiinh.core.util.Resource
import com.myaasiinh.core.util.exhaustive
import com.myaasiinh.core.util.showSnackbar
import com.myaasiinh.newsapp.MainActivity
import com.myaasiinh.newsapp.R
import com.myaasiinh.newsapp.WebViewActivity
import com.myaasiinh.newsapp.databinding.FragmentBreakingNewsBinding
import com.myaasiinh.newsapp.shared.NewsArticleListAdapter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BreakingNewsFragment : Fragment(R.layout.fragment_breaking_news),
    MainActivity.OnBottomNavigationFragmentReselectedListener {
    private val viewModel: BreakingNewsViewModel by viewModels() /* property delegates */

    private var currentBinding: FragmentBreakingNewsBinding? = null
    private val binding get() = currentBinding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        currentBinding = FragmentBreakingNewsBinding.bind(view)

        val newsArticleAdapter = NewsArticleListAdapter(
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

        newsArticleAdapter.stateRestorationPolicy =
            RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY

        binding.apply {
            recyclerView.apply {
                adapter = newsArticleAdapter
                layoutManager = LinearLayoutManager(requireContext())
                setHasFixedSize(true)
                itemAnimator?.changeDuration = 0
            }

            viewLifecycleOwner.lifecycleScope.launchWhenStarted {
                viewModel.breakingNews.collect {
                    val result = it ?: return@collect

                    swipeRefreshLayout.isRefreshing = result is Resource.Loading
                    recyclerView.isVisible = !result.data.isNullOrEmpty()
                    textViewError.isVisible = result.error != null && result.data.isNullOrEmpty()
                    buttonRetry.isVisible = result.error != null && result.data.isNullOrEmpty()
                    textViewError.text =
                        getString(
                            R.string.could_not_refresh,
                            result.error?.localizedMessage ?: R.string.unknown_error_occurred
                        )

                    newsArticleAdapter.submitList(result.data) {
                        if (viewModel.pendingScrollToTopAfterRefresh) {
                            recyclerView.scrollToPosition(0)
                            viewModel.pendingScrollToTopAfterRefresh = false
                        }
                    }
                }
            }

            swipeRefreshLayout.setOnRefreshListener {
                viewModel.onManualRefresh()
            }

            buttonRetry.setOnClickListener {
                viewModel.onManualRefresh()
            }

            viewLifecycleOwner.lifecycleScope.launchWhenStarted {
                viewModel.events.collect { event ->
                    when (event) {
                        is BreakingNewsViewModel.Event.ShowErrorMessage -> showSnackbar(
                            getString(
                                R.string.could_not_refresh,
                                event.error.localizedMessage
                                    ?: getString(R.string.unknown_error_occurred)
                            )
                        )
                    }.exhaustive
                }
            }
        }
        setHasOptionsMenu(true)
    }

    override fun onStart() {
        super.onStart()
        viewModel.onStart()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_breaking_news, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) =
        when (item.itemId) {
            R.id.action_refresh -> {
                viewModel.onManualRefresh()
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