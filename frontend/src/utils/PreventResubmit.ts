/**
 * 防重复提交指令
 * 使用方式：v-prevent-resubmit
 */

interface ExtendedHTMLElement extends HTMLElement {
  _preventResubmitTimer?: number
  _preventResubmitDisabled?: boolean
}

export const preventResubmit = {
  mounted(el: ExtendedHTMLElement, binding: any) {
    const delay = binding.value || 2000 // 默认 2 秒防抖

    el.addEventListener('click', () => {
      if (el._preventResubmitDisabled) {
        return
      }

      // 禁用按钮
      el._preventResubmitDisabled = true
      el.classList.add('is-disabled')
      el.setAttribute('disabled', 'disabled')

      // 保存原始文本
      const originalText = el.textContent || ''

      // 显示提交中状态
      if (el.tagName === 'BUTTON') {
        el.textContent = '提交中...'
      }

      // 设置定时器恢复
      el._preventResubmitTimer = window.setTimeout(() => {
        el._preventResubmitDisabled = false
        el.classList.remove('is-disabled')
        el.removeAttribute('disabled')

        // 恢复原始文本
        if (el.tagName === 'BUTTON') {
          el.textContent = originalText
        }
      }, delay)
    })
  },

  unmounted(el: ExtendedHTMLElement) {
    if (el._preventResubmitTimer) {
      clearTimeout(el._preventResubmitTimer)
    }
  },
}

/**
 * 防重复点击指令（更轻量级）
 * 使用方式：v-debounce-click
 */
export const debounceClick = {
  mounted(el: ExtendedHTMLElement, binding: any) {
    const delay = binding.value || 500 // 默认 500ms 防抖
    let timer: number | null = null

    el.addEventListener('click', (event: Event) => {
      if (timer) {
        event.stopImmediatePropagation()
        return
      }

      timer = window.setTimeout(() => {
        timer = null
      }, delay)
    })
  },
}
