package com.myaasiinh.newsapp

import android.os.Bundle
import android.os.PersistableBundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.myaasiinh.newsapp.databinding.ActivityMainBinding
import com.myaasiinh.newsapp.feature.bookmarks.BookmarksFragment
import com.myaasiinh.newsapp.feature.breakingnews.BreakingNewsFragment
import com.myaasiinh.newsapp.feature.searchnews.SearchNewsFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    /**
     * Bottom Navigation Code
     * */
    private lateinit var breakingNewsFragment: BreakingNewsFragment
    private lateinit var searchNewsFragment: SearchNewsFragment
    private lateinit var bookmarksFragment: BookmarksFragment

    private val fragments: Array<Fragment>
        get() = arrayOf(breakingNewsFragment, searchNewsFragment, bookmarksFragment)

    private var selectedIndex = 0
    private val selectedFragment get() = fragments[selectedIndex]

    /**
     * Method to change the fragments
     * */
    private fun selectFragment(selectedFragment: Fragment) {
        var transaction = supportFragmentManager.beginTransaction()
        fragments.forEachIndexed { index, fragment ->
            if (selectedFragment == fragment) {
                transaction = transaction.attach(fragment)
                selectedIndex = index
            } else {
                transaction = transaction.detach(fragment)
            }
        }
        transaction.commit()
        // Setting title of the current fragment at the top of the Activity
        title = when (selectedFragment) {
            is com.myaasiinh.newsapp.feature.breakingnews.BreakingNewsFragment -> getString(R.string.title_breaking_news)
            is com.myaasiinh.newsapp.feature.searchnews.SearchNewsFragment -> getString(R.string.title_search_news)
            is com.myaasiinh.newsapp.feature.bookmarks.BookmarksFragment -> getString(R.string.title_bookmarks)
            else -> ""
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) { // Not configuration change
            breakingNewsFragment = com.myaasiinh.newsapp.feature.breakingnews.BreakingNewsFragment()
            searchNewsFragment = com.myaasiinh.newsapp.feature.searchnews.SearchNewsFragment()
            bookmarksFragment = com.myaasiinh.newsapp.feature.bookmarks.BookmarksFragment()

            supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container, breakingNewsFragment, TAG_BREAKING_NEWS_FRAGMENT)
                .add(R.id.fragment_container, searchNewsFragment, TAG_SEARCH_NEWS_FRAGMENT)
                .add(R.id.fragment_container, bookmarksFragment, TAG_BOOKMARKS_FRAGMENT)
                .commit()
        } else { // Configuration change happens
            breakingNewsFragment =
                supportFragmentManager.findFragmentByTag(TAG_BREAKING_NEWS_FRAGMENT) as com.myaasiinh.newsapp.feature.breakingnews.BreakingNewsFragment
            searchNewsFragment =
                supportFragmentManager.findFragmentByTag(TAG_SEARCH_NEWS_FRAGMENT) as com.myaasiinh.newsapp.feature.searchnews.SearchNewsFragment
            bookmarksFragment =
                supportFragmentManager.findFragmentByTag(TAG_BOOKMARKS_FRAGMENT) as com.myaasiinh.newsapp.feature.bookmarks.BookmarksFragment

            selectedIndex = savedInstanceState.getInt(KEY_SELECTED_INDEX, 0)
        }
        selectFragment(selectedFragment)

        /**
         * Setting Listener for Bottom Navigation Change by the user.
         * */
        binding.bottomNav.setOnItemSelectedListener { item ->
            val fragment = when (item.itemId) {
                R.id.nav_breaking -> breakingNewsFragment
                R.id.nav_search -> searchNewsFragment
                R.id.nav_bookmark -> bookmarksFragment
                else -> throw IllegalArgumentException("Unexpected itemId")
            }
            if (selectedFragment === fragment) { // three equals means -> comparing references
                fragment.onPrimaryNavigationFragmentChanged(true)
            } else {
                selectFragment(fragment)
            }
            true
        }
    }

    interface OnBottomNavigationFragmentReselectedListener {
        fun onBottomNavigationFragmentReselected()

    }

    override fun onBackPressed() {
        if (selectedIndex != 0) {
            binding.bottomNav.selectedItemId = R.id.nav_breaking
        } else {
            super.onBackPressed()
        }
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
        outState.putInt(KEY_SELECTED_INDEX, selectedIndex)
    }
}

private const val TAG_BREAKING_NEWS_FRAGMENT = "TAG_BREAKING_NEWS_FRAGMENT"
private const val TAG_SEARCH_NEWS_FRAGMENT = "TAG_SEARCH_NEWS_FRAGMENT"
private const val TAG_BOOKMARKS_FRAGMENT = "TAG_BOOKMARKS_FRAGMENT"
private const val KEY_SELECTED_INDEX = "KEY_SELECTED_INDEX"