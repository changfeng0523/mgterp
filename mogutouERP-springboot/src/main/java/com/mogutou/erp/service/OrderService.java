package com.mogutou.erp.service;

import com.mogutou.erp.entity.Order;
import com.mogutou.erp.entity.OrderGoods;
import com.mogutou.erp.entity.OrderStatus;
import com.mogutou.erp.entity.User;
import com.mogutou.erp.entity.Goods;
import com.mogutou.erp.repository.OrderRepository;
import com.mogutou.erp.repository.GoodsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OrderService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OrderService.class);
    
    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private GoodsRepository goodsRepository;
    
    /**
     * 获取订单列表，支持分页
     */
    @Transactional(readOnly = true)
    public Page<Order> getOrderList(Integer page, Integer size) {
        log.info("获取订单列表: page={}, size={}", page, size);
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            return orderRepository.findAll(pageable);
        } catch (Exception e) {
            log.error("获取订单列表失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取订单列表失败: " + e.getMessage(), e);
        }
    }

    @Transactional
    public Order createOrder(Order order) {
        try {
            log.info("开始创建订单: {}", order);
            
            // 设置订单类型
            String orderType = order.getType();
            if (orderType == null) {
                orderType = order.getOrderType();
            }
            
            // 根据订单类型设置orderType字段
            if ("customer".equalsIgnoreCase(orderType) || "SALE".equalsIgnoreCase(orderType)) {
                order.setOrderType("SALE");
            } else if ("purchase".equalsIgnoreCase(orderType) || "PURCHASE".equalsIgnoreCase(orderType)) {
                order.setOrderType("PURCHASE");
            } else {
                // 默认为销售订单
                order.setOrderType("SALE");
            }
            
            // 设置商品关联
            List<OrderGoods> goods = order.getGoods();
            if (goods != null && !goods.isEmpty()) {
                log.info("处理订单商品列表，共{}项", goods.size());
                
                float totalAmount = 0.0f;
                for (OrderGoods item : goods) {
                    // 查找商品
                    Goods goodsItem = null;
                    if (item.getGoods() != null && item.getGoods().getId() != null) {
                        log.info("查找商品: ID={}", item.getGoods().getId());
                        goodsItem = goodsRepository.findById(item.getGoods().getId())
                            .orElseThrow(() -> new RuntimeException("商品不存在: ID=" + item.getGoods().getId()));
                    }
                    
                    // 计算金额
                    float itemAmount = item.getQuantity() * item.getUnitPrice();
                    totalAmount += itemAmount;
                    log.info("商品金额: {}×{}={}", item.getQuantity(), item.getUnitPrice(), itemAmount);
                    
                    // 设置商品关联
                    if (goodsItem != null) {
                        item.setGoods(goodsItem);
                    }
                    
                    item.setOrder(order);
                    log.info("设置订单商品关联: 商品ID={}, 数量={}, 单价={}", 
                        item.getGoods().getId(), item.getQuantity(), item.getUnitPrice());
                }
                order.setGoods(goods);
            } else {
                log.warn("订单商品列表为空");
                order.setGoods(new java.util.ArrayList<>());
            }
            
            // 保存订单
            log.info("保存订单到数据库");
            try {
                return orderRepository.save(order);
            } catch (Exception e) {
                log.error("保存订单到数据库失败: {}", e.getMessage(), e);
                throw new RuntimeException("保存订单失败: " + e.getMessage(), e);
            }
        } catch (Exception e) {
            log.error("创建订单失败: {}", e.getMessage(), e);
            throw new RuntimeException("创建订单失败: " + e.getMessage(), e);
        }
    }

    @Transactional
    public List<Order> getAllOrders() {
        try {
            log.info("开始获取所有订单");
            return orderRepository.findAll();
        } catch (Exception e) {
            log.error("获取所有订单失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取订单列表失败: " + e.getMessage(), e);
        }
    }
    
    @Transactional(readOnly = true)
    public List<Order> getOrdersByType(String type) {
        log.info("获取类型为{}的订单", type);
        
        try {
            // 根据类型获取订单
            if ("customer".equalsIgnoreCase(type) || "SALE".equalsIgnoreCase(type)) {
                return orderRepository.findByOrderType("SALE");
            } else if ("purchase".equalsIgnoreCase(type) || "PURCHASE".equalsIgnoreCase(type)) {
                return orderRepository.findByOrderType("PURCHASE");
            } else {
                log.warn("未知的订单类型: {}", type);
                return java.util.Collections.emptyList();
            }
        } catch (Exception e) {
            log.error("获取类型为{}的订单列表失败: {}", type, e.getMessage(), e);
            throw new RuntimeException("获取订单列表失败: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Page<Order> getOrdersByType(String type, Integer page, Integer size) {
        try {
            log.info("获取类型为{}的订单: page={}, size={}", type, page, size);
            
            try {
                Pageable pageable = PageRequest.of(page, size);
                
                // 根据类型获取订单
                if ("customer".equalsIgnoreCase(type) || "SALE".equalsIgnoreCase(type)) {
                    return orderRepository.findByOrderType("SALE", pageable);
                } else if ("purchase".equalsIgnoreCase(type) || "PURCHASE".equalsIgnoreCase(type)) {
                    return orderRepository.findByOrderType("PURCHASE", pageable);
                } else {
                    log.warn("未知的订单类型: {}", type);
                    return Page.empty();
                }
            } catch (Exception e) {
                log.error("获取类型为{}的订单列表失败: {}", type, e.getMessage(), e);
                throw new RuntimeException("获取订单列表失败: " + e.getMessage());
            }
        } catch (Exception e) {
            log.error("获取类型为{}的订单列表时发生错误: {}", type, e.getMessage(), e);
            throw new RuntimeException("获取订单列表失败: " + e.getMessage());
        }
    }

    @Transactional
    public void deleteOrder(Long id) {
        orderRepository.deleteById(id);
    }

    @Transactional
    public Order confirmOrder(Long id, float freight) {
        Order order = orderRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("销售订单不存在"));
        
        if (OrderStatus.COMPLETED.equals(order.getStatus())) {
            throw new RuntimeException("销售订单请勿重复确认");
        }
        
        order.setStatus(OrderStatus.COMPLETED);
        order.setFreight(freight);
        return orderRepository.save(order);
    }
}