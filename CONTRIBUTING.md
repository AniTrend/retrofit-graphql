# Contributing

When contributing to this repository, please first discuss the change you wish to make via issue, email, or any other method with the owners of this repository before making a change via a pull request.

Please note we have a code of conduct, please follow it in all your interactions with the project.

## Contributing Guidelines

Please ensure your pull request adheres to the following guidelines:

- Search previous suggestions for duplicates before making a new one.
- Make an individual pull request for each suggestion or feature.
- Use the following format: `- [Bookmark Title](link): Description.`
- Titles should be [capitalized](http://grammar.yourdictionary.com/capitalization/rules-for-capitalization-in-titles.html).
- New categories or improvements to the existing categorization are welcome.
- Be sure not to stage any files in excluded in .gitignore
- Check your spelling and grammar.

See [code of conduct](./CODE_OF_CONDUCT.md)

## AI Agent Customization

This repository includes agent-agnostic customizations to improve development experience with AI coding assistants:

### Repository Context (`AGENTS.md`)

The root `AGENTS.md` file provides comprehensive project context including module organization, build conventions, code style, testing guidelines, and scope/limitations. This is the canonical reference for any AI agent working on this repository.

### Agent Skills (`.agents/skills/`)

Repository-specific skills provide specialized guidance for recurring tasks:

| Skill | Purpose |
|-------|---------|
| `jenv-gradle-low-ram` | JDK alignment and low-RAM Gradle invocation |
| `retrofit-graphql-build-dependencies` | Build map, module dependencies, CI concerns |
| `retrofit-graphql-kdoc-dokka` | KDoc checklist and Dokka generation workflow |
| `retrofit-graphql-reference-map` | Module-to-package map and placement heuristics |

### Conventional Commit Support

Use the **conventional-commit** prompt to generate properly formatted commit messages. The prompt supports generic commit scopes appropriate for library development, such as:
- `feat(api)`: New API features
- `fix(converter)`: Bug fixes in converter logic  
- `docs`: Documentation updates
- `test`: Test improvements
- `refactor`: Code refactoring
- `chore`: Maintenance tasks

### Documentation

For comprehensive API documentation and usage examples, visit the [Dokka site](https://anitrend.github.io/retrofit-graphql/).
