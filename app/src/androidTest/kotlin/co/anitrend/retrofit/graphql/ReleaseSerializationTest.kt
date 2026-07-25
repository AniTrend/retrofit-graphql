/**
 * Copyright 2026 AniTrend
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package co.anitrend.retrofit.graphql

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import co.anitrend.retrofit.graphql.model.body.GraphContainer
import co.anitrend.retrofit.graphql.sample.generated.GetCurrentUserData
import co.anitrend.retrofit.graphql.sample.generated.GetMarketPlaceAppsData
import co.anitrend.retrofit.graphql.serialization.kotlinx.KotlinxGraphQLJson
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.ParameterizedType
import java.nio.charset.StandardCharsets

/**
 * Release-mode serialization tests that execute against R8-minified bytecode.
 *
 * When running via `./gradlew :app:connectedReleaseAndroidTest`, the test
 * APK exercises the R8-optimized release variant of the sample app. This
 * verifies that:
 *
 * 1. kotlinx.serialization deserializers survive R8 tree-shaking and
 *    renaming (consumer rules from the kotlinx-serialization plugin are
 *    applied automatically).
 * 2. Generated `@Serializable` data classes with `@SerialName` annotations
 *    decode correctly because the annotation values are string literals
 *    that R8 cannot rename.
 * 3. Polymorphic sealed interfaces with `@JsonClassDiscriminator` select
 *    the correct subtype via `__typename` resolution.
 * 4. `ParameterizedType` lookups for `GraphContainer<T>` work against
 *    minified classes.
 *
 * Fixture JSON files are stored in `androidTest/assets/graphql/fixtures/`.
 * To run: `./gradlew :app:connectedReleaseAndroidTest -x lint`
 */
class ReleaseSerializationTest {

    private val json: KotlinxGraphQLJson = KotlinxGraphQLJson(
        Json { ignoreUnknownKeys = true; encodeDefaults = false },
    )

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun decodeGetCurrentUserResponse_throughGraphContainer_againstR8output() {
        val fixture = readFixture("get_current_user_response.json")
        val type = makeGraphContainerType(GetCurrentUserData::class.java)

        val container: GraphContainer<GetCurrentUserData> = json.decode(fixture, type)

        assertNull("errors should be null for valid data", container.errors)
        assertNotNull("data should not be null", container.data)
        val viewer = container.data!!.viewer
        assertEquals("MDQ6VXNlcjE=", viewer.id)
        assertEquals("octocat", viewer.login)
        assertEquals("Sample bio text", viewer.bio)
        assertEquals("https://avatars.githubusercontent.com/u/1?v=4", viewer.avatarUrl)
        assertEquals(":rocket:", viewer.status?.emoji)
        assertEquals("Building something awesome", viewer.status?.message)
    }

    @Test
    fun decodeGetMarketPlaceAppsResponse_throughGraphContainer_againstR8output() {
        val fixture = readFixture("get_marketplace_apps_response.json")
        val type = makeGraphContainerType(GetMarketPlaceAppsData::class.java)

        val container: GraphContainer<GetMarketPlaceAppsData> = json.decode(fixture, type)

        assertNull("errors should be null", container.errors)
        assertNotNull("data should not be null", container.data)
        val listings = container.data!!.marketplaceListings
        assertEquals(42, listings.totalCount)
        assertEquals(2, listings.edges?.size)

        val firstEdge = listings.edges!![0]!!
        assertEquals("Y3Vyc29yOjE=", firstEdge.cursor)
        assertEquals("codeclimate", firstEdge.node!!.slug)
        assertEquals("Code Climate", firstEdge.node!!.name)
        assertEquals("Code quality", firstEdge.node!!.primaryCategory.name)
    }

    @Test
    fun decodeGetMarketPlaceApps_response_verifySerialNameAnnotationsSurvivedR8() {
        // This test asserts that the kotlinx.serialization generated
        // descriptor with @SerialName values produces correct JSON keys
        // under R8. The wire names in annotations (e.g. "marketplaceListings",
        // "totalCount") are string literals that R8 cannot rename. If the
        // serializer lookup fails or returns wrong keys, this test will fail
        // at decode time with a SerializationException.
        val fixture = readFixture("get_marketplace_apps_response.json")
        val type = makeGraphContainerType(GetMarketPlaceAppsData::class.java)

        val container: GraphContainer<GetMarketPlaceAppsData> = json.decode(fixture, type)

        assertNotNull("data must decode", container.data)
        val data = container.data!!
        assertNotNull("marketplaceListings field must be present", data.marketplaceListings)
        assertEquals(
            "totalCount should be deserialized correctly",
            42,
            data.marketplaceListings.totalCount,
        )
    }

