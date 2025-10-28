<template>
  <div class="machines">
    <h2>Machines</h2>
    
    <div class="controls">
      <div class="search-box">
        <input 
          type="text" 
          v-model="searchQuery" 
          placeholder="Search machines..."
          class="search-input"
        >
      </div>
      
      <div class="filter-buttons">
        <button 
          v-for="status in statusFilters" 
          :key="status"
          :class="['filter-btn', { active: selectedStatus === status }]"
          @click="selectedStatus = status"
        >
          {{ status }}
        </button>
      </div>
    </div>
    
    <div class="loading" v-if="loading">
      <p>Loading machines...</p>
    </div>
    
    <div class="error" v-if="error">
      <p>Error loading machines: {{ error }}</p>
    </div>
    
    <div class="machines-table-container" v-if="!loading && !error">
      <table class="machines-table">
        <thead>
          <tr>
            <th class="checkbox-col">
              <input type="checkbox" v-model="selectAll" @change="toggleSelectAll">
            </th>
            <th class="fqdn-col">FQDN</th>
            <th class="power-col">POWER</th>
            <th class="status-col">STATUS</th>
            <th class="owner-col">OWNER | TAGS</th>
            <th class="pool-col">POOL</th>
            <th class="zone-col">ZONE</th>
            <th class="fabric-col">FABRIC</th>
            <th class="cores-col">CORES</th>
            <th class="ram-col">RAM</th>
            <th class="disks-col">DISKS</th>
            <th class="storage-col">STORAGE</th>
            <th class="actions-col">ACTIONS</th>
          </tr>
        </thead>
        <tbody>
          <tr 
            v-for="machine in filteredMachines" 
            :key="machine.id"
            :class="['machine-row', { selected: selectedMachines.includes(machine.id) }]"
          >
            <td class="checkbox-col">
              <input 
                type="checkbox" 
                :value="machine.id"
                v-model="selectedMachines"
              >
            </td>
            <td class="fqdn-col">
              <div class="machine-name">
                <strong>{{ machine.hostname || `Machine ${machine.id}` }}</strong>
                <div class="machine-details">
                  <span class="mac-address">{{ machine.mac_addresses?.[0] || 'N/A' }}</span>
                  <span class="ip-address">{{ machine.ip_addresses?.[0] || 'N/A' }}</span>
                </div>
              </div>
            </td>
            <td class="power-col">
              <span class="power-status">
                {{ machine.power_state || 'Unknown' }}
              </span>
            </td>
            <td class="status-col">
              <span :class="['status-badge', machine.status]">
                {{ getStatusText(machine.status) }}
              </span>
            </td>
            <td class="owner-col">
              <span class="owner">{{ machine.owner || '-' }}</span>
              <div class="tags" v-if="machine.tags && machine.tags.length > 0">
                <span v-for="tag in machine.tags" :key="tag" class="tag">{{ tag }}</span>
              </div>
            </td>
            <td class="pool-col">
              <span class="pool">{{ machine.pool || 'default' }}</span>
            </td>
            <td class="zone-col">
              <span class="zone">{{ machine.zone || 'default' }}</span>
            </td>
            <td class="fabric-col">
              <span class="fabric">{{ machine.fabric || '-' }}</span>
            </td>
            <td class="cores-col">
              <span class="cores">{{ machine.cpu_count || 0 }} {{ machine.architecture || 'amd64' }}</span>
            </td>
            <td class="ram-col">
              <span class="ram">{{ formatMemory(machine.memory) }}</span>
            </td>
            <td class="disks-col">
              <span class="disks">{{ machine.disk_count || 0 }}</span>
            </td>
            <td class="storage-col">
              <span class="storage">{{ formatStorage(machine.storage) }}</span>
            </td>
            <td class="actions-col">
              <div class="action-buttons">
                <button class="btn-small btn-primary" @click="viewDetails(machine)">
                  View
                </button>
                <button class="btn-small btn-secondary" @click="showActions(machine)">
                  Actions
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
    
    <div v-if="!loading && !error && filteredMachines.length === 0" class="no-machines">
      <p>No machines found matching your criteria.</p>
    </div>
    
    <!-- Pagination -->
    <div class="pagination" v-if="!loading && !error && filteredMachines.length > 0">
      <div class="pagination-info">
        Showing {{ filteredMachines.length }} out of {{ machines.length }} machines
      </div>
      <div class="pagination-controls">
        <button class="btn-small" :disabled="currentPage === 1" @click="currentPage = 1">
          &lt; Page {{ currentPage }} of {{ totalPages }} &gt;
        </button>
        <select v-model="itemsPerPage" class="page-size-select">
          <option value="25">25/page</option>
          <option value="50">50/page</option>
          <option value="100">100/page</option>
        </select>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted } from 'vue'
