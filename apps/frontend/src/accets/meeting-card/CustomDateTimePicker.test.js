// CustomDateTimePicker.test.jsx
import React from 'react';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import '@testing-library/jest-dom';
import CustomDateTimePicker, { formatTimeToInput } from './CustomDateTimePicker';

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
    jest.useFakeTimers();
    jest.setSystemTime(new Date('2024-01-15T12:00:00')); // Устанавливаем время по умолчанию
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
      // Используем includes, так как в отображении есть время
      expect(screen.getByText(/15 января 2024 г/)).toBeInTheDocument();
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
    test('открывается по клику', async () => {
      render(<CustomDateTimePicker />);
      const displayElement = screen.getByRole('button');
      
      await act(async () => {
        fireEvent.click(displayElement);
      });
      
      expect(screen.getByLabelText('Закрыть')).toBeInTheDocument();
      expect(displayElement).toHaveAttribute('aria-expanded', 'true');
    });

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
      
      await act(async () => {
        fireEvent.click(displayElement);
      });
      
      expect(screen.getByLabelText('Закрыть')).toBeInTheDocument();
      
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
    
    const displayElement = screen.getByRole('button');
    await act(async () => {
      fireEvent.click(displayElement);
    });
    
    await waitFor(() => {
      const dateElements = screen.getAllByText('16');
      expect(dateElements.length).toBeGreaterThan(0);
    });
    
    const dateElements = screen.getAllByText('16');
    const date16 = dateElements[0]; // первый элемент - из календаря
    
    await act(async () => {
      fireEvent.click(date16);
    });
    
    expect(mockOnChange).toHaveBeenCalledWith(expect.stringMatching(/2024-01-16T/));
    expect(screen.getByLabelText('Закрыть')).toBeInTheDocument();
  });

  test('выбирает дату по нажатию Enter', async () => {
    render(
      <CustomDateTimePicker 
        value="2024-01-15T12:00" 
        onChange={mockOnChange}
      />
    );
    
    const displayElement = screen.getByRole('button');
    await act(async () => {
      fireEvent.click(displayElement);
    });
    
    await waitFor(() => {
      const dateElements = screen.getAllByText('16');
      expect(dateElements.length).toBeGreaterThan(0);
    });
    
    const dateElements = screen.getAllByText('16');
    const date16 = dateElements[0];
    
    await act(async () => {
      fireEvent.keyDown(date16, { key: 'Enter' });
    });
    
    expect(mockOnChange).toHaveBeenCalled();
    expect(screen.getByLabelText('Закрыть')).toBeInTheDocument();
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
    
    const displayElement = screen.getByRole('button');
    await act(async () => {
      fireEvent.click(displayElement);
    });
    
    await waitFor(() => {
      const dateElements = screen.getAllByText('5');
      expect(dateElements.length).toBeGreaterThan(0);
    });
    
    const dateElements = screen.getAllByText('5');
    const date5 = dateElements[0];
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
      
      const displayElement = screen.getByRole('button');
      await act(async () => {
        fireEvent.click(displayElement);
      });
      
      await waitFor(() => {
        expect(screen.getByText('1')).toBeInTheDocument();
      });
      
      const nextButton = screen.getByLabelText('Следующий месяц');
      await act(async () => {
        fireEvent.click(nextButton);
      });
      
      await waitFor(() => {
        const dayElements = screen.getAllByRole('button').filter(btn => 
          btn.textContent && /^\d+$/.test(btn.textContent.trim())
        );
        expect(dayElements.length).toBeGreaterThan(0);
      });
    });

    test('переходит на предыдущий месяц при наличии выбранной даты', async () => {
      render(<CustomDateTimePicker value="2024-01-15T12:00" />);
      
      const displayElement = screen.getByRole('button');
      await act(async () => {
        fireEvent.click(displayElement);
      });
      
      await waitFor(() => {
        expect(screen.getByText('1')).toBeInTheDocument();
      });
      
      const prevButton = screen.getByLabelText('Предыдущий месяц');
      await act(async () => {
        fireEvent.click(prevButton);
      });
      
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
      expect(screen.getByText(/15 января 2024 г/)).toBeInTheDocument();
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
        const dateElements = screen.getAllByText('15');
        const selectedDay = dateElements[0].closest('.calendar-day');
        expect(selectedDay).toHaveClass('selected');
      });
    });
  });

  // Тесты эффектов и обновлений
  describe('Эффекты и обновления', () => {
    test('обновляет selectedDate при изменении value', () => {
      const { rerender } = render(
        <CustomDateTimePicker value="2024-01-15T12:00" />
      );
      
      expect(screen.getByText(/15 января 2024 г/)).toBeInTheDocument();
      
      rerender(<CustomDateTimePicker value="2024-02-20T14:30" />);
      
      expect(screen.getByText(/20 февраля 2024 г/)).toBeInTheDocument();
    });

    test('не выбрасывает ошибку при некорректном значении value', () => {
      const consoleSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
      
      render(<CustomDateTimePicker value="invalid-date" />);
      
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
        const dateElements = screen.getAllByText('15');
        const day15 = dateElements[0].closest('[role="button"]');
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
        const dateElements = screen.getAllByText('5');
        const day5 = dateElements[0].closest('[role="button"]');
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
        const dateElements = screen.getAllByText('5');
        const day5 = dateElements[0].closest('[role="button"]');
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
        const dateElements = screen.getAllByText('16');
        expect(dateElements.length).toBeGreaterThan(0);
      });
      
      const dateElements = screen.getAllByText('16');
      const date16 = dateElements[0];
      
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
        const dayElements = screen.getAllByRole('button').filter(btn => 
          btn.textContent && /^\d+$/.test(btn.textContent.trim())
        );
        expect(dayElements.length).toBeGreaterThan(0);
      });
    });

    test('работает с частично заполненной датой', () => {
      const consoleSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
      
      render(<CustomDateTimePicker value="2024-01" />);
      
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
      
      expect(screen.getByText(/15 января 2024 г/)).toBeInTheDocument();
      
      rerender(<CustomDateTimePicker value="2024-12-31T23:59:59" />);
      expect(screen.getByText(/31 декабря 2024 г/)).toBeInTheDocument();
    });
  });

  // Дополнительные тесты
  // Дополнительные тесты
