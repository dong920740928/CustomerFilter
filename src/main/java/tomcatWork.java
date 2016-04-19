/**
 * Created by dong on 15-8-20.
 */

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.io.*;
import java.net.URLDecoder;
import javax.servlet.http.*;

public class tomcatWork extends HttpServlet{
    private static Logger log4j;
    static {
        log4j=Logger.getLogger(tomcatWork.class.getClass());
        PropertyConfigurator.configure("tomcatWork.properties");
    }
    public void doPost(HttpServletRequest request,HttpServletResponse response)throws IOException
    {
        //收到请求
        StringBuilder res = new StringBuilder(1024);
        try
        {
            filterWork work = new filterWork();
            work.connectRedis();
            work.getInputs(getMessageBody(request));
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
        PrintWriter out = response.getWriter();
        out.println(res.toString());
        out.close();
    }
    public void doGet(HttpServletRequest request,HttpServletResponse response)throws IOException
    {
        PrintWriter out = response.getWriter();
        String classPath = this.getClass().getClassLoader().getResource("/").getPath();

        try {
            classPath = URLDecoder.decode(classPath, "gb2312");
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        out.println(classPath);
        out.close();
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
