export { useAppStore } from './app'

import { defineStore } from 'pinia'

export const usePermissionStore = defineStore('permission', {
  state: () => ({
    routes: []
  }),
  actions: {
    setRoutes(routes) {
      this.routes = routes
    }
  }
})