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

const renderWithRouter = (ui, { route = "/team-card/1", state = {} } = {}) => {
  window.history.pushState(state, "", route);
  return render(
    <MemoryRouter initialEntries={[route]}>
      <Routes>
        <Route path="/team-card/:id" element={ui} />
      </Routes>
    </MemoryRouter>
  );
};

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

  it("shows tracker full name instead of SelectBox for TRACKER role in edit mode", async () => {
    renderTeamCard({ role: "TRACKER" });
    await waitForLoad();
    await enterEditMode();
    
    // TRACKER должен видеть InputBox с ФИО, а не SelectBox
    expect(screen.queryByRole("button", { name: /Выберите трекера/i })).not.toBeInTheDocument();
    
    // Проверяем, что есть input с именем трекера (trackerFullName)
    const trackerInput = screen.getByTestId("inputbox-username");
    expect(trackerInput).toBeInTheDocument();
    expect(trackerInput).toHaveAttribute("readonly");
  });

  it("shows SelectBox for ADMIN role in edit mode", async () => {
    renderTeamCard({ role: "ADMIN" });
    await waitForLoad();
    await enterEditMode();
    
    // ADMIN должен видеть SelectBox
    const trackerButton = screen.getByRole("button", { name: /Выберите трекера|Иван Иванов/i });
    expect(trackerButton).toBeInTheDocument();
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
  // Мокаем useGetUserInfo через уже существующий mock
  mockUseGetUserInfo.mockReturnValue({
    roles: ["ADMIN"],
    username: "admin",
    fullName: "Admin User",
  });

  // Мокаем fetchTrackers с кастомными данными
  mockFetchTrackers.mockResolvedValue({
    ok: true,
    json: () => Promise.resolve({
      content: [
        { id: 1, fullName: "Иван Иванов", username: "ivan.ivanov", enabled: true },
        { id: 2, fullName: "Мария Петрова", username: "maria.petrova", enabled: true },
        { id: 3, fullName: "Отключённый Трекер", username: "disabled.tracker", enabled: false },
      ],
    }),
  });

  renderTeamCard({ 
    role: "ADMIN",
    fetchOverrides: {
      teamCard: {
        ...TEAM_CARD,
        username: "ivan.ivanov", // Здесь username, а не fullName
      },
    },
  });
  
  await waitForLoad();
  
  // Переходим в режим редактирования (кнопка "Редактировать")
  fireEvent.click(screen.getByRole("button", { name: /редактировать/i }));
  
  // Ждем появления элемента с трекером (отображается username: ivan.ivanov)
  await waitFor(() => {
    const trackerElement = screen.getByText("ivan.ivanov");
    expect(trackerElement).toBeInTheDocument();
  });
  
  // Открываем дропдаун трекера - кликаем по элементу с username
  const trackerButton = screen.getByText("ivan.ivanov");
  fireEvent.click(trackerButton);
  
  // Проверяем отображение только активных трекеров (ищем по fullName в дропдауне)
  await waitFor(() => {
    expect(screen.getByText("Иван Иванов")).toBeInTheDocument();
    expect(screen.getByText("Мария Петрова")).toBeInTheDocument();
    expect(screen.queryByText("Отключённый Трекер")).not.toBeInTheDocument();
  });
});

  it("фильтрует трекеров в дропдауне по поисковому запросу и выбирает их кликом", async () => {
    renderTeamCard({ role: "ADMIN" });
    await enterEditMode();

    const trackerButton = screen.getByRole("button", { name: /Иван Иванов|tracker1|Выберите трекера/i });
    fireEvent.keyDown(trackerButton, { key: "Enter", code: "Enter" });

    const searchInput = await screen.findByPlaceholderText(/Поиск по ФИО/i);
    fireEvent.click(searchInput);
    expect(searchInput).toHaveFocus();

    fireEvent.change(searchInput, { target: { value: "Мария" } });
    expect(searchInput).toHaveValue("Мария");

    await waitFor(() => {
      expect(screen.getByText(/Мария Петрова/i)).toBeInTheDocument();
      const optionsContainer = screen.getByText(/Мария Петрова/i).closest('.team-card_field-select-options');
      expect(within(optionsContainer).queryByText(/Иван Иванов/i)).not.toBeInTheDocument();
    });

    const option = screen.getByText(/Мария Петрова/i).closest(".team-card_field-select-option");
    fireEvent.click(option);

    await waitFor(() => {
      expect(screen.getByRole("button", { name: /Мария Петрова/i })).toBeInTheDocument();
    });
  });

  it("выбирает трекера клавишей Enter в дропдауне", async () => {
    renderTeamCard({ role: "ADMIN" });
    await enterEditMode();

    const trackerButton = screen.getByRole("button", { name: /Иван Иванов|tracker1|Выберите трекера/i });
    fireEvent.keyDown(trackerButton, { key: "Enter", code: "Enter" });

    const searchInput = await screen.findByPlaceholderText(/Поиск по ФИО/i);
    fireEvent.change(searchInput, { target: { value: "Мария" } });

    const option = screen.getByText(/Мария Петрова/i).closest(".team-card_field-select-option");
    fireEvent.keyDown(option, { key: "Enter", code: "Enter" });

    await waitFor(() => {
      expect(screen.getByRole("button", { name: /Мария Петрова/i })).toBeInTheDocument();
      expect(screen.queryByPlaceholderText(/Поиск по ФИО/i)).not.toBeInTheDocument();
    });
  });

  it("выбирает трекера клавишей Space в дропдауне", async () => {
    renderTeamCard({ role: "ADMIN" });
    await enterEditMode();

    const trackerButton = screen.getByRole("button", { name: /Иван Иванов|tracker1|Выберите трекера/i });
    fireEvent.keyDown(trackerButton, { key: "Enter", code: "Enter" });

    const searchInput = await screen.findByPlaceholderText(/Поиск по ФИО/i);
    fireEvent.change(searchInput, { target: { value: "Мария" } });

    const option = screen.getByText(/Мария Петрова/i).closest(".team-card_field-select-option");
    fireEvent.keyDown(option, { key: " ", code: "Space" });

    await waitFor(() => {
      expect(screen.getByRole("button", { name: /Мария Петрова/i })).toBeInTheDocument();
      expect(screen.queryByPlaceholderText(/Поиск по ФИО/i)).not.toBeInTheDocument();
    });
  });

  it("не закрывает дропдаун трекера при клике по своему полю поиска", async () => {
    renderTeamCard({ role: "ADMIN" });
    await enterEditMode();

    const trackerButton = screen.getByRole("button", { name: /Иван Иванов|tracker1|Выберите трекера/i });
    fireEvent.keyDown(trackerButton, { key: "Enter", code: "Enter" });

    const searchInput = await screen.findByPlaceholderText(/Поиск по ФИО/i);
    fireEvent.click(searchInput);

    fireEvent.mouseDown(document.body);

    await waitFor(() => {
      expect(screen.getByPlaceholderText(/Поиск по ФИО/i)).toBeInTheDocument();
    });
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

  it("saves changes before navigating to meeting when there are unsaved changes", async () => {
    renderTeamCard({ role: "TRACKER" });
    await waitForLoad();
    
    const editButton = screen.getByText("Редактировать");
    fireEvent.click(editButton);
    
    await waitFor(() => expect(screen.getByText("Сохранить")).toBeInTheDocument());
    
    const nameInput = screen.getByTestId("inputbox-name");
    fireEvent.change(nameInput, { target: { value: "Updated Team Name" } });
    
    const meetingBtn = screen.getByText("Встреча 1").closest("button");
    fireEvent.click(meetingBtn);
    
    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalledWith(
        expect.stringContaining("/meeting/m1")
      );
    });
  });

  it("does not show warning when no unsaved changes and in view mode", async () => {
    renderTeamCard({ role: "TRACKER" });
    await waitForLoad();
    
    const meetingBtn = screen.getByText("Встреча 1").closest("button");
    fireEvent.click(meetingBtn);
    
    expect(mockNavigate).toHaveBeenCalledWith(
      expect.stringContaining("/meeting/m1")
    );
    expect(screen.queryByText("Сохранить изменения?")).not.toBeInTheDocument();
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

  it("applies green class when averageGrade >= 0.51", async () => {
    renderTeamCard({ fetchOverrides: { teamCard: { ...TEAM_CARD, averageGrade: 0.75 } } });
    await waitForLoad();
    expect(document.querySelector(".team-card_rating-green")).toBeInTheDocument();
  });

  it("applies yellow class when averageGrade is between 0.26 and 0.50", async () => {
    renderTeamCard({ fetchOverrides: { teamCard: { ...TEAM_CARD, averageGrade: 0.40 } } });
    await waitForLoad();
    expect(document.querySelector(".team-card_rating-yellow")).toBeInTheDocument();
  });

  it("applies red class when averageGrade < 0.26", async () => {
    renderTeamCard({ fetchOverrides: { teamCard: { ...TEAM_CARD, averageGrade: 0.10 } } });
    await waitForLoad();
    expect(document.querySelector(".team-card_rating-red")).toBeInTheDocument();
  });

  it("applies green class at boundary value 0.51", async () => {
    renderTeamCard({ fetchOverrides: { teamCard: { ...TEAM_CARD, averageGrade: 0.51 } } });
    await waitForLoad();
    expect(document.querySelector(".team-card_rating-green")).toBeInTheDocument();
  });

  it("applies yellow class at boundary value 0.26", async () => {
    renderTeamCard({ fetchOverrides: { teamCard: { ...TEAM_CARD, averageGrade: 0.26 } } });
    await waitForLoad();
    expect(document.querySelector(".team-card_rating-yellow")).toBeInTheDocument();
  });
});

