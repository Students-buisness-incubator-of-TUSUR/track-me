import React from "react";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import TeamCard from "./team-card-create";
import { createBrowserRouter, RouterProvider } from "react-router-dom";
import "@testing-library/jest-dom";

// Mock react-router-dom
jest.mock("react-router-dom", () => ({
  ...jest.requireActual("react-router-dom"),
  useNavigate: jest.fn(),
}));

// ========== ОПРЕДЕЛЕНИЯ ДЛЯ ТЕСТОВ ==========
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
  streams: [{ id: "stream-1", name: "Поток Альфа", active: true, ntiMarkets: [{ id: 1, displayName: "Аэронет" }] }],
};

const STREAM_WITH_MARKETS = {
  id: "stream-1",
  name: "Поток Альфа",
  active: true,
  ntiMarkets: [{ id: 1, displayName: "Аэронет" }, { id: 2, displayName: "Маринет" }]
};

const TEAM_WITH_MARKETS = {
  ...TEAM_CARD,
  ntiMarketIds: [1],
  streams: [STREAM_WITH_MARKETS],
  stream: STREAM_WITH_MARKETS,
  ntiMarkets: [{ id: 1, displayName: "Аэронет" }]
};

// Общий мок fetch (исправлен: добавлены ntiMarkets для потока)
beforeEach(() => {
  global.fetch = jest.fn((url) => {
    if (url.includes("/account/info")) {
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve({ roles: [], fullName: "Иван Иванов" }),
      });
    }

    if (url.includes("/streams?page=0")) {
      return Promise.resolve({
        ok: true,
        json: () =>
          Promise.resolve({
            content: [{ 
              id: 1, 
              name: "Stream 1", 
              active: true, 
              ntiMarkets: [{ id: 1, displayName: "Market 1" }]
            }],
          }),
      });
    }

    if (url.includes("/streams/nti-markets")) {
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve([
          { id: 1, displayName: "Market 1" },
          { id: 2, displayName: "Market 2" },
          { id: 3, displayName: "Market 3" },
        ]),
      });
    }

    if (url.includes("/users/trackers")) {
      return Promise.resolve({
        ok: true,
        json: () =>
          Promise.resolve({
            content: [
              {
                id: 1,
                fullName: "Трекер A",
                username: "tracker1",
                enabled: true,
              },
            ],
          }),
      });
    }

    if (url.includes("/team-card")) {
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve({ id: 42 }),
      });
    }

    return Promise.resolve({
      ok: true,
      json: () => Promise.resolve({}),
    });
  });
});

// Хелпер для рендера с маршрутизацией
const router = createBrowserRouter(
  [{ path: "/", element: <TeamCard /> }],
  { future: { v7_startTransition: true, v7_relativeSplatPath: true } }
);

const renderComponent = () => render(<RouterProvider router={router} />);

