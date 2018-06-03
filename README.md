# Retrofit Converter - With GraphQL Support &nbsp; [![](https://jitpack.io/v/AniTrend/retrofit-graphql.svg)](https://jitpack.io/#AniTrend/retrofit-graphql)

This is a retrofit converter which uses annotations to inject .graphql query or mutation files into a request body along with any GraphQL variables. The included example makes use of [GitHunt GraphQL API](http://api.githunt.com/graphiql) that sometimes responds with null fields, feel free to try it with any other GraphQL API like `GitHub API v4` also this project does not teach you how to use Retrofit, Glide or the ViewModel.

## Why This Project Exists?

Many might wonder why this exists when an android GraphQL library like [Apollo](https://github.com/apollographql/apollo-android) exists. Unfortunately Apollo for Android still lacks some basic but important features/functionality which led to the following questions about [General Design Questions Regarding Apollo](https://github.com/apollographql/apollo-android/issues/847), [Polymorphic Type Handling](https://github.com/apollographql/apollo-android/issues/334) and [Non Shared Types](https://github.com/apollographql/apollo-android/issues/898). Don't get me wrong Apollo is not inferior any way, it has amazing features such as:

- Code Generation (Classes and Data Types)
- Custom Scalar Types
- Cached Responses

But since model classes are automatically generated for you, the developer loses some flexibility, such as use of generics, abstraction and inheritance. Also Android Peformance best practice suggests that developers should use StringDef and IntDef over enums and [here's why](https://stackoverflow.com/questions/29183904/should-i-strictly-avoid-using-enums-on-android).

Strangly there are tons of simple examples all over Medium using apollo graphql for Android, but none of them address these issues because most of them just construct a simple single resource request demo application. These look just fine at first glance until you start working with multiple data types and apollo starts generating classes for every fragment and query even if the data models are the same, or share similar properties. Thus this project came to be

____

## How Everything Works

Seeing how we already have a really powerful type-safe HTTP client for Android and Java [Retrofit](http://square.github.io/retrofit/) why not use it and extend it's functionality?

For a detailed example please clone the project and look at the included sample application. The entire project & example app makes use of the following libraries:

- [Retrofit](http://square.github.io/retrofit/)
- [Gson](https://github.com/google/gson)
- [Glide](https://bumptech.github.io/glide/)
- [Architecture Lifecycle](https://developer.android.com/topic/libraries/architecture/lifecycle)
- [ViewModel](https://developer.android.com/topic/libraries/architecture/viewmodel)
- [LiveData](https://developer.android.com/topic/libraries/architecture/livedata)

### The Basics

Firstly you'll need some to save your GraphQL queries and mutations into .graphql files, You can use this tool to generate your Insomnia workspaces into directories and files [insomnia-graphql-generator](https://github.com/AniTrend/insomnia-graphql-generator) and place these files into your assets folder as shown below:

<img src="https://github.com/AniTrend/retrofit-graphql/raw/develop/screenshots/assets_files.png" width=250 />

- __Add the JitPack repository to your build file__

```javascript
allprojects {
	repositories {
		...
		maven { url 'https://jitpack.io' }
	}
}
```

- __Add the dependency__

```javascript
dependencies {
    implementation 'com.github.AniTrend:retrofit-graphql:{latest_version}'
}
```
___

Next we make our retrofit interfaces and annotate them with the `@GraphQuery` annotation using the name of the .graphql file without the extention, this will allow the runtime resolution of the target file inside your assets to be loaded before the request is sent. e.g.

<img src="https://github.com/AniTrend/retrofit-graphql/raw/develop/screenshots/request_files.png" width=650 />

### Models

The model creation is upto the developer, this is where retrofit-graphql differs from apollo, this way you can design your models in anyway you desire. By default the library supplies you with a `QueryContainerBuilder` which is a holder for your GraphQL variables and request. Also __two__ basic top level models, which you don't have to use if you want to design your own:

###### QueryContainerBuilder

Suggest using this as is, but if you want to make your own that's not a problem either. The QueryContainerBuilder is used as follows:

_Given a .graphql files such as the following:_

```graphql
query Trending($type: FeedType!, $offset: Int, $limit: Int) {
  feed(type: $type, offset: $offset, limit: $limit) {
    id
    hotScore
    repository {
      ...repository
    }
    postedBy {
      ...user
    }
  }
}
```

_Adding parameters to the request would be done as follows:_

```java
QueryContainerBuilder queryBuilder = new QueryContainerBuilder()
            .putVariable("type", "TRENDING")
            .putVariable("offset", 1)
            .putVariable("limit", 15);
```
The queryBuilder is then passed into your retrofit interface method as parameter and that's it! Just like an ordinary retrofit application.


###### GraphError

Common GraphQL Error

```java
public class GraphError {

    private String message;
    private int status;
    private List<Map<String, Integer>> locations;

    public String getMessage() {
        return message;
    }

    public int getStatus() {
        return status;
    }

    public List<Map<String, Integer>> getLocations() {
        return locations;
    }

    @Override
    public String toString() {
        return "GraphError{" +
                "message='" + message + '\'' +
                ", status=" + status +
                ", locations=" + locations +
                '}';
    }
}
```

###### GraphContainer

Similar to the top level GraphQL response, but the data type is generic to allow easy reuse.

```java
public class GraphContainer<T> {

    private T data;
    private List<GraphError> errors;

    public T getData() {
        return data;
    }

    public List<GraphError> getErrors() {
        return errors;
    }

    public boolean isEmpty() {
        return data == null;
    }
}
```

## Working Example

__Retrofit Factory__

```java
public class WebFactory {

    private static Retrofit mRetrofit;

    /**
     * Generates retrofit service classes
     *
     * @param serviceClass The interface class method representing your request to use
     *                     @see IndexModel methods
     * @param context A valid application, fragment or activity context
     */
    public static <S> S createService(@NonNull Class<S> serviceClass, Context context) {
        if(mRetrofit == null) {
            OkHttpClient.Builder httpClient = new OkHttpClient.Builder()
                    .readTimeout(35, TimeUnit.SECONDS)
                    .connectTimeout(35, TimeUnit.SECONDS);

			// Optional include http logging:
			// implementation "com.squareup.okhttp3:logging-interceptor:3.9.1"
            if(BuildConfig.DEBUG) {
                HttpLoggingInterceptor httpLoggingInterceptor = new HttpLoggingInterceptor()
                        .setLevel(HttpLoggingInterceptor.Level.HEADERS);
                httpClient.addInterceptor(httpLoggingInterceptor);
            }

            // Note, we are not adding the default gson converter
            // because the GraphConverter will handle both body and parameter conversion
            mRetrofit = new Retrofit.Builder()
                    .client(httpClient.build())
                    .baseUrl("https://api.githunt.com/")
                    .addConverterFactory(GraphConverter.create(context))
                    .build();
        }
        return mRetrofit.create(serviceClass);
    }
}
```

__Retrofit Model__

```java
public interface IndexModel {

    @POST("graphql")
    @GraphQuery("Trending")
    @Headers("Content-Type: application/json")
    Call<GraphContainer<TrendingFeed>> getTrending(@Body QueryContainerBuilder request);

    @POST("graphql")
    @GraphQuery("RepoEntries")
    @Headers("Content-Type: application/json")
    Call<GraphContainer<EntryFeed>> getRepoEntries(@Body QueryContainerBuilder request);
}
```

__Data Model Container Class__

```java
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
```

__Making The Request__

```java
IndexModel indexModel = WebFactory.createService(IndexModel.class, getApplicationContext());
QueryContainerBuilder queryContainerBuilder = QueryContainerBuilder()
        .putVariable("type", TrendingFeed.NEW)
        .putVariable("limit", 20)
        .putVariable("offset", 1);
indexModel.getTrending(queryContainerBuilder).enqueue(MainActy.this);
```

__Handling The Response__

```java
@Override
public void onResponse(@NonNull Call<GraphContainer<TrendingFeed>> call,@NonNull Response<GraphContainer<TrendingFeed>> response) {
    GraphContainer<TrendingFeed> container;
    if(response.isSuccessful() && (container = response.body()) != null) {
        if(!container.isEmpty()) {
            List<Entry> entryList = container.getData().getFeed();
            // TODO: 2018/05/05  do as you please with the network response
        }
    } else {
        // TODO: 2018/05/05 What you will do with any errors is up to you :)
        // GraphErrorUtil is included in the library for you ;)
        List<GraphError> errorList = GraphErrorUtil.getError(response);
    }
}

@Override
public void onFailure(@NonNull Call<GraphContainer<TrendingFeed>> call,@NonNull Throwable throwable) {
    throwable.printStackTrace();
    Toast.makeText(getApplicationContext(), "Handle the error",Toast.LENGTH_SHORT).show();
}
```

## The Result

<img src="https://github.com/AniTrend/retrofit-graphql/raw/develop/screenshots/device-2018-05-20-161049.png" width="300"/> <img src="https://github.com/AniTrend/retrofit-graphql/raw/develop/screenshots/device-2018-05-20-161111.png" width="300"/>

## Proof Of Concept?

This project is derived from [AniTrend](https://github.com/AniTrend/anitrend-app) which is already published on the PlayStore
