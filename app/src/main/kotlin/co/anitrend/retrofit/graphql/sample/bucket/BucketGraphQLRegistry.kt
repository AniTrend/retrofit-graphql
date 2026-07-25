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

import co.anitrend.retrofit.graphql.model.GraphQLDocumentRegistry
import kotlin.String

/**
 * Registry for bucket operation documents and hashes.
 *
 * Manually maintained because the bucket backend uses a separate schema
 * from the GitHub API schema used by codegen.
 */
public object BucketGraphQLRegistry : GraphQLDocumentRegistry {
    override fun document(operationName: String): String? = when (operationName) {
        StorageBucketFiles.name -> StorageBucketFiles.document
        UploadToStorageBucket.name -> UploadToStorageBucket.document
        else -> null
    }

    override fun hash(operationName: String): String? = when (operationName) {
        StorageBucketFiles.name -> StorageBucketFiles.sha256Hash
        UploadToStorageBucket.name -> UploadToStorageBucket.sha256Hash
        else -> null
    }
}
