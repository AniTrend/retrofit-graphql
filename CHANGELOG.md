> This project adheres to [Semantic Versioning](http://semver.org/).

Change Log
==========

Version 0.11.0 *(2020-06-28)*
----------------------------

- **Update**: Refactor of graph request converter to reflect method signature changes in `retrofit 2.9.0`

- **Update**: Refactor `GraphConverter` signature and expose constructor aff4ab5

- **New**: Add new persisted query hash calculator and errors to new packages and minor class signature changes with deprecation warnings on old classes 175e40c, af619fa

- **Update**: Refactor `GraphProcessor` and add deprecation warning to singleton factory #71, 278f292 & bd6fe97

- **New**: Add abstract contract for a files discovery plugin #71,  c6dfa15

- **New**: Add new logger facade contracts for full control over logger #59, 3202ec6 & fcf329e

- **Breaking**: Bump minimum sdk version from API 17 to API 21 90ec67f

- **Update**: Rewrite sample app, migrate to github api 8ce9257

- **New**: Migrate to buildSrc and bump retofit to 2.9.0 cff0fd8

- **Update**: Bump gradle from 3.6.3 to 4.0.0 #104

- **Update**: Bump mockk from 1.9.3 to 1.10.0 #95

Version 0.10.3 *(2020-04-16)*
----------------------------

* **New**: Add `Logger` for library [#53](https://github.com/AniTrend/retrofit-graphql/pull/53)
* **New**: Add standard extensions `Map<String, Object>` to GraphError [#90](https://github.com/AniTrend/retrofit-graphql/pull/90)
* **Update**: `gradle` from 3.6.1 to 3.6.2 [#88](https://github.com/AniTrend/retrofit-graphql/pull/88)
* **Update**: `kotlin` from 1.3.70 to 1.3.71 [0c9864e](https://github.com/AniTrend/retrofit-graphql/commit/0c9864e7a0941cd400cd88f2fa24125b51308e02)

Version 0.10.2 *(2019-09-16)*
----------------------------

* **Update**: [mockk](https://github.com/mockk/mockk) from 1.9 to 1.9.3 [#24](https://github.com/AniTrend/retrofit-graphql/pull/24)
* **Update**: `kotlin-gradle-plugin` from 1.3.41 to 1.3.50 [#39](https://github.com/AniTrend/retrofit-graphql/pull/39)
* **Update**: `retrofit` from 2.6.0 to 2.6.1 [#31](https://github.com/AniTrend/retrofit-graphql/pull/31)
* **Update**: `gradle` from 3.4.2 to 3.5.0 [#38](https://github.com/AniTrend/retrofit-graphql/pull/38)
* **Update**: `target` & `compile` sdk from 28 to 29 [8bd43c2](https://github.com/AniTrend/retrofit-graphql/commit/8bd43c226064f6819ae8c0fb72e8e233e06dbfdc)

Version 0.10.1 *(2019-06-05)*
----------------------------

* **New**: Add class to build fragment `Strings` to be appended to a query [#4](https://github.com/AniTrend/retrofit-graphql/pull/4)
* **New**: Wire up the new fragment patching logic to the GraphProcessor [#5](https://github.com/AniTrend/retrofit-graphql/pull/5)
* **New**: Allow `fragments` to be defined in their own files for reuse [#10](https://github.com/AniTrend/retrofit-graphql/pull/10)
* **Fix**: Support externalized fragment definitions on all `operations` not just query [#19](https://github.com/AniTrend/retrofit-graphql/pull/19)

Version 0.9.1 *(2019-03-28)*
----------------------------

* **Fix**: Default `R8/ProGuard` rules for library model classes [#7](https://github.com/AniTrend/retrofit-graphql/pull/7)

Version 0.9 *(2019-03-20)*
----------------------------

* **New**: Support for [Automated Persisted Queries (APQ)](https://blog.apollographql.com/improve-graphql-performance-with-automatic-persisted-queries-c31d27b8e6ea) complying with [this protocol](https://github.com/apollographql/apollo-link-persisted-queries) [#5](https://github.com/AniTrend/retrofit-graphql/pull/5)

Version 0.8 *(2019-01-10)*
----------------------------

* **Fix**: Remove AndroidX artifact from library [#2](https://github.com/AniTrend/retrofit-graphql/pull/2)

Version 0.7 *(2019-01-08)*
----------------------------

* **New**: Support for custom GSON added [#1](https://github.com/AniTrend/retrofit-graphql/pull/1)