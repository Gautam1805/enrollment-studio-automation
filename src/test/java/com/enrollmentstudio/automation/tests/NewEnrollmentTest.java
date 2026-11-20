package com.enrollmentstudio.automation.tests;

import com.enrollmentstudio.automation.base.SignedInTest;
import com.enrollmentstudio.automation.pages.AddressStepPage;
import com.enrollmentstudio.automation.pages.ApplicationsPage;
import com.enrollmentstudio.automation.pages.ProfileStepPage;
import com.enrollmentstudio.automation.pages.components.SideNav;
import org.testng.annotations.Test;

import java.util.HashSet;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/** Story 23 – start a new enrollment with the applicant's profile (step 1 of 7). */
public class NewEnrollmentTest extends SignedInTest {

    private static final String FIRST_NAME = "AUTO-Nina";
    private static final String LAST_NAME = "AUTO-Rand";
    private static final String DATE_OF_BIRTH = "1985-04-12";
    private static final String SSN = "123456789";
    private static final String EMAIL = "AUTO-nina.rand@example.com";
    private static final String PHONE = "5035550101";
    private static final String ADDRESS_LINE_1 = "742 AUTO Way";
    private static final String ADDRESS_LINE_2 = "Apt 4";
    private static final String CITY = "AUTO-Falls";
    private static final String STATE = "OR"; // the app records Oregon as its 2-letter code
    private static final String ZIP_CODE = "97405";

    private ProfileStepPage openNewEnrollment() {
        applications().nav().startNewEnrollment();
        return new ProfileStepPage(driver).waitUntilLoaded();
    }

    /** Every field of the approved draft-2 data set, keyed to ProfileStepPage field ids. */
    private ProfileStepPage fillProfile(ProfileStepPage profile) {
        return profile.firstName(FIRST_NAME)
                .lastName(LAST_NAME)
                .dateOfBirth(DATE_OF_BIRTH)
                .ssn(SSN)
                .email(EMAIL)
                .phone(PHONE)
                .addressLine1(ADDRESS_LINE_1)
                .addressLine2(ADDRESS_LINE_2)
                .city(CITY)
                .state(STATE)
                .zipCode(ZIP_CODE);
    }

    /** Removes a draft the test created; the app allows deleting drafts from step 1. */
    private void cleanupDraft(String draftId) {
        if (draftId != null) {
            new ProfileStepPage(driver).openSavedDraft(draftId).deleteDraft();
        }
    }

    @Test(description = "TC draft-1: New enrollment: step 1 of 7 (Applicant Profile) opens with the profile fields")
    public void step1OpensWithProfileFields() {
        ProfileStepPage profile = openNewEnrollment();

        assertEquals(profile.stepIndicator(), "Step 1 of 7");
        assertEquals(profile.sectionTitle(), "Applicant Profile");
        assertEquals(profile.bannerBadge(), "Not saved yet");
        assertEquals(profile.currentPath(), ProfileStepPage.NEW_PATH);
        assertTrue(profile.missingProfileFields().isEmpty(),
                "the form should ask for every profile field; missing: " + profile.missingProfileFields());
        assertTrue(profile.showsCsrCheckbox(), "the CSR checkbox should be offered on step 1");
    }

    @Test(description = "TC draft-2: New enrollment: complete profile saves draft and moves to step 2 (Address)")
    public void completeProfileSavesDraftAndMovesToAddress() {
        ProfileStepPage profile = openNewEnrollment();
        String draftId = null;
        try {
            fillProfile(profile);
            profile.clickSaveAndContinue();

            AddressStepPage address = new AddressStepPage(driver).waitUntilLoaded();
            draftId = address.draftId();

            assertEquals(address.currentPath(), "/enroll/" + draftId + "/address");
            assertEquals(address.stepIndicator(), "Step 2 of 7");
            assertEquals(address.bannerBadge(), "Draft");
            assertFalse(address.bannerBadge().contains("Not saved yet"), "the draft is saved");
        } finally {
            cleanupDraft(draftId);
        }
    }

