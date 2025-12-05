<template>
  <div class="machines">
    <div class="header">
    <h2>Machines</h2>
      
      <!-- Action Bar (shown when machines are selected) -->
      <div v-if="!loading && !error && selectedMachines.length > 0" class="action-bar">
      <div 
        ref="actionsMenuButton"
        class="action-bar-item action-bar-actions-item"
        @click.stop="toggleActionsMenu($event)"
      >
        <span class="action-bar-label">Actions</span>
        <span 
          class="action-bar-dropdown-icon"
          :class="{ 'active': openActionsMenu }"
        >
          {{ openActionsMenu ? '^' : 'v' }}
        </span>
        <Teleport to="body">
          <div 
            v-if="openActionsMenu"
            class="action-bar-dropdown-menu"
            :style="{
              top: ((actionsMenuPosition && actionsMenuPosition.top) || 0) + 'px',
              left: ((actionsMenuPosition && actionsMenuPosition.left) || 0) + 'px'
            }"
            @click.stop
          >
            <div 
              class="action-bar-dropdown-item"
              @click="handleBulkAction('commission')"
              :class="{ 'disabled': !canBulkCommission() }"
            >
              Commission...
            </div>
            <div 
              class="action-bar-dropdown-item"
              @click="handleBulkAction('allocate')"
              :class="{ 'disabled': !canBulkAllocate() }"
            >
              Allocate...
            </div>
            <div 
              class="action-bar-dropdown-item"
              @click="handleBulkAction('deploy')"
              :class="{ 'disabled': !canBulkDeploy() }"
            >
              Deploy...
            </div>
            <div 
              class="action-bar-dropdown-item"
              @click="handleBulkAction('release')"
              :class="{ 'disabled': !canBulkRelease() }"
            >
              Release...
            </div>
            <div 
              class="action-bar-dropdown-item"
              @click="handleBulkAction('abort')"
              :class="{ 'disabled': !canBulkAbort() }"
            >
              Abort...
            </div>
          </div>
        </Teleport>
      </div>
      <div 
        ref="powerActionMenuButton"
        class="action-bar-item action-bar-power-item"
        @click.stop="togglePowerActionMenu($event)"
      >
        <span class="action-bar-label">Power</span>
        <span 
          class="action-bar-dropdown-icon"
          :class="{ 'active': openPowerActionMenu }"
        >
          {{ openPowerActionMenu ? '^' : 'v' }}
        </span>
        <Teleport to="body">
          <div 
            v-if="openPowerActionMenu"
            class="action-bar-dropdown-menu"
            :style="{
              top: ((powerActionMenuPosition && powerActionMenuPosition.top) || 0) + 'px',
              left: ((powerActionMenuPosition && powerActionMenuPosition.left) || 0) + 'px'
            }"
            @click.stop
          >
            <div 
              class="action-bar-dropdown-item"
              :class="{ 'disabled': !canBulkPowerOn() }"
              @click="handleBulkPowerAction('on')"
            >
              Turn on...
            </div>
            <div 
              class="action-bar-dropdown-item"
              :class="{ 'disabled': !canBulkPowerOff() }"
              @click="handleBulkPowerAction('off')"
            >
              Turn off...
            </div>
          </div>
        </Teleport>
      </div>
      <div 
        class="action-bar-item"
        @click.stop="handleBulkDelete()"
      >
        <span class="action-bar-label">Delete</span>
        <span class="action-bar-icon">
          🗑️
        </span>
      </div>
      <div class="action-bar-selected-count">
        {{ selectedMachines.length }} selected
      </div>
      </div>
      
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
              <div class="select-all-container">
              <input type="checkbox" v-model="selectAll" @change="toggleSelectAll">
                <span 
                  class="status-select-dropdown-icon"
                  @click.stop="toggleStatusSelectMenu($event)"
                >
                  ▼
                </span>
              </div>
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
        v-for="machine in paginatedMachines" 
        :key="machine.id"
        :data-machine-id="machine.id"
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
                <strong 
                  @click="showMachineDetails(machine)"
                  class="machine-hostname-clickable"
                >
                  {{ machine.hostname || `Machine ${machine.id}` }}
                </strong>
                <div class="machine-details">
                  <span class="mac-address">{{ getFirstMacAddress(machine) }}</span>
                  <span class="ip-address">{{ getFirstIpAddress(machine) }}</span>
                </div>
              </div>
            </td>
            <td class="power-col">
              <div class="power-container" 
                   @mouseenter="hoveredPowerMachine = machine.id"
                   @mouseleave="hoveredPowerMachine = null">
              <span class="power-status">
                  <span class="power-led" :class="getPowerStateClass(machine.power_state)"></span>
                  {{ formatPowerState(machine.power_state) }}
              </span>
                <span class="power-type">
                  {{ formatPowerType(machine.power_type) }}
                </span>
                <span 
                  v-if="hoveredPowerMachine === machine.id || openPowerMenu === machine.id"
                  class="power-dropdown-icon"
                  @click.stop="togglePowerMenu(machine.id, $event)"
                >
                  ▼
                </span>
              </div>
            </td>
            <td class="status-col">
              <div class="status-container">
                <div class="status-badge-row">
                  <span :class="['status-badge', machine.status]">
                    {{ getStatusText(machine.status) }}
                  </span>
                  <span v-if="machine.status?.toLowerCase() === 'deploying' && machine.deployingOS && machine.deployingRelease" class="status-os-version">
                    {{ getDeployingOSVersion(machine) }}
                  </span>
                </div>
                <div v-if="machine.status_message && (isStatusInProgress(machine.status) || machine.status?.toLowerCase() === 'deployed')" class="status-message">
                  {{ getStatusMessage(machine) }}
                </div>
              </div>
            </td>
            <td class="owner-col">
              <div class="owner-info">
                <span class="owner" v-if="machine.owner">{{ machine.owner }}</span>
                <span class="owner" v-else>-</span>
              <div class="tags" v-if="machine.tags && machine.tags.length > 0">
                <span v-for="tag in machine.tags" :key="tag" class="tag">{{ tag }}</span>
                </div>
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
                      <!-- Failed Deployment, Deployed, 또는 Allocated 상태일 때는 Release 버튼 표시 -->
                       <button 
                        v-if="isFailedDeployment(machine.status) || machine.status?.toLowerCase() === 'deployed' || machine.status?.toLowerCase() === 'allocated'"
                        class="btn-small btn-release"
                        @click="releaseMachine(machine)"
                        :disabled="releasingMachines.includes(machine.id)"
                      >
                        <span v-if="releasingMachines.includes(machine.id)">...</span>
                        <span v-else>Release</span>
                      </button>
                       <!-- 그 외 상태에서는 Commission 버튼 표시 -->
                       <button 
                         v-else
                         class="btn-small"
                         :class="getCommissionButtonClass(machine)"
                         @click="handleCommissionButtonClick(machine)"
                         :disabled="getCommissionButtonDisabled(machine)"
                       >
                         <span v-if="machine.status?.toLowerCase() === 'commissioning'">
                           <span v-if="abortingMachines.includes(machine.id)">...</span>
                           <span v-else>Abort</span>
                         </span>
                         <span v-else>
                           <span v-if="commissioningMachines.includes(machine.id)">...</span>
                           <span v-else>Commission</span>
                         </span>
                       </button>
                       <button 
                         class="btn-small"
                         :class="getNetworkButtonClass(machine)"
                         @click="showNetworkModal(machine)"
                         :disabled="getNetworkButtonDisabled(machine)"
                       >
                         Network
                       </button>
                       <div class="deploy-button-container" 
                            @mouseenter="hoveredDeployMachine = machine.id"
                            @mouseleave="hoveredDeployMachine = null">
                         <button 
                           class="btn-small"
                           :class="getDeployButtonClass(machine)"
                           @click="handleDeployButtonClick(machine, $event)"
                           :disabled="getDeployButtonDisabled(machine)"
                         >
                           <span v-if="machine.status?.toLowerCase() === 'deploying'">
                             <span v-if="abortingDeployMachines.includes(machine.id)">...</span>
                             <span v-else>Abort</span>
                           </span>
                           <span v-else-if="machine.status?.toLowerCase() === 'deployed'">
                             Deployed
                           </span>
                           <span v-else>
                             <span v-if="deployingMachines.includes(machine.id)">...</span>
                             <span v-else>Deploy</span>
                           </span>
                         </button>
                         <span 
                           v-if="(hoveredDeployMachine === machine.id || openDeployMenu === machine.id) && 
                                 machine.status?.toLowerCase() !== 'deploying' && 
                                 machine.status?.toLowerCase() !== 'deployed' &&
                                 (machine.status?.toLowerCase() === 'ready' || machine.status?.toLowerCase() === 'allocated')"
                           class="deploy-dropdown-icon"
                           @click.stop="toggleDeployMenu(machine.id, $event)"
                         >
                           ▼
                         </span>
                       </div>
                     </div>
                   </td>
          </tr>
        </tbody>
      </table>
        </div>
    
    <!-- Confirmation Modal -->
    <Teleport to="body">
      <div v-if="showConfirmModal" class="modal-overlay" @click.self="cancelConfirm">
        <div 
          class="modal-content confirm-modal-content"
          :style="confirmModalPosition.top || confirmModalPosition.left ? { position: 'fixed', top: confirmModalPosition.top + 'px', left: confirmModalPosition.left + 'px', margin: 0 } : {}"
        >
          <div 
            class="modal-header modal-draggable-header"
            @mousedown="startDragConfirmModal"
            :style="isDraggingConfirmModal ? { cursor: 'grabbing' } : { cursor: 'grab' }"
          >
            <h3>{{ confirmModalTitle }}</h3>
          </div>
          <div class="modal-body confirm-modal-body">
            <p class="confirm-message">{{ confirmModalMessage }}</p>
          </div>
          <div class="modal-footer confirm-modal-footer">
            <button class="btn-secondary" @click="cancelConfirm">취소</button>
            <button class="btn-primary" @click="confirmAction">{{ confirmModalButtonText }}</button>
          </div>
        </div>
      </div>
    </Teleport>
    
    <!-- Alert Modal -->
    <Teleport to="body">
      <div v-if="showAlertModal" class="modal-overlay" @click.self="closeAlert">
        <div 
          class="modal-content alert-modal-content"
          :style="confirmModalPosition.top || confirmModalPosition.left ? { position: 'fixed', top: confirmModalPosition.top + 'px', left: confirmModalPosition.left + 'px', margin: 0 } : {}"
        >
          <div 
            class="modal-header modal-draggable-header"
            @mousedown="startDragConfirmModal"
            :style="isDraggingConfirmModal ? { cursor: 'grabbing' } : { cursor: 'grab' }"
          >
            <h3>{{ alertModalTitle }}</h3>
          </div>
          <div class="modal-body alert-modal-body">
            <p class="alert-message">{{ alertModalMessage }}</p>
          </div>
          <div class="modal-footer alert-modal-footer">
            <button class="btn-primary" @click="closeAlert">확인</button>
          </div>
        </div>
      </div>
    </Teleport>
    
    <!-- Status Select Dropdown Menu (Teleport outside of v-for) -->
    <Teleport to="body">
      <div 
        v-if="openStatusSelectMenu && statusSelectMenuPosition && statusSelectMenuPosition.top !== undefined"
        class="status-select-dropdown-menu"
        :style="{
          top: ((statusSelectMenuPosition && statusSelectMenuPosition.top) || 0) + 'px',
          left: ((statusSelectMenuPosition && statusSelectMenuPosition.left) || 0) + 'px'
        }"
        @click.stop
      >
        <div class="status-select-dropdown-header">SELECT BY STATUS:</div>
        <div 
          v-for="status in availableStatusesForSelection" 
          :key="status"
          class="status-select-dropdown-item"
          @click="toggleSelectByStatus(status)"
        >
          <input 
            type="checkbox" 
            :checked="isStatusSelected(status)"
            @mousedown.stop.prevent="toggleSelectByStatus(status)"
            @change.stop.prevent="toggleSelectByStatus(status)"
            tabindex="0"
          >
          <span>{{ getStatusDisplayName(status) }}</span>
        </div>
      </div>
    </Teleport>

    <!-- Power Dropdown Menu (Teleport outside of v-for) -->
    <Teleport to="body">
      <div 
        v-if="openPowerMenu && powerMenuPosition && powerMenuPosition.top !== undefined"
        class="power-dropdown-menu"
        :style="{
          top: ((powerMenuPosition && powerMenuPosition.top) || 0) + 'px',
          left: ((powerMenuPosition && powerMenuPosition.left) || 0) + 'px'
        }"
        @click.stop
      >
        <div class="power-dropdown-header">TAKE ACTION:</div>
        <div 
          v-if="getMachineById(openPowerMenu) && getMachineById(openPowerMenu).power_state !== 'on'"
          class="power-dropdown-item"
          @click="handlePowerAction(getMachineById(openPowerMenu), 'on')"
        >
          <span class="power-icon power-on">●</span>
          <span>Turn on</span>
        </div>
        <div 
          v-if="getMachineById(openPowerMenu) && getMachineById(openPowerMenu).power_state === 'on'"
          class="power-dropdown-item"
          @click="handlePowerAction(getMachineById(openPowerMenu), 'off')"
        >
          <span class="power-icon power-off">●</span>
          <span>Turn off</span>
        </div>
        <div 
          v-if="getMachineById(openPowerMenu)"
          class="power-dropdown-item"
          @click="handleCheckPower(getMachineById(openPowerMenu))"
        >
          <span class="power-icon">🔍</span>
          <span>Check power</span>
        </div>
      </div>
    </Teleport>

    <!-- Deploy Modal -->
    <div v-if="showDeployModalState" class="modal-overlay" @click="closeDeployModal">
      <div 
        class="modal-content deploy-modal-content" 
        :style="(deployModalPosition?.top || deployModalPosition?.left) ? { position: 'fixed', top: (deployModalPosition?.top || 0) + 'px', left: (deployModalPosition?.left || 0) + 'px', margin: 0 } : {}"
        @click.stop
      >
        <div 
          class="modal-header modal-draggable-header"
          @mousedown="startDragDeployModal"
          :style="isDraggingDeployModal ? { cursor: 'grabbing' } : { cursor: 'grab' }"
        >
          <h3>Deploy Machine - {{ selectedDeployMachine?.hostname || selectedDeployMachine?.id }}</h3>
          <button class="close-btn" @click="closeDeployModal">&times;</button>
        </div>
        
        <div class="deploy-modal-body">
          <div v-if="loadingDeployableOS" class="loading">
            <p>Loading OS images...</p>
          </div>
          
          <div v-else-if="deployableOSList.length === 0" class="error">
            <p>No OS images available</p>
          </div>
          
          <div v-else class="deploy-form">
            <!-- OS Selection -->
            <div class="form-section">
              <label class="form-label">Operating System</label>
              <select 
                v-model="selectedDeployOS" 
                class="form-select"
                :disabled="deployingMachine"
              >
                <option :value="null">Select OS...</option>
                <option 
                  v-for="os in deployableOSList"
                  :key="`${os.os}-${os.release}-${os.arches?.join(',') || ''}`"
                  :value="os"
                >
                  {{ formatOSName(os.os, os.release) }} 
                  <span v-if="os.arches && os.arches.length > 0">({{ os.arches.join(', ') }})</span>
                  <span v-if="os.isDefault" class="default-badge"> - Default</span>
                </option>
              </select>
            </div>
            
            <!-- Cloud-Config Template Selection -->
            <div class="form-section">
              <label class="form-label">Cloud-Config Template</label>
              <select 
                v-model="selectedCloudConfigTemplate" 
                class="form-select"
                :disabled="deployingMachine"
              >
                <option value="none">None</option>
                <optgroup v-if="matchedTemplates.length > 0" label="Recommended (matches machine tags)">
                  <option 
                    v-for="template in matchedTemplates"
                    :key="template.id"
                    :value="template.id"
                  >
                    {{ template.name }}<span v-if="template.tags && template.tags.length > 0"> ({{ template.tags.join(', ') }})</span>
                  </option>
                </optgroup>
                <optgroup v-if="otherTemplates.length > 0" label="Other Templates">
                  <option 
                    v-for="template in otherTemplates"
                    :key="template.id"
                    :value="template.id"
                  >
                    {{ template.name }}<span v-if="template.tags && template.tags.length > 0"> ({{ template.tags.join(', ') }})</span>
                  </option>
                </optgroup>
                <option value="custom">Custom...</option>
              </select>
              <p v-if="selectedCloudConfigTemplate !== 'none' && selectedCloudConfigTemplate !== 'custom' && getSelectedTemplateCloudConfig" class="form-hint">
                Template: {{ cloudConfigTemplates.find(t => t.id === selectedCloudConfigTemplate)?.description || 'No description' }}
              </p>
              <p v-else-if="matchedTemplates.length > 0" class="form-hint">
                {{ matchedTemplates.length }} template(s) match this machine's tags. Recommended templates are shown first.
              </p>
              <p v-else class="form-hint">Select a template or choose Custom to enter your own</p>
            </div>
            
            <!-- Template Preview -->
            <div v-if="selectedCloudConfigTemplate !== 'none' && selectedCloudConfigTemplate !== 'custom' && getSelectedTemplateCloudConfig" class="form-section">
              <label class="form-label">Template Preview</label>
              <pre class="template-preview">{{ getSelectedTemplateCloudConfig }}</pre>
            </div>
            
            <!-- Custom Cloud-Config Input -->
            <div v-if="selectedCloudConfigTemplate === 'custom'" class="form-section">
              <label class="form-label">Custom Cloud-Config</label>
              <textarea 
                v-model="customCloudConfig"
                class="form-textarea code-editor"
                rows="10"
                placeholder="#cloud-config&#10;users:&#10;  - name: ubuntu&#10;    ssh-authorized-keys:&#10;      - ssh-rsa ..."
                :disabled="deployingMachine"
              ></textarea>
              <p class="form-hint">Enter cloud-config YAML format (userdata will be added later)</p>
            </div>
          </div>
        </div>
        
        <div class="modal-footer">
          <button 
            class="btn-secondary btn-sm" 
            @click="closeDeployModal"
            :disabled="deployingMachine"
          >
            Cancel
          </button>
          <button 
            class="btn-primary btn-sm" 
            @click="startDeployFromModal"
            :disabled="!selectedDeployOS || deployingMachine"
          >
            <span v-if="deployingMachine">Deploying...</span>
            <span v-else>Deploy</span>
          </button>
        </div>
      </div>
    </div>
        
    <div v-if="!loading && !error && filteredMachines.length === 0" class="no-machines">
      <p>No machines found matching your criteria.</p>
          </div>
    
    <!-- Pagination -->
    <div class="pagination" v-if="!loading && !error && filteredMachines.length > 0">
      <div class="pagination-info">
        Showing {{ (currentPage - 1) * itemsPerPage + 1 }}-{{ Math.min(currentPage * itemsPerPage, filteredMachines.length) }} of {{ filteredMachines.length }} machines ({{ machines.length }} total)
          </div>
      <div class="pagination-controls">
        <button class="btn-small" :disabled="currentPage === 1" @click="currentPage = 1">
          &lt; Page {{ currentPage }} of {{ totalPages }} &gt;
        </button>
        <select v-model="itemsPerPage" class="page-size-select">
          <option value="10">10/page</option>
          <option value="25">25/page</option>
          <option value="50">50/page</option>
          <option value="100">100/page</option>
        </select>
          </div>
          </div>

    <!-- Network Modal -->
    <div v-if="showNetworkModalState" class="modal-overlay" @click="closeNetworkModal">
      <div 
        class="modal-content network-modal-content" 
        :style="networkModalPosition.top || networkModalPosition.left ? { position: 'fixed', top: networkModalPosition.top + 'px', left: networkModalPosition.left + 'px', margin: 0 } : {}"
        @click.stop
      >
        <div 
          class="modal-header modal-draggable-header"
          @mousedown="startDragNetworkModal"
          :style="isDraggingNetworkModal ? { cursor: 'grabbing' } : { cursor: 'grab' }"
        >
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
                    <option :value="-1">Disconnect</option>
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
                
                <!-- IP Assignment는 Fabric이 선택되었을 때만 표시 -->
                <div class="form-group" v-if="networkInterface.editableFabric !== null && networkInterface.editableFabric !== undefined && networkInterface.editableFabric !== '' && networkInterface.editableFabric !== -1">
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
                
                <div class="form-group" v-if="networkInterface.editableFabric !== null && networkInterface.editableFabric !== undefined && networkInterface.editableFabric !== '' && networkInterface.editableFabric !== -1 && networkInterface.ipAssignment === 'static'">
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
                
                <!-- Secondary IP는 Fabric이 선택되었을 때만 표시 -->
                <div class="form-group" v-if="networkInterface.editableFabric !== null && networkInterface.editableFabric !== undefined && networkInterface.editableFabric !== '' && networkInterface.editableFabric !== -1 && networkInterface.secondaryIpAddresses && networkInterface.secondaryIpAddresses.length > 0">
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
                
                <!-- Add Secondary IP 버튼은 Fabric이 선택되었을 때만 표시 -->
                <div class="form-group" v-if="networkInterface.editableFabric !== null && networkInterface.editableFabric !== undefined && networkInterface.editableFabric !== '' && networkInterface.editableFabric !== -1">
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
            <button type="button" class="btn-primary" @click="saveNetworkChanges" :disabled="savingNetwork || !canSaveNetworkChanges(selectedMachine)">
              <span v-if="savingNetwork">Saving...</span>
              <span v-else>Save Changes</span>
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Machine Details Modal -->
    <div v-if="showMachineDetailsModal" class="modal-overlay" @click="closeMachineDetailsModal">
      <div 
        class="modal-content machine-details-modal-content" 
        :style="modalPosition.top || modalPosition.left ? { position: 'fixed', top: modalPosition.top + 'px', left: modalPosition.left + 'px', margin: 0 } : {}"
        @click.stop
      >
        <div 
          class="modal-header modal-draggable-header"
          @mousedown="startDragModal"
          :style="isDraggingModal ? { cursor: 'grabbing' } : { cursor: 'grab' }"
        >
          <h3>Machine Details: {{ selectedMachineForDetails?.hostname || selectedMachineForDetails?.id }}</h3>
          <button class="close-btn" @click="closeMachineDetailsModal">&times;</button>
        </div>
        
        <div class="machine-details-modal-body">
          <div class="machine-details-content">
            <!-- Tabs -->
            <div class="details-tabs">
              <button 
                class="details-tab"
                :class="{ active: activeDetailsTab === 'overview' }"
                @click="activeDetailsTab = 'overview'"
              >
                Overview
              </button>
              <button 
                class="details-tab"
                :class="{ active: activeDetailsTab === 'power' }"
                @click="activeDetailsTab = 'power'"
              >
                Power
              </button>
              <button 
                class="details-tab"
                :class="{ active: activeDetailsTab === 'hardware' }"
                @click="activeDetailsTab = 'hardware'"
              >
                Hardware
              </button>
              <button 
                class="details-tab"
                :class="{ active: activeDetailsTab === 'network' }"
                @click="activeDetailsTab = 'network'"
              >
                Network
              </button>
              <button 
                class="details-tab"
                :class="{ active: activeDetailsTab === 'os' }"
                @click="activeDetailsTab = 'os'"
              >
                Operating System
              </button>
              <button 
                class="details-tab"
                :class="{ active: activeDetailsTab === 'events' }"
                @click="activeDetailsTab = 'events'"
              >
                Events
              </button>
            </div>
            
            <!-- Tab Content -->
            <div class="details-tab-content">
              <!-- Loading State -->
              <div v-if="loadingMachineDetails" class="details-section">
                <div class="loading">
                  <p>Loading machine details...</p>
                </div>
              </div>
              
              <!-- Error State -->
              <div v-else-if="machineDetailsError" class="details-section">
                <div class="error">
                  <p>{{ machineDetailsError }}</p>
                </div>
              </div>
              
              <!-- Content when loaded -->
              <template v-else-if="machineDetails">
              <!-- Overview Tab -->
              <div v-if="activeDetailsTab === 'overview'" class="details-section">
                <div class="details-info-grid">
                  <div class="info-item">
                    <label>Status</label>
                    <div>
                      <span :class="['status-badge', machineDetails.status_name?.toLowerCase() || machineDetails.status]">
                        {{ getStatusText(machineDetails.status_name || machineDetails.status) }}
                      </span>
                      <div v-if="machineDetails.status_message" class="status-message-detail">
                        {{ getStatusMessage({ status: machineDetails.status_name || machineDetails.status, osystem: machineDetails.osystem, distro_series: machineDetails.distro_series, status_message: machineDetails.status_message }) }}
                      </div>
                    </div>
                  </div>
                  
                  <div class="info-item">
                    <label>System ID</label>
                    <div>{{ machineDetails.system_id || machineDetails.id || '-' }}</div>
                  </div>
                  
                  <div class="info-item">
                    <label>Hostname</label>
                    <div>{{ machineDetails.hostname || '-' }}</div>
                  </div>
                  
                  <div class="info-item">
                    <label>Owner</label>
                    <div>{{ machineDetails.owner || '-' }}</div>
                  </div>
                  
                  <div class="info-item">
                    <label>Tags</label>
                    <div>
                      <span v-if="machineDetails.tag_names && machineDetails.tag_names.length > 0">
                        <span v-for="tag in machineDetails.tag_names" :key="tag" class="tag">{{ tag }}</span>
                      </span>
                      <span v-else>-</span>
                    </div>
                  </div>
                  
                  <div class="info-item">
                    <label>Pool</label>
                    <div>{{ machineDetails.pool?.name || 'default' }}</div>
                  </div>
                  
                  <div class="info-item">
                    <label>Zone</label>
                    <div>{{ machineDetails.zone?.name || 'default' }}</div>
                  </div>
                  
                  <div class="info-item" v-if="machineDetails.description">
                    <label>Description</label>
                    <div>{{ machineDetails.description }}</div>
                  </div>
                  
                  <div class="info-item">
                    <label>Power Type</label>
                    <div>{{ formatPowerType(machineDetails.power_type) }}</div>
                  </div>
                </div>
              </div>
              
              <!-- Power Tab -->
              <div v-if="activeDetailsTab === 'power'" class="details-section details-section-power">
                <div v-if="loadingPowerParameters" class="loading">
                  <p>Loading power parameters...</p>
                </div>
                <div v-else-if="powerParametersError" class="error">
                  <p>{{ powerParametersError }}</p>
                </div>
                <div v-else-if="machineDetails.power_type === 'ipmi' && powerParameters">
                  <!-- Read Mode -->
                  <div v-if="!isEditingPowerParameters" class="details-info-grid">
                  <div class="info-item">
                    <label>Power Type</label>
                    <div>{{ formatPowerType(machineDetails.power_type) }}</div>
                  </div>
                  
                  <div class="info-item">
                    <label>Power State</label>
                    <div>
                      <span class="power-led" :class="getPowerStateClass(machineDetails.power_state)"></span>
                      {{ formatPowerState(machineDetails.power_state) }}
                    </div>
                  </div>
                  
                  <div v-if="powerParameters.power_driver" class="info-item">
                    <label>Power Driver</label>
                    <div>{{ formatPowerDriver(powerParameters.power_driver) }}</div>
                  </div>
                  
                  <div v-if="powerParameters.power_boot_type" class="info-item">
                    <label>Power Boot Type</label>
                    <div>{{ formatPowerBootType(powerParameters.power_boot_type) }}</div>
                  </div>
                  
                  <div v-if="powerParameters.power_address" class="info-item">
                    <label>IP Address</label>
                    <div>{{ powerParameters.power_address }}</div>
                  </div>
                  
                  <div v-if="powerParameters.power_user" class="info-item">
                    <label>Power User</label>
                    <div>{{ powerParameters.power_user }}</div>
                  </div>
                  
                  <div v-if="powerParameters.power_pass" class="info-item">
                    <label>Power Password</label>
                    <div>••••••••</div>
                  </div>
                  
                  <div class="info-item">
                    <label>K_g BMC key</label>
                    <div>{{ powerParameters.k_g || '-' }}</div>
                  </div>
                  
                  <div v-if="powerParameters.cipher_suite_id !== undefined && powerParameters.cipher_suite_id !== null" class="info-item">
                    <label>Cipher Suite ID</label>
                    <div>{{ formatCipherSuiteId(powerParameters.cipher_suite_id) }}</div>
                  </div>
                  
                  <div v-if="powerParameters.privilege_level" class="info-item">
                    <label>Privilege Level</label>
                    <div>{{ formatPrivilegeLevel(powerParameters.privilege_level) }}</div>
                  </div>
                  
                  <div v-if="powerParameters.workaround_flags" class="info-item">
                    <label>Workaround Flags</label>
                    <div>{{ formatWorkaroundFlags(powerParameters.workaround_flags) }}</div>
                  </div>
                  
                  <div class="info-item">
                    <label>Power MAC</label>
                    <div>{{ powerParameters.mac_address || '-' }}</div>
                  </div>
                  </div>
                  
                  <!-- Edit Button (Read Mode) - Always at bottom -->
                  <div v-if="!isEditingPowerParameters" class="power-edit-actions">
                    <button class="btn-primary btn-sm" @click="startEditingPowerParameters">
                      Edit
                    </button>
                  </div>
                  
                  <!-- Edit Mode -->
                  <div v-else class="power-edit-form">
                    <!-- Power Type (Always visible at top) -->
                    <div class="form-group">
                      <label for="editPowerType">Power Type</label>
                      <select id="editPowerType" v-model="editingPowerParameters.powerType" class="form-select">
                        <option value="manual">Manual</option>
                        <option value="ipmi">IPMI</option>
                      </select>
                    </div>
                    
                    <!-- IPMI Configuration (Only when IPMI is selected) -->
                    <template v-if="editingPowerParameters.powerType === 'ipmi'">
                      <div class="ipmi-fields-group">
                        <div class="ipmi-fields-header">
                          <label>IPMI Configuration</label>
                        </div>
                        <div class="ipmi-fields-content ipmi-edit-grid">
                          <div class="form-group">
                            <label for="editPowerDriver">Power Driver</label>
                            <select id="editPowerDriver" v-model="editingPowerParameters.powerDriver" class="form-select">
                              <option value="LAN">LAN [IPMI 1.5]</option>
                              <option value="LAN_2_0">LAN_2_0 [IPMI 2.0]</option>
                            </select>
                          </div>

                          <div class="form-group">
                            <label for="editPowerBootType">Power Boot Type</label>
                            <select id="editPowerBootType" v-model="editingPowerParameters.powerBootType" class="form-select">
                              <option value="auto">Automatic</option>
                              <option value="legacy">Legacy boot</option>
                              <option value="efi">EFI boot</option>
                            </select>
                          </div>

                          <div class="form-group">
                            <label for="editPowerIpAddress">IP Address</label>
                            <input
                              type="text"
                              id="editPowerIpAddress"
                              v-model="editingPowerParameters.powerIpAddress"
                              placeholder="e.g., 192.168.1.100"
                              class="form-input"
                            >
                          </div>

                          <div class="form-group">
                            <label for="editPowerUser">Power User</label>
                            <input
                              type="text"
                              id="editPowerUser"
                              v-model="editingPowerParameters.powerUser"
                              placeholder="IPMI username"
                              class="form-input"
                            >
                          </div>

                          <div class="form-group">
                            <label for="editPowerPassword">Power Password</label>
                            <input
                              type="password"
                              id="editPowerPassword"
                              v-model="editingPowerParameters.powerPassword"
                              placeholder="IPMI password (leave blank to keep current)"
                              class="form-input"
                            >
                          </div>

                          <div class="form-group">
                            <label for="editPowerKgBmcKey">K_g BMC key</label>
                            <input
                              type="text"
                              id="editPowerKgBmcKey"
                              v-model="editingPowerParameters.powerKgBmcKey"
                              placeholder="K_g BMC key"
                              class="form-input"
                            >
                          </div>

                          <div class="form-group">
                            <label for="editCipherSuiteId">Cipher Suite ID</label>
                            <select id="editCipherSuiteId" v-model="editingPowerParameters.cipherSuiteId" class="form-select">
                              <option value="17">17 - HMAC-SHA256::HMAC_SHA256_128::AES-CBC-128</option>
                              <option value="3">3 - HMAC-SHA1::HMAC-SHA1-96::AES-CBC-128</option>
                              <option value="">freeipmi-tools default</option>
                              <option value="8">8 - HMAC-MD5::HMAC-MD5-128::AES-CBC-128</option>
                              <option value="12">12 - HMAC-MD5::MD5-128::AES-CBC-128</option>
                            </select>
                          </div>

                          <div class="form-group">
                            <label for="editPrivilegeLevel">Privilege Level</label>
                            <select id="editPrivilegeLevel" v-model="editingPowerParameters.privilegeLevel" class="form-select">
                              <option value="USER">User</option>
                              <option value="OPERATOR">Operator</option>
                              <option value="ADMIN">Administrator</option>
                            </select>
                          </div>

                          <div class="form-group">
                            <label for="editPowerMac">Power MAC</label>
                            <input
                              type="text"
                              id="editPowerMac"
                              v-model="editingPowerParameters.powerMac"
                              placeholder="e.g., 08:00:27:11:34:26"
                              class="form-input"
                            >
                          </div>

                          <div class="form-group form-group-full-width">
                            <label>Workaround Flags</label>
                            <div class="checkbox-group">
                              <label class="checkbox-label">
                                <input
                                  type="checkbox"
                                  value="opensesspriv"
                                  v-model="editingPowerParameters.workaroundFlags"
                                  class="form-checkbox"
                                >
                                Opensesspriv
                              </label>
                              <label class="checkbox-label">
                                <input
                                  type="checkbox"
                                  value="authcap"
                                  v-model="editingPowerParameters.workaroundFlags"
                                  class="form-checkbox"
                                >
                                Authcap
                              </label>
                              <label class="checkbox-label">
                                <input
                                  type="checkbox"
                                  value="idzero"
                                  v-model="editingPowerParameters.workaroundFlags"
                                  class="form-checkbox"
                                >
                                Idzero
                              </label>
                              <label class="checkbox-label">
                                <input
                                  type="checkbox"
                                  value="unexpectedauth"
                                  v-model="editingPowerParameters.workaroundFlags"
                                  class="form-checkbox"
                                >
                                Unexpectedauth
                              </label>
                              <label class="checkbox-label">
                                <input
                                  type="checkbox"
                                  value="forcepermsg"
                                  v-model="editingPowerParameters.workaroundFlags"
                                  class="form-checkbox"
                                >
                                Forcepermsg
                              </label>
                              <label class="checkbox-label">
                                <input
                                  type="checkbox"
                                  value="endianseq"
                                  v-model="editingPowerParameters.workaroundFlags"
                                  class="form-checkbox"
                                >
                                Endianseq
                              </label>
                              <label class="checkbox-label">
                                <input
                                  type="checkbox"
                                  value="intel20"
                                  v-model="editingPowerParameters.workaroundFlags"
                                  class="form-checkbox"
                                >
                                Intel20
                              </label>
                              <label class="checkbox-label">
                                <input
                                  type="checkbox"
                                  value="supermicro20"
                                  v-model="editingPowerParameters.workaroundFlags"
                                  class="form-checkbox"
                                >
                                Supermicro20
                              </label>
                              <label class="checkbox-label">
                                <input
                                  type="checkbox"
                                  value="sun20"
                                  v-model="editingPowerParameters.workaroundFlags"
                                  class="form-checkbox"
                                >
                                Sun20
                              </label>
                              <label class="checkbox-label">
                                <input
                                  type="checkbox"
                                  value="nochecksumcheck"
                                  v-model="editingPowerParameters.workaroundFlags"
                                  class="form-checkbox"
                                >
                                Nochecksumcheck
                              </label>
                              <label class="checkbox-label">
                                <input
                                  type="checkbox"
                                  value="integritycheckvalue"
                                  v-model="editingPowerParameters.workaroundFlags"
                                  class="form-checkbox"
                                >
                                Integritycheckvalue
                              </label>
                              <label class="checkbox-label">
                                <input
                                  type="checkbox"
                                  value="ipmiping"
                                  v-model="editingPowerParameters.workaroundFlags"
                                  class="form-checkbox"
                                >
                                Ipmiping
                              </label>
                              <label class="checkbox-label">
                                <input
                                  type="checkbox"
                                  value=""
                                  v-model="editingPowerParameters.workaroundFlags"
                                  class="form-checkbox"
                                >
                                None
                              </label>
                            </div>
                          </div>
                        </div>
                      </div>
                    </template>
                    
                    <div class="power-edit-actions">
                      <button class="btn-secondary btn-sm" @click="cancelEditingPowerParameters">
                        Cancel
                      </button>
                      <button class="btn-primary btn-sm" @click="savePowerParameters" :disabled="savingPowerParameters">
                        <span v-if="savingPowerParameters">Saving...</span>
                        <span v-else>Save</span>
                      </button>
                    </div>
                  </div>
                </div>
                <div v-else-if="machineDetails.power_type !== 'ipmi'">
                  <!-- Read Mode -->
                  <div v-if="!isEditingPowerParameters" class="details-info-grid">
                    <div class="info-item">
                      <label>Power Type</label>
                      <div>{{ formatPowerType(machineDetails.power_type) }}</div>
                    </div>
                    
                    <div class="info-item">
                      <label>Power State</label>
                      <div>
                        <span class="power-led" :class="getPowerStateClass(machineDetails.power_state)"></span>
                        {{ formatPowerState(machineDetails.power_state) }}
                      </div>
                    </div>
                  </div>
                  
                  <!-- Edit Button (Read Mode) -->
                  <div v-if="!isEditingPowerParameters" class="power-edit-actions">
                    <button class="btn-primary btn-sm" @click="startEditingPowerParameters">
                      Edit
                    </button>
                  </div>
                  
                  <!-- Edit Mode -->
                  <div v-else class="power-edit-form">
                    <!-- Power Type (Always visible at top) -->
                    <div class="form-group">
                      <label for="editPowerTypeManual">Power Type</label>
                      <select id="editPowerTypeManual" v-model="editingPowerParameters.powerType" class="form-select">
                        <option value="manual">Manual</option>
                        <option value="ipmi">IPMI</option>
                      </select>
                    </div>
                    
                    <!-- IPMI Configuration (Only when IPMI is selected) -->
                    <template v-if="editingPowerParameters.powerType === 'ipmi'">
                      <div class="ipmi-fields-group">
                        <div class="ipmi-fields-header">
                          <label>IPMI Configuration</label>
                        </div>
                        <div class="ipmi-fields-content ipmi-edit-grid">
                          <div class="form-group">
                            <label for="editPowerDriverManual">Power Driver</label>
                            <select id="editPowerDriverManual" v-model="editingPowerParameters.powerDriver" class="form-select">
                              <option value="LAN">LAN [IPMI 1.5]</option>
                              <option value="LAN_2_0">LAN_2_0 [IPMI 2.0]</option>
                            </select>
                          </div>

                          <div class="form-group">
                            <label for="editPowerBootTypeManual">Power Boot Type</label>
                            <select id="editPowerBootTypeManual" v-model="editingPowerParameters.powerBootType" class="form-select">
                              <option value="auto">Automatic</option>
                              <option value="legacy">Legacy boot</option>
                              <option value="efi">EFI boot</option>
                            </select>
                          </div>

                          <div class="form-group">
                            <label for="editPowerIpAddressManual">IP Address</label>
                            <input
                              type="text"
                              id="editPowerIpAddressManual"
                              v-model="editingPowerParameters.powerIpAddress"
                              placeholder="e.g., 192.168.1.100"
                              class="form-input"
                            >
                          </div>

                          <div class="form-group">
                            <label for="editPowerUserManual">Power User</label>
                            <input
                              type="text"
                              id="editPowerUserManual"
                              v-model="editingPowerParameters.powerUser"
                              placeholder="IPMI username"
                              class="form-input"
                            >
                          </div>

                          <div class="form-group">
                            <label for="editPowerPasswordManual">Power Password</label>
                            <input
                              type="password"
                              id="editPowerPasswordManual"
                              v-model="editingPowerParameters.powerPassword"
                              placeholder="IPMI password (leave blank to keep current)"
                              class="form-input"
                            >
                          </div>

                          <div class="form-group">
                            <label for="editPowerKgBmcKeyManual">K_g BMC key</label>
                            <input
                              type="text"
                              id="editPowerKgBmcKeyManual"
                              v-model="editingPowerParameters.powerKgBmcKey"
                              placeholder="K_g BMC key"
                              class="form-input"
                            >
                          </div>

                          <div class="form-group">
                            <label for="editCipherSuiteIdManual">Cipher Suite ID</label>
                            <select id="editCipherSuiteIdManual" v-model="editingPowerParameters.cipherSuiteId" class="form-select">
                              <option value="17">17 - HMAC-SHA256::HMAC_SHA256_128::AES-CBC-128</option>
                              <option value="3">3 - HMAC-SHA1::HMAC-SHA1-96::AES-CBC-128</option>
                              <option value="">freeipmi-tools default</option>
                              <option value="8">8 - HMAC-MD5::HMAC-MD5-128::AES-CBC-128</option>
                              <option value="12">12 - HMAC-MD5::MD5-128::AES-CBC-128</option>
                            </select>
                          </div>

                          <div class="form-group">
                            <label for="editPrivilegeLevelManual">Privilege Level</label>
                            <select id="editPrivilegeLevelManual" v-model="editingPowerParameters.privilegeLevel" class="form-select">
                              <option value="USER">User</option>
                              <option value="OPERATOR">Operator</option>
                              <option value="ADMIN">Administrator</option>
                            </select>
                          </div>

                          <div class="form-group">
                            <label for="editPowerMacManual">Power MAC</label>
                            <input
                              type="text"
                              id="editPowerMacManual"
                              v-model="editingPowerParameters.powerMac"
                              placeholder="e.g., 08:00:27:11:34:26"
                              class="form-input"
                            >
                          </div>

                          <div class="form-group form-group-full-width">
                            <label>Workaround Flags</label>
                            <div class="checkbox-group">
                              <label class="checkbox-label">
                                <input
                                  type="checkbox"
                                  value="opensesspriv"
                                  v-model="editingPowerParameters.workaroundFlags"
                                  class="form-checkbox"
                                >
                                Opensesspriv
                              </label>
                              <label class="checkbox-label">
                                <input
                                  type="checkbox"
                                  value="authcap"
                                  v-model="editingPowerParameters.workaroundFlags"
                                  class="form-checkbox"
                                >
                                Authcap
                              </label>
                              <label class="checkbox-label">
                                <input
                                  type="checkbox"
                                  value="idzero"
                                  v-model="editingPowerParameters.workaroundFlags"
                                  class="form-checkbox"
                                >
                                Idzero
                              </label>
                              <label class="checkbox-label">
                                <input
                                  type="checkbox"
                                  value="unexpectedauth"
                                  v-model="editingPowerParameters.workaroundFlags"
                                  class="form-checkbox"
                                >
                                Unexpectedauth
                              </label>
                              <label class="checkbox-label">
                                <input
                                  type="checkbox"
                                  value="forcepermsg"
                                  v-model="editingPowerParameters.workaroundFlags"
                                  class="form-checkbox"
                                >
                                Forcepermsg
                              </label>
                              <label class="checkbox-label">
                                <input
                                  type="checkbox"
                                  value="endianseq"
                                  v-model="editingPowerParameters.workaroundFlags"
                                  class="form-checkbox"
                                >
                                Endianseq
                              </label>
                              <label class="checkbox-label">
                                <input
                                  type="checkbox"
                                  value="intel20"
                                  v-model="editingPowerParameters.workaroundFlags"
                                  class="form-checkbox"
                                >
                                Intel20
                              </label>
                              <label class="checkbox-label">
                                <input
                                  type="checkbox"
                                  value="supermicro20"
                                  v-model="editingPowerParameters.workaroundFlags"
                                  class="form-checkbox"
                                >
                                Supermicro20
                              </label>
                              <label class="checkbox-label">
                                <input
                                  type="checkbox"
                                  value="sun20"
                                  v-model="editingPowerParameters.workaroundFlags"
                                  class="form-checkbox"
                                >
                                Sun20
                              </label>
                              <label class="checkbox-label">
                                <input
                                  type="checkbox"
                                  value="nochecksumcheck"
                                  v-model="editingPowerParameters.workaroundFlags"
                                  class="form-checkbox"
                                >
                                Nochecksumcheck
                              </label>
                              <label class="checkbox-label">
                                <input
                                  type="checkbox"
                                  value="integritycheckvalue"
                                  v-model="editingPowerParameters.workaroundFlags"
                                  class="form-checkbox"
                                >
                                Integritycheckvalue
                              </label>
                              <label class="checkbox-label">
                                <input
                                  type="checkbox"
                                  value="ipmiping"
                                  v-model="editingPowerParameters.workaroundFlags"
                                  class="form-checkbox"
                                >
                                Ipmiping
                              </label>
                              <label class="checkbox-label">
                                <input
                                  type="checkbox"
                                  value=""
                                  v-model="editingPowerParameters.workaroundFlags"
                                  class="form-checkbox"
                                >
                                None
                              </label>
                            </div>
                          </div>
                        </div>
                      </div>
                    </template>
                    
                    <div class="power-edit-actions">
                      <button class="btn-secondary btn-sm" @click="cancelEditingPowerParameters">
                        Cancel
                      </button>
                      <button class="btn-primary btn-sm" @click="savePowerParameters" :disabled="savingPowerParameters">
                        <span v-if="savingPowerParameters">Saving...</span>
                        <span v-else>Save</span>
                      </button>
                    </div>
                  </div>
                </div>
                <div v-else class="no-data">
                  <p>No power parameters found.</p>
                </div>
              </div>
              
              <!-- Hardware Tab -->
              <div v-if="activeDetailsTab === 'hardware'" class="details-section">
                <div class="details-info-grid">
                  <div class="info-item">
                    <label>CPU Architecture</label>
                    <div>{{ machineDetails.architecture || '-' }}</div>
                  </div>
                  
                  <div class="info-item">
                    <label>CPU Cores</label>
                    <div>{{ machineDetails.cpu_count || 0 }}</div>
                  </div>
                  
                  <div class="info-item">
                    <label>Memory</label>
                    <div>{{ 
                      selectedMachineForDetails?.memory !== undefined 
                        ? formatMemory(selectedMachineForDetails.memory) 
                        : formatMemory(machineDetails?.memory || 0)
                    }}</div>
                  </div>
                  
                  <div class="info-item">
                    <label>Total Storage</label>
                    <div>{{ 
                      selectedMachineForDetails?.storage 
                        ? formatStorage(selectedMachineForDetails.storage) 
                        : formatStorage(calculateStorageFromBlockDevices(machineBlockDevices.length > 0 ? machineBlockDevices : (machineDetails?.blockdevice_set || [])))
                    }}</div>
                  </div>
                  
                  <div class="info-item">
                    <label>Disk Count</label>
                    <div>{{ 
                      selectedMachineForDetails?.disk_count !== undefined 
                        ? selectedMachineForDetails.disk_count 
                        : (machineBlockDevices.length > 0 ? machineBlockDevices.length : (machineDetails?.blockdevice_set?.length || 0))
                    }}</div>
                  </div>
                </div>
                
                <!-- Block Devices -->
                <div v-if="loadingBlockDevices" class="loading">
                  <p>Loading block devices...</p>
                </div>
                <div v-else-if="machineBlockDevices.length > 0" class="block-devices-section">
                  <h4>Block Devices</h4>
                  <table class="block-devices-table">
                    <thead>
                      <tr>
                        <th>Name</th>
                        <th>Type</th>
                        <th>Size</th>
                        <th>Model</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="device in machineBlockDevices" :key="device.id">
                        <td>{{ device.name || '-' }}</td>
                        <td>{{ device.type || '-' }}</td>
                        <td>{{ formatStorage(device.size || 0) }}</td>
                        <td>{{ device.model || '-' }}</td>
                      </tr>
                    </tbody>
                  </table>
                </div>
                <div v-else class="no-data">
                  <p>No block devices found.</p>
                </div>
              </div>
              
              <!-- Network Tab -->
              <div v-if="activeDetailsTab === 'network'" class="details-section details-section-network">
                <div v-if="machineDetails.interface_set && machineDetails.interface_set.length > 0" class="network-interfaces-detail">
                  <div 
                    v-for="(iface, index) in machineDetails.interface_set" 
                    :key="iface.id || index"
                    class="interface-detail-item"
                  >
                    <div class="interface-detail-header">
                      <h4>{{ iface.name || `Interface ${index + 1}` }}</h4>
                      <span class="interface-type">{{ iface.type || 'Unknown' }}</span>
                    </div>
                    
                    <div class="interface-detail-info">
                      <div class="info-row">
                        <label>Interface ID:</label>
                        <span>{{ iface.id || '-' }}</span>
                      </div>
                      
                      <div class="info-row">
                        <label>MAC Address:</label>
                        <span>{{ iface.mac_address || '-' }}</span>
                      </div>
                      
                      <div class="info-row" v-if="iface.vlan">
                        <label>Fabric:</label>
                        <span>{{ iface.vlan.fabric || '-' }}</span>
                      </div>
                      
                      <div class="info-row" v-if="iface.vlan">
                        <label>VLAN:</label>
                        <span>{{ iface.vlan.name || iface.vlan.vid || '-' }}</span>
                      </div>
                      
                      <template v-if="iface.links && iface.links.length > 0">
                        <div v-for="(link, linkIndex) in iface.links" :key="linkIndex">
                          <div class="info-row">
                            <label>IP Address:</label>
                            <span v-if="link.ip_address">{{ link.ip_address }}</span>
                            <span v-else class="auto-ip">(AUTO)</span>
                          </div>
                          <div v-if="link.subnet" class="info-row">
                            <label>Subnet:</label>
                            <span>{{ link.subnet.cidr || link.subnet }}</span>
                          </div>
                        </div>
                      </template>
                      
                      <div class="info-row" v-if="iface.link_speed">
                        <label>Link Speed:</label>
                        <span>{{ iface.link_speed }} Mbps</span>
                      </div>
                    </div>
                  </div>
                </div>
                <div v-else class="no-data">
                  <p>No network interfaces found.</p>
                </div>
              </div>
              
              <!-- OS Tab -->
              <div v-if="activeDetailsTab === 'os'" class="details-section">
                <div class="details-info-grid">
                  <div class="info-item" v-if="machineDetails.osystem">
                    <label>Operating System</label>
                    <div>{{ machineDetails.osystem }}</div>
                  </div>
                  
                  <div class="info-item" v-if="machineDetails.distro_series">
                    <label>Distro Series</label>
                    <div>{{ machineDetails.distro_series }}</div>
                  </div>
                  
                  <div class="info-item" v-if="machineDetails.osystem === 'ubuntu' && machineDetails.distro_series">
                    <label>OS Version</label>
                    <div>{{ getUbuntuVersionFromDistroSeries(machineDetails.distro_series) ? `Ubuntu ${getUbuntuVersionFromDistroSeries(machineDetails.distro_series)}` : '-' }}</div>
                  </div>
                  
                  <div class="info-item" v-if="machineDetails.hwe_kernel">
                    <label>HWE Kernel</label>
                    <div>{{ machineDetails.hwe_kernel }}</div>
                  </div>
                  
                  <div class="info-item" v-if="machineDetails.status_name === 'Deployed' || machineDetails.status === 6">
                    <label>Deployment Status</label>
                    <div>Deployed</div>
                  </div>
                </div>
              </div>
              
              <!-- Events Tab -->
              <div v-if="activeDetailsTab === 'events'" class="details-section details-section-events">
                <div class="events-section">
                  <div v-if="loadingEvents" class="loading">
                    <p>Loading events...</p>
                  </div>
                  
                  <div v-else class="events-list">
                    <!-- 현재 상태 정보를 이벤트처럼 표시 (이벤트가 없거나 최상단에 표시) -->
                    <div v-if="machineDetails && (machineDetails.status_message || machineDetails.status_name || machineDetails.status)" class="event-item event-item-current-status">
                      <div class="event-header">
                        <div class="event-time">{{ new Date().toLocaleString() }}</div>
                        <div class="event-level event-level-info">CURRENT</div>
                      </div>
                      <div class="event-content">
                        <div class="event-type">{{ getStatusText(machineDetails.status_name || machineDetails.status) }}</div>
                        <div class="event-description">{{ getStatusMessage({ status: machineDetails.status_name || machineDetails.status, osystem: machineDetails.osystem, distro_series: machineDetails.distro_series, status_message: machineDetails.status_message }) }}</div>
                      </div>
                    </div>
                    
                    <!-- MAAS 이벤트 목록 -->
                    <div v-for="event in machineEvents" :key="event.id" class="event-item">
                      <div class="event-header">
                        <div class="event-time">{{ event.created || '-' }}</div>
                        <div class="event-level" :class="'event-level-' + (event.level?.toLowerCase() || 'info')">
                          {{ event.level || 'INFO' }}
                        </div>
                      </div>
                      <div class="event-content">
                        <div class="event-type">{{ event.type || '-' }}</div>
                        <div v-if="event.description" class="event-description">{{ event.description }}</div>
                        <div v-if="event.username" class="event-username">User: {{ event.username }}</div>
                      </div>
                    </div>
                    
                    <!-- 이벤트가 없을 때 메시지 -->
                    <div v-if="machineEvents.length === 0 && (!machineDetails || !machineDetails.status_message)" class="events-info">
                      <p>No events found for this machine.</p>
                    </div>
                  </div>
                </div>
              </div>
              </template>
            </div>
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
              <option value="ipmi">ipmi</option>
            </select>
          </div>

          <!-- IPMI-specific fields -->
          <template v-if="newMachine.powerType === 'ipmi'">
            <div class="ipmi-fields-group">
              <div class="ipmi-fields-header">
                <label>IPMI Configuration</label>
              </div>
              <div class="ipmi-fields-content">
            <div class="form-group">
              <label for="powerDriver">Power Driver</label>
              <select id="powerDriver" v-model="newMachine.powerDriver" class="form-select">
                <option value="LAN">LAN [IPMI 1.5]</option>
                <option value="LAN_2_0">LAN_2_0 [IPMI 2.0]</option>
              </select>
            </div>

            <div class="form-group">
              <label for="powerBootType">Power Boot Type</label>
              <select id="powerBootType" v-model="newMachine.powerBootType" class="form-select">
                <option value="auto">Automatic</option>
                <option value="legacy">Legacy boot</option>
                <option value="efi">EFI boot</option>
              </select>
            </div>

            <div class="form-group">
              <label for="powerIpAddress">IP Address</label>
              <input
                type="text"
                id="powerIpAddress"
                v-model="newMachine.powerIpAddress"
                placeholder="e.g., 192.168.1.100"
                class="form-input"
              >
            </div>

            <div class="form-group">
              <label for="powerUser">Power User</label>
              <input
                type="text"
                id="powerUser"
                v-model="newMachine.powerUser"
                placeholder="IPMI username"
                class="form-input"
              >
            </div>

            <div class="form-group">
              <label for="powerPassword">Power Password</label>
              <input
                type="password"
                id="powerPassword"
                v-model="newMachine.powerPassword"
                placeholder="IPMI password"
                class="form-input"
              >
            </div>

            <div class="form-group">
              <label for="powerKgBmcKey">K_g BMC key</label>
              <input
                type="text"
                id="powerKgBmcKey"
                v-model="newMachine.powerKgBmcKey"
                placeholder="K_g BMC key"
                class="form-input"
              >
            </div>

            <div class="form-group">
              <label for="cipherSuiteId">Cipher Suite ID</label>
              <select id="cipherSuiteId" v-model="newMachine.cipherSuiteId" class="form-select">
                <option value="17">17 - HMAC-SHA256::HMAC_SHA256_128::AES-CBC-128</option>
                <option value="3">3 - HMAC-SHA1::HMAC-SHA1-96::AES-CBC-128</option>
                <option value="">freeipmi-tools default</option>
                <option value="8">8 - HMAC-MD5::HMAC-MD5-128::AES-CBC-128</option>
                <option value="12">12 - HMAC-MD5::MD5-128::AES-CBC-128</option>
              </select>
            </div>

            <div class="form-group">
              <label for="privilegeLevel">Privilege Level</label>
              <select id="privilegeLevel" v-model="newMachine.privilegeLevel" class="form-select">
                <option value="USER">User</option>
                <option value="OPERATOR">Operator</option>
                <option value="ADMIN">Administrator</option>
              </select>
            </div>

            <div class="form-group">
              <label>Workaround Flags</label>
              <div class="checkbox-group">
                <label class="checkbox-label">
                  <input
                    type="checkbox"
                    value="opensesspriv"
                    v-model="newMachine.workaroundFlags"
                    class="form-checkbox"
                  >
                  Opensesspriv
                </label>
                <label class="checkbox-label">
                  <input
                    type="checkbox"
                    value="authcap"
                    v-model="newMachine.workaroundFlags"
                    class="form-checkbox"
                  >
                  Authcap
                </label>
                <label class="checkbox-label">
                  <input
                    type="checkbox"
                    value="idzero"
                    v-model="newMachine.workaroundFlags"
                    class="form-checkbox"
                  >
                  Idzero
                </label>
                <label class="checkbox-label">
                  <input
                    type="checkbox"
                    value="unexpectedauth"
                    v-model="newMachine.workaroundFlags"
                    class="form-checkbox"
                  >
                  Unexpectedauth
                </label>
                <label class="checkbox-label">
                  <input
                    type="checkbox"
                    value="forcepermsg"
                    v-model="newMachine.workaroundFlags"
                    class="form-checkbox"
                  >
                  Forcepermsg
                </label>
                <label class="checkbox-label">
                  <input
                    type="checkbox"
                    value="endianseq"
                    v-model="newMachine.workaroundFlags"
                    class="form-checkbox"
                  >
                  Endianseq
                </label>
                <label class="checkbox-label">
                  <input
                    type="checkbox"
                    value="intel20"
                    v-model="newMachine.workaroundFlags"
                    class="form-checkbox"
                  >
                  Intel20
                </label>
                <label class="checkbox-label">
                  <input
                    type="checkbox"
                    value="supermicro20"
                    v-model="newMachine.workaroundFlags"
                    class="form-checkbox"
                  >
                  Supermicro20
                </label>
                <label class="checkbox-label">
                  <input
                    type="checkbox"
                    value="sun20"
                    v-model="newMachine.workaroundFlags"
                    class="form-checkbox"
                  >
                  Sun20
                </label>
                <label class="checkbox-label">
                  <input
                    type="checkbox"
                    value="nochecksumcheck"
                    v-model="newMachine.workaroundFlags"
                    class="form-checkbox"
                  >
                  Nochecksumcheck
                </label>
                <label class="checkbox-label">
                  <input
                    type="checkbox"
                    value="integritycheckvalue"
                    v-model="newMachine.workaroundFlags"
                    class="form-checkbox"
                  >
                  Integritycheckvalue
                </label>
                <label class="checkbox-label">
                  <input
                    type="checkbox"
                    value="ipmiping"
                    v-model="newMachine.workaroundFlags"
                    class="form-checkbox"
                  >
                  Ipmiping
                </label>
                <label class="checkbox-label">
                  <input
                    type="checkbox"
                    value=""
                    v-model="newMachine.workaroundFlags"
                    class="form-checkbox"
                  >
                  None
                </label>
              </div>
            </div>

            <div class="form-group">
              <label for="powerMac">Power MAC</label>
              <input
                type="text"
                id="powerMac"
                v-model="newMachine.powerMac"
                placeholder="e.g., 08:00:27:11:34:26"
                class="form-input"
              >
            </div>
              </div>
            </div>
          </template>

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
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
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
    
    // Status Select Menu
    const openStatusSelectMenu = ref(false)
    const statusSelectMenuPosition = ref({ top: 0, left: 0 })
    const selectedStatusesForSelection = ref([]) // 선택된 상태 목록
    
    // WebSocket 연결
    // ⚠️ 중요: useWebSocket()은 반드시 최상단에서 먼저 호출해야 함
    // useSettings() 등 다른 composable 호출보다 먼저 호출하여 watch() 의존성 수집에 영향을 주지 않도록 함
    const { connectionStatus, lastMessage, sendMessage } = useWebSocket()
    
    // 설정 로드 (lazy 로딩을 위해 함수로 사용)
    // ⚠️ 주의: useSettings()는 reactive 객체를 생성하므로 watch() 등록 전에 호출해도
    //           WebSocket watch 로직과 분리되어야 함. settingsStore 객체를 직접 참조하지 말고
    //           필요할 때만 getApiParams.value를 사용하도록 함
    const settingsStore = useSettings()
    
    // Settings에서 itemsPerPage 값 가져오기
    const itemsPerPage = ref(settingsStore.settings.itemsPerPage || 25)
    
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
      description: '',
      // IPMI fields
      powerDriver: 'LAN_2_0',
      powerBootType: 'auto',
      powerIpAddress: '',
      powerUser: '',
      powerPassword: '',
      powerKgBmcKey: '',
      cipherSuiteId: '3',
      privilegeLevel: 'OPERATOR',
      workaroundFlags: ['opensesspriv'],
      powerMac: ''
    })
    
    // Commission Machine
    const commissioningMachines = ref([])
    const abortingMachines = ref([])
    
    // Deploy Machine
    const deployingMachines = ref([])
    const abortingDeployMachines = ref([])
    
    // Release Machine
    const releasingMachines = ref([])
    
    // Power Menu
    const hoveredPowerMachine = ref(null)
    const openPowerMenu = ref(null)
    const powerMenuPosition = ref({ top: 0, left: 0 })
    
    // Deploy Menu
    const hoveredDeployMachine = ref(null)
    const openDeployMenu = ref(null)
    const deployMenuPosition = ref({ top: 0, left: 0 })
    const deployableOSList = ref([])
    const loadingDeployableOS = ref(false)
    
    // Action Bar Menu
    const openActionsMenu = ref(false)
    const actionsMenuPosition = ref({ top: 0, left: 0 })
    const openPowerActionMenu = ref(false)
    const powerActionMenuPosition = ref({ top: 0, left: 0 })
    const actionsMenuButton = ref(null)
    const powerActionMenuButton = ref(null)
    
    // Confirmation Modal
    const showConfirmModal = ref(false)
    const confirmModalTitle = ref('확인')
    const confirmModalMessage = ref('')
    const confirmModalButtonText = ref('확인')
    const confirmModalResolve = ref(null)
    
    // Alert Modal
    const showAlertModal = ref(false)
    const alertModalTitle = ref('알림')
    const alertModalMessage = ref('')
    const alertModalResolve = ref(null)
    
    // 커스텀 confirm 함수
    const customConfirm = (message, title = '확인', buttonText = '확인') => {
      return new Promise((resolve) => {
        confirmModalTitle.value = title
        confirmModalMessage.value = message
        confirmModalButtonText.value = buttonText
        confirmModalResolve.value = resolve
        showConfirmModal.value = true
      })
    }
    
    // 커스텀 alert 함수
    const customAlert = (message, title = '알림') => {
      return new Promise((resolve) => {
        alertModalTitle.value = title
        alertModalMessage.value = message
        alertModalResolve.value = resolve
        showAlertModal.value = true
      })
    }
    
    // 확인 모달 확인 버튼
    const confirmAction = () => {
      showConfirmModal.value = false
      if (confirmModalResolve.value) {
        confirmModalResolve.value(true)
        confirmModalResolve.value = null
      }
    }
    
    // 확인 모달 취소 버튼
    const cancelConfirm = () => {
      showConfirmModal.value = false
      if (confirmModalResolve.value) {
        confirmModalResolve.value(false)
        confirmModalResolve.value = null
      }
      // Reset modal position when closing
      confirmModalPosition.value = { top: 0, left: 0 }
    }
    
    // 알림 모달 닫기
    const closeAlert = () => {
      showAlertModal.value = false
      if (alertModalResolve.value) {
        alertModalResolve.value()
        alertModalResolve.value = null
      }
      // Reset modal position when closing
      confirmModalPosition.value = { top: 0, left: 0 }
    }
    
    // Power Menu Functions
    const togglePowerMenu = (machineId, event) => {
      if (openPowerMenu.value === machineId) {
        openPowerMenu.value = null
        powerMenuPosition.value = { top: 0, left: 0 }
      } else {
        // 클릭한 버튼의 위치 계산
        if (event && event.target) {
          const powerContainer = event.target.closest('.power-container')
          if (powerContainer) {
            const buttonRect = powerContainer.getBoundingClientRect()
            if (buttonRect) {
              // position: fixed를 사용하므로 viewport 기준 좌표 사용 (스크롤 오프셋 없음)
              powerMenuPosition.value = {
                top: buttonRect.bottom + 4,
                left: buttonRect.left
              }
              openPowerMenu.value = machineId
            } else {
              console.warn('Could not get button position')
              openPowerMenu.value = null
            }
          } else {
            console.warn('Could not find power-container')
            openPowerMenu.value = null
          }
        } else {
          console.warn('No event or target provided')
          openPowerMenu.value = null
        }
      }
    }
    
    // 머신 ID로 머신 객체 찾기
    const getMachineById = (machineId) => {
      return machines.value.find(m => m.id === machineId) || null
    }
    
    const handlePowerAction = async (machine, action) => {
      if (!machine) {
        console.warn('Machine not found')
        openPowerMenu.value = null
        powerMenuPosition.value = { top: 0, left: 0 }
        return
      }
      
      console.log(`Power action: ${action} for machine ${machine.id}`)
      
      // 메뉴 닫기
      openPowerMenu.value = null
      powerMenuPosition.value = { top: 0, left: 0 }
      
      try {
        const apiParams = settingsStore.getApiParams.value
        const endpoint = action === 'on' 
          ? `http://localhost:8081/api/machines/${machine.id}/power-on`
          : `http://localhost:8081/api/machines/${machine.id}/power-off`
        
        const response = await axios.post(endpoint, null, {
          params: {
            maasUrl: apiParams.maasUrl,
            apiKey: apiParams.apiKey
          }
        })
        
        if (response.data && response.data.success) {
          console.log(`Machine ${action === 'on' ? 'powered on' : 'powered off'} successfully:`, response.data)
          // Power state will be updated via WebSocket
          // Optionally refresh machine list after a short delay
          setTimeout(() => {
            loadMachines()
          }, 2000)
        } else {
          error.value = response.data?.error || `Failed to power ${action} machine`
          console.error(`Failed to power ${action}:`, response.data)
        }
      } catch (err) {
        console.error(`Error powering ${action} machine:`, err)
        error.value = err.response?.data?.error || err.message || `Failed to power ${action} machine`
      }
    }
    
    // Check power - 전원 상태만 조회하고 업데이트 (다른 액션과 완전히 분리)
    const handleCheckPower = async (machine) => {
      if (!machine) {
        console.warn('Machine not found')
        openPowerMenu.value = null
        powerMenuPosition.value = { top: 0, left: 0 }
        return
      }
      
      console.log(`Check power for machine ${machine.id}`)
      
      // 메뉴 닫기
      openPowerMenu.value = null
      powerMenuPosition.value = { top: 0, left: 0 }
      
      try {
        const apiParams = settingsStore.getApiParams.value
        const endpoint = `http://localhost:8081/api/machines/${machine.id}/query-power-state`
        
        const response = await axios.get(endpoint, {
          params: {
            maasUrl: apiParams.maasUrl,
            apiKey: apiParams.apiKey
          }
        })
        
        if (response.data && response.data.success && response.data.state) {
          console.log(`Power state queried successfully:`, response.data)
          // Update machine's power_state with the queried state only
          const machineIndex = machines.value.findIndex(m => m.id === machine.id)
          if (machineIndex !== -1) {
            machines.value[machineIndex].power_state = response.data.state
          }
        } else {
          error.value = response.data?.error || 'Failed to query power state'
          console.error('Failed to query power state:', response.data)
        }
      } catch (err) {
        console.error('Error querying power state:', err)
        error.value = err.response?.data?.error || err.message || 'Failed to query power state'
      }
    }
    
    // Machine Details Modal
    const showMachineDetailsModal = ref(false)
    const selectedMachineForDetails = ref(null)
    const machineDetails = ref(null)
    const loadingMachineDetails = ref(false)
    const machineDetailsError = ref(null)
    const activeDetailsTab = ref('overview')
    const machineBlockDevices = ref([])
    const loadingBlockDevices = ref(false)
    const machineEvents = ref([])
    const loadingEvents = ref(false)
    const powerParameters = ref(null)
    const loadingPowerParameters = ref(false)
    const powerParametersError = ref(null)
    const powerTypeChanged = ref(false) // Power Type이 변경되었는지 추적
    const isEditingPowerParameters = ref(false)
    const editingPowerParameters = ref({
      powerType: 'manual',
      powerDriver: '',
      powerBootType: 'auto',
      powerIpAddress: '',
      powerUser: '',
      powerPassword: '',
      powerKgBmcKey: '',
      cipherSuiteId: '3',
      privilegeLevel: 'OPERATOR',
      workaroundFlags: ['opensesspriv'],
      powerMac: ''
    })
    
    // Modal drag state
    const isDraggingModal = ref(false)
    const modalPosition = ref({ top: 0, left: 0 })
    const dragStartPosition = ref({ x: 0, y: 0 })
    
    // Confirm/Alert Modal drag state
    const isDraggingConfirmModal = ref(false)
    const confirmModalPosition = ref({ top: 0, left: 0 })
    const confirmDragStartPosition = ref({ x: 0, y: 0 })
    
    // Network Modal drag state
    const isDraggingNetworkModal = ref(false)
    const networkModalPosition = ref({ top: 0, left: 0 })
    const networkDragStartPosition = ref({ x: 0, y: 0 })
    
    // Deploy Modal drag state
    const isDraggingDeployModal = ref(false)
    const deployModalPosition = ref({ top: 0, left: 0 })
    const deployDragStartPosition = ref({ x: 0, y: 0 })
    
    // Deploy Modal state
    const showDeployModalState = ref(false)
    const selectedDeployMachine = ref(null)
    const selectedDeployOS = ref(null)
    const selectedCloudConfigTemplate = ref('none')
    const customCloudConfig = ref('')
    const deployingMachine = ref(false)
    const cloudConfigTemplates = ref([])
    
    // Machine Details Modal Functions
    const showMachineDetails = async (machine) => {
      selectedMachineForDetails.value = machine
      showMachineDetailsModal.value = true
      activeDetailsTab.value = 'overview'
      loadingMachineDetails.value = true
      machineDetailsError.value = null
      machineDetails.value = null
      machineBlockDevices.value = []
      
      try {
        const apiParams = settingsStore.getApiParams.value
        
        // 최신 머신 정보 가져오기
        console.log(`[Machine Details] Fetching details for machine: ${machine.id}`)
        const machineResponse = await axios.get(`http://localhost:8081/api/machines/${machine.id}`, {
          params: apiParams
        })
        
        if (machineResponse.data && !machineResponse.data.error) {
          machineDetails.value = machineResponse.data
          console.log('[Machine Details] Machine details loaded:', machineDetails.value)
          
          // Block Devices 정보도 가져오기
          loadingBlockDevices.value = true
          try {
            const blockDevicesResponse = await axios.get(`http://localhost:8081/api/machines/${machine.id}/block-devices`, {
              params: apiParams
            })
            
            if (blockDevicesResponse.data && !blockDevicesResponse.data.error) {
              // API 응답 형식: {results: [...]} 또는 blockdevice_set
              machineBlockDevices.value = blockDevicesResponse.data.results || blockDevicesResponse.data.blockdevice_set || []
              console.log('[Machine Details] Block devices loaded:', machineBlockDevices.value)
            }
          } catch (err) {
            console.warn('[Machine Details] Failed to load block devices:', err)
            machineBlockDevices.value = []
          } finally {
            loadingBlockDevices.value = false
          }
          
          // Power Parameters 정보 가져오기 (IPMI인 경우만)
          if (machineDetails.value.power_type === 'ipmi') {
            await loadMachinePowerParameters(machine.id)
          } else {
            powerParameters.value = null
            powerParametersError.value = null
          }
          
          // Events 정보도 가져오기
          await loadMachineEvents(machine.id)
        } else {
          machineDetailsError.value = machineResponse.data?.error || 'Failed to load machine details'
        }
      } catch (err) {
        console.error('[Machine Details] Error loading machine details:', err)
        machineDetailsError.value = err.response?.data?.error || err.message || 'Failed to load machine details'
      } finally {
        loadingMachineDetails.value = false
      }
    }
    
    const loadMachinePowerParameters = async (systemId) => {
      loadingPowerParameters.value = true
      powerParameters.value = null
      powerParametersError.value = null
      
      try {
        const apiParams = settingsStore.getApiParams.value
        console.log('[Machine Details] Loading power parameters for systemId:', systemId)
        const powerParamsResponse = await axios.get(`http://localhost:8081/api/machines/${systemId}/power-parameters`, {
          params: apiParams
        })
        
        console.log('[Machine Details] Power Parameters API response:', powerParamsResponse.data)
        
        if (powerParamsResponse.data && !powerParamsResponse.data.error) {
          powerParameters.value = powerParamsResponse.data
          console.log('[Machine Details] Power parameters loaded:', powerParameters.value)
        } else {
          powerParametersError.value = powerParamsResponse.data?.error || 'Failed to load power parameters'
        }
      } catch (err) {
        console.error('[Machine Details] Error loading power parameters:', err)
        powerParametersError.value = err.response?.data?.error || err.message || 'Failed to load power parameters'
      } finally {
        loadingPowerParameters.value = false
      }
    }
    
    const savingPowerParameters = ref(false)
    
    const startEditingPowerParameters = () => {
      if (!machineDetails.value) return
      
      // 현재 power type과 power parameters 값을 편집용 데이터로 복사
      editingPowerParameters.value = {
        powerType: machineDetails.value.power_type || 'manual',
        powerDriver: powerParameters.value?.power_driver || 'LAN_2_0',
        powerBootType: powerParameters.value?.power_boot_type || 'auto',
        powerIpAddress: powerParameters.value?.power_address || '',
        powerUser: powerParameters.value?.power_user || '',
        powerPassword: '', // 보안을 위해 비워둠
        powerKgBmcKey: powerParameters.value?.k_g || '',
        cipherSuiteId: powerParameters.value?.cipher_suite_id !== undefined && powerParameters.value?.cipher_suite_id !== null 
          ? String(powerParameters.value.cipher_suite_id) 
          : '3',
        privilegeLevel: powerParameters.value?.privilege_level || 'OPERATOR',
        workaroundFlags: Array.isArray(powerParameters.value?.workaround_flags) 
          ? [...powerParameters.value.workaround_flags]
          : (powerParameters.value?.workaround_flags 
              ? powerParameters.value.workaround_flags.split(',').map(f => f.trim()).filter(f => f)
              : ['opensesspriv']),
        powerMac: powerParameters.value?.mac_address || ''
      }
      
      isEditingPowerParameters.value = true
    }
    
    const cancelEditingPowerParameters = () => {
      isEditingPowerParameters.value = false
      // 편집 데이터 초기화
      editingPowerParameters.value = {
        powerType: 'manual',
        powerDriver: '',
        powerBootType: 'auto',
        powerIpAddress: '',
        powerUser: '',
        powerPassword: '',
        powerKgBmcKey: '',
        cipherSuiteId: '3',
        privilegeLevel: 'OPERATOR',
        workaroundFlags: ['opensesspriv'],
        powerMac: ''
      }
    }
    
    const savePowerParameters = async () => {
      if (!selectedMachineForDetails.value || !machineDetails.value) {
        console.error('[Machine Details] No machine selected for saving power parameters')
        return
      }
      
      console.log('[Machine Details] Saving power parameters:', editingPowerParameters.value)
      savingPowerParameters.value = true
      
      try {
        const settingsStore = useSettings()
        const settings = settingsStore.settings
        if (!settings.maasUrl || !settings.apiKey) {
          throw new Error('MAAS server URL and API key must be configured')
        }
        
        const systemId = selectedMachineForDetails.value.id
        const params = {
          maasUrl: settings.maasUrl,
          apiKey: settings.apiKey,
          powerType: editingPowerParameters.value.powerType,
        }
        
        // IPMI 파라미터는 power_type이 'ipmi'일 때만 추가
        if (editingPowerParameters.value.powerType === 'ipmi') {
          if (editingPowerParameters.value.powerDriver) {
            params.powerDriver = editingPowerParameters.value.powerDriver
          }
          if (editingPowerParameters.value.powerBootType) {
            params.powerBootType = editingPowerParameters.value.powerBootType
          }
          if (editingPowerParameters.value.powerIpAddress) {
            params.powerIpAddress = editingPowerParameters.value.powerIpAddress
          }
          if (editingPowerParameters.value.powerUser) {
            params.powerUser = editingPowerParameters.value.powerUser
          }
          // Password는 비어있지 않을 때만 전송 (비워두면 기존 값 유지)
          if (editingPowerParameters.value.powerPassword) {
            params.powerPassword = editingPowerParameters.value.powerPassword
          }
          if (editingPowerParameters.value.powerKgBmcKey) {
            params.powerKgBmcKey = editingPowerParameters.value.powerKgBmcKey
          }
          if (editingPowerParameters.value.cipherSuiteId) {
            params.cipherSuiteId = editingPowerParameters.value.cipherSuiteId
          }
          if (editingPowerParameters.value.privilegeLevel) {
            params.privilegeLevel = editingPowerParameters.value.privilegeLevel
          }
          // Workaround Flags는 배열을 문자열로 변환
          if (editingPowerParameters.value.workaroundFlags && editingPowerParameters.value.workaroundFlags.length > 0) {
            // None('')이 포함되어 있으면 빈 문자열, 아니면 쉼표로 구분된 문자열
            if (editingPowerParameters.value.workaroundFlags.includes('')) {
              params.workaroundFlags = ''
            } else {
              params.workaroundFlags = editingPowerParameters.value.workaroundFlags.join(',')
            }
          }
          if (editingPowerParameters.value.powerMac) {
            params.powerMac = editingPowerParameters.value.powerMac
          }
        }
        
        const response = await axios.put(`http://localhost:8081/api/machines/${systemId}/power-parameters`, null, { params })
        
        if (response.data.success) {
          console.log('[Machine Details] Power parameters saved successfully')
          
          // Power Type이 변경되었는지 확인
          const originalPowerType = machineDetails.value?.power_type
          const newPowerType = editingPowerParameters.value.powerType
          if (originalPowerType !== newPowerType) {
            powerTypeChanged.value = true
            console.log(`[Machine Details] Power type changed from ${originalPowerType} to ${newPowerType}`)
          }
          
          // 저장 성공 후 편집 모드 종료
          isEditingPowerParameters.value = false
          
          // Machine details와 power parameters 다시 로드
          await showMachineDetails(selectedMachineForDetails.value)
        } else {
          throw new Error(response.data.error || 'Failed to save power parameters')
        }
      } catch (err) {
        console.error('[Machine Details] Error saving power parameters:', err)
        const errorMessage = err.response?.data?.error || err.message || 'Failed to save power parameters'
        alert('Error saving power parameters: ' + errorMessage)
      } finally {
        savingPowerParameters.value = false
      }
    }
    
    // Watch editingPowerParameters.workaroundFlags to handle "None" option
    watch(() => editingPowerParameters.value.workaroundFlags, (newFlags, oldFlags) => {
      if (!oldFlags) return
      
      // If "None" (empty string) is selected, clear all other options
      if (newFlags.includes('')) {
        editingPowerParameters.value.workaroundFlags = ['']
      } 
      // If any other option is selected, remove "None"
      else if (oldFlags.includes('') && newFlags.length > 0) {
        editingPowerParameters.value.workaroundFlags = newFlags.filter(flag => flag !== '')
      }
    }, { deep: true })
    
    // Handle wheel event in modal to prevent parent scroll
    const handleModalWheel = (event) => {
      const modalContent = event.currentTarget
      const scrollableContent = modalContent.querySelector('.details-section')
      
      if (!scrollableContent) return
      
      const { scrollTop, scrollHeight, clientHeight } = scrollableContent
      const isAtTop = scrollTop === 0
      const isAtBottom = scrollTop + clientHeight >= scrollHeight - 1
      
      // If scrolling up and already at top, prevent default
      if (event.deltaY < 0 && isAtTop) {
        event.preventDefault()
        return
      }
      
      // If scrolling down and already at bottom, prevent default
      if (event.deltaY > 0 && isAtBottom) {
        event.preventDefault()
        return
      }
      
      // Otherwise, allow scrolling within the modal
      scrollableContent.scrollTop += event.deltaY
      event.preventDefault()
    }
    
    const loadMachineEvents = async (systemId) => {
      loadingEvents.value = true
      machineEvents.value = []
      
      try {
        const apiParams = settingsStore.getApiParams.value
        console.log('[Machine Details] Loading events for systemId:', systemId)
        const eventsResponse = await axios.get(`http://localhost:8081/api/events/op-query`, {
          params: apiParams
        })
        
        console.log('[Machine Details] Events API response:', eventsResponse.data)
        
        if (eventsResponse.data && !eventsResponse.data.error) {
          // API 응답이 배열인 경우와 {results: [...]} 형식인 경우 모두 처리
          let allEvents = []
          if (Array.isArray(eventsResponse.data)) {
            allEvents = eventsResponse.data
          } else if (eventsResponse.data.results && Array.isArray(eventsResponse.data.results)) {
            allEvents = eventsResponse.data.results
          }
          
          console.log('[Machine Details] Total events received:', allEvents.length)
          console.log('[Machine Details] Looking for systemId:', systemId)
          
          // node 필드가 systemId와 일치하는 이벤트만 필터링
          const filteredEvents = allEvents.filter(event => {
            const matches = event.node === systemId
            if (!matches && allEvents.length < 20) {
              // 디버깅: 처음 몇 개 이벤트의 node 값 확인
              console.log('[Machine Details] Event node:', event.node, 'systemId:', systemId, 'match:', matches)
            }
            return matches
          })
          
          console.log('[Machine Details] Filtered events count:', filteredEvents.length)
          
          // 날짜 순으로 정렬 (created 필드 기준, 최신순)
          filteredEvents.sort((a, b) => {
            const dateA = new Date(a.created)
            const dateB = new Date(b.created)
            return dateB - dateA // 최신순 (내림차순)
          })
          
          machineEvents.value = filteredEvents
          console.log('[Machine Details] Events loaded:', machineEvents.value)
        } else {
          console.warn('[Machine Details] Events API returned error or no data:', eventsResponse.data)
        }
      } catch (err) {
        console.error('[Machine Details] Failed to load events:', err)
        machineEvents.value = []
      } finally {
        loadingEvents.value = false
      }
    }
    
    const closeMachineDetailsModal = async () => {
      // Power Type이 변경되었으면 머신 목록 리로드
      if (powerTypeChanged.value) {
        console.log('[Machine Details] Power type was changed, reloading machines list')
        await loadMachines()
        powerTypeChanged.value = false
      }
      
      showMachineDetailsModal.value = false
      selectedMachineForDetails.value = null
      machineDetails.value = null
      machineDetailsError.value = null
      machineBlockDevices.value = []
      machineEvents.value = []
      activeDetailsTab.value = 'overview'
      // Reset modal position when closing
      modalPosition.value = { top: 0, left: 0 }
    }
    
    // Modal drag handlers
    const startDragModal = (event) => {
      if (event.button !== 0) return // Only left mouse button
      isDraggingModal.value = true
      const modalElement = event.currentTarget.closest('.machine-details-modal-content')
      if (modalElement) {
        const rect = modalElement.getBoundingClientRect()
        dragStartPosition.value = {
          x: event.clientX - rect.left,
          y: event.clientY - rect.top
        }
        // If modal hasn't been moved yet, center it
        if (modalPosition.value.top === 0 && modalPosition.value.left === 0) {
          const viewportWidth = window.innerWidth
          const viewportHeight = window.innerHeight
          const modalWidth = rect.width
          const modalHeight = rect.height
          modalPosition.value = {
            top: (viewportHeight - modalHeight) / 2,
            left: (viewportWidth - modalWidth) / 2
          }
        }
      }
      event.preventDefault()
    }
    
    const onDragModal = (event) => {
      if (!isDraggingModal.value) return
      
      const viewportWidth = window.innerWidth
      const viewportHeight = window.innerHeight
      
      // Find modal element by querying the DOM
      const modalElement = document.querySelector('.machine-details-modal-content')
      if (!modalElement) return
      
      const modalWidth = modalElement.offsetWidth
      const modalHeight = modalElement.offsetHeight
      
      // Calculate new position
      let newLeft = event.clientX - dragStartPosition.value.x
      let newTop = event.clientY - dragStartPosition.value.y
      
      // Constrain to viewport bounds
      newLeft = Math.max(0, Math.min(newLeft, viewportWidth - modalWidth))
      newTop = Math.max(0, Math.min(newTop, viewportHeight - modalHeight))
      
      modalPosition.value = {
        left: newLeft,
        top: newTop
      }
    }
    
    const stopDragModal = () => {
      isDraggingModal.value = false
    }
    
    // Add global mousemove and mouseup listeners when dragging
    watch(isDraggingModal, (dragging) => {
      if (dragging) {
        document.addEventListener('mousemove', onDragModal)
        document.addEventListener('mouseup', stopDragModal)
      } else {
        document.removeEventListener('mousemove', onDragModal)
        document.removeEventListener('mouseup', stopDragModal)
      }
    })
    
    // Confirm/Alert Modal drag handlers
    const startDragConfirmModal = (event) => {
      if (event.button !== 0) return
      isDraggingConfirmModal.value = true
      const modalElement = event.currentTarget.closest('.confirm-modal-content, .alert-modal-content')
      if (modalElement) {
        const rect = modalElement.getBoundingClientRect()
        confirmDragStartPosition.value = {
          x: event.clientX - rect.left,
          y: event.clientY - rect.top
        }
        if (confirmModalPosition.value.top === 0 && confirmModalPosition.value.left === 0) {
          const viewportWidth = window.innerWidth
          const viewportHeight = window.innerHeight
          const modalWidth = rect.width
          const modalHeight = rect.height
          confirmModalPosition.value = {
            top: (viewportHeight - modalHeight) / 2,
            left: (viewportWidth - modalWidth) / 2
          }
        }
      }
      event.preventDefault()
    }
    
    const onDragConfirmModal = (event) => {
      if (!isDraggingConfirmModal.value) return
      
      const viewportWidth = window.innerWidth
      const viewportHeight = window.innerHeight
      const modalElement = document.querySelector('.confirm-modal-content, .alert-modal-content')
      if (!modalElement) return
      
      const modalWidth = modalElement.offsetWidth
      const modalHeight = modalElement.offsetHeight
      
      let newLeft = event.clientX - confirmDragStartPosition.value.x
      let newTop = event.clientY - confirmDragStartPosition.value.y
      
      newLeft = Math.max(0, Math.min(newLeft, viewportWidth - modalWidth))
      newTop = Math.max(0, Math.min(newTop, viewportHeight - modalHeight))
      
      confirmModalPosition.value = {
        left: newLeft,
        top: newTop
      }
    }
    
    const stopDragConfirmModal = () => {
      isDraggingConfirmModal.value = false
    }
    
    watch(isDraggingConfirmModal, (dragging) => {
      if (dragging) {
        document.addEventListener('mousemove', onDragConfirmModal)
        document.addEventListener('mouseup', stopDragConfirmModal)
      } else {
        document.removeEventListener('mousemove', onDragConfirmModal)
        document.removeEventListener('mouseup', stopDragConfirmModal)
      }
    })
    
    // Network Modal drag handlers
    const startDragNetworkModal = (event) => {
      if (event.button !== 0) return
      isDraggingNetworkModal.value = true
      const modalElement = event.currentTarget.closest('.network-modal-content')
      if (modalElement) {
        const rect = modalElement.getBoundingClientRect()
        networkDragStartPosition.value = {
          x: event.clientX - rect.left,
          y: event.clientY - rect.top
        }
        if (networkModalPosition.value.top === 0 && networkModalPosition.value.left === 0) {
          const viewportWidth = window.innerWidth
          const viewportHeight = window.innerHeight
          const modalWidth = rect.width
          const modalHeight = rect.height
          networkModalPosition.value = {
            top: (viewportHeight - modalHeight) / 2,
            left: (viewportWidth - modalWidth) / 2
          }
        }
      }
      event.preventDefault()
    }
    
    const onDragNetworkModal = (event) => {
      if (!isDraggingNetworkModal.value) return
      
      const viewportWidth = window.innerWidth
      const viewportHeight = window.innerHeight
      const modalElement = document.querySelector('.network-modal-content')
      if (!modalElement) return
      
      const modalWidth = modalElement.offsetWidth
      const modalHeight = modalElement.offsetHeight
      
      let newLeft = event.clientX - networkDragStartPosition.value.x
      let newTop = event.clientY - networkDragStartPosition.value.y
      
      newLeft = Math.max(0, Math.min(newLeft, viewportWidth - modalWidth))
      newTop = Math.max(0, Math.min(newTop, viewportHeight - modalHeight))
      
      networkModalPosition.value = {
        left: newLeft,
        top: newTop
      }
    }
    
    const stopDragNetworkModal = () => {
      isDraggingNetworkModal.value = false
    }
    
    watch(isDraggingNetworkModal, (dragging) => {
      if (dragging) {
        document.addEventListener('mousemove', onDragNetworkModal)
        document.addEventListener('mouseup', stopDragNetworkModal)
      } else {
        document.removeEventListener('mousemove', onDragNetworkModal)
        document.removeEventListener('mouseup', stopDragNetworkModal)
      }
    })
    
    // Deploy Modal drag handlers
    const startDragDeployModal = (event) => {
      if (event.button !== 0) return
      isDraggingDeployModal.value = true
      const modalElement = event.currentTarget.closest('.deploy-modal-content')
      if (modalElement) {
        const rect = modalElement.getBoundingClientRect()
        deployDragStartPosition.value = {
          x: event.clientX - rect.left,
          y: event.clientY - rect.top
        }
        if (deployModalPosition.value.top === 0 && deployModalPosition.value.left === 0) {
          const viewportWidth = window.innerWidth
          const viewportHeight = window.innerHeight
          const modalWidth = rect.width
          const modalHeight = rect.height
          deployModalPosition.value = {
            top: (viewportHeight - modalHeight) / 2,
            left: (viewportWidth - modalWidth) / 2
          }
        }
      }
      event.preventDefault()
    }
    
    const onDragDeployModal = (event) => {
      if (!isDraggingDeployModal.value) return
      
      const viewportWidth = window.innerWidth
      const viewportHeight = window.innerHeight
      const modalElement = document.querySelector('.deploy-modal-content')
      if (!modalElement) return
      
      const modalWidth = modalElement.offsetWidth
      const modalHeight = modalElement.offsetHeight
      
      let newLeft = event.clientX - deployDragStartPosition.value.x
      let newTop = event.clientY - deployDragStartPosition.value.y
      
      newLeft = Math.max(0, Math.min(newLeft, viewportWidth - modalWidth))
      newTop = Math.max(0, Math.min(newTop, viewportHeight - modalHeight))
      
      deployModalPosition.value = {
        left: newLeft,
        top: newTop
      }
    }
    
    const stopDragDeployModal = () => {
      isDraggingDeployModal.value = false
    }
    
    watch(isDraggingDeployModal, (dragging) => {
      if (dragging) {
        document.addEventListener('mousemove', onDragDeployModal)
        document.addEventListener('mouseup', stopDragDeployModal)
      } else {
        document.removeEventListener('mousemove', onDragDeployModal)
        document.removeEventListener('mouseup', stopDragDeployModal)
      }
    })
    
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
    const globalFabricsMap = ref({}) // fabric id -> fabric name mapping (전역 fabric 목록)
    
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
        filtered = filtered.filter(machine => {
          const normalizedIps = normalizeIpAddresses(machine.ip_addresses)
          const normalizedMacs = normalizeMacAddresses(machine.mac_addresses)
          return (
          (machine.hostname && machine.hostname.toLowerCase().includes(query)) ||
            (normalizedIps.length > 0 && normalizedIps.some(ip => ip && ip.toLowerCase().includes(query))) ||
            (normalizedMacs.length > 0 && normalizedMacs.some(mac => mac && mac.toLowerCase().includes(query))) ||
          machine.id.toString().includes(query)
        )
        })
      }
      
      return filtered
    })
    
    const totalPages = computed(() => {
      return Math.ceil(filteredMachines.value.length / itemsPerPage.value)
    })
    
    // 페이지네이션이 적용된 머신 목록
    const paginatedMachines = computed(() => {
      const start = (currentPage.value - 1) * itemsPerPage.value
      const end = start + itemsPerPage.value
      return filteredMachines.value.slice(start, end)
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
            // MAC 주소 추출
            const macAddresses = extractMacAddresses(machineData)
            // MAC 주소로 fabric 찾기
            const fabricName = findFabricByMacAddress({
              ...machineData,
              mac_addresses: macAddresses
            })
            
            // IP 주소 추출
            const extractedIps = extractIpAddresses(machineData)
            const normalizedIps = extractedIps.length > 0 ? extractedIps : normalizeIpAddresses(machineData.ip_addresses)
            
            // 기존 머신 정보를 업데이트 (interface_set 포함)
            machines.value[machineIndex] = {
              ...machines.value[machineIndex],
              hostname: machineData.hostname,
              status: getStatusName(machineData.status_name || machineData.status),
              status_message: machineData.status_message,
              osystem: machineData.osystem || machines.value[machineIndex].osystem,
              distro_series: machineData.distro_series || machines.value[machineIndex].distro_series,
              ip_addresses: normalizedIps,
              mac_addresses: macAddresses,
              architecture: machineData.architecture,
              cpu_count: machineData.cpu_count || 0,
              memory: machineData.memory || 0,
              power_state: machineData.power_state,
              power_type: machineData.power_type || 'Manual',
              owner: machineData.owner,
              tags: machineData.tag_names || [],
              pool: machineData.pool?.name || 'default',
              zone: machineData.zone?.name || 'default',
              fabric: fabricName,
              interface_set: machineData.interface_set || [] // 네트워크 인터페이스 정보 업데이트
            }
            
            // blockdevice_set 정보를 사용하여 disk_count와 storage 업데이트
            const blockDeviceSet = machineData.blockdevice_set || []
            console.log(`[Refresh] Block device set for ${systemId}:`, blockDeviceSet, `(count: ${blockDeviceSet.length})`)
            
            machines.value[machineIndex].disk_count = blockDeviceSet.length || 0
            machines.value[machineIndex].storage = calculateStorage(blockDeviceSet)
            console.log(`✅ Updated block devices for ${systemId}: ${machines.value[machineIndex].disk_count} disks, ${formatStorage(machines.value[machineIndex].storage)}`)
            
            console.log(`✅ Machine details refreshed for: ${systemId}`)
          }
        }
      } catch (err) {
        console.error(`Error refreshing machine details for ${systemId}:`, err)
      }
    }
    
    // Fabric 목록을 로드하는 함수
    const loadFabrics = async () => {
      try {
        const apiParams = settingsStore.getApiParams.value
        const fabricsResponse = await axios.get('http://localhost:8081/api/fabrics', {
          params: apiParams
        })
        
        if (fabricsResponse.data && fabricsResponse.data.results) {
          // fabric id -> fabric name 매핑 저장
          globalFabricsMap.value = {}
          fabricsResponse.data.results.forEach(fabric => {
            const fabricId = fabric.id
            const fabricName = fabric.name || `fabric-${fabricId}`
            // 여러 타입의 키로 저장 (타입 불일치 대비)
            globalFabricsMap.value[fabricId] = fabricName
            globalFabricsMap.value[String(fabricId)] = fabricName
            if (typeof fabricId === 'string') {
              const numId = parseInt(fabricId, 10)
              if (!isNaN(numId)) {
                globalFabricsMap.value[numId] = fabricName
              }
            } else if (typeof fabricId === 'number') {
              globalFabricsMap.value[String(fabricId)] = fabricName
            }
          })
          console.log('✅ Global fabrics map loaded:', Object.keys(globalFabricsMap.value).length, 'fabrics')
        }
      } catch (err) {
        console.warn('⚠️ Failed to load fabrics:', err.message)
      }
    }
    
    const loadMachines = async () => {
      loading.value = true
      error.value = null
      
      try {
        console.log('🔄 Loading machines via REST API...')
        
        // Fabric 목록 먼저 로드
        await loadFabrics()
        
        // REST API로 머신 목록 가져오기
        const response = await axios.get('http://localhost:8081/api/machines', {
          params: settingsStore.getApiParams.value
        })
        
        if (response.data && response.data.results) {
          // MAAS API 응답을 우리 UI 형식으로 변환
          machines.value = response.data.results.map(machine => {
            const macAddresses = extractMacAddresses(machine)
            // IP 주소 추출
            const extractedIps = extractIpAddresses(machine)
            const normalizedIps = extractedIps.length > 0 ? extractedIps : normalizeIpAddresses(machine.ip_addresses)
            
            // MAC 주소로 fabric 찾기
            const fabricName = findFabricByMacAddress({
              ...machine,
              mac_addresses: macAddresses
            })
            
            return {
            id: machine.system_id,
            hostname: machine.hostname,
              status: getStatusName(machine.status_name || machine.status),
            status_message: machine.status_message,
              osystem: machine.osystem, // OS 이름 (예: 'ubuntu')
              distro_series: machine.distro_series, // 배포판 시리즈 (예: 'noble', 'jammy')
              ip_addresses: normalizedIps,
              mac_addresses: macAddresses,
            architecture: machine.architecture,
            cpu_count: machine.cpu_count || 0,
            memory: machine.memory || 0,
              disk_count: machine.blockdevice_set?.length || 0,
              storage: calculateStorage(machine.blockdevice_set),
            power_state: machine.power_state,
              power_type: machine.power_type || 'Manual',
            owner: machine.owner,
            tags: machine.tag_names || [],
            pool: machine.pool?.name || 'default',
            zone: machine.zone?.name || 'default',
              fabric: fabricName,
              interface_set: machine.interface_set || [] // 네트워크 인터페이스 정보 저장
            }
          })
          console.log(`✅ Loaded ${machines.value.length} machines via REST API`)
          
          // 각 머신의 상세 정보를 가져와서 blockdevice_set 정보 업데이트
          console.log('🔄 Fetching detailed machine information for disks and storage...')
          const apiParams = settingsStore.getApiParams.value
          const detailPromises = machines.value.map(async (machine) => {
            try {
              console.log(`[Machine Details] Fetching for machine ${machine.id} (${machine.hostname || 'N/A'})`)
              const detailResponse = await axios.get(`http://localhost:8081/api/machines/${machine.id}`, {
                params: apiParams
              })
              
              if (detailResponse.data && !detailResponse.data.error) {
                const machineData = detailResponse.data
                const blockDeviceSet = machineData.blockdevice_set || []
                console.log(`[Machine Details] Block device set for ${machine.id}:`, blockDeviceSet, `(count: ${blockDeviceSet.length})`)
                
                const machineIndex = machines.value.findIndex(m => m.id === machine.id)
                if (machineIndex !== -1) {
                  machines.value[machineIndex].disk_count = blockDeviceSet.length || 0
                  machines.value[machineIndex].storage = calculateStorage(blockDeviceSet)
                  console.log(`✅ Updated disk info for ${machine.hostname || machine.id}: ${machines.value[machineIndex].disk_count} disks, ${formatStorage(machines.value[machineIndex].storage)}`)
                } else {
                  console.warn(`[Machine Details] Machine index not found for ${machine.id}`)
                }
              } else {
                console.warn(`[Machine Details] Error in response for ${machine.id}:`, detailResponse.data?.error)
              }
            } catch (err) {
              console.warn(`⚠️ Failed to fetch details for machine ${machine.id}:`, err.message, err.response?.data)
              // 개별 머신 정보 가져오기 실패해도 계속 진행
            }
          })
          
          // 모든 상세 정보를 병렬로 가져오기
          await Promise.all(detailPromises)
          console.log('✅ Finished updating disk and storage information')
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
    
    const formatPowerState = (powerState) => {
      if (!powerState) return 'Unknown'
      return powerState.charAt(0).toUpperCase() + powerState.slice(1).toLowerCase()
    }
    
    const getPowerStateClass = (powerState) => {
      if (!powerState) return 'power-led-unknown'
      const state = powerState.toLowerCase()
      if (state === 'on') return 'power-led-on'
      if (state === 'off') return 'power-led-off'
      // IPMI error 상태 감지
      if (state === 'error' || state.includes('error')) return 'power-led-error'
      return 'power-led-unknown'
    }
    
    const formatPowerType = (powerType) => {
      if (!powerType) return 'Manual'
      const type = powerType.toLowerCase()
      if (type === 'ipmi') return 'IPMI'
      if (type === 'unknown') return 'Unknown'
      return powerType.charAt(0).toUpperCase() + powerType.slice(1).toLowerCase()
    }
    
    const formatPowerDriver = (driver) => {
      if (!driver) return '-'
      const driverMap = {
        'LAN': 'LAN [IPMI 1.5]',
        'LAN_2_0': 'LAN_2_0 [IPMI 2.0]'
      }
      return driverMap[driver] || driver
    }
    
    const formatPowerBootType = (bootType) => {
      if (!bootType) return '-'
      const bootTypeMap = {
        'auto': 'Automatic',
        'legacy': 'Legacy boot',
        'efi': 'EFI boot'
      }
      return bootTypeMap[bootType] || bootType
    }
    
    const formatCipherSuiteId = (cipherSuiteId) => {
      if (cipherSuiteId === undefined || cipherSuiteId === null || cipherSuiteId === '') {
        return 'freeipmi-tools default'
      }
      const cipherSuiteMap = {
        '17': '17 - HMAC-SHA256::HMAC_SHA256_128::AES-CBC-128',
        '3': '3 - HMAC-SHA1::HMAC-SHA1-96::AES-CBC-128',
        '8': '8 - HMAC-MD5::HMAC-MD5-128::AES-CBC-128',
        '12': '12 - HMAC-MD5::MD5-128::AES-CBC-128'
      }
      const key = String(cipherSuiteId)
      return cipherSuiteMap[key] || cipherSuiteId
    }
    
    const formatPrivilegeLevel = (privilegeLevel) => {
      if (!privilegeLevel) return '-'
      const privilegeLevelMap = {
        'USER': 'User',
        'OPERATOR': 'Operator',
        'ADMIN': 'Administrator'
      }
      return privilegeLevelMap[privilegeLevel] || privilegeLevel
    }
    
    const formatWorkaroundFlags = (flags) => {
      if (!flags) return '-'
      if (Array.isArray(flags)) {
        return flags.filter(f => f !== '' && f !== null && f !== undefined).join(', ') || '-'
      }
      return flags
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
        const normalizedStatus = statusCode.toLowerCase().trim()
        
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
        
        // 매핑에 있는 경우만 변환하고, 그 외는 원본 상태 문자열 유지 (예: "failed deployment")
        if (stringStatusMap[normalizedStatus]) {
          return stringStatusMap[normalizedStatus]
        }
        
        // 매핑에 없는 복합 상태 문자열은 원본 유지 (소문자로 정규화)
        return normalizedStatus || 'unknown'
      }
      
      console.warn('Unknown status type:', typeof statusCode, statusCode)
      return 'unknown'
    }
    
    const calculateStorage = (blockDeviceSet) => {
      if (!blockDeviceSet || !Array.isArray(blockDeviceSet)) return 0
      return blockDeviceSet.reduce((total, device) => {
        // blockdevice_set의 각 항목의 size 값을 합산
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
    
    // interface_set에서 IP 주소를 추출하는 함수
    const extractIpAddresses = (machine) => {
      const ipAddresses = []
      
      // interface_set에서 IP 주소 추출
      if (machine.interface_set && Array.isArray(machine.interface_set)) {
        machine.interface_set.forEach(networkInterface => {
          // links 배열에서 IP 주소 추출
          if (networkInterface.links && Array.isArray(networkInterface.links)) {
            networkInterface.links.forEach(link => {
              if (link.ip_address && typeof link.ip_address === 'string' && link.ip_address.trim() !== '') {
                const ip = link.ip_address.trim()
                if (!ipAddresses.includes(ip)) {
                  ipAddresses.push(ip)
                }
              }
            })
          }
        })
      }
      
      // boot_interface에서 IP 주소 추출
      if (machine.boot_interface && machine.boot_interface.links && Array.isArray(machine.boot_interface.links)) {
        machine.boot_interface.links.forEach(link => {
          if (link.ip_address && typeof link.ip_address === 'string' && link.ip_address.trim() !== '') {
            const ip = link.ip_address.trim()
            if (!ipAddresses.includes(ip)) {
              ipAddresses.push(ip)
            }
          }
        })
      }
      
      return ipAddresses
    }
    
    // IP 주소를 배열로 정규화하는 함수
    const normalizeIpAddresses = (ipAddresses) => {
      if (!ipAddresses) {
        return []
      }
      
      // 이미 배열인 경우
      if (Array.isArray(ipAddresses)) {
        return ipAddresses.filter(ip => ip && typeof ip === 'string')
      }
      
      // 문자열인 경우 파싱 시도
      if (typeof ipAddresses === 'string') {
        try {
          // JSON 문자열인 경우 파싱
          const parsed = JSON.parse(ipAddresses)
          if (Array.isArray(parsed)) {
            return parsed.filter(ip => ip && typeof ip === 'string')
          } else if (parsed && typeof parsed === 'object') {
            // { "ip": "..." } 형태인 경우
            if (parsed.ip) {
              return [parsed.ip]
            }
            // 객체의 값들을 배열로 변환
            const values = Object.values(parsed).filter(v => v && typeof v === 'string')
            return values.length > 0 ? values : []
          }
        } catch (e) {
          // JSON 파싱 실패 시 일반 문자열로 처리
          return [ipAddresses]
        }
      }
      
      return []
    }
    
    // MAC 주소를 배열로 정규화하는 함수
    const normalizeMacAddresses = (macAddresses) => {
      if (!macAddresses) {
        return []
      }
      
      // 이미 배열인 경우
      if (Array.isArray(macAddresses)) {
        return macAddresses.filter(mac => mac && typeof mac === 'string')
      }
      
      // 문자열인 경우 파싱 시도
      if (typeof macAddresses === 'string') {
        try {
          // JSON 문자열인 경우 파싱
          const parsed = JSON.parse(macAddresses)
          if (Array.isArray(parsed)) {
            return parsed.filter(mac => mac && typeof mac === 'string')
          } else if (parsed && typeof parsed === 'object') {
            // 객체의 값들을 배열로 변환
            const values = Object.values(parsed).filter(v => v && typeof v === 'string')
            return values.length > 0 ? values : []
          }
        } catch (e) {
          // JSON 파싱 실패 시 일반 문자열로 처리
          return [macAddresses]
        }
      }
      
      return []
    }
    
    // 첫 번째 MAC 주소를 안전하게 가져오는 함수
    const getFirstMacAddress = (machine) => {
      if (!machine || !machine.mac_addresses) {
        return ''
      }
      const normalized = normalizeMacAddresses(machine.mac_addresses)
      return normalized.length > 0 ? normalized[0] : ''
    }
    
    // 첫 번째 IP 주소를 안전하게 가져오는 함수
    const getFirstIpAddress = (machine) => {
      if (!machine || !machine.ip_addresses) {
        return ''
      }
      const normalized = normalizeIpAddresses(machine.ip_addresses)
      return normalized.length > 0 ? normalized[0] : ''
    }
    
    // MAC 주소로 fabric 이름을 찾는 함수
    const findFabricByMacAddress = (machine) => {
      if (!machine || !machine.interface_set || !Array.isArray(machine.interface_set)) {
        return '-'
      }
      
      // 머신의 첫 번째 MAC 주소 찾기
      const firstMac = machine.mac_addresses?.[0]
      if (!firstMac) {
        return '-'
      }
      
      // interface_set에서 해당 MAC 주소를 가진 인터페이스 찾기
      const matchingInterface = machine.interface_set.find(iface => {
        return iface.mac_address && iface.mac_address.toLowerCase() === firstMac.toLowerCase()
      })
      
      if (!matchingInterface || !matchingInterface.vlan) {
        return '-'
      }
      
      // 인터페이스의 vlan에서 fabric_id 가져오기
      const fabricId = matchingInterface.vlan.fabric_id
      if (fabricId === null || fabricId === undefined || fabricId === '') {
        return '-'
      }
      
      // globalFabricsMap에서 fabric 이름 찾기
      const fabricName = globalFabricsMap.value[fabricId] || 
                         globalFabricsMap.value[String(fabricId)] || 
                         globalFabricsMap.value[Number(fabricId)]
      
      return fabricName || '-'
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
      
      // 매핑에 있는 경우
      if (statusMap[status]) {
        return statusMap[status]
      }
      
      // 매핑에 없는 경우 (예: "failed deployment") - 각 단어의 첫 글자를 대문자로 변환
      if (typeof status === 'string') {
        return status.split(' ').map(word => 
          word.charAt(0).toUpperCase() + word.slice(1).toLowerCase()
        ).join(' ')
      }
      
      return status
    }
    
    const isStatusInProgress = (status) => {
      if (!status || typeof status !== 'string') {
        return false
      }
      // 상태가 "ing"로 끝나는지 확인 (예: "commissioning", "deploying")
      // 또는 "erasing"으로 끝나는지 확인 (예: "disk erasing")
      const normalizedStatus = status.toLowerCase().trim()
      return normalizedStatus.endsWith('ing') || normalizedStatus.endsWith('erasing')
    }
    
    // distro_series를 Ubuntu 버전으로 변환하는 함수
    const getUbuntuVersionFromDistroSeries = (distroSeries) => {
      const distroSeriesMap = {
        'xenial': '16.04 LTS',
        'bionic': '18.04 LTS',
        'focal': '20.04 LTS',
        'jammy': '22.04 LTS',
        'noble': '24.04 LTS'
      }
      
      if (!distroSeries || typeof distroSeries !== 'string') {
        return null
      }
      
      return distroSeriesMap[distroSeries.toLowerCase()] || null
    }
    
    // Deploying 상태일 때 OS 버전 표시 (배지 옆에 표시)
    const getDeployingOSVersion = (machine) => {
      if (!machine.deployingOS || !machine.deployingRelease) {
        return ''
      }
      const osName = machine.deployingOS.charAt(0).toUpperCase() + machine.deployingOS.slice(1)
      const version = formatVersionForDeploying(machine.deployingOS, machine.deployingRelease)
      return `${osName} ${version}`
    }
    
    // Status message를 가져오는 함수 (Deployed 상태일 때 OS 버전 표시)
    const getStatusMessage = (machine) => {
      const status = machine.status?.toLowerCase() || ''
      
      // Deployed 상태이고 Ubuntu인 경우 OS 버전 표시
      if (status === 'deployed' && machine.osystem?.toLowerCase() === 'ubuntu') {
        const ubuntuVersion = getUbuntuVersionFromDistroSeries(machine.distro_series)
        if (ubuntuVersion) {
          return `Ubuntu ${ubuntuVersion}`
        }
      }
      
      // 그 외의 경우 기존 status_message 반환
      return machine.status_message || ''
    }
    
    // Deploying 상태에서 표시할 버전 포맷팅 (숫자만 표시)
    const formatVersionForDeploying = (os, release) => {
      if (!os || !release) return release || ''
      
      // Ubuntu release to version mapping (숫자만)
      if (os.toLowerCase() === 'ubuntu') {
        const versionMap = {
          'xenial': '16.04',
          'bionic': '18.04',
          'focal': '20.04',
          'jammy': '22.04',
          'noble': '24.04'
        }
        return versionMap[release.toLowerCase()] || release
      }
      
      return release
    }
    
    // Block Devices에서 스토리지 계산하는 함수
    const calculateStorageFromBlockDevices = (blockDevices) => {
      if (!blockDevices || !Array.isArray(blockDevices)) {
        return 0
      }
      return blockDevices.reduce((total, device) => {
        return total + (device.size || 0)
      }, 0)
    }
    
    // 메모리 포맷팅 함수 (bytes 단위)
    const formatMemoryBytes = (bytes) => {
      if (!bytes || bytes === 0) return '0 B'
      const units = ['B', 'KB', 'MB', 'GB', 'TB']
      let size = bytes
      let unitIndex = 0
      while (size >= 1024 && unitIndex < units.length - 1) {
        size /= 1024
        unitIndex++
      }
      return `${size.toFixed(2)} ${units[unitIndex]}`
    }
    
    // 스토리지 포맷팅 함수 (bytes 단위) - 기존 formatStorage와 동일하지만 명확성을 위해 유지
    
    // 멈춰 있는 상태 목록 (진행 중 상태 제외)
    const availableStatusesForSelection = computed(() => {
      const allStatuses = new Set()
      filteredMachines.value.forEach(machine => {
        const status = machine.status?.toLowerCase() || ''
        if (status && !isStatusInProgress(status)) {
          allStatuses.add(status)
        }
      })
      // 상태를 정렬하여 반환
      return Array.from(allStatuses).sort()
    })
    
    // 상태의 표시 이름 반환
    const getStatusDisplayName = (status) => {
      const statusMap = {
        'new': 'New',
        'ready': 'Ready',
        'allocated': 'Allocated',
        'deployed': 'Deployed',
        'failed': 'Failed',
        'failed commissioning': 'Failed Commissioning',
        'failed deployment': 'Failed Deployment',
        'failed disk erasing': 'Failed Disk Erasing',
        'reserved': 'Reserved',
        'retired': 'Retired',
        'broken': 'Broken'
      }
      return statusMap[status.toLowerCase()] || status.charAt(0).toUpperCase() + status.slice(1)
    }
    
    // 특정 상태가 선택되어 있는지 확인
    const isStatusSelected = (status) => {
      return selectedStatusesForSelection.value.includes(status)
    }
    
    // selectAll 상태 업데이트
    const updateSelectAllState = () => {
      if (filteredMachines.value.length === 0) {
        selectAll.value = false
        return
      }
      const allSelected = filteredMachines.value.every(m => selectedMachines.value.includes(m.id))
      selectAll.value = allSelected
    }
    
    // 상태별 선택 토글
    const toggleSelectByStatus = (status) => {
      const index = selectedStatusesForSelection.value.indexOf(status)
      if (index > -1) {
        // 이미 선택된 상태면 해제
        selectedStatusesForSelection.value.splice(index, 1)
        // 해당 상태의 머신들 선택 해제
        const machinesToDeselect = filteredMachines.value
          .filter(m => (m.status?.toLowerCase() || '') === status)
          .map(m => m.id)
        selectedMachines.value = selectedMachines.value.filter(id => !machinesToDeselect.includes(id))
      } else {
        // 선택되지 않은 상태면 선택
        selectedStatusesForSelection.value.push(status)
        // 해당 상태의 머신들 선택
        const machinesToSelect = filteredMachines.value
          .filter(m => (m.status?.toLowerCase() || '') === status)
          .map(m => m.id)
        // 중복 제거하여 추가
        machinesToSelect.forEach(id => {
          if (!selectedMachines.value.includes(id)) {
            selectedMachines.value.push(id)
          }
        })
      }
      // selectAll 상태 업데이트
      updateSelectAllState()
    }
    
    // Status Select Menu 토글
    const toggleStatusSelectMenu = (event) => {
      if (openStatusSelectMenu.value) {
        openStatusSelectMenu.value = false
        statusSelectMenuPosition.value = { top: 0, left: 0 }
      } else {
        if (event && event.target) {
          const selectAllContainer = event.target.closest('.select-all-container')
          if (selectAllContainer) {
            const containerRect = selectAllContainer.getBoundingClientRect()
            if (containerRect) {
              statusSelectMenuPosition.value = {
                top: containerRect.bottom + window.scrollY + 4,
                left: containerRect.left + window.scrollX
              }
              openStatusSelectMenu.value = true
            } else {
              console.warn('Could not get select all container position')
              openStatusSelectMenu.value = false
            }
          } else {
            console.warn('Could not find select-all-container')
            openStatusSelectMenu.value = false
          }
        } else {
          console.warn('No event or target provided')
          openStatusSelectMenu.value = false
        }
      }
    }
    
    const toggleSelectAll = () => {
      if (selectAll.value) {
        selectedMachines.value = filteredMachines.value.map(m => m.id)
        // 모든 멈춰 있는 상태를 선택된 상태 목록에 추가
        availableStatusesForSelection.value.forEach(status => {
          if (!selectedStatusesForSelection.value.includes(status)) {
            selectedStatusesForSelection.value.push(status)
          }
        })
      } else {
        selectedMachines.value = []
        selectedStatusesForSelection.value = []
      }
    }
    
    // selectedMachines 변경 시 selectAll 상태 업데이트
    watch(selectedMachines, () => {
      updateSelectAllState()
    }, { deep: true })
    
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
        description: '',
        // IPMI fields
        powerDriver: 'LAN_2_0',
        powerBootType: 'auto',
        powerIpAddress: '',
        powerUser: '',
        powerPassword: '',
        powerKgBmcKey: '',
        cipherSuiteId: '3',
        privilegeLevel: 'OPERATOR',
        workaroundFlags: ['opensesspriv'],
        powerMac: ''
      }
      macAddressError.value = ''
    }
    
    // Watch powerType to auto-set commission to true when IPMI is selected
    watch(() => newMachine.value.powerType, (newPowerType) => {
      if (newPowerType === 'ipmi') {
        newMachine.value.commission = 'true'
      }
    })
    
    // Watch workaroundFlags to handle "None" option
    watch(() => newMachine.value.workaroundFlags, (newFlags, oldFlags) => {
      if (!oldFlags) return
      
      // If "None" (empty string) is selected, clear all other options
      if (newFlags.includes('')) {
        newMachine.value.workaroundFlags = ['']
      } 
      // If any other option is selected, remove "None"
      else if (oldFlags.includes('') && newFlags.length > 0) {
        newMachine.value.workaroundFlags = newFlags.filter(flag => flag !== '')
      }
    }, { deep: true })
    
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
        
        // Add IPMI parameters if power type is IPMI
        if (newMachine.value.powerType === 'ipmi') {
          // Default values are always included
          if (newMachine.value.powerDriver && newMachine.value.powerDriver.trim()) {
            formData.append('powerDriver', newMachine.value.powerDriver.trim())
          }
          if (newMachine.value.powerBootType && newMachine.value.powerBootType.trim()) {
            formData.append('powerBootType', newMachine.value.powerBootType.trim())
          }
          // Optional fields - only include if not empty
          if (newMachine.value.powerIpAddress && newMachine.value.powerIpAddress.trim()) {
            formData.append('powerIpAddress', newMachine.value.powerIpAddress.trim())
          }
          if (newMachine.value.powerUser && newMachine.value.powerUser.trim()) {
            formData.append('powerUser', newMachine.value.powerUser.trim())
          }
          if (newMachine.value.powerPassword && newMachine.value.powerPassword.trim()) {
            formData.append('powerPassword', newMachine.value.powerPassword.trim())
          }
          if (newMachine.value.powerKgBmcKey && newMachine.value.powerKgBmcKey.trim()) {
            formData.append('powerKgBmcKey', newMachine.value.powerKgBmcKey.trim())
          }
          if (newMachine.value.cipherSuiteId !== undefined && newMachine.value.cipherSuiteId !== null && String(newMachine.value.cipherSuiteId).trim()) {
            formData.append('cipherSuiteId', String(newMachine.value.cipherSuiteId).trim())
          }
          if (newMachine.value.privilegeLevel && newMachine.value.privilegeLevel.trim()) {
            formData.append('privilegeLevel', newMachine.value.privilegeLevel.trim())
          }
          if (newMachine.value.workaroundFlags && newMachine.value.workaroundFlags.length > 0) {
            // Workaround Flags는 배열이므로 쉼표로 구분된 문자열로 변환
            const flagsStr = newMachine.value.workaroundFlags.filter(f => f !== '' && f !== null && f !== undefined).join(',')
            if (flagsStr && flagsStr.trim()) {
              formData.append('workaroundFlags', flagsStr.trim())
            }
          }
          if (newMachine.value.powerMac && newMachine.value.powerMac.trim()) {
            formData.append('powerMac', newMachine.value.powerMac.trim())
          }
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
    
    // Failed Deployment 또는 Failed Disk Erasing 상태인지 확인하는 함수 (Release 버튼 표시)
    const isFailedDeployment = (status) => {
      if (!status || typeof status !== 'string') {
        return false
      }
      const normalizedStatus = status.toLowerCase().trim()
      // "failed deployment" 또는 "failed disk erasing" 상태
      return normalizedStatus === 'failed deployment' || 
             normalizedStatus.startsWith('failed deployment') ||
             normalizedStatus === 'failed disk erasing' ||
             normalizedStatus.startsWith('failed disk erasing')
    }
    
    // Commission 버튼의 클래스를 결정하는 함수
    const getCommissionButtonClass = (machine) => {
      const status = machine.status?.toLowerCase() || ''
      
      if (status === 'commissioning') {
        return 'btn-warning' // Abort 버튼 (주황색)
      } else if (status === 'deploying' || status === 'disk erasing') {
        return 'btn-secondary' // 회색 disabled 버튼
      } else if (status === 'ready' || status === 'allocated' || status === 'deployed') {
        return 'btn-success-light' // 연한 녹색 (재커미셔닝 가능)
      } else if (status.startsWith('failed') && !isFailedDeployment(status)) {
        // "failed" 상태는 일반 녹색 (단, "failed deployment"는 제외)
        return 'btn-success'
      } else {
        return 'btn-success' // 일반 녹색 (New 등)
      }
    }
    
    // Commission 버튼의 disabled 상태를 결정하는 함수
    const getCommissionButtonDisabled = (machine) => {
      const status = machine.status?.toLowerCase() || ''
      
      if (status === 'commissioning') {
        // Commissioning 중일 때는 Abort 버튼이므로 abortingMachines에 포함되어 있으면 disabled
        return abortingMachines.value.includes(machine.id)
      } else if (status === 'deploying' || status === 'disk erasing') {
        // Deploying 또는 Disk Erasing 상태일 때는 항상 disabled
        return true
      } else {
        // 그 외 상태에서는 canCommission 체크와 commissioningMachines 체크
        return !canCommission(machine) || commissioningMachines.value.includes(machine.id)
      }
    }
    
    // Commission 버튼 클릭 핸들러
    const handleCommissionButtonClick = (machine) => {
      const status = machine.status?.toLowerCase() || ''
      if (status === 'commissioning') {
        abortCommissioning(machine)
      } else {
        commissionMachine(machine)
      }
    }
    
    // Network 버튼의 클래스를 결정하는 함수
    const getNetworkButtonClass = (machine) => {
      const status = machine.status?.toLowerCase() || ''
      
      // 진행 중인 상태: 회색 disabled
      if (status === 'commissioning' || status === 'deploying' || status === 'disk erasing') {
        return 'btn-secondary'
      }
      
      // Ready, Allocated: 현재 그대로 (파란색)
      if (status === 'ready' || status === 'allocated') {
        return 'btn-primary'
      }
      
      // New, Failed xxx, Deployed: 연한 파랑
      if (status === 'new' || status === 'deployed' || status.startsWith('failed')) {
        return 'btn-primary-light'
      }
      
      // 기본값: 파란색
      return 'btn-primary'
    }
    
    // Network 버튼의 disabled 상태를 결정하는 함수
    const getNetworkButtonDisabled = (machine) => {
      const status = machine.status?.toLowerCase() || ''
      
      // 진행 중인 상태: disabled
      if (status === 'commissioning' || status === 'deploying' || status === 'disk erasing') {
        return true
      }
      
      // 그 외는 활성화
      return false
    }
    
    // Network 변경사항 저장 가능 여부를 결정하는 함수
    const canSaveNetworkChanges = (machine) => {
      if (!machine) return false
      
      const status = machine.status?.toLowerCase() || ''
      
      // New, Failed xxx, Deployed 상태에서는 저장 불가
      if (status === 'new' || status === 'deployed' || status.startsWith('failed')) {
        return false
      }
      
      // Ready, Allocated 상태에서만 저장 가능
      return status === 'ready' || status === 'allocated'
    }
    
    // Polling removed - will be replaced with WebSocket implementation
    
    const commissionMachine = async (machine) => {
      if (!canCommission(machine)) {
        return
      }
      
      // Ready, Allocated, Deployed 상태일 때 확인 메시지 표시
      if (machine.status === 'ready' || machine.status === 'allocated' || machine.status === 'deployed') {
        let statusText = 'Ready'
        if (machine.status === 'allocated') {
          statusText = 'Allocated'
        } else if (machine.status === 'deployed') {
          statusText = 'Deployed'
        }
        const confirmMessage = `이 머신은 이미 Commissioning이 완료되어 ${statusText} 상태입니다.\n\n정말로 다시 Commissioning을 진행하시겠습니까?`
        const confirmed = await customConfirm(confirmMessage, '재커미셔닝 확인')
        if (!confirmed) {
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
          // Update status immediately to 'new' (WebSocket will update later if needed)
          const machineIndex = machines.value.findIndex(m => m.id === machine.id)
          if (machineIndex !== -1) {
            machines.value[machineIndex].status = 'new'
            machines.value[machineIndex].status_message = ''
          }
        } else {
          error.value = response.data?.error || 'Failed to abort commissioning'
        }
        
      } catch (err) {
        console.error('Error aborting commissioning:', err)
        // 503 Service Unavailable 또는 타임아웃 에러인 경우
        const statusCode = err.response?.status
        let errorMessage = err.response?.data?.error || err.message || 'Failed to abort commissioning'
        
        if (statusCode === 503 || errorMessage.includes('timeout') || errorMessage.includes('30000') || errorMessage.includes('60000') || errorMessage.includes('Service Unavailable')) {
          errorMessage = 'Abort operation may have timed out, but the operation might still be in progress. Refreshing machine status...'
          // 503 에러나 타임아웃의 경우, 실제로는 성공했을 수 있으므로 일정 시간 후 상태 확인
          setTimeout(() => {
            loadMachines()
          }, 2000)
        }
        error.value = errorMessage
      } finally {
        // Remove from aborting list
        const index = abortingMachines.value.indexOf(machine.id)
        if (index > -1) {
          abortingMachines.value.splice(index, 1)
        }
      }
    }
    
    // Release Machine
    const releaseMachine = async (machine, skipConfirm = false) => {
      // 확인 메시지 표시 (skipConfirm이 true이면 건너뛰기 - 일괄 작업에서 이미 확인했을 경우)
      if (!skipConfirm) {
        const machineName = machine.hostname || `Machine ${machine.id}`
        const confirmMessage = `머신 "${machineName}"을(를) Release 하시겠습니까?\n\n이 작업은 되돌릴 수 없습니다.`
        const confirmed = await customConfirm(confirmMessage, 'Release 확인', 'Release')
        if (!confirmed) {
          return
        }
      }
      
      releasingMachines.value.push(machine.id)
      
      try {
        const apiParams = settingsStore.getApiParams.value
        const response = await axios.post(`http://localhost:8081/api/machines/${machine.id}/release`, null, {
          params: {
            maasUrl: apiParams.maasUrl,
            apiKey: apiParams.apiKey
          }
        })
        
        if (response.data && response.data.success) {
          console.log('Machine released successfully:', response.data)
          // Update only the specific machine's status instead of reloading all machines
          const machineIndex = machines.value.findIndex(m => m.id === machine.id)
          if (machineIndex !== -1) {
            // Update status - release 후 상태는 WebSocket으로 업데이트됨
            // Status will be updated via WebSocket
          }
        } else {
          error.value = response.data?.error || 'Failed to release machine'
        }
        
      } catch (err) {
        console.error('Error releasing machine:', err)
        error.value = err.response?.data?.error || err.message || 'Failed to release machine'
      } finally {
        // Remove from releasing list
        const index = releasingMachines.value.indexOf(machine.id)
        if (index > -1) {
          releasingMachines.value.splice(index, 1)
        }
      }
    }
    
    // Deploy 버튼의 클래스를 결정하는 함수
    const getDeployButtonClass = (machine) => {
      const status = machine.status?.toLowerCase() || ''
      
      if (status === 'deploying') {
        return 'btn-warning' // Abort 버튼 (주황색)
      } else if (status === 'deployed') {
        return 'btn-secondary' // 회색 disabled 버튼
      } else if (status === 'ready' || status === 'allocated') {
        return 'btn-deploy' // 옅은 분홍색 (활성화)
      } else {
        return 'btn-secondary' // 회색 disabled (New, Commissioning, Failed 등)
      }
    }
    
    // Deploy 버튼의 disabled 상태를 결정하는 함수
    const getDeployButtonDisabled = (machine) => {
      const status = machine.status?.toLowerCase() || ''
      
      if (status === 'deploying') {
        // Deploying 중일 때는 Abort 버튼이므로 abortingDeployMachines에 포함되어 있으면 disabled
        return abortingDeployMachines.value.includes(machine.id)
      } else if (status === 'deployed') {
        // Deployed 상태일 때는 항상 disabled
        return true
      } else if (status === 'ready' || status === 'allocated') {
        // Ready, Allocated 상태에서는 deployingMachines 체크
        return deployingMachines.value.includes(machine.id)
      } else {
        // New, Commissioning, Failed 등은 항상 disabled
        return true
      }
    }
    
    // Deploy 버튼 클릭 핸들러
    const handleDeployButtonClick = (machine, event) => {
      console.log('Deploy button clicked for machine:', machine.id, machine.status)
      const status = machine.status?.toLowerCase() || ''
      if (status === 'deploying') {
        abortDeploy(machine)
      } else if (status === 'ready' || status === 'allocated') {
        // Deploy 모달 표시
        if (event) {
          event.stopPropagation()
        }
        showDeployModal(machine)
      }
    }
    
    // Load Cloud-Config Templates from localStorage
    const loadCloudConfigTemplates = () => {
      try {
        const stored = localStorage.getItem('maas-cloud-config-templates')
        if (stored) {
          const templates = JSON.parse(stored)
          // Migrate old format (tag -> tags)
          cloudConfigTemplates.value = templates.map(template => {
            if (template.tag && !template.tags) {
              // Old format: single tag
              return {
                ...template,
                tags: template.tag ? [template.tag] : []
              }
            } else if (!template.tags) {
              // No tags at all
              return {
                ...template,
                tags: []
              }
            }
            return template
          })
          // Save migrated data back to localStorage
          localStorage.setItem('maas-cloud-config-templates', JSON.stringify(cloudConfigTemplates.value))
        } else {
          cloudConfigTemplates.value = []
        }
      } catch (err) {
        console.error('Error loading cloud-config templates:', err)
        cloudConfigTemplates.value = []
      }
    }
    
    // Get selected template's cloud-config
    const getSelectedTemplateCloudConfig = computed(() => {
      if (selectedCloudConfigTemplate.value === 'none' || selectedCloudConfigTemplate.value === 'custom') {
        return null
      }
      const template = cloudConfigTemplates.value.find(t => t.id === selectedCloudConfigTemplate.value)
      return template ? template.cloudConfig : null
    })
    
    // Get templates that match machine tags
    const matchedTemplates = computed(() => {
      if (!selectedDeployMachine.value || !selectedDeployMachine.value.tags || selectedDeployMachine.value.tags.length === 0) {
        return []
      }
      
      const machineTags = selectedDeployMachine.value.tags.map(tag => tag.toLowerCase())
      
      return cloudConfigTemplates.value.filter(template => {
        if (!template.tags || template.tags.length === 0) {
          return false
        }
        // Check if any template tag matches any machine tag
        return template.tags.some(tag => machineTags.includes(tag.toLowerCase()))
      })
    })
    
    // Get templates that don't match machine tags
    const otherTemplates = computed(() => {
      const matchedIds = new Set(matchedTemplates.value.map(t => t.id))
      return cloudConfigTemplates.value.filter(template => !matchedIds.has(template.id))
    })
    
    // Deploy Modal Functions
    const showDeployModal = async (machine) => {
      selectedDeployMachine.value = machine
      showDeployModalState.value = true
      selectedDeployOS.value = null
      selectedCloudConfigTemplate.value = 'none'
      customCloudConfig.value = ''
      deployingMachine.value = false
      
      // Load cloud-config templates
      loadCloudConfigTemplates()
      
      // OS 목록 로드
      if (deployableOSList.value.length === 0 && !loadingDeployableOS.value) {
        console.log('Loading deployable OS list...')
        await loadDeployableOS()
      }
      
      // 기본 OS 선택
      if (deployableOSList.value.length > 0 && !selectedDeployOS.value) {
        const defaultOS = deployableOSList.value.find(o => o.isDefault)
        if (defaultOS) {
          selectedDeployOS.value = defaultOS
        } else {
          selectedDeployOS.value = deployableOSList.value[0]
        }
      }
    }
    
    const closeDeployModal = () => {
      if (deployingMachine.value) return // 배포 중이면 닫기 방지
      showDeployModalState.value = false
      selectedDeployMachine.value = null
      selectedDeployOS.value = null
      selectedCloudConfigTemplate.value = 'none'
      customCloudConfig.value = ''
    }
    
    const startDeployFromModal = async () => {
      if (!selectedDeployMachine.value || !selectedDeployOS.value) {
        return
      }
      
      deployingMachine.value = true
      try {
        await deployMachine(selectedDeployMachine.value, selectedDeployOS.value)
        // 배포 시작 성공 시 모달 닫기
        closeDeployModal()
      } catch (err) {
        console.error('Error starting deploy from modal:', err)
        // 에러 발생 시 모달은 열어두고 에러 메시지 표시
      } finally {
        deployingMachine.value = false
      }
    }
    
    // Deployable OS 목록 로드
    const loadDeployableOS = async () => {
      if (loadingDeployableOS.value) return
      
      loadingDeployableOS.value = true
      try {
        const settings = settingsStore.settings
        if (!settings.maasUrl || !settings.apiKey) {
          console.warn('MAAS URL and API Key must be configured')
          return
        }
        
        const response = await axios.get('http://localhost:8081/api/deployable-os', {
          params: {
            maasUrl: settings.maasUrl,
            apiKey: settings.apiKey
          }
        })
        
        if (response.data.results) {
          // OS 목록 정렬 및 기본값 설정
          const osList = response.data.results.map(os => ({
            ...os,
            sortKey: getOSSortKey(os.os, os.release)
          }))
          
          // 정렬: OS 이름 -> release (최신순)
          osList.sort((a, b) => {
            if (a.os !== b.os) {
              return a.os.localeCompare(b.os)
            }
            return b.sortKey - a.sortKey
          })
          
          // 각 OS별로 최신 release를 기본값으로 설정
          const osMap = new Map()
          osList.forEach(os => {
            const key = os.os
            if (!osMap.has(key) || osMap.get(key).sortKey < os.sortKey) {
              osMap.set(key, os)
            }
          })
          
          // 기본값 표시
          deployableOSList.value = osList.map(os => ({
            ...os,
            isDefault: osMap.get(os.os) === os
          }))
        } else {
          deployableOSList.value = []
        }
      } catch (err) {
        console.error('Error loading deployable OS:', err)
        deployableOSList.value = []
      } finally {
        loadingDeployableOS.value = false
      }
    }
    
    // OS 이름 포맷팅
    const formatOSName = (os, release) => {
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
        const version = versionMap[release.toLowerCase()] || release
        return `Ubuntu ${version}`
      }
      
      // Capitalize first letter of OS
      const osName = os.charAt(0).toUpperCase() + os.slice(1)
      return `${osName} ${release}`
    }
    
    // OS 정렬 키 생성 (release 기준)
    const getOSSortKey = (os, release) => {
      if (os.toLowerCase() === 'ubuntu') {
        const versionMap = {
          'xenial': 16,
          'bionic': 18,
          'focal': 20,
          'jammy': 22,
          'noble': 24
        }
        return versionMap[release.toLowerCase()] || 0
      }
      // 다른 OS는 release 문자열을 숫자로 변환 시도
      const match = release.match(/(\d+)/)
      return match ? parseInt(match[1]) : 0
    }
    
    // OS 선택 및 배포
    const selectDeployOS = (machine, os) => {
      openDeployMenu.value = null
      deployMenuPosition.value = { top: 0, left: 0 }
      deployMachine(machine, os)
    }
    
    // Deploy 중단 함수
    const abortDeploy = async (machine) => {
      if (machine.status !== 'deploying') {
        return
      }
      
      abortingDeployMachines.value.push(machine.id)
      
      try {
        const apiParams = settingsStore.getApiParams.value
        const response = await axios.post(`http://localhost:8081/api/machines/${machine.id}/abort`, null, {
          params: {
            maasUrl: apiParams.maasUrl,
            apiKey: apiParams.apiKey
          }
        })
        
        if (response.data && response.data.success) {
          console.log('Machine deployment aborted successfully:', response.data)
          // Update status immediately to 'ready' (WebSocket will update later if needed)
          const machineIndex = machines.value.findIndex(m => m.id === machine.id)
          if (machineIndex !== -1) {
            machines.value[machineIndex].status = 'ready'
            machines.value[machineIndex].status_message = ''
          }
        } else {
          error.value = response.data?.error || 'Failed to abort deployment'
        }
        
      } catch (err) {
        console.error('Error aborting deployment:', err)
        // 503 Service Unavailable 또는 타임아웃 에러인 경우
        const statusCode = err.response?.status
        let errorMessage = err.response?.data?.error || err.message || 'Failed to abort deployment'
        
        if (statusCode === 503 || errorMessage.includes('timeout') || errorMessage.includes('30000') || errorMessage.includes('60000') || errorMessage.includes('Service Unavailable')) {
          errorMessage = 'Abort operation may have timed out, but the operation might still be in progress. Refreshing machine status...'
          // 503 에러나 타임아웃의 경우, 실제로는 성공했을 수 있으므로 일정 시간 후 상태 확인
          setTimeout(() => {
            loadMachines()
          }, 2000)
        }
        error.value = errorMessage
      } finally {
        // Remove from aborting list
        const index = abortingDeployMachines.value.indexOf(machine.id)
        if (index > -1) {
          abortingDeployMachines.value.splice(index, 1)
        }
      }
    }
    
    // 통합 Abort 함수 (Commissioning과 Deploying 모두 처리)
    const abortMachine = async (machine) => {
      const status = machine.status?.toLowerCase() || ''
      
      // Failed deployment 상태에서는 Abort 대신 Release를 사용
      if (isFailedDeployment(status)) {
        console.log('Failed deployment detected, using Release instead of Abort')
        await releaseMachine(machine, true) // skipConfirm = true
        return
      }
      
      if (status === 'commissioning') {
        await abortCommissioning(machine)
      } else if (status === 'deploying') {
        await abortDeploy(machine)
      } else {
        console.warn(`Cannot abort machine in status: ${status}`)
        error.value = `Cannot abort machine in ${status} state. Please use Release instead.`
      }
    }
    
    // Action Bar Functions
    const toggleActionsMenu = (event) => {
      console.log('toggleActionsMenu called', { event, currentState: openActionsMenu.value, buttonRef: actionsMenuButton.value })
      
      if (openActionsMenu.value) {
        openActionsMenu.value = false
        actionsMenuPosition.value = { top: 0, left: 0 }
      } else {
        // Power Action 메뉴가 열려있으면 닫기
        if (openPowerActionMenu.value) {
          openPowerActionMenu.value = false
          powerActionMenuPosition.value = { top: 0, left: 0 }
        }
        
        // nextTick을 사용하여 DOM이 완전히 렌더링된 후 위치 계산
        nextTick(() => {
          // ref를 사용하여 요소 찾기
          const targetElement = actionsMenuButton.value || 
                                (event && event.currentTarget) || 
                                (event && event.target && event.target.closest('.action-bar-item'))
          
          console.log('Target element found:', targetElement)
          
          if (targetElement) {
            const containerRect = targetElement.getBoundingClientRect()
            console.log('Container rect:', containerRect, 'window.scrollY:', window.scrollY, 'window.scrollX:', window.scrollX)
            
            if (containerRect && containerRect.width > 0 && containerRect.height > 0) {
              // position: fixed를 사용하므로 viewport 기준 좌표 사용 (스크롤 오프셋 없음)
              actionsMenuPosition.value = {
                top: containerRect.bottom + 4,
                left: containerRect.left
              }
              console.log('Menu position set:', actionsMenuPosition.value)
              // 위치 설정 후 메뉴 열기
              openActionsMenu.value = true
            } else {
              console.warn('Invalid container rect:', containerRect)
              openActionsMenu.value = false
            }
          } else {
            console.warn('Could not find action-bar-item')
            openActionsMenu.value = false
          }
        })
      }
    }
    
    const togglePowerActionMenu = (event) => {
      if (openPowerActionMenu.value) {
        openPowerActionMenu.value = false
        powerActionMenuPosition.value = { top: 0, left: 0 }
      } else {
        // Actions 메뉴가 열려있으면 닫기
        if (openActionsMenu.value) {
          openActionsMenu.value = false
          actionsMenuPosition.value = { top: 0, left: 0 }
        }
        
        // nextTick을 사용하여 DOM이 완전히 렌더링된 후 위치 계산
        nextTick(() => {
          // ref를 사용하여 요소 찾기
          const targetElement = powerActionMenuButton.value || 
                                (event && event.currentTarget) || 
                                (event && event.target && event.target.closest('.action-bar-item'))
          
          if (targetElement) {
            const containerRect = targetElement.getBoundingClientRect()
            
            if (containerRect && containerRect.width > 0 && containerRect.height > 0) {
              // position: fixed를 사용하므로 viewport 기준 좌표 사용 (스크롤 오프셋 없음)
              powerActionMenuPosition.value = {
                top: containerRect.bottom + 4,
                left: containerRect.left
              }
              // 위치 설정 후 메뉴 열기
              openPowerActionMenu.value = true
            } else {
              console.warn('Invalid container rect:', containerRect)
              openPowerActionMenu.value = false
            }
          } else {
            console.warn('Could not find power action-bar-item')
            openPowerActionMenu.value = false
          }
        })
      }
    }
    
    // 선택된 머신들 가져오기
    const getSelectedMachines = () => {
      return machines.value.filter(m => selectedMachines.value.includes(m.id))
    }
    
    // 각 머신의 가능한 액션 목록을 반환하는 함수
    const getAvailableActions = (machine) => {
      const status = machine.status?.toLowerCase() || ''
      const actions = []
      
      if (status === 'new') {
        actions.push('commission')
      } else if (status === 'commissioning') {
        actions.push('abort')
      } else if (status === 'ready') {
        actions.push('commission', 'deploy')
      } else if (status === 'allocated') {
        actions.push('commission', 'deploy', 'release')
      } else if (status === 'deployed') {
        actions.push('release')
      } else if (isFailedDeployment(status)) {
        actions.push('release')
      } else if (status === 'deploying') {
        actions.push('abort')
      } else if (status.startsWith('failed')) {
        // 기타 failed 상태는 commission 가능
        actions.push('commission')
      }
      
      return actions
    }
    
    // 일괄 작업 가능 여부 확인 함수들 (교집합 로직)
    const canBulkCommission = () => {
      const selected = getSelectedMachines()
      if (selected.length === 0) return false
      // 모든 선택된 머신이 commission 가능해야 함
      return selected.every(m => getAvailableActions(m).includes('commission'))
    }
    
    const canBulkAllocate = () => {
      // Allocate는 항상 false (API 미구현)
      return false
    }
    
    const canBulkDeploy = () => {
      const selected = getSelectedMachines()
      if (selected.length === 0) return false
      // 모든 선택된 머신이 deploy 가능해야 함
      return selected.every(m => getAvailableActions(m).includes('deploy'))
    }
    
    const canBulkRelease = () => {
      const selected = getSelectedMachines()
      if (selected.length === 0) return false
      // 모든 선택된 머신이 release 가능해야 함
      return selected.every(m => getAvailableActions(m).includes('release'))
    }
    
    const canBulkAbort = () => {
      const selected = getSelectedMachines()
      if (selected.length === 0) return false
      // 모든 선택된 머신이 abort 가능해야 함
      return selected.every(m => getAvailableActions(m).includes('abort'))
    }
    
    // Power 상태 확인 함수들
    const canBulkPowerOn = () => {
      const selected = getSelectedMachines()
      if (selected.length === 0) return false
      // 모든 선택된 머신이 off 상태여야 Turn on 가능
      // power_state가 'on'이 아닌 모든 경우를 off로 간주
      return selected.every(m => m.power_state !== 'on')
    }
    
    const canBulkPowerOff = () => {
      const selected = getSelectedMachines()
      if (selected.length === 0) return false
      // 모든 선택된 머신이 on 상태여야 Turn off 가능
      return selected.every(m => m.power_state === 'on')
    }
    
    // 일괄 Power 작업 핸들러
    const handleBulkPowerAction = async (action) => {
      openPowerActionMenu.value = false
      const selected = getSelectedMachines()
      
      if (selected.length === 0) {
        return
      }
      
      // 확인 메시지
      const actionText = action === 'on' ? '켜기' : '끄기'
      const confirmMessage = `선택된 ${selected.length}개의 머신의 전원을 ${actionText} 하시겠습니까?`
      
      const confirmed = await customConfirm(confirmMessage, '일괄 Power 작업 확인')
      if (!confirmed) {
        return
      }
      
      // 각 머신에 대해 Power 작업 수행
      const results = []
      for (const machine of selected) {
        try {
          const apiParams = settingsStore.getApiParams.value
          const endpoint = action === 'on' 
            ? `http://localhost:8081/api/machines/${machine.id}/power-on`
            : `http://localhost:8081/api/machines/${machine.id}/power-off`
          
          const response = await axios.post(endpoint, null, {
            params: {
              maasUrl: apiParams.maasUrl,
              apiKey: apiParams.apiKey
            }
          })
          
          if (response.data && response.data.success) {
            results.push({ machine: machine.id, success: true })
          } else {
            results.push({ 
              machine: machine.id, 
              success: false, 
              error: response.data?.error || `Failed to power ${action}` 
            })
          }
        } catch (err) {
          console.error(`Failed to power ${action} machine ${machine.id}:`, err)
          results.push({ 
            machine: machine.id, 
            success: false, 
            error: err.response?.data?.error || err.message || `Failed to power ${action}` 
          })
        }
      }
      
      // 결과 요약
      const successCount = results.filter(r => r.success).length
      const failCount = results.length - successCount
      
      if (failCount > 0) {
        error.value = `Power ${action}: ${successCount}개 성공, ${failCount}개 실패`
      } else {
        console.log(`All ${selected.length} machines powered ${action} successfully`)
      }
      
      // 2초 후 머신 목록 새로고침
      setTimeout(() => {
        loadMachines()
      }, 2000)
    }
    
    // 일괄 작업 핸들러
    const handleBulkAction = async (action) => {
      openActionsMenu.value = false
      const selected = getSelectedMachines()
      
      if (selected.length === 0) {
        return
      }
      
      // 확인 메시지
      let confirmMessage = ''
      switch (action) {
        case 'commission':
          confirmMessage = `선택된 ${selected.length}개의 머신을 Commissioning 하시겠습니까?`
          break
        case 'allocate':
          confirmMessage = `선택된 ${selected.length}개의 머신을 Allocate 하시겠습니까?`
          break
        case 'deploy':
          confirmMessage = `선택된 ${selected.length}개의 머신을 Deploy 하시겠습니까?`
          break
        case 'release':
          confirmMessage = `선택된 ${selected.length}개의 머신을 Release 하시겠습니까?\n\n이 작업은 되돌릴 수 없습니다.`
          break
        case 'abort':
          confirmMessage = `선택된 ${selected.length}개의 머신의 작업을 Abort 하시겠습니까?`
          break
      }
      
      const confirmed = await customConfirm(confirmMessage, '일괄 작업 확인')
      if (!confirmed) {
        return
      }
      
      // 각 머신에 대해 작업 수행
      for (const machine of selected) {
        try {
          switch (action) {
            case 'commission':
              if (canCommission(machine)) {
                await commissionMachine(machine)
              }
              break
            case 'allocate':
              // Allocate는 API 미구현이므로 아무것도 하지 않음
              console.log('Allocate is not implemented yet')
              break
            case 'deploy':
              const deployStatus = machine.status?.toLowerCase() || ''
              if (deployStatus === 'ready' || deployStatus === 'allocated') {
                await deployMachine(machine)
              }
              break
            case 'release':
              const releaseStatus = machine.status?.toLowerCase() || ''
              if (releaseStatus === 'failed deployment' || releaseStatus === 'failed disk erasing' || releaseStatus === 'deployed' || releaseStatus === 'allocated') {
                // 일괄 작업에서는 이미 확인했으므로 skipConfirm = true
                await releaseMachine(machine, true)
              }
              break
            case 'abort':
              await abortMachine(machine)
              break
          }
        } catch (err) {
          console.error(`Error performing ${action} on machine ${machine.id}:`, err)
        }
      }
    }
    
    const handleBulkDelete = async () => {
      console.log('handleBulkDelete called')
      const selected = getSelectedMachines()
      console.log('Selected machines:', selected)
      
      if (selected.length === 0) {
        console.warn('No machines selected for deletion')
        await customAlert('삭제할 머신을 선택해주세요.', '알림')
        return
      }
      
      const confirmMessage = `선택된 ${selected.length}개의 머신을 삭제하시겠습니까?\n\n이 작업은 되돌릴 수 없습니다.`
      const confirmed = await customConfirm(confirmMessage, '삭제 확인', '삭제')
      if (!confirmed) {
        console.log('Delete cancelled by user')
        return
      }
      
      console.log('Starting deletion of', selected.length, 'machine(s)')
      
      const apiParams = settingsStore.getApiParams.value
      const deletePromises = selected.map(async (machine) => {
        try {
          const response = await axios.delete(`http://localhost:8081/api/machines/${machine.id}`, {
            params: {
              maasUrl: apiParams.maasUrl,
              apiKey: apiParams.apiKey
            }
          })
          
          console.log(`Delete response for machine ${machine.id}:`, response.status, response.data)
          
          // HTTP 상태 코드가 2xx이고, 응답 데이터가 있으면 success 확인
          if (response.status >= 200 && response.status < 300) {
            if (response.data && response.data.success !== false) {
              console.log(`Machine ${machine.id} deleted successfully:`, response.data)
              return { success: true, machineId: machine.id }
            } else if (response.data && response.data.success === false) {
              console.error(`Failed to delete machine ${machine.id}:`, response.data?.error)
              return { success: false, machineId: machine.id, error: response.data?.error || 'Unknown error' }
            } else {
              // 응답 본문이 없거나 success 필드가 없어도 2xx 상태 코드면 성공으로 간주
              console.log(`Machine ${machine.id} deleted successfully (no response body)`)
              return { success: true, machineId: machine.id }
            }
          } else {
            console.error(`Failed to delete machine ${machine.id}: HTTP ${response.status}`)
            return { success: false, machineId: machine.id, error: `HTTP ${response.status}` }
          }
        } catch (err) {
          console.error(`Error deleting machine ${machine.id}:`, err)
          // HTTP 상태 코드가 2xx인 경우에도 성공으로 간주 (DELETE는 응답 본문이 없을 수 있음)
          if (err.response && err.response.status >= 200 && err.response.status < 300) {
            console.log(`Machine ${machine.id} deleted successfully (status code: ${err.response.status})`)
            return { success: true, machineId: machine.id }
          }
          return { 
            success: false, 
            machineId: machine.id, 
            error: err.response?.data?.error || err.message || 'Failed to delete machine' 
          }
        }
      })
      
      const results = await Promise.all(deletePromises)
      const successCount = results.filter(r => r.success).length
      const failCount = results.filter(r => !r.success).length
      
      if (failCount > 0) {
        const failedMachines = results.filter(r => !r.success)
        const errorMessages = failedMachines.map(r => `Machine ${r.machineId}: ${r.error}`).join('\n')
        await customAlert(`일부 머신 삭제 실패 (${successCount}/${selected.length} 성공):\n${errorMessages}`, '삭제 실패')
      } else {
        console.log(`Successfully deleted ${successCount} machine(s)`)
      }
      
      // 삭제 후 잠시 대기 후 머신 목록 다시 로드 (서버 처리 시간 고려)
      await new Promise(resolve => setTimeout(resolve, 500))
      await loadMachines()
      
      // 선택 해제
      selectedMachines.value = []
    }
    
    // Deploy Machine
    const deployMachine = async (machine, os = null) => {
      if (machine.status !== 'ready' && machine.status !== 'allocated') {
        return
      }
      
      // OS가 선택되지 않았고 목록이 비어있으면 로드
      if (!os && deployableOSList.value.length === 0 && !loadingDeployableOS.value) {
        await loadDeployableOS()
        // 기본 OS 선택
        const defaultOS = deployableOSList.value.find(o => o.isDefault)
        if (defaultOS) {
          os = defaultOS
        } else if (deployableOSList.value.length > 0) {
          os = deployableOSList.value[0]
        }
      }
      
      // OS가 여전히 없으면 기본값 사용
      if (!os && deployableOSList.value.length > 0) {
        const defaultOS = deployableOSList.value.find(o => o.isDefault)
        os = defaultOS || deployableOSList.value[0]
      }
      
      deployingMachines.value.push(machine.id)
      
      try {
        const apiParams = settingsStore.getApiParams.value
        const params = {
          maasUrl: apiParams.maasUrl,
          apiKey: apiParams.apiKey
        }
        
        // OS 파라미터 추가
        if (os) {
          params.os = os.os
          params.release = os.release
          if (os.arches && os.arches.length > 0) {
            params.arch = os.arches[0] // 첫 번째 architecture 사용
          }
        }
        
        const response = await axios.post(`http://localhost:8081/api/machines/${machine.id}/deploy`, null, {
          params: params
        })
        
        if (response.data && response.data.success) {
          console.log('Machine deployed successfully:', response.data)
          // Update only the specific machine's status instead of reloading all machines
          const machineIndex = machines.value.findIndex(m => m.id === machine.id)
          if (machineIndex !== -1) {
            // Update status to deploying
            machines.value[machineIndex].status = 'deploying'
            // 배포 중인 OS 정보 저장
            if (os) {
              machines.value[machineIndex].deployingOS = os.os
              machines.value[machineIndex].deployingRelease = os.release
            }
            machines.value[machineIndex].status_message = 'Starting deployment...'
            // Status will be updated via WebSocket
          }
        } else {
          error.value = response.data?.error || 'Failed to deploy machine'
        }
        
      } catch (err) {
        console.error('Error deploying machine:', err)
        error.value = err.response?.data?.error || err.message || 'Failed to deploy machine'
      } finally {
        // Remove from deploying list
        const index = deployingMachines.value.indexOf(machine.id)
        if (index > -1) {
          deployingMachines.value.splice(index, 1)
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
            
            // 원래 fabric ID 저장 (fabric 변경 시 unlink에 사용)
            const originalFabricId = iface.vlan?.fabric_id !== null && iface.vlan?.fabric_id !== undefined ? 
              (typeof iface.vlan.fabric_id === 'string' ? parseInt(iface.vlan.fabric_id, 10) : iface.vlan.fabric_id) : 
              null
            
            return {
              ...iface,
              editableFabric: fabricId !== null && fabricId !== undefined && fabricId !== '' ? Number(fabricId) : null,
              originalFabricId: originalFabricId, // 원본 Fabric ID 저장 (fabric 변경 시 unlink에 사용)
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
      
      // Disconnect 선택 시 (-1)
      if (fabricId === -1) {
        networkInterface.matchedSubnet = null
        // IP Assignment를 unconfigured로 변경
        networkInterface.ipAssignment = 'unconfigured'
        networkInterface.primaryIpAddress = ''
        return
      }
      
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
      // Reset modal position when closing
      networkModalPosition.value = { top: 0, left: 0 }
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
            const originalFabricId = networkInterface.originalFabricId
            const newFabricId = networkInterface.editableFabric
            
            // Disconnect 선택 시 (-1): VLAN 삭제 (vlan=""로 설정)
            if (newFabricId === -1) {
              console.log(`[Save Network] Disconnect selected for interface ${interfaceName}, removing VLAN`)
              
              try {
                // VLAN을 빈 문자열로 설정하여 삭제
                const vlanResponse = await axios.put(
                  `http://localhost:8081/api/machines/${machineId}/interfaces/${interfaceIdStr}/vlan`,
                  null,
                  {
                    params: {
                      maasUrl: apiParams.maasUrl,
                      apiKey: apiParams.apiKey,
                      vlanId: '' // 빈 문자열로 VLAN 삭제
                    }
                  }
                )
                
                if (!vlanResponse.data || !vlanResponse.data.success) {
                  const errorMessage = vlanResponse.data?.error || 'Unknown error'
                  console.error(`[Save Network] Failed to remove VLAN for Disconnect:`, errorMessage)
                  throw new Error(`Failed to remove VLAN for interface ${interfaceName}: ${errorMessage}`)
                }
                
                console.log(`[Save Network] VLAN removed successfully for Disconnect on interface ${interfaceName}`)
              } catch (err) {
                console.error(`[Save Network] Error removing VLAN for Disconnect:`, err)
                throw err
              }
              
              // Disconnect 처리 완료, 다음 인터페이스로
              continue
            }
            
            // Fabric이 변경되었는지 확인
            const fabricChangedFlag = originalFabricId !== null && 
                                     newFabricId !== null && 
                                     newFabricId !== undefined && 
                                     newFabricId !== '' &&
                                     newFabricId !== -1 &&
                                     (Number(originalFabricId) !== Number(newFabricId))
            
            if (fabricChangedFlag || (originalFabricId === null && newFabricId !== null && newFabricId !== undefined && newFabricId !== '' && newFabricId !== -1)) {
              console.log(`[Save Network] Fabric changed for interface ${interfaceName}: originalFabricId=${originalFabricId}, newFabricId=${newFabricId}`)
              
              // Fabric이 변경된 경우, 먼저 원래 fabric의 link를 unlink해야 함
              // 단, unlink가 실패해도 Fabric 변경은 계속 진행 (이미 Fabric이 변경되었을 수 있음)
              if (originalFabricId !== null && networkInterface.originalPrimaryLinkId) {
                console.log(`[Save Network] Unlinking Primary link with original fabric ID ${originalFabricId} for interface ${interfaceName} (linkId: ${networkInterface.originalPrimaryLinkId})`)
                
                try {
                  const unlinkResponse = await axios.post(
                    `http://localhost:8081/api/machines/${machineId}/interfaces/${interfaceIdStr}/unlink-subnet`,
                    null,
                    {
                      params: {
                        maasUrl: apiParams.maasUrl,
                        apiKey: apiParams.apiKey,
                        linkId: networkInterface.originalPrimaryLinkId
                      }
                    }
                  )
                  
                  if (!unlinkResponse.data || !unlinkResponse.data.success) {
                    const errorMessage = unlinkResponse.data?.error || 'Unknown error'
                    console.warn(`[Save Network] Failed to unlink Primary link with original fabric for interface ${interfaceName}:`, errorMessage)
                    console.warn(`[Save Network] Continuing with fabric change despite unlink failure (link may already be removed or fabric already changed)`)
                    // unlink 실패해도 계속 진행 (Fabric 변경은 이미 성공했을 수 있고, IP Assignment 저장도 계속 진행해야 함)
                  } else {
                    console.log(`[Save Network] Primary link unlinked successfully with original fabric for interface ${interfaceName}`)
                  }
                } catch (err) {
                  console.warn(`[Save Network] Error unlinking with original fabric (continuing anyway):`, err)
                  // unlink 실패해도 계속 진행 (Fabric 변경은 이미 성공했을 수 있고, IP Assignment 저장도 계속 진행해야 함)
                }
              }
              
              // 새로운 fabric로 VLAN 업데이트
              if (newFabricId !== null && newFabricId !== undefined && newFabricId !== '') {
                console.log(`Saving fabric for interface ${interfaceId}: editableFabric=${newFabricId} (${typeof newFabricId})`)
                // 타입 안전한 비교를 위해 여러 방법 시도
                const fabric = availableFabrics.value.find(f => 
                  f.id === newFabricId || 
                  String(f.id) === String(newFabricId) ||
                  Number(f.id) === Number(newFabricId)
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
                
                // 원래 fabric ID를 새로운 fabric ID로 업데이트 (다음 변경을 위해)
                networkInterface.originalFabricId = Number(newFabricId)
                
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
                      } else {
                        // Fabric 변경 후 link가 없으면 originalPrimaryLinkId를 null로 설정 (새로 link 생성해야 함)
                        console.log(`[Save Network] No primary link found after fabric change for interface ${interfaceName}, will create new link`)
                        networkInterface.originalPrimaryLinkId = null
                      }
                    } else {
                      // links가 없으면 originalPrimaryLinkId를 null로 설정
                      console.log(`[Save Network] No links found after fabric change for interface ${interfaceName}, will create new link`)
                      networkInterface.originalPrimaryLinkId = null
                    }
                  }
                } catch (err) {
                  console.warn(`[Save Network] Failed to fetch latest machine info after fabric change:`, err)
                  // 에러가 발생해도 계속 진행
                  // Fabric 변경 후 link가 없을 수 있으므로 null로 설정
                  networkInterface.originalPrimaryLinkId = null
                }
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
                // 단, Fabric 변경 후 link가 없을 수 있으므로 null이면 unlink 시도하지 않음
                if (originalPrimaryLinkId) {
                  console.log(`[Save Network] Unlinking existing Primary link (id: ${originalPrimaryLinkId}) for interface ${interfaceName}`)
                  
                  try {
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
                      console.warn(`[Save Network] Failed to unlink Primary link for interface ${interfaceName} (id: ${interfaceIdStr}):`, errorMessage)
                      console.warn(`[Save Network] Continuing with static IP link creation (link may already be removed)`)
                      // unlink 실패해도 계속 진행 (link가 이미 제거되었을 수 있음)
                    } else {
                      console.log(`[Save Network] Primary link unlinked successfully for interface ${interfaceName} (id: ${interfaceIdStr})`)
                    }
                  } catch (err) {
                    console.warn(`[Save Network] Error unlinking Primary link (continuing anyway):`, err)
                    // unlink 실패해도 계속 진행 (link가 이미 제거되었을 수 있음)
                  }
                } else {
                  console.log(`[Save Network] No existing Primary link to unlink for interface ${interfaceName}, creating new link`)
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
            const newStatus = getStatusName(machineData.status_name || machineData.status)
            
            // console.log(`✅ [WebSocket Debug] Machine updated: ${machineData.system_id}, Status: ${oldStatus} → ${newStatus}`)
            
            // Ready 상태로 변경될 때 머신 정보를 다시 가져오기 (커미셔닝 후 네트워크 정보가 변경될 수 있음)
            if (newStatus === 'ready' && oldStatus !== 'ready') {
              // console.log(`🔄 [WebSocket Debug] Status changed to Ready, refreshing machine details for: ${machineData.system_id}`)
              refreshMachineDetails(machineData.system_id)
            }
            
            // MAC 주소 추출 및 fabric 찾기
            const macAddresses = extractMacAddresses(machineData) || machines.value[machineIndex].mac_addresses || []
            // IP 주소 추출
            const extractedIps = extractIpAddresses(machineData)
            const normalizedIps = extractedIps.length > 0 ? extractedIps : normalizeIpAddresses(machineData.ip_addresses)
            const finalIps = normalizedIps.length > 0 ? normalizedIps : (machines.value[machineIndex].ip_addresses || [])
            
            const fabricName = findFabricByMacAddress({
              ...machineData,
              mac_addresses: macAddresses,
              interface_set: machineData.interface_set || machines.value[machineIndex].interface_set || []
            })
            
            machines.value[machineIndex] = {
              ...machines.value[machineIndex],
              status: newStatus,
              status_message: machineData.status_message,
              osystem: machineData.osystem || machines.value[machineIndex].osystem,
              distro_series: machineData.distro_series || machines.value[machineIndex].distro_series,
              power_state: machineData.power_state || machines.value[machineIndex].power_state,
              power_type: machineData.power_type || machines.value[machineIndex].power_type || 'Manual',
              hostname: machineData.hostname,
              ip_addresses: finalIps,
              mac_addresses: macAddresses,
              fabric: fabricName,
              interface_set: machineData.interface_set || machines.value[machineIndex].interface_set || []
            }
            // console.log(`✅ Machine updated: ${machineData.system_id}, Status: ${oldStatus} → ${newStatus}`)
          } else {
            // console.log(`❌ [WebSocket Debug] Machine not found in list: ${machineData.system_id}`)
          }
        } else if (newMessage.action === 'create') {
          // MAC 주소 추출 및 fabric 찾기
          const macAddresses = extractMacAddresses(machineData)
          // IP 주소 추출
          const extractedIps = extractIpAddresses(machineData)
          const normalizedIps = extractedIps.length > 0 ? extractedIps : normalizeIpAddresses(machineData.ip_addresses)
          
          const fabricName = findFabricByMacAddress({
            ...machineData,
            mac_addresses: macAddresses
          })
          
          const newMachine = {
            id: machineData.system_id,
            hostname: machineData.hostname,
            status: getStatusName(machineData.status_name || machineData.status),
            status_message: machineData.status_message,
            osystem: machineData.osystem, // OS 이름 (예: 'ubuntu')
            distro_series: machineData.distro_series, // 배포판 시리즈 (예: 'noble', 'jammy')
            ip_addresses: normalizedIps,
            mac_addresses: macAddresses,
            architecture: machineData.architecture,
            cpu_count: machineData.cpu_count || 0,
            memory: machineData.memory || 0,
            disk_count: machineData.blockdevice_set?.length || 0,
            storage: calculateStorage(machineData.blockdevice_set),
            power_state: machineData.power_state,
            power_type: machineData.power_type || 'Manual',
            owner: machineData.owner,
            tags: machineData.tag_names || [],
            pool: machineData.pool?.name || 'default',
            zone: machineData.zone?.name || 'default',
            fabric: fabricName,
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
    
    // Settings의 itemsPerPage 값이 변경되면 반영
    watch(() => settingsStore.settings.itemsPerPage, (newValue) => {
      if (newValue) {
        itemsPerPage.value = newValue
        // 페이지 변경 시 첫 페이지로 리셋
        currentPage.value = 1
      }
    })
    
    // itemsPerPage가 변경되면 Settings에도 반영 (자동 저장)
    watch(itemsPerPage, (newValue) => {
      if (newValue && settingsStore.settings.itemsPerPage !== newValue) {
        settingsStore.settings.itemsPerPage = newValue
        // Settings에 자동 저장
        settingsStore.save()
      }
    })
    
    // 외부 클릭 시 Power 메뉴, Status Select 메뉴, Action Bar 메뉴 닫기
    const handleClickOutside = (event) => {
      // Power 메뉴 (개별 머신)
      if (!event.target.closest('.power-container') && !event.target.closest('.power-dropdown-menu')) {
        openPowerMenu.value = null
        powerMenuPosition.value = { top: 0, left: 0 }
      }
      // Deploy 메뉴 (개별 머신)
      if (!event.target.closest('.deploy-button-container') && !event.target.closest('.deploy-dropdown-menu')) {
        openDeployMenu.value = null
        deployMenuPosition.value = { top: 0, left: 0 }
      }
      // Status Select 메뉴
      if (!event.target.closest('.select-all-container') && !event.target.closest('.status-select-dropdown-menu')) {
        openStatusSelectMenu.value = false
        statusSelectMenuPosition.value = { top: 0, left: 0 }
      }
      // Action Bar - Actions 메뉴
      const clickedElement = event.target
      const isActionsItem = clickedElement.closest('.action-bar-actions-item') !== null
      const isActionsMenu = clickedElement.closest('.action-bar-dropdown-menu') !== null && openActionsMenu.value
      if (!isActionsItem && !isActionsMenu) {
        openActionsMenu.value = false
        actionsMenuPosition.value = { top: 0, left: 0 }
      }
      // Action Bar - Power 메뉴
      const isPowerItem = clickedElement.closest('.action-bar-power-item') !== null
      const isPowerMenu = clickedElement.closest('.action-bar-dropdown-menu') !== null && openPowerActionMenu.value
      if (!isPowerItem && !isPowerMenu) {
        openPowerActionMenu.value = false
        powerActionMenuPosition.value = { top: 0, left: 0 }
      }
    }
    
    onMounted(() => {
      // 초기 로드는 항상 REST API로
      loadMachines()
      
      // 외부 클릭 이벤트 리스너 추가
      document.addEventListener('click', handleClickOutside)
    })
    
    onUnmounted(() => {
      // 외부 클릭 이벤트 리스너 제거
      document.removeEventListener('click', handleClickOutside)
    })
    
      return {
        machines,
        loading,
        error,
        searchQuery,
        selectedStatus,
        statusFilters,
        filteredMachines,
        paginatedMachines,
        selectedMachines,
        selectAll,
        currentPage,
        itemsPerPage,
        totalPages,
        formatMemory,
        formatStorage,
        formatPowerState,
        formatPowerType,
        getPowerStateClass,
        formatPowerDriver,
        formatPowerBootType,
        formatCipherSuiteId,
        formatPrivilegeLevel,
        formatWorkaroundFlags,
        extractMacAddresses,
        extractIpAddresses,
        normalizeIpAddresses,
        normalizeMacAddresses,
        getFirstMacAddress,
        getFirstIpAddress,
        getStatusText,
        isStatusInProgress,
        getStatusMessage,
        getDeployingOSVersion,
        formatVersionForDeploying,
        getUbuntuVersionFromDistroSeries,
        calculateStorageFromBlockDevices,
        formatMemoryBytes,
        toggleSelectAll,
        // Status Select Menu
        openStatusSelectMenu,
        statusSelectMenuPosition,
        availableStatusesForSelection,
        selectedStatusesForSelection,
        isStatusSelected,
        toggleSelectByStatus,
        toggleStatusSelectMenu,
        getStatusDisplayName,
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
        deployingMachines,
        abortingDeployMachines,
        releasingMachines,
        canCommission,
        isFailedDeployment,
        getCommissionButtonClass,
        getCommissionButtonDisabled,
        handleCommissionButtonClick,
        commissionMachine,
        abortCommissioning,
        releaseMachine,
        getDeployButtonClass,
        getDeployButtonDisabled,
        handleDeployButtonClick,
        abortDeploy,
        deployMachine,
        // Power Menu
        hoveredPowerMachine,
        openPowerMenu,
        powerMenuPosition,
        togglePowerMenu,
        handlePowerAction,
        handleCheckPower,
        getMachineById,
        // Deploy Modal
        showDeployModalState,
        selectedDeployMachine,
        selectedDeployOS,
        selectedCloudConfigTemplate,
        customCloudConfig,
        deployingMachine,
        cloudConfigTemplates,
        getSelectedTemplateCloudConfig,
        matchedTemplates,
        otherTemplates,
        showDeployModal,
        closeDeployModal,
        startDeployFromModal,
        deployableOSList,
        loadingDeployableOS,
        formatOSName,
        // Action Bar
        openActionsMenu,
        openPowerActionMenu,
        actionsMenuPosition,
        powerActionMenuPosition,
        actionsMenuButton,
        powerActionMenuButton,
        toggleActionsMenu,
        togglePowerActionMenu,
        handleBulkAction,
        canBulkPowerOn,
        canBulkPowerOff,
        handleBulkPowerAction,
        handleBulkDelete,
        canBulkCommission,
        canBulkAllocate,
        canBulkDeploy,
        canBulkRelease,
        canBulkAbort,
        abortMachine,
        // Confirmation & Alert Modals
        showConfirmModal,
        confirmModalTitle,
        confirmModalMessage,
        confirmModalButtonText,
        confirmAction,
        cancelConfirm,
        showAlertModal,
        alertModalTitle,
        alertModalMessage,
        closeAlert,
        // Machine Details Modal
        showMachineDetailsModal,
        selectedMachineForDetails,
        machineDetails,
        loadingMachineDetails,
        machineDetailsError,
        activeDetailsTab,
        machineBlockDevices,
        loadingBlockDevices,
        machineEvents,
        loadingEvents,
        loadMachineEvents,
        powerParameters,
        loadingPowerParameters,
        powerParametersError,
        loadMachinePowerParameters,
        isEditingPowerParameters,
        editingPowerParameters,
        savingPowerParameters,
        startEditingPowerParameters,
        cancelEditingPowerParameters,
        savePowerParameters,
        handleModalWheel,
        showMachineDetails,
        closeMachineDetailsModal,
        modalPosition,
        isDraggingModal,
        startDragModal,
        confirmModalPosition,
        isDraggingConfirmModal,
        startDragConfirmModal,
        networkModalPosition,
        isDraggingNetworkModal,
        startDragNetworkModal,
        deployModalPosition,
        isDraggingDeployModal,
        startDragDeployModal,
        // Network Modal
        showNetworkModalState,
        selectedMachine,
        networkInterfaces,
        loadingNetwork,
        networkError,
        savingNetwork,
        availableFabrics,
        availableSubnets,
        getNetworkButtonClass,
        getNetworkButtonDisabled,
        canSaveNetworkChanges,
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
  gap: 1rem;
  margin-bottom: 1rem;
  flex-wrap: nowrap;
  min-height: 48px; /* 액션 바가 나타날 때를 고려한 최소 높이 */
  height: auto;
}

.machines h2 {
  margin: 0;
  color: #2c3e50;
  font-size: 1.8rem;
  flex: 0 0 auto;
}

.connection-status {
  display: flex;
  align-items: center;
  flex: 0 0 auto;
  margin-left: auto;
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

/* Action Bar Styles */
.action-bar {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 0.5rem 0.75rem;
  background: white;
  border-radius: 6px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
  border: 1px solid #dee2e6;
  flex: 0 0 auto;
  margin-left: auto;
  margin-right: 1rem; /* LIVE와의 간격 */
  height: fit-content;
  max-height: 48px; /* 헤더 최소 높이와 맞춤 */
}

.action-bar-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 0.75rem;
  cursor: pointer;
  border-radius: 4px;
  transition: background-color 0.15s;
}

.action-bar-item:hover {
  background-color: #f8f9fa;
}

.action-bar-label {
  font-size: 0.9rem;
  font-weight: 500;
  color: #212529;
}

.action-bar-dropdown-icon {
  font-size: 0.7rem;
  color: #6c757d;
  user-select: none;
  transition: color 0.15s;
  width: 0.7rem;
  text-align: center;
  display: inline-block;
  flex-shrink: 0;
}

.action-bar-dropdown-icon.active {
  color: #495057;
}

.action-bar-icon {
  font-size: 1rem;
  cursor: pointer;
  user-select: none;
}

.action-bar-selected-count {
  margin-left: auto;
  font-size: 0.875rem;
  color: #6c757d;
  font-weight: 500;
  min-width: 120px;
  text-align: right;
  white-space: nowrap;
}

.action-bar-dropdown-menu {
  position: fixed;
  background: white;
  border: 1px solid #dee2e6;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  z-index: 10000;
  min-width: 180px;
}

.action-bar-dropdown-item {
  padding: 8px 12px;
  font-size: 0.875rem;
  color: #212529;
  cursor: pointer;
  transition: background-color 0.15s;
}

.action-bar-dropdown-item:hover:not(.disabled) {
  background-color: #f8f9fa;
}

.action-bar-dropdown-item.disabled {
  color: #6c757d;
  cursor: not-allowed;
  opacity: 0.6;
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
  text-align: left;
  padding-left: 8px;
  padding-right: 0px;
}

.fqdn-col {
  width: 80px !important; /* 확실히 작게 */
  min-width: 80px;
  max-width: 80px;
  padding-left: 0px;
}

.power-col {
  width: 80px; /* 드롭다운 메뉴를 위한 공간 확보 */
  min-width: 80px;
  max-width: 80px;
  position: relative;
}

.status-col {
  width: 140px; /* STATUS 컬럼 크기 증가 (OS 버전 표시를 위해) */
  min-width: 140px;
  max-width: 140px;
}

.owner-col {
  width: 100px !important; /* OWNER 컬럼 크기 감소 */
  min-width: 100px;
  max-width: 100px;
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
  width: 180px !important; /* 버튼들이 한 줄로 보이도록 늘림 */
  min-width: 180px;
  max-width: 180px;
  text-align: left !important; /* 왼쪽 정렬 */
  vertical-align: top;
}

.action-buttons-wrapper {
  display: flex;
  justify-content: flex-start !important; /* 왼쪽 정렬 강제 */
  align-items: flex-start;
  width: 100%;
  margin: 0;
  padding: 0;
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
  min-height: 2.2rem; /* MAC과 IP 두 줄을 위한 최소 높이 */
  display: flex;
  flex-direction: column;
}

.mac-address, .ip-address {
  display: block;
  margin-bottom: 0.05rem;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  min-height: 1rem; /* 각 항목의 최소 높이 */
  line-height: 1rem; /* 텍스트가 없어도 공간 유지 */
}

.power-container {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.power-status {
  font-size: 0.75rem;
  color: #495057;
  display: flex;
  align-items: center;
  gap: 0.3rem;
}

.power-led {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
  flex-shrink: 0;
}

.power-led-on {
  background-color: #28a745;
  box-shadow: 0 0 4px rgba(40, 167, 69, 0.6);
}

.power-led-off {
  background-color: #6c757d;
  box-shadow: 0 0 2px rgba(108, 117, 125, 0.4);
}

.power-led-unknown {
  background-color: #ffc107;
  box-shadow: 0 0 2px rgba(255, 193, 7, 0.4);
}

.power-led-error {
  background-color: #dc3545;
  box-shadow: 0 0 4px rgba(220, 53, 69, 0.6);
}

.power-type {
  font-size: 0.7rem;
  color: #6c757d;
}

.power-dropdown-icon {
  position: absolute;
  right: 0;
  top: 0;
  font-size: 0.6rem;
  color: #6c757d;
  cursor: pointer;
  padding: 2px 4px;
  user-select: none;
}

.power-dropdown-icon:hover {
  color: #495057;
}

.power-dropdown-menu {
  position: fixed;
  background: white;
  border: 1px solid #dee2e6;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  z-index: 10000;
  min-width: 150px;
}

.power-dropdown-header {
  padding: 8px 12px;
  font-size: 0.75rem;
  font-weight: 600;
  color: #495057;
  border-bottom: 1px solid #dee2e6;
  background-color: #f8f9fa;
}

.deploy-button-container {
  position: relative;
  display: inline-block;
}

.deploy-dropdown-icon {
  position: absolute;
  right: -16px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 0.6rem;
  color: #6c757d;
  cursor: pointer;
  padding: 2px 4px;
  user-select: none;
}

.deploy-dropdown-icon:hover {
  color: #495057;
}

.deploy-dropdown-menu {
  position: fixed;
  background: white;
  border: 1px solid #dee2e6;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  z-index: 10000;
  min-width: 200px;
  max-height: 400px;
  overflow-y: auto;
}

.deploy-dropdown-header {
  padding: 8px 12px;
  font-size: 0.75rem;
  font-weight: 600;
  color: #495057;
  border-bottom: 1px solid #dee2e6;
  background-color: #f8f9fa;
}

.deploy-dropdown-item {
  padding: 8px 12px;
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  align-items: center;
  transition: background-color 0.2s;
}

.deploy-dropdown-item:hover:not(.disabled) {
  background-color: #d0e7ff;
}

.deploy-dropdown-item.disabled {
  color: #6c757d;
  cursor: not-allowed;
}

.deploy-dropdown-item.default {
  font-weight: 600;
  background-color: #e7f3ff;
}

.deploy-dropdown-item.default:hover {
  background-color: #d0e7ff;
}

.deploy-dropdown-item .os-name {
  flex: 1;
}

.deploy-dropdown-item .os-arch {
  font-size: 0.75rem;
  color: #6c757d;
  margin-left: 8px;
}

.power-dropdown-item {
  padding: 8px 12px;
  font-size: 0.875rem;
  color: #212529;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: background-color 0.15s;
}

.power-dropdown-item:hover {
  background-color: #f8f9fa;
}

.power-icon {
  font-size: 0.75rem;
  font-weight: bold;
}

.power-icon.power-on {
  color: #28a745;
}

.power-icon.power-off {
  color: #212529;
}

.select-all-container {
  position: relative;
  display: flex;
  align-items: center;
  gap: 4px;
  justify-content: flex-start;
}

.checkbox-col input[type="checkbox"] {
  margin: 0;
  cursor: pointer;
}

.status-select-dropdown-icon {
  font-size: 0.6rem;
  color: #6c757d;
  cursor: pointer;
  padding: 2px 4px;
  user-select: none;
}

.status-select-dropdown-icon:hover {
  color: #495057;
}

.status-select-dropdown-menu {
  position: fixed;
  background: white;
  border: 1px solid #dee2e6;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  z-index: 10000;
  min-width: 200px;
  max-height: 400px;
  overflow-y: auto;
}

.status-select-dropdown-header {
  padding: 8px 12px;
  font-size: 0.75rem;
  font-weight: 600;
  color: #495057;
  border-bottom: 1px solid #dee2e6;
  background-color: #f8f9fa;
}

.status-select-dropdown-item {
  padding: 8px 12px;
  font-size: 0.875rem;
  color: #212529;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: background-color 0.15s;
}

.status-select-dropdown-item:hover {
  background-color: #f8f9fa;
}

.status-select-dropdown-item input[type="checkbox"] {
  margin: 0;
  cursor: pointer;
}

.status-container {
  display: flex;
  flex-direction: column;
  gap: 0.2rem;
}

.status-badge-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.3rem;
}

.status-badge {
  padding: 0.2rem 0.4rem;
  border-radius: 3px;
  font-size: 0.7rem;
  font-weight: 500;
  text-transform: uppercase;
  display: inline-block;
  white-space: nowrap;
}

.status-os-version {
  font-size: 0.65rem;
  color: #495057;
  font-weight: 500;
  white-space: nowrap;
  white-space: nowrap;
}

.status-os-version {
  font-size: 0.65rem;
  color: #495057;
  font-weight: 500;
  white-space: nowrap;
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
  flex-direction: row; /* 한 줄로 배치 */
  gap: 0.2rem; /* Horizontal gap between buttons */
  align-items: flex-start; /* 왼쪽 정렬 */
}

.action-buttons-row {
  display: flex;
  gap: 0.2rem; /* Horizontal gap between buttons */
  flex-wrap: nowrap;
  justify-content: flex-start; /* 왼쪽 정렬 */
}

.btn-small {
  padding: 0.25rem 0.5rem; /* Increased padding for better height */
  border: none;
  border-radius: 4px; /* 모든 버튼 동일한 모서리 */
  cursor: pointer;
  font-size: 0.7rem; /* 모든 버튼 동일한 폰트 크기 */
  font-weight: 500; /* 폰트 굵기 통일 */
  transition: all 0.2s ease;
  white-space: nowrap;
  text-align: center;
  height: 24px; /* Fixed height for consistency */
  display: flex;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
  width: auto; /* 글자 길이에 맞춤 */
  min-width: auto; /* 최소 너비 제거 */
}

.btn-small.btn-primary {
  background-color: #007bff;
  color: white;
  font-size: 0.7rem; /* 녹색 버튼과 동일한 폰트 크기 */
  font-weight: 500; /* 폰트 굵기 통일 */
  border-radius: 4px; /* 녹색 버튼과 동일한 모서리 */
  padding: 0.25rem 0.4rem; /* Network 버튼 가로 크기 줄이기 */
}

.btn-small.btn-primary:hover:not(:disabled) {
  background-color: #0056b3;
}

.btn-small.btn-primary-light {
  background-color: #a8d0f0; /* 연한 파랑 */
  color: white;
  font-size: 0.7rem; /* 모든 버튼 동일한 폰트 크기 */
  font-weight: 500; /* 폰트 굵기 통일 */
  border-radius: 4px; /* 모든 버튼 동일한 모서리 */
  padding: 0.25rem 0.4rem;
}

.btn-small.btn-primary-light:hover:not(:disabled) {
  background-color: #8fc0e0; /* 약간 더 진한 연한 파랑 */
}

.btn-small.btn-primary-light:disabled {
  background-color: #6c757d;
  cursor: not-allowed;
  opacity: 0.6;
}

.btn-small.btn-secondary {
  background-color: #6c757d;
  color: white;
  font-size: 0.7rem; /* 녹색 버튼과 동일한 폰트 크기 */
  font-weight: 500; /* 폰트 굵기 통일 */
  border-radius: 4px; /* 녹색 버튼과 동일한 모서리 */
  padding: 0.25rem 0.5rem; /* Deploy 버튼과 동일한 패딩 */
}

.btn-small.btn-secondary:hover:not(:disabled) {
  background-color: #545b62;
}

.btn-small.btn-success {
  background-color: #28a745;
  color: white;
  font-size: 0.7rem; /* 모든 버튼 동일한 폰트 크기 */
  font-weight: 500; /* 폰트 굵기 통일 */
  border-radius: 4px; /* 모든 버튼 동일한 모서리 */
}

.btn-small.btn-success:hover:not(:disabled) {
  background-color: #218838;
}

.btn-small.btn-success-light {
  background-color: #b0e0b0; /* 더 연한 녹색 */
  color: white; /* 흰색 텍스트 */
  font-size: 0.7rem; /* 모든 버튼 동일한 폰트 크기 */
  font-weight: 500; /* 폰트 굵기 통일 */
  border-radius: 4px; /* 모든 버튼 동일한 모서리 */
}

.btn-small.btn-success-light:hover:not(:disabled) {
  background-color: #9dd89d; /* 약간 더 진한 연한 녹색 */
}

.btn-small.btn-success-light:disabled {
  background-color: #6c757d;
  cursor: not-allowed;
  opacity: 0.6;
}

.btn-small.btn-success:disabled {
  background-color: #6c757d;
  cursor: not-allowed;
}

.btn-small.btn-warning {
  background-color: #ffc107;
  color: #212529;
  font-size: 0.7rem; /* 녹색 버튼과 동일한 폰트 크기 */
  font-weight: 500; /* 폰트 굵기 통일 */
  border-radius: 4px; /* 녹색 버튼과 동일한 모서리 */
}

.btn-small.btn-warning:hover:not(:disabled) {
  background-color: #e0a800;
}

.btn-small.btn-warning:disabled {
  background-color: #6c757d;
  cursor: not-allowed;
}

.btn-small.btn-primary:disabled,
.btn-small.btn-secondary:disabled {
  background-color: #6c757d;
  cursor: not-allowed;
  opacity: 0.6;
}

.btn-small.btn-deploy {
  background-color: #f8b4d4; /* 옅은 분홍색 */
  color: white; /* 흰색 텍스트 */
  font-size: 0.7rem; /* 모든 버튼 동일한 폰트 크기 */
  font-weight: 500; /* 폰트 굵기 통일 */
  border-radius: 4px; /* 모든 버튼 동일한 모서리 */
  padding: 0.25rem 0.5rem; /* 비활성화된 Deploy 버튼과 동일한 패딩 */
}

.btn-small.btn-deploy:hover:not(:disabled) {
  background-color: #f5a0c4; /* 약간 더 진한 분홍색 */
}

.btn-small.btn-deploy:disabled {
  background-color: #6c757d;
  cursor: not-allowed;
  opacity: 0.6;
}

.btn-small.btn-release {
  background-color: #f5a5a5; /* 연한 빨강 계열 */
  color: white;
  font-size: 0.7rem; /* 모든 버튼 동일한 폰트 크기 */
  font-weight: 500; /* 폰트 굵기 통일 */
  border-radius: 4px; /* 모든 버튼 동일한 모서리 */
  padding: 0.25rem 0.5rem;
}

.btn-small.btn-release:hover:not(:disabled) {
  background-color: #f18f8f; /* 약간 더 진한 연한 빨강 */
}

.btn-small.btn-release:disabled {
  background-color: #6c757d;
  cursor: not-allowed;
  opacity: 0.6;
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

/* Machine Details Modal 내부의 로딩/에러 상태도 고정 높이 */
.machine-details-modal-body .loading,
.machine-details-modal-body .error {
  min-height: 500px;
  height: 500px;
  display: flex;
  align-items: center;
  justify-content: center;
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

.checkbox-group {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 0.5rem 1rem;
  padding: 0.75rem;
  border: 1px solid #ced4da;
  border-radius: 6px;
  background-color: #f8f9fa;
}

@media (max-width: 1000px) {
  .checkbox-group {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 600px) {
  .checkbox-group {
    grid-template-columns: 1fr;
  }
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-weight: 400;
  color: #495057;
  font-size: 0.9rem;
  cursor: pointer;
}

.form-checkbox {
  width: auto;
  margin: 0;
  cursor: pointer;
}

.ipmi-fields-group {
  margin: 1.5rem 0;
  border: 1px solid #dee2e6;
  border-radius: 8px;
  background-color: #f8f9fa;
  overflow: hidden;
}

.ipmi-fields-header {
  padding: 0.75rem 1rem;
  background-color: #e9ecef;
  border-bottom: 1px solid #dee2e6;
}

.ipmi-fields-header label {
  margin: 0;
  font-weight: 600;
  color: #495057;
  font-size: 0.95rem;
}

.ipmi-fields-content {
  padding: 1rem 1.5rem;
}

.ipmi-fields-content .form-group {
  margin-bottom: 1rem;
}

.ipmi-edit-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 1rem 1.5rem;
}

.ipmi-edit-grid .form-group-full-width {
  grid-column: 1 / -1;
}

@media (max-width: 1200px) {
  .ipmi-edit-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 800px) {
  .ipmi-edit-grid {
    grid-template-columns: 1fr;
  }
}

.ipmi-fields-content .form-group:last-child {
  margin-bottom: 0;
}

.power-edit-form {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.power-edit-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  margin-top: 1rem;
  padding-top: 1rem;
  border-top: 1px solid #dee2e6;
}

.btn-sm {
  padding: 0.5rem 1rem;
  font-size: 0.875rem;
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

/* Confirmation & Alert Modal Styles */
.confirm-modal-content,
.alert-modal-content {
  max-width: 500px;
  width: 90%;
  transition: none; /* Disable transition during drag */
}

.confirm-modal-content.dragging,
.alert-modal-content.dragging {
  transition: none;
}

.modal-body {
  padding: 1.5rem;
}

.confirm-modal-body,
.alert-modal-body {
  padding: 1.5rem;
}

.confirm-message,
.alert-message {
  margin: 0;
  color: #495057;
  font-size: 1rem;
  line-height: 1.6;
  white-space: pre-line;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 1rem;
  padding: 1.5rem;
  border-top: 1px solid #e9ecef;
}

.confirm-modal-footer,
.alert-modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 1rem;
  padding: 1.5rem;
  border-top: 1px solid #e9ecef;
}

/* Network Modal Styles */
.network-modal-content {
  max-width: 800px;
  max-height: 90vh;
  transition: none; /* Disable transition during drag */
}

.network-modal-content.dragging {
  transition: none;
}

.network-modal-body {
  padding: 1.5rem;
  max-height: calc(90vh - 120px);
  overflow-y: auto;
}

/* Deploy Modal Styles */
.deploy-modal-content {
  max-width: 600px;
  max-height: 90vh;
  transition: none; /* Disable transition during drag */
}

.deploy-modal-content.dragging {
  transition: none;
}

.deploy-modal-body {
  padding: 1.5rem;
  max-height: calc(90vh - 120px);
  overflow-y: auto;
}

.deploy-form {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.form-section {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.form-label {
  font-weight: 600;
  color: #495057;
  font-size: 0.9rem;
}

.form-hint {
  font-size: 0.75rem;
  color: #6c757d;
  margin-top: 0.25rem;
}

.default-badge {
  color: #007bff;
  font-weight: 600;
}

.template-preview {
  background-color: #f8f9fa;
  border: 1px solid #dee2e6;
  border-radius: 6px;
  padding: 1rem;
  font-family: 'Courier New', monospace;
  font-size: 0.85rem;
  white-space: pre-wrap;
  word-wrap: break-word;
  max-height: 200px;
  overflow-y: auto;
  color: #495057;
  margin: 0;
}

.code-editor {
  font-family: 'Courier New', monospace;
  font-size: 0.85rem;
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

/* Machine Details Modal Styles */
.machine-details-modal-content {
  max-width: 1400px; /* Network 탭에서 여러 인터페이스를 가로로 배치하기 위해 크게 확장 */
  max-height: 90vh;
  width: 95%; /* 가로 공간 활용 */
  transition: none; /* Disable transition during drag */
}

.machine-details-modal-content.dragging {
  transition: none;
}

.modal-draggable-header {
  user-select: none; /* Prevent text selection during drag */
  cursor: grab;
}

.modal-draggable-header:active {
  cursor: grabbing;
}

.machine-details-modal-body {
  padding: 0;
  max-height: calc(90vh - 80px);
  overflow: hidden; /* 스크롤바 제거, 내부 컨텐츠에서만 스크롤 */
  display: flex;
  flex-direction: column;
}

.machine-details-content {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.details-tabs {
  flex-shrink: 0; /* 탭이 줄어들지 않도록 */
}

.machine-hostname-clickable {
  cursor: pointer;
  color: #007bff;
  text-decoration: none;
  transition: color 0.2s ease;
}

.machine-hostname-clickable:hover {
  color: #0056b3;
  text-decoration: underline;
}

.details-tabs {
  display: flex;
  border-bottom: 2px solid #e9ecef;
  background-color: #f8f9fa;
  padding: 0 1.5rem;
}

.details-tab {
  padding: 1rem 1.5rem;
  background: none;
  border: none;
  border-bottom: 3px solid transparent;
  cursor: pointer;
  font-size: 0.95rem;
  font-weight: 500;
  color: #6c757d;
  transition: all 0.2s ease;
  margin-bottom: -2px;
}

.details-tab:hover {
  color: #007bff;
  background-color: #ffffff;
}

.details-tab.active {
  color: #007bff;
  border-bottom-color: #007bff;
  background-color: #ffffff;
}

.details-tab-content {
  padding: 1.5rem;
  min-height: 600px; /* Power 탭 편집 모드를 포함한 고정 높이 */
  max-height: 600px;
  overflow: hidden; /* 기본적으로 스크롤바 숨김 */
  box-sizing: border-box; /* padding 포함한 크기 계산 */
  display: flex;
  flex-direction: column;
  position: relative; /* 스크롤바 위치 계산을 위해 */
}

.details-section {
  min-height: 600px; /* 모든 탭의 섹션 높이 통일 (Power 탭 편집 모드 포함) */
  height: 600px; /* 고정 높이 */
  overflow-y: hidden; /* 기본적으로 스크롤바 숨김 */
  box-sizing: border-box; /* padding 포함한 크기 계산 */
  flex: 1 1 auto; /* 부모 컨테이너의 남은 공간 채우기 */
  min-height: 0; /* flex item이 부모보다 작아질 수 있도록 */
  max-height: 100%; /* 부모 컨테이너를 넘지 않도록 */
}

.details-section-power {
  overflow-y: auto;
  flex: 1 1 auto;
  min-height: 0;
  max-height: 100%;
  height: auto;
  padding-bottom: 0.5rem;
}

/* Network 탭은 인터페이스가 많을 수 있으므로 스크롤 허용 */
.details-section-network {
  overflow-y: auto; /* Network 탭에만 스크롤 허용 */
  flex: 1 1 auto;
  min-height: 0; /* flex item이 부모보다 작아질 수 있도록 */
  max-height: 100%; /* 부모 컨테이너를 넘지 않도록 */
  height: auto; /* 높이를 자동으로 조정 */
}

/* Events 탭은 이벤트가 많을 수 있으므로 스크롤 허용 */
.details-section-events {
  overflow-y: auto; /* Events 탭에만 스크롤 허용 */
  flex: 1 1 auto;
  min-height: 0; /* flex item이 부모보다 작아질 수 있도록 */
  max-height: 100%; /* 부모 컨테이너를 넘지 않도록 */
  height: auto; /* 높이를 자동으로 조정 */
  padding-bottom: 0.5rem; /* 스크롤 시 하단 여백 */
}

.details-info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 1.5rem;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.info-item label {
  font-weight: 600;
  color: #2c3e50; /* 더 진한 색상으로 구분 */
  font-size: 0.9rem;
  margin-bottom: 0.25rem; /* label과 value 사이 간격 추가 */
}

.info-item > div {
  color: #495057; /* 약간 연한 색상으로 구분 */
  font-size: 0.95rem;
  font-weight: 400; /* 명시적으로 일반 글꼴 */
}

.status-message-detail {
  margin-top: 0.5rem;
  font-size: 0.85rem;
  color: #6c757d;
}

.tag {
  display: inline-block;
  padding: 0.25rem 0.5rem;
  background-color: #e9ecef;
  border-radius: 4px;
  font-size: 0.85rem;
  color: #495057;
  margin-right: 0.5rem;
  margin-bottom: 0.25rem;
}

.block-devices-section {
  margin-top: 2rem;
}

.block-devices-section h4 {
  margin-bottom: 1rem;
  color: #2c3e50;
}

.block-devices-table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 1rem;
}

.block-devices-table thead {
  background-color: #f8f9fa;
}

.block-devices-table th,
.block-devices-table td {
  padding: 0.75rem;
  text-align: left;
  border-bottom: 1px solid #dee2e6;
}

.block-devices-table th {
  font-weight: 600;
  color: #495057;
  font-size: 0.9rem;
}

.block-devices-table tbody tr:hover {
  background-color: #f8f9fa;
}

.network-interfaces-detail {
  display: flex;
  flex-direction: row; /* 가로 배치 */
  flex-wrap: wrap; /* 다음 줄로 넘어가도록 */
  gap: 1rem; /* 간격 조정 */
  align-items: stretch; /* 모든 항목이 같은 높이를 갖도록 */
}

.interface-detail-item {
  border: 1px solid #dee2e6;
  border-radius: 8px;
  padding: 1.5rem;
  background-color: #ffffff;
  flex: 0 0 calc(23% - 0.75rem); /* 한 줄에 4개가 들어가도록 (gap 고려) */
  min-width: 280px; /* 최소 너비 설정으로 내용이 잘리지 않도록 */
  max-width: calc(23% - 0.75rem); /* 최대 너비도 동일하게 */
  box-sizing: border-box; /* padding 포함한 크기 계산 */
  display: flex;
  flex-direction: column; /* 내부 요소를 세로로 배치 */
  height: 100%; /* 부모의 높이에 맞춤 */
}

.interface-detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
  padding-bottom: 0.75rem;
  border-bottom: 1px solid #e9ecef;
}

.interface-detail-header h4 {
  margin: 0;
  color: #2c3e50;
}

.interface-type {
  padding: 0.25rem 0.75rem;
  background-color: #e9ecef;
  border-radius: 4px;
  font-size: 0.85rem;
  color: #495057;
}

.interface-detail-info {
  display: flex;
  flex-direction: column;
  gap: 1rem; /* 세로 간격을 넓혀서 시각적으로 여유있게 */
  flex: 1; /* 남은 공간을 채우도록 */
}

.info-row {
  display: flex;
  gap: 0.75rem; /* 간격을 줄여서 텍스트가 다음 줄로 넘어가는 것을 방지 */
  align-items: flex-start; /* 텍스트가 길 경우 상단 정렬 */
  flex-shrink: 0; /* 줄어들지 않도록 */
}

.info-row label {
  font-weight: 600;
  color: #2c3e50; /* 더 진한 색상으로 구분 */
  min-width: 120px;
  flex-shrink: 0; /* label은 줄어들지 않도록 */
  font-size: 0.9rem;
}

.info-row span {
  color: #495057; /* 약간 연한 색상으로 구분 */
  font-size: 0.95rem;
  font-weight: 400; /* 명시적으로 일반 글꼴 */
  word-break: break-word; /* 긴 텍스트는 적절히 줄바꿈 */
  overflow-wrap: break-word; /* 단어가 길 경우 줄바꿈 */
}

.ip-addresses-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.ip-address-item {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.ip-address-display {
  font-weight: 500;
  color: #212529;
}

.auto-ip {
  color: #6c757d;
  font-style: italic;
}

.subnet-info {
  color: #6c757d;
  font-size: 0.85rem;
  margin-top: 0.25rem;
}

.events-section {
  padding: 1rem 0;
  padding-bottom: 1.5rem; /* 스크롤 시 하단 여백 추가 */
}

.events-info {
  margin-bottom: 2rem;
  padding: 1rem;
  background-color: #f8f9fa;
  border-radius: 6px;
  border-left: 4px solid #007bff;
}

.events-info p {
  margin: 0.5rem 0;
  color: #495057;
}

.events-note {
  font-size: 0.85rem;
  color: #6c757d;
  font-style: italic;
}

.event-item {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  padding: 1rem;
  border-left: 3px solid #007bff;
  background-color: #f8f9fa;
  border-radius: 4px;
}

.event-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
}

.event-time {
  font-size: 0.85rem;
  color: #6c757d;
  font-weight: 500;
}

.event-level {
  padding: 0.25rem 0.75rem;
  border-radius: 12px;
  font-size: 0.75rem;
  font-weight: 600;
  text-transform: uppercase;
}

.event-level-info {
  background-color: #d1ecf1;
  color: #0c5460;
}

.event-level-warning {
  background-color: #fff3cd;
  color: #856404;
}

.event-level-error {
  background-color: #f8d7da;
  color: #721c24;
}

.event-level-critical {
  background-color: #f5c6cb;
  color: #721c24;
}

.event-content {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.event-type {
  font-weight: 600;
  color: #2c3e50;
  font-size: 0.95rem;
}

.event-description {
  color: #495057;
  font-size: 0.9rem;
}

.event-username {
  color: #6c757d;
  font-size: 0.85rem;
  font-style: italic;
}

.event-content {
  flex: 1;
}

.event-status {
  font-weight: 600;
  color: #2c3e50;
  margin-bottom: 0.25rem;
}

.event-message {
  color: #495057;
  font-size: 0.9rem;
}

.no-data {
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
  
  .details-info-grid {
    grid-template-columns: 1fr;
  }
  
  .details-tabs {
    flex-wrap: wrap;
    padding: 0 1rem;
  }
  
  .details-tab {
    padding: 0.75rem 1rem;
    font-size: 0.85rem;
  }
  
  .info-row {
    flex-direction: column;
    gap: 0.25rem;
  }
  
  .info-row label {
    min-width: auto;
  }
}
</style>