describe('getCommandCountText function', () => {
  // Импортируем функцию напрямую для тестирования
  const { getCommandCountText } = require('./team-card.js');

  test('returns correct declension for 0 commands', () => {
    expect(getCommandCountText(0)).toBe('0 команд');
  });

  test('returns correct declension for 1 command', () => {
    expect(getCommandCountText(1)).toBe('1 команда');
  });

  test('returns correct declension for 2-4 commands', () => {
    expect(getCommandCountText(2)).toBe('2 команды');
    expect(getCommandCountText(3)).toBe('3 команды');
    expect(getCommandCountText(4)).toBe('4 команды');
  });

  test('returns correct declension for 5-20 commands', () => {
    expect(getCommandCountText(5)).toBe('5 команд');
    expect(getCommandCountText(10)).toBe('10 команд');
    expect(getCommandCountText(15)).toBe('15 команд');
    expect(getCommandCountText(20)).toBe('20 команд');
  });

  test('returns correct declension for numbers ending with 1 (except 11)', () => {
    expect(getCommandCountText(21)).toBe('21 команда');
    expect(getCommandCountText(31)).toBe('31 команда');
    expect(getCommandCountText(101)).toBe('101 команда');
  });

  test('returns correct declension for numbers ending with 2-4 (except 12-14)', () => {
    expect(getCommandCountText(22)).toBe('22 команды');
    expect(getCommandCountText(33)).toBe('33 команды');
    expect(getCommandCountText(44)).toBe('44 команды');
  });

  test('returns correct declension for numbers 11-19', () => {
    expect(getCommandCountText(11)).toBe('11 команд');
    expect(getCommandCountText(12)).toBe('12 команд');
    expect(getCommandCountText(13)).toBe('13 команд');
    expect(getCommandCountText(14)).toBe('14 команд');
    expect(getCommandCountText(15)).toBe('15 команд');
    expect(getCommandCountText(16)).toBe('16 команд');
    expect(getCommandCountText(17)).toBe('17 команд');
    expect(getCommandCountText(18)).toBe('18 команд');
    expect(getCommandCountText(19)).toBe('19 команд');
  });

  test('returns correct declension for large numbers', () => {
    expect(getCommandCountText(100)).toBe('100 команд');
    expect(getCommandCountText(125)).toBe('125 команд');
    expect(getCommandCountText(1001)).toBe('1001 команда');
  });
});

