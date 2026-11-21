package com.enrollmentstudio.automation.base;

import com.enrollmentstudio.automation.driver.DriverFactory;
import com.enrollmentstudio.automation.listeners.ScreenshotOnFailureListener;
import com.enrollmentstudio.automation.pages.ApplicationsPage;
import com.enrollmentstudio.automation.pages.LoginPage;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;

/**
 * One browser per test class, closed after the class. Signing in is the
 * slowest step in the app, so classes that need a signed-in user extend
 * {@link SignedInTest}, which signs in once and reuses the session.
 *
 * The failure-screenshot listener is registered here rather than only in
 * testng.xml, so it also runs when tests are picked with -Dtest=... (which
 * bypasses the suite file).
 */
@Listeners(ScreenshotOnFailureListener.class)
public abstract class BaseTest {

    protected WebDriver driver;

    @BeforeClass(alwaysRun = true)
    public void startBrowser() {
        driver = DriverFactory.start();
    }

    @AfterClass(alwaysRun = true)
    public void closeBrowser() {
        DriverFactory.quit();
    }

    /** Signs in with the staff test account and lands on Applications. */
    protected ApplicationsPage signInAsStaff() {
        return new LoginPage(driver).open().signInAsStaff();
    }
}
