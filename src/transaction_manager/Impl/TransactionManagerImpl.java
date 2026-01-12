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

    // region 静态方法
    /**
     * 创建新数据库时调用：创建并初始化 xid 文件
     *
     * @param path 创建 xid 的路径
     * @return
     */
    public static TransactionManagerImpl create(String path) {
        File xidFile = new File(path + XID_SUFFIX);
        RandomAccessFile raf  = null;
        FileChannel fc = null;
        try {
            if (xidFile.exists()) {
                throw new RuntimeException("xid file already exists: " + xidFile.getAbsolutePath());
            }
            if (!xidFile.createNewFile()) {
                throw new RuntimeException("cannot create xid file: " + xidFile.getAbsolutePath());
            }
            // RandomAccessFile raf 文件数据
            raf = new RandomAccessFile(xidFile, "rw");
            // FileChannel fc 搬运工 position 为搬运(复制)文件数据的位置
            fc = raf.getChannel();
            ByteBuffer buf = ByteBuffer.allocate(HEADER_LEN);
            // 把 8 字节的 0L 写进 ByteBuffer 的内部数组 做初始化
            // 同时把 buf 指针的 position 从 0 移到 8
            buf.putLong(0L);
            // 把 buf 指针的 position 从 8 移到 0 用于后续写入 file
            buf.flip();
            // 把 ByteBuffer 当前可读区间的数据写入到文件
            // 写入位置是文件的 offset=0 (传入的 position 参数)
            long pos = 0;
            // 因为文件系统/OS/实现 不同可能需要多次写入
            while (buf.hasRemaining()) {
                int n = fc.write(buf, pos);
                if (n <= 0) {
                    throw new RuntimeException("cannot write xid header: " + xidFile.getAbsolutePath());
                }
                pos += n;
            }
            // Forces any updates to this channel's file to be written
            //      to the storage device that contains it.
            fc.force(false);

            return new TransactionManagerImpl(raf, fc);
        } catch (Exception e) {
            try { if (fc != null) fc.close(); } catch (Exception ignore) {}
            try { if (raf != null) raf.close(); } catch (Exception ignore) {}
            try { xidFile.delete(); } catch (Exception ignore) {}
            throw new RuntimeException(e);
        }
    }

    /**
     * 打开已有数据库时调用：打开并校验 xid 文件
     *
     * @param path 所要打开的 xid 路径
     * @return
     */
    public static TransactionManagerImpl open(String path) {
        File xidFile = new File(path + XID_SUFFIX);
        RandomAccessFile raf  = null;
        FileChannel fc = null;
        try {
            if (!xidFile.exists()) {
                throw new RuntimeException("xid file not found: " + xidFile.getAbsolutePath());
            }
            // RandomAccessFile raf 文件数据
            raf = new RandomAccessFile(xidFile, "rw");
            // FileChannel fc 搬运工 position 为搬运(复制)文件数据的位置
            fc = raf.getChannel();

            // 检验 8 字节 header
            // fc 从文件开头开始搬运
            fc.position(0);
            ByteBuffer buf = ByteBuffer.allocate(HEADER_LEN);
            // fc 开始工作 一直到 buf 装满
            while (buf.hasRemaining()) {
                // 实际装进去的 n 取决于文件系统/OS/实现
                // 顺序读：fc 复制文件数据 -> buf
                int n = fc.read(buf);
                if (n < 0) {
                    throw new RuntimeException("xid file too short: " + xidFile.getAbsolutePath());
                }
            }
            // buf.flip() 让 buf 的 position 指针 = 0
            buf.flip();
            // 从 position 位置开始读 long 个字节 <=> 事务数量
            long xidCounter = buf.getLong();
            if (xidCounter < 0) throw new RuntimeException("bad xidCounter: " + xidCounter);

            // 校验文件长度
            long fileLen = fc.size();
            long expected = HEADER_LEN + xidCounter * XID_FIELD_SIZE;
            if (fileLen != expected) {
                throw new RuntimeException("bad xid file. expected len=" + expected + ", actual len=" + fileLen);
            }

            return new TransactionManagerImpl(raf, fc);
        } catch (Exception e) {
            try { if (fc != null) fc.close(); } catch (Exception ignore) {}
            try { if (raf != null) raf.close(); } catch (Exception ignore) {}
            throw new RuntimeException(e);
        }
    }
    // endregion
}
