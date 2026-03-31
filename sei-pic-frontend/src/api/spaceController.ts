// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** createSpace POST /api/space/create */
export async function createSpaceUsingPost(
  body: API.SpaceAddRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseLong_>('/api/space/create', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** deleteSpace POST /api/space/delete */
export async function deleteSpaceUsingPost(body: API.IdRequest, options?: { [key: string]: any }) {
  return request<API.BaseResponseBoolean_>('/api/space/delete', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** editSpace POST /api/space/edit */
export async function editSpaceUsingPost(
  body: API.SpaceEditRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/space/edit', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** listSpaceLevel POST /api/space/list/level */
export async function listSpaceLevelUsingPost(options?: { [key: string]: any }) {
  return request<API.BaseResponseListSpaceLevel_>('/api/space/list/level', {
    method: 'POST',
    ...(options || {}),
  })
}

/** getSpacePage POST /api/space/page */
export async function getSpacePageUsingPost(
  body: API.SpaceQueryRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponsePageSpaceVO_>('/api/space/page', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** updateSpace POST /api/space/update */
export async function updateSpaceUsingPost(
  body: API.SpaceUpdateRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseBoolean_>('/api/space/update', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** getSpaceVo POST /api/space/vo */
export async function getSpaceVoUsingPost(options?: { [key: string]: any }) {
  return request<API.BaseResponseSpaceVO_>('/api/space/vo', {
    method: 'POST',
    ...(options || {}),
  })
}
