import React from "react";
import {
  render,
  screen,
  fireEvent,
  waitFor,
  within,
  act,
} from "@testing-library/react";
import '@testing-library/jest-dom';
import { MemoryRouter, Route, Routes } from "react-router-dom";
import TeamCard from "./team-card";


const mockNavigate = jest.fn();

jest.mock("react-router-dom", () => ({
  ...jest.requireActual("react-router-dom"),
  useNavigate: () => mockNavigate,
}));

jest.mock("../meeting-card/MeetingCreate.js", () =>
  function MeetingCreate({ onClose, teamId, userRole }) {
    return (
      <div data-testid="meeting-create" data-team-id={teamId} data-role={userRole}>
        <button data-testid="meeting-create-close" onClick={onClose}>
          Close
        </button>
      </div>
    );
  }
);

jest.mock("../header/header", () =>
  function Header({ userRole }) {
    return <header data-testid="header" data-role={userRole} />;
  }
);

jest.mock("../input-box/input-box", () =>
  function InputBox({ value, onChange, readOnly, name, className, placeholder }) {
    return (
      <input
        data-testid={`inputbox-${name || "unnamed"}`}
        value={value || ""}
        onChange={onChange}
        readOnly={readOnly}
        name={name}
        className={className}
        placeholder={placeholder}
      />
    );
  }
);

jest.mock("../text-box/text-box", () =>
  function TextBox({ value, onChange, readOnly, name, className, placeholder }) {
    return (
      <textarea
        data-testid={`textbox-${name || "unnamed"}`}
        value={value || ""}
        onChange={onChange}
        readOnly={readOnly}
        name={name}
        className={className}
        placeholder={placeholder}
      />
    );
  }
);

jest.mock("../select-box/select-box", () =>
  function SelectBox({ value, onChange, readOnly, name, className, children }) {
    return (
      <select
        data-testid={`selectbox-${name || "unnamed"}`}
        value={value || ""}
        onChange={onChange}
        disabled={readOnly}
        name={name}
        className={className}
      >
        {children}
      </select>
    );
  }
);

jest.mock("../check-box/check-box", () =>
  function CheckBox({ title, children, className }) {
    return (
      <div data-testid="checkbox-container" className={className}>
        <span data-testid="checkbox-title">{title}</span>
        <div data-testid="checkbox-children">{children}</div>
      </div>
    );
  }
);

jest.mock("../../utils/csrf-utils", () => ({
  getCsrfConfigForFetch: () => ({ "X-CSRF-TOKEN": "test-token" }),
}));

const mockValidateMeetingDateChange = jest.fn(() => ({
  isValid: true,
  errorMessage: "",
}));

jest.mock("../../utils/date-utils", () => ({
  validateMeetingDateChange: (...args) => mockValidateMeetingDateChange(...args),
}));

const mockUseGetUserInfo = jest.fn();
jest.mock("../../services/util", () => ({
  useGetUserInfo: () => mockUseGetUserInfo(),
}));

const mockFetchTrackers = jest.fn();
jest.mock("../../services/requests", () => ({
  fetchTrackers: (...args) => mockFetchTrackers(...args),
}));

jest.mock("../../files/close.svg", () => ({
  ReactComponent: function CloseIcon() {
    return <svg data-testid="close-icon" />;
  },
}));

jest.mock("../../services/constants", () => ({
  adminRoleName: "ADMIN",
  superadminRoleName: "SUPER_ADMIN",
  backendURLBackend: "http://backend.test",
  backendURLMeeting: "http://meeting.test",
  backendURLSSO: "http://sso.test",
}));

const STREAM = {
  id: "stream-1",
  name: "Поток Альфа",
  active: true,
  startDate: "2024-01-15",
  endDate: "2024-06-30",
  meetingsCount: 5,
};

const TEAM_CARD = {
  id: "42",
  name: "Команда Икс",
  username: "tracker1",
  description: "Описание команды",
  meetingRoomLink: "https://zoom.us/room/123",
  readinessLevel: "3-5",
  averageGrade: 4.75,
  ntiMarkets: [{ id: 1, displayName: "Аэронет" }],
  ntiMarketIds: [1],
  streams: [STREAM],
};

const MEETINGS = [
  { id: "m1", number: "1", startDate: "2024-03-01T10:00:00", status: "COMPLETED" },
  { id: "m2", number: "2", startDate: "2024-04-01T10:00:00", status: "SCHEDULED" },
  { id: "m3", number: "3", startDate: "2024-05-01T10:00:00", status: "NOT_HAPPENED" },
];

