const MIN_LENGTH = 6;

export const passwordChecks = (password = "") => {
        return ""
            + (!/[A-Z]/.test(password) ? "\u{2022} пароль должен содержать заглавную букву\n" : "")
            + (!/[a-z]/.test(password) ? "\u{2022} пароль должен содержать строчную букву\n" : "")
            + (!/[0-9]/.test(password) ? "\u{2022} пароль должен содержать цифру\n" : "")
            + (!/[@$!%*?&]/.test(password) ? "\u{2022} пароль должен содержать специальный символ (@$!%*?&)\n" : "")
            + ((password.length < MIN_LENGTH) ? `\u{2022} длина пароля должна быть не менее ${MIN_LENGTH} символов\n` : "")
            + (password.length !== 0 && (!/[A-Za-z0-9@$!%*?&]+$/.test(password)) ? "\u{2022} пароль должен содержать только латинские символы\n" : "");
    }

export const passwordsMatch = (password, confirmPassword) =>
    confirmPassword && password !== confirmPassword
        ? "\u{2022} пароли не совпадают"
        : "";