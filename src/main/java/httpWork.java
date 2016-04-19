/**
 * Created by dong on 15-8-14.
 */

import com.sun.net.httpserver.*;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.log4j.*;
import org.eclipse.jetty.server.handler.ContextHandler;

public class httpWork {
    public static void main(String[] args)throws IOException
    {
        //创建server
        HttpServer server = HttpServer.create(new InetSocketAddress(8000),0);

        server.createContext("/kaiyang/reqwork", new MyHandler());
        server.createContext("/kaiyang/test", new testHandler());

        //thread pool
        ThreadPoolExecutor thp = (ThreadPoolExecutor) Executors.newCachedThreadPool();

        thp.setMaximumPoolSize(1000);
        thp.setCorePoolSize(1000);
        thp.prestartAllCoreThreads();

        server.setExecutor(thp);
        //redispool
        redisManager.getInstance().connect();
        //监听
        server.start();
    }

    public static String getMessageBody(HttpExchange t)throws IOException
    {
        InputStream is = t.getRequestBody();
        StringBuilder sb = new StringBuilder(1024);
        byte[] bytes = new byte[1024];
        int length;
        while ((length = is.read(bytes)) != -1) {
            String str = new String(bytes, 0, length);
            sb.append(str);
        } // .. read the request body
        return sb.toString();
    }
}

class MyHandler implements HttpHandler {
    public void handle(HttpExchange t) throws IOException{
        //收到请求
        StringBuilder res = new StringBuilder(1024);
        try
        {
            filterWork work = new filterWork();
            work.connectRedis();
            work.getInputs(httpWork.getMessageBody(t));
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
            e.printStackTrace();
            res.append("request failed.");
        }
        t.sendResponseHeaders(200, res.length());
        OutputStream os = t.getResponseBody();
        os.write(res.toString().getBytes());
        os.close();
        t.close();
    }

}

class testHandler implements HttpHandler {
    public void handle(HttpExchange t) throws IOException{
        String sb=httpWork.getMessageBody(t)+"test done.";
        sb+=t.getRemoteAddress();
        System.out.println(sb);
        t.sendResponseHeaders(200, sb.length());
        OutputStream os = t.getResponseBody();
        System.out.println(os);
        //os.write(t.getRequestMethod().getBytes());
        os.write(sb.getBytes());
        os.close();
        t.close();
    }
}
