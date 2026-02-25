import { Button, Flex, Typography, Tooltip, Grid } from 'antd'
import { motion, AnimatePresence } from 'framer-motion'
import {
    CloseOutlined,
    DeleteOutlined,
    DownloadOutlined,
    ShareAltOutlined,
    SwapOutlined,
} from '@ant-design/icons'

const { Text } = Typography
const { useBreakpoint } = Grid

export interface FloatingActionDockProps {
    selectedCount: number
    onClearSelection: () => void
    onBatchMove: () => void
    onBatchShare: () => void
    onBatchDownload: () => void
    onBatchDelete: () => void
}

export const FloatingActionDock = ({
    selectedCount,
    onClearSelection,
    onBatchMove,
    onBatchShare,
    onBatchDownload,
    onBatchDelete,
}: FloatingActionDockProps) => {
    const screens = useBreakpoint()
    const isMobile = !screens.md

    return (
        <AnimatePresence>
            {selectedCount > 0 && (
                <motion.div
                    initial={{ y: 100, opacity: 0, scale: 0.95, x: '-50%' }}
                    animate={{ y: 0, opacity: 1, scale: 1, x: '-50%' }}
                    exit={{ y: 100, opacity: 0, scale: 0.95, x: '-50%' }}
                    transition={{ type: 'spring', damping: 25, stiffness: 300 }}
                    style={{
                        position: 'fixed',
                        bottom: isMobile ? 20 : 40,
                        left: '50%',
                        zIndex: 1000,
                    }}
                >
                    <Flex
                        align="center"
                        gap={isMobile ? 12 : 24}
                        style={{
                            background: 'rgba(255, 255, 255, 0.85)',
                            backdropFilter: 'blur(20px) saturate(180%)',
                            border: '1px solid rgba(255, 255, 255, 0.6)',
                            boxShadow: '0 12px 32px rgba(31, 111, 139, 0.15), 0 2px 8px rgba(31, 111, 139, 0.08)',
                            borderRadius: 24,
                            padding: isMobile ? '8px 16px' : '12px 24px',
                            minWidth: isMobile ? 'auto' : 320,
                        }}
                    >
                        <Flex align="center" gap={8} style={{ paddingRight: 8 }}>
                            <Button
                                type="text"
                                icon={<CloseOutlined />}
                                onClick={onClearSelection}
                                style={{ color: '#8c8c8c' }}
                                className="btn-icon-round"
                                size={isMobile ? 'small' : 'middle'}
                            />
                            <Text strong style={{ color: '#164f68', whiteSpace: 'nowrap', fontSize: isMobile ? 14 : 15 }}>
                                已选 {selectedCount} 项
                            </Text>
                        </Flex>

                        <Flex align="center" gap={isMobile ? 2 : 8}>
                            <Tooltip title="移动">
                                <Button
                                    type="text"
                                    icon={<SwapOutlined />}
                                    onClick={onBatchMove}
                                    style={{ color: '#164f68' }}
                                    className="btn-icon-round"
                                    size={isMobile ? 'small' : 'middle'}
                                />
                            </Tooltip>
                            <Tooltip title="分享">
                                <Button
                                    type="text"
                                    icon={<ShareAltOutlined />}
                                    onClick={onBatchShare}
                                    style={{ color: '#164f68' }}
                                    className="btn-icon-round"
                                    size={isMobile ? 'small' : 'middle'}
                                />
                            </Tooltip>
                            <Tooltip title="下载">
                                <Button
                                    type="text"
                                    icon={<DownloadOutlined />}
                                    onClick={onBatchDownload}
                                    style={{ color: '#164f68' }}
                                    className="btn-icon-round"
                                    size={isMobile ? 'small' : 'middle'}
                                />
                            </Tooltip>
                            <div style={{ width: 1, height: isMobile ? 16 : 24, background: 'rgba(31, 111, 139, 0.15)', margin: '0 4px' }} />
                            <Tooltip title="删除">
                                <Button
                                    type="text"
                                    danger
                                    icon={<DeleteOutlined />}
                                    onClick={onBatchDelete}
                                    className="btn-icon-round"
                                    size={isMobile ? 'small' : 'middle'}
                                />
                            </Tooltip>
                        </Flex>
                    </Flex>
                </motion.div>
            )}
        </AnimatePresence>
    )
}
