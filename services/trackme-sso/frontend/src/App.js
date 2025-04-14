import './App.css';
import {BrowserRouter as Router, Route, Routes} from "react-router-dom";
import Login from "./assets/login/Login";
import Register from "./assets/registration/Register";
import RegistrationSuccess from "./assets/registration/RegistrationSuccess";

function App() {
    return (
        <Router>
            <Routes>
                <Route path="/login" element={<Login/>}/>
                <Route path="/register" element={<Register/>}/>
                <Route path="/registration-success" element={<RegistrationSuccess/>}/>
            </Routes>
        </Router>
    );
}

export default App;
