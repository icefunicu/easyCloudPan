import { createApp, defineAsyncComponent } from 'vue'
import App from './App.vue'
import router from './router'
import { createPinia } from 'pinia'
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'
// 引入element plus
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
// 图标 图标在附件中
import '@/assets/icon/iconfont.css'
import '@/assets/base.scss'
// 引入cookie
import VueCookies from 'vue-cookies'

// 引入国际化
import i18n from '@/locales'

// 引入图片懒加载
import VueLazyLoad from 'vue3-lazy'
import loadingImage from '@/assets/loading_image.png'

// 引入代码高亮
import HljsVuePlugin from '@highlightjs/vue-plugin'
import "highlight.js/styles/atom-one-light.css";
import 'highlight.js/lib/common'

import Verify from '@/utils/Verify'
import Message from '@/utils/Message'
import Request from '@/utils/Request'
import Confirm from '@/utils/Confirm'
import Utils from '@/utils/Utils'

// 自定义组件
import Dialog from '@/components/Dialog.vue'
import Avatar from '@/components/Avatar.vue'
import Table from '@/components/Table.vue'
import Icon from '@/components/Icon.vue'
import NoData from '@/components/NoData.vue'
import FolderSelect from '@/components/FolderSelect.vue'
import Navigation from '@/components/Navigation.vue'
const Preview = defineAsyncComponent(() => import('@/components/preview/Preview.vue'))
import Window from '@/components/Window.vue'


const app = createApp(App)
app.use(ElementPlus)
app.use(i18n)
app.use(HljsVuePlugin)

const pinia = createPinia()
pinia.use(piniaPluginPersistedstate)
app.use(pinia)

app.use(VueLazyLoad, {
    loading: loadingImage,
    error: loadingImage
})

app.use(router)

app.component("Dialog", Dialog);
app.component("Avatar", Avatar);
app.component("Table", Table);
app.component("Icon", Icon);
app.component("NoData", NoData);
app.component("FolderSelect", FolderSelect);
app.component("Navigation", Navigation);
app.component("Preview", Preview);
app.component("Window", Window);

// 配置全局组件
app.config.globalProperties.Verify = Verify;
app.config.globalProperties.Message = Message;
app.config.globalProperties.Request = Request;
app.config.globalProperties.Confirm = Confirm;
app.config.globalProperties.Utils = Utils;

app.config.globalProperties.VueCookies = VueCookies;
app.config.globalProperties.globalInfo = {
    avatarUrl: "/api/getAvatar/",
    imageUrl: "/api/file/getImage/"
}
// Custom Directives
// @ts-ignore
import TouchDirective from '@/utils/TouchDirective'
app.directive('touch', TouchDirective)

import { initWebVitals } from '@/utils/webVitals'
initWebVitals()

app.mount('#app')

// @ts-ignore
import { registerSW } from 'virtual:pwa-register'

const updateSW = registerSW({
    onNeedRefresh() {
        // Optionally show a notification to the user
        console.log('New content available, click on reload button to update.')
    },
    onOfflineReady() {
        console.log('App ready to work offline')
    },
})
