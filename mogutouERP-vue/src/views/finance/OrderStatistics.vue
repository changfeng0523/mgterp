<template>
  <div class="order-statistics-page">
    <el-card>
      <div class="header">
        <div class="left">
          <el-select v-model="selectedYear" placeholder="选择年份" @change="handleYearChange">
            <el-option
              v-for="year in availableYears"
              :key="year"
              :label="year + '年'"
              :value="year">
            </el-option>
          </el-select>
        </div>
      </div>

      <div class="chart-container" v-loading="loading">
        <el-card>
          <div id="order-stats-chart" style="width: 100%; height: 400px;"></div>
        </el-card>
      </div>

      <!-- AI Insights Card -->
      <el-card class="ai-insights-card" v-if="aiInsights || aiLoading" style="margin-top: 20px;">
        <template #header>
          <div class="card-header">
            <span>智能订单洞察与建议</span>
          </div>
        </template>
        <div v-if="aiLoading" v-loading="aiLoading" element-loading-text="AI分析中..." style="min-height: 100px; display: flex; align-items: center; justify-content: center;">
          <el-empty description="AI正在分析数据..." :image-size="80"></el-empty>
        </div>
        <div v-else style="white-space: pre-wrap;">{{ aiInsights }}</div>
      </el-card>

    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, computed } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { getFinance } from '@/api/finance' 
import { sendNLIRequest } from '@/api/nli' // Import NLI API

const loading = ref(false)
const selectedYear = ref(new Date().getFullYear())
const chartInstance = ref(null)
const financeData = ref(null) // This holds all data including order quantities and amounts

// AI Insights states
const aiInsights = ref('');
const aiLoading = ref(false);

const availableYears = computed(() => {
  const currentYear = new Date().getFullYear()
  const years = []
  for (let i = 0; i < 5; i++) {
    years.push(currentYear - i)
  }
  return years
})

const fetchAIInsightsForOrders = async () => {
  if (!financeData.value || 
      (!financeData.value.salesOrderQuantity && !financeData.value.purchaseOrderQuantity)) {
    aiInsights.value = '暂无足够订单数据进行分析。';
    aiLoading.value = false;
    return;
  }
  aiLoading.value = true;
  aiInsights.value = '';

  const year = selectedYear.value;
  let queryContext = `${year}年度订单数据分析：\n`;
  if (financeData.value.salesOrderQuantity) {
    queryContext += `销售订单月度数量: ${financeData.value.salesOrderQuantity.join(', ')}。月度销售总额: ${financeData.value.salesTotalAmounts?.map(a => a.toFixed(2)).join(', ') ?? '无金额数据'}。\n`;
  }
  if (financeData.value.purchaseOrderQuantity) {
    queryContext += `采购订单月度数量: ${financeData.value.purchaseOrderQuantity.join(', ')}。月度采购总额: ${financeData.value.purchaseTotalAmounts?.map(a => a.toFixed(2)).join(', ') ?? '无金额数据'}。\n`;
  }

  const query = `${queryContext}请基于以上订单数据，分析销售和采购的趋势，例如哪些月份订单较多/较少，销售额和采购额的波动情况。识别任何显著的模式、潜在的增长机会或风险点（如库存积压风险、销售旺季/淡季等），并提供3-5条关于库存优化、销售策略调整或采购计划方面的具体建议。`;

  try {
    const response = await sendNLIRequest(query);
    if (response && response.reply) { 
      aiInsights.value = response.reply;
    } else {
      aiInsights.value = '未能获取AI洞察，请稍后再试。 (返回内容格式不符)';
    }
  } catch (error) {
    console.error('获取订单AI洞察失败:', error);
    aiInsights.value = '获取AI洞察时发生错误: ' + (error.message || '未知错误');
  } finally {
    aiLoading.value = false;
  }
};

const fetchOrderStatistics = async () => {
  loading.value = true
  aiInsights.value = ''; 
  aiLoading.value = false;
  try {
    const response = await getFinance(selectedYear.value)
    if (response && response.code === 200 && response.data) {
      financeData.value = response.data
      await nextTick() 
      renderChart(response.data)
      fetchAIInsightsForOrders(); // Call AI insights
    } else {
      ElMessage.error(response.message || '获取订单统计数据失败')
      financeData.value = null
      renderChart(null) 
      aiInsights.value = '无订单数据可供分析。';
    }
  } catch (error) {
    console.error('获取订单统计数据失败:', error)
    ElMessage.error('获取订单统计数据失败')
    financeData.value = null
    renderChart(null)
    aiInsights.value = '获取订单数据失败，无法进行AI分析。';
  } finally {
    loading.value = false
  }
}

