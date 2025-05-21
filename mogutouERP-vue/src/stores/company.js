import { defineStore } from 'pinia'
import { getCompanyList, createCompany, updateCompany, deleteCompany } from '@/api/company'

export const useCompanyStore = defineStore('company', {
  state: () => ({
    companies: [],
    currentCompany: null
  }),
  
  actions: {
    // 获取公司列表
    async getCompanyList() {
      try {
        const response = await getCompanyList()
        this.companies = response.data
      } catch (error) {
        throw error
      }
    },

    // 创建公司
    async createCompany(companyData) {
      try {
        const response = await createCompany(companyData)
        return response
      } catch (error) {
        throw error
      }
    },

    // 更新公司
    async updateCompany(id, companyData) {
      try {
        const response = await updateCompany(id, companyData)
        return response
      } catch (error) {
        throw error
      }
    },

    // 删除公司
    async deleteCompany(id) {
      try {
        const response = await deleteCompany(id)
        return response
      } catch (error) {
        throw error
      }
    },

    // 设置当前公司
    setCurrentCompany(company) {
      this.currentCompany = company
    }
  }
})