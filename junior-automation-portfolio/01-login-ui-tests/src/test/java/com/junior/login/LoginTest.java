package com.junior.login;

import com.microsoft.playwright.*;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Junior Project 1: UI login tests (data-driven).
 * Demo site: https://www.saucedemo.com/
 * Run: mvn test
 */
public class LoginTest {

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

    @DataProvider(name = "badLogins")
    public Object[][] badLogins() {
        return new Object[][]{
                {"locked_out_user", "secret_sauce", "Sorry, this user has been locked out"},
                {"standard_user", "wrong_password", "Username and password do not match"},
                {"", "", "Username is required"},
        };
    }

    @Test
    public void validLoginShowsInventory() {
        page.navigate("https://www.saucedemo.com/");
        page.locator("[data-test='username']").fill("standard_user");
        page.locator("[data-test='password']").fill("secret_sauce");
        page.locator("[data-test='login-button']").click();

        Assert.assertTrue(page.url().contains("inventory"),
                "Expected inventory page, got: " + page.url());
        Assert.assertTrue(page.locator(".inventory_item").count() > 0,
                "No inventory items visible after login");
    }

    @Test(dataProvider = "badLogins")
    public void invalidLoginShowsError(String user, String pass, String expectedError) {
        page.navigate("https://www.saucedemo.com/");
        page.locator("[data-test='username']").fill(user);
        page.locator("[data-test='password']").fill(pass);
        page.locator("[data-test='login-button']").click();

        String error = page.locator("[data-test='error']").innerText();
        Assert.assertTrue(error.contains(expectedError),
                "Expected error containing '" + expectedError + "' but got: " + error);
    }

    @AfterMethod
    public void tearDown() {
        if (page != null) page.close();
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();
    }
}
