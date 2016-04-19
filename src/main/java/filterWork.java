import org.json.JSONArray;
import org.json.JSONObject;
import redis.clients.jedis.Jedis;

import java.io.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.*;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * Created by dong on 15-8-13.
 */

/**
 * 使用顺序
 * 1 filterWork work=new filterWork();
 * 2 work.connectRedis();
 * 3 work.getInputs(paras);
 * 4 work.getStrategy();
 * 5 work.init();
 * 6 if(work.isFilterTrue())
 * System.out.println(work.getExportValue());
 * else ...
 * 7 work.endWork();
 */
public class filterWork {
    private Jedis redis;

    private Map<String, String> netInput;
    private JSONObject settings;

    private JSONObject filter;
    private JSONObject export;

    private static Logger log4j;

    static {
        //log4j
        log4j = Logger.getLogger(filterWork.class.getClass());
        PropertyConfigurator.configure("judgment.properties");
    }

    public filterWork() {
        this.redis = null;
        this.netInput = new HashMap<String, String>();
        this.settings = null;
        this.filter = null;
        this.export = null;
    }

    /**
     * 读取json文件
     */
    public void getStrategy() throws IOException {
        /*File file=new File("../src/setting.json");
        if(!file.exists())
        {
            System.out.println("error: setting file not found.");
            return;
        }
        FileInputStream fis=new FileInputStream(file);
        StringBuilder sets=new StringBuilder();
        int length;
        byte by[]=new byte[1024];
        while((length=fis.read(by))!=-1)
        {
            String str=new String(by,0,length);
            sets.append(str);
        }*/
        String strategyName = this.netInput.get("strategy");
        String strategy = this.redis.get(strategyName);
        this.settings = new JSONObject(strategy);
    }

    /**
     * 连接redis
     */
    public void connectRedis() {
        try {
            this.redis = redisManager.getInstance().getJedis();
            //redis = new Jedis("127.0.0.1", 6379);//连接redis
        } catch (Exception e) {
            log4j.error("redis connect fail");
        }
    }

    /**
     * 检查来自服务器的输入
     */
    public void checkInputs() {
        Iterator iter;
        iter = this.netInput.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            Object key = entry.getKey();
            Object val = entry.getValue();
            System.out.println(key + " " + val);
        }
    }


    /**
     * para 格式     city_id=10&obj_id=1001
     */
    public void getInputs(String paras) {
        for (String para : paras.split("&")) {
            String pair[] = para.split("=");
            if (pair.length > 1) {
                this.netInput.put(pair[0], pair[1]);
            } else {
                this.netInput.put(pair[0], "");
            }
        }
    }

    /**
     * 从setting中拿出filter和export
     * 参数初始化
     */
    public void init() {
        this.filter = this.settings.getJSONObject("filter");
        this.export = this.settings.getJSONObject("export");
    }

    /**
     * 判断当前样例的filter真值
     *
     * @return boolean 1filter通过 0不通过
     */
    public boolean isFilterTrue() {
        return isChildrensTrue(this.filter.getJSONArray("children"), 1, this.filter.getString("op").equals("AND"));
    }

    /**
     * 获得当前case的export结果
     *
     * @return 策略是否匹配（YES NO）YES 后面给出执行方案。
     */
    public String getExportValue() {
        return getKeyValue(getTagKey(this.export));
    }

    /**
     * 判断children子块，从第k项到最后的组合逻辑的真值
     *
     * @param children 一个item队列，元素可能是tag或filter
     * @param k        偏移量
     * @param and      1表示and合取 0表示or析取
     * @return 子块是否为真
     */
    private boolean isChildrensTrue(JSONArray children, int k, boolean and) {
        boolean res;
        JSONObject tmpjs;
        if ((tmpjs = children.getJSONObject(k - 1)).has("children")) {
            res = isChildrensTrue(tmpjs.getJSONArray("children"), 1, tmpjs.getString("op").equals("AND"));
        } else {
            res = isTagTrue(tmpjs);
        }

        if (k < children.length()) {
            if (and)
                res = res && isChildrensTrue(children, k + 1, and);
            else
                res = res || isChildrensTrue(children, k + 1, and);
        }

        return res;
    }

    /**
     * 判断某个tag块的真值（某个单一命题的真值）
     * 先根据tag块的paras从redis中取出相应的key-value
     * 再根据op指示的操作和filter指示的阈值进行命题的拼接与判断
     * 若op没有给出，则表示操作为判断k-v是否存在，filter取0或1表示预定的期望
     * 真值表
     * k-v                             op                filter       func
     * 不存在相应的k-v对（下用0代替       不存在（用0代替)         0           1
     * 0                            0                   1           0
     * 0                            1                   x           0
     * <p/>
     * 1                            0                   0           0
     * 1                            0                   1           1
     * 1                            1                   x           拼凑命题后判断
     *
     * @param tag
     * @return
     */
    private boolean isTagTrue(JSONObject tag) {
        boolean flag = false;
        String value = null;//获取这个tag的value
        String operator = null;
        String tag_type;
        String filter_value = tag.getString("filter_value");//获取判断阈值
        String key = getTagKey(tag);

        if (!tag.has("operator"))    //op不存在
        {
            flag = (hasKey(key)) ^ filter_value.equals("0");
        } else    //op存在
        {
            value = getKeyValue(key);
            if (value == null)  //value 不存在
            {
                flag = false;
            } else    //value存在
            {
                tag_type = tag.getString("tag_type");
                if (tag_type.equals("SV"))//字符串型tag
                {
                    //op : =
                    flag = value.equals(filter_value);
                } else //数值型tag
                {
                    operator = tag.getString("operator");
                    if (operator.equals(">")) {
                        flag = Integer.parseInt(value) > Integer.parseInt(filter_value);
                    } else if (operator.equals("=")) {
                        flag = Integer.parseInt(value) == Integer.parseInt(filter_value);
                    } else if (operator.equals("<")) {
                        flag = Integer.parseInt(value) < Integer.parseInt(filter_value);
                    }
                }
            }
        }
        //debug
        log4j.info("key: " + key);
        log4j.info("value: " + value + " op: " + operator + " filter_value: " + filter_value);
        log4j.info(" is determined as " + flag);

        return flag;
    }

    /**
     * 根据key获取value
     *
     * @param key
     * @return
     */
    private String getKeyValue(String key) {
        return this.redis.get(key);
    }

    /**
     * 判断某个tag所指定的key-value是否存在
     *
     * @param key
     * @return
     */
    private boolean hasKey(String key) {
        return this.redis.exists(key);
    }

    /**
     * 获取某个tag所指定的key
     *
     * @param tag
     * @return
     */
    private String getTagKey(JSONObject tag) {
        StringBuilder keyStr = new StringBuilder(128);
        keyStr.append(tag.getString("tag_id"));
        JSONArray paras = tag.getJSONArray("parameters");
        int length = paras.length();
        for (int i = 0; i < length; ++i) {
            keyStr.append(":");
            keyStr.append(getParaValue(paras.getJSONObject(i)));
        }
        return keyStr.toString();
    }

    /**
     * 获取tag的某个para的value值
     *
     * @param para
     * @return
     */
    private String getParaValue(JSONObject para) {
        String type = para.getString("type");
        if (type.equals("const"))
            return para.getString("value");
        else if (type.equals("tag")) {
            return getKeyValue(getTagKey(para.getJSONObject("value")));
        }

        return this.netInput.get(para.getString("id"));
    }

    /**
     * 回收
     */
    public void endWork() {
        if (this.redis != null)
            redisManager.getInstance().returnJedis(this.redis);
    }
}
