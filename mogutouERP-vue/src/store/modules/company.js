import { defineStore } from 'pinia'
import { getCompanyList, createCompany, updateCompany, deleteCompany, getCompanyDetail } from '@/api/company'

export const useCompanyStore = defineStore('company', {
  state: () => ({
    companies: [],
    currentCompany: null,
    pagination: {
      page: 0,
      size: 10,
      totalElements: 0,
      totalPages: 0
    }
  }),

  actions: {
    // 获取公司列表
    async getCompanyList(params = { page: 0, size: 10 }) {
      try {
        const response = await getCompanyList(params)
        
        // 处理分页数据
        if (response.data && response.data.content) {
          this.companies = response.data.content
          this.pagination = {
            page: response.data.number || 0,
            size: response.data.size || 10,
            totalElements: response.data.totalElements || 0,
            totalPages: response.data.totalPages || 0
          }
        } else {
          // 兼容非分页数据
          this.companies = Array.isArray(response.data) ? response.data : []
          this.pagination = {
            page: params.page || 0,
            size: params.size || 10,
            totalElements: response.total || this.companies.length,
            totalPages: Math.ceil((response.total || this.companies.length) / (params.size || 10))
          }
        }
        
        return response
      } catch (error) {
        console.error('获取公司列表失败:', error)
        throw error
      }
    },
    
    // 获取公司详情
    async getCompanyDetail(id) {
      try {
        // 确保ID是数字
        const numId = Number(id)
        if (isNaN(numId)) {
          throw new Error('无效的公司ID')
        }
        
        // 先尝试从本地缓存获取
        const company = this.companies.find(item => item.id === numId)
        if (company) {
          return company
        }
        
        // 如果本地没有，则通过API获取
        const response = await getCompanyDetail(numId)
        return response.data || {}
      } catch (error) {
        console.error('获取公司详情失败:', error)
        throw error
      }
    },

    // 创建公司
    async createCompany(companyData) {
      try {
        const response = await createCompany(companyData)
        // 创建成功后，重新获取第一页数据
        await this.getCompanyList({ page: 0, size: this.pagination.size })
        return response
      } catch (error) {
        console.error('创建公司失败:', error)
        throw error
      }
    },

    // 更新公司
    async updateCompany({ id, companyData }) {
      try {
        const response = await updateCompany(id, companyData)
        
        // 更新成功后，重新获取当前页数据
        await this.getCompanyList({ page: this.pagination.page, size: this.pagination.size })
        
        return response
      } catch (error) {
        console.error('更新公司失败:', error)
        throw error
      }
    },

    // 删除公司
    async deleteCompany(id) {
      try {
        const response = await deleteCompany(id)
        
        // 删除成功后，重新获取当前页数据
        await this.getCompanyList({ page: this.pagination.page, size: this.pagination.size })
        
        return response
      } catch (error) {
        console.error('删除公司失败:', error)
        throw error
      }
    },

    // 设置当前公司
    setCurrentCompany(company) {
      this.currentCompany = company
    },
    
    // 清空公司数据
    clearCompanyData() {
      this.companies = []
      this.currentCompany = null
      this.pagination = {
        page: 0,
        size: 10,
        totalElements: 0,
        totalPages: 0
      }
    }
  }
})