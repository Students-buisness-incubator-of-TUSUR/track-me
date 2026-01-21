// CustomDateTimePicker.test.jsx
import React from 'react';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import '@testing-library/jest-dom';
import CustomDateTimePicker from './CustomDateTimePicker';

// Mock для Date, чтобы тесты были стабильными
const mockToday = new Date('2024-01-15T12:00:00');
const realDate = Date;

describe('CustomDateTimePicker', () => {
  beforeAll(() => {
    global.Date = class extends realDate {
      constructor(...args) {
        if (args.length === 0) {
          return new realDate(mockToday);
        }
        return new realDate(...args);
      }
      
      static now() {
        return mockToday.getTime();
      }
    };
    
    // Mock для toLocaleDateString
    global.Date.prototype.toLocaleDateString = jest.fn((locale, options) => {
      if (options && options.month === 'long' && options.year === 'numeric') {
        return 'январь 2024 г.';
      }
      if (options && options.day === 'numeric' && options.month === 'long' && options.year === 'numeric') {
        return '15 января 2024 г.';
      }
      return '15.01.2024';
    });
  });

  afterAll(() => {
    global.Date = realDate;
    global.Date.prototype.toLocaleDateString = realDate.prototype.toLocaleDateString;
  });

  beforeEach(() => {
    jest.clearAllMocks();
    // Сбросить все таймеры
    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.runOnlyPendingTimers();
    jest.useRealTimers();
  });

  // Тесты базового рендеринга
  describe('Рендеринг компонента', () => {
    test('рендерится без ошибок', () => {
      render(<CustomDateTimePicker />);
      expect(screen.getByText('Выберите дату')).toBeInTheDocument();
    });

    test('рендерится с переданным значением', () => {
      render(<CustomDateTimePicker value="2024-01-15T12:00" />);
      expect(screen.getByText('15 января 2024 г.')).toBeInTheDocument();
    });

    test('рендерится в disabled состоянии', () => {
      render(<CustomDateTimePicker disabled={true} />);
      const displayElement = screen.getByRole('button');
      expect(displayElement).toHaveClass('disabled');
      expect(displayElement).toHaveAttribute('tabindex', '-1');
    });

    test('имеет правильные ARIA атрибуты', () => {
      render(<CustomDateTimePicker />);
      const displayElement = screen.getByRole('button');
      expect(displayElement).toHaveAttribute('aria-haspopup', 'dialog');
      expect(displayElement).toHaveAttribute('aria-expanded', 'false');
    });
  });

  // Тесты открытия/закрытия пикера
  describe('Интерактивность пикера', () => {
   

    test('открывается по нажатию Enter', async () => {
      render(<CustomDateTimePicker />);
      const displayElement = screen.getByRole('button');
      
      await act(async () => {
        fireEvent.keyDown(displayElement, { key: 'Enter' });
      });
      
      expect(screen.getByLabelText('Закрыть')).toBeInTheDocument();
      expect(displayElement).toHaveAttribute('aria-expanded', 'true');
    });

    test('открывается по нажатию пробела', async () => {
      render(<CustomDateTimePicker />);
      const displayElement = screen.getByRole('button');
      
      await act(async () => {
        fireEvent.keyDown(displayElement, { key: ' ' });
      });
      
      expect(screen.getByLabelText('Закрыть')).toBeInTheDocument();
      expect(displayElement).toHaveAttribute('aria-expanded', 'true');
    });

    test('не открывается при disabled состоянии', () => {
      render(<CustomDateTimePicker disabled={true} />);
      const displayElement = screen.getByRole('button');
      
      fireEvent.click(displayElement);
      
      expect(screen.queryByLabelText('Закрыть')).not.toBeInTheDocument();
    });

    test('закрывается по клику на кнопку закрытия', async () => {
      render(<CustomDateTimePicker value="2024-01-15T12:00" />);
      const displayElement = screen.getByRole('button');
      
      // Открываем пикер
      await act(async () => {
        fireEvent.click(displayElement);
      });
      
      expect(screen.getByLabelText('Закрыть')).toBeInTheDocument();
      
      // Закрываем через кнопку
      const closeButton = screen.getByLabelText('Закрыть');
      await act(async () => {
        fireEvent.click(closeButton);
      });
      
      await waitFor(() => {
        expect(screen.queryByLabelText('Закрыть')).not.toBeInTheDocument();
      });
    });

    test('закрывается по клику вне компонента', async () => {
      render(
        <div>
          <CustomDateTimePicker value="2024-01-15T12:00" />
          <div data-testid="outside">Внешний элемент</div>
        </div>
      );
      
      const displayElement = screen.getByRole('button');
      await act(async () => {
        fireEvent.click(displayElement);
      });
      
      expect(screen.getByLabelText('Закрыть')).toBeInTheDocument();
      
      const outsideElement = screen.getByTestId('outside');
      await act(async () => {
        fireEvent.mouseDown(outsideElement);
      });
      
      await waitFor(() => {
        expect(screen.queryByLabelText('Закрыть')).not.toBeInTheDocument();
      });
    });
  });

  // Тесты выбора даты
  describe('Выбор даты', () => {
    const mockOnChange = jest.fn();

    beforeEach(() => {
      mockOnChange.mockClear();
    });

    test('выбирает дату по клику', async () => {
      render(
        <CustomDateTimePicker 
          value="2024-01-15T12:00" 
          onChange={mockOnChange}
        />
      );
      
      // Открываем пикер
      const displayElement = screen.getByRole('button');
      await act(async () => {
        fireEvent.click(displayElement);
      });
      
      // Даем время для рендеринга дней
      await waitFor(() => {
        expect(screen.getByText('16')).toBeInTheDocument();
      });
      
      // Находим и кликаем на другую дату (например, 16 число)
      const date16 = screen.getByText('16');
      await act(async () => {
        fireEvent.click(date16);
      });
      
      jest.advanceTimersByTime(300); // Продвигаем таймер
      
      expect(mockOnChange).toHaveBeenCalledWith('2024-01-16T12:00');
    });

    test('выбирает дату по нажатию Enter', async () => {
      render(
        <CustomDateTimePicker 
          value="2024-01-15T12:00" 
          onChange={mockOnChange}
        />
      );
      
      // Открываем пикер
      const displayElement = screen.getByRole('button');
      await act(async () => {
        fireEvent.click(displayElement);
      });
      
      // Ждем рендеринг дней
      await waitFor(() => {
        expect(screen.getByText('16')).toBeInTheDocument();
      });
      
      // Находим день
      const date16 = screen.getByText('16');
      await act(async () => {
        fireEvent.keyDown(date16, { key: 'Enter' });
      });
      
      jest.advanceTimersByTime(300);
      
      expect(mockOnChange).toHaveBeenCalledWith('2024-01-16T12:00');
    });

    

    
    test('не позволяет выбрать недоступную дату', async () => {
      const minDate = '2024-01-10';
      const maxDate = '2024-01-20';
      
      render(
        <CustomDateTimePicker 
          value="2024-01-15T12:00"
          min={`${minDate}T00:00`}
          max={`${maxDate}T23:59`}
          onChange={mockOnChange}
        />
      );
      
      // Открываем пикер
      const displayElement = screen.getByRole('button');
      await act(async () => {
        fireEvent.click(displayElement);
      });
      
      // Ждем рендеринг
      await waitFor(() => {
        expect(screen.getByText('5')).toBeInTheDocument();
      });
      
      // Пытаемся кликнуть на недоступную дату (например, 5 число, которое до min)
      const date5 = screen.getByText('5');
      const dayCell = date5.closest('.calendar-day');
      expect(dayCell).toHaveClass('disabled');
      
      await act(async () => {
        fireEvent.click(date5);
      });
      
      expect(mockOnChange).not.toHaveBeenCalled();
    });
  });

  // Тесты навигации по месяцам
  describe('Навигация по месяцам', () => {
    test('переходит на следующий месяц при наличии выбранной даты', async () => {
      render(<CustomDateTimePicker value="2024-01-15T12:00" />);
      
      // Открываем пикер
      const displayElement = screen.getByRole('button');
      await act(async () => {
        fireEvent.click(displayElement);
      });
      
      await waitFor(() => {
        expect(screen.getByText('1')).toBeInTheDocument();
      });
      
      // Кликаем "следующий месяц"
      const nextButton = screen.getByLabelText('Следующий месяц');
      await act(async () => {
        fireEvent.click(nextButton);
      });
      
      // Проверяем что дни февраля появились
      await waitFor(() => {
        const dayElements = screen.getAllByRole('button').filter(btn => 
          btn.textContent && /^\d+$/.test(btn.textContent.trim())
        );
        expect(dayElements.length).toBeGreaterThan(0);
      });
    });

    test('переходит на предыдущий месяц при наличии выбранной даты', async () => {
      render(<CustomDateTimePicker value="2024-01-15T12:00" />);
      
      // Открываем пикер
      const displayElement = screen.getByRole('button');
      await act(async () => {
        fireEvent.click(displayElement);
      });
      
      await waitFor(() => {
        expect(screen.getByText('1')).toBeInTheDocument();
      });
      
      // Кликаем "предыдущий месяц"
      const prevButton = screen.getByLabelText('Предыдущий месяц');
      await act(async () => {
        fireEvent.click(prevButton);
      });
      
      // Проверяем что дни декабря появились
      await waitFor(() => {
        const dayElements = screen.getAllByRole('button').filter(btn => 
          btn.textContent && /^\d+$/.test(btn.textContent.trim())
        );
        expect(dayElements.length).toBeGreaterThan(0);
      });
    });
  });

  // Тесты вспомогательных функций
  describe('Вспомогательные функции', () => {
    test('правильно форматирует дату для отображения', () => {
      render(<CustomDateTimePicker value="2024-01-15T12:00" />);
      expect(screen.getByText('15 января 2024 г.')).toBeInTheDocument();
    });

    test('показывает "Выберите дату" при отсутствии значения', () => {
      render(<CustomDateTimePicker />);
      expect(screen.getByText('Выберите дату')).toBeInTheDocument();
    });

    test('отображает заголовки дней недели на русском', async () => {
      render(<CustomDateTimePicker value="2024-01-15T12:00" />);
      
      const displayElement = screen.getByRole('button');
      await act(async () => {
        fireEvent.click(displayElement);
      });
      
      const weekdays = ['Пн', 'Вт', 'Ср', 'Чт', 'Пт', 'Сб', 'Вс'];
      await waitFor(() => {
        weekdays.forEach(day => {
          expect(screen.getByText(day)).toBeInTheDocument();
        });
      });
    });

   

    test('отмечает выбранную дату', async () => {
      render(<CustomDateTimePicker value="2024-01-15T12:00" />);
      
      const displayElement = screen.getByRole('button');
      await act(async () => {
        fireEvent.click(displayElement);
      });
      
      await waitFor(() => {
        // 15 число должно быть отмечено как выбранное
        const selectedCell = screen.getByText('15').closest('.calendar-day');
        expect(selectedCell).toHaveClass('selected');
      });
    });
  });

  // Тесты эффектов и хуков
  describe('Эффекты и обновления', () => {
    test('обновляет selectedDate при изменении value', () => {
      const { rerender } = render(
        <CustomDateTimePicker value="2024-01-15T12:00" />
      );
      
      expect(screen.getByText('15 января 2024 г.')).toBeInTheDocument();
      
      rerender(<CustomDateTimePicker value="2024-02-20T14:30" />);
      
      expect(screen.getByText('20 февраля 2024 г.')).toBeInTheDocument();
    });

    test('не выбрасывает ошибку при некорректном значении value', () => {
      const consoleSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
      
      render(<CustomDateTimePicker value="invalid-date" />);
      
      // Проверяем что компонент не падает
      expect(screen.getByRole('button')).toBeInTheDocument();
      
      consoleSpy.mockRestore();
    });
  });

  // Тесты доступности
  describe('Доступность (Accessibility)', () => {
    test('имеет правильные ARIA атрибуты для дней', async () => {
      render(<CustomDateTimePicker value="2024-01-15T12:00" />);
      
      const displayElement = screen.getByRole('button');
      await act(async () => {
        fireEvent.click(displayElement);
      });
      
      await waitFor(() => {
        // Проверяем ARIA атрибуты для дня
        const day15 = screen.getByText('15').closest('[role="button"]');
        expect(day15).toHaveAttribute('aria-label');
        expect(day15).toHaveAttribute('aria-disabled', 'false');
      });
    });

    test('дни с ограничениями имеют aria-disabled="true"', async () => {
      render(
        <CustomDateTimePicker 
          value="2024-01-15T12:00"
          min="2024-01-10T00:00"
          max="2024-01-20T23:59"
        />
      );
      
      const displayElement = screen.getByRole('button');
      await act(async () => {
        fireEvent.click(displayElement);
      });
      
      await waitFor(() => {
        // День до min должен быть disabled
        const day5 = screen.getByText('5').closest('[role="button"]');
        expect(day5).toHaveAttribute('aria-disabled', 'true');
      });
    });

    test('disabled дни не могут получить фокус', async () => {
      render(
        <CustomDateTimePicker 
          value="2024-01-15T12:00"
          min="2024-01-10T00:00"
        />
      );
      
      const displayElement = screen.getByRole('button');
      await act(async () => {
        fireEvent.click(displayElement);
      });
      
      await waitFor(() => {
        // День до min должен иметь tabindex="-1"
        const day5 = screen.getByText('5').closest('[role="button"]');
        expect(day5).toHaveAttribute('tabindex', '-1');
      });
    });
  });

  // Тесты крайних случаев
  describe('Крайние случаи', () => {
    test('работает без обработчика onChange', async () => {
      const consoleSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
      
      render(<CustomDateTimePicker value="2024-01-15T12:00" />);
      
      const displayElement = screen.getByRole('button');
      await act(async () => {
        fireEvent.click(displayElement);
      });
      
      await waitFor(() => {
        expect(screen.getByText('16')).toBeInTheDocument();
      });
      
      const date16 = screen.getByText('16');
      
      expect(() => {
        fireEvent.click(date16);
      }).not.toThrow();
      
      consoleSpy.mockRestore();
    });

    test('работает без ограничений min/max', async () => {
      render(<CustomDateTimePicker value="2024-01-15T12:00" />);
      
      const displayElement = screen.getByRole('button');
      await act(async () => {
        fireEvent.click(displayElement);
      });
      
      await waitFor(() => {
        // Проверяем что все дни рендерятся
        const dayElements = screen.getAllByRole('button').filter(btn => 
          btn.textContent && /^\d+$/.test(btn.textContent.trim())
        );
        expect(dayElements.length).toBeGreaterThan(0);
      });
    });

    test('работает с частично заполненной датой', () => {
      const consoleSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
      
      render(<CustomDateTimePicker value="2024-01" />);
      
      // Проверяем что компонент не падает
      expect(screen.getByRole('button')).toBeInTheDocument();
      
      consoleSpy.mockRestore();
    });
  });

  // Тесты на обработку временной зоны
  describe('Обработка временной зоны', () => {
    test('правильно парсит дату с временем', () => {
      const { rerender } = render(
        <CustomDateTimePicker value="2024-01-15T14:30:45" />
      );
      
      expect(screen.getByText('15 января 2024 г.')).toBeInTheDocument();
      
      rerender(<CustomDateTimePicker value="2024-12-31T23:59:59" />);
      expect(screen.getByText('31 декабря 2024 г.')).toBeInTheDocument();
    });
  });

  // Новые тесты для покрытия проблемных мест
  describe('Дополнительные тесты', () => {
    test('обрабатывает пустое значение', () => {
      render(<CustomDateTimePicker value="" />);
      expect(screen.getByText('Выберите дату')).toBeInTheDocument();
    });

    test('обрабатывает null значение', () => {
      render(<CustomDateTimePicker value={null} />);
      expect(screen.getByText('Выберите дату')).toBeInTheDocument();
    });

    test('обрабатывает undefined значение', () => {
      render(<CustomDateTimePicker value={undefined} />);
      expect(screen.getByText('Выберите дату')).toBeInTheDocument();
    });

    test('сохраняет время при смене даты', async () => {
      const mockOnChange = jest.fn();
      render(
        <CustomDateTimePicker 
          value="2024-01-15T14:30"
          onChange={mockOnChange}
        />
      );
      
      const displayElement = screen.getByRole('button');
      await act(async () => {
        fireEvent.click(displayElement);
      });
      
      await waitFor(() => {
        expect(screen.getByText('16')).toBeInTheDocument();
      });
      
      const date16 = screen.getByText('16');
      await act(async () => {
        fireEvent.click(date16);
      });
      
      jest.advanceTimersByTime(300);
      
      expect(mockOnChange).toHaveBeenCalledWith('2024-01-16T14:30');
    });

    test('использует время по умолчанию при отсутствии времени', async () => {
      const mockOnChange = jest.fn();
      render(
        <CustomDateTimePicker 
          value="2024-01-15"
          onChange={mockOnChange}
        />
      );
      
      const displayElement = screen.getByRole('button');
      await act(async () => {
        fireEvent.click(displayElement);
      });
      
      await waitFor(() => {
        expect(screen.getByText('16')).toBeInTheDocument();
      });
      
      const date16 = screen.getByText('16');
      await act(async () => {
        fireEvent.click(date16);
      });
      
      jest.advanceTimersByTime(300);
      
      expect(mockOnChange).toHaveBeenCalledWith('2024-01-16T12:00');
    });

    test('не генерирует календарь при пустой selectedDate', async () => {
      render(<CustomDateTimePicker />);
      
      const displayElement = screen.getByRole('button');
      await act(async () => {
        fireEvent.click(displayElement);
      });
      
      // Проверяем что пикер открылся, но может не показывать дни
      expect(screen.getByLabelText('Закрыть')).toBeInTheDocument();
    });
  });
});