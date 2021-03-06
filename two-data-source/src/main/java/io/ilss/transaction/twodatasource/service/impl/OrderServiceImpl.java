package io.ilss.transaction.twodatasource.service.impl;

import io.ilss.transaction.twodatasource.dao.account.AccountDAO;
import io.ilss.transaction.twodatasource.dao.order.OrderInfoDAO;
import io.ilss.transaction.twodatasource.entities.AccountDO;
import io.ilss.transaction.twodatasource.entities.OrderInfoDO;
import io.ilss.transaction.twodatasource.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.config.JtaTransactionManagerBeanDefinitionParser;
import org.springframework.transaction.support.DefaultTransactionDefinition;

/**
 * @author feng
 */
@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderInfoDAO orderInfoDAO;

    @Autowired
    private AccountDAO accountDAO;

    @Autowired
    PlatformTransactionManager transactionManager;


    @Override
    @Transactional
    public String createOrder(OrderInfoDO orderInfoDO) {
        AccountDO accountDO = accountDAO.selectByPrimaryKey(orderInfoDO.getAccountId());
        if (null == accountDO) {
            log.error("createOrder user is not present, accountId: {}", orderInfoDO.getAccountId());
            return "用户不存在！";
        }
        // 用户费用扣除
        accountDO.setBalance(accountDO.getBalance().subtract(orderInfoDO.getAmount()));
        accountDAO.updateByPrimaryKey(accountDO);
        orderInfoDAO.insertSelective(orderInfoDO);

        return "成功";
    }

    @Override
    public String createOrderCode(OrderInfoDO orderInfoDO) {
        TransactionDefinition transactionDefinition = new DefaultTransactionDefinition();
        // 获取事务 开始业务执行
        TransactionStatus transaction = transactionManager.getTransaction(transactionDefinition);
        try {
            AccountDO accountDO = accountDAO.selectByPrimaryKey(orderInfoDO.getAccountId());
            if (null == accountDO) {
                log.error("createOrder user is not present, accountId: {}", orderInfoDO.getAccountId());
                return "用户不存在！";
            }
            // 用户费用扣除
            accountDO.setBalance(accountDO.getBalance().subtract(orderInfoDO.getAmount()));
            accountDAO.updateByPrimaryKey(accountDO);
            orderInfoDAO.insertSelective(orderInfoDO);
            error("createOrderCode error");
            transactionManager.commit(transaction);

            return "成功";
        } catch (Exception e) {
            log.error("create order failed, accountId: {}, errMsg: {}", orderInfoDO.getAccountId(), e.getMessage());
            transactionManager.rollback(transaction);
        }

        return "失败";
    }

    public static void error(String  msg) {
        throw new RuntimeException(msg);
    }
}
