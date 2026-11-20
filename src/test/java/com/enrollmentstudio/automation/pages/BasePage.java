package com.enrollmentstudio.automation.pages;

import com.enrollmentstudio.automation.config.Config;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.List;

/** Shared waits and helpers. Pages locate elements by data-testid where the app provides one. */
public abstract class BasePage {

    protected final WebDriver driver;
    protected final WebDriverWait wait;

    protected BasePage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Config.explicitWait());
    }

    protected static By testId(String id) {
        return By.cssSelector("[data-testid='" + id + "']");
    }

    protected WebElement visible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    protected List<WebElement> allVisible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));
    }

    protected void click(By locator) {
        wait.until(ExpectedConditions.elementToBeClickable(locator)).click();
    }

    protected void type(By locator, String text) {
        WebElement field = visible(locator);
        field.clear();
        field.sendKeys(text);
    }

    protected String text(By locator) {
        return visible(locator).getText().trim();
    }

    protected boolean isPresent(By locator) {
        return !driver.findElements(locator).isEmpty();
    }

    protected void waitForPath(String path) {
        wait.until(d -> currentPath().equals(path));
    }

    public String currentPath() {
        String url = driver.getCurrentUrl();
        String withoutOrigin = url.replaceFirst("^https?://[^/]+", "");
        String path = withoutOrigin.split("[?#]")[0];
        return path.isEmpty() ? "/" : path;
    }

    /** The page's main heading. */
    public String heading() {
        return text(By.cssSelector("main h1, h1"));
    }
}
