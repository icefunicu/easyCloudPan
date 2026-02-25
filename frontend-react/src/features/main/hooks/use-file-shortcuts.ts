import { useEffect, type RefObject } from 'react'
import type { InputRef } from 'antd/es/input'

interface UseFileShortcutsProps {
    searchInputRef: RefObject<InputRef | null>
    selectedCount: number
    onClearSelection: () => void
    onConfirmBatchDelete: () => void
}

export const useFileShortcuts = ({
    searchInputRef,
    selectedCount,
    onClearSelection,
    onConfirmBatchDelete,
}: UseFileShortcutsProps) => {
    useEffect(() => {
        const handleShortcuts = (event: KeyboardEvent) => {
            const target = event.target as HTMLElement | null
            const editableTag = target?.tagName === 'INPUT' || target?.tagName === 'TEXTAREA'
            const contentEditable = Boolean(target?.isContentEditable)
            const inEditable = editableTag || contentEditable

            // Search shortcut: / or Cmd/Ctrl + K
            if ((event.key === '/' || (event.key.toLowerCase() === 'k' && (event.metaKey || event.ctrlKey))) && !inEditable) {
                event.preventDefault()
                searchInputRef.current?.focus()
                return
            }

            // Clear selection shortcut: Escape
            if (event.key === 'Escape' && selectedCount > 0) {
                onClearSelection()
                return
            }

            // Batch delete shortcut: Delete or Backspace
            if ((event.key === 'Delete' || event.key === 'Backspace') && selectedCount > 0 && !inEditable) {
                event.preventDefault()
                onConfirmBatchDelete()
            }
        }

        window.addEventListener('keydown', handleShortcuts)
        return () => {
            window.removeEventListener('keydown', handleShortcuts)
        }
    }, [onConfirmBatchDelete, selectedCount, onClearSelection, searchInputRef])
}
