import { defineStore } from 'pinia'
import { getFinanceStatistics, getFinanceData } from '@/api/finance'
import request from '@/utils/request'

export const useFinanceStore = defineStore('finance', {
  state: () => ({
    statistics: {},
    financeData: [],
    loading: false
  }),
  
  actions: {
    // 获取财务统计数据 - 优化API响应处理
    async getFinanceStatistics() {
      if (this.loading) return this.statistics // 防止重复请求
      this.loading = true
      try {
        const response = await getFinanceStatistics()
        // API已经在拦截器中处理了response.data，直接使用response
        this.statistics = response
        return this.statistics
      } catch (error) {
        console.error('获取财务统计数据失败:', error)
        // 返回默认数据，避免UI显示错误
        this.statistics = {
          totalProfit: 0,
          totalTurnover: 0,
          totalOrderQuantity: 0,
          averageProfit: 0,
          averageTurnover: 0
        }
        return this.statistics
      } finally {
        this.loading = false
      }
    },

    // 获取财务数据 - 优化API响应处理
    async getFinanceData(params) {
      this.loading = true
      try {
        const response = await getFinanceData(params)
        // API已经在拦截器中处理了response.data，直接使用response
        this.financeData = response
        this.loading = false
        return this.financeData
      } catch (error) {
        this.loading = false
        throw error
      }
    },
    
    // 创建财务记录
    async createFinanceRecord(record) {
      try {
        const response = await request.post('/api/finance', record)
        return response
      } catch (error) {
        throw error
      }
    },
    
    // 更新财务记录
    async updateFinanceRecord(id, record) {
      try {
        const response = await request.put(`/api/finance/${id}`, record)
        return response
      } catch (error) {
        throw error
      }
    },
    
    // 删除财务记录
    async deleteFinanceRecord(id) {
      try {
        const response = await request.delete(`/api/finance/${id}`)
        return response
      } catch (error) {
        throw error
      }
    },

    // 设置加载状态
    setLoading(status) {
      this.loading = status
    }
  }
})