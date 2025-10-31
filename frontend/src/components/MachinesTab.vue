<template>
  <div class="machines">
    <div class="header">
    <h2>Machines</h2>
      <div class="connection-status">
        <span :class="['status-indicator', connectionStatus]">
          {{ connectionStatus === 'connected' ? 'Live' : 
             connectionStatus === 'connecting' ? 'Connecting' : 
             connectionStatus === 'error' ? 'Error' : 'Offline' }}
        </span>
      </div>
    </div>
    
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

            <div class="action-buttons">
              <button class="btn-primary add-machine-btn" @click="showAddMachineModal">
                <span class="btn-icon">+</span>
                Add Machine
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
              <div class="status-container">
          <span :class="['status-badge', machine.status]">
                  {{ getStatusText(machine.status) }}
          </span>
                <div v-if="machine.status_message" class="status-message">
                  {{ machine.status_message }}
                </div>
              </div>
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
                       <button 
                         class="btn-small"
                         :class="machine.status === 'commissioning' ? 'btn-warning' : 'btn-success'"
                         @click="machine.status === 'commissioning' ? abortCommissioning(machine) : commissionMachine(machine)"
                         :disabled="machine.status === 'commissioning' ? abortingMachines.includes(machine.id) : (!canCommission(machine) || commissioningMachines.includes(machine.id))"
                       >
                         <span v-if="machine.status === 'commissioning'">
                           <span v-if="abortingMachines.includes(machine.id)">...</span>
                           <span v-else>Abort</span>
                         </span>
                         <span v-else>
                           <span v-if="commissioningMachines.includes(machine.id)">...</span>
                           <span v-else>Commission</span>
                         </span>
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

    <!-- Add Machine Modal -->
    <div v-if="showAddModal" class="modal-overlay" @click="closeAddMachineModal">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>Add New Machine</h3>
          <button class="close-btn" @click="closeAddMachineModal">&times;</button>
        </div>
        
        <form @submit.prevent="addMachine" class="add-machine-form">
          <div class="form-group">
            <label for="hostname">Hostname (Optional)</label>
            <input
              type="text"
              id="hostname"
              v-model="newMachine.hostname"
              placeholder="e.g., web-server-02"
              class="form-input"
            >
        </div>

          <div class="form-group">
            <label for="architecture">Architecture *</label>
            <select id="architecture" v-model="newMachine.architecture" class="form-select" required>
              <option value="amd64">amd64</option>
            </select>
      </div>

          <div class="form-group">
            <label for="macAddresses">MAC Address *</label>
            <input
              type="text"
              id="macAddresses"
              v-model="newMachine.macAddresses"
              placeholder="e.g., 08:00:27:11:34:26 or 080027113426"
              class="form-input"
              required
              @blur="validateMacAddress"
            >
            <div v-if="macAddressError" class="error-message">{{ macAddressError }}</div>
    </div>
    
          <div class="form-group">
            <label for="powerType">Power Type</label>
            <select id="powerType" v-model="newMachine.powerType" class="form-select">
              <option value="manual">manual</option>
            </select>
          </div>

          <div class="form-group">
            <label for="commission">Commission</label>
            <select id="commission" v-model="newMachine.commission" class="form-select">
              <option value="false">false</option>
              <option value="true">true</option>
            </select>
          </div>

          <div class="form-group">
            <label for="description">Description (Optional)</label>
            <textarea
              id="description"
              v-model="newMachine.description"
              placeholder="Machine description..."
              class="form-textarea"
              rows="3"
            ></textarea>
          </div>

          <div class="form-actions">
            <button type="button" class="btn-secondary" @click="closeAddMachineModal">
              Cancel
            </button>
            <button type="submit" class="btn-primary" :disabled="addingMachine">
              <span v-if="addingMachine">Adding...</span>
              <span v-else>Add Machine</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted, watch } from 'vue'
