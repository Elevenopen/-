import { describe, it, expect } from 'vitest'
import { formatBandwidth, formatBytes, formatPercentage } from '@/utils/format'

describe('格式化工具函数测试', () => {
  describe('formatBandwidth', () => {
    it('应正确格式化带宽值', () => {
      expect(formatBandwidth(100)).toBe('100.00 Mbps')
      expect(formatBandwidth(1000)).toBe('1.00 Gbps')
      expect(formatBandwidth(0)).toBe('0 Mbps')
    })

    it('应处理null和undefined', () => {
      expect(formatBandwidth(null)).toBe('0.00 Mbps')
      expect(formatBandwidth(undefined)).toBe('0.00 Mbps')
    })
  })

  describe('formatBytes', () => {
    it('应正确格式化字节大小', () => {
      expect(formatBytes(1024)).toContain('KB')
      expect(formatBytes(1048576)).toContain('MB')
      expect(formatBytes(1073741824)).toContain('GB')
    })

    it('应处理零值', () => {
      expect(formatBytes(0)).toBe('0 B')
    })

    it('应处理null和undefined', () => {
      expect(formatBytes(null)).toBe('0 B')
      expect(formatBytes(undefined)).toBe('0 B')
    })
  })

  describe('formatPercentage', () => {
    it('应正确格式化百分比', () => {
      expect(formatPercentage(50)).toBe('50.00%')
      expect(formatPercentage(100)).toBe('100.00%')
      expect(formatPercentage(0)).toBe('0.00%')
    })

    it('应处理小数', () => {
      expect(formatPercentage(33.33)).toBe('33.33%')
    })

    it('应处理null和undefined', () => {
      expect(formatPercentage(null)).toBe('0%')
      expect(formatPercentage(undefined)).toBe('0%')
    })
  })
})
