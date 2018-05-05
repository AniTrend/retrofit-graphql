package io.github.wax911.library.model.request;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;

import java.util.Map;

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

    public @Nullable Object getVariable(String key) {
        if(containsVariable(key))
            return queryContainer.getVariables().get(key);
        return null;
    }

    public boolean containsVariable(String key) {
        return queryContainer.containsVariable(key);
    }

    public QueryContainer build() {
        return queryContainer;
    }
}