const TRACKERS = [
  { id: "t1", username: "tracker1", fullName: "Иван Иванов", enabled: true },
  { id: "t2", username: "tracker2", fullName: "Мария Петрова", enabled: true },
  { id: "t3", username: "tracker3", fullName: "Отключённый Трекер", enabled: false },
];

const NTI_MARKETS = [
  { id: 1, displayName: "Аэронет" },
  { id: 2, displayName: "Маринет" },
  { id: 3, displayName: "Нейронет" },
];

const STREAMS_LIST = [
  { id: "stream-1", name: "Поток Альфа", active: true },
  { id: "stream-2", name: "Поток Бета", active: true },
];

function ok(data) {
  return Promise.resolve({
    ok: true,
    status: 200,
    json: () => Promise.resolve(data),
    text: () => Promise.resolve(JSON.stringify(data)),
  });
}

function fail(status, text) {
  return Promise.resolve({
    ok: false,
    status,
    json: () => Promise.resolve({}),
    text: () => Promise.resolve(text),
  });
}

function buildFetch({
  teamCard = TEAM_CARD,
  meetings = MEETINGS,
  ntiMarkets = NTI_MARKETS,
  streamsList = STREAMS_LIST,
  teamCardsCount = 7,
  deleteSuccess = true,
} = {}) {
  return jest.fn((url, options = {}) => {
    const method = (options.method || "GET").toUpperCase();

    if (url.includes("meeting.test/api/v1/meetings")) {
      return ok({ content: meetings, totalPages: 1 });
    }
    if (url.includes("/api/v1/update-meeting/") && method === "PATCH") {
      return ok({ ...meetings[0], startDate: "2024-06-01T10:00:00" });
    }
    if (url.includes("/api/v1/delete-meeting/") && method === "DELETE") {
      return deleteSuccess ? ok({}) : fail(500, "Internal Server Error");
    }
    if (url.includes("backend.test/api/v1/admin/team-cards")) {
      return ok({ content: [teamCard], totalPages: 1 });
    }
    if (url.includes("backend.test/api/v1/team-cards")) {
      return ok({ content: [teamCard], totalPages: 1 });
    }
    if (url.includes("backend.test/api/v1/admin/team-card") && method === "PATCH") {
      return ok(teamCard);
    }
    if (url.includes("backend.test/api/v1/team-card") && method === "PATCH") {
      return ok(teamCard);
    }
    if (url.includes("backend.test/api/v1/admin/team-card") && method === "DELETE") {
      return ok({});
    }
    if (url.includes("backend.test/api/v1/team-card") && method === "DELETE") {
      return ok({});
    }
    if (url.includes("/api/v1/team-card/count")) {
      return ok(teamCardsCount);
    }
    if (url.includes("backend.test/api/v1/streams?")) {
      return ok({ content: streamsList });
    }
    if (url.includes("/api/v1/streams/nti-markets")) {
      return ok(ntiMarkets);
    }
    if (url.includes("sso.test/api/v1/users/") || url.includes("sso.test/api/v1/account/info")) {
      return ok({ fullName: "Test FullName" });
    }
    return ok({});
  });
}

function renderTeamCard({
  role = "TRACKER",
  cardId = "42",
  search = "",
  locationState = {},
  fetchOverrides = {},
} = {}) {
  mockUseGetUserInfo.mockReturnValue({ roles: [role], username: "tracker1" });
  mockFetchTrackers.mockResolvedValue({
    ok: true,
    json: () => Promise.resolve({ content: fetchOverrides.trackers || TRACKERS }),
  });
  global.fetch = buildFetch(fetchOverrides);

  return render(
    <MemoryRouter
      initialEntries={[
        { pathname: `/team-cards/${cardId}`, search, state: locationState },
      ]}
    >
      <Routes>
        <Route path="/team-cards/:id" element={<TeamCard />} />
        <Route path="/team-cards" element={<div data-testid="team-cards-page" />} />
        <Route path="/meeting/:id" element={<div data-testid="meeting-page" />} />
      </Routes>
    </MemoryRouter>
  );
}

async function waitForLoad() {
  await waitFor(() => {
    expect(screen.getByTestId("inputbox-name")).toHaveValue("Команда Икс");
    expect(screen.getByText("Поток Альфа")).toBeInTheDocument();
  });
}

async function enterEditMode() {
  await waitForLoad();
  fireEvent.click(screen.getByRole("button", { name: /редактировать/i }));
}

