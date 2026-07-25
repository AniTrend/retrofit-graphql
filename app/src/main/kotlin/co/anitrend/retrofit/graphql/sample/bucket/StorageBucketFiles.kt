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

import co.anitrend.retrofit.graphql.model.GraphQLNoVarOperation
import kotlin.String

/**
 * Operation constants for the [StorageBucketFiles] query.
 *
 * Manually maintained because the bucket backend uses a separate schema
 * from the GitHub API schema used by codegen.
 */
public object StorageBucketFiles : GraphQLNoVarOperation {
    override val name: String = "StorageBucketFiles"

    override val document: String =
        "query StorageBucketFiles {\n" +
            "  files {\n" +
            "    ... on StorageBucketFile {\n" +
            "      contentType\n" +
            "      filename\n" +
            "      id\n" +
            "      url\n" +
            "    }\n" +
            "  }\n" +
            "}"

    override val sha256Hash: String =
        "9856c1ad594a71832c1cccfe53c25e929f3d7e88d5b5097ed31f7f60ae8f85d6"
}
