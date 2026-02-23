export default {
  mounted(el, binding) {
    const { value } = binding
    let startX = 0
    let startY = 0
    let startTime = 0
    let longPressTimer = null
    const longPressDuration = 500 // ms
    const swipeThreshold = 50 // px
    const scrollThreshold = 30 // px - allow some vertical movement before cancelling swipe

    const onTouchStart = e => {
      const touch = e.touches[0]
      startX = touch.clientX
      startY = touch.clientY
      startTime = Date.now()

      // 长按检测
      longPressTimer = setTimeout(() => {
        if (value && value.onLongPress) {
          value.onLongPress(e)
        }
      }, longPressDuration)
    }

    const onTouchMove = e => {
      const touch = e.touches[0]
      const diffY = touch.clientY - startY

      // 若用户纵向滑动，则取消长按（避免误触发）
      if (Math.abs(diffY) > scrollThreshold) {
        clearTimeout(longPressTimer)
      }
    }

    const onTouchEnd = e => {
      clearTimeout(longPressTimer)

      const touch = e.changedTouches[0]
      const diffX = touch.clientX - startX
      const diffY = touch.clientY - startY
      const duration = Date.now() - startTime

      // 滑动检测（非长按且横向位移主导时触发）
      if (duration < longPressDuration && Math.abs(diffX) > swipeThreshold && Math.abs(diffX) > Math.abs(diffY)) {
        if (diffX < 0) {
          if (value && value.onSwipeLeft) value.onSwipeLeft(e)
        } else {
          if (value && value.onSwipeRight) value.onSwipeRight(e)
        }
      }
    }

    el.addEventListener('touchstart', onTouchStart, { passive: true })
    el.addEventListener('touchmove', onTouchMove, { passive: true })
    el.addEventListener('touchend', onTouchEnd, { passive: true })
  },
  unmounted(_el) {
    // 兜底清理监听器（通常元素销毁时已自动释放）
  },
}