const handleYearChange = () => {
  fetchOrderStatistics()
}

const renderChart = (data) => {
  const chartDom = document.getElementById('order-stats-chart')
  if (!chartDom) {
    console.warn('Chart DOM element not found')
    return
  }

  if (chartInstance.value) {
    chartInstance.value.dispose() // Dispose of old instance before re-initializing
  }
  chartInstance.value = echarts.init(chartDom)

  const months = ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月']
  const salesOrderQuantities = data?.salesOrderQuantity || Array(12).fill(0);
  const purchaseOrderQuantities = data?.purchaseOrderQuantity || Array(12).fill(0);
  const salesTotalAmounts = data?.salesTotalAmounts || Array(12).fill(0.0);
  const purchaseTotalAmounts = data?.purchaseTotalAmounts || Array(12).fill(0.0);

  const option = {
    title: {
      text: selectedYear.value + '年订单统计趋势',
      left: 'center'
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'cross',
        label: {
          backgroundColor: '#6a7985'
        }
      },
      formatter: function (params) {
        let tooltipContent = params[0].name + '<br/>'; // Month name
        params.forEach(item => {
          tooltipContent += item.marker + item.seriesName + ': ' + item.value + ' 个';
          if (item.seriesName === '销售订单数量' && salesTotalAmounts[item.dataIndex] !== undefined) {
            tooltipContent += ' (总金额: ￥' + salesTotalAmounts[item.dataIndex].toFixed(2) + ')';
          } else if (item.seriesName === '采购订单数量' && purchaseTotalAmounts[item.dataIndex] !== undefined) {
            tooltipContent += ' (总金额: ￥' + purchaseTotalAmounts[item.dataIndex].toFixed(2) + ')';
          }
          tooltipContent += '<br/>';
        });
        return tooltipContent;
      }
    },
    legend: {
      data: ['销售订单数量', '采购订单数量'],
      top: '10%'
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '20%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: months
    },
    yAxis: [
      {
        type: 'value',
        name: '订单数量 (个)',
        axisLabel: {
          formatter: '{value} 个'
        }
      }
      // Add another yAxis for turnover if needed
      // {
      //   type: 'value',
      //   name: '销售额 (元)',
      //   axisLabel: {
      //     formatter: '￥{value}'
      //   }
      // }
    ],
    series: [
      {
        name: '销售订单数量',
        type: 'line',
        smooth: true,
        data: salesOrderQuantities,
        itemStyle: {
          color: '#5470C6' 
        },
        areaStyle: { // Optional: add area style
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{
                offset: 0,
                color: 'rgba(84, 112, 198, 0.3)'
            }, {
                offset: 1,
                color: 'rgba(84, 112, 198, 0)'
            }])
        }
      },
      {
        name: '采购订单数量',
        type: 'line',
        smooth: true,
        data: purchaseOrderQuantities,
        itemStyle: {
          color: '#91CC75' // Different color for purchase orders
        },
        areaStyle: { 
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{
                offset: 0,
                color: 'rgba(145, 204, 117, 0.3)'
            }, {
                offset: 1,
                color: 'rgba(145, 204, 117, 0)'
            }])
        }
      }
    ]
  }
  chartInstance.value.setOption(option)
}

onMounted(() => {
  fetchOrderStatistics()
  // Add window resize listener if needed
  // window.addEventListener('resize', () => {
  //   if (chartInstance.value) {
  //     chartInstance.value.resize();
  //   }
  // });
})

// Before unmounting, dispose ECharts instance and remove event listener
// import { onBeforeUnmount } from 'vue';
// onBeforeUnmount(() => {
//   if (chartInstance.value) {
//     chartInstance.value.dispose();
//   }
//   // window.removeEventListener('resize', ...);
// });

</script>

<style scoped>
.order-statistics-page .header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.order-statistics-page .chart-container {
  margin-top: 20px;
}
.ai-insights-card .el-card__header {
  background-color: #f5f7fa;
  font-weight: bold;
}
.ai-insights-card .el-card__body div[v-loading] .el-loading-mask {
  background-color: rgba(255, 255, 255, 0.8);
}
</style> 