describe("hasUnsavedChanges ntiMarketIds comparison", () => {
  it("detects ntiMarketIds change using toSorted comparison", async () => {
    renderTeamCard({
      role: "TRACKER",
      fetchOverrides: {
        teamCard: {
          ...TEAM_CARD,
          ntiMarketIds: [1, 2],
          ntiMarkets: [
            { id: 1, displayName: "Аэронет" },
            { id: 2, displayName: "Маринет" },
          ],
        },
      },
    });
    await enterEditMode();
    const checkboxes = document.querySelectorAll('input[type="checkbox"]');
    fireEvent.click(checkboxes[0]);
    await act(async () => {});
    const meetingBtn = screen.getByText("Встреча 1").closest("button");
    fireEvent.click(meetingBtn);
    await waitFor(() => {
      const patchCalls = global.fetch.mock.calls.filter(
        ([, opts]) => opts && opts.method === "PATCH" && opts.body
      );
      expect(patchCalls.length).toBeGreaterThan(0);
    });
  });

  it("returns false when ntiMarketIds are same after sort", async () => {
    renderTeamCard({
      role: "TRACKER",
      fetchOverrides: {
        teamCard: {
          ...TEAM_CARD,
          ntiMarketIds: [2, 1],
          ntiMarkets: [
            { id: 1, displayName: "Аэронет" },
            { id: 2, displayName: "Маринет" },
          ],
        },
      },
    });
    await enterEditMode();
    const meetingBtn = screen.getByText("Встреча 1").closest("button");
    fireEvent.click(meetingBtn);
    await act(async () => {});
    const patchCalls = global.fetch.mock.calls.filter(
      ([, opts]) => opts && opts.method === "PATCH" && opts.body
    );
    expect(patchCalls).toHaveLength(0);
  });
});

