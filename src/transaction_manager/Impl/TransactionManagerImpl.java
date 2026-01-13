package transaction_manager.Impl;

import transaction_manager.TransactionManager;
import transaction_manager.TransactionStatus;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.locks.ReentrantLock;

import static transaction_manager.TransactionStatus.*;
import static transaction_manager.XidFile.*;

/**
 *
 * @author RabbitTank
 * @date 2026/1/12
 * @description
 */
public class TransactionManagerImpl implements TransactionManager {

    private final RandomAccessFile raf;
    private final FileChannel fc;
    private final ReentrantLock lock = new ReentrantLock();

    public TransactionManagerImpl(RandomAccessFile raf, FileChannel fc) {
        this.raf = raf;
        this.fc = fc;
    }


    // region 接口方法实现
    @Override
    public long begin() {
        lock.lock();
        try {
            long oldXidCounter = readHeader();
            long newXidCounter = oldXidCounter + 1;

            // 先写状态，再更新header（更安全）
            writeStatus(newXidCounter, ACTIVE);
            writeHeader(newXidCounter);

            fc.force(false);
            return newXidCounter;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }


    @Override
    public void commit(long xid) {
        if (xid == SUPER_XID) return;
        checkXid(xid);
        lock.lock();
        try {
            writeStatus(xid, COMMITTED);
            fc.force(false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }

    }

    @Override
    public void abort(long xid) {
        if (xid == SUPER_XID) return;
        checkXid(xid);
        lock.lock();
        try {
            writeStatus(xid, ABORTED);
            fc.force(false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }

    }

    @Override
    public void close() {
        lock.lock();
        try {
            try { fc.force(true); } catch (Exception ignore) {}
            try { fc.close(); } catch (Exception ignore) {}
            try { raf.close(); } catch (Exception ignore) {}
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean isActive(long xid) {
        if (xid == SUPER_XID) return false;
        checkXid(xid);
        return readStatus(xid) == ACTIVE;
    }

    @Override
    public boolean isCommitted(long xid) {
        if (xid == SUPER_XID) return true;
        checkXid(xid);
        return readStatus(xid) == COMMITTED;
    }

    @Override
    public boolean isAborted(long xid) {
        if (xid == SUPER_XID) return false;
        checkXid(xid);
        return readStatus(xid) == ABORTED;
    }
    // endregion

    // region 私有方法

    /**
     * 读取 xid 文件的文件头
     *
     * @return 返回 xid 文件头 即 总事务数
     */
    private long readHeader() {
        ByteBuffer buf = ByteBuffer.allocate(HEADER_LEN);
        long pos = 0;
        try {
            // 注意 带 offset 的 read/write 是 定位读写，fc.position() 在这次操作里基本不参与。
            while (buf.hasRemaining()) {
                int n = fc.read(buf, pos);
                if (n < 0) throw new RuntimeException("EOF while reading header");
                pos += n;
            }
            buf.flip();
            return buf.getLong();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 读取输入 xid 事务的状态
     *
     * @param xid 事务 ID
     * @return 状态
     */
    private TransactionStatus readStatus(long xid) {
        try {
            ByteBuffer buf = ByteBuffer.allocate(XID_FIELD_SIZE);
            int n = fc.read(buf, statusOffset(xid));
            if (n != 1) throw new RuntimeException("cannot read xid status, xid=" + xid);
            buf.flip();
            byte b = buf.get();
            return switch (b) {
                case 0 -> ACTIVE;
                case 1 -> COMMITTED;
                case 2 -> ABORTED;
                default -> throw new RuntimeException("bad xid status code=" + b + ", xid=" + xid);
            };
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 新事务加入后 写入(更新)文件头
     *
     * @param xidCounter xid计数(事务数)
     */
    private void writeHeader(long xidCounter) {
        ByteBuffer buf = ByteBuffer.allocate(HEADER_LEN);
        buf.putLong(xidCounter);
        buf.flip();
        long pos = 0;
        try {
            while (buf.hasRemaining()) {
                int n = fc.write(buf, pos);
                if (n <= 0) throw new RuntimeException("cannot write header");
                pos += n;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 写入(更新)状态
     *
     * @param xid    需要更新的事务 id
     * @param status 需要更新的事务的状态
     */
    private void writeStatus(long xid, TransactionStatus status) {
        try {
            ByteBuffer buf = ByteBuffer.allocate(XID_FIELD_SIZE);
            buf.put(status.getCode());
            buf.flip();
            int written = fc.write(buf, statusOffset(xid));
            if (written != 1) throw new RuntimeException("cannot write xid status, xid=" + xid);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void checkXid(long xid) {
        if (xid <= 0) throw new IllegalArgumentException("xid must be >= 1, but was " + xid);
    }


    // endregion
}
