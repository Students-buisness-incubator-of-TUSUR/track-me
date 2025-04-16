import axios from 'axios';
import store from '../store';
import {setUser} from '../store/userSlice';

export class LoginAPI {
    __LOGIN_URL = "/login";
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
                if (result.headers.has(this.__LOCATION_HEADER)) {
                    this.getUserinfo().then(() => {
                        window.location = result.headers.get(LoginAPI.__LOCATION_HEADER);
                    })
                }
            });
    }

    getUserinfo() {
        return axios.get("/userinfo", {
            withCredentials: true
        })
            .then(result => {
                store.dispatch(setUser(result.data));
            });
    }

    register(userData) {
        return axios.post("/register", userData)
            .then(response => {
                if (response.status === 200) {
                    window.location = "/registration-success";
                } else {
                    throw new Error("Registration failed");
                }
            });
    }
}


export default new LoginAPI();