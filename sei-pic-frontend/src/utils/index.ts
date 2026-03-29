export const formatSize = (size?: number) => {
  if (!size) return '未知'
  else if (size < 1024) return size + 'B'
  else if (size < 1024 * 1024) return (size / 1024).toFixed(2) + 'KB'
  else return (size / (1024 * 1024)).toFixed(2) + 'MB'
}
