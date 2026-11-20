package com.enrollmentstudio.automation.pages;

import com.enrollmentstudio.automation.config.Config;
import com.enrollmentstudio.automation.pages.components.SideNav;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** The landing page after sign-in: the list of enrollment applications. */
public class ApplicationsPage extends BasePage {

    public static final String PATH = "/";

    /** Status filters on the list: their label, filter button testid and the badge shown in rows. */
    public enum StatusFilter {
        ALL("All", "csr-search-status-all", null),
        IN_PROGRESS("In progress", "csr-search-status-draft", "Draft"),
        SUBMITTED("Submitted", "csr-search-status-submitted", "Submitted"),
        ENROLLED("Enrolled", "csr-search-status-enrolled", "Enrolled"),
        REJECTED("Rejected", "csr-search-status-rejected", "Rejected"),
        EXPIRED("Expired", "csr-search-status-expired", "Expired");

        private final String label;
        private final String filterTestId;
        private final String badgeLabel;

        StatusFilter(String label, String filterTestId, String badgeLabel) {
            this.label = label;
            this.filterTestId = filterTestId;
            this.badgeLabel = badgeLabel;
        }

        public String label() {
            return label;
        }

        public String badgeLabel() {
            return badgeLabel;
        }
    }

    /**
     * One row of the current page's list, read in a single script pass: the
     * applicant, the status badge text, the masked SSN and the row link's href.
     */
    public record RowSnapshot(String applicant, String status, String ssn, String applicationId) {

        static RowSnapshot from(Object raw) {
            List<?> cells = (List<?>) raw;
            return new RowSnapshot(cellText(cells.get(0)), cellText(cells.get(1)),
                    cellText(cells.get(2)), cellText(cells.get(3)));
        }

        private static String cellText(Object value) {
            return value == null ? "" : value.toString();
        }
    }

    private static final By TOTAL = testId("summary-total");
    private static final By TABLE = By.cssSelector("main table");
    private static final By BODY_ROW = By.cssSelector("main table tbody tr");
    private static final By APPLICANT_LINK = By.cssSelector("a[data-testid^='member-']");
    private static final By SSN_CELL = By.cssSelector("td:nth-of-type(2)");
    private static final By STATUS_BADGE = By.cssSelector("td:nth-of-type(4) [data-slot='badge']");
    private static final By SEARCH_QUERY = testId("csr-search-query");
    private static final By SEARCH_CLEAR = testId("csr-search-clear");
    private static final By PAGER_PAGE = testId("applications-pager-page");
    private static final By PAGER_RANGE = testId("applications-pager-range");
    private static final By PAGER_PREVIOUS = testId("applications-pager-previous");
    private static final By PAGER_NEXT = testId("applications-pager-next");
    private static final By NO_MATCHES = By.xpath("//*[contains(text(), 'No applications match')]");

    private static final Pattern PAGE_OF = Pattern.compile("Page (\\d+) of (\\d+)");
    private static final Pattern FIRST_SHOWN = Pattern.compile("^\\s*(\\d+)");
    private static final Pattern OF_TOTAL = Pattern.compile("of (\\d+)");

    /**
     * The status region shows "Loading applications..." and the table and pager
     * are hidden while the list reloads after a search, filter or page change.
     * That state can come and go between two wait polls and is part of the
     * returned fingerprint, so callers can predict list data changes.
     */
    private static final String LIST_STATE_JS = """
            const main = document.querySelector('main');
            if (!main) { return null; }
            const text = main.textContent;
            if (text.indexOf('Loading applications...') >= 0) { return null; }
            const rows = Array.from(main.querySelectorAll('table tbody tr'));
            const rowsPart = rows.map(function (r) {
                const name = r.querySelector('a[data-testid^="member-"]');
                const badge = r.querySelector('td:nth-of-type(4) [data-slot="badge"]');
                const ssn = r.querySelector('td:nth-of-type(2)');
                return (name ? name.textContent.trim() : '')
                    + '/' + (badge ? badge.textContent.trim() : '')
                    + '/' + (ssn ? ssn.textContent.trim() : '');
            }).join(';');
            const page = main.querySelector('[data-testid="applications-pager-page"]');
            const range = main.querySelector('[data-testid="applications-pager-range"]');
            const query = main.querySelector('[data-testid="csr-search-query"]');
            return (page ? page.textContent.trim() : '')
                + '|' + (range ? range.textContent.trim() : '')
                + '|' + (query ? query.value : '')
                + '|' + rows.length
                + '|' + (text.indexOf('No applications match') >= 0 ? 'empty' : 'list')
                + '|' + rowsPart;
            """;

