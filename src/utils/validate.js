/**
 * 表单验证工具
 */

/**
 * 验证手机号（中国大陆）
 * @param {string} phone
 * @returns {boolean}
 */
export function validatePhone(phone) {
  if (!phone) return false
  return /^1[3-9]\d{9}$/.test(phone)
}

/**
 * 验证邮箱
 * @param {string} email
 * @returns {boolean}
 */
export function validateEmail(email) {
  if (!email) return false
  return /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/.test(email)
}

/**
 * 验证IPv4地址
 * @param {string} ip
 * @returns {boolean}
 */
export function validateIP(ip) {
  if (!ip) return false
  const pattern = /^(25[0-5]|2[0-4]\d|1\d{2}|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d{2}|[1-9]?\d)){3}$/
  return pattern.test(ip)
}

/**
 * 验证是否为正数
 * @param {number|string} value
 * @returns {boolean}
 */
export function isPositiveNumber(value) {
  const num = Number(value)
  return !isNaN(num) && isFinite(num) && num > 0
}
