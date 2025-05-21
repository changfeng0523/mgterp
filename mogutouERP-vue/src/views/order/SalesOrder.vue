<template>
  <div class="sales-order">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>销售订单管理</span>
          <el-button type="primary" @click="handleCreateOrder">创建销售订单</el-button>
        </div>
      </template>
      
      <el-table :data="tableData" border style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="订单ID" width="120" />
        <el-table-column prop="customerName" label="客户名称" width="180" />
        <el-table-column prop="totalAmount" label="总金额" width="120">
          <template #default="scope">
            {{ getTotalAmount(scope.row) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)">{{ getStatusText(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="scope">
            <el-button size="small" @click="handleViewDetail(scope.row)">详情</el-button>
            <el-button 
              size="small" 
              type="success" 
              @click="handleConfirmOrder(scope.row)"
              :disabled="scope.row.status !== '待确认' && scope.row.status !== 'PENDING'"
            >确认</el-button>
            <el-button 
              size="small" 
              type="danger" 
              @click="handleDeleteOrder(scope.row)"
              :disabled="scope.row.status === '已完成' || scope.row.status === 'COMPLETED'"
            >删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <el-pagination
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        :current-page="currentPage"
        :page-sizes="[10, 20, 50]"
        :page-size="pageSize"
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        class="pagination"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, onActivated, onDeactivated, markRaw, h, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useOrderStore } from '@/store/modules/order'
import { ElMessage, ElMessageBox, ElTable, ElTableColumn } from 'element-plus'

const router = useRouter()
const orderStore = useOrderStore()

const tableData = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)

// 使用AbortController来处理请求中断
let abortController = new AbortController()

// 格式化货币
const formatCurrency = (value) => {
  // 处理value为undefined或null的情况
  if (value === undefined || value === null || isNaN(value)) {
    return '¥0.00';
  }
  return `¥${Number(value).toFixed(2)}`;
}

// 在表格中显示总金额的处理函数，兼容不同的字段名
const getTotalAmount = (row) => {
  // 优先使用amount字段，如果不存在则使用totalAmount字段
  const amount = row.amount !== undefined && row.amount !== null ? row.amount : row.totalAmount;
  return formatCurrency(amount);
}

const fetchData = async () => {
  if (loading.value) return // 避免重复请求
  
  try {
    loading.value = true
    
    console.log('开始获取销售订单数据, 页码:', currentPage.value, '每页数量:', pageSize.value)
    
    // 不再使用 signal 参数
    await orderStore.fetchCustormerOrders({
      page: currentPage.value - 1, // 后端页码从0开始
      size: pageSize.value
    })
    
    console.log('获取到的销售订单数据:', orderStore.custormerOrders)
    
    // 确保数据是数组
    tableData.value = Array.isArray(orderStore.custormerOrders) 
      ? orderStore.custormerOrders 
      : []
    
    console.log('处理后的表格数据:', tableData.value)
      
    // 使用 pagination 中的 totalElements
    total.value = orderStore.pagination.totalElements || 0
    
    console.log('销售订单总数:', total.value)
    
    // 检查每条数据的状态值
    tableData.value.forEach((item, index) => {
      console.log(`订单[${index}] ID=${item.id}, 状态=${item.status}, 客户=${item.customerName}, 总金额=${item.amount || item.totalAmount}`)
    })
  } catch (error) {
    console.error('获取销售订单列表失败:', error)
    ElMessage.error('获取销售订单列表失败')
  } finally {
    loading.value = false
  }
}

const getStatusType = (status) => {
  const statusMap = {
    '待确认': 'warning',
    '已确认': 'success',
    '已取消': 'info',
    '已完成': 'primary',
    'PENDING': 'warning',
    'PROCESSING': 'success',
    'COMPLETED': 'primary',
    'CANCELLED': 'info'
  }
  return statusMap[status] || 'info'
}

const getStatusText = (status) => {
  const statusMap = {
    '待确认': '待确认',
    '已确认': '已确认',
    '已取消': '已取消',
    '已完成': '已完成',
    'PENDING': '待确认',
    'PROCESSING': '处理中',
    'COMPLETED': '已完成',
    'CANCELLED': '已取消'
  }
  return statusMap[status] || status || '未知状态'
}

const handleCreateOrder = () => {
  // 先清理状态
  resetComponentState()
  
  // 使用普通导航模式
  router.push('/order/create')
}

