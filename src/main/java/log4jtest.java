/**
 * Created by dong on 15-8-19.
 */
import org.apache.log4j.*;


public class log4jtest {
    private static Logger log4j;
    static {
        log4j=Logger.getLogger(filterWork.class.getClass());
        PropertyConfigurator.configure("../src/main/resources/log4j.properties");
    }

    public static void main(String[] args)
    {
        log4j.info("hello");
    }
}
