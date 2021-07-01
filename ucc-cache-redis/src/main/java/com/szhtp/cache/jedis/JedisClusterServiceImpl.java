package com.szhtp.cache.jedis;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPoolConfig;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * JedisService 集群实现
 *
 * @author null
 */
public class JedisClusterServiceImpl implements JedisService {

    private static final Logger log = LoggerFactory.getLogger(JedisClusterServiceImpl.class);

    /**
     * 前缀，多模块使用同一个redis时，自动增加前缀进行区分
     */
    private String prefix = "ucc_";

    /**
     * redis密码
     */
    private String password;

    /**
     * 可用连接实例的最大数目，默认值为8；
     * 如果赋值为-1，则表示不限制；如果pool已经分配了maxActive个jedis实例，则此时pool的状态为exhausted(耗尽)。
     */
    private int maxActive;

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

    private JedisCluster jedisCluster;

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public JedisClusterServiceImpl(String prefix, String[] serverList, String[] portList, String password, int maxActive) {
        if (prefix != null && !"".equals(prefix.trim())) {
            this.prefix = prefix.trim();
        }
        this.serverList = serverList;
        this.portList = portList;
        this.password = password;
        this.maxActive = maxActive;
        this.initialPoolConfig();
    }

    /**
     * 同步获取Jedis实例
     *
     * @return Jedis
     */
    public void initialPoolConfig() {
        Set<HostAndPort> nodes = new HashSet<>();
        for (int i = 0; i < serverList.length; i++) {
            nodes.add(new HostAndPort(serverList[i], Integer.valueOf(portList[i])));
        }
        if (this.password != null && !"".equals(this.password)) {
            JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
            jedisPoolConfig.setMaxIdle(maxIdle);
            jedisPoolConfig.setMaxWaitMillis(maxWait);
            this.jedisCluster = new JedisCluster(nodes, timeout, timeout, 5, password, jedisPoolConfig);
        } else {
            this.jedisCluster = new JedisCluster(nodes, timeout);
        }
    }

    /**
     * 同步获取Jedis实例
     *
     * @return Jedis
     */
    public synchronized JedisCluster getJedis() {
        if (jedisCluster == null) {
            initialPoolConfig();
        }
        return jedisCluster;
    }

