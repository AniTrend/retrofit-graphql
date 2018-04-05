package io.github.wax911.library.model.request;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Created by max on 2018/03/16.
 * Query & Variable builder for graph requests
 */
public class QueryContainerBuilder implements Parcelable {

    private QueryContainer queryContainer;

    public QueryContainerBuilder() {
        queryContainer = new QueryContainer();
    }

    protected QueryContainerBuilder(Parcel in) {
        queryContainer = in.readParcelable(QueryContainer.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(queryContainer, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<QueryContainerBuilder> CREATOR = new Creator<QueryContainerBuilder>() {
        @Override
        public QueryContainerBuilder createFromParcel(Parcel in) {
            return new QueryContainerBuilder(in);
        }

        @Override
        public QueryContainerBuilder[] newArray(int size) {
            return new QueryContainerBuilder[size];
        }
    };

    public QueryContainerBuilder setQuery(String query) {
        this.queryContainer.setQuery(query);
        return this;
    }

    public QueryContainerBuilder putVariable(String key, Object value) {
        queryContainer.putVariable(key, value);
        return this;
    }

    public boolean containsVariable(String key) {
        return queryContainer.containsVariable(key);
    }

    /**
     * Optionally remove some variables if they are null, or perform other checks.
     * This method will be called in the retrofit requestBodyConverter
     */
    public QueryContainer build() {
        return queryContainer;
    }

    /**
     * Actual query and variable container
     * used in retrofit requestBodyConverter
     */
    public static class QueryContainer implements Parcelable {

        private String query;
        private Map<String, Object> variables;

        QueryContainer() {
            variables = new WeakHashMap<>();
        }

        QueryContainer(Parcel in) {
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

        boolean containsVariable(String key) {
            return variables != null && variables.containsKey(key);
        }
    }
}
