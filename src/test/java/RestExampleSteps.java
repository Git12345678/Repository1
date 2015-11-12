import org.jbehave.core.annotations.Given;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;
import org.junit.Assert;

/**
 * Created by orln on 12/11/2015.
 */
public class RestExampleSteps {
    private ContactHelper _helper;
    public String message;
    public String URL;
    public int messageId;
    @Given("$URL and $message")
    public void name1(){
        _helper = new ContactHelper("site", "", "", "http://jsonplaceholder.typicode.com/");
        message = "Hola que assseeee";
        URL = "http://jsonplaceholder.typicode.com/";
    }

    @When("I browse the $URL and I send the $message")
    public void name2(){

    }

    @Then("I get the $messageId = ")
    public void name3(){
        Assert.assertEquals(messageId,3);
    }
}