beforeEach(() => {
  jest.clearAllMocks();
  window.confirm = jest.fn(() => true);
  mockValidateMeetingDateChange.mockReturnValue({ isValid: true, errorMessage: "" });
});

afterEach(() => {
  jest.useRealTimers();
});

describe("Initial render", () => {
  it("renders the Header with correct role", async () => {
    renderTeamCard({ role: "TRACKER" });
    await waitForLoad();
    expect(screen.getByTestId("header")).toHaveAttribute("data-role", "TRACKER");
  });

  it("renders team name after data loads", async () => {
    renderTeamCard();
    await waitForLoad();
    expect(screen.getByTestId("inputbox-name")).toHaveValue("Команда Икс");
  });

  it("renders team description after data loads", async () => {
    renderTeamCard();
    await waitForLoad();
    expect(screen.getByTestId("textbox-description")).toHaveValue("Описание команды");
  });

  it("renders average grade formatted in Russian locale", async () => {
    renderTeamCard();
    await waitForLoad();
    expect(screen.getByText("4,75")).toBeInTheDocument();
  });

  it("does not render grade element when averageGrade is null", async () => {
    renderTeamCard({
      fetchOverrides: { teamCard: { ...TEAM_CARD, averageGrade: null } },
    });
    await waitFor(() => expect(screen.getByTestId("header")).toBeInTheDocument());
    expect(screen.queryByText(/,\d{2}/)).not.toBeInTheDocument();
  });

  it("renders 'Редактировать' button in view mode", async () => {
    renderTeamCard();
    await waitForLoad();
    expect(screen.getByRole("button", { name: /редактировать/i })).toBeInTheDocument();
  });

  it("renders close button with CloseIcon", async () => {
    renderTeamCard();
    await waitForLoad();
    expect(screen.getByTestId("close-icon")).toBeInTheDocument();
  });

  it("renders all meetings sorted by number", async () => {
    renderTeamCard();
    await waitForLoad();
    await waitFor(() => {
      expect(screen.getByText("Встреча 1")).toBeInTheDocument();
      expect(screen.getByText("Встреча 2")).toBeInTheDocument();
      expect(screen.getByText("Встреча 3")).toBeInTheDocument();
    });
  });

  it("renders stream info panel with name and team count", async () => {
    renderTeamCard();
    await waitForLoad();
    await waitFor(() => {
      expect(screen.getByText("Поток Альфа")).toBeInTheDocument();
      expect(screen.getByText("7 команд")).toBeInTheDocument();
    });
  });

  it("renders 'Запланировать' button", async () => {
    renderTeamCard();
    await waitForLoad();
    expect(screen.getByRole("button", { name: /запланировать/i })).toBeInTheDocument();
  });

  it("renders NTI market in view mode", async () => {
    renderTeamCard();
    await waitForLoad();
    await waitFor(() =>
      expect(screen.getByDisplayValue("Аэронет")).toBeInTheDocument()
    );
  });

  it("renders TRL level in view mode", async () => {
    renderTeamCard();
    await waitForLoad();
    expect(screen.getByDisplayValue("3-5")).toBeInTheDocument();
  });
});

describe("Meeting status CSS classes", () => {
  it("applies completed class to COMPLETED meetings", async () => {
    renderTeamCard();
    await waitForLoad();
    await waitFor(() => {
      const btn = screen.getByText("Встреча 1").closest("button");
      expect(btn).toHaveClass("team-card_meeting-status-completed");
    });
  });

  it("applies scheduled class to SCHEDULED meetings", async () => {
    renderTeamCard();
    await waitForLoad();
    await waitFor(() => {
      const btn = screen.getByText("Встреча 2").closest("button");
      expect(btn).toHaveClass("team-card_meeting-status-scheduled");
    });
  });

  it("applies not-happened class to NOT_HAPPENED meetings", async () => {
    renderTeamCard();
    await waitForLoad();
    await waitFor(() => {
      const btn = screen.getByText("Встреча 3").closest("button");
      expect(btn).toHaveClass("team-card_meeting-status-not-happened");
    });
  });

  it("applies not-happened class to COMPLETED_AS_NOT_HAPPENED meetings", async () => {
    renderTeamCard({
      fetchOverrides: {
        meetings: [
          {
            id: "mx",
            number: "4",
            startDate: "2024-06-01T10:00:00",
            status: "COMPLETED_AS_NOT_HAPPENED",
          },
        ],
      },
    });
    await waitForLoad();
    await waitFor(() => {
      const btn = screen.getByText("Встреча 4").closest("button");
      expect(btn).toHaveClass("team-card_meeting-status-not-happened");
    });
  });
});

