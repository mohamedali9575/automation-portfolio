# Junior Automation Portfolio (Java + Maven + TestNG)

3 small automation projects, junior level. Each runs with `mvn test` inside its folder.

| # | Project | What it shows |
|---|---------|---------------|
| 1 | `01-login-ui-tests` | Playwright UI tests, data-driven logins (valid + invalid) on saucedemo.com |
| 2 | `02-rest-api-tests` | REST API tests with JDK HttpClient (GET + POST) on jsonplaceholder.typicode.com |
| 3 | `03-hunger-e2e-simulator` | Report-logic unit tests + nested-dropdown UI test (same expand-then-click pattern as a Hunger Station sidebar) |

## Run everything (from this folder, needs Maven + Java 17)

```powershell
cd 01-login-ui-tests; mvn test; cd ..
cd 02-rest-api-tests; mvn test; cd ..
cd 03-hunger-e2e-simulator; mvn test; cd ..
```

First time only (Playwright browsers for projects 1 and 3):

```powershell
cd 01-login-ui-tests; mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install chromium"; cd ..
```
