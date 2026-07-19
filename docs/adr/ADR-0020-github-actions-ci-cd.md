# ADR-0020: Use GitHub Actions for CI/CD

## Status

Accepted

## Date

2026-07-19

## Context

The Deck Assemble project requires a continuous integration and continuous deployment (CI/CD) pipeline. We need to automate the compilation, testing, and packaging of our application to ensure that broken changes do not reach production.

Manual testing and deployment are slow, inconsistent, and prone to human error.

## Decision

We will use GitHub Actions to run our CI/CD workflows. The workflow will trigger on pull requests and pushes to the main branch. It will compile the Java code, run unit and integration tests (using Testcontainers), build the application artifact, and package it as a Docker image.

This pipeline will act as a gate, blocking pull requests from being merged if any tests or builds fail.

## Alternatives Considered

### Alternative 1: Railway native deployment hooks only
- **Pros**: Simple setup, automatic deployments on push.
- **Cons**: Lacks a build and test gate, meaning broken code or failing tests can be deployed directly to production.
- **Why rejected**: We need a strict testing gate to prevent broken code from being deployed.

### Alternative 2: CircleCI
- **Pros**: Fast build times, powerful configuration options.
- **Cons**: Requires managing an additional third-party service, potential integration overhead.
- **Why rejected**: GitHub Actions is already integrated with our repositories and provides sufficient features for our needs.

### Alternative 3: Jenkins
- **Pros**: Highly customizable, self-hosted, no external service dependency.
- **Cons**: High operational overhead to set up, secure, and maintain the Jenkins server.
- **Why rejected**: The maintenance overhead of a self-hosted Jenkins server is not justified for our team size.

## Consequences

### Positive
- Automated testing and validation for every code change.
- Prevention of broken code reaching the main branch or production.
- Free tier usage for public repositories.

### Negative
- Build times are subject to GitHub runner availability and performance.
- Developers must manage YAML workflow configurations.

### Neutral
- Secrets for deployment must be securely stored in GitHub repository settings.

## References

- GitHub Actions Documentation
- ADR-0014: Use Testcontainers for database integration tests
- ADR-0019: Maintain local, dev, and prod environments
