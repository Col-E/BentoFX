# BentoFX Maintainers Guide

## Table of Contents

- [Project Health](#project-health)
- [GitHub Workflows](#github-workflows)
  - [Build](#build-workflow)
  - [Static Analysis and Coverage](#static-analysis-and-coverage)
    - [Running SonarQube locally](#running-sonarqube-locally)
- [Releases](#releases)
- [Repository Administration](#repository-administration)
- [Credentials](#credentials)
  - [Credential Resolution Order](#credential-resolution-order)
  - [Credential Usage Matrix](#credential-usage-matrix)

This guide complements [CONTRIBUTING.md](CONTRIBUTING.md) by documenting repository administration,
release management, CI/CD maintenance, and other tasks requiring repository write access or maintainer judgment.

## Project Health

| Service | Health | Purpose |
|---------|--------|---------|
| **[GitHub Actions](https://github.com/philliplbryant/BentoFX/actions)** | [![Build](https://github.com/philliplbryant/BentoFX/actions/workflows/build.yml/badge.svg)](https://github.com/philliplbryant/BentoFX/actions/workflows/build.yml) | Build and test status |
| **[SonarCloud](https://sonarcloud.io/summary/new_code?id=philliplbryant_BentoFX)** | [![Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=philliplbryant_BentoFX&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=philliplbryant_BentoFX) | Quality Gate, Maintainability, Reliability, and Security |
| **[Codecov](https://codecov.io/gh/philliplbryant/BentoFX)** | [![Codecov](https://codecov.io/gh/philliplbryant/BentoFX/graph/badge.svg)](https://codecov.io/gh/philliplbryant/BentoFX) | Coverage reports and historical trends |
| **[CodeQL](https://github.com/philliplbryant/BentoFX/security/code-scanning)** | **[Security Results](https://github.com/philliplbryant/BentoFX/security/code-scanning)** | GitHub code scanning results |
| **[Qodana](https://github.com/philliplbryant/BentoFX/actions/workflows/qodana.yml)** | **[Inspection Results](https://github.com/philliplbryant/BentoFX/actions/workflows/qodana.yml)** | JetBrains inspections and static analysis |
| **[Maven Central](https://central.sonatype.com/artifact/software.coley.bento-fx/core)** | [![Maven Central](https://img.shields.io/maven-central/v/software.coley.bento-fx/core.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/software.coley.bento-fx/core) | Published artifacts and latest release |

## GitHub Workflows

<h3 id="build-workflow">Build</h3>
Ensure the Build workflow passes before merging pull requests.
```terminal
gradlew build checkAll
```

<h3 id="static-analysis-and-coverage">Static Analysis and Coverage</h3>

SonarQube, CodeQL, Qodana, and Codecov run on every push and pull request. See [CONTRIBUTING.md](CONTRIBUTING.md#static-analysis-and-coverage) for what each tool does, which can fail the build, and where to find results.

<h4 id="running-sonarqube-locally">Running SonarQube locally</h4>

A local scan publishes to the same SonarQube Cloud project, so name the branch and let the results land under it rather than the main branch. `SONAR_TOKEN` must be set.

Analysis quality depends on the Java classpath. `sonar-project.properties` points `sonar.java.binaries` at the compiled classes, but the dependency jars live in the Gradle cache, so `check` runs `sonarClasspath` in each module to write `build/sonar/libraries.txt` and `build/sonar/test-libraries.txt`. Without them, the rules needing type resolution are skipped silently and a scan disagrees with what SonarQube for IDE reports on identical code.

The lists are too long for a Windows command line, so join them into a settings file instead of passing them as arguments:

```terminal
gradlew build checkAll
join() { find . -path "*/build/sonar/$1" -exec cat {} \; -exec echo \; | grep -v '^$' | paste -sd, -; }
{ cat sonar-project.properties
  echo "sonar.java.libraries=$(join libraries.txt)"
  echo "sonar.java.test.libraries=$(join test-libraries.txt)"
} > /tmp/sonar-local.properties
sonar-scanner -Dproject.settings=/tmp/sonar-local.properties \
  -Dsonar.host.url=https://sonarcloud.io \
  -Dsonar.token="$SONAR_TOKEN" \
  -Dsonar.branch.name="$(git branch --show-current)"
```

Coverage comes from the aggregated JaCoCo reports, so run the build first. A scan run against stale reports imports no coverage and replaces whatever the branch last reported.

Maintainer responsibilities:

- Review new findings from each tool and determine whether they should be fixed or intentionally accepted.
- For Qodana specifically: there is no committed baseline. Every finding Qodana reports is live, and anything to be permanently accepted belongs in `qodana.yaml` (an `exclude` entry or a profile change) where the decision is reviewable in a diff.
- Monitor Codecov coverage trends for unexpected drops.
- Review CodeQL alerts in the GitHub Security tab.

## Releases

- Update project version(s).
- Publish the release with JReleaser.
- Verify the generated GitHub Release notes.
- Verify publication to Maven Central.

See the [JReleaser documentation](https://jreleaser.org/guide/latest/) for release configuration, signing, and publishing details.
## Repository Administration

- Review Dependabot pull requests.
- Review Codecov coverage trends and CodeQL alerts.
- Maintain branch protection rules and required status checks.

## Credentials

The repository does not store publishing or analysis credentials. Configure them externally for local development or CI.

<h3 id="credential-resolution-order">Credential Resolution Order</h3>

When multiple configuration mechanisms are available, use the following preference order:

1. **GitHub Actions** (repository or organization secrets)
2. **Local Environment** (environment variables)
3. **User Config** (`~/.jreleaser/config.properties` and `~/.gradle/gradle.properties`)
4. **Repository Configuration** (references to credential names only; never secret values)

Never commit credentials, personal access tokens, signing keys, passwords, or IDE workspace files (such as `.idea/workspace.xml`) that may contain local credential configuration.

<h3 id="credential-usage-matrix">Credential Usage Matrix</h3>

| Task / Workflow | Required Credentials | Typical Location |
|-----------------|----------------------|------------------|
| Run SonarCloud analysis | `SONAR_TOKEN` | **Local**<br/>**GitHub Actions** |
| Publish a release with JReleaser | `JRELEASER_GITHUB_TOKEN`, `JRELEASER_MAVENCENTRAL_USERNAME`,`JRELEASER_MAVENCENTRAL_TOKEN`,<br/>`JRELEASER_GPG_PUBLIC_KEY`, `JRELEASER_GPG_SECRET_KEY`, `JRELEASER_GPG_PASSPHRASE` | **Local**<br/>**GitHub Actions** |
| Publish signed artifacts to Maven Central | `JRELEASER_MAVENCENTRAL_USERNAME`, `JRELEASER_MAVENCENTRAL_TOKEN`, `JRELEASER_GPG_SECRET_KEY`,<br/>`JRELEASER_GPG_PASSPHRASE` | **Local**<br/>**GitHub Actions** |
| Create GitHub releases | `JRELEASER_GITHUB_TOKEN` | **Local**<br/>**GitHub Actions** |
| Upload coverage to Codecov (if enabled) | `CODECOV_TOKEN` | **GitHub Actions** |
| Run Qodana analysis (optional) | `QODANA_TOKEN` | **GitHub Actions** |
| Local development and testing | None | **None** |
