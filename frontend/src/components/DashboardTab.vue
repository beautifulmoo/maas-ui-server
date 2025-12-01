<template>
  <div class="dashboard">
    <h2>Dashboard</h2>
    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-icon">🖥️</div>
        <div class="stat-content">
          <h3>{{ totalMachines }}</h3>
          <p>Total Machines</p>
        </div>
      </div>
      
      <div class="stat-card">
        <div class="stat-icon">✅</div>
        <div class="stat-content">
          <h3>{{ commissionedMachines }}</h3>
          <p>Commissioned Machines</p>
        </div>
      </div>
      
      <div class="stat-card">
        <div class="stat-icon">🚀</div>
        <div class="stat-content">
          <h3>{{ deployedMachines }}</h3>
          <p>Deployed Machines</p>
        </div>
      </div>
    </div>
    
    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-icon">💿</div>
        <div class="stat-content">
          <h3>{{ deployableOSCount }}</h3>
          <p>Deployable OS</p>
        </div>
      </div>
    </div>
    
    <div class="loading" v-if="loading">
      <p>Loading machine statistics...</p>
    </div>
    
    <div class="error" v-if="error">
      <p>Error loading data: {{ error }}</p>
    </div>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import axios from 'axios'
import { useSettings } from '../composables/useSettings'

export default {
  name: 'DashboardTab',
  setup() {
    const totalMachines = ref(0)
    const commissionedMachines = ref(0)
    const deployedMachines = ref(0)
    const deployableOSCount = ref(0)
    const loading = ref(true)
    const error = ref(null)
    
    // 설정 로드
    const { getApiParams } = useSettings()
    
    const loadMachineStats = async () => {
      try {
        loading.value = true
        error.value = null
        
        // 실제 백엔드 API 호출
        const response = await axios.get('http://localhost:8081/api/dashboard/stats', {
          params: getApiParams.value
        })
        
        if (response.data) {
          totalMachines.value = response.data.totalMachines || 0
          commissionedMachines.value = response.data.commissionedMachines || 0
          deployedMachines.value = response.data.deployedMachines || 0
        } else {
          // API 응답이 없는 경우 목업 데이터 사용
          totalMachines.value = 12
          commissionedMachines.value = 8
          deployedMachines.value = 5
        }
        
        loading.value = false
        
      } catch (err) {
        console.error('Error loading machine stats:', err)
        error.value = err.response?.data?.error || err.message || 'Failed to load machine statistics'
        loading.value = false
        
        // 에러 발생 시 목업 데이터 사용
        totalMachines.value = 12
        commissionedMachines.value = 8
        deployedMachines.value = 5
      }
    }
    
    const loadDeployableOSCount = async () => {
      try {
        const response = await axios.get('http://localhost:8081/api/deployable-os', {
          params: getApiParams.value
        })
        
        if (response.data && response.data.results && Array.isArray(response.data.results)) {
          deployableOSCount.value = response.data.results.length
        } else if (response.data && typeof response.data.count === 'number') {
          deployableOSCount.value = response.data.count
        } else {
          deployableOSCount.value = 0
        }
      } catch (err) {
        console.error('Error loading deployable OS count:', err)
        deployableOSCount.value = 0
      }
    }
    
    onMounted(() => {
      loadMachineStats()
      loadDeployableOSCount()
    })
    
    return {
      totalMachines,
      commissionedMachines,
      deployedMachines,
      deployableOSCount,
      loading,
      error
    }
  }
}
</script>

<style scoped>
.dashboard h2 {
  margin-bottom: 2rem;
  color: #2c3e50;
  font-size: 1.8rem;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 1.5rem;
  margin-bottom: 2rem;
}

.stat-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 2rem;
  border-radius: 12px;
  display: flex;
  align-items: center;
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
  transition: transform 0.2s ease;
}

.stat-card:hover {
  transform: translateY(-2px);
}

.stat-card:nth-child(2) {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.stat-card:nth-child(3) {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

.stats-grid:last-child .stat-card {
  background: linear-gradient(135deg, #fa709a 0%, #fee140 100%);
}

.stat-icon {
  font-size: 2.5rem;
  margin-right: 1rem;
}

.stat-content h3 {
  font-size: 2.5rem;
  font-weight: bold;
  margin-bottom: 0.5rem;
}

.stat-content p {
  font-size: 1rem;
  opacity: 0.9;
}

.loading, .error {
  text-align: center;
  padding: 2rem;
  color: #6c757d;
}

.error {
  color: #dc3545;
  background-color: #f8d7da;
  border: 1px solid #f5c6cb;
  border-radius: 8px;
}
</style>
