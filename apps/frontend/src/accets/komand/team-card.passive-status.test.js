import React from "react";
import {
  render,
  screen,
  fireEvent,
  waitFor,
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
  passive: false,
  enabled: true,
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

function buildFetch({
  teamCard = TEAM_CARD,
  meetings = MEETINGS,
  ntiMarkets = NTI_MARKETS,
  streamsList = STREAMS_LIST,
  teamCardsCount = 7,
} = {}) {
  return jest.fn((url, options = {}) => {
    const method = (options.method || "GET").toUpperCase();

    if (url.includes("meeting.test/api/v1/meetings")) {
      return ok({ content: meetings, totalPages: 1 });
    }
    if (url.includes("/api/v1/update-meeting/") && method === "PATCH") {
      return ok({ ...meetings[0], startDate: "2024-06-01T10:00:00" });
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
  passive = false,
} = {}) {
  const teamCardWithPassive = { ...TEAM_CARD, passive };

  mockUseGetUserInfo.mockReturnValue({ roles: [role], username: "tracker1" });
  mockFetchTrackers.mockResolvedValue({
    ok: true,
    json: () => Promise.resolve({ content: TRACKERS }),
  });
  global.fetch = buildFetch({ teamCard: teamCardWithPassive });

  return render(
    <MemoryRouter initialEntries={[`/team-cards/${cardId}`]}>
      <Routes>
        <Route path="/team-cards/:id" element={<TeamCard />} />
      </Routes>
    </MemoryRouter>
  );
}

async function waitForLoad() {
  await waitFor(() => {
    expect(screen.getByTestId("inputbox-name")).toHaveValue("Команда Икс");
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

describe("Пассивный статус команды", () => {
  // Тест 1: Чекбокс отображается для ADMIN
  it("отображает чекбокс пассивного статуса для ADMIN в режиме редактирования", async () => {
    renderTeamCard({ role: "ADMIN", passive: false });
    await enterEditMode();

    const passiveLabel = await screen.findByText("Команда в пассиве (неактивна)");
    expect(passiveLabel).toBeInTheDocument();

    const checkbox = passiveLabel.closest('label').querySelector('input[type="checkbox"]');
    expect(checkbox).toBeInTheDocument();
  });

  // Тест 2: Чекбокс НЕ отображается для TRACKER
  it("НЕ отображает чекбокс пассивного статуса для TRACKER в режиме редактирования", async () => {
    renderTeamCard({ role: "TRACKER", passive: false });
    await enterEditMode();

    const passiveLabel = screen.queryByText("Команда в пассиве (неактивна)");
    expect(passiveLabel).not.toBeInTheDocument();
  });

  // Тест 3: Можно включить/выключить чекбокс
  it("позволяет включить и выключить чекбокс пассивного статуса", async () => {
    renderTeamCard({ role: "ADMIN", passive: false });
    await enterEditMode();

    const passiveLabel = await screen.findByText("Команда в пассиве (неактивна)");
    const checkbox = passiveLabel.closest('label').querySelector('input[type="checkbox"]');

    expect(checkbox.checked).toBe(false);

    fireEvent.click(checkbox);
    expect(checkbox.checked).toBe(true);

    fireEvent.click(checkbox);
    expect(checkbox.checked).toBe(false);
  });

  // Тест 4: При сохранении отправляется поле passive
    it("отправляет поле passive на сервер при сохранении", async () => {
      let patchRequest = null;

      // Сохраняем оригинальный fetch
      const originalFetch = global.fetch;

      // Переопределяем fetch для перехвата PATCH запроса
      global.fetch = jest.fn((url, options) => {
        if (options?.method === "PATCH" && url.includes("/api/v1/admin/team-card")) {
          patchRequest = options;
          return Promise.resolve({
            ok: true,
            status: 200,
            json: () => Promise.resolve({ ...TEAM_CARD, passive: true }),
            text: () => Promise.resolve(JSON.stringify({ ...TEAM_CARD, passive: true })),
          });
        }
        // Для остальных запросов используем оригинальную мокированную функцию
        if (url.includes("backend.test/api/v1/admin/team-cards")) {
          return Promise.resolve({
            ok: true,
            json: () => Promise.resolve({ content: [{ ...TEAM_CARD, passive: false }], totalPages: 1 }),
          });
        }
        if (url.includes("meeting.test/api/v1/meetings")) {
          return Promise.resolve({
            ok: true,
            json: () => Promise.resolve({ content: MEETINGS, totalPages: 1 }),
          });
        }
        if (url.includes("/api/v1/streams/nti-markets")) {
          return Promise.resolve({
            ok: true,
            json: () => Promise.resolve(NTI_MARKETS),
          });
        }
        if (url.includes("backend.test/api/v1/streams?")) {
          return Promise.resolve({
            ok: true,
            json: () => Promise.resolve({ content: STREAMS_LIST }),
          });
        }
        if (url.includes("/api/v1/team-card/count")) {
          return Promise.resolve({
            ok: true,
            json: () => Promise.resolve(7),
          });
        }
        if (url.includes("sso.test/api/v1/users/") || url.includes("sso.test/api/v1/account/info")) {
          return Promise.resolve({
            ok: true,
            json: () => Promise.resolve({ fullName: "Test FullName" }),
          });
        }
        return Promise.resolve({ ok: true, json: () => Promise.resolve({}) });
      });

      mockUseGetUserInfo.mockReturnValue({ roles: ["ADMIN"], username: "admin" });
      mockFetchTrackers.mockResolvedValue({
        ok: true,
        json: () => Promise.resolve({ content: TRACKERS }),
      });

      // Рендерим компонент
      render(
        <MemoryRouter initialEntries={["/team-cards/42"]}>
          <Routes>
            <Route path="/team-cards/:id" element={<TeamCard />} />
          </Routes>
        </MemoryRouter>
      );

      // Ждем загрузки данных
      await waitFor(() => {
        expect(screen.getByTestId("inputbox-name")).toHaveValue("Команда Икс");
      });

      // Нажимаем кнопку "Редактировать"
      const editButton = screen.getByRole("button", { name: /редактировать/i });
      fireEvent.click(editButton);

      // Ждем появления чекбокса и нажимаем его
      const passiveLabel = await screen.findByText("Команда в пассиве (неактивна)");
      const checkbox = passiveLabel.closest('label').querySelector('input[type="checkbox"]');
      fireEvent.click(checkbox);
      expect(checkbox.checked).toBe(true);

      // Нажимаем "Сохранить"
      const saveButton = screen.getByRole("button", { name: /сохранить/i });
      fireEvent.click(saveButton);

      // Проверяем, что PATCH запрос был отправлен с правильным телом
      await waitFor(() => {
        expect(patchRequest).not.toBeNull();
        const body = JSON.parse(patchRequest.body);
        expect(body).toHaveProperty("passive", true);
      });

      // Восстанавливаем оригинальный fetch
      global.fetch = originalFetch;
    });

  // Тест 5: Трекер не может создать встречу для пассивной команды
  it("показывает ошибку при попытке трекера создать встречу для пассивной команды", async () => {
    renderTeamCard({ role: "TRACKER", passive: true });
    await waitForLoad();

    const scheduleButton = screen.getByRole("button", { name: /запланировать/i });
    fireEvent.click(scheduleButton);

    await waitFor(() => {
      const errorMessage = screen.queryByText("Нельзя создавать встречи для пассивной команды");
      if (errorMessage) {
        expect(errorMessage).toBeInTheDocument();
      }
    });
  });
});
