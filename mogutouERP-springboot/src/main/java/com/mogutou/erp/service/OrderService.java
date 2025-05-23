package com.mogutou.erp.service;

import com.mogutou.erp.entity.Order;
import com.mogutou.erp.entity.OrderGoods;
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
    public Order createOrder(Order order, List<OrderGoods> goods) {
        log.info("开始创建订单，订单类型: {}", order.getOrderType());
        
        try {
            // 生成订单编号
            if (order.getOrderNo() == null || order.getOrderNo().isEmpty()) {
                String orderNoPrefix = "ORD";
                order.setOrderNo(orderNoPrefix + System.currentTimeMillis());
                log.info("生成订单编号: {}", order.getOrderNo());
            }
            
            // 处理订单类型字段
            String orderType = order.getOrderType();
            if (orderType == null || orderType.isEmpty()) {
                if (order.getType() != null) {
                    orderType = order.getType().equalsIgnoreCase("customer") ? "SALE" : "PURCHASE";
                    order.setOrderType(orderType);
                    log.info("从type字段设置订单类型: {}", orderType);
                } else {
                    order.setOrderType("SALE"); // 默认为销售订单
                    log.info("设置默认订单类型: SALE");
                }
            }
            
            // 设置商品关联
            if (goods != null && !goods.isEmpty()) {
                log.info("处理订单商品，数量: {}", goods.size());
                for (OrderGoods item : goods) {
                    if (item.getGoods() == null) {
                        throw new RuntimeException("订单商品中的商品对象不能为空");
                    }
                    
                    // 处理商品关联 - 如果只有名称没有ID，尝试根据名称查找或创建商品
                    Goods goodsItem = item.getGoods();
                    if (goodsItem.getId() == null && goodsItem.getName() != null) {
                        List<Goods> existingGoods = goodsRepository.findByName(goodsItem.getName());
                        
                        if (!existingGoods.isEmpty()) {
                            goodsItem = existingGoods.get(0);
                            log.info("使用现有商品: {}", goodsItem.getName());
                        } else {
                            // 创建新商品
                            goodsItem.setCode("G" + System.currentTimeMillis());
                            goodsItem.setStock(0);
                            goodsItem.setStatus(1);
                            goodsItem = goodsRepository.save(goodsItem);
                            log.info("创建新商品: {}", goodsItem.getName());
                        }
                        item.setGoods(goodsItem);
                    }
                    
                    item.setOrder(order);
                }
                order.setGoods(goods);
            } else {
                order.setGoods(new java.util.ArrayList<>());
            }
            
            // 保存订单
            return orderRepository.save(order);
        } catch (Exception e) {
            log.error("创建订单失败: {}", e.getMessage(), e);
            throw new RuntimeException("创建订单失败: " + e.getMessage(), e);
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
                    // 获取销售订单
                    return orderRepository.findByOrderType("SALE", pageable);
                } else if ("purchase".equalsIgnoreCase(type) || "PURCHASE".equalsIgnoreCase(type)) {
                    // 获取采购订单
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
            .orElseThrow(() -> new RuntimeException("订单不存在"));
        
        if ("COMPLETED".equals(order.getStatus())) {
            throw new RuntimeException("订单请勿重复确认");
        }
        
        order.setStatus("COMPLETED");
        order.setFreight(freight);
        return orderRepository.save(order);
    }
}