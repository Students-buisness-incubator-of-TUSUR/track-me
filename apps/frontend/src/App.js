import React from "react";
import './App.css';
import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
import Login from "./accets/login/Login.js";
import Stream from "./accets/stream-page/stream-page.js";
import TrackerPage from "./accets/teamcard/TrackerPage.js";
import Register from "./accets/register/Registration.js"
const AdminPage = () => <h1>Страница Админа</h1>;
const SuperAdminPage = () => <h1>Страница Суперадмина</h1>;


function App() {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<Login />} />
        <Route path="/stream" element={<Stream />} />
        <Route path="/register" element={<Register />} />
        <Route path="/admin" element={<AdminPage />} />
        <Route path="/superadmin" element={<SuperAdminPage />} />
        <Route path="/tracker" element={<TrackerPage />} />
      </Routes>
    </Router>
  );
}

export default App;
