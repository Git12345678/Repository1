;
import org.jbehave.core.annotations.Given;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;
import org.junit.Assert;

/**
 * Created by orln on 11/11/2015.
 */
public class ExampleSteps {
    public int number1;
    public int number2;
    public int result;
    @Given("$number1 and $number2")
    public void name1(){
        number1 = 1;
        number2 = 2;
    }

    @When("I do the agregation of $number1 and $number2")
    public void name2(){
        result = Calculator.calculator(number1, number2);
    }

    @Then("I get the $Result = $number1 + $number2")
    public void name3(){
        Assert.assertEquals(result,3);
    }
}