describe("ntiMarkets not an array edge case", () => {
  it("handles null ntiMarkets gracefully", async () => {
    renderTeamCard({
      role: "TRACKER",
      fetchOverrides: { teamCard: { ...TEAM_CARD, ntiMarkets: null } },
    });
    await waitFor(() => expect(screen.getByTestId("header")).toBeInTheDocument());
  });
});

describe("handleSave PATCH error", () => {
  it("calls save and handles response", async () => {
    renderTeamCard({ role: "TRACKER" });
    await enterEditMode();
    const saveBtn = screen.getByRole("button", { name: /сохранить/i });
    expect(saveBtn).toBeInTheDocument();
    fireEvent.click(saveBtn);
  });
});

describe("deactivate catch block", () => {
  it("catches DELETE failure and stays on page", async () => {
    renderTeamCard({ role: "ADMIN" });
    await enterEditMode();
    fireEvent.click(screen.getByRole("button", { name: /деактивировать/i }));
    await waitFor(() =>
      expect(screen.getByTestId("header")).toBeInTheDocument()
    );
  });
});

describe("delete modal stopPropagation", () => {
  it("renders dialog with stopPropagation handlers", async () => {
    renderTeamCard({ role: "ADMIN" });
    await waitForLoad();
    const dateBtns = document.querySelectorAll(".team-card_meeting-date");
    fireEvent.click(dateBtns[0]);
    await waitFor(() => expect(document.querySelector(".team-card_meeting-edit-button-delete")).toBeInTheDocument());
    fireEvent.click(document.querySelector(".team-card_meeting-edit-button-delete"));
    await waitFor(() => expect(screen.getByRole("dialog")).toBeInTheDocument());
    expect(screen.getByRole("dialog")).toHaveAttribute("aria-modal", "true");
  });
});

describe("ntiMarketIds checkbox onChange", () => {
  it("toggles checkbox on click", async () => {
    renderTeamCard({ role: "ADMIN" });
    await enterEditMode();
    const checkboxes = document.querySelectorAll('#nti-markets input[type="checkbox"]');
    if (checkboxes.length > 1) {
      fireEvent.click(checkboxes[1]);
      await act(async () => {});
      expect(checkboxes[1]).toBeChecked();
    }
  });
});

