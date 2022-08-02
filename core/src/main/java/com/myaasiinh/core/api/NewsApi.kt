package com.myaasiinh.core.api

import com.myaasiinh.core.util.Constant
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

/**
 * Retrofit Interface for API call
 * */
interface NewsApi {
    companion object {
        const val BASE_URL = Constant.NEWS_API_BASE_URL
        const val API_KEY = Constant.NEWS_API_ACCESS_KEY

    }

    /**
     * suspend fun call another thread, without freezing the main thread
     * */
    @Headers("X-Api-Key: $API_KEY")
    @GET("top-headlines?country=us&pageSize=100")
    suspend fun getBreakingNews(): NewsResponse

    @Headers("X-Api-Key: $API_KEY")
    @GET("everything")
    suspend fun searchNews(
        @Query("q") query: String,
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int
    ): NewsResponse
}