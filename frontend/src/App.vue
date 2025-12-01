<template>
  <div id="app">
    <nav class="navbar">
      <div class="nav-brand">
        <h1>MAAS UI Server</h1>
      </div>
    </nav>
    
    <div class="main-content">
      <div class="tab-container">
        <div class="tab-buttons">
          <button 
            v-for="tab in tabs" 
            :key="tab.id"
            :class="['tab-button', { active: activeTab === tab.id }]"
            @click="activeTab = tab.id"
          >
            {{ tab.name }}
          </button>
        </div>
        
        <div class="tab-content">
          <component :is="currentTabComponent" />
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed } from 'vue'
import DashboardTab from './components/DashboardTab.vue'
import MachinesTab from './components/MachinesTab.vue'
import ConfigurationTab from './components/ConfigurationTab.vue'
import SettingsTab from './components/SettingsTab.vue'

export default {
  name: 'App',
  components: {
    DashboardTab,
    MachinesTab,
    ConfigurationTab,
    SettingsTab
  },
  setup() {
    const activeTab = ref('dashboard')
    const tabs = [
      { id: 'dashboard', name: 'Dashboard', component: 'DashboardTab' },
      { id: 'machines', name: 'Machines', component: 'MachinesTab' },
      { id: 'configuration', name: 'Configuration', component: 'ConfigurationTab' },
      { id: 'settings', name: 'Settings', component: 'SettingsTab' }
    ]
    
    const currentTabComponent = computed(() => {
      const tab = tabs.find(t => t.id === activeTab.value)
      return tab ? tab.component : 'DashboardTab'
    })
    
    return {
      activeTab,
      tabs,
      currentTabComponent
    }
  }
}
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  background-color: #f5f5f5;
}

#app {
  min-height: 100vh;
}

.navbar {
  background-color: #2c3e50;
  color: white;
  padding: 1rem 2rem;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.nav-brand h1 {
  font-size: 1.5rem;
  font-weight: 600;
}

.main-content {
  padding: 2rem;
  max-width: none;
  width: 95%;
  margin: 0 auto;
}

.tab-container {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  overflow: hidden;
}

.tab-buttons {
  display: flex;
  background-color: #f8f9fa;
  border-bottom: 1px solid #e9ecef;
}

.tab-button {
  flex: 1;
  padding: 1rem 2rem;
  border: none;
  background: none;
  cursor: pointer;
  font-size: 1rem;
  font-weight: 500;
  color: #6c757d;
  transition: all 0.2s ease;
}

.tab-button:hover {
  background-color: #e9ecef;
  color: #495057;
}

.tab-button.active {
  background-color: white;
  color: #2c3e50;
  border-bottom: 2px solid #007bff;
}

.tab-content {
  padding: 2rem;
  min-height: 400px;
}
</style>
