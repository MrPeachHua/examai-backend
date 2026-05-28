package cn.iocoder.yudao.module.system.service.auth;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.common.util.monitor.TracerUtils;
import cn.iocoder.yudao.framework.common.util.servlet.ServletUtils;
import cn.iocoder.yudao.module.system.api.logger.dto.LoginLogCreateReqDTO;
import cn.iocoder.yudao.module.system.api.social.SocialClientApi;
import cn.iocoder.yudao.module.system.api.social.SocialUserApi;
import cn.iocoder.yudao.module.system.api.social.dto.SocialUserBindReqDTO;
import cn.iocoder.yudao.module.system.api.social.dto.SocialUserRespDTO;
import cn.iocoder.yudao.module.system.controller.app.auth.vo.*;
import cn.iocoder.yudao.module.system.dal.dataobject.oauth2.OAuth2AccessTokenDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper;
import cn.iocoder.yudao.module.system.enums.logger.LoginLogTypeEnum;
import cn.iocoder.yudao.module.system.enums.logger.LoginResultEnum;
import cn.iocoder.yudao.module.system.enums.oauth2.OAuth2ClientConstants;
import cn.iocoder.yudao.module.system.enums.social.SocialTypeEnum;
import cn.iocoder.yudao.module.system.service.logger.LoginLogService;
import cn.iocoder.yudao.module.system.service.oauth2.OAuth2TokenService;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.servlet.ServletUtils.getClientIP;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.*;

/**
 * ExamAI 认证 Service 实现类
 */
@Service
@Slf4j
public class ExamAuthServiceImpl implements ExamAuthService {

    @Resource
    private AdminUserService userService;
    @Resource
    private AdminUserMapper userMapper;
    @Resource
    private PasswordEncoder passwordEncoder;
    @Resource
    private OAuth2TokenService oauth2TokenService;
    @Resource
    private LoginLogService loginLogService;
    @Resource
    private SocialUserApi socialUserApi;
    @Resource
    private SocialClientApi socialClientApi;

    @Value("${wechat.app-id:}")
    private String wechatAppId;
    @Value("${wechat.app-secret:}")
    private String wechatAppSecret;

