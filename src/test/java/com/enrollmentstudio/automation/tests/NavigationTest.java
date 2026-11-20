package com.enrollmentstudio.automation.tests;

import com.enrollmentstudio.automation.base.SignedInTest;
import com.enrollmentstudio.automation.pages.components.SideNav;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class NavigationTest extends SignedInTest {

    @DataProvider(name = "sidebar")
    public Object[][] sidebar() {
        return new Object[][]{
                {SideNav.Item.REVIEW_QUEUE, "Review queue"},
                {SideNav.Item.POSSIBLE_DUPLICATES, "Possible duplicates"},
                {SideNav.Item.AUDIT_LOG, "Audit log"},
                {SideNav.Item.DELETION_REQUESTS, "Deletion requests"},
                {SideNav.Item.STAFF_ACCESS, "Staff access"},
        };
    }

    @Test(dataProvider = "sidebar", description = "Each sidebar entry opens its page")
    public void sidebarOpensPage(SideNav.Item item, String expectedHeading) {
        SideNav nav = applications().nav();

        nav.open(item);

        assertEquals(nav.currentPath(), item.path());
        assertEquals(nav.heading(), expectedHeading);
    }
}
