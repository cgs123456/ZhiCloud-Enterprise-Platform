package cn.zhicloud.framework.encrypt.core.filter;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.CryptoException;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.asymmetric.AsymmetricDecryptor;
import cn.hutool.crypto.asymmetric.AsymmetricEncryptor;
import cn.hutool.crypto.symmetric.SymmetricDecryptor;
import cn.hutool.crypto.symmetric.SymmetricEncryptor;
import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.util.object.ObjectUtils;
import cn.zhicloud.framework.common.util.servlet.ServletUtils;
import cn.zhicloud.framework.encrypt.config.ApiEncryptProperties;
import cn.zhicloud.framework.encrypt.core.annotation.ApiEncrypt;
import cn.zhicloud.framework.web.config.WebProperties;
import cn.zhicloud.framework.web.core.filter.ApiRequestFilter;
import cn.zhicloud.framework.web.core.handler.GlobalExceptionHandler;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.ServletRequestPathUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;

/**
 * API 加密过滤器，处理 {@link ApiEncrypt} 注解。
 *
 * 1. 解密请求参数
 * 2. 加密响应结果
 *
 * 疑问：为什么不使用 SpringMVC 的 RequestBodyAdvice 或 ResponseBodyAdvice 机制呢？
 * 回答：考虑到项目中会记录访问日志、异常日志，以及 HTTP API 签名等场景，最好是全局级、且提前做解析！！！
 *
 * @author 智云
 */
@Slf4j
public class ApiEncryptFilter extends ApiRequestFilter {

    /**
     * AES/CBC/PKCS5Padding 变换名称（替代不安全的 ECB 模式）
     */
    private static final String AES_CBC_PADDING = "AES/CBC/PKCS5Padding";

    /**
     * AES 分组长度固定为 16 字节，IV 长度与之相同
     */
    private static final int IV_LENGTH = 16;

    /**
     * IV 随机数生成器
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final ApiEncryptProperties apiEncryptProperties;

    private final RequestMappingHandlerMapping requestMappingHandlerMapping;

    private final GlobalExceptionHandler globalExceptionHandler;

    private final SymmetricDecryptor requestSymmetricDecryptor;
    private final AsymmetricDecryptor requestAsymmetricDecryptor;

    private final SymmetricEncryptor responseSymmetricEncryptor;
    private final AsymmetricEncryptor responseAsymmetricEncryptor;

    public ApiEncryptFilter(WebProperties webProperties,
                            ApiEncryptProperties apiEncryptProperties,
                            RequestMappingHandlerMapping requestMappingHandlerMapping,
                            GlobalExceptionHandler globalExceptionHandler) {
        super(webProperties);
        this.apiEncryptProperties = apiEncryptProperties;
        this.requestMappingHandlerMapping = requestMappingHandlerMapping;
        this.globalExceptionHandler = globalExceptionHandler;
        if (StrUtil.equalsIgnoreCase(apiEncryptProperties.getAlgorithm(), "AES")) {
            // 安全加固：ECB 模式相同明文输出相同密文，存在模式泄露风险，改为 CBC + 每次随机 IV。
            // 传输格式为 Base64(IV(16B) || 密文)，解密时先取前 16 字节作为 IV
            this.requestSymmetricDecryptor = buildAesCbcDecryptor(StrUtil.utf8Bytes(apiEncryptProperties.getRequestKey()));
            this.requestAsymmetricDecryptor = null;
            this.responseSymmetricEncryptor = buildAesCbcEncryptor(StrUtil.utf8Bytes(apiEncryptProperties.getResponseKey()));
            this.responseAsymmetricEncryptor = null;
        } else if (StrUtil.equalsIgnoreCase(apiEncryptProperties.getAlgorithm(), "RSA")) {
            this.requestSymmetricDecryptor = null;
            this.requestAsymmetricDecryptor = SecureUtil.rsa(apiEncryptProperties.getRequestKey(), null);
            this.responseSymmetricEncryptor = null;
            this.responseAsymmetricEncryptor = SecureUtil.rsa(null, apiEncryptProperties.getResponseKey());
        } else {
            // 补充说明：如果要支持 SM2、SM4 等算法，可在此处增加对应实例的创建，并添加相应的 Maven 依赖即可。
            throw new IllegalArgumentException("不支持的加密算法：" + apiEncryptProperties.getAlgorithm());
        }
    }

    @Override
    @SuppressWarnings("NullableProblems")
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // 获取 @ApiEncrypt 注解
        ApiEncrypt apiEncrypt = getApiEncrypt(request);
        boolean requestEnable = apiEncrypt != null && apiEncrypt.request();
        boolean responseEnable = apiEncrypt != null && apiEncrypt.response();
        String encryptHeader = request.getHeader(apiEncryptProperties.getHeader());
        if (!requestEnable && !responseEnable && StrUtil.isBlank(encryptHeader))  {
            chain.doFilter(request, response);
            return;
        }

        // 1. 解密请求
        if (ObjectUtils.equalsAny(HttpMethod.valueOf(request.getMethod()),
                HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE)) {
            try {
                if (StrUtil.isNotBlank(encryptHeader)) {
                    request = new ApiDecryptRequestWrapper(request,
                            requestSymmetricDecryptor, requestAsymmetricDecryptor);
                } else if (requestEnable) {
                    throw invalidParamException("请求未包含加密标头，请检查是否正确配置了加密标头");
                }
            } catch (Exception ex) {
                CommonResult<?> result = globalExceptionHandler.allExceptionHandler(request, ex);
                ServletUtils.writeJSON(response, result);
                return;
            }
        }

        // 2. 执行过滤器链
        if (responseEnable) {
            // 特殊：仅包装，最后执行。目的：Response 内容可以被重复读取！！！
            response = new ApiEncryptResponseWrapper(response);
        }
        chain.doFilter(request, response);

        // 3. 加密响应（真正执行）
        if (responseEnable) {
            ((ApiEncryptResponseWrapper) response).encrypt(apiEncryptProperties,
                    responseSymmetricEncryptor, responseAsymmetricEncryptor);
        }
    }

    /**
     * 获取 @ApiEncrypt 注解
     *
     * @param request 请求
     */
    @SuppressWarnings("PatternVariableCanBeUsed")
    private ApiEncrypt getApiEncrypt(HttpServletRequest request) {
        try {
            // 特殊：兼容 SpringBoot 2.X 版本会报错的问题 https://t.zsxq.com/kqyiB
            if (!ServletRequestPathUtils.hasParsedRequestPath(request)) {
                ServletRequestPathUtils.parseAndCache(request);
            }

            // 解析 @ApiEncrypt 注解
            HandlerExecutionChain mappingHandler = requestMappingHandlerMapping.getHandler(request);
            if (mappingHandler == null) {
                return null;
            }
            Object handler = mappingHandler.getHandler();
            if (handler instanceof HandlerMethod) {
                HandlerMethod handlerMethod = (HandlerMethod) handler;
                ApiEncrypt annotation = handlerMethod.getMethodAnnotation(ApiEncrypt.class);
                if (annotation == null) {
                    annotation = handlerMethod.getBeanType().getAnnotation(ApiEncrypt.class);
                }
                return annotation;
            }
        } catch (Exception e) {
            log.error("[getApiEncrypt][url({}/{}) 获取 @ApiEncrypt 注解失败]",
                    request.getRequestURI(), request.getMethod(), e);
        }
        return null;
    }

