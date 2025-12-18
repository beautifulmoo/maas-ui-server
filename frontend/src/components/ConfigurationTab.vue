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
                <th>Deployed Machines</th>
              </tr>
            </thead>
            <tbody>
              <template v-for="(osGroup, osName) in groupedOS" :key="osName">
                <tr v-for="(os, index) in osGroup" :key="`${osName}-${index}`">
                  <td v-if="index === 0" :rowspan="osGroup.length" class="os-name-cell">
                    {{ formatOS(os.os) }}
                  </td>
                  <td>{{ os.release }}</td>
                  <td>{{ formatVersion(os.os, os.release) }}</td>
                  <td>{{ formatArchitectures(os.arches) }}</td>
                  <td>{{ getDeployedMachineCount(os.os, os.release) }}</td>
                </tr>
              </template>
            </tbody>
          </table>
        </div>
      </div>
      
      <div class="configuration-section">
        <div class="section-header">
          <h3>Tags</h3>
          <button @click="openTagModal()" class="btn-primary btn-sm">
            + Add Tag
          </button>
        </div>
        
        <div v-if="loadingTags" class="loading">
          <p>Loading tags...</p>
        </div>
        
        <div v-else-if="tagsError" class="error">
          <p>{{ tagsError }}</p>
          <button @click="loadTags" class="btn-primary btn-sm">Retry</button>
        </div>
        
        <div v-else-if="tags.length === 0" class="no-data">
          <p>No tags found. Click "+ Add Tag" to create one.</p>
        </div>
        
        <div v-else class="tags-list">
          <table class="tags-table">
            <thead>
              <tr>
                <th>Name</th>
                <th>Definition</th>
                <th>Comment</th>
                <th>Kernel Options</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="tag in tags" :key="tag.name" @click="openTagModal(tag)" class="tag-row">
                <td><span class="tag-badge">{{ tag.name }}</span></td>
                <td>{{ tag.definition || '-' }}</td>
                <td>{{ tag.comment || '-' }}</td>
                <td>{{ tag.kernel_opts || '-' }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
      
      <div class="configuration-section">
        <div class="section-header">
          <h3>Cloud-Config Templates</h3>
          <button @click="openTemplateModal(null)" class="btn-primary btn-sm">
            + Add Template
          </button>
        </div>
        
        <div v-if="cloudConfigTemplates.length === 0" class="no-data">
          <p>No cloud-config templates. Click "+ Add Template" to create one.</p>
        </div>
        
        <div v-else class="templates-list">
          <table class="templates-table">
            <thead>
              <tr>
                <th>Name</th>
                <th>Tags</th>
                <th>Description</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="template in cloudConfigTemplates" :key="template.id" @click="openTemplateModal(template)" class="template-row">
                <td>{{ template.name }}</td>
                <td>
                  <div class="tags-container" v-if="template.tags && template.tags.length > 0">
                    <span v-for="tag in template.tags" :key="tag" class="tag-badge">{{ tag }}</span>
                  </div>
                  <span v-else class="no-tag">-</span>
                </td>
                <td>{{ template.description || '-' }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
    
    <!-- Cloud-Config Template Modal -->
    <div v-if="showTemplateModal" class="modal-overlay">
      <div 
        class="modal-content template-modal-content" 
        :style="(templateModalPosition?.top || templateModalPosition?.left) ? { position: 'fixed', top: (templateModalPosition?.top || 0) + 'px', left: (templateModalPosition?.left || 0) + 'px', margin: 0 } : {}"
        @click.stop
      >
        <div 
          class="modal-header modal-draggable-header"
          @mousedown="startDragTemplateModal"
          :style="isDraggingTemplateModal ? { cursor: 'grabbing' } : { cursor: 'grab' }"
        >
          <h3>{{ editingTemplate ? 'Template Information' : 'New Template' }}</h3>
          <button class="close-btn" @click="closeTemplateModal">&times;</button>
        </div>
        
        <div class="modal-body">
          <div class="form-section">
            <label class="form-label">Name *</label>
            <input 
              v-model="templateForm.name" 
              type="text" 
              class="form-input"
              placeholder="e.g., web-server, database"
            />
          </div>
          
          <div class="form-section">
            <label class="form-label">Tags</label>
            <div v-if="loadingTags" class="loading-tags">
              <p>Loading tags...</p>
            </div>
            <div v-else-if="tags.length === 0" class="no-tags-message">
              <p>No tags available. Tags will be loaded from MAAS.</p>
            </div>
            <div v-else class="tags-selection">
              <div class="tags-checkbox-container">
                <label 
                  v-for="tag in tags" 
                  :key="tag.name"
                  class="tag-checkbox-label"
                >
                  <input 
                    type="checkbox"
                    :value="tag.name"
                    v-model="templateForm.selectedTags"
                    class="tag-checkbox"
                  />
                  <span class="tag-badge">{{ tag.name }}</span>
                  <span v-if="tag.comment" class="tag-comment">{{ tag.comment }}</span>
                </label>
              </div>
            </div>
            <p class="form-hint">Select MAAS tags to associate with this template. Templates matching machine tags will be recommended during deploy.</p>
          </div>
          
          <div class="form-section">
            <label class="form-label">Description</label>
            <textarea 
              v-model="templateForm.description" 
              class="form-textarea"
              rows="2"
              placeholder="Brief description of this template"
            ></textarea>
          </div>
          
          <div class="form-section">
            <label class="form-label">Cloud-Config YAML *</label>
            <textarea 
              v-model="templateForm.cloudConfig" 
              class="form-textarea code-editor"
              rows="15"
              placeholder="#cloud-config&#10;users:&#10;  - name: ubuntu&#10;    ssh-authorized-keys:&#10;      - ssh-rsa ...&#10;packages:&#10;  - nginx&#10;  - curl"
            ></textarea>
            <p class="form-hint">Enter cloud-config in YAML format</p>
          </div>
        </div>
        
        <div class="modal-footer">
          <button 
            v-if="editingTemplate" 
            @click="deleteTemplate(editingTemplate.id)" 
            class="btn-delete btn-sm" 
            :disabled="!editingTemplate"
          >
            Delete
          </button>
          <button @click="closeTemplateModal" class="btn-secondary btn-sm">Cancel</button>
          <button @click="saveTemplate" class="btn-primary btn-sm" :disabled="!isTemplateFormChanged">
            {{ editingTemplate ? 'Update' : 'Create' }}
          </button>
        </div>
      </div>
    </div>
    
    <!-- Add Tag Modal -->
    <div v-if="showTagModal" class="modal-overlay">
      <div 
        class="modal-content tag-modal-content" 
        :style="(tagModalPosition?.top || tagModalPosition?.left) ? { position: 'fixed', top: (tagModalPosition?.top || 0) + 'px', left: (tagModalPosition?.left || 0) + 'px', margin: 0 } : {}"
        @click.stop
      >
        <div 
          class="modal-header modal-draggable-header"
          @mousedown="startDragTagModal"
          :style="isDraggingTagModal ? { cursor: 'grabbing' } : { cursor: 'grab' }"
        >
          <h3>Tag Information</h3>
          <button class="close-btn" @click="closeTagModal">&times;</button>
        </div>
        
        <div class="modal-body">
          <div class="form-section">
            <label class="form-label">Name *</label>
            <input 
              v-model="tagForm.name" 
              type="text" 
              class="form-input"
              placeholder="e.g., web, db, k8s-node"
            />
          </div>
          
          <div class="form-section">
            <label class="form-label">Comment</label>
            <textarea 
              v-model="tagForm.comment" 
              class="form-textarea"
              rows="3"
              placeholder="Tag description"
            ></textarea>
          </div>
          
          <div class="form-section">
            <label class="form-label">Kernel Options</label>
            <input 
              v-model="tagForm.kernelOpts" 
              type="text" 
              class="form-input"
              disabled
              placeholder="Disabled for now"
            />
          </div>
          
          <div class="form-section">
            <label class="form-label">Definition</label>
            <input 
              v-model="tagForm.definition" 
              type="text" 
              class="form-input"
              disabled
              placeholder="Disabled for now"
            />
          </div>
        </div>
        
        <div class="modal-footer">
          <button 
            v-if="editingTag" 
            @click="deleteTag" 
            class="btn-delete btn-sm" 
            :disabled="creatingTag"
          >
            <span v-if="creatingTag">Deleting...</span>
            <span v-else>Delete</span>
          </button>
          <button @click="closeTagModal" class="btn-secondary btn-sm" :disabled="creatingTag">Cancel</button>
          <button @click="saveTag" class="btn-primary btn-sm" :disabled="!isTagFormChanged || creatingTag">
            <span v-if="creatingTag">{{ editingTag ? 'Updating...' : 'Creating...' }}</span>
            <span v-else>{{ editingTag ? 'Update' : 'Create' }}</span>
          </button>
        </div>
      </div>
    </div>
    
    <!-- Confirmation Modal -->
    <Teleport to="body">
      <div v-if="showConfirmModal" class="modal-overlay">
        <div 
          class="modal-content confirm-modal-content"
          :style="confirmModalPosition?.top || confirmModalPosition?.left ? { position: 'fixed', top: confirmModalPosition.top + 'px', left: confirmModalPosition.left + 'px', margin: 0 } : {}"
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
      <div v-if="showAlertModal" class="modal-overlay">
        <div 
          class="modal-content alert-modal-content"
          :style="confirmModalPosition?.top || confirmModalPosition?.left ? { position: 'fixed', top: confirmModalPosition.top + 'px', left: confirmModalPosition.left + 'px', margin: 0 } : {}"
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
  </div>
</template>

<script>
import { ref, onMounted, computed, watch } from 'vue'
import axios from 'axios'
import { useSettings } from '../composables/useSettings'

const API_BASE_URL = 'http://localhost:8081/api'

export default {
  name: 'ConfigurationTab',
  setup() {
    const settingsStore = useSettings()
    const deployableOS = ref([])
    const loading = ref(false)
    const error = ref(null)
    const machines = ref([])
    const loadingMachines = ref(false)
    
    // Tags
    const tags = ref([])
    const loadingTags = ref(false)
    const tagsError = ref(null)
    const showTagModal = ref(false)
    const creatingTag = ref(false)
    const editingTag = ref(null)
    const tagForm = ref({
      name: '',
      comment: '',
      kernelOpts: '',
      definition: ''
    })
    
    // Cloud-Config Templates
    const cloudConfigTemplates = ref([])
    const showTemplateModal = ref(false)
    const editingTemplate = ref(null)
    const templateForm = ref({
      name: '',
      selectedTags: [],
      description: '',
      cloudConfig: ''
    })
    const isDraggingTemplateModal = ref(false)
    const templateModalPosition = ref({ top: 0, left: 0 })
    const templateDragStartPosition = ref({ x: 0, y: 0 })
    
    // Tag Modal drag
    const isDraggingTagModal = ref(false)
    const tagModalPosition = ref({ top: 0, left: 0 })
    const tagDragStartPosition = ref({ x: 0, y: 0 })
    
    // Confirmation Modal
    const showConfirmModal = ref(false)
    const confirmModalTitle = ref('확인')
    const confirmModalMessage = ref('')
    const confirmModalButtonText = ref('확인')
    const confirmModalResolve = ref(null)
    const isDraggingConfirmModal = ref(false)
    const confirmModalPosition = ref({ top: 0, left: 0 })
    const confirmDragStartPosition = ref({ x: 0, y: 0 })
    
    // Alert Modal
    const showAlertModal = ref(false)
    const alertModalTitle = ref('알림')
    const alertModalMessage = ref('')
    const alertModalResolve = ref(null)
    
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
    
    // OS별로 그룹화된 데이터 생성
    const groupedOS = computed(() => {
      const groups = {}
      
      deployableOS.value.forEach(os => {
        const osKey = os.os.toLowerCase()
        if (!groups[osKey]) {
          groups[osKey] = []
        }
        groups[osKey].push(os)
      })
      
      // 각 OS 그룹 내에서 release로 정렬 (최신순)
      Object.keys(groups).forEach(osKey => {
        groups[osKey].sort((a, b) => {
          // release 이름으로 정렬 (간단한 문자열 비교)
          return b.release.localeCompare(a.release)
        })
      })
      
      return groups
    })
    
    const loadMachines = async () => {
      loadingMachines.value = true
      
      try {
        const settings = settingsStore.settings
        if (!settings.maasUrl || !settings.apiKey) {
          loadingMachines.value = false
          return
        }
        
        const response = await axios.get('http://localhost:8081/api/machines', {
          params: {
            maasUrl: settings.maasUrl,
            apiKey: settings.apiKey
          }
        })
        
        if (response.data && response.data.results) {
          machines.value = response.data.results
        } else {
          machines.value = []
        }
      } catch (err) {
        console.error('Error loading machines:', err)
        machines.value = []
      } finally {
        loadingMachines.value = false
      }
    }
    
    const getDeployedMachineCount = (os, release) => {
      if (!machines.value || machines.value.length === 0) {
        return 0
      }
      
      return machines.value.filter(machine => {
        // Deployed 상태인 머신만 카운트
        const isDeployed = machine.status_name === 'Deployed' || machine.status === 6
        
        if (!isDeployed) {
          return false
        }
        
        // OS와 Release 매칭
        const machineOS = machine.osystem
        const machineDistroSeries = machine.distro_series
        
        if (!machineOS || !machineDistroSeries) {
          return false
        }
        
        // distro_series 형식: "ubuntu/noble" 또는 "noble"
        // os와 release를 매칭
        const osMatch = machineOS.toLowerCase() === os.toLowerCase()
        
        // distro_series가 "os/release" 형식인지 확인
        let releaseMatch = false
        if (machineDistroSeries.includes('/')) {
          const parts = machineDistroSeries.split('/')
          if (parts.length === 2) {
            releaseMatch = parts[1].toLowerCase() === release.toLowerCase()
          }
        } else {
          // distro_series가 release만 있는 경우
          releaseMatch = machineDistroSeries.toLowerCase() === release.toLowerCase()
        }
        
        return osMatch && releaseMatch
      }).length
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
    
    // Cloud-Config Templates Functions
    const loadCloudConfigTemplates = async () => {
      try {
        const response = await axios.get(`${API_BASE_URL}/cloud-config-templates`)
        if (response.data && response.data.success && response.data.templates) {
          cloudConfigTemplates.value = response.data.templates
        } else {
          cloudConfigTemplates.value = []
        }
      } catch (err) {
        console.error('Error loading cloud-config templates:', err)
        cloudConfigTemplates.value = []
      }
    }
    
    const saveCloudConfigTemplates = async () => {
      try {
        const response = await axios.post(`${API_BASE_URL}/cloud-config-templates`, cloudConfigTemplates.value)
        if (!response.data || !response.data.success) {
          throw new Error(response.data?.error || 'Failed to save templates')
        }
      } catch (err) {
        console.error('Error saving cloud-config templates:', err)
        throw err
      }
    }
    
    const openTemplateModal = async (template) => {
      // Load tags if not already loaded
      if (tags.value.length === 0 && !loadingTags.value) {
        await loadTags()
      }
      
      if (template) {
        editingTemplate.value = template
        templateForm.value = {
          name: template.name || '',
          selectedTags: (template.tags && template.tags.length > 0) ? [...template.tags] : [],
          description: template.description || '',
          cloudConfig: template.cloudConfig || ''
        }
      } else {
        editingTemplate.value = null
        templateForm.value = {
          name: '',
          selectedTags: [],
          description: '',
          cloudConfig: ''
        }
      }
      // Reset modal position when opening
      templateModalPosition.value = { top: 0, left: 0 }
      showTemplateModal.value = true
    }
    
    const isTemplateFormValid = computed(() => {
      return templateForm.value.name.trim() !== '' && 
             templateForm.value.cloudConfig.trim() !== ''
    })
    
    // Template 폼이 실제로 변경되었는지 확인
    const isTemplateFormChanged = computed(() => {
      if (!editingTemplate.value) {
        // 새로 생성하는 경우는 항상 활성화 (유효성 검사만)
        return isTemplateFormValid.value
      }
      
      // 편집 모드: 원본 데이터와 비교
      const original = editingTemplate.value
      const current = templateForm.value
      
      // Tags 배열 비교 (순서 무관)
      const originalTags = (original.tags || []).sort().join(',')
      const currentTags = (current.selectedTags || []).sort().join(',')
      
      return original.name !== current.name.trim() ||
             (original.description || '') !== (current.description || '') ||
             originalTags !== currentTags ||
             (original.cloudConfig || '') !== (current.cloudConfig || '')
    })
    
    const saveTemplate = async () => {
      if (!isTemplateFormValid.value) {
        return
      }
      
      // Use selected tags directly
      const selectedTags = templateForm.value.selectedTags || []
      const cloudConfigYaml = templateForm.value.cloudConfig.trim()
      
      // Base64 인코딩 (JavaScript의 btoa(unescape(encodeURIComponent()))와 동일)
      const cloudConfigBase64 = btoa(unescape(encodeURIComponent(cloudConfigYaml)))
      
      const templateData = {
        id: editingTemplate.value ? editingTemplate.value.id : Date.now().toString(),
        name: templateForm.value.name.trim(),
        tags: selectedTags,
        description: templateForm.value.description.trim(),
        cloudConfig: cloudConfigYaml,
        cloudConfigBase64: cloudConfigBase64,
        updatedAt: new Date().toISOString()
      }
      
      if (editingTemplate.value) {
        // Update existing template - createdAt 유지
        if (editingTemplate.value.createdAt) {
          templateData.createdAt = editingTemplate.value.createdAt
        }
        const index = cloudConfigTemplates.value.findIndex(t => t.id === editingTemplate.value.id)
        if (index !== -1) {
          cloudConfigTemplates.value[index] = templateData
        }
      } else {
        // Add new template
        templateData.createdAt = new Date().toISOString()
        cloudConfigTemplates.value.push(templateData)
      }
      
      try {
        await saveCloudConfigTemplates()
        closeTemplateModal()
      } catch (err) {
        await customAlert('템플릿 저장에 실패했습니다: ' + (err.response?.data?.error || err.message || 'Unknown error'), '오류')
      }
    }
    
    const deleteTemplate = async (templateId) => {
      const template = cloudConfigTemplates.value.find(t => t.id === templateId)
      const templateName = template ? template.name : 'this template'
      const confirmMessage = `템플릿 "${templateName}"을(를) 삭제하시겠습니까?\n\n이 작업은 되돌릴 수 없습니다.`
      const confirmed = await customConfirm(confirmMessage, '삭제 확인', '삭제')
      if (confirmed) {
        cloudConfigTemplates.value = cloudConfigTemplates.value.filter(t => t.id !== templateId)
        try {
          await saveCloudConfigTemplates()
          // If the deleted template was being edited, close the modal
          if (editingTemplate.value && editingTemplate.value.id === templateId) {
            closeTemplateModal()
          }
        } catch (err) {
          await customAlert('템플릿 삭제에 실패했습니다: ' + (err.response?.data?.error || err.message || 'Unknown error'), '오류')
        }
      }
    }
    
    const loadTags = async () => {
      loadingTags.value = true
      tagsError.value = null
      
      try {
        const settings = settingsStore.settings
        if (!settings.maasUrl || !settings.apiKey) {
          tagsError.value = 'MAAS URL and API Key must be configured in Settings'
          loadingTags.value = false
          return
        }
        
        const response = await axios.get('http://localhost:8081/api/tags', {
          params: {
            maasUrl: settings.maasUrl,
            apiKey: settings.apiKey
          }
        })
        
        if (response.data.results) {
          tags.value = response.data.results
        } else {
          tags.value = []
        }
      } catch (err) {
        console.error('Error loading tags:', err)
        tagsError.value = err.response?.data?.error || err.message || 'Failed to load tags'
      } finally {
        loadingTags.value = false
      }
    }
    
    const openTagModal = (tag) => {
      if (tag) {
        editingTag.value = tag
        tagForm.value = {
          name: tag.name || '',
          comment: tag.comment || '',
          kernelOpts: tag.kernel_opts || '',
          definition: tag.definition || ''
        }
      } else {
        editingTag.value = null
        tagForm.value = {
          name: '',
          comment: '',
          kernelOpts: '',
          definition: ''
        }
      }
      // Reset modal position when opening
      tagModalPosition.value = { top: 0, left: 0 }
      showTagModal.value = true
    }
    
    const closeTagModal = () => {
      showTagModal.value = false
      editingTag.value = null
      tagForm.value = {
        name: '',
        comment: '',
        kernelOpts: '',
        definition: ''
      }
      // Reset modal position
      tagModalPosition.value = { top: 0, left: 0 }
    }
    
    const closeTemplateModal = () => {
      showTemplateModal.value = false
      editingTemplate.value = null
      templateForm.value = {
        name: '',
        selectedTags: [],
        description: '',
        cloudConfig: ''
      }
      // Reset modal position
      templateModalPosition.value = { top: 0, left: 0 }
    }
    
    const isTagFormValid = computed(() => {
      return tagForm.value.name && tagForm.value.name.trim().length > 0
    })
    
    // Tag 폼이 실제로 변경되었는지 확인
    const isTagFormChanged = computed(() => {
      if (!editingTag.value) {
        // 새로 생성하는 경우는 항상 활성화 (유효성 검사만)
        return isTagFormValid.value
      }
      
      // 편집 모드: 원본 데이터와 비교
      const original = editingTag.value
      const current = tagForm.value
      
      return original.name !== current.name.trim() ||
             (original.comment || '') !== (current.comment || '') ||
             (original.kernel_opts || '') !== (current.kernelOpts || '') ||
             (original.definition || '') !== (current.definition || '')
    })
    
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
      // Reset modal position when closing
      confirmModalPosition.value = { top: 0, left: 0 }
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
    
    // Confirm Modal drag handlers
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
        if (!confirmModalPosition.value.top && !confirmModalPosition.value.left) {
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
    
    const saveTag = async () => {
      if (!isTagFormValid.value || creatingTag.value) {
        return
      }
      
      creatingTag.value = true
      
      try {
        const settings = settingsStore.settings
        if (!settings.maasUrl || !settings.apiKey) {
          await customAlert('MAAS URL and API Key are required', '오류')
          creatingTag.value = false
          return
        }
        
        const formData = new URLSearchParams()
        formData.append('name', tagForm.value.name.trim())
        formData.append('comment', tagForm.value.comment || '')
        formData.append('definition', tagForm.value.definition || '')
        
        let response
        if (editingTag.value) {
          // Update existing tag
          const oldName = editingTag.value.name
          response = await axios.put(`http://localhost:8081/api/tags/${encodeURIComponent(oldName)}`, formData, {
            params: {
              maasUrl: settings.maasUrl,
              apiKey: settings.apiKey
            },
            headers: {
              'Content-Type': 'application/x-www-form-urlencoded'
            }
          })
        } else {
          // Create new tag
          formData.append('kernelOpts', tagForm.value.kernelOpts || '')
          response = await axios.post('http://localhost:8081/api/tags', formData, {
            params: {
              maasUrl: settings.maasUrl,
              apiKey: settings.apiKey
            },
            headers: {
              'Content-Type': 'application/x-www-form-urlencoded'
            }
          })
        }
        
        if (response.data.success) {
          closeTagModal()
          // Reload tags list
          await loadTags()
        } else {
          await customAlert((editingTag.value ? '태그 수정 실패: ' : '태그 생성 실패: ') + (response.data.error || 'Unknown error'), '오류')
        }
      } catch (err) {
        console.error('Error saving tag:', err)
        await customAlert((editingTag.value ? '태그 수정 실패: ' : '태그 생성 실패: ') + (err.response?.data?.error || err.message || 'Unknown error'), '오류')
      } finally {
        creatingTag.value = false
      }
    }
    
    const deleteTag = async () => {
      if (!editingTag.value || creatingTag.value) {
        return
      }
      
      const confirmMessage = `태그 "${editingTag.value.name}"을(를) 삭제하시겠습니까?\n\n이 작업은 되돌릴 수 없습니다.`
      const confirmed = await customConfirm(confirmMessage, '삭제 확인', '삭제')
      if (!confirmed) {
        return
      }
      
      creatingTag.value = true
      
      try {
        const settings = settingsStore.settings
        if (!settings.maasUrl || !settings.apiKey) {
          await customAlert('MAAS URL and API Key are required', '오류')
          creatingTag.value = false
          return
        }
        
        const response = await axios.delete(`http://localhost:8081/api/tags/${encodeURIComponent(editingTag.value.name)}`, {
          params: {
            maasUrl: settings.maasUrl,
            apiKey: settings.apiKey
          }
        })
        
        if (response.data.success) {
          closeTagModal()
          // Reload tags list
          await loadTags()
        } else {
          await customAlert('태그 삭제 실패: ' + (response.data.error || 'Unknown error'), '오류')
        }
      } catch (err) {
        console.error('Error deleting tag:', err)
        await customAlert('태그 삭제 실패: ' + (err.response?.data?.error || err.message || 'Unknown error'), '오류')
      } finally {
        creatingTag.value = false
      }
    }
    
    // Template Modal drag handlers
    const startDragTemplateModal = (event) => {
      if (event.button !== 0) return // Only left mouse button
      isDraggingTemplateModal.value = true
      const modalElement = event.currentTarget.closest('.template-modal-content')
      if (modalElement) {
        const rect = modalElement.getBoundingClientRect()
        templateDragStartPosition.value = {
          x: event.clientX - rect.left,
          y: event.clientY - rect.top
        }
        // If modal hasn't been moved yet, center it
        if (!templateModalPosition.value.top && !templateModalPosition.value.left) {
          const viewportWidth = window.innerWidth
          const viewportHeight = window.innerHeight
          const modalWidth = rect.width
          const modalHeight = rect.height
          templateModalPosition.value = {
            top: (viewportHeight - modalHeight) / 2,
            left: (viewportWidth - modalWidth) / 2
          }
        }
      }
      event.preventDefault()
    }
    
    const onDragTemplateModal = (event) => {
      if (!isDraggingTemplateModal.value) return
      
      const viewportWidth = window.innerWidth
      const viewportHeight = window.innerHeight
      const modalElement = document.querySelector('.template-modal-content')
      if (!modalElement) return
      
      const modalWidth = modalElement.offsetWidth
      const modalHeight = modalElement.offsetHeight
      
      let newLeft = event.clientX - templateDragStartPosition.value.x
      let newTop = event.clientY - templateDragStartPosition.value.y
      
      newLeft = Math.max(0, Math.min(newLeft, viewportWidth - modalWidth))
      newTop = Math.max(0, Math.min(newTop, viewportHeight - modalHeight))
      
      templateModalPosition.value = {
        left: newLeft,
        top: newTop
      }
    }
    
    const stopDragTemplateModal = () => {
      isDraggingTemplateModal.value = false
    }
    
    // Tag Modal drag handlers
    const startDragTagModal = (event) => {
      if (event.button !== 0) return
      isDraggingTagModal.value = true
      const modalElement = event.currentTarget.closest('.tag-modal-content')
      if (modalElement) {
        const rect = modalElement.getBoundingClientRect()
        tagDragStartPosition.value = {
          x: event.clientX - rect.left,
          y: event.clientY - rect.top
        }
        if (!tagModalPosition.value.top && !tagModalPosition.value.left) {
          const viewportWidth = window.innerWidth
          const viewportHeight = window.innerHeight
          const modalWidth = rect.width
          const modalHeight = rect.height
          tagModalPosition.value = {
            top: (viewportHeight - modalHeight) / 2,
            left: (viewportWidth - modalWidth) / 2
          }
        }
      }
      event.preventDefault()
    }
    
    const onDragTagModal = (event) => {
      if (!isDraggingTagModal.value) return
      
      const viewportWidth = window.innerWidth
      const viewportHeight = window.innerHeight
      const modalElement = document.querySelector('.tag-modal-content')
      if (!modalElement) return
      
      const modalWidth = modalElement.offsetWidth
      const modalHeight = modalElement.offsetHeight
      
      let newLeft = event.clientX - tagDragStartPosition.value.x
      let newTop = event.clientY - tagDragStartPosition.value.y
      
      newLeft = Math.max(0, Math.min(newLeft, viewportWidth - modalWidth))
      newTop = Math.max(0, Math.min(newTop, viewportHeight - modalHeight))
      
      tagModalPosition.value = {
        left: newLeft,
        top: newTop
      }
    }
    
    const stopDragTagModal = () => {
      isDraggingTagModal.value = false
    }
    
    // Add global mousemove and mouseup listeners when dragging
    watch(isDraggingTemplateModal, (dragging) => {
      if (dragging) {
        document.addEventListener('mousemove', onDragTemplateModal)
        document.addEventListener('mouseup', stopDragTemplateModal)
      } else {
        document.removeEventListener('mousemove', onDragTemplateModal)
        document.removeEventListener('mouseup', stopDragTemplateModal)
      }
    })
    
    watch(isDraggingTagModal, (dragging) => {
      if (dragging) {
        document.addEventListener('mousemove', onDragTagModal)
        document.addEventListener('mouseup', stopDragTagModal)
      } else {
        document.removeEventListener('mousemove', onDragTagModal)
        document.removeEventListener('mouseup', stopDragTagModal)
      }
    })
    
    onMounted(() => {
      loadDeployableOS()
      loadCloudConfigTemplates()
      loadTags()
      loadMachines()
    })
    
    return {
      deployableOS,
      loading,
      error,
      formatOS,
      formatVersion,
      formatArchitectures,
      loadDeployableOS,
      getDeployedMachineCount,
      groupedOS,
      // Tags
      tags,
      loadingTags,
      tagsError,
      loadTags,
      showTagModal,
      creatingTag,
      editingTag,
      tagForm,
      isTagFormValid,
      openTagModal,
      closeTagModal,
      saveTag,
      deleteTag,
      isTagFormChanged,
      isDraggingTagModal,
      tagModalPosition,
      startDragTagModal,
      // Cloud-Config Templates
      cloudConfigTemplates,
      showTemplateModal,
      editingTemplate,
      templateForm,
      isTemplateFormValid,
      openTemplateModal,
      closeTemplateModal,
      saveTemplate,
      deleteTemplate,
      isTemplateFormChanged,
      isDraggingTemplateModal,
      templateModalPosition,
      startDragTemplateModal,
      // Confirmation & Alert Modals
      showConfirmModal,
      showAlertModal,
      confirmModalTitle,
      confirmModalMessage,
      confirmModalButtonText,
      confirmAction,
      cancelConfirm,
      closeAlert,
      customConfirm,
      customAlert,
      isDraggingConfirmModal,
      confirmModalPosition,
      startDragConfirmModal
    }
  }
}
</script>

<style scoped>
.configuration h2 {
  margin: 0 0 2rem 0;
  padding: 0;
  color: #2c3e50;
  font-size: 1.75rem;
  line-height: 1.2;
  min-height: calc(1.75rem * 1.2);
  display: block;
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

.os-name-cell {
  vertical-align: top;
  font-weight: 600;
  background-color: #f8f9fa;
  border-right: 2px solid #dee2e6;
}

.tags-list {
  overflow-x: auto;
}

.tags-table {
  width: 100%;
  border-collapse: collapse;
  background: white;
  border-radius: 4px;
  overflow: hidden;
}

.tags-table thead {
  background-color: #2c3e50;
  color: white;
}

.tags-table th {
  padding: 0.75rem 1rem;
  text-align: left;
  font-weight: 600;
  font-size: 0.9rem;
}

.tags-table tbody tr {
  border-bottom: 1px solid #e9ecef;
  transition: background-color 0.2s;
}

.tags-table tbody tr:hover {
  background-color: #f8f9fa;
}

.tags-table tbody tr:last-child {
  border-bottom: none;
}

.tags-table td {
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

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.section-header h3 {
  margin-bottom: 0;
  border-bottom: none;
  padding-bottom: 0;
}

.templates-list {
  overflow-x: auto;
}

.templates-table {
  width: 100%;
  border-collapse: collapse;
  background: white;
  border-radius: 4px;
  overflow: hidden;
}

.templates-table thead {
  background-color: #2c3e50;
  color: white;
}

.templates-table th {
  padding: 0.75rem 1rem;
  text-align: left;
  font-weight: 600;
  font-size: 0.9rem;
}

.templates-table tbody tr {
  border-bottom: 1px solid #e9ecef;
  transition: background-color 0.2s;
}

.templates-table tbody tr:hover {
  background-color: #f8f9fa;
}

.templates-table tbody tr:last-child {
  border-bottom: none;
}

.templates-table td {
  padding: 0.75rem 1rem;
  color: #495057;
}

.tags-container {
  display: flex;
  flex-wrap: wrap;
  gap: 0.25rem;
}

.tag-badge {
  display: inline-block;
  padding: 0.25rem 0.5rem;
  background-color: #007bff;
  color: white;
  border-radius: 4px;
  font-size: 0.75rem;
  font-weight: 600;
}

.no-tag {
  color: #6c757d;
  font-style: italic;
}

.action-buttons {
  display: flex;
  gap: 0.5rem;
}

.btn-edit {
  background-color: #28a745;
  color: white;
  border: none;
  padding: 0.375rem 0.75rem;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.875rem;
  transition: background-color 0.2s;
}

.btn-edit:hover {
  background-color: #218838;
}

.btn-delete {
  background-color: #dc3545;
  color: white;
  border: none;
  padding: 0.375rem 0.75rem;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.875rem;
  transition: background-color 0.2s;
}

.btn-delete:hover {
  background-color: #c82333;
}

.btn-secondary {
  background-color: #6c757d;
  color: white;
  border: none;
  padding: 0.5rem 1rem;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.9rem;
  transition: background-color 0.2s;
}

.btn-secondary:hover {
  background-color: #545b62;
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
  z-index: 10000;
}

.modal-content {
  background: white;
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
  max-width: 700px;
  width: 90%;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
}

.template-modal-content {
  max-width: 800px;
}

.tag-modal-content {
  max-width: 600px;
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
  border-radius: 4px;
  transition: background-color 0.2s;
}

.close-btn:hover {
  background-color: #f8f9fa;
}

.modal-body {
  padding: 1.5rem;
  overflow-y: auto;
  flex: 1;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 1rem;
  padding: 1.5rem;
  border-top: 1px solid #e9ecef;
}

.form-section {
  margin-bottom: 1.5rem;
}

.form-label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 600;
  color: #495057;
  font-size: 0.9rem;
}

.form-input,
.form-textarea {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid #ced4da;
  border-radius: 6px;
  font-size: 0.9rem;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
  box-sizing: border-box;
  font-family: inherit;
}

.form-textarea {
  resize: vertical;
  min-height: 100px;
}

.form-textarea.code-editor {
  font-family: 'Courier New', monospace;
  font-size: 0.85rem;
}

.form-input:focus,
.form-textarea:focus {
  outline: none;
  border-color: #007bff;
  box-shadow: 0 0 0 3px rgba(0, 123, 255, 0.1);
}

.form-hint {
  font-size: 0.75rem;
  color: #6c757d;
  margin-top: 0.25rem;
}

.loading-tags,
.no-tags-message {
  padding: 1rem;
  text-align: center;
  color: #6c757d;
  font-size: 0.9rem;
}

.tags-selection {
  border: 1px solid #dee2e6;
  border-radius: 6px;
  padding: 1rem;
  background-color: #f8f9fa;
  max-height: 300px;
  overflow-y: auto;
}

.tags-checkbox-container {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.tag-checkbox-label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  cursor: pointer;
  padding: 0.5rem;
  border-radius: 4px;
  transition: background-color 0.2s;
}

.tag-checkbox-label:hover {
  background-color: #e9ecef;
}

.tag-checkbox {
  cursor: pointer;
  width: 18px;
  height: 18px;
}

.tag-comment {
  font-size: 0.85rem;
  color: #6c757d;
  font-style: italic;
  margin-left: 0.25rem;
}

.tag-row {
  cursor: pointer;
  transition: background-color 0.2s;
}

.tag-row:hover {
  background-color: #f8f9fa;
}

.template-row {
  cursor: pointer;
  transition: background-color 0.2s;
}

.template-row:hover {
  background-color: #f8f9fa;
}

.modal-draggable-header {
  user-select: none;
  -webkit-user-select: none;
  -moz-user-select: none;
  -ms-user-select: none;
}

.modal-draggable-header:active {
  cursor: grabbing !important;
}

/* Confirm & Alert Modal Styles */
.confirm-modal-content,
.alert-modal-content {
  max-width: 500px;
  width: 90%;
}

.confirm-modal-body,
.alert-modal-body {
  padding: 1.5rem;
}

.confirm-message,
.alert-message {
  margin: 0;
  white-space: pre-line;
  line-height: 1.6;
  color: #495057;
}

.confirm-modal-footer,
.alert-modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  padding: 1rem 1.5rem;
  border-top: 1px solid #e9ecef;
}
</style>

