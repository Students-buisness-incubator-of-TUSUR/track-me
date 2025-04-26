import './App.css';
import {BrowserRouter as Router, Route, Routes} from "react-router-dom";
import Login from "./assets/login/Login";
import Register from "./assets/registration/Register";
import RegistrationSuccess from "./assets/registration-success/RegistrationSuccess";
import ConfirmRegistration from "./assets/registration-confirm/ConfirmRegistration";

function App() {
    return (
        <Router>
            <Routes>
                <Route path="/sso/client/login" element={<Login/>}/>
                <Route path="/sso/client/registration" element={<Register/>}/>
                <Route path="/sso/client/registration-success" element={<RegistrationSuccess/>}/>
                <Route path="/sso/client/registration-confirm" element={<ConfirmRegistration/>}/>
            </Routes>
        </Router>
    );
}

export default App;
