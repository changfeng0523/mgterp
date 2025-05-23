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
            Page<Order> orderPage = orderService.getOrderList(page, size);
            log.info("成功返回订单列表，总数: {}, 当前页数量: {}", 
                orderPage.getTotalElements(), orderPage.getContent().size());
            return Result.success(orderPage);
        } catch (Exception e) {
            log.error("获取订单列表失败: {}", e.getMessage(), e);
            return Result.error("获取订单列表失败: " + e.getMessage());
        }
    }
    
    @PostMapping
    public Result<Order> createOrder(@RequestBody Order order) {
        try {
            log.info("接收到订单创建请求，订单类型: {}", order.getType());
            
            List<OrderGoods> goods = order.getGoods();
            if (goods == null || goods.isEmpty()) {
                return Result.error("订单商品不能为空");
            }
            
            // 确保订单类型参数正确传递给Service层
            if (order.getType() == null && order.getOrderType() != null) {
                order.setType(order.getOrderType().equalsIgnoreCase("SALE") ? "customer" : "purchase");
            } else if (order.getType() != null && order.getOrderType() == null) {
                order.setOrderType(order.getType().equalsIgnoreCase("customer") ? "SALE" : "PURCHASE");
            }
            
            // 调用服务层创建订单
            Order createdOrder = orderService.createOrder(order, goods);
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
            Page<Order> orderPage = orderService.getOrdersByType(type, page, size);
            log.info("成功返回{}类型订单列表，总数: {}, 当前页数量: {}", 
                type, orderPage.getTotalElements(), orderPage.getContent().size());
            return Result.success(orderPage);
        } catch (Exception e) {
            log.error("获取{}类型订单列表失败: {}", type, e.getMessage(), e);
            return Result.error("获取订单列表失败: " + e.getMessage());
        }
    }
}