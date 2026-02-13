const regs = {
    email: /^[\w-]+(\.[\w-]+)*@[\w-]+(\.[\w-]+)+$/,
    number: /^([0]|[1-9][0-9]*)$/,
    password: /^(?=.*\d)(?=.*[a-zA-Z])(?=.*[~!@#$%^&*_])[\da-zA-Z~!@#$%^&*_]{8,}$/,
    shareCode: /^[A-Za-z0-9]+$/
}

const verify = (rule: any, value: any, reg: RegExp, callback: (e?: Error) => void) => {
    if (value) {
        if (reg.test(value)) {
            callback()
        } else {
            callback(new Error(rule.message))
        }
    } else {
        callback()
    }
}

const getPasswordError = (value: string): string | null => {
    if (!value) return null
    if (value.length < 8) {
        return "密码长度至少8位"
    }
    if (!/\d/.test(value)) {
        return "密码必须包含数字"
    }
    if (!/[a-zA-Z]/.test(value)) {
        return "密码必须包含字母"
    }
    if (!/[~!@#$%^&*_]/.test(value)) {
        return "密码必须包含特殊字符(~!@#$%^&*_)"
    }
    if (!/^[\da-zA-Z~!@#$%^&*_]+$/.test(value)) {
        return "密码只能包含数字、字母和特殊字符(~!@#$%^&*_)"
    }
    return null
}

export default {
    email: (rule: any, value: any, callback: any) => {
        return verify(rule, value, regs.email, callback)
    },
    number: (rule: any, value: any, callback: any) => {
        return verify(rule, value, regs.number, callback)
    },
    password: (rule: any, value: any, callback: any) => {
        if (!value) {
            callback()
            return
        }
        const error = getPasswordError(value)
        if (error) {
            callback(new Error(error))
        } else {
            callback()
        }
    },
    shareCode: (rule: any, value: any, callback: any) => {
        return verify(rule, value, regs.shareCode, callback)
    },
}
