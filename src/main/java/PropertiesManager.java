package main.java;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

public class PropertiesManager {
    /**
     * Class logger
     */
    protected Log logger = LogFactory.getLog(this.getClass());

    /**
     * Properties file
     */
    private String propertiesFileName = "test.properties";

    /**
     * Properties object
     */
    private Properties prop;

    /**
     * Properties object
     */
    private boolean localChanges = false;

    /**
     * Properties names
     */
    public final String SERVER_HOST = "qa.server.host";
    public final String SERVER_PORT = "qa.server.port";
    public final String SCREENSHOTS_PATH = "qa.screenshots.path";
    public final String SCREENSHOTS_ENABLED = "qa.screenshots.enabled";
    public final String DATABASE_ENABLED = "qa.database.enabled";
    public final String DATABASE_DRIVERNAME = "qa.database.drivername";
    public final String DATABASE_URL = "qa.database.url";
    public final String DATABASE_USERNAME = "qa.database.username";
    public final String DATABASE_PASSWORD = "qa.database.password";
    public final String BROWSER = "qa.browser";
    public final String FIREFOX_PROFILE = "qa.firefox.profile";
    public final String FIREFOX_BIN = "qa.firefox.bin";
    public final String EXPLORER_SERVER_FILE = "qa.explorer.serverfile";
    public final String CHROME_SERVER_FILE = "qa.chromeserver.file";
    public final String ANDROID_SERVER_FILE = "qa.android.serverfile";
    public final String ANDROID_SDK_PATH = "qa.android.sdkpath";
    public final String URL = "qa.url";
    public final String USERNAME = "qa.username";
    public final String PASSWORD = "qa.password";
    public final String JSCOVERAGE_ENABLED = "qa.jscoverage.enabled";
    public final String JSCOVERAGE_OUTPUTFILE = "qa.jscoverage.outputFile";
    public final String JSCOVERAGE_SOURCEPATH = "qa.jscoverage.sourcePath";
    public final String JIRA_ENABLED = "qa.jira.enabled";
    public final String JIRA_FIXVERSION = "qa.jira.fixversion";
    public final String JIRA_COMMENT = "qa.jira.comment";
    public final String JIRA_ONLYIFCHANGES = "qa.jira.onlyifchanges";
    public final String TIMEOUT = "qa.timeout";
    public final String DRIVER_REUSE = "qa.driver.reuse";

    /**
     * Constructs a PropertiesManager with the default file
     */
    public PropertiesManager() {
        prop = loadProperties();
    }

    /**
     * Constructs a PropertiesManager with the specified file
     *
     * @param name prop file name.
     */
    public PropertiesManager(String newPropertiesFileName) {
        if (newPropertiesFileName != null && !newPropertiesFileName.equals("")) {
            propertiesFileName = newPropertiesFileName;
        }
        prop = loadProperties();
    }

    /**
     * Reads the test suite prop file and sets the prop in an object that's
     * going to be used by all tests
     *
     * @return the prop object or null
     */
    private Properties loadProperties() {
        Properties properties = new Properties();
        try {
            properties.load((HtmlTestCase.class).getResourceAsStream("/"
                    + propertiesFileName));
        } catch (Exception e) {
            logger.error("Error loading properties file '" + propertiesFileName
                    + "': " + e.getMessage());
            return null;
        }
        logger.info("Loaded properties file '" + propertiesFileName + "'");
        return properties;
    }

    /**
     * Store the prop object in the prop file
     *
     * @return true if success
     */
    public boolean storeProperties() {
        if (localChanges) {
            String filePath = Utils.getResourcePath(propertiesFileName);
            if (filePath == null) {
                logger.error("Error saving properties file '"
                        + propertiesFileName + "'");
                return false;
            }

            try {
                prop.store(new FileOutputStream(filePath), null);
            } catch (Exception e) {
                logger.error("Error saving properties file '" + filePath
                        + "': " + e.getMessage());
                return false;
            }
            localChanges = false;
            logger.info("Saved properties file '" + propertiesFileName + "'");
        }
        return true;
    }

    /**
     * Searches for the property with the specified key
     *
     * @param key property key
     * @return the property value
     */
    public String getProperty(String key) {
        // Get system property, readed from 'mvn -Dkey=value'
        String propertyValue = System.getProperty(key);
        if (propertyValue == null) {
            // Get environment property, readed from system
            propertyValue = System.getenv(key);
            if (propertyValue == null) {
                // Get local property, readed from test.properties file
                propertyValue = getPropertyNotNull(key);
            }
        }

        if (key.equals(SCREENSHOTS_PATH) || key.equals(JSCOVERAGE_OUTPUTFILE)
                || key.equals(FIREFOX_BIN) || key.equals(EXPLORER_SERVER_FILE)
                || key.equals(CHROME_SERVER_FILE)
                || key.equals(ANDROID_SERVER_FILE)
                || key.equals(ANDROID_SDK_PATH)) {
            // Replaces file separators of the properties that have local paths
            propertyValue = propertyValue.replace("/", File.separator);
        } else if (key.startsWith(URL)) {
            propertyValue = addAuthToUrl(propertyValue);
        }

        return propertyValue;
    }

