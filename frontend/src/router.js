import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', redirect: '/concerts' },
  { path: '/login', component: () => import('./views/LoginView.vue') },
  { path: '/concerts', component: () => import('./views/ConcertListView.vue') },
  { path: '/concerts/:id', component: () => import('./views/ConcertDetailView.vue') },
  { path: '/orders', component: () => import('./views/OrdersView.vue') }
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach(to => {
  const isPublic = to.path === '/login' || to.path === '/concerts' || /^\/concerts\/\d+$/.test(to.path)
  if (!isPublic && !localStorage.getItem('token')) return '/login'
})

export default router
