package com.szhtp.cache.jedis;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

import java.util.List;

/**
 * jedis 抽象实现类，减少重复代码
 *
 * @author heguixing
 */
public abstract class AbstractJedisServiceImpl implements JedisService {

    private static final Logger log = LoggerFactory.getLogger(AbstractJedisServiceImpl.class);

    /**
     * 前缀，多模块使用同一个redis时，自动增加前缀进行区分
     */
    protected String prefix = "ucc_";

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    /**
     * 同步获取Jedis实例
     *
     * @return Jedis
     */
    public abstract Jedis getJedis();

    /**
     * 释放jedis资源
     *
     * @param jedis jedis
     */
    private void returnResource(Jedis jedis) {
        if (jedis != null) {
            jedis.close();
        }
    }

    /**
     * 组装redis真实KEY
     *
     * @param key 未加前缀的key
     * @return 加了前缀的key
     */
    private String realKey(String key) {
        return StringUtils.isBlank(prefix) ? key : prefix + key;
    }

    @Override
    public void setString(String key, String value) {
        Jedis js = null;
        try {
            js = getJedis();
            if (js != null) {
                value = StringUtils.isBlank(value) ? "" : value;
                js.set(realKey(key), value);
            }
        } catch (Exception e) {
            log.error("set -> [" + key + "] error : " + e);
        } finally {
            returnResource(js);
        }
    }

    @Override
    public boolean setString(String key, String value, int seconds) {
        Jedis js = null;
        try {
            js = getJedis();
            value = StringUtils.isEmpty(value) ? "" : value;
            String result = js.setex(realKey(key), seconds, value);
            return "OK".equals(result);
        } catch (Exception e) {
            log.error("setex -> [" + key + "] error : " + e);
        } finally {
            returnResource(js);
        }
        return false;
    }

    @Override
    public Long setnxString(String key, String value, int seconds) {
        Long ret = 0L;
        Jedis js = null;
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

    @Override
    public String getString(String key) {
        String ret = null;
        Jedis js = null;
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

    @Override
    public <T> List<T> getList(String key, Class<T> clazz) {
        String s = getString(key);
        if (s != null && !"".equals(s.trim())) {
            return JSONArray.parseArray(s, clazz);
        }
        return null;
    }

    @Override
    public Long append(String key, String str) {
        Jedis js = null;
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
        Jedis js = null;
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
        Jedis js = null;
        try {
            js = getJedis();
            if (js != null) {
                // expire 秒后过期
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
        Jedis js = null;
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

    @Override
    public String getSet(String key, String value, int seconds) {
        String ret = null;
        Jedis js = null;
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
