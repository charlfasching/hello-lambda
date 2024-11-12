package cap.cca.mig;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;


/**
 * Lambda function entry point. You can change to use other pojo type or implement
 * a different RequestHandler.
 *
 * @see <a href=https://docs.aws.amazon.com/lambda/latest/dg/java-handler.html>Lambda Java Handler</a> for more information
 */
public class App implements RequestHandler<Object, Object> {

    public App() {}

    @Override
    // Kept all inputs and output Generic, to keep the demo simple
    public Object handleRequest(final Object input, final Context context) {

        return "hello";
    }

}
