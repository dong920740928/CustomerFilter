import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Jedis;

/**
 * Created by dong on 15-8-26.
 */
public class redisManager {
    private static final redisManager instance = new redisManager();
    private static JedisPoolConfig redisConfig;
    private static JedisPool redisPool;
    private redisManager() {}
    public final static redisManager getInstance() {
        return instance;
    }
    public void connect() {
        //redis config
        redisConfig =  new JedisPoolConfig();
        //连接耗尽时是否阻塞, false报异常,ture阻塞直到超时, 默认true
        //config.setBlockWhenExhausted(true);
        //设置的逐出策略类名, 默认DefaultEvictionPolicy(当连接超过最大空闲时间,或连接数超过最大空闲连接数)
        //config.setEvictionPolicyClassName( "org.apache.commons.pool2.impl.DefaultEvictionPolicy" );
        //是否启用pool的jmx管理功能, 默认true
        //config.setJmxEnabled( true );
        //MBean ObjectName = new ObjectName("org.apache.commons.pool2:type=GenericObjectPool,name=" + "pool" + i); 默 认为"pool", JMX不熟,具体不知道是干啥的...默认就好.
        //config.setJmxNamePrefix( "pool" );
        //是否启用后进先出, 默认true
        //config.setLifo( true );
        //最大空闲连接数, 默认8个
        redisConfig.setMaxIdle(50);
        //最小空闲连接数, 默认0
        redisConfig.setMinIdle(5);
        //最大连接数, 默认8个
        redisConfig.setMaxTotal(512);
        //获取连接时的最大等待毫秒数(如果设置为阻塞时BlockWhenExhausted),如果超时就抛异常, 小于零:阻塞不确定的时间,  默认-1
        redisConfig.setMaxWaitMillis(3000);

        //在获取连接的时候检查有效性, 默认false
        redisConfig.setTestOnBorrow(true);
        //返回一个jedis实例给连接池时，是否检查连接可用性（ping()）
        redisConfig.setTestOnReturn( true );
        //在空闲时检查有效性, 默认false
        redisConfig.setTestWhileIdle(true);
        //逐出连接的最小空闲时间 默认1800000毫秒(30分钟)
        redisConfig.setMinEvictableIdleTimeMillis(1000L * 60L * 1L);
        //对象空闲多久后逐出, 当空闲时间>该值 ，且 空闲连接>最大空闲数 时直接逐出,不再根据MinEvictableIdleTimeMillis判断  (默认逐出策略)，默认30m
        redisConfig.setSoftMinEvictableIdleTimeMillis(1000L * 60L * 1L);
        //逐出扫描的时间间隔(毫秒) 如果为负数,则不运行逐出线程, 默认-1
        redisConfig.setTimeBetweenEvictionRunsMillis(60000); //1m
        //每次逐出检查时 逐出的最大数目 如果为负数就是 : 1/abs(n), 默认3
        redisConfig.setNumTestsPerEvictionRun(10);

        redisPool = new JedisPool(redisConfig,"127.0.0.1",6379);
    }
    public void release() {
        redisPool.destroy();
    }
    public Jedis getJedis() {
        return redisPool.getResource();
    }
    public void returnJedis(Jedis jedis) {
        redisPool.returnResourceObject(jedis);
    }
}