    /**
     * Searches for the property with the specified key
     *
     * @param key property key
     * @return the property value or "" if the property does not exist
     */
    private String getPropertyNotNull(String key) {
        String property = prop.getProperty(key);
        String modifiedProperty = (property == null) ? "" : property.trim();
        try {
            // Properties.load() uses the ISO 8859-1 character encoding with
            // unicode escapes
            //logger.debug("Property value pre: " + modifiedProperty);
            byte[] utf8 = modifiedProperty.getBytes("ISO-8859-1");
            modifiedProperty = new String(utf8, "UTF-8");
            //logger.debug("Property value: " + modifiedProperty);
        } catch (UnsupportedEncodingException e) {
            logger.warn("UnsupportedEncodingException with this property value: "
                    + modifiedProperty);
        }

        return modifiedProperty;
    }

    /**
     * Searches for the boolean property with the specified key
     *
     * @param key property key
     * @return the property value
     */
    public boolean getBooleanProperty(String key) {
        boolean propertyValue = false;
        String stringPropertyValue = getProperty(key);
        if (stringPropertyValue.equals("true")) {
            propertyValue = true;
        }
        return propertyValue;
    }

    /**
     * Sets the value in the property with the specified key
     *
     * @param key   property key
     * @param value property value
     * @return the previous property value
     */
    public Object setProperty(String key, String value) {
        localChanges = true;
        return prop.setProperty(key, value);
    }

    /**
     * Get name_version_os of the configured browser
     *
     * @return the name, version and OS of the browser
     */
    public String getBrowserInfo() {
        String browser = getProperty(BROWSER);

        // The browser info has name, version and OS of the browser variable
        // i.e. qa.browser = firefox C:\firefox.exe -> browserInfo = firefox
        // i.e. qa.browser = firefox -> browserInfo = firefox
        // i.e. qa.browser = firefox-4.0-on-windows_7 -> browserInfo =
        // firefox_4.0_windows_7
        String[] browserSplit = browser.split(" ")[0].split("-");
        String browserInfo = browserSplit[0];
        if (browserSplit.length > 3) {
            browserInfo += "_" + browserSplit[1] + "_" + browserSplit[3];
        }

        return browserInfo;
    }

    /**
     * Get the name of the configured browser
     *
     * @return the name of the browser
     */
    public String getBrowserName() {
        String browser = getProperty(BROWSER);

        // The browser name is the first part of the browser variable
        // i.e. qa.browser = firefox C:\firefox.exe -> browserName = firefox
        // i.e. qa.browser = firefox -> browserName = firefox
        // i.e. qa.browser = firefox-4.0-on-windows_7 -> browserName = firefox
        return browser.split(" ")[0].split("-")[0];
    }

    /**
     * Get the version of the configured browser
     *
     * @return the version of the browser or null if it is not specified
     */
    public String getBrowserVersion() {
        String browser = getProperty(BROWSER);

        // The browser version is the second part of the browser variable
        // i.e. qa.browser = firefox C:\firefox.exe -> version = null
        // i.e. qa.browser = firefox -> version = null
        // i.e. qa.browser = firefox-4.0-on-windows_7 -> version = 4.0
        String[] browserSplit = browser.split(" ")[0].split("-");
        String version = null;
        if (browserSplit.length > 1) {
            version = browserSplit[1];
        }
        return version;
    }

    /**
     * Get the platform of the configured browser
     *
     * @return the platform of the browser or null if it is not specified
     */
    public String getBrowserPlatform() {
        String browser = getProperty(BROWSER);

        // The browser platform is the fourth part of the browser variable
        // i.e. qa.browser = firefox C:\firefox.exe -> platform = null
        // i.e. qa.browser = firefox -> platform = null
        // i.e. qa.browser = firefox-4.0-on-windows_7 -> platform = windows 7
        String[] browserSplit = browser.split(" ")[0].split("-");
        String platform = null;
        if (browserSplit.length > 3) {
            platform = browserSplit[3].replace("_", " ");
        }
        return platform;
    }

    /**
     * Adds the HTTP Basic Authentication to the url
     *
     * @return the composed url
     */
    private String addAuthToUrl(String url) {
        // Adding username and password to the url
        String username = getProperty(USERNAME);
        String password = getProperty(PASSWORD);
        if (username != null && password != null && !username.equals("")
                && !password.equals("")) {
            String[] urlSplit = url.split("http://");
            if (urlSplit.length == 2) {
                url = "http://" + username + ":" + password + "@" + urlSplit[1];
            } else {
                urlSplit = url.split("https://");
                if (urlSplit.length == 2) {
                    url =
                            "https://" + username + ":" + password + "@"
                                    + urlSplit[1];
                }
            }
        }
        return url;
    }
}
