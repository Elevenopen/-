import { describe, it, expect } from 'vitest'
import { formatDate, formatDateTime, getRelativeTime } from '@/utils/date'

describe('日期工具函数测试', () => {
  describe('formatDate', () => {
    it('应正确格式化日期', () => {
      const date = new Date('2024-03-15T10:30:00')
      const result = formatDate(date, 'YYYY-MM-DD')
      expect(result).toBe('2024-03-15')
    })

    it('应处理空值', () => {
      expect(formatDate(null)).toBe('-')
      expect(formatDate(undefined)).toBe('-')
    })
  })

  describe('formatDateTime', () => {
    it('应正确格式化日期时间', () => {
      const date = new Date('2024-03-15T10:30:00')
      const result = formatDateTime(date)
      expect(result).toMatch(/2024-03-15/)
    })

    it('应处理空值', () => {
      expect(formatDateTime(null)).toBe('-')
    })
  })

  describe('getRelativeTime', () => {
    it('应返回相对时间描述', () => {
      const now = new Date()
      const oneHourAgo = new Date(now.getTime() - 60 * 60 * 1000)
      const result = getRelativeTime(oneHourAgo)
      expect(result).toContain('小时前')
    })
  })
})