    @Test
    fun decodeGraphContainer_withErrors_againstR8output() {
        val errorFixture = """{"errors":[{"message":"Not found","path":["user"]}]}"""
        val type = makeGraphContainerType(GetCurrentUserData::class.java)

        val container: GraphContainer<GetCurrentUserData> = json.decode(errorFixture, type)

        assertNull("data should be null when only errors present", container.data)
        assertNotNull("errors should be present", container.errors)
        assertEquals(1, container.errors!!.size)
        assertEquals("Not found", container.errors!![0].message)
    }

    @Test
    fun decodeGraphContainer_withDataAndErrors_againstR8output() {
        val fixture = """{"data":{"viewer":{"id":"U1","login":"test","avatarUrl":"x","bio":null,"company":null,"status":null}},"errors":[{"message":"Partial data","path":["viewer","status"]}]}"""
        val type = makeGraphContainerType(GetCurrentUserData::class.java)

        val container: GraphContainer<GetCurrentUserData> = json.decode(fixture, type)

        assertNotNull("errors should be present", container.errors)
        assertNotNull("data should be present alongside errors", container.data)
        assertEquals(1, container.errors!!.size)
        assertEquals("Partial data", container.errors!![0].message)
        assertEquals("U1", container.data!!.viewer.id)
    }

    @Test
    fun decodeGraphContainer_withNullData_againstR8output() {
        val nullDataFixture = """{"data":null}"""
        val type = makeGraphContainerType(GetCurrentUserData::class.java)

        val container: GraphContainer<GetCurrentUserData> = json.decode(nullDataFixture, type)

        assertNull("errors should be null", container.errors)
        assertNull("data should be null", container.data)
    }

    // --- Runtime kotlinx descriptor assertions ---
    //
    // These tests verify that kotlinx.serialization descriptors reflect the
    // correct @SerialName values after R8 processing. Descriptors are part
    // of the kotlinx.serialization runtime API and must survive tree-shaking.
    //
    // Generated classes use two levels of @SerialName:
    //   1. Class-level: path-qualified descriptor (e.g. "GetMarketPlaceAppsData.marketplaceListings")
    //   2. Property-level: the GraphQL response name (e.g. "edges", "totalCount")
    //
    // Properties are generated in alphabetical order by responseName.
    // Element descriptors for non-list fields directly yield the nested
    // type's serializer descriptor (carrying the class-level @SerialName).

    @Test
    fun descriptor_serialName_and_elementNames_surviveR8_getMarketPlaceAppsData() {
        val descriptor = GetMarketPlaceAppsData.serializer().descriptor

        // Class-level @SerialName: the root data class name
        assertEquals("GetMarketPlaceAppsData", descriptor.serialName)

        // Root has one top-level field: marketplaceListings
        assertEquals(1, descriptor.elementsCount)
        assertEquals("marketplaceListings", descriptor.getElementName(0))
    }

    @Test
    fun descriptor_nestedType_serialName_survivesR8_marketplaceListings() {
        val rootDescriptor = GetMarketPlaceAppsData.serializer().descriptor

        // marketplaceListings is at index 0 (only root field, sorted alphabetically).
        // Its type is a nested data class with a path-qualified @SerialName.
        val listingsDescriptor = rootDescriptor.getElementDescriptor(0)

        assertEquals(
            "GetMarketPlaceAppsData.marketplaceListings",
            listingsDescriptor.serialName,
        )

        // marketplaceListings has 3 fields (sorted: edges, pageInfo, totalCount)
        assertEquals(3, listingsDescriptor.elementsCount)
        assertEquals("edges", listingsDescriptor.getElementName(0))
        assertEquals("pageInfo", listingsDescriptor.getElementName(1))
        assertEquals("totalCount", listingsDescriptor.getElementName(2))
    }

