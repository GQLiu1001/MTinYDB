package tm_test;

import main.transaction_manager.TransactionManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 * @author RabbitTank
 * @date 2026/1/13
 * @description
 */
public class TransactionManagerTest {
    String path = "src/test";
    /**
     * 1) begin -> active
     * 2) commit 后 -> committed
     * 3) 重启 open 后仍然 committed
     */
    @Test
    void testBeginCommitAndReopen() {
        TransactionManager tm = TransactionManager.create(path);

        long xid = tm.begin();
        assertTrue(tm.isActive(xid), "begin 后应该是 ACTIVE");

        tm.commit(xid);
        assertTrue(tm.isCommitted(xid), "commit 后应该是 COMMITTED");
        assertFalse(tm.isActive(xid), "commit 后不应该还是 ACTIVE");
        assertFalse(tm.isAborted(xid), "commit 后不应该是 ABORTED");

        tm.close();

        TransactionManager tm2 = TransactionManager.open(path);
        assertTrue(tm2.isCommitted(xid), "重启后 xid 状态仍应是 COMMITTED");
        tm2.close();
    }

    /**
     * 1) begin -> active
     * 2) abort 后 -> aborted
     * 3) 重启 open 后仍然 aborted
     */
    @Test
    void testBeginAbortAndReopen() {
        TransactionManager tm = TransactionManager.create(path);

        long xid = tm.begin();
        assertTrue(tm.isActive(xid), "begin 后应该是 ACTIVE");

        tm.abort(xid);
        assertTrue(tm.isAborted(xid), "abort 后应该是 ABORTED");
        assertFalse(tm.isActive(xid), "abort 后不应该还是 ACTIVE");
        assertFalse(tm.isCommitted(xid), "abort 后不应该是 COMMITTED");

        tm.close();

        TransactionManager tm2 = TransactionManager.open(path);
        assertTrue(tm2.isAborted(xid), "重启后 xid 状态仍应是 ABORTED");
        tm2.close();
    }

    /**
     * 连续 begin 多次，检查 xid 单调递增，重启后仍可查询老 xid 状态。
     */
    @Test
    void testMultipleBegins() {
        TransactionManager tm = TransactionManager.create(path);

        long xid1 = tm.begin();
        long xid2 = tm.begin();
        long xid3 = tm.begin();

        assertTrue(xid2 > xid1 && xid3 > xid2, "xid 应该递增");

        tm.commit(xid1);
        tm.abort(xid2);
        // xid3 保持 active

        assertTrue(tm.isCommitted(xid1));
        assertTrue(tm.isAborted(xid2));
        assertTrue(tm.isActive(xid3));

        tm.close();

        TransactionManager tm2 = TransactionManager.open(path);

        assertTrue(tm2.isCommitted(xid1), "重启后 xid1 仍应 committed");
        assertTrue(tm2.isAborted(xid2), "重启后 xid2 仍应 aborted");
        assertTrue(tm2.isActive(xid3), "重启后 xid3 仍应 active（除非你恢复时会自动处理 active）");

        tm2.close();
    }
}
