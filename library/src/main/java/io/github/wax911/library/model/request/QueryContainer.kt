package io.github.wax911.library.model.request

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.RawValue
import java.util.*

/**
 * Actual query and variable container
 * used in retrofit requestBodyConverter
 */
@Parcelize
data class QueryContainer internal constructor(
        var operationName: String? = null,
        var query: String? = null,
        val variables: @RawValue
        MutableMap<String, Any?> = WeakHashMap(),
        val extensions: @RawValue
        MutableMap<String, Any?> = WeakHashMap()) : Parcelable {

    internal fun putVariable(key: String, value: Any?) {
        variables[key] = value
    }

    internal fun putExtension(key: String, value: Any?) {
        extensions[key] = value
    }
}