describe("Edit mode", () => {
  it("switches to edit mode and shows 'Сохранить'", async () => {
    renderTeamCard();
    await enterEditMode();
    expect(screen.getByRole("button", { name: /сохранить/i })).toBeInTheDocument();
  });

  it("shows meeting room link field only in edit mode", async () => {
    renderTeamCard();
    await waitForLoad();
    expect(screen.queryByTestId("inputbox-meetingRoomLink")).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: /редактировать/i }));
    expect(screen.getByTestId("inputbox-meetingRoomLink")).toBeInTheDocument();
  });

  it("shows NTI CheckBox in edit mode", async () => {
    renderTeamCard();
    await enterEditMode();
    const containers = screen.getAllByTestId("checkbox-container");
    expect(containers.length).toBeGreaterThan(0);
  });

  it("shows TRL CheckBox title in edit mode", async () => {
    renderTeamCard();
    await enterEditMode();
    const titles = screen.getAllByTestId("checkbox-title");
    const trlTitle = titles.find((el) => el.textContent === "3-5");
    expect(trlTitle).toBeTruthy();
  });

  it("updates name field value on change", async () => {
    renderTeamCard();
    await enterEditMode();
    const nameInput = screen.getByTestId("inputbox-name");
    fireEvent.change(nameInput, {
      target: { name: "name", value: "Новое название" },
    });
    expect(nameInput).toHaveValue("Новое название");
  });

  it("updates description field value on change", async () => {
    renderTeamCard();
    await enterEditMode();
    const descInput = screen.getByTestId("textbox-description");
    fireEvent.change(descInput, {
      target: { name: "description", value: "Обновлённое описание" },
    });
    expect(descInput).toHaveValue("Обновлённое описание");
  });

  it("updates meetingRoomLink field on change", async () => {
    renderTeamCard();
    await enterEditMode();
    const linkInput = screen.getByTestId("inputbox-meetingRoomLink");
    fireEvent.change(linkInput, {
      target: { name: "meetingRoomLink", value: "https://meet.new/room" },
    });
    expect(linkInput).toHaveValue("https://meet.new/room");
  });

  it("activates edit mode immediately when ?edit=true is in URL", async () => {
    renderTeamCard({ search: "?edit=true" });
    await waitForLoad();
    expect(screen.getByRole("button", { name: /сохранить/i })).toBeInTheDocument();
  });
});

describe("ADMIN-specific edit mode", () => {
  it("passes ADMIN role to Header", async () => {
    renderTeamCard({ role: "ADMIN" });
    await waitForLoad();
    expect(screen.getByTestId("header")).toHaveAttribute("data-role", "ADMIN");
  });

  it("shows stream selector in edit mode for ADMIN", async () => {
    renderTeamCard({ role: "ADMIN" });
    await enterEditMode();
    await waitFor(() =>
      expect(screen.getByTestId("stream-field")).toBeInTheDocument()
    );
  });

  it("shows Деактивировать button for ADMIN in edit mode", async () => {
    renderTeamCard({ role: "ADMIN" });
    await enterEditMode();
    expect(screen.getByRole("button", { name: /деактивировать/i })).toBeInTheDocument();
  });

  it("shows Деактивировать button for SUPER_ADMIN in edit mode", async () => {
    renderTeamCard({ role: "SUPER_ADMIN" });
    await enterEditMode();
    expect(screen.getByRole("button", { name: /деактивировать/i })).toBeInTheDocument();
  });

  it("does NOT show Деактивировать button for TRACKER even in edit mode", async () => {
    renderTeamCard({ role: "TRACKER" });
    await enterEditMode();
    expect(
      screen.queryByRole("button", { name: /деактивировать/i })
    ).not.toBeInTheDocument();
  });

  it("fetches and displays only enabled trackers in SelectBox", async () => {
    renderTeamCard({ role: "ADMIN" });
    await waitForLoad();
    const select = screen.getByTestId("selectbox-username");
    expect(within(select).getByText("Иван Иванов")).toBeInTheDocument();
    expect(within(select).getByText("Мария Петрова")).toBeInTheDocument();
    expect(within(select).queryByText("Отключённый Трекер")).not.toBeInTheDocument();
  });

  it("does NOT show stream selector for TRACKER", async () => {
    renderTeamCard({ role: "TRACKER" });
    await enterEditMode();
    expect(screen.queryByTestId("stream-field")).not.toBeInTheDocument();
  });
});

