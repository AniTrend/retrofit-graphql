package io.github.wax911.library.apq

import android.content.Context
import com.google.gson.Gson
import io.github.wax911.library.annotation.processor.GraphProcessor
import io.github.wax911.library.model.request.PersistedQuery
import io.github.wax911.library.model.request.QueryContainer

class APQParameterFactory constructor(context: Context?, private val gson: Gson) {

    private val graphProcessor: GraphProcessor by lazy {
        GraphProcessor.getInstance(context?.assets)
    }

    private var queryContainer: QueryContainer = QueryContainer()

    fun queryName(operationName: String) : APQParameterFactory {
        queryContainer.operationName = operationName
        return this
    }

    fun putVariable(key: String, value: Any?): APQParameterFactory {
        queryContainer.putVariable(key, value)
        return this
    }

    fun putExtension(key: String, value: Any?): APQParameterFactory {
        queryContainer.putExtension(key, value)
        return this
    }

    fun build(): APQParameters {
        putExtension(EXTENSION_KEY_APQ, persistedQueryFieldForName(queryContainer.operationName.orEmpty()))
        return APQParameters(
                extensions = gson.toJson(queryContainer.extensions),
                operationName = queryContainer.operationName.orEmpty(),
                variables = gson.toJson(queryContainer.variables)
        )
    }


    private fun persistedQueryFieldForName(queryName: String): PersistedQuery? {
        return PersistedQuery(sha256Hash = graphProcessor.getOrCreateAPQHash(queryName).orEmpty(), version = 1)
    }

    companion object {
        const val EXTENSION_KEY_APQ = "persistedQuery"
    }
}