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

package io.github.wax911.library.converter

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.github.wax911.library.annotation.processor.GraphProcessor
import io.github.wax911.library.annotation.processor.contract.AbstractGraphProcessor
import io.github.wax911.library.annotation.processor.plugin.AssetManagerDiscoveryPlugin
import io.github.wax911.library.converter.request.GraphRequestConverter
import io.github.wax911.library.converter.response.GraphResponseConverter
import io.github.wax911.library.logger.DefaultGraphLogger
import io.github.wax911.library.logger.contract.ILogger
import io.github.wax911.library.logger.core.AbstractLogger
import io.github.wax911.library.util.LogLevel
import java.lang.reflect.Type
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit

/**
 * Body for GraphQL requests and responses, closed for modification
 * but open for extension.
 *
 * @param graphProcessor A singleton reference of [AbstractLogger]
 * @param gson Any valid application context
*/
open class GraphConverter(
    protected val graphProcessor: AbstractGraphProcessor,
    protected val gson: Gson
) : Converter.Factory() {

    /**
     * Response body converter delegates logic processing to a child class that handles
     * wrapping and deserialization of the json response data.
     *
     * @param annotations All the annotation applied to the requesting method
     * @param retrofit The retrofit object representing the response
     * @param type The generic type declared on the method
     *
     * @see GraphResponseConverter
     */
    override fun responseBodyConverter(
        type: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit
    ): Converter<ResponseBody, *>? {
        return when (type) {
            is ResponseBody -> super.responseBodyConverter(type, annotations, retrofit)
            else -> GraphResponseConverter<Any>(type, gson)
        }
    }

    /**
     * Response body converter delegates logic processing to a child class that handles
     * wrapping and deserialization of the json response data.
     *
     * @param parameterAnnotations All the annotation applied to request parameters
     * @param methodAnnotations All the annotation applied to the requesting method
     * @param retrofit The retrofit object representing the response
     * @param type The type of the parameter of the request
     *
     * @see GraphRequestConverter
     */
    override fun requestBodyConverter(
        type: Type,
        parameterAnnotations: Array<out Annotation>,
        methodAnnotations: Array<out Annotation>,
        retrofit: Retrofit
    ): Converter<*, RequestBody>? {
        return GraphRequestConverter(methodAnnotations, graphProcessor, gson)
    }

    /**
     * Sets the minimum level for log messages. Attempted messages with a too low
     * log level are skipped and not printed to the system log.
     *
     * @param logLevel The minimum log level used to print log messages
     */
    @Deprecated(
        "Use setMinimumLogLevel instead or alternatively setup your on logger on GraphConverter.create",
        ReplaceWith(
            "setMinimumLogLevel(ILogger.Level)",
            "io.github.wax911.library.logger.contract.ILogger.Level"
        ),
        level = DeprecationLevel.ERROR
    )
    fun setLogLevel(logLevel: LogLevel) {
        val level = when (logLevel) {
            LogLevel.DEBUG -> ILogger.Level.DEBUG
            LogLevel.ERROR -> ILogger.Level.ERROR
            LogLevel.INFO -> ILogger.Level.INFO
            LogLevel.VERBOSE -> ILogger.Level.VERBOSE
            LogLevel.WARN -> ILogger.Level.WARNING
            else -> ILogger.Level.NONE
        }
        setMinimumLogLevel(level)
    }

    /**
     * Overrides the minimum level for log messages on the logger.
     *
     * @param level The minimum log level
     */
    fun setMinimumLogLevel(level: ILogger.Level) {
        graphProcessor.logger.level = level
    }

    companion object {

        const val MimeType = "application/graphql"

        /**
         * Default creator that uses a predefined gson configuration
         *
         * @param context A valid application context
         * @param level Minimum log level
         */
        @JvmOverloads
        fun create(context: Context, level: ILogger.Level = ILogger.Level.INFO): GraphConverter =
            GraphConverter(
                graphProcessor = GraphProcessor(
                    AssetManagerDiscoveryPlugin(context.assets),
                    DefaultGraphLogger(level)
                ),
                gson = GsonBuilder()
                    .enableComplexMapKeySerialization()
                    .serializeNulls()
                    .setLenient()
                    .create()
            )

        /**
         * Allows you to provide your own [Gson] configuration which will be used when serialize or
         * deserialize response and request bodies.
         *
         * @param context A valid application context
         * @param gson Custom gson implementation
         * @param level Minimum log level
         */
        @JvmOverloads
        fun create(context: Context, gson: Gson, level: ILogger.Level = ILogger.Level.INFO): GraphConverter =
            GraphConverter(
                graphProcessor = GraphProcessor(
                    AssetManagerDiscoveryPlugin(context.assets),
                    DefaultGraphLogger(level)
                ),
                gson = gson
            )
    }
}
