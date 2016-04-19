import redis.clients.jedis.Jedis;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.util.Scanner;

/**
 * Created by dong on 15-8-13.
 */
public class importdata {
    public static Jedis redis;

    public static void main(String[] args)throws Exception
    {
        connectRedis();

        File file=new File("../src/data.txt");
        if(!file.exists())
        {
            System.out.println("error: data file not found.");
            return;
        }
        Scanner sc=new Scanner(new FileReader(file));
        String key,value;
        while (!((key=sc.next()).equals("end")))
        {
            value=sc.next();
            redis.set(key,value);
        }

        File strategy=new File("../src/setting.json");
        FileInputStream fis=new FileInputStream(strategy);
        int length;
        byte by[]=new byte[1024];
        StringBuilder sb=new StringBuilder(1024);
        while ((length=fis.read(by))!=-1)
        {
            sb.append(new String(by,0,length));
        }
        redis.set("strategy1",sb.toString());

        endWork();
    }

    public static void connectRedis()
    {
        try {
            redis = new Jedis("127.0.0.1", 6379);//连接redis
        }catch (Exception e)
        {
            System.out.println("redis connect fail");
        }
    }

    public static void endWork()
    {
        redis.disconnect();
        redis.close();
    }
}
