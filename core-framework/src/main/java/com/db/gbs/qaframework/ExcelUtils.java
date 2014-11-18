package com.db.gbs.qaframework;


import au.com.bytecode.opencsv.CSVReader;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.FileReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;


/**
 * Implements the basic operations used by across different tests.
 * <p/>
 */
public class ExcelUtils {
    /**
     * Class logger
     */
    protected static Log logger = LogFactory.getLog(ExcelUtils.class);

    /**
     * Invoke a test method several times with the data of the csv/excel file
     *
     * @param testClass
     * @param methodName
     * @param file
     * @param sheetNumber [excel] the sheet number
     * @param header      true if the first row is the header and has no data
     * @param separator   [csv] the delimiter to use for separating entries
     * @param quotechar   [csv] the character to use for quoted elements
     * @return an array list with the error messages
     */
    public static ArrayList<String> fileTestExecution(BasicTestCase testClass,
                                                      String methodName, String filePath, int sheetNumber,
                                                      boolean header, char separator, char quotechar) {
        ArrayList<String> errors = new ArrayList<String>();
        ArrayList<ArrayList<String>> data =
                getFileData(filePath, sheetNumber, header, separator, quotechar);
        if (data == null) {
            String errorMessage = "Error reading the file '" + filePath + "'";
            errors.add(errorMessage);
            return errors;
        }
        logger.info("Iterator test: " + data.size() + " values");
        if (data.size() <= 0) {
            String errorMessage =
                    "There are no data in the sheet " + sheetNumber
                            + " of the file '" + filePath + "'";
            logger.error(errorMessage);
            errors.add(errorMessage);
            return errors;
        }

        int itrNumber = 1;
        Iterator<ArrayList<String>> iter = data.iterator();
        while (iter.hasNext()) {
            ArrayList<String> rowData = iter.next();
            int paramsSize = rowData.size();
            logger.info("Iteration " + itrNumber + " of " + data.size());

            /*
             * Class<?> testClass; try { testClass =
             * Class.forName(excelUtils.getPreviousClassName()); } catch
             * (ClassNotFoundException e) {
             * logger.error("ClassNotFoundException: " + e.getMessage());
             * fail("ClassNotFoundException"); return; }
             */

            Method test;
            try {
                Class<?>[] paramTypes = new Class[paramsSize];
                for (int i = 0; i < paramsSize; i++) {
                    paramTypes[i] = String.class;
                }

                test = testClass.getClass().getMethod(methodName, paramTypes);
            } catch (SecurityException e) {
                String errorMessage =
                        "Security error searching the method '" + methodName
                                + "'";
                logger.error(errorMessage + ": " + e.getMessage());
                errors.add(errorMessage);
                return errors;
            } catch (NoSuchMethodException e) {
                String errorMessage =
                        "The method name (" + methodName
                                + ") or the number of paramaters ("
                                + paramsSize + ") are wrong";
                logger.error(errorMessage + ": " + e.getMessage());
                errors.add(errorMessage);
                return errors;
            }

            try {
                Object[] params = new Object[paramsSize];
                for (int i = 0; i < paramsSize; i++) {
                    params[i] = rowData.get(i);
                }

                test.invoke(testClass, params);
            } catch (IllegalArgumentException e) {
                String errorMessage =
                        "Illegal argument invoking the method '" + methodName
                                + "'";
                logger.error(errorMessage + ": " + e.getMessage());
                errors.add(errorMessage);
                return errors;
            } catch (IllegalAccessException e) {
                String errorMessage =
                        "Illegal access invoking the method '" + methodName
                                + "'";
                logger.error(errorMessage + ": " + e.getMessage());
                errors.add(errorMessage);
                return errors;
            } catch (InvocationTargetException e) {
                // Saving the original exception
                Throwable cause = e.getCause();
                logger.warn("Error in row " + itrNumber + " of " + data.size()
                        + ": " + cause.getMessage());
                errors.add("Error in row " + itrNumber + " of " + data.size()
                        + ": " + cause.getMessage());
                if (testClass instanceof HtmlTestCase) {
                    ((HtmlTestCase) testClass).captureAndSaveErrorScreenshot();
                }
            }

            itrNumber++;
        }

        return errors;
    }

