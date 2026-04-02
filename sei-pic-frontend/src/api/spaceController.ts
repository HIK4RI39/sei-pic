// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** getSpaceCategoryAnalyze POST /api/space/analyze/category */
export async function getSpaceCategoryAnalyzeUsingPost(
  body: API.SpaceCategoryAnalyzeRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseListSpaceCategoryAnalyzeResponse_>('/api/space/analyze/category', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** getSpaceRankAnalyze POST /api/space/analyze/rank */
export async function getSpaceRankAnalyzeUsingPost(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getSpaceRankAnalyzeUsingPOSTParams,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseListSpace_>('/api/space/analyze/rank', {
    method: 'POST',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** getSpaceSizeAnalyze POST /api/space/analyze/size */
export async function getSpaceSizeAnalyzeUsingPost(
  body: API.SpaceSizeAnalyzeRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseListSpaceSizeAnalyzeResponse_>('/api/space/analyze/size', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** getSpaceTagAnalyze POST /api/space/analyze/tag */
export async function getSpaceTagAnalyzeUsingPost(
  body: API.SpaceTagAnalyzeRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseListSpaceTagAnalyzeResponse_>('/api/space/analyze/tag', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** getSpaceUsageAnalyze POST /api/space/analyze/usage */
export async function getSpaceUsageAnalyzeUsingPost(
  body: API.SpaceUsageAnalyzeRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseSpaceUsageAnalyzeResponse_>('/api/space/analyze/usage', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** getSpaceUserAnalyze POST /api/space/analyze/user */
export async function getSpaceUserAnalyzeUsingPost(
  body: API.SpaceUserAnalyzeRequest,
  options?: { [key: string]: any }
) {
  return request<API.BaseResponseListSpaceUserAnalyzeResponse_>('/api/space/analyze/user', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

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
