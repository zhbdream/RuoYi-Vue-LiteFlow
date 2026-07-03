<template>
  <div class="login-page">
    <!-- 左侧品牌区 -->
    <div class="login-brand">
      <div class="brand-inner">
        <div class="brand-logo">
          <i class="el-icon-s-operation"></i>
        </div>
        <h1 class="brand-title">RuoYi-Vue-LiteFlow</h1>
        <p class="brand-slogan">拖拽编排 · EL 双向同步 · 规则热更新</p>
        <ul class="brand-features">
          <li><i class="el-icon-check"></i> AntV X6 可视化编排器</li>
          <li><i class="el-icon-check"></i> LiteFlow 2.16 全算子支持</li>
          <li><i class="el-icon-check"></i> 若依 RBAC + 审计 + 开放 API</li>
          <li><i class="el-icon-check"></i> Demo0~6 开箱即用</li>
        </ul>
        <div class="brand-tags">
          <span>THEN</span><span>IF</span><span>SWITCH</span><span>WHEN</span><span>FOR</span><span>CATCH</span>
        </div>
      </div>
      <div class="brand-decoration">
        <div class="deco-circle c1"></div>
        <div class="deco-circle c2"></div>
        <div class="deco-circle c3"></div>
      </div>
    </div>

    <!-- 右侧登录区 -->
    <div class="login-panel">
      <el-form ref="loginForm" :model="loginForm" :rules="loginRules" class="login-form">
        <div class="form-header">
          <h3 class="form-title">欢迎登录</h3>
          <p class="form-subtitle">业务编排中台管理后台</p>
        </div>

        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            type="text"
            auto-complete="off"
            placeholder="账号"
          >
            <svg-icon slot="prefix" icon-class="user" class="el-input__icon input-icon" />
          </el-input>
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            auto-complete="off"
            placeholder="密码"
            @keyup.enter.native="handleLogin"
          >
            <svg-icon slot="prefix" icon-class="password" class="el-input__icon input-icon" />
          </el-input>
        </el-form-item>
        <el-form-item prop="code" v-if="captchaEnabled">
          <el-input
            v-model="loginForm.code"
            auto-complete="off"
            placeholder="验证码"
            style="width: 63%"
            @keyup.enter.native="handleLogin"
          >
            <svg-icon slot="prefix" icon-class="validCode" class="el-input__icon input-icon" />
          </el-input>
          <div class="login-code">
            <img :src="codeUrl" @click="getCode" class="login-code-img"/>
          </div>
        </el-form-item>

        <div class="login-options">
          <el-checkbox v-model="loginForm.rememberMe">记住密码</el-checkbox>
        </div>

        <el-form-item style="width:100%; margin-bottom: 0;">
          <el-button
            :loading="loading"
            size="medium"
            type="primary"
            class="login-btn"
            @click.native.prevent="handleLogin"
          >
            <span v-if="!loading">登 录</span>
            <span v-else>登 录 中...</span>
          </el-button>
          <div class="register-link" v-if="register">
            <router-link class="link-type" :to="'/register'">立即注册</router-link>
          </div>
        </el-form-item>
      </el-form>

      <div class="login-footer">
        <span>{{ footerContent }}</span>
      </div>
    </div>
  </div>
</template>

<script>
import { getCodeImg } from "@/api/login"
import Cookies from "js-cookie"
import { encrypt, decrypt } from '@/utils/jsencrypt'
import defaultSettings from '@/settings'

export default {
  name: "Login",
  data() {
    return {
      title: process.env.VUE_APP_TITLE,
      footerContent: defaultSettings.footerContent,
      codeUrl: "",
      loginForm: {
        username: "admin",
        password: "admin123",
        rememberMe: false,
        code: "",
        uuid: ""
      },
      loginRules: {
        username: [
          { required: true, trigger: "blur", message: "请输入您的账号" }
        ],
        password: [
          { required: true, trigger: "blur", message: "请输入您的密码" }
        ],
        code: [{ required: true, trigger: "change", message: "请输入验证码" }]
      },
      loading: false,
      captchaEnabled: true,
      register: false,
      redirect: undefined
    }
  },
  watch: {
    $route: {
      handler: function(route) {
        this.redirect = route.query && route.query.redirect
      },
      immediate: true
    }
  },
  created() {
    this.getCode()
    this.getCookie()
  },
  methods: {
    getCode() {
      getCodeImg().then(res => {
        this.captchaEnabled = res.captchaEnabled === undefined ? true : res.captchaEnabled
        if (this.captchaEnabled) {
          this.codeUrl = "data:image/gif;base64," + res.img
          this.loginForm.uuid = res.uuid
        }
      })
    },
    getCookie() {
      const username = Cookies.get("username")
      const password = Cookies.get("password")
      const rememberMe = Cookies.get('rememberMe')
      this.loginForm = {
        username: username === undefined ? this.loginForm.username : username,
        password: password === undefined ? this.loginForm.password : decrypt(password),
        rememberMe: rememberMe === undefined ? false : Boolean(rememberMe)
      }
    },
    handleLogin() {
      this.$refs.loginForm.validate(valid => {
        if (valid) {
          this.loading = true
          if (this.loginForm.rememberMe) {
            Cookies.set("username", this.loginForm.username, { expires: 30 })
            Cookies.set("password", encrypt(this.loginForm.password), { expires: 30 })
            Cookies.set('rememberMe', this.loginForm.rememberMe, { expires: 30 })
          } else {
            Cookies.remove("username")
            Cookies.remove("password")
            Cookies.remove('rememberMe')
          }
          this.$store.dispatch("Login", this.loginForm).then(() => {
            this.$router.push({ path: this.redirect || "/" }).catch(()=>{})
          }).catch(() => {
            this.loading = false
            if (this.captchaEnabled) {
              this.getCode()
            }
          })
        }
      })
    }
  }
}
</script>

