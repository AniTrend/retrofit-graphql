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

## GitHub Copilot Customization

This repository includes GitHub Copilot Chat customizations to improve development experience:

### Chat Modes and Prompts

- **Chat modes** are available under `.github/chatmodes/` providing specialized AI personas for different development tasks
- **Prompts** are located in `.github/prompts/` offering specific templates for common development workflows

### Repository-specific Instructions

Custom instructions under `.github/instructions/` help tailor GitHub Copilot Chat responses to this project's architecture and conventions. These instructions provide context about:

- The retrofit-graphql library's purpose and architecture
- Project scope and integration patterns  
- Development guidelines and best practices

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
