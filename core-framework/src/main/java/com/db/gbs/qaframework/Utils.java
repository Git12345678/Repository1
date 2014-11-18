package com.db.gbs.qaframework;


import com.sshtools.j2ssh.SshClient;
import com.sshtools.j2ssh.authentication.AuthenticationProtocolState;
import com.sshtools.j2ssh.authentication.PasswordAuthenticationClient;
import com.sshtools.j2ssh.session.SessionChannelClient;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;


/**
 * Implements the basic operations used by across different tests.
 * <p/>
 */
public class Utils {
    /**
     * Class logger
     */
    protected static Log logger = LogFactory.getLog(Utils.class);

    /**
     * @return the name of the class
     */
    public static String getMyClassName() {
        return new Exception().getStackTrace()[1].getClassName();
    }

    /**
     * Sleep a time
     *
     * @param seconds The numbers of seconds.
     */
    public static void wait(int seconds) {
        try {
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException e) {
            logger.error("Sleep error: " + e.getMessage());
        }
    }

    /**
     * Execute a command in a ssh connection
     *
     * @param hostname
     * @param username
     * @param password
     * @param command
     * @return true if the command is successfully executed
     */
    public static OutputCommand executeSshCommand(String hostname,
                                                  String username, String password, String command) {
        logger.debug("Opening SSH connection to '" + hostname + "' ("
                + username + "/" + password + ")");
        OutputCommand result = new OutputCommand(false, null, null);
        SshClient ssh = new SshClient();
        try {
            // Connection
            ssh.connect(hostname);
        } catch (IOException e) {
            logger.error("Couldn't open connection: " + e.getMessage());
            return result;
        }

        logger.debug("Connection stablished successfully");
        try {
            // Authentication
            PasswordAuthenticationClient auth =
                    new PasswordAuthenticationClient();
            auth.setUsername(username);
            auth.setPassword(password);
            int resultAuth = ssh.authenticate(auth);

            if (resultAuth != AuthenticationProtocolState.COMPLETE) {
                return result;
            }
            logger.debug("Authentication done successfuly");
            // Command
            logger.info("Executing command: " + command);
            SessionChannelClient session = ssh.openSessionChannel();
            session.executeCommand(command);
            int waitCommandEnd = 0;
            logger.debug("Waiting for EOF remote");
            while (session.isRemoteEOF() == false && waitCommandEnd < 10) {
                try {
                    logger.debug("Still trying");
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    logger.error("Sleep error: " + e.getMessage());
                }
                logger.debug("Continue incrementing");
                waitCommandEnd++;
            }
            logger.debug("EOF gotten");
            result.setResult(true);
            if (session.getExitCode() != null && session.getExitCode() == 0) {
                String sshOutput = "";
                int read;
                byte buffer[] = new byte[255];
                InputStream in = session.getInputStream();
                while (in.available() > 0 && (read = in.read(buffer)) > 0) {
                    String readLine = new String(buffer, 0, read);
                    sshOutput = sshOutput + readLine;
                }
                in.close();
                String outputString =
                        (sshOutput == null) ? "null" : sshOutput.replace('\r',
                                ' ').replace('\n', ' ');
                logger.debug("Command output: " + outputString);
                result.setOutput(sshOutput);
            } else {
                // Getting Standard Output and output from error
                InputStream in = session.getStderrInputStream();
                int read;
                byte buffer[] = new byte[255];

                String sshError = "";
                while (in.available() > 0 && (read = in.read(buffer)) > 0) {
                    String error = new String(buffer, 0, read);
                    sshError = sshError + error;
                }
                String errorString =
                        (sshError == null) ? "null" : sshError.replace('\r',
                                ' ').replace('\n', ' ');
                logger.error("Command error: " + errorString);
                result.setError(sshError);
            }

            String outputString =
                    (result.getOutput() == null) ? "null" : result.getOutput()
                            .replace('\r', ' ').replace('\n', ' ');
            String errorString =
                    (result.getError() == null) ? "null" : result.getError()
                            .replace('\r', ' ').replace('\n', ' ');
            logger.debug("Command exit code : " + session.getExitCode()
                    + " output : [" + outputString + "] error : ["
                    + errorString + "] ");

            // Disconnect
            ssh.disconnect();

        } catch (IOException e) {
            // Disconnect
            ssh.disconnect();
            logger.error("Couldn't execute the command: " + e.getMessage());
            result.setResult(false);
            return result;
        }
        return result;
    }

    public static ArrayList<String> executeSystemCommand(String command)
            throws IOException {
        ArrayList<String> cmdOutput = new ArrayList<String>();

        logger.debug("Executing system command: " + command);
        Process p = Runtime.getRuntime().exec(command);
        BufferedReader bri =
                new BufferedReader(new InputStreamReader(p.getInputStream()));
        String tmpLine = "";
        int line = 0;
        while ((tmpLine = bri.readLine()) != null) {
            logger.debug("Result " + line + ": " + tmpLine);
            line++;
            cmdOutput.add(tmpLine);
        }

        return cmdOutput;
    }

    /**
     * Open a database connection
     *
     * @param driverName
     * @param databaseUrl
     * @param username
     * @param password
     * @return the connection class
     */
    public static Connection openDBConnection(String driverName,
                                              String databaseUrl, String username, String password) {
        Connection connection = null;
        try {
            // Load the database driver
            Class.forName(driverName);

            // Create a connection to the database
            connection =
                    DriverManager
                            .getConnection(databaseUrl, username, password);
            return connection;
        } catch (ClassNotFoundException error) {
            logger.error("Database error: " + error.getMessage());
            return null;
            // Could not find the database driver
        } catch (SQLException error) {
            // Could not connect to the database
            logger.error("Database error: " + error.getMessage());
            return null;
        }
    }

