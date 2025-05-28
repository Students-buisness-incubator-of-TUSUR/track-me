import React from "react";
import TrackerListPage from "../listuser/TrackerListPage";

function AdminList() {
  return <TrackerListPage endpoint="/api/v1/users/administrators" />;
}

export default AdminList;
