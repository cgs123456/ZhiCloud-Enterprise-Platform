import { JSEncrypt } from 'jsencrypt'

// 密钥对生成 http://web.chacuo.net/netrsakeypair

// 公钥：仅用于前端加密，私钥保存在服务端，严禁在前端硬编码
const publicKey =
  'MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAKoR8mX0rGKLqzcWmOzbfj64K8ZIgOdH\n' +
  'nzkXSOVOZbFu/TJhZ7rFAN+eaGkl3C4buccQd/EjEsj9ir7ijT7h96MCAwEAAQ=='

// 加密
export const encrypt = (txt: string) => {
  const encryptor = new JSEncrypt()
  encryptor.setPublicKey(publicKey) // 设置公钥
  return encryptor.encrypt(txt) // 对数据进行加密
}
