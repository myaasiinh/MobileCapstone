package com.myaasiinh.newsapp.data

import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.myaasiinh.newsapp.api.NewsApi
import kotlinx.coroutines.flow.first
import retrofit2.HttpException
import java.io.IOException

private const val NEWS_STARTING_PAGE_INDEX = 1

class SearchNewsRemoteMediator(
    private val searchQuery: String,
    private val newApi: NewsApi,
    private val newsArticleDb: NewsArticleDatabase,
    private val refreshOnInit: Boolean
) : RemoteMediator<Int, NewsArticle>() {

    private val newsArticleDao = newsArticleDb.newsArticleDao()
    private val searchQueryRemoteKeyDao = newsArticleDb.searchQueryRemoteKeyDao()

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, NewsArticle>
    ): MediatorResult {
        val page = when (loadType) {
            LoadType.REFRESH -> NEWS_STARTING_PAGE_INDEX
            LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
            LoadType.APPEND -> searchQueryRemoteKeyDao.getRemoteKey(searchQuery).nextPageKey
        }

        try {
            val response = newApi.searchNews(searchQuery, page, state.config.pageSize)
            val serverSearchResult = response.articles

            val bookmarkedArticles = newsArticleDao.getAllBookmarkedArticles().first()

            val searchResultArticles = serverSearchResult.map { serverSearchResultArticle ->
                val isBookmarked = bookmarkedArticles.any { bookmarkedArticle ->
                    bookmarkedArticle.url == serverSearchResultArticle.url
                }

                NewsArticle(
                    title = serverSearchResultArticle.title,
                    url = serverSearchResultArticle.url,
                    thumbnailUrl = serverSearchResultArticle.urlToImage,
                    isBookmarked = isBookmarked
                )
            }
            newsArticleDb.withTransaction {
                if (loadType == LoadType.REFRESH) {
                    newsArticleDao.deleteSearchResultForQuery(searchQuery)
                }

                val lastQueryPosition = newsArticleDao.getLastQueryPosition(searchQuery) ?: 0
                var queryPosition = lastQueryPosition + 1

                val searchResult = searchResultArticles.map { article ->
                    SearchResult(searchQuery, article.url, queryPosition++)
                }

                val nextPageKey = page + 1

                newsArticleDao.insertArticles(searchResultArticles)
                newsArticleDao.insertSearchResult(searchResult)
                searchQueryRemoteKeyDao.insertRemoteKey(
                    SearchQueryRemoteKey(searchQuery, nextPageKey)
                )
            }
            return MediatorResult.Success(endOfPaginationReached = serverSearchResult.isEmpty())
        } catch (ex: IOException) {
            return MediatorResult.Error(ex)
        } catch (ex: HttpException) {
            return MediatorResult.Error(ex)
        }
    }

    override suspend fun initialize(): InitializeAction {
        return if (refreshOnInit) {
            InitializeAction.LAUNCH_INITIAL_REFRESH
        } else {
            InitializeAction.SKIP_INITIAL_REFRESH
        }
    }
}