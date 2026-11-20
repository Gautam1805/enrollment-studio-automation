package com.enrollmentstudio.automation.config;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Properties;

/**
 * Settings from config.properties, overridable by -D system properties.
 * Credentials come only from the environment.
 */
public final class Config {

    private static final Properties PROPS = load();

    private Config() {
    }

    private static Properties load() {
        Properties props = new Properties();
        try (InputStream in = Config.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (in == null) {
                throw new IllegalStateException("config.properties not found on the test classpath");
            }
            props.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("could not read config.properties", e);
        }
        return props;
    }

    private static String get(String key) {
        String value = System.getProperty(key, PROPS.getProperty(key));
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("missing setting: " + key);
        }
        return value.trim();
    }

    private static String env(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("set the " + name + " environment variable (see README)");
        }
        return value;
    }

    public static String baseUrl() {
        return get("baseUrl").replaceAll("/+$", "");
    }

    public static String browser() {
        return get("browser");
    }

    /** HEADLESS=true in the environment (CI, containers) wins over the file. */
    public static boolean headless() {
        String fromEnv = System.getenv("HEADLESS");
        return fromEnv != null ? Boolean.parseBoolean(fromEnv) : Boolean.parseBoolean(get("headless"));
    }

    public static Duration explicitWait() {
        return Duration.ofSeconds(Long.parseLong(get("explicitWaitSeconds")));
    }

    public static String username() {
        return env("ENROLL_USERNAME");
    }

    public static String password() {
        return env("ENROLL_PASSWORD");
    }
}
