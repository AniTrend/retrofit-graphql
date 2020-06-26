package co.anitrend.retrofit.graphql.data.user.datasource.local

import androidx.room.Dao
import androidx.room.Query
import co.anitrend.retrofit.graphql.data.arch.database.common.ILocalSource
import co.anitrend.retrofit.graphql.data.user.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
internal interface UserLocalSource : ILocalSource<UserEntity> {

    @Query("""
        select count(id) 
        from users 
    """)
    override suspend fun count(): Int

    @Query("""
        delete from users
    """)
    override suspend fun clear()

    @Query("""
        select * 
        from users 
        where id = :id
    """)
    fun getUserById(id: String): Flow<UserEntity?>

    @Query("""
        select * 
        from users 
        limit 1
    """)
    fun getDefaultUser(): Flow<UserEntity?>
}