describe("Save (handleSave)", () => {
  it("calls PATCH /api/v1/team-card for TRACKER on save", async () => {
    renderTeamCard({ role: "TRACKER" });
    await enterEditMode();
    fireEvent.click(screen.getByRole("button", { name: /сохранить/i }));
    await waitFor(() => {
      const call = global.fetch.mock.calls.find(
        ([url, opts]) =>
          opts && opts.method === "PATCH" && url.includes("backend.test/api/v1/team-card")
      );
      expect(call).toBeTruthy();
    });
  });

  it("calls PATCH /api/v1/admin/team-card for ADMIN on save", async () => {
    renderTeamCard({ role: "ADMIN" });
    await enterEditMode();
    fireEvent.click(screen.getByRole("button", { name: /сохранить/i }));
    await waitFor(() => {
      const call = global.fetch.mock.calls.find(
        ([url, opts]) =>
          opts &&
          opts.method === "PATCH" &&
          url.includes("backend.test/api/v1/admin/team-card")
      );
      expect(call).toBeTruthy();
    });
  });

  it("sends correct JSON body on save", async () => {
    renderTeamCard({ role: "TRACKER" });
    await enterEditMode();
    fireEvent.click(screen.getByRole("button", { name: /сохранить/i }));
    await waitFor(() => {
      const call = global.fetch.mock.calls.find(
        ([url, opts]) =>
          opts && opts.method === "PATCH" && url.includes("/api/v1/team-card")
      );
      expect(call).toBeTruthy();
      const body = JSON.parse(call[1].body);
      expect(body.name).toBe("Команда Икс");
      expect(body.description).toBe("Описание команды");
    });
  });

  it("returns to view mode after successful save", async () => {
    renderTeamCard({ role: "TRACKER" });
    await enterEditMode();
    fireEvent.click(screen.getByRole("button", { name: /сохранить/i }));
    await waitFor(() =>
      expect(screen.getByRole("button", { name: /редактировать/i })).toBeInTheDocument()
    );
  });

  it("does not send PATCH when required fields are empty", async () => {
    renderTeamCard({
      fetchOverrides: {
        teamCard: {
          ...TEAM_CARD,
          name: "",
          description: "",
          meetingRoomLink: "",
        },
      },
    });
    await waitFor(() =>
      expect(screen.getByTestId("inputbox-name")).toHaveValue("")
    );
    fireEvent.click(screen.getByRole("button", { name: /редактировать/i }));
    fireEvent.click(screen.getByRole("button", { name: /сохранить/i }));
    await act(async () => { });
    const calls = global.fetch.mock.calls.filter(
      ([, opts]) => opts && opts.method === "PATCH"
    );
    expect(calls).toHaveLength(0);
  });
});

// describe("Meeting creation", () => {
//   it("opens MeetingCreate when 'Запланировать' is clicked", async () => {
//     renderTeamCard();
//     await waitForLoad();
//     fireEvent.click(screen.getByRole("button", { name: /запланировать/i }));
//     expect(screen.getByTestId("meeting-create")).toBeInTheDocument();
//   });
//
//   it("passes teamId and role to MeetingCreate", async () => {
//     renderTeamCard({ role: "TRACKER" });
//     await waitForLoad();
//     fireEvent.click(screen.getByRole("button", { name: /запланировать/i }));
//     const mc = screen.getByTestId("meeting-create");
//     expect(mc).toHaveAttribute("data-team-id", "42");
//     expect(mc).toHaveAttribute("data-role", "TRACKER");
//   });
//
//   it("closes MeetingCreate on onClose callback", async () => {
//     renderTeamCard();
//     await waitForLoad();
//     fireEvent.click(screen.getByRole("button", { name: /запланировать/i }));
//     fireEvent.click(screen.getByTestId("meeting-create-close"));
//     await waitFor(() =>
//       expect(screen.queryByTestId("meeting-create")).not.toBeInTheDocument()
//     );
//   });
//
//   it("shows error and does NOT open MeetingCreate when meetings are at max capacity", async () => {
//     const maxMeetings = Array.from({ length: 5 }, (_, i) => ({
//       id: `m${i}`,
//       number: String(i + 1),
//       startDate: "2024-03-01T10:00:00",
//       status: "COMPLETED",
//     }));
//     renderTeamCard({ fetchOverrides: { meetings: maxMeetings } });
//     await waitForLoad();
//     fireEvent.click(screen.getByRole("button", { name: /запланировать/i }));
//     await waitFor(() =>
//       expect(screen.getByTestId("meeting-error")).toBeInTheDocument()
//     );
//     expect(screen.getByTestId("meeting-error")).toHaveTextContent(
//       /максимальное количество встреч/i
//     );
//     expect(screen.queryByTestId("meeting-create")).not.toBeInTheDocument();
//   });
// });

