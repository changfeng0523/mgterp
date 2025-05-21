import { useApi } from './index'
import md5 from 'js-md5'

export const useLoginApi = () => {
  const { get, post } = useApi()

  const login = async (username, password) => {
    return await post('/api/auth/login', {
      username: username,
      password: md5(password)
    })
  }

  const getInfo = async () => {
    return await get('/api/auth/user')
  }

  const logout = async () => {
    return await get('/api/auth/logout')
  }

  return {
    login,
    getInfo,
    logout
  }
}