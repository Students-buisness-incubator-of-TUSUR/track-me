import React from "react";
import './App.css';
import {BrowserRouter as Router, Route, Routes} from "react-router-dom";
import Stream from "./accets/stream-page/stream-page.js";
import TrackerPage from "./accets/teamcard/TrackerPage.js";
import ProfilePage from "./accets/profile/ProfilePage.js"; // <-- наш компонент
import CreateStream from "./accets/create-stream-page/create-stream-page.js";
import TrackerList from "./accets/list-trackers/TrackerList.js";
import AdminList from "./accets/list-admins/AdminList.js"
import EditStream from "./accets/redo-stream-page/redo-stream-page.js"
import MeetingCard from "./accets/meeting-card/meeting-card.js";
import MeetingCard2 from "./accets/team-card-2/meeting-card-team.js";
import TeamCard from "./accets/komand/team-card.js"
import HomePage from "./accets/home/HomePage";
import AfterLogin from "./accets/after-login/AfterLogin";
import TeamCardCreate from "./accets/create-teamcard/team-card-create.js";
import MeetingCreate from "./accets/meeting-card/MeetingCreate.js";
import FeedbackWidget from "./accets/FeedbackWidget/FeedbackWidget.js"; // Импортируем layout
const AdminPage = () => <h1>Страница Админа</h1>;
const SuperAdminPage = () => <h1>Страница Суперадмина</h1>;

function App() {
    return (
        <Router>
            <div>
        <Routes>
          <Route path="/meeting-create/:teamId" element={<MeetingCreate />} />
          <Route path="/" element={<HomePage />} />
          <Route path="/after-login" element={<AfterLogin />} />
          <Route path="/streams" element={<Stream />} />
          <Route path="/create-stream" element={<CreateStream />} />
          <Route path="/edit-stream/:id" element={<EditStream />} />
          <Route path="/admin" element={<AdminPage />} />
          <Route path="/superadmin" element={<SuperAdminPage />} />
          <Route path="/team-cards" element={<TrackerPage />} />
          <Route path="/team-cards-PR" element={<MeetingCard2 />} />
          <Route path="/profile" element={<ProfilePage />} />
          <Route path="/list-admins" element={<AdminList />} />
          <Route path="/list-trackers" element={<TrackerList />} />
          <Route path="/metcard" element={<MeetingCard />} />
          <Route path="/team-card" element={<TeamCard />} />
          <Route path="/teamcard/create" element={<TeamCardCreate />} />
          <Route path="/teamcard/:id" element={<TeamCard />} />
          <Route path="/meeting/new" element={<MeetingCard />} />
          <Route path="/all-team-cards" element={<TrackerPage showAllCards={true} />} />
          <Route path="/meeting/:meetingId" element={<MeetingCard />} />
        </Routes>
        <FeedbackWidget />
      </div>
        </Router>
    );
}

export default App;
