package io.github.wax911.library.model.request

import android.os.Parcelable
import com.google.gson.Gson
import kotlinx.android.parcel.Parcelize

/**
 * Query & Variable builder for URL parameter based GET requests
 */
class PersistedQueryUrlParameterBuilder(private val queryContainer: QueryContainer = QueryContainer(), private val gson: Gson) {

    fun build(): PersistedQueryUrlParameters {
        return PersistedQueryUrlParameters(
                extensions = gson.toJson(queryContainer.extensions),
                operationName = queryContainer.operationName.orEmpty(),
                variables = gson.toJson(queryContainer.variables)
        )
    }

    companion object {
        const val EXTENSION_KEY_APQ = "persistedQuery"
    }
}