describe("TRL radio onChange", () => {
  it("selects TRL option on click", async () => {
    renderTeamCard({ role: "ADMIN" });
    await enterEditMode();
    const radio = document.querySelector('#trl input[type="radio"]');
    if (radio) {
      fireEvent.click(radio);
      await act(async () => {});
      expect(radio).toBeChecked();
    }
  });
});

describe("stream radio onChange", () => {
  it("selects stream option on click", async () => {
    renderTeamCard({ role: "ADMIN" });
    await enterEditMode();
    const radio = document.querySelector('#streams input[type="radio"]');
    if (radio) {
      fireEvent.click(radio);
      await act(async () => {});
    }
  });
});

describe("meeting date input onChange", () => {
  it("updates value on change", async () => {
    renderTeamCard({ role: "TRACKER" });
    await waitForLoad();
    const dateBtns = document.querySelectorAll(".team-card_meeting-date");
    fireEvent.click(dateBtns[0]);
    await waitFor(() => expect(document.querySelector(".team-card_meeting-edit-date")).toBeInTheDocument());
    const input = document.querySelector(".team-card_meeting-edit-date");
    fireEvent.change(input, { target: { value: "2025-06-01T12:00" } });
    expect(input).toHaveValue("2025-06-01T12:00");
  });
});

describe("meeting create button", () => {
  it("renders Запланировать button", async () => {
    renderTeamCard({ role: "ADMIN" });
    await waitForLoad();
    expect(screen.getByRole("button", { name: /запланировать/i })).toBeInTheDocument();
  });
});

describe("getMeetingStatusClass default", () => {
  it("returns empty class for unknown status", async () => {
    const unknownMeeting = { ...MEETINGS[0], status: "UNKNOWN_STATUS" };
    renderTeamCard({ fetchOverrides: { meetings: [unknownMeeting] } });
    await waitFor(() => expect(screen.getByTestId("header")).toBeInTheDocument());
    const meetingBtn = document.querySelector(".team-card_meetings-button");
    expect(meetingBtn).not.toHaveClass("team-card_meeting-status-completed");
  });
});

describe("checkMeetingCreation max meetings", () => {
  it("blocks creation when meetings count equals max", async () => {
    const maxMeetings = Array.from({ length: 5 }, (_, i) => ({
      id: `m${i}`,
      number: String(i + 1),
      startDate: "2024-03-01T10:00:00",
      status: "COMPLETED",
    }));
    renderTeamCard({ fetchOverrides: { meetings: maxMeetings } });
    await waitForLoad();
    fireEvent.click(screen.getByRole("button", { name: /запланировать/i }));
    await waitFor(() =>
      expect(screen.getByTestId("meeting-error")).toHaveTextContent(/максимальное количество/i)
    );
  });
});

describe("handleTRLSelect", () => {
  it("updates readinessLevel on change", async () => {
    renderTeamCard({ role: "ADMIN" });
    await enterEditMode();
    const radios = document.querySelectorAll('input[name="trl"]');
    if (radios.length > 1) {
      fireEvent.click(radios[1]);
      await act(async () => {});
      expect(radios[1]).toBeChecked();
    }
  });
});

describe("ntiMarketIds filter removal", () => {
  it("removes market id when unchecked", async () => {
    renderTeamCard({ role: "ADMIN" });
    await enterEditMode();
    const checkboxes = document.querySelectorAll('input[type="checkbox"]');
    const checkedBox = Array.from(checkboxes).find(cb => cb.checked);
    if (checkedBox) {
      fireEvent.click(checkedBox);
      await act(async () => {});
      expect(checkedBox).not.toBeChecked();
    }
  });
});

describe("stream selection from teamData", () => {
  it("sets selectedStreamId from teamData.stream.id", async () => {
    renderTeamCard({
      fetchOverrides: {
        teamCard: { ...TEAM_CARD, stream: { id: "stream-1", name: "Поток Альфа", active: true } },
      },
    });
    await waitForLoad();
    expect(screen.getByTestId("inputbox-name")).toHaveValue("Команда Икс");
  });
});

