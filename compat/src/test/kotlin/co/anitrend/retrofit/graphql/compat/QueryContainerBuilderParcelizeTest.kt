package co.anitrend.retrofit.graphql.compat

import android.os.Parcel
import co.anitrend.retrofit.graphql.model.request.PersistedQuery
import co.anitrend.retrofit.graphql.model.request.QueryContainerBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Proves the legacy [QueryContainerBuilder] keeps its Android Parcelable
 * contract after the move to `:compat`.
 *
 * The round trip is exercised with Robolectric because the mockable
 * android.jar used by plain unit tests cannot perform stateful
 * `android.os.Parcel` writes.
 *
 * The `@RawValue` variable/extensions maps are written through
 * `Parcel.writeValue`, which supports primitives, strings, maps, lists, and
 * `Parcelable`/`Serializable` entries. Non-parcelable values (such as the
 * `PersistedQuery` extension payload) are a documented legacy limitation and
 * fail on write, exactly as in the historical artifact.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@Suppress("DEPRECATION")
class QueryContainerBuilderParcelizeTest {

    @Test
    fun `builder metadata and variables survive parcel round trip`() {
        val builder =
            QueryContainerBuilder()
                .setOperationName("GetCurrentUser")
                .setQuery("query GetCurrentUser { viewer { login } }")
                .putVariable("first", 15)
                .putVariable("after", "cursor-1")

        val parcel = Parcel.obtain()
        try {
            parcel.writeParcelable(builder, 0)
            parcel.setDataPosition(0)
            val restored =
                parcel.readParcelable<QueryContainerBuilder>(
                    QueryContainerBuilder::class.java.classLoader,
                )

            assertNotNull(restored)
            val container = restored!!.build()
            assertEquals("GetCurrentUser", container.operationName)
            assertEquals("query GetCurrentUser { viewer { login } }", container.query)
            // Variable maps are restored as regular maps with the same values.
            assertEquals(15, container.variables["first"])
            assertEquals("cursor-1", container.variables["after"])
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun `builder extensions with parcel-safe values survive parcel round trip`() {
        val builder =
            QueryContainerBuilder()
                .setOperationName("GetCurrentUser")
                .setQuery("query GetCurrentUser { viewer { login } }")
                .putExtension("vendor", "acme")

        val parcel = Parcel.obtain()
        try {
            parcel.writeParcelable(builder, 0)
            parcel.setDataPosition(0)
            val restored =
                parcel.readParcelable<QueryContainerBuilder>(
                    QueryContainerBuilder::class.java.classLoader,
                )

            assertNotNull(restored)
            val container = restored!!.build()
            assertEquals("acme", container.extensions["vendor"])
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun `parcel round trip of unset builder yields empty container`() {
        val builder = QueryContainerBuilder()

        val parcel = Parcel.obtain()
        try {
            parcel.writeParcelable(builder, 0)
            parcel.setDataPosition(0)
            val restored =
                parcel.readParcelable<QueryContainerBuilder>(
                    QueryContainerBuilder::class.java.classLoader,
                )

            assertNotNull(restored)
            val container = restored!!.build()
            assertNull(container.operationName)
            assertNull(container.query)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun `persisted query extension is not parcelable - legacy limitation`() {
        // Documented legacy behavior: PersistedQuery is neither Parcelable
        // nor Serializable, so a builder carrying it fails on write.
        val builder =
            QueryContainerBuilder()
                .putPersistedQueryHash(sha256Hash = "abc123", version = 1)

        val parcel = Parcel.obtain()
        try {
            assertThrows(IllegalArgumentException::class.java) {
                parcel.writeParcelable(builder, 0)
            }
        } finally {
            parcel.recycle()
        }
    }
}
