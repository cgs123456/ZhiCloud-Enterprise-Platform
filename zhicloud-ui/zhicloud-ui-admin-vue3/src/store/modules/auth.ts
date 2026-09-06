import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

export enum LoginStateEnum {
  LOGIN,
  REGISTER,
  RESET_PASSWORD,
  MOBILE,
  QR_CODE,
  SSO
}

// ===== Pinia Store: Auth =====
export const useAuthStore = defineStore('auth', () => {
  // Login state management
  const currentState = ref<LoginStateEnum>(LoginStateEnum.LOGIN)

  function setLoginState(state: LoginStateEnum) {
    currentState.value = state
  }

  const getLoginState = computed(() => currentState.value)

  function handleBackLogin() {
    setLoginState(LoginStateEnum.LOGIN)
  }

  // ===== Captcha state management (Unified) =====
  const captchaState = ref({
    enable: false,
    type: 'blockPuzzle' // blockPuzzle, clickWord, pictureWord
  })

  function setCaptchaEnable(enable: boolean) {
    captchaState.value.enable = enable
  }

  function setCaptchaType(type: string) {
    captchaState.value.type = type
  }

  // ===== User info (optional) =====
  const userInfo = ref<any>(null)
  const token = ref<string | null>(null)

  function setUserInfo(info: any) {
    userInfo.value = info
  }

  function setToken(tok: string | null) {
    token.value = tok
  }

  function clearAuth() {
    currentState.value = LoginStateEnum.LOGIN
    captchaState.value = { enable: false, type: 'blockPuzzle' }
    userInfo.value = null
    token.value = null
  }

  return {
    // State
    currentState,
    getLoginState,
    captchaState,
    userInfo,
    token,

    // Actions
    setLoginState,
    handleBackLogin,
    setCaptchaEnable,
    setCaptchaType,
    setUserInfo,
    setToken,
    clearAuth
  }
})