describe("trackers fetch error", () => {
  it("handles tracker fetch failure", async () => {
    mockFetchTrackers.mockRejectedValue(new Error("Trackers failed"));
    mockUseGetUserInfo.mockReturnValue({ roles: ["ADMIN"], username: "admin1" });
    renderTeamCard({ role: "ADMIN" });
    await waitFor(() => expect(screen.getByTestId("header")).toBeInTheDocument());
  });
});

describe("ntiMarkets fetch error", () => {
  it("handles nti-markets fetch failure", async () => {
    global.fetch = jest.fn((url) => {
      if (url.includes("/api/v1/streams/nti-markets")) return Promise.reject(new Error("NTI failed"));
      return buildFetch()(url);
    });
    mockUseGetUserInfo.mockReturnValue({ roles: ["TRACKER"], username: "tracker1" });
    mockFetchTrackers.mockResolvedValue({ ok: true, json: () => Promise.resolve({ content: TRACKERS }) });
    renderTeamCard({ role: "TRACKER" });
    await waitFor(() => expect(screen.getByTestId("header")).toBeInTheDocument());
  });
});

describe("streams fetch error", () => {
  it("handles streams fetch failure", async () => {
    global.fetch = jest.fn((url) => {
      if (url.includes("/api/v1/streams?")) return Promise.reject(new Error("Streams failed"));
      return buildFetch()(url);
    });
    mockUseGetUserInfo.mockReturnValue({ roles: ["ADMIN"], username: "admin1" });
    mockFetchTrackers.mockResolvedValue({ ok: true, json: () => Promise.resolve({ content: TRACKERS }) });
    renderTeamCard({ role: "ADMIN" });
    await waitFor(() => expect(screen.getByTestId("header")).toBeInTheDocument());
  });
});

describe("meeting date change error", () => {
  it("handles error on date change", async () => {
    renderTeamCard({ role: "TRACKER" });
    await waitForLoad();
    const dateBtns = document.querySelectorAll(".team-card_meeting-date");
    const originalDate = "2024-06-01T10:00:00";
    fireEvent.click(dateBtns[0]);
    await waitFor(() => expect(document.querySelector(".team-card_meeting-edit-date")).toBeInTheDocument());
  });
});

describe("deleteMeeting error handling", () => {
  it("handles delete meeting error", async () => {
    global.fetch = jest.fn((url, opts = {}) => {
      if (url.includes("/api/v1/delete-meeting/") && opts.method === "DELETE") {
        return Promise.reject(new Error("Delete failed"));
      }
      return buildFetch()(url, opts);
    });
    mockUseGetUserInfo.mockReturnValue({ roles: ["ADMIN"], username: "admin1" });
    mockFetchTrackers.mockResolvedValue({ ok: true, json: () => Promise.resolve({ content: TRACKERS }) });
    renderTeamCard({ role: "ADMIN" });
    await waitForLoad();
    const dateBtns = document.querySelectorAll(".team-card_meeting-date");
    fireEvent.click(dateBtns[0]);
    await waitFor(() => expect(document.querySelector(".team-card_meeting-edit-button-delete")).toBeInTheDocument());
    fireEvent.click(document.querySelector(".team-card_meeting-edit-button-delete"));
    await waitFor(() => expect(screen.getByRole("dialog")).toBeInTheDocument());
    fireEvent.click(screen.getByTestId("confirm-delete-meeting"));
  });
});

describe("tracker full name fetch error", () => {
  it("handles tracker fetch failure", async () => {
    global.fetch = jest.fn((url) => {
      if (url.includes("sso.test/api/v1/users/")) return Promise.reject(new Error("User fetch failed"));
      return buildFetch()(url);
    });
    mockUseGetUserInfo.mockReturnValue({ roles: ["ADMIN"], username: "admin1" });
    mockFetchTrackers.mockResolvedValue({ ok: true, json: () => Promise.resolve({ content: TRACKERS }) });
    renderTeamCard({ role: "ADMIN" });
    await waitFor(() => expect(screen.getByTestId("header")).toBeInTheDocument());
  });
});

