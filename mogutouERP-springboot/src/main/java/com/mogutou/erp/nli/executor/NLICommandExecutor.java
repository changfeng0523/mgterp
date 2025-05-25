package com.mogutou.erp.nli.executor;

import com.fasterxml.jackson.databind.JsonNode;
import com.mogutou.erp.entity.Goods;
import com.mogutou.erp.entity.Order;
import com.mogutou.erp.entity.OrderGoods;
import com.mogutou.erp.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
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

            case "query_order":
                return handleQueryOrder(root);

            case "confirm_order":
                return handleConfirmOrder(root);

            default:
                return "❓ 未知操作类型：" + action;
        }
    }

    private String handleCreateOrder(JsonNode root) {
        try {
            String orderType = root.path("order_type").asText("PURCHASE").toUpperCase();
            String supplier = root.path("supplier").asText("");
            JsonNode goodsArray = root.path("goods");

            if (supplier.isEmpty() || !goodsArray.isArray() || goodsArray.size() == 0) {
                return "❌ 缺少必要信息（supplier 或 goods）";
            }

            Order order = new Order();
            order.setOrderType(orderType);
            order.setCustomerName(supplier);

            List<OrderGoods> goodsList = new ArrayList<>();
            float totalAmount = 0;

            for (JsonNode item : goodsArray) {
                String product = item.path("product").asText("");
                int quantity = item.path("quantity").asInt(0);
                float unitPrice = (float) item.path("unit_price").asDouble(0.0);

                if (product.isEmpty() || quantity <= 0 || unitPrice <= 0) {
                    return "❌ 商品信息不完整，请提供名称、数量、单价";
                }

                Goods goods = new Goods();
                goods.setName(product);

                OrderGoods og = new OrderGoods();
                og.setGoods(goods);
                og.setQuantity(quantity);
                og.setUnitPrice(unitPrice);
                og.setTotalPrice(unitPrice * quantity);

                goodsList.add(og);
                totalAmount += og.getTotalPrice();
            }

            order.setAmount(totalAmount);
            orderService.createOrder(order, goodsList);
            return "✅ " + (orderType.equals("SALE") ? "销售" : "采购") + "订单已创建，总金额 ¥" + totalAmount;

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

    private String handleQueryOrder(JsonNode root) {
        try {
            String keyword = root.path("keyword").asText(null);
            String type = root.path("order_type").asText(null);

            // 获取所有订单（不分页）
            List<Order> orders = new ArrayList<>();
            orders.addAll(orderService.getOrdersByType("SALE", 0, 100).getContent());
            orders.addAll(orderService.getOrdersByType("PURCHASE", 0, 100).getContent());

            if (keyword != null && !keyword.isEmpty()) {
                orders = orders.stream().filter(order ->
                        (order.getCustomerName() != null && order.getCustomerName().contains(keyword)) ||
                                (order.getOrderNo() != null && order.getOrderNo().contains(keyword)) ||
                                order.getGoods().stream().anyMatch(g ->
                                        g.getGoods().getName() != null && g.getGoods().getName().contains(keyword))
                ).toList();
            }

            if (type != null && !type.isEmpty()) {
                orders = orders.stream().filter(order ->
                        order.getOrderType().equalsIgnoreCase(type)
                ).toList();
            }

            if (orders.isEmpty()) {
                return "📭 没有找到相关订单。";
            }

            StringBuilder sb = new StringBuilder("📦 查询结果（" + (type != null ? "类型：" + type : "关键词：" + keyword) + "）：\n");
            for (Order o : orders) {
                sb.append(" - ID: ").append(o.getId())
                        .append("，订单号: ").append(o.getOrderNo())
                        .append("，客户: ").append(o.getCustomerName())
                        .append("，状态: ").append(o.getStatus())
                        .append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            return "❌ 查询订单失败: " + e.getMessage();
        }
    }


    private String handleConfirmOrder(JsonNode root) {
        try {
            long orderId = root.path("order_id").asLong();
            float freight = (float) root.path("freight").asDouble();

            if (orderId <= 0 || freight < 0) {
                return "❌ 缺少确认订单所需的字段（order_id, freight）";
            }

            Order confirmed = orderService.confirmOrder(orderId, freight);
            return "✅ 已确认订单：" + confirmed.getOrderNo() + "，运费：" + freight;

        } catch (Exception e) {
            return "❌ 确认订单失败: " + e.getMessage();
        }
    }
}