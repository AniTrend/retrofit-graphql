---
applyTo: **
description: This file describes the overall architecture and purpose of the retrofit-graphql library.
---

# Retrofit GraphQL Library Overview

**Retrofit GraphQL** is a converter library for Retrofit that enables the injection of `.graphql` query or mutation files into HTTP request bodies along with GraphQL variables. This library bridges the gap between Retrofit's HTTP capabilities and GraphQL's query-based approach, providing a seamless integration for Android developers who prefer working with raw GraphQL files rather than generated code.

## Library Architecture and Design

The library follows a **converter-based architecture** designed to integrate seamlessly with Retrofit's existing converter ecosystem. Key architectural principles include:

- **Converter Pattern** – The core functionality is implemented as a Retrofit `Converter.Factory` that handles the transformation of annotated method calls into proper GraphQL HTTP requests.

- **Annotation-Driven** – The library uses custom annotations (like `@GraphQuery`, `@GraphMutation`) to specify which GraphQL files should be loaded and how variables should be injected.

- **File-Based Queries** – Unlike code generation approaches, this library reads `.graphql` files directly from the application's assets folder, preserving the original GraphQL syntax and allowing for easier collaboration with backend teams.

## Module Organization

The project is organized into distinct modules:

- **`library`** – The core converter implementation containing:
  - Annotation processors for handling `@GraphQuery`, `@GraphMutation`, and related annotations
  - File discovery and loading mechanisms for `.graphql` files
  - Request body generation and variable injection logic
  - Error handling and validation utilities

- **`app`** – Sample application demonstrating library usage with:
  - Example GraphQL queries and mutations for file upload scenarios
  - Sample UI implementation showing network requests in action
  - Integration examples with OkHttp interceptors and custom loggers

- **`buildSrc`** – Build configuration and shared build logic:
  - Android build configuration (targeting API levels, compile options)
  - Publishing configuration for JitPack distribution
  - Dokka documentation generation setup
  - Dependency management and version catalogs

## Key Features and Capabilities

The library provides several important features:

1. **GraphQL File Integration** – Automatically loads `.graphql` files from the assets folder and injects their contents into HTTP request bodies.

2. **Variable Binding** – Supports binding method parameters to GraphQL variables using annotations, enabling dynamic query execution.

3. **File Upload Support** – Includes special handling for multipart file uploads following GraphQL multipart request specification.

4. **Fragment Support** – Processes GraphQL fragments and automatically includes them when referenced in queries or mutations.

5. **Custom Discovery Plugins** – Provides extensibility through custom file discovery mechanisms for advanced use cases.

6. **Logger Integration** – Offers configurable logging for debugging GraphQL requests and responses.

## Target Use Cases

This library is particularly useful when:

- You want to maintain GraphQL queries as separate `.graphql` files rather than generating Kotlin code
- You need flexibility in model classes (generics, inheritance, abstractions) that generated code might not provide
- You're working with large GraphQL schemas where code generation creates excessive classes
- You want to use Android-specific optimizations like `@StringDef` and `@IntDef` instead of enums
- You need to handle polymorphic types in a way that generated solutions don't support well

## Dependencies and Integration

The library integrates with:

- **Retrofit 2.x** – Primary integration point as a converter factory
- **OkHttp** – For HTTP transport and interceptor support
- **Gson/Moshi** – For JSON serialization of variables and response parsing
- **Android SDK** – For asset file access and Android-specific utilities

## Documentation and Resources

- **API Documentation** – Generated via Dokka and available at: https://anitrend.github.io/retrofit-graphql/
- **Sample Application** – Demonstrates practical usage patterns and integration examples
- **Wiki** – Contains additional examples, setup guides, and advanced usage scenarios