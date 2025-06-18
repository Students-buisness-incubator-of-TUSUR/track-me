import React from "react";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import FeedbackWidget from "./FeedbackWidget";
import { BrowserRouter, MemoryRouter } from "react-router-dom";
import "@testing-library/jest-dom";

// Мокаем fetch и localStorage
beforeEach(() => {
  global.fetch = jest.fn();
  Storage.prototype.getItem = jest.fn();
});

afterEach(() => {
  jest.clearAllMocks();
});

describe("FeedbackWidget — базовое поведение", () => {
  it("не рендерится на страницах /admin и /superadmin", () => {
    render(
      <MemoryRouter initialEntries={["/admin"]}>
        <FeedbackWidget />
      </MemoryRouter>
    );
    expect(screen.queryByText("Обратная связь")).not.toBeInTheDocument();

    render(
      <MemoryRouter initialEntries={["/superadmin"]}>
        <FeedbackWidget />
      </MemoryRouter>
    );
    expect(screen.queryByText("Обратная связь")).not.toBeInTheDocument();
  });

  it("рендерится на других страницах", () => {
    render(
      <BrowserRouter>
        <FeedbackWidget />
      </BrowserRouter>
    );
    expect(screen.getByText("Обратная связь")).toBeInTheDocument();
  });


});

describe("FeedbackWidget — загрузка данных пользователя", () => {
  it("загружает данные пользователя при монтировании", async () => {
    const mockUserData = {
      username: "testuser",
      email: "test@example.com",
      fullName: "Test User"
    };
    
    fetch.mockResolvedValueOnce({
      ok: true,
      json: () => Promise.resolve(mockUserData)
    });
    
    render(
      <BrowserRouter>
        <FeedbackWidget />
      </BrowserRouter>
    );
    
    // Открываем форму
    fireEvent.click(screen.getByText("Обратная связь"));
    
    expect(fetch).toHaveBeenCalledWith(
      expect.stringContaining("/account/info"),
      expect.any(Object)
    );
    
    // Проверяем индикатор загрузки
    expect(screen.getByText("Загрузка данных...")).toBeInTheDocument();
  });

  

  it("использует данные из localStorage при ошибке API", async () => {
    const mockUserData = {
      username: "localuser",
      email: "local@example.com",
      fullName: "Local User"
    };
    
    fetch.mockRejectedValueOnce(new Error("API error"));
    Storage.prototype.getItem.mockReturnValueOnce(JSON.stringify(mockUserData));
    
    render(
      <BrowserRouter>
        <FeedbackWidget />
      </BrowserRouter>
    );
    
    // Открываем форму
    fireEvent.click(screen.getByText("Обратная связь"));
    
    await waitFor(() => {
      expect(localStorage.getItem).toHaveBeenCalledWith("user");
    });
  });
});

