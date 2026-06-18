---
name: retrofit-graphql-kdoc-dokka
description: 'Write or improve KDoc for public APIs in retrofit-graphql. Use for Dokka updates, annotation docs, converter docs, discovery plugin docs, logger docs, and explaining how consumers should integrate or extend the library.'
argument-hint: 'Describe the public API, package, or documentation gap you need to cover'
---

# Retrofit GraphQL KDoc And Dokka

## What This Skill Produces

- Consumer-facing KDoc that reads well on the published Dokka site: `https://anitrend.github.io/retrofit-graphql/`.
- Documentation that explains annotation contracts, converter behavior, discovery extension points, and logger integration.
- A repeatable checklist for updating docs whenever public behavior changes.

## When To Use

- Adding or changing a public class, interface, annotation, function, property, or enum.
- Explaining how a downstream app should register, configure, or extend the converter or its plugins.
- Tightening documentation before a release or after a behavior change.

## Procedure

1. Identify the public or protected surface that changed.
2. Read the [KDoc checklist](./references/kdoc-checklist.md) and match the API shape to the closest template.
3. Document what the API does, when to use it, and what a consumer is expected to provide or observe.
4. For annotations, explain the expected value format, the asset path convention, and what the converter does with the annotation at runtime.
5. For converter and factory types, document the Retrofit registration contract, supported request types, and any ordering requirements.
6. For discovery and plugin interfaces, document the extension contract: what the implementation must return and when it is called.
7. Link adjacent types with KDoc references so Dokka helps consumers navigate the API surface.
8. If the type belongs to a new package, consider whether nearby package or module docs also need an update.

## Quality Bar

- Summary first, details second.
- Avoid tautologies such as repeating the type name without explaining behavior.
- Keep docs aligned with real behavior in the code, not the intended behavior from an older implementation.

## References

- [KDoc checklist](./references/kdoc-checklist.md)