    /**
     * Facts of every row of the current page, read in one script pass so that a
     * re-render between round trips cannot make the row elements go stale.
     */
    private static final String ROW_FACTS_JS = """
            const main = document.querySelector('main');
            if (!main) { return null; }
            return Array.from(main.querySelectorAll('table tbody tr')).map(function (r) {
                const link = r.querySelector('a[data-testid^="member-"]');
                const badge = r.querySelector('td:nth-of-type(4) [data-slot="badge"]');
                const ssn = r.querySelector('td:nth-of-type(2)');
                return [link ? link.textContent.trim() : '',
                        badge ? badge.textContent.trim() : '',
                        ssn ? ssn.textContent.trim() : '',
                        link ? (link.getAttribute('href') || '') : ''];
            });
            """;

    /** Reloading the list can be slower than the default wait; overlap that like sign-in does. */
    private static final Duration SETTLE_WAIT = Config.explicitWait().multipliedBy(3);

    private String lastSeenState;

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

    // ---------------------------------------------------------------- actions

    /** Applies a status filter and waits for the filtered list to replace the old one. */
    public ApplicationsPage selectStatus(StatusFilter status) {
        prepareToWaitOut();
        click(testId(status.filterTestId));
        settled();
        return this;
    }

    /** Enters a search term and waits for the narrowed list to replace the old one. */
    public ApplicationsPage search(String term) {
        prepareToWaitOut();
        type(SEARCH_QUERY, term);
        settled();
        return this;
    }

    /** Clears the search box (by its clear button when one is shown) and waits for the list. */
    public ApplicationsPage clearSearch() {
        prepareToWaitOut();
        if (isPresent(SEARCH_CLEAR)) {
            click(SEARCH_CLEAR);
        } else {
            driver.findElement(SEARCH_QUERY).clear();
        }
        settled();
        return this;
    }

    public ApplicationsPage nextPage() {
        prepareToWaitOut();
        click(PAGER_NEXT);
        settled();
        return this;
    }

    public ApplicationsPage previousPage() {
        prepareToWaitOut();
        click(PAGER_PREVIOUS);
        settled();
        return this;
    }

    private void prepareToWaitOut() {
        lastSeenState = null;
    }

    /**
     * Waits out any in-flight reload of the list: no "Loading applications..."
     * state, and the fingerprint of page, pager, search box and rows is equal on
     * two consecutive wait polls.
     */
    private void settled() {
        new WebDriverWait(driver, SETTLE_WAIT).until(d -> {
            String now = listState();
            if (now == null) {
                prepareToWaitOut();
                return false;
            }
            if (now.equals(lastSeenState)) {
                return true;
            }
            lastSeenState = now;
            return false;
        });
    }

    private String listState() {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        return (String) js.executeScript(LIST_STATE_JS);
    }

    // ------------------------------------------------------------------- facts

    /** The table's header labels, in order. */
    public List<String> columnNames() {
        List<WebElement> headers = wait.until(d -> d.findElements(By.cssSelector("main table thead th")));
        List<String> names = new ArrayList<>();
        for (WebElement header : headers) {
            names.add(header.getText().trim());
        }
        return names;
    }

    /** The filter labels as rendered, in order. */
    public List<String> statusFilterLabels() {
        List<String> labels = new ArrayList<>();
        for (StatusFilter status : StatusFilter.values()) {
            labels.add(text(testId(status.filterTestId)));
        }
        return labels;
    }

    /** The status filter currently checked (radio behaviour). */
    public StatusFilter selectedStatus() {
        for (StatusFilter status : StatusFilter.values()) {
            String checked = driver.findElement(testId(status.filterTestId)).getAttribute("aria-checked");
            if (Boolean.parseBoolean(checked)) {
                return status;
            }
        }
        throw new IllegalStateException("no status filter is checked on the Applications list");
    }

    /** Rows of the current list page (empty when the list has no matches). */
    public List<WebElement> rows() {
        return driver.findElements(BODY_ROW);
    }

