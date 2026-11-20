package com.enrollmentstudio.automation.pages;

import com.enrollmentstudio.automation.pages.components.SideNav;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/** The landing page after sign-in: the list of enrollment applications. */
public class ApplicationsPage extends BasePage {

    public static final String PATH = "/";

    private static final By TOTAL = testId("summary-total");
    private static final By TABLE = By.cssSelector("main table");

    public ApplicationsPage(WebDriver driver) {
        super(driver);
    }

    public ApplicationsPage waitUntilLoaded() {
        waitForPath(PATH);
        visible(TOTAL);
        return this;
    }

    public SideNav nav() {
        return new SideNav(driver);
    }

    public boolean showsApplicationsTable() {
        return isPresent(TABLE);
    }
}
