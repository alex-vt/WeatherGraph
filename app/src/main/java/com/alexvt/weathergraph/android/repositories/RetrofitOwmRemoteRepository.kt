/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.alexvt.weathergraph.android.repositories

import android.content.Context
import com.alexvt.weathergraph.BuildConfig
import com.alexvt.weathergraph.repositories.LogRepository
import com.alexvt.weathergraph.repositories.OwmRemoteRepository
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.Cache
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import javax.inject.Inject


class RetrofitOwmRemoteRepository @Inject constructor(
    private val context: Context,
    private val log: LogRepository
) : OwmRemoteRepository {

    override fun getProviderName() = "OpenWeatherMap"

    override fun getProviderLink() = "https://openweathermap.org/api"

    @JsonClass(generateAdapter = true)
    internal data class WeatherData(val list: List<DataPoint>)

    @JsonClass(generateAdapter = true)
    internal data class Main(val temp: Double)

    @JsonClass(generateAdapter = true)
    internal data class Weather(val main: String)

    @JsonClass(generateAdapter = true)
    internal data class Clouds(val all: Int)

    @JsonClass(generateAdapter = true)
    internal data class Wind(val speed: Double)

    @JsonClass(generateAdapter = true)
    internal data class Rain(
        @field:Json(name = "1h") val _1h: Double?,
        @field:Json(name = "3h") val _3h: Double?
    )

    @JsonClass(generateAdapter = true)
    internal data class DataPoint(
        val dt: Long, val main: Main, val weather: List<Weather>,
        val clouds: Clouds, val wind: Wind, val rain: Rain?, val timezone: Long?,
        val sys: OtherInfo
    )

    @JsonClass(generateAdapter = true)
    internal data class OtherInfo(val sunrise: Long?, val sunset: Long?)

    private interface WeatherForecastService {

        companion object {
            const val basePath = "https://api.openweathermap.org/data/2.5/"
            private const val apiKey = BuildConfig.OWM_API_KEY
        }

        @GET("forecast")
        suspend fun getForecast(
            @Query("id") cityId: Int,
            @Query("appid") appId: String = apiKey
        ): WeatherData

        @GET("weather")
        suspend fun getCurrent(
            @Query("id") cityId: Int,
            @Query("appid") appId: String = apiKey
        ): DataPoint
    }

    private val service by lazy {
        val cacheSize = (5 * 1024 * 1024).toLong()
        val cache = Cache(context.cacheDir, cacheSize) // todo manage/clean cache
        val okHttpClient = OkHttpClient.Builder()
            .cache(cache)
            .addInterceptor { chain ->
                val request = chain.request()
                val response = chain.proceed(request)
                log.v(
                    "Outward (network ${cache.networkCount()}, " +
                            "cached ${cache.hitCount()}): ${request.url()} -> ${response.code()}"
                )
                response
            }
            .addNetworkInterceptor { chain ->
                val cacheMaxAgeSeconds = 3
                val request = chain.request()
                val response = chain.proceed(chain.request())
                log.v(
                    "Network (network ${cache.networkCount()}, " +
                            "cached ${cache.hitCount()}): ${request.url()} -> ${response.code()}"
                )
                response.newBuilder()
                    .header("Cache-Control", "public, max-age=$cacheMaxAgeSeconds")
                    .removeHeader("Pragma")
                    .build()
            }
            .build()

        Retrofit.Builder()
            .baseUrl(WeatherForecastService.basePath)
            .addConverterFactory(MoshiConverterFactory.create())
            .client(okHttpClient)
            .build()
            .create(WeatherForecastService::class.java)
    }


    private suspend inline fun <R> getForecast(locationId: Int, transform: (DataPoint) -> R) =
        service.getForecast(locationId).list.map { transform(it) }

    private suspend inline fun <R> getCurrent(locationId: Int, transform: (DataPoint) -> R) =
        service.getCurrent(locationId).let { transform(it) }

    private suspend inline fun <R> getAll(locationId: Int, transform: (DataPoint) -> R) =
        listOf(getCurrent(locationId, transform)) + getForecast(locationId, transform)

    private fun Rain?.getRainMmHour() = this?._3h?.let { it / 3 } ?: this?._1h ?: 0.0


    override suspend fun getTemperatureKelvinPoints(locationId: Int) =
        getAll(locationId) { Pair(it.main.temp, it.dt * 1000) }

    override suspend fun getCloudPercentPoints(locationId: Int) =
        getAll(locationId) { Pair(it.clouds.all, it.dt * 1000) }

    override suspend fun getPrecipitationMmPoints(locationId: Int) =
        getAll(locationId) { Pair(it.rain.getRainMmHour(), it.dt * 1000) }

    override suspend fun getWindMsPoints(locationId: Int) =
        getAll(locationId) { Pair(it.wind.speed, it.dt * 1000) }

    override suspend fun getTemperatureKelvinNow(locationId: Int) =
        getCurrent(locationId) { Pair(it.main.temp, it.dt * 1000) }

    override suspend fun getCloudPercentNow(locationId: Int) =
        getCurrent(locationId) { Pair(it.clouds.all, it.dt * 1000) }

    override suspend fun getPrecipitationMmNow(locationId: Int) =
        getCurrent(locationId) { Pair(it.rain.getRainMmHour(), it.dt * 1000) }

    override suspend fun getWindMsNow(locationId: Int) =
        getCurrent(locationId) { Pair(it.wind.speed, it.dt * 1000) }

    override suspend fun getSunriseSunset(locationId: Int) =
        getCurrent(locationId) {
            Pair(
                it.sys.sunrise!! * 1000,
                it.sys.sunset!! * 1000
            )
        } // here it's always

    override suspend fun getTimezoneShift(locationId: Int) =
        getCurrent(locationId) { it.timezone!! * 1000 } // here it's always
}