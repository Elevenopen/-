import { describe, it, expect } from 'vitest'
import { validatePhone, validateEmail, validateIP, isPositiveNumber } from '@/utils/validate'

describe('表单验证工具测试', () => {
  describe('validatePhone', () => {
    it('应正确验证有效手机号', () => {
      expect(validatePhone('13800138000')).toBe(true)
      expect(validatePhone('15912345678')).toBe(true)
      expect(validatePhone('18612345678')).toBe(true)
    })

    it('应拒绝无效手机号', () => {
      expect(validatePhone('12345678901')).toBe(false)
      expect(validatePhone('1380013800')).toBe(false)
      expect(validatePhone('abcdefghijk')).toBe(false)
    })
  })

  describe('validateEmail', () => {
    it('应正确验证有效邮箱', () => {
      expect(validateEmail('test@example.com')).toBe(true)
      expect(validateEmail('admin@company.cn')).toBe(true)
      expect(validateEmail('user.name@domain.co.uk')).toBe(true)
    })

    it('应拒绝无效邮箱', () => {
      expect(validateEmail('invalid-email')).toBe(false)
      expect(validateEmail('@example.com')).toBe(false)
      expect(validateEmail('test@')).toBe(false)
    })
  })

  describe('validateIP', () => {
    it('应正确验证有效IP地址', () => {
      expect(validateIP('192.168.1.1')).toBe(true)
      expect(validateIP('10.0.0.1')).toBe(true)
      expect(validateIP('255.255.255.255')).toBe(true)
    })

    it('应拒绝无效IP地址', () => {
      expect(validateIP('256.1.1.1')).toBe(false)
      expect(validateIP('192.168.1')).toBe(false)
      expect(validateIP('abc.def.ghi.jkl')).toBe(false)
    })
  })

  describe('isPositiveNumber', () => {
    it('应正确验证正数', () => {
      expect(isPositiveNumber(100)).toBe(true)
      expect(isPositiveNumber(0.5)).toBe(true)
      expect(isPositiveNumber('100')).toBe(true)
    })

    it('应拒绝非正数', () => {
      expect(isPositiveNumber(0)).toBe(false)
      expect(isPositiveNumber(-1)).toBe(false)
      expect(isPositiveNumber('abc')).toBe(false)
    })
  })
})
