import request from '@/utils/request'

export function sendNLIRequest(input, confirmed = false) {
  return request({
    url: '/ai/parse',
    method: 'post',
    data: { input, confirmed },
    timeout: 30000 // 增加到30秒，适应AI分析时间
  })
}

// 添加带重试机制的AI洞察请求
export function sendNLIRequestWithRetry(input, confirmed = false, maxRetries = 2) {
  const attempt = async (retryCount = 0) => {
    try {
      return await sendNLIRequest(input, confirmed)
    } catch (error) {
      if (error.code === 'ECONNABORTED' && retryCount < maxRetries) {
        console.log(`第${retryCount + 1}次重试AI请求...`)
        return attempt(retryCount + 1)
      }
      throw error
    }
  }
  return attempt()
}
