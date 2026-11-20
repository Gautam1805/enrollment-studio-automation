package com.enrollmentstudio.automation.pages.components;

import com.enrollmentstudio.automation.pages.BasePage;
import com.enrollmentstudio.automation.pages.LoginPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/** The sidebar shown on every signed-in page. */
public class SideNav extends BasePage {

    /** Sidebar entries: the data-testid suffix and the path each one opens. */
    public enum Item {
        APPLICATIONS("nav-applications", "/"),
        REVIEW_QUEUE("nav-review-queue", "/review-queue"),
        POSSIBLE_DUPLICATES("nav-duplicates", "/duplicates"),
        AUDIT_LOG("nav-audit-log", "/audit-log"),
        DELETION_REQUESTS("nav-deletion-requests", "/deletion-requests"),
        STAFF_ACCESS("nav-staff", "/staff");

        private final String testId;
        private final String path;

        Item(String testId, String path) {
            this.testId = testId;
            this.path = path;
        }

        public String path() {
            return path;
        }
    }

    private static final By SESSION_EMAIL = testId("session-email");
    private static final By SIGN_OUT = testId("sign-out");
    private static final By NEW_ENROLLMENT = testId("member-new");

    public SideNav(WebDriver driver) {
        super(driver);
    }

    public void open(Item item) {
        click(testId(item.testId));
        waitForPath(item.path);
    }

    public String signedInEmail() {
        return visible(SESSION_EMAIL).getText().trim();
    }

    public void startNewEnrollment() {
        click(NEW_ENROLLMENT);
    }

    public LoginPage signOut() {
        click(SIGN_OUT);
        return new LoginPage(driver).waitUntilShown();
    }
}
