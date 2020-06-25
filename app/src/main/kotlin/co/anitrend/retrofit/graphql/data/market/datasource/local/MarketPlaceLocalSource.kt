package co.anitrend.retrofit.graphql.data.market.datasource.local

import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Query
import co.anitrend.retrofit.graphql.data.arch.database.common.ILocalSource
import co.anitrend.retrofit.graphql.data.market.entity.MarketPlaceEntity

@Dao
internal interface MarketPlaceLocalSource : ILocalSource<MarketPlaceEntity> {

    @Query("""
        select count(id) 
        from market_place 
    """)
    override suspend fun count(): Int

    @Query("""
        delete from market_place
    """)
    override suspend fun clear()

    @Query(
        """
        select * 
        from market_place
        """
    )
    fun findAllByFactory(): DataSource.Factory<Int, MarketPlaceEntity>
}