<template>
  <div class="settings">
    <h2>Settings</h2>
    
    <div class="settings-sections">
      <div class="settings-section">
        <h3>MAAS API Configuration</h3>
        <div class="form-group">
          <label for="maas-url">MAAS Server URL:</label>
          <input 
            id="maas-url"
            type="text" 
            v-model="settings.maasUrl" 
            placeholder="http://192.168.189.71:5240"
            class="form-input"
          >
        </div>
        
        <div class="form-group">
          <label for="api-key">API Key:</label>
          <input 
            id="api-key"
            type="password" 
            v-model="settings.apiKey" 
            placeholder="Enter your MAAS API key"
            class="form-input"
          >
        </div>
        
        <button @click="testConnection" class="btn-primary" :disabled="testing">
          {{ testing ? 'Testing...' : 'Test Connection' }}
        </button>
        
        <div v-if="connectionStatus" :class="['connection-status', connectionStatus.type]">
          {{ connectionStatus.message }}
        </div>
      </div>
      
      <div class="settings-section">
        <h3>Refresh Settings</h3>
        <div class="form-group">
          <label for="refresh-interval">Auto Refresh Interval (seconds):</label>
          <input 
            id="refresh-interval"
            type="number" 
            v-model="settings.refreshInterval" 
            min="10"
            max="300"
            class="form-input"
          >
        </div>
        
        <div class="form-group">
          <label class="checkbox-label">
            <input 
              type="checkbox" 
              v-model="settings.autoRefresh"
            >
            Enable Auto Refresh
          </label>
        </div>
      </div>
      
      <div class="settings-section">
        <h3>Display Settings</h3>
        <div class="form-group">
          <label for="items-per-page">Items per page:</label>
          <select id="items-per-page" v-model="settings.itemsPerPage" class="form-select">
            <option value="10">10</option>
            <option value="25">25</option>
            <option value="50">50</option>
            <option value="100">100</option>
          </select>
        </div>
        
        <div class="form-group">
          <label class="checkbox-label">
            <input 
              type="checkbox" 
              v-model="settings.showAdvancedInfo"
            >
            Show Advanced Machine Information
          </label>
        </div>
      </div>
    </div>
    
    <div class="settings-actions">
      <button @click="saveSettings" class="btn-primary">
        Save Settings
      </button>
      <button @click="resetSettings" class="btn-secondary">
        Reset to Defaults
      </button>
    </div>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { useSettings } from '../composables/useSettings'

export default {
  name: 'SettingsTab',
  setup() {
    const testing = ref(false)
    const connectionStatus = ref(null)
    
    // useSettings composable 사용
    const { settings, save, reset, refresh } = useSettings()
    
    const saveSettings = () => {
      if (save()) {
        showMessage('Settings saved successfully!', 'success')
      } else {
        showMessage('Failed to save settings!', 'error')
      }
    }
    
    const resetSettings = () => {
      reset()
      showMessage('Settings reset to defaults!', 'info')
    }
    
    const testConnection = async () => {
      if (!settings.maasUrl || !settings.apiKey) {
        showMessage('Please enter both MAAS URL and API Key', 'error')
        return
      }
      
      testing.value = true
      connectionStatus.value = null
      
      try {
        // 실제 백엔드 API 호출
        const response = await axios.get('http://localhost:8081/api/test-connection', {
          params: {
            maasUrl: settings.maasUrl,
            apiKey: settings.apiKey
          }
        })
        
        if (response.data && response.data.success) {
          showMessage('Connection successful! MAAS server is reachable.', 'success')
        } else {
          showMessage(`Connection failed: ${response.data?.error || 'Unknown error'}`, 'error')
        }
        
      } catch (error) {
        console.error('Connection test error:', error)
        const errorMessage = error.response?.data?.error || error.message || 'Connection failed'
        showMessage(`Connection failed: ${errorMessage}`, 'error')
      } finally {
        testing.value = false
      }
    }
    
    const showMessage = (message, type) => {
      connectionStatus.value = { message, type }
      setTimeout(() => {
        connectionStatus.value = null
      }, 5000)
    }
    
    onMounted(() => {
      // 설정 새로고침 (localStorage에서 로드)
      refresh()
    })
    
    return {
      testing,
      connectionStatus,
      settings,
      saveSettings,
      resetSettings,
      testConnection
    }
  }
}
</script>

<style scoped>
.settings h2 {
  margin-bottom: 2rem;
  color: #2c3e50;
  font-size: 1.8rem;
}

.settings-sections {
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

.settings-section {
  background: white;
  border: 1px solid #e9ecef;
  border-radius: 8px;
  padding: 1.5rem;
}

.settings-section h3 {
  margin-bottom: 1.5rem;
  color: #495057;
  font-size: 1.2rem;
  border-bottom: 2px solid #e9ecef;
  padding-bottom: 0.5rem;
}

.form-group {
  margin-bottom: 1.5rem;
}

.form-group label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 500;
  color: #495057;
}

.form-input, .form-select {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 1rem;
  transition: border-color 0.2s ease;
}

.form-input:focus, .form-select:focus {
  outline: none;
  border-color: #007bff;
  box-shadow: 0 0 0 2px rgba(0, 123, 255, 0.25);
}

.checkbox-label {
  display: flex !important;
  align-items: center;
  cursor: pointer;
}

.checkbox-label input[type="checkbox"] {
  margin-right: 0.5rem;
  width: auto;
}

.btn-primary, .btn-secondary {
  padding: 0.75rem 1.5rem;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 1rem;
  font-weight: 500;
  transition: all 0.2s ease;
  margin-right: 1rem;
}

.btn-primary {
  background-color: #007bff;
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background-color: #0056b3;
}

.btn-primary:disabled {
  background-color: #6c757d;
  cursor: not-allowed;
}

.btn-secondary {
  background-color: #6c757d;
  color: white;
}

.btn-secondary:hover {
  background-color: #545b62;
}

.connection-status {
  margin-top: 1rem;
  padding: 0.75rem;
  border-radius: 6px;
  font-weight: 500;
}

.connection-status.success {
  background-color: #d4edda;
  color: #155724;
  border: 1px solid #c3e6cb;
}

.connection-status.error {
  background-color: #f8d7da;
  color: #721c24;
  border: 1px solid #f5c6cb;
}

.connection-status.info {
  background-color: #d1ecf1;
  color: #0c5460;
  border: 1px solid #bee5eb;
}

.settings-actions {
  margin-top: 2rem;
  padding-top: 2rem;
  border-top: 1px solid #e9ecef;
  text-align: center;
}
</style>
