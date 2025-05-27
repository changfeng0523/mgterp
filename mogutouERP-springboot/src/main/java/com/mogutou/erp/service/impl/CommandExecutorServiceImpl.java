package com.mogutou.erp.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.mogutou.erp.entity.Goods;
import com.mogutou.erp.entity.Order;
import com.mogutou.erp.entity.OrderGoods;
import com.mogutou.erp.service.OrderService;
import com.mogutou.erp.service.CommandExecutorService;
import com.mogutou.erp.service.external.DeepSeekAIService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 命令执行服务实现类
 * 负责处理各种业务命令的执行
 */
@Service
public class CommandExecutorServiceImpl implements CommandExecutorService {

    @Autowired
    private OrderService orderService;

    @Autowired
    private DeepSeekAIService deepSeekAIService;

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String execute(JsonNode root) {
        String action = root.path("action").asText();
        
        System.out.println("🎮 执行指令: " + action + " - " + root.toString());

        return switch (action) {
            case "create_order" -> handleCreateOrder(root);
            case "delete_order" -> handleDeleteOrder(root);
            case "query_order" -> handleQueryOrder(root);
            case "confirm_order" -> handleConfirmOrder(root);
            case "query_sales" -> handleQuerySales(root);
            case "query_inventory" -> handleQueryInventory(root);
            case "analyze_finance" -> handleAnalyzeFinance(root);
            case "analyze_order" -> handleAnalyzeOrder(root);
            default -> "❓ 未知操作类型：" + action + "\n\n💡 支持的操作：\n• create_order (创建订单)\n• query_order (查询订单)\n• delete_order (删除订单)\n• confirm_order (确认订单)\n• query_sales (销售查询)\n• query_inventory (库存查询)\n• analyze_finance (财务分析)\n• analyze_order (订单分析)";
        };
    }

    /**
     * 智能创建订单 - 强化字段提取和容错处理
     */
    private String handleCreateOrder(JsonNode root) {
        try {
            System.out.println("🔍 解析订单创建请求: " + root.toString());
            
            // 智能提取订单类型
            String orderType = smartExtractOrderType(root);
            
            // 智能提取客户信息 - 更加宽松的处理
            String customerName = smartExtractCustomer(root);
            if (customerName.isEmpty()) {
                // 如果没有客户信息，使用默认客户或要求用户补充
                System.out.println("⚠️ 未提取到客户信息，使用默认处理");
                return "❌ 缺少客户信息\n\n💡 请这样表达：\n• '为张三创建订单，苹果10个单价5元'\n• '给李四下单，橙子20个每个3元'\n• '帮王五买香蕉15个单价2元'";
            }
            
            // 智能提取商品列表
            List<ProductInfo> productList = smartExtractProducts(root);
            if (productList.isEmpty()) {
                System.out.println("❌ 未能提取到商品信息");
                return "❌ 缺少商品信息\n\n💡 请这样表达：\n• '苹果10个单价5元'\n• '橙子，数量20，单价3元'\n• '买香蕉15个每个2块钱'\n\n📝 完整示例：'为张三创建订单，苹果10个单价5元'";
            }

            // 创建订单对象
            Order order = new Order();
            order.setOrderType(orderType);
            order.setCustomerName(customerName);
            order.setCreatedAt(LocalDateTime.now());

            List<OrderGoods> goodsList = new ArrayList<>();
            float totalAmount = 0;
            int totalItems = 0;

            // 处理商品列表
            for (ProductInfo product : productList) {
                // 验证产品信息
                if (product.quantity <= 0) {
                    return String.format("❌ 商品'%s'的数量无效\n💡 请提供正确的数量信息", product.name);
                }
                
                // 允许单价为0，后续可以补充
                if (product.unitPrice < 0) {
                    product.unitPrice = 0; // 设为0，表示待补充价格
                }

                // 创建商品和订单商品关联
                Goods goods = new Goods();
                goods.setName(product.name);

                OrderGoods orderGoods = new OrderGoods();
                orderGoods.setGoods(goods);
                orderGoods.setQuantity(product.quantity);
                orderGoods.setUnitPrice(product.unitPrice);
                orderGoods.setTotalPrice(product.unitPrice * product.quantity);

                goodsList.add(orderGoods);
                totalAmount += orderGoods.getTotalPrice();
                totalItems += product.quantity;
            }

            order.setAmount(totalAmount);
            Order savedOrder = orderService.createOrder(order, goodsList);

            // 生成简洁智能回复
            String orderTypeDesc = order.getOrderType().equals("PURCHASE") ? "采购" : "销售";
            String partnerLabel = order.getOrderType().equals("PURCHASE") ? "供应商" : "客户";
            String typeIcon = order.getOrderType().equals("PURCHASE") ? "📦" : "💰";
            
            StringBuilder result = new StringBuilder();
            result.append(String.format("✅ %s%s订单创建成功！\n\n", typeIcon, orderTypeDesc));
            result.append(String.format("📋 订单号：%s | %s：%s | 金额：¥%.2f\n", 
                savedOrder.getOrderNo(), partnerLabel, customerName, totalAmount));
            
            // 简化的商品明细
            result.append(String.format("📦 商品：%d种/%d件", goodsList.size(), totalItems));
            if (goodsList.size() <= 2) {
                result.append(" (");
                for (int i = 0; i < goodsList.size(); i++) {
                    ProductInfo product = productList.get(i);
                    result.append(product.name).append("×").append(product.quantity);
                    if (i < goodsList.size() - 1) result.append(", ");
                }
                result.append(")");
            }
            result.append("\n\n💡 可以说'查询订单").append(savedOrder.getOrderNo()).append("'查看详情");
            
            return result.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return "❌ 创建订单失败：" + e.getMessage() + 
                "\n\n💡 请尝试更清晰的表达，如：'为张三创建订单，商品苹果10个单价5元'";
        }
    }

