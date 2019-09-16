package io.github.wax911.retgraph.model.container

import androidx.annotation.StringDef
import io.github.wax911.retgraph.model.parent.Entry

data class TrendingFeed(val feed: List<Entry>?) {

    @StringDef(HOT, NEW, TOP)
    internal annotation class FeedType

    companion object {
        // https://api.githunt.com/graphiql feed types, represented as StringDef instead of enums
        const val HOT = "HOT"
        const val NEW = "NEW"
        const val TOP = "TOP"
    }
}
