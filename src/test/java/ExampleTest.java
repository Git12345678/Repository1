package test.java;

import main.java.HtmlTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.PageFactory;
import pages.ExamplePage;

/**
 * Created by nelsjor on 30/09/2014.
 */
public class ExampleTest extends HtmlTestCase {

    @Before
    public void getURL() {
        driver.get("https://dummyUrl.com");
        ExamplePage examplePage = PageFactory.initElements(driver, ExamplePage.class);
    }

    @Test
    public void testHere(){
        ExamplePage examplePage = PageFactory.initElements(driver, ExamplePage.class);
        Actions ExampleAction = new Actions(driver);

        examplePage.DummyId.click();
        ExampleAction.moveToElement(examplePage.DummyId);
        waitForElementDisplayed(By.id("dummyId"), 30);

    }

    @After
    public void quit(){
        driver.quit();
    }
}