describe("TeamCard — создание карточки команды", () => {
  it("рендерит поля ввода", async () => {
    renderComponent();
    await waitFor(() => {
      expect(screen.getByPlaceholderText(/Введите название команды/i)).toBeInTheDocument();
      expect(screen.getByPlaceholderText(/Введите описание карточки команды/i)).toBeInTheDocument();
    });
  });

  it("по умолчанию отображает имя трекера для не-админа", async () => {
    renderComponent();
    await waitFor(() => {
      expect(screen.getByDisplayValue("Иван Иванов")).toBeInTheDocument();
    });
  });

  it("показывает ошибку, если поля не заполнены", async () => {
    renderComponent();
    const createBtn = await screen.findByText(/Создать/i);
    fireEvent.click(createBtn);
    await waitFor(() => {
      expect(screen.getByText(/Название команды обязательно/i)).toBeInTheDocument();
    });
  });

  it("открывает выпадающий список потоков", async () => {
    renderComponent();
    await waitFor(() => {
      expect(screen.getByText(/Поток/i, { selector: ".create-dropdown-toggle" })).toBeInTheDocument();
    });
    fireEvent.click(screen.getByText(/Поток/i, { selector: ".create-dropdown-toggle" }));
    await waitFor(() => {
      expect(screen.getByText(/Stream 1/i)).toBeInTheDocument();
    });
  });

  it("корректно вводит текст в поля", async () => {
    renderComponent();
    const nameInput = await screen.findByPlaceholderText(/Введите название команды/i);
    fireEvent.change(nameInput, { target: { value: "Моя Команда" } });
    expect(nameInput).toHaveValue("Моя Команда");
  });

  it("открывает и выбирает TRL", async () => {
    renderComponent();
    fireEvent.click(screen.getByText(/TRL/i, { selector: ".create-dropdown-toggle" }));
    const trlOption = await screen.findByText(/3-5/i);
    const trlRadio = trlOption.closest(".create-checkbox-item").querySelector('input[type="radio"]');
    fireEvent.click(trlRadio);
    await waitFor(() => {
      expect(trlRadio).toBeChecked();
    });
  });

  it("фильтрует список трекеров админа при вводе поискового запроса", async () => {
    fetch.mockImplementation((url) => {
      if (url.includes("/account/info")) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({ roles: ["ADMIN"], fullName: "Admin User" }),
        });
      }
      if (url.includes("/users/trackers")) {
        return Promise.resolve({
          ok: true,
          json: () =>
            Promise.resolve({
              content: [
                { id: 1, fullName: "Иван Иванов", username: "ivan", enabled: true },
                { id: 2, fullName: "Мария Петрова", username: "maria", enabled: true },
                { id: 3, fullName: "Отключённый Трекер", username: "disabled", enabled: false },
              ],
            }),
        });
      }
      if (url.includes("/streams?page=0")) {
        return Promise.resolve({
          ok: true,
          json: () =>
            Promise.resolve({
              content: [{ id: 1, name: "Stream 1", active: true, ntiMarkets: [{ id: 1, displayName: "Market 1" }] }],
            }),
        });
      }
      if (url.includes("/streams/nti-markets")) {
        return Promise.resolve({
          ok: true,
          json: () =>
            Promise.resolve([
              { id: 1, displayName: "Market 1" },
              { id: 2, displayName: "Market 2" },
              { id: 3, displayName: "Market 3" },
            ]),
        });
      }
      return Promise.resolve({ ok: true, json: () => Promise.resolve({}) });
    });

    renderComponent();

    await waitFor(() => {
      expect(screen.getByText(/Выберите трекера/i)).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText(/Выберите трекера/i));

    const searchInput = await screen.findByPlaceholderText(/Поиск по ФИО/i);
    fireEvent.click(searchInput);
    expect(searchInput).toHaveFocus();

    fireEvent.change(searchInput, { target: { value: "Мария" } });
    expect(searchInput).toHaveValue("Мария");

    await waitFor(() => {
      expect(screen.getByText(/Мария Петрова/i)).toBeInTheDocument();
      expect(screen.queryByText(/Иван Иванов/i)).not.toBeInTheDocument();
    });
  });

  it("не падает при клике вне дропдауна трекеров", async () => {
    fetch.mockImplementation((url) => {
      if (url.includes("/account/info")) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({ roles: ["ADMIN"], fullName: "Admin User" }),
        });
      }
      if (url.includes("/users/trackers")) {
        return Promise.resolve({
          ok: true,
          json: () =>
            Promise.resolve({
              content: [
                { id: 1, fullName: "Иван Иванов", username: "ivan", enabled: true },
              ],
            }),
        });
      }
      if (url.includes("/streams?page=0")) {
        return Promise.resolve({
          ok: true,
          json: () =>
            Promise.resolve({
              content: [{ id: 1, name: "Stream 1", active: true, ntiMarkets: [{ id: 1, displayName: "Market 1" }] }],
            }),
        });
      }
      if (url.includes("/streams/nti-markets")) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve([{ id: 1, displayName: "Market 1" }]),
        });
      }
      return Promise.resolve({ ok: true, json: () => Promise.resolve({}) });
    });

    renderComponent();

    await waitFor(() => {
      expect(screen.getByText(/Выберите трекера/i)).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText(/Выберите трекера/i));
    await waitFor(() => {
      expect(screen.getByPlaceholderText(/Поиск по ФИО/i)).toBeInTheDocument();
    });

    fireEvent.mouseDown(document.body);

    await waitFor(() => {
      expect(screen.getByPlaceholderText(/Поиск по ФИО/i)).toBeInTheDocument();
    });
  });

  it("открывает и закрывает dropdown при клике вне", async () => {
    renderComponent();
    fireEvent.click(screen.getByText(/TRL/i, { selector: ".create-dropdown-toggle" }));
    await waitFor(() => {
      expect(screen.getByText(/0-2/i)).toBeInTheDocument();
    });
    fireEvent.mouseDown(document.body);
    await waitFor(() => {
      expect(screen.queryByText(/0-2/i)).not.toBeInTheDocument();
    });
  });

  it("отображает список трекеров для админа", async () => {
    fetch.mockImplementation((url) => {
      if (url.includes("/account/info")) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({ roles: ["ADMIN"], fullName: "Admin User" }),
        });
      }
      if (url.includes("/users/trackers")) {
        return Promise.resolve({
          ok: true,
          json: () =>
            Promise.resolve({
              content: [
                {
                  id: 1,
                  fullName: "Трекер A",
                  username: "tracker1",
                  enabled: true,
                },
              ],
            }),
        });
      }
      if (url.includes("/streams?page=0")) {
        return Promise.resolve({
          ok: true,
          json: () =>
            Promise.resolve({
              content: [{ id: 1, name: "Stream 1", active: true, ntiMarkets: [{ id: 1, displayName: "Market 1" }] }],
            }),
        });
      }
      if (url.includes("/streams/nti-markets")) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve([{ id: 1, displayName: "Market 1" }]),
        });
      }
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve({}),
      });
    });

    renderComponent();
    
    await waitFor(() => {
      expect(screen.getByText(/Выберите трекера/i)).toBeInTheDocument();
    });
    
    fireEvent.click(screen.getByText(/Выберите трекера/i));
    
    const trackerOption = await screen.findByText(/Трекер A/i);
    fireEvent.click(trackerOption);
    
    expect(screen.getByText(/Трекер A/i)).toBeInTheDocument();
  });

  it("показывает сообщение при нажатии Запланировать", async () => {
    renderComponent();
    const button = await screen.findByText(/Запланировать/i);
    fireEvent.click(button);
    await waitFor(() => {
      expect(screen.getByText(/Сначала создайте карточку команды/i)).toBeInTheDocument();
    });
  });

  it("показывает ошибку при сбое запроса /account/info", async () => {
    fetch.mockImplementationOnce(() => Promise.reject(new Error("Network error")));
    renderComponent();
    await waitFor(() => {
      expect(screen.getByText(/Ошибка при получении данных пользователя/i)).toBeInTheDocument();
    });
  });

  it("показывает ошибку при сбое загрузки трекеров", async () => {
    fetch.mockImplementation((url) => {
      if (url.includes("/account/info")) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({ roles: ["ADMIN"], fullName: "Admin" }),
        });
      }
      if (url.includes("/users/trackers")) {
        return Promise.reject(new Error("fail"));
      }
      if (url.includes("/streams?page=0")) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({ content: [{ id: 1, name: "Stream 1", active: true, ntiMarkets: [{ id: 1, displayName: "Market 1" }] }] }),
        });
      }
      if (url.includes("/streams/nti-markets")) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve([{ id: 1, displayName: "Market 1" }]),
        });
      }
      return Promise.resolve({ ok: true, json: () => Promise.resolve({}) });
    });

    renderComponent();
    await waitFor(() => {
      expect(screen.getByText(/Ошибка при загрузке трекеров/i)).toBeInTheDocument();
    });
  });

  it("перенаправляет на страницу карточки после создания", async () => {
    const navigate = jest.fn();
    jest.mocked(require("react-router-dom").useNavigate).mockReturnValue(navigate);
    renderComponent();

    fireEvent.change(screen.getByPlaceholderText(/Введите название команды/i), {
      target: { value: "Тестовая команда" },
    });

    fireEvent.change(screen.getByPlaceholderText(/https:\/\/webinar\.tusur\.ru/i), {
      target: { value: "https://test.link" },
    });

    fireEvent.click(screen.getByText(/Рынки НТИ/i, { selector: ".create-dropdown-toggle" }));
    const market1Label = await screen.findByText(/Market 1/i);
    const market1Checkbox = market1Label.closest(".create-checkbox-item").querySelector('input[type="checkbox"]');
    fireEvent.click(market1Checkbox);

    await waitFor(() => {
      expect(screen.getByText(/Market 1/i, { selector: ".create-dropdown-toggle" })).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText(/TRL/i, { selector: ".create-dropdown-toggle" }));
    const trlLabel = await screen.findByText(/3-5/i);
    const trlRadio = trlLabel.closest(".create-checkbox-item").querySelector('input[type="radio"]');
    fireEvent.click(trlRadio);
    await waitFor(() => {
      expect(trlRadio).toBeChecked();
    });

    fireEvent.click(screen.getByText(/Поток/i, { selector: ".create-dropdown-toggle" }));
    const streamLabel = await screen.findByText(/Stream 1/i);
    const streamRadio = streamLabel.closest(".create-checkbox-item").querySelector('input[type="radio"]');
    fireEvent.click(streamRadio);
    await waitFor(() => {
      expect(streamRadio).toBeChecked();
    });

    fireEvent.click(screen.getByText(/Создать/i));

    await waitFor(() => {
      const fetchCall = global.fetch.mock.calls.find((call) => call[0].includes("/team-card"));
      expect(fetchCall).toBeDefined();
      const body = JSON.parse(fetchCall[1].body);
      expect(body.ntiMarketIds).toEqual([1]);
    });

    await waitFor(() => {
      expect(navigate).toHaveBeenCalledWith("/teamcard/42", { state: { streamId: 1 } });
    });
  });
});