describe("Meeting date editing", () => {
  async function openDateEditor(meetingIndex = 0) {
    await waitForLoad();
    await waitFor(() => expect(screen.getByText("Встреча 1")).toBeInTheDocument());
    const dateBtns = document.querySelectorAll(".team-card_meeting-date");
    fireEvent.click(dateBtns[meetingIndex]);
  }

  it("enters date-editing mode on clicking meeting date button", async () => {
    renderTeamCard();
    await openDateEditor();
    await waitFor(() =>
      expect(document.querySelector(".team-card_meeting-edit-date")).toBeInTheDocument()
    );
  });

  it("renders datetime-local input while editing", async () => {
    renderTeamCard();
    await openDateEditor();
    await waitFor(() => {
      const input = document.querySelector(".team-card_meeting-edit-date");
      expect(input).toHaveAttribute("type", "datetime-local");
    });
  });

  it("cancels editing on 'Отмена' click", async () => {
    renderTeamCard();
    await openDateEditor();
    await waitFor(() => expect(screen.getByText("Отмена")).toBeInTheDocument());
    fireEvent.click(screen.getByText("Отмена"));
    await waitFor(() =>
      expect(document.querySelector(".team-card_meeting-edit-date")).not.toBeInTheDocument()
    );
  });

  it("calls PATCH /api/v1/update-meeting/:id on save", async () => {
    renderTeamCard();
    await openDateEditor();
    await waitFor(() =>
      expect(document.querySelector(".team-card_meeting-edit-button-save")).toBeInTheDocument()
    );
    fireEvent.click(document.querySelector(".team-card_meeting-edit-button-save"));
    await waitFor(() => {
      const call = global.fetch.mock.calls.find(
        ([url, opts]) =>
          opts && opts.method === "PATCH" && url.includes("/api/v1/update-meeting/")
      );
      expect(call).toBeTruthy();
    });
  });

  it("closes editing container after successful date save", async () => {
    renderTeamCard();
    await openDateEditor();
    await waitFor(() =>
      expect(document.querySelector(".team-card_meeting-edit-button-save")).toBeInTheDocument()
    );
    fireEvent.click(document.querySelector(".team-card_meeting-edit-button-save"));
    await waitFor(() =>
      expect(document.querySelector(".team-card_meeting-edit-date")).not.toBeInTheDocument()
    );
  });

  it("shows error and does NOT open editor when validateMeetingDateChange returns invalid", async () => {
    mockValidateMeetingDateChange.mockReturnValue({
      isValid: false,
      errorMessage: "Дата занята другой встречей",
    });
    renderTeamCard();
    await waitForLoad();
    await waitFor(() => expect(screen.getByText("Встреча 1")).toBeInTheDocument());
    const dateBtns = document.querySelectorAll(".team-card_meeting-date");
    fireEvent.click(dateBtns[0]);
    await waitFor(() =>
      expect(screen.getByTestId("meeting-error")).toHaveTextContent(
        "Дата занята другой встречей"
      )
    );
    expect(document.querySelector(".team-card_meeting-edit-date")).not.toBeInTheDocument();
  });

  it("shows error and exits editing when saveMeetingDate validation fails", async () => {
    mockValidateMeetingDateChange
      .mockReturnValueOnce({ isValid: true, errorMessage: "" })
      .mockReturnValueOnce({ isValid: false, errorMessage: "Конфликт дат" });

    renderTeamCard();
    await openDateEditor();
    await waitFor(() =>
      expect(document.querySelector(".team-card_meeting-edit-button-save")).toBeInTheDocument()
    );
    fireEvent.click(document.querySelector(".team-card_meeting-edit-button-save"));
    await waitFor(() =>
      expect(screen.getByTestId("meeting-error")).toHaveTextContent("Конфликт дат")
    );
    expect(document.querySelector(".team-card_meeting-edit-date")).not.toBeInTheDocument();
  });
});

