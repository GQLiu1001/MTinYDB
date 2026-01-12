package transaction_manager;

import transaction_manager.Impl.TransactionManagerImpl;

/**
 *
 * @author RabbitTank
 * @date 2026/1/12
 * @description TransactionManager 的工厂类负责创建/打开 TM 实例，屏蔽具体实现类
 */
public final class TransactionManagerFactory {

    private TransactionManagerFactory() {}

    public static TransactionManager create(String path) {
        return TransactionManagerImpl.create(path);
    }

    public static TransactionManager open(String path) {
        return TransactionManagerImpl.open(path);
    }
}
