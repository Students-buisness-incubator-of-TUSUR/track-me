import axios from 'axios';

export class LoginAPI {
    __LOGIN_URL = "/client/login";
    __LOCATION_HEADER = process.env.REACT_APP_SSO_LOCATION_HEADER;

    /**
     * Вход черед логин/пароль.
     * При успешной аутентификации получает в заголовках ответа специальный
     * заголовок {@see process.env.VUE_APP_SSO_LOCATION_HEADER} в котором содержится URL для дальнейшего перехода
     * @param username - логин
     * @param password - пароль
     */
    login(username, password) {
        let formData = new FormData();
        formData.append("username", username);
        formData.append("password", password);

        console.log(formData.get("username"));
        console.log(formData.get("password"));
        return axios.post(this.__LOGIN_URL, formData)
            .then(result => {
                console.log(result.headers);
                if (result.headers.has(this.__LOCATION_HEADER)) {
                    window.location = result.headers.get(this.__LOCATION_HEADER);
                }
            });
    }

    register(userData) {
        return axios.post("/api/v1/registration/init", userData);
    }
}

let loginAPI = new LoginAPI();
export default loginAPI;