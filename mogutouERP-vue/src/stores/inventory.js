import { defineStore } from 'pinia'
import { getInventoryList, createInventory, updateInventory, deleteInventory } from '@/api/inventory'

export const useInventoryStore = defineStore('inventory', {
  state: () => ({
    inventories: [],
    currentInventory: null,
    loading: false
  }),
  
  actions: {
    // 获取库存列表
    async getInventoryList() {
      this.loading = true
      try {
        const response = await getInventoryList()
        this.inventories = response.data
        this.loading = false
      } catch (error) {
        this.loading = false
        throw error
      }
    },

    // 创建库存
    async createInventory(inventoryData) {
      try {
        const response = await createInventory(inventoryData)
        return response
      } catch (error) {
        throw error
      }
    },

    // 更新库存
    async updateInventory(id, inventoryData) {
      try {
        const response = await updateInventory(id, inventoryData)
        return response
      } catch (error) {
        throw error
      }
    },

    // 删除库存
    async deleteInventory(id) {
      try {
        const response = await deleteInventory(id)
        return response
      } catch (error) {
        throw error
      }
    },

    // 设置当前库存
    setCurrentInventory(inventory) {
      this.currentInventory = inventory
    },

    // 设置加载状态
    setLoading(status) {
      this.loading = status
    }
  }
})