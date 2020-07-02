package co.anitrend.retrofit.graphql.data.arch.database.converter

import androidx.room.TypeConverter
import co.anitrend.retrofit.graphql.data.arch.database.extensions.fromCommaSeparatedValues
import co.anitrend.retrofit.graphql.data.arch.database.extensions.toCommaSeparatedValues

internal object SampleTypeConverters {

    @TypeConverter
    @JvmStatic
    fun fromListOfString(source: List<String>) = source.toCommaSeparatedValues()

    @TypeConverter
    @JvmStatic
    fun toListOfString(source: String) = source.fromCommaSeparatedValues()
}