package co.anitrend.retrofit.graphql.domain.repositories

interface UserRepository<D> {
    fun getCurrentUser(): D
}