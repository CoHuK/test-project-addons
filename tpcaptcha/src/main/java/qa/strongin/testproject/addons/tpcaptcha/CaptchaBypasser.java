package qa.strongin.testproject.addons.tpcaptcha;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import com.ibm.cloud.sdk.core.http.Response;
import com.ibm.cloud.sdk.core.http.ServiceCall;
import com.ibm.cloud.sdk.core.security.IamAuthenticator;
import com.ibm.watson.speech_to_text.v1.SpeechToText;
import com.ibm.watson.speech_to_text.v1.model.RecognizeOptions;
import com.ibm.watson.speech_to_text.v1.model.SpeechRecognitionResults;

import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;

import io.testproject.java.annotations.v2.Action;
import io.testproject.java.annotations.v2.Parameter;
import io.testproject.java.sdk.v2.addons.WebElementAction;
import io.testproject.java.sdk.v2.addons.helpers.WebAddonHelper;
import io.testproject.java.sdk.v2.drivers.WebDriver;
import io.testproject.java.sdk.v2.enums.ExecutionResult;
import io.testproject.java.sdk.v2.exceptions.FailureException;


@Action(name = "Bypass Captcha", description = "Bypass captcha in provided iframe")
public class CaptchaBypasser implements WebElementAction {
    private int TIMEOUT = 1000;
    private int DEFAULT_TIMEOUT;

    @Parameter
    public String watsonAPIKey;

    @Parameter
    public String watsonCloudURL;

    public ExecutionResult execute(WebAddonHelper helper, WebElement element) throws FailureException {
        Boolean audioButtonExists = false;
        WebElement challengeIframe = null;
        WebDriver driver = helper.getDriver();
        DEFAULT_TIMEOUT = driver.getTimeout();

        // Start re-captcha challenge
        element = driver.findElement(helper.getSearchCriteria());
        element.click();

        List<WebElement> iframes = driver.findElementsByTagName("iframe");

        driver.setTimeout(TIMEOUT);
        for (WebElement iframe: iframes){
            driver.switchTo().defaultContent();
            String iframeTitle = iframe.getAttribute("title");

            driver.switchTo().frame(iframe);

            // Check if captcha is already passed
            try {
                WebElement checkbox = driver.findElementById("recaptcha-anchor");
                if (checkbox.getAttribute("aria-checked").equals("true")) {
                    return ExecutionResult.PASSED;
                }
            } catch (Exception e) {
                // No luck
            }

            // Saving time as we know which iframe contains captcha challenge
            if (!iframeTitle.contains("recaptcha challenge")){
                continue;
            }

            // Looking for the audio button
            try {
                WebElement audioButton = driver.findElementById("recaptcha-audio-button");
                audioButton.click();
                audioButtonExists = true;
                challengeIframe = iframe;
            }
            catch (Exception e){
                continue;
            }
        }
        if(audioButtonExists){
            while(true){
                try{
                    // Try to parse audio to text
                    String sourceURL = driver.findElementById("audio-source").getAttribute("src");
                    String parsedAudio = audioToText(sourceURL);
                    if(parsedAudio == null || parsedAudio.isEmpty()){
                        throw new FailureException("Could not parse audio");
                    }

                    // Refreshing iframe as it is being re-created after switch to audio
                    driver.switchTo().defaultContent();
                    driver.switchTo().frame(challengeIframe);

                    // Input resulted text
                    WebElement inputBtn = driver.findElementById("audio-response");
                    inputBtn.sendKeys(parsedAudio);
                    inputBtn.sendKeys(Keys.ENTER);

                    // Check for error messages
                    WebElement errorMsg = driver.findElementsByClassName("rc-audiochallenge-error-message").get(0);
                    if(!errorMsg.getText().equals("") && !errorMsg.getText().equals(null)){
                        // Sometimes it's required to pass audio challenge several times
                        if(errorMsg.getText().contains("Multiple")){
                            continue;
                        }
                        throw new FailureException("Error message found: " + errorMsg.getText());
                    }
                    break;
                }
                catch(Exception e){
                    throw new FailureException("Failed to bypass captcha:" + e.getMessage());
                }
            }
        }
        else{
            throw new FailureException("Failed to bypass captcha: Audio button not found");
        }
        driver.setTimeout(DEFAULT_TIMEOUT);
        return ExecutionResult.PASSED;
    }

    //Make an HTTP get request to get audio InputSupplier from source_url
    private InputStream getAudioInputStream(String source_url) throws IOException, InterruptedException{
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(source_url))
                .build();
        HttpResponse<InputStream> response;
        response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        return response.body();
    }

    private String audioToText(String source_url) throws IOException, InterruptedException {
        IamAuthenticator authenticator = new IamAuthenticator.Builder().apikey(watsonAPIKey).build();
        SpeechToText speechToText = new SpeechToText(authenticator);
        speechToText.setServiceUrl(watsonCloudURL);
        RecognizeOptions options = new RecognizeOptions.Builder()
                .audio(getAudioInputStream(source_url))
                .contentType("audio/mp3")
                .build();
        ServiceCall<SpeechRecognitionResults> results = speechToText.recognize(options);
        Response<SpeechRecognitionResults> response = results.execute();
        String parsedAudio = response.getResult().getResults().get(0).getAlternatives().get(0).getTranscript();
        return parsedAudio;
    }
}
