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
                       <button 
                         class="btn-small btn-primary"
                         @click="showNetworkModal(machine)"
                       >
                         Network
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

    <!-- Network Modal -->
    <div v-if="showNetworkModalState" class="modal-overlay" @click="closeNetworkModal">
      <div class="modal-content network-modal-content" @click.stop>
        <div class="modal-header">
          <h3>Network Configuration - {{ selectedMachine?.hostname || selectedMachine?.id }}</h3>
          <button class="close-btn" @click="closeNetworkModal">&times;</button>
        </div>
        
        <div class="network-modal-body">
          <div v-if="loadingNetwork" class="loading">
            <p>Loading network information...</p>
          </div>
          
          <div v-else-if="networkError" class="error">
            <p>{{ networkError }}</p>
          </div>
          
          <div v-else-if="networkInterfaces.length === 0" class="no-interfaces">
            <p>No network interfaces found.</p>
          </div>
          
          <div v-else class="network-interfaces-list">
            <div 
              v-for="(networkInterface, index) in networkInterfaces" 
              :key="networkInterface.id || index"
              class="network-interface-item"
            >
              <div class="interface-header">
                <div class="interface-title-section">
                  <h4>{{ networkInterface.name || `Interface ${index + 1}` }}</h4>
                  <span class="interface-id">
                    ID: {{ networkInterface.id || '알 수 없음' }}
                  </span>
                </div>
                <span class="interface-type">{{ networkInterface.type || 'Unknown' }}</span>
              </div>
              
              <div class="interface-details">
                <div class="form-group">
                  <label>MAC Address</label>
                  <input 
                    type="text" 
                    :value="networkInterface.mac_address || 'N/A'"
                    class="form-input"
                    readonly
                  >
                </div>
                
                <div class="form-group">
                  <label>Fabric</label>
                  <select 
                    v-model.number="networkInterface.editableFabric"
                    class="form-select"
                    @change="updateFabricForInterface(networkInterface)"
                  >
                    <option :value="null">Select Fabric</option>
                    <option 
                      v-for="fabric in availableFabrics" 
                      :key="fabric.id"
                      :value="fabric.id"
                    >
                      {{ fabric.name }} (id: {{ fabric.id }}, type: {{ typeof fabric.id }})
                    </option>
                  </select>
                  <div style="margin-top: 5px; font-size: 12px; color: #666;">
                    <div>Selected editableFabric: {{ networkInterface.editableFabric }} (type: {{ typeof networkInterface.editableFabric }})</div>
                    <div v-if="networkInterface.vlan">
                      Original vlan: fabric_id={{ networkInterface.vlan.fabric_id }}, fabric={{ networkInterface.vlan.fabric }}
                    </div>
                  </div>
                  <span class="current-value" v-if="networkInterface.vlan && networkInterface.vlan.fabric">
                    Current: {{ networkInterface.vlan.fabric }}
                  </span>
                </div>
                
                <div class="form-group">
                  <label>IP Assignment</label>
                  <select 
                    v-model="networkInterface.ipAssignment"
                    class="form-select"
                    @change="handleIpAssignmentChange(networkInterface)"
                  >
                    <option value="unconfigured">Unconfigured</option>
                    <option value="automatic">Automatic</option>
                    <option value="static">Static</option>
                  </select>
                </div>
                
                <div class="form-group" v-if="networkInterface.ipAssignment === 'static'">
                  <label>IP Address (Primary)</label>
                  <div class="ip-address-primary">
                    <input 
                      type="text" 
                      v-model="networkInterface.primaryIpAddress"
                      :placeholder="networkInterface.matchedSubnet ? `예: ${getDefaultIpExample(networkInterface.matchedSubnet.cidr)}` : 'IP 주소 입력'"
                      class="form-input"
                      :class="{ 'ip-invalid': networkInterface.primaryIpInvalid }"
                      @input="validatePrimaryIpAddress(networkInterface)"
                      @blur="validatePrimaryIpAddress(networkInterface)"
                      @focus="handlePrimaryIpFocus(networkInterface)"
                    >
                    <span class="ip-validation-message" v-if="networkInterface.primaryIpInvalid">
                      유효하지 않은 IP 주소입니다
                    </span>
                    <span class="ip-subnet" v-if="networkInterface.matchedSubnet">
                      Subnet: {{ networkInterface.matchedSubnet.cidr }}
                    </span>
                  </div>
                </div>
                
                <div class="form-group" v-if="networkInterface.secondaryIpAddresses && networkInterface.secondaryIpAddresses.length > 0">
                  <label>IP Address (Secondary)</label>
                  <div 
                    v-for="(secondaryIp, secIndex) in networkInterface.secondaryIpAddresses" 
                    :key="secIndex"
                    class="ip-address-secondary-item"
                  >
                    <div class="secondary-ip-header">
                      <label class="secondary-ip-label">Secondary IP #{{ secIndex + 1 }}</label>
                      <button 
                        type="button"
                        class="btn-remove-secondary"
                        @click="removeSecondaryIp(networkInterface, secIndex)"
                        title="Remove Secondary IP"
                      >
                        ×
                      </button>
                    </div>
                    <div class="secondary-ip-input-group">
                      <select 
                        v-model="secondaryIp.subnet"
                        class="form-select"
                        @change="updateSecondaryIpPrefix(networkInterface, secIndex)"
                      >
                        <option :value="null">Select Subnet</option>
                        <option 
                          v-for="subnet in getFilteredSubnetsForInterface(networkInterface)" 
                          :key="subnet.id" 
                          :value="subnet"
                        >
                          {{ subnet.cidr }} (ID: {{ subnet.id }})
                        </option>
                      </select>
                      <input 
                        type="text" 
                        v-model="secondaryIp.address"
                        :placeholder="secondaryIp.subnet ? `예: ${getDefaultIpExample(secondaryIp.subnet.cidr)}` : 'IP 주소 입력 (선택사항)'"
                        class="form-input"
                        :class="{ 'ip-invalid': secondaryIp.invalid }"
                        @input="validateSecondaryIpAddress(networkInterface, secIndex)"
                        @blur="validateSecondaryIpAddress(networkInterface, secIndex)"
                      >
                    </div>
                    <span class="ip-validation-message" v-if="secondaryIp.invalid">
                      유효하지 않은 IP 주소입니다
                    </span>
                  </div>
                </div>
                
                <div class="form-group">
                  <button 
                    type="button"
                    class="btn-add-secondary"
                    @click="addSecondaryIp(networkInterface)"
                  >
                    + Add Secondary IP
                  </button>
                </div>
                
                <div class="form-group" v-if="networkInterface.vlan">
                  <label>VLAN</label>
                  <input 
                    type="text" 
                    :value="networkInterface.vlan.name || networkInterface.vlan || 'N/A'"
                    class="form-input"
                    readonly
                  >
                </div>
              </div>
            </div>
          </div>
          
          <div class="form-actions">
            <button type="button" class="btn-secondary" @click="closeNetworkModal">
              Cancel
            </button>
            <button type="button" class="btn-primary" @click="saveNetworkChanges" :disabled="savingNetwork">
              <span v-if="savingNetwork">Saving...</span>
              <span v-else>Save Changes</span>
            </button>
          </div>
        </div>
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
    
    // Network Modal
    const showNetworkModalState = ref(false)
    const selectedMachine = ref(null)
    const networkInterfaces = ref([])
    const loadingNetwork = ref(false)
    const networkError = ref(null)
    const savingNetwork = ref(false)
    const availableFabrics = ref([])
    const availableSubnets = ref([])
    const fabricVlanMap = ref({}) // fabric id -> vlan_id mapping
    const fabricVlanIdsMap = ref({}) // fabric id -> [vlan_id, ...] mapping (fabric에 속한 모든 vlan_id 목록)
    
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
    
    // 개별 머신 정보를 가져와서 업데이트하는 함수
    const refreshMachineDetails = async (systemId) => {
      try {
        const apiParams = settingsStore.getApiParams.value
        const response = await axios.get(`http://localhost:8081/api/machines/${systemId}`, {
          params: apiParams
        })
        
        if (response.data && !response.data.error) {
          const machineData = response.data
          const machineIndex = machines.value.findIndex(m => m.id === systemId)
          
          if (machineIndex !== -1) {
            // 기존 머신 정보를 업데이트 (interface_set 포함)
            machines.value[machineIndex] = {
              ...machines.value[machineIndex],
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
              fabric: machineData.fabric?.name || '-',
              interface_set: machineData.interface_set || [] // 네트워크 인터페이스 정보 업데이트
            }
            console.log(`✅ Machine details refreshed for: ${systemId}`)
          }
        }
      } catch (err) {
        console.error(`Error refreshing machine details for ${systemId}:`, err)
      }
    }
    
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
            fabric: machine.fabric?.name || '-',
            interface_set: machine.interface_set || [] // 네트워크 인터페이스 정보 저장
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
      
      // Ready나 Deployed 상태일 때 확인 메시지 표시
      if (machine.status === 'ready' || machine.status === 'deployed') {
        const confirmMessage = `이 머신은 이미 Commissioning이 완료되어 ${machine.status === 'ready' ? 'Ready' : 'Deployed'} 상태입니다.\n\n정말로 다시 Commissioning을 진행하시겠습니까?`
        if (!window.confirm(confirmMessage)) {
          return // 사용자가 취소하면 진행하지 않음
        }
      }
      // NEW나 Error 상태는 확인 없이 바로 진행
      
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
    
    // CIDR에서 네트워크 부분 추출 (예: "192.168.189.0/24" -> "192.168.189.")
    const extractNetworkPrefix = (cidr) => {
      if (!cidr) return ''
      const parts = cidr.split('/')
      if (parts.length !== 2) return ''
      
      const ipParts = parts[0].split('.')
      if (ipParts.length !== 4) return ''
      
      const subnetMask = parseInt(parts[1])
      if (subnetMask < 0 || subnetMask > 32) return ''
      
      // 서브넷 마스크에 따라 표시할 옥텟 수 계산
      // /8 = 1개 옥텟 (192.), /16 = 2개 옥텟 (192.168.), /24 = 3개 옥텟 (192.168.189.)
      // /23, /25 등은 3번째 옥텟까지 표시 (192.168.188.)
      let octetsToShow = 0
      
      if (subnetMask < 8) {
        octetsToShow = 0
      } else if (subnetMask < 16) {
        octetsToShow = 1 // /8-15: 1개 옥텟
      } else if (subnetMask < 32) {
        octetsToShow = 3 // /16-31: 3개 옥텟 (일반적으로 사용자가 입력하는 IP는 3개 옥텟까지 표시)
      } else {
        return '' // /32 이상은 전체 IP가 네트워크
      }
      
      if (octetsToShow === 0) return ''
      
      // 옥텟들을 조인하고 마지막에 점 추가
      const prefix = ipParts.slice(0, octetsToShow).join('.') + '.'
      return prefix
    }
    
    // IP 주소에서 호스트 부분 추출
    const extractHostPart = (ipAddress) => {
      if (!ipAddress) return ''
      const parts = ipAddress.split('.')
      if (parts.length === 4) {
        // 마지막 옥텟만 반환
        return parts[3]
      }
      return ''
    }
    
    // IP 주소 유효성 검사
    const isValidIpAddress = (ipAddress) => {
      if (!ipAddress) return false
      const ipRegex = /^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$/
      return ipRegex.test(ipAddress)
    }
    
    // CIDR에서 기본 IP 예시 생성 (예: "192.168.189.0/24" -> "192.168.189.100")
    const getDefaultIpExample = (cidr) => {
      if (!cidr) return '192.168.1.100'
      const parts = cidr.split('/')
      if (parts.length !== 2) return '192.168.1.100'
      
      const ipParts = parts[0].split('.')
      if (ipParts.length !== 4) return '192.168.1.100'
      
      // 마지막 옥텟을 100으로 설정한 예시 IP 반환
      return `${ipParts[0]}.${ipParts[1]}.${ipParts[2]}.100`
    }
    
    // Primary IP 주소 유효성 검사
    const validatePrimaryIpAddress = (networkInterface) => {
      if (!networkInterface) {
        networkInterface.primaryIpInvalid = false
        return
      }
      
      const ipAddress = networkInterface.primaryIpAddress || ''
      if (!ipAddress) {
        networkInterface.primaryIpInvalid = false
        return
      }
      
      networkInterface.primaryIpInvalid = !isValidIpAddress(ipAddress)
    }
    
    // Primary IP 입력 필드 focus 시 prefix 자동 채우기
    const handlePrimaryIpFocus = (networkInterface) => {
      if (!networkInterface) return
      
      // matchedSubnet이 있고 IP 주소가 비어있거나 prefix로 시작하지 않으면 prefix 설정
      if (networkInterface.matchedSubnet && networkInterface.matchedSubnet.cidr) {
        const networkPrefix = extractNetworkPrefix(networkInterface.matchedSubnet.cidr)
        
        if (networkPrefix) {
          const currentIp = networkInterface.primaryIpAddress || ''
          
          // IP 주소가 비어있거나 prefix로 시작하지 않으면 prefix로 설정
          if (!currentIp || !currentIp.startsWith(networkPrefix)) {
            networkInterface.primaryIpAddress = networkPrefix
          }
        }
      }
    }
    
    // IP Assignment 변경 시 처리
    const handleIpAssignmentChange = (networkInterface) => {
      if (!networkInterface) return
      
      if (networkInterface.ipAssignment === 'static') {
        // Static 선택 시: IP Address 필드 표시 및 prefix 설정
        if (networkInterface.matchedSubnet && networkInterface.matchedSubnet.cidr) {
          const networkPrefix = extractNetworkPrefix(networkInterface.matchedSubnet.cidr)
          if (networkPrefix && (!networkInterface.primaryIpAddress || !networkInterface.primaryIpAddress.startsWith(networkPrefix))) {
            networkInterface.primaryIpAddress = networkPrefix
          }
        }
      } else if (networkInterface.ipAssignment === 'automatic') {
        // Automatic 선택 시: IP Address 필드 숨김 및 IP 주소 초기화
        networkInterface.primaryIpAddress = ''
        
        // Automatic일 때는 fabric에 맞는 subnet이 필요하므로, Fabric이 선택되어 있으면 subnet 자동 매칭
        if (networkInterface.editableFabric !== null && networkInterface.editableFabric !== undefined && networkInterface.editableFabric !== '') {
          const fabric = availableFabrics.value.find(f => 
            f.id === networkInterface.editableFabric || 
            String(f.id) === String(networkInterface.editableFabric) ||
            Number(f.id) === Number(networkInterface.editableFabric)
          )
          
          if (fabric && fabric.vlan_id) {
            const vlanId = fabric.vlan_id
            let searchVlanId = vlanId
            if (typeof searchVlanId === 'string') {
              searchVlanId = parseInt(searchVlanId, 10)
            }
            
            // 해당 vlan_id에 맞는 subnet 찾기
            const matchedSubnet = availableSubnets.value.find(subnet => {
              let subnetVlanId = subnet.vlan_id
              if (subnetVlanId !== null && subnetVlanId !== undefined && typeof subnetVlanId === 'string') {
                subnetVlanId = parseInt(subnetVlanId, 10)
              }
              return subnetVlanId === searchVlanId
            })
            
            if (matchedSubnet) {
              console.log(`[IP Assignment] Automatic selected, matched subnet: ${matchedSubnet.id} (${matchedSubnet.cidr})`)
              networkInterface.matchedSubnet = matchedSubnet
            } else {
              console.warn(`[IP Assignment] Automatic selected but no subnet found for vlan_id=${vlanId}`)
            }
          }
        }
      } else if (networkInterface.ipAssignment === 'unconfigured') {
        // Unconfigured 선택 시: IP Address 필드 숨김 및 IP 주소 초기화
        networkInterface.primaryIpAddress = ''
      }
    }
    
    // Secondary IP 주소 유효성 검사
    const validateSecondaryIpAddress = (networkInterface, secIndex) => {
      if (!networkInterface || !networkInterface.secondaryIpAddresses) {
        return
      }
      
      const secondaryIp = networkInterface.secondaryIpAddresses[secIndex]
      if (!secondaryIp) {
        return
      }
      
      const ipAddress = secondaryIp.address || ''
      if (!ipAddress) {
        secondaryIp.invalid = false
        return
      }
      
      secondaryIp.invalid = !isValidIpAddress(ipAddress)
    }
    
    // Secondary IP 추가
    const addSecondaryIp = (networkInterface) => {
      if (!networkInterface.secondaryIpAddresses) {
        networkInterface.secondaryIpAddresses = []
      }
      
      // Secondary IP 추가 시 fabric 기반으로 subnet 자동 매칭
      let matchedSubnet = null
      if (networkInterface.editableFabric !== null && networkInterface.editableFabric !== undefined && networkInterface.editableFabric !== '') {
        // fabric의 vlan_id 찾기
        const fabric = availableFabrics.value.find(f => 
          f.id === networkInterface.editableFabric || 
          String(f.id) === String(networkInterface.editableFabric) ||
          Number(f.id) === Number(networkInterface.editableFabric)
        )
        
        if (fabric && fabric.vlan_id) {
          const vlanId = fabric.vlan_id
          // vlan_id로 subnet 찾기
          matchedSubnet = availableSubnets.value.find(subnet => {
            let subnetVlanId = subnet.vlan_id
            if (subnetVlanId !== null && subnetVlanId !== undefined) {
              if (typeof subnetVlanId === 'string') {
                subnetVlanId = parseInt(subnetVlanId, 10)
              }
            }
            return subnetVlanId === vlanId
          })
        }
      }
      
      const newSecondaryIp = {
        address: '',
        subnet: matchedSubnet, // fabric 기반으로 자동 매칭된 subnet 사용
        invalid: false
      }
      
      // Subnet이 있으면 prefix 자동 설정
      if (matchedSubnet && matchedSubnet.cidr) {
        const networkPrefix = extractNetworkPrefix(matchedSubnet.cidr)
        if (networkPrefix) {
          newSecondaryIp.address = networkPrefix
        }
      }
      
      networkInterface.secondaryIpAddresses.push(newSecondaryIp)
    }
    
    // Secondary IP 제거
    const removeSecondaryIp = (networkInterface, secIndex) => {
      if (networkInterface.secondaryIpAddresses && networkInterface.secondaryIpAddresses.length > secIndex) {
        networkInterface.secondaryIpAddresses.splice(secIndex, 1)
      }
    }
    
    // 인터페이스의 fabric에 속한 subnet만 필터링해서 반환
    const getFilteredSubnetsForInterface = (networkInterface) => {
      if (!networkInterface || networkInterface.editableFabric === null || networkInterface.editableFabric === undefined || networkInterface.editableFabric === '') {
        // fabric이 선택되지 않았으면 빈 배열 반환
        return []
      }
      
      const fabricId = networkInterface.editableFabric
      // fabric에 속한 vlan_id 목록 가져오기
      const vlanIds = fabricVlanIdsMap.value[fabricId] || 
                      fabricVlanIdsMap.value[String(fabricId)] || 
                      fabricVlanIdsMap.value[Number(fabricId)] || 
                      []
      
      if (vlanIds.length === 0) {
        console.warn(`No vlans found for fabric ${fabricId}`)
        return []
      }
      
      // 해당 vlan_id를 가진 subnet만 필터링
      const filteredSubnets = availableSubnets.value.filter(subnet => {
        let subnetVlanId = subnet.vlan_id
        if (subnetVlanId !== null && subnetVlanId !== undefined) {
          if (typeof subnetVlanId === 'string') {
            subnetVlanId = parseInt(subnetVlanId, 10)
          }
        }
        return vlanIds.includes(subnetVlanId)
      })
      
      console.log(`Filtered subnets for fabric ${fabricId}:`, filteredSubnets.map(s => ({ id: s.id, cidr: s.cidr, vlan_id: s.vlan_id })))
      return filteredSubnets
    }
    
    // Secondary IP의 subnet 선택 시 prefix 자동 설정
    const updateSecondaryIpPrefix = (networkInterface, secIndex) => {
      const secondaryIp = networkInterface.secondaryIpAddresses[secIndex]
      if (!secondaryIp) return
      
      const subnet = secondaryIp.subnet
      if (!subnet || !subnet.cidr) {
        // Subnet이 선택되지 않았으면 기존 IP 주소만 유지 (prefix 제거)
        const currentIp = secondaryIp.address || ''
        if (currentIp && currentIp.endsWith('.')) {
          // prefix만 있는 경우 제거
          secondaryIp.address = ''
        }
        return
      }
      
      // 네트워크 프리픽스 추출
      const networkPrefix = extractNetworkPrefix(subnet.cidr)
      console.log(`Secondary IP subnet 선택: cidr=${subnet.cidr}, prefix=${networkPrefix}`)
      
      // 기존 IP 주소가 없거나 네트워크 프리픽스로 시작하지 않으면 네트워크 프리픽스로 설정
      if (!secondaryIp.address || !secondaryIp.address.startsWith(networkPrefix)) {
        secondaryIp.address = networkPrefix
      }
    }
    
    // Network Modal Functions
    const showNetworkModal = async (machine) => {
      selectedMachine.value = machine
      showNetworkModalState.value = true
      loadingNetwork.value = true
      networkError.value = null
      networkInterfaces.value = []
      availableFabrics.value = []
      availableSubnets.value = []
      fabricVlanMap.value = {}
      fabricVlanIdsMap.value = {}
      
      try {
        const apiParams = settingsStore.getApiParams.value
        
        // 최신 머신 정보 가져오기 (네트워크 정보 포함)
        console.log(`🔄 [Network Modal] Fetching latest machine info for: ${machine.id}`)
        const machineResponse = await axios.get(`http://localhost:8081/api/machines/${machine.id}`, {
          params: apiParams
        })
        
        if (machineResponse.data && machineResponse.data.error) {
          throw new Error(machineResponse.data.error)
        }
        
        // 최신 머신 정보 사용
        const latestMachine = machineResponse.data
        if (!latestMachine || !latestMachine.interface_set) {
          throw new Error('Failed to fetch machine network information')
        }
        
        console.log(`✅ [Network Modal] Latest machine info loaded for: ${machine.id}`)
        
        // Fabric 목록 가져오기
        const fabricsResponse = await axios.get('http://localhost:8081/api/fabrics', {
          params: apiParams
        })
        
        if (fabricsResponse.data && fabricsResponse.data.results) {
          // Fabric 목록을 {id, name, vlan_id} 형태로 변환
          availableFabrics.value = fabricsResponse.data.results.map(fabric => {
            // vlans 배열에서 첫 번째 vlan의 id 추출
            let vlanId = null
            if (fabric.vlans && Array.isArray(fabric.vlans) && fabric.vlans.length > 0) {
              vlanId = fabric.vlans[0].id
              // 타입을 숫자로 통일 (문자열이면 숫자로 변환)
              if (typeof vlanId === 'string') {
                vlanId = parseInt(vlanId, 10)
              }
            }
            
            // fabric id -> vlan_id 매핑 저장 (fabric.id가 숫자/문자열 모두 가능하므로 두 가지 키로 저장)
            if (vlanId !== null && !isNaN(vlanId)) {
              // 원본 fabric.id를 그대로 키로 사용
              fabricVlanMap.value[fabric.id] = vlanId
              // 문자열/숫자 변환 버전도 저장 (타입 불일치 대비)
              if (typeof fabric.id === 'number') {
                fabricVlanMap.value[String(fabric.id)] = vlanId
              } else if (typeof fabric.id === 'string') {
                const numId = parseInt(fabric.id, 10)
                if (!isNaN(numId)) {
                  fabricVlanMap.value[numId] = vlanId
                }
              }
            }
            
            return {
              id: fabric.id,
              name: fabric.name || `fabric-${fabric.id}`,
              vlan_id: vlanId
            }
          })
          console.log('Fabrics loaded:', availableFabrics.value.map(f => ({ id: f.id, id_type: typeof f.id, name: f.name, vlan_id: f.vlan_id, vlan_id_type: typeof f.vlan_id })))
          console.log('Fabric VLAN map:', fabricVlanMap.value)
          
          // 각 fabric에 대한 vlans 가져오기 (Secondary IP subnet 필터링용)
          fabricVlanIdsMap.value = {}
          for (const fabric of availableFabrics.value) {
            const fabricId = String(fabric.id)
            try {
              const vlansResponse = await axios.get(`http://localhost:8081/api/fabrics/${fabricId}/vlans`, {
                params: apiParams
              })
              
              if (vlansResponse.data && vlansResponse.data.results) {
                const vlanIds = vlansResponse.data.results.map(vlan => {
                  let vlanId = vlan.id
                  if (vlanId !== null && vlanId !== undefined) {
                    if (typeof vlanId === 'string') {
                      vlanId = parseInt(vlanId, 10)
                    }
                  }
                  return vlanId
                }).filter(id => id !== null && !isNaN(id))
                
                // fabric id를 키로 하여 vlan_id 배열 저장 (타입 안전성을 위해 여러 키로 저장)
                fabricVlanIdsMap.value[fabric.id] = vlanIds
                fabricVlanIdsMap.value[String(fabric.id)] = vlanIds
                if (typeof fabric.id === 'number') {
                  fabricVlanIdsMap.value[String(fabric.id)] = vlanIds
                } else if (typeof fabric.id === 'string') {
                  const numId = parseInt(fabric.id, 10)
                  if (!isNaN(numId)) {
                    fabricVlanIdsMap.value[numId] = vlanIds
                  }
                }
                
                console.log(`Fabric ${fabricId} vlans loaded:`, vlanIds)
              }
            } catch (err) {
              console.warn(`Failed to load vlans for fabric ${fabricId}:`, err)
            }
          }
        } else {
          console.warn('No fabrics found in response')
          availableFabrics.value = []
        }
        
        // Subnet 목록 가져오기
        const subnetsResponse = await axios.get('http://localhost:8081/api/subnets', {
          params: apiParams
        })
        
        if (subnetsResponse.data && subnetsResponse.data.results) {
          availableSubnets.value = subnetsResponse.data.results.map(subnet => {
            let vlanId = subnet.vlan?.id || subnet.vlan_id
            // 타입을 숫자로 통일 (문자열이면 숫자로 변환)
            if (vlanId !== null && vlanId !== undefined) {
              if (typeof vlanId === 'string') {
                vlanId = parseInt(vlanId, 10)
              }
            }
            return {
              id: subnet.id,
              cidr: subnet.cidr,
              vlan_id: vlanId
            }
          })
          console.log('Subnets loaded:', availableSubnets.value.map(s => ({ id: s.id, cidr: s.cidr, vlan_id: s.vlan_id, vlan_id_type: typeof s.vlan_id })))
        } else {
          console.warn('No subnets found in response')
          availableSubnets.value = []
        }
        
        // 머신의 interface_set 정보 가져오기 (최신 정보 사용)
        if (latestMachine.interface_set && Array.isArray(latestMachine.interface_set) && latestMachine.interface_set.length > 0) {
          // interface_set 데이터를 Primary/Secondary 구조로 변환
          networkInterfaces.value = latestMachine.interface_set.map(iface => {
            console.log('=== Processing interface:', iface.name, '===')
            console.log('Interface ID:', iface.id, 'type:', typeof iface.id)
            console.log('Raw vlan object:', JSON.stringify(iface.vlan, null, 2))
            
            // Fabric ID 추출: interface.vlan.fabric_id에서 가져오기
            let fabricId = null
            if (iface.vlan && iface.vlan.fabric_id !== null && iface.vlan.fabric_id !== undefined) {
              fabricId = iface.vlan.fabric_id
              // 숫자로 변환 (문자열일 수도 있음)
              if (typeof fabricId === 'string') {
                const numId = parseInt(fabricId, 10)
                if (!isNaN(numId)) {
                  fabricId = numId
                }
              }
              console.log(`Interface ${iface.name}: extracted fabricId=${fabricId} (${typeof fabricId}) from vlan.fabric_id`)
            } else {
              console.log(`Interface ${iface.name}: No fabric_id found in vlan`)
            }
            
            console.log(`Available fabrics:`, availableFabrics.value.map(f => ({ id: f.id, id_type: typeof f.id, name: f.name })))
            console.log(`Checking if fabricId exists in availableFabrics:`, fabricId !== null ? availableFabrics.value.some(f => 
              f.id === fabricId || 
              String(f.id) === String(fabricId) || 
              Number(f.id) === Number(fabricId)
            ) : false)
            
            // vlan_id 찾기: interface.vlan.id에서 직접 가져오기
            let vlanId = null
            if (iface.vlan && iface.vlan.id !== null && iface.vlan.id !== undefined) {
              vlanId = iface.vlan.id
              // 숫자로 변환
              if (typeof vlanId === 'string') {
                const numId = parseInt(vlanId, 10)
                if (!isNaN(numId)) {
                  vlanId = numId
                }
              }
              console.log(`Interface ${iface.name}: vlan_id=${vlanId} (${typeof vlanId}) from vlan.id`)
            } else if (fabricId !== null && fabricId !== undefined && fabricId !== '') {
              // vlan.id가 없으면 fabricVlanMap에서 찾기
              vlanId = fabricVlanMap.value[fabricId] || 
                       fabricVlanMap.value[String(fabricId)] || 
                       fabricVlanMap.value[Number(fabricId)]
              console.log(`Interface ${iface.name}: vlan_id=${vlanId} from fabricVlanMap`)
            }
            
            // vlan_id에 맞는 subnet 찾기 (Primary용)
            let matchedSubnet = null
            if (vlanId) {
              let searchVlanId = vlanId
              if (typeof searchVlanId === 'string') {
                searchVlanId = parseInt(searchVlanId, 10)
              }
              matchedSubnet = availableSubnets.value.find(subnet => {
                let subnetVlanId = subnet.vlan_id
                if (subnetVlanId !== null && subnetVlanId !== undefined && typeof subnetVlanId === 'string') {
                  subnetVlanId = parseInt(subnetVlanId, 10)
                }
                return subnetVlanId === searchVlanId
              })
            }
            
            console.log(`Interface ${iface.name}: matchedSubnet=`, matchedSubnet)
            console.log(`Interface ${iface.name}: links=`, iface.links?.map(l => ({ 
              ip: l.ip_address, 
              subnet_id: l.subnet?.id, 
              subnet_cidr: l.subnet?.cidr 
            })))
            
            // Primary IP와 Secondary IPs 결정
            let primaryIp = ''
            let primaryLink = null
            const secondaryIpAddresses = []
            
            if (iface.links && Array.isArray(iface.links) && iface.links.length > 0) {
              // Primary IP: matchedSubnet의 id와 일치하는 link 찾기
              if (matchedSubnet && matchedSubnet.id) {
                const primaryLinkFound = iface.links.find(link => {
                  // link.subnet이 객체인 경우 id 확인
                  const linkSubnetId = link.subnet?.id || link.subnet
                  const linkSubnetCidr = link.subnet?.cidr || link.cidr
                  
                  // 1. subnet ID로 매칭
                  if (linkSubnetId) {
                    const matchById = linkSubnetId === matchedSubnet.id || 
                                     String(linkSubnetId) === String(matchedSubnet.id) ||
                                     Number(linkSubnetId) === Number(matchedSubnet.id)
                    if (matchById) {
                      console.log(`Link subnet match by ID: linkSubnetId=${linkSubnetId}, matchedSubnet.id=${matchedSubnet.id}`)
                      return true
                    }
                  }
                  
                  // 2. CIDR로 매칭 (subnet ID가 없거나 매칭되지 않은 경우)
                  if (linkSubnetCidr && matchedSubnet.cidr) {
                    const matchByCidr = linkSubnetCidr === matchedSubnet.cidr
                    if (matchByCidr) {
                      console.log(`Link subnet match by CIDR: linkSubnetCidr=${linkSubnetCidr}, matchedSubnet.cidr=${matchedSubnet.cidr}`)
                      return true
                    }
                  }
                  
                  console.log(`Link subnet no match: linkSubnetId=${linkSubnetId} (${typeof linkSubnetId}), linkSubnetCidr=${linkSubnetCidr}, matchedSubnet.id=${matchedSubnet.id} (${typeof matchedSubnet.id}), matchedSubnet.cidr=${matchedSubnet.cidr}`)
                  return false
                })
                
                if (primaryLinkFound) {
                  primaryLink = primaryLinkFound
                  primaryIp = primaryLinkFound.ip_address || ''
                  console.log(`Primary IP found for ${iface.name}: ${primaryIp}`)
                  
                  // 나머지 links를 Secondary로
                  iface.links.forEach(link => {
                    if (link !== primaryLinkFound) {
                      const secIp = link.ip_address || ''
                      const linkSubnetId = link.subnet?.id || link.subnet
                      const linkId = link.id ? String(link.id) : null
                      let secSubnet = null
                      
                      if (linkSubnetId) {
                        secSubnet = availableSubnets.value.find(subnet => 
                          subnet.id === linkSubnetId || 
                          String(subnet.id) === String(linkSubnetId) ||
                          Number(subnet.id) === Number(linkSubnetId)
                        )
                      }
                      
                      if (!secSubnet && link.subnet?.cidr) {
                        secSubnet = availableSubnets.value.find(subnet => subnet.cidr === link.subnet.cidr)
                      }
                      
                      secondaryIpAddresses.push({
                        address: secIp,
                        subnet: secSubnet,
                        originalLinkId: linkId, // 기존 link ID 저장 (삭제 시 사용)
                        invalid: false
                      })
                    }
                  })
                } else {
                  // Primary subnet과 매칭되는 link가 없으면 첫 번째 link를 Primary로
                  console.log(`No matching link found for Primary subnet, using first link`)
                  primaryLink = iface.links[0]
                  primaryIp = iface.links[0]?.ip_address || ''
                  // 나머지 links를 Secondary로
                  for (let i = 1; i < iface.links.length; i++) {
                    const link = iface.links[i]
                    const secIp = link.ip_address || ''
                    const linkSubnetId = link.subnet?.id || link.subnet
                    const linkId = link.id ? String(link.id) : null
                    let secSubnet = null
                    
                    if (linkSubnetId) {
                      secSubnet = availableSubnets.value.find(subnet => 
                        subnet.id === linkSubnetId || 
                        String(subnet.id) === String(linkSubnetId) ||
                        Number(subnet.id) === Number(linkSubnetId)
                      )
                    }
                    
                    if (!secSubnet && link.subnet?.cidr) {
                      secSubnet = availableSubnets.value.find(subnet => subnet.cidr === link.subnet.cidr)
                    }
                    
                    secondaryIpAddresses.push({
                      address: secIp,
                      subnet: secSubnet,
                      originalLinkId: linkId, // 기존 link ID 저장 (삭제 시 사용)
                      invalid: false
                    })
                  }
                }
              } else {
                // Fabric이 선택되지 않았거나 subnet이 없으면 첫 번째 link를 Primary로
                console.log(`No matchedSubnet, using first link as Primary`)
                primaryLink = iface.links[0]
                primaryIp = iface.links[0]?.ip_address || ''
                // 나머지 links를 Secondary로
                for (let i = 1; i < iface.links.length; i++) {
                  const link = iface.links[i]
                  const secIp = link.ip_address || ''
                  const linkSubnetId = link.subnet?.id || link.subnet
                  let secSubnet = null
                  
                  if (linkSubnetId) {
                    secSubnet = availableSubnets.value.find(subnet => 
                      subnet.id === linkSubnetId || 
                      String(subnet.id) === String(linkSubnetId) ||
                      Number(subnet.id) === Number(linkSubnetId)
                    )
                  }
                  
                  if (!secSubnet && link.subnet?.cidr) {
                    secSubnet = availableSubnets.value.find(subnet => subnet.cidr === link.subnet.cidr)
                  }
                  
                  secondaryIpAddresses.push({
                    address: secIp,
                    subnet: secSubnet,
                    invalid: false
                  })
                }
              }
            }
            
            // Primary IP 설정: 기존 IP만 표시 (fabric 선택 전까지는 prefix를 넣지 않음)
            const primaryIpValue = primaryIp || ''
            // Primary link의 ID 저장 (IP 변경 시 기존 link 삭제용)
            const originalPrimaryLinkId = primaryLink?.id ? String(primaryLink.id) : null
            
            // IP Assignment 상태 결정
            // - Primary link가 있고 IP 주소가 있으면 'static'
            // - Primary link가 있지만 IP 주소가 없으면 'automatic'
            // - Primary link가 없으면 'unconfigured'
            let ipAssignment = 'unconfigured'
            if (primaryLink) {
              if (primaryIpValue && primaryIpValue.trim() !== '') {
                ipAssignment = 'static'
              } else {
                ipAssignment = 'automatic'
              }
            }
            
            console.log(`Interface ${iface.name}: Final - fabricId=${fabricId}, primaryIp=${primaryIpValue}, secondaryCount=${secondaryIpAddresses.length}, originalPrimaryLinkId=${originalPrimaryLinkId || 'N/A'}, ipAssignment=${ipAssignment}`)
            
            // Secondary IP에 subnet이 있으면 prefix 설정
            secondaryIpAddresses.forEach(secIp => {
              if (secIp.subnet && secIp.subnet.cidr) {
                const networkPrefix = extractNetworkPrefix(secIp.subnet.cidr)
                if (networkPrefix) {
                  // 기존 IP가 있으면 그대로 유지 (실제 IP가 있는 경우)
                  // 기존 IP가 없거나 prefix로 시작하지 않으면 prefix 설정
                  if (!secIp.address || !secIp.address.startsWith(networkPrefix)) {
                    // IP가 없으면 prefix만 설정
                    secIp.address = networkPrefix
                  }
                  // 기존 IP가 있고 prefix로 시작하면 그대로 유지 (실제 IP가 있는 경우)
                }
              }
            })
            
            // 원본 Secondary IP 목록 저장 (삭제 감지용)
            const originalSecondaryIpAddresses = JSON.parse(JSON.stringify(secondaryIpAddresses))
            
            return {
              ...iface,
              editableFabric: fabricId !== null && fabricId !== undefined && fabricId !== '' ? Number(fabricId) : null,
              ipAssignment: ipAssignment, // IP Assignment 상태
              originalIpAssignment: ipAssignment, // 원본 IP Assignment 저장 (변경 감지용)
              primaryIpAddress: primaryIpValue,
              originalPrimaryIpAddress: primaryIpValue, // 원본 Primary IP 저장 (변경 감지용)
              originalPrimaryLinkId: originalPrimaryLinkId, // 원본 Primary link ID 저장 (IP 변경 시 기존 link 삭제용)
              originalSecondaryIpAddresses: originalSecondaryIpAddresses, // 원본 Secondary IP 목록 저장 (삭제 감지용)
              primaryIpInvalid: false,
              matchedSubnet: matchedSubnet,
              secondaryIpAddresses: secondaryIpAddresses
            }
          })
          
          console.log('Network interfaces loaded:', networkInterfaces.value)
        } else {
          networkInterfaces.value = []
          networkError.value = 'No network interfaces found for this machine'
        }
        
        loadingNetwork.value = false
      } catch (err) {
        console.error('Error loading network interfaces:', err)
        networkError.value = err.response?.data?.error || err.message || 'Failed to load network interfaces'
        loadingNetwork.value = false
      }
    }
    
    // Fabric 선택 시 해당 인터페이스의 subnet 업데이트
    const updateFabricForInterface = (networkInterface) => {
      const fabricId = networkInterface.editableFabric
      // fabricId가 null, undefined, 빈 문자열인 경우만 체크 (0은 유효한 값)
      if (fabricId === null || fabricId === undefined || fabricId === '') {
        networkInterface.matchedSubnet = null
        // Fabric 선택 해제 시 기존 IP 주소만 유지 (prefix 제거)
        const currentIp = networkInterface.primaryIpAddress || ''
        if (currentIp && currentIp.endsWith('.')) {
          // prefix만 있는 경우 제거
          networkInterface.primaryIpAddress = ''
        }
        return
      }
      
      // Fabric의 vlan_id 찾기 (fabric.id가 숫자/문자열 모두 가능하므로 여러 방법으로 시도)
      const fabric = availableFabrics.value.find(f => {
        // fabric.id가 숫자/문자열 모두 가능하므로 두 가지 방법으로 비교
        return f.id === fabricId || f.id == fabricId || String(f.id) === String(fabricId)
      })
      
      console.log(`Fabric 선택: fabricId=${fabricId} (${typeof fabricId}), fabric=`, fabric)
      console.log(`사용 가능한 fabrics:`, availableFabrics.value.map(f => ({ id: f.id, id_type: typeof f.id, name: f.name, vlan_id: f.vlan_id })))
      
      // vlanId 찾기: fabric에서 직접 찾거나 fabricVlanMap에서 찾기
      let vlanId = null
      if (fabric) {
        vlanId = fabric.vlan_id
      } else {
        // fabricVlanMap에서 찾기 (fabricId가 숫자/문자열 모두 가능)
        vlanId = fabricVlanMap.value[fabricId] || 
                 fabricVlanMap.value[String(fabricId)] || 
                 fabricVlanMap.value[Number(fabricId)]
      }
      
      console.log(`Fabric VLAN map keys:`, Object.keys(fabricVlanMap.value))
      console.log(`Fabric VLAN map:`, fabricVlanMap.value)
      console.log(`찾은 vlanId=${vlanId} (${typeof vlanId})`)
      
      // vlan_id 타입 통일 (숫자로)
      if (vlanId !== null && vlanId !== undefined) {
        if (typeof vlanId === 'string') {
          vlanId = parseInt(vlanId, 10)
        }
      }
      
      if (!vlanId || isNaN(vlanId)) {
        console.warn(`Fabric ${fabricId}의 vlan_id를 찾을 수 없습니다. fabric=`, fabric)
        console.warn(`fabricVlanMap에서 직접 확인:`, {
          'fabricId': fabricVlanMap.value[fabricId],
          'String(fabricId)': fabricVlanMap.value[String(fabricId)],
          'Number(fabricId)': fabricVlanMap.value[Number(fabricId)]
        })
        networkInterface.matchedSubnet = null
        networkInterface.primaryIpAddress = ''
        return
      }
      
      // vlan_id에 맞는 subnet 찾기 (타입 변환 후 비교)
      console.log(`Subnet 찾기 시작: vlanId=${vlanId} (${typeof vlanId})`)
      console.log(`사용 가능한 subnets 전체:`, availableSubnets.value.map(s => {
        let svlanId = s.vlan_id
        const originalType = typeof svlanId
        if (svlanId !== null && svlanId !== undefined && typeof svlanId === 'string') {
          svlanId = parseInt(svlanId, 10)
        }
        return { 
          id: s.id, 
          cidr: s.cidr, 
          vlan_id: svlanId, 
          vlan_id_original_type: originalType,
          vlan_id_converted_type: typeof svlanId,
          matches: svlanId === vlanId
        }
      }))
      
      const matchedSubnet = availableSubnets.value.find(subnet => {
        let subnetVlanId = subnet.vlan_id
        if (subnetVlanId !== null && subnetVlanId !== undefined) {
          if (typeof subnetVlanId === 'string') {
            subnetVlanId = parseInt(subnetVlanId, 10)
          }
        }
        const matches = subnetVlanId === vlanId
        if (matches) {
          console.log(`매칭된 subnet 발견:`, { cidr: subnet.cidr, vlan_id: subnetVlanId, original_vlan_id: subnet.vlan_id })
        }
        return matches
      })
      
      console.log(`Subnet 찾기 결과: vlanId=${vlanId} (${typeof vlanId}), matchedSubnet=`, matchedSubnet)
      
      networkInterface.matchedSubnet = matchedSubnet || null
      
      // 네트워크 프리픽스를 Primary IP에 설정
      if (matchedSubnet && matchedSubnet.cidr) {
        const networkPrefix = extractNetworkPrefix(matchedSubnet.cidr)
        console.log(`네트워크 프리픽스 추출: cidr=${matchedSubnet.cidr}, prefix=${networkPrefix}`)
        
        // 기존 IP 주소가 없거나 네트워크 프리픽스로 시작하지 않으면 네트워크 프리픽스로 설정
        if (!networkInterface.primaryIpAddress || !networkInterface.primaryIpAddress.startsWith(networkPrefix)) {
          networkInterface.primaryIpAddress = networkPrefix
        }
      } else {
        console.warn(`Subnet을 찾을 수 없거나 CIDR이 없습니다. fabricId=${fabricId}, vlanId=${vlanId}`)
        networkInterface.primaryIpAddress = ''
      }
    }
    
    const closeNetworkModal = () => {
      showNetworkModalState.value = false
      selectedMachine.value = null
      networkInterfaces.value = []
      networkError.value = null
      availableFabrics.value = []
      availableSubnets.value = []
      fabricVlanMap.value = {}
    }
    
    const saveNetworkChanges = async () => {
      savingNetwork.value = true
      networkError.value = null
      
      try {
        const apiParams = settingsStore.getApiParams.value
        const machineId = selectedMachine.value.id
        
        // 각 인터페이스에 대해 변경사항 저장
        console.log(`[Save Network] Starting save for ${networkInterfaces.value.length} interfaces`)
        const saveErrors = [] // 각 인터페이스별 에러 수집
        
        for (const networkInterface of networkInterfaces.value) {
          const interfaceId = networkInterface.id
          const interfaceName = networkInterface.name || interfaceId
          const currentPrimaryIp = networkInterface.primaryIpAddress ? networkInterface.primaryIpAddress.trim() : ''
          const originalPrimaryIp = networkInterface.originalPrimaryIpAddress ? networkInterface.originalPrimaryIpAddress.trim() : ''
          const primaryIpChanged = currentPrimaryIp !== originalPrimaryIp
          console.log(`[Save Network] Processing interface: name=${interfaceName}, id=${interfaceId}, id_type=${typeof interfaceId}, primaryIp=${currentPrimaryIp || 'N/A'}, originalPrimaryIp=${originalPrimaryIp || 'N/A'}, changed=${primaryIpChanged}`)
          if (!interfaceId) {
            console.warn('Interface ID가 없습니다:', networkInterface)
            continue
          }
          
          // 인터페이스 ID를 문자열로 명시적으로 변환 (모든 블록에서 사용)
          const interfaceIdStr = String(interfaceId)
          
          // 각 인터페이스를 독립적으로 처리 (에러가 발생해도 다음 인터페이스 계속 처리)
          try {
            // 1. Fabric 변경 저장 (editableFabric이 변경되었으면)
            let fabricChanged = false
            if (networkInterface.editableFabric !== null && networkInterface.editableFabric !== undefined && networkInterface.editableFabric !== '') {
              console.log(`Saving fabric for interface ${interfaceId}: editableFabric=${networkInterface.editableFabric} (${typeof networkInterface.editableFabric})`)
              // 타입 안전한 비교를 위해 여러 방법 시도
              const fabric = availableFabrics.value.find(f => 
                f.id === networkInterface.editableFabric || 
                String(f.id) === String(networkInterface.editableFabric) ||
                Number(f.id) === Number(networkInterface.editableFabric)
              )
              if (fabric && fabric.vlan_id) {
                const vlanId = String(fabric.vlan_id)
                
                console.log(`Updating VLAN for interface ${interfaceIdStr} (original: ${interfaceId}, type: ${typeof interfaceId}): vlanId=${vlanId}`)
                
                const vlanResponse = await axios.put(
                  `http://localhost:8081/api/machines/${machineId}/interfaces/${interfaceIdStr}/vlan`,
                  null,
                  {
                    params: {
                      maasUrl: apiParams.maasUrl,
                      apiKey: apiParams.apiKey,
                      vlanId: vlanId
                    }
                  }
                )
                
                if (!vlanResponse.data || !vlanResponse.data.success) {
                  throw new Error(`Failed to update VLAN for interface ${interfaceName}: ${vlanResponse.data?.error || 'Unknown error'}`)
                }
                
                console.log(`VLAN updated successfully for interface ${interfaceId}`)
                fabricChanged = true
                
                // Fabric 변경 후 최신 머신 정보를 가져와서 올바른 link ID와 subnet 업데이트
                console.log(`[Save Network] Fabric changed, fetching latest machine info to update link IDs and subnet...`)
                try {
                  // 먼저 선택한 fabric에 맞는 subnet 찾기
                  const selectedFabric = availableFabrics.value.find(f => 
                    f.id === networkInterface.editableFabric || 
                    String(f.id) === String(networkInterface.editableFabric) ||
                    Number(f.id) === Number(networkInterface.editableFabric)
                  )
                  
                  if (selectedFabric && selectedFabric.vlan_id) {
                    const vlanId = selectedFabric.vlan_id
                    let searchVlanId = vlanId
                    if (typeof searchVlanId === 'string') {
                      searchVlanId = parseInt(searchVlanId, 10)
                    }
                    
                    // 해당 vlan_id에 맞는 subnet 찾기
                    const matchedSubnet = availableSubnets.value.find(subnet => {
                      let subnetVlanId = subnet.vlan_id
                      if (subnetVlanId !== null && subnetVlanId !== undefined && typeof subnetVlanId === 'string') {
                        subnetVlanId = parseInt(subnetVlanId, 10)
                      }
                      return subnetVlanId === searchVlanId
                    })
                    
                    if (matchedSubnet) {
                      console.log(`[Save Network] Updated matchedSubnet for interface ${interfaceName}: ${networkInterface.matchedSubnet?.id || 'N/A'} → ${matchedSubnet.id} (${matchedSubnet.cidr})`)
                      networkInterface.matchedSubnet = matchedSubnet
                      // subnet이 업데이트되었으므로 확인
                      console.log(`[Save Network] After update, networkInterface.matchedSubnet=`, networkInterface.matchedSubnet)
                    } else {
                      console.warn(`[Save Network] No subnet found for vlan_id=${vlanId} for interface ${interfaceName}`)
                      console.warn(`[Save Network] Available subnets:`, availableSubnets.value.map(s => ({ id: s.id, cidr: s.cidr, vlan_id: s.vlan_id })))
                    }
                  }
                  
                  // 최신 머신 정보 가져오기
                  const machineResponse = await axios.get(`http://localhost:8081/api/machines/${machineId}`, {
                    params: apiParams
                  })
                  
                  if (machineResponse.data && !machineResponse.data.error) {
                    const latestMachine = machineResponse.data
                    const latestInterface = latestMachine.interface_set?.find(iface => 
                      String(iface.id) === interfaceIdStr || iface.name === interfaceName
                    )
                    
                    if (latestInterface && latestInterface.links && latestInterface.links.length > 0) {
                      // Primary link 찾기 (업데이트된 matchedSubnet과 일치하는 link 또는 첫 번째 link)
                      const matchedSubnet = networkInterface.matchedSubnet
                      let primaryLink = null
                      
                      if (matchedSubnet && matchedSubnet.id) {
                        primaryLink = latestInterface.links.find(link => {
                          const linkSubnetId = link.subnet?.id || link.subnet
                          return String(linkSubnetId) === String(matchedSubnet.id)
                        })
                      }
                      
                      if (!primaryLink && latestInterface.links.length > 0) {
                        primaryLink = latestInterface.links[0]
                      }
                      
                      if (primaryLink && primaryLink.id) {
                        const latestPrimaryLinkId = String(primaryLink.id)
                        console.log(`[Save Network] Updated Primary link ID for interface ${interfaceName}: ${networkInterface.originalPrimaryLinkId || 'N/A'} → ${latestPrimaryLinkId}`)
                        networkInterface.originalPrimaryLinkId = latestPrimaryLinkId
                      }
                    }
                  }
                } catch (err) {
                  console.warn(`[Save Network] Failed to fetch latest machine info after fabric change:`, err)
                  // 에러가 발생해도 계속 진행 (기존 link ID 사용)
                }
              }
            }
          
            // 2. Primary IP 저장 (IP Assignment에 따라 처리)
            const subnet = networkInterface.matchedSubnet
            const ipAssignment = networkInterface.ipAssignment || 'unconfigured'
            const originalIpAssignment = networkInterface.originalIpAssignment || 'unconfigured'
            const ipAssignmentChanged = ipAssignment !== originalIpAssignment
            
            console.log(`[Save Network] Checking Primary IP save for interface ${interfaceName}: ipAssignment=${ipAssignment}, originalIpAssignment=${originalIpAssignment}, changed=${ipAssignmentChanged}, subnet=`, subnet)
            
            // IP Assignment가 변경되었거나, Static/Automatic이고 IP가 변경된 경우 처리
            // Automatic일 때도 처리해야 함 (Fabric 변경 시 subnet 매칭 필요)
            if (ipAssignmentChanged || (ipAssignment === 'static' && primaryIpChanged) || (ipAssignment === 'automatic' && ipAssignmentChanged)) {
              const subnetId = subnet ? String(subnet.id) : null
              const originalPrimaryLinkId = networkInterface.originalPrimaryLinkId
              
              if (ipAssignment === 'unconfigured') {
                // Unconfigured: 기존 link 삭제
                if (originalPrimaryLinkId) {
                  console.log(`[Save Network] Unlinking Primary link for Unconfigured interface ${interfaceName} (id: ${interfaceIdStr})`)
                  
                  const unlinkResponse = await axios.post(
                    `http://localhost:8081/api/machines/${machineId}/interfaces/${interfaceIdStr}/unlink-subnet`,
                    null,
                    {
                      params: {
                        maasUrl: apiParams.maasUrl,
                        apiKey: apiParams.apiKey,
                        linkId: originalPrimaryLinkId
                      }
                    }
                  )
                  
                  if (!unlinkResponse.data || !unlinkResponse.data.success) {
                    const errorMessage = unlinkResponse.data?.error || 'Unknown error'
                    console.error(`[Save Network] Failed to unlink Primary link for interface ${interfaceName} (id: ${interfaceIdStr}):`, errorMessage)
                    throw new Error(`Failed to unlink Primary link for interface ${interfaceName} (id: ${interfaceIdStr}): ${errorMessage}`)
                  }
                  
                  console.log(`[Save Network] Primary link unlinked successfully for Unconfigured interface ${interfaceName} (id: ${interfaceIdStr})`)
                }
              } else if (ipAssignment === 'automatic') {
                // Automatic: AUTO mode로 link 생성
                if (!subnet || !subnet.id) {
                  throw new Error(`Automatic IP Assignment을 저장하려면 Fabric을 선택하고 Subnet이 매칭되어야 합니다. (Interface: ${networkInterface.name || interfaceId})`)
                }
                
                console.log(`[Save Network] Creating Primary link with AUTO mode for interface ${interfaceName} (id: ${interfaceIdStr}): subnetId=${subnetId}`)
                
                // 기존 Primary link가 있으면 먼저 삭제
                if (originalPrimaryLinkId) {
                  console.log(`[Save Network] Unlinking existing Primary link (id: ${originalPrimaryLinkId}) for interface ${interfaceName}`)
                  
                  const unlinkResponse = await axios.post(
                    `http://localhost:8081/api/machines/${machineId}/interfaces/${interfaceIdStr}/unlink-subnet`,
                    null,
                    {
                      params: {
                        maasUrl: apiParams.maasUrl,
                        apiKey: apiParams.apiKey,
                        linkId: originalPrimaryLinkId
                      }
                    }
                  )
                  
                  if (!unlinkResponse.data || !unlinkResponse.data.success) {
                    const errorMessage = unlinkResponse.data?.error || 'Unknown error'
                    console.error(`[Save Network] Failed to unlink Primary link for interface ${interfaceName} (id: ${interfaceIdStr}):`, errorMessage)
                    throw new Error(`Failed to unlink Primary link for interface ${interfaceName} (id: ${interfaceIdStr}): ${errorMessage}`)
                  }
                  
                  console.log(`[Save Network] Primary link unlinked successfully for interface ${interfaceName} (id: ${interfaceIdStr})`)
                }
                
                // 새로운 Primary link 생성 (AUTO mode - ipAddress 없이)
                const linkResponse = await axios.post(
                  `http://localhost:8081/api/machines/${machineId}/interfaces/${interfaceIdStr}/link-subnet`,
                  null,
                  {
                    params: {
                      maasUrl: apiParams.maasUrl,
                      apiKey: apiParams.apiKey,
                      subnetId: subnetId
                      // ipAddress를 포함하지 않으면 백엔드에서 AUTO mode로 처리
                    }
                  }
                )
                
                if (!linkResponse.data || !linkResponse.data.success) {
                  const errorMessage = linkResponse.data?.error || 'Unknown error'
                  console.error(`[Save Network] Failed to link Primary link with AUTO mode for interface ${interfaceName} (id: ${interfaceIdStr}):`, errorMessage)
                  throw new Error(`Failed to link Primary link with AUTO mode for interface ${interfaceName} (id: ${interfaceIdStr}): ${errorMessage}`)
                }
                
                console.log(`[Save Network] Primary link with AUTO mode linked successfully for interface ${interfaceName} (id: ${interfaceIdStr})`)
              } else if (ipAssignment === 'static') {
                // Static: STATIC mode로 link 생성 (IP 주소 필요)
                if (!subnet || !subnet.id) {
                  throw new Error(`Static IP Assignment을 저장하려면 Fabric을 선택하고 Subnet이 매칭되어야 합니다. (Interface: ${networkInterface.name || interfaceId})`)
                }
                
                // 유효한 IP 주소 확인
                const hasValidIp = currentPrimaryIp && 
                                   currentPrimaryIp.trim() !== '' && 
                                   !currentPrimaryIp.endsWith('.') && 
                                   !networkInterface.primaryIpInvalid &&
                                   isValidIpAddress(currentPrimaryIp)
                
                if (!hasValidIp) {
                  throw new Error(`Static IP Assignment을 저장하려면 유효한 IP 주소를 입력해야 합니다. (Interface: ${networkInterface.name || interfaceId})`)
                }
                
                const ipAddress = currentPrimaryIp
                console.log(`[Save Network] Updating Primary IP for interface ${interfaceName} (id: ${interfaceIdStr}): ip=${ipAddress} (changed from ${originalPrimaryIp || 'empty'}), subnetId=${subnetId}, originalLinkId=${originalPrimaryLinkId || 'N/A'}`)
                
                // 기존 Primary link가 있으면 먼저 삭제
                if (originalPrimaryLinkId) {
                  console.log(`[Save Network] Unlinking existing Primary link (id: ${originalPrimaryLinkId}) for interface ${interfaceName}`)
                  
                  const unlinkResponse = await axios.post(
                    `http://localhost:8081/api/machines/${machineId}/interfaces/${interfaceIdStr}/unlink-subnet`,
                    null,
                    {
                      params: {
                        maasUrl: apiParams.maasUrl,
                        apiKey: apiParams.apiKey,
                        linkId: originalPrimaryLinkId
                      }
                    }
                  )
                  
                  if (!unlinkResponse.data || !unlinkResponse.data.success) {
                    const errorMessage = unlinkResponse.data?.error || 'Unknown error'
                    console.error(`[Save Network] Failed to unlink Primary link for interface ${interfaceName} (id: ${interfaceIdStr}):`, errorMessage)
                    throw new Error(`Failed to unlink Primary link for interface ${interfaceName} (id: ${interfaceIdStr}): ${errorMessage}`)
                  }
                  
                  console.log(`[Save Network] Primary link unlinked successfully for interface ${interfaceName} (id: ${interfaceIdStr})`)
                }
                
                // 새로운 Primary link 생성 (STATIC mode)
                console.log(`[Save Network] Linking new Primary IP for interface ${interfaceName} (id: ${interfaceIdStr})`)
                
                const linkResponse = await axios.post(
                  `http://localhost:8081/api/machines/${machineId}/interfaces/${interfaceIdStr}/link-subnet`,
                  null,
                  {
                    params: {
                      maasUrl: apiParams.maasUrl,
                      apiKey: apiParams.apiKey,
                      ipAddress: ipAddress,
                      subnetId: subnetId
                    }
                  }
                )
                
                if (!linkResponse.data || !linkResponse.data.success) {
                  const errorMessage = linkResponse.data?.error || 'Unknown error'
                  console.error(`[Save Network] Failed to link Primary IP for interface ${interfaceName} (id: ${interfaceIdStr}):`, errorMessage)
                  throw new Error(`Failed to link Primary IP for interface ${interfaceName} (id: ${interfaceIdStr}): ${errorMessage}`)
                }
                
                console.log(`[Save Network] Primary IP linked successfully for interface ${interfaceName} (id: ${interfaceIdStr})`)
              }
            }
          
            // 3. Secondary IPs 처리
            const originalSecondaryIps = networkInterface.originalSecondaryIpAddresses || []
            const currentSecondaryIps = networkInterface.secondaryIpAddresses || []
            
            // 3-1. 삭제된 Secondary IP 처리 (originalLinkId가 있지만 현재 배열에 없는 것)
            for (const originalSecIp of originalSecondaryIps) {
              if (originalSecIp.originalLinkId) {
                // 현재 배열에 같은 originalLinkId가 있는지 확인
                const stillExists = currentSecondaryIps.some(secIp => secIp.originalLinkId === originalSecIp.originalLinkId)
                
                if (!stillExists) {
                  // 삭제된 Secondary IP - unlink 호출
                  console.log(`[Save Network] Unlinking deleted Secondary link (id: ${originalSecIp.originalLinkId}) for interface ${interfaceName}`)
                  
                  const unlinkResponse = await axios.post(
                    `http://localhost:8081/api/machines/${machineId}/interfaces/${interfaceIdStr}/unlink-subnet`,
                    null,
                    {
                      params: {
                        maasUrl: apiParams.maasUrl,
                        apiKey: apiParams.apiKey,
                        linkId: originalSecIp.originalLinkId
                      }
                    }
                  )
                  
                  if (!unlinkResponse.data || !unlinkResponse.data.success) {
                    const errorMessage = unlinkResponse.data?.error || 'Unknown error'
                    console.error(`[Save Network] Failed to unlink Secondary link for interface ${interfaceName}:`, errorMessage)
                    throw new Error(`Failed to unlink Secondary link for interface ${interfaceName}: ${errorMessage}`)
                  }
                  
                  console.log(`[Save Network] Secondary link unlinked successfully for interface ${interfaceName}`)
                }
              }
            }
            
            // 3-2. 새로운 또는 변경된 Secondary IP 처리
            for (const secondaryIp of currentSecondaryIps) {
              const subnet = secondaryIp.subnet
              
              if (!subnet || !subnet.id) {
                // Subnet이 없으면 건너뛰기 (새로 추가한 항목이 아직 subnet을 선택하지 않은 경우)
                console.warn(`[Save Network] Secondary IP를 저장하려면 Subnet이 필요합니다. 건너뜁니다. (Interface: ${interfaceName})`)
                continue
              }
              
              const subnetId = String(subnet.id)
              const ipAddress = secondaryIp.address ? secondaryIp.address.trim() : ''
              const hasIpAddress = ipAddress && !secondaryIp.invalid
              const isNewSecondaryIp = !secondaryIp.originalLinkId // originalLinkId가 없으면 새로운 Secondary IP
              
              if (isNewSecondaryIp) {
                // 새로운 Secondary IP - link 호출
                console.log(`[Save Network] Linking new Secondary IP for interface ${interfaceName}: ip=${ipAddress || 'AUTO'}, subnetId=${subnetId}, hasIpAddress=${hasIpAddress}`)
                
                // IP가 없으면 params에서 ipAddress를 제외 (백엔드에서 AUTO mode로 처리)
                const linkParams = {
                  maasUrl: apiParams.maasUrl,
                  apiKey: apiParams.apiKey,
                  subnetId: subnetId
                }
                // IP가 있으면 params에 추가
                if (hasIpAddress) {
                  linkParams.ipAddress = ipAddress
                  console.log(`[Save Network] Adding ipAddress to params: ${ipAddress}`)
                } else {
                  console.log(`[Save Network] Not adding ipAddress to params (will use AUTO mode)`)
                }
                
                console.log(`[Save Network] Request params:`, linkParams)
                
                const linkResponse = await axios.post(
                  `http://localhost:8081/api/machines/${machineId}/interfaces/${interfaceIdStr}/link-subnet`,
                  null,
                  {
                    params: linkParams
                  }
                )
                
                if (!linkResponse.data || !linkResponse.data.success) {
                  const errorMessage = linkResponse.data?.error || 'Unknown error'
                  console.error(`[Save Network] Failed to link Secondary IP for interface ${interfaceName}:`, errorMessage)
                  throw new Error(`Failed to link Secondary IP for interface ${interfaceName}: ${errorMessage}`)
                }
                
                console.log(`[Save Network] Secondary IP linked successfully for interface ${interfaceName}`)
              } else {
                // 기존 Secondary IP가 변경된 경우 (IP 주소나 Subnet이 변경된 경우)
                // 기존 link 삭제 후 새로 생성
                const originalSecIp = originalSecondaryIps.find(orig => orig.originalLinkId === secondaryIp.originalLinkId)
                const ipChanged = originalSecIp && originalSecIp.address !== ipAddress
                const subnetChanged = originalSecIp && originalSecIp.subnet?.id !== subnet.id
                
                if (ipChanged || subnetChanged) {
                  console.log(`[Save Network] Updating Secondary IP for interface ${interfaceName}: ip changed=${ipChanged}, subnet changed=${subnetChanged}`)
                  
                  // 기존 link 삭제
                  const unlinkResponse = await axios.post(
                    `http://localhost:8081/api/machines/${machineId}/interfaces/${interfaceIdStr}/unlink-subnet`,
                    null,
                    {
                      params: {
                        maasUrl: apiParams.maasUrl,
                        apiKey: apiParams.apiKey,
                        linkId: secondaryIp.originalLinkId
                      }
                    }
                  )
                  
                  if (!unlinkResponse.data || !unlinkResponse.data.success) {
                    const errorMessage = unlinkResponse.data?.error || 'Unknown error'
                    console.error(`[Save Network] Failed to unlink Secondary link for interface ${interfaceName}:`, errorMessage)
                    throw new Error(`Failed to unlink Secondary link for interface ${interfaceName}: ${errorMessage}`)
                  }
                  
                  console.log(`[Save Network] Secondary link unlinked successfully for interface ${interfaceName}`)
                  
                  // 새로운 link 생성
                  // IP가 없으면 params에서 ipAddress를 제외 (백엔드에서 AUTO mode로 처리)
                  const linkParams = {
                    maasUrl: apiParams.maasUrl,
                    apiKey: apiParams.apiKey,
                    subnetId: subnetId
                  }
                  // IP가 있으면 params에 추가
                  if (hasIpAddress) {
                    linkParams.ipAddress = ipAddress
                  }
                  
                  const linkResponse = await axios.post(
                    `http://localhost:8081/api/machines/${machineId}/interfaces/${interfaceIdStr}/link-subnet`,
                    null,
                    {
                      params: linkParams
                    }
                  )
                  
                  if (!linkResponse.data || !linkResponse.data.success) {
                    const errorMessage = linkResponse.data?.error || 'Unknown error'
                    console.error(`[Save Network] Failed to link Secondary IP for interface ${interfaceName}:`, errorMessage)
                    throw new Error(`Failed to link Secondary IP for interface ${interfaceName}: ${errorMessage}`)
                  }
                  
                  console.log(`[Save Network] Secondary IP linked successfully for interface ${interfaceName}`)
                }
              }
            }
            
            console.log(`[Save Network] Successfully saved all changes for interface ${interfaceName}`)
          } catch (err) {
            // 각 인터페이스별 에러를 수집하고 다음 인터페이스 계속 처리
            const errorMessage = err.response?.data?.error || err.message || 'Unknown error'
            console.error(`[Save Network] Error saving interface ${interfaceName}:`, errorMessage)
            saveErrors.push({
              interface: interfaceName,
              error: errorMessage
            })
          }
        }
        
        // 에러가 있으면 표시
        if (saveErrors.length > 0) {
          const errorMessages = saveErrors.map(e => `${e.interface}: ${e.error}`).join('\n')
          console.error(`[Save Network] Errors occurred for ${saveErrors.length} interface(s):`, errorMessages)
          networkError.value = `일부 인터페이스 저장 실패 (${saveErrors.length}/${networkInterfaces.value.length}):\n${errorMessages}`
        } else {
          console.log('All network changes saved successfully')
        }
        
        // 저장 후 머신 목록 다시 로드 (에러가 있어도 성공한 인터페이스는 반영)
        await loadMachines()
        
        // 에러가 없으면 모달 닫기
        if (saveErrors.length === 0) {
          closeNetworkModal()
        }
        
      } catch (err) {
        // 예상치 못한 에러 (전체 프로세스 실패)
        console.error('Error saving network changes:', err)
        networkError.value = err.response?.data?.error || err.message || 'Failed to save network changes'
      } finally {
        savingNetwork.value = false
      }
    }
    
    // WebSocket 메시지 처리 (실시간 업데이트만)
    // ⚠️ 중요: 이 watch()는 useWebSocket()의 lastMessage를 감시함
    //           - useSettings() 등 다른 reactive 객체와 섞이지 않도록 주의
    //           - watch()는 반드시 useWebSocket() 호출 이후에 등록되어야 함
    //           - 이 로직을 수정할 때는 반드시 WebSocket 연결 및 메시지 수신 로직을 함께 확인해야 함
    watch(lastMessage, (newMessage) => {
      if (!newMessage) return
      
      // 디버깅: abort 문제 파악을 위해 모든 메시지 로그
      // console.log('🔔 [WebSocket Debug] 메시지 수신 at', new Date().toLocaleTimeString(), ':', {
      //   type: newMessage.type,
      //   method: newMessage.method,
      //   name: newMessage.name,
      //   action: newMessage.action,
      //   fullMessage: newMessage
      // })
      
      // 재연결 알림 처리
      if (newMessage.type === 'reconnect') {
        // console.log('🔄 [WebSocket] 재연결 감지 - machine 상태 업데이트 재시작')
        return
      }
      
      // pong 메시지는 heartbeat 응답이므로 처리하지 않음
      if (newMessage.method === 'pong') {
        // console.log('💓 [WebSocket] Pong received at', new Date().toLocaleTimeString())
        return
      }
      
      // 모든 메시지 타입 로그 출력
      if (newMessage.type === 2) {
        // console.log('📋 Type 2 메시지 상세:', {
        //   name: newMessage.name,
        //   action: newMessage.action,
        //   hasData: !!newMessage.data,
        //   dataKeys: newMessage.data ? Object.keys(newMessage.data) : []
        // })
        
        // 머신이 아닌 다른 타입의 메시지도 로그 출력 (디버깅용)
        if (newMessage.name !== 'machine' && newMessage.data) {
          // console.log('⚠️ Non-machine message:', {
          //   name: newMessage.name,
          //   action: newMessage.action,
          //   data: newMessage.data
          // })
        }
      }
      
      // 실시간 업데이트만 처리 (type === 2)
      // name이 'machine'인 경우만 처리
      
      // 디버깅: abort 후 메시지 확인을 위해 로그 활성화
      // if (newMessage.type === 2) {
      //   console.log('🔍 [WebSocket Debug] Type 2 message received:', {
      //     type: newMessage.type,
      //     name: newMessage.name,
      //     action: newMessage.action,
      //     hasData: !!newMessage.data,
      //     dataKeys: newMessage.data ? Object.keys(newMessage.data) : []
      //   })
      //   
      //   if (newMessage.data && newMessage.name === 'machine') {
      //     console.log('🔍 [WebSocket Debug] Machine message details:', {
      //       system_id: newMessage.data.system_id,
      //       status: newMessage.data.status,
      //       status_type: typeof newMessage.data.status,
      //       action: newMessage.action
      //     })
      //   }
      // }
      
      if (newMessage.type === 2 && newMessage.data && newMessage.name === 'machine') {
        // console.log('🔍 Processing machine event:', newMessage.name, newMessage.action)
        const machineData = newMessage.data
        // console.log('🔔 Machine update:', newMessage.action, 'for', machineData.system_id)
        
        if (newMessage.action === 'update') {
          const machineIndex = machines.value.findIndex(m => m.id === machineData.system_id)
          // console.log('🔍 [WebSocket Debug] Machine update details:', {
          //   system_id: machineData.system_id,
          //   found_index: machineIndex,
          //   raw_status: machineData.status,
          //   status_type: typeof machineData.status,
          //   status_message: machineData.status_message,
          //   old_status: machineIndex !== -1 ? machines.value[machineIndex].status : 'N/A'
          // })
          
          if (machineIndex !== -1) {
            const oldStatus = machines.value[machineIndex].status
            const newStatus = getStatusName(machineData.status)
            
            // console.log(`✅ [WebSocket Debug] Machine updated: ${machineData.system_id}, Status: ${oldStatus} → ${newStatus}`)
            
            // Ready 상태로 변경될 때 머신 정보를 다시 가져오기 (커미셔닝 후 네트워크 정보가 변경될 수 있음)
            if (newStatus === 'ready' && oldStatus !== 'ready') {
              // console.log(`🔄 [WebSocket Debug] Status changed to Ready, refreshing machine details for: ${machineData.system_id}`)
              refreshMachineDetails(machineData.system_id)
            }
            
            machines.value[machineIndex] = {
              ...machines.value[machineIndex],
              status: newStatus,
              status_message: machineData.status_message,
              power_state: machineData.power_state,
              hostname: machineData.hostname,
              interface_set: machineData.interface_set || machines.value[machineIndex].interface_set || []
            }
            // console.log(`✅ Machine updated: ${machineData.system_id}, Status: ${oldStatus} → ${newStatus}`)
          } else {
            // console.log(`❌ [WebSocket Debug] Machine not found in list: ${machineData.system_id}`)
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
            fabric: machineData.fabric?.name || '-',
            interface_set: machineData.interface_set || []
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
        // Network Modal
        showNetworkModalState,
        selectedMachine,
        networkInterfaces,
        loadingNetwork,
        networkError,
        savingNetwork,
        availableFabrics,
        availableSubnets,
        showNetworkModal,
        closeNetworkModal,
        saveNetworkChanges,
        updateFabricForInterface,
        validatePrimaryIpAddress,
        handlePrimaryIpFocus,
        handleIpAssignmentChange,
        validateSecondaryIpAddress,
        addSecondaryIp,
        removeSecondaryIp,
        getFilteredSubnetsForInterface,
        updateSecondaryIpPrefix,
        getDefaultIpExample,
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

/* Network Modal Styles */
.network-modal-content {
  max-width: 800px;
  max-height: 90vh;
}

.network-modal-body {
  padding: 1.5rem;
  max-height: calc(90vh - 120px);
  overflow-y: auto;
}

.network-interfaces-list {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.network-interface-item {
  border: 2px solid #dee2e6;
  border-radius: 12px;
  padding: 1.5rem;
  background-color: #ffffff;
  margin-bottom: 1.5rem;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  transition: all 0.2s ease;
}

.network-interface-item:hover {
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
  border-color: #007bff;
}

.interface-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
  padding-bottom: 0.75rem;
  border-bottom: 1px solid #dee2e6;
}

.interface-title-section {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.interface-header h4 {
  margin: 0;
  color: #2c3e50;
  font-size: 1.1rem;
  font-weight: 600;
}

.interface-id {
  font-size: 0.75rem;
  color: #6c757d;
  font-weight: 400;
}

.interface-type {
  padding: 0.25rem 0.75rem;
  background-color: #e9ecef;
  color: #495057;
  border-radius: 12px;
  font-size: 0.75rem;
  font-weight: 500;
}

.interface-details {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.current-value {
  display: block;
  margin-top: 0.5rem;
  font-size: 0.8rem;
  color: #6c757d;
  font-style: italic;
}

.ip-addresses-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.ip-address-item {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.ip-subnet {
  font-size: 0.75rem;
  color: #6c757d;
  margin-top: 0.25rem;
}

.no-ip {
  width: 100%;
}

.no-interfaces {
  text-align: center;
  padding: 2rem;
  color: #6c757d;
}

.ip-address-primary {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.ip-address-secondary-item {
  margin-bottom: 1rem;
  padding: 0.75rem;
  background-color: #f8f9fa;
  border-radius: 6px;
  border: 1px solid #e9ecef;
  position: relative;
}

.secondary-ip-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.5rem;
}

.secondary-ip-label {
  font-size: 0.875rem;
  font-weight: 500;
  color: #495057;
  margin: 0;
}

.secondary-ip-input-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.secondary-ip-input-group .form-select {
  width: 100%;
  padding: 0.5rem;
  border: 1px solid #ced4da;
  border-radius: 4px;
  font-size: 0.875rem;
}

.secondary-ip-input-group .form-input {
  width: 100%;
  padding: 0.5rem;
  border: 1px solid #ced4da;
  border-radius: 4px;
  font-size: 0.875rem;
}

.btn-remove-secondary {
  width: 28px;
  height: 28px;
  border: none;
  background-color: #dc3545;
  color: white;
  border-radius: 4px;
  cursor: pointer;
  font-size: 1.2rem;
  font-weight: bold;
  line-height: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  transition: all 0.2s ease;
  flex-shrink: 0;
}

.btn-remove-secondary:hover {
  background-color: #c82333;
  transform: scale(1.05);
}

.btn-add-secondary {
  padding: 0.5rem 1rem;
  border: 1px dashed #007bff;
  background-color: transparent;
  color: #007bff;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.9rem;
  font-weight: 500;
  transition: all 0.2s ease;
}

.btn-add-secondary:hover {
  background-color: #e7f3ff;
  border-color: #0056b3;
  color: #0056b3;
}

.form-input.ip-invalid {
  border-color: #dc3545;
}

.form-input.ip-invalid:focus {
  border-color: #dc3545;
  box-shadow: 0 0 0 3px rgba(220, 53, 69, 0.1);
}

.ip-validation-message {
  display: block;
  color: #dc3545;
  font-size: 0.75rem;
  margin-top: 0.25rem;
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
