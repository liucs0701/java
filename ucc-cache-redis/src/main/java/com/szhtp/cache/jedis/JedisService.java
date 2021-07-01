package com.szhtp.cache.jedis;

import java.util.List;

/**
 * Jedis接口
 *
 * @author null
 */
public interface JedisService {

    /**
     * 给指定key插入字符串
     *
     * @param key   键
     * @param value 值
     * @author wangjx
     */
    void setString(String key, String value);

    /**
     * 设置 过期时间
     *
     * @param key     键
     * @param value   值
     * @param seconds 以秒为单位
     * @return true 成功
     */
    boolean setString(String key, String value, int seconds);

    /**
     * 加锁方式设置字符串
     * 将 key 的值设为 value ，当且仅当 key 不存在。
     *
     * @param key     键
     * @param value   值
     * @param seconds 过期时间
     * @return 设置成功，返回 1 。设置失败，返回 0 。
     * @author wangjx
     */
    Long setnxString(String key, String value, int seconds);

    /**
     * 获取String值
     *
     * @param key 键
     * @return value 值
     */
    String getString(String key);


    /***
     * 设置對象
     *
     * @param key 键
     * @param obj 对象
     */
    <T> void setObject(String key, T obj);

    /**
     * 设置對象,此方法针对非序列化对象使用，将对象转换成字符串存储到redis中
     *
     * @param key     键
     * @param seconds 过期时间秒
     * @param obj     对象
     * @return true 成功
     * @author wangjx
     */
    <T> boolean setObject(String key, T obj, int seconds);


    /**
     * 获取对象, 此方法针对非序列化对象使用，将对象转换成字符串存储到redis中
     *
     * @param key   键
     * @param clazz class
     * @return value 值
     */
    <T> T getObject(String key, Class<T> clazz);

    /**
     * 设置對象, 将对象序列化后存储到redis
     *
     * @param key     键
     * @param seconds 过期时间秒
     * @param objList 集合
     * @author wangjx
     */
    <T> void setList(String key, List<T> objList, int seconds);

    /**
     * 获取list
     *
     * @param key   键
     * @param clazz 对象class
     * @return list
     */
    <T> List<T> getList(String key, Class<T> clazz);


    /**
     * 通过key向指定的value值追加值
     *
     * @param key 键
     * @param str 值
     * @return 成功返回 添加后value的长度 失败 返回 添加的 value 的长度 异常返回0L
     */
    Long append(String key, String str);

    /**
     * 删除key
     *
     * @param key 键
     * @return 被删除 key 的数量
     */
    Long delKey(String key);

    /**
     * redis getSet操作
     *
     * @param key     键
     * @param value   值
     * @param seconds 过期时间秒
     * @return 返回给定 key 的旧值。  当 key 没有旧值时，也即是， key 不存在时，返回 nil 。
     */
    String getSet(String key, String value, int seconds);

    /**
     * 自增操作，并制定过期时间
     *
     * @param key    键
     * @param expire 过期时间秒
     * @return 执行 INCR 命令之后 key 的值。
     */
    Long incr(String key, int expire);

    /**
     * 自增1
     *
     * @param key 键
     * @return 执行 INCR 命令之后 key 的值。
     */
    Long incr(String key);
}
