import axios from 'axios';
export async function memberWrite(method, path) {
  const {data: csrf} = await axios.get('/api/auth/csrf');
  return axios.request({method, url:path, headers:{[csrf.headerName]:csrf.token}});
}
export function startNaverLogin() { window.location.assign('/oauth2/authorization/naver'); }