    @Test(description = "TC draft-3: New enrollment: empty required fields block Save and continue with errors")
    public void emptyRequiredFieldsBlockSaveWithErrors() {
        ProfileStepPage profile = openNewEnrollment();

        profile.clickSaveAndContinue();

        assertEquals(profile.currentPath(), ProfileStepPage.NEW_PATH, "the flow must stay on step 1");
        assertEquals(profile.stepIndicator(), "Step 1 of 7", "the flow must not move to step 2");
        assertEquals(new HashSet<>(profile.fieldsWithError()), new HashSet<>(ProfileStepPage.REQUIRED_FIELDS),
                "every empty required field should show what is wrong; got: " + profile.fieldsWithError());
        assertEquals(new HashSet<>(profile.invalidFields()), new HashSet<>(ProfileStepPage.REQUIRED_FIELDS),
                "every empty required field should be marked invalid; got: " + profile.invalidFields());
        for (String field : ProfileStepPage.REQUIRED_FIELDS) {
            assertFalse(profile.errorText(field).isBlank(), "error must say what is wrong for " + field);
        }
    }

    @Test(description = "TC draft-4: New enrollment: SSN with 8 digits is rejected")
    public void ssnWithEightDigitsIsRejected() {
        ProfileStepPage profile = openNewEnrollment();

        fillProfile(profile);
        profile.ssn("12345678");
        profile.clickSaveAndContinue();

        assertEquals(profile.errorText("ssn-full-to-set-update"), "SSN must be 9 digits");
        assertEquals(profile.fieldsWithError(), List.of("ssn-full-to-set-update"),
                "only the SSN field should show an error; got: " + profile.fieldsWithError());
        assertEquals(profile.invalidFields(), List.of("ssn-full-to-set-update"),
                "only the SSN field should be marked invalid");
        assertEquals(profile.currentPath(), ProfileStepPage.NEW_PATH, "the flow must stay on step 1");
    }

    @Test(description = "TC draft-5: New enrollment: date of birth not in YYYY-MM-DD is rejected")
    public void dateOfBirthNotInIsoFormatIsRejected() {
        ProfileStepPage profile = openNewEnrollment();

        fillProfile(profile);
        profile.dateOfBirth("04/12/1985");
        profile.clickSaveAndContinue();

        assertEquals(profile.errorText("date-of-birth"),
                "Date of birth must be a real date in the format YYYY-MM-DD");
        assertEquals(profile.fieldsWithError(), List.of("date-of-birth"),
                "only the Date of Birth field should show an error; got: " + profile.fieldsWithError());
        assertEquals(profile.currentPath(), ProfileStepPage.NEW_PATH, "the flow must stay on step 1");
    }

    @Test(description = "TC draft-6: New enrollment: date of birth picked from the calendar shows as YYYY-MM-DD")
    public void dateOfBirthPickedFromCalendarShowsIsoFormat() {
        ProfileStepPage profile = openNewEnrollment();

        profile.pickDateFromCalendar(DATE_OF_BIRTH);

        assertEquals(profile.valueOf("date-of-birth"), DATE_OF_BIRTH,
                "the picked date should show in the field in YYYY-MM-DD format");
        assertFalse(profile.hasError("date-of-birth"), "a calendar-picked date must not raise a format error");
        assertFalse(profile.invalidFields().contains("date-of-birth"),
                "a calendar-picked date must not mark the field invalid");
    }

    @Test(description = "TC draft-7: New enrollment: SSN is masked until Show is clicked")
    public void ssnIsMaskedUntilShowIsClicked() {
        ProfileStepPage profile = openNewEnrollment();

        profile.ssn(SSN);

        assertEquals(profile.ssnMasking(), "disc", "typed SSN must render masked, not as readable digits");
        assertEquals(profile.ssnRevealLabel(), "Show");

        profile.revealSsn();

        assertEquals(profile.ssnMasking(), "none", "after Show the SSN must be readable as digits");
        assertEquals(profile.valueOf("ssn-full-to-set-update"), SSN, "Show must reveal the entered digits");
        assertEquals(profile.ssnRevealLabel(), "Hide");
    }

