package transaction_manager.Impl;

import transaction_manager.TransactionManager;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.locks.ReentrantLock;

import static transaction_manager.XID.*;

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
        return 0;
    }

    @Override
    public void commit(long xid) {

    }

    @Override
    public void abort(long xid) {

    }

    @Override
    public boolean isActive(long xid) {
        return false;
    }

    @Override
    public boolean isCommitted(long xid) {
        return false;
    }

    @Override
    public boolean isAborted(long xid) {
        return false;
    }
    // endregion

}
