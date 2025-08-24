---
applyTo: **
description: This file provides guidelines for developing, testing, and extending the retrofit-graphql library.
---

# Development Guidelines and Best Practices

When contributing to or extending the retrofit-graphql library, follow these established patterns and conventions to maintain consistency and quality.

## Code Organization and Architecture

### Library Structure
- **Core Logic** should reside in the `library` module under appropriate packages:
  - `annotation` – Custom annotations for GraphQL operations
  - `converter` – Retrofit converter implementation
  - `discovery` – File discovery and loading mechanisms
  - `model` – Request/response model classes
  - `util` – Utility classes and helpers

- **Sample Code** belongs in the `app` module to demonstrate usage patterns
- **Build Configuration** should be centralized in `buildSrc` for consistency

### Following Retrofit Patterns
- Implement converters using Retrofit's `Converter.Factory` pattern
- Use appropriate `RequestBody` and `ResponseBody` types for HTTP operations
- Leverage Retrofit annotations and maintain compatibility with existing interceptors
- Follow Retrofit's error handling conventions

## Coding Standards

### Kotlin Style
- Use **Ktlint/Spotless** for code formatting (configured in the build)
- Write idiomatic Kotlin with proper use of:
  - Immutable `val` declarations where possible
  - Data classes for model objects
  - Sealed classes for representing different states
  - Extension functions for utility operations

### Naming Conventions
- **Classes**: Use descriptive names that clearly indicate purpose
  - `GraphQLConverterFactory` for the main converter factory
  - `*Request` suffix for request model classes
  - `*Discovery` suffix for file discovery implementations
- **Methods**: Use verb-noun patterns that describe the operation
- **Constants**: Use SCREAMING_SNAKE_CASE for constant values
- **Packages**: Use lowercase with clear hierarchical organization

### Documentation Standards
- **KDoc Comments**: Document all public classes and methods
- **API Documentation**: Generated via Dokka for public API reference
- **Code Examples**: Include usage examples in KDoc for complex operations
- **README Updates**: Keep the main README current with setup instructions

## Testing Guidelines

### Unit Testing
- **JUnit 5** is configured for testing framework
- **MockK** is available for mocking dependencies
- Test coverage should focus on:
  - Annotation processing logic
  - GraphQL file parsing and loading
  - Variable binding and injection
  - Error handling scenarios

### Integration Testing
- Test actual Retrofit integration with sample GraphQL operations
- Verify multipart upload functionality with real file operations
- Test error scenarios with malformed GraphQL files or network issues

### Sample App Testing
- Use the sample app for manual testing of new features
- Ensure the sample app demonstrates all major library capabilities
- Test with various Android versions and configurations

## GraphQL File Management

### File Organization
- Store sample GraphQL files in `app/src/main/assets/graphql/`
- Organize files by operation type (queries, mutations, fragments)
- Use descriptive filenames that indicate the operation purpose

### Fragment Handling
- Implement proper fragment dependency resolution
- Test fragment inclusion in queries and mutations
- Ensure fragment references are correctly processed

### Variable Binding
- Support various parameter types (primitives, objects, lists)
- Handle nullable parameters appropriately
- Validate variable names match GraphQL definitions

## Build and Release Process

### Gradle Configuration
- **Android Configuration**: Maintain compatibility with modern Android versions
- **Dependency Management**: Keep dependencies current and minimal
- **Build Variants**: Support debug and release configurations appropriately

### Publishing Setup
- **JitPack Integration**: Ensure proper configuration for JitPack publishing
- **Version Management**: Follow semantic versioning for releases
- **Documentation Publishing**: Automated Dokka documentation generation

### Continuous Integration
- Maintain build stability across different environments
- Ensure tests pass on various Android API levels
- Validate code formatting and style checks

## Error Handling Best Practices

### Exception Management
- Use specific exception types for different error scenarios
- Provide meaningful error messages that help developers debug issues
- Handle file I/O errors gracefully when reading GraphQL files

### Logging Integration
- Support configurable logging levels
- Provide useful debug information without overwhelming output
- Allow developers to integrate their preferred logging frameworks

### Validation and Feedback
- Validate GraphQL file syntax where possible
- Provide clear error messages for annotation misuse
- Guide developers toward correct usage patterns in error messages

## Performance Considerations

### File Loading Optimization
- Cache loaded GraphQL files to avoid repeated asset access
- Implement efficient fragment resolution to minimize file reads
- Consider memory usage for large GraphQL files

### Network Efficiency
- Generate minimal request bodies by avoiding unnecessary data
- Support proper HTTP caching headers where applicable
- Minimize object allocation during request/response processing

## Extension Points

### Custom Discovery Plugins
- Follow the established plugin interface for file discovery
- Support various file sources (assets, external storage, network)
- Maintain backward compatibility with existing discovery mechanisms

### Logger Integration
- Provide clear interfaces for custom logger implementations
- Support different logging levels and filtering options
- Allow integration with popular Android logging frameworks

### Interceptor Compatibility
- Ensure custom OkHttp interceptors work correctly with GraphQL requests
- Test compatibility with common interceptor patterns (authentication, caching, etc.)
- Document any special considerations for interceptor usage

## Contributing Guidelines

- **Pull Requests**: Include comprehensive tests for new features
- **Issue Reporting**: Provide clear reproduction steps and environment details
- **Feature Requests**: Explain use cases and provide implementation suggestions
- **Documentation**: Update relevant documentation for any public API changes

Follow these guidelines to maintain the library's quality, consistency, and usability for the Android development community.