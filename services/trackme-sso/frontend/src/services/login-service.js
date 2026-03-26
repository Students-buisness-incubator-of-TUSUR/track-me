import axios from "../csrf-axios-interceptor";

export class LoginAPI {
    basePath = process.env.REACT_APP_BASE_PATH || "http://localhost:9000";
    __LOGIN_URL = "/client/login";
    __LOCATION_HEADER = process.env.REACT_APP_SSO_LOCATION_HEADER;

    login(username, password) {
        let formData = new FormData();
        formData.append("username", username);
        formData.append("password", password);

        return axios.post(this.__LOGIN_URL, formData, {
            withCredentials: true
        }).then(response => {
            if (response.redirected) {
                window.location = response.url;
            }
        });
    }

    register(userData) {
        return axios.post(`${(this.basePath)}/api/v1/registration/init`, userData, {
            withCredentials: true
        });
    }

    recoveryPassword(email) {
        return axios.post(`${(this.basePath)}/api/v1/registration/recovery-password`, 
            { email },
            { withCredentials: true }
        );
    }

    resetPassword(token, password) {
        return axios.post(`${this.basePath}/api/v1/registration/reset-password`,
            { password },
            {
                params: { token },
                withCredentials: true
            }
    );
}
}

let loginAPI = new LoginAPI();
export default loginAPI;