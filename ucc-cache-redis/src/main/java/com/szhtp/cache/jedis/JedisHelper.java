package com.szhtp.cache.jedis;


import lombok.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 单机redis操作,适用于主备方式
 *
 * @author wjx
 */
@Setter
@Getter
@ToString
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class JedisHelper implements JedisService {

    private static final Logger log = LoggerFactory.getLogger(JedisHelper.class);

    /**
     * 前缀
     */
    private String prefix = "ucc_";

    /**
     * redis端口号
     */
    private String port = "6379";

    /**
     * redis服务列表，用于主备方案, 服务器ip集合, s分割
     */
    private String servers;

    /**
     * redis密码
     */
    private String password;

    /**
     * redis 部署模式 standalone sentinel cluster
     */
    private String mode = "standalone";
    /**
     * masterName
     */
    private String masterName = "";

    /**
     * 可用连接实例的最大数目，默认值为8；
     * 如果赋值为-1，则表示不限制；如果pool已经分配了maxActive个jedis实例，则此时pool的状态为exhausted(耗尽)。
     */
    private int maxActive = 100;

    private String[] serverList;

    private String[] portList;

    private JedisService service;

    /**
     * 初始化Redis连接池
     */
    public void initialPool() {
        log.info("initialPool:mode:{}", mode);
        if ("sentinel".equals(mode)) {
            service = new JedisSentinelImpl(this.prefix, this.serverList, this.portList, this.password, this.maxActive, this.masterName);
        } else if ("cluster".equals(mode)) {
            service = new JedisClusterServiceImpl(this.prefix, this.serverList, this.portList, this.password, this.maxActive);
        } else {
            service = new JedisServiceImpl(this.prefix, this.serverList, this.portList, this.password, this.maxActive);
        }
    }

    public void setPort(String port) {
        this.port = port;
        this.portList = port.split(",");
    }

    public void setServers(String servers) {
        this.serverList = servers.split(",");
    }

    @Override
    public void setString(String key, String value) {
        service.setString(key, value);
    }

    @Override
    public boolean setString(String key, String value, int seconds) {
        return service.setString(key, value, seconds);
    }

    @Override
    public Long setnxString(String key, String value, int seconds) {
        return service.setnxString(key, value, seconds);
    }

    @Override
    public String getString(String key) {
        return service.getString(key);
    }

    @Override
    public <T> void setObject(String key, T obj) {
        service.setObject(key, obj);
    }

    @Override
    public <T> boolean setObject(String key, T obj, int seconds) {
        return service.setObject(key, obj, seconds);
    }

    @Override
    public <T> T getObject(String key, Class<T> clazz) {
        return service.getObject(key, clazz);
    }

    @Override
    public <T> void setList(String key, List<T> objList, int seconds) {
        service.setList(key, objList, seconds);
    }

    @Override
    public <T> List<T> getList(String key, Class<T> clazz) {
        return service.getList(key, clazz);
    }

    @Override
    public Long append(String key, String str) {
        return service.append(key, str);
    }

    @Override
    public Long delKey(String key) {
        return service.delKey(key);
    }

    @Override
    public String getSet(String key, String value, int seconds) {
        return service.getSet(key, value, seconds);
    }

    @Override
    public Long incr(String key, int expire) {
        return service.incr(key, expire);
    }

    @Override
    public Long incr(String key) {
        return service.incr(key);
    }

}
