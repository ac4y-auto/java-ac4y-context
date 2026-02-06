package ac4y.base;

import org.junit.Test;
import static org.junit.Assert.*;

public class Ac4yContextTest {

    @Test
    public void testContextInitialization() {
        Ac4yContext context = new Ac4yContext();
        assertNotNull(context);
    }

    @Test(expected = Exception.class)
    public void testGetDBConnectionWithInvalidModule() throws Exception {
        Ac4yContext context = new Ac4yContext();
        context.getDBConnection("invalid_module", "InvalidClass");
    }
}
