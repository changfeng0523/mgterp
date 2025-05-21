<template>
  <div class="order-form">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>{{ form.type === 'customer' ? '创建销售订单' : '创建采购订单' }}</span>
          <el-button size="small" @click="goBack">返回</el-button>
        </div>
      </template>
      
      <el-form :model="form" :rules="rules" ref="formRef" label-width="120px" v-loading="loading">
        <el-form-item label="订单类型" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio value="customer">销售订单</el-radio>
            <el-radio value="purchase">采购订单</el-radio>
          </el-radio-group>
        </el-form-item>
        
        <el-form-item :label="form.type === 'customer' ? '客户名称' : '供应商名称'" prop="name">
          <el-input v-model="form.name" autocomplete="off" />
        </el-form-item>
        
        <el-form-item label="商品列表">
          <el-table :data="form.goods" border style="width: 100%">
            <el-table-column label="商品名称" min-width="120">
              <template #default="scope">
                <el-input v-model="scope.row.name" placeholder="输入商品名称" autocomplete="off" />
              </template>
            </el-table-column>
            <el-table-column label="单价" width="180">
              <template #default="scope">
                <el-input-number 
                  v-model="scope.row.price" 
                  :precision="2" 
                  :step="0.1" 
                  :min="0" 
                  @change="calculateTotal"
                  style="width: 100%"
                  controls-position="right"
                />
              </template>
            </el-table-column>
            <el-table-column label="数量" width="180">
              <template #default="scope">
                <el-input-number 
                  v-model="scope.row.quantity" 
                  :min="1" 
                  @change="calculateTotal"
                  style="width: 100%"
                  controls-position="right"
                />
              </template>
            </el-table-column>
            <el-table-column label="金额" width="180">
              <template #default="scope">
                <span class="amount-value">{{ (scope.row.price * scope.row.quantity).toFixed(2) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120">
              <template #default="scope">
                <el-button size="small" type="danger" @click="removeGoods(scope.$index)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-button class="add-btn" type="primary" @click="addGoods">添加商品</el-button>
        </el-form-item>
        
        <el-form-item label="总金额">
          <el-input-number v-model="form.totalAmount" :precision="2" :step="0.1" :min="0" disabled />
        </el-form-item>
        
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="submitForm">提交</el-button>
          <el-button @click="resetForm">重置</el-button>
          <el-button @click="goBack">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onBeforeUnmount, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useOrderStore } from '@/store/modules/order'
import { ElMessage } from 'element-plus'

const router = useRouter()
const orderStore = useOrderStore()
const formRef = ref(null)
const loading = ref(false)

// 使用AbortController来处理请求中断
let abortController = new AbortController()

const form = reactive({
  type: 'customer',
  name: '',
  goods: [],
  totalAmount: 0
})

const rules = {
  type: [{ required: true, message: '请选择订单类型', trigger: 'change' }],
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }]
}

const addGoods = () => {
  form.goods.push({
    name: '',
    price: 0,
    quantity: 1,
    amount: 0
  })
  calculateTotal()
}

const removeGoods = (index) => {
  form.goods.splice(index, 1)
  calculateTotal()
}

const calculateTotal = () => {
  form.totalAmount = form.goods.reduce((total, item) => {
    const amount = item.price * item.quantity
    item.amount = parseFloat(amount.toFixed(2))
    return total + amount
  }, 0)
  form.totalAmount = parseFloat(form.totalAmount.toFixed(2))
}

const goBack = () => {
  // 根据订单类型决定返回哪个页面
  const route = form.type === 'customer' ? '/order/sales' : '/order/purchase'
  resetComponentState()
  router.push(route)
}

const submitForm = async () => {
  if (!formRef.value) return
  
  try {
    await formRef.value.validate()
    
    if (form.goods.length === 0) {
      ElMessage.warning('请至少添加一个商品')
      return
    }
    
    // 验证每个商品项
    for (const item of form.goods) {
      if (!item.name) {
        ElMessage.warning('请填写所有商品名称')
        return
      }
      if (item.price <= 0) {
        ElMessage.warning('商品价格必须大于0')
        return
      }
    }
    
    loading.value = true
    
    try {
      // 构造符合后端实体类的数据结构
      const orderData = {
        orderNo: 'ORD' + new Date().getTime(), // 生成订单编号
        customerName: form.name,
        type: form.type, // customer 或 purchase
        status: 'PENDING',
        amount: form.totalAmount,
        goods: form.goods.map(item => {
          return {
            goods: {
              name: item.name,
              // 这里只提供商品名称，后端会根据名称查找或创建商品
            },
            quantity: item.quantity,
            unitPrice: item.price,
            totalPrice: parseFloat((item.price * item.quantity).toFixed(2))
          }
        })
      }
      
      console.log('提交的订单数据:', JSON.stringify(orderData))
      
      if (form.type === 'customer') {
        await orderStore.createCustormerOrder(orderData)
        ElMessage.success('提交成功')
        // 添加延迟，确保后端处理完成
        setTimeout(() => {
          // 直接返回销售订单页面
          resetComponentState()
          router.push('/order/sales')
        }, 500)
      } else {
        await orderStore.createPurchaseOrder(orderData)
        ElMessage.success('提交成功')
        // 添加延迟，确保后端处理完成
        setTimeout(() => {
          // 直接返回采购订单页面
          resetComponentState()
          router.push('/order/purchase')
        }, 500)
      }
    } catch (error) {
      console.error('提交失败:', error)
      ElMessage.error('提交失败: ' + (error.message || '未知错误'))
    } finally {
      loading.value = false
    }
  } catch (error) {
    console.error('表单验证失败:', error)
  }
}

const resetForm = () => {
  if (formRef.value) {
    formRef.value.resetFields()
  }
  form.goods = []
  form.totalAmount = 0
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

onMounted(() => {
  // 初始化时添加一个空商品行
  if (form.goods.length === 0) {
    addGoods()
  }
})

// 使用Vue Router 4的路由钩子
defineExpose({
  onBeforeRouteLeave
})

onBeforeUnmount(() => {
  resetComponentState()
})
</script>

<style scoped>
.order-form {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.add-btn {
  margin-top: 10px;
  font-weight: 500;
}

.amount-value {
  display: block;
  text-align: right;
  padding-right: 15px;
  font-weight: 500;
}

:deep(.el-input-number) {
  width: 100%;
}

:deep(.el-input-number__decrease),
:deep(.el-input-number__increase) {
  border-radius: 0;
  height: 100%;
  top: 0;
  margin-top: 0;
}
</style>