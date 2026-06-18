# KDoc Checklist

Use this checklist when adding or updating KDoc for any public or protected API in `library/src/main`.

## Annotations (`annotation/`)

- [ ] Summary line states what operation the annotation marks (query, mutation, variable, field mapping).
- [ ] `@param` or `@property` documents accepted values and format (e.g., filename without extension, variable name as declared in the `.graphql` file).
- [ ] Documents where the annotated element must appear (Retrofit interface method, method parameter).
- [ ] Explains what the converter does with the annotation at runtime.

## Converter and Factory (`converter/`)

- [ ] Summary line names the Retrofit integration role clearly.
- [ ] Documents how to register the factory (`Retrofit.Builder.addConverterFactory(...)`).
- [ ] For `GraphQLConverterFactory`, documents optional parameters (logger, discovery plugin, JSON delegate converter).
- [ ] Documents which `@Annotation`-driven request types the converter handles and which it delegates.
- [ ] Notes any ordering requirements relative to other converters (e.g., must be added before the JSON converter).

## Discovery (`discovery/`)

- [ ] Summary line names the file source and lookup strategy.
- [ ] For interface/abstract types, documents the extension contract: what the implementation must return, threading assumptions, and nullability.
- [ ] For concrete implementations, documents the asset path format and fallback behavior when a file is not found.
- [ ] Documents fragment auto-inclusion behavior where applicable.

## Model (`model/`)

- [ ] Summary line names the data type and its role in the request/response lifecycle.
- [ ] `@property` documents each field's role and serialization key where it differs from the property name.
- [ ] For sealed or abstract hierarchy roots, documents the expected subtypes and when each is produced.

## Logger (`logger/`)

- [ ] Summary line names the log surface and verbosity intent.
- [ ] For interface/abstract logger types, documents the extension contract and expected behavior per log level.
- [ ] Documents how to register a custom logger with `GraphQLConverterFactory`.

## Utility (`util/`)

- [ ] Summary line names the operation concisely.
- [ ] `@param` and `@return` document each significant parameter and the return contract.
- [ ] Documents any important side effects, threading assumptions, or exception conditions.

## General Rules

- Short summary first, elaboration second.
- Use `@see` to cross-reference closely related types so Dokka navigation is useful.
- Do not add `@since` unless the version is already established in adjacent code.
- Do not place consumer-facing APIs in `.internal` packages; those are suppressed from the published Dokka site.
- If behavior changes, update the KDoc in the same patch.
