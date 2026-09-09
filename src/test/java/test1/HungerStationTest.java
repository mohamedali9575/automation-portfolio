package test1;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.nio.file.Paths;

/**
 * Hunger Station report test.
 * Flow:
 * 1. Open https://rabeh-test.nozomtech.com/
 * 2. Login with username: shams1 / password: 123 (trimmed, no spaces)
 * 3. Wait 2 sec
 * 4. Click "Hunger Station" button in the left sidebar
 * 5. Click "Top Riders" report in the left sidebar
 */
public class HungerStationTest {

    private static final String BASE_URL = "https://rabeh-test.nozomtech.com/index.html#/login";

    // Credentials (trimmed -> no spaces as requested)
    private static final String USERNAME = "shams1".trim();
    private static final String PASSWORD = "123".trim();

    private Playwright playwright;
    private Browser browser;
    private BrowserContext context;
    private Page page;

    @BeforeMethod
    public void setUp() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(false)          // set true for CI / headless run
                .setSlowMo(300));            // slows down actions so you can see the flow
        context = browser.newContext(new Browser.NewContextOptions()
                .setViewportSize(1366, 768));
        page = context.newPage();
        page.setDefaultTimeout(30000);
    }

    @Test
    public void testHungerStationReport() {
        // 1. Open login page
        System.out.println("Step 1: Opening login page: " + BASE_URL);
        page.navigate(BASE_URL);
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        page.screenshot(new Page.ScreenshotOptions()
                .setPath(Paths.get("target/screenshots/01-login-page.png")));

        // 2. Login
        System.out.println("Step 2: Logging in as: " + USERNAME);
        fillLoginForm();
        page.screenshot(new Page.ScreenshotOptions()
                .setPath(Paths.get("target/screenshots/02-after-login.png")));

        // 3. Wait 2 seconds (explicit requirement)
        System.out.println("Step 3: Waiting 2 seconds after login...");
        page.waitForTimeout(2000);

        // 4. Click "Hunger Station" in the left sidebar
        System.out.println("Step 4: Clicking 'Hunger Station' in left sidebar...");
        waitForSidebar();
        debugSidebarItems();
        clickSidebarItem("Hunger Station",
                new String[]{"Hunger Station", "Hunger station", "Hunger", "هنقر"});
        page.waitForTimeout(2000);
        page.screenshot(new Page.ScreenshotOptions()
                .setPath(Paths.get("target/screenshots/03-hunger-station-open.png")));
        System.out.println("Current URL (after Hunger Station click): " + page.url());

        // 5. Click "Top Riders" report (submenu under Hunger Station)
        System.out.println("Step 5: Clicking 'Top Riders' report in left sidebar...");
        debugAntMenu();
        clickTopRidersSubmenu();
        page.waitForTimeout(2000);
        try {
            // Hash-routing app: wait for the Angular route to change instead of NETWORKIDLE
            // (the dashboard polls, so NETWORKIDLE never settles - seen in previous run).
            page.waitForURL("**/top-riders**", new Page.WaitForURLOptions().setTimeout(15000));
        } catch (PlaywrightException e) {
            System.out.println("waitForURL(top-riders) timeout, continuing. URL now: " + page.url());
        }
        System.out.println("Current URL (top-riders): " + page.url());
        Assert.assertTrue(page.url().toLowerCase().contains("top-riders"),
                "Did not land on top-riders page. Actual URL: " + page.url());
        assertPageHasContent("top-riders");
        page.screenshot(new Page.ScreenshotOptions()
                .setPath(Paths.get("target/screenshots/04-top-riders.png")));

        System.out.println("TEST PASSED: Hunger Station -> Top Riders report opened successfully.");
    }

    /**
     * Tries common username/password/login-button selectors so the test
     * works even if input attributes change slightly.
     */
    private void fillLoginForm() {
        Locator usernameInput = page.locator(
                "input[name*='user' i], input[name*='login' i], input[id*='user' i], " +
                "input[placeholder*='user' i], input[placeholder*='name' i], " +
                "input[type='text'], input:not([type])").first();
        Locator passwordInput = page.locator("input[type='password']").first();
        Locator loginButton = page.locator(
                "button[type='submit'], input[type='submit'], " +
                "button:has-text('Login'), button:has-text('Log in'), " +
                "button:has-text('Sign in'), button:has-text('Sign In'), " +
                "button:has-text('دخول'), button:has-text('تسجيل')").first();

        usernameInput.waitFor();
        passwordInput.waitFor();

        // "no spaces" -> trim + clear before fill
        usernameInput.fill("");
        usernameInput.fill(USERNAME);
        passwordInput.fill("");
        passwordInput.fill(PASSWORD);

        // Verify no accidental spaces
        Assert.assertEquals(usernameInput.inputValue().trim(), USERNAME, "Username has extra spaces!");
        Assert.assertEquals(passwordInput.inputValue().trim(), PASSWORD, "Password has extra spaces!");

        loginButton.waitFor();
        loginButton.click();

        // Give the app a chance to process login (redirect / token storage).
        // We still keep the explicit 2-sec wait in the main test afterwards.
        page.waitForLoadState(LoadState.DOMCONTENTLOADED, new Page.WaitForLoadStateOptions().setTimeout(30000));
    }

    private void waitForSidebar() {
        // Left menu is usually aside / mat-sidenav / nav. Wait for any of them.
        Locator sidebar = page.locator("aside, mat-sidenav, mat-nav-list, nav, .sidebar, .sidenav").first();
        try {
            sidebar.waitFor(new Locator.WaitForOptions().setTimeout(30000));
            System.out.println("Sidebar detected: " + sidebar.evaluate("el => el.tagName + ' / ' + el.className"));
        } catch (PlaywrightException e) {
            System.out.println("WARNING: no aside/nav/mat-sidenav found, continuing anyway. URL: " + page.url());
        }
        // Extra settle time for Angular menu rendering
        page.waitForTimeout(2000);
    }

    private void debugSidebarItems() {
        try {
            Object result = page.locator("aside li, aside a, aside button, aside div.ant-menu-submenu-title, aside li.ant-menu-item")
                    .evaluateAll("els => els.map(e => (e.innerText || e.textContent || '').trim()).filter(t => t.length > 0).slice(0, 60)");
            System.out.println("Sidebar items found: " + result);
        } catch (Exception e) {
            System.out.println("Could not list sidebar items: " + e.getMessage());
        }
    }

    private void debugAntMenu() {
        debugSidebarItems();
        try {
            Object items = page.locator(".ant-menu-item, .ant-menu-submenu-title")
                    .evaluateAll("els => els.map(e => ((e.textContent||'').trim() + ' | visible=' + (e.offsetParent !== null) + ' | cls=' + e.className).substring(0,120)).slice(0,60)");
            System.out.println("Ant menu items: " + items);
        } catch (Exception e) {
            System.out.println("Could not list ant menu: " + e.getMessage());
        }
        try {
            Object links = page.locator("a[href*='hunger'], a[href*='rider'], a[href*='top']")
                    .evaluateAll("els => els.map(e => ((e.innerText||e.textContent||'').trim().substring(0,60) + ' -> ' + e.getAttribute('href'))).slice(0,30)");
            System.out.println("Rider-related links: " + links);
        } catch (Exception e) {
            System.out.println("Could not list rider links: " + e.getMessage());
        }
    }

    /**
     * Clicks the Top Riders submenu item under Hunger Station.
     * Sidebar is Ant Design (ant-layout-sider): parent is li.ant-menu-submenu
     * with div.ant-menu-submenu-title containing an <a href="#/hunger...dashboard">.
     * Clicking the LINK navigates to dashboard but does NOT expand the dropdown,
     * so we must click the ARROW / title edge to expand, then click the child
     * li.ant-menu-item.
     */
    private void clickTopRidersSubmenu() {
        Locator submenu = page.locator("li.ant-menu-submenu:has-text('Hunger Station')").first();
        if (submenu.count() == 0) {
            submenu = page.locator("li.ant-menu-submenu:has-text('Hunger')").first();
        }

        // Diagnostic: dump submenu state + HTML so we can see if it is open/closed
        try {
            if (submenu.count() > 0) {
                System.out.println("Hunger submenu class: " + submenu.getAttribute("class"));
                System.out.println("Hunger submenu innerHTML (first 2000 chars): "
                        + submenu.innerHTML().substring(0, Math.min(2000, submenu.innerHTML().length())));
                Object titleInfo = page.locator("li.ant-menu-submenu:has-text('Hunger Station') .ant-menu-submenu-title")
                        .first().evaluate("el => 'cls=' + el.className + ' | expanded=' + el.getAttribute('aria-expanded')");
                System.out.println("Hunger title state: " + titleInfo);
            } else {
                System.out.println("WARNING: li.ant-menu-submenu for Hunger NOT found. Dumping aside HTML:");
                System.out.println(page.locator("aside").innerHTML().substring(0, 3000));
            }
        } catch (Exception e) {
            System.out.println("Submenu dump failed: " + e.getMessage());
        }

        // 1. Expand the dropdown via the ARROW (not the dashboard link).
        //    Try arrow click -> title-edge click -> Enter key -> JS click.
        String[] expandAttempts = new String[]{
                "li.ant-menu-submenu:has-text('Hunger Station') .ant-menu-submenu-arrow",
                "li.ant-menu-submenu:has-text('Hunger Station') .ant-menu-submenu-expand-icon",
                "li.ant-menu-submenu:has-text('Hunger Station') .anticon",
                "li.ant-menu-submenu:has-text('Hunger Station') > .ant-menu-submenu-title"
        };
        boolean expanded = false;
        for (String sel : expandAttempts) {
            try {
                Locator topCheck = page.locator("li.ant-menu-submenu:has-text('Hunger Station') li.ant-menu-item:has-text('Top Riders')");
                if (topCheck.count() > 0 && topCheck.first().isVisible()) {
                    expanded = true;
                    System.out.println("Submenu already expanded (Top Riders visible).");
                    break;
                }
                Locator expander = page.locator(sel).first();
                if (expander.count() == 0) continue;
                System.out.println("Expand attempt via: " + sel);
                expander.scrollIntoViewIfNeeded();
                try {
                    expander.click(new Locator.ClickOptions().setTimeout(5000));
                } catch (PlaywrightException clickFail) {
                    System.out.println("Normal click failed, trying JS click: " + clickFail.getMessage());
                    expander.evaluate("el => el.click()");
                }
                page.waitForTimeout(1200);
                if (topCheck.count() > 0 && topCheck.first().isVisible()) {
                    expanded = true;
                    System.out.println("Expanded successfully via: " + sel);
                    break;
                }
            } catch (Exception e) {
                System.out.println("Expand via '" + sel + "' note: " + e.getMessage());
            }
        }
        if (!expanded) {
            // Last resort: keyboard expand (focus title + ArrowDown/Enter)
            try {
                Locator title = page.locator("li.ant-menu-submenu:has-text('Hunger Station') > .ant-menu-submenu-title").first();
                if (title.count() > 0) {
                    System.out.println("Trying keyboard expand (focus + Enter)...");
                    title.scrollIntoViewIfNeeded();
                    title.focus();
                    page.keyboard().press("Enter");
                    page.waitForTimeout(1200);
                }
            } catch (Exception e) {
                System.out.println("Keyboard expand note: " + e.getMessage());
            }
        }
        debugAntMenu();

        // 2. Click the now-visible Top Riders child item.
        String[] childSelectors = new String[]{
                "li.ant-menu-submenu:has-text('Hunger Station') li.ant-menu-item:has-text('Top Riders')",
                "li.ant-menu-item:has-text('Top Riders')",
                ".ant-menu-item:has-text('Top Riders')",
                "aside li:visible:has-text('Top Riders')"
        };
        for (String sel : childSelectors) {
            try {
                Locator loc = page.locator(sel).first();
                if (loc.count() > 0 && loc.isVisible()) {
                    System.out.println("Clicking Top Riders via: " + sel);
                    loc.scrollIntoViewIfNeeded();
                    loc.click();
                    return;
                } else {
                    System.out.println("Top Riders not visible via: " + sel);
                }
            } catch (PlaywrightException e) {
                System.out.println("Child selector '" + sel + "' failed: " + e.getMessage());
            }
        }

        // 3. Force-click hidden item via JS as last resort (proves route works).
        try {
            Locator hidden = page.locator("li.ant-menu-item:has-text('Top Riders')").first();
            if (hidden.count() > 0) {
                System.out.println("Force-clicking hidden Top Riders item via JS...");
                hidden.evaluate("el => el.click()");
                page.waitForTimeout(1500);
                if (page.url().toLowerCase().contains("top-riders")) {
                    System.out.println("JS click navigated to top-riders!");
                    return;
                }
            }
        } catch (Exception e) {
            System.out.println("JS force-click note: " + e.getMessage());
        }

        // 4. Nothing worked -> dump diagnostics and fail.
        page.screenshot(new Page.ScreenshotOptions()
                .setPath(Paths.get("target/screenshots/ERROR-Top-Riders.png")));
        debugAntMenu();
        try {
            Object allTop = page.locator(":text('Top')")
                    .evaluateAll("els => els.map(e => (e.tagName + ': ' + (e.innerText||e.textContent||'').trim()).substring(0,120)).slice(0,30)");
            System.out.println("All elements containing 'Top': " + allTop);
        } catch (Exception e) {
            System.out.println("Could not dump 'Top' elements: " + e.getMessage());
        }
        Assert.fail("Could not click visible 'Top Riders' submenu item. See 'Ant menu items' log + ERROR-Top-Riders.png. URL: " + page.url());
    }

    /**
     * Clicks an item in the LEFT sidebar by visible text.
     * Tries each candidate text inside sidebar containers first,
     * then falls back to a global text search.
     * Handles expandable parent menus (clicks parent, then child appears).
     */
    private void clickSidebarItem(String logicalName, String[] candidates) {
        PlaywrightException lastError = null;

        String[] sidebarScopes = new String[]{
                "aside", "mat-sidenav", "mat-nav-list", "nav", ".sidebar", ".sidenav", "mat-sidenav-container"
        };

        for (String text : candidates) {
            // 1. Try scoped to left menu containers
            for (String scope : sidebarScopes) {
                try {
                    Locator loc = page.locator(scope + " a, " + scope + " button, " + scope + " mat-list-item, " + scope + " li")
                            .filter(new Locator.FilterOptions().setHasText(text)).first();
                    if (loc.count() > 0) {
                        try {
                            if (!loc.isVisible()) continue;
                        } catch (Exception ignored) { }
                        System.out.println("Clicking '" + logicalName + "' via scope '" + scope + "' matching text '" + text + "'");
                        loc.scrollIntoViewIfNeeded();
                        loc.click();
                        return;
                    }
                } catch (PlaywrightException e) {
                    lastError = e;
                }
            }

            // 2. Fallback: global role/text search (button or link containing text)
            try {
                Locator global = page.getByRole(com.microsoft.playwright.options.AriaRole.BUTTON,
                                new Page.GetByRoleOptions().setName(text))
                        .or(page.getByRole(com.microsoft.playwright.options.AriaRole.LINK,
                                new Page.GetByRoleOptions().setName(text)))
                        .or(page.getByText(text, new Page.GetByTextOptions().setExact(false))).first();
                if (global.count() > 0) {
                    System.out.println("Clicking '" + logicalName + "' via global text '" + text + "'");
                    global.scrollIntoViewIfNeeded();
                    global.click();
                    return;
                }
            } catch (PlaywrightException e) {
                lastError = e;
            }
        }

        page.screenshot(new Page.ScreenshotOptions()
                .setPath(Paths.get("target/screenshots/ERROR-" + logicalName.replaceAll("\\s+", "-") + ".png")));
        debugSidebarItems();
        String msg = "Could not find/click left-menu item '" + logicalName + "' (tried: " + String.join(", ", candidates) + "). URL: " + page.url();
        if (lastError != null) msg += " Last error: " + lastError.getMessage();
        Assert.fail(msg);
    }

    private void assertPageHasContent(String pageName) {
        String bodyText = page.locator("body").innerText();
        System.out.println("---- " + pageName + " body text (first 500 chars) ----");
        System.out.println(bodyText.length() > 500 ? bodyText.substring(0, 500) : bodyText);
        System.out.println("------------------------------------------------");
        Assert.assertFalse(bodyText.trim().isEmpty(), pageName + " page body is empty!");
        // Fail fast on obvious error pages
        String lower = bodyText.toLowerCase();
        Assert.assertFalse(lower.contains("404 not found") && bodyText.length() < 500,
                pageName + " page shows 404: " + bodyText);
    }

    @AfterMethod
    public void tearDown() {
        if (page != null) page.close();
        if (context != null) context.close();
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();
    }
}