describe("TeamCard — валидация формы для админа", () => {
  it("показывает ошибку, если админ не выбрал трекера", async () => {
    fetch.mockImplementation((url) => {
      if (url.includes("/account/info")) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({ roles: ["ADMIN"], fullName: "Admin User" }),
        });
      }
      if (url.includes("/streams?page=0")) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({ content: [{ id: 1, name: "Stream 1", active: true, ntiMarkets: [{ id: 1, displayName: "Market 1" }] }] }),
        });
      }
      if (url.includes("/streams/nti-markets")) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve([{ id: 1, displayName: "Market 1" }]),
        });
      }
      if (url.includes("/users/trackers")) {
        return Promise.resolve({
          ok: true,
          json: () =>
            Promise.resolve({
              content: [
                {
                  id: 1,
                  fullName: "Трекер A",
                  username: "tracker1",
                  enabled: true,
                },
              ],
            }),
        });
      }
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve({}),
      });
    });

    renderComponent();

    await waitFor(() => {
      expect(screen.getByText(/Рынки НТИ/i, { selector: ".create-dropdown-toggle" })).toBeInTheDocument();
    });

    fireEvent.change(screen.getByPlaceholderText(/Введите название команды/i), {
      target: { value: "Тестовая команда" },
    });

    fireEvent.click(screen.getByText(/Рынки НТИ/i, { selector: ".create-dropdown-toggle" }));
    const marketLabel = await screen.findByText(/Market 1/i);
    const marketCheckbox = marketLabel.closest(".create-checkbox-item").querySelector('input[type="checkbox"]');
    fireEvent.click(marketCheckbox);

    fireEvent.click(screen.getByText(/TRL/i, { selector: ".create-dropdown-toggle" }));
    const trlLabel = await screen.findByText(/3-5/i);
    const trlRadio = trlLabel.closest(".create-checkbox-item").querySelector('input[type="radio"]');
    fireEvent.click(trlRadio);

    fireEvent.click(screen.getByText(/Поток/i, { selector: ".create-dropdown-toggle" }));
    const streamLabel = await screen.findByText(/Stream 1/i);
    const streamRadio = streamLabel.closest(".create-checkbox-item").querySelector('input[type="radio"]');
    fireEvent.click(streamRadio);

    fireEvent.click(screen.getByText(/Создать/i));

    await waitFor(() => {
      const errorMessage = screen.getByText((content, element) => {
        return element.classList?.contains('error-message') && 
               content.includes('Выберите трекера');
      });
      expect(errorMessage).toBeInTheDocument();
    });
  });

  it("не показывает ошибку выбора трекера для обычного пользователя", async () => {
    fetch.mockImplementation((url) => {
      if (url.includes("/account/info")) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({ roles: [], fullName: "Обычный пользователь" }),
        });
      }
      if (url.includes("/streams?page=0")) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({ content: [{ id: 1, name: "Stream 1", active: true, ntiMarkets: [{ id: 1, displayName: "Market 1" }] }] }),
        });
      }
      if (url.includes("/streams/nti-markets")) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve([{ id: 1, displayName: "Market 1" }]),
        });
      }
      if (url.includes("/team-card")) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({ id: 42 }),
        });
      }
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve({}),
      });
    });

    renderComponent();

    await waitFor(() => {
      expect(screen.getByText(/Рынки НТИ/i, { selector: ".create-dropdown-toggle" })).toBeInTheDocument();
    });

    fireEvent.change(screen.getByPlaceholderText(/Введите название команды/i), {
      target: { value: "Тестовая команда" },
    });

    fireEvent.click(screen.getByText(/Рынки НТИ/i, { selector: ".create-dropdown-toggle" }));
    const marketLabel = await screen.findByText(/Market 1/i);
    const marketCheckbox = marketLabel.closest(".create-checkbox-item").querySelector('input[type="checkbox"]');
    fireEvent.click(marketCheckbox);

    fireEvent.click(screen.getByText(/TRL/i, { selector: ".create-dropdown-toggle" }));
    const trlLabel = await screen.findByText(/3-5/i);
    const trlRadio = trlLabel.closest(".create-checkbox-item").querySelector('input[type="radio"]');
    fireEvent.click(trlRadio);

    fireEvent.click(screen.getByText(/Поток/i, { selector: ".create-dropdown-toggle" }));
    const streamLabel = await screen.findByText(/Stream 1/i);
    const streamRadio = streamLabel.closest(".create-checkbox-item").querySelector('input[type="radio"]');
    fireEvent.click(streamRadio);

    fireEvent.click(screen.getByText(/Создать/i));

    await waitFor(() => {
      expect(screen.queryByText(/Выберите трекера/i)).not.toBeInTheDocument();
    });
  });
});