    /**
     * Every row of the current page as a {@link RowSnapshot}, read in one script
     * pass; use this instead of holding row WebElements across a re-render, which
     * makes them stale. Applicant, status, ssn and applicationId come from one
     * DOM state, so per-row reads cannot disagree between round trips.
     */
    public List<RowSnapshot> rowSnapshots() {
        Object rowsData = ((JavascriptExecutor) driver).executeScript(ROW_FACTS_JS);
        if (!(rowsData instanceof List<?> rows)) {
            return List.of();
        }
        List<RowSnapshot> snapshots = new ArrayList<>();
        for (Object row : rows) {
            snapshots.add(RowSnapshot.from(row));
        }
        return snapshots;
    }

    /** The applicant cell of a row: full name shown as the row link. */
    public String applicantOf(WebElement row) {
        return row.findElement(APPLICANT_LINK).getText().trim();
    }

    /** A stable identity for a row: the application's own enrollment URL on the row link. */
    public String applicationIdOf(WebElement row) {
        return row.findElement(APPLICANT_LINK).getAttribute("href");
    }

    /** The SSN cell of a row as masked by the app. */
    public String ssnOf(WebElement row) {
        return row.findElement(SSN_CELL).getText().trim();
    }

    /** The status badge text of a row, e.g. Draft, Enrolled. */
    public String statusOf(WebElement row) {
        return row.findElement(STATUS_BADGE).getText().trim();
    }

    public boolean showsNoMatches() {
        return isPresent(NO_MATCHES);
    }

    /** The number shown in the All applications summary card. */
    public int totalCount() {
        return Integer.parseInt(visible(TOTAL).getText().trim());
    }

    /** The number shown in the In progress summary card. */
    public int inProgressCount() {
        return Integer.parseInt(text(testId("summary-draft")).trim());
    }

    /** The number shown in the Submitted summary card. */
    public int submittedCount() {
        return Integer.parseInt(text(testId("summary-submitted")).trim());
    }

    /** The number shown in the Enrolled summary card. */
    public int enrolledCount() {
        return Integer.parseInt(text(testId("summary-enrolled")).trim());
    }

    /** The number shown in the Rejected summary card. */
    public int rejectedCount() {
        return Integer.parseInt(text(testId("summary-rejected")).trim());
    }

    /** The number shown in the Expired summary card. */
    public int expiredCount() {
        return Integer.parseInt(text(testId("summary-expired")).trim());
    }

    /** The number shown in the Awaiting review summary card. */
    public int awaitingReviewCount() {
        return Integer.parseInt(text(testId("summary-review")).trim());
    }

    /** Total applications of the current list, from the pager line; null when no matches. */
    public Integer listTotal() {
        if (!isPresent(PAGER_RANGE)) {
            return null;
        }
        Matcher total = OF_TOTAL.matcher(text(PAGER_RANGE));
        return total.find() ? Integer.parseInt(total.group(1)) : null;
    }

    /** The first row number shown in the pager line, e.g. 26 on page 2; null when no matches. */
    public Integer rangeFirstShown() {
        if (!isPresent(PAGER_RANGE)) {
            return null;
        }
        Matcher first = FIRST_SHOWN.matcher(text(PAGER_RANGE));
        return first.find() ? Integer.parseInt(first.group(1)) : null;
    }

    /** The current page, 1-based, parsed from e.g. "Page 2 of 4". */
    public int pageNumber() {
        Matcher page = PAGE_OF.matcher(text(PAGER_PAGE));
        if (!page.find()) {
            throw new IllegalStateException("pager does not show a page number: " + text(PAGER_PAGE));
        }
        return Integer.parseInt(page.group(1));
    }

    /** The total page count, parsed from e.g. "Page 2 of 4". */
    public int pageCount() {
        Matcher page = PAGE_OF.matcher(text(PAGER_PAGE));
        if (!page.find()) {
            throw new IllegalStateException("pager does not show a page count: " + text(PAGER_PAGE));
        }
        return Integer.parseInt(page.group(2));
    }

    public boolean hasNextPage() {
        return clickabilityOf(PAGER_NEXT);
    }

    public boolean hasPreviousPage() {
        return clickabilityOf(PAGER_PREVIOUS);
    }

    private boolean clickabilityOf(By pagerButton) {
        List<WebElement> buttons = driver.findElements(pagerButton);
        return !buttons.isEmpty() && buttons.get(0).isEnabled();
    }
}
