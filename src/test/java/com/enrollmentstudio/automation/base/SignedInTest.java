package com.enrollmentstudio.automation.base;

import com.enrollmentstudio.automation.config.Config;
import com.enrollmentstudio.automation.pages.ApplicationsPage;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

/** Signs in once for the class; every test then starts on Applications. */
public abstract class SignedInTest extends BaseTest {

    @BeforeClass(alwaysRun = true, dependsOnMethods = "startBrowser")
    public void signInOnce() {
        signInAsStaff();
    }

    @BeforeMethod(alwaysRun = true)
    public void startOnApplications() {
        driver.get(Config.baseUrl() + ApplicationsPage.PATH);
        new ApplicationsPage(driver).waitUntilLoaded();
    }

    protected ApplicationsPage applications() {
        return new ApplicationsPage(driver);
    }
}
