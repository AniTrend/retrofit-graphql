# Naming Contract

retrofit-graphql's codegen plugin follows a strict naming contract with three distinct names per generated identifier. This page documents the contract.

## The Three Names

Each generated identifier carries three representations stored in `GeneratedName`:

| Name | Purpose | Example |
|------|---------|---------|
| `kotlinName` | Identifier used in generated Kotlin source code | `privateValue` |
| `wireName` | Original GraphQL name for serialization annotations | `"private"` |
| `descriptorName` | Stable class-level path descriptor for `@SerialName` | `"GetCurrentUserData.viewer.status"` |

### Invariant

```kotlin
// wireName is ALWAYS the original GraphQL name. Never derived from kotlinName.
val name = GraphNameAllocator().allocatePropertyName("private")
name.wireName == "private"    // always true
name.kotlinName == "privateValue"  // keyword-escaped for Kotlin
```

This invariant guarantees that `@SerialName("private")` always produces the correct JSON key matching the GraphQL schema, regardless of Kotlin keyword escaping or collision resolution.

## Keyword Escaping

### Hard Keywords (always escaped)

Kotlin hard keywords cannot be used as identifiers in any context. When a GraphQL name matches a hard keyword, the `Value` suffix is appended.

**Examples** (see [NamePolicy.kt](https://github.com/AniTrend/retrofit-graphql/blob/develop/codegen-core/src/main/kotlin/co/anitrend/retrofit/graphql/codegen/naming/NamePolicy.kt) for the full set of 26 hard keywords):

| GraphQL Name | Kotlin Name | `@SerialName` |
|-------------|-------------|---------------|
| `as` | `asValue` | `"as"` |
| `class` | `classValue` | `"class"` |
| `false` | `falseValue` | `"false"` |
| `fun` | `funValue` | `"fun"` |
| `if` | `ifValue` | `"if"` |
| `in` | `inValue` | `"in"` |
| `interface` | `interfaceValue` | `"interface"` |
| `is` | `isValue` | `"is"` |
| `null` | `nullValue` | `"null"` |
| `object` | `objectValue` | `"object"` |
| `package` | `packageValue` | `"package"` |
| `return` | `returnValue` | `"return"` |
| `super` | `superValue` | `"super"` |
| `this` | `thisValue` | `"this"` |
| `throw` | `throwValue` | `"throw"` |
| `true` | `trueValue` | `"true"` |
| `try` | `tryValue` | `"try"` |
| `typealias` | `typealiasValue` | `"typealias"` |
| `val` | `valValue` | `"val"` |
| `var` | `varValue` | `"var"` |
| `when` | `whenValue` | `"when"` |
| `while` | `whileValue` | `"while"` |

### Visibility Modifiers (escaped)

Visibility modifiers are valid identifiers in some contexts but cause compilation ambiguity alongside actual visibility declarations. They are escaped:

| GraphQL Name | Kotlin Name | `@SerialName` |
|-------------|-------------|---------------|
| `private` | `privateValue` | `"private"` |
| `protected` | `protectedValue` | `"protected"` |
| `public` | `publicValue` | `"public"` |
| `internal` | `internalValue` | `"internal"` |

### Digits at Start

Names starting with a digit are prefixed with `_`:

| GraphQL Name | Kotlin Name | `@SerialName` |
|-------------|-------------|---------------|
| `3dModel` | `_3dModel` | `"3dModel"` |
| `123` | `_123` | `"123"` |

### Soft Keywords (not escaped)

Other soft keywords are **not** escaped because they are valid identifiers in most generated contexts (property names, parameter names, class names):

- `actual`, `data`, `inner`, `sealed`, `open`, `override`, `operator`, etc.

If a consumer encounters a problem with a specific soft keyword in a specific context, file an issue and the policy will be extended.

## Case Conventions

### Property Names

lowerCamelCase. The escaped name's first character is lowercased:

```
GraphQL "myField"    -> kotlinName="myField",    wireName="myField"
GraphQL "MyField"    -> kotlinName="myField",    wireName="MyField"
GraphQL "private"    -> kotlinName="privateValue", wireName="private"
```

### Class Names

PascalCase. The escaped name's first character is uppercased:

```
GraphQL "GetCurrentUserData"  -> kotlinName="GetCurrentUserData"
GraphQL "searchResult"        -> kotlinName="SearchResult"
```

### Enum Constants

SCREAMING_SNAKE_CASE. The escaped name is uppercased in its entirety:

```
GraphQL "OPEN"          -> kotlinName="OPEN",          wireName="OPEN"
GraphQL "created_at"    -> kotlinName="CREATED_AT",    wireName="created_at"
GraphQL "private"       -> kotlinName="PRIVATEVALUE",  wireName="private"
```

## Collision Resolution

When two distinct GraphQL names produce the same Kotlin candidate (after keyword escaping and case conversion), KotlinPoet's `NameAllocator` appends deterministic underscore-based suffixes:

```
Allocate "MyField"   -> myField       (first char lowercased)
Allocate "myField"   -> myField_      (collision -- suffix added)
Allocate "myField2"  -> myField2      (distinct -- no collision)
```

In this example, `"MyField"` and `"myField"` are distinct GraphQL names, but `replaceFirstChar { it.lowercase() }` in `allocatePropertyName` normalises both to `"myField"`. The allocator resolves the collision by appending `_` to the second candidate.

Each `GraphNameAllocator` instance is scoped (e.g., per class or per operation), so collisions are resolved within that scope only.

## Response Model Descriptor Names

Nested response classes carry path-qualified `@SerialName` descriptors. The descriptor encodes the full selection path from the root data class:

```kotlin
@Serializable
@SerialName("GetCurrentUserData")                          // root
data class GetCurrentUserData(
    @SerialName("viewer") val viewer: Viewer?,
) {
    @Serializable
    @SerialName("GetCurrentUserData.viewer")               // path-qualified
    data class Viewer(
        @SerialName("status") val status: Status?,
    ) {
        @Serializable
        @SerialName("GetCurrentUserData.viewer.status")    // path-qualified
        data class Status(val message: String?)
    }
}
```

The descriptor name ensures unique class-level identifiers, which is important when the same GraphQL nested type appears at different positions in different selections.

## See Also

- [Code Generation](Codegen.md) -- codegen DSL and naming convention reference
