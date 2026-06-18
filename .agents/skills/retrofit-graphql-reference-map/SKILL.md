---
name: retrofit-graphql-reference-map
description: 'Reference map for retrofit-graphql modules, package roots, dependency direction, consumer entry points, and Dokka navigation. Use for questions like which module should own this code, where a class should live, what consumers should import, or how the library is organized.'
argument-hint: 'Describe the feature, type, or consumer workflow you are trying to place or understand'
---

# Retrofit GraphQL Reference Map

## What This Skill Produces

- A fast module-placement decision for new or existing code.
- A package-level map of where to search next.
- A consumer-oriented view of which abstractions are likely to be imported, configured, or extended.

## When To Use

- Choosing where a new class, interface, helper, or model type belongs.
- Understanding which module a consumer depends on.
- Mapping a downstream use case back to the owning package.
- Explaining repo structure to an LLM before deeper implementation work.

## Procedure

1. Start with the [module reference map](./references/module-map.md) and identify the package that can own the behavior.
2. Match the task to a package family before picking a file.
3. Confirm the existing dependency direction: :app depends on modular runtime components (`:runtime`, `:api`, `:android-assets`, `:annotations`), not the reverse. The deprecated `:library` aggregator transitively exposes all modules.
4. Open the Dokka page for the library if you need consumer-facing context or neighboring public types.
5. If the task changes a public API, also apply the `retrofit-graphql-kdoc-dokka` skill so the published docs stay aligned.

## Outputs To Aim For

- Module name
- Candidate package or namespace
- Relevant neighboring abstractions
- Consumer impact summary

## References

- [module reference map](./references/module-map.md)
