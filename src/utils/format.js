/**
 * 格式化工具
 */

/**
 * 格式化带宽值
 * @param {number} mbps - 带宽，单位 Mbps
 * @returns {string}
 */
export function formatBandwidth(mbps) {
  if (mbps === null || mbps === undefined || isNaN(mbps)) return '0.00 Mbps'
  if (mbps === 0) return '0 Mbps'
  if (mbps >= 1000) {
    return `${(mbps / 1000).toFixed(2)} Gbps`
  }
  return `${Number(mbps).toFixed(2)} Mbps`
}

/**
 * 格式化字节大小
 * @param {number} bytes
 * @returns {string}
 */
export function formatBytes(bytes) {
  if (bytes === null || bytes === undefined || bytes === 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(1024))
  return `${(bytes / Math.pow(1024, i)).toFixed(2)} ${units[i]}`
}

/**
 * 格式化百分比
 * @param {number} value - 0-100 的数值
 * @returns {string}
 */
export function formatPercentage(value) {
  if (value === null || value === undefined || isNaN(value)) return '0%'
  return `${Number(value).toFixed(2)}%`
}
