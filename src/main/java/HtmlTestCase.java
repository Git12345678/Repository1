package main.java;

import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.openqa.selenium.*;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.*;
import org.openqa.selenium.safari.SafariDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.WebDriverWait;
import com.gargoylesoftware.htmlunit.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Set;
import java.util.concurrent.TimeUnit;


/**
 * Implements a generic test case for Html Selenium tests.
 * <p/>
 * Extends the SeleneseTestCase class.
 */
public class HtmlTestCase extends BasicTestCase {
    /**
     * Static class logger
     */
    protected static Log staticLogger = LogFactory.getLog(HtmlTestCase.class);

    /**
     * Class logger
     */
    protected Log logger = LogFactory.getLog(this.getClass());

    @Override
    public Log getLogger() {
        return logger;
    }

    /**
     * Screenshots
     */
    private static String screenshotsPath;
    private static int screenshotsNumber;
    private File screenshotFile = null;

    /**
     * WebDriver instance
     */
    public static WebDriver driver;

    public WebDriver getDriver() {
        return driver;
    }

    public void setDriver(WebDriver driver) {
        HtmlTestCase.driver = driver;
    }

    /**
     * Starts Selenium server before each test
     */
    @Before
    public void htmlSetUp() throws Exception {
        // Return if the driver is running
        if (properties.getBooleanProperty(properties.DRIVER_REUSE)
                && HtmlTestCase.driver != null) {
            return;
        }

        // Initializing screenshots configuration
        initScreenshots();

        // Initializing WebDriver instance
        HtmlTestCase.driver = createWebDriver();

        // Implicit wait
        String timeout = properties.getProperty(properties.TIMEOUT);
        if (!timeout.isEmpty()) {
            logger.debug("Implicit wait of '" + timeout + "' seconds");
            int seconds = new Integer(timeout);
            driver.manage().timeouts()
                    .implicitlyWait(seconds, TimeUnit.SECONDS);
        }

        // Open the URL
        String initialUrl = properties.getProperty(properties.URL);
        if (!initialUrl.equals("")) {
            logger.debug("URL: " + initialUrl);
            driver.get(initialUrl);

            // Skipping IE security alert
            skipIESecurityAlert();
        }

        // Maximizing the window
        windowMaximize();
    }

    /**
     * Initializes the screenshots configuration
     */
    private void initScreenshots() {
        if (screenshotsPath != null) {
            return;
        }

        // Unique screenshots directory
        String DATE_FORMAT_NOW = "yyyy-MM-dd_HHmmss";
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
        Calendar cal = Calendar.getInstance();
        String date = sdf.format(cal.getTime());
        String browserInfo = properties.getBrowserInfo();
        screenshotsPath =
                properties.getProperty(properties.SCREENSHOTS_PATH) + date
                        + "_" + browserInfo + File.separator;
        screenshotsNumber = 1;
    }

    /**
     * Creates and returns a WebDriver instance corresponding to the browser
     *
     * @return the WebDriver object
     * @throws Exception
     */
    public WebDriver createWebDriver() throws Exception {
        WebDriver driver = null;
        String serverHost = properties.getProperty(properties.SERVER_HOST);
        String serverPort = properties.getProperty(properties.SERVER_PORT);

        if (!serverHost.isEmpty() && !serverPort.isEmpty()) {
            driver = createRemoteWebDriver(serverHost, serverPort);
        } else {
            driver = createLocalWebDriver();
        }
        return driver;
    }