    /**
     * 构建 AES/CBC/PKCS5Padding 加密器：每次加密生成随机 16 字节 IV，输出格式为 IV(16B) || 密文
     *
     * @param key 加密秘钥
     * @return SymmetricEncryptor 对象
     */
    private static SymmetricEncryptor buildAesCbcEncryptor(byte[] key) {
        return new SymmetricEncryptor() {

            @Override
            public byte[] encrypt(byte[] data) {
                byte[] iv = new byte[IV_LENGTH];
                SECURE_RANDOM.nextBytes(iv);
                byte[] encrypted = aesCbc(Cipher.ENCRYPT_MODE, key, iv, data);
                // 输出格式：IV(16B) || 密文
                byte[] result = new byte[iv.length + encrypted.length];
                System.arraycopy(iv, 0, result, 0, iv.length);
                System.arraycopy(encrypted, 0, result, iv.length, encrypted.length);
                return result;
            }

            @Override
            public void encrypt(InputStream in, OutputStream out, boolean isClose) {
                try {
                    out.write(encrypt(IoUtil.readBytes(in, false)));
                } catch (IOException e) {
                    throw new CryptoException(e);
                } finally {
                    IoUtil.close(in);
                    IoUtil.close(out);
                }
            }
        };
    }

    /**
     * 构建 AES/CBC/PKCS5Padding 解密器：入参格式为 IV(16B) || 密文，先取前 16 字节作为 IV 再解密
     *
     * @param key 解密秘钥
     * @return SymmetricDecryptor 对象
     */
    private static SymmetricDecryptor buildAesCbcDecryptor(byte[] key) {
        return new SymmetricDecryptor() {

            @Override
            public byte[] decrypt(byte[] data) {
                if (data.length <= IV_LENGTH) {
                    throw new CryptoException("密文长度不合法，缺少 IV 前缀");
                }
                // 前 16 字节为随机 IV，剩余部分为密文
                byte[] iv = new byte[IV_LENGTH];
                System.arraycopy(data, 0, iv, 0, IV_LENGTH);
                byte[] encrypted = new byte[data.length - IV_LENGTH];
                System.arraycopy(data, IV_LENGTH, encrypted, 0, encrypted.length);
                return aesCbc(Cipher.DECRYPT_MODE, key, iv, encrypted);
            }

            @Override
            public void decrypt(InputStream in, OutputStream out, boolean isClose) {
                try {
                    out.write(decrypt(IoUtil.readBytes(in, false)));
                } catch (IOException e) {
                    throw new CryptoException(e);
                } finally {
                    IoUtil.close(in);
                    IoUtil.close(out);
                }
            }
        };
    }

    /**
     * 执行 AES/CBC/PKCS5Padding 加解密
     *
     * @param opMode Cipher.ENCRYPT_MODE 或 Cipher.DECRYPT_MODE
     * @param key    秘钥
     * @param iv     初始向量
     * @param data   数据（加密时为明文、解密时为密文）
     * @return 结果数据
     */
    private static byte[] aesCbc(int opMode, byte[] key, byte[] iv, byte[] data) {
        try {
            Cipher cipher = Cipher.getInstance(AES_CBC_PADDING);
            cipher.init(opMode, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
            return cipher.doFinal(data);
        } catch (GeneralSecurityException e) {
            throw new CryptoException(e);
        }
    }

}