const handleViewDetail = (row) => {
  // 使用全局对话框服务和h函数渲染
  ElMessageBox({
    title: '销售订单详情',
    message: h('div', { class: 'detail-dialog' }, [
      h('div', { class: 'order-info' }, [
        h('div', { class: 'info-item' }, [
          h('span', { class: 'label' }, '订单ID:'),
          h('span', { class: 'value' }, row.id)
        ]),
        h('div', { class: 'info-item' }, [
          h('span', { class: 'label' }, '客户名称:'),
          h('span', { class: 'value' }, row.customerName)
        ]),
        h('div', { class: 'info-item' }, [
          h('span', { class: 'label' }, '总金额:'),
          h('span', { class: 'value' }, getTotalAmount(row))
        ]),
        h('div', { class: 'info-item' }, [
          h('span', { class: 'label' }, '状态:'),
          h('span', { class: 'value' }, [
            h('el-tag', { type: getStatusType(row.status) }, getStatusText(row.status))
          ])
        ]),
        h('div', { class: 'info-item' }, [
          h('span', { class: 'label' }, '创建时间:'),
          h('span', { class: 'value' }, row.createTime)
        ])
      ]),
      h('div', { class: 'goods-list' }, [
        h('h3', '商品列表'),
        h(markRaw(ElTable), {
          data: row.goods || [],
          border: true,
          style: 'width: 100%'
        }, {
          default: () => [
            h(ElTableColumn, { prop: 'name', label: '商品名称' }),
            h(ElTableColumn, { prop: 'price', label: '单价' }),
            h(ElTableColumn, { prop: 'quantity', label: '数量' }),
            h(ElTableColumn, { prop: 'amount', label: '金额' })
          ]
        })
      ])
    ]),
    showCancelButton: false,
    confirmButtonText: '关闭',
    customClass: 'wide-dialog'
  })
}

const handleConfirmOrder = (row) => {
  // 创建临时表单数据
  const confirmForm = {
    deliveryDate: new Date(),
    remark: '',
    freight: 0  // 添加运费参数，默认为0
  }
  
  // 使用对话框服务确认订单，避免嵌套组件
  ElMessageBox.confirm('确认此销售订单吗？确认后订单状态将改变', '确认销售订单', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning',
    beforeClose: async (action, instance, done) => {
      if (action === 'confirm') {
        instance.confirmButtonLoading = true
        try {
          await orderStore.confirmCustormerOrder(row.id, confirmForm)
          ElMessage.success('订单确认成功')
          await fetchData()
          done()
        } catch (error) {
          console.error('订单确认失败:', error)
          ElMessage.error('订单确认失败')
        } finally {
          instance.confirmButtonLoading = false
        }
      } else {
        done()
      }
    }
  })
}

const handleDeleteOrder = (row) => {
  ElMessageBox.confirm('确定删除该销售订单吗?', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      loading.value = true
      await orderStore.deleteCustormerOrder(row.id)
      ElMessage.success('删除成功')
      await fetchData()
    } catch (error) {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    } finally {
      loading.value = false
    }
  }).catch(() => {})
}

const handleSizeChange = (val) => {
  pageSize.value = val
  fetchData()
}

const handleCurrentChange = (val) => {
  currentPage.value = val
  fetchData()
}

// 重置组件状态
const resetComponentState = () => {
  // 中止正在进行的请求
  abortController.abort()
  abortController = new AbortController()
  
  // 重置其他状态
  loading.value = false
}

// 添加路由离开前的钩子
const onBeforeRouteLeave = (to, from, next) => {
  resetComponentState()
  next()
}

// 组件挂载时获取数据
onMounted(() => {
  fetchData()
})

// 组件激活时刷新数据（用于keep-alive）
onActivated(() => {
  console.log('销售订单组件激活')
  fetchData()
})

// 组件卸载时清理
onBeforeUnmount(() => {
  resetComponentState()
})

// 使用Vue Router 4的路由钩子
defineExpose({
  onBeforeRouteLeave
})

// 当组件被停用时重置状态
onDeactivated(() => {
  resetComponentState()
})
</script>

<style scoped>
.sales-order {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.pagination {
  margin-top: 20px;
  text-align: right;
}

/* 详情对话框样式 */
:deep(.detail-dialog) {
  padding: 10px;
}

:deep(.order-info) {
  margin-bottom: 20px;
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}

:deep(.info-item) {
  display: flex;
  line-height: 30px;
}

:deep(.label) {
  width: 80px;
  font-weight: bold;
  color: #606266;
}

:deep(.value) {
  flex: 1;
}

:deep(.goods-list) {
  margin-top: 20px;
}

:deep(.goods-list h3) {
  margin-bottom: 10px;
  font-size: 16px;
  color: #303133;
}

:deep(.wide-dialog) {
  max-width: 800px;
  width: 70%;
}
</style>