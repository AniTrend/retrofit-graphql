package co.anitrend.retrofit.graphql.domain.repositories

interface MarketPlaceRepository<D> {
    fun getMarketPlaceListings(): D
}