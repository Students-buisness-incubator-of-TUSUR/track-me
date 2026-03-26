import './App.css';
import {BrowserRouter as Router, Route, Routes} from "react-router-dom";
import Login from "./assets/login/Login";
import Register from "./assets/registration/Register";

import RegistrationSuccess from "./assets/registration-success/RegistrationSuccess";
import ConfirmRegistration from "./assets/registration-confirm/ConfirmRegistration";

import PasswordRecovery from "./assets/password-recovery/PasswordRecovery";
import PasswordReset from './assets/password-reset/PasswordReset';

const basePath = process.env.REACT_APP_BASE_PATH || "";

function App() {
    return (
        <Router>
            <Routes>
                <Route path={`${basePath}/client/login`} element={<Login/>}/>
                <Route path={`${basePath}/client/reset`} element={<PasswordReset />} />
                <Route path={`${basePath}/client/registration`} element={<Register/>}/>
                <Route path={`${basePath}/client/registration-success`}
                       element={<RegistrationSuccess/>}/>
                <Route path={`${basePath}/client/registration-confirm`}
                       element={<ConfirmRegistration/>}/>
                <Route path={`${basePath}/client/recovery`}
                       element={<PasswordRecovery/>}/>
            </Routes>
        </Router>
    );
}

export default App;