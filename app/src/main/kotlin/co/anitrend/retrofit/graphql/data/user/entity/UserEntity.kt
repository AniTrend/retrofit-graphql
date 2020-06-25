package co.anitrend.retrofit.graphql.data.user.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import co.anitrend.retrofit.graphql.domain.common.EntityId

@Entity(
    tableName = "users",
    primaryKeys = ["id"]
)
internal data class UserEntity(
    @ColumnInfo(name = "id") override val id: String,
    @ColumnInfo(name = "avatar_url") val avatarUrl: String,
    @ColumnInfo(name = "bio") val bio: String?,
    @ColumnInfo(name = "status_message") val statusMessage: String?,
    @ColumnInfo(name = "status_emoji") val statusEmoji: String?,
    @ColumnInfo(name = "username") val username: String
) : EntityId