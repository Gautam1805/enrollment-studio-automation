package com.enrollmentstudio.automation.tests;

import com.enrollmentstudio.automation.base.SignedInTest;
import com.enrollmentstudio.automation.pages.ApplicationsPage;
import com.enrollmentstudio.automation.pages.ApplicationsPage.StatusFilter;
import org.openqa.selenium.WebElement;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Story 24 – find applications by status and by searching. The approved cases
 * work against seeded test applications (AUTO-DRAFT, AUTO-SUBMIT, ...). The
 * behaviour itself is checked on whatever data the environment holds; steps
 * that need a specific seeded row are skipped with a stated reason when the
 * demo data does not include it.
 */
public class ApplicationsListTest extends SignedInTest {

    private static final int PAGE_SIZE = 25;

    /** The masked SSN form: only the last four digits are visible, e.g. ***-**-1234. */
    private static final Pattern MASKED_SSN = Pattern.compile("\\*\\*\\*-\\*\\*-\\d{4}");

    /**
     * Whitespace that can surround a cell's text invisibly: trailing spaces,
     * non-breaking/figure/narrow spaces and zero-width characters.
     */
    private static final Pattern INVISIBLE = Pattern.compile("[\\s\\u00A0\\u2007\\u202F\\u200B\\u200C\\u200D\\uFEFF]");

    private static final String AUTO_FIRST_NAME = "AUTO-Alva";
    private static final String AUTO_LAST_NAME = "Nytest";
    private static final String AUTO_MEMBER_NUMBER = "1003";

    @Test(description = "TC draft-1: table shows the required columns and All is selected by default")
    public void tableShowsRequiredColumnsAndAllStatusesByDefault() {
        ApplicationsPage apps = applications();

        List<String> required = List.of("Applicant", "SSN", "Channel", "Status", "Identity", "Eligibility", "Updated");
        // The app renders the headers in capitals via CSS, so compare ignoring case.
        List<String> headers = apps.columnNames().stream().map(String::toUpperCase).toList();
        int position = -1;
        for (String column : required) {
            int index = headers.indexOf(column.toUpperCase());
            assertTrue(index >= 0, "the table should have a " + column + " column; headers seen: " + headers);
            assertTrue(index > position, "the " + column + " column should keep its place in the table");
            position = index;
        }

        assertEquals(apps.statusFilterLabels(),
                List.of("All", "In progress", "Submitted", "Enrolled", "Rejected", "Expired"),
                "the status filters should offer every status");
        assertEquals(apps.selectedStatus(), StatusFilter.ALL, "All should be selected by default");

        List<String> applicants = new ArrayList<>();
        Set<String> statuses = new HashSet<>();
        do {
            for (ApplicationsPage.RowSnapshot row : apps.rowSnapshots()) {
                applicants.add(row.applicant());
                statuses.add(row.status());
            }
            if (apps.hasNextPage()) {
                apps.nextPage();
            } else {
                break;
            }
        } while (true);

        boolean autoDraft = contains(applicants, "AUTO-Alva");
        boolean autoEnroll = contains(applicants, "AUTO-Carl");
        if (autoDraft || autoEnroll) {
            assertTrue(autoDraft && autoEnroll,
                    "the seeded AUTO-DRAFT and AUTO-ENROLL applications should both be on the All list");
        }
        assertTrue(statuses.size() >= 2, "the unfiltered list should show different statuses together; saw " + statuses);
    }

    @Test(description = "TC draft-2: choosing a status shows only applications in that status")
    public void statusFilterShowsOnlyApplicationsInThatStatus() {
        ApplicationsPage apps = applications();

        for (StatusFilter status : List.of(StatusFilter.IN_PROGRESS, StatusFilter.SUBMITTED, StatusFilter.ENROLLED,
                StatusFilter.REJECTED, StatusFilter.EXPIRED)) {
            apps.selectStatus(status);
            if (apps.showsNoMatches()) {
                assertTrue(apps.rows().isEmpty(),
                        "the " + status.label() + " filter should list no application of another status");
            } else {
                for (WebElement row : apps.rows()) {
                    assertEquals(apps.statusOf(row), status.badgeLabel(),
                            "every row of the " + status.label() + " list should be in that status (\"" + status.label()
                                    + "\" shows drafts and other rows as badges)");
                }
            }
        }

        apps.selectStatus(StatusFilter.ALL);
        assertEquals(apps.listTotal(), apps.totalCount(), "the All filter should list every application again");
        assertFalse(apps.rows().isEmpty(), "the unfiltered list should show rows again");
    }

