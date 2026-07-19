# DevOps Standards

## Overview

This document defines language-agnostic DevOps standards for CI/CD, deployment safety, supply chain security, and operational readiness.

These standards are the baseline for all repositories using GitHub Actions.

**When to load this document:** Any change involving workflow files, build/deploy automation, Dockerfiles, release pipelines, environments, or operational controls.

## Scope and Precedence

- **`DEVOPS_STANDARDS.md`** defines cross-repository policy (must/should rules).
- **`devops-spec.md`** defines repository-specific implementation details.
- If there is a conflict, **this standards document wins**.

## Core Principles

### 1. AI-Assisted, Human-Verified

- Use AI to accelerate pipeline authoring and debugging.
- Treat AI output as a draft; maintainers must verify correctness, security, and least privilege.
- Reject AI changes that introduce unnecessary services, broad permissions, or speculative complexity (YAGNI).

### 2. Keep Pipelines Minimal and Deterministic

- Prefer a small number of clear jobs with explicit dependencies.
- Avoid hidden behavior and magic defaults.
- Pin versions for tools and actions to keep outcomes reproducible.

### 3. Security by Default

- Default to least privilege, deny-by-default permissions.
- Use short-lived credentials where possible (OIDC over long-lived secrets).
- Protect CI configuration with review gates.

## Repository DevOps Contract (`devops-spec.md`)

Each repository should include a root-level `devops-spec.md` that captures:

- Runtime and build toolchain versions (for example Java 21, Node 22)
- Required CI steps and exact commands
- Test tiers (unit, integration, optional E2E)
- Container build and runtime requirements
- Artifact and image publishing rules
- Environment model (dev/stage/prod)
- Security controls required for this repository

This file is intentionally concrete and implementation-specific so automation and AI output remain consistent.

### Documentation Repository Note

- In a standards-only repository (like this one), `devops-spec.md` serves as a reference template, not an executable pipeline contract.
- The authoritative `devops-spec.md` must live in each application repository (for example backend/frontend), where real build, test, container, and deploy commands run.
- Enforcement (required checks, branch protection, and workflow behavior) should be configured in those application repositories.

## GitHub Actions Standards

### Workflow Design

- Standard workflow naming:
  - `ci.yml` for continuous integration
  - `release.yml` for release/publish
  - `deploy.yml` for deployment (if used)
- CI must run on pull requests and pushes to the default branch.
- Use path filters only when needed; avoid accidentally skipping required checks.

### Required CI Behavior

- On pull request:
  - Run unit tests and build validation.
  - Run integration tests when configured in the repository spec.
  - Upload test reports/artifacts on failure.
- On default branch push:
  - Run the same validation as pull requests.
  - Publish artifacts only if explicitly defined by repository policy.

### Concurrency and Cost Control

- Use `concurrency` to avoid duplicate runs on stale commits.
- Prefer canceling in-progress runs for the same PR branch.
- Keep cache usage targeted and bounded to avoid cache thrashing.

### Reusable Workflows

- Prefer `workflow_call` reusable workflows for common CI patterns across repositories.
- Keep repository-specific logic in the caller workflow and repo-specific spec.
- Reusable workflows must not elevate permissions beyond caller intent.

## Actions and Dependency Hygiene

- Pin third-party actions to full commit SHAs in production workflows.
- Use trusted actions and review source before adoption.
- Keep actions updated with controlled automation (for example Dependabot).
- Avoid introducing third-party actions when native shell/script is simpler and safer.

## Token Permissions and Secrets

### GITHUB_TOKEN Permissions

- Always set explicit `permissions` at workflow or job scope.
- Start with `contents: read` and add only required scopes.
- Do not grant write scopes unless the job needs them.

### Secrets

- Never hardcode credentials in workflow files.
- Use repository/environment/organization secrets with least access.
- Prefer OIDC for cloud authentication to reduce long-lived static credentials.
- Do not print secrets; mask sensitive non-secret values with `::add-mask::`.

### Cache Safety

- Never cache files containing tokens, credentials, or private keys.
- Use deterministic cache keys derived from lockfiles and runner OS.

## Branch Protection and Change Control

- Protect default branches with required status checks.
- Require PR review for changes under `.github/workflows/**`.
- Use CODEOWNERS for workflow and deployment configuration paths.
- Do not allow automation to bypass PR review controls.

## Test and Quality Gates

- CI must fail the workflow on test/build failures.
- Unit tests are mandatory for every code change.
- Integration tests are mandatory before release/deploy.
- Enforce lint/static checks relevant to the repository stack.

