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

import co.anitrend.retrofit.graphql.data.bucket.mapper.BucketResponseMapper
import co.anitrend.retrofit.graphql.data.bucket.mapper.UploadResponseMapper
import co.anitrend.retrofit.graphql.data.bucket.model.StorageBucket
import co.anitrend.retrofit.graphql.data.bucket.model.node.BucketFileNode
import co.anitrend.retrofit.graphql.data.bucket.model.upload.UploadResult
import co.anitrend.retrofit.graphql.data.market.datasource.local.MarketPlaceLocalSource
import co.anitrend.retrofit.graphql.data.market.mapper.MarketPlaceResponseMapper
import co.anitrend.retrofit.graphql.data.user.datasource.local.UserLocalSource
import co.anitrend.retrofit.graphql.data.user.mapper.UserResponseMapper
import co.anitrend.retrofit.graphql.sample.generated.GetCurrentUserData
import co.anitrend.retrofit.graphql.sample.generated.GetMarketPlaceAppsData
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests verifying that response mappers correctly convert
 * generated response DTOs into entity/domain types.
 */
class MapperTest {

    @Test
    fun `UserResponseMapper converts GetCurrentUserData to UserEntity`() = runTest {
        val localSource = mockk<UserLocalSource>(relaxed = true)
        val mapper = UserResponseMapper(localSource)

        val data = GetCurrentUserData(
            viewer = GetCurrentUserData.Viewer(
                id = "user-1",
                avatarUrl = "https://example.com/avatar.png",
                bio = "A developer",
                company = null,
                login = "dev42",
                status = GetCurrentUserData.ViewerStatus(
                    createdAt = "",
                    emoji = ":rocket:",
                    message = "Coding",
                ),
            )
        )

        val entity = mapper.onResponseMapFrom(data)

        assertEquals("user-1", entity.id)
        assertEquals("dev42", entity.username)
        assertEquals("A developer", entity.bio)
        assertEquals("https://example.com/avatar.png", entity.avatarUrl)
        assertEquals(":rocket:", entity.statusEmoji)
        assertEquals("Coding", entity.statusMessage)
    }

    @Test
    fun `MarketPlaceResponseMapper converts GetMarketPlaceAppsData to List MarketPlaceEntity`() = runTest {
        val localSource = mockk<MarketPlaceLocalSource>(relaxed = true)
        val mapper = MarketPlaceResponseMapper(localSource)

        val data = GetMarketPlaceAppsData(
            marketplaceListings = GetMarketPlaceAppsData.MarketplaceListings(
                edges = listOf(
                    GetMarketPlaceAppsData.MarketplaceListingsEdges(
                        cursor = "cursor-1",
                        node = GetMarketPlaceAppsData.MarketplaceListingsEdgesNode(
                            id = "node-1",
                            primaryCategory = GetMarketPlaceAppsData.MarketplaceListingsEdgesNodePrimaryCategory(
                                name = "Code quality"
                            ),
                            secondaryCategory = null,
                            slug = "codeclimate",
                            shortDescription = "Automated code review",
                            isPaid = false,
                            isVerified = true,
                            logoBackgroundColor = "#1e272e",
                            logoUrl = "https://logo.example.com/1.png",
                            name = "Code Climate",
                        ),
                    ),
                    GetMarketPlaceAppsData.MarketplaceListingsEdges(
                        cursor = "cursor-2",
                        node = GetMarketPlaceAppsData.MarketplaceListingsEdgesNode(
                            id = "node-2",
                            primaryCategory = GetMarketPlaceAppsData.MarketplaceListingsEdgesNodePrimaryCategory(
                                name = "CI"
                            ),
                            secondaryCategory = GetMarketPlaceAppsData.MarketplaceListingsEdgesNodeSecondaryCategory(
                                name = "Utilities"
                            ),
                            slug = "travis-ci",
                            shortDescription = "Test and deploy",
                            isPaid = true,
                            isVerified = true,
                            logoBackgroundColor = "#000",
                            logoUrl = "https://logo.example.com/2.png",
                            name = "Travis CI",
                        ),
                    ),
                ),
                pageInfo = GetMarketPlaceAppsData.MarketplaceListingsPageInfo(
                    endCursor = null,
                    hasNextPage = false,
                    hasPreviousPage = false,
                    startCursor = null,
                ),
                totalCount = 2,
            )
        )

        val entities = mapper.onResponseMapFrom(data)

        assertEquals(2, entities.size)
        assertEquals("node-1", entities[0].id)
        assertEquals("cursor-1", entities[0].cursorId)
        assertEquals(listOf("Code quality"), entities[0].categories)
        assertEquals("node-2", entities[1].id)
        assertEquals("cursor-2", entities[1].cursorId)
        assertEquals(listOf("CI", "Utilities"), entities[1].categories)
    }

    @Test
    fun `BucketResponseMapper converts StorageBucket to List BucketFile`() = runTest {
        val mapper = BucketResponseMapper()

        val bucket = StorageBucket(
            files = listOf(
                BucketFileNode(
                    id = "file-001",
                    contentType = "image/webp",
                    filename = "photo.webp",
                    url = "https://storage.example.com/files/photo.webp",
                ),
            )
        )

        val domainFiles = mapper.onResponseMapFrom(bucket)

        assertEquals(1, domainFiles.size)
        val file = domainFiles[0]
        assertEquals("file-001", file.id)
        assertEquals("image/webp", file.contentType)
        assertEquals("photo.webp", file.fileName)
    }

    @Test
    fun `UploadResponseMapper converts UploadResult to BucketFile`() = runTest {
        val mapper = UploadResponseMapper()

        val result = UploadResult(
            uploaded = BucketFileNode(
                id = "file-999",
                contentType = "image/webp",
                filename = "uploaded.webp",
                url = "https://storage.example.com/files/uploaded.webp",
            )
        )

        val domainFile = mapper.onResponseMapFrom(result)

        assertEquals("file-999", domainFile.id)
        assertEquals("image/webp", domainFile.contentType)
        assertEquals("uploaded.webp", domainFile.fileName)
        assertEquals("https://storage.example.com/files/uploaded.webp", domainFile.url)
    }

    @Test
    fun `MarketPlaceResponseMapper handles empty edges`() = runTest {
        val localSource = mockk<MarketPlaceLocalSource>(relaxed = true)
        val mapper = MarketPlaceResponseMapper(localSource)

        val data = GetMarketPlaceAppsData(
            marketplaceListings = GetMarketPlaceAppsData.MarketplaceListings(
                edges = emptyList(),
                pageInfo = GetMarketPlaceAppsData.MarketplaceListingsPageInfo(
                    endCursor = null,
                    hasNextPage = false,
                    hasPreviousPage = false,
                    startCursor = null,
                ),
                totalCount = 0,
            )
        )

        val entities = mapper.onResponseMapFrom(data)
        assertEquals(0, entities.size)
    }
}
