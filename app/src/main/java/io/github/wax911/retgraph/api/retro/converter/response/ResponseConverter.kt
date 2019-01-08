package io.github.wax911.retgraph.api.retro.converter.response

import com.google.gson.Gson
import io.github.wax911.library.converter.response.GraphResponseConverter
import okhttp3.ResponseBody
import java.io.IOException
import java.lang.reflect.Type

class ResponseConverter<T>(type: Type?, gson: Gson) : GraphResponseConverter<T>(type, gson) {

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