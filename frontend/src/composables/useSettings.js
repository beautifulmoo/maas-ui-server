import { reactive, computed } from 'vue'

// 백엔드 application.properties와 일치하는 기본값
const DEFAULT_MAAS_URL = 'http://192.168.189.71:5240'
const DEFAULT_API_KEY = 'PHjPyRf0iCJyyrGZic:VCrnm5FoEa6dYhTFOV:nuBrXB3YBH5uztxKyhMYZYl1ulvUvrav'

// localStorage에서 설정 로드
const loadSettings = () => {
  const savedSettings = localStorage.getItem('maas-ui-settings')
  if (savedSettings) {
    try {
      const parsed = JSON.parse(savedSettings)
      return {
        maasUrl: parsed.maasUrl || DEFAULT_MAAS_URL,
        apiKey: parsed.apiKey || DEFAULT_API_KEY,
        refreshInterval: parsed.refreshInterval || 30,
        autoRefresh: parsed.autoRefresh !== undefined ? parsed.autoRefresh : true,
        itemsPerPage: parsed.itemsPerPage || 25,
        showAdvancedInfo: parsed.showAdvancedInfo || false
      }
    } catch (e) {
      console.error('Error parsing saved settings:', e)
    }
  }
  
  // 기본값 반환
  return {
    maasUrl: DEFAULT_MAAS_URL,
    apiKey: DEFAULT_API_KEY,
    refreshInterval: 30,
    autoRefresh: true,
    itemsPerPage: 25,
    showAdvancedInfo: false
  }
}

// 설정 저장
const saveSettings = (settings) => {
  try {
    localStorage.setItem('maas-ui-settings', JSON.stringify(settings))
    return true
  } catch (e) {
    console.error('Error saving settings:', e)
    return false
  }
}

/**
 * MAAS 설정을 관리하는 composable
 * @returns {Object} 설정 객체와 유틸리티 함수들
 */
export function useSettings() {
  const settings = reactive(loadSettings())
  
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
  
  // 설정 저장
  const save = () => {
    return saveSettings(settings)
  }
  
  // 설정 새로고침 (localStorage에서 다시 로드)
  const refresh = () => {
    const loaded = loadSettings()
    Object.assign(settings, loaded)
  }
  
  // 설정 리셋
  const reset = () => {
    Object.assign(settings, {
      maasUrl: DEFAULT_MAAS_URL,
      apiKey: DEFAULT_API_KEY,
      refreshInterval: 30,
      autoRefresh: true,
      itemsPerPage: 25,
      showAdvancedInfo: false
    })
    localStorage.removeItem('maas-ui-settings')
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

