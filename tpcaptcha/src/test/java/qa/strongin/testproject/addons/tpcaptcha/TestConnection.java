package qa.strongin.testproject.addons.tpcaptcha;

import io.testproject.java.enums.AutomatedBrowserType;
import io.testproject.java.sdk.v2.Runner;
import io.testproject.java.sdk.v2.drivers.WebDriver;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import java.io.IOException;


public class TestConnection {

    private static Runner runner;

    @BeforeAll
    public static void setup() throws InstantiationException {
        runner = Runner.createWeb("hccSLNN6XWHtKpGzbLJFuELptr8pYinj2kgEFE9GTas", AutomatedBrowserType.Chrome);
    }

    @Test
    public void runAction() throws Exception {
        CaptchaBypasser action = new CaptchaBypasser();
        action.watsonAPIKey = "";
        action.watsonCloudURL = "";
        WebDriver driver = runner.getDriver();
        driver.navigate().to("https://www.google.com/recaptcha/api2/demo");
        runner.run(action, By.xpath("//div[@id='recaptcha-demo']"));
    }

    @AfterAll
    public static void tearDown() throws IOException {
        runner.close();
    }
}