import axios from 'axios'

export default {
  name: 'MachinesTab',
  setup() {
    const machines = ref([])
    const loading = ref(true)
    const error = ref(null)
    const searchQuery = ref('')
    const selectedStatus = ref('All')
    const statusFilters = ['All', 'New', 'Commissioning', 'Ready', 'Allocated', 'Deployed', 'Failed']
    const selectedMachines = ref([])
    const selectAll = ref(false)
    const currentPage = ref(1)
    const itemsPerPage = ref(25)
    
    const filteredMachines = computed(() => {
      let filtered = machines.value
      
      // Filter by status
      if (selectedStatus.value !== 'All') {
        filtered = filtered.filter(machine => 
          machine.status === selectedStatus.value.toLowerCase()
        )
      }
      
      // Filter by search query
      if (searchQuery.value) {
        const query = searchQuery.value.toLowerCase()
        filtered = filtered.filter(machine => 
          (machine.hostname && machine.hostname.toLowerCase().includes(query)) ||
          (machine.ip_addresses && machine.ip_addresses.some(ip => ip.includes(query))) ||
          (machine.mac_addresses && machine.mac_addresses.some(mac => mac.includes(query))) ||
          machine.id.toString().includes(query)
        )
      }
      
      return filtered
    })
    
    const totalPages = computed(() => {
      return Math.ceil(filteredMachines.value.length / itemsPerPage.value)
    })
    
    const loadMachines = async () => {
      try {
        loading.value = true
        error.value = null
        
        // 실제 백엔드 API 호출
        const response = await axios.get('http://localhost:8081/api/machines', {
          params: {
            maasUrl: 'http://192.168.189.71:5240',
            apiKey: '0CaFHNt9yHWIJWcijm:OGpxrpkB9nCOVhhrvL:GqcGMp8URhJp8zmDQu2x100OHbSFkJic'
          }
        })
        
        if (response.data && response.data.results) {
          // MAAS API 응답을 우리 UI 형식으로 변환
          machines.value = response.data.results.map(machine => ({
            id: machine.system_id,
            hostname: machine.hostname,
            status: getStatusName(machine.status),
            ip_addresses: machine.ip_addresses || [],
            mac_addresses: extractMacAddresses(machine),
            architecture: machine.architecture,
            cpu_count: machine.cpu_count || 0,
            memory: machine.memory || 0,
            disk_count: machine.block_devices?.length || 0,
            storage: calculateStorage(machine.block_devices),
            power_state: machine.power_state,
            owner: machine.owner,
            tags: machine.tag_names || [],
            pool: machine.pool?.name || 'default',
            zone: machine.zone?.name || 'default',
            fabric: machine.fabric?.name || '-'
          }))
        } else {
          // 실제 데이터가 없는 경우 빈 배열
          machines.value = []
        }
        
        loading.value = false
        
      } catch (err) {
        console.error('Error loading machines:', err)
        error.value = err.response?.data?.error || err.message || 'Failed to load machines'
        loading.value = false
        
        // 에러 발생 시 빈 배열로 설정 (목업 데이터 사용 안함)
        machines.value = []
      }
    }
    
    const formatMemory = (memoryMB) => {
      if (!memoryMB) return '0 GiB'
      if (memoryMB >= 1024) {
        return `${(memoryMB / 1024).toFixed(1)} GiB`
      }
      return `${memoryMB} MiB`
    }
    
    const formatStorage = (storageBytes) => {
      if (!storageBytes) return '0 B'
      const gb = storageBytes / (1024 * 1024 * 1024)
      if (gb >= 1) {
        return `${gb.toFixed(1)} GiB`
      }
      const mb = storageBytes / (1024 * 1024)
      return `${mb.toFixed(0)} MiB`
    }
    
    const getStatusName = (statusCode) => {
      const statusMap = {
        0: 'new',
        1: 'commissioning', 
        2: 'failed',
        3: 'commissioning',
        4: 'ready',
        5: 'reserved',
        6: 'deployed',
        7: 'retired',
        8: 'broken',
        9: 'deploying',
        10: 'allocated'
      }
      return statusMap[statusCode] || 'unknown'
    }
    
    const calculateStorage = (blockDevices) => {
      if (!blockDevices || !Array.isArray(blockDevices)) return 0
      return blockDevices.reduce((total, device) => {
        return total + (device.size || 0)
      }, 0)
    }

    const extractMacAddresses = (machine) => {
      const macAddresses = []
      
      // interface_set에서 MAC 주소 추출
      if (machine.interface_set && Array.isArray(machine.interface_set)) {
        machine.interface_set.forEach(networkInterface => {
          if (networkInterface.mac_address) {
            macAddresses.push(networkInterface.mac_address)
          }
        })
      }
      
      // boot_interface에서 MAC 주소 추출
      if (machine.boot_interface && machine.boot_interface.mac_address) {
        const bootMac = machine.boot_interface.mac_address
        if (!macAddresses.includes(bootMac)) {
          macAddresses.push(bootMac)
        }
      }
      
      return macAddresses.length > 0 ? macAddresses : []
    }
    
    const getStatusText = (status) => {
      const statusMap = {
        'new': 'New',
        'commissioning': 'Commissioning',
        'ready': 'Ready',
        'allocated': 'Allocated',
        'deployed': 'Deployed',
        'failed': 'Failed'
      }
      return statusMap[status] || status
    }
    
    const toggleSelectAll = () => {
      if (selectAll.value) {
        selectedMachines.value = filteredMachines.value.map(m => m.id)
      } else {
        selectedMachines.value = []
      }
    }
    
    const viewDetails = (machine) => {
      console.log('View details for machine:', machine)
      // TODO: Implement machine details view
    }
    
    const showActions = (machine) => {
      console.log('Show actions for machine:', machine)
      // TODO: Implement machine actions menu
    }
    
    onMounted(() => {
      loadMachines()
    })
    
      return {
        machines,
        loading,
        error,
        searchQuery,
        selectedStatus,
        statusFilters,
        filteredMachines,
        selectedMachines,
        selectAll,
        currentPage,
        itemsPerPage,
        totalPages,
        formatMemory,
        formatStorage,
        extractMacAddresses,
        getStatusText,
        toggleSelectAll,
        viewDetails,
        showActions
      }
  }
}
</script>