describe("TeamCard — выбор рынков НТИ", () => {
  beforeEach(() => {
    fetch.mockImplementation((url) => {
      if (url.includes("/account/info")) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({ roles: [], fullName: "Иван Иванов" }),
        });
      }
      if (url.includes("/streams?page=0")) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({ content: [{ id: 1, name: "Stream 1", active: true, ntiMarkets: [{ id: 1, displayName: "Market 1" }] }] }),
        });
      }
      if (url.includes("/streams/nti-markets")) {
        return Promise.resolve({
          ok: true,
          json: () =>
            Promise.resolve([
              { id: 1, displayName: "Market 1" },
              { id: 2, displayName: "Market 2" },
              { id: 3, displayName: "Market 3" },
            ]),
        });
      }
      if (url.includes("/team-card")) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({ id: 42 }),
        });
      }
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve({}),
      });
    });
  });

  it("отображает выпадающий список рынков НТИ", async () => {
    renderComponent();
    fireEvent.click(screen.getByText(/Рынки НТИ/i, { selector: ".create-dropdown-toggle" }));
    await waitFor(() => {
      expect(screen.getByText(/Market 1/i)).toBeInTheDocument();
      expect(screen.getByText(/Market 2/i)).toBeInTheDocument();
      expect(screen.getByText(/Market 3/i)).toBeInTheDocument();
    });
  });

  it("позволяет выбрать несколько рынков НТИ", async () => {
    renderComponent();
    fireEvent.click(screen.getByText(/Рынки НТИ/i, { selector: ".create-dropdown-toggle" }));

    const market1Label = await screen.findByText(/Market 1/i);
    const market1Checkbox = market1Label.closest(".create-checkbox-item").querySelector('input[type="checkbox"]');
    fireEvent.click(market1Checkbox);

    await waitFor(() => {
      expect(screen.getByText(/Market 1/i, { selector: ".create-dropdown-toggle" })).toBeInTheDocument();
      expect(market1Checkbox).toBeChecked();
    });

    const market2Label = await screen.findByText(/Market 2/i);
    const market2Checkbox = market2Label.closest(".create-checkbox-item").querySelector('input[type="checkbox"]');
    fireEvent.click(market2Checkbox);

    await waitFor(() => {
      expect(screen.getByText(/Market 1, Market 2/i, { selector: ".create-dropdown-toggle" })).toBeInTheDocument();
      expect(market2Checkbox).toBeChecked();
    });
  });

  it("позволяет снять выбор рынка НТИ", async () => {
    renderComponent();
    fireEvent.click(screen.getByText(/Рынки НТИ/i, { selector: ".create-dropdown-toggle" }));

    const market1Label = await screen.findByText(/Market 1/i);
    const market1Checkbox = market1Label.closest(".create-checkbox-item").querySelector('input[type="checkbox"]');
    fireEvent.click(market1Checkbox);

    await waitFor(() => {
      expect(screen.getByText(/Market 1/i, { selector: ".create-dropdown-toggle" })).toBeInTheDocument();
    });

    fireEvent.click(market1Checkbox);

    await waitFor(() => {
      expect(screen.getByText(/Рынки НТИ/i, { selector: ".create-dropdown-toggle" })).toBeInTheDocument();
      expect(market1Checkbox).not.toBeChecked();
    });
  });

  it("отображает '+N' при выборе более 2 рынков", async () => {
    renderComponent();
    fireEvent.click(screen.getByText(/Рынки НТИ/i, { selector: ".create-dropdown-toggle" }));

    const market1Label = await screen.findByText(/Market 1/i);
    const market1Checkbox = market1Label.closest(".create-checkbox-item").querySelector('input[type="checkbox"]');
    fireEvent.click(market1Checkbox);

    const market2Label = await screen.findByText(/Market 2/i);
    const market2Checkbox = market2Label.closest(".create-checkbox-item").querySelector('input[type="checkbox"]');
    fireEvent.click(market2Checkbox);

    const market3Label = await screen.findByText(/Market 3/i);
    const market3Checkbox = market3Label.closest(".create-checkbox-item").querySelector('input[type="checkbox"]');
    fireEvent.click(market3Checkbox);

    await waitFor(() => {
      expect(screen.getByText(/Market 1, Market 2 \+1/i, { selector: ".create-dropdown-toggle" })).toBeInTheDocument();
      expect(market1Checkbox).toBeChecked();
      expect(market2Checkbox).toBeChecked();
      expect(market3Checkbox).toBeChecked();
    });
  });

  it("показывает ошибку, если не выбран ни один рынок НТИ", async () => {
    renderComponent();
    fireEvent.change(screen.getByPlaceholderText(/Введите название команды/i), {
      target: { value: "Тестовая команда" },
    });
    fireEvent.click(screen.getByText(/TRL/i, { selector: ".create-dropdown-toggle" }));
    const trlLabel = await screen.findByText(/3-5/i);
    const trlRadio = trlLabel.closest(".create-checkbox-item").querySelector('input[type="radio"]');
    fireEvent.click(trlRadio);
    fireEvent.click(screen.getByText(/Поток/i, { selector: ".create-dropdown-toggle" }));
    const streamLabel = await screen.findByText(/Stream 1/i);
    const streamRadio = streamLabel.closest(".create-checkbox-item").querySelector('input[type="radio"]');
    fireEvent.click(streamRadio);
    fireEvent.click(screen.getByText(/Создать/i));
    await waitFor(() => {
      expect(screen.getByText(/Выберите хотя бы один рынок НТИ/i)).toBeInTheDocument();
    });
  });

  it("отправляет корректные ntiMarketIds при создания команды", async () => {
    const navigate = jest.fn();
    jest.mocked(require("react-router-dom").useNavigate).mockReturnValue(navigate);
    
    renderComponent();

    fireEvent.change(screen.getByPlaceholderText(/Введите название команды/i), {
      target: { value: "Тестовая команда" },
    });

    fireEvent.change(screen.getByPlaceholderText(/https:\/\/webinar\.tusur\.ru/i), {
      target: { value: "https://test.link" },
    });

    fireEvent.click(screen.getByText(/Рынки НТИ/i, { selector: ".create-dropdown-toggle" }));
    const market1Label = await screen.findByText(/Market 1/i);
    const market1Checkbox = market1Label.closest(".create-checkbox-item").querySelector('input[type="checkbox"]');
    fireEvent.click(market1Checkbox);

    await waitFor(() => {
      expect(screen.getByText(/Market 1/i, { selector: ".create-dropdown-toggle" })).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText(/TRL/i, { selector: ".create-dropdown-toggle" }));
    const trlLabel = await screen.findByText(/3-5/i);
    const trlRadio = trlLabel.closest(".create-checkbox-item").querySelector('input[type="radio"]');
    fireEvent.click(trlRadio);
    await waitFor(() => {
      expect(trlRadio).toBeChecked();
    });

    fireEvent.click(screen.getByText(/Поток/i, { selector: ".create-dropdown-toggle" }));
    const streamLabel = await screen.findByText(/Stream 1/i);
    const streamRadio = streamLabel.closest(".create-checkbox-item").querySelector('input[type="radio"]');
    fireEvent.click(streamRadio);
    await waitFor(() => {
      expect(streamRadio).toBeChecked();
    });

    fireEvent.click(screen.getByText(/Создать/i));

    await waitFor(() => {
      const fetchCall = global.fetch.mock.calls.find((call) => call[0].includes("/team-card"));
      expect(fetchCall).toBeDefined();
      const body = JSON.parse(fetchCall[1].body);
      expect(body.ntiMarketIds).toEqual([1]);
    });

    await waitFor(() => {
      expect(navigate).toHaveBeenCalledWith("/teamcard/42", { state: { streamId: 1 } });
    });
  });
});

