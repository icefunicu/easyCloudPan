<template>
  <div class="login-body">
    <div class="bg"></div>
    <div class="login-panel">
      <el-form ref="formDataRef" class="login-register" :model="formData" :rules="rules" @submit.prevent>
        <div class="login-title">Easy云盘</div>
        <!-- input输入 -->
        <el-form-item prop="email">
          <el-input v-model.trim="formData.email" size="large" clearable placeholder="请输入邮箱" maxlength="150">
            <template #prefix>
              <span class="iconfont icon-account"></span>
            </template>
          </el-input>
        </el-form-item>
        <!-- 登录密码 -->
        <el-form-item v-if="opType == 1" prop="password">
          <el-input
            v-model.trim="formData.password"
            type="password"
            size="large"
            placeholder="请输入密码"
            show-password
          >
            <template #prefix>
              <span class="iconfont icon-password"></span>
            </template>
          </el-input>
        </el-form-item>
        <!-- 注册 -->
        <div v-if="opType == 0 || opType == 2">
          <el-form-item prop="emailCode">
            <div class="send-email-panel">
              <el-input v-model.trim="formData.emailCode" size="large" placeholder="请输入邮箱验证码">
                <template #prefix>
                  <span class="iconfont icon-checkcode"></span>
                </template>
              </el-input>
              <el-button class="send-mail-btn" type="primary" size="large" @click="getEmailCode">获取验证码</el-button>
            </div>
            <el-popover placement="left" :width="500" trigger="click">
              <div>
                <p>1.在垃圾箱中查找邮箱验证码</p>
                <p>2.在邮箱中头像->设置->反垃圾->白名单->设置邮件地址白名单</p>
                <p>3.将邮箱【3503201604@qq.com】添加到白名单不知道怎么设置?</p>
              </div>
              <template #reference>
                <span class="a-link" :style="{ 'font-size': '14px' }">未收到邮箱验证码?</span>
              </template>
            </el-popover>
          </el-form-item>
          <!-- 昵称 -->
          <el-form-item v-if="opType == 0" prop="nickName">
            <el-input v-model.trim="formData.nickName" size="large" placeholder="请输入昵称" maxlength="20">
              <template #prefix>
                <span class="iconfont icon-account"></span>
              </template>
            </el-input>
          </el-form-item>
          <!-- 注册密码, 找回密码 -->
          <el-form-item prop="registerPassword">
            <el-input
              v-model.trim="formData.registerPassword"
              type="password"
              size="large"
              placeholder="请输入密码"
              show-password
            >
              <template #prefix>
                <span class="iconfont icon-password"></span>
              </template>
            </el-input>
            <div v-if="formData.registerPassword" class="password-strength-bar">
                <el-progress 
                  :percentage="passwordStrength" 
                  :color="strengthColor"
                  :show-text="false"
                  :stroke-width="6"
                  style="margin-top: 5px"
                />
            </div>
          </el-form-item>
          <!-- 重复密码 -->
          <el-form-item prop="reRegisterPassword">
            <el-input
              v-model.trim="formData.reRegisterPassword"
              type="password"
              size="large"
              placeholder="请再次输入密码"
              show-password
            >
              <template #prefix>
                <span class="iconfont icon-password"></span>
              </template>
            </el-input>
          </el-form-item>
        </div>
        <!-- 验证码 -->
        <el-form-item prop="checkCode">
          <div class="check-code-panel">
            <el-input v-model.trim="formData.checkCode" size="large" placeholder="请输入验证码" @keyup.enter="doSubmit">
              <template #prefix>
                <span class="iconfont icon-checkcode"></span>
              </template>
            </el-input>
            <img :src="checkCodeUrl" class="check-code" @click="changeCheckCode(0)" />
          </div>
        </el-form-item>
        <!-- 登录 -->
        <el-form-item v-if="opType == 1">
          <div class="rememberme-panel">
            <el-checkbox v-model="formData.rememberMe">记住我</el-checkbox>
          </div>
          <div class="no-account">
            <a href="javascript:void(0)" class="a-link" @click="showPanel(2)">忘记密码?</a>
            <a href="javascript:void(0)" class="a-link" @click="showPanel(0)">没有账号?</a>
          </div>
        </el-form-item>
        <!-- 找回密码 -->
        <el-form-item v-if="opType == 2">
          <a href="javascript:void(0)" class="a-link" @click="showPanel(1)">去登录?</a>
        </el-form-item>
        <el-form-item v-if="opType == 0">
          <a href="javascript:void(0)" class="a-link" @click="showPanel(1)">已有账号?</a>
        </el-form-item>
        <!-- 登录按钮 -->
        <el-form-item>
          <el-button type="primary" class="op-btn" size="large" @click="doSubmit" :loading="isSubmitting">
            <span v-if="opType == 0">注册</span>
            <span v-if="opType == 1">登录</span>
            <span v-if="opType == 2">重置密码</span>
          </el-button>
        </el-form-item>
        <div v-if="opType == 1" class="quick-login-panel">
          <div class="quick-login-title">选择其他方式登录</div>
          <div class="oauth-buttons">
            <!-- GitHub -->
            <div class="oauth-btn github" @click="oauthLogin('github')" title="GitHub登录">
              <svg viewBox="0 0 1024 1024" width="24" height="24">
                <path
                  d="M511.6 76.3C264.3 76.2 64 276.4 64 523.5 64 718.9 189.3 885 363.8 946c23.5 5.9 19.9-10.8 19.9-22.2v-77.5c-135.7 15.9-141.2-73.9-150.3-88.9C215 726 171.5 718 184.5 703c30.9-15.9 62.4 4 98.9 57.9 26.4 39.1 77.9 32.5 104 26 5.7-23.5 17.9-44.5 34.7-60.8-140.6-25.2-199.2-111-199.2-213 0-49.5 16.3-95 48.3-131.7-20.4-60.5 1.9-112.3 4.9-120 58.1-5.2 118.5 41.6 123.2 45.3 33-8.9 70.7-13.6 112.9-13.6 42.4 0 80.2 4.9 113.5 13.9 11.3-8.6 67.3-48.8 121.3-43.9 2.9 7.7 24.7 58.3 5.5 118 32.4 36.8 48.9 82.7 48.9 132.3 0 102.2-59 188.1-200 212.9a127.5 127.5 0 0 1 38.1 91v112.5c0.8 9 0 17.9 15 17.9 177.1-59.7 304.6-227 304.6-424.1 0-247.2-200.4-447.3-447.5-447.3z"
                  fill="#333"
                />
              </svg>
            </div>
            <!-- Gitee -->
            <div class="oauth-btn gitee" @click="oauthLogin('gitee')" title="Gitee登录">
              <svg viewBox="0 0 1024 1024" width="24" height="24">
                <path
                  d="M512 1024C230.4 1024 0 793.6 0 512S230.4 0 512 0s512 230.4 512 512-230.4 512-512 512z m259.2-569.6H480c-12.8 0-25.6 12.8-25.6 25.6v64c0 12.8 12.8 25.6 25.6 25.6h176c12.8 0 25.6 12.8 25.6 25.6v12.8c0 41.6-35.2 76.8-76.8 76.8h-240c-12.8 0-25.6-12.8-25.6-25.6V416c0-41.6 35.2-76.8 76.8-76.8h355.2c12.8 0 25.6-12.8 25.6-25.6v-64c0-12.8-12.8-25.6-25.6-25.6H416c-105.6 0-192 86.4-192 192v256c0 105.6 86.4 192 192 192h240c105.6 0 192-86.4 192-192V476.8c0-12.8-12.8-22.4-25.6-22.4z"
                  fill="#C71D23"
                />
              </svg>
            </div>
            <!-- Google -->
            <div class="oauth-btn google" @click="oauthLogin('google')" title="Google登录">
              <svg viewBox="0 0 1024 1024" width="24" height="24">
                <path
                  d="M512 426.666667v170.666666h241.066667c-10.666667 59.733333-68.266667 170.666667-241.066667 170.666667-145.066667 0-262.4-117.333333-262.4-262.4s117.333333-262.4 262.4-262.4c68.266667 0 113.066667 29.866667 138.666667 55.466667L742.4 198.4C680.533333 138.666667 603.733333 106.666667 512 106.666667 288 106.666667 106.666667 288 106.666667 512S288 917.333333 512 917.333333c202.666667 0 339.2-142.933333 339.2-345.6 0-23.466667-2.133333-40.533333-6.4-59.733333l-332.8-85.333333z"
                  fill="#EA4335"
                />
              </svg>
            </div>
            <!-- Microsoft -->
            <div class="oauth-btn microsoft" @click="oauthLogin('microsoft')" title="Microsoft登录">
              <svg viewBox="0 0 23 23" width="24" height="24">
                <path fill="#f35325" d="M1 1h10v10H1z" />
                <path fill="#81bc06" d="M12 1h10v10H12z" />
                <path fill="#05a6f0" d="M1 12h10v10H1z" />
                <path fill="#ffba08" d="M12 12h10v10H12z" />
              </svg>
            </div>
          </div>
          <!-- QQ (Keep Old) -->
          <div class="oauth-btn qq" @click="qqLogin" title="QQ登录" style="display: none">
            <!-- Hidden old qq login to use new style if needed, or just keep it simple -->
          </div>
        </div>
      </el-form>
    </div>
    <Dialog
      :show="dialogConfig4SendMailCode.show"
      :title="dialogConfig4SendMailCode.title"
      :buttons="dialogConfig4SendMailCode.buttons"
      width="500px"
      :show-cancel="false"
      @close="dialogConfig4SendMailCode.show = false"
    >
      <el-form
        ref="formData4SendMailCodeRef"
        :model="formData4SendMailCode"
        :rules="rules"
        label-width="80px"
        @submit.prevent
      >
        <!-- 邮箱 -->
        <el-form-item label="邮箱">
          {{ formData.email }}
        </el-form-item>
        <!-- 邮箱验证码 -->
        <el-form-item label="验证码" prop="checkCode">
          <div class="check-code-panel">
            <el-input v-model.trim="formData4SendMailCode.checkCode" size="large" placeholder="请输入验证码">
              <template #prefix>
                <span class="iconfont icon-checkcode"></span>
              </template>
            </el-input>
            <img :src="checkCodeUrl4SendMailCode" class="check-code" @click="changeCheckCode(1)" />
          </div>
        </el-form-item>
      </el-form>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, reactive, getCurrentInstance, nextTick, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserInfoStore } from '@/stores/userInfoStore'
