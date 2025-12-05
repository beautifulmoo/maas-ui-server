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
      
      <div class="configuration-section">
        <h3>Tags</h3>
        
        <div v-if="loadingTags" class="loading">
          <p>Loading tags...</p>
        </div>
        
        <div v-else-if="tagsError" class="error">
          <p>{{ tagsError }}</p>
          <button @click="loadTags" class="btn-primary btn-sm">Retry</button>
        </div>
        
        <div v-else-if="tags.length === 0" class="no-data">
          <p>No tags found.</p>
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
              <tr v-for="tag in tags" :key="tag.name">
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
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="template in cloudConfigTemplates" :key="template.id">
                <td>{{ template.name }}</td>
                <td>
                  <div class="tags-container" v-if="template.tags && template.tags.length > 0">
                    <span v-for="tag in template.tags" :key="tag" class="tag-badge">{{ tag }}</span>
                  </div>
                  <span v-else class="no-tag">-</span>
                </td>
                <td>{{ template.description || '-' }}</td>
                <td>
                  <div class="action-buttons">
                    <button @click="openTemplateModal(template)" class="btn-edit btn-sm">Edit</button>
                    <button @click="deleteTemplate(template.id)" class="btn-delete btn-sm">Delete</button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
    
    <!-- Cloud-Config Template Modal -->
    <div v-if="showTemplateModal" class="modal-overlay" @click="closeTemplateModal">
      <div class="modal-content template-modal-content" @click.stop>
        <div class="modal-header">
          <h3>{{ editingTemplate ? 'Edit Template' : 'New Template' }}</h3>
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
          <button @click="closeTemplateModal" class="btn-secondary btn-sm">Cancel</button>
          <button @click="saveTemplate" class="btn-primary btn-sm" :disabled="!isTemplateFormValid">
            {{ editingTemplate ? 'Update' : 'Create' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted, computed } from 'vue'
import axios from 'axios'
import { useSettings } from '../composables/useSettings'

const STORAGE_KEY = 'maas-cloud-config-templates'

export default {
  name: 'ConfigurationTab',
  setup() {
    const settingsStore = useSettings()
    const deployableOS = ref([])
    const loading = ref(false)
    const error = ref(null)
    
    // Tags
    const tags = ref([])
    const loadingTags = ref(false)
    const tagsError = ref(null)
    
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
    
    // Cloud-Config Templates Functions
    const loadCloudConfigTemplates = () => {
      try {
        const stored = localStorage.getItem(STORAGE_KEY)
        if (stored) {
          cloudConfigTemplates.value = JSON.parse(stored)
        } else {
          cloudConfigTemplates.value = []
        }
      } catch (err) {
        console.error('Error loading cloud-config templates:', err)
        cloudConfigTemplates.value = []
      }
    }
    
    const saveCloudConfigTemplates = () => {
      try {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(cloudConfigTemplates.value))
      } catch (err) {
        console.error('Error saving cloud-config templates:', err)
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
      showTemplateModal.value = true
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
    }
    
    const isTemplateFormValid = computed(() => {
      return templateForm.value.name.trim() !== '' && 
             templateForm.value.cloudConfig.trim() !== ''
    })
    
    const saveTemplate = () => {
      if (!isTemplateFormValid.value) {
        return
      }
      
      // Use selected tags directly
      const selectedTags = templateForm.value.selectedTags || []
      
      const templateData = {
        id: editingTemplate.value ? editingTemplate.value.id : Date.now().toString(),
        name: templateForm.value.name.trim(),
        tags: selectedTags,
        description: templateForm.value.description.trim(),
        cloudConfig: templateForm.value.cloudConfig.trim(),
        updatedAt: new Date().toISOString()
      }
      
      if (editingTemplate.value) {
        // Update existing template
        const index = cloudConfigTemplates.value.findIndex(t => t.id === editingTemplate.value.id)
        if (index !== -1) {
          cloudConfigTemplates.value[index] = templateData
        }
      } else {
        // Add new template
        templateData.createdAt = new Date().toISOString()
        cloudConfigTemplates.value.push(templateData)
      }
      
      saveCloudConfigTemplates()
      closeTemplateModal()
    }
    
    const deleteTemplate = (templateId) => {
      if (confirm('Are you sure you want to delete this template?')) {
        cloudConfigTemplates.value = cloudConfigTemplates.value.filter(t => t.id !== templateId)
        saveCloudConfigTemplates()
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
    
    onMounted(() => {
      loadDeployableOS()
      loadCloudConfigTemplates()
      loadTags()
    })
    
    return {
      deployableOS,
      loading,
      error,
      formatOS,
      formatVersion,
      formatArchitectures,
      loadDeployableOS,
      // Tags
      tags,
      loadingTags,
      tagsError,
      loadTags,
      // Cloud-Config Templates
      cloudConfigTemplates,
      showTemplateModal,
      editingTemplate,
      templateForm,
      isTemplateFormValid,
      openTemplateModal,
      closeTemplateModal,
      saveTemplate,
      deleteTemplate
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
</style>

