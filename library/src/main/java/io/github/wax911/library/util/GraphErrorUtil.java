package io.github.wax911.library.util;

import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

import io.github.wax911.library.model.attribute.GraphError;
import io.github.wax911.library.model.body.GraphContainer;
import okhttp3.ResponseBody;
import retrofit2.Response;

public class GraphErrorUtil {

    private static final String TAG = "GraphErrorUtil";

    /**
     * Converts the response error response into an object.
     *
     * @return The error object, or null if an exception was encountered
     * @see Error
     */
    public static @Nullable List<GraphError> getError(@Nullable Response response) {
        try {
            if(response != null) {
                ResponseBody responseBody = response.errorBody();
                String message;
                List<GraphError> graphErrors;
                if (responseBody != null && !TextUtils.isEmpty(message = responseBody.string()))
                    if((graphErrors = getGraphQLError(message)) != null)
                        return graphErrors;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    private static List<GraphError> getGraphQLError(String errorJson) {
        Log.e(TAG, errorJson);
        Gson gson = new Gson();
        Type tokenType = new TypeToken<GraphContainer<?>>(){}.getType();
        GraphContainer<?> graphContainer = gson.fromJson(errorJson, tokenType);
        return graphContainer.getErrors();
    }
}
