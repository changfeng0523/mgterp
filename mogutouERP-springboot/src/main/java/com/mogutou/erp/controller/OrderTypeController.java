package com.mogutou.erp.controller;

import com.mogutou.erp.common.Result;
import com.mogutou.erp.entity.Order;
import com.mogutou.erp.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders/type")
@CrossOrigin
public class OrderTypeController {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OrderTypeController.class);
    
    @Autowired
    private OrderRepository orderRepository;
    
    @GetMapping("/purchase")
    public Result<org.springframework.data.domain.Page<Order>> getPurchaseOrders(
            @RequestParam(value = "page", defaultValue = "0") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size) {
        try {
            log.info("接收到获取采购订单的请求: page={}, size={}", page, size);
            
            // 检查仓库组件是否正确注入
            if (orderRepository == null) {
                log.error("数据访问组件未正确注入");
                return Result.error("系统错误：服务组件未初始化");
            }
            
            org.springframework.data.domain.Pageable pageable = 
                org.springframework.data.domain.PageRequest.of(page, size);
            
            log.info("尝试获取采购订单数据");
            
            try {
                org.springframework.data.domain.Page<Order> orderPage = 
                    orderRepository.findByOrderType("PURCHASE", pageable);
                
                List<Order> purchaseOrders = orderPage.getContent();
                log.info("从OrderRepository获取到{}条采购订单记录", purchaseOrders.size());
                
                // 处理每个订单
                for (Order order : purchaseOrders) {
                    if (order != null) {
                        processOrder(order);
                    }
                }
                
                log.info("处理采购订单完成，共{}条有效数据", purchaseOrders.size());
                return Result.success(orderPage);
            } catch (Exception e) {
                log.error("使用OrderRepository获取采购订单失败: {}", e.getMessage(), e);
                return Result.error("获取采购订单失败: " + e.getMessage());
            }
        } catch (Exception e) {
            log.error("获取采购订单列表失败(未预期的异常): {}", e.getMessage(), e);
            return Result.error("系统错误，请联系管理员");
        }
    }
    
    // 处理订单数据，确保必要的字段都已初始化
    private void processOrder(Order order) {
        try {
            // 确保订单商品列表不为null
            if (order.getGoods() == null) {
                order.setGoods(new java.util.ArrayList<>());
            }
            
            // 确保operator不为null，避免序列化错误
            if (order.getOperator() == null) {
                // 创建一个空的User对象，避免NPE
                com.mogutou.erp.entity.User emptyUser = new com.mogutou.erp.entity.User();
                emptyUser.setUsername("未知操作员");
                order.setOperator(emptyUser);
                log.warn("订单ID: {}的操作员为null，已设置为默认值", order.getId());
            }
            
            // 确保所有关联对象都已初始化，避免懒加载异常
            order.getGoods().size(); // 触发懒加载
            
            // 计算订单总金额
            float totalAmount = 0.0f;
            for (com.mogutou.erp.entity.OrderGoods goods : order.getGoods()) {
                if (goods.getTotalPrice() != null) {
                    totalAmount += goods.getTotalPrice();
                } else if (goods.getUnitPrice() != null && goods.getQuantity() != null) {
                    float itemTotal = goods.getUnitPrice() * goods.getQuantity();
                    goods.setTotalPrice(itemTotal);
                    totalAmount += itemTotal;
                }
            }
            
            // 设置总金额
            if (order.getAmount() == null || order.getAmount() == 0.0f) {
                order.setAmount(totalAmount);
            }
        } catch (Exception ex) {
            log.error("处理订单ID: {}时发生错误: {}", order.getId(), ex.getMessage(), ex);
        }
    }
    
    @GetMapping("/customer")
    public Result<org.springframework.data.domain.Page<Order>> getCustomerOrders(
            @RequestParam(value = "page", defaultValue = "0") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size) {
        try {
            log.info("接收到获取客户订单的请求: page={}, size={}", page, size);
            
            // 检查仓库组件是否正确注入
            if (orderRepository == null) {
                log.error("数据访问组件未正确注入");
                return Result.error("系统错误：服务组件未初始化");
            }
            
            try {
                // 使用OrderRepository获取客户订单，添加分页支持
                org.springframework.data.domain.Pageable pageable = 
                    org.springframework.data.domain.PageRequest.of(page, size);
                org.springframework.data.domain.Page<Order> orderPage = 
                    orderRepository.findByOrderType("SALE", pageable);
                
                List<Order> customerOrders = orderPage.getContent();
                log.info("从OrderRepository获取到{}条客户订单记录，总数: {}", 
                    customerOrders.size(), orderPage.getTotalElements());
                
                // 处理每个订单
                for (Order order : customerOrders) {
                    if (order != null) {
                        processOrder(order);
                    }
                }
                
                log.info("成功处理客户订单列表，有效订单数量: {}", customerOrders.size());
                return Result.success(orderPage);
            } catch (org.springframework.dao.DataAccessException e) {
                log.error("数据库访问错误: {}", e.getMessage(), e);
                return Result.error("数据库访问错误: " + e.getMessage());
            } catch (RuntimeException e) {
                log.error("获取客户订单列表失败(RuntimeException): {}", e.getMessage(), e);
                return Result.error("获取客户订单列表失败: " + e.getMessage());
            }
        } catch (Exception e) {
            log.error("获取客户订单列表失败(未预期的异常): {}", e.getMessage(), e);
            return Result.error("系统错误，请联系管理员");
        }
    }
}