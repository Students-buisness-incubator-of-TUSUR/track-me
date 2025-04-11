import {useEffect} from "react";

function AfterLogin() {
    useEffect(() => {
        fetch('http://localhost:8081/api/sso/userinfo', {credentials: 'include', method: 'GET'})
            .then(response => response.json())
            .then(data => {
                return data
            })
            .catch(error => {
                console.error("Error fetching user info:", error);
            });
    }, []);
}

export default AfterLogin;