    /**
     * Creates and returns a remote WebDriver instance corresponding to the
     * browser
     *
     * @param serverPort
     * @param serverHost
     * @return the WebDriver object
     * @throws Exception
     */
    private WebDriver createRemoteWebDriver(String serverHost, String serverPort)
            throws Exception {
        String loadingMessage = "Loading remote driver ";
        loadingMessage += "'" + serverHost + ":" + serverPort + "' (";

        DesiredCapabilities capabilities = null;
        String browserName = properties.getBrowserName();
        if (browserName.equals("firefox")) {
            FirefoxProfile prf = new FirefoxProfile();
            prf.setPreference("dom.max_chrome_script_run_time", 60);
            prf.setAcceptUntrustedCertificates(true);
            prf.setEnableNativeEvents(true);
            capabilities = DesiredCapabilities.firefox();
            capabilities.setCapability("firefox.profile", prf);
        } else if (browserName.equals("iexplore")) {
            capabilities = DesiredCapabilities.internetExplorer();
            // Selenium Grid uses 'internet explorer' instead of 'iexplore'
            browserName = "internet explorer";
        } else if (browserName.equals("chrome")) {
            capabilities = DesiredCapabilities.chrome();
            capabilities.setCapability("chrome.binary", "%SELENIUM_DRIVERS%\\chrome.exe");
            ChromeOptions chromeOptions = new ChromeOptions();
            chromeOptions.addArguments("start-maximized");
            chromeOptions.addArguments("ignore-certificate-errors");
            // Add auth extension to allow basic authentication in Chrome 19+
            //chromeOptions.addExtensions(new File(TidUtils
            //        .getTemporalResourcePath("ChromeHTTPAuth.crx")));

            capabilities.setCapability(ChromeOptions.CAPABILITY, chromeOptions);
        } else if (browserName.equals("safari")) {
            capabilities = DesiredCapabilities.safari();
        } else if (browserName.equals("opera")) {
            capabilities = DesiredCapabilities.opera();
            capabilities.setCapability("opera.autostart", true);
            capabilities.setCapability("opera.arguments", "-fullscreen");
        } else {
            String errorMessage =
                    "There are no remote drivers for the browser '"
                            + browserName + "'";
            logger.error(errorMessage);
            throw new Exception(errorMessage);
        }
        capabilities.setBrowserName(browserName);
        loadingMessage += "browser: " + browserName;

        String version = properties.getBrowserVersion();
        if (version != null && !version.isEmpty()) {
            capabilities.setVersion(version);
            loadingMessage += ", version: " + version;
        }

        String platform = properties.getBrowserPlatform();
        if (platform != null && !platform.isEmpty()) {
            capabilities.setPlatform(Platform.extractFromSysProperty(platform));
            loadingMessage += ", platform: " + platform;
        }
        loadingMessage += ")";

        String urlServerString =
                "http://" + serverHost + ":" + serverPort + "/wd/hub";
        URL urlServer = new URL(urlServerString);
        logger.debug(loadingMessage);

        // RemoteWebDriver with upload file capability
        RemoteWebDriver remoteWebDriver =
                new RemoteWebDriver(urlServer, capabilities);
        remoteWebDriver.setFileDetector(new LocalFileDetector());

        // Added the capability to capture screenshots
        WebDriver augmentedDriver = new Augmenter().augment(remoteWebDriver);
        return augmentedDriver;
    }

    /**
     * Creates and returns a local WebDriver instance corresponding to the
     * browser
     *
     * @return the WebDriver object
     * @throws Exception
     */
    @SuppressWarnings("deprecation")
    private WebDriver createLocalWebDriver() throws Exception {
        WebDriver driver = null;

        String browserName = properties.getBrowserName();
        logger.debug("Loading local driver (browser: " + browserName + ")");
        if (browserName.equals("firefox")) {
            // Firefox binary path
            String firefoxBin = properties.getProperty(properties.FIREFOX_BIN);
            if (!firefoxBin.equals("")) {
                logger.debug("Openning firefox from: " + firefoxBin);
                System.setProperty("webdriver.firefox.bin", firefoxBin);
            }

            // Firefox profile file
            String firefoxProfile =
                    properties.getProperty(properties.FIREFOX_PROFILE);
            if (!firefoxProfile.equals("")) {
                logger.debug("Using this firefox profile: " + firefoxProfile);
                System.setProperty("webdriver.firefox.profile", firefoxProfile);
            }

            FirefoxProfile prf = new FirefoxProfile();
            prf.setPreference("dom.max_chrome_script_run_time", 60);
            //TODO Nuevo parametro para decidir si activarlo o no
            //Default: true
            //Con false fallan los untrusted
            //prf.setAssumeUntrustedCertificateIssuer(false);
            prf.setAcceptUntrustedCertificates(true);
            prf.setEnableNativeEvents(true);
            driver = new FirefoxDriver(prf);
        } else if (browserName.equals("iexplore")) {
            String explorerServerFile =
                    properties.getProperty(properties.EXPLORER_SERVER_FILE);
            if (!explorerServerFile.isEmpty()) {
                logger.debug("Starting explorer driver (explorerServerFile: "
                        + explorerServerFile + ")");
                System.setProperty("webdriver.ie.driver", explorerServerFile);
            }
            driver = new InternetExplorerDriver();
        } else if (browserName.equals("chrome")) {
            ChromeOptions chromeOptions = new ChromeOptions();
            chromeOptions.addArguments("start-maximized");
            chromeOptions.addArguments("ignore-certificate-errors");
            // Add auth extension to allow basic authentication in Chrome 19+
            //chromeOptions.addExtensions(new File(TidUtils
            //        .getTemporalResourcePath("ChromeHTTPAuth.crx")));

            /*if (SeleniumServer.getChromeService() != null) {
                logger.debug("Getting chrome driver from chrome service");
                // Chrome server already started
                DesiredCapabilities capabilities = DesiredCapabilities.chrome();
                capabilities.setCapability(ChromeOptions.CAPABILITY,
                        chromeOptions);
                driver =
                        new Augmenter().augment(new RemoteWebDriver(
                                SeleniumServers.getChromeService().getUrl(),
                                capabilities));
                } 
              else {
                String chromeServerFile =
                        properties.getProperty(properties.CHROME_SERVER_FILE);
                logger.debug("Starting chrome driver (chromeServerFile: "
                        + chromeServerFile + ")");
                System.setProperty("webdriver.chrome.driver", chromeServerFile);
                driver = new ChromeDriver(chromeOptions);
             }*/

        } else if (browserName.equals("safari")) {
            driver = new SafariDriver();
        /*} else if (browserName.equals("opera")) {
            DesiredCapabilities capabilities = DesiredCapabilities.opera();
            capabilities.setCapability("opera.autostart", true);
            capabilities.setCapability("opera.arguments", "-fullscreen");
            driver = new OperaDriver(capabilities);
        } else if (browserName.equals("android")) {
            DesiredCapabilities capabilities = DesiredCapabilities.android();
            capabilities.setCapability(CapabilityType.ACCEPT_SSL_CERTS, true);
            driver = new AndroidDriver(capabilities);
        } else if (browserName.equals("iphone")) {
            try {
                driver = new IPhoneDriver();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }*/
        } else if (browserName.equals("htmlunit")) {
            driver = new HtmlUnitDriver(true);
        } else {
            String errorMessage =
                    "There are no drivers for the browser '" + browserName
                            + "'";
            logger.error(errorMessage);
            throw new Exception(errorMessage);
        }
        return driver;
    }

