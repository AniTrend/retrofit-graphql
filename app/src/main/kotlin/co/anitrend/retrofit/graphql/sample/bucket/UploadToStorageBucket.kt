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

package co.anitrend.retrofit.graphql.sample.bucket

import co.anitrend.retrofit.graphql.model.GraphQLOperation
import co.anitrend.retrofit.graphql.model.GraphQLRequest
import kotlin.String

/**
 * Operation constants for the [UploadToStorageBucket] mutation.
 *
 * Manually maintained because the bucket backend uses a separate schema
 * from the GitHub API schema used by codegen.
 */
public object UploadToStorageBucket : GraphQLOperation<UploadToStorageBucketVariables> {
    override val name: String = "UploadToStorageBucket"

    override val document: String =
        "mutation UploadToStorageBucket(\$upload: Upload!) {\n" +
            "  uploadFile(fileData: \$upload) {\n" +
            "    ... on StorageBucketFile {\n" +
            "      contentType\n" +
            "      filename\n" +
            "      id\n" +
            "      url\n" +
            "    }\n" +
            "  }\n" +
            "}"

    override val sha256Hash: String =
        "1e8d3000694d7d6dcf85b2259f026cc1d8f73a13b5b7175a0845a079d32c1e79"

    /**
     * Creates a [GraphQLRequest] for the upload mutation with the given file path.
     *
     * @param upload The file path to upload.
     * @return A configured [GraphQLRequest].
     */
    public fun request(upload: String): GraphQLRequest<UploadToStorageBucketVariables> =
        GraphQLRequest(
            query = document,
            operationName = name,
            variables = UploadToStorageBucketVariables(upload = upload),
        )
}
