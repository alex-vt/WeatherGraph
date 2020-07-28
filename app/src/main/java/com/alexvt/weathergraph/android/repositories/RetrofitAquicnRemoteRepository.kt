/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.alexvt.weathergraph.android.repositories

import android.os.Build
import com.alexvt.weathergraph.BuildConfig
import com.alexvt.weathergraph.repositories.AquicnRemoteRepository
import com.squareup.moshi.JsonClass
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

class RetrofitAquicnRemoteRepository : AquicnRemoteRepository {

    override fun getProviderName() = "World Air Quality Index"

    override fun getProviderLink() = "https://aqicn.org/api"

    @JsonClass(generateAdapter = true)
    internal data class AqiData(val data: Data)

    @JsonClass(generateAdapter = true)
    internal data class Data(val aqi: Int, val time: Time)

    @JsonClass(generateAdapter = true)
    internal data class Time(val v: Long)

    private interface AqiService {
        companion object {
            const val basePath = "https://api.waqi.info/feed/"
            private const val apiKey = BuildConfig.AQICN_API_KEY
        }

        @GET("{location}/")
        suspend fun getAqiData(
            @Path("location") cityName: String,
            @Query("token") appId: String = apiKey
        ): AqiData

        @GET("{location}/")
        fun getAqiJson(
            @Path("location") cityName: String,
            @Query("token") appId: String = apiKey
        ): Call<ResponseBody>
    }

    private val aqiService by lazy {
        Retrofit.Builder()
            .baseUrl(AqiService.basePath)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(AqiService::class.java)
    }

    override suspend fun getAqiNow(cityName: String) =
        aqiService.getAqiData(cityName).let { Pair(it.data.aqi, it.data.time.v * 1000) }

    override fun isAqiAvailable(cityName: String) =
        !aqiService.getAqiJson(cityName).execute().body()!!.string()
            .contains("\"data\":\"Unknown station\"")

}