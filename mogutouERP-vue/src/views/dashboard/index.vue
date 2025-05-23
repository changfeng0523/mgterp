<template>
  <div class="dashboard-container page-container">
    <div v-if="loading" class="loading-container">
      <el-skeleton :rows="10" animated />
    </div>
    <div v-else>
      <h2 class="dashboard-title">系统概览</h2>
      <el-row :gutter="24">
        <el-col :xs="24" :sm="12" :md="6" :lg="6" :xl="6">
          <el-card class="box-card data-card" shadow="hover">
            <template #header>
              <div class="card-header">
                <span><i class="el-icon-shopping-cart-full"></i> 销售订单</span>
              </div>
            </template>
            <div class="card-body">
              <div class="data-value">{{ statistics.customerOrderCount }}</div>
              <div class="data-label">订单总数</div>
              <div class="data-trend">
                <span class="trend-up"><i class="el-icon-top"></i> 12% 较上月</span>
              </div>
            </div>
          </el-card>
        </el-col>
        
        <el-col :xs="24" :sm="12" :md="6" :lg="6" :xl="6">
          <el-card class="box-card data-card" shadow="hover">
            <template #header>
              <div class="card-header">
                <span><i class="el-icon-shopping-bag-1"></i> 采购订单</span>
              </div>
            </template>
            <div class="card-body">
              <div class="data-value">{{ statistics.purchaseOrderCount }}</div>
              <div class="data-label">订单总数</div>
              <div class="data-trend">
                <span class="trend-up"><i class="el-icon-top"></i> 8% 较上月</span>
              </div>
            </div>
          </el-card>
        </el-col>
        
        <el-col :xs="24" :sm="12" :md="6" :lg="6" :xl="6">
          <el-card class="box-card data-card" shadow="hover">
            <template #header>
              <div class="card-header">
                <span><i class="el-icon-goods"></i> 商品</span>
              </div>
            </template>
            <div class="card-body">
              <div class="data-value">{{ statistics.commodityCount }}</div>
              <div class="data-label">商品总数</div>
              <div class="data-trend">
                <span class="trend-flat"><i class="el-icon-right"></i> 0% 较上月</span>
              </div>
            </div>
          </el-card>
        </el-col>
        
        <el-col :xs="24" :sm="12" :md="6" :lg="6" :xl="6">
          <el-card class="box-card data-card" shadow="hover">
            <template #header>
              <div class="card-header">
                <span><i class="el-icon-user"></i> 员工</span>
              </div>
            </template>
            <div class="card-body">
              <div class="data-value">{{ statistics.staffCount }}</div>
              <div class="data-label">员工总数</div>
              <div class="data-trend">
                <span class="trend-up"><i class="el-icon-top"></i> 5% 较上月</span>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
      
      <el-row :gutter="24" class="chart-row">
        <el-col :xs="24" :lg="12">
          <el-card class="box-card chart-card" shadow="hover">
            <template #header>
              <div class="card-header">
                <span><i class="el-icon-data-line"></i> 销售趋势</span>
                <el-radio-group v-model="salesChartPeriod" size="small" class="chart-period-selector">
                  <el-radio-button value="week">周</el-radio-button>
                  <el-radio-button value="month">月</el-radio-button>
                  <el-radio-button value="year">年</el-radio-button>
                </el-radio-group>
              </div>
            </template>
            <div v-loading="chartLoading" class="chart-container">
              <div id="sales-chart" style="width: 100%; height: 350px;"></div>
            </div>
          </el-card>
        </el-col>
        
        <el-col :xs="24" :lg="12">
          <el-card class="box-card chart-card" shadow="hover">
            <template #header>
              <div class="card-header">
                <span><i class="el-icon-money"></i> 财务概览</span>
                <el-radio-group v-model="financeChartPeriod" size="small" class="chart-period-selector">
                  <el-radio-button value="week">周</el-radio-button>
                  <el-radio-button value="month">月</el-radio-button>
                  <el-radio-button value="year">年</el-radio-button>
                </el-radio-group>
              </div>
            </template>
            <div v-loading="chartLoading" class="chart-container">
              <div id="finance-chart" style="width: 100%; height: 350px;"></div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import * as echarts from 'echarts'
import { useFinanceStore } from '@/stores/finance'
import { ElMessage } from 'element-plus'

const financeStore = useFinanceStore()
const loading = ref(true)
const chartLoading = ref(false)
const salesChartPeriod = ref('month')
const financeChartPeriod = ref('month')

const statistics = ref({
  customerOrderCount: 0,
  purchaseOrderCount: 0,
  commodityCount: 0,
  staffCount: 0,
  salesData: [],
  financeData: []
})

// 使用防抖函数包装图表渲染，避免频繁重绘
const debouncedRenderCharts = () => {
  if (window._chartRenderTimer) clearTimeout(window._chartRenderTimer)
  window._chartRenderTimer = setTimeout(() => {
    renderCharts()
  }, 200)
}

// 预设默认数据，避免等待API响应时UI显示空白
const setDefaultData = () => {
  statistics.value = {
    totalProfit: 0,
    totalTurnover: 0,
    totalOrderQuantity: 0,
    averageProfit: 0,
    averageTurnover: 0,
    customerOrderCount: 0,
    purchaseOrderCount: 0,
    commodityCount: 0,
    staffCount: 0,
    salesData: Array(6).fill(0).map((_, i) => ({ month: `${i+1}月`, sales: 0 })),
    financeData: Array(6).fill(0).map((_, i) => ({ month: `${i+1}月`, income: 0, expense: 0, profit: 0 }))
  }
}

