package com.mogutou.erp.controller;

import com.mogutou.erp.common.Result;
import com.mogutou.erp.entity.Order;
import com.mogutou.erp.entity.OrderGoods;
import com.mogutou.erp.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.List;

@RestController
@RequestMapping("/api/customer-order")
@CrossOrigin(origins = {"http://localhost:5174", "http://localhost:9876"}, allowCredentials = "true")
public class OrderController {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OrderController.class);
    
    @Autowired
    private OrderService orderService;
    
    /**
     * 获取订单列表，支持分页
     */
    @GetMapping("/list")
    public Result<Page<Order>> getOrderList(
            @RequestParam(value = "page", defaultValue = "0") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size) {
        try {
            log.info("接收到获取订单列表请求: page={}, size={}", page, size);
            
            if (orderService == null) {
                log.error("orderService未正确注入");
                return Result.error("系统错误：服务组件未初始化");
            }
            
            try {
                // 调用服务层获取订单列表，使用分页
                Page<Order> orderPage = orderService.getOrderList(page, size);
                
                if (orderPage == null) {
                    log.warn("orderService.getOrderList()返回了null");
                    return Result.success(Page.empty());
                }
                
                log.info("成功返回订单列表，总数: {}, 当前页数量: {}", 
                    orderPage.getTotalElements(), orderPage.getContent().size());
                return Result.success(orderPage);
            } catch (Exception e) {
                log.error("获取订单列表失败: {}", e.getMessage(), e);
                return Result.error("获取订单列表失败: " + e.getMessage());
            }
        } catch (Exception e) {
            log.error("获取订单列表失败(未预期的异常): {}", e.getMessage(), e);
            return Result.error("系统错误，请联系管理员");
        }
    }
    
    @PostMapping
    public Result<Order> createOrder(@RequestBody Order order) {
        try {
            log.info("接收到订单创建请求: {}", order);
            
            List<OrderGoods> goods = order.getGoods();
            if (goods == null || goods.isEmpty()) {
                log.warn("订单商品列表为空");
                return Result.error("订单商品不能为空");
            }
            
            // 记录前端传递的订单类型参数
            log.info("订单类型: {}, 商品数量: {}", order.getType(), goods.size());
            
            // 详细记录每个商品信息
            for (int i = 0; i < goods.size(); i++) {
                OrderGoods item = goods.get(i);
                log.info("商品[{}]: {}, 数量: {}, 单价: {}", 
                    i, item.getGoods() != null ? item.getGoods().getName() : "未知商品", 
                    item.getQuantity(), item.getUnitPrice());
            }
            
            // 确保订单类型参数正确传递给Service层
            if (order.getType() == null && order.getOrderType() != null) {
                // 如果前端使用orderType而不是type，进行兼容处理
                order.setType(order.getOrderType().equalsIgnoreCase("SALE") ? "customer" : "purchase");
            } else if (order.getType() != null && order.getOrderType() == null) {
                // 如果前端只提供了type，设置对应的orderType
                order.setOrderType(order.getType().equalsIgnoreCase("customer") ? "SALE" : "PURCHASE");
            }
            
            log.info("处理后的订单类型: type={}, orderType={}", order.getType(), order.getOrderType());
            
            // 调用服务层创建订单
            Order createdOrder = orderService.createOrder(order);
            log.info("订单创建成功: ID={}", createdOrder.getId());
            
            return Result.success(createdOrder);
        } catch (Exception e) {
            log.error("创建订单失败: {}", e.getMessage(), e);
            return Result.error("创建订单失败: " + e.getMessage());
        }
    }
    
    @DeleteMapping("/{id}")
    public Result<Void> deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);
        return Result.success();
    }
    
    @PostMapping("/{id}/confirm")
    public Result<Order> confirmOrder(
            @PathVariable Long id, 
            @RequestParam float freight) {
        return Result.success(orderService.confirmOrder(id, freight));
    }
    
    @GetMapping("/type/{type}")
    public Result<Page<Order>> getOrdersByType(
            @PathVariable String type,
            @RequestParam(value = "page", defaultValue = "0") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size) {
        try {
            log.info("接收到获取特定类型订单的请求: type={}, page={}, size={}", type, page, size);
            
            if (orderService == null) {
                log.error("orderService未正确注入");
                return Result.error("系统错误：服务组件未初始化");
            }
            
            try {
                // 调用服务层获取特定类型的订单列表，使用分页
                Page<Order> orderPage = orderService.getOrdersByType(type, page, size);
                
                if (orderPage == null) {
                    log.warn("orderService.getOrdersByType()返回了null");
                    return Result.success(Page.empty());
                }
                
                log.info("成功返回{}类型订单列表，总数: {}, 当前页数量: {}", 
                    type, orderPage.getTotalElements(), orderPage.getContent().size());
                return Result.success(orderPage);
            } catch (Exception e) {
                log.error("获取{}类型订单列表失败: {}", type, e.getMessage(), e);
                return Result.error("获取订单列表失败: " + e.getMessage());
            }
        } catch (Exception e) {
            log.error("获取{}类型订单列表失败(未预期的异常): {}", type, e.getMessage(), e);
            return Result.error("系统错误，请联系管理员");
        }
    }
    
    @GetMapping
    public Result<List<Order>> getAllOrders() {
        try {
            log.info("接收到获取所有订单的请求");
            
            // 检查orderService是否正确注入
            if (orderService == null) {
                log.error("orderService未正确注入");
                return Result.error("系统错误：服务组件未初始化");
            }
            
            try {
                // 调用服务层获取订单列表
                List<Order> orders = orderService.getAllOrders();
                
                // 检查返回的订单列表是否为null
                if (orders == null) {
                    log.warn("orderService.getAllOrders()返回了null");
                    return Result.success(new java.util.ArrayList<>());
                }
                
                // 确保每个订单的goods属性不为null
                for (Order order : orders) {
                    if (order.getGoods() == null) {
                        order.setGoods(new java.util.ArrayList<>());
                    }
                }
                
                log.info("成功返回订单列表，数量: {}", orders.size());
                return Result.success(orders);
            } catch (org.springframework.dao.DataAccessException e) {
                log.error("数据库访问错误: {}", e.getMessage(), e);
                return Result.error("数据库访问错误: " + e.getMessage());
            } catch (RuntimeException e) {
                log.error("获取订单列表失败(RuntimeException): {}", e.getMessage(), e);
                return Result.error("获取订单列表失败: " + e.getMessage());
            }
        } catch (Exception e) {
            log.error("获取订单列表失败(未预期的异常): {}", e.getMessage(), e);
            return Result.error("系统错误，请联系管理员");
        }
    }
}