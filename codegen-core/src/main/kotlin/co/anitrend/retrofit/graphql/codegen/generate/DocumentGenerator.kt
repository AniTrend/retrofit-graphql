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

package co.anitrend.retrofit.graphql.codegen.generate

import co.anitrend.retrofit.graphql.codegen.model.GraphQLOperationInfo
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

/**
 * Generates a `GraphQLDocuments` Kotlin object containing the full document text
 * for each operation as raw string constants.
 *
 * Example output:
 * ```kotlin
 * public object GraphQLDocuments {
 *     public const val GetCurrentUser: String = """
 *         query GetCurrentUser {
 *           Viewer {
 *             id
 *             name
 *           }
 *         }
 *     """
 * }
 * ```
 */
object DocumentGenerator {
    fun generate(
        operations: List<GraphQLOperationInfo>,
        packageName: String,
    ): FileSpec {
        return FileSpec.builder(packageName, "GraphQLDocuments")
            .addType(
                TypeSpec.objectBuilder("GraphQLDocuments")
                    .addModifiers(KModifier.PUBLIC)
                    .apply {
                        operations.forEach { op ->
                            addProperty(
                                PropertySpec.builder(op.name, String::class)
                                    .addModifiers(KModifier.PUBLIC, KModifier.CONST)
                                    .initializer("%S", op.document)
                                    .build(),
                            )
                        }
                    }
                    .build(),
            )
            .build()
    }
}
