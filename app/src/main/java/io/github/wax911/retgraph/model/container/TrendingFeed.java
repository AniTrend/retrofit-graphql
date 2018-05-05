package io.github.wax911.retgraph.model.container;

import android.support.annotation.StringDef;

import java.util.List;

import io.github.wax911.retgraph.model.parent.Entry;

public class TrendingFeed {

    // https://api.githunt.com/graphiql feed types, represented as StringDef instead of enums
    public final static String HOT = "HOT", NEW = "NEW", TOP = "TOP";
    @StringDef({HOT,NEW, TOP})
    @interface FeedType {}

    private List<Entry> feed;

    public List<Entry> getFeed() {
        return feed;
    }
}
