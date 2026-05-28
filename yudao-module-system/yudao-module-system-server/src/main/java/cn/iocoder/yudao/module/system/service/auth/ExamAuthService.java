package cn.iocoder.yudao.module.system.service.auth;

import cn.iocoder.yudao.module.system.controller.app.auth.vo.*;

import javax.validation.Valid;

/**
 * ExamAI 认证 Service 接口
 *
 * 提供用户登录、注册、退出等能力
 */
public interface ExamAuthService {

    /**
     * 账号密码登录
     *
     * @param reqVO 登录信息
     * @return 登录结果（令牌 + 用户信息）
     */
    AppAuthRespVO login(@Valid AppAuthLoginReqVO reqVO);

    /**
     * 用户注册
     *
     * @param reqVO 注册信息
     * @return 注册结果（令牌 + 用户信息）
     */
    AppAuthRespVO register(@Valid AppAuthRegisterReqVO reqVO);

    /**
     * 获取当前登录用户信息
     *
     * @param userId 用户编号
     * @return 用户信息
     */
    AppUserInfoRespVO getUserInfo(Long userId);

    /**
     * 退出登录
     *
     * @param token 访问令牌
     */
    void logout(String token);

    /**
     * 修改密码
     *
     * @param userId 用户编号
     * @param reqVO 修改密码信息
     */
    void changePassword(Long userId, @Valid AppChangePasswordReqVO reqVO);

    /**
     * 发送验证码
     *
     * @param reqVO 发送验证码信息
     */
    void sendCode(@Valid AppSendCodeReqVO reqVO);

    /**
     * 重置密码
     *
     * @param reqVO 重置密码信息
     */
    void resetPassword(@Valid AppResetPasswordReqVO reqVO);

}
