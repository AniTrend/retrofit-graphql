package co.anitrend.retrofit.graphql.data.market.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import co.anitrend.retrofit.graphql.domain.common.EntityId

@Entity(
    tableName = "market_place",
    primaryKeys = ["id"],
    indices = [
        Index("cursor_id", unique = true)
    ]
)
internal data class MarketPlaceEntity(
    @ColumnInfo(name = "id") override val id: String,
    @ColumnInfo(name = "cursor_id") val cursorId: String,
    @ColumnInfo(name = "logo_url") val logoUrl: String?,
    @ColumnInfo(name = "logo_background") val logoBackground: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "categories") val categories: List<String>,
    @ColumnInfo(name = "slug") val slug: String,
    @ColumnInfo(name = "description") val description: String,
    @ColumnInfo(name = "is_paid") val isPaid: Boolean,
    @ColumnInfo(name = "is_verified") val isVerified: Boolean
) : EntityId