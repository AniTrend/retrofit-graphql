package io.github.wax911.retgraph.api.converter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.gson.reflect.TypeToken;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;

import io.github.wax911.library.converter.GraphConverter;
import io.github.wax911.library.model.attribute.GraphError;
import io.github.wax911.library.model.body.GraphContainer;
import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Retrofit;

public class GraphQLConverter extends GraphConverter {

    public static GraphConverter create(Context context) {
        return new GraphQLConverter(context);
    }

    /**
     * Protected constructor because we want to make use of the
     * Factory Pattern to create our converter
     * <br/>
     *
     * @param context Any valid application context
     */
    private GraphQLConverter(Context context) {
        super(context);
    }

    @Override
    public Converter<ResponseBody, ?> responseBodyConverter(Type type, Annotation[] annotations, Retrofit retrofit) {
        if(type instanceof ResponseBody)
            return super.responseBodyConverter(type, annotations, retrofit);
        return new ResponseBodyConverter<>(type);
    }

    private class ResponseBodyConverter<T> extends GraphResponseConverter<T> {

        ResponseBodyConverter(Type type) {
            super(type);
        }

        /**
         * Converter contains logic on how to handle responses, since GraphQL responses follow
         * the JsonAPI spec it makes sense to wrap our base query response data and errors response
         * in here, the logic remains open to the implementation
         * <br/>
         *
         * @param responseBody The retrofit response body received from the network
         * @return The type declared in the Call of the request
         */
        @Override
        public T convert(@NonNull ResponseBody responseBody) {
            GraphContainer<T> container;
            T targetResult = null;
            try {
                container = gson.fromJson(responseBody.string(), new TypeToken<GraphContainer<T>>(){}.getType());
                if(container != null) {
                    if(container.isSuccess()) {
                        T dataContainer = container.getData();
                        if (dataContainer != null) {
                            String response = gson.toJson(dataContainer);
                            targetResult = gson.fromJson(response, type);
                        }
                    } else {
                        List<GraphError> graphErrors = container.getErrors();
                        for (GraphError error: graphErrors)
                            Log.e(this.toString(), error.toString());
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                responseBody.close();
            }
            return targetResult;
        }
    }
}