## DevSecOps Baseline

At minimum, repositories should enable:

- Dependency review on pull requests
- Secret scanning and push protection
- Dependency vulnerability alerts/updates (for example Dependabot)

Recommended additions:

- Code scanning (for example CodeQL)
- Artifact attestations for published artifacts/images
- SBOM generation for release artifacts
- Container image scanning when images are produced

## Docker and Container Standards

- Use multi-stage Docker builds for production images.
- Use a minimal runtime image and run as non-root.
- Keep build context small with `.dockerignore`.
- Order layers for caching efficiency (dependencies before source where possible).
- Expose only required ports and avoid unnecessary packages/tools.

## Testcontainers in CI

- Integration tests using Testcontainers require a Docker-compatible runtime.
- On GitHub-hosted Linux runners, Docker support is typically available out of the box.
- Do not enable reusable containers in CI unless there is a documented reason.
- Capture diagnostics (test reports and selective Docker info/logs) on failure.

## Deployment Safety Standards

- Separate build and deploy concerns into distinct jobs/stages.
- Require explicit environment controls for production deployments.
- Support rollback strategy before enabling automated production deploys.
- Use progressive rollout strategies (canary/blue-green) when operationally justified.

## Pre-Merge, Pre-Deploy, Post-Deploy Checklists

### Pre-Merge

- [ ] Workflow changes reviewed by code owners
- [ ] Actions pinned and permissions minimized
- [ ] Required tests pass and failure artifacts are configured
- [ ] No new unmanaged secrets introduced

### Pre-Deploy

- [ ] Build artifacts are from trusted CI runs
- [ ] Required quality and security checks passed
- [ ] Deployment target and change scope are confirmed
- [ ] Rollback path is documented and tested

### Post-Deploy

- [ ] Smoke checks complete successfully
- [ ] Error rate and latency are within expected bounds
- [ ] Alerts and logs show no critical regressions
- [ ] Deployment record and follow-up actions are documented

## Anti-Patterns (Reject)

- Broad default permissions (for example unnecessary write scopes)
- Unpinned third-party actions in production workflows
- Skipping tests for speed without explicit emergency process
- Committing credentials or passing secrets through unsafe channels
- Adding deployment complexity before CI quality gates are stable

## Infrastructure as Code (IaC) Standards

### General Principles

- **Version Control**: All infrastructure code in git repositories with proper branching strategy
- **Reproducible**: Identical infrastructure deployable from same code + configuration
- **Immutable**: Replace infrastructure components rather than modify in place
- **Least Privilege**: Minimal permissions for all resources and service accounts
- **Fail Fast**: Validate infrastructure changes before applying to environments

### Code Organization

```
infrastructure/
├── environments/
│   ├── dev/                 # Development environment configs
│   ├── staging/             # Pre-production environment
│   └── prod/                # Production environment
├── modules/                 # Reusable infrastructure components
│   ├── networking/          # VPC, subnets, security groups
│   ├── compute/             # VMs, containers, serverless functions
│   └── storage/             # Databases, object storage, caches
├── shared/                  # Cross-environment resources
│   └── monitoring/          # Shared observability infrastructure
└── policies/                # Security and compliance policies
```

### IaC Quality Gates

- [ ] **Code formatted**: `terraform fmt`, proper YAML/JSON formatting
- [ ] **Syntax validation**: `terraform validate`, template syntax checks
- [ ] **Security scanning**: Tools like Checkov, tfsec for configuration security
- [ ] **Cost estimation**: Automated cost impact analysis for changes
- [ ] **Change review**: Infrastructure changes reviewed by platform team

### Drift Detection

- **Daily drift checks**: Compare live infrastructure against code definitions
- **Automated alerts**: Notify on configuration drift detection
- **Remediation process**: Document and approve legitimate drift, fix unauthorized changes

---

## Environment and Configuration Standards

### Environment Parity

Maintain consistency across development, staging, and production:

- [ ] **Infrastructure**: Same resource types, network topology, security policies
- [ ] **Dependencies**: Same service versions, database schemas, external integrations  
- [ ] **Configuration**: Same config management approach (different values)
- [ ] **Data**: Representative data volume and patterns in staging
- [ ] **Monitoring**: Same observability tools and alerting configuration

### Configuration Management

#### Hierarchy (highest to lowest precedence)
1. **Command-line flags**: `--database-host=override.example.com`
2. **Environment variables**: `DATABASE_HOST=prod.example.com`
3. **Configuration files**: `config/production.yaml`
4. **Application defaults**: Hard-coded fallback values