const fetchData = async () => {
  // 如果已经在加载中，避免重复请求
  if (loading.value) return
  
  loading.value = true
  setDefaultData() // 先设置默认数据，避免UI闪烁
  
  try {
    // 使用Promise.race设置超时处理
    const timeoutPromise = new Promise((_, reject) => {
      setTimeout(() => reject(new Error('请求超时')), 3000)
    })
    
    const dataPromise = financeStore.getFinanceStatistics()
    const data = await Promise.race([dataPromise, timeoutPromise])
    
    // 合并API数据和模拟数据
    statistics.value = {
      ...data,
      // 模拟数据
      customerOrderCount: 125,
      purchaseOrderCount: 87,
      commodityCount: 56,
      staffCount: 12,
      salesData: [
        { month: '1月', sales: 320 },
        { month: '2月', sales: 332 },
        { month: '3月', sales: 301 },
        { month: '4月', sales: 334 },
        { month: '5月', sales: 390 },
        { month: '6月', sales: 330 }
      ],
      financeData: [
        { month: '1月', income: 4200, expense: 3800, profit: 400 },
        { month: '2月', income: 5000, expense: 4000, profit: 1000 },
        { month: '3月', income: 5500, expense: 4200, profit: 1300 },
        { month: '4月', income: 6000, expense: 4500, profit: 1500 },
        { month: '5月', income: 7000, expense: 5000, profit: 2000 },
        { month: '6月', income: 7500, expense: 5500, profit: 2000 }
      ]
    }
    debouncedRenderCharts()
  } catch (error) {
    console.error('获取统计数据失败:', error)
    ElMessage.error('获取统计数据失败，使用默认数据显示')
    // 错误时仍然渲染图表，使用默认数据
    debouncedRenderCharts()
  } finally {
    loading.value = false
  }
}

const renderCharts = () => {
  renderSalesChart()
  renderFinanceChart()
}

const renderSalesChart = () => {
  const chartDom = document.getElementById('sales-chart')
  if (!chartDom) return
  
  const myChart = echarts.init(chartDom)
  const option = {
    tooltip: {
      trigger: 'axis'
    },
    xAxis: {
      type: 'category',
      data: statistics.value.salesData.map(item => item.month)
    },
    yAxis: {
      type: 'value'
    },
    series: [
      {
        name: '销售额',
        type: 'line',
        data: statistics.value.salesData.map(item => item.sales),
        smooth: true,
        lineStyle: {
          width: 3,
          color: '#409EFF'
        },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(64, 158, 255, 0.7)' },
              { offset: 1, color: 'rgba(64, 158, 255, 0.1)' }
            ]
          }
        }
      }
    ]
  }
  
  myChart.setOption(option)
  window.addEventListener('resize', () => {
    myChart.resize()
  })
}

const renderFinanceChart = () => {
  const chartDom = document.getElementById('finance-chart')
  if (!chartDom) return
  
  const myChart = echarts.init(chartDom)
  const option = {
    tooltip: {
      trigger: 'axis'
    },
    legend: {
      data: ['收入', '支出', '利润']
    },
    xAxis: {
      type: 'category',
      data: statistics.value.financeData.map(item => item.month)
    },
    yAxis: {
      type: 'value'
    },
    series: [
      {
        name: '收入',
        type: 'bar',
        data: statistics.value.financeData.map(item => item.income),
        itemStyle: {
          color: '#67C23A'
        }
      },
      {
        name: '支出',
        type: 'bar',
        data: statistics.value.financeData.map(item => item.expense),
        itemStyle: {
          color: '#F56C6C'
        }
      },
      {
        name: '利润',
        type: 'line',
        data: statistics.value.financeData.map(item => item.profit),
        smooth: true,
        lineStyle: {
          width: 3,
          color: '#E6A23C'
        }
      }
    ]
  }
  
  myChart.setOption(option)
  window.addEventListener('resize', () => {
    myChart.resize()
  })
}

onMounted(() => {
  fetchData()
})
</script>

<style lang="scss" scoped>
.dashboard-container {
  padding: 24px;
  
  .dashboard-title {
    margin-top: 0;
    margin-bottom: 24px;
    font-size: 24px;
    font-weight: 500;
    color: rgba(0, 0, 0, 0.85);
  }
  
  .chart-row {
    margin-top: 24px;
  }
  
  .box-card {
    margin-bottom: 20px;
  }
  
  .data-card {
    height: 100%;
    
    .card-header {
      display: flex;
      align-items: center;
      
      i {
        margin-right: 8px;
      }
    }
    
    .card-body {
      position: relative;
      
      .data-value {
        font-size: 28px;
        font-weight: 500;
        color: rgba(0, 0, 0, 0.85);
        margin-bottom: 8px;
      }
      
      .data-label {
        font-size: 14px;
        color: rgba(0, 0, 0, 0.45);
        margin-bottom: 12px;
      }
      
      .data-trend {
        font-size: 13px;
        
        .trend-up {
          color: #52c41a;
        }
        
        .trend-down {
          color: #f5222d;
        }
        
        .trend-flat {
          color: #faad14;
        }
        
        i {
          margin-right: 4px;
        }
      }
    }
  }
  
  .chart-card {
    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      
      i {
        margin-right: 8px;
      }
    }
    
    .chart-period-selector {
      margin-left: auto;
    }
    
    .chart-container {
      min-height: 350px;
    }
  }
  
  align-items: center;

  @media (max-width: 768px) {
    padding: 16px;
    
    .el-col {
      margin-bottom: 16px;
    }
    
    .chart-row {
      margin-top: 0;
    }
  }
}


.card-body {
  text-align: center;
  padding: 20px 0;
}

.data-value {
  font-size: 36px;
  font-weight: bold;
  color: #409EFF;
  margin-bottom: 10px;
}

.data-label {
  font-size: 14px;
  color: #606266;
}

.chart-row {
  margin-top: 20px;
}
</style>