import { defineStore } from 'pinia'

export const useAppStore = defineStore('app', {
  state: () => ({
    sidebar: {
      opened: true
    },
    device: 'desktop'
  }),
  actions: {
    closeSideBar({ withoutAnimation }) {
      this.sidebar.opened = false
    },
    toggleSideBar() {
      this.sidebar.opened = !this.sidebar.opened
    },
    toggleDevice(device) {
      this.device = device
    }
  }
})