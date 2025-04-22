import axios from 'axios';

export class LoginAPI {
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
        return axios.post("/api/v1/registration/init", userData, {
            withCredentials: true
        });
    }
}

let loginAPI = new LoginAPI();
export default loginAPI;