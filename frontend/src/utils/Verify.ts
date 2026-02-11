const regs = {
    email: /^[\w-]+(\.[\w-]+)*@[\w-]+(\.[\w-]+)+$/,
    number: /^([0]|[1-9][0-9]*)$/,
    password: /^(?=.*\d)(?=.*[a-zA-Z])[\da-zA-Z~!@#$%^&*_]{8,}$/,
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

export default {
    email: (rule: any, value: any, callback: any) => {
        return verify(rule, value, regs.email, callback)
    },
    number: (rule: any, value: any, callback: any) => {
        return verify(rule, value, regs.number, callback)
    },
    password: (rule: any, value: any, callback: any) => {
        return verify(rule, value, regs.password, callback)
    },
    shareCode: (rule: any, value: any, callback: any) => {
        return verify(rule, value, regs.shareCode, callback)
    },
}
