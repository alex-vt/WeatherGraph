/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.alexvt.weathergraph.android.repositories

import android.content.Context
import androidx.room.*
import com.alexvt.weathergraph.entities.WeatherWidget
import com.alexvt.weathergraph.repositories.LogRepository
import com.alexvt.weathergraph.repositories.WeatherWidgetLocalRepository
import com.squareup.moshi.*
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.reactivex.Observable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import javax.inject.Inject

@Entity
data class WeatherWidgetEntry(
    @PrimaryKey val id: Int,
    val widgetJsonString: String
)

@Dao
interface WeatherWidgetEntryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addOrUpdate(entry: WeatherWidgetEntry)

    @Query("SELECT * FROM weatherWidgetEntry")
    fun getAll(): List<WeatherWidgetEntry>

    @Query("SELECT * FROM weatherWidgetEntry")
    fun observeAll(): Observable<List<WeatherWidgetEntry>>

    @Query("DELETE FROM weatherWidgetEntry WHERE id = :id")
    fun delete(id: Int)
}

@Database(entities = [WeatherWidgetEntry::class], version = 1)
abstract class WeatherWidgetRoomDatabase : RoomDatabase() {
    abstract fun weatherWidgetEntryDao(): WeatherWidgetEntryDao
}

class RoomWeatherWidgetLocalRepository @Inject constructor(
    context: Context,
    private val log: LogRepository
) : WeatherWidgetLocalRepository {

    private val database by lazy {
        Room.databaseBuilder(
            context, WeatherWidgetRoomDatabase::class.java,
            WeatherWidgetRoomDatabase::class.java.simpleName
        ).build()
    }

    //init { runBlocking(Dispatchers.Default) { database.clearAllTables() } }

    internal class PairAdapter(
        private val firstAdapter: JsonAdapter<Any>,
        private val secondAdapter: JsonAdapter<Any>,
        private val listAdapter: JsonAdapter<List<String>>
    ) : JsonAdapter<Pair<Any, Any>>() {

        override fun toJson(writer: JsonWriter, value: Pair<Any, Any>?) {
            value ?: throw NullPointerException("value == null")

            writer.beginArray()
            firstAdapter.toJson(writer, value.first)
            secondAdapter.toJson(writer, value.second)
            writer.endArray()
        }

        override fun fromJson(reader: JsonReader): Pair<Any, Any>? {
            val list = listAdapter.fromJson(reader) ?: return null

            require(list.size == 2) { "Pair with more or less than two elements: $list" }

            val first = firstAdapter.fromJsonValue(list[0])
                ?: throw IllegalStateException("Pair without first")
            val second = secondAdapter.fromJsonValue(list[1])
                ?: throw IllegalStateException("Pair without second")

            return first to second
        }
    }

    class PairAdapterFactory : JsonAdapter.Factory {

        override fun create(
            type: Type,
            annotations: MutableSet<out Annotation>,
            moshi: Moshi
        ): JsonAdapter<*>? {
            if (type !is ParameterizedType || Pair::class.java != type.rawType) return null

            val listType = Types.newParameterizedType(List::class.java, String::class.java)
            val listAdapter = moshi.adapter<List<String>>(listType)

            return PairAdapter(
                moshi.adapter(type.actualTypeArguments[0]),
                moshi.adapter(type.actualTypeArguments[1]),
                listAdapter
            )
        }
    }

    private val jsonMapper by lazy {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .add(PairAdapterFactory())
            .build()
            .adapter(WeatherWidget::class.java)
    }

    private fun WeatherWidget.toJson() = measureTimeMillis {
        jsonMapper.toJson(this)
    }

    private fun String.fromJson() = measureTimeMillis {
        jsonMapper.fromJson(this)!!
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
            database.weatherWidgetEntryDao().getAll().map { it.widgetJsonString.fromJson() }
        }
    }

    override fun observeAll(): Observable<List<WeatherWidget>> =
        database.weatherWidgetEntryDao().observeAll().map {
            it.map { it.widgetJsonString.fromJson() }.toList()
        }

    override fun addOrUpdate(widget: WeatherWidget) = measureTimeMillis {
        runBlocking(Dispatchers.Default) {
            WeatherWidgetEntry(widget.widgetId, widget.toJson()).let {
                database.weatherWidgetEntryDao().addOrUpdate(it)
                log.v("added or updated $it")
            }.let {
                widget
            }
        }
    }

    override fun remove(widget: WeatherWidget) = measureTimeMillis {
        runBlocking(Dispatchers.Default) {
            database.weatherWidgetEntryDao().delete(widget.widgetId)
        }
    }

}