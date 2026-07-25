package co.anitrend.retrofit.graphql.sample

import co.anitrend.retrofit.graphql.sample.generated.GeneralSearchData

/**
 * Keeps representative generated polymorphic response types reachable from the
 * release app graph so R8 mapping verification can prove they are renamed.
 */
internal object ReleaseR8VerificationAnchor {
    @Volatile
    private var lastTouch: Int = 0

    fun touchGeneratedUnionTypes() {
        lastTouch = listOf(
            GeneralSearchData::class.java.name,
            GeneralSearchData.SearchEdgesNode::class.java.name,
            GeneralSearchData.SearchEdgesNode.Repository::class.java.name,
        ).sumOf(String::length)
    }
}
