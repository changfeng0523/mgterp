package com.mogutou.erp.service.impl;

import com.mogutou.erp.entity.Inventory;
import com.mogutou.erp.repository.InventoryRepository;
import com.mogutou.erp.service.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;

@Service
public class InventoryServiceImpl implements InventoryService {

    @Autowired
    private InventoryRepository inventoryRepository;

    @Override
    public Page<Inventory> getInventoryList(Integer page, Integer size) {
        Pageable pageable = PageRequest.of(page, size);
        return inventoryRepository.findAll(pageable);
    }

    @Override
    public Inventory getInventoryById(Long id) {
        return inventoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("库存不存在，ID: " + id));
    }

    @Override
    @Transactional
    public Inventory createInventory(Inventory inventory) {
        return inventoryRepository.save(inventory);
    }

    @Override
    @Transactional
    public Inventory updateInventory(Inventory inventory) {
        // 检查库存是否存在
        if (!inventoryRepository.existsById(inventory.getId())) {
            throw new EntityNotFoundException("库存不存在，ID: " + inventory.getId());
        }
        return inventoryRepository.save(inventory);
    }

    @Override
    @Transactional
    public void deleteInventory(Long id) {
        // 检查库存是否存在
        if (!inventoryRepository.existsById(id)) {
            throw new EntityNotFoundException("库存不存在，ID: " + id);
        }
        inventoryRepository.deleteById(id);
    }

    @Override
    @Transactional
    public Inventory stockIn(Inventory inventoryData) {
        // 获取现有库存
        Inventory existingInventory = getInventoryById(inventoryData.getId());
        
        // 增加库存数量
        int newQuantity = existingInventory.getQuantity() + inventoryData.getQuantity();
        existingInventory.setQuantity(newQuantity);
        
        // 更新其他可能变更的字段
        if (inventoryData.getUnitPrice() != null) {
            existingInventory.setUnitPrice(inventoryData.getUnitPrice());
        }
        if (inventoryData.getLocation() != null) {
            existingInventory.setLocation(inventoryData.getLocation());
        }
        
        return inventoryRepository.save(existingInventory);
    }

    @Override
    @Transactional
    public Inventory stockOut(Inventory inventoryData) {
        // 获取现有库存
        Inventory existingInventory = getInventoryById(inventoryData.getId());
        
        // 检查库存是否足够
        if (existingInventory.getQuantity() < inventoryData.getQuantity()) {
            throw new IllegalArgumentException("库存不足，当前库存: " + existingInventory.getQuantity());
        }
        
        // 减少库存数量
        int newQuantity = existingInventory.getQuantity() - inventoryData.getQuantity();
        existingInventory.setQuantity(newQuantity);
        
        return inventoryRepository.save(existingInventory);
    }
}