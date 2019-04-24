package io.github.wax911.library.model.request

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

/**
 * Created by max on 2018/03/16.
 * Query & Variable builder for graph requests
 */
@Parcelize
class QueryContainerBuilder(private val queryContainer: QueryContainer = QueryContainer()) : Parcelable {

    fun setQuery(query: String?): QueryContainerBuilder {
        this.queryContainer.query = query
        return this
    }

    fun setOperationName(operationName: String?): QueryContainerBuilder{
        this.queryContainer.operationName = operationName
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
        putExtension(persistedQueryExtensionName, PersistedQuery(sha256Hash = sha256Hash, version = version))
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
     * @see io.github.wax911.library.converter.GraphConverter
     */
    fun build(): QueryContainer {
        return queryContainer
    }

    companion object {
        const val persistedQueryExtensionName = "persistedQuery"
    }
}