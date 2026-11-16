package com.tumba.ai.data.network

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Url

/**
 * Retrofit is used for lightweight discovery/health calls. Chat streaming stays on
 * OkHttp directly because SSE needs line-by-line access to the response body.
 */
interface ModelDiscoveryApi {

    @GET
    suspend fun get(
        @Url url: String,
        @Header("Authorization") authorization: String?,
        @Header("x-goog-api-key") apiKey: String?,
    ): Response<ResponseBody>
}
