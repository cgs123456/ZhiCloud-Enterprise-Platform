<template>
  <div
    :class="prefixCls"
    class="relative h-[100%] lt-md:px-10px lt-sm:px-10px lt-xl:px-10px lt-xl:px-10px"
  >
    <div class="relative mx-auto h-full flex">
      <!-- 左侧品牌面板：深蓝渐变 + 产品定位文案，替代原卡通插画 -->
      <div :class="`${prefixCls}__left flex-1 relative lt-xl:hidden overflow-hidden`">
        <div class="relative z-10 h-full flex flex-col p-48px lt-2xl:p-32px">
          <!-- 品牌标识 -->
          <div class="brand-fade flex items-center text-white">
            <img alt="" class="mr-12px h-44px w-44px" src="@/assets/imgs/logo.png" />
            <span class="text-20px font-600 tracking-wide">
              {{ underlineToHump(appStore.getTitle) }}
            </span>
          </div>
          <!-- 定位文案 -->
          <div class="max-w-500px flex-1 flex flex-col justify-center">
            <div class="brand-fade brand-fade-1 text-13px font-500 tracking-2px text-[#8ab6e8]">
              {{ t('login.brandEyebrow') }}
            </div>
            <h1
              class="brand-fade brand-fade-2 mt-16px whitespace-pre-line text-38px leading-[1.2] font-600 text-white"
            >
              {{ t('login.brandTitle') }}
            </h1>
            <p
              class="brand-fade brand-fade-3 mt-16px max-w-[46ch] text-15px leading-[1.8] text-white/65"
            >
              {{ t('login.brandMessage') }}
            </p>
            <!-- 核心能力 -->
            <ul class="brand-fade brand-fade-4 mt-40px space-y-20px">
              <li
                v-for="item in capabilities"
                :key="item.title"
                class="flex items-center text-white/85"
              >
                <span
                  class="mr-12px flex h-36px w-36px shrink-0 items-center justify-center rounded-8px bg-white/8"
                >
                  <Icon :icon="item.icon" :size="18" />
                </span>
                <span class="text-14px">
                  <span class="font-500 text-white">{{ item.title }}</span>
                  <span class="ml-8px text-13px text-white/50">{{ item.desc }}</span>
                </span>
              </li>
            </ul>
          </div>
          <!-- 版权 -->
          <div class="brand-fade brand-fade-5 text-12px text-white/40">
            © {{ year }} {{ appStore.getTitle }}
          </div>
        </div>
      </div>
      <div
        class="relative flex-1 p-30px dark:bg-[var(--login-bg-color)] lt-sm:p-10px overflow-x-hidden overflow-y-auto"
      >
        <!-- 右上角的主题、语言选择 -->
        <div
          class="flex items-center justify-between at-2xl:justify-end at-xl:justify-end"
          style="color: var(--el-text-color-primary)"
        >
          <div class="flex items-center at-2xl:hidden at-xl:hidden">
            <img alt="" class="mr-10px h-48px w-48px" src="@/assets/imgs/logo.png" />
            <span class="text-20px font-bold">{{ underlineToHump(appStore.getTitle) }}</span>
          </div>
          <div class="flex items-center justify-end space-x-10px h-48px">
            <ThemeSwitch />
            <LocaleDropdown />
          </div>
        </div>
        <!-- 右边的登录界面 -->
        <div
          class="form-fade m-auto h-[calc(100%-60px)] w-[100%] flex items-center at-2xl:max-w-500px at-lg:max-w-500px at-md:max-w-500px at-xl:max-w-500px"
        >
          <!-- 账号登录 -->
          <LoginForm class="m-auto h-auto p-20px lt-xl:(rounded-3xl light:bg-white)" />
          <!-- 手机登录 -->
          <MobileForm class="m-auto h-auto p-20px lt-xl:(rounded-3xl light:bg-white)" />
          <!-- 二维码登录 -->
          <QrCodeForm class="m-auto h-auto p-20px lt-xl:(rounded-3xl light:bg-white)" />
          <!-- 注册 -->
          <RegisterForm class="m-auto h-auto p-20px lt-xl:(rounded-3xl light:bg-white)" />
          <!-- 三方登录 -->
          <SSOLoginVue class="m-auto h-auto p-20px lt-xl:(rounded-3xl light:bg-white)" />
          <!-- 忘记密码 -->
          <ForgetPasswordForm class="m-auto h-auto p-20px lt-xl:(rounded-3xl light:bg-white)" />
        </div>
      </div>
    </div>
  </div>
</template>
<script lang="ts" setup>
import { underlineToHump } from '@/utils'

import { useDesign } from '@/hooks/web/useDesign'
import { useAppStore } from '@/store/modules/app'
import { ThemeSwitch } from '@/layout/components/ThemeSwitch'
import { LocaleDropdown } from '@/layout/components/LocaleDropdown'

import {
  LoginForm,
  MobileForm,
  QrCodeForm,
  RegisterForm,
  SSOLoginVue,
  ForgetPasswordForm
} from './components'

defineOptions({ name: 'Login' })

const { t } = useI18n()
const appStore = useAppStore()
const { getPrefixCls } = useDesign()
const prefixCls = getPrefixCls('login')
const year = new Date().getFullYear()

// 左侧面板核心能力（与平台实际启用模块对齐，不虚构数据）
const capabilities = [
  { icon: 'ep:grid', title: t('login.capabilityDomainTitle'), desc: t('login.capabilityDomainDesc') },
  {
    icon: 'ep:set-up',
    title: t('login.capabilityWorkflowTitle'),
    desc: t('login.capabilityWorkflowDesc')
  },
  { icon: 'tabler:ai', title: t('login.capabilityAiTitle'), desc: t('login.capabilityAiDesc') }
]
</script>

<style lang="scss" scoped>
$prefix-cls: #{$namespace}-login;

.#{$prefix-cls} {
  overflow: auto;

  &__left {
    // 品牌渐变背景：主色蓝的深色调低饱和衍生，非 AI-purple
    background:
      radial-gradient(ellipse 640px 420px at 85% 12%, rgba(64, 158, 255, 0.16), transparent 62%),
      radial-gradient(ellipse 520px 400px at 8% 92%, rgba(64, 158, 255, 0.1), transparent 58%),
      linear-gradient(168deg, #1c2637 0%, #293146 58%, #2b3a55 100%);
  }
}

// 进场动效：一次性 fade-up 错落，替代原 bounce 弹跳；低动效偏好下降级为直出
@media (prefers-reduced-motion: no-preference) {
  .brand-fade {
    animation: brand-fade-up 0.55s cubic-bezier(0.16, 1, 0.3, 1) both;

    &-1 {
      animation-delay: 0.04s;
    }

    &-2 {
      animation-delay: 0.1s;
    }

    &-3 {
      animation-delay: 0.16s;
    }

    &-4 {
      animation-delay: 0.24s;
    }

    &-5 {
      animation-delay: 0.32s;
    }
  }

  .form-fade {
    animation: form-fade-in 0.45s cubic-bezier(0.16, 1, 0.3, 1) both;
  }
}

@keyframes brand-fade-up {
  from {
    opacity: 0;
    transform: translateY(14px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes form-fade-in {
  from {
    opacity: 0;
    transform: translateY(8px);
  }

  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>

<style lang="scss">
.dark .login-form {
  .el-divider__text {
    background-color: var(--login-bg-color);
  }

  .el-card {
    background-color: var(--login-bg-color);
  }
}
</style>
