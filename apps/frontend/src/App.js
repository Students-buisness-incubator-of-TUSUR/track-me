import React from "react";
import './App.css';
import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
import Login from "./accets/Login.js";
import Stream from "./accets/stream-page/stream-page.js";

const Register = () => <h1>Страница регистрации</h1>;
const AdminPage = () => <h1>Страница Админа</h1>;
const SuperAdminPage = () => <h1>Страница Суперадмина</h1>;
const TrackerPage = () => <h1>Страница Трекера</h1>;

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
