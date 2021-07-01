package com.szhtp.cache.jedis;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;

/**
 * JedisService 单机模式实现
 *
 * @author null
 */
public class JedisServiceImpl extends AbstractJedisServiceImpl implements JedisService {

    private static final Logger log = LoggerFactory.getLogger(JedisServiceImpl.class);

    private String password;

    private int maxActive;

    private static int SERVER_INDEX = 0;

    private JedisPool jedisPool = null;

    private JedisPoolConfig config;

    private String[] serverList;

    private String[] portList;

    /**
     * 控制一个pool最多有多少个状态为idle(空闲的)的jedis实例，
     * 默认值是8。
     */
    private int maxIdle = 10;
    /**
     * 等待可用连接的最大时间，单位毫秒，默认值为-1，表示永不超时。如果超过等待时间，则直接抛出JedisConnectionException；
     */
    private int maxWait = 10 * 1000;
    /**
     * 超时时间
     */
    private int timeout = 3000;

    @Override
    public String getPrefix() {
        return prefix;
    }

    @Override
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public JedisServiceImpl(String prefix, String[] serverList, String[] portList, String password, int maxActive) {
        if (prefix != null && !"".equals(prefix.trim())) {
            this.prefix = prefix.trim();
        }
        this.serverList = serverList;
        this.portList = portList;
        this.password = password;
        this.maxActive = maxActive;
    }

    private void initialPoolConfig() {
        config = new JedisPoolConfig();
        // 控制一个pool可分配多少个jedis实例，通过pool.getResource()来获取；
        // 如果赋值为-1，则表示不限制；如果pool已经分配了maxActive个jedis实例，则此时pool的状态为exhausted(耗尽)。
        config.setMaxTotal(maxActive);
        // 控制一个pool最多有多少个状态为idle(空闲的)的jedis实例。
        config.setMinIdle(5);
        config.setMaxIdle(maxIdle);
        // 表示当borrow(引入)一个jedis实例时，最大的等待时间，如果超过等待时间，则直接抛出JedisConnectionException；
        config.setMaxWaitMillis(maxWait);
        // 在borrow一个jedis实例时，是否提前进行validate操作；如果为true，则得到的jedis实例均是可用的
        config.setTestOnBorrow(true);
        config.setTestOnReturn(true);
        //Idle时进行连接扫描
        config.setTestWhileIdle(true);
        //表示idle object evitor两次扫描之间要sleep的毫秒数
        config.setTimeBetweenEvictionRunsMillis(30000);
        //表示idle object evitor每次扫描的最多的对象数
        config.setNumTestsPerEvictionRun(10);
        //表示一个对象至少停留在idle状态的最短时间，然后才能被idle object evitor扫描并驱逐；
        //这一项只有在timeBetweenEvictionRunsMillis大于0时才有意义
        config.setMinEvictableIdleTimeMillis(60000);
    }

    private void initialPool() {
        try {
            if (config == null) {
                initialPoolConfig();
            }
            if (StringUtils.isNotBlank(password)) {
                jedisPool = new JedisPool(config, serverList[SERVER_INDEX], Integer.parseInt(portList[SERVER_INDEX]), timeout, password);
            } else {
                jedisPool = new JedisPool(config, serverList[SERVER_INDEX], Integer.parseInt(portList[SERVER_INDEX]), timeout);
            }
            log.info("[initialPool]... -> " + serverList[SERVER_INDEX] + " : " + portList[SERVER_INDEX]);
        } catch (Exception e) {
            log.error("create JedisPool error : " + e);
        }
    }

    private synchronized void poolInit() {
        if (jedisPool == null) {
            initialPool();
        }
    }

    /**
     * 同步获取Jedis实例
     *
     * @return Jedis
     */
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
        } catch (JedisConnectionException e1) {
            log.error("[getJedis] --> JedisConnectionException : " + serverList[SERVER_INDEX] + " : " + portList[SERVER_INDEX]);
            jedisPool = null;
            if (SERVER_INDEX < serverList.length - 1) {
                SERVER_INDEX = SERVER_INDEX + 1;
                poolInit();
            } else {
                SERVER_INDEX = 0;
            }
        } catch (JedisException e) {
            log.error("Get jedis error", e);
        }
        return jedis;
    }

}
