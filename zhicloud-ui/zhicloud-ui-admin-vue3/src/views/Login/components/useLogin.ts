import { Ref } from 'vue'

export enum LoginStateEnum {
  LOGIN,
  REGISTER,
  RESET_PASSWORD,
  MOBILE,
  QR_CODE,
  SSO
}

const currentState = ref(LoginStateEnum.LOGIN)

// ===== 统一验证码状态（登录/注册表单共享）=====
export const captchaState = ref({
  enable: import.meta.env.VITE_APP_CAPTCHA_ENABLE === 'true',
  type: 'blockPuzzle' // blockPuzzle 滑块 clickWord 点击文字 pictureWord 文字验证码
})

export function useLoginState() {
  function setLoginState(state: LoginStateEnum) {
    currentState.value = state
  }
  const getLoginState = computed(() => currentState.value)

  function handleBackLogin() {
    setLoginState(LoginStateEnum.LOGIN)
  }

  return {
    setLoginState,
    getLoginState,
    handleBackLogin
  }
}

export function useFormValid<T extends Object = any>(formRef: Ref<any>) {
  async function validForm() {
    const form = unref(formRef)
    if (!form) return
    const data = await form.validate()
    return data as T
  }

  return {
    validForm
  }
}