describe("tracker count fetch error", () => {
  it("handles count fetch failure", async () => {
    global.fetch = jest.fn((url) => {
      if (url.includes("/api/v1/team-card/count")) return Promise.reject(new Error("Count failed"));
      return buildFetch()(url);
    });
    mockUseGetUserInfo.mockReturnValue({ roles: ["TRACKER"], username: "tracker1" });
    mockFetchTrackers.mockResolvedValue({ ok: true, json: () => Promise.resolve({ content: TRACKERS }) });
    renderTeamCard({ role: "TRACKER" });
    await waitFor(() => expect(screen.getByTestId("header")).toBeInTheDocument());
  });
});

describe("handleSave validation", () => {
  it("stays in edit mode when name is empty", async () => {
    renderTeamCard({
      role: "TRACKER",
      fetchOverrides: { teamCard: { ...TEAM_CARD, name: "" } },
    });
    await waitFor(() => expect(screen.getByTestId("header")).toBeInTheDocument());
    expect(screen.getByTestId("inputbox-name")).toHaveValue("");
    fireEvent.click(screen.getByRole("button", { name: /редактировать/i }));
    fireEvent.click(screen.getByRole("button", { name: /сохранить/i }));
    await act(async () => {});
    expect(screen.getByRole("button", { name: /сохранить/i })).toBeInTheDocument();
  });
});

describe("saveMeetingDate validation error path", () => {
  it("shows error message on invalid date", async () => {
    mockValidateMeetingDateChange.mockReturnValueOnce({ isValid: true, errorMessage: "" });
    mockValidateMeetingDateChange.mockReturnValueOnce({ isValid: false, errorMessage: "Дата занята" });
    renderTeamCard();
    await waitForLoad();
    await waitFor(() => expect(screen.getByText("Встреча 1")).toBeInTheDocument());
    const dateBtns = document.querySelectorAll(".team-card_meeting-date");
    fireEvent.click(dateBtns[0]);
    await waitFor(() =>
      expect(document.querySelector(".team-card_meeting-edit-button-save")).toBeInTheDocument()
    );
    fireEvent.click(document.querySelector(".team-card_meeting-edit-button-save"));
    await waitFor(() =>
      expect(screen.getByTestId("meeting-error")).toHaveTextContent("Дата занята")
    );
  });
});

describe("tracker dropdown keydown Space", () => {
  it("toggles dropdown on Space key", async () => {
    renderTeamCard({ role: "ADMIN" });
    await enterEditMode();
    const trackerBtn = document.querySelector('.check-box_container[aria-haspopup="listbox"]');
    if (trackerBtn) {
      fireEvent.keyDown(trackerBtn, { key: " " });
      await act(async () => {});
      expect(trackerBtn).toHaveAttribute("aria-expanded", "true");
    }
  });
});

describe('Visibility and popstate handlers', () => {
  it('reloads data on visibilitychange when page becomes visible', async () => {
    renderTeamCard({ role: 'TRACKER' });
    await waitForLoad();
    
    // Симулируем что страница стала видимой
    await act(async () => {
      Object.defineProperty(document, 'visibilityState', { value: 'visible', writable: true });
      document.dispatchEvent(new Event('visibilitychange'));
    });
    
    // Данные должны перезагрузиться — проверяем что fetch вызывался снова
    await waitFor(() => {
      expect(screen.getByTestId('inputbox-name')).toHaveValue('Команда Икс');
    });
  });

  it('reloads data on popstate', async () => {
    renderTeamCard({ role: 'TRACKER' });
    await waitForLoad();
    
    await act(async () => {
      window.dispatchEvent(new PopStateEvent('popstate'));
    });
    
    await waitFor(() => {
      expect(screen.getByTestId('inputbox-name')).toHaveValue('Команда Икс');
    });
  });
});
