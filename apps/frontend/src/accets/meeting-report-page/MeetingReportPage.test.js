import React from "react";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import "@testing-library/jest-dom";
import { MemoryRouter } from "react-router-dom";
import MeetingReportPage from "./MeetingReportPage";
import * as requests from "../../services/requests";
import * as util from "../../services/util";

jest.mock("../../services/requests", () => ({
  fetchMeetingReport: jest.fn(),
  fetchMeetingReportExcel: jest.fn(),
}));

jest.mock("../../services/util", () => ({
  useGetUserInfo: jest.fn(),
}));

jest.mock("../header/header", () => () => <div data-testid="mock-header">Header</div>);

const mockNavigate = jest.fn();
jest.mock("react-router-dom", () => ({
  ...jest.requireActual("react-router-dom"),
  useParams: () => ({ streamId: "123" }),
  useNavigate: () => mockNavigate,
}));

describe("MeetingReportPage Component", () => {
  const mockData = {
    content: [
      {
        teamId: "team-111",
        teamName: "Team Alpha",
        startDate: "2023-10-01T10:00:00Z",
        trackerName: "tracker1",
        trackerFullName: "Иван Иванов",
        status: "COMPLETED",
        teamStatus: "OK",
        tasksNextMeeting: "Сделать А",
        tasksCurrentMeeting: "Сделали Б",
      },
      {
        teamId: "team-222",
        teamName: "Team Beta",
        startDate: "2023-10-02T10:00:00Z",
        trackerName: "tracker2",
        trackerFullName: "Петр Петров",
        status: "SCHEDULED",
        teamStatus: null,
      },
      {
        teamId: "team-333",
        teamName: "Team Gamma",
        startDate: "2023-10-03T10:00:00Z",
        trackerName: "tracker1",
        trackerFullName: "Иван Иванов",
        status: "COMPLETED_AS_NOT_HAPPENED",
        teamStatus: null,
      },
    ],
  };

  beforeAll(() => {
    HTMLAnchorElement.prototype.click = jest.fn();
  });

  beforeEach(() => {
    jest.clearAllMocks();
    util.useGetUserInfo.mockReturnValue({ roles: ["ADMIN"] });
    requests.fetchMeetingReport.mockResolvedValue({
      ok: true,
      json: async () => mockData,
    });

    global.URL.createObjectURL = jest.fn();
    global.URL.revokeObjectURL = jest.fn();
  });

  const renderComponent = () =>
    render(
      <MemoryRouter>
        <MeetingReportPage />
      </MemoryRouter>
    );

  test("рендерит начальное состояние и делает запрос", async () => {
    renderComponent();

    expect(screen.getByText("Загрузка...")).toBeInTheDocument();
    await waitFor(() => {
      expect(requests.fetchMeetingReport).toHaveBeenCalledWith(
        expect.objectContaining({
          streamId: "123",
          filters: [],
          page: 0,
          size: 10000,
          sort: ["teamName,asc", "startDate,desc"],
        })
      );
    });

    await waitFor(() => {
      expect(screen.queryByText("Загрузка...")).not.toBeInTheDocument();
    });
  });

  test("отображает данные в таблице и корректно форматирует статусы", async () => {
    renderComponent();

    await waitFor(() => {
      expect(screen.getByText("Team Alpha")).toBeInTheDocument();
    });

    expect(screen.getByText("Всё ок")).toBeInTheDocument();
    expect(screen.getByText("Запланирована")).toBeInTheDocument();
    expect(screen.getByText("Не состоялась")).toBeInTheDocument();
    expect(screen.getAllByText("Иван Иванов")).toHaveLength(2);
  });

  test("кнопка 'Назад' вызывает navigate(-1)", async () => {
    renderComponent();

    const backButton = screen.getByText("← Назад");
    fireEvent.click(backButton);

    expect(mockNavigate).toHaveBeenCalledWith(-1);
  });

  test("клик по названию команды вызывает navigate с teamId", async () => {
    renderComponent();

    await waitFor(() => {
      expect(screen.getByText("Team Alpha")).toBeInTheDocument();
    });

    const teamLink = screen.getByText("Team Alpha");
    fireEvent.click(teamLink);

    expect(mockNavigate).toHaveBeenCalledWith("/teamcard/team-111");
  });

  test("клик по названию второй команды вызывает navigate с правильным teamId", async () => {
    renderComponent();

    await waitFor(() => {
      expect(screen.getByText("Team Beta")).toBeInTheDocument();
    });

    const teamLink = screen.getByText("Team Beta");
    fireEvent.click(teamLink);

    expect(mockNavigate).toHaveBeenCalledWith("/teamcard/team-222");
  });

  test("работает фильтр по трекеру", async () => {
    renderComponent();

    await waitFor(() => {
      expect(screen.getByText("Team Alpha")).toBeInTheDocument();
    });

    const trackerFilterBtn = screen.getByText("Трекеры");
    fireEvent.click(trackerFilterBtn);

    const trackerOption = screen.getByRole("button", { name: "Иван Иванов (@tracker1)" });
    fireEvent.click(trackerOption);

    await waitFor(() => {
      expect(requests.fetchMeetingReport).toHaveBeenCalledWith(
        expect.objectContaining({
          filters: [{ fieldName: "trackerFullName", type: "EQ", value: "Иван Иванов" }],
        })
      );
    });
  });

  test("работает фильтр по команде", async () => {
    renderComponent();

    await waitFor(() => {
      expect(screen.getByText("Team Alpha")).toBeInTheDocument();
    });

    const teamFilterBtn = screen.getByText("Команда");
    fireEvent.click(teamFilterBtn);

    const teamOption = screen.getByRole("button", { name: "Team Beta" });
    fireEvent.click(teamOption);

    await waitFor(() => {
      expect(requests.fetchMeetingReport).toHaveBeenCalledWith(
        expect.objectContaining({
          filters: [{ fieldName: "teamName", type: "EQ", value: "Team Beta" }],
        })
      );
    });
  });

  test("работает фильтр по статусу", async () => {
    renderComponent();

    await waitFor(() => {
      expect(screen.getByText("Team Alpha")).toBeInTheDocument();
    });

    const statusFilterBtn = screen.getByText("Статус");
    fireEvent.click(statusFilterBtn);

    const okStatusOption = screen.getByRole("button", { name: "Всё ок" });
    fireEvent.click(okStatusOption);

    await waitFor(() => {
      expect(requests.fetchMeetingReport).toHaveBeenCalledWith(
        expect.objectContaining({
          filters: [
            { fieldName: "teamStatus", type: "EQ", value: "OK" },
            { fieldName: "status", type: "EQ", value: "COMPLETED" },
          ],
        })
      );
    });
  });

  test("работает сортировка при клике на заголовок таблицы", async () => {
    renderComponent();

    await waitFor(() => {
      expect(screen.getByText("Team Alpha")).toBeInTheDocument();
    });

    const teamNameHeader = screen.getByText(/Название команды/i);
    fireEvent.click(teamNameHeader);

    await waitFor(() => {
      expect(requests.fetchMeetingReport).toHaveBeenCalledWith(
        expect.objectContaining({
          sort: ["teamName,desc", "startDate,desc"],
        })
      );
    });
  });

  test("функция экспорта вызывает fetchMeetingReportExcel", async () => {
    requests.fetchMeetingReportExcel.mockResolvedValue({
      ok: true,
      blob: async () => new Blob(["test"], { type: "application/vnd.ms-excel" }),
    });

    renderComponent();

    await waitFor(() => {
      expect(screen.getByText("Team Alpha")).toBeInTheDocument();
    });

    const exportBtn = screen.getByText("Выгрузить отчет");
    fireEvent.click(exportBtn);

    await waitFor(() => {
      expect(requests.fetchMeetingReportExcel).toHaveBeenCalledWith(
        expect.objectContaining({
          streamId: "123",
          filters: [],
        })
      );
    });
  });

  test("отображает 'Нет данных', если API возвращает пустой массив", async () => {
    requests.fetchMeetingReport.mockResolvedValueOnce({
      ok: true,
      json: async () => ({ content: [] }),
    });

    renderComponent();

    await waitFor(() => {
      expect(screen.getByText("Нет данных")).toBeInTheDocument();
    });
  });
});