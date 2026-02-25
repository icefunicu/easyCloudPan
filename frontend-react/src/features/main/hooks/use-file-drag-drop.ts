import { type DragEvent, useCallback, useEffect, useRef, useState } from 'react'
import { eventBus } from '@/lib/event-bus'
import { App } from 'antd'

interface UseFileDragDropProps {
    currentFolderId: string
}

export const useFileDragDrop = ({ currentFolderId }: UseFileDragDropProps) => {
    const { message } = App.useApp()
    const [draggingUpload, setDraggingUpload] = useState(false)
    const dragCounterRef = useRef(0)

    const addFiles = useCallback(
        (files: File[]) => {
            files.forEach((file) => eventBus.emit('upload:add', { file, filePid: currentFolderId }))
            message.success(`已添加 ${files.length} 个上传任务`)
        },
        [currentFolderId, message]
    )

    const resetDragState = useCallback(() => {
        dragCounterRef.current = 0
        setDraggingUpload(false)
    }, [])

    const handleDragEnter = useCallback((event: DragEvent<HTMLElement>) => {
        event.preventDefault()
        event.stopPropagation()
        if (!Array.from(event.dataTransfer.types).includes('Files')) {
            return
        }
        dragCounterRef.current += 1
        setDraggingUpload(true)
    }, [])

    const handleDragOver = useCallback((event: DragEvent<HTMLElement>) => {
        event.preventDefault()
        event.stopPropagation()
        if (Array.from(event.dataTransfer.types).includes('Files')) {
            event.dataTransfer.dropEffect = 'copy'
        }
    }, [])

    const handleDragLeave = useCallback((event: DragEvent<HTMLElement>) => {
        event.preventDefault()
        event.stopPropagation()
        if (dragCounterRef.current === 0) {
            return
        }
        dragCounterRef.current = Math.max(0, dragCounterRef.current - 1)
        if (dragCounterRef.current === 0) {
            setDraggingUpload(false)
        }
    }, [])

    const handleDrop = useCallback(
        (event: DragEvent<HTMLElement>) => {
            event.preventDefault()
            event.stopPropagation()
            const files = Array.from(event.dataTransfer.files || [])
            resetDragState()
            if (files.length) {
                addFiles(files)
            }
        },
        [addFiles, resetDragState]
    )

    useEffect(() => {
        const reset = () => {
            dragCounterRef.current = 0
            setDraggingUpload(false)
        }
        window.addEventListener('drop', reset)
        window.addEventListener('dragend', reset)
        return () => {
            window.removeEventListener('drop', reset)
            window.removeEventListener('dragend', reset)
        }
    }, [])

    return {
        draggingUpload,
        dragProps: {
            onDragEnter: handleDragEnter,
            onDragOver: handleDragOver,
            onDragLeave: handleDragLeave,
            onDrop: handleDrop,
        },
        addFiles,
    }
}
