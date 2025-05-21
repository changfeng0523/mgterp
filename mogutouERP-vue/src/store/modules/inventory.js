import { defineStore } from 'pinia'
import { useInventoryApi } from '@/api/inventory'

export const useInventoryStore = defineStore('inventory', {
  state: () => ({
    inventoryList: [],
    currentInventory: null
  }),
  
  actions: {
    // 获取库存列表
    async getInventoryList({ page, size }) {
      try {
        const api = useInventoryApi()
        const response = await api.getInventoryList({ page, size })
        this.inventoryList = response.data
        return response
      } catch (error) {
        throw error
      }
    },
    
    // 获取单个库存详情
    async getInventoryById(id) {
      try {
        // 确保ID是数字
        const numId = Number(id)
        if (isNaN(numId)) {
          throw new Error('无效的库存ID')
        }
        
        const api = useInventoryApi()
        const response = await api.getInventoryById(numId)
        this.currentInventory = response
        return response
      } catch (error) {
        throw error
      }
    },
    
    // 创建库存
    async createInventory(data) {
      try {
        const api = useInventoryApi()
        const response = await api.createInventory(data)
        return response
      } catch (error) {
        throw error
      }
    },
    
    // 更新库存
    async updateInventory(id, data) {
      try {
        // 确保ID是数字
        const numId = Number(id)
        if (isNaN(numId)) {
          throw new Error('无效的库存ID')
        }
        
        const api = useInventoryApi()
        const response = await api.updateInventory(numId, data)
        return response
      } catch (error) {
        throw error
      }
    },
    
    // 删除库存
    async deleteInventory(id) {
      try {
        // 确保ID是数字
        const numId = Number(id)
        if (isNaN(numId)) {
          throw new Error('无效的库存ID')
        }
        
        const api = useInventoryApi()
        const response = await api.deleteInventory(numId)
        return response
      } catch (error) {
        throw error
      }
    },
    
    // 库存入库
    async stockIn(data) {
      try {
        const api = useInventoryApi()
        const response = await api.stockIn(data)
        return response
      } catch (error) {
        throw error
      }
    },
    
    // 库存出库
    async stockOut(data) {
      try {
        const api = useInventoryApi()
        const response = await api.stockOut(data)
        return response
      } catch (error) {
        throw error
      }
    }
  }
})