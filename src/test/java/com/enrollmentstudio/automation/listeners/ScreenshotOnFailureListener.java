package com.enrollmentstudio.automation.listeners;

import com.enrollmentstudio.automation.driver.DriverFactory;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.testng.ITestListener;
import org.testng.ITestResult;
import org.testng.Reporter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

/**
 * When a test fails: saves a screenshot to target/screenshots and embeds it
 * in TestNG's report (target/surefire-reports/emailable-report.html), under
 * the failed test.
 */
public class ScreenshotOnFailureListener implements ITestListener {

    @Override
    public void onTestFailure(ITestResult result) {
        if (!DriverFactory.isStarted()) {
            return;
        }
        try {
            byte[] png = ((TakesScreenshot) DriverFactory.get()).getScreenshotAs(OutputType.BYTES);
            Path dir = Path.of("target", "screenshots");
            Files.createDirectories(dir);
            String name = result.getTestClass().getRealClass().getSimpleName() + "_" + result.getName() + ".png";
            Files.write(dir.resolve(name), png);

            Reporter.setCurrentTestResult(result);
            Reporter.log("<p>Screen at failure:</p><img alt=\"Screen at failure\" style=\"max-width:900px\" src=\"data:image/png;base64,"
                    + Base64.getEncoder().encodeToString(png) + "\"/>");
        } catch (IOException | RuntimeException e) {
            System.err.println("could not save failure screenshot: " + e.getMessage());
        }
    }
}
