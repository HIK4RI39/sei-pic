// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** health GET /api/user/health */
export async function healthUsingGet(options?: { [key: string]: any }) {
  return request<API.BaseResponseObject_>('/api/user/health', {
    method: 'GET',
    ...(options || {}),
  })
}