#### Environment Variable Standards
```bash
# Good: Clear naming with prefixes
DATABASE_HOST=db.example.com
DATABASE_PORT=5432
FEATURE_BETA_CHECKOUT=enabled
LOG_LEVEL=INFO
API_RATE_LIMIT_PER_MINUTE=1000

# Bad: Ambiguous or inconsistent naming
HOST=somewhere
CONFIG=mixed,values
DEBUG=1
VERBOSE=true
```

#### Configuration Security
- [ ] **No secrets in code**: Use secret management systems (see SECURITY_STANDARDS.md)
- [ ] **Environment separation**: Different credentials per environment
- [ ] **Access controls**: Limit who can modify production configuration
- [ ] **Audit logging**: Track configuration changes and access

---

## Observability Standards

### Golden Signals Monitoring

Monitor these four metrics for every service:

| Signal | Definition | Target | Alert Threshold |
|--------|------------|--------|----------------|
| **Latency** | Response time for requests | p95 < 500ms | p95 > 1000ms |
| **Traffic** | Request volume per second | Baseline varies | 50% deviation from baseline |
| **Errors** | Error rate percentage | < 0.1% | > 1% for 5 minutes |
| **Saturation** | Resource utilization | < 80% CPU/Memory | > 90% for 10 minutes |

### Required Service Metrics

```prometheus
# HTTP metrics (required for web services)
http_requests_total{method, status, endpoint}
http_request_duration_seconds{method, endpoint}  
http_requests_in_flight{method, endpoint}

# Resource metrics (required for all services)
cpu_usage_percent
memory_usage_bytes
disk_usage_bytes

# Business metrics (customize per service)
orders_processed_total
payment_amount_dollars
active_user_sessions
```

### Service Level Objectives (SLOs)

Define SLOs for customer-facing services:

```yaml
# Example SLO definition
service: order-api
slo:
  availability:
    target: 99.9%          # 99.9% uptime
    window: 30d            # Monthly measurement
    
  latency:
    target: 95%            # 95% of requests under threshold
    threshold: 500ms
    window: 24h
    
  error_rate:
    target: 99.5%          # 99.5% success rate
    window: 7d
```

### Distributed Tracing

- [ ] **Correlation IDs**: Every request has unique identifier across services
- [ ] **Span hierarchy**: Parent-child relationships between operations
- [ ] **Critical path tracing**: End-to-end request flow visibility
- [ ] **Error attribution**: Associate failures with specific operations

Integrate with logging standards: [LOGGING_STANDARDS.md](./LOGGING_STANDARDS.md)

---

## Incident Response and Postmortems

### Incident Severity Classification

| Severity | Impact | Response SLA | Examples |
|----------|---------|-------------|----------|
| **P1** | Complete service outage | 15 minutes | API down, database unavailable |
| **P2** | Major degradation | 1 hour | High error rates, slow performance |  
| **P3** | Minor service impact | 4 hours | Single feature broken |
| **P4** | No customer impact | Next business day | Internal tool issues |

### Incident Response Process

#### Immediate Actions (First 15 minutes)
1. **Acknowledge**: Confirm incident within SLA response time
2. **Assess**: Determine severity and scope of impact
3. **Communicate**: Update status page, notify stakeholders
4. **Mitigate**: Take immediate actions to reduce customer impact

#### Investigation and Resolution
- **Root Cause Analysis**: Identify what failed and why
- **Fix Implementation**: Apply permanent fix (not just workaround)
- **Verification**: Confirm resolution across all affected systems
- **Communication**: Keep stakeholders informed throughout

### Postmortem Requirements

**Required for**: P1 and P2 incidents, near-misses with potential for major impact

#### Postmortem Template
```markdown
# Incident Postmortem: [Service] - [Date]

## Summary
- **Incident Date**: YYYY-MM-DD HH:MM UTC
- **Duration**: X hours Y minutes
- **Severity**: P1/P2/P3
- **Services Affected**: [List]
- **Customer Impact**: [Description]

## Timeline
| Time (UTC) | Event Description |
|------------|------------------|
| 14:30 | Alert: High error rate detected |
| 14:35 | On-call engineer paged |
| 14:45 | Root cause identified |
| 15:15 | Fix deployed |
| 15:30 | Service fully restored |

## Root Cause
[Technical explanation of failure]

## Impact Assessment
- **Users Affected**: X users
- **Revenue Impact**: $Y estimated loss
- **SLO Impact**: Used Z% of monthly error budget

## Action Items
| Action | Owner | Due Date | Status |
|--------|-------|----------|--------|
| Add monitoring for X | @engineer | 2026-03-21 | Open |
| Improve rollback automation | @sre | 2026-03-28 | In Progress |

## Lessons Learned
- **What went well**: [Positive aspects]
- **What could improve**: [Areas for enhancement]
```