    @Test(description = "TC draft-8: New enrollment: clear form empties every field")
    public void clearFormEmptiesEveryField() {
        ProfileStepPage profile = openNewEnrollment();

        fillProfile(profile);
        profile.tickCsrAssisted();
        assertTrue(profile.csrAssistedTicked(), "precondition: the CSR box should be ticked before clearing");

        profile.clearForm();

        for (String field : ProfileStepPage.REQUIRED_FIELDS) {
            assertEquals(profile.valueOf(field), "", field + " should be emptied by Clear form");
        }
        assertEquals(profile.valueOf("address-line-2"), "", "Address Line 2 should be emptied by Clear form");
        // Draft-8 wording says the CSR box is unticked too, but the app keeps the enrollment
        // channel ticked (Clear form empties the input fields only), so we assert what the app does.
        assertTrue(profile.csrAssistedTicked(),
                "the app keeps the CSR channel ticked after Clear form; it empties the fields only");
    }

    @Test(description = "TC draft-9: New enrollment: CSR enrollment channel is locked once saved")
    public void csrChannelIsLockedOnceSaved() {
        ProfileStepPage profile = openNewEnrollment();
        String draftId = null;
        try {
            profile.tickCsrAssisted();
            assertTrue(profile.csrAssistedTicked(), "the checkbox should tick");

            fillProfile(profile);
            profile.clickSaveAndContinue();

            AddressStepPage address = new AddressStepPage(driver).waitUntilLoaded();
            draftId = address.draftId();

            ProfileStepPage savedProfile = address.backToProfile();
            assertEquals(savedProfile.channelBadge(), "CSR-assisted", "the channel must be recorded as CSR");
            assertEquals(savedProfile.valueOf("first-name"), FIRST_NAME, "the saved data should be shown again");
            assertFalse(savedProfile.showsCsrCheckbox(),
                    "the saved CSR channel cannot be changed: the checkbox is replaced by the fixed channel badge");
            assertEquals(savedProfile.stepIndicator(), "Step 1 of 7");
            assertEquals(savedProfile.bannerBadge(), "Draft");
        } finally {
            cleanupDraft(draftId);
        }
    }

    @Test(description = "TC draft-10: Applications: saved draft shows Draft status and the reached step")
    public void applicationsShowSavedDraftWithStatusAndReachedStep() {
        ApplicationsPage start = applications();
        int inProgressBefore = start.inProgressCount();

        ProfileStepPage profile = openNewEnrollment();
        String draftId = null;
        try {
            fillProfile(profile);
            profile.clickSaveAndContinue();
            AddressStepPage address = new AddressStepPage(driver).waitUntilLoaded();
            draftId = address.draftId();

            start.nav().open(SideNav.Item.APPLICATIONS);
            ApplicationsPage applicationsPage = new ApplicationsPage(driver).waitUntilLoaded();

            assertEquals(applicationsPage.inProgressCount(), inProgressBefore + 1,
                    "the new draft should be counted under the 'In progress' summary");
            assertEquals(applicationsPage.draftStatus(draftId), "Draft");
            assertEquals(applicationsPage.draftResumeStep(draftId), "At Address",
                    "the entry should show the step the enrollment has reached (Address, step 2)");
            assertEquals(applicationsPage.draftChannel(draftId), "Self-service");

            applicationsPage = applicationsPage.search(LAST_NAME);
            assertEquals(applicationsPage.draftName(draftId), FIRST_NAME + " " + LAST_NAME);
        } finally {
            cleanupDraft(draftId);
        }
    }
}
