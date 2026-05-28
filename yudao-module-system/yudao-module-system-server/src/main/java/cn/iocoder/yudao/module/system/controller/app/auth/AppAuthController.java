package cn.iocoder.yudao.module.system.controller.app.auth;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.security.config.SecurityProperties;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.system.controller.app.auth.vo.*;
import cn.iocoder.yudao.module.system.service.auth.ExamAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

@Tag(name = "用户 APP - 认证")
@RestController
@RequestMapping("/auth")
@Validated
@Slf4j
public class AppAuthController {

    @Resource
    private ExamAuthService authService;

    @Resource
    private SecurityProperties securityProperties;

    @PostMapping("/login")
    @Operation(summary = "使用账号密码登录")
    @PermitAll
    public CommonResult<AppAuthRespVO> login(@RequestBody @Valid AppAuthLoginReqVO reqVO) {
        return success(authService.login(reqVO));
    }

    @PostMapping("/register")
    @Operation(summary = "注册用户")
    @PermitAll
    public CommonResult<AppAuthRespVO> register(@RequestBody @Valid AppAuthRegisterReqVO reqVO) {
        return success(authService.register(reqVO));
    }

    @GetMapping("/user")
    @Operation(summary = "获取当前登录用户信息")
    public CommonResult<AppUserInfoRespVO> getUserInfo() {
        AppUserInfoRespVO userInfo = authService.getUserInfo(getLoginUserId());
        return success(userInfo);
    }

    @PostMapping("/logout")
    @Operation(summary = "登出系统")
    @PermitAll
    public CommonResult<Boolean> logout(HttpServletRequest request) {
        String token = SecurityFrameworkUtils.obtainAuthorization(request,
                securityProperties.getTokenHeader(), securityProperties.getTokenParameter());
        if (StrUtil.isNotBlank(token)) {
            authService.logout(token);
        }
        return success(true);
    }

    @PostMapping("/change-password")
    @Operation(summary = "修改密码")
    public CommonResult<Boolean> changePassword(@RequestBody @Valid AppChangePasswordReqVO reqVO) {
        authService.changePassword(getLoginUserId(), reqVO);
        return success(true);
    }

    @PostMapping("/send-code")
    @Operation(summary = "发送验证码")
    @PermitAll
    public CommonResult<Boolean> sendCode(@RequestBody @Valid AppSendCodeReqVO reqVO) {
        authService.sendCode(reqVO);
        return success(true);
    }

    @PostMapping("/reset-password")
    @Operation(summary = "重置密码")
    @PermitAll
    public CommonResult<Boolean> resetPassword(@RequestBody @Valid AppResetPasswordReqVO reqVO) {
        authService.resetPassword(reqVO);
        return success(true);
    }

    // ========== 微信登录相关 ==========

    @GetMapping("/wechat-auth-url")
    @Operation(summary = "获取微信授权URL")
    @PermitAll
    public CommonResult<String> getWechatAuthUrl(@RequestParam("redirectUri") String redirectUri,
                                                  @RequestParam(value = "role", required = false) String role) {
        return success(authService.getWechatAuthUrl(redirectUri, role));
    }

    @PostMapping("/wechat-login")
    @Operation(summary = "微信登录")
    @PermitAll
    public CommonResult<AppAuthRespVO> wechatLogin(@RequestBody @Valid AppAuthWechatLoginReqVO reqVO) {
        return success(authService.wechatLogin(reqVO));
    }

    @PostMapping("/wechat-bind")
    @Operation(summary = "微信绑定已有账号")
    @PermitAll
    public CommonResult<AppAuthRespVO> wechatBind(@RequestBody @Valid AppAuthWechatBindReqVO reqVO) {
        return success(authService.wechatBind(reqVO));
    }

}