    /**
     * Skip IE security alert
     */
    public void skipIESecurityAlert() {
        String browserName = properties.getBrowserName();
        if (browserName.equals("iexplore")
                && driver.getPageSource().contains("id=overridelink")) {
            driver.navigate()
                    .to("javascript:document.getElementById('overridelink').click()");
            logger.info("Explorer security link clicked");
        }
    }

    /**
     * Clicks on an element using sendKeys() in Explorer and click() in other
     * browsers
     *
     * @param element web element object
     */
    public void customClick(WebElement element) {
        String browserName = properties.getBrowserName();
        if (browserName.equals("iexplore")) {
            element.sendKeys("\n");
        } else {
            element.click();
        }
    }

    /**
     * Maximizes the window
     * <p/>
     * Bug: http://code.google.com/p/selenium/issues/detail?id=174
     */
    private void windowMaximize() {
        String browserName = properties.getBrowserName();
        if (browserName.equals("iexplore") || browserName.equals("firefox")
                || browserName.equals("safari")) {
            logger.debug("Maximizing the window");

            // Get the available screen size
            JavascriptExecutor js = (JavascriptExecutor) driver;
            Long availWidth =
                    (Long) js.executeScript("return window.screen.availWidth;");
            Long availHeight =
                    (Long) js
                            .executeScript("return window.screen.availHeight;");

            // Move the window to position 0,0
            driver.manage().window().setPosition(new Point(0, 0));

            // Resize the window to the screen width/height
            driver.manage()
                    .window()
                    .setSize(
                            new Dimension(availWidth.intValue(), availHeight
                                    .intValue()));
        } else if (browserName.equals("opera") || browserName.equals("chrome")) {
            // Window maximized with browser capabilities
        } else {
            logger.warn("WindowMaximize function not available in '"
                    + browserName + "'");
        }
    }

    /**
     * Stop selenium browser after test execution
     */
    @After
    public void htmlTearDown() throws Exception {
        // Capturing the last screenshot
        if (!isExcelTest()) {
            captureScreenshot();
        }

        // Stopping driver
        if (!properties.getBooleanProperty(properties.DRIVER_REUSE)) {
            driver.quit();
        }
    }


    /**
     * Captures and saves a PNG screenshot if the 'screenshot_enable' property
     * is true
     *
     * @param fileSuffix string added to the file name
     */
    public void captureAndSaveScreenshot(String fileSuffix) {
        if (properties.getBooleanProperty(properties.SCREENSHOTS_ENABLED)) {
            captureScreenshot();
            saveScreenshot(fileSuffix);
        }
    }

