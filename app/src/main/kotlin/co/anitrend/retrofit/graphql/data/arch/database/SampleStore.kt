package co.anitrend.retrofit.graphql.data.arch.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import co.anitrend.retrofit.graphql.data.arch.database.converter.SampleTypeConverters
import co.anitrend.retrofit.graphql.data.market.entity.MarketPlaceEntity
import co.anitrend.retrofit.graphql.data.user.entity.UserEntity

@Database(
    entities = [
        MarketPlaceEntity::class, UserEntity::class
    ],
    version = SampleStore.DATABASE_SCHEMA_VERSION
)

@TypeConverters(
    value = [SampleTypeConverters::class]
)
internal abstract class SampleStore: RoomDatabase(), ISampleStore {

    companion object {
        const val DATABASE_SCHEMA_VERSION = 1

        internal fun create(applicationContext: Context): ISampleStore {
            return Room.databaseBuilder(
                applicationContext,
                SampleStore::class.java,
                "sample-db"
            ).fallbackToDestructiveMigration()
                .build()
        }
    }
}