    @Override
    public AppAuthRespVO login(AppAuthLoginReqVO reqVO) {
        // 1. 校验账号密码
        AdminUserDO user = authenticate(reqVO.getUsername(), reqVO.getPassword());

        // 2. 创建 Token 令牌，记录登录日志
        OAuth2AccessTokenDO accessTokenDO = createTokenAfterLoginSuccess(user, LoginLogTypeEnum.LOGIN_USERNAME);

        // 3. 构建返回结果
        return buildAuthResp(accessTokenDO, user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppAuthRespVO register(AppAuthRegisterReqVO reqVO) {
        // 1. 校验用户名是否已存在
        AdminUserDO existUser = userService.getUserByUsername(reqVO.getUsername());
        if (existUser != null) {
            throw exception(AUTH_USERNAME_ALREADY_EXISTS);
        }

        // 2. 创建用户
        AdminUserDO user = new AdminUserDO();
        user.setUsername(reqVO.getUsername());
        user.setPassword(passwordEncoder.encode(reqVO.getPassword()));
        user.setNickname(StrUtil.isNotBlank(reqVO.getNickname()) ? reqVO.getNickname() : reqVO.getUsername());
        user.setEmail(reqVO.getEmail());
        user.setMobile(reqVO.getPhone());
        user.setRole(reqVO.getRole() != null ? reqVO.getRole() : "student");
        user.setKnowledgeBeans(0);
        user.setIsVip(false);
        user.setStatus(CommonStatusEnum.ENABLE.getStatus());
        userMapper.insert(user);

        // 3. 创建 Token 令牌，记录登录日志
        OAuth2AccessTokenDO accessTokenDO = createTokenAfterLoginSuccess(user, LoginLogTypeEnum.LOGIN_USERNAME);

        // 4. 构建返回结果
        return buildAuthResp(accessTokenDO, user);
    }

    @Override
    public AppUserInfoRespVO getUserInfo(Long userId) {
        AdminUserDO user = userService.getUser(userId);
        if (user == null) {
            throw exception(AUTH_USER_NOT_FOUND);
        }
        return convertUserInfo(user);
    }

    @Override
    public void logout(String token) {
        OAuth2AccessTokenDO accessTokenDO = oauth2TokenService.removeAccessToken(token);
        if (accessTokenDO == null) {
            return;
        }
        // 记录登出日志
        createLogoutLog(accessTokenDO.getUserId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long userId, AppChangePasswordReqVO reqVO) {
        AdminUserDO user = userService.getUser(userId);
        if (user == null) {
            throw exception(AUTH_USER_NOT_FOUND);
        }
        // 校验旧密码
        if (!userService.isPasswordMatch(reqVO.getOldPassword(), user.getPassword())) {
            throw exception(AUTH_CHANGE_PASSWORD_FAILED);
        }
        // 更新密码
        userService.updateUserPassword(user.getId(), reqVO.getNewPassword());
        // 移除所有 Token，强制重新登录
        oauth2TokenService.removeAccessToken(user.getId(), UserTypeEnum.MEMBER.getValue());
    }

    @Override
    public void sendCode(AppSendCodeReqVO reqVO) {
        if (StrUtil.isNotBlank(reqVO.getPhone())) {
            log.info("发送短信验证码: phone={}, type={}", reqVO.getPhone(), reqVO.getType());
        } else if (StrUtil.isNotBlank(reqVO.getEmail())) {
            log.info("发送邮箱验证码: email={}, type={}", reqVO.getEmail(), reqVO.getType());
        } else {
            throw exception(AUTH_SEND_CODE_FAILED);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(AppResetPasswordReqVO reqVO) {
        // 根据手机号或邮箱查找用户
        AdminUserDO user = null;
        if (StrUtil.isNotBlank(reqVO.getPhone())) {
            user = userService.getUserByMobile(reqVO.getPhone());
        } else if (StrUtil.isNotBlank(reqVO.getEmail())) {
            user = userService.getUserByUsername(reqVO.getEmail());
        }
        if (user == null) {
            throw exception(AUTH_USER_NOT_FOUND);
        }
        // 更新密码
        userService.updateUserPassword(user.getId(), reqVO.getNewPassword());
        // 移除所有 Token
        oauth2TokenService.removeAccessToken(user.getId(), UserTypeEnum.MEMBER.getValue());
    }

    // ========== 私有方法 ==========

    /**
     * 校验账号密码
     */
    private AdminUserDO authenticate(String username, String password) {
        final LoginLogTypeEnum logTypeEnum = LoginLogTypeEnum.LOGIN_USERNAME;
        AdminUserDO user = userService.getUserByUsername(username);
        if (user == null) {
            createLoginLog(null, username, logTypeEnum, LoginResultEnum.BAD_CREDENTIALS);
            throw exception(AUTH_LOGIN_BAD_CREDENTIALS);
        }
        if (!userService.isPasswordMatch(password, user.getPassword())) {
            createLoginLog(user.getId(), username, logTypeEnum, LoginResultEnum.BAD_CREDENTIALS);
            throw exception(AUTH_LOGIN_BAD_CREDENTIALS);
        }
        if (CommonStatusEnum.isDisable(user.getStatus())) {
            createLoginLog(user.getId(), username, logTypeEnum, LoginResultEnum.USER_DISABLED);
            throw exception(AUTH_LOGIN_USER_DISABLED);
        }
        return user;
    }

    /**
     * 登录成功后创建 Token 并记录日志
     */
    private OAuth2AccessTokenDO createTokenAfterLoginSuccess(AdminUserDO user, LoginLogTypeEnum logType) {
        createLoginLog(user.getId(), user.getUsername(), logType, LoginResultEnum.SUCCESS);
        return oauth2TokenService.createAccessToken(user.getId(),
                UserTypeEnum.MEMBER.getValue(), OAuth2ClientConstants.CLIENT_ID_DEFAULT, null);
    }

    /**
     * 构建认证响应（令牌 + 用户信息）
     */
    private AppAuthRespVO buildAuthResp(OAuth2AccessTokenDO accessTokenDO, AdminUserDO user) {
        AppAuthRespVO resp = new AppAuthRespVO();
        resp.setToken(accessTokenDO.getAccessToken());
        resp.setUser(convertUserInfo(user));
        return resp;
    }

    /**
     * 将 AdminUserDO 转换为 AppUserInfoRespVO
     */
    private AppUserInfoRespVO convertUserInfo(AdminUserDO user) {
        AppUserInfoRespVO resp = new AppUserInfoRespVO();
        resp.setId(user.getId());
        resp.setName(user.getNickname());
        resp.setAvatar(user.getAvatar());
        resp.setEmail(user.getEmail());
        resp.setPhone(user.getMobile());
        resp.setRole(user.getRole() != null ? user.getRole() : "student");
        resp.setKnowledgeBeans(user.getKnowledgeBeans() != null ? user.getKnowledgeBeans() : 0);
        resp.setIsVip(user.getIsVip() != null ? user.getIsVip() : false);
        resp.setVipExpireTime(user.getVipExpireTime() != null ? user.getVipExpireTime().toString() : null);
        return resp;
    }

    private void createLoginLog(Long userId, String username, LoginLogTypeEnum logType, LoginResultEnum loginResult) {
        LoginLogCreateReqDTO reqDTO = new LoginLogCreateReqDTO();
        reqDTO.setLogType(logType.getType());
        reqDTO.setTraceId(TracerUtils.getTraceId());
        reqDTO.setUserId(userId);
        reqDTO.setUserType(UserTypeEnum.MEMBER.getValue());
        reqDTO.setUsername(username);
        reqDTO.setUserAgent(ServletUtils.getUserAgent());
        reqDTO.setUserIp(ServletUtils.getClientIP());
        reqDTO.setResult(loginResult.getResult());
        loginLogService.createLoginLog(reqDTO);
        if (userId != null && Objects.equals(LoginResultEnum.SUCCESS.getResult(), loginResult.getResult())) {
            userService.updateUserLogin(userId, ServletUtils.getClientIP());
        }
    }

    private void createLogoutLog(Long userId) {
        LoginLogCreateReqDTO reqDTO = new LoginLogCreateReqDTO();
        reqDTO.setLogType(LoginLogTypeEnum.LOGOUT_SELF.getType());
        reqDTO.setTraceId(TracerUtils.getTraceId());
        reqDTO.setUserId(userId);
        reqDTO.setUserType(UserTypeEnum.MEMBER.getValue());
        reqDTO.setUsername(getUsername(userId));
        reqDTO.setUserAgent(ServletUtils.getUserAgent());
        reqDTO.setUserIp(ServletUtils.getClientIP());
        reqDTO.setResult(LoginResultEnum.SUCCESS.getResult());
        loginLogService.createLoginLog(reqDTO);
    }

    private String getUsername(Long userId) {
        if (userId == null) return null;
        AdminUserDO user = userService.getUser(userId);
        return user != null ? user.getUsername() : null;
    }

    // ========== 微信登录相关 ==========

    @Override
    public String getWechatAuthUrl(String redirectUri, String role) {
        // 使用系统内置的社交登录功能
        return socialClientApi.getAuthorizeUrl(
                SocialTypeEnum.WECHAT_OPEN.getType(),
                UserTypeEnum.MEMBER.getValue(),
                redirectUri
        ).getCheckedData();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppAuthRespVO wechatLogin(AppAuthWechatLoginReqVO reqVO) {
        // 1. 使用 code 获取社交用户信息
        SocialUserRespDTO socialUser = socialUserApi.getSocialUserByCode(
                UserTypeEnum.MEMBER.getValue(),
                SocialTypeEnum.WECHAT_OPEN.getType(),
                reqVO.getCode(),
                reqVO.getState()
        ).getCheckedData();

        if (socialUser == null) {
            throw exception(AUTH_WECHAT_LOGIN_FAILED);
        }

        // 2. 判断是否已经绑定了用户
        AdminUserDO user;
        if (socialUser.getUserId() != null) {
            // 已绑定，直接获取用户信息
            user = userService.getUser(socialUser.getUserId());
            if (user == null) {
                throw exception(AUTH_USER_NOT_FOUND);
            }
        } else {
            // 未绑定，创建新用户
            user = createUserFromWechat(socialUser, reqVO.getRole());
            // 绑定社交用户
            socialUserApi.bindSocialUser(new SocialUserBindReqDTO(
                    user.getId(),
                    UserTypeEnum.MEMBER.getValue(),
                    SocialTypeEnum.WECHAT_OPEN.getType(),
                    reqVO.getCode(),
                    reqVO.getState()
            )).checkError();
        }

        // 3. 检查用户状态
        if (CommonStatusEnum.isDisable(user.getStatus())) {
            createLoginLog(user.getId(), user.getUsername(), LoginLogTypeEnum.LOGIN_SOCIAL, LoginResultEnum.USER_DISABLED);
            throw exception(AUTH_LOGIN_USER_DISABLED);
        }

        // 4. 创建 Token 并返回
        OAuth2AccessTokenDO accessTokenDO = createTokenAfterLoginSuccess(user, LoginLogTypeEnum.LOGIN_SOCIAL);
        return buildAuthResp(accessTokenDO, user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppAuthRespVO wechatBind(AppAuthWechatBindReqVO reqVO) {
        // 1. 验证账号密码
        AdminUserDO user = authenticate(reqVO.getUsername(), reqVO.getPassword());

        // 2. 绑定微信
        socialUserApi.bindSocialUser(new SocialUserBindReqDTO(
                user.getId(),
                UserTypeEnum.MEMBER.getValue(),
                SocialTypeEnum.WECHAT_OPEN.getType(),
                reqVO.getCode(),
                reqVO.getState()
        )).checkError();

        // 3. 创建 Token 并返回
        OAuth2AccessTokenDO accessTokenDO = createTokenAfterLoginSuccess(user, LoginLogTypeEnum.LOGIN_SOCIAL);
        return buildAuthResp(accessTokenDO, user);
    }

    /**
     * 根据微信用户信息创建新用户
     */
    private AdminUserDO createUserFromWechat(SocialUserRespDTO socialUser, String role) {
        AdminUserDO user = new AdminUserDO();
        // 生成唯一用户名
        String username = "wx_" + System.currentTimeMillis();
        user.setUsername(username);
        // 随机生成密码（用户后续可通过手机号重置）
        user.setPassword(passwordEncoder.encode(StrUtil.uuid()));
        user.setNickname(StrUtil.isNotBlank(socialUser.getNickname()) ? socialUser.getNickname() : "微信用户");
        user.setAvatar(socialUser.getAvatar());
        user.setRole(StrUtil.isNotBlank(role) ? role : "student");
        user.setKnowledgeBeans(0);
        user.setIsVip(false);
        user.setStatus(CommonStatusEnum.ENABLE.getStatus());
        userMapper.insert(user);
        return user;
    }

}
