package io.github.wax911.library.helpers.annotations

import io.github.wax911.library.annotation.GraphQuery

interface MockAnnotationStubs {
    @GraphQuery("StorageBucketFiles") fun getStorageBucketFiles()
    @GraphQuery("GetMarketPlaceApps") fun getMarketPlaceApps()
    @GraphQuery("FindIssueId") fun findIssueById()
    @GraphQuery("GetRepository") fun getRepository()
    @GraphQuery("GeneralSearch") fun searchFor()
    @GraphQuery("GetCurrentUser") fun getCurrentUser()
}