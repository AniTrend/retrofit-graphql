package co.anitrend.retrofit.graphql.data.user.mapper

import co.anitrend.retrofit.graphql.data.arch.mapper.GraphQLMapper
import co.anitrend.retrofit.graphql.data.user.converters.UserModelConverter
import co.anitrend.retrofit.graphql.data.user.datasource.local.UserLocalSource
import co.anitrend.retrofit.graphql.data.user.entity.UserEntity
import co.anitrend.retrofit.graphql.data.user.model.Viewer

internal class UserResponseMapper(
    private val localSource: UserLocalSource,
    private val converter: UserModelConverter
) : GraphQLMapper<Viewer, UserEntity>() {
    /**
     * Inserts the given object into the implemented room database,
     *
     * @param mappedData mapped object from [onResponseMapFrom] to insert into the database
     */
    override suspend fun onResponseDatabaseInsert(mappedData: UserEntity) {
        localSource.upsert(mappedData)
    }

    /**
     * Creates mapped objects and handles the database operations which may be required to map various objects,
     *
     * @param source the incoming data source type
     * @return mapped object that will be consumed by [onResponseDatabaseInsert]
     */
    override suspend fun onResponseMapFrom(source: Viewer): UserEntity {
        val node = source.viewer
        return converter.convertTo(node)
    }
}