<style rel="stylesheet/scss" lang="scss" scoped>
.login-page {
  display: flex;
  min-height: 100vh;
  background: #f0f2f5;
}

/* 左侧品牌 */
.login-brand {
  flex: 1;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(145deg, #0f1c2e 0%, #1a3a5c 40%, #2563a8 100%);
  overflow: hidden;
  padding: 48px 40px;
}

.brand-inner {
  position: relative;
  z-index: 2;
  max-width: 420px;
  color: #fff;
}

.brand-logo {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.12);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 24px;
  border: 1px solid rgba(255, 255, 255, 0.18);

  i {
    font-size: 28px;
    color: #fff;
  }
}

.brand-title {
  margin: 0 0 10px;
  font-size: 30px;
  font-weight: 700;
  letter-spacing: -0.5px;
}

.brand-slogan {
  margin: 0 0 28px;
  font-size: 15px;
  opacity: 0.85;
  letter-spacing: 1px;
}

.brand-features {
  list-style: none;
  padding: 0;
  margin: 0 0 28px;

  li {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 8px 0;
    font-size: 14px;
    opacity: 0.88;

    i {
      color: #67c23a;
      font-size: 16px;
    }
  }
}

.brand-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;

  span {
    padding: 4px 10px;
    border-radius: 4px;
    font-size: 11px;
    font-weight: 600;
    background: rgba(255, 255, 255, 0.1);
    border: 1px solid rgba(255, 255, 255, 0.15);
    letter-spacing: 0.5px;
  }
}

.brand-decoration {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.deco-circle {
  position: absolute;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.08);

  &.c1 {
    width: 400px;
    height: 400px;
    right: -120px;
    top: -80px;
    background: rgba(64, 158, 255, 0.08);
  }
  &.c2 {
    width: 260px;
    height: 260px;
    left: -60px;
    bottom: -40px;
    background: rgba(103, 194, 58, 0.06);
  }
  &.c3 {
    width: 160px;
    height: 160px;
    right: 60px;
    bottom: 80px;
    background: rgba(255, 255, 255, 0.04);
  }
}

/* 右侧登录 */
.login-panel {
  width: 480px;
  flex-shrink: 0;
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px;
  background: #fff;
  box-shadow: -4px 0 24px rgba(0, 0, 0, 0.04);
}

.login-form {
  width: 100%;
  max-width: 360px;
  margin-top: 8vh;

  .el-input {
    height: 42px;
    input {
      height: 42px;
      border-radius: 6px;
    }
  }
  .input-icon {
    height: 42px;
    width: 14px;
    margin-left: 4px;
  }
}

.form-header {
  margin-bottom: 28px;
}

.form-title {
  margin: 0 0 6px;
  font-size: 24px;
  font-weight: 700;
  color: #303133;
}

.form-subtitle {
  margin: 0;
  font-size: 14px;
  color: #909399;
}

.login-options {
  margin-bottom: 20px;
}

.login-btn {
  width: 100%;
  height: 42px;
  font-size: 15px;
  letter-spacing: 4px;
  border-radius: 6px;
}

.login-code {
  width: 33%;
  height: 42px;
  float: right;
  img {
    cursor: pointer;
    vertical-align: middle;
    border-radius: 4px;
  }
}

.login-code-img {
  height: 42px;
}

.register-link {
  text-align: center;
  margin-top: 12px;
}

.login-footer {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 24px;
  text-align: center;
  color: #c0c4cc;
  font-size: 12px;
}

/* 响应式：小屏隐藏左侧 */
@media (max-width: 900px) {
  .login-brand {
    display: none;
  }
  .login-panel {
    width: 100%;
    box-shadow: none;
    margin-top: 0;
  }

  .login-form {
    margin-top: 12vh;
  }
}
</style>
