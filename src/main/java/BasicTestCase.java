package main.java;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.MethodRule;
import org.junit.rules.TestName;
import org.junit.rules.TestWatchman;
import org.junit.runners.model.FrameworkMethod;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Implements a generic test case.
 * <p/>
 * Extends the SeleneseTestCase class.
 *
 */
//@RunWith(JiraTestRunner.class)
public class BasicTestCase {
    /**
     * Static class logger
     */
    protected static Log staticLogger = LogFactory.getLog(BasicTestCase.class);

    /**
     * Class logger
     */
    protected Log logger = LogFactory.getLog(this.getClass());

    public Log getLogger() {
        return logger;
    }

    /**
     * Jira Test Case configuration
     */
    private static String JIRA_EXECUTION_URL = "http://dbatlas.db.com/jira01";
    protected String jiraTestCaseId = "";
    protected String jiraComment = "";

    @Rule
    public TestName name = new TestName();

    /**
     * BasicTestCase Properties
     */
    protected static PropertiesManager properties;

    public static PropertiesManager getProperties() {
        if (properties == null) {
            // Initializing properties manager and loading file
            properties = new PropertiesManager();
        }
        return properties;
    }

    /**
     * Database connection *
     */
    protected static Connection dbconnection;

    public static Connection getDbconnection() {
        return dbconnection;
    }

    /**
     * Identify excel tests
     */
    private boolean excelTest = true;

    public boolean isExcelTest() {
        return excelTest;
    }

    public void setExcelTest(boolean excelTest) {
        this.excelTest = excelTest;
    }

    /**
     * Reads properties and starts Selenium server before each test
     */
    @Before
    public void basicSetUp() throws Exception {
        logger.debug("New TestCase: " + name.getMethodName());

        // Initializing properties manager
        getProperties();

        // Initializing the database
        initDatabase();
    }

    /**
     * Initializes the database
     */
    private void initDatabase() {
        if (dbconnection == null
                && properties.getBooleanProperty(properties.DATABASE_ENABLED)) {
            // Opening DB connection
            logger.debug("Opening database connection");
            String dbDriverName = properties
                    .getProperty(properties.DATABASE_DRIVERNAME);
            String dbUrl = properties.getProperty(properties.DATABASE_URL);
            String dbUsername = properties
                    .getProperty(properties.DATABASE_USERNAME);
            String dbPassword = properties
                    .getProperty(properties.DATABASE_PASSWORD);

            dbconnection = Utils.openDBConnection(dbDriverName, dbUrl,
                    dbUsername, dbPassword);
        }
    }

    /**
     * Close database and save properties
     */
    @After
    public void basicTearDown() throws Exception {
        // Storing properties
        properties.storeProperties();

        // Closing the database
        closeDatabase();
    }

    /**
     * Closes the database
     */
    private void closeDatabase() {
        // Closing DB connection
        if (dbconnection != null) {
            logger.debug("Closing database connection");
            try {
                dbconnection.close();
            } catch (SQLException e) {
                logger.error("Error closing database connection: "
                        + e.getMessage());
            }
            dbconnection = null;
        }
    }

    @SuppressWarnings("deprecation")
    @Rule
    public MethodRule basicWatchman = new TestWatchman() {
        @Override
        public void failed(Throwable e, FrameworkMethod method) {
            logger.error("The test has failed");

            // Changing Jira Status
            changeJiraStatus("Fail");
        }

        @Override
        public void succeeded(FrameworkMethod method) {
            logger.info("The test has succeeded");

            // Changing Jira Status
            changeJiraStatus("Pass");
        }
    };

    /**
     * Changes the Jira Status if it is enabled
     *
     * @param jiraStatus Fail or Pass
     */
    private void changeJiraStatus(String jiraStatus) {
        if (properties.getBooleanProperty(properties.JIRA_ENABLED)) {
            String fixVersion = properties
                    .getProperty(properties.JIRA_FIXVERSION);
            boolean onlyIfStatusChanges = properties
                    .getBooleanProperty(properties.JIRA_ONLYIFCHANGES);

            changeJiraStatus(jiraTestCaseId, jiraStatus, fixVersion,
                    jiraComment, onlyIfStatusChanges);
        }
    }

    /**
     * Changes the Jira Status of the Test Case
     *
     * @param jiraTestCaseId Test Case Id in Jira
     * @param jiraStatus     execution status: Fail or Pass
     * @param fixVersion     execution Fix Version
     * @param jiraComment    execution comment
     */
    public static void changeJiraStatus(String jiraTestCaseId,
                                        String jiraStatus, String fixVersion, String jiraComment) {
        changeJiraStatus(jiraTestCaseId, jiraStatus, fixVersion, jiraComment,
                false);
    }

    /**
     * Changes the Jira Status of the Test Case
     *
     * @param jiraTestCaseId  Test Case Id in Jira
     * @param jiraStatus      execution status: Fail or Pass
     * @param fixVersion      execution Fix Version
     * @param jiraComment     execution comment
     * @param onlyIfDifferent if true, create a new execution only if the status has changed
     */
    public static void changeJiraStatus(String jiraTestCaseId,
                                        String jiraStatus, String fixVersion, String jiraComment,
                                        boolean onlyIfStatusChanges) {
        if (jiraTestCaseId != null && !jiraTestCaseId.equals("")) {
            // Change the test case status
            try {
                String jiraUrl = JIRA_EXECUTION_URL + "jiraTestCaseId="
                        + jiraTestCaseId + "&jiraStatus=" + jiraStatus;
                if (jiraComment != null && !jiraComment.isEmpty()) {
                    jiraUrl = jiraUrl + "&comments="
                            + URLEncoder.encode(jiraComment, "UTF-8");
                }
                if (fixVersion != null && !fixVersion.isEmpty()) {
                    jiraUrl = jiraUrl + "&version="
                            + URLEncoder.encode(fixVersion, "UTF-8");
                }
                if (onlyIfStatusChanges == true) {
                    jiraUrl = jiraUrl + "&onlyIfStatusChanges=true";
                }
                URL url = new URL(jiraUrl);
                HttpURLConnection connection = (HttpURLConnection) url
                        .openConnection();

                // Check the HTTP response code
                Scanner s;
                if (connection.getResponseCode() != 200) {
                    // Get HTTP response
                    s = new Scanner(connection.getErrorStream());
                    s.useDelimiter("\\Z");
                    String response = s.next();
                    s.close();

                    // Extract error message from the HTTP response
                    Pattern p = Pattern.compile(".*<u>(.*)</u></p><p>.*");
                    Matcher m = p.matcher(response);
                    if (m.find()) {
                        response = m.group(1);
                    }
                    staticLogger.error("Test Case '" + jiraTestCaseId
                            + "' not changed: " + response);
                } else {
                    s = new Scanner(connection.getInputStream());
                    s.close();
                    staticLogger.debug("Test Case '" + jiraTestCaseId
                            + "' updated in Jira (" + jiraStatus + ")");
                }
            } catch (Throwable t) {
                staticLogger.error("Test Case '" + jiraTestCaseId
                        + "' not changed: " + t.getMessage());
            }
        }
    }
}
