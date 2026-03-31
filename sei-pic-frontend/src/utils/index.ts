import { saveAs } from 'file-saver'

export const formatSize = (size?: number) => {
  if (!size) return '未知'
  else if (size < 1024) return size + 'B'
  else if (size < 1024 * 1024) return (size / 1024).toFixed(2) + 'KB'
  else return (size / (1024 * 1024)).toFixed(2) + 'MB'
}

export function downloadImage(url?: string, fileName?: string) {
  if (!url) {
    return
  }

  saveAs(url, fileName)
}

export function toHexColor(input: string) {
  // 去掉 0x 前缀
  const colorValue = input.startsWith('0x') ? input.slice(2) : input

  // 将剩余部分解析为十六进制数，再转成 6 位十六进制字符串
  const hexColor = parseInt(colorValue, 16).toString(16).padStart(6, '0')

  // 返回标准 #RRGGBB 格式
  return `#${hexColor}`
}
