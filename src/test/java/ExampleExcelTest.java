package test.java;

import main.java.HtmlTestCase;
import main.java.Utils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.PageFactory;
import pages.ExamplePage;

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

    public void testHere(String ExcelField1, String ExcelField2, String ExcelField3){
        ExamplePage examplePage = PageFactory.initElements(driver, ExamplePage.class);
        Actions ExampleAction = new Actions(driver);

        examplePage.DummyId.sendKeys(ExcelField1);
        ExampleAction.sendKeys(examplePage.DummyId, ExcelField2);
        waitForElementDisplayed(By.id(ExcelField3), 30);

    }

    @After
    public void quit(){
        driver.quit();
    }
}


