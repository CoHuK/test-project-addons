package qa.strongin.testproject.addons.tp2fa;

import org.openqa.selenium.WebElement;

import io.testproject.java.annotations.v2.Action;
import io.testproject.java.annotations.v2.Parameter;
import io.testproject.java.enums.ParameterDirection;
import io.testproject.java.sdk.v2.addons.WebElementAction;
import io.testproject.java.sdk.v2.addons.helpers.WebAddonHelper;
import io.testproject.java.sdk.v2.enums.ExecutionResult;
import io.testproject.java.sdk.v2.exceptions.FailureException;

import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;

@Action(name = "Generate 2FA TOTP", description = "Generate TOTP based on {{secret}} and input it")
public class TOTPGenerator implements WebElementAction {

    @Parameter
    public String secret;

    @Parameter(direction = ParameterDirection.OUTPUT)
    public String totpCode;

    public ExecutionResult execute(WebAddonHelper helper, WebElement element) throws FailureException {
        element = helper.getDriver().findElement(helper.getSearchCriteria());

        TimeProvider timeProvider = new SystemTimeProvider();
        CodeGenerator codeGenerator = new DefaultCodeGenerator();
        totpCode = "Failed to generate code";
        long currentBucket = Math.floorDiv(timeProvider.getTime(), 30);
        try {
            totpCode = codeGenerator.generate(secret, currentBucket);
        } catch (Exception e) {
            throw new FailureException("Failed to generate TOTP code");
        }
        helper.getReporter().result("Generated OTP is: " + totpCode);
        
        element.sendKeys(totpCode);

        return ExecutionResult.PASSED;
    }
}
