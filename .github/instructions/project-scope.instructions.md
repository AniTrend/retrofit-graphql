---
applyTo: **
description: This file describes the project scope and purpose for the retrofit-graphql library.
---

## Project Purpose and Scope

**Retrofit GraphQL** is an Android library that serves as a Retrofit converter for GraphQL operations. The library's primary goal is to enable developers to use raw `.graphql` files with Retrofit while maintaining the flexibility and control that hand-written model classes provide.

### Core Problem Solved

The library addresses limitations found in existing GraphQL solutions for Android:

- **Apollo Android** generates classes automatically, which can limit flexibility in terms of generics, abstraction, and inheritance patterns
- Generated code often creates duplicate classes for similar data structures used across different queries
- Android performance best practices suggest using `@StringDef` and `@IntDef` over traditional enums, which generated solutions don't typically support
- Polymorphic type handling and non-shared types can be challenging with code generation approaches

### Key Value Propositions

1. **File-Based Approach** – Work directly with `.graphql` files stored in your app's assets folder, enabling:
   - Better collaboration with backend teams who can share the same query files
   - IDE support for GraphQL syntax highlighting and validation
   - Version control of queries as human-readable files rather than generated code

2. **Flexible Model Classes** – Maintain full control over your data models with support for:
   - Generic type parameters
   - Inheritance hierarchies and abstract base classes
   - Android-specific annotations like `@StringDef` and `@IntDef`
   - Custom serialization strategies

3. **Retrofit Integration** – Seamless integration with existing Retrofit setups:
   - Works alongside other Retrofit converters
   - Compatible with existing OkHttp interceptors and network configurations
   - Familiar annotation-based API for developers already using Retrofit

4. **Multipart Upload Support** – Built-in support for GraphQL file uploads following the multipart request specification

### Supported Use Cases

The library is designed to handle various GraphQL scenarios:

- **Query Operations** – Fetch data using GraphQL queries with variable binding
- **Mutation Operations** – Modify data through GraphQL mutations
- **File Uploads** – Handle single or multiple file uploads via GraphQL mutations
- **Fragment Usage** – Automatically include GraphQL fragments when referenced
- **Custom Variable Binding** – Map method parameters to GraphQL variables using annotations

### Integration Points

The library integrates with several key technologies:

- **Retrofit 2.x** – Primary integration as a converter factory
- **GraphQL** – Supports standard GraphQL syntax and specifications
- **Android Assets** – Reads GraphQL files from the application's assets folder
- **JSON Serialization** – Works with Gson, Moshi, or other JSON libraries
- **OkHttp** – Leverages OkHttp for HTTP transport and interceptor support

### Scope Limitations

What the library does NOT provide:

- **Schema Validation** – No compile-time validation of queries against schemas
- **Code Generation** – No automatic generation of type-safe classes
- **Caching** – No built-in response caching (relies on HTTP caching or external solutions)
- **Real-time Features** – No WebSocket or subscription support (HTTP-based only)
- **Schema Introspection** – No runtime schema discovery or validation

### Target Developers

This library is ideal for Android developers who:

- Have experience with Retrofit and prefer its annotation-based approach
- Want to maintain control over their data model classes
- Work in teams where GraphQL queries are shared between frontend and backend
- Need to handle complex polymorphic types or inheritance patterns
- Prefer file-based query management over generated code
- Require Android-specific optimizations that code generation might not provide

### Distribution and Availability

The library is distributed through:

- **JitPack** – Primary distribution method for GitHub-based projects
- **Source Code** – Available as open-source on GitHub for custom builds
- **Documentation** – Comprehensive Dokka-generated API docs and usage examples