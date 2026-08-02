package co.anitrend.retrofit.graphql.helpers.annotations

import co.anitrend.retrofit.graphql.annotation.GraphQuery

interface MockAnnotationStubs {
    @GraphQuery("StorageBucketFiles") fun getStorageBucketFiles()
    @GraphQuery("GetMarketPlaceApps") fun getMarketPlaceApps()
    @GraphQuery("FindIssueId") fun findIssueById()
    @GraphQuery("GetRepository") fun getRepository()
    @GraphQuery("GeneralSearch") fun searchFor()
    @GraphQuery("GetCurrentUser") fun getCurrentUser()
}