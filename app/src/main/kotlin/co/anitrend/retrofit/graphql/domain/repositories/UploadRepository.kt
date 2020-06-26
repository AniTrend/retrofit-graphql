package co.anitrend.retrofit.graphql.domain.repositories

import co.anitrend.retrofit.graphql.domain.models.common.IGraphQuery

interface UploadRepository<D> {
    fun uploadToBucket(mutation: IGraphQuery): D
}