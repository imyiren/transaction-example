package io.ilss.transaction.onedatasource.service.impl;

import io.ilss.transaction.onedatasource.dao.AccountDAO;
import io.ilss.transaction.onedatasource.entities.AccountDO;
import io.ilss.transaction.onedatasource.service.AccountService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author feng
 */
@Slf4j
@Service
public class AccountServiceImpl implements AccountService {

    @Autowired
    private AccountDAO accountDAO;

    @Autowired
    PlatformTransactionManager transactionManager;


    /**
     * 声明式事务
     * propagation = Propagation.REQUIRED （默认值就是REQUIRED） 如果调用方有事务就直接使用调用方的事务，如果没有就新建一个事务
     * transactionManager = "transactionManager" 也是默认值
     * isolation= Isolation.DEFAULT 隔离级别
     * 还有timeout等参数 可自行查看Transactional的源码 里面都有说明
     *
     * @param sourceAccountId 源账户
     * @param targetAccountId 目标账户
     * @param amount          金额
     * @return 操作结果信息
     */
    @Override
    @Transactional(transactionManager = "transactionManager", propagation = Propagation.REQUIRED, rollbackFor = Exception.class, isolation = Isolation.DEFAULT)
    public String transferAnnotation(Long sourceAccountId, Long targetAccountId, BigDecimal amount) {
        AccountDO sourceAccountDO = accountDAO.selectByPrimaryKey(sourceAccountId);
        AccountDO targetAccountDO = accountDAO.selectByPrimaryKey(targetAccountId);
        if (null == sourceAccountDO || null == targetAccountDO) {
            return "转入或者转出账户不存在";
        }
        if (sourceAccountDO.getBalance().compareTo(amount) < 0) {
            return "转出账户余额不足";
        }
        sourceAccountDO.setBalance(sourceAccountDO.getBalance().subtract(amount));
        accountDAO.updateByPrimaryKeySelective(sourceAccountDO);
        error("annotation error!");
        targetAccountDO.setBalance(targetAccountDO.getBalance().add(amount));
        accountDAO.updateByPrimaryKeySelective(targetAccountDO);
        return "转账成功!";
    }


    @Override
    public String transferCode(Long sourceAccountId, Long targetAccountId, BigDecimal amount) {
        TransactionDefinition transactionDefinition = new DefaultTransactionDefinition();
        // 获取事务 开始业务执行
        TransactionStatus transaction = transactionManager.getTransaction(transactionDefinition);
        try {
            AccountDO targetAccountDO = accountDAO.selectByPrimaryKey(targetAccountId);
            AccountDO sourceAccountDO = accountDAO.selectByPrimaryKey(sourceAccountId);
            if (null == sourceAccountDO || null == targetAccountDO) {
                return "转入或者转出账户不存在";
            }
            error("code error");
            if (sourceAccountDO.getBalance().compareTo(amount) < 0) {
                return "转出账户余额不足";
            }
            sourceAccountDO.setBalance(sourceAccountDO.getBalance().subtract(amount));
            targetAccountDO.setBalance(targetAccountDO.getBalance().add(amount));
            accountDAO.updateByPrimaryKeySelective(sourceAccountDO);
            accountDAO.updateByPrimaryKeySelective(sourceAccountDO);
            // 提交事务
            transactionManager.commit(transaction);
            return "转账成功!";
        } catch (Exception e) {
            log.error("转账发生错误，开始回滚，source: {}, target: {}, amount: {}, errMsg: {}",
                    sourceAccountId, targetAccountId, amount, e.getMessage());
            // 报错回滚
            transactionManager.rollback(transaction);
        }
        return "转账失败";
    }

    @Override
    public List<AccountDO> listAll() {
        return accountDAO.selectAll();
    }


    private static void error(String msg) {
        throw new RuntimeException(msg);
    }
}