    /**
     * Invoke a test method several times with the data of the excel file
     *
     * @param testClass
     * @param methodName
     * @param filePath
     */
    public static void excelTest(BasicTestCase testClass, String methodName,
                                 String filePath) {
        excelTest(testClass, methodName, filePath, 0, false);
    }

    /**
     * Invoke a test method several times with the data of the excel file
     *
     * @param testClass
     * @param methodName
     * @param filePath
     * @param sheetNumber
     * @param header      true if the first row is the header and has no data
     */
    public static void excelTest(BasicTestCase testClass, String methodName,
                                 String filePath, int sheetNumber, boolean header) {
        testClass.setExcelTest(true);
        ArrayList<String> errors =
                ExcelUtils.fileTestExecution(testClass, methodName, filePath,
                        sheetNumber, header, ' ', ' ');

        if (errors.size() > 0) {
            String errorMessage = (errors.size() == 1) ? " error" : " errors";
            logger.warn(errors.size() + errorMessage + " in the excel test");

            // Launching JUnit error
            TestCase.fail(errors.size() + errorMessage + ": "
                    + errors.toString());
        }
    }

    /**
     * Invoke a test method several times with the data of the csv file
     *
     * @param testClass
     * @param methodName
     * @param filePath
     * @param header     true if the first row is the header and has no data
     * @param separator  the delimiter to use for separating entries
     * @param quotechar  the character to use for quoted elements
     */
    public static void csvTest(BasicTestCase testClass, String methodName,
                               String filePath, boolean header, char separator, char quotechar) {
        testClass.setExcelTest(true);
        ArrayList<String> errors =
                ExcelUtils.fileTestExecution(testClass, methodName, filePath,
                        0, header, separator, quotechar);

        if (errors.size() > 0) {
            String errorMessage = (errors.size() == 1) ? " error" : " errors";
            logger.warn(errors.size() + errorMessage + " in the csv test");

            // Launching JUnit error
            TestCase.fail(errors.size() + errorMessage + ": "
                    + errors.toString());
        }
    }

    /**
     * Gets the absolute path of a resource file
     *
     * @param fileName
     * @return the absolute path of the file
     */
    public static String getResourcePath(String fileName) {
        URL url = (HtmlTestCase.class).getResource("/" + fileName);
        if (url == null) {
            logger.error("File '" + fileName
                    + "' not found in resources folder");
            return null;
        }

        String filePath = null;
        try {
            URI uri = new URI(url.getFile());
            filePath = uri.getPath();
        } catch (URISyntaxException e) {
            logger.error("Error getting the URI of '" + url.getFile() + "': "
                    + e.getMessage());
            return null;
        }

        // Change file separator in Windows
        if (File.separator.equals("\\")) {
            filePath = filePath.substring(1).replace("/", "\\");
        }
        logger.debug("Resource file path: " + filePath);

        return filePath;
    }

    /**
     * Copy a resource file and returns the absolute path of the new file
     *
     * @param fileName
     * @return the absolute path of the new file
     */
    public static String getTemporalResourcePath(String fileName) {
        InputStream in =
                (HtmlTestCase.class).getResourceAsStream("/" + fileName);
        if (in == null) {
            // Finding in class loader (jar)
            in =
                    (HtmlTestCase.class).getClassLoader().getResourceAsStream(
                            "/" + fileName);
            if (in == null) {
                logger.error("File '" + fileName
                        + "' not found in resources folder");
                return null;
            }
        }

        OutputStream out;
        String newFilePath;
        try {
            // Create temp file
            String[] fileNameSplitted = fileName.split("\\.");
            File temp =
                    File.createTempFile(fileNameSplitted[0], "."
                            + fileNameSplitted[fileNameSplitted.length - 1]);
            // Delete temp file when program exits
            temp.deleteOnExit();
            newFilePath = temp.getPath();

            logger.debug("Copying the resource file '" + fileName
                    + "' to a temporal file '" + newFilePath + "'");

            // Write to temp file
            out = new FileOutputStream(temp);
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
        } catch (FileNotFoundException e) {
            logger.error("Error saving the temporal file: " + fileName);
            return null;
        } catch (IOException e) {
            logger.error("Error saving the temporal file: " + fileName);
            return null;
        }

        return newFilePath;
    }

    /**
     * Open the mailinator mail of the user, search the last mail with the
     * subject and return the mail body
     *
     * @param testClass
     * @param username
     * @param subject
     * @param delete    true if the mail must be deleted after read it
     * @return the mail body
     * @throws Exception
     */
    public static WebElement getMailinatorMail(HtmlTestCase testClass,
                                               String username, String subject, boolean delete) throws Exception {
        logger.info("Getting mail at mailinator (user: " + username + ")");

        // Open a new webdriver
        WebDriver mailinatorDriver = testClass.createWebDriver();

        // Open the URL
        mailinatorDriver.get("http://" + username + "." + "mailinator.com");

        // Open the last mail with the subject
        List<WebElement> mails =
                mailinatorDriver.findElements(By.linkText(subject));
        logger.debug("Number of mails with the subject '" + subject + "': "
                + mails.size());
        if (mails.size() <= 0) {
            mailinatorDriver.quit();
            TestCase.fail("There are no mails with the subject '" + subject
                    + "'");
        }
        WebElement lastMail = mails.get(mails.size() - 1);
        lastMail.click();

        // Obtain the mail body
        WebDriverWait wait = new WebDriverWait(mailinatorDriver, 5);
        WebElement body =
                wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("message")));
        logger.debug("Mail body: " + body);

        if (delete) {
            logger.debug("Deleting mail");
            mailinatorDriver.findElement(By.name("Delete")).click();
        }

        // Stopping driver
        mailinatorDriver.quit();

        return body;
    }
}