    /**
     * 智能提取订单类型 - 增强版
     */
    private String smartExtractOrderType(JsonNode root) {
        // 1. 尝试从JSON字段中提取
        String[] typeFields = {"order_type", "type", "orderType", "order_type"};
        for (String field : typeFields) {
            if (root.has(field)) {
                String type = root.get(field).asText().toUpperCase();
                if (type.equals("SALE") || type.equals("PURCHASE")) {
                    System.out.println("📦 从字段提取订单类型: " + type);
                    return type;
                }
            }
        }
        
        // 2. 从原始输入中基于关键词识别
        if (root.has("original_input")) {
            String input = root.get("original_input").asText().toLowerCase();
            String detectedType = detectOrderTypeFromText(input);
            if (!detectedType.isEmpty()) {
                System.out.println("📦 从文本识别订单类型: " + detectedType);
                return detectedType;
            }
        }
        
        // 3. 尝试从其他字段推断
        String allText = root.toString().toLowerCase();
        String inferredType = detectOrderTypeFromText(allText);
        if (!inferredType.isEmpty()) {
            System.out.println("📦 从JSON推断订单类型: " + inferredType);
            return inferredType;
        }
        
        // 4. 默认为销售订单
        System.out.println("📦 使用默认订单类型: SALE");
        return "SALE";
    }

