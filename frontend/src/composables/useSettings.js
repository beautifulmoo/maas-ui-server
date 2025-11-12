import { reactive, computed } from 'vue'
import axios from 'axios'

// 백엔드 application.properties와 일치하는 기본값
const DEFAULT_MAAS_URL = 'http://192.168.189.71:5240'
// 기존 API Key (나중에 다시 사용할 수 있으므로 주석처리)
// const DEFAULT_API_KEY = 'PHjPyRf0iCJyyrGZic:VCrnm5FoEa6dYhTFOV:nuBrXB3YBH5uztxKyhMYZYl1ulvUvrav'
const DEFAULT_API_KEY = 'idwbvWv1cxnNajGSaX:nBFwezD08jUznN0pMA:VcrKMpOquA0C31egWGM07kxDEZTXVAiF'

const API_BASE_URL = 'http://localhost:8081/api'

// 기본값 반환
const getDefaultSettings = () => {
  return {
    maasUrl: DEFAULT_MAAS_URL,
    apiKey: DEFAULT_API_KEY,
    refreshInterval: 30,
    autoRefresh: true,
    itemsPerPage: 25,
    showAdvancedInfo: false
  }
}

// 백엔드 API에서 설정 로드
const loadSettings = async () => {
  try {
    const response = await axios.get(`${API_BASE_URL}/settings`)
    if (response.data && !response.data.error) {
      // 기본값과 병합하여 누락된 필드가 있으면 기본값 사용
      const defaults = getDefaultSettings()
      return {
        maasUrl: response.data.maasUrl || defaults.maasUrl,
        apiKey: response.data.apiKey || defaults.apiKey,
        refreshInterval: response.data.refreshInterval || defaults.refreshInterval,
        autoRefresh: response.data.autoRefresh !== undefined ? response.data.autoRefresh : defaults.autoRefresh,
        itemsPerPage: response.data.itemsPerPage || defaults.itemsPerPage,
        showAdvancedInfo: response.data.showAdvancedInfo !== undefined ? response.data.showAdvancedInfo : defaults.showAdvancedInfo
      }
    }
  } catch (e) {
    console.error('Error loading settings from API:', e)
  }
  
  // 기본값 반환
  return getDefaultSettings()
}

// 백엔드 API에 설정 저장
const saveSettings = async (settings) => {
  try {
    const response = await axios.post(`${API_BASE_URL}/settings`, settings)
    if (response.data && response.data.success) {
      return true
    } else {
      console.error('Failed to save settings:', response.data?.error)
      return false
    }
  } catch (e) {
    console.error('Error saving settings to API:', e)
    return false
  }
}

/**
 * MAAS 설정을 관리하는 composable
 * @returns {Object} 설정 객체와 유틸리티 함수들
 */
export function useSettings() {
  // 초기값으로 기본 설정 사용 (비동기 로드 전까지)
  const settings = reactive(getDefaultSettings())
  
  // 초기 로드 (비동기)
  let isInitialLoad = true
  loadSettings().then(loadedSettings => {
    Object.assign(settings, loadedSettings)
    isInitialLoad = false
  }).catch(err => {
    console.error('Failed to load settings on init:', err)
    isInitialLoad = false
  })
  
  // 설정이 유효한지 확인
  const isValid = computed(() => {
    return settings.maasUrl && settings.maasUrl.trim() !== '' && 
           settings.apiKey && settings.apiKey.trim() !== ''
  })
  
  // API 호출에 사용할 파라미터 객체 반환
  const getApiParams = computed(() => ({
    maasUrl: settings.maasUrl,
    apiKey: settings.apiKey
  }))
  
  // 설정 저장 (비동기)
  const save = async () => {
    return await saveSettings(settings)
  }
  
  // 설정 새로고침 (API에서 다시 로드)
  const refresh = async () => {
    try {
      const loaded = await loadSettings()
      Object.assign(settings, loaded)
    } catch (err) {
      console.error('Failed to refresh settings:', err)
    }
  }
  
  // 설정 리셋
  const reset = async () => {
    const defaults = getDefaultSettings()
    Object.assign(settings, defaults)
    // 리셋된 값도 서버에 저장
    await saveSettings(settings)
  }
  
  return {
    settings,
    isValid,
    getApiParams,
    save,
    refresh,
    reset
  }
}

