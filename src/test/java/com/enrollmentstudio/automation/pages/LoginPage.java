package com.enrollmentstudio.automation.pages;

import com.enrollmentstudio.automation.config.Config;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class LoginPage extends BasePage {

    public static final String PATH = "/login";

    /** Signing in is the app's slowest step; it gets its own, longer wait. */
    private static final Duration SIGN_IN_WAIT = Duration.ofSeconds(45);

    private static final By EMAIL = testId("login-email");
    private static final By PASSWORD = testId("login-password");
    private static final By SUBMIT = testId("login-submit");
    private static final By FIRST_TIME = testId("login-first-time");
    private static final By FORGOT_PASSWORD = testId("login-forgot-password");

    public LoginPage(WebDriver driver) {
        super(driver);
    }

    public LoginPage open() {
        driver.get(Config.baseUrl() + PATH);
        return waitUntilShown();
    }

    public LoginPage waitUntilShown() {
        waitForPath(PATH);
        visible(EMAIL);
        return this;
    }

    /** Signs the browser out by dropping cookies and storage, then reloads sign-in. */
    public LoginPage clearSession() {
        driver.manage().deleteAllCookies();
        ((JavascriptExecutor) driver).executeScript("window.localStorage.clear(); window.sessionStorage.clear();");
        return open();
    }

    public ApplicationsPage signIn(String email, String password) {
        type(EMAIL, email);
        type(PASSWORD, password);
        click(SUBMIT);
        new WebDriverWait(driver, SIGN_IN_WAIT).until(d -> currentPath().equals(ApplicationsPage.PATH));
        return new ApplicationsPage(driver).waitUntilLoaded();
    }

    public ApplicationsPage signInAsStaff() {
        return signIn(Config.username(), Config.password());
    }

    public boolean isDisplayed() {
        return currentPath().equals(PATH) && isPresent(EMAIL) && isPresent(PASSWORD);
    }

    public boolean offersFirstTimeSignIn() {
        return isPresent(FIRST_TIME);
    }

    public boolean offersPasswordReset() {
        return isPresent(FORGOT_PASSWORD);
    }
}
