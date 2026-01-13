package main.transaction_manager;

/**
 *
 * @author RabbitTank
 * @date 2026/1/12
 * @description 事务三个状态：<br/>
 * Active 事务正在进行中 代码 0<br/>
 * Committed 事务已提交 修改最终生效 代码 1<br/>
 * Aborted 事务已回滚/中止 修改最终不生效 代码 2
 */
public enum TransactionStatus {

    ACTIVE((byte) 0),
    COMMITTED((byte) 1),
    ABORTED((byte) 2);

    private final byte code;

    TransactionStatus(byte code) {
        this.code = code;
    }

    public byte getCode() {
        return code;
    }
}
