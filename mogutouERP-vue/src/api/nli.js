import request from '@/utils/request'

export function sendNLIRequest(input, confirmed = false) {
  return request({
    url: '/nli/parse',
    method: 'post',
    data: { input, confirmed },
    timeout: 15000 // ✅ 增加超时时间
  })
}
