package cn.iocoder.yudao.module.system.controller.app.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "用户 APP - 用户信息 Response VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppUserInfoRespVO {

    @Schema(description = "用户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "用户昵称", example = "张三")
    private String name;

    @Schema(description = "用户头像", example = "https://example.com/avatar.png")
    private String avatar;

    @Schema(description = "邮箱", example = "user@example.com")
    private String email;

    @Schema(description = "手机号", example = "13800138000")
    private String phone;

    @Schema(description = "角色", example = "student")
    private String role;

    @Schema(description = "知识豆数量", example = "100")
    private Integer knowledgeBeans;

    @Schema(description = "是否 VIP", example = "false")
    private Boolean isVip;

    @Schema(description = "VIP 过期时间", example = "2026-12-31 23:59:59")
    private String vipExpireTime;

}
