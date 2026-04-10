# GitHub Copilot Chat Customization

This directory contains GitHub Copilot customization files for the retrofit-graphql repository.

## Structure

- `instructions/` – Repository-specific context and guidance automatically applied to Copilot sessions.
- `skills/` – Reusable skill definitions for recurring tasks such as Gradle builds, KDoc, and build dependency management.
- `workflows/` – GitHub Actions workflows, including `copilot-setup-steps.yml` for pre-warming the Copilot agent environment.

## Usage

Files in `instructions/` are automatically recognized by GitHub Copilot to provide enhanced assistance tailored to the retrofit-graphql library. Skills in `skills/` can be invoked explicitly when working on specific concerns such as low-RAM Gradle builds or KDoc updates.
