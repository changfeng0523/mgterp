<template>
  <div :class="classObj" class="app-wrapper">
    <div v-if="isMobile && sidebarOpen" class="drawer-bg" @click="closeSidebar" />
    <sidebar class="sidebar-container" />
    <div class="main-container">
      <navbar />
      <div class="main-content">
        <transition name="fade-transform" mode="out-in">
          <app-main />
        </transition>
      </div>
      <div class="footer">
        <p>© {{ new Date().getFullYear() }} 蘑菇头ERP系统 | 进销存管理系统</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useAppStore } from '@/stores/app'
import AppMain from './components/AppMain.vue'
import Navbar from './components/Navbar.vue'
import Sidebar from './components/Sidebar/index.vue'

const route = useRoute()
const appStore = useAppStore()
const isMobile = ref(false)

const sidebarOpen = computed(() => appStore.sidebar.opened)
const withoutAnimation = computed(() => appStore.sidebar.withoutAnimation)

const closeSidebar = () => {
  appStore.closeSideBar({ withoutAnimation: false })
}

// 监听路由变化，在移动设备上自动关闭侧边栏
watch(
  () => route.path,
  () => {
    if (isMobile.value && sidebarOpen.value) {
      closeSidebar()
    }
  }
)

const classObj = computed(() => ({
  hideSidebar: !sidebarOpen.value,
  openSidebar: sidebarOpen.value,
  withoutAnimation: withoutAnimation.value,
  mobile: isMobile.value
}))

// 响应式布局处理
const handleResize = () => {
  const width = document.documentElement.clientWidth
  isMobile.value = width < 992
  
  // 设置应用的设备类型
  appStore.toggleDevice(isMobile.value ? 'mobile' : 'desktop')
  
  if (isMobile.value) {
    appStore.closeSideBar({ withoutAnimation: true })
  }
}

onMounted(() => {
  handleResize()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
})
</script>

<style lang="scss" scoped>
.app-wrapper {
  position: relative;
  height: 100%;
  width: 100%;
  background-color: #f5f7fa;
  
  &.mobile.openSidebar {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
  }
  
  &.withoutAnimation {
    .main-container,
    .sidebar-container {
      transition: none !important;
    }
  }
}

.drawer-bg {
  background: #000;
  opacity: 0.3;
  width: 100%;
  top: 0;
  height: 100%;
  position: absolute;
  z-index: 999;
}

.main-container {
  min-height: 100%;
  transition: margin-left .28s;
  position: relative;
  display: flex;
  flex-direction: column;
  
  .hideSidebar & {
    margin-left: 64px;
  }
  
  .openSidebar:not(.mobile) & {
    margin-left: 210px;
  }
  
  .mobile & {
    margin-left: 0;
    margin-right: 0;
  }
}

.main-content {
  flex: 1;
  padding: 16px;
  box-sizing: border-box;
  background-color: #f5f7fa;
  border-radius: 4px;
  box-shadow: 0 1px 4px rgba(0,21,41,.08);
  margin: 10px;
}

.footer {
  height: 50px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #fff;
  color: #606266;
  font-size: 14px;
  border-top: 1px solid #e6e6e6;
  margin-top: auto;
  
  p {
    margin: 0;
  }
}

@media (max-width: 992px) {
  .app-wrapper {
    width: 100%;
  }
  
  .main-content {
    margin: 5px;
    padding: 10px;
  }
}
</style>