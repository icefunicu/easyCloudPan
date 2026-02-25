const prefetchLoaders = [
  () => import('@/features/main/main-page'),
  () => import('@/features/share/my-share-page'),
  () => import('@/features/recycle/recycle-page'),
  () => import('@/features/admin/admin-user-list-page'),
  () => import('@/features/admin/admin-file-list-page'),
  () => import('@/features/admin/admin-sys-settings-page'),
]

let prefetched = false

export const prefetchPrivateRoutes = async (): Promise<void> => {
  if (prefetched) {
    return
  }
  prefetched = true
  await Promise.allSettled(prefetchLoaders.map((loader) => loader()))
}
