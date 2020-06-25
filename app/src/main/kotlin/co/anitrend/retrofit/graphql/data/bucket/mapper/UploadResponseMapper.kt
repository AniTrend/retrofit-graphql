package co.anitrend.retrofit.graphql.data.bucket.mapper

import co.anitrend.retrofit.graphql.data.arch.mapper.GraphQLMapper
import co.anitrend.retrofit.graphql.data.bucket.model.upload.UploadResult
import co.anitrend.retrofit.graphql.domain.entities.bucket.BucketFile

internal class UploadResponseMapper : GraphQLMapper<UploadResult, BucketFile>() {
    /**
     * Inserts the given object into the implemented room database,
     *
     * @param mappedData mapped object from [onResponseMapFrom] to insert into the database
     */
    override suspend fun onResponseDatabaseInsert(mappedData: BucketFile) {
        // not going to persist anything into the db
    }

    /**
     * Creates mapped objects and handles the database operations which may be required to map various objects,
     *
     * @param source the incoming data source type
     * @return mapped object that will be consumed by [onResponseDatabaseInsert]
     */
    override suspend fun onResponseMapFrom(source: UploadResult): BucketFile {
        return BucketFile(
            id = source.uploaded.id,
            contentType = source.uploaded.contentType,
            fileName = source.uploaded.filename,
            url = source.uploaded.url
        )
    }
}