describe('TeamCard — покрытие непокрытых строк', () => {
  test('покрывает setError("Нельзя выбрать более 3-х рынков")', async () => {
    const originalFetch = global.fetch;
    global.fetch = jest.fn((url, options) => {
      if (url.includes("/streams/nti-markets")) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve([
            { id: 1, displayName: "Market 1" },
            { id: 2, displayName: "Market 2" },
            { id: 3, displayName: "Market 3" },
            { id: 4, displayName: "Market 4" },
          ]),
        });
      }
      return originalFetch(url);
    });

    renderComponent();
    await waitFor(() => {
      expect(screen.getByPlaceholderText(/Введите название команды/i)).toBeInTheDocument();
    });

    fireEvent.change(screen.getByPlaceholderText(/Введите название команды/i), {
      target: { value: "Тест" },
    });
    fireEvent.change(screen.getByPlaceholderText(/https:\/\/webinar\.tusur\.ru/i), {
      target: { value: "https://test.link" },
    });

    fireEvent.click(screen.getByText(/Рынки НТИ/i, { selector: ".create-dropdown-toggle" }));
    const marketLabels = await screen.findAllByText(/Market \d/);
    for (let i = 0; i < 4 && i < marketLabels.length; i++) {
      const checkbox = marketLabels[i].closest('.create-checkbox-item').querySelector('input[type="checkbox"]');
      if (checkbox && !checkbox.checked) fireEvent.click(checkbox);
    }
    await waitFor(() => {
      expect(screen.getByText(/Нельзя выбрать более 3-х/i)).toBeInTheDocument();
    });
    global.fetch = originalFetch;
  });

  test('покрывает очистку ошибки при выборе подходящего рынка', async () => {
    const originalFetch = global.fetch;
    global.fetch = jest.fn((url, options) => {
      if (url.includes("/streams/nti-markets")) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve([
            { id: 1, displayName: "Market 1" },
            { id: 2, displayName: "Market 2" },
          ]),
        });
      }
      if (url.includes("/streams?page=0")) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({ content: [{ id: 1, name: "Stream 1", active: true, ntiMarkets: [{ id: 1, displayName: "Market 1" }] }] }),
        });
      }
      return originalFetch(url);
    });

    renderComponent();
    await waitFor(() => {
      expect(screen.getByPlaceholderText(/Введите название команды/i)).toBeInTheDocument();
    });

    fireEvent.change(screen.getByPlaceholderText(/Введите название команды/i), {
      target: { value: "Тест" },
    });
    fireEvent.change(screen.getByPlaceholderText(/https:\/\/webinar\.tusur\.ru/i), {
      target: { value: "https://test.link" },
    });

    fireEvent.click(screen.getByText(/Рынки НТИ/i, { selector: ".create-dropdown-toggle" }));
    const market2Label = await screen.findByText(/Market 2/i);
    const market2Checkbox = market2Label.closest('.create-checkbox-item').querySelector('input[type="checkbox"]');
    fireEvent.click(market2Checkbox);

    fireEvent.click(screen.getByText(/Поток/i, { selector: ".create-dropdown-toggle" }));
    const streamLabel = await screen.findByText(/Stream 1/i);
    const streamRadio = streamLabel.closest('.create-checkbox-item').querySelector('input[type="radio"]');
    fireEvent.click(streamRadio);

    await waitFor(() => {
      expect(screen.getByText(/Хотя бы один рынок НТИ/i)).toBeInTheDocument();
    });

    const market1Label = await screen.findByText(/Market 1/i);
    const market1Checkbox = market1Label.closest('.create-checkbox-item').querySelector('input[type="checkbox"]');
    fireEvent.click(market1Checkbox);

    await waitFor(() => {
      expect(screen.queryByText(/Хотя бы один рынок НТИ/i)).not.toBeInTheDocument();
    });

    global.fetch = originalFetch;
  });
});