import {
  oauthLogin as oauthLoginService,
  sendEmailCode as sendEmailCodeService,
  register as registerService,
  login as loginService,
  resetPwd as resetPwdService,
  qqLogin as qqLoginService,
} from '@/services'
const { proxy } = getCurrentInstance()
const router = useRouter()
const route = useRoute()
const userInfoStore = useUserInfoStore()
const api = {
  checkCode: '/api/checkCode',
}

const oauthLogin = async provider => {
  const callbackUrl = typeof route.query.redirectUrl === 'string' ? route.query.redirectUrl : ''
  const loginUrl = await oauthLoginService(provider, callbackUrl)
  if (!loginUrl) {
    return
  }
  document.location.href = loginUrl
}

// 操作类型 0:注册 1:登录 2:重置密码
const opType = ref(1)
const showPanel = type => {
  opType.value = type
  restForm()
}

onMounted(() => {
  showPanel(1)
})

const checkRePassword = (rule, value, callback) => {
  if (value !== formData.value.registerPassword) {
    callback(new Error(rule.message))
  } else {
    callback()
  }
}
const formData = ref({})
const formDataRef = ref()
const rules = {
  email: [
    { required: true, message: '请输入正确的邮箱' },
    { validator: proxy.Verify.email, message: '请输入正确的邮箱' },
  ],
  password: [{ required: true, message: '请输入密码' }],
  emailCode: [{ required: true, message: '请输入邮箱验证码' }],
  nickName: [{ required: true, message: '请输入昵称' }],
  registerPassword: [
    { required: true, message: '请输入密码' },
    {
      validator: proxy.Verify.password,
    },
  ],
  reRegisterPassword: [
    { required: true, message: '请再次输入密码' },
    {
      validator: checkRePassword,
      message: '两次输入的密码不一致',
    },
  ],
  checkCode: [{ required: true, message: '请输入图片验证码' }],
}

