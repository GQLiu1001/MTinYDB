package transaction_manager;

/**
 *
 * @author RabbitTank
 * @date 2026/1/12
 * @description 事务管理器 负责发 xid + 记录事务最终状态 + 提供查询<hr/>
 * xid = 0 为超级事务，视为 Committed
 */
public interface TransactionManager {

    /**
     * 开始一个事务 生成 xid 事务并标记为 Active
     */
    long begin();

    /**
     * 完成一个事务 将 xid 事务的状态标记为 Committed
     * @param xid 完成事务的 xid
     */
    void commit(long xid);

    /**
     * 关闭一个事务 将 xid 事务的状态标记为 Aborted
     * @param xid 关闭事务的 xid
     */
    void abort(long xid);


    boolean isActive(long xid);
    boolean isCommitted(long xid);
    boolean isAborted(long xid);

}
