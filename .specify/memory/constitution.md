# Schema Kit V2 Constitution

## Core Principles

### I. Library-First Architecture
Every feature starts as a standalone library. Libraries must be self-contained, independently testable, documented with clear purpose. No organizational-only libraries.

### II. Test-First Development (NON-NEGOTIABLE)
TDD mandatory: Tests written → User approved → Tests fail → Then implement. Red-Green-Refactor cycle strictly enforced. All functional requirements must have corresponding tests.

### III. Multi-Module Gradle Structure
Project organized as multi-module Gradle build with:
- One module per functional domain
- Clear separation of concerns
- Minimal coupling between modules
- Shared utilities in separate module

### IV. API Design
Public APIs are stable and versioned. Migration between versions supported. Clear interfaces, minimal surface area.

### V. Database Dialect Support
Support multiple database platforms with proper abstraction. Dialect-specific logic isolated. No platform-specific leaks into core interfaces.

### VI. Performance Standards
Schema comparison and migration generation complete within 30 seconds. Memory usage bounded by schema size. Lazy loading for large schemas.

### VII. CLI and Programmatic Access
All functionality accessible via both CLI and programmatic API. Text-based protocol: stdin/args → stdout, errors → stderr. Support JSON + human-readable formats.

## Additional Constraints

### Java Version Compatibility
- Source compatibility: Java 8
- Maintain compatibility across major releases
- Clear deprecation policy with migration guides

### Testing Requirements
- Unit tests for all public APIs
- Integration tests for database providers
- Contract tests for inter-module communication
- Minimum 80% code coverage

## Development Workflow

- Code review required for all changes
- PRs must pass all tests including integration tests
- Breaking changes require major version bump
- Documentation updated with every feature

## Governance

Constitution supersedes all other practices. All PRs/reviews must verify compliance. Complexity must be justified with architectural decision record.

**Version**: 1.0.0 | **Ratified**: 2025-12-01 | **Last Amended**: 2025-12-01
