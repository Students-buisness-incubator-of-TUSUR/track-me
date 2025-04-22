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
                <Route path="/client/login" element={<Login/>}/>
                <Route path="/client/registration" element={<Register/>}/>
                <Route path="/client/registration-success" element={<RegistrationSuccess/>}/>
                <Route path="/client/registration-confirm" element={<ConfirmRegistration/>}/>
            </Routes>
        </Router>
    );
}

export default App;
