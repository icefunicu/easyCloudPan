package com.easypan.entity.enums;


public enum ResponseCodeEnum {
    CODE_200(200, "请求成功", null),
    CODE_404(404, "请求地址不存在", "请检查请求路径是否正确"),
    CODE_500(500, "服务器返回错误，请联系管理员", "请稍后重试或联系技术支持"),
    CODE_600(600, "请求参数错误", "请检查请求参数是否正确"),
    CODE_601(601, "信息已经存在", "请修改后重试"),
    CODE_602(602, "文件不存在", "请检查文件是否已被删除"),
    CODE_603(603, "目录不存在", "请检查目录路径是否正确"),
    CODE_604(604, "文件名已存在", "请修改文件名后重试"),
    CODE_605(605, "不允许上传可执行文件类型", "请上传其他类型的文件"),
    CODE_606(606, "文件类型不匹配", "请上传正确类型的文件"),
    CODE_607(607, "文件类型校验失败", "请检查文件内容是否与扩展名匹配"),
    CODE_608(608, "分片参数错误", "请重新上传文件"),
    CODE_609(609, "分片大小校验失败", "请重试上传"),
    CODE_610(610, "上传请求过多", "请稍后重试"),
    CODE_611(611, "文件上传失败", "请重试上传"),
    CODE_612(612, "文件合并失败", "请重新上传文件"),
    CODE_613(613, "未找到分片文件", "请重新上传文件"),
    CODE_614(614, "图片验证码不正确", "请重新输入验证码"),
    CODE_615(615, "邮箱验证码不正确", "请重新获取验证码"),
    CODE_616(616, "邮箱验证码已失效", "请重新获取验证码"),
    CODE_617(617, "账号或密码错误", "请检查账号密码是否正确"),
    CODE_618(618, "账号已禁用", "请联系管理员解除禁用"),
    CODE_619(619, "邮箱账号已存在", "请使用其他邮箱注册"),
    CODE_620(620, "昵称已存在", "请使用其他昵称"),
    CODE_621(621, "邮箱账号不存在", "请检查邮箱是否正确"),
    CODE_622(622, "提取码错误", "请重新输入提取码"),
    CODE_623(623, "无法保存自己分享的文件", "请使用其他账号保存"),
    CODE_624(624, "邮件发送失败", "请检查邮箱地址是否正确"),
    CODE_625(625, "转码失败", "文件可能已损坏"),
    CODE_626(626, "视频处理失败", "文件格式可能不支持"),
    CODE_627(627, "存储配置错误", "请联系管理员检查存储配置"),
    CODE_628(628, "上传失败", "请重试上传"),
    CODE_629(629, "下载失败", "请重试下载"),
    CODE_630(630, "删除失败", "请重试删除"),
    CODE_631(631, "并发上传数超限", "请等待当前上传完成后再试"),
    CODE_632(632, "请先登录", "请登录后再操作"),
    CODE_633(633, "文件不存在或无权访问", "请检查文件权限"),
    CODE_634(634, "QQ登录失败", "请重试或使用其他登录方式"),
    CODE_635(635, "租户已被禁用", "请联系管理员"),
    CODE_636(636, "租户存储空间不足", "请联系管理员扩容"),
    CODE_637(637, "租户用户数量已达上限", "请联系管理员"),
    CODE_901(901, "登录超时，请重新登录", "请刷新页面重新登录"),
    CODE_902(902, "分享链接不存在或已失效", "分享可能已过期或被取消"),
    CODE_903(903, "分享验证失效，请重新验证", "请重新输入提取码"),
    CODE_904(904, "网盘空间不足，请扩容", "请删除部分文件或联系管理员扩容");

    private final Integer code;
    private final String msg;
    private final String suggestion;

    ResponseCodeEnum(Integer code, String msg, String suggestion) {
        this.code = code;
        this.msg = msg;
        this.suggestion = suggestion;
    }

    public Integer getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public static ResponseCodeEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ResponseCodeEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }
}