// ========== ФИНАЛЬНЫЕ ТЕСТЫ ДЛЯ 80% ПОКРЫТИЯ (team-card-create) ==========
describe('Final coverage for team-card-create.js', () => {
  beforeEach(() => {
    global.fetch = jest.fn((url) => {
      if (url.includes('/account/info')) {
        return Promise.resolve({ ok: true, json: () => Promise.resolve({ roles: [], fullName: 'User' }) });
      }
      if (url.includes('/streams?page=0')) {
        return Promise.resolve({ ok: true, json: () => Promise.resolve({ content: [{ id: 1, name: 'Stream 1', active: true }] }) });
      }
      if (url.includes('/streams/nti-markets')) {
        return Promise.resolve({ ok: true, json: () => Promise.resolve([{ id: 1, displayName: 'Market 1' }]) });
      }
      if (url.includes('/users/trackers')) {
        return Promise.resolve({ ok: true, json: () => Promise.resolve({ content: [] }) });
      }
      if (url.includes('/team-card')) {
        return Promise.resolve({ ok: true, json: () => Promise.resolve({ id: 42 }) });
      }
      return Promise.resolve({ ok: true, json: () => Promise.resolve({}) });
    });
  });

  test('covers setError for more than 3 markets', async () => {
    global.fetch = jest.fn((url) => {
      if (url.includes('/streams/nti-markets')) {
        return Promise.resolve({ ok: true, json: () => Promise.resolve([
          { id: 1, displayName: 'Market 1' },
          { id: 2, displayName: 'Market 2' },
          { id: 3, displayName: 'Market 3' },
          { id: 4, displayName: 'Market 4' },
        ]) });
      }
      return Promise.resolve({ ok: true, json: () => Promise.resolve({}) });
    });
    renderComponent();
    await waitFor(() => {
      expect(screen.getByPlaceholderText(/Введите название команды/i)).toBeInTheDocument();
    });
    fireEvent.change(screen.getByPlaceholderText(/Введите название команды/i), { target: { value: 'Team' } });
    fireEvent.change(screen.getByPlaceholderText(/https:\/\/webinar\.tusur\.ru/i), { target: { value: 'https://link.com' } });
    fireEvent.click(screen.getByText(/Рынки НТИ/i, { selector: '.create-dropdown-toggle' }));
    const checkboxes = await screen.findAllByRole('checkbox');
    for (let i = 0; i < 4 && i < checkboxes.length; i++) {
      fireEvent.click(checkboxes[i]);
    }
    await waitFor(() => {
      expect(screen.getByText(/Нельзя выбрать более 3-х/i)).toBeInTheDocument();
    });
  });

  test('covers error clearing when matching market selected', async () => {
    global.fetch = jest.fn((url) => {
      if (url.includes('/streams/nti-markets')) {
        return Promise.resolve({ ok: true, json: () => Promise.resolve([
          { id: 1, displayName: 'Market 1' },
          { id: 2, displayName: 'Market 2' },
        ]) });
      }
      if (url.includes('/streams?page=0')) {
        return Promise.resolve({ ok: true, json: () => Promise.resolve({ content: [{ id: 1, name: 'Stream 1', active: true, ntiMarkets: [{ id: 1, displayName: 'Market 1' }] }] }) });
      }
      return Promise.resolve({ ok: true, json: () => Promise.resolve({}) });
    });
    renderComponent();
    await waitFor(() => {
      expect(screen.getByPlaceholderText(/Введите название команды/i)).toBeInTheDocument();
    });
    fireEvent.change(screen.getByPlaceholderText(/Введите название команды/i), { target: { value: 'Team' } });
    fireEvent.change(screen.getByPlaceholderText(/https:\/\/webinar\.tusur\.ru/i), { target: { value: 'https://link.com' } });
    fireEvent.click(screen.getByText(/Рынки НТИ/i, { selector: '.create-dropdown-toggle' }));
    const market2 = await screen.findByText(/Market 2/i);
    const market2Checkbox = market2.closest('.create-checkbox-item').querySelector('input[type="checkbox"]');
    fireEvent.click(market2Checkbox);
    fireEvent.click(screen.getByText(/Поток/i, { selector: '.create-dropdown-toggle' }));
    const streamRadio = (await screen.findByText(/Stream 1/i)).closest('.create-checkbox-item').querySelector('input[type="radio"]');
    fireEvent.click(streamRadio);
    await waitFor(() => {
      expect(screen.getByText(/Хотя бы один рынок/i)).toBeInTheDocument();
    });
    const market1 = await screen.findByText(/Market 1/i);
    const market1Checkbox = market1.closest('.create-checkbox-item').querySelector('input[type="checkbox"]');
    fireEvent.click(market1Checkbox);
    await waitFor(() => {
      expect(screen.queryByText(/Хотя бы один рынок/i)).not.toBeInTheDocument();
    });
  });

  test('covers streams fetch error', async () => {
    global.fetch = jest.fn((url) => {
      if (url.includes('/streams?page=0')) {
        return Promise.reject(new Error('Streams error'));
      }
      return Promise.resolve({ ok: true, json: () => Promise.resolve({}) });
    });
    renderComponent();
    await waitFor(() => {
      expect(screen.getByText(/Ошибка при загрузке потоков/i)).toBeInTheDocument();
    });
  });

  test('covers nti-markets fetch error', async () => {
    global.fetch = jest.fn((url) => {
      if (url.includes('/streams/nti-markets')) {
        return Promise.reject(new Error('Markets error'));
      }
      if (url.includes('/account/info')) {
        return Promise.resolve({ ok: true, json: () => Promise.resolve({ roles: [], fullName: 'User' }) });
      }
      if (url.includes('/streams?page=0')) {
        return Promise.resolve({ ok: true, json: () => Promise.resolve({ content: [] }) });
      }
      return Promise.resolve({ ok: true, json: () => Promise.resolve({}) });
    });
    renderComponent();
    await waitFor(() => {
      expect(screen.getByText(/Ошибка при загрузке рынков НТИ/i)).toBeInTheDocument();
    });
  });

  test('covers close error message click', async () => {
    renderComponent();
    fireEvent.click(screen.getByRole('button', { name: /создать/i }));
    await waitFor(() => {
      expect(screen.getByText(/Название команды обязательно/i)).toBeInTheDocument();
    });
    const errorButton = screen.getByRole('button', { name: /Название команды обязательно/i });
    fireEvent.click(errorButton);
    await waitFor(() => {
      expect(screen.queryByText(/Название команды обязательно/i)).not.toBeInTheDocument();
    });
  });

  test('covers keyboard events for tracker dropdown', async () => {
    global.fetch = jest.fn((url) => {
      if (url.includes('/account/info')) {
        return Promise.resolve({ ok: true, json: () => Promise.resolve({ roles: ['ADMIN'], fullName: 'Admin' }) });
      }
      if (url.includes('/users/trackers')) {
        return Promise.resolve({ ok: true, json: () => Promise.resolve({ content: [{ id: 1, fullName: 'Tracker A', username: 'tracker', enabled: true }] }) });
      }
      if (url.includes('/streams?page=0')) {
        return Promise.resolve({ ok: true, json: () => Promise.resolve({ content: [{ id: 1, name: 'Stream 1', active: true }] }) });
      }
      if (url.includes('/streams/nti-markets')) {
        return Promise.resolve({ ok: true, json: () => Promise.resolve([{ id: 1, displayName: 'Market 1' }]) });
      }
      return Promise.resolve({ ok: true, json: () => Promise.resolve({}) });
    });
    renderComponent();
    await waitFor(() => {
      expect(screen.getByText(/Выберите трекера/i)).toBeInTheDocument();
    });
    const trackerButton = screen.getByText(/Выберите трекера/i);
    fireEvent.keyDown(trackerButton, { key: 'Enter', code: 'Enter' });
    await waitFor(() => {
      expect(screen.getByPlaceholderText(/Поиск по ФИО/i)).toBeInTheDocument();
    });
    fireEvent.keyDown(trackerButton, { key: ' ', code: 'Space' });
    expect(document.querySelector('.create-card-container')).toBeInTheDocument();
  });
});