describe("Delete meeting modal", () => {
  async function openDeleteModal() {
    renderTeamCard({ role: "ADMIN" });
    await waitForLoad();
    await waitFor(() => expect(screen.getByText("Встреча 1")).toBeInTheDocument());
    const dateBtns = document.querySelectorAll(".team-card_meeting-date");
    fireEvent.click(dateBtns[0]);
    await waitFor(() =>
      expect(document.querySelector(".team-card_meeting-edit-button-delete")).toBeInTheDocument()
    );
    fireEvent.click(document.querySelector(".team-card_meeting-edit-button-delete"));
    await waitFor(() => expect(screen.getByRole("dialog")).toBeInTheDocument());
  }

  it("opens delete confirmation modal for ADMIN", async () => {
    await openDeleteModal();
    expect(screen.getByText("Подтвердите удаление")).toBeInTheDocument();
  });

  it("closes modal on 'Отмена'", async () => {
    await openDeleteModal();
    const cancelBtn = screen.getByTestId("close-confirm-meeting");
    fireEvent.click(cancelBtn);
    await waitFor(() =>
      expect(screen.queryByRole("dialog")).not.toBeInTheDocument()
    );
  });

  it("closes modal on overlay click", async () => {
    await openDeleteModal();
    fireEvent.click(screen.getByTestId("delete-modal-overlay"));
    await waitFor(() =>
      expect(screen.queryByRole("dialog")).not.toBeInTheDocument()
    );
  });

  it("calls DELETE /api/v1/delete-meeting/:id on confirm", async () => {
    await openDeleteModal();
    const confirmBtn = screen.getByTestId("confirm-delete-meeting");
    fireEvent.click(confirmBtn);
    await waitFor(() => {
      const call = global.fetch.mock.calls.find(
        ([url, opts]) =>
          opts && opts.method === "DELETE" && url.includes("/api/v1/delete-meeting/")
      );
      expect(call).toBeTruthy();
    });
  });

  it("removes the meeting from the list after deletion", async () => {
    await openDeleteModal();
    const confirmBtn = screen.getByTestId("confirm-delete-meeting");
    fireEvent.click(confirmBtn);
    await waitFor(() =>
      expect(screen.queryByText("Встреча 1")).not.toBeInTheDocument()
    );
  });

  it("shows error message when DELETE fails", async () => {
    renderTeamCard({ role: "ADMIN", fetchOverrides: { deleteSuccess: false } });
    await waitForLoad();
    await waitFor(() => expect(screen.getByText("Встреча 1")).toBeInTheDocument());
    const dateBtns = document.querySelectorAll(".team-card_meeting-date");
    fireEvent.click(dateBtns[0]);
    await waitFor(() =>
      expect(document.querySelector(".team-card_meeting-edit-button-delete")).toBeInTheDocument()
    );
    fireEvent.click(document.querySelector(".team-card_meeting-edit-button-delete"));
    await waitFor(() => expect(screen.getByRole("dialog")).toBeInTheDocument());
    fireEvent.click(screen.getByTestId("confirm-delete-meeting"));
    await waitFor(() =>
      expect(screen.getByTestId("meeting-error")).toHaveTextContent(
        /не удалось удалить встречу/i
      )
    );
  });

  it("does NOT render delete button for TRACKER role", async () => {
    renderTeamCard({ role: "TRACKER" });
    await waitForLoad();
    await waitFor(() => expect(screen.getByText("Встреча 1")).toBeInTheDocument());
    const dateBtns = document.querySelectorAll(".team-card_meeting-date");
    fireEvent.click(dateBtns[0]);
    await waitFor(() =>
      expect(document.querySelector(".team-card_meeting-edit-date")).toBeInTheDocument()
    );
    expect(
      document.querySelector(".team-card_meeting-edit-button-delete")
    ).not.toBeInTheDocument();
  });
});

describe("Deactivate team card", () => {
  it("calls DELETE and navigates away after confirmation", async () => {
    renderTeamCard({ role: "ADMIN" });
    await enterEditMode();
    fireEvent.click(screen.getByRole("button", { name: /деактивировать/i }));
    await waitFor(() => {
      const call = global.fetch.mock.calls.find(
        ([url, opts]) =>
          opts &&
          opts.method === "DELETE" &&
          url.includes("backend.test/api/v1/admin/team-card")
      );
      expect(call).toBeTruthy();
    });
    expect(mockNavigate).toHaveBeenCalled();
  });

  it("does NOT send DELETE when confirm dialog is cancelled", async () => {
    window.confirm = jest.fn(() => false);
    renderTeamCard({ role: "ADMIN" });
    await enterEditMode();
    fireEvent.click(screen.getByRole("button", { name: /деактивировать/i }));
    await act(async () => { });
    const deleteCall = global.fetch.mock.calls.find(
      ([url, opts]) =>
        opts &&
        opts.method === "DELETE" &&
        url.includes("backend.test/api/v1/admin/team-card")
    );
    expect(deleteCall).toBeUndefined();
    expect(mockNavigate).not.toHaveBeenCalled();
  });

  it("uses state.from as navigation target if provided", async () => {
    renderTeamCard({ role: "ADMIN", locationState: { from: "/custom-path" } });
    await enterEditMode();
    fireEvent.click(screen.getByRole("button", { name: /деактивировать/i }));
    await waitFor(() =>
      expect(mockNavigate).toHaveBeenCalledWith("/custom-path")
    );
  });
});

