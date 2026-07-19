# ADR-0018: Use Railway for managed PostgreSQL and API hosting

## Status

Accepted

## Date

2026-07-19

## Context

The Deck Assemble backend API and PostgreSQL database need a hosting platform for development and production environments. We need a platform that simplifies database management, environment variable configuration, and application deployment.

We want to avoid complex cloud infrastructure management while ensuring high availability and simple scaling.

## Decision

We will use Railway to host the Spring Boot API service and managed PostgreSQL database in both `dev` and `prod` environments. Railway provides a single platform for compute and database services, simple environment variable and secret management, and automated deployments triggered by GitHub pushes.

This platform allows us to deploy and scale our backend services with minimal operational overhead.

## Alternatives Considered

### Alternative 1: AWS ECS and RDS
- **Pros**: Highly scalable, enterprise-grade security, extensive configuration options.
- **Cons**: High complexity, steep learning curve, significant time required for setup and maintenance.
- **Why rejected**: The operational complexity of AWS is unnecessary for our current scale and would slow down development.

### Alternative 2: Fly.io
- **Pros**: Global deployments, close to users, simple CLI.
- **Cons**: Managed PostgreSQL experience is less polished and requires more manual maintenance compared to Railway.
- **Why rejected**: Railway offers a more user-friendly and reliable managed database experience.

### Alternative 3: Self-hosted VPS (e.g., DigitalOcean Droplet)
- **Pros**: Full control over the server, low direct costs.
- **Cons**: High operational burden, manual setup of database backups, security patches, and deployment scripts.
- **Why rejected**: The team wants to focus on building features rather than managing server infrastructure.

## Consequences

### Positive
- Single platform for both API compute and PostgreSQL database.
- Automated deployments triggered by GitHub pushes.
- Simple management of secrets and environment variables.

### Negative
- Dependency on Railway's platform availability and pricing model.
- Less control over low-level database configuration compared to AWS RDS or self-hosting.

### Neutral
- Database backups and scaling are managed through the Railway dashboard.

## References

- Railway Documentation
- ADR-0003: Use PostgreSQL for persistence
