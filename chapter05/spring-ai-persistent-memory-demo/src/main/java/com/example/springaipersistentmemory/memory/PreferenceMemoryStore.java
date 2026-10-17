package com.example.springaipersistentmemory.memory;
import java.sql.Timestamp;
import java.time.*;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;
/** 结构化偏好持久化到独立表；示例 DDL 可用于 H2，生产应交给迁移工具。 */
@Repository
public class PreferenceMemoryStore {
    private final JdbcTemplate jdbc;
    private final Clock clock;
    private final TransactionTemplate tx;
    public PreferenceMemoryStore(JdbcTemplate jdbc,Clock clock) {
        this.jdbc=jdbc;this.clock=clock;
        tx=new TransactionTemplate(new DataSourceTransactionManager(java.util.Objects.requireNonNull(jdbc.getDataSource())));
        jdbc.execute("create table if not exists user_preference_memory(user_id varchar(128) not null,type varchar(32) not null,content varchar(255) not null,source varchar(64) not null,observed_at timestamp not null,expires_at timestamp not null,primary key(user_id,type))");
    }
    public synchronized void save(String userId,String type,String content,Duration ttl) {
        if(userId==null||userId.isBlank()||userId.length()>128||ttl.isNegative()||ttl.isZero())
            throw new IllegalArgumentException("用户和有效期不合法");
        Instant now=clock.instant();
        tx.executeWithoutResult(status -> {
            jdbc.update("delete from user_preference_memory where user_id=? and type=?",userId,type);
            jdbc.update("insert into user_preference_memory values(?,?,?,?,?,?)",userId,type,content,"explicit-user-rule",Timestamp.from(now),Timestamp.from(now.plus(ttl)));
        });
    }
    public List<UserPreferenceMemory> findActive(String userId) {
        return jdbc.query("select * from user_preference_memory where user_id=? and expires_at>? order by type",
                (rs,n)->new UserPreferenceMemory(rs.getString("user_id"),rs.getString("type"),rs.getString("content"),rs.getString("source"),rs.getTimestamp("observed_at").toInstant(),rs.getTimestamp("expires_at").toInstant()),
                userId,Timestamp.from(clock.instant()));
    }
    public void delete(String userId) { jdbc.update("delete from user_preference_memory where user_id=?",userId); }
    public int purgeExpired() { return jdbc.update("delete from user_preference_memory where expires_at<=?",Timestamp.from(clock.instant())); }
}
