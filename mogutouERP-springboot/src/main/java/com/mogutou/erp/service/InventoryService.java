package com.mogutou.erp.service;

import com.mogutou.erp.entity.Inventory;
import org.springframework.data.domain.Page;

public interface InventoryService {
    /**
     * 获取库存列表
     */
    Page<Inventory> getInventoryList(Integer page, Integer size);

    /**
     * 获取单个库存详情
     */
    Inventory getInventoryById(Long id);

    /**
     * 创建库存
     */
    Inventory createInventory(Inventory inventory);

    /**
     * 更新库存
     */
    Inventory updateInventory(Inventory inventory);

    /**
     * 删除库存
     */
    void deleteInventory(Long id);

    /**
     * 库存入库
     */
    Inventory stockIn(Inventory inventory);

    /**
     * 库存出库
     */
    Inventory stockOut(Inventory inventory);
}