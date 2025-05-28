import React from "react";
import TrackerListPage from "../listuser/TrackerListPage";

function TrackerList() {
  return <TrackerListPage endpoint="/api/v1/users/trackers" />;
}

export default TrackerList;
