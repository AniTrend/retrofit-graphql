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

package co.anitrend.retrofit.graphql.serialization

import co.anitrend.retrofit.graphql.data.bucket.model.StorageBucket
import co.anitrend.retrofit.graphql.model.body.GraphContainer
import co.anitrend.retrofit.graphql.sample.generated.GetCurrentUserData
import co.anitrend.retrofit.graphql.sample.generated.GetMarketPlaceAppsData
import co.anitrend.retrofit.graphql.sample.generated.GetMarketPlaceAppsData.MarketplaceListings
import co.anitrend.retrofit.graphql.sample.generated.GetMarketPlaceAppsData.MarketplaceListingsEdges
import co.anitrend.retrofit.graphql.sample.generated.GetMarketPlaceAppsData.MarketplaceListingsEdgesNode
import co.anitrend.retrofit.graphql.sample.generated.GetMarketPlaceAppsData.MarketplaceListingsEdgesNodePrimaryCategory
import co.anitrend.retrofit.graphql.sample.generated.GetMarketPlaceAppsData.MarketplaceListingsEdgesNodeSecondaryCategory
import co.anitrend.retrofit.graphql.sample.generated.GetMarketPlaceAppsData.MarketplaceListingsPageInfo
import co.anitrend.retrofit.graphql.serialization.kotlinx.KotlinxGraphQLJson
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.lang.reflect.ParameterizedType

/**
 * Integration tests verifying that generated response DTOs decorated with
 * kotlinx.serialization annotations correctly decode JSON fixtures through the
 * [KotlinxGraphQLJson] converter.
 */
class ResponseDecodingTest {

    private val json: KotlinxGraphQLJson = KotlinxGraphQLJson(
        Json { ignoreUnknownKeys = true; encodeDefaults = false }
    )

    @Test
    fun `decode GetCurrentUser response through GraphContainer`() {
        val fixture = readFixture("get_current_user_response.json")
        val type = makeGraphContainerType(GetCurrentUserData::class.java)

        val container: GraphContainer<GetCurrentUserData> = json.decode(fixture, type)

        assertNull(container.errors)
        assertNotNull(container.data)
        val viewer = container.data!!.viewer
        assertEquals("MDQ6VXNlcjE=", viewer.id)
        assertEquals("octocat", viewer.login)
        assertEquals("Sample bio text", viewer.bio)
        assertEquals("https://avatars.githubusercontent.com/u/1?v=4", viewer.avatarUrl)
        assertEquals(":rocket:", viewer.status?.emoji)
        assertEquals("Building something awesome", viewer.status?.message)
    }

    @Test
    fun `decode GetMarketPlaceApps response through GraphContainer`() {
        val fixture = readFixture("get_marketplace_apps_response.json")
        val type = makeGraphContainerType(GetMarketPlaceAppsData::class.java)

        val container: GraphContainer<GetMarketPlaceAppsData> = json.decode(fixture, type)

        assertNull(container.errors)
        assertNotNull(container.data)
        val data = container.data!!
        val listings = data.marketplaceListings
        assertEquals(42, listings.totalCount)
        assertEquals(2, listings.edges?.size)

        val firstEdge = listings.edges!![0]!!
        assertEquals("Y3Vyc29yOjE=", firstEdge.cursor)
        assertEquals("codeclimate", firstEdge.node!!.slug)
        assertEquals("Code Climate", firstEdge.node!!.name)
        assertEquals("Code quality", firstEdge.node!!.primaryCategory.name)

        val pageInfo = listings.pageInfo
        assertEquals("Y3Vyc29yOjI=", pageInfo.endCursor)
        assertTrue(pageInfo.hasNextPage)
    }

    @Test
    fun `decode StorageBucketFiles response through GraphContainer`() {
        val fixture = readFixture("storage_bucket_files_response.json")
        val type = makeGraphContainerType(StorageBucket::class.java)

        val container: GraphContainer<StorageBucket> = json.decode(fixture, type)

        assertNull(container.errors)
        assertNotNull(container.data)
        val data = container.data!!
        assertEquals(2, data.files.size)

        val firstFile = data.files[0]
        assertEquals("file-001", firstFile.id)
        assertEquals("photo.webp", firstFile.filename)
        assertEquals("image/webp", firstFile.contentType)
        assertEquals("https://storage.example.com/files/photo.webp", firstFile.url)
    }

    @Test
    fun `decode GraphContainer with errors`() {
        val errorFixture = """{"errors":[{"message":"Not found","path":["user"]}]}"""
        val type = makeGraphContainerType(GetCurrentUserData::class.java)

        val container: GraphContainer<GetCurrentUserData> = json.decode(errorFixture, type)

        assertNull(container.data)
        assertNotNull(container.errors)
        assertEquals(1, container.errors!!.size)
        assertEquals("Not found", container.errors!![0].message)
    }

    @Test
    fun `decode GraphContainer with both data and errors`() {
        val fixture = """{"data":{"viewer":{"id":"U1","login":"test","avatarUrl":"x","bio":null,"company":null,"status":null}},"errors":[{"message":"Partial data","path":["viewer","status"]}]}"""
        val type = makeGraphContainerType(GetCurrentUserData::class.java)

        val container: GraphContainer<GetCurrentUserData> = json.decode(fixture, type)

        assertNotNull(container.errors)
        assertNotNull(container.data)
        assertEquals(1, container.errors!!.size)
        assertEquals("Partial data", container.errors!![0].message)
        assertEquals("U1", container.data!!.viewer.id)
    }

    @Test
    fun `decode GraphContainer with null data`() {
        val nullDataFixture = """{"data":null}"""
        val type = makeGraphContainerType(GetCurrentUserData::class.java)

        val container: GraphContainer<GetCurrentUserData> = json.decode(nullDataFixture, type)

        assertNull(container.errors)
        assertNull(container.data)
    }

    /**
     * Reads a JSON fixture file from the test resources directory.
     */
    private fun readFixture(fileName: String): String {
        val resourcePath = "/graphql/fixtures/$fileName"
        val inputStream = this.javaClass.getResourceAsStream(resourcePath)
            ?: throw IllegalArgumentException("Fixture not found: $resourcePath")
        return inputStream.bufferedReader().use { it.readText() }
    }

    /**
     * Creates a [ParameterizedType] for `GraphContainer<T>`.
     */
    private fun makeGraphContainerType(dataType: Class<*>): ParameterizedType {
        return object : ParameterizedType {
            override fun getRawType(): java.lang.reflect.Type = GraphContainer::class.java
            override fun getOwnerType(): java.lang.reflect.Type? = null
            override fun getActualTypeArguments(): Array<java.lang.reflect.Type> = arrayOf(dataType)
        }
    }
}
