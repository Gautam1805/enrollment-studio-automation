package com.enrollmentstudio.automation.pages;

import com.enrollmentstudio.automation.config.Config;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Step 1 of the enrollment wizard, Applicant Profile. Serves both the unsaved
 * flow (/enroll/new/profile, opened by 'New enrollment' in the sidebar) and
 * step 1 of a saved draft (/enroll/{draftId}/profile).
 */
public class ProfileStepPage extends BasePage {

    public static final String NEW_PATH = "/enroll/new/profile";

    /** The fields the step asks for; each one underlies input-<field>, label-<field> and error-<field>. */
    public static final List<String> REQUIRED_FIELDS = List.of(
            "first-name", "last-name", "date-of-birth", "ssn-full-to-set-update", "email", "phone",
            "address-line-1", "city", "state", "zip-code");

    private static final Pattern SAVED_PATH = Pattern.compile("/enroll/[0-9a-f-]{36}/profile");

    private static final By SECTION_TITLE = By.cssSelector("#profile-title");
    private static final By CSR_CHECKBOX = testId("csr-assisted-toggle");
    private static final By DATE_CALENDAR = testId("calendar-date-of-birth");
    private static final By DATE_PICKER = testId("picker-date-of-birth");
    private static final By SSN = testId("input-ssn-full-to-set-update");
    private static final By SSN_TOGGLE = testId("toggle-ssn-full-to-set-update");
    private static final By CLEAR = testId("member-clear");
    private static final By SAVE = testId("member-save");
    private static final By DELETE_DRAFT = testId("member-delete");
    private static final By CHANNEL_BADGE = testId("member-channel");
    private static final By BANNER_BADGE = By.cssSelector("main h1 ~ span[data-slot='badge']");

    public ProfileStepPage(WebDriver driver) {
        super(driver);
    }

    /** The unsaved flow as opened by 'New enrollment'. */
    public ProfileStepPage waitUntilLoaded() {
        waitForPath(NEW_PATH);
        visible(SECTION_TITLE);
        return this;
    }

    /** Step 1 of an already saved draft. */
    public ProfileStepPage waitUntilSaved(String draftId) {
        waitForPath("/enroll/" + draftId + "/profile");
        visible(SECTION_TITLE);
        return this;
    }

    /** Opens step 1 of a saved draft by id, e.g. to clean a test draft up. */
    public ProfileStepPage openSavedDraft(String draftId) {
        driver.get(Config.baseUrl() + "/enroll/" + draftId + "/profile");
        return waitUntilSaved(draftId);
    }

    private ProfileStepPage typeInto(String field, String value) {
        type(testId("input-" + field), value);
        return this;
    }

    public ProfileStepPage firstName(String value) { return typeInto("first-name", value); }

    public ProfileStepPage lastName(String value) { return typeInto("last-name", value); }

    public ProfileStepPage dateOfBirth(String value) { return typeInto("date-of-birth", value); }

    public ProfileStepPage ssn(String value) { return typeInto("ssn-full-to-set-update", value); }

    public ProfileStepPage email(String value) { return typeInto("email", value); }

    public ProfileStepPage phone(String value) { return typeInto("phone", value); }

    public ProfileStepPage addressLine1(String value) { return typeInto("address-line-1", value); }

    public ProfileStepPage addressLine2(String value) { return typeInto("address-line-2", value); }

    public ProfileStepPage city(String value) { return typeInto("city", value); }

    /** The app records the state as its two-letter code, e.g. OR for Oregon. */
    public ProfileStepPage state(String value) { return typeInto("state", value); }

    public ProfileStepPage zipCode(String value) { return typeInto("zip-code", value); }

    public ProfileStepPage tickCsrAssisted() {
        click(CSR_CHECKBOX);
        return this;
    }

    /** Reveals the masked SSN via its 'Show' control. */
    public ProfileStepPage revealSsn() {
        click(SSN_TOGGLE);
        return this;
    }

