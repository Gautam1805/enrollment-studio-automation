package com.enrollmentstudio.automation.driver;

import com.enrollmentstudio.automation.config.Config;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.util.Map;

/** One WebDriver per test thread. Selenium Manager resolves the driver binary. */
public final class DriverFactory {

    private static final ThreadLocal<WebDriver> DRIVER = new ThreadLocal<>();

    private DriverFactory() {
    }

    public static WebDriver start() {
        if (!"chrome".equalsIgnoreCase(Config.browser())) {
            throw new IllegalStateException("only chrome is set up; got " + Config.browser());
        }
        ChromeOptions options = new ChromeOptions();
        // The app is a single-page app that keeps loading in the background;
        // commands return once the DOM is ready and explicit waits do the rest.
        options.setPageLoadStrategy(PageLoadStrategy.EAGER);
        options.addArguments("--window-size=1440,900", "--disable-gpu", "--no-sandbox",
                "--disable-dev-shm-usage",
                // No OS keyring lookups when a login form is submitted (they can
                // stall Chrome on Linux machines and containers without one).
                "--password-store=basic");
        // No "save password" or breached-password prompts after signing in.
        options.setExperimentalOption("prefs", Map.of(
                "credentials_enable_service", false,
                "profile.password_manager_enabled", false,
                "profile.password_manager_leak_detection", false));
        if (Config.headless()) {
            options.addArguments("--headless=new");
        }
        WebDriver driver = new ChromeDriver(options);
        DRIVER.set(driver);
        return driver;
    }

    public static WebDriver get() {
        WebDriver driver = DRIVER.get();
        if (driver == null) {
            throw new IllegalStateException("no browser started for this thread");
        }
        return driver;
    }

    public static boolean isStarted() {
        return DRIVER.get() != null;
    }

    public static void quit() {
        WebDriver driver = DRIVER.get();
        if (driver != null) {
            try {
                driver.quit();
            } finally {
                DRIVER.remove();
            }
        }
    }
}
