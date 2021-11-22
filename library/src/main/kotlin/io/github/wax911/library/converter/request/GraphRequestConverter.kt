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

package io.github.wax911.library.converter.request

import com.google.gson.Gson
import io.github.wax911.library.annotation.processor.contract.AbstractGraphProcessor
import io.github.wax911.library.model.request.QueryContainerBuilder
import okhttp3.MediaType
import okhttp3.RequestBody
import retrofit2.Converter

/**
 * GraphQL request body converter and injector, uses method annotation for a given retrofit method
 */
open class GraphRequestConverter(
    protected val methodAnnotations: Array<out Annotation>,
    protected val graphProcessor: AbstractGraphProcessor,
    protected val gson: Gson
) : Converter<QueryContainerBuilder, RequestBody> {

    /**
     * Converter for the request body, gets the GraphQL query from the method annotation
     * and constructs a GraphQL request body to send over the network.
     *
     * @param containerBuilder The constructed builder method of your query with variables
     */
    override fun convert(containerBuilder: QueryContainerBuilder): RequestBody {
        val rawQuery = graphProcessor.getQuery(methodAnnotations)
        val queryContainer =
            containerBuilder.setQuery(rawQuery)
                .build()
        val queryJson = gson.toJson(queryContainer)
        return RequestBody.create(MEDIA_TYPE, queryJson)
    }

    companion object {
        private val MEDIA_TYPE = MediaType.parse("application/json")
    }
}