const checkCodeUrl = ref(api.checkCode)
const checkCodeUrl4SendMailCode = ref(api.checkCode)
const changeCheckCode = type => {
  if (type == 0) {
    checkCodeUrl.value = api.checkCode + '?type=' + type + '&time=' + new Date().getTime()
  } else {
    checkCodeUrl4SendMailCode.value = api.checkCode + '?type=' + type + '&time=' + new Date().getTime()
  }
}

// 发送邮箱验证码
const formData4SendMailCode = ref({})
const formData4SendMailCodeRef = ref()

const dialogConfig4SendMailCode = reactive({
  show: false,
  title: '发送邮箱验证码',
  buttons: [
    {
      type: 'primary',
      text: '发送验证码',
      click: () => {
        sendEmailCode()
      },
    },
  ],
})
const getEmailCode = () => {
  formDataRef.value.validateField('email', valid => {
    if (!valid) {
      return
    }
    dialogConfig4SendMailCode.show = true
    nextTick(() => {
      changeCheckCode(1)
      formData4SendMailCodeRef.value.resetFields()
      formData4SendMailCode.value = {
        email: formData.value.email,
      }
    })
  })
}
// 发送邮箱验证码
const sendEmailCode = () => {
  formData4SendMailCodeRef.value.validate(async valid => {
    if (!valid) {
      return
    }
    const params = Object.assign({}, formData4SendMailCode.value)
    params.type = opType.value == 0 ? 0 : 1
    const result = await sendEmailCodeService(params)
    if (!result) {
      changeCheckCode(1)
      return
    }
    proxy.Message.success('验证码发送成功,请登录邮箱查看')
    dialogConfig4SendMailCode.show = false
  })
}