<style scoped>
.machines h2 {
  margin-bottom: 2rem;
  color: #2c3e50;
  font-size: 1.8rem;
}

.controls {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2rem;
  gap: 1rem;
}

.search-box {
  flex: 1;
  max-width: 300px;
}

.search-input {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 1rem;
}

.search-input:focus {
  outline: none;
  border-color: #007bff;
  box-shadow: 0 0 0 2px rgba(0, 123, 255, 0.25);
}

.filter-buttons {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.filter-btn {
  padding: 0.5rem 1rem;
  border: 1px solid #ddd;
  background: white;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.9rem;
  transition: all 0.2s ease;
}

.filter-btn:hover {
  background-color: #f8f9fa;
}

.filter-btn.active {
  background-color: #007bff;
  color: white;
  border-color: #007bff;
}

.machines-table-container {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  overflow: hidden;
  margin-bottom: 2rem;
}

.machines-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.85rem;
  table-layout: fixed;
}

.machines-table th {
  background-color: #f8f9fa;
  padding: 0.75rem 0.5rem;
  text-align: left;
  font-weight: 600;
  color: #495057;
  border-bottom: 2px solid #e9ecef;
  white-space: nowrap;
  font-size: 0.8rem;
}

.machines-table td {
  padding: 0.75rem 0.5rem;
  border-bottom: 1px solid #e9ecef;
  vertical-align: top;
  font-size: 0.8rem;
}