#### Postmortem Culture
- **Blameless**: Focus on systems and processes, not individuals
- **Learning-focused**: Share knowledge across the organization
- **Action-oriented**: Concrete follow-up items with clear ownership
- **Transparent**: Make postmortems visible organization-wide

---

## Cost and Capacity Management

### Resource Optimization

#### Automated Cost Controls
- [ ] **Resource tagging**: Tag all cloud resources with owner, environment, project
- [ ] **Usage monitoring**: Daily cost and utilization tracking with alerts
- [ ] **Right-sizing**: Regular review of over/under-provisioned resources
- [ ] **Scheduled scaling**: Scale down non-production environments outside business hours
- [ ] **Lifecycle policies**: Automatic deletion of temporary/unused resources

#### Capacity Planning Process

```yaml
# Example capacity metrics tracking
service: order-processing-api
current_state:
  instances: 8
  cpu_cores: 32
  memory_gb: 64
  storage_gb: 500

utilization:
  avg_cpu: 45%
  peak_cpu: 75% 
  avg_memory: 60%
  peak_memory: 85%

growth_forecast:
  6_months: 130%    # 30% growth expected
  12_months: 180%   # 80% growth expected

scaling_triggers:
  cpu_threshold: 80%
  memory_threshold: 85%
  latency_p95: 500ms
```

### Budget Governance

#### Cost Monitoring
- [ ] **Budget alerts**: Set spending limits per environment/team
- [ ] **Anomaly detection**: Alert on unusual spending patterns
- [ ] **Cost attribution**: Track expenses by business unit/project
- [ ] **Forecast warnings**: Predict and prevent budget overruns

#### Resource Lifecycle Management
- [ ] **Development**: Auto-delete inactive resources after 7 days
- [ ] **Testing**: Scale down outside business hours
- [ ] **Production**: Use reserved instances for predictable workloads
- [ ] **Storage**: Automated tiering to optimize costs

---

## Safe Deployment Example Workflow

This example demonstrates end-to-end deployment following DevOps standards:

### Scenario: Payment Service Update

**Context**: Deploying fraud detection enhancement to payment processing service

#### Phase 1: Pre-Deployment Preparation

**Infrastructure Readiness:**
```bash
# 1. Verify environment health
kubectl get pods -n payment-service
# All pods running ✓

# 2. Check monitoring dashboards
# - Error rate < 0.1% ✓
# - Response time p95 < 400ms ✓ 
# - Resource utilization < 70% ✓

# 3. Validate configuration
kubectl get configmap payment-config -o yaml
# Fraud detection settings configured ✓
```

**Deployment Plan:**
- **Strategy**: Blue-green deployment for zero downtime
- **Rollback**: Switch traffic back to blue environment if issues detected
- **Testing**: Automated smoke tests + manual fraud detection validation
- **Monitoring**: Enhanced alerting for fraud detection accuracy

#### Phase 2: Blue-Green Deployment

**Step 1: Deploy to Green Environment**
```bash
# Deploy new version to green environment
kubectl apply -f k8s/green-deployment.yaml
kubectl rollout status deployment/payment-service-green

# Verify green environment health
kubectl get pods -l app=payment-service,version=green
# All pods ready ✓

# Run smoke tests against green
npm run test:smoke -- --environment=green
# Payment processing: ✓
# Fraud detection: ✓
# Database connectivity: ✓
```

**Step 2: Traffic Switch**
```bash
# Switch 10% traffic to green for canary testing
kubectl patch service payment-service -p '{"spec":{"selector":{"version":"green"}}}'
# Monitor for 15 minutes

# Key metrics to watch:
# - Error rate: Should remain < 0.1%
# - Latency p95: Should remain < 500ms  
# - Fraud detection rate: Should be within 5% of baseline
# - Payment success rate: Should remain > 99.5%
```

**Step 3: Full Cutover**
```bash
# If canary successful, switch 100% traffic
kubectl patch service payment-service -p '{"spec":{"selector":{"version":"green"}}}'

# Keep blue environment running for 1 hour as rollback safety net
```

#### Phase 3: Post-Deployment Validation

