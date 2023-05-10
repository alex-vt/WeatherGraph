/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.alexvt.weathergraph.android.repositories

import android.content.Context
import androidx.room.*
import com.alexvt.weathergraph.entities.OwmLocation
import com.alexvt.weathergraph.repositories.KnownLocationRepository
import com.alexvt.weathergraph.repositories.LogRepository
import com.squareup.moshi.*
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@Entity
data class OwmLocationEntry(
    @PrimaryKey val id: Int,
    val longitude: Double,
    val latitude: Double,
    val name: String,
    val country: String,
    val population: Long
)

@Dao
interface OwmLocationEntryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addOrUpdate(entries: List<OwmLocationEntry>)

    @Query("SELECT * FROM owmLocationEntry")
    fun getAll(): List<OwmLocationEntry>
}

@Database(entities = [OwmLocationEntry::class], version = 3)
abstract class OwmLocationRoomDatabase : RoomDatabase() {
    abstract fun owmLocationEntryDao(): OwmLocationEntryDao
}

class RoomKnownLocationRepository @Inject constructor(
    private val context: Context,
    private val log: LogRepository
) : KnownLocationRepository {

    private val database by lazy {
        Room.databaseBuilder(
            context, OwmLocationRoomDatabase::class.java,
            OwmLocationRoomDatabase::class.java.simpleName
        )
            .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
            .createFromAssetIfExists("known.locations.db")
            .build()
    }

    private fun <T : RoomDatabase> RoomDatabase.Builder<T>.createFromAssetIfExists(
        databaseFilePath: String
    ) = try {
        context.assets.open(databaseFilePath).close()
        createFromAsset(databaseFilePath)
    } catch (t: Throwable) {
        this
    }

    init {
        runBlocking(Dispatchers.Default) {
            //database.clearAllTables()
            if (getAll().isEmpty()) {
                importFromAssetBulkList() // todo check
            }
        }
    }

    private fun importFromAssetBulkList() = context.assets.open("current.city.list.min.json")
        .bufferedReader()
        .use { it.readText() }
        .fromJson()
        .map {
            OwmLocationEntry(
                it.id, it.coord.lon, it.coord.lat, it.name, it.country, it.stat.population
            )
        }.let {
            database.owmLocationEntryDao().addOrUpdate(it)
        }.also {
            log.d("Populated location DB from assets.")
        }

    override fun getRawDataPath(): String? = database.openHelper.writableDatabase.path

    override fun getProviderName() = "OpenWeatherMap"

    override fun getProviderLink() = "https://openweathermap.org/bulk"

    @JsonClass(generateAdapter = true)
    data class RawLocation(
        val id: Int,
        val coord: RawCoordinates,
        val name: String,
        val country: String,
        val stat: RawStats
    )

    @JsonClass(generateAdapter = true)
    data class RawCoordinates(val lon: Double, val lat: Double)

    @JsonClass(generateAdapter = true)
    data class RawStats(val population: Long)

    private fun String.fromJson() = measureTimeMillis {
        log.d(this.take(1000))
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
            .adapter<List<RawLocation>>(
                Types.newParameterizedType(
                    List::class.java,
                    RawLocation::class.java
                )
            ).fromJson(this)!!
    }

    private inline fun <T> measureTimeMillis(function: () -> T): T {
        val startTime = System.currentTimeMillis()
        val result: T = function.invoke()
        val executionTime = System.currentTimeMillis() - startTime
        log.v("In $executionTime ms got $result")
        return result
    }

    override fun getAll() = measureTimeMillis {
        runBlocking(Dispatchers.Default) {
            database.owmLocationEntryDao().getAll().map {
                OwmLocation(it.id, it.longitude, it.latitude, it.name, it.country, it.population)
            }
        }
    }
}