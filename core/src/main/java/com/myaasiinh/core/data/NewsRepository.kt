package com.myaasiinh.core.data


import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.room.withTransaction
import com.myaasiinh.core.api.NewsApi
import com.myaasiinh.core.util.Resource
import com.myaasiinh.core.util.networkBoundResource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import retrofit2.HttpException
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class NewsRepository @Inject constructor(
    private val newsApi: NewsApi,
    private val newsArticleDb: NewsArticleDatabase
) {
    private val newsArticleDao = newsArticleDb.newsArticleDao()

    fun getBreakingNews(
        forceRefresh: Boolean,
        onFetchSuccess: () -> Unit,
        onFetchFailed: (Throwable) -> Unit
    ): Flow<Resource<List<NewsArticle>>> =
        networkBoundResource(
            query = {
                newsArticleDao.getAllBreakingNewsArticles()
            },
            fetch = {
                val response = newsApi.getBreakingNews()
                response.articles
            },
            saveFetchResult = { serverBreakingNewsArticles ->
                val bookmarkedArticle = newsArticleDao.getAllBookmarkedArticles().first()
                val breakingNewsArticles =
                    serverBreakingNewsArticles.map { serverBreakingNewsArticle ->
                        val isBookmarked = bookmarkedArticle.any { bookmarkedArticle ->
                            bookmarkedArticle.url == serverBreakingNewsArticle.url
                        }
                        NewsArticle(
                            title = serverBreakingNewsArticle.title,
                            url = serverBreakingNewsArticle.url,
                            thumbnailUrl = serverBreakingNewsArticle.urlToImage,
                            isBookmarked = isBookmarked
                        )
                    }

                val breakingNews = breakingNewsArticles.map { article -> BreakingNews(article.url) }
                // if anyone of transaction fails, all transactions will be rolled-backed
                newsArticleDb.withTransaction {
                    newsArticleDao.deleteAllBreakingNews()
                    newsArticleDao.insertArticles(breakingNewsArticles)
                    newsArticleDao.insertBreakingNews(breakingNews)
                }
            },
            shouldFetch = { cachedArticles ->
                if (forceRefresh) {
                    true
                } else {
                    val sortedArticles = cachedArticles.sortedBy { article ->
                        article.updatedAt
                    }
                    val oldestTimestamp = sortedArticles.firstOrNull()?.updatedAt
                    val needsRefresh = oldestTimestamp == null ||
                            oldestTimestamp < System.currentTimeMillis() -
                            TimeUnit.MINUTES.toMillis(60)
                    needsRefresh
                }
            },
            onFetchSuccess = onFetchSuccess,
            onFetchFailed = { t ->
                if (t !is HttpException && t !is IOException) {
                    throw t
                }
                onFetchFailed(t)
            }
        )

    @OptIn(ExperimentalPagingApi::class)
    fun getSearchResultPaged(query: String, refreshOnInit: Boolean): Flow<PagingData<NewsArticle>> =
        Pager(
            config = PagingConfig(pageSize = 20, maxSize = 200),
            remoteMediator = SearchNewsRemoteMediator(query, newsApi, newsArticleDb, refreshOnInit),
            pagingSourceFactory = { newsArticleDao.getSearchResultArticlesPaged(query) }
        ).flow

    fun getAllBookmarkedArticles(): Flow<List<NewsArticle>> =
        newsArticleDao.getAllBookmarkedArticles()

    suspend fun deleteNonBookmarkedArticlesOlderThan(timestampInMillis: Long) {
        newsArticleDao.deleteNonBookmarkedArticlesOlderThan(timestampInMillis)
    }

    suspend fun updateArticle(article: NewsArticle) {
        newsArticleDao.updateArticle(article)
    }

    suspend fun resetAllBookmarks() {
        newsArticleDao.resetAllBookmarks()
    }
}