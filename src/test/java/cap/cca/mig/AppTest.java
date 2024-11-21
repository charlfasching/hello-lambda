package cap.cca.mig;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;

public class AppTest {

    @Test
    public void handleRequest_shouldReturnConstantValue() {
        App function = new App();
        LinkedHashMap<String,String> input = new LinkedHashMap<>();
        input.put("payload", "World");
        Object result = function.handleRequest(input, null);
        assertEquals("hello World", result);
    }
}
