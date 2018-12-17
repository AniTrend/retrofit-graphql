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
class QueryContainer internal constructor(var query: String? = null,
                     val variables: @RawValue
                     MutableMap<String, Any?> = WeakHashMap()) : Parcelable {

    internal fun putVariable(key: String, value: Any?) {
        variables[key] = value
    }
}