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

<img src="./images/screenshots/assets_files.png" width=250 />

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

```java

    @POST("graphql")
    @GraphQuery("Trending")
    @Headers("Content-Type: application/json")
    fun getTrending(@Body request: QueryContainerBuilder): Call<GraphContainer<TrendingFeed>>

    @POST("graphql")
    @GraphQuery("RepoEntries")
    @Headers("Content-Type: application/json")
    fun getRepoEntries(@Body request: QueryContainerBuilder): Call<GraphContainer<EntryFeed>>
```

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

_Check the example project named app for a more extensive overview of how everything works_

## The Result

<img src="./images/screenshots/device-2018-12-17-172758.png" width="300"/> &nbsp; <img src="./images/screenshots/device-2018-12-17-172740.png" width="300"/> &nbsp; <img src="./images/screenshots/device-2018-12-17-172811.png" width="300"/>

## Proof Of Concept?

This project is derived from [AniTrend](https://github.com/AniTrend/anitrend-app) which is already published on the PlayStore