    /**
     * Captures and saves an error screenshot
     */
    public void captureAndSaveErrorScreenshot() {
        captureScreenshot();
        saveScreenshot("error");
    }

    /**
     * Captures a PNG screenshot
     */
    // TODO Eliminar funcion si al final se captura igual con todos los
    // navegadores
    private void captureScreenshot() {
        String browserName = properties.getBrowserName();
        if (browserName.equals("firefox")) {
            captureScreenshotFirefox();
        } else if (browserName.equals("iexplore")) {
            captureScreenshotFirefox();
        } else if (browserName.equals("chrome")) {
            captureScreenshotFirefox();
        } else if (browserName.equals("opera")) {
            captureScreenshotFirefox();
        } else {
            logger.warn("Couldn't capture the screenshot with this browser '"
                    + browserName + "'");
        }
    }

    /**
     * Captures a PNG screenshot in Firefox
     *
     * @return true if there are no errors
     */
    private boolean captureScreenshotFirefox() {
        // Capture the screenshot
        try {
            screenshotFile =
                    ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
        } catch (java.lang.ClassCastException e) {
            logger.error("Couldn't capture the screenshot: " + e.getMessage());
            return false;
        } catch (WebDriverException e) {
            logger.error("Couldn't capture the screenshot: " + e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * Captures a PNG screenshot in Explorer
     *
     * @return true if there are no errors
     */
    @SuppressWarnings("unused")
    private boolean captureScreenshotExplorer() {
        // Capture the screenshot
        Rectangle captureSize =
                new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
        Robot robot;
        try {
            robot = new Robot();
        } catch (AWTException e) {
            logger.error("Couldn't capture the screenshot: " + e.getMessage());
            return false;
        }
        BufferedImage bufferedImage = robot.createScreenCapture(captureSize);

        if (screenshotsNumber == 1) {
            // Create the screenshots folder the first time
            File f = new File(screenshotsPath);
            if (!f.mkdirs()) {
                logger.error("Error creating folder: " + screenshotsPath);
            }
        }

        // Create temporary file.
        File tempfile;
        try {
            tempfile = File.createTempFile("screenshot", ".png");
            tempfile.deleteOnExit();
        } catch (IOException e) {
            logger.error("Couldn't save the screenshot in a temporal file: "
                    + e.getMessage());
            return false;
        }

        // Save the screenshot
        try {
            ImageIO.write(bufferedImage, "png", tempfile);
        } catch (IOException e) {
            logger.error("Couldn't save the screenshot in a temporal file: "
                    + e.getMessage());
            return false;
        }
        screenshotFile = tempfile;
        return true;
    }

    /**
     * Saves the last screenshot to a file
     *
     * @param fileSuffix string added to the file name
     * @return true if there are no errors
     */
    private boolean saveScreenshot(String fileSuffix) {
        if (screenshotFile == null) {
            logger.error("There are no screenshots to save");
            return false;
        }

        // Screenshot file name
        String browserInfo = properties.getBrowserInfo();
        String path =
                screenshotsPath + String.format("%02d", screenshotsNumber)
                        + "_" + browserInfo + "_" + name.getMethodName();
        if (fileSuffix != null) {
            path = path + "_" + fileSuffix;
        }
        path = path + ".png";

        // Save the screenshot
        try {
            FileUtils.copyFile(screenshotFile, new File(path));
        } catch (Exception e) {
            logger.error("Couldn't save the screenshot " + path + ": "
                    + e.getMessage());
            return false;
        }

        if (fileSuffix != null && fileSuffix.compareTo("error") == 0) {
            logger.warn("Saved screenshot " + path);
        } else {
            logger.debug("Saved screenshot " + path);
        }
        screenshotsNumber++;
        screenshotFile = null;
        return true;
    }

    /**
     * Wait until an element is present in web page
     *
     * @param by
     * @param seconds
     * @return the web element
     */
    public static WebElement waitForElementPresent(By by, int seconds) {
        WebDriverWait wait = new WebDriverWait(driver, seconds);
        return wait.until(ExpectedConditions.visibilityOfElementLocated(by));
    }

    // Segunda opcion para esperar a que se cargue un elemento
    // Activar la espera implicita con su timeout (en vez de dar
    // error si un elemento no existe, espera a que se cargue),
    // buscar el elemento y desactivar la espera implicita
    @SuppressWarnings("unused")
    private WebElement implicitWaitForElementPresent(By by, int seconds) {
        driver.manage().timeouts().implicitlyWait(seconds, TimeUnit.SECONDS);
        WebElement element = driver.findElement(by);
        driver.manage().timeouts().implicitlyWait(0, TimeUnit.SECONDS);
        return element;
    }

    /**
     * Wait until an element is not present in web page
     *
     * @param by
     * @param seconds
     * @return true if it is not present
     */
    public static WebElement waitForElementNotPresent(By by, int seconds) {
        WebDriverWait wait = new WebDriverWait(driver, seconds);
        return wait.until(ExpectedConditions.visibilityOfElementLocated(by));
    }

    /**
     * Wait until an element is present and displayed in web page
     *
     * @param by
     * @param seconds
     * @return the web element
     */
    public static WebElement waitForElementDisplayed(By by, int seconds) {
        WebDriverWait wait = new WebDriverWait(driver, seconds);
        return wait.until(ExpectedConditions.visibilityOfElementLocated(by));
    }

    /**
     * Wait until an element is clickable in web page
     *
     * @param by
     * @param seconds
     * @return the web element
     */
    public WebElement waitForElementClickable(By by, int seconds) {
        WebDriverWait wait = new WebDriverWait(driver, seconds);
        return wait.until(ExpectedConditions.elementToBeClickable(by));
    }

    /**
     * Wait until an alert is present in web page
     *
     * @param seconds
     * @return the alert object
     */
    public Alert waitForAlertPresent(int seconds) {
        FluentWait<WebDriver> wait =
                new WebDriverWait(driver, seconds).ignoring(
                        NullPointerException.class).ignoring(
                        ClassCastException.class);
        return wait.until(ExpectedConditions.alertIsPresent());
    }

    /**
     * Wait until an alert is present in web page, verify the message and
     * confirm the alert
     *
     * @param message
     * @param seconds
     */
    public void waitAndConfirmAlert(String message, int seconds) {
        Alert alert = waitForAlertPresent(seconds);
        String alertMessage = alert.getText();
        Assert.assertTrue("The alert message '" + alertMessage
                + "' should be '" + message + "'", alertMessage.equals(message));
        alert.accept();
    }

    /**
     * Verify if a text is present in web page
     *
     * @param text text to find
     */
    public void verifyText(String text) {
        logger.debug("Verifying that the text '" + text
                + "' is present on the page");
        if (!driver.findElement(By.tagName("html")).getText().contains(text)) {
            TestCase.fail("Text '" + text + "' not found in web page");
        }
        return;
    }

    /**
     * Verify if a text is not present in web page
     *
     * @param text text to find
     */
    public void verifyNonText(String text) {
        logger.debug("Verifying that the text '" + text
                + "' is not present on the page");
        if (driver.findElement(By.tagName("html")).getText().contains(text)) {
            TestCase.fail("Text '" + text + "' found in web page");
        }
        return;
    }

    /**
     * Looks for a window that contains the given string in its url and switches
     * to that window
     *
     * @param windowUrl string to be found in the window url
     * @throws Exception if the window was not found
     */
    public void switchTo(String windowUrl) throws Exception {
        logger.info("Switching to the window with the url '" + windowUrl + "'");
        boolean windowFound = false;
        Set<String> handlers = driver.getWindowHandles();
        if (driver.getWindowHandles().size() >= 1) {
            for (String handler : handlers) {
                driver.switchTo().window(handler);
                logger.debug("Window handler: " + handler);
                logger.debug("Window url: " + driver.getCurrentUrl());
                if (driver.getCurrentUrl().contains(windowUrl)) {
                    logger.info("Window found");
                    windowFound = true;
                    break;
                }
            }
            if (!windowFound) {
                String message =
                        "Window with url '" + windowUrl + "' not found";
                logger.error(message);
                throw new Exception(message);
            }
        } else {
            String message = "There are no windows opened";
            logger.error(message);
            throw new Exception(message);
        }
    }

    /**
     * Opens the url in a new window
     *
     * @param url
     * @param windowName
     */
    public void openWindow(String url, String windowName) {
        String jsCommand = "window.open('" + url + "', '" + windowName + "');";
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript(jsCommand, "");
        driver.switchTo().window(windowName);
    }

    public void moveTo(WebElement we){
        Actions action = new Actions(driver);
        action.moveToElement(we).build().perform();
    }
}
