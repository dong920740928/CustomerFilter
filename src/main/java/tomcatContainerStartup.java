/**
 * Created by dong on 15-8-26.
 */

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class tomcatContainerStartup implements ServletContextListener {
    public void contextInitialized(ServletContextEvent event) {
        // Do your thing during webapp's startup.
        //create and configure a RedisPool.
        redisManager.getInstance().connect();
    }
    public void contextDestroyed(ServletContextEvent event) {
        // Do your thing during webapp's shutdown.
        //destroy a RedisPool.
        redisManager.getInstance().release();
    }
}