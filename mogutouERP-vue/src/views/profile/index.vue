<template>
  <div class="app-container">
    <el-card class="profile-card">
      <template #header>
        <div class="card-header">
          <h3>个人信息</h3>
        </div>
      </template>
      
      <div class="profile-content">
        <div class="avatar-container">
          <el-avatar :size="100" :src="userInfo.avatar"></el-avatar>
          <el-button type="primary" size="small" class="mt-2">更换头像</el-button>
        </div>
        
        <div class="info-container">
          <el-form :model="userInfo" label-width="80px">
            <el-form-item label="用户名">
              <el-input v-model="userInfo.name" disabled></el-input>
            </el-form-item>
            <el-form-item label="角色">
              <el-tag v-for="role in userInfo.roles" :key="role">{{ role }}</el-tag>
            </el-form-item>
            <el-form-item label="电话">
              <el-input v-model="userInfo.tel"></el-input>
            </el-form-item>
            <el-form-item label="邮箱">
              <el-input v-model="userInfo.email"></el-input>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="updateProfile">保存修改</el-button>
            </el-form-item>
          </el-form>
        </div>
      </div>
    </el-card>
    
    <el-card class="password-card mt-4">
      <template #header>
        <div class="card-header">
          <h3>修改密码</h3>
        </div>
      </template>
      
      <el-form :model="passwordForm" :rules="passwordRules" ref="passwordFormRef" label-width="100px">
        <el-form-item label="当前密码" prop="oldPassword">
          <el-input v-model="passwordForm.oldPassword" type="password" show-password></el-input>
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="passwordForm.newPassword" type="password" show-password></el-input>
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password></el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="updatePassword">修改密码</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/modules/user'
import { getInfo } from '@/api/user'

const userStore = useUserStore()

// 用户信息
const userInfo = reactive({
  name: '',
  avatar: '',
  roles: [],
  tel: '',
  email: ''
})

// 密码表单
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 密码表单验证规则
const passwordRules = {
  oldPassword: [
    { required: true, message: '请输入当前密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { 
      validator: (rule, value, callback) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      }, 
      trigger: 'blur' 
    }
  ]
}

const passwordFormRef = ref(null)

// 获取用户信息
onMounted(async () => {
  try {
    const response = await getInfo()
    userInfo.name = response.name
    userInfo.avatar = response.avatar
    userInfo.roles = response.roles
    userInfo.tel = response.tel
    userInfo.email = response.email
  } catch (error) {
    console.error('获取用户信息失败:', error)
    ElMessage.error('获取用户信息失败')
  }
})

// 更新个人信息
const updateProfile = async () => {
  try {
    // 这里需要调用API更新用户信息
    ElMessage.success('个人信息更新成功')
  } catch (error) {
    console.error('更新个人信息失败:', error)
    ElMessage.error('更新个人信息失败')
  }
}

// 更新密码
const updatePassword = async () => {
  if (!passwordFormRef.value) return
  
  await passwordFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        // 这里需要调用API更新密码
        ElMessage.success('密码修改成功')
        // 清空表单
        passwordForm.oldPassword = ''
        passwordForm.newPassword = ''
        passwordForm.confirmPassword = ''
      } catch (error) {
        console.error('密码修改失败:', error)
        ElMessage.error('密码修改失败')
      }
    }
  })
}
</script>

<style scoped>
.profile-card, .password-card {
  max-width: 800px;
  margin: 0 auto;
}

.profile-content {
  display: flex;
  gap: 30px;
}

.avatar-container {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.info-container {
  flex: 1;
}

.mt-2 {
  margin-top: 10px;
}

.mt-4 {
  margin-top: 20px;
}
</style>