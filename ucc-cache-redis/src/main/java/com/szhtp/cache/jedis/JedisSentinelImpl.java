package com.szhtp.cache.jedis;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisSentinelPool;
import redis.clients.jedis.exceptions.JedisException;

import java.util.HashSet;
import java.util.Set;

/**
 * JedisService 哨兵模式实现
 * 需要开启redis-sentinel服务,并开启redis服务的主从复制：slaveof masterIP masterPort
 *
 * @author heguixing
 */
public class JedisSentinelImpl extends AbstractJedisServiceImpl implements JedisService {

    private static final Logger log = LoggerFactory.getLogger(JedisSentinelImpl.class);

    private String[] serverList;

    private String[] portList;

    private String masterName;

    private JedisSentinelPool jedisPool = null;

    private String password;

    private int maxActive;

    public JedisSentinelImpl(String prefix, String[] serverList, String[] portList, String password, int maxActive, String masterName) {
        if (prefix != null && !"".equals(prefix.trim())) {
            this.prefix = prefix.trim();
        }
        this.serverList = serverList;
        this.portList = portList;
        this.password = password;
        this.maxActive = maxActive;
        this.masterName = masterName;
    }

    /**
     * 初始化Redis连接池
     */
    private synchronized void initialPool() {
        try {
            // 建立连接池配置参数
            JedisPoolConfig config = new JedisPoolConfig();
            config.setMaxTotal(maxActive);
            // 设置最大阻塞时间，记住是毫秒数milliseconds
            config.setMaxWaitMillis(10000);
            config.setTimeBetweenEvictionRunsMillis(30000);
            // 设置空间连接
            config.setMaxIdle(30);
            //设置最小空闲数 
            config.setMinIdle(8);
            // jedis实例是否可用
            config.setTestOnBorrow(true);
            Set<String> sentinels = new HashSet<>();
            for (int i = 0; i < serverList.length; i++) {
                sentinels.add(serverList[i] + ":" + Integer.valueOf(portList[i]));
            }
            if (StringUtils.isBlank(this.password)) {
                jedisPool = new JedisSentinelPool(masterName, sentinels, config);
            } else {
                jedisPool = new JedisSentinelPool(masterName, sentinels, config, password);
            }
        } catch (Exception e) {
            log.error("init pool fail : ", e);
        }
    }

    private synchronized void poolInit() {
        if (jedisPool == null) {
            initialPool();
        }
    }

    @Override
    public synchronized Jedis getJedis() {
        if (jedisPool == null) {
            poolInit();
        }
        Jedis jedis = null;
        try {
            if (jedisPool != null) {
                jedis = jedisPool.getResource();
            }
        } catch (JedisException e) {
            log.error("Get jedis error : ", e);
        }
        return jedis;
    }
}


