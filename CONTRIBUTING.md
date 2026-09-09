Thank you for contributing!

Quick checklist
- Fork & branch from main: chore/..., feat/..., fix/...
- Write tests for code changes (commonTest for shared logic).
- Run `./gradlew clean build` and fix linting/format issues.
- PR title format: [area] short-summary (e.g., [docs] add android quickstart).
- Link any relevant issues in the PR description.

Code style
- Follow Kotlin idioms. Run ktlint or the provided formatting task: `./gradlew ktlintFormat` (if configured).

Review & CI
- Pull requests must pass CI (build & tests on Linux + macOS).
- Maintain backward compatibility where possible; use deprecation annotations for breaking changes.

