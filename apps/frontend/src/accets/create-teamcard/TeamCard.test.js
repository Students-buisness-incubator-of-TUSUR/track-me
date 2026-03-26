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

// Общий мок fetch
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
            content: [{ id: 1, name: "Stream 1", active: true }],
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
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve({}),
      });
    });

    renderComponent();
    await waitFor(() => {
      expect(screen.getByPlaceholderText(/Выберите трекера/i)).toBeInTheDocument();
    });
    fireEvent.click(screen.getByPlaceholderText(/Выберите трекера/i));
    const trackerOption = await screen.findByText(/Трекер A/i);
    fireEvent.click(trackerOption);
    expect(screen.getByDisplayValue(/Трекер A/i)).toBeInTheDocument();
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

    // Заполняем название
    fireEvent.change(screen.getByPlaceholderText(/Введите название команды/i), {
      target: { value: "Тестовая команда" },
    });

    // Заполняем ссылку на комнату
        fireEvent.change(screen.getByPlaceholderText(/https:\/\/webinar\.tusur\.ru/i), {
          target: { value: "https://test.link" },
    });

    // Выбираем рынок
    fireEvent.click(screen.getByText(/Рынки НТИ/i, { selector: ".create-dropdown-toggle" }));
    const market1Label = await screen.findByText(/Market 1/i);
    const market1Checkbox = market1Label.closest(".create-checkbox-item").querySelector('input[type="checkbox"]');
    fireEvent.click(market1Checkbox);

    // Выбираем TRL
    fireEvent.click(screen.getByText(/TRL/i, { selector: ".create-dropdown-toggle" }));
    const trlLabel = await screen.findByText(/3-5/i);
    const trlRadio = trlLabel.closest(".create-checkbox-item").querySelector('input[type="radio"]');
    fireEvent.click(trlRadio);

    // Выбираем поток
    fireEvent.click(screen.getByText(/Поток/i, { selector: ".create-dropdown-toggle" }));
    const streamLabel = await screen.findByText(/Stream 1/i);
    const streamRadio = streamLabel.closest(".create-checkbox-item").querySelector('input[type="radio"]');
    fireEvent.click(streamRadio);

    fireEvent.click(screen.getByText(/Создать/i));

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
          json: () => Promise.resolve({ content: [{ id: 1, name: "Stream 1", active: true }] }),
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
      expect(screen.getByText(/Выберите трекера/i)).toBeInTheDocument();
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
          json: () => Promise.resolve({ content: [{ id: 1, name: "Stream 1", active: true }] }),
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
          json: () => Promise.resolve({ content: [{ id: 1, name: "Stream 1", active: true }] }),
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

    const market3Label = await screen.findByText(/Market 3/i);
    const market3Checkbox = market3Label.closest(".create-checkbox-item").querySelector('input[type="checkbox"]');
    fireEvent.click(market3Checkbox);

    await waitFor(() => {
      expect(screen.getByText(/Market 1, Market 3/i, { selector: ".create-dropdown-toggle" })).toBeInTheDocument();

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
      expect(body.ntiMarketIds).toEqual([1, 3]);
    }, { timeout: 2000 });
  });
});