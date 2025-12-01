<template>
  <div class="configuration">
    <h2>Configuration</h2>
    
    <div class="configuration-sections">
      <div class="configuration-section">
        <h3>Images</h3>
        
        <div v-if="loading" class="loading">
          <p>Loading deployable OS images...</p>
        </div>
        
        <div v-else-if="error" class="error">
          <p>{{ error }}</p>
          <button @click="loadDeployableOS" class="btn-primary btn-sm">Retry</button>
        </div>
        
        <div v-else-if="deployableOS.length === 0" class="no-data">
          <p>No deployable OS images found.</p>
        </div>
        
        <div v-else class="os-list">
          <table class="os-table">
            <thead>
              <tr>
                <th>OS</th>
                <th>Release</th>
                <th>Version</th>
                <th>Architecture</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(os, index) in deployableOS" :key="index">
                <td>{{ formatOS(os.os) }}</td>
                <td>{{ os.release }}</td>
                <td>{{ formatVersion(os.os, os.release) }}</td>
                <td>{{ formatArchitectures(os.arches) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import axios from 'axios'
import { useSettings } from '../composables/useSettings'

export default {
  name: 'ConfigurationTab',
  setup() {
    const settingsStore = useSettings()
    const deployableOS = ref([])
    const loading = ref(false)
    const error = ref(null)
    
    const formatOS = (os) => {
      if (!os) return '-'
      // Capitalize first letter
      return os.charAt(0).toUpperCase() + os.slice(1)
    }
    
    const formatVersion = (os, release) => {
      if (!os || !release) return '-'
      
      // Ubuntu release to version mapping
      if (os.toLowerCase() === 'ubuntu') {
        const versionMap = {
          'xenial': '16.04 LTS',
          'bionic': '18.04 LTS',
          'focal': '20.04 LTS',
          'jammy': '22.04 LTS',
          'noble': '24.04 LTS'
        }
        return versionMap[release.toLowerCase()] || release
      }
      
      return release
    }
    
    const formatArchitectures = (arches) => {
      if (!arches || !Array.isArray(arches) || arches.length === 0) {
        return '-'
      }
      return arches.join(', ')
    }
    
    const loadDeployableOS = async () => {
      loading.value = true
      error.value = null
      
      try {
        const settings = settingsStore.settings
        if (!settings.maasUrl || !settings.apiKey) {
          error.value = 'MAAS URL and API Key must be configured in Settings'
          loading.value = false
          return
        }
        
        const response = await axios.get('http://localhost:8081/api/deployable-os', {
          params: {
            maasUrl: settings.maasUrl,
            apiKey: settings.apiKey
          }
        })
        
        if (response.data.results) {
          deployableOS.value = response.data.results
        } else {
          deployableOS.value = []
        }
      } catch (err) {
        console.error('Error loading deployable OS:', err)
        error.value = err.response?.data?.error || err.message || 'Failed to load deployable OS images'
      } finally {
        loading.value = false
      }
    }
    
    onMounted(() => {
      loadDeployableOS()
    })
    
    return {
      deployableOS,
      loading,
      error,
      formatOS,
      formatVersion,
      formatArchitectures,
      loadDeployableOS
    }
  }
}
</script>

<style scoped>
.configuration h2 {
  margin-bottom: 2rem;
  color: #2c3e50;
  font-size: 1.75rem;
}

.configuration-sections {
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

.configuration-section {
  background: #f8f9fa;
  border-radius: 8px;
  padding: 1.5rem;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.configuration-section h3 {
  margin-bottom: 1rem;
  color: #495057;
  font-size: 1.25rem;
  border-bottom: 2px solid #dee2e6;
  padding-bottom: 0.5rem;
}

.loading, .error, .no-data {
  text-align: center;
  padding: 2rem;
  color: #6c757d;
}

.error {
  color: #dc3545;
}

.error button {
  margin-top: 1rem;
}

.os-list {
  overflow-x: auto;
}

.os-table {
  width: 100%;
  border-collapse: collapse;
  background: white;
  border-radius: 4px;
  overflow: hidden;
}

.os-table thead {
  background-color: #2c3e50;
  color: white;
}

.os-table th {
  padding: 0.75rem 1rem;
  text-align: left;
  font-weight: 600;
  font-size: 0.9rem;
}

.os-table tbody tr {
  border-bottom: 1px solid #e9ecef;
  transition: background-color 0.2s;
}

.os-table tbody tr:hover {
  background-color: #f8f9fa;
}

.os-table tbody tr:last-child {
  border-bottom: none;
}

.os-table td {
  padding: 0.75rem 1rem;
  color: #495057;
}

.btn-primary {
  background-color: #007bff;
  color: white;
  border: none;
  padding: 0.5rem 1rem;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.9rem;
  transition: background-color 0.2s;
}

.btn-primary:hover {
  background-color: #0056b3;
}

.btn-primary:disabled {
  background-color: #6c757d;
  cursor: not-allowed;
}

.btn-sm {
  padding: 0.375rem 0.75rem;
  font-size: 0.875rem;
}
</style>

