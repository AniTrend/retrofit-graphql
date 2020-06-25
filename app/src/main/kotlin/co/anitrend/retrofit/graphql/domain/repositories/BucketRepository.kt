package co.anitrend.retrofit.graphql.domain.repositories

interface BucketRepository<D> {
    fun getAllFiles() : D
}