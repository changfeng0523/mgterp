<template>
  <section class="app-main">
    <transition name="fade-transform" mode="out-in">
      <keep-alive :include="cachedViews">
        <router-view :key="key" v-if="$route.meta.keepAlive" />
      </keep-alive>
    </transition>
    <transition name="fade-transform" mode="out-in">
      <router-view :key="key" v-if="!$route.meta.keepAlive" />
    </transition>
  </section>
</template>

<script setup>
import { computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

// 创建一个计算属性用于生成key，确保路由变化时组件重新渲染
const key = computed(() => {
  return route.path + JSON.stringify(route.query) + JSON.stringify(route.params)
})

// 获取路由配置中需要缓存的组件名称列表
const cachedViews = computed(() => {
  const cachedComponents = []
  router.getRoutes().forEach(route => {
    if (route.meta && route.meta.keepAlive && route.name) {
      cachedComponents.push(route.name)
    }
  })
  return cachedComponents
})
</script>

<style lang="scss" scoped>
.app-main {
  min-height: calc(100vh - 110px); /* 考虑导航栏和页脚的高度 */
  width: 100%;
  position: relative;
  overflow: hidden;
  padding: 20px;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  transition: all 0.3s;
  
  /* 响应式设计 */
  @media screen and (max-width: 992px) {
    padding: 15px;
    min-height: calc(100vh - 100px);
  }
  
  @media screen and (max-width: 768px) {
    padding: 10px;
    min-height: calc(100vh - 90px);
  }
}
</style>