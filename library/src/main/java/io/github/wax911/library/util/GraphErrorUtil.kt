package io.github.wax911.library.util

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.wax911.library.model.attribute.GraphError
import io.github.wax911.library.model.body.GraphContainer
import retrofit2.Response

object GraphErrorUtil {

    private const val TAG = "GraphErrorUtil"

    /**
     * Converts the response error response into an object.
     *
     * @return The error object, or null if an exception was encountered
     * @see Error
     */
    fun getError(response: Response<*>?): List<GraphError>? {
        try {
            if (response != null) {
                val responseBody = response.errorBody()
                val message = responseBody?.string()
                if (responseBody != null && !message.isNullOrBlank()) {
                    val graphErrors= getGraphQLError(message)
                    if (graphErrors != null)
                        return graphErrors
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

        return null
    }

    private fun getGraphQLError(errorJson: String): List<GraphError>? {
        Log.e(TAG, errorJson)
        val tokenType = object : TypeToken<GraphContainer<*>>() {}.type
        val graphContainer = Gson().fromJson<GraphContainer<*>>(errorJson, tokenType)
        return graphContainer.errors
    }
}