    /**
     * 释放jedis资源
     *
     * @param jedis 资源
     */
    private void returnResource(JedisCluster jedis) {
        if (jedis != null) {
            try {
                jedis.close();
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    /**
     * 组装redis真实KEY
     *
     * @param key
     * @return
     */
    private String realKey(String key) {
        return (prefix == null || "".equals(prefix)) ? key : prefix + key;
    }


    @Override
    public void setString(String key, String value) {
        JedisCluster js = null;
        try {
            js = getJedis();
            if (js != null) {
                value = StringUtils.isEmpty(value) ? "" : value;
                js.set(realKey(key), value);
            }
        } catch (Exception e) {
            log.error("set -> [" + key + "] error : " + e);
        } finally {
            returnResource(js);
        }
    }

    /**
     * 设置 过期时间
     *
     * @param key
     * @param value
     * @param seconds 以秒为单位
     */
    @Override
    public boolean setString(String key, String value, int seconds) {
        JedisCluster js = null;
        try {
            js = getJedis();
            value = StringUtils.isEmpty(value) ? "" : value;
            String setex = js.setex(realKey(key), seconds, value);
            return "OK".equals(setex);
        } catch (Exception e) {
            log.error("setex -> [" + key + "] error : " + e);
        } finally {
            returnResource(js);
        }
        return false;
    }

    /**
     * 设置
     *
     * @param key
     * @param value
     * @param seconds 过期时间
     */
    @Override
    public Long setnxString(String key, String value, int seconds) {
        Long ret = 0L;
        JedisCluster js = null;
        try {
            js = getJedis();
            if (js != null) {
                value = StringUtils.isEmpty(value) ? "" : value;
                ret = js.setnx(realKey(key), value);
                js.expire(realKey(key), seconds);
            }
        } catch (Exception e) {
            log.error("set -> [" + key + "] error : " + e);
        } finally {
            returnResource(js);
        }
        return ret;
    }

    /**
     * 获取String值
     *
     * @param key
     * @return value
     */
    @Override
    public String getString(String key) {
        String ret = null;
        JedisCluster js = null;
        try {
            js = getJedis();
            if (js != null) {
                ret = js.get(realKey(key));
            }
        } catch (Exception e) {
            log.error("get -> [" + key + "] error : " + e);
        } finally {
            returnResource(js);
        }
        return ret;
    }

    public Long getLong(String key) {
        String s = getString(key);
        if (s == null || "".equals(s)) {
            return null;
        }
        try {
            return Long.parseLong(s);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public <T> void setObject(String key, T obj) {
        setString(key, JSON.toJSONString(obj));
    }

    @Override
    public <T> boolean setObject(String key, T obj, int seconds) {
        return setString(key, JSON.toJSONString(obj), seconds);
    }

    @Override
    public <T> T getObject(String key, Class<T> clazz) {
        String s = getString(key);
        if (s != null && !"".equals(s.trim())) {
            return JSON.parseObject(s, clazz);
        }
        return null;
    }

    @Override
    public <T> void setList(String key, List<T> objList, int seconds) {
        setString(key, JSONArray.toJSONString(objList), seconds);
    }

    /**
     * 获取list
     *
     * @param <T>
     * @param key
     * @return list
     */
    @Override
    public <T> List<T> getList(String key, Class<T> clazz) {
        String s = getString(key);
        if (s != null && !"".equals(s.trim())) {
            return JSONArray.parseArray(s, clazz);
        }
        return null;
    }

    /**
     * 通过key向指定的value值追加值
     *
     * @param key
     * @param str
     * @return 成功返回 添加后value的长度 失败 返回 添加的 value 的长度 异常返回0L
     */
    @Override
    public Long append(String key, String str) {
        JedisCluster js = null;
        Long res = 0L;
        try {
            js = getJedis();
            if (js != null) {
                res = js.append(realKey(key), str);
            }
        } catch (Exception e) {
            log.error("append -> [" + realKey(key) + "] error : " + e);
        } finally {
            returnResource(js);
        }
        return res;
    }

    @Override
    public Long delKey(String key) {
        Long ret = null;
        JedisCluster js = null;
        try {
            js = getJedis();
            if (js != null) {
                ret = js.del(realKey(key));
            }
        } catch (Exception e) {
            log.error("delKey -> [" + key + "] error : " + e);
        } finally {
            returnResource(js);
        }
        return ret;
    }

    @Override
    public Long incr(String key, int expire) {
        JedisCluster js = null;
        try {
            js = getJedis();
            if (js != null) {
                // 5秒后过期
                js.expire(realKey(key), expire);
                return js.incr(realKey(key));
            }
        } catch (Exception e) {
            log.error("delKey -> [" + key + "] error : " + e);
        } finally {
            returnResource(js);
        }
        return null;
    }

    @Override
    public Long incr(String key) {
        JedisCluster js = null;
        try {
            js = getJedis();
            if (js != null) {
                return js.incr(realKey(key));
            }
        } catch (Exception e) {
            log.error("delKey -> [" + key + "] error : " + e);
        } finally {
            returnResource(js);
        }
        return null;
    }

    /**
     * @param key
     * @param value
     */
    @Override
    public String getSet(String key, String value, int seconds) {
        String ret = null;
        JedisCluster js = null;
        try {
            js = getJedis();
            if (js != null && js.exists(key.getBytes())) {
                if (value == null) {
                    value = "";
                }
                ret = js.getSet(key, value);
                js.expire(key, seconds);
            }
        } catch (Exception e) {
            log.error("getSet -> [" + key + "] error : " + e);
        } finally {
            returnResource(js);
        }
        return ret;
    }
}
