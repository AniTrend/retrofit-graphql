package io.github.wax911.library.model.request

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import java.util.*

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
    val extensions: @RawValue MutableMap<String, Any?> = WeakHashMap()
) : Parcelable {

    internal fun putVariables(values: Map<String, Any?>) {
        variables.putAll(values)
    }

    internal fun putVariable(key: String, value: Any?) {
        variables[key] = value
    }

    internal fun putExtension(key: String, value: Any?) {
        extensions[key] = value
    }
}