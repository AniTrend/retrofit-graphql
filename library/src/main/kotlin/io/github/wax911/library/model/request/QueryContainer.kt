/**
 * Copyright 2021 AniTrend
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

package io.github.wax911.library.model.request

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import java.util.WeakHashMap

/**
 * GraphQL request that follows the common GraphQL HTTP request format
 *
 * @see [GraphQL Over HTTP](https://graphql.org/learn/serving-over-http/#post-request)
*/
@Parcelize
data class QueryContainer internal constructor(
    var operationName: String? = null,
    var query: String? = null,
    val variables: @RawValue MutableMap<String, Any?> = WeakHashMap(),
    val extensions: @RawValue MutableMap<String, Any?> = WeakHashMap(),
) : Parcelable {
    internal fun putVariables(values: Map<String, Any?>) {
        variables.putAll(values)
    }

    internal fun putVariable(
        key: String,
        value: Any?,
    ) {
        variables[key] = value
    }

    internal fun putExtension(
        key: String,
        value: Any?,
    ) {
        extensions[key] = value
    }
}
