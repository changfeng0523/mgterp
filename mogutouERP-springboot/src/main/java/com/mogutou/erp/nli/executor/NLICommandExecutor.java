package com.mogutou.erp.nli.executor;

import com.fasterxml.jackson.databind.JsonNode;
import com.mogutou.erp.entity.Goods;
import com.mogutou.erp.entity.Order;
import com.mogutou.erp.entity.OrderGoods;
import com.mogutou.erp.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class NLICommandExecutor {

    @Autowired
    private OrderService orderService;

    public String execute(JsonNode root) {
        String action = root.path("action").asText();

        switch (action) {
            case "create_order":
                return handleCreateOrder(root);

            case "delete_order":
                return handleDeleteOrder(root);

            default:
                return "❓ 未知操作类型：" + action;
        }


    }

    private String handleCreateOrder(JsonNode root) {
        try {
            String supplier = root.path("supplier").asText("");
            String product = root.path("product").asText("");
            int amount = root.path("amount").asInt(0);

            if (supplier.isEmpty() || product.isEmpty() || amount <= 0) {
                return "❌ 缺少创建订单所需的信息（supplier/product/amount）";
            }

            Order order = new Order();
            order.setOrderType("PURCHASE"); // 默认为采购订单
            order.setCustomerName(supplier);

            // 构造商品列表
            Goods goods = new Goods();
            goods.setName(product);

            OrderGoods orderGoods = new OrderGoods();
            orderGoods.setGoods(goods);
            orderGoods.setQuantity(amount);

            List<OrderGoods> goodsList = new ArrayList<>();
            goodsList.add(orderGoods);
            System.out.println("🧪 supplier: " + supplier);
            System.out.println("🧪 product: " + product);
            System.out.println("🧪 amount: " + amount);


            orderService.createOrder(order, goodsList);
            return "✅ 采购订单已创建：" + product + " x" + amount;



        } catch (Exception e) {
            return "❌ 创建订单失败: " + e.getMessage();
        }
    }

    private String handleDeleteOrder(JsonNode root) {
        try {
            long orderId = root.path("order_id").asLong();
            if (orderId <= 0) {
                return "❌ 缺少有效的订单 ID（order_id）";
            }

            orderService.deleteOrder(orderId);
            return "✅ 已删除订单 ID：" + orderId;

        } catch (Exception e) {
            return "❌ 删除订单失败: " + e.getMessage();
        }
    }
}
