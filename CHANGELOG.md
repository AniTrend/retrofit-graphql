> This project adheres to [Semantic Versioning](http://semver.org/).

Change Log
==========

Version 0.11.0-alpha03 *(2021-02-24)*
----------------------------

- **Update**: Bump compile and target SDK from 29 to 30 [18c670](https://github.com/AniTrend/retrofit-graphql/commit/18c6705bebfdc23d3a4e1c0afc91747ebc686fce)

- **New**: Open QueryContainerBuilder for extension [#116](https://github.com/AniTrend/retrofit-graphql/issues/116)

Version 0.11.0-alpha02 *(2020-08-21)*
----------------------------

- **Update**: Revert minimum sdk from 21 to 17 for library module [#112](https://github.com/AniTrend/retrofit-graphql/issues/112)

- **Update**: Bump kotlin version from 1.3.72 to 1.4.0 [f918c7](https://github.com/AniTrend/retrofit-graphql/commit/f918c71d82f970c6a48281a9cc1f2e23996ef9a0)

- **Update**: Bump gradle from 4.0.0 to 4.0.1 [3c6496](https://github.com/AniTrend/retrofit-graphql/commit/3c6496d345ee97373cdb218b0126473a62c6727d)

Version 0.11.0-alpha01 *(2020-06-28)*
----------------------------

- **Update**: Refactor of graph request converter to reflect method signature changes in `retrofit 2.9.0`

- **Update**: Refactor `GraphConverter` signature and expose constructor [aff4ab5](https://github.com/AniTrend/retrofit-graphql/commit/aff4ab5029978b659f119c4a8d36f5ae803e3ac0)

- **New**: Add new persisted query hash calculator and errors to new packages and minor class signature changes with deprecation warnings on old classes [175e40c](https://github.com/AniTrend/retrofit-graphql/commit/175e40c724153b4d76a04b1ac6077304aa4f1793), [af619fa](https://github.com/AniTrend/retrofit-graphql/commit/af619faf488e75dc45d8a293886783a53c13684a)

- **Update**: Refactor `GraphProcessor` and add deprecation warning to singleton factory [#71](https://github.com/AniTrend/retrofit-graphql/issues/71), [278f292](https://github.com/AniTrend/retrofit-graphql/commit/278f2926514984ec5a9d4390dd5575c1d90935a4) & [bd6fe97](https://github.com/AniTrend/retrofit-graphql/commit/bd6fe97f060a58890aa5819b5a1c2222ef99ff33)

- **New**: Add abstract contract for a files discovery plugin [#71](https://github.com/AniTrend/retrofit-graphql/issues/71), [c6dfa15](https://github.com/AniTrend/retrofit-graphql/commit/c6dfa1546bfebe95242ac07962d9b138bf5e08a0)

- **New**: Add new logger facade contracts for full control over logger [#59](https://github.com/AniTrend/retrofit-graphql/issues/59), [3202ec6](https://github.com/AniTrend/retrofit-graphql/commit/3202ec651c8b17c6385dc38356339d2a58414aba) & [fcf329e](https://github.com/AniTrend/retrofit-graphql/commit/fcf329ee9f1f952c3000b97522d466dad8e5e184)

- **Breaking**: Bump minimum sdk version from API 17 to API 21 [90ec67f](https://github.com/AniTrend/retrofit-graphql/commit/90ec67f2f6221dad043cdd029e76972ee8835133)

- **Update**: Rewrite sample app, migrate to github api [8ce9257](https://github.com/AniTrend/retrofit-graphql/commit/8ce9257fa1379b03df8b4483c5e6a58c3a7104c2)

- **New**: Migrate to buildSrc and bump retofit to 2.9.0 [cff0fd8](https://github.com/AniTrend/retrofit-graphql/commit/cff0fd8a90e58164264a395b22d0f9f8cf6a4a1f)

- **Update**: Bump gradle from 3.6.3 to 4.0.0 [#104](https://github.com/AniTrend/retrofit-graphql/pull/104)

- **Update**: Bump mockk from 1.9.3 to 1.10.0 [#95](https://github.com/AniTrend/retrofit-graphql/pull/95)

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