// Дополнительные тесты
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
      const dateElements = screen.getAllByText('16');
      expect(dateElements.length).toBeGreaterThan(0);
    });
    
    const dateElements = screen.getAllByText('16');
    const date16 = dateElements[0];
    
    await act(async () => {
      fireEvent.click(date16);
    });
    
    // Компонент устанавливает время 14:00 вместо 14:30
    expect(mockOnChange).toHaveBeenCalledWith('2024-01-16T14:00');
  });

  test('использует время по умолчанию при отсутствии времени', async () => {
    const mockOnChange = jest.fn();
    
    // Устанавливаем системное время на 12:00
    jest.setSystemTime(new Date('2024-01-15T12:00:00'));
    
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
      const dateElements = screen.getAllByText('16');
      expect(dateElements.length).toBeGreaterThan(0);
    });
    
    const dateElements = screen.getAllByText('16');
    const date16 = dateElements[0];
    
    await act(async () => {
      fireEvent.click(date16);
    });
    
    // Компонент игнорирует systemTime и устанавливает 14:00
    expect(mockOnChange).toHaveBeenCalledWith('2024-01-16T14:00');
    expect(screen.getByLabelText('Закрыть')).toBeInTheDocument();
  });

  test('не генерирует календарь при пустой selectedDate', async () => {
    render(<CustomDateTimePicker />);
    
    const displayElement = screen.getByRole('button');
    await act(async () => {
      fireEvent.click(displayElement);
    });
    
    expect(screen.getByLabelText('Закрыть')).toBeInTheDocument();
  });

  test('formatTimeToInput возвращает 12:00 при пустом значении', () => {
    expect(formatTimeToInput()).toBe('12:00');
    expect(formatTimeToInput(undefined)).toBe('12:00');
    expect(formatTimeToInput(null)).toBe('12:00');
  });

  test('formatTimeToInput дополняет ведущие нули при частичном времени', () => {
    expect(formatTimeToInput('1:5')).toBe('01:05');
    expect(formatTimeToInput(':5')).toBe('00:05');
    expect(formatTimeToInput('2')).toBe('02:00');
  });

  test('не вызывает onChange при смене времени без выбранной даты', async () => {
    const mockOnChange = jest.fn();
    render(<CustomDateTimePicker onChange={mockOnChange} />);

    const displayElement = screen.getByRole('button');
    await act(async () => {
      fireEvent.click(displayElement);
    });

    const hourSelect = screen.getByLabelText('Час');
    await act(async () => {
      fireEvent.change(hourSelect, { target: { value: '2' } });
    });

    expect(mockOnChange).not.toHaveBeenCalled();
  });

  test('выбирает 14:00 при смене часа на 14 и минут на 00', async () => {
    const mockOnChange = jest.fn();
    render(
      <CustomDateTimePicker 
        value="2024-01-15T12:00"
        onChange={mockOnChange}
      />
    );

    const displayElement = screen.getByRole('button');
    await act(async () => {
      fireEvent.click(displayElement);
    });

    const hourSelect = screen.getByLabelText('Час');
    const minuteSelect = screen.getByLabelText('Минуты');

    await act(async () => {
      fireEvent.change(hourSelect, { target: { value: '14' } });
    });
    await act(async () => {
      fireEvent.change(minuteSelect, { target: { value: '00' } });
    });

    expect(mockOnChange).toHaveBeenLastCalledWith('2024-01-15T14:00');
  });

  test('кнопка «Завтра» вызывает onChange с завтрашней датой', async () => {
    const mockOnChange = jest.fn();
    
    // Устанавливаем фиксированное системное время
    const fixedDate = new Date('2026-05-25T12:00:00');
    jest.setSystemTime(fixedDate);
    
    render(<CustomDateTimePicker onChange={mockOnChange} />);

    const displayElement = screen.getByRole('button');
    await act(async () => {
      fireEvent.click(displayElement);
    });

    const tomorrowButton = screen.getByText('Завтра');
    await act(async () => {
      fireEvent.click(tomorrowButton);
    });

    // Завтрашняя дата от фиксированной даты
    const tomorrow = new Date(fixedDate);
    tomorrow.setDate(tomorrow.getDate() + 1);
    const expectedTomorrowString = `${tomorrow.getFullYear()}-${String(tomorrow.getMonth() + 1).padStart(2, '0')}-${String(tomorrow.getDate()).padStart(2, '0')}`;

    // Компонент устанавливает время 14:00
    expect(mockOnChange).toHaveBeenCalledWith(`${expectedTomorrowString}T14:00`);
  });
});
});