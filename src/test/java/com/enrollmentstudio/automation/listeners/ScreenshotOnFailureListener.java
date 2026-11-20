package com.enrollmentstudio.automation.listeners;

import com.enrollmentstudio.automation.driver.DriverFactory;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Saves a screenshot to target/screenshots when a test fails. */
public class ScreenshotOnFailureListener implements ITestListener {

    @Override
    public void onTestFailure(ITestResult result) {
        if (!DriverFactory.isStarted()) {
            return;
        }
        try {
            Path dir = Path.of("target", "screenshots");
            Files.createDirectories(dir);
            Path source = ((TakesScreenshot) DriverFactory.get()).getScreenshotAs(OutputType.FILE).toPath();
            String name = result.getTestClass().getRealClass().getSimpleName() + "_" + result.getName() + ".png";
            Files.copy(source, dir.resolve(name), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException | RuntimeException e) {
            System.err.println("could not save failure screenshot: " + e.getMessage());
        }
    }
}
