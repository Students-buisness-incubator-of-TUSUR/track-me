import './App.css';
import {BrowserRouter as Router, Route, Routes} from "react-router-dom";
import Login from "./assets/login/Login";
import Register from "./assets/registration/Register";
import RegistrationSuccess from "./assets/registration-success/RegistrationSuccess";

function App() {
    return (
        <Router>
            <Routes>
                <Route path="/client/login" element={<Login/>}/>
                <Route path="/client/registration" element={<Register/>}/>
                <Route path="/client/registration-success" element={<RegistrationSuccess/>}/>
            </Routes>
        </Router>
    );
}

export default App;
