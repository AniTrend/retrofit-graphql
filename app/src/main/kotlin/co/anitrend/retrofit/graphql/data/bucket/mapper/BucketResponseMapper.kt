package co.anitrend.retrofit.graphql.data.bucket.mapper

import co.anitrend.retrofit.graphql.data.arch.mapper.GraphQLMapper
import co.anitrend.retrofit.graphql.data.bucket.model.StorageBucket
import co.anitrend.retrofit.graphql.domain.entities.bucket.BucketFile
import co.anitrend.retrofit.graphql.sample.BuildConfig

internal class BucketResponseMapper : GraphQLMapper<StorageBucket, List<BucketFile>>() {
    /**
     * Inserts the given object into the implemented room database,
     *
     * @param mappedData mapped object from [onResponseMapFrom] to insert into the database
     */
    override suspend fun onResponseDatabaseInsert(mappedData: List<BucketFile>) {
        // not going to persist anything into the db
    }

    /**
     * Creates mapped objects and handles the database operations which may be required to map various objects,
     *
     * @param source the incoming data source type
     * @return mapped object that will be consumed by [onResponseDatabaseInsert]
     */
    override suspend fun onResponseMapFrom(source: StorageBucket): List<BucketFile> {
        return source.files.map { node ->
            BucketFile(
                id = node.id,
                contentType = node.contentType,
                fileName = node.filename,
                url = "${BuildConfig.bucket}/${node.url}"
            )
        }
    }
}