describe("Close button navigation", () => {
  it("calls navigate(-1) when close button is clicked", async () => {
    renderTeamCard();
    await waitForLoad();
    const closeBtn = document.querySelector(".team-card_close-button");
    fireEvent.click(closeBtn);
    expect(mockNavigate).toHaveBeenCalledWith(-1);
  });
});

describe("Meeting navigation", () => {
  it("navigates to meeting page on meeting button click", async () => {
    renderTeamCard({ role: "TRACKER" });
    await waitForLoad();
    await waitFor(() => expect(screen.getByText("Встреча 1")).toBeInTheDocument());
    const meetingBtn = screen.getByText("Встреча 1").closest("button");
    fireEvent.click(meetingBtn);
    expect(mockNavigate).toHaveBeenCalledWith(
      expect.stringContaining("/meeting/m1")
    );
  });
});

describe("Stream info display", () => {
  it("renders formatted date range in ru-RU locale", async () => {
    renderTeamCard();
    await waitForLoad();
    await waitFor(() =>
      expect(screen.getByText(/15\.01\.2024/)).toBeInTheDocument()
    );
  });

  it("shows correct team count from API", async () => {
    renderTeamCard({ fetchOverrides: { teamCardsCount: 12 } });
    await waitForLoad();
    await waitFor(() =>
      expect(screen.getByText("12 команд")).toBeInTheDocument()
    );
  });

  it("does not render stream info when team has no streams", async () => {
    renderTeamCard({
      fetchOverrides: { teamCard: { ...TEAM_CARD, streams: [] } },
    });
    await waitFor(() => expect(screen.getByTestId("header")).toBeInTheDocument());
    expect(screen.queryByText("Поток Альфа")).not.toBeInTheDocument();
  });
});

describe("Error handling / edge cases", () => {
  it("renders without crash when team card is not found in list", async () => {
    renderTeamCard({
      fetchOverrides: { teamCard: { ...TEAM_CARD, id: "999" } },
    });
    await waitFor(() =>
      expect(screen.getByTestId("header")).toBeInTheDocument()
    );
  });

  it("renders without crash when meetings fetch fails", async () => {
    global.fetch = jest.fn((url, opts) => {
      if (url.includes("meeting.test/api/v1/meetings")) {
        return Promise.reject(new Error("Network error"));
      }
      return buildFetch()(url, opts);
    });
    mockUseGetUserInfo.mockReturnValue({ roles: ["TRACKER"], username: "tracker1" });
    mockFetchTrackers.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({ content: [] }),
    });
    render(
      <MemoryRouter initialEntries={["/team-cards/42"]}>
        <Routes>
          <Route path="/team-cards/:id" element={<TeamCard />} />
        </Routes>
      </MemoryRouter>
    );
    await waitFor(() => expect(screen.getByTestId("header")).toBeInTheDocument());
    expect(screen.queryByText("Встреча 1")).not.toBeInTheDocument();
  });

  it("renders without crash when nti-markets fetch fails", async () => {
    global.fetch = jest.fn((url, opts) => {
      if (url.includes("/api/v1/streams/nti-markets")) {
        return Promise.reject(new Error("Network error"));
      }
      return buildFetch()(url, opts);
    });
    mockUseGetUserInfo.mockReturnValue({ roles: ["TRACKER"], username: "tracker1" });
    mockFetchTrackers.mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({ content: [] }),
    });
    render(
      <MemoryRouter initialEntries={["/team-cards/42"]}>
        <Routes>
          <Route path="/team-cards/:id" element={<TeamCard />} />
        </Routes>
      </MemoryRouter>
    );
    await waitFor(() => expect(screen.getByTestId("header")).toBeInTheDocument());
  });
});
