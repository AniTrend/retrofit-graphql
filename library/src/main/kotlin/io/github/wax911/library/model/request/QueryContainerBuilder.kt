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

/**
 * Query & Variable builder for graph requests
 */
@Parcelize
open class QueryContainerBuilder(
    private val queryContainer: QueryContainer = QueryContainer()
) : Parcelable {

    fun setQuery(query: String?): QueryContainerBuilder {
        queryContainer.query = query
        return this
    }

    fun setOperationName(operationName: String?): QueryContainerBuilder {
        queryContainer.operationName = operationName
        return this
    }

    fun putVariable(key: String, value: Any?): QueryContainerBuilder {
        queryContainer.putVariable(key, value)
        return this
    }

    fun putVariables(values: Map<String, Any?>): QueryContainerBuilder {
        queryContainer.putVariables(values)
        return this
    }

    fun putExtension(key: String, value: Any?): QueryContainerBuilder {
        queryContainer.putExtension(key, value)
        return this
    }

    fun putPersistedQueryHash(sha256Hash: String, version: Int = 1): QueryContainerBuilder {
        putExtension(
            persistedQueryExtensionName,
            PersistedQuery(
                sha256Hash = sha256Hash,
                version = version
            )
        )
        return this
    }

    fun getVariable(key: String): Any? {
        return when {
            containsKey(key) -> queryContainer.variables[key]
            else -> null
        }
    }

    fun containsKey(key: String): Boolean {
        return queryContainer.variables.containsKey(key)
    }

    /**
     * Should only be called by the GraphQLConverter or any other subclasses of it
     * after the query has been added to the request
     *
     * @see io.github.wax911.library.converter.GraphConverter
     */
    fun build(): QueryContainer {
        return queryContainer
    }

    companion object {
        const val persistedQueryExtensionName = "persistedQuery"
    }
}
