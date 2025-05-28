import React from "react";
import { render } from "@testing-library/react";
import AdminList from "./AdminList";
import TrackerListPage from "../listuser/TrackerListPage";

jest.mock("../listuser/TrackerListPage", () => jest.fn(() => <div>Mocked TrackerListPage</div>));

test("renders AdminList with correct endpoint", () => {
  render(<AdminList />);
  const callArgs = TrackerListPage.mock.calls[0][0]; // первый вызов, первый аргумент (props)
  expect(callArgs).toMatchObject({ endpoint: "/api/v1/users/administrators" });
});
