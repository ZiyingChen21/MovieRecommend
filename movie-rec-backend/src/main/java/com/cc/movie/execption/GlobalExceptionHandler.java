package com.cc.movie.execption;

import com.cc.movie.common.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    /**
     * 专门捕获并处理参数校验异常 (@NotBlank, @NotNull 等触发的异常)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<String> handlerValidationException(MethodArgumentNotValidException e) {
        BindingResult bindingResult = e.getBindingResult();
        StringBuilder errMsg = new StringBuilder();

        for (FieldError fieldError : bindingResult.getFieldErrors()) {
            errMsg.append(fieldError.getDefaultMessage()).append("; ");
        }
        log.warn("参数校验失败: {}", errMsg.toString());
        return R.error(errMsg.toString());
    }

    @ExceptionHandler(Exception.class)
    public R<String> handleException(Exception e) {
        log.error("系统发生异常: ", e.getMessage());
        return R.error("服务器开小差了，请稍后再试");
    }

    /**
     * 专门处理业务中抛出的 RuntimeException
     */
    @ExceptionHandler(RuntimeException.class)
    public R<String> handlerRuntimeException(RuntimeException e) {
        log.warn("业务异常拦截: {}", e.getMessage());
        return R.error(e.getMessage());
    }
}
