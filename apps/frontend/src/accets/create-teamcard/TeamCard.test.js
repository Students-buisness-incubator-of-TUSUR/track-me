import React from "react";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import TeamCard from "./team-card-create";
import { BrowserRouter } from "react-router-dom";
import "@testing-library/jest-dom";

// Общий мок fetch — универсальный для всех вызовов
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
        json: () => Promise.resolve({ content: [{ id: 1, name: "Stream 1" }] }),
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
});

// Хелпер для рендера с маршрутизацией
const renderComponent = () =>
  render(
    <BrowserRouter>
      <TeamCard />
    </BrowserRouter>
  );

describe("TeamCard — создание карточки команды", () => {
  it("рендерит поля ввода", async () => {
    renderComponent();
    expect(
      await screen.findByPlaceholderText(/Введите название команды/i)
    ).toBeInTheDocument();
    expect(
      await screen.findByPlaceholderText(/Введите описание карточки команды/i)
    ).toBeInTheDocument();
  });

  it("по умолчанию отображает имя трекера для не-админа", async () => {
    renderComponent();
    expect(
      await screen.findByDisplayValue("Иван Иванов")
    ).toBeInTheDocument();
  });

  it("показывает ошибку, если поля не заполнены", async () => {
    renderComponent();
    const createBtn = await screen.findByText(/Создать/i);
    fireEvent.click(createBtn);
    expect(
      await screen.findByText(/Название команды обязательно/i)
    ).toBeInTheDocument();
  });

  it("открывает выпадающий список потоков", async () => {
    renderComponent();
    const toggle = await screen.findByText(/Поток/i);
    fireEvent.click(toggle);
    expect(await screen.findByText(/Stream 1/i)).toBeInTheDocument();
  });

  it("корректно вводит текст в поля", async () => {
    renderComponent();
    const nameInput = await screen.findByPlaceholderText(
      /Введите название команды/i
    );
    fireEvent.change(nameInput, { target: { value: "Моя Команда" } });
    expect(nameInput.value).toBe("Моя Команда");
  });

  it("открывает и выбирает TRL", async () => {
    renderComponent();
    const toggle = await screen.findByText("TRL");
    fireEvent.click(toggle);
    const option = await screen.findByText("3-5");
    fireEvent.click(option);
    expect(screen.getByText("3-5")).toBeInTheDocument();
  });

  it("открывает и закрывает dropdown при клике вне", async () => {
    renderComponent();
    const toggle = await screen.findByText("TRL");
    fireEvent.click(toggle);
    expect(await screen.findByText("0-2")).toBeInTheDocument();

    fireEvent.mouseDown(document.body);
    await waitFor(() =>
      expect(screen.queryByText("0-2")).not.toBeInTheDocument()
    );
  });

  it("отображает список трекеров для админа", async () => {
    // Заменяем fetch на ADMIN-роль
    fetch.mockImplementation((url) => {
      if (url.includes("/account/info")) {
        return Promise.resolve({
          ok: true,
          json: () =>
            Promise.resolve({ roles: ["ADMIN"], fullName: "Admin User" }),
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
        json: () => Promise.resolve([]),
      });
    });

    renderComponent();
    const trackerInput = await screen.findByPlaceholderText(/Выберите трекера/i);
    fireEvent.click(trackerInput);
    const trackerOption = await screen.findByText(/Трекер A/i);
    fireEvent.click(trackerOption);
    expect(trackerInput.value).toBe("Трекер A");
  });

  
  it("показывает сообщение при нажатии Запланировать", async () => {
    renderComponent();
    const button = await screen.findByText("Запланировать");
    fireEvent.click(button);
    expect(
      await screen.findByText("Сначала создайте карточку команды")
    ).toBeInTheDocument();
  });
});

it("показывает ошибку при сбое запроса /account/info", async () => {
  fetch.mockImplementationOnce(() => Promise.reject("Network error"));

  renderComponent();

  await waitFor(() => {
    expect(screen.getByText("Ошибка при получении данных пользователя")).toBeInTheDocument();
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
      return Promise.reject("fail");
    }

    return Promise.resolve({ ok: true, json: () => Promise.resolve([]) });
  });

  renderComponent();

  await waitFor(() => {
    expect(screen.getByText("Ошибка при загрузке трекеров")).toBeInTheDocument();
  });
});
describe("TeamCard — валидация формы для админа", () => {
  it("показывает ошибку, если админ не выбрал трекера", async () => {
    // Мокаем пользователя с ролью ADMIN и рынки НТИ
    fetch.mockImplementation((url) => {
      if (url.includes("/account/info")) {
        return Promise.resolve({
          ok: true,
          json: () => 
            Promise.resolve({ 
              roles: ["ADMIN"], 
              fullName: "Admin User" 
            }),
        });
      }
      if (url.includes("/streams?page=0")) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({ 
            content: [{ id: 1, name: "Stream 1" }] 
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

    // Ждем загрузки компонента
    await waitFor(() => {
      expect(screen.getByText(/Рынок НТИ/i)).toBeInTheDocument();
    });

    // Заполняем название команды
    fireEvent.change(
      screen.getByPlaceholderText(/Введите название команды/i),
      { target: { value: "Тестовая команда" } }
    );

    // Выбираем рынок НТИ
    fireEvent.click(screen.getByText(/Рынок НТИ/i));
    fireEvent.click(await screen.findByText(/Market 1/i));

    // Выбираем TRL
    fireEvent.click(screen.getByText(/TRL/i));
    fireEvent.click(await screen.findByText(/3-5/i));

    // Выбираем поток (упрощаем проверку, так как проблема с отображением списка)
    // Вместо клика просто устанавливаем значение в formData
    // В реальном приложении это делается через выбор из списка
    // Найти и открыть dropdown с потоками
fireEvent.click(screen.getByText(/Поток/i));

// Найти пункт "Stream 1" и кликнуть по нему
fireEvent.click(await screen.findByText(/Stream 1/i));

// Заполнить другие обязательные поля, например, имя команды
fireEvent.change(screen.getByPlaceholderText(/Введите название команды/i), {
  target: { value: "Тестовая команда" },
});

// ...заполнить или выбрать остальные поля по необходимости...

// Нажать кнопку создания
fireEvent.click(screen.getByText(/Создать/i));


    // Нажимаем кнопку создания
    fireEvent.click(screen.getByText(/Создать/i));

    // Проверяем наличие ошибки
    expect(
      await screen.findByText(/Выберите трекера/i)
    ).toBeInTheDocument();
  });

  it("не показывает ошибку выбора трекера для обычного пользователя", async () => {
    // Мокаем обычного пользователя (без роли ADMIN)
    fetch.mockImplementation((url) => {
      if (url.includes("/account/info")) {
        return Promise.resolve({
          ok: true,
          json: () => 
            Promise.resolve({ 
              roles: [], 
              fullName: "Обычный пользователь" 
            }),
        });
      }
      if (url.includes("/streams?page=0")) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({ 
            content: [{ id: 1, name: "Stream 1" }] 
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

    // Ждем загрузки компонента
    await waitFor(() => {
      expect(screen.getByText(/Рынок НТИ/i)).toBeInTheDocument();
    });

    // Заполняем название команды
    fireEvent.change(
      screen.getByPlaceholderText(/Введите название команды/i),
      { target: { value: "Тестовая команда" } }
    );

    // Выбираем рынок НТИ
    fireEvent.click(screen.getByText(/Рынок НТИ/i));
    fireEvent.click(await screen.findByText(/Market 1/i));

    // Выбираем TRL
    fireEvent.click(screen.getByText(/TRL/i));
    fireEvent.click(await screen.findByText(/3-5/i));

    // Выбираем поток (упрощаем проверку)
    const stream = { id: 1, name: "Stream 1" };
    // Вместо прямого доступа к formData делаем пользовательские действия:

// Открыть dropdown с потоками
fireEvent.click(screen.getByText(/Поток/i));

// Кликнуть на пункт "Stream 1"
fireEvent.click(await screen.findByText(/Stream 1/i));

    // Нажимаем кнопку создания
    fireEvent.click(screen.getByText(/Создать/i));

    // Проверяем, что ошибки выбора трекера нет
    await waitFor(() => {
      expect(screen.queryByText(/Выберите трекера/i)).not.toBeInTheDocument();
    });
  });
});

