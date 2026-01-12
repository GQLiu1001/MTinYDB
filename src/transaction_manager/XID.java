package transaction_manager;

/**
 *
 * @author RabbitTank
 * @date 2026/1/12
 * @description 定义 XID 相关参数及工具<br/>
 * 例:00000003210 <br/>
 * 00000003 有三个事务 <br/>
 * 2 第一个事务状态为 ABORTED <br/>
 * 1 第二个事务状态为 COMMITTED <br/>
 * 0 第三个事务状态为 ACTIVE
 */
public class XID{
    /**
     * xid header 长度：8 字节 long
     */
    public static final int HEADER_LEN = 8;

    /**
     * 每个事务状态占用 1 字节
     */
    public static final int XID_FIELD_SIZE = 1;

    /**
     * xid 文件后缀
     */
    public static final String XID_SUFFIX = ".xid";

    /**
     * 超级事务 xid
     */
    public static final long SUPER_XID = 0L;

    /**
     * @param xid 所要查询的事务 xid
     * @return 返回 xid 对应的状态字节在文件中的偏移量
     */
    public static long statusOffset(long xid) {
        if (xid <= 0) {
            throw new IllegalArgumentException("xid must be >= 1, but was " + xid);
        }
        return HEADER_LEN + (xid - 1) * XID_FIELD_SIZE;
    }

    /**
     *
     * @param dbPathPrefix 根据数据库路径前缀
     * @return 根据数据库路径前缀生成 xid 文件名 <br/>
     * （如 /tmp/mydb -> /tmp/mydb.xid）
     */
    public static String xidFilePath(String dbPathPrefix) {
        return dbPathPrefix + XID_SUFFIX;
    }

    /**
     *
     * @param xidCounter 事务计数
     * @return 根据 xidCounter 计算期望的文件长度
     */
    public static long expectedFileLength(long xidCounter) {
        if (xidCounter < 0) {
            throw new IllegalArgumentException("xidCounter must be >= 0, but was " + xidCounter);
        }
        return HEADER_LEN + xidCounter * XID_FIELD_SIZE;
    }
}
