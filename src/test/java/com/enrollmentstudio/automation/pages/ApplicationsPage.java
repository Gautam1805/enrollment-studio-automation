package com.enrollmentstudio.automation.pages;

import com.enrollmentstudio.automation.pages.components.SideNav;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/** The landing page after sign-in: the list of enrollment applications. */
public class ApplicationsPage extends BasePage {

    public static final String PATH = "/";

    private static final By TOTAL = testId("summary-total");
    private static final By TABLE = By.cssSelector("main table");
    private static final By DRAFT_SUMMARY = testId("summary-draft");
    private static final By SEARCH = testId("csr-search-query");

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

    /** Narrows the list by first name, last name or member id. */
    public ApplicationsPage search(String term) {
        type(SEARCH, term);
        return this;
    }

    /** Applications with status 'Draft', counted under the 'In progress' summary. */
    public int inProgressCount() {
        return Integer.parseInt(text(DRAFT_SUMMARY));
    }

    public String draftName(String draftId) {
        return text(testId("member-draft-" + draftId));
    }

    /** Status badge of the draft's row, e.g. 'Draft'. */
    public String draftStatus(String draftId) {
        WebElement badge = row(draftId).findElements(By.tagName("td")).get(3)
                .findElement(By.cssSelector("span[data-slot='badge']"));
        return badge.getText().trim();
    }

    /** The step the draft has reached, e.g. 'At Address'. */
    public String draftResumeStep(String draftId) {
        return text(testId("resume-step-" + draftId));
    }

    /** Enrollment channel of the draft, e.g. 'Self-service' or 'CSR-assisted'. */
    public String draftChannel(String draftId) {
        return row(draftId).findElements(By.tagName("td")).get(2).getText().trim();
    }

    private WebElement row(String draftId) {
        return visible(By.cssSelector("tr:has([data-testid='member-draft-" + draftId + "'])"));
    }
}
