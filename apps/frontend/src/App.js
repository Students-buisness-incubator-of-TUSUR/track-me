import React from "react";
import './App.css';
import {BrowserRouter as Router, Route, Routes} from "react-router-dom";
import Stream from "./accets/stream-page/stream-page.js";
import TrackerPage from "./accets/teamcard/TrackerPage.js";
import ProfilePage from "./accets/profile/ProfilePage.js";
import CreateStream from "./accets/stream-page-create/create-stream-page.js";
import TrackerList from "./accets/list-trackers/TrackerList.js";
import AdminList from "./accets/list-admins/AdminList.js";
import EditStream from "./accets/stream-page-redo/redo-stream-page.js";
import MeetingCard from "./accets/meeting-card/meeting-card.js";
import MeetingCard2 from "./accets/team-card-2/meeting-card-team.js";
import TeamCard from "./accets/komand/team-card.js";
import HomePage from "./accets/home/HomePage";
import AfterLogin from "./accets/after-login/AfterLogin";
import TeamCardCreate from "./accets/create-teamcard/team-card-create.js";
import MeetingCreate from "./accets/meeting-card/MeetingCreate.js";
import FeedbackWidget from "./accets/FeedbackWidget/FeedbackWidget.js";
import ProtectedRoute from "./accets/ProtectedRoute/ProtectedRoute.js"; // Импортируем компонент защиты

const AdminPage = () => <h1>Страница Админа</h1>;
const SuperAdminPage = () => <h1>Страница Суперадмина</h1>;

function App() {
    return (
        <Router>
            <div>
                <Routes>
                    {/* Публичные маршруты (не требуют авторизации) */}
                    <Route path="/" element={<HomePage />} />
                    <Route path="/after-login" element={<AfterLogin />} />
                    
                    {/* Защищённые маршруты (требуют авторизации) */}
                    <Route path="/meeting-create/:teamId" element={
                        <ProtectedRoute>
                            <MeetingCreate />
                        </ProtectedRoute>
                    } />
                    <Route path="/streams" element={
                        <ProtectedRoute>
                            <Stream />
                        </ProtectedRoute>
                    } />
                    <Route path="/create-stream" element={
                        <ProtectedRoute>
                            <CreateStream />
                        </ProtectedRoute>
                    } />
                    <Route path="/edit-stream/:id" element={
                        <ProtectedRoute>
                            <EditStream />
                        </ProtectedRoute>
                    } />
                    <Route path="/admin" element={
                        <ProtectedRoute>
                            <AdminPage />
                        </ProtectedRoute>
                    } />
                    <Route path="/superadmin" element={
                        <ProtectedRoute>
                            <SuperAdminPage />
                        </ProtectedRoute>
                    } />
                    <Route path="/team-cards" element={
                        <ProtectedRoute>
                            <TrackerPage />
                        </ProtectedRoute>
                    } />
                    <Route path="/team-cards-PR" element={
                        <ProtectedRoute>
                            <MeetingCard2 />
                        </ProtectedRoute>
                    } />
                    <Route path="/profile" element={
                        <ProtectedRoute>
                            <ProfilePage />
                        </ProtectedRoute>
                    } />
                    <Route path="/list-admins" element={
                        <ProtectedRoute>
                            <AdminList />
                        </ProtectedRoute>
                    } />
                    <Route path="/list-trackers" element={
                        <ProtectedRoute>
                            <TrackerList />
                        </ProtectedRoute>
                    } />
                    <Route path="/metcard" element={
                        <ProtectedRoute>
                            <MeetingCard />
                        </ProtectedRoute>
                    } />
                    <Route path="/team-card" element={
                        <ProtectedRoute>
                            <TeamCard />
                        </ProtectedRoute>
                    } />
                    <Route path="/teamcard/create" element={
                        <ProtectedRoute>
                            <TeamCardCreate />
                        </ProtectedRoute>
                    } />
                    <Route path="/teamcard/:id" element={
                        <ProtectedRoute>
                            <TeamCard />
                        </ProtectedRoute>
                    } />
                    <Route path="/meeting/new" element={
                        <ProtectedRoute>
                            <MeetingCard />
                        </ProtectedRoute>
                    } />
                    <Route path="/all-team-cards" element={
                        <ProtectedRoute>
                            <TrackerPage showAllCards={true} />
                        </ProtectedRoute>
                    } />
                    <Route path="/meeting/:meetingId" element={
                        <ProtectedRoute>
                            <MeetingCard />
                        </ProtectedRoute>
                    } />
                </Routes>
                <FeedbackWidget />
            </div>
        </Router>
    );
}

export default App;