**Business Metrics Verification:**
```bash
# Check fraud detection accuracy (enhanced feature)
# Expected: 15% improvement in fraud catch rate
curl -s https://api.internal/metrics/fraud-detection | jq '.catch_rate'
# Baseline: 85% → Target: 98%+ ✓

# Verify payment processing performance
# Expected: No degradation in legitimate transaction processing  
curl -s https://api.internal/metrics/payments | jq '.success_rate'
# Target: > 99.5% ✓
```

**System Health Checks:**
- [ ] All health checks passing
- [ ] Error logs show no new error patterns
- [ ] Resource utilization within normal ranges
- [ ] No customer support tickets related to payments
- [ ] SLO error budget consumption < 0.01%

#### Phase 4: Success Criteria & Rollback Plan

**Success Criteria (must achieve within 2 hours):**
- [ ] Fraud detection accuracy improved by 10%+ (target: 15%)
- [ ] Payment processing latency unchanged (< 500ms p95)
- [ ] Error rate remains < 0.1% 
- [ ] No increase in customer complaints
- [ ] Resource usage within 10% of baseline

**Automated Rollback Triggers:**
```yaml
# Rollback automation configuration
rollback_triggers:
  error_rate:
    threshold: 1%
    duration: 300s    # 5 minutes
    
  latency:
    metric: p95
    threshold: 1000ms
    duration: 600s    # 10 minutes
    
  fraud_accuracy:
    threshold: 80%    # Below baseline
    duration: 900s    # 15 minutes
```

**Manual Rollback Process:**
```bash
# Emergency rollback (< 2 minutes)
kubectl patch service payment-service -p '{"spec":{"selector":{"version":"blue"}}}'
kubectl rollout status deployment/payment-service-blue

# Verify rollback success
curl -s https://healthz.payment-service.internal/health
# Should return 200 OK

# Post-rollback investigation
# 1. Capture metrics and logs from failed deployment
# 2. Analyze root cause in development environment  
# 3. Implement fixes and repeat deployment process
```

This example demonstrates how DevOps standards ensure safe, observable, and reversible deployments with clear success criteria and automated safety nets.

## References

### GitHub Actions and CI/CD
- [GitHub Actions secure use](https://docs.github.com/en/actions/security-guides/security-hardening-for-github-actions)
- [Workflow syntax](https://docs.github.com/en/actions/reference/workflows-and-actions/workflow-syntax-for-github-actions)
- [Reusable workflows](https://docs.github.com/en/actions/how-tos/reuse-automations/reuse-workflows)
- [Dependency caching reference](https://docs.github.com/en/actions/reference/workflows-and-actions/dependency-caching)
- [Use GITHUB_TOKEN for authentication](https://docs.github.com/en/actions/tutorials/authenticate-with-github_token)
- [Using secrets in workflows](https://docs.github.com/en/actions/how-tos/write-workflows/choose-what-workflows-do/use-secrets)
- [Dependency review action](https://github.com/actions/dependency-review-action)

### Infrastructure and Deployment
- [Infrastructure as Code best practices](https://docs.aws.amazon.com/wellarchitected/latest/framework/welcome.html)
- [Terraform best practices](https://www.terraform.io/docs/cloud/guides/recommended-practices/index.html)
- [Blue-green deployment patterns](https://martinfowler.com/bliki/BlueGreenDeployment.html)
- [Canary deployment strategies](https://martinfowler.com/bliki/CanaryRelease.html)

### Observability and Monitoring
- [Site Reliability Engineering (SRE) practices](https://sre.google/sre-book/table-of-contents/)
- [Prometheus monitoring best practices](https://prometheus.io/docs/practices/naming/)
- [Service Level Objectives (SLO) guidance](https://cloud.google.com/blog/products/devops-sre/availability-part-deux-CRE-life-lessons)
- [Distributed tracing standards](https://opentelemetry.io/docs/concepts/observability-primer/)

### Security and Secrets Management  
- [SECURITY_STANDARDS.md](./SECURITY_STANDARDS.md) - Comprehensive security controls
- [LOGGING_STANDARDS.md](./LOGGING_STANDARDS.md) - Structured logging and correlation IDs
- [Secret management best practices](https://cloud.google.com/secret-manager/docs/best-practices)

### Container and Build Optimization
- [Docker build cache optimization](https://docs.docker.com/build/cache/optimize/)
- [Dockerfile best practices](https://docs.docker.com/develop/develop-images/dockerfile_best-practices/)
- [Testcontainers runtime requirements](https://java.testcontainers.org/supported_docker_environment/)

---

**Last Updated**: March 14, 2026  
**Version**: 2.0 (Expanded to include comprehensive DevOps operational standards)