    /**
     * 从文本中检测订单类型
     */
    private String detectOrderTypeFromText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }
        
        // 采购关键词 - 优先级更高，因为销售是默认
        String[] purchaseKeywords = {
            "采购", "进货", "购买", "进料", "补货", "订购", "进仓", "入库",
            "从供应商", "向厂家", "向供应商", "从厂家", "供应商", "厂家", 
            "批发", "进购", "采买", "购进", "收货", "进材料", "买材料"
        };
        
        for (String keyword : purchaseKeywords) {
            if (text.contains(keyword)) {
                return "PURCHASE";
            }
        }
        
        // 销售关键词
        String[] saleKeywords = {
            "销售", "出售", "卖给", "售给", "发货", "交付", "为客户", "给客户",
            "销", "卖", "售", "出货", "零售", "批售", "出售给", "卖出",
            "客户订单", "销售订单", "出库", "发给"
        };
        
        for (String keyword : saleKeywords) {
            if (text.contains(keyword)) {
                return "SALE";
            }
        }
        
        return ""; // 无法确定
    }

    /**
     * 智能提取客户信息
     */
    private String smartExtractCustomer(JsonNode root) {
        // 尝试多种字段名和格式
        String[] customerFields = {"customer", "customer_name", "customerName", "client", "supplier", "供应商", "客户"};
        
        for (String field : customerFields) {
            if (root.has(field) && !root.get(field).asText().trim().isEmpty()) {
                return root.get(field).asText().trim();
            }
        }
        
        // 尝试从原始指令中提取（如果有的话）
        if (root.has("original_input")) {
            String input = root.get("original_input").asText();
            // 使用正则表达式匹配常见模式
            return extractCustomerFromText(input);
        }
        
        return "";
    }

    /**
     * 从文本中提取客户名称 - 增强版
     */
    private String extractCustomerFromText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }
        
        // 更全面的客户表达模式 - 新增更多匹配模式
        String[] patterns = {
            // 基础创建模式
            "为\\s*([\\u4e00-\\u9fa5a-zA-Z]+)\\s*创建",     // 为张三创建
            "给\\s*([\\u4e00-\\u9fa5a-zA-Z]+)\\s*创建",     // 给张三创建 
            "帮\\s*([\\u4e00-\\u9fa5a-zA-Z]+)\\s*创建",     // 帮张三创建
            "为\\s*([\\u4e00-\\u9fa5a-zA-Z]+)\\s*下",       // 为张三下单
            "给\\s*([\\u4e00-\\u9fa5a-zA-Z]+)\\s*下",       // 给张三下单
            "帮\\s*([\\u4e00-\\u9fa5a-zA-Z]+)\\s*买",       // 帮张三买
            
            // 🆕 新增：从XX处/那里购买的模式
            "从\\s*([\\u4e00-\\u9fa5a-zA-Z]+)\\s*那里",     // 从哈振宇那里
            "从\\s*([\\u4e00-\\u9fa5a-zA-Z]+)\\s*这里",     // 从张三这里
            "从\\s*([\\u4e00-\\u9fa5a-zA-Z]+)\\s*处",       // 从李四处
            "从\\s*([\\u4e00-\\u9fa5a-zA-Z]+)\\s*买",       // 从王五买
            "从\\s*([\\u4e00-\\u9fa5a-zA-Z]+)\\s*购买",     // 从张三购买
            "从\\s*([\\u4e00-\\u9fa5a-zA-Z]+)\\s*进",       // 从供应商进
            "向\\s*([\\u4e00-\\u9fa5a-zA-Z]+)\\s*买",       // 向厂家买
            "向\\s*([\\u4e00-\\u9fa5a-zA-Z]+)\\s*购买",     // 向供应商购买
            
            // 🆕 新增：销售给XX的模式  
            "卖给\\s*([\\u4e00-\\u9fa5a-zA-Z]+)",           // 卖给张三
            "售给\\s*([\\u4e00-\\u9fa5a-zA-Z]+)",           // 售给李四
            "发给\\s*([\\u4e00-\\u9fa5a-zA-Z]+)",           // 发给王五
            "交付给\\s*([\\u4e00-\\u9fa5a-zA-Z]+)",         // 交付给客户
            "出售给\\s*([\\u4e00-\\u9fa5a-zA-Z]+)",         // 出售给张三
            
            // 标准格式
            "客户[:：]?\\s*([\\u4e00-\\u9fa5a-zA-Z]+)",      // 客户：张三
            "供应商[:：]?\\s*([\\u4e00-\\u9fa5a-zA-Z]+)",    // 供应商：张三
            "([\\u4e00-\\u9fa5a-zA-Z]+)\\s*的订单",          // 张三的订单
            "([\\u4e00-\\u9fa5a-zA-Z]+)\\s*要",             // 张三要
            "([\\u4e00-\\u9fa5a-zA-Z]+)\\s*订购",           // 张三订购
            
            // 🆕 新增：灵活的中文表达模式
            "([\\u4e00-\\u9fa5a-zA-Z]+)\\s*说",             // 张三说
            "([\\u4e00-\\u9fa5a-zA-Z]+)\\s*需要",           // 李四需要  
            "([\\u4e00-\\u9fa5a-zA-Z]+)\\s*想要",           // 王五想要
            "和\\s*([\\u4e00-\\u9fa5a-zA-Z]+)\\s*",         // 和张三
            "跟\\s*([\\u4e00-\\u9fa5a-zA-Z]+)\\s*"          // 跟李四
        };
        
        for (String pattern : patterns) {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(text);
            if (m.find()) {
                String customerName = m.group(1).trim();
                // 过滤掉一些明显不是客户名的词 - 扩展过滤词汇
                if (!isInvalidCustomerName(customerName)) {
                    System.out.println("🎯 从文本中提取到客户: " + customerName);
                    return customerName;
                }
            }
        }
        
        return "";
    }
    
    /**
     * 🆕 判断是否为无效的客户名
     */
    private boolean isInvalidCustomerName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return true;
        }
        
        // 扩展的无效客户名词汇列表
        String[] invalidNames = {
            // 操作词汇
            "创建", "订单", "下单", "购买", "买", "卖", "销售", "查询", "删除",
            // 商品词汇
            "商品", "苹果", "橙子", "香蕉", "梨子", "葡萄", "西瓜", "草莓", "芒果", "桃子", "樱桃",
            "大米", "面粉", "面条", "馒头", "包子", "饺子", "汤圆", "水", "饮料", "牛奶",
            "鸡蛋", "鱼", "肉", "鸡", "鸭", "猪肉", "牛肉", "羊肉",
            "青菜", "白菜", "萝卜", "土豆", "西红柿", "黄瓜", "茄子",
            // 数量单位词汇
            "数量", "单价", "价格", "元", "块", "钱", "个", "件", "只", "瓶", "袋", "箱", "斤", "公斤",
            // 其他系统词汇
            "订单", "客户", "供应商", "那里", "这里", "地方", "处"
        };
        
        String lowerName = name.toLowerCase();
        for (String invalid : invalidNames) {
            if (lowerName.equals(invalid) || lowerName.equals(invalid.toLowerCase())) {
                return true;
            }
        }
        
        // 检查是否只包含数字（可能是误识别的数量）
        if (name.matches("^\\d+$")) {
            return true;
        }
        
        return false;
    }

    /**
     * 智能提取商品列表 - 增强版
     */
    private List<ProductInfo> smartExtractProducts(JsonNode root) {
        List<ProductInfo> products = new ArrayList<>();
        
        // 尝试从products数组提取
        String[] productArrayFields = {"products", "goods", "items", "商品", "货物"};
        for (String field : productArrayFields) {
            if (root.has(field) && root.get(field).isArray()) {
                JsonNode array = root.get(field);
                for (JsonNode item : array) {
                    ProductInfo product = extractProductFromNode(item);
                    if (product != null) {
                        System.out.println("🛒 从数组提取商品: " + product.name + " x" + product.quantity + " @" + product.unitPrice);
                        products.add(product);
                    }
                }
                break;
            }
        }
        
        // 如果没有找到数组，尝试单个产品字段
        if (products.isEmpty()) {
            ProductInfo singleProduct = extractSingleProduct(root);
            if (singleProduct != null) {
                System.out.println("🛒 提取单个商品: " + singleProduct.name + " x" + singleProduct.quantity + " @" + singleProduct.unitPrice);
                products.add(singleProduct);
            }
        }
        
        // 如果还是没有商品，尝试从原始输入中用正则表达式提取
        if (products.isEmpty() && root.has("original_input")) {
            String input = root.get("original_input").asText();
            ProductInfo extractedProduct = extractProductFromText(input);
            if (extractedProduct != null) {
                System.out.println("🛒 从文本提取商品: " + extractedProduct.name + " x" + extractedProduct.quantity + " @" + extractedProduct.unitPrice);
                products.add(extractedProduct);
            }
        }
        
        return products;
    }

    /**
     * 从文本中提取商品信息 - 正则表达式方法
     */
    private ProductInfo extractProductFromText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        
        // 🆕 大幅扩展商品名提取：涵盖更多常见商品
        String[] productPatterns = {
            // 🆕 饮品类 - 新增
            "(水|饮用水|矿泉水|纯净水|饮料|可乐|雪碧|果汁|茶|咖啡|奶茶|豆浆)",
            
            // 水果类 - 保持原有
            "(苹果|橙子|香蕉|梨子|葡萄|西瓜|草莓|芒果|桃子|樱桃|柠檬|橘子|柚子|猕猴桃|火龙果|榴莲)",
            
            // 🆕 主食类 - 扩展
            "(大米|面粉|面条|馒头|包子|饺子|汤圆|米饭|面包|饼干|蛋糕|粥|粉条|河粉|方便面)",
            
            // 🆕 乳制品类 - 扩展  
            "(鸡蛋|牛奶|酸奶|奶酪|黄油|奶粉|豆奶|酸奶|乳制品)",
            
            // 🆕 肉类 - 扩展
            "(鱼|肉|鸡|鸭|猪肉|牛肉|羊肉|火腿|香肠|腊肉|培根|鸡翅|鸡腿|排骨)",
            
            // 🆕 蔬菜类 - 扩展
            "(青菜|白菜|萝卜|土豆|西红柿|黄瓜|茄子|豆角|辣椒|洋葱|蒜|姜|韭菜|菠菜|芹菜)",
            
            // 🆕 日用品类 - 新增
            "(纸巾|卫生纸|洗发水|沐浴露|牙膏|牙刷|毛巾|香皂|洗衣粉|洗洁精)",
            
            // 🆕 通用商品词 - 灵活匹配
            "([\\u4e00-\\u9fa5]{1,4}(?:商品|产品|货物|物品|用品))",  // XX商品、XX产品等
            "([\\u4e00-\\u9fa5]{2,6})"  // 2-6个中文字符的通用商品名
        };
        
        String productName = "";
        for (String pattern : productPatterns) {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(text);
            if (m.find()) {
                String candidate = m.group(1);
                // 🆕 添加更严格的商品名验证
                if (isValidProductName(candidate)) {
                    productName = candidate;
                    break;
                }
            }
        }
        
        if (productName.isEmpty()) {
            return null;
        }
        
        // 🆕 大幅优化数量提取：支持更多表达方式
        int quantity = 0;
        String[] quantityPatterns = {
            // 基础数量模式
            "(\\d+)\\s*个\\s*" + productName,               // 5个水
            "(\\d+)\\s*瓶\\s*" + productName,               // 5瓶水
            "(\\d+)\\s*件\\s*" + productName,               // 5件商品
            "(\\d+)\\s*只\\s*" + productName,               // 5只鸡
            "(\\d+)\\s*袋\\s*" + productName,               // 5袋大米
            "(\\d+)\\s*箱\\s*" + productName,               // 5箱饮料
            "(\\d+)\\s*斤\\s*" + productName,               // 5斤苹果
            "(\\d+)\\s*公斤\\s*" + productName,             // 5公斤米
            
            // 🆕 倒序模式：商品+数量
            productName + "\\s*(\\d+)\\s*个",               // 水5个
            productName + "\\s*(\\d+)\\s*瓶",               // 水5瓶
            productName + "\\s*(\\d+)\\s*件",               // 商品5件
            
            // 🆕 灵活的中文表达
            "(\\d+)\\s*" + productName,                     // 5水（简化表达）
            productName + "\\s*(\\d+)",                     // 水5（简化表达）
            "买\\s*(\\d+)\\s*" + productName,              // 买5个水
            "要\\s*(\\d+)\\s*" + productName,              // 要5瓶水
            "需要\\s*(\\d+)\\s*" + productName,            // 需要5件商品
            
            // 通用数量模式
            "数量\\s*(\\d+)",                               // 数量5
            "(\\d+)\\s*(?:个|瓶|件|只|袋|箱|斤|公斤)",      // 数字+单位
        };
        
        for (String pattern : quantityPatterns) {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(text);
            if (m.find()) {
                try {
                    quantity = Integer.parseInt(m.group(1));
                    if (quantity > 0) {
                        break; // 找到有效数量就停止
                    }
                } catch (NumberFormatException e) {
                    // 忽略解析错误，继续尝试下一个模式
                }
            }
        }
        
        // 🆕 大幅优化单价提取：支持更多价格表达
        float unitPrice = 0.0f;
        String[] pricePatterns = {
            // 🆕 "一瓶X元"、"每个X元"模式
            "一\\s*瓶\\s*(\\d+(?:\\.\\d+)?)\\s*元",           // 一瓶3元
            "一\\s*个\\s*(\\d+(?:\\.\\d+)?)\\s*元",           // 一个5元
            "一\\s*件\\s*(\\d+(?:\\.\\d+)?)\\s*元",           // 一件10元
            "一\\s*只\\s*(\\d+(?:\\.\\d+)?)\\s*元",           // 一只20元
            "一\\s*袋\\s*(\\d+(?:\\.\\d+)?)\\s*元",           // 一袋30元
            "一\\s*斤\\s*(\\d+(?:\\.\\d+)?)\\s*元",           // 一斤8元
            
            "每\\s*瓶\\s*(\\d+(?:\\.\\d+)?)\\s*元",           // 每瓶3元
            "每\\s*个\\s*(\\d+(?:\\.\\d+)?)\\s*元",           // 每个5元
            "每\\s*件\\s*(\\d+(?:\\.\\d+)?)\\s*元",           // 每件10元
            "每\\s*只\\s*(\\d+(?:\\.\\d+)?)\\s*元",           // 每只20元
            "每\\s*袋\\s*(\\d+(?:\\.\\d+)?)\\s*元",           // 每袋30元
            "每\\s*斤\\s*(\\d+(?:\\.\\d+)?)\\s*元",           // 每斤8元
            
            // 基础价格模式
            "(\\d+(?:\\.\\d+)?)\\s*元\\s*一",                // 3元一瓶
            "(\\d+(?:\\.\\d+)?)\\s*块\\s*一",                // 3块一个
            "(\\d+(?:\\.\\d+)?)\\s*钱\\s*一",                // 3钱一件
            
            // 标准价格模式 - 保持原有
            "(\\d+(?:\\.\\d+)?)\\s*元",                      // 3元
            "(\\d+(?:\\.\\d+)?)\\s*块",                      // 3块
            "(\\d+(?:\\.\\d+)?)\\s*钱",                      // 3钱
            "单价\\s*(\\d+(?:\\.\\d+)?)",                    // 单价3
            "价格\\s*(\\d+(?:\\.\\d+)?)",                    // 价格3
            
            // 🆕 通用价格模式
            "([0-9]+(?:\\.[0-9]+)?)\\s*(?:元|块|钱|￥|¥)",   // 支持￥符号
        };
        
        for (String pattern : pricePatterns) {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(text);
            if (m.find()) {
                try {
                    unitPrice = Float.parseFloat(m.group(1));
                    if (unitPrice >= 0) {
                        break; // 找到有效价格就停止
                    }
                } catch (NumberFormatException e) {
                    // 忽略解析错误，继续尝试下一个模式
                }
            }
        }
        
        // 如果至少有商品名和数量，就创建商品信息
        if (!productName.isEmpty() && quantity > 0) {
            System.out.println(String.format("🛒 成功提取商品信息: %s × %d @ ¥%.2f", productName, quantity, unitPrice));
            return new ProductInfo(productName, quantity, unitPrice);
        }
        
        return null;
    }
    
    /**
     * 🆕 验证商品名是否有效
     */
    private boolean isValidProductName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        
        // 过滤明显不是商品的词汇
        String[] invalidProducts = {
            "创建", "订单", "查询", "删除", "买", "卖", "购买", "销售",
            "客户", "供应商", "数量", "单价", "价格", "元", "块", "钱",
            "个", "件", "只", "瓶", "袋", "箱", "斤", "公斤", "那里", "这里", "处"
        };
        
        String lowerName = name.toLowerCase();
        for (String invalid : invalidProducts) {
            if (lowerName.equals(invalid) || lowerName.equals(invalid.toLowerCase())) {
                return false;
            }
        }
        
        // 检查长度：商品名应该在合理范围内
        if (name.length() < 1 || name.length() > 10) {
            return false;
        }
        
        // 检查是否只包含数字
        if (name.matches("^\\d+$")) {
            return false;
        }
        
        return true;
    }

    /**
     * 从单个节点提取产品信息
     */
    private ProductInfo extractProductFromNode(JsonNode node) {
        String name = getStringValue(node, "name", "product", "productName", "商品名", "产品名");
        int quantity = getIntValue(node, "quantity", "qty", "count", "数量", "个数");
        float unitPrice = getFloatValue(node, "unit_price", "price", "unitPrice", "单价", "价格");
        
        if (!name.isEmpty() && quantity > 0) {
            return new ProductInfo(name, quantity, Math.max(0, unitPrice));
        }
        
        return null;
    }

    /**
     * 提取单个产品信息（当没有数组时）
     */
    private ProductInfo extractSingleProduct(JsonNode root) {
        String name = getStringValue(root, "product", "product_name", "商品", "商品名");
        int quantity = getIntValue(root, "quantity", "qty", "数量");
        float unitPrice = getFloatValue(root, "unit_price", "price", "单价");
        
        if (!name.isEmpty() && quantity > 0) {
            return new ProductInfo(name, quantity, Math.max(0, unitPrice));
        }
        
        return null;
    }

    /**
     * 产品信息内部类
     */
    private static class ProductInfo {
        String name;
        int quantity;
        float unitPrice;
        
        ProductInfo(String name, int quantity, float unitPrice) {
            this.name = name;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
        }
    }

    /**
     * 删除订单
     */
    private String handleDeleteOrder(JsonNode root) {
        try {
            long orderId = getLongValue(root, "order_id", "id", "订单ID");
            
            if (orderId <= 0) {
                return "❌ 请提供有效的订单ID\n💡 示例：'删除订单123' 或 '删除ID为123的订单'";
            }

            // 直接删除订单，依靠 OrderService 的异常处理
            try {
                orderService.deleteOrder(orderId);
                return "✅ 订单删除成功\n\n🗑️ 已删除订单ID：" + orderId;
                
            } catch (Exception e) {
                if (e.getMessage().contains("not found") || e.getMessage().contains("不存在") || 
                    e.getMessage().contains("No value present")) {
                    return "❌ 找不到ID为 " + orderId + " 的订单\n💡 请检查订单ID是否正确";
                }
                throw e;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "❌ 删除订单失败：" + e.getMessage();
        }
    }

    /**
     * 查询订单 - 简化版本，提供基础信息
     */
    private String handleQueryOrder(JsonNode root) {
        try {
            String keyword = getStringValue(root, "keyword", "search", "关键词");
            String orderType = getStringValue(root, "order_type", "type", "订单类型");
            int limit = getIntValue(root, "limit", "count", "数量");
            if (limit <= 0) limit = 10; // 默认返回10条

            // 获取订单
            List<Order> allOrders = new ArrayList<>();
            
            if (orderType.isEmpty() || orderType.equalsIgnoreCase("SALE")) {
                Page<Order> salesOrders = orderService.getOrdersByType("SALE", 0, limit);
                allOrders.addAll(salesOrders.getContent());
            }
            
            if (orderType.isEmpty() || orderType.equalsIgnoreCase("PURCHASE")) {
                Page<Order> purchaseOrders = orderService.getOrdersByType("PURCHASE", 0, limit);
                allOrders.addAll(purchaseOrders.getContent());
            }

            // 关键词筛选
            if (!keyword.isEmpty()) {
                allOrders = allOrders.stream()
                    .filter(order -> matchesKeyword(order, keyword))
                    .collect(Collectors.toList());
            }

            // 限制数量
            if (allOrders.size() > limit) {
                allOrders = allOrders.subList(0, limit);
            }

            if (allOrders.isEmpty()) {
                String searchInfo = keyword.isEmpty() ? "" : "关键词'" + keyword + "'";
                return "📭 没有找到相关订单" + (searchInfo.isEmpty() ? "" : "（" + searchInfo + "）") + 
                    "\n\n💡 试试：\n• 查询所有订单\n• 查询销售订单\n• 查询客户张三的订单";
            }

            // 生成简洁的订单列表
            StringBuilder result = new StringBuilder();
            result.append("🔍 查询到 ").append(allOrders.size()).append(" 个订单：\n\n");

            for (int i = 0; i < Math.min(allOrders.size(), 5); i++) { // 最多显示5个
                Order order = allOrders.get(i);
                String typeIcon = order.getOrderType().equals("SALE") ? "💰" : "📦";
                String statusIcon = getStatusIcon(order.getStatus());
                
                result.append(typeIcon).append(" ").append(order.getOrderNo())
                    .append(" | ").append(order.getCustomerName())
                    .append(" | ¥").append(String.format("%.2f", order.getAmount()))
                    .append(" ").append(statusIcon).append("\n");
            }

            if (allOrders.size() > 5) {
                result.append("\n... 还有 ").append(allOrders.size() - 5).append(" 个订单\n");
            }

            result.append("\n💡 如需详细分析，请说：'分析这些订单'");
            return result.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return "❌ 查询订单失败：" + e.getMessage();
        }
    }

    /**
     * 确认订单
     */
    private String handleConfirmOrder(JsonNode root) {
        try {
            long orderId = getLongValue(root, "order_id", "id", "订单ID");
            float freight = getFloatValue(root, "freight", "shipping", "运费");

            if (orderId <= 0) {
                return "❌ 请提供有效的订单ID\n💡 示例：'确认订单123，运费10元'";
            }

            if (freight < 0) {
                return "❌ 运费不能为负数\n💡 如无运费请设为0";
            }

            Order confirmedOrder = orderService.confirmOrder(orderId, freight);
            
            return String.format("✅ 订单确认成功！\n\n📋 确认详情：\n• 订单号：%s\n• 客户：%s\n• 订单金额：¥%.2f\n• 运费：¥%.2f\n• 总计：¥%.2f", 
                confirmedOrder.getOrderNo(), confirmedOrder.getCustomerName(), 
                confirmedOrder.getAmount(), freight, confirmedOrder.getAmount() + freight);

        } catch (Exception e) {
            e.printStackTrace();
            if (e.getMessage().contains("not found") || e.getMessage().contains("不存在")) {
                return "❌ 找不到指定的订单\n💡 请检查订单ID是否正确";
            }
            return "❌ 确认订单失败：" + e.getMessage();
        }
    }

    /**
     * 销售查询
     */
    private String handleQuerySales(JsonNode root) {
        try {
            String timeRange = getStringValue(root, "time_range", "period", "时间范围");
            String customer = getStringValue(root, "customer", "client", "客户");
            
            Page<Order> salesOrders = orderService.getOrdersByType("SALE", 0, 50);
            List<Order> orders = salesOrders.getContent();

            if (!customer.isEmpty()) {
                orders = orders.stream()
                    .filter(order -> order.getCustomerName() != null && 
                            order.getCustomerName().contains(customer))
                    .collect(Collectors.toList());
            }

            if (orders.isEmpty()) {
                return "📊 暂无销售数据" + (customer.isEmpty() ? "" : "（客户：" + customer + "）");
            }

            double totalAmount = orders.stream().mapToDouble(Order::getAmount).sum();
            int totalOrders = orders.size();
            double avgAmount = totalAmount / totalOrders;

            StringBuilder result = new StringBuilder();
            result.append("💰 销售数据统计").append(timeRange.isEmpty() ? "" : "（" + timeRange + "）").append("：\n\n");
            result.append("📈 总销售额：¥").append(String.format("%.2f", totalAmount)).append("\n");
            result.append("📋 订单数量：").append(totalOrders).append("个\n");
            result.append("📊 平均订单金额：¥").append(String.format("%.2f", avgAmount)).append("\n");

            if (!customer.isEmpty()) {
                result.append("👤 客户：").append(customer).append("\n");
            }

            return result.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return "❌ 销售查询失败：" + e.getMessage();
        }
    }

    /**
     * 库存查询（暂时返回提示信息）
     */
    private String handleQueryInventory(JsonNode root) {
        return "📦 库存查询功能开发中...\n\n💡 您可以尝试：\n• 查询订单\n• 查询销售数据\n• 创建新订单";
    }

    /**
     * 财务分析（暂时返回提示信息）
     */
    private String handleAnalyzeFinance(JsonNode root) {
        return "📊 财务分析功能开发中...\n\n💡 您可以尝试：\n• 查询销售数据\n• 查询订单信息";
    }

    /**
     * 订单数据分析 - 智能订单洞察
     */
    private String handleAnalyzeOrder(JsonNode root) {
        try {
            // 获取筛选参数
            String orderType = getStringValue(root, "order_type", "type", "订单类型");
            String customer = getStringValue(root, "customer", "client", "客户");
            int limit = getIntValue(root, "limit", "count", "数量");
            if (limit <= 0) limit = 100; // 分析更多数据

            // 获取所有相关订单数据 - 优化查询性能
            List<Order> allOrders = new ArrayList<>();
            
            System.out.println("🔍 开始查询订单数据，类型: " + orderType + ", 限制: " + limit);
            
            try {
                if (orderType.isEmpty() || orderType.equalsIgnoreCase("SALE")) {
                    Page<Order> salesOrders = orderService.getOrdersByType("SALE", 0, limit);
                    allOrders.addAll(salesOrders.getContent());
                    System.out.println("✅ 销售订单查询完成: " + salesOrders.getContent().size() + "条");
                }
                
                if (orderType.isEmpty() || orderType.equalsIgnoreCase("PURCHASE")) {
                    Page<Order> purchaseOrders = orderService.getOrdersByType("PURCHASE", 0, limit);
                    allOrders.addAll(purchaseOrders.getContent());
                    System.out.println("✅ 采购订单查询完成: " + purchaseOrders.getContent().size() + "条");
                }
            } catch (Exception dbError) {
                System.err.println("❌ 数据库查询失败: " + dbError.getMessage());
                return "❌ 数据查询失败：" + dbError.getMessage() + "\n\n💡 请稍后重试或检查数据库连接";
            }

            // 客户筛选
            if (!customer.isEmpty()) {
                allOrders = allOrders.stream()
                    .filter(order -> order.getCustomerName() != null && 
                            order.getCustomerName().contains(customer))
                    .collect(Collectors.toList());
            }

            if (allOrders.isEmpty()) {
                return "📭 没有找到订单数据进行分析\n\n💡 请先创建一些订单，或调整筛选条件";
            }

            // 构建AI分析请求
            StringBuilder analysisData = new StringBuilder();
            analysisData.append("📊 订单数据分析请求 (共").append(allOrders.size()).append("个订单)\n\n");
            
            // 基础统计数据
            List<Order> salesOrders = allOrders.stream()
                .filter(o -> "SALE".equals(o.getOrderType()))
                .collect(Collectors.toList());
            List<Order> purchaseOrders = allOrders.stream()
                .filter(o -> "PURCHASE".equals(o.getOrderType()))
                .collect(Collectors.toList());

            analysisData.append("📈 销售订单: ").append(salesOrders.size()).append("个\n");
            analysisData.append("📦 采购订单: ").append(purchaseOrders.size()).append("个\n\n");

            // 状态分布
            Map<String, Long> statusStats = allOrders.stream()
                .collect(Collectors.groupingBy(Order::getStatus, Collectors.counting()));
            analysisData.append("📋 订单状态分布:\n");
            statusStats.forEach((status, count) -> 
                analysisData.append("  • ").append(status).append(": ").append(count).append("个\n"));

            // 金额统计
            double totalSalesAmount = salesOrders.stream().mapToDouble(Order::getAmount).sum();
            double totalPurchaseAmount = purchaseOrders.stream().mapToDouble(Order::getAmount).sum();
            
            analysisData.append("\n💰 金额统计:\n");
            analysisData.append("  • 销售总额: ¥").append(String.format("%.2f", totalSalesAmount)).append("\n");
            analysisData.append("  • 采购总额: ¥").append(String.format("%.2f", totalPurchaseAmount)).append("\n");
            analysisData.append("  • 毛利润: ¥").append(String.format("%.2f", totalSalesAmount - totalPurchaseAmount)).append("\n");

            // 客户分析
            Map<String, Long> customerStats = allOrders.stream()
                .filter(o -> o.getCustomerName() != null && !o.getCustomerName().trim().isEmpty())
                .collect(Collectors.groupingBy(Order::getCustomerName, Collectors.counting()));
            
            if (!customerStats.isEmpty()) {
                analysisData.append("\n👥 客户订单分布 (TOP 5):\n");
                customerStats.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(5)
                    .forEach(entry -> 
                        analysisData.append("  • ").append(entry.getKey()).append(": ").append(entry.getValue()).append("个订单\n"));
            }

            // 平均订单金额
            if (!allOrders.isEmpty()) {
                double avgAmount = allOrders.stream().mapToDouble(Order::getAmount).average().orElse(0);
                analysisData.append("\n📊 平均订单金额: ¥").append(String.format("%.2f", avgAmount)).append("\n");
            }

            // 时间分析（最近订单）
            List<Order> recentOrders = allOrders.stream()
                .filter(o -> o.getCreatedAt() != null)
                .sorted((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt()))
                .limit(5)
                .collect(Collectors.toList());

            if (!recentOrders.isEmpty()) {
                analysisData.append("\n🕒 最近订单趋势:\n");
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd HH:mm");
                for (Order order : recentOrders) {
                    String typeIcon = "SALE".equals(order.getOrderType()) ? "💰" : "📦";
                    analysisData.append("  ").append(typeIcon).append(" ")
                        .append(order.getCreatedAt().format(formatter)).append(" | ")
                        .append(order.getCustomerName() != null ? order.getCustomerName() : "未知客户").append(" | ¥")
                        .append(String.format("%.2f", order.getAmount())).append("\n");
                }
            }

            // 调用AI进行深度分析 - 优化超时处理
            try {
                System.out.println("🤖 开始AI订单分析，数据长度: " + analysisData.length());
                
                // 尝试快速AI分析
                String aiAnalysis = deepSeekAIService.analyzeOrderData(analysisData.toString());
                
                // 清理AI输出中的markdown格式
                String cleanedAnalysis = cleanMarkdownFormat(aiAnalysis);
                
                return "🤖 AI订单分析报告\n\n" + cleanedAnalysis;
                
            } catch (Exception aiError) {
                // AI调用失败时，返回增强版基础统计分析
                System.err.println("⚠️ AI分析超时/失败，使用本地分析: " + aiError.getMessage());
                
                return generateLocalOrderAnalysis(allOrders, salesOrders, purchaseOrders, 
                    totalSalesAmount, totalPurchaseAmount, customerStats, analysisData.toString());
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "❌ 订单分析失败：" + e.getMessage() + "\n\n💡 请稍后重试或联系管理员";
        }
    }

    /**
     * 生成本地订单分析报告 - AI分析失败时的fallback
     */
    private String generateLocalOrderAnalysis(List<Order> allOrders, List<Order> salesOrders, 
                                            List<Order> purchaseOrders, double totalSalesAmount, 
                                            double totalPurchaseAmount, Map<String, Long> customerStats, 
                                            String basicData) {
        StringBuilder result = new StringBuilder();
        result.append("📊 快速订单分析报告 (本地分析)\n\n");
        
        // 核心指标总结
        result.append("🎯 核心指标\n");
        result.append("• 订单总数：").append(allOrders.size()).append("个\n");
        result.append("• 销售订单：").append(salesOrders.size()).append("个 | 采购订单：").append(purchaseOrders.size()).append("个\n");
        result.append("• 销售总额：¥").append(String.format("%.2f", totalSalesAmount)).append("\n");
        result.append("• 采购总额：¥").append(String.format("%.2f", totalPurchaseAmount)).append("\n");
        result.append("• 毛利润：¥").append(String.format("%.2f", totalSalesAmount - totalPurchaseAmount)).append("\n\n");
        
        // 智能洞察
        result.append("💡 业务洞察\n");
        
        // 业务结构分析
        if (salesOrders.size() > purchaseOrders.size() * 2) {
            result.append("• 🔥 销售主导型业务，销售活跃度高，建议加强库存管理\n");
        } else if (purchaseOrders.size() > salesOrders.size() * 2) {
            result.append("• 📦 采购密集期，可能在备货或业务扩张，关注资金流动\n");
        } else {
            result.append("• ⚖️ 销采平衡，业务运营相对稳定\n");
        }
        
        // 盈利分析
        if (totalSalesAmount > totalPurchaseAmount) {
            double profitMargin = ((totalSalesAmount - totalPurchaseAmount) / totalSalesAmount) * 100;
            if (profitMargin > 50) {
                result.append("• 💚 盈利优秀，毛利率达 ").append(String.format("%.1f%%", profitMargin)).append("，业务健康\n");
            } else if (profitMargin > 20) {
                result.append("• 💙 盈利良好，毛利率约 ").append(String.format("%.1f%%", profitMargin)).append("，可持续发展\n");
            } else {
                result.append("• 💛 盈利偏低，毛利率仅 ").append(String.format("%.1f%%", profitMargin)).append("，需优化成本\n");
            }
        } else {
            result.append("• ⚠️ 成本压力，支出超过收入，需重点关注现金流\n");
        }
        
        // 客户结构分析
        if (!customerStats.isEmpty()) {
            String topCustomer = customerStats.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("未知");
            long topCount = customerStats.values().stream().max(Long::compareTo).orElse(0L);
            
            if (customerStats.size() == 1) {
                result.append("• 👤 单一客户依赖，主要客户：").append(topCustomer).append("，建议拓展客户群\n");
            } else if (topCount > allOrders.size() * 0.5) {
                result.append("• 👑 头部客户集中，").append(topCustomer).append(" 贡献超过50%订单，注意客户风险\n");
            } else {
                result.append("• 👥 客户分布良好，前5客户较为均衡，业务风险分散\n");
            }
        }
        
        // 平均订单分析
        if (!allOrders.isEmpty()) {
            double avgAmount = (totalSalesAmount + totalPurchaseAmount) / allOrders.size();
            if (avgAmount > 1000) {
                result.append("• 💎 高价值订单，平均金额 ¥").append(String.format("%.0f", avgAmount)).append("，客户质量较高\n");
            } else if (avgAmount > 100) {
                result.append("• 💼 中等订单规模，平均金额 ¥").append(String.format("%.0f", avgAmount)).append("，业务稳健\n");
            } else {
                result.append("• 🛒 小额订单为主，平均金额 ¥").append(String.format("%.0f", avgAmount)).append("，可考虑提升客单价\n");
            }
        }
        
        result.append("\n🚀 优化建议\n");
        
        // 基于数据的具体建议
        if (totalSalesAmount > totalPurchaseAmount * 3) {
            result.append("• 增加采购频次，避免库存断货影响销售\n");
        }
        if (customerStats.size() <= 3 && allOrders.size() > 10) {
            result.append("• 拓展客户群体，降低客户集中风险\n");
        }
        if (!allOrders.isEmpty()) {
            long pendingCount = allOrders.stream()
                .filter(o -> "PENDING".equals(o.getStatus()))
                .count();
            if (pendingCount > allOrders.size() * 0.3) {
                result.append("• 及时处理待确认订单，提升客户满意度\n");
            }
        }
        
        result.append("• 定期分析订单趋势，制定数据驱动的业务策略\n");
        result.append("• 关注现金流，优化收付款周期\n");
        
        return result.toString();
    }

    /**
     * 清理AI输出中的markdown格式
     */
    private String cleanMarkdownFormat(String text) {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }
        
        // 移除markdown粗体标记
        String cleaned = text.replaceAll("\\*\\*([^*]+)\\*\\*", "$1");
        
        // 移除其他markdown标记
        cleaned = cleaned.replaceAll("\\*([^*]+)\\*", "$1");  // 斜体
        cleaned = cleaned.replaceAll("```[\\s\\S]*?```", "");  // 代码块
        cleaned = cleaned.replaceAll("`([^`]+)`", "$1");      // 行内代码
        
        return cleaned.trim();
    }

    // 辅助方法：获取字符串值（支持多个字段名）
    private String getStringValue(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            if (node.has(fieldName) && !node.get(fieldName).asText().isEmpty()) {
                return node.get(fieldName).asText().trim();
            }
        }
        return "";
    }

    // 辅助方法：获取整数值
    private int getIntValue(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            if (node.has(fieldName)) {
                return node.get(fieldName).asInt(0);
            }
        }
        return 0;
    }

    // 辅助方法：获取长整数值
    private long getLongValue(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            if (node.has(fieldName)) {
                return node.get(fieldName).asLong(0L);
            }
        }
        return 0L;
    }

    // 辅助方法：获取浮点数值
    private float getFloatValue(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            if (node.has(fieldName)) {
                return (float) node.get(fieldName).asDouble(0.0);
            }
        }
        return 0.0f;
    }

    // 辅助方法：获取数组值
    private JsonNode getArrayValue(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            if (node.has(fieldName) && node.get(fieldName).isArray()) {
                return node.get(fieldName);
            }
        }
        return mapper.createArrayNode(); // 返回空数组
    }

    // 辅助方法：检查订单是否匹配关键词
    private boolean matchesKeyword(Order order, String keyword) {
        String lowerKeyword = keyword.toLowerCase();
        
        return (order.getCustomerName() != null && order.getCustomerName().toLowerCase().contains(lowerKeyword)) ||
               (order.getOrderNo() != null && order.getOrderNo().toLowerCase().contains(lowerKeyword)) ||
               (order.getGoods() != null && order.getGoods().stream().anyMatch(og -> 
                   og.getGoods() != null && og.getGoods().getName() != null && 
                   og.getGoods().getName().toLowerCase().contains(lowerKeyword)));
    }

    // 辅助方法：获取状态图标
    private String getStatusIcon(Object status) {
        if (status == null) return "⏳";
        String statusStr = status.toString().toLowerCase();
        return switch (statusStr) {
            case "confirmed", "完成" -> "✅";
            case "pending", "待确认" -> "⏳";
            case "cancelled", "已取消" -> "❌";
            default -> "📝";
        };
    }
} 