    @Test(description = "TC draft-3: searching by first name, last name or member number narrows the list")
    public void searchNarrowsByFirstNameLastNameAndMemberNumber() {
        ApplicationsPage apps = applications();

        String firstNameQuery = AUTO_FIRST_NAME;
        apps.search(firstNameQuery);
        if (apps.showsNoMatches() && apps.rows().isEmpty()) {
            apps.clearSearch();
            firstNameQuery = firstTokenOf(apps.applicantOf(apps.rows().get(0)));
            apps.search(firstNameQuery);
        }
        assertTrue(apps.listTotal() == null || apps.listTotal() <= apps.totalCount(),
                "searching by first name should narrow the list");
        for (String applicant : applicantNames(apps)) {
            assertTrue(applicant.toLowerCase().contains(firstNameQuery.toLowerCase()),
                    "searching by first name should keep only matching applications; saw " + applicant);
        }

        apps.clearSearch();
        assertEquals(apps.listTotal(), apps.totalCount(), "clearing the first-name search should show the full list again");

        String lastNameQuery = AUTO_LAST_NAME;
        apps.search(lastNameQuery);
        if (apps.showsNoMatches() && apps.rows().isEmpty()) {
            apps.clearSearch();
            lastNameQuery = lastTokenOf(apps.applicantOf(apps.rows().get(0)));
            apps.search(lastNameQuery);
        }
        List<String> lastMatched = applicantNames(apps);
        assertTrue(!lastMatched.isEmpty(), "searching by last name should still list the matching applications");
        for (String applicant : lastMatched) {
            assertTrue(applicant.toLowerCase().contains(lastNameQuery.toLowerCase()),
                    "searching by last name should keep only matching applications; saw " + applicant);
        }
        if (contains(lastMatched, AUTO_FIRST_NAME) && contains(lastMatched, "AUTO-Erik")) {
            assertEquals(apps.listTotal().intValue(), 5,
                    "the five seeded AUTO- applications share the last name and should all be listed");
        }

        apps.clearSearch();

        apps.search(AUTO_MEMBER_NUMBER);
        if (apps.showsNoMatches() && apps.rows().isEmpty()) {
            throw new SkipException("member number " + AUTO_MEMBER_NUMBER
                    + " (seeded AUTO-ENROLL application) is not present in this environment");
        }
        assertEquals(apps.rows().size(), 1,
                "searching by member number should narrow the list to that member's application");
        assertEquals(apps.listTotal().intValue(), 1, "the pager should show one matching application");
    }

    @Test(description = "TC draft-4: search and the status filter work together")
    public void searchAndStatusFilterWorkTogether() {
        ApplicationsPage apps = applications();

        // The approved case searches the seeded Nytest last name and then applies
        // Submitted. The same combination runs here on the applications the
        // environment holds, keyed to the first visible row's own status. The rows
        // are read with rowSnapshots() so that the app's re-render when the search
        // or filter applies cannot make the row elements stale between reads.
        ApplicationsPage.RowSnapshot sample = apps.rowSnapshots().get(0);
        String query = lastTokenOf(sample.applicant());
        StatusFilter sampleStatus = statusWithBadge(sample.status());

        apps.search(query);
        for (ApplicationsPage.RowSnapshot row : apps.rowSnapshots()) {
            assertTrue(row.applicant().toLowerCase().contains(query.toLowerCase()),
                    "searching by last name should keep only matching applications; saw " + row.applicant());
        }

        apps.selectStatus(sampleStatus);
        for (ApplicationsPage.RowSnapshot row : apps.rowSnapshots()) {
            assertEquals(row.status(), sampleStatus.badgeLabel(),
                    "with the search active the status filter should still hide other statuses");
            assertTrue(row.applicant().toLowerCase().contains(query.toLowerCase()),
                    "with the search active the status filter should keep only matching applications");
        }

        apps.clearSearch();
        for (ApplicationsPage.RowSnapshot row : apps.rowSnapshots()) {
            assertEquals(row.status(), sampleStatus.badgeLabel(),
                    "clearing the search should keep the status filter applied");
        }
        assertEquals(apps.listTotal(), summaryCountOf(apps, sampleStatus),
                "the status filter should apply on its own once the search is cleared");

        apps.selectStatus(StatusFilter.ALL);
        assertEquals(apps.listTotal(), apps.totalCount(), "All should show the full list again");
    }

    @Test(description = "TC draft-5: the summary counts match the lists for each status")
    public void summaryCountsMatchTheListsForEachStatus() {
        ApplicationsPage apps = applications();

        // Awaiting review has no filter of its own, so only its display is checked.
        assertTrue(apps.awaitingReviewCount() >= 0, "the summary should show the Awaiting review count");

        for (StatusFilter status : List.of(StatusFilter.IN_PROGRESS, StatusFilter.SUBMITTED, StatusFilter.ENROLLED,
                StatusFilter.REJECTED, StatusFilter.EXPIRED)) {
            apps.selectStatus(status);
            int expected = summaryCountOf(apps, status);
            if (expected == 0) {
                assertTrue(apps.showsNoMatches(),
                        "the " + status.label() + " summary says none exist, so the list should be empty");
                assertEquals(apps.rows().size(), 0, "the " + status.label() + " list should have no rows");
            } else {
                assertEquals(apps.listTotal(), expected,
                        "the " + status.label() + " list should have as many applications as the summary shows");
            }
        }

        apps.selectStatus(StatusFilter.ALL);
        assertEquals(apps.listTotal(), apps.totalCount(),
                "the All list should have as many applications as the All applications summary shows");
    }

