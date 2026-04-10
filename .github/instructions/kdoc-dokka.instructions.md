---
description: Use when adding or changing public Kotlin APIs, KDoc, Dokka output, class docs, function docs, annotation docs, or property docs in the retrofit-graphql library module.
applyTo: library/src/main/**/*.kt
---

# KDoc And Dokka Guidance

- Treat KDoc as consumer documentation. The generated Dokka site is how downstream apps learn the library surface: `https://anitrend.github.io/retrofit-graphql/`.
- Document every new or changed public or protected class, interface, object, enum, annotation, function, and property that a consumer may touch.
- Write documentation for someone outside this repo who does not already know the architecture. Explain what the API is for, when to use it, and which part of the library it belongs to.
- For annotations such as `@GraphQuery` and `@GraphMutation`, document the expected argument format, the assets folder path convention, and what happens at runtime when the annotation is processed.
- For converter and factory types, document the Retrofit integration contract: how to register the converter, which request types it handles, and any ordering requirements relative to other converters.
- For discovery and plugin types, document the extension contract: what implementations must return, which lifecycle events trigger discovery, and any threading or nullability assumptions.
- For logger types, document the levels, filtering behavior, and how to register a custom logger with the converter factory.
- For classes with important collaborators, link to nearby types with KDoc references instead of forcing consumers to search the repo manually.
- Preserve the existing house style: a short summary first, then focused detail, with `@param`, `@property`, `@return`, `@throws`, `@see`, and `@since` where they add value.
- Do not invent version history. Only add `@since` when the version is already known or established in adjacent code.
- Avoid placeholder KDoc that only restates the type name. Explain behavior, expectations, and integration points.
- If behavior changes, update the docs in the same patch so the published site stays trustworthy.
- Packages under `.internal` are suppressed from Dokka. If an API is meant for library consumers, keep it in a documented public package.
