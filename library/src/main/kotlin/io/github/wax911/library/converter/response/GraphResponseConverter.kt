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

package io.github.wax911.library.converter.response

import com.google.gson.Gson
import java.io.IOException
import java.lang.reflect.Type
import okhttp3.ResponseBody
import retrofit2.Converter

/**
 * GraphQL response body converter to unwrap nested object results,
 * resulting in a smaller generic tree for requests
 */
open class GraphResponseConverter<T>(
    protected val type: Type?,
    protected val gson: Gson
) : Converter<ResponseBody, T> {

    /**
     * Converter contains logic on how to handle responses, since GraphQL responses follow
     * the JsonAPI spec it makes sense to wrap our base query response data and errors response
     * in here, the logic remains open to the implementation
     * <br></br>
     *
     * @param responseBody The retrofit response body received from the network
     * @return The type declared in the Call of the request
     */
    override fun convert(responseBody: ResponseBody): T? {
        var response: T? = null
        try {
            val responseString = responseBody.string()
            response = gson.fromJson<T>(responseString, type)
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return response
    }
}
