import React from "react";
import { render } from "@testing-library/react";
import TrackerList from "./TrackerList";
import TrackerListPage from "../listuser/TrackerListPage";

jest.mock("../listuser/TrackerListPage", () => jest.fn(() => <div>Mocked TrackerListPage</div>));

test("renders TrackerList with correct endpoint", () => {
  render(<TrackerList />);
  const callArgs = TrackerListPage.mock.calls[0][0];
  expect(callArgs).toMatchObject({ endpoint: "/api/v1/users/trackers" });
});
