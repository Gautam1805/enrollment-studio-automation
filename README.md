# Member Enrollment Studio – UI automation

Selenium 4 + Java 17 + TestNG + Maven, page object model.

## Layout

```
src/test/java/com/enrollmentstudio/automation/
  base/        BaseTest – a fresh browser per test, signIn helper
  config/      Config – reads config.properties, -D overrides, credentials from env
  driver/      DriverFactory – Chrome, headless switch, one driver per thread
  listeners/   ScreenshotOnFailureListener – target/screenshots on failure
  pages/       Page objects (BasePage holds waits and helpers)
    components/  Pieces shared by pages, e.g. SideNav
  tests/       Test classes
src/test/resources/config.properties   base URL, browser, waits
testng.xml                              suite definition
```

## Conventions

- Locate elements by `data-testid` (`BasePage.testId("...")`) when the app has one; otherwise a short CSS selector.
- Page objects expose actions and facts (`signIn`, `heading()`); assertions live in tests.
- Explicit waits only (`BasePage.visible/click`), no `Thread.sleep`, no implicit waits.
- One behaviour per test method, with a `description`.
- Test data that a test creates is named with a `AUTO-` prefix so it is easy to find and remove.

## Running

Credentials are never committed. Set them in the environment:

```bash
export ENROLL_USERNAME=...   # PowerShell: $env:ENROLL_USERNAME="..."
export ENROLL_PASSWORD=...
mvn test                      # whole suite
mvn test -Dtest=LoginTest     # one class
mvn test -Dheadless=true      # no browser window (HEADLESS=true env var also works)
```

Other settings (`baseUrl`, `explicitWaitSeconds`) can be overridden with `-D<name>=...`.