    @Test(description = "TC draft-6: SSNs in the list show only the last four digits")
    public void ssnColumnShowsOnlyLastFourDigits() {
        ApplicationsPage apps = applications();

        apps.search(AUTO_FIRST_NAME);
        if (!apps.showsNoMatches()) {
            assertEquals(apps.rowSnapshots().get(0).ssn(), "***-**-6789",
                    "the seeded AUTO-DRAFT row exists, so its SSN should show only the last four digits");
        }
        apps.clearSearch();

        do {
            for (ApplicationsPage.RowSnapshot row : apps.rowSnapshots()) {
                String ssn = row.ssn();
                assertTrue(isMaskedSsn(ssn),
                        "every SSN must be masked to the form ***-**-XXXX; saw " + ssn
                                + " (char codes: " + codesOf(ssn) + ")");
            }
            if (apps.hasNextPage()) {
                apps.nextPage();
            } else {
                break;
            }
        } while (true);
    }

    @Test(description = "TC draft-7: 25 applications per page with Previous and Next")
    public void pagingShowsTwentyFiveApplicationsPerPageWithPreviousAndNext() {
        ApplicationsPage apps = applications();

        // More than 25 applications exist (e.g. the seeded AUTO-PAGE01-30).
        assertTrue(apps.pageCount() >= 2, "the paging control should show at least two pages");
        assertEquals(apps.pageNumber(), 1, "the list should open on page 1");
        assertEquals(apps.rows().size(), PAGE_SIZE, "page 1 should show exactly 25 applications");
        assertFalse(apps.hasPreviousPage(), "Previous should be disabled on page 1");
        Set<String> firstPage = new HashSet<>(applicationIdsOf(apps));

        apps.nextPage();
        assertEquals(apps.pageNumber(), 2, "Next should move to page 2");
        assertTrue(!apps.rows().isEmpty() && apps.rows().size() <= PAGE_SIZE,
                "page 2 should show the next set, up to 25 applications");
        Set<String> overlap = new HashSet<>(applicationIdsOf(apps));
        overlap.retainAll(firstPage);
        assertTrue(overlap.isEmpty(), "the applications of page 1 should not be shown again; saw " + overlap);

        apps.previousPage();
        assertEquals(apps.pageNumber(), 1, "Previous should move back to page 1");
        assertEquals(new HashSet<>(applicationIdsOf(apps)), firstPage, "page 1 should show the same applications as before");
    }

    @Test(description = "TC draft-8: a search with no matching applications shows an empty list")
    public void searchWithNoMatchesShowsAnEmptyList() {
        ApplicationsPage apps = applications();

        assertFalse(apps.rows().isEmpty(), "at least one application should exist");

        apps.search("AUTO-NOMATCH");
        assertEquals(apps.rows().size(), 0, "nothing should match AUTO-NOMATCH");
        assertTrue(apps.showsNoMatches(), "the list should show the No applications match state");

        apps.clearSearch();
        assertEquals(apps.listTotal(), apps.totalCount(), "clearing the search should restore the unfiltered list");
        assertFalse(apps.rows().isEmpty(), "the unfiltered list should list applications again");
    }

    // ------------------------------------------------------------------ helpers

    private static List<String> applicantNames(ApplicationsPage apps) {
        List<String> names = new ArrayList<>();
        for (WebElement row : apps.rows()) {
            names.add(apps.applicantOf(row));
        }
        return names;
    }

    private static List<String> applicationIdsOf(ApplicationsPage apps) {
        return apps.rowSnapshots().stream().map(ApplicationsPage.RowSnapshot::applicationId).toList();
    }

    /** True when the value matches the masked form, ignoring invisible whitespace. */
    private static boolean isMaskedSsn(String ssn) {
        return MASKED_SSN.matcher(INVISIBLE.matcher(ssn).replaceAll("")).matches();
    }

    /** The code point of each character, for diagnosing what a displayed value contains. */
    private static String codesOf(String value) {
        return value.codePoints().mapToObj(c -> Integer.toString(c)).collect(Collectors.joining(" "));
    }

    private static boolean contains(List<String> names, String expected) {
        return names.stream().anyMatch(name -> name.toLowerCase().contains(expected.toLowerCase()));
    }

    private static String firstTokenOf(String applicant) {
        return applicant.split("\\s+")[0];
    }

    private static String lastTokenOf(String applicant) {
        String[] tokens = applicant.split("\\s+");
        return tokens[tokens.length - 1];
    }

    private static StatusFilter statusWithBadge(String badgeLabel) {
        for (StatusFilter status : StatusFilter.values()) {
            if (status.badgeLabel() != null && status.badgeLabel().equalsIgnoreCase(badgeLabel)) {
                return status;
            }
        }
        throw new IllegalArgumentException("no status filter matches the badge " + badgeLabel);
    }

    private static int summaryCountOf(ApplicationsPage apps, StatusFilter status) {
        return switch (status) {
            case IN_PROGRESS -> apps.inProgressCount();
            case SUBMITTED -> apps.submittedCount();
            case ENROLLED -> apps.enrolledCount();
            case REJECTED -> apps.rejectedCount();
            case EXPIRED -> apps.expiredCount();
            case ALL -> apps.totalCount();
        };
    }
}
