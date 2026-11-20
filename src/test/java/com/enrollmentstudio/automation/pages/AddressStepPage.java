package com.enrollmentstudio.automation.pages;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import java.util.regex.Pattern;

/**
 * Step 2 of the enrollment wizard, Address, shown after a draft is saved.
 * Reached at the path /enroll/{draftId}/address.
 */
public class AddressStepPage extends BasePage {

    private static final Pattern PATH_TEMPLATE = Pattern.compile("/enroll/[0-9a-f-]{36}/address");
    private static final By BANNER_BADGE = By.cssSelector("main h1 ~ span[data-slot='badge']");
    private static final By STEP_BACK = testId("step-back");

    public AddressStepPage(WebDriver driver) {
        super(driver);
    }

    public AddressStepPage waitUntilLoaded() {
        wait.until(d -> PATH_TEMPLATE.matcher(currentPath()).matches());
        visible(STEP_BACK);
        return this;
    }

    /** The saved draft's id, taken from the path. */
    public String draftId() {
        return currentPath().split("/")[2];
    }

    /** Text of the page's status badge, e.g. 'Draft' once the flow is saved. */
    public String bannerBadge() {
        return text(BANNER_BADGE);
    }

    /**
     * Text like 'Step 2 of 7', as the app authors it. The indicator only renders
     * uppercase via CSS (getText() would return 'STEP 2 OF 7'), so the lookup
     * reads the DOM text and matches it case-insensitively; the shortest matching
     * text is the indicator itself, not a container that merely contains it.
     */
    public String stepIndicator() {
        return wait.until(d -> {
            Object best = ((JavascriptExecutor) d).executeScript(
                    "var best = null;"
                            + "for (var element of document.querySelectorAll('main *')) {"
                            + "  var candidate = (element.textContent || '').trim();"
                            + "  if (/step\\s*\\d\\s*of\\s*\\d/i.test(candidate)"
                            + "      && (best === null || candidate.length < best.length)) best = candidate;"
                            + "}"
                            + "return best;");
            return best instanceof String ? (String) best : null;
        });
    }

    /** Returns to step 1 (Applicant Profile) of the same draft. */
    public ProfileStepPage backToProfile() {
        String draftId = draftId();
        click(STEP_BACK);
        return new ProfileStepPage(driver).waitUntilSaved(draftId);
    }
}
