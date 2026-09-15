import { createApp } from 'vue'
import { createRouter, createWebHistory } from 'vue-router'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
import App from './App.vue'
import Library from './views/Library.vue'
import Artists from './views/Artists.vue'
import Albums from './views/Albums.vue'
import ArtistDetail from './views/ArtistDetail.vue'
import AlbumDetail from './views/AlbumDetail.vue'
import Search from './views/Search.vue'
import Community from './views/Community.vue'
import Manage from './views/Manage.vue'
import 'element-plus/theme-chalk/dark/css-vars.css'
import './store/theme'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: Library },
    { path: '/artists', component: Artists },
    { path: '/albums', component: Albums },
    { path: '/artist/:name', component: ArtistDetail },
    { path: '/album/:name', component: AlbumDetail },
    { path: '/search', component: Search },
    { path: '/community', component: Community },
    { path: '/admin', component: Manage }
  ],
  scrollBehavior() {
    return { top: 0 }
  }
})

const app = createApp(App)
app.use(router)
app.use(ElementPlus, { locale: zhCn })
app.mount('#app')
