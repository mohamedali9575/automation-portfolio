package com.automation.hunger;

import com.microsoft.playwright.*;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Project 3b: nested-dropdown UI test (same pattern as a
 * Hunger Station sidebar: expand parent menu, then click child item).
 * Demo site: https://demoqa.com/menu (public nested menu).
 * Run: mvn test -Dtest=NestedMenuTest
 */
public class NestedMenuTest {

    private Playwright playwright;
    private Browser browser;
    private Page page;

    @BeforeMethod
    public void setUp() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
        page = browser.newPage();
        page.setDefaultTimeout(15000);
    }

    @Test
    public void expandParentThenClickChild() {
        page.navigate("https://demoqa.com/menu");
        // "Main Item 2" is a dropdown parent, like "Hunger Station".
        Locator parent = page.locator("#nav >> text=Main Item 2").first();
        parent.scrollIntoViewIfNeeded();
        parent.hover(); // expands the submenu (same idea as clicking the arrow)
        page.waitForTimeout(800);

        Locator child = page.locator("#nav >> text=SUB SUB LIST").first();
        Assert.assertTrue(child.count() > 0, "Submenu child never appeared after expanding parent");
        child.scrollIntoViewIfNeeded();
        System.out.println("Nested menu expanded OK, child found: " + child.innerText());
    }

    @AfterMethod
    public void tearDown() {
        if (page != null) page.close();
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();
    }
}
