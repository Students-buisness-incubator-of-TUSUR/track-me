# Checkstyle Configuration

This project uses Checkstyle to enforce code quality and style standards.

## Configuration

Checkstyle is configured at the root level and applied to all subprojects.

### Configuration Files

- **`config/checkstyle/checkstyle.xml`**: Main Checkstyle configuration file with rules based on industry best practices
- **`config/checkstyle/suppressions.xml`**: Suppressions file to exclude generated code and certain patterns from checks

### Suppressions

The following are automatically suppressed from Checkstyle:
- Generated code (in `generated/`, `build/`, `target/` directories)
- Test resources
- Configuration classes (`*Configuration.java`)
- Application classes (`*Application.java`)


## Running Checkstyle

### Run for all projects
```bash
./gradlew checkstyleMain checkstyleTest
```

### Run for a specific module
```bash
./gradlew :meeting-service:checkstyleMain
./gradlew :backend:checkstyleMain
```

### Reports

Checkstyle reports are generated in:
- XML: `build/reports/checkstyle/main.xml`
- HTML: `build/reports/checkstyle/main.html`

## Integration with SonarQube

Checkstyle reports are automatically integrated with SonarQube analysis. The reports are configured in each module's `build.gradle`:

```groovy
property "sonar.java.checkstyle.reportPaths", "build/reports/checkstyle/main.xml,build/reports/checkstyle/test.xml"
```

### Running SonarQube with Checkstyle

1. Run Checkstyle first to generate reports:
   ```bash
   ./gradlew checkstyleMain checkstyleTest
   ```

2. Run SonarQube analysis:
   ```bash
   ./gradlew sonar -Dsonar.token=YOUR_TOKEN
   ```

Or combine both:
```bash
./gradlew clean build checkstyleMain checkstyleTest sonar -Dsonar.token=YOUR_TOKEN
```

## Customization

To modify Checkstyle rules, edit `config/checkstyle/checkstyle.xml`.

To add suppressions, edit `config/checkstyle/suppressions.xml`.

## CI/CD Integration

Add Checkstyle to your CI/CD pipeline:

```yaml
# Example for GitHub Actions
- name: Run Checkstyle
  run: ./gradlew checkstyleMain checkstyleTest

- name: Upload Checkstyle Reports
  uses: actions/upload-artifact@v3
  with:
    name: checkstyle-reports
    path: '**/build/reports/checkstyle/**'
```

## Fixing Issues

When Checkstyle reports issues:

1. Review the HTML report for details
2. Fix the code style issues
3. Re-run Checkstyle to verify fixes
4. If a rule needs to be suppressed for a specific case, add it to `suppressions.xml`

## Best Practices

- Run Checkstyle locally before committing
- Keep `ignoreFailures = false` to enforce quality
- Review and address all warnings
- Update suppressions judiciously - don't suppress real issues

