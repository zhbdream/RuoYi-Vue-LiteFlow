import { getLiteFlowConfig } from '@/api/liteflow/platform'

const state = {
  readonly: false,
  readonlyMessage: '',
  configLoaded: false
}

const mutations = {
  SET_CONFIG(state, config) {
    state.readonly = !!(config && config.readonly)
    state.readonlyMessage = (config && config.readonlyMessage) || ''
    state.configLoaded = true
  }
}

const actions = {
  LoadConfig({ commit, state }, force) {
    if (state.configLoaded && !force) {
      return Promise.resolve({ readonly: state.readonly, readonlyMessage: state.readonlyMessage })
    }
    return getLiteFlowConfig().then(res => {
      const data = (res && res.data) || {}
      commit('SET_CONFIG', data)
      return data
    }).catch(() => {
      commit('SET_CONFIG', { readonly: false, readonlyMessage: '' })
      return { readonly: false, readonlyMessage: '' }
    })
  }
}

export default {
  namespaced: true,
  state,
  mutations,
  actions
}