    /**
     * Reads a file and returns a matrix with the cell values
     *
     * @param filePath    File path
     * @param sheetNumber [excel] the sheet number
     * @param header      true if the first row is the header and has no data
     * @param separator   [csv] the delimiter to use for separating entries
     * @param quotechar   [csv] the character to use for quoted elements
     * @return the matrix with the file data
     */
    public static ArrayList<ArrayList<String>> getFileData(String filePath,
                                                           int sheetNumber, boolean header, char separator, char quotechar) {
        if (filePath.endsWith(".xls") || filePath.endsWith(".xlsx")) {
            return getExcelData(filePath, sheetNumber, header);
        } else if (filePath.endsWith(".csv")) {
            return getCsvData(filePath, header, separator, quotechar);
        } else {
            logger.error("Error reading the file '" + filePath
                    + "': the file format must be csv, xls or xlsx");
            return null;
        }
    }

    /**
     * Reads a csv file and returns a matrix with the cell values
     *
     * @param filePath  File path
     * @param header    true if the first row is the header and has no data
     * @param separator the delimiter to use for separating entries
     * @param quotechar the character to use for quoted elements
     * @return the matrix with the csv data
     */
    private static ArrayList<ArrayList<String>> getCsvData(String filePath,
                                                           boolean header, char separator, char quotechar) {
        ArrayList<ArrayList<String>> data = new ArrayList<ArrayList<String>>();
        logger.debug("Getting data from csv '" + filePath + "'");

        try {
            char escape = '\\';
            int line = header ? 1 : 0;

            CSVReader reader =
                    new CSVReader(new FileReader(filePath), separator,
                            quotechar, escape, line);
            String[] nextLine;
            while ((nextLine = reader.readNext()) != null) {
                ArrayList<String> rowData = new ArrayList<String>();
                for (String col : nextLine) {
                    rowData.add(col);
                }
                data.add(rowData);
            }
            reader.close();
        } catch (Exception e) {
            logger.error("Error reading the csv file '" + filePath + "': " + e);
            return null;
        }

        return data;
    }

    /**
     * Reads an excel file and returns a matrix with the cell values
     *
     * @param filePath    File path
     * @param sheetNumber
     * @param header      true if the first row is the header and has no data
     * @return the matrix with the excel data
     */
    private static ArrayList<ArrayList<String>> getExcelData(String filePath,
                                                             int sheetNumber, boolean header) {
        ArrayList<ArrayList<String>> data = new ArrayList<ArrayList<String>>();
        logger.debug("Getting data from excel '" + filePath + "', sheet "
                + sheetNumber);

        try {
            Workbook wb;
            if (filePath.endsWith(".xlsx")) {
                wb = new XSSFWorkbook(new FileInputStream(filePath));
            } else {
                wb = new HSSFWorkbook(new FileInputStream(filePath));
            }

            Sheet sheet = wb.getSheetAt(sheetNumber);
            Row row;
            Cell cell;

            int rows; // No of rows
            rows = sheet.getPhysicalNumberOfRows();
            int firstRow = 0;
            if (header) {
                firstRow = 1;
            }

            int cols = 0; // No of columns
            int tmp = 0;

            // This trick ensures that we get the data properly even if it
            // doesn't start from first few rows
            for (int i = firstRow; i < 10 || i < rows; i++) {
                row = sheet.getRow(i);
                if (row != null) {
                    tmp = sheet.getRow(i).getPhysicalNumberOfCells();
                    if (tmp > cols)
                        cols = tmp;
                }
            }

            for (int r = firstRow; r < rows; r++) {
                row = sheet.getRow(r);
                if (row != null) {
                    ArrayList<String> rowData = new ArrayList<String>();
                    for (int c = 0; c < cols; c++) {
                        cell = row.getCell(c);
                        if (cell != null) {
                            try {
                                rowData.add(cell.getStringCellValue());
                            } catch (Exception e) {
                                rowData.add(Double.toString(cell
                                        .getNumericCellValue()));
                            }
                        } else {
                            rowData.add("");
                        }
                    }
                    data.add(rowData);
                }
            }
        } catch (Exception e) {
            logger.error("Error reading the excel file '" + filePath + "': "
                    + e);
            return null;
        }
        return data;
    }
}


