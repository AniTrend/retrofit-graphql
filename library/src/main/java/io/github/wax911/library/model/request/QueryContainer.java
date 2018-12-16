package io.github.wax911.library.model.request;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Actual query and variable container
 * used in retrofit requestBodyConverter
 */
public class QueryContainer implements Parcelable {

    private String query;
    private Map<String, Object> variables;

    QueryContainer() {
        variables = new WeakHashMap<>();
    }

    @SuppressWarnings("unchecked")
    private QueryContainer(Parcel in) {
        query = in.readString();
        variables = in.readHashMap(WeakHashMap.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(query);
        dest.writeMap(variables);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<QueryContainer> CREATOR = new Creator<QueryContainer>() {
        @Override
        public QueryContainer createFromParcel(Parcel in) {
            return new QueryContainer(in);
        }

        @Override
        public QueryContainer[] newArray(int size) {
            return new QueryContainer[size];
        }
    };

    protected void setQuery(String query) {
        this.query = query;
    }

    void putVariable(String key, Object value) {
        variables.put(key, value);
    }

    public Map<String, Object> getVariables() {
        return variables;
    }

    boolean containsVariableKey(String key) {
        return variables != null && variables.containsKey(key);
    }
}