// 重置表单
const restForm = () => {
  changeCheckCode(0)
  formDataRef.value.resetFields()
  formData.value = {}
  // 登录
  if (opType.value == 1) {
    const savedEmail = localStorage.getItem('loginEmail')
    if (savedEmail) {
      formData.value = {
        email: savedEmail,
        rememberMe: true,
      }
    }
  }
}
// 登录、注册、重置密码、提交表单
const isSubmitting = ref(false);
// 密码强度
const passwordStrength = computed(() => {
  const pwd = formData.value.registerPassword || '';
  if (!pwd) return 0;
  let score = 0;
  if (pwd.length > 5) score += 20;
  if (/[a-z]/.test(pwd)) score += 20;
  if (/[A-Z]/.test(pwd)) score += 20;
  if (/\d/.test(pwd)) score += 20;
  if (/[!@#$%^&*]/.test(pwd)) score += 20;
  return score;
});
const strengthColor = computed(() => {
  const score = passwordStrength.value;
  if (score < 40) return '#f56c6c';
  if (score < 80) return '#e6a23c';
  return '#67c23a';
});

const doSubmit = () => {
    if (isSubmitting.value) return;
    formDataRef.value.validate(async valid => {
    if (!valid) {
      return
    }
    isSubmitting.value = true;
    try {
    if (opType.value == 0) {
      const result = await registerService({
        email: formData.value.email,
        nickName: formData.value.nickName,
        password: formData.value.registerPassword,
        checkCode: formData.value.checkCode,
        emailCode: formData.value.emailCode,
      })
      if (!result) {
        changeCheckCode(0)
        return
      }
      proxy.Message.success('注册成功,请登录')
      showPanel(1)
    } else if (opType.value == 1) {
      const loginResult = await loginService({
        email: formData.value.email,
        password: formData.value.password,
        checkCode: formData.value.checkCode,
      })
      if (!loginResult) {
        changeCheckCode(0)
        return
      }
      if (formData.value.rememberMe) {
        if (formData.value.email) {
          localStorage.setItem('loginEmail', formData.value.email)
        }
      } else {
        localStorage.removeItem('loginEmail')
      }
      proxy.Message.success('登录成功')
      userInfoStore.setUserInfo({
        ...loginResult.userInfo,
        token: loginResult.token,
        refreshToken: loginResult.refreshToken,
      })
      // 重定向到原始页面
      const redirectUrl = typeof route.query.redirectUrl === 'string' ? route.query.redirectUrl : '/'
      router.push(redirectUrl)
    } else if (opType.value == 2) {
      const result = await resetPwdService({
        email: formData.value.email,
        password: formData.value.registerPassword,
        checkCode: formData.value.checkCode,
        emailCode: formData.value.emailCode,
      })
      if (!result) {
        changeCheckCode(0)
        return
      }
      // 重置密码
      proxy.Message.success('重置密码成功,请登录')
      showPanel(1)
    }
    } catch (error) {
    // Error handled by Interceptor usually, but just in case
  } finally {
    isSubmitting.value = false;
  }
  })
}

// qq登录
const qqLogin = async () => {
  const callbackUrl = typeof route.query.redirectUrl === 'string' ? route.query.redirectUrl : ''
  const loginUrl = await qqLoginService(callbackUrl)
  if (!loginUrl) {
    return
  }
  userInfoStore.clearUserInfo()
  document.location.href = loginUrl
}
</script>

<style lang="scss" scoped>

.login-body {
  height: 100vh;
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;

  /* New Gradient Background */
  background: 
    radial-gradient(circle at 20% 80%, rgba(37, 99, 235, 0.15) 0%, transparent 50%),
    radial-gradient(circle at 80% 20%, rgba(249, 115, 22, 0.1) 0%, transparent 50%),
    linear-gradient(135deg, #F8FAFC 0%, #E2E8F0 100%);
  
  /* Abstract Shapes for "Geometric" feel */
  &::before, &::after {
    content: '';
    position: absolute;
    border-radius: 50%;
    filter: blur(80px);
    z-index: 0;
  }
  
  &::before {
    top: -10%;
    right: -5%;
    width: 50vw;
    height: 50vw;
    background: radial-gradient(circle, rgba(37, 99, 235, 0.1) 0%, transparent 70%);
  }
  
  &::after {
    bottom: -10%;
    left: -5%;
    width: 40vw;
    height: 40vw;
    background: radial-gradient(circle, rgba(249, 115, 22, 0.08) 0%, transparent 70%);
  }

  .bg {
    display: none; 
  }

  .login-panel {
    width: min(480px, calc(100vw - 24px));
    position: relative;
    z-index: 1;

    .login-register {
      padding: 40px;
      /* Glassmorphism */
      background: rgba(255, 255, 255, 0.85);
      backdrop-filter: blur(20px);
      -webkit-backdrop-filter: blur(20px);
      border-radius: var(--border-radius-xl);
      box-shadow: var(--shadow-xl), var(--shadow-glow); /* Enhanced shadow */
      border: 1px solid rgba(255, 255, 255, 0.6);

      .login-title {
        text-align: center;
        font-size: 28px;
        font-weight: 700;
        color: var(--primary);
        letter-spacing: 0.5px;
        margin-bottom: 30px;
        font-family: var(--font-heading);
        text-shadow: 0 2px 4px rgba(37, 99, 235, 0.1);
      }

      .send-email-panel {
        display: flex;
        width: 100%;
        gap: 10px;

        .send-mail-btn {
          /* Using global button styles, just ensuring layout */
          white-space: nowrap;
        }
      }

      .rememberme-panel {
        width: 100%;
        margin-bottom: 5px;
      }

      .no-account {
        width: 100%;
        display: flex;
        justify-content: space-between;
        margin-top: 10px;
        font-size: 14px;
      }

      .op-btn {
        width: 100%;
        background: var(--primary-gradient); /* Use gradient */
        border: 0;
        color: #fff;
        font-size: 16px;
        font-weight: 600;
        letter-spacing: 1px;
        margin-top: 10px;
        height: 44px;
        border-radius: var(--border-radius-md);
        box-shadow: 0 4px 12px rgba(37, 99, 235, 0.3);
        transition: var(--transition-fast);

        &:hover {
          opacity: 0.95;
          transform: translateY(-2px);
          box-shadow: 0 6px 20px rgba(37, 99, 235, 0.4);
        }
        
        &:active {
            transform: translateY(0);
        }
      }
    }
  }

  .check-code-panel {
    width: 100%;
    display: flex;
    gap: 10px;

    .check-code {
      height: 40px;
      cursor: pointer;
      border-radius: var(--border-radius-sm);
      border: 1px solid var(--border-color);
      transition: transform 0.3s ease;
      
      &:hover {
        transform: scale(1.05);
        box-shadow: var(--shadow-sm);
      }
    }
  }

  .quick-login-panel {
    margin-top: 24px;
    text-align: center;

    .quick-login-title {
      font-size: 13px;
      color: var(--text-light);
      margin-bottom: 12px;
      display: block;
      position: relative;
      
      &::before, &::after {
          content: '';
          position: absolute;
          top: 50%;
          width: 30px;
          height: 1px;
          background: var(--border-color);
      }
      &::before { right: 55%; }
      &::after { left: 55%; }
    }

    .oauth-buttons {
      display: flex;
      justify-content: center;
      gap: 16px;
      flex-wrap: wrap;
    }

    .oauth-btn {
      width: 44px;
      height: 44px;
      border-radius: 50%;
      border: 1px solid var(--border-color);
      background: rgba(255, 255, 255, 0.8);
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      box-shadow: var(--shadow-sm);
      transition: all 0.2s ease;

      &:hover {
        transform: translateY(-3px);
        box-shadow: var(--shadow-md);
        background: #fff;
        border-color: var(--primary-light);
      }

      svg {
        width: 22px;
        height: 22px;
        display: block;
        transition: transform 0.2s;
      }
      
      &:hover svg {
          transform: scale(1.1);
      }
    }
  }

  @media screen and (max-width: 768px) {
    .login-panel {
      .login-register {
        padding: 28px 20px;
      }
    }
  }
}
</style>
