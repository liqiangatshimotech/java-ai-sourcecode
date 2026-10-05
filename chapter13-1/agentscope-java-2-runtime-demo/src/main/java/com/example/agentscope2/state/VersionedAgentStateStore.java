package com.example.agentscope2.state;

import io.agentscope.core.state.AgentStateStore;
import io.agentscope.core.state.State;
import io.agentscope.core.util.JsonUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;

/**
 * RC5 官方 AgentStateStore 的一次调用快照适配器。
 * 框架读写先落本次快照，调用成功后 commit 用版本条件一次持久化所有 key。
 * 一个实例绑定一个认证后的 tenant/user/session，不能跨请求共享。
 */
public final class VersionedAgentStateStore implements AgentStateStore {
    private static final ObjectMapper JSON = new ObjectMapper();
    public record Identity(String tenant, String user, String session) {
        public Identity {
            for (String value : List.of(tenant, user, session)) {
                if (value.isBlank() || value.length() > 160) throw new IllegalArgumentException("身份字段长度应为 1 到 160");
            }
        }
    }
    public static final class VersionConflict extends RuntimeException {
        public VersionConflict() { super("会话版本冲突，请重新加载状态；本次结果未提交"); }
    }
    private final String jdbc;
    private final Identity identity;
    private long version;
    // 外层 Map 是本次调用的快照，内层字符串由框架的 JsonCodec 序列化。
    // single:/list: 区分接口的单值和列表槽位，避免相同业务 key 互相覆盖。
    private final Map<String,List<String>> snapshot = new LinkedHashMap<>();

    public VersionedAgentStateStore(Path database, Identity identity) {
        this.jdbc = "jdbc:h2:file:" + database.toAbsolutePath().normalize() + ";DB_CLOSE_ON_EXIT=FALSE";
        this.identity = identity;
        try (Connection c = connection(); Statement s = c.createStatement()) {
            s.execute("CREATE TABLE IF NOT EXISTS course_agent_state (tenant_id VARCHAR(160), user_id VARCHAR(160), session_id VARCHAR(160), version BIGINT NOT NULL, payload CLOB NOT NULL, PRIMARY KEY(tenant_id,user_id,session_id))");
            try (PreparedStatement q = c.prepareStatement("SELECT version,payload FROM course_agent_state WHERE tenant_id=? AND user_id=? AND session_id=?")) {
                bind(q,1);
                try (ResultSet r=q.executeQuery()) {
                    if(r.next()) {
                        version=r.getLong(1);
                        snapshot.putAll(JSON.readValue(r.getString(2),new TypeReference<Map<String,List<String>>>(){}));
                    }
                }
            }
        } catch(Exception e) { throw new IllegalStateException("加载持久化会话失败",e); }
    }
    private Connection connection() throws SQLException { return DriverManager.getConnection(jdbc,"sa",""); }
    private void bind(PreparedStatement p,int offset)throws SQLException {
        p.setString(offset,identity.tenant());p.setString(offset+1,identity.user());p.setString(offset+2,identity.session());
    }
    private void check(String user,String session) {
        if(!identity.user().equals(user)||!identity.session().equals(session)) throw new SecurityException("状态存储不接受其他身份或会话");
    }
    /** 0 表示尚无数据行；删除后的会话仍保留大于 0 的版本墓碑。 */
    public long version(){return version;}

    /**
     * 单条 SQL 是提交边界。版本不匹配时绝不自动覆盖或重试模型/工具。
     * 删除提交空快照但保留增长的版本，旧请求无法在删除后复活历史。
     */
    public synchronized void commit() {
        try(Connection c=connection()) {
            String payload=JSON.writeValueAsString(snapshot);
            if(version==0) {
                try(PreparedStatement p=c.prepareStatement("INSERT INTO course_agent_state(tenant_id,user_id,session_id,version,payload) VALUES(?,?,?,1,?)")) {
                    bind(p,1);p.setString(4,payload);
                    try {p.executeUpdate();} catch(SQLException e) {
                        if("23505".equals(e.getSQLState())) throw new VersionConflict();
                        throw e;
                    }
                }
            } else {
                try(PreparedStatement p=c.prepareStatement("UPDATE course_agent_state SET version=version+1,payload=? WHERE tenant_id=? AND user_id=? AND session_id=? AND version=?")) {
                    p.setString(1,payload);bind(p,2);p.setLong(5,version);
                    if(p.executeUpdate()!=1) throw new VersionConflict();
                }
            }
            version++;
        } catch(VersionConflict e) {throw e;} catch(Exception e) {throw new IllegalStateException("提交会话失败",e);}
    }
    /** 框架保存单个 State 时仅修改本次快照，不提前对其他请求可见。 */
    @Override public synchronized void save(String u,String s,String key,State value) {
        check(u,s);snapshot.put("single:"+key,List.of(JsonUtils.getJsonCodec().toJson(value)));
    }
    /** 框架列表状态完整替换；与单值状态在同一次 commit 中持久化。 */
    @Override public synchronized void save(String u,String s,String key,List<? extends State> values) {
        check(u,s);snapshot.put("list:"+key,values.stream().map(x->JsonUtils.getJsonCodec().toJson(x)).toList());
    }
    /** 由调用方提供明确的 State 类型，使用官方 JsonCodec 恢复消息等框架对象。 */
    @Override public synchronized <T extends State> Optional<T> get(String u,String s,String key,Class<T> type) {
        // RC5 构造 Agent 时会探测匿名默认槽；只返回空，绝不开放匿名写入。
        if(u==null) return Optional.empty();
        check(u,s);var value=snapshot.get("single:"+key);
        return value==null?Optional.empty():Optional.of(JsonUtils.getJsonCodec().fromJson(value.getFirst(),type));
    }
    @Override public synchronized <T extends State> List<T> getList(String u,String s,String key,Class<T> type) {
        if(u==null) return List.of();
        check(u,s);return snapshot.getOrDefault("list:"+key,List.of()).stream()
                .map(x->JsonUtils.getJsonCodec().fromJson(x,type)).toList();
    }
    /** 存在性指会话仍有内容，数据库里只剩墓碑时返回 false。 */
    @Override public synchronized boolean exists(String u,String s){check(u,s);return !snapshot.isEmpty();}
    /** 删除也先修改快照，必须显式 commit 后才持久化。 */
    @Override public synchronized void delete(String u,String s){check(u,s);snapshot.clear();}
    @Override public synchronized void delete(String u,String s,String key){check(u,s);snapshot.remove("single:"+key);snapshot.remove("list:"+key);}
    @Override public synchronized Set<String> listSessionIds(String u){check(u,identity.session());return snapshot.isEmpty()?Set.of():Set.of(identity.session());}
}
