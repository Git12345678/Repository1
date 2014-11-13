package com.db.gbs.qaframework;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.support.PageFactory;
import static org.junit.Assert.assertNotNull;

/**
 * Created by nelsjor on 30/09/2014.
 */
public class ExampleExcelTest extends HtmlTestCase {

    @Before
    public void getURL() {
        driver.get("https://dummyUrl.com");
        ExamplePage examplePage = PageFactory.initElements(driver, ExamplePage.class);
    }

    @Test
    public void ExcelDefinition(){
        String methodName = "createTest";
        String fileName = "DummyDataSet.xlsx";
        String filePath = Utils.getResourcePath(fileName);
        int sheetNumber = 0;
        boolean header = false;
        assertNotNull("File'" + fileName + "' not found in resources folder", filePath);
        Utils.excelTest(this, methodName, filePath, sheetNumber, header);
    }

    @After
    public void quit(){
        driver.quit();
    }
}


