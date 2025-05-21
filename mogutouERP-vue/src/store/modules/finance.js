import { defineStore } from 'pinia'
import { getFinanceStatistics, getFinanceData as fetchFinanceData } from '@/api/finance'

export const useFinanceStore = defineStore('finance', {
  state: () => ({
    statistics: {},
    financeData: [],
    loading: false
  }),

  actions: {
    // 获取财务统计数据
    async getFinanceStatistics() {
      this.loading = true
      try {
        const response = await getFinanceStatistics()
        this.statistics = response.data
      } catch (error) {
        throw error
      } finally {
        this.loading = false
      }
    },
    
    // 获取财务数据
    async getFinanceData(params) {
      this.loading = true
      try {
        const response = await fetchFinanceData(params)
        this.financeData = response.data
        return this.financeData
      } catch (error) {
        throw error
      } finally {
        this.loading = false
      }
    }
  }
})