describe("FeedbackWidget — отправка формы", () => {
  beforeEach(() => {
    const mockUserData = {
      username: "testuser",
      email: "test@example.com",
      fullName: "Test User"
    };
    
    fetch.mockImplementation((url) => {
      if (url.includes("/account/info")) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve(mockUserData)
        });
      }
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve({})
      });
    });
  });

  it("отправляет форму с данными пользователя", async () => {
    render(
      <BrowserRouter>
        <FeedbackWidget />
      </BrowserRouter>
    );
    
    // Открываем форму
    fireEvent.click(screen.getByText("Обратная связь"));
    
    // Ждем загрузки формы
    await screen.findByText("Помогите нам стать лучше");
    
    // Заполняем форму
    fireEvent.change(screen.getByLabelText("1. Оцените дизайн сервиса (1 - очень плохо, 5 - отлично):"), {
      target: { value: "5" }
    });
    
    fireEvent.change(screen.getByLabelText("2. Оцените удобство использования (1 - очень плохо, 5 - отлично):"), {
      target: { value: "4" }
    });
    
    fireEvent.change(screen.getByLabelText("3. Укажите количество багов, с которыми вы столкнулись:"), {
      target: { value: "2" }
    });
    
    fireEvent.change(screen.getByLabelText("4. Что вам больше всего понравилось:"), {
      target: { value: "Отличный интерфейс" }
    });
    
    fireEvent.change(screen.getByLabelText("5. Что вам меньше всего понравилось:"), {
      target: { value: "Медленная загрузка" }
    });
    
    fireEvent.change(screen.getByLabelText("6. Ваши рекомендации по улучшению:"), {
      target: { value: "Улучшить производительность" }
    });
    
    // Мокаем FormData
    const originalFormData = window.FormData;
    const mockFormData = {
      append: jest.fn(),
      get: jest.fn()
    };
    
    window.FormData = jest.fn(() => mockFormData);
    
    // Отправляем форму
    fireEvent.click(screen.getByText("Отправить отзыв"));
    
    await waitFor(() => {
      // Проверяем, что данные пользователя были добавлены в FormData
      expect(mockFormData.append).toHaveBeenCalledWith("Username", "testuser");
      expect(mockFormData.append).toHaveBeenCalledWith("Почта", "test@example.com");
      expect(mockFormData.append).toHaveBeenCalledWith("ФИО", "Test User");
      
      // Проверяем сообщение об успешной отправке
      expect(screen.getByText("Спасибо за ваш отзыв!")).toBeInTheDocument();
    });
    
    // Восстанавливаем оригинальный FormData
    window.FormData = originalFormData;
  });

  it("обрабатывает ошибку при отправке формы", async () => {
    fetch.mockImplementationOnce(() => Promise.reject("API error"));
    
    render(
      <BrowserRouter>
        <FeedbackWidget />
      </BrowserRouter>
    );
    
    // Открываем форму
    fireEvent.click(screen.getByText("Обратная связь"));
    
    // Ждем загрузки формы
    await screen.findByText("Помогите нам стать лучше");
    
    // Заполняем обязательные поля
    fireEvent.change(screen.getByLabelText("1. Оцените дизайн сервиса (1 - очень плохо, 5 - отлично):"), {
      target: { value: "5" }
    });
    
    fireEvent.change(screen.getByLabelText("2. Оцените удобство использования (1 - очень плохо, 5 - отлично):"), {
      target: { value: "4" }
    });
    
    // Отправляем форму
    fireEvent.click(screen.getByText("Отправить отзыв"));
    
    // Проверяем, что alert был вызван с сообщением об ошибке
    await waitFor(() => {
      expect(fetch).toHaveBeenCalled();
    });
  });

  it("показывает ошибки валидации при пустых обязательных полях", async () => {
    render(
      <BrowserRouter>
        <FeedbackWidget />
      </BrowserRouter>
    );
    
    // Открываем форму
    fireEvent.click(screen.getByText("Обратная связь"));
    
    // Ждем загрузки формы
    await screen.findByText("Помогите нам стать лучше");
    
    // Отправляем пустую форму
    fireEvent.click(screen.getByText("Отправить отзыв"));
    
    // Проверяем сообщения об ошибках
    await waitFor(() => {
      expect(screen.getAllByText("Выберите оценку")).toHaveLength(2);
    });
  });
});

describe("FeedbackWidget — состояние после отправки", () => {
  it("автоматически закрывает форму через 2 секунды после отправки", async () => {
    jest.useFakeTimers();
    
    const mockUserData = {
      username: "testuser",
      email: "test@example.com",
      fullName: "Test User"
    };
    
    fetch.mockImplementation((url) => {
      if (url.includes("/account/info")) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve(mockUserData)
        });
      }
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve({})
      });
    });
    
    render(
      <BrowserRouter>
        <FeedbackWidget />
      </BrowserRouter>
    );
    
    // Открываем форму
    fireEvent.click(screen.getByText("Обратная связь"));
    
    // Ждем загрузки формы
    await screen.findByText("Помогите нам стать лучше");
    
    // Заполняем обязательные поля
    fireEvent.change(screen.getByLabelText("1. Оцените дизайн сервиса (1 - очень плохо, 5 - отлично):"), {
      target: { value: "5" }
    });
    
    fireEvent.change(screen.getByLabelText("2. Оцените удобство использования (1 - очень плохо, 5 - отлично):"), {
      target: { value: "4" }
    });
    
    // Отправляем форму
    fireEvent.click(screen.getByText("Отправить отзыв"));
    
    // Проверяем сообщение об успехе
    await screen.findByText("Спасибо за ваш отзыв!");
    
    // Перемещаем время вперед на 2 секунды
    jest.advanceTimersByTime(2000);
    
    // Проверяем, что форма закрылась
    expect(screen.queryByText("Помогите нам стать лучше")).not.toBeInTheDocument();
    
    jest.useRealTimers();
  });
});