.machine-row:hover {
  background-color: #f8f9fa;
}

.machine-row.selected {
  background-color: #e3f2fd;
}

.checkbox-col {
  width: 30px;
  text-align: center;
}

.fqdn-col {
  min-width: 180px;
  max-width: 200px;
}

.power-col {
  width: 80px;
}

.status-col {
  width: 100px;
}

.owner-col {
  min-width: 120px;
  max-width: 150px;
}

.pool-col {
  width: 80px;
}

.zone-col {
  width: 80px;
}

.fabric-col {
  width: 80px;
}

.cores-col {
  width: 80px;
}

.ram-col {
  width: 70px;
}

.disks-col {
  width: 50px;
}

.storage-col {
  width: 70px;
}

.actions-col {
  width: 100px;
}

.machine-name strong {
  color: #2c3e50;
  font-size: 0.85rem;
  display: block;
}

.machine-details {
  font-size: 0.7rem;
  color: #6c757d;
  margin-top: 0.2rem;
}

.mac-address, .ip-address {
  display: block;
  margin-bottom: 0.05rem;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.power-status {
  font-size: 0.75rem;
  color: #495057;
}

.status-badge {
  padding: 0.2rem 0.4rem;
  border-radius: 3px;
  font-size: 0.7rem;
  font-weight: 500;
  text-transform: uppercase;
  display: inline-block;
}

.status-badge.new { background-color: #e3f2fd; color: #1976d2; }
.status-badge.commissioning { background-color: #fff3e0; color: #f57c00; }
.status-badge.ready { background-color: #e8f5e8; color: #388e3c; }
.status-badge.allocated { background-color: #f3e5f5; color: #7b1fa2; }
.status-badge.deployed { background-color: #e0f2f1; color: #00695c; }
.status-badge.failed { background-color: #ffebee; color: #d32f2f; }

.owner {
  font-size: 0.75rem;
  color: #495057;
}

.tags {
  margin-top: 0.2rem;
}

.tag {
  display: inline-block;
  background-color: #e9ecef;
  color: #495057;
  padding: 0.05rem 0.3rem;
  border-radius: 2px;
  font-size: 0.65rem;
  margin-right: 0.2rem;
  margin-bottom: 0.05rem;
}

.pool, .zone, .fabric {
  font-size: 0.75rem;
  color: #495057;
}

.cores, .ram, .disks, .storage {
  font-size: 0.75rem;
  color: #495057;
}

.action-buttons {
  display: flex;
  gap: 0.2rem;
}

.btn-small {
  padding: 0.2rem 0.4rem;
  border: none;
  border-radius: 3px;
  cursor: pointer;
  font-size: 0.7rem;
  transition: all 0.2s ease;
}

.btn-small.btn-primary {
  background-color: #007bff;
  color: white;
}

.btn-small.btn-primary:hover {
  background-color: #0056b3;
}

.btn-small.btn-secondary {
  background-color: #6c757d;
  color: white;
}

.btn-small.btn-secondary:hover {
  background-color: #545b62;
}

.pagination {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.pagination-info {
  font-size: 0.9rem;
  color: #6c757d;
}

.pagination-controls {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.page-size-select {
  padding: 0.5rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 0.9rem;
}

.loading, .error, .no-machines {
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

/* Responsive design */
@media (max-width: 1400px) {
  .machines-table-container {
    overflow-x: auto;
  }
  
  .machines-table {
    min-width: 1200px;
  }
}
</style>
