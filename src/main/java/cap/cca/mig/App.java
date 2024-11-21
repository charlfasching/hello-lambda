package cap.cca.mig;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import java.util.LinkedHashMap;
import java.util.Map;


/**
 * Lambda function entry point. You can change to use other pojo type or implement
 * a different RequestHandler.
 *
 * @see <a href=https://docs.aws.amazon.com/lambda/latest/dg/java-handler.html>Lambda Java Handler</a> for more information
 */
public class App implements RequestHandler<Map, String> {

    public App() {}



    /**
     * Simple Hello World Lambda Handler
     * Changed the inputs and output from Generic Object, to keep the demo simple, but be a bit more specific.
     * At runtime the Lambda runtime will expect and send a JSON message, in format of HashMap.
     *
     * @see LinkedHashMap
     *
     * @param input The Lambda Function input, in this case
     * @param context The Lambda execution environment context object.
     *
     * @return The Lambda Function output, in this case plain String
     */
    @Override
    public String handleRequest(final Map input, final Context context) {
        Object echo = input.get("payload");
        System.out.println(input);
        return String.format("hello %s", echo);
    }

}
