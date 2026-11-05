package com.ticket.system.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 事务提交后执行工具：
 * <p>
 * 将“释放库存后的补票”等操作延迟到数据库事务提交后执行，避免以下两类问题：
 * <ul>
 *   <li>补票创建订单等 DB 操作与释放库存在同一事务中时，一旦后续步骤异常，整单回滚但 Redis 队列/库存已被消费，两边不一致；</li>
 *   <li>补票过程中新建事务/长时间循环会长时间占住取消/过期等业务事务的数据库连接与行锁。</li>
 * </ul>
 */
@Slf4j
public final class AfterCommit {

    private AfterCommit() {
    }

    /**
     * 当前存在事务时注册 afterCommit 回调；不存在事务时直接执行。
     * 回调内部的异常会被吞掉并记录日志，绝不因补票失败反向影响已经成功的业务事务。
     */
    public static void run(Runnable action) {
        if (action == null) {
            return;
        }
        try {
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        safeRun(action);
                    }
                });
            } else {
                safeRun(action);
            }
        } catch (Exception e) {
            log.error("注册事务提交后任务失败", e);
        }
    }

    private static void safeRun(Runnable action) {
        try {
            action.run();
        } catch (Exception e) {
            log.error("事务提交后任务执行失败", e);
        }
    }
}
