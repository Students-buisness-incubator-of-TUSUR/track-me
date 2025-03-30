import React from "react";
import './App.css';
import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
import Login from "./accets/login/Login.js";
import Stream from "./accets/stream-page/stream-page.js";
import TrackerPage from "./accets/teamcard/TrackerPage.js";
import Register from "./accets/register/Registration.js";
import ProfilePage from "./accets/profile/ProfilePage.js"; // <-- наш компонент
import LoginRecovery from "./accets/login-recovery/login-recovery.js";
import CreateStream from "./accets/create-stream-page/create-stream-page.js";
import TrackerList from "./accets/list-trackers/TrackerList.js";
import AdminList from "./accets/list-admins/AdminList.js"
import LoginRecovery2 from "./accets/login-recovery-2/login-recovery.js";
import EditStream from "./accets/redo-stream-page/redo-stream-page.js"
import MeetingCard from "./accets/meeting-card/meeting-card.js";
import MeetingCard2 from "./accets/team-card-2/meeting-card-team.js";
import TeamCard from "./accets/komand/team-card.js"
const AdminPage = () => <h1>Страница Админа</h1>;
const SuperAdminPage = () => <h1>Страница Суперадмина</h1>;

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<Login />} />
        <Route path="/streams" element={<Stream />} />
        <Route path="/create-stream" element={<CreateStream />} />
        <Route path="/edit-stream/:id" element={<EditStream />} />
        <Route path="/register" element={<Register />} />
        <Route path="/admin" element={<AdminPage />} />
        <Route path="/superadmin" element={<SuperAdminPage />} />
        <Route path="/team-cards" element={<TrackerPage />} />
        <Route path="/team-cards-PR" element={<MeetingCard2 />} />
        <Route path="/recovery" element={<LoginRecovery />} />
        <Route path="/reset-password" element={<LoginRecovery2 />} />
        <Route path="/profile" element={<ProfilePage />} />
        <Route path="/list-admins" element={<AdminList />} />
        <Route path="/list-trackers" element={<TrackerList />} />
        <Route path="/metcard" element={<MeetingCard />} />
        <Route path="/team-card" element={<TeamCard />} />

      </Routes>
    </Router>
  );
}

export default App;