// ========== ПОКРЫТИЕ ПОСЛЕДНИХ НЕПОКРЫТЫХ СТРОК ==========
describe('Final missing coverage for create', () => {
  test('covers streams fetch error', async () => {
    global.fetch = jest.fn((url) => {
      if (url.includes('/streams?page=0')) {
        return Promise.reject(new Error('Streams error'));
      }
      if (url.includes('/account/info')) {
        return Promise.resolve({ ok: true, json: () => Promise.resolve({ roles: [], fullName: 'User' }) });
      }
      if (url.includes('/streams/nti-markets')) {
        return Promise.resolve({ ok: true, json: () => Promise.resolve([]) });
      }
      return Promise.resolve({ ok: true, json: () => Promise.resolve({}) });
    });
    renderComponent();
    await waitFor(() => {
      expect(screen.getByText(/Ошибка при загрузке потоков/i)).toBeInTheDocument();
    });
  });

  test('covers nti-markets fetch error', async () => {
    global.fetch = jest.fn((url) => {
      if (url.includes('/streams/nti-markets')) {
        return Promise.reject(new Error('Markets error'));
      }
      if (url.includes('/account/info')) {
        return Promise.resolve({ ok: true, json: () => Promise.resolve({ roles: [], fullName: 'User' }) });
      }
      if (url.includes('/streams?page=0')) {
        return Promise.resolve({ ok: true, json: () => Promise.resolve({ content: [] }) });
      }
      return Promise.resolve({ ok: true, json: () => Promise.resolve({}) });
    });
    renderComponent();
    await waitFor(() => {
      expect(screen.getByText(/Ошибка при загрузке рынков НТИ/i)).toBeInTheDocument();
    });
  });

  test('covers setError("") after creating error and clicking error button', async () => {
    renderComponent();
    const createBtn = await screen.findByText(/Создать/i);
    fireEvent.click(createBtn);
    await waitFor(() => {
      expect(screen.getByText(/Название команды обязательно/i)).toBeInTheDocument();
    });
    const errorButton = screen.getByText(/Название команды обязательно/i);
    fireEvent.click(errorButton);
    await waitFor(() => {
      expect(screen.queryByText(/Название команды обязательно/i)).not.toBeInTheDocument();
    });
  });

  test('covers handleMarketSelect when selecting more than 3 markets', async () => {
    global.fetch = jest.fn((url) => {
      if (url.includes('/streams/nti-markets')) {
        return Promise.resolve({ ok: true, json: () => Promise.resolve([
          { id: 1, displayName: 'Market 1' },
          { id: 2, displayName: 'Market 2' },
          { id: 3, displayName: 'Market 3' },
          { id: 4, displayName: 'Market 4' },
        ]) });
      }
      if (url.includes('/account/info')) {
        return Promise.resolve({ ok: true, json: () => Promise.resolve({ roles: [], fullName: 'User' }) });
      }
      if (url.includes('/streams?page=0')) {
        return Promise.resolve({ ok: true, json: () => Promise.resolve({ content: [] }) });
      }
      return Promise.resolve({ ok: true, json: () => Promise.resolve({}) });
    });
    renderComponent();
    await waitFor(() => {
      expect(screen.getByPlaceholderText(/Введите название команды/i)).toBeInTheDocument();
    });
    fireEvent.click(screen.getByText(/Рынки НТИ/i, { selector: '.create-dropdown-toggle' }));
    const checkboxes = await screen.findAllByRole('checkbox');
    for (let i = 0; i < 4 && i < checkboxes.length; i++) {
      fireEvent.click(checkboxes[i]);
    }
    await waitFor(() => {
      expect(screen.getByText(/Нельзя выбрать более 3-х/i)).toBeInTheDocument();
    });
  });

});
