## Test Fixtures

The `fixtures/` directory contains `.graphql` operation files and `.graphqls` schema files used
by the codegen-core module's unit and integration tests. Together they exercise the full range of
GraphQL constructs that the code generator must handle correctly.

### Directory Layout

```
fixtures/
├── simple/               # Minimal operations: basic queries, mutations, schemas
│   ├── queries/          # Single-operation queries (no fragments, no nesting)
│   ├── mutations/        # Single-operation mutations with variables
│   ├── fragments/        # Placeholder for simple fragment tests
│   ├── schemas/          # Minimal schemas tailored to the simple operations
│   └── responses/        # JSON payloads for response-conversion tests
├── advanced/             # Realistic operations exercising complex GraphQL features
│   ├── fragments/        # Queries with nested, multi-level fragments
│   ├── aliases/          # Queries that alias field names
│   ├── conditionals/     # Queries with `@include` / `@skip` directives
│   ├── interfaces/       # Queries whose response shapes include connections
│   ├── unions/           # Placeholder for union-type response tests
│   └── paging/           # Paginated queries with Page / PageInfo types
│       └── responses/    # Sample paged JSON payloads
├── schemas/              # Multi-target schemas for cross-schema tests
│   ├── anilist.graphqls  # Reduced AniList-like schema
│   └── edge.graphqls     # Minimal second schema with a different shape
└── README.md             # This file
```

### Provenance

The fixtures are inspired by, but reduced from, the GraphQL contracts used in
[anitrend-v2](https://github.com/AniTrend/anitrend-v2). Domain concepts (Media, Page, characters,
staff, activities) are borrowed from the public AniList API, while the simple fixtures use
GitHub-style viewer concepts. No real API keys or secrets are present; all payloads are
synthetic test data.

### Capability Map

| Fixture                                    | Exercises                                                              |
|--------------------------------------------|------------------------------------------------------------------------|
| `simple/queries/GetUser.graphql`            | Basic query parsing, simple scalar fields, no variables                |
| `simple/mutations/UpdateBio.graphql`        | Mutation with `String!` variable, nested response type                 |
| `simple/schemas/github-simple.graphqls`     | Minimal schema: interface (`Node`), mutations, input payload           |
| `simple/responses/GetUser.json`             | Response model deserialization for a flat viewer object                |
| `advanced/fragments/MediaDetail.graphql`    | Multi-fragment query, nested object types (title, coverImage)          |
| `advanced/aliases/AliasedFields.graphql`    | Field aliasing creates distinct Kotlin property names                  |
| `advanced/conditionals/FeedMedia.graphql`   | `@include` directive, inline fragments on union members                |
| `advanced/interfaces/MediaCharacters.graphql` | Connection types with `nodes` arrays                                  |
| `advanced/interfaces/responses/MediaCharacters.json` | Deserializing connection-shaped payloads                    |
| `advanced/paging/PagedMedia.graphql`        | Paginated query with `PageInfo`, list responses                        |
| `advanced/paging/responses/PagedMedia.json` | Deserializing paginated payloads with `hasNextPage`                   |
| `schemas/anilist.graphqls`                  | Reduced AniList schema for all advanced fixtures                       |
| `schemas/edge.graphqls`                     | Second schema shape for multi-target / multi-schema tests              |

### Adding New Fixtures

1. Place `.graphql` operation files in the appropriate directory under `simple/` or `advanced/`.
2. If the operation references types that aren't already covered, add them to one of the
   schema files under `schemas/` or create a new schema file.
3. For response-model tests, add a matching `.json` payload in the corresponding `responses/`
   directory.
4. Update the capability map in this README.
5. Use meaningful operation names that describe what the fixture tests.

### Schema Selection Rule

- The **`simple/`** fixtures use `schemas/github-simple.graphqls`.
- The **`advanced/`** fixtures use `schemas/anilist.graphqls`.
- The **`schemas/edge.graphqls`** file is reserved for tests that exercise the multi-schema
  (cross-registry) code path where the generator must distinguish types from different schemas.