import axios from 'axios'
import { useWebSocket } from '../composables/useWebSocket'
import { useSettings } from '../composables/useSettings'

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
    
    // WebSocket 연결
    // ⚠️ 중요: useWebSocket()은 반드시 최상단에서 먼저 호출해야 함
    // useSettings() 등 다른 composable 호출보다 먼저 호출하여 watch() 의존성 수집에 영향을 주지 않도록 함
    const { connectionStatus, lastMessage, sendMessage } = useWebSocket()
    
    // 설정 로드 (lazy 로딩을 위해 함수로 사용)
    // ⚠️ 주의: useSettings()는 reactive 객체를 생성하므로 watch() 등록 전에 호출해도
    //           WebSocket watch 로직과 분리되어야 함. settingsStore 객체를 직접 참조하지 말고
    //           필요할 때만 getApiParams.value를 사용하도록 함
    const settingsStore = useSettings()
    
    // Add Machine Modal
    const showAddModal = ref(false)
    const addingMachine = ref(false)
    const macAddressError = ref('')
    const newMachine = ref({
      hostname: '',
      architecture: 'amd64',
      macAddresses: '',
      powerType: 'manual',
      commission: 'false',
      description: ''
    })
    
    // Commission Machine
    const commissioningMachines = ref([])
    const abortingMachines = ref([])
    
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
      loading.value = true
      error.value = null
      
      try {
        console.log('🔄 Loading machines via REST API...')
        
        // REST API로 머신 목록 가져오기
        const response = await axios.get('http://localhost:8081/api/machines', {
          params: settingsStore.getApiParams.value
        })
        
        if (response.data && response.data.results) {
          // MAAS API 응답을 우리 UI 형식으로 변환
          machines.value = response.data.results.map(machine => ({
            id: machine.system_id,
            hostname: machine.hostname,
            status: getStatusName(machine.status),
            status_message: machine.status_message,
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
          console.log(`✅ Loaded ${machines.value.length} machines via REST API`)
        } else {
          machines.value = []
        }
        
        loading.value = false
        
      } catch (err) {
        console.error('Error loading machines:', err)
        error.value = err.response?.data?.error || err.message || 'Failed to load machines'
        loading.value = false
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
      // 숫자 코드인 경우
      if (typeof statusCode === 'number') {
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
      
      // 문자열인 경우 (이미 변환된 상태)
      if (typeof statusCode === 'string') {
        const stringStatusMap = {
          'new': 'new',
          'commissioning': 'commissioning',
          'failed': 'failed',
          'ready': 'ready',
          'reserved': 'reserved',
          'deployed': 'deployed',
          'retired': 'retired',
          'broken': 'broken',
          'deploying': 'deploying',
          'allocated': 'allocated'
        }
        return stringStatusMap[statusCode] || statusCode.toLowerCase() || 'unknown'
      }
      
      console.warn('Unknown status type:', typeof statusCode, statusCode)
      return 'unknown'
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
    
    // Add Machine Modal Functions
    const showAddMachineModal = () => {
      showAddModal.value = true
      resetNewMachineForm()
    }
    
    const closeAddMachineModal = () => {
      showAddModal.value = false
      resetNewMachineForm()
    }
    
    const resetNewMachineForm = () => {
      newMachine.value = {
        hostname: '',
        architecture: 'amd64',
        macAddresses: '',
        powerType: 'manual',
        commission: 'false',
        description: ''
      }
      macAddressError.value = ''
    }
    
    const validateMacAddress = () => {
      const mac = newMachine.value.macAddresses.trim()
      if (!mac) {
        macAddressError.value = 'MAC address is required'
        return false
      }
      
      // Remove colons and validate format
      const cleanMac = mac.replace(/:/g, '')
      if (!/^[0-9A-Fa-f]{12}$/.test(cleanMac)) {
        macAddressError.value = 'MAC address must be 12 hexadecimal characters (with or without colons)'
        return false
      }
      
      // Format with colons
      const formattedMac = cleanMac.match(/.{2}/g).join(':')
      newMachine.value.macAddresses = formattedMac
      macAddressError.value = ''
      return true
    }
    
    const addMachine = async () => {
      if (!validateMacAddress()) {
        return
      }
      
      addingMachine.value = true
      
      try {
        const formData = new FormData()
        if (newMachine.value.hostname) {
          formData.append('hostname', newMachine.value.hostname)
        }
        formData.append('architecture', newMachine.value.architecture)
        formData.append('macAddresses', newMachine.value.macAddresses)
        formData.append('powerType', newMachine.value.powerType)
        formData.append('commission', newMachine.value.commission)
        if (newMachine.value.description) {
          formData.append('description', newMachine.value.description)
        }
        const apiParams = settingsStore.getApiParams.value
        formData.append('maasUrl', apiParams.maasUrl)
        formData.append('apiKey', apiParams.apiKey)
        
        const response = await axios.post('http://localhost:8081/api/machines', formData, {
          headers: {
            'Content-Type': 'multipart/form-data'
          }
        })
        
        if (response.data && response.data.success) {
          console.log('Machine added successfully:', response.data)
          closeAddMachineModal()
          // Reload machines list
          await loadMachines()
        } else {
          error.value = response.data?.error || 'Failed to add machine'
        }
        
      } catch (err) {
        console.error('Error adding machine:', err)
        error.value = err.response?.data?.error || err.message || 'Failed to add machine'
      } finally {
        addingMachine.value = false
      }
    }
    
    // Commission Machine Functions
    const canCommission = (machine) => {
      // Allow commission for all machines (for testing purposes)
      return true
    }
    
    // Polling removed - will be replaced with WebSocket implementation
    
    const commissionMachine = async (machine) => {
      if (!canCommission(machine)) {
        return
      }
      
      commissioningMachines.value.push(machine.id)
      
      try {
        const formData = new FormData()
        formData.append('skipBmcConfig', '1')
        const apiParams = settingsStore.getApiParams.value
        formData.append('maasUrl', apiParams.maasUrl)
        formData.append('apiKey', apiParams.apiKey)
        
        const response = await axios.post(`http://localhost:8081/api/machines/${machine.id}/commission`, formData, {
          headers: {
            'Content-Type': 'multipart/form-data'
          }
        })
        
        if (response.data && response.data.success) {
          console.log('Machine commissioned successfully:', response.data)
          // Update only the specific machine's status instead of reloading all machines
          const machineIndex = machines.value.findIndex(m => m.id === machine.id)
          if (machineIndex !== -1) {
            // Update status to commissioning
            machines.value[machineIndex].status = 'commissioning'
            machines.value[machineIndex].status_message = 'Starting commissioning...'
            
            // Polling removed - will be replaced with WebSocket implementation
          }
        } else {
          error.value = response.data?.error || 'Failed to commission machine'
        }
        
      } catch (err) {
        console.error('Error commissioning machine:', err)
        error.value = err.response?.data?.error || err.message || 'Failed to commission machine'
      } finally {
        // Remove from commissioning list
        const index = commissioningMachines.value.indexOf(machine.id)
        if (index > -1) {
          commissioningMachines.value.splice(index, 1)
        }
      }
    }
    
    // Abort Commissioning
    const abortCommissioning = async (machine) => {
      if (machine.status !== 'commissioning') {
        return
      }
      
      abortingMachines.value.push(machine.id)
      
      try {
        const apiParams = settingsStore.getApiParams.value
        const response = await axios.post(`http://localhost:8081/api/machines/${machine.id}/abort`, null, {
          params: {
            maasUrl: apiParams.maasUrl,
            apiKey: apiParams.apiKey
          }
        })
        
        if (response.data && response.data.success) {
          console.log('Machine commissioning aborted successfully:', response.data)
          // Status will be updated via WebSocket
        } else {
          error.value = response.data?.error || 'Failed to abort commissioning'
        }
        
      } catch (err) {
        console.error('Error aborting commissioning:', err)
        error.value = err.response?.data?.error || err.message || 'Failed to abort commissioning'
      } finally {
        // Remove from aborting list
        const index = abortingMachines.value.indexOf(machine.id)
        if (index > -1) {
          abortingMachines.value.splice(index, 1)
        }
      }
    }
    
    // WebSocket 메시지 처리 (실시간 업데이트만)
    // ⚠️ 중요: 이 watch()는 useWebSocket()의 lastMessage를 감시함
    //           - useSettings() 등 다른 reactive 객체와 섞이지 않도록 주의
    //           - watch()는 반드시 useWebSocket() 호출 이후에 등록되어야 함
    //           - 이 로직을 수정할 때는 반드시 WebSocket 연결 및 메시지 수신 로직을 함께 확인해야 함
    watch(lastMessage, (newMessage) => {
      if (!newMessage) return
      
      console.log('🔔 [WebSocket] 메시지 수신 at', new Date().toLocaleTimeString(), ':', newMessage)
      
      // 재연결 알림 처리
      if (newMessage.type === 'reconnect') {
        console.log('🔄 [WebSocket] 재연결 감지 - machine 상태 업데이트 재시작')
        return
      }
      
      // pong 메시지는 heartbeat 응답이므로 처리하지 않음
      if (newMessage.method === 'pong') {
        console.log('💓 [WebSocket] Pong received at', new Date().toLocaleTimeString())
        return
      }
      
      // 모든 메시지 타입 로그 출력
      if (newMessage.type === 2) {
        console.log('📋 Type 2 메시지 상세:', {
          name: newMessage.name,
          action: newMessage.action,
          hasData: !!newMessage.data,
          dataKeys: newMessage.data ? Object.keys(newMessage.data) : []
        })
        
        // 머신이 아닌 다른 타입의 메시지도 로그 출력 (디버깅용)
        if (newMessage.name !== 'machine' && newMessage.data) {
          console.log('⚠️ Non-machine message:', {
            name: newMessage.name,
            action: newMessage.action,
            data: newMessage.data
          })
        }
      }
      
      // 실시간 업데이트만 처리 (type === 2)
      // name이 'machine'인 경우만 처리
      if (newMessage.type === 2 && newMessage.data && newMessage.name === 'machine') {
        console.log('🔍 Processing machine event:', newMessage.name, newMessage.action)
        const machineData = newMessage.data
        console.log('🔔 Machine update:', newMessage.action, 'for', machineData.system_id)
        
        if (newMessage.action === 'update') {
          const machineIndex = machines.value.findIndex(m => m.id === machineData.system_id)
          console.log('🔍 Machine update details:', {
            system_id: machineData.system_id,
            found_index: machineIndex,
            raw_status: machineData.status,
            status_type: typeof machineData.status,
            status_message: machineData.status_message
          })
          
          if (machineIndex !== -1) {
            const oldStatus = machines.value[machineIndex].status
            const newStatus = getStatusName(machineData.status)
            
            machines.value[machineIndex] = {
              ...machines.value[machineIndex],
              status: newStatus,
              status_message: machineData.status_message,
              power_state: machineData.power_state,
              hostname: machineData.hostname
            }
            console.log(`✅ Machine updated: ${machineData.system_id}, Status: ${oldStatus} → ${newStatus}`)
          } else {
            console.log(`❌ Machine not found in list: ${machineData.system_id}`)
          }
        } else if (newMessage.action === 'create') {
          const newMachine = {
            id: machineData.system_id,
            hostname: machineData.hostname,
            status: getStatusName(machineData.status),
            status_message: machineData.status_message,
            ip_addresses: machineData.ip_addresses || [],
            mac_addresses: extractMacAddresses(machineData),
            architecture: machineData.architecture,
            cpu_count: machineData.cpu_count || 0,
            memory: machineData.memory || 0,
            disk_count: machineData.block_devices?.length || 0,
            storage: calculateStorage(machineData.block_devices),
            power_state: machineData.power_state,
            owner: machineData.owner,
            tags: machineData.tag_names || [],
            pool: machineData.pool?.name || 'default',
            zone: machineData.zone?.name || 'default',
            fabric: machineData.fabric?.name || '-'
          }
          machines.value.unshift(newMachine)
          console.log('✅ Machine created:', machineData.system_id)
        } else if (newMessage.action === 'delete') {
          machines.value = machines.value.filter(m => m.id !== machineData.system_id)
          console.log('✅ Machine deleted:', machineData.system_id)
        }
      }
    })
    
    onMounted(() => {
      // 초기 로드는 항상 REST API로
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
        // Add Machine Modal
        showAddModal,
        addingMachine,
        macAddressError,
        newMachine,
        showAddMachineModal,
        closeAddMachineModal,
        validateMacAddress,
        addMachine,
        // Commission Machine
        commissioningMachines,
        abortingMachines,
        canCommission,
        commissionMachine,
        abortCommissioning,
        // WebSocket
        connectionStatus,
        lastMessage
      }
  }
}
</script>

<style scoped>
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2rem;
}

.machines h2 {
  margin: 0;
  color: #2c3e50;
  font-size: 1.8rem;
}

.connection-status {
  display: flex;
  align-items: center;
}

.status-indicator {
  padding: 0.25rem 0.75rem;
  border-radius: 12px;
  font-size: 0.75rem;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.status-indicator.connected {
  background-color: #d4edda;
  color: #155724;
  border: 1px solid #c3e6cb;
}

.status-indicator.connecting {
  background-color: #fff3cd;
  color: #856404;
  border: 1px solid #ffeaa7;
}

.status-indicator.error {
  background-color: #f8d7da;
  color: #721c24;
  border: 1px solid #f5c6cb;
}

.status-indicator.disconnected {
  background-color: #e2e3e5;
  color: #6c757d;
  border: 1px solid #d6d8db;
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
  overflow: hidden; /* 가로 스크롤 제거 */
  margin-bottom: 2rem;
}

.machines-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.85rem;
  table-layout: auto; /* fixed에서 auto로 변경 */
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
  width: 80px !important; /* 확실히 작게 */
  min-width: 80px;
  max-width: 80px;
}

.power-col {
  width: 60px; /* 조금 줄임 */
}

.status-col {
  width: 120px; /* 더 넓게 */
}

.owner-col {
  width: 60px !important; /* TAGS가 POOL과 겹치지 않도록 조정 */
  min-width: 60px;
  max-width: 60px;
}

.pool-col {
  width: 60px; /* 조금 줄임 */
}

.zone-col {
  width: 60px; /* 조금 줄임 */
}

.fabric-col {
  width: 60px; /* 조금 줄임 */
}

.cores-col {
  width: 60px; /* 조금 줄임 */
}

.ram-col {
  width: 50px; /* 조금 줄임 */
}

.disks-col {
  width: 40px; /* 조금 줄임 */
}

.storage-col {
  width: 60px !important; /* 겹치지 않도록 충분한 공간 */
  min-width: 60px;
  max-width: 60px;
}

.actions-col {
  width: 140px !important; /* 겹치지 않도록 조정 */
  min-width: 140px;
  max-width: 140px;
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

.status-container {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}

.status-message {
  font-size: 0.6rem;
  color: #6c757d;
  font-style: italic;
  line-height: 1.2;
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
  gap: 0.2rem; /* Reduced gap for tighter spacing */
  flex-wrap: nowrap; /* Force buttons to stay on one line */
}

.btn-small {
  padding: 0.25rem 0.5rem; /* Increased padding for better height */
  border: none;
  border-radius: 4px; /* View 버튼과 동일한 곡률로 통일 */
  cursor: pointer;
  font-size: 0.7rem; /* 폰트 크기 통일 */
  transition: all 0.2s ease;
  white-space: nowrap;
  text-align: center;
  height: 24px; /* Fixed height for consistency */
  display: flex;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
  min-width: auto; /* Remove fixed minimum width */
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

.btn-small.btn-success {
  background-color: #28a745;
  color: white;
}

.btn-small.btn-success:hover:not(:disabled) {
  background-color: #218838;
}

.btn-small.btn-success:disabled {
  background-color: #6c757d;
  cursor: not-allowed;
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

/* Add Machine Button */
.action-buttons {
  display: flex;
  gap: 1rem;
  align-items: center;
}

.add-machine-btn {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1.5rem;
  background-color: #007bff;
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 0.9rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.add-machine-btn:hover {
  background-color: #0056b3;
  transform: translateY(-1px);
}

.btn-icon {
  font-size: 1.1rem;
  font-weight: bold;
}

/* Modal Styles */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 12px;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
  width: 90%;
  max-width: 600px;
  max-height: 90vh;
  overflow-y: auto;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.5rem;
  border-bottom: 1px solid #e9ecef;
}

.modal-header h3 {
  margin: 0;
  color: #2c3e50;
  font-size: 1.25rem;
}

.close-btn {
  background: none;
  border: none;
  font-size: 1.5rem;
  color: #6c757d;
  cursor: pointer;
  padding: 0;
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: all 0.2s ease;
}

.close-btn:hover {
  background-color: #f8f9fa;
  color: #495057;
}

/* Form Styles */
.add-machine-form {
  padding: 1.5rem;
}

.form-group {
  margin-bottom: 1.5rem;
}

.form-group label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 500;
  color: #495057;
  font-size: 0.9rem;
}

.form-input,
.form-select,
.form-textarea {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid #ced4da;
  border-radius: 6px;
  font-size: 0.9rem;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
  box-sizing: border-box;
}

.form-input:focus,
.form-select:focus,
.form-textarea:focus {
  outline: none;
  border-color: #007bff;
  box-shadow: 0 0 0 3px rgba(0, 123, 255, 0.1);
}

.form-textarea {
  resize: vertical;
  min-height: 80px;
}

.error-message {
  color: #dc3545;
  font-size: 0.8rem;
  margin-top: 0.25rem;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 1rem;
  margin-top: 2rem;
  padding-top: 1.5rem;
  border-top: 1px solid #e9ecef;
}

.btn-primary,
.btn-secondary {
  padding: 0.75rem 1.5rem;
  border: none;
  border-radius: 6px;
  font-size: 0.9rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
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

/* Responsive design */
@media (max-width: 1400px) {
  .machines-table-container {
    overflow-x: auto;
  }
  
  .machines-table {
    min-width: 1200px;
  }
}

@media (max-width: 768px) {
  .modal-content {
    width: 95%;
    margin: 1rem;
  }
  
  .modal-header,
  .add-machine-form {
    padding: 1rem;
  }
  
  .form-actions {
    flex-direction: column;
  }
  
  .btn-primary,
  .btn-secondary {
    width: 100%;
  }
}
</style>