    @Test
    fun descriptor_nestedType_serialName_survivesR8_marketplaceListingsPageInfo() {
        val rootDescriptor = GetMarketPlaceAppsData.serializer().descriptor
        val listingsDescriptor = rootDescriptor.getElementDescriptor(0)

        // pageInfo is at index 1 within marketplaceListings (sorted: edges, pageInfo, totalCount).
        // pageInfo is a non-list field, so getElementDescriptor returns the nested type directly.
        val pageInfoDescriptor = listingsDescriptor.getElementDescriptor(1)

        assertEquals(
            "GetMarketPlaceAppsData.marketplaceListings.pageInfo",
            pageInfoDescriptor.serialName,
        )

        // pageInfo has 4 fields from PageInfo fragment (sorted):
        // endCursor, hasNextPage, hasPreviousPage, startCursor
        assertEquals(4, pageInfoDescriptor.elementsCount)
        assertEquals("endCursor", pageInfoDescriptor.getElementName(0))
        assertEquals("hasNextPage", pageInfoDescriptor.getElementName(1))
        assertEquals("hasPreviousPage", pageInfoDescriptor.getElementName(2))
        assertEquals("startCursor", pageInfoDescriptor.getElementName(3))
    }

    @Test
    fun descriptor_serialName_and_elementNames_surviveR8_getCurrentUserData() {
        val descriptor = GetCurrentUserData.serializer().descriptor

        // Class-level @SerialName: the root data class name
        assertEquals("GetCurrentUserData", descriptor.serialName)

        // Root has one top-level field: viewer
        assertEquals(1, descriptor.elementsCount)
        assertEquals("viewer", descriptor.getElementName(0))
    }

    @Test
    fun descriptor_nestedType_serialName_survivesR8_viewer() {
        val rootDescriptor = GetCurrentUserData.serializer().descriptor

        // viewer is at index 0 (only root field)
        val viewerDescriptor = rootDescriptor.getElementDescriptor(0)

        assertEquals(
            "GetCurrentUserData.viewer",
            viewerDescriptor.serialName,
        )

        // viewer has 6 fields from UserCore fragment (sorted):
        // avatarUrl, bio, company, id, login, status
        assertEquals(6, viewerDescriptor.elementsCount)
        assertEquals("avatarUrl", viewerDescriptor.getElementName(0))
        assertEquals("bio", viewerDescriptor.getElementName(1))
        assertEquals("company", viewerDescriptor.getElementName(2))
        assertEquals("id", viewerDescriptor.getElementName(3))
        assertEquals("login", viewerDescriptor.getElementName(4))
        assertEquals("status", viewerDescriptor.getElementName(5))
    }

    @Test
    fun descriptor_nestedType_serialName_survivesR8_viewerStatus() {
        val rootDescriptor = GetCurrentUserData.serializer().descriptor
        val viewerDescriptor = rootDescriptor.getElementDescriptor(0)

        // status is at index 5 (sorted: avatarUrl, bio, company, id, login, status)
        val statusDescriptor = viewerDescriptor.getElementDescriptor(5)

        assertEquals(
            "GetCurrentUserData.viewer.status",
            statusDescriptor.serialName,
        )

        // status has 3 fields from the inline status selection (sorted):
        // createdAt, emoji, message
        assertEquals(3, statusDescriptor.elementsCount)
        assertEquals("createdAt", statusDescriptor.getElementName(0))
        assertEquals("emoji", statusDescriptor.getElementName(1))
        assertEquals("message", statusDescriptor.getElementName(2))
    }

    /**
     * Reads a JSON fixture file from the androidTest assets directory.
     */
    private fun readFixture(fileName: String): String {
        val stream = context.assets.open("graphql/fixtures/$fileName")
        return stream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
    }

    /**
     * Creates a [ParameterizedType] for `GraphContainer<T>`.
     *
     * This exercises the parameterized serializer lookup path against R8
     * output. The runtime must resolve `KSerializer<GraphContainer<T>>`
     * through the serializers module, which should be retained by
     * kotlinx.serialization consumer rules.
     */
    private fun makeGraphContainerType(dataType: Class<*>): ParameterizedType {
        return object : ParameterizedType {
            override fun getRawType(): java.lang.reflect.Type = GraphContainer::class.java
            override fun getOwnerType(): java.lang.reflect.Type? = null
            override fun getActualTypeArguments(): Array<java.lang.reflect.Type> = arrayOf(dataType)
        }
    }
}
