package com.enrollmentstudio.automation.tests;

import com.enrollmentstudio.automation.base.BaseTest;
import com.enrollmentstudio.automation.config.Config;
import com.enrollmentstudio.automation.pages.ApplicationsPage;
import com.enrollmentstudio.automation.pages.LoginPage;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/** Sign-in and sign-out. Each test starts signed out. */
public class LoginTest extends BaseTest {

    @BeforeMethod(alwaysRun = true)
    public void startSignedOut() {
        new LoginPage(driver).open().clearSession();
    }

    @Test(description = "The sign-in page offers first-time sign-in and password reset")
    public void signInPageShowsHelpLinks() {
        LoginPage login = new LoginPage(driver).open();

        assertTrue(login.isDisplayed(), "sign-in form should be shown");
        assertTrue(login.offersFirstTimeSignIn(), "first-time sign-in link should be shown");
        assertTrue(login.offersPasswordReset(), "forgot-password link should be shown");
    }

    @Test(description = "Signed-out visitors are sent to sign in")
    public void protectedPageRedirectsToSignIn() {
        driver.get(Config.baseUrl() + "/review-queue");
        LoginPage login = new LoginPage(driver).waitUntilShown();

        assertTrue(login.isDisplayed(), "visiting a staff page signed out should show sign-in");
    }

    @Test(description = "Staff sign in, land on Applications signed in as themselves, and can sign out")
    public void staffCanSignInAndOut() {
        ApplicationsPage applications = signInAsStaff();

        assertEquals(applications.heading(), "Applications");
        assertTrue(applications.showsApplicationsTable(), "applications table should be shown");
        assertEquals(applications.nav().signedInEmail().toLowerCase(), Config.username().toLowerCase());

        LoginPage login = applications.nav().signOut();

        assertTrue(login.isDisplayed(), "sign-in form should be shown after signing out");
    }
}
