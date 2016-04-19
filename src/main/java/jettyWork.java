/**
 * Created by dong on 15-8-20.
 */

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.BufferedReader;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.ServerConnector;

import org.eclipse.jetty.util.thread.QueuedThreadPool;

public class jettyWork
{
    public static void main(String[] args) throws Exception
    {
        QueuedThreadPool threadPool = new QueuedThreadPool();
        threadPool.setMaxThreads(1024);
        Server server = new Server(threadPool);
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(8040);
        server.setConnectors(new Connector[] { connector });
        /*
        ContextHandler context = new ContextHandler();
        context.setContextPath("/");
        context.setResourceBase(".");
        context.setClassLoader(Thread.currentThread().getContextClassLoader());
        server.setHandler(context);
        context.setHandler(new jettyHandler());*/
        server.setHandler(new jettyHandler());

        //redispool
        redisManager.getInstance().connect();

        server.start();
        server.join();
    }

    public static String getMessageBody(HttpServletRequest request)throws IOException
    {
        BufferedReader reader=request.getReader();
        StringBuilder sb = new StringBuilder(1024);
        String line;
        while ((line=reader.readLine())!=null) {
            sb.append(line);
        } // .. read the request body
        reader.close();
        return sb.toString();
    }
}

class jettyHandler extends AbstractHandler
{
    private static Logger log4j;
    static {
        log4j=Logger.getLogger(jettyWork.class.getClass());
        PropertyConfigurator.configure("jettyWork.properties");
    }
    public void handle(String target,
                       Request baseRequest,
                       HttpServletRequest request,
                       HttpServletResponse response)
            throws IOException, ServletException
    {
        //收到请求
        StringBuilder res = new StringBuilder(1024);
        try
        {
            filterWork work = new filterWork();
            work.connectRedis();
            work.getInputs(jettyWork.getMessageBody(request));
            work.getStrategy();
            work.init();

            if (work.isFilterTrue()) {
                res.append("YES ");
                res.append(work.getExportValue());
            } else {
                res.append("NO");
            }

            work.endWork();
        }
        catch (Exception e)
        {
            log4j.info(e.toString());
            res.append("request failed.");
        }
        response.setContentType("text/html;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        baseRequest.setHandled(true);
        response.getWriter().println(res.toString());
    }
}