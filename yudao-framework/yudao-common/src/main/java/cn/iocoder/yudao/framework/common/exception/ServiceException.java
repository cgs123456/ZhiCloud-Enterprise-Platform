package cn.iocoder.yudao.framework.common.exception;

import cn.iocoder.yudao.framework.common.exception.enums.ServiceErrorCodeRange;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 业务逻辑异常 Exception
 */
@Data
@EqualsAndHashCode(callSuper = true)
public final class ServiceException extends RuntimeException {

    /**
     * 业务错误码
     *
     * @see ServiceErrorCodeRange
     */
    private Integer code;
    /**
     * 错误提示
     */
    private String message;

    /**
     * 空构造方法，避免反序列化问题
     */
    public ServiceException() {
    }

    public ServiceException(ErrorCode errorCode) {
        this.code = errorCode.getCode();
        this.message = errorCode.getMsg();
    }

    /**
     * 携带自定义业务提示的构造方法（裸抛异常统一改造用）。
     * 原始 {@code throw new RuntimeException/IllegalStateException("msg")} 应改用本方法，
     * 复用全局 {@code GlobalErrorCodeConstants.INTERNAL_SERVER_ERROR} 错误码，避免绕过 ErrorCode 体系。
     */
    public ServiceException(ErrorCode errorCode, String message) {
        this.code = errorCode.getCode();
        this.message = message;
    }

    /**
     * 携带自定义业务提示与根因的构造方法（裸抛异常统一改造用）。
     * 原始 {@code throw new RuntimeException/IllegalStateException("msg", cause)} 应改用本方法，
     * 既保留堆栈溯源，又通过 ErrorCode 向前端返回友好提示。
     */
    public ServiceException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.code = errorCode.getCode();
        this.message = message;
    }

    public ServiceException(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public ServiceException setCode(Integer code) {
        this.code = code;
        return this;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public ServiceException setMessage(String message) {
        this.message = message;
        return this;
    }

}