    /**
     * Opens the calendar for Date of Birth and records the picked date. The calendar
     * popup itself is the browser's native widget (outside the page DOM); the app wires
     * it to a hidden date field behind the calendar button, which a pick writes into.
     */
    public ProfileStepPage pickDateFromCalendar(String isoDate) {
        click(DATE_CALENDAR);
        ((JavascriptExecutor) driver).executeScript(
                "var picker = arguments[0], iso = arguments[1];"
                        + "var set = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;"
                        + "set.call(picker, iso);"
                        + "picker.dispatchEvent(new Event('input', {bubbles: true}));"
                        + "picker.dispatchEvent(new Event('change', {bubbles: true}));",
                driver.findElement(DATE_PICKER), isoDate);
        wait.until(d -> isoDate.equals(valueOf("date-of-birth")));
        return this;
    }

    public ProfileStepPage clearForm() {
        click(CLEAR);
        return this;
    }

    public void clickSaveAndContinue() {
        click(SAVE);
    }

    /** Deletes the draft (from step 1 of a saved draft) and returns to Applications. */
    public ApplicationsPage deleteDraft() {
        click(DELETE_DRAFT);
        return new ApplicationsPage(driver).waitUntilLoaded();
    }

    /** The card's title, e.g. 'Applicant Profile'. */
    public String sectionTitle() {
        return text(SECTION_TITLE);
    }

    /** The page's status badge: 'Not saved yet' on an unsaved flow, 'Draft' once saved. */
    public String bannerBadge() {
        return text(BANNER_BADGE);
    }

    /**
     * Text like 'Step 1 of 7', as the app authors it. The indicator only renders
     * uppercase via CSS (getText() would return 'STEP 1 OF 7'), so the lookup
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

    /** Input ids (input- prefix removed) that have no label on this step. */
    public List<String> missingProfileFields() {
        List<String> missing = new ArrayList<>();
        for (String field : REQUIRED_FIELDS) {
            if (!isPresent(testId("label-" + field))) {
                missing.add(field);
            }
        }
        return missing;
    }

    public String valueOf(String field) {
        return String.valueOf(((JavascriptExecutor) driver).executeScript(
                "return arguments[0].value;", driver.findElement(testId("input-" + field))));
    }

    /** Current validation text of the field, e.g. 'First name is required'. */
    public String errorText(String field) {
        return visible(testId("error-" + field)).getText().trim();
    }

    public boolean hasError(String field) {
        return isPresent(testId("error-" + field));
    }

    /** Input ids (input- prefix removed) currently showing a validation error. */
    public List<String> fieldsWithError() {
        List<String> fields = new ArrayList<>();
        for (WebElement error : driver.findElements(By.cssSelector("[data-testid^='error-']"))) {
            fields.add(error.getAttribute("data-testid").replaceFirst("^error-", ""));
        }
        return fields;
    }

    /** Input ids (input- prefix removed) currently marked invalid. */
    public List<String> invalidFields() {
        List<String> fields = new ArrayList<>();
        for (WebElement input : driver.findElements(By.cssSelector("[data-testid^='input-'][aria-invalid='true']"))) {
            fields.add(input.getAttribute("data-testid").replaceFirst("^input-", ""));
        }
        return fields;
    }

    public boolean csrAssistedTicked() {
        return "true".equals(visible(CSR_CHECKBOX).getAttribute("aria-checked"));
    }

    public boolean showsCsrCheckbox() {
        return isPresent(CSR_CHECKBOX);
    }

    /** Label of the reveal control: 'Show' while the SSN is masked, 'Hide' once revealed. */
    public String ssnRevealLabel() {
        return text(SSN_TOGGLE);
    }

    /** How the SSN is rendered: 'disc' for masked bullets, 'none' for readable digits. */
    public String ssnMasking() {
        return String.valueOf(((JavascriptExecutor) driver).executeScript(
                "return getComputedStyle(arguments[0]).webkitTextSecurity;",
                driver.findElement(SSN)));
    }

    /** The saved draft's recorded enrollment channel, e.g. 'CSR-assisted'. Saved drafts only. */
    public String channelBadge() {
        return text(CHANNEL_BADGE);
    }
}
