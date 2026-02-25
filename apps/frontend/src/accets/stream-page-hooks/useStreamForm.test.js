const { renderHook, act, waitFor } = require('@testing-library/react');
const { useStreamForm } = require('./useStreamForm');

beforeEach(() => {
  jest.clearAllMocks();
  global.fetch = jest.fn();
});
// Добавим в начало файла, после других импортов
const mockNavigate = jest.fn();
jest.mock('../../utils/csrf-utils', () => ({
  getCsrfConfigForFetch: jest.fn().mockReturnValue({ 'X-CSRF-Token': 'test-token' })
}));

const { getCsrfConfigForFetch } = require('../../utils/csrf-utils');
describe('useStreamForm', () => {
  const backendHost = 'http://localhost:8080/backend';
  const mockNavigate = jest.fn();

  it('должен инициализироваться с начальными значениями', () => {
    const { result } = renderHook(() => useStreamForm(null, mockNavigate));
    expect(result.current.name).toBe('');
    expect(result.current.startDate).toBe('');
    expect(result.current.endDate).toBe('');
    expect(result.current.showCheckboxes2).toBe(false);
    expect(result.current.error).toBeNull();
    expect(result.current.checkboxesData2).toEqual([]);
    expect(result.current.selectedCheckboxes).toEqual([]);
    expect(result.current.image).toBeNull();
  });

  it('должен загружать данные чекбоксов', async () => {
    global.fetch.mockImplementationOnce(() =>
      Promise.resolve({
        ok: true,
        json: () => Promise.resolve([{ id: 1, name: 'Market 1' }, { id: 2, name: 'Market 2' }]),
      })
    );

    const { result } = renderHook(() => useStreamForm(null, mockNavigate));
    await waitFor(() => expect(result.current.checkboxesData2).toEqual([{ id: 1, name: 'Market 1' }, { id: 2, name: 'Market 2' }]));
    expect(result.current.error).toBeNull();
  });

  

  it('должен обрабатывать ошибку при загрузке данных потока', async () => {
    global.fetch
      .mockImplementationOnce(() =>
        Promise.resolve({
          ok: true,
          json: () => Promise.resolve([{ id: 1, name: 'Market 1' }]),
        })
      )
      .mockImplementationOnce(() => Promise.reject(new Error('Network error')));

    const { result } = renderHook(() => useStreamForm(1, mockNavigate));
    await waitFor(() => expect(result.current.error).toBe('Не удалось загрузить данные потока.'));
  });

  it('должен обрабатывать изменение имени', () => {
    const { result } = renderHook(() => useStreamForm(null, mockNavigate));
    act(() => {
      result.current.handleNameChange({ target: { value: 'New Stream' } });
    });
    expect(result.current.name).toBe('New Stream');
  });

  it('должен переключать видимость чекбоксов', () => {
    const { result } = renderHook(() => useStreamForm(null, mockNavigate));
    act(() => {
      result.current.handleShowCheckboxes2();
    });
    expect(result.current.showCheckboxes2).toBe(true);
  });
it('должен обрабатывать загрузку изображения', () => {
  const { result } = renderHook(() => useStreamForm(null, mockNavigate));
  const mockFile = new File(['dummy'], 'test.png', { type: 'image/png' });
  const mockReader = {
    readAsDataURL: jest.fn(),
    onloadend: null,
    result: 'data:image/png;base64,test',
  };
  window.FileReader = jest.fn(() => mockReader);

  act(() => {
    result.current.handleImageUpload({ target: { files: [mockFile] } });
    mockReader.onloadend(); // эмулируем окончание загрузки
  });

  expect(result.current.image).toBe('data:image/png;base64,test');
});

it('не должен устанавливать изображение, если выбран не image-файл', () => {
  const { result } = renderHook(() => useStreamForm(null, mockNavigate));
  const mockFile = new File(['dummy'], 'test.txt', { type: 'text/plain' });

  act(() => {
    result.current.handleImageUpload({ target: { files: [mockFile] } });
  });

  expect(result.current.error).toBe('Пожалуйста, выберите файл изображения (JPEG, PNG, GIF)');
});


// Тест на дату начала позже даты окончания
it('не валидирует, если дата начала позже даты окончания', () => {
  const { result } = renderHook(() => useStreamForm(null, mockNavigate));
  act(() => {
    result.current.handleNameChange({ target: { value: 'Stream' } });
    result.current.handleStartDateChange({ target: { value: '2024-01-03' } });
    result.current.handleEndDateChange({ target: { value: '2024-01-02' } });
    result.current.handleTrackStartDateChange({ target: { value: '2024-01-02' } });
    result.current.handleMeetingsCountChange({ target: { value: '5' } });
    result.current.handleCheckboxChange(1);
  });

  act(() => {
    result.current.handleSubmit();
  });

  expect(result.current.error).toBe('Дата начала должна быть раньше даты конца.');
});

// Тест на отсутствие чекбоксов
it('не валидирует, если не выбрано ни одного чекбокса', () => {
  const { result } = renderHook(() => useStreamForm(null, mockNavigate));
  act(() => {
    result.current.handleNameChange({ target: { value: 'Stream' } });
    result.current.handleStartDateChange({ target: { value: '2024-01-01' } });
    result.current.handleEndDateChange({ target: { value: '2024-01-02' } });
    result.current.handleTrackStartDateChange({ target: { value: '2024-01-01' } });
    result.current.handleMeetingsCountChange({ target: { value: '5' } });
  });

  act(() => {
    result.current.handleSubmit();
  });

  expect(result.current.error).toBe('Пожалуйста, укажите хотя бы один рынок НТИ.');
});

// Тест на неправильную дату
it('не валидирует неправильную дату', () => {
  const { result } = renderHook(() => useStreamForm(null, mockNavigate));

  act(() => {
    result.current.handleNameChange({ target: { value: 'Stream' } });
    result.current.handleStartDateChange({ target: { value: '2024-02-31' } }); // 31.02 — неверно
    result.current.handleEndDateChange({ target: { value: '2024-03-01' } });
    result.current.handleTrackStartDateChange({ target: { value: '2024-02-01' } });
    result.current.handleMeetingsCountChange({ target: { value: '5' } });
    result.current.handleCheckboxChange(1);
  });

  act(() => {
    result.current.handleSubmit();
  });

  expect(result.current.error).toBe('Некорректный формат даты. Используйте формат ДД.ММ.ГГГГ.');
});

// Тест на ошибки submit
it('should handle submission errors', async () => {
  global.fetch
    .mockResolvedValueOnce({ ok: true, json: () => Promise.resolve([]) }) // fetchCheckboxesData
    .mockRejectedValueOnce(new Error('Network error')); // handleSubmit
    
  const { result } = renderHook(() => useStreamForm(null, mockNavigate));
  
  act(() => {
    result.current.handleNameChange({ target: { value: 'Test Stream' } });
    result.current.handleStartDateChange({ target: { value: '2023-01-01' } });
    result.current.handleEndDateChange({ target: { value: '2023-01-02' } });
    result.current.handleTrackStartDateChange({ target: { value: '2023-01-01' } });
    result.current.handleMeetingsCountChange({ target: { value: '5' } });
    result.current.handleCheckboxChange(1);
  });
  
  await act(async () => {
    await result.current.handleSubmit(false);
  });
  
  expect(result.current.error).toBe('Не удалось создать поток или загрузить изображение.');
});

// Тест на разные сообщения ошибок
// Исправленный тест для разных сообщений ошибок
it('should handle different error messages for create and edit modes', async () => {
  // Мокируем сначала успешную загрузку чекбоксов
  global.fetch
    .mockImplementationOnce(() => 
      Promise.resolve({
        ok: true,
        json: () => Promise.resolve([])
      })
    )
    // Затем ошибку при создании
    .mockImplementationOnce(() => 
      Promise.reject(new Error('Network error'))
    )
    // Затем снова чекбоксы для edit mode
    .mockImplementationOnce(() => 
      Promise.resolve({
        ok: true,
        json: () => Promise.resolve([])
      })
    )
    // Затем ошибку при редактировании
    .mockImplementationOnce(() => 
      Promise.reject(new Error('Network error'))
    );

  const { result, rerender } = renderHook((props) => useStreamForm(props.streamId, mockNavigate), {
    initialProps: { streamId: null }
  });

  // Ждем загрузки чекбоксов
  await waitFor(() => expect(result.current.checkboxesData2).toEqual([]));

  act(() => {
    result.current.handleNameChange({ target: { value: 'Test Stream' } });
    result.current.handleStartDateChange({ target: { value: '2023-01-01' } });
    result.current.handleEndDateChange({ target: { value: '2023-01-02' } });
    result.current.handleTrackStartDateChange({ target: { value: '2023-01-01' } });
    result.current.handleMeetingsCountChange({ target: { value: '5' } });
    result.current.handleCheckboxChange(1);
  });

  await act(async () => {
    await result.current.handleSubmit(false);
  });

  expect(result.current.error).toBe('Не удалось создать поток или загрузить изображение.');

  // Переключаемся в edit mode
  rerender({ streamId: 1 });

  await act(async () => {
    await result.current.handleSubmit(true);
  });

  expect(result.current.error).toBe('Не удалось обновить поток или загрузить изображение.');
});




  
});
describe('useStreamForm', () => {
  const backendHost = 'http://localhost:8080/backend';
  const mockNavigate = jest.fn();

  it('должен инициализироваться с начальными значениями', () => {
    const { result } = renderHook(() => useStreamForm(null, mockNavigate));
    expect(result.current.name).toBe('');
    expect(result.current.startDate).toBe('');
    expect(result.current.endDate).toBe('');
    expect(result.current.showCheckboxes2).toBe(false);
    expect(result.current.error).toBeNull();
    expect(result.current.checkboxesData2).toEqual([]);
    expect(result.current.selectedCheckboxes).toEqual([]);
    expect(result.current.image).toBeNull();
  });

  it('должен загружать данные чекбоксов', async () => {
    global.fetch.mockImplementationOnce(() =>
      Promise.resolve({
        ok: true,
        json: () => Promise.resolve([{ id: 1, name: 'Market 1' }, { id: 2, name: 'Market 2' }]),
      })
    );

    const { result } = renderHook(() => useStreamForm(null, mockNavigate));
    await waitFor(() => expect(result.current.checkboxesData2).toEqual([{ id: 1, name: 'Market 1' }, { id: 2, name: 'Market 2' }]));
    expect(result.current.error).toBeNull();
  });

  it('должен обрабатывать ошибку при загрузке данных потока', async () => {
    global.fetch
      .mockImplementationOnce(() =>
        Promise.resolve({
          ok: true,
          json: () => Promise.resolve([{ id: 1, name: 'Market 1' }]),
        })
      )
      .mockImplementationOnce(() => Promise.reject(new Error('Network error')));

    const { result } = renderHook(() => useStreamForm(1, mockNavigate));
    await waitFor(() => expect(result.current.error).toBe('Не удалось загрузить данные потока.'));
  });

  it('должен обрабатывать изменение имени', () => {
    const { result } = renderHook(() => useStreamForm(null, mockNavigate));
    act(() => {
      result.current.handleNameChange({ target: { value: 'New Stream' } });
    });
    expect(result.current.name).toBe('New Stream');
  });

  it('должен форматировать дату при вводе', () => {
    const { result } = renderHook(() => useStreamForm(null, mockNavigate));
    act(() => {
      result.current.handleStartDateChange({ target: { value: '2025-02-01' } });
    });
    expect(result.current.startDate).toBe('2025-02-01');
  });

  it('должен переключать видимость чекбоксов', () => {
    const { result } = renderHook(() => useStreamForm(null, mockNavigate));
    act(() => {
      result.current.handleShowCheckboxes2();
    });
    expect(result.current.showCheckboxes2).toBe(true);
  });

  it('должен обрабатывать загрузку изображения', () => {
    const { result } = renderHook(() => useStreamForm(null, mockNavigate));
    const mockFile = new File(['dummy'], 'test.png', { type: 'image/png' });
    const mockReader = {
      readAsDataURL: jest.fn(),
      onloadend: null,
      result: 'data:image/png;base64,test',
    };
    window.FileReader = jest.fn(() => mockReader);

    act(() => {
      result.current.handleImageUpload({ target: { files: [mockFile] } });
      mockReader.onloadend();
    });

    expect(result.current.image).toBe('data:image/png;base64,test');
  });

  it('не должен устанавливать изображение, если выбран не image-файл', () => {
    const { result } = renderHook(() => useStreamForm(null, mockNavigate));
    const mockFile = new File(['dummy'], 'test.txt', { type: 'text/plain' });

    act(() => {
      result.current.handleImageUpload({ target: { files: [mockFile] } });
    });

    expect(result.current.error).toBe('Пожалуйста, выберите файл изображения (JPEG, PNG, GIF)');
  });

  it('валидирует форму корректно', () => {
    const { result } = renderHook(() => useStreamForm(null, mockNavigate));

    act(() => {
      result.current.handleNameChange({ target: { value: 'Stream' } });
      result.current.handleStartDateChange({ target: { value: '2024-01-01' } });
      result.current.handleEndDateChange({ target: { value: '2024-01-02' } });
      result.current.handleCheckboxChange(1);
    });

    expect(result.current.name).toBe('Stream');
    expect(result.current.startDate).toBe('2024-01-01');
    expect(result.current.endDate).toBe('2024-01-02');
  });

  

 
  it('не валидирует, если поля не заполнены', () => {
    const { result } = renderHook(() => useStreamForm(null, mockNavigate));

    act(() => {
      result.current.handleSubmit();
    });

    expect(result.current.error).toBe('Пожалуйста, заполните все обязательные поля.');
  });
  // Исправленный тест для разных сообщений ошибок
it('should handle different error messages for create and edit modes', async () => {
  // Мокируем сначала успешную загрузку чекбоксов
  global.fetch
    .mockImplementationOnce(() => 
      Promise.resolve({
        ok: true,
        json: () => Promise.resolve([])
      })
    )
    // Затем ошибку при создании
    .mockImplementationOnce(() => 
      Promise.reject(new Error('Network error'))
    )
    // Затем снова чекбоксы для edit mode
    .mockImplementationOnce(() => 
      Promise.resolve({
        ok: true,
        json: () => Promise.resolve([])
      })
    )
    // Затем ошибку при редактировании
    .mockImplementationOnce(() => 
      Promise.reject(new Error('Network error'))
    );

  const { result, rerender } = renderHook((props) => useStreamForm(props.streamId, mockNavigate), {
    initialProps: { streamId: null }
  });

  // Ждем загрузки чекбоксов
  await waitFor(() => expect(result.current.checkboxesData2).toEqual([]));

  act(() => {
    result.current.handleNameChange({ target: { value: 'Test Stream' } });
    result.current.handleStartDateChange({ target: { value: '2023-01-01' } });
    result.current.handleEndDateChange({ target: { value: '2023-01-02' } });
    result.current.handleTrackStartDateChange({ target: { value: '2023-01-01' } });
    result.current.handleMeetingsCountChange({ target: { value: '5' } });
    result.current.handleCheckboxChange(1);
  });

  await act(async () => {
    await result.current.handleSubmit(false);
  });

  expect(result.current.error).toBe('Не удалось создать поток или загрузить изображение.');

  // Переключаемся в edit mode
  rerender({ streamId: 1 });

  await act(async () => {
    await result.current.handleSubmit(true);
  });

  expect(result.current.error).toBe('Не удалось обновить поток или загрузить изображение.');
});
  it('должен прокручивать к errorRef при наличии ошибки', () => {
  const scrollIntoViewMock = jest.fn();
  const errorElement = { scrollIntoView: scrollIntoViewMock };

  const { result } = renderHook(() => useStreamForm(null, mockNavigate));
  act(() => {
    result.current.errorRef.current = errorElement;
    result.current.handleSubmit(); // вызовет setError
  });

  expect(scrollIntoViewMock).toHaveBeenCalledWith({ behavior: 'smooth', block: 'center' });
});
it('должен установить ошибку при сбое загрузки чекбоксов', async () => {
  global.fetch.mockRejectedValueOnce(new Error('Network error'));

  const { result } = renderHook(() => useStreamForm(null, mockNavigate));

  await waitFor(() => {
    expect(result.current.error).toBe('Не удалось загрузить данные для чекбоксов.');
  });
});
it('должен возвращать null при ошибке загрузки изображения потока', async () => {
  global.fetch.mockRejectedValueOnce(new Error('Network error'));
  const { result } = renderHook(() => useStreamForm(1, mockNavigate));

  const image = await result.current.handleSubmit(); // косвенно вызывает
  expect(result.current.image).toBeNull();
});
it('должен форматировать дату из ISO в формат ДД.ММ.ГГГГ', async () => {
  global.fetch
    .mockImplementationOnce(() => Promise.resolve({
      ok: true,
      json: () => Promise.resolve([{ id: 1, name: 'Market 1' }]),
    }))
    .mockImplementationOnce(() => Promise.resolve({
      ok: true,
      json: () => Promise.resolve({
        name: 'Test Stream',
        startDate: '2025-07-01T00:00:00Z',
        endDate: '2025-07-10T00:00:00Z',
        ntiMarkets: [],
      }),
    }))
    .mockImplementationOnce(() => Promise.resolve({
      ok: false,
    }));

  const { result } = renderHook(() => useStreamForm(1, mockNavigate));
  await waitFor(() => {
    expect(result.current.startDate).toBe('2025-07-01');
    expect(result.current.endDate).toBe('2025-07-10');
  });
});
it('должен скрывать чекбоксы при клике вне', () => {
  const { result } = renderHook(() => useStreamForm(null, mockNavigate));

  const fakeElement = document.createElement('div');
  document.body.appendChild(fakeElement);

  act(() => {
    result.current.checkboxesRef.current = fakeElement;
    result.current.handleShowCheckboxes2(); // открыть
    document.dispatchEvent(new MouseEvent('mousedown', { bubbles: true }));
  });

  expect(result.current.showCheckboxes2).toBe(false);
});

it('fetchCheckboxesData устанавливает данные чекбоксов при успешном ответе', async () => {
    // Строки 42-45: fetchCheckboxesData с сетевым запросом и установкой setCheckboxesData2
    global.fetch.mockResolvedValueOnce({
      ok: true,
      json: async () => [{ id: 1, name: 'Market 1' }, { id: 2, name: 'Market 2' }],
    });

    const { result } = renderHook(() => useStreamForm(null, mockNavigate));

    // Ждем, пока асинхронные данные загрузятся и состояние обновится
    await waitFor(() => expect(result.current.checkboxesData2.length).toBe(2));
    expect(result.current.error).toBeNull();
  });

  it('fetchCheckboxesData устанавливает ошибку при неуспешном ответе', async () => {
    // Строки 42-45: обработка ошибки fetchCheckboxesData
    global.fetch.mockRejectedValueOnce(new Error('Network error'));

    const { result } = renderHook(() => useStreamForm(null, mockNavigate));

    await waitFor(() => {
      expect(result.current.error).toBe('Не удалось загрузить данные для чекбоксов.');
    });
  });


  it('fetchStreamData устанавливает ошибку при неуспешном ответе', async () => {
    global.fetch
      .mockResolvedValueOnce({ // fetchCheckboxesData
        ok: true,
        json: async () => [],
      })
      .mockResolvedValueOnce({ // fetchStreamData — ошибка
        ok: false,
      });

    const { result } = renderHook(() => useStreamForm(123, mockNavigate));

    await waitFor(() => {
      expect(result.current.error).toBe('Не удалось загрузить данные потока.');
    });
  });
  
describe('handleSubmit', () => {
  const alertMock = jest.spyOn(window, 'alert').mockImplementation(() => {});

  beforeEach(() => {
    global.fetch
      .mockResolvedValueOnce({ ok: true, json: () => Promise.resolve([]) }); // fetchCheckboxesData
  });

  afterEach(() => {
    alertMock.mockClear();
  });

  it('успешно обновляет поток без загрузки нового изображения', async () => {
  global.fetch
    .mockImplementationOnce(() => 
      Promise.resolve({
        ok: true,
        json: () => Promise.resolve([])
      })
    )
    .mockImplementationOnce(() => 
      Promise.resolve({
        ok: true,
        json: () => Promise.resolve({ id: 123 })
      })
    );

  const { result } = renderHook(() => useStreamForm(123, mockNavigate));

  act(() => {
    result.current.handleNameChange({ target: { value: 'Stream Edit' } });
    result.current.handleStartDateChange({ target: { value: '2024-01-01' } });
    result.current.handleEndDateChange({ target: { value: '2024-01-02' } });
    result.current.handleTrackStartDateChange({ target: { value: '2024-01-01' } });
    result.current.handleMeetingsCountChange({ target: { value: '5' } });
    result.current.handleCheckboxChange(1);
  });

  await act(async () => {
    await result.current.handleSubmit(true);
  });

  expect(alertMock).toHaveBeenCalledWith('Поток успешно обновлен!');
  expect(mockNavigate).toHaveBeenCalledWith('/team-cards');
});

  it('создает поток и загружает дефолтное изображение, если imageFile не указан', async () => {
    global.fetch
      .mockResolvedValueOnce({ ok: true, json: () => Promise.resolve({ id: 999 }) }) // POST
      .mockResolvedValueOnce({ ok: true, blob: () => Promise.resolve(new Blob()) }) // rabbit.png
      .mockResolvedValueOnce({ ok: true }); // image upload
    
    const { result } = renderHook(() => useStreamForm(null, mockNavigate));

    act(() => {
      result.current.handleNameChange({ target: { value: 'Stream' } });
      result.current.handleStartDateChange({ target: { value: '2024-01-01' } });
      result.current.handleEndDateChange({ target: { value: '2024-01-02' } });
      result.current.handleTrackStartDateChange({ target: { value: '2024-01-01' } });
      result.current.handleMeetingsCountChange({ target: { value: '5' } });
      result.current.handleCheckboxChange(1);
    });
    
    await act(async () => {
      await result.current.handleSubmit(false);
    });
    
    expect(alertMock).toHaveBeenCalledWith('Поток успешно создан!');
    expect(mockNavigate).toHaveBeenCalledWith('/team-cards');
  });

  it('выводит ошибку, если не удалось создать поток', async () => {
    global.fetch
      .mockRejectedValueOnce(new Error('Network error')); // POST
    
    const { result } = renderHook(() => useStreamForm(null, mockNavigate));

    act(() => {
      result.current.handleNameChange({ target: { value: 'Stream' } });
      result.current.handleStartDateChange({ target: { value: '2024-01-01' } });
      result.current.handleEndDateChange({ target: { value: '2024-01-02' } });
      result.current.handleTrackStartDateChange({ target: { value: '2024-01-01' } });
      result.current.handleMeetingsCountChange({ target: { value: '5' } });
      result.current.handleCheckboxChange(1);
    });
    
    await act(async () => {
      await result.current.handleSubmit(false);
    });
    
    expect(result.current.error).toBe('Не удалось создать поток или загрузить изображение.');
    expect(alertMock).not.toHaveBeenCalled();
  });

  it('выводит ошибку, если загрузка изображения не удалась', async () => {
    const mockFile = new File(['dummy'], 'image.png', { type: 'image/png' });
    
    global.fetch
      .mockResolvedValueOnce({ ok: true, json: () => Promise.resolve({ id: 123 }) }) // POST
      .mockResolvedValueOnce({ ok: false }); // image upload
    
    const { result } = renderHook(() => useStreamForm(null, mockNavigate));

    act(() => {
      result.current.handleNameChange({ target: { value: 'Stream' } });
      result.current.handleStartDateChange({ target: { value: '2024-01-01' } });
      result.current.handleEndDateChange({ target: { value: '2024-01-02' } });
      result.current.handleTrackStartDateChange({ target: { value: '2024-01-01' } });
      result.current.handleMeetingsCountChange({ target: { value: '5' } });
      result.current.handleCheckboxChange(1);
      result.current.imageFile = mockFile;
    });
    
    await act(async () => {
      await result.current.handleSubmit(false);
    });
    
    expect(result.current.error).toBe('Не удалось создать поток или загрузить изображение.');
    expect(alertMock).not.toHaveBeenCalled();
  });
});
});


describe('Track meeting date validation', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    global.fetch = jest.fn().mockResolvedValue({
      ok: true,
      json: () => Promise.resolve([]),
    });
  });

  it('should show error when track start date is before stream start date', () => {
    const { result } = renderHook(() => useStreamForm(null, mockNavigate));
    
    act(() => {
      result.current.handleNameChange({ target: { value: 'Stream' } });
      result.current.handleStartDateChange({ target: { value: '2024-01-02' } }); // 02.01.2024
      result.current.handleEndDateChange({ target: { value: '2024-01-03' } }); // 03.01.2024
      result.current.handleTrackStartDateChange({ target: { value: '2024-01-01' } }); // 01.01.2024 (before start)
      result.current.handleMeetingsCountChange({ target: { value: '5' } });
      result.current.handleCheckboxChange(1);
    });

    act(() => {
      result.current.handleSubmit();
    });

    expect(result.current.error).toBe('Дата начала трекшен-митинга должна быть между датой начала и конца потока.');
  });

  it('should show error when track start date is after stream end date', () => {
    const { result } = renderHook(() => useStreamForm(null, mockNavigate));
    
    act(() => {
      result.current.handleNameChange({ target: { value: 'Stream' } });
      result.current.handleStartDateChange({ target: { value: '2024-01-01' } }); // 01.01.2024
      result.current.handleEndDateChange({ target: { value: '2024-01-02' } }); // 02.01.2024
      result.current.handleTrackStartDateChange({ target: { value: '2024-01-03' } }); // 03.01.2024 (after end)
      result.current.handleMeetingsCountChange({ target: { value: '5' } });
      result.current.handleCheckboxChange(1);
    });

    act(() => {
      result.current.handleSubmit();
    });

    expect(result.current.error).toBe('Дата начала трекшен-митинга должна быть между датой начала и конца потока.');
  });
});

describe('Meetings count validation', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    global.fetch = jest.fn().mockResolvedValue({
      ok: true,
      json: () => Promise.resolve([]),
    });
  });

 

  it('should accept valid meetings count between 1 and 100', () => {
    const { result } = renderHook(() => useStreamForm(null, mockNavigate));
    
    act(() => {
      result.current.handleMeetingsCountChange({ target: { value: '1' } });
    });
    expect(result.current.meetingsCount).toBe('1');

    act(() => {
      result.current.handleMeetingsCountChange({ target: { value: '50' } });
    });
    expect(result.current.meetingsCount).toBe('50');

    act(() => {
      result.current.handleMeetingsCountChange({ target: { value: '100' } });
    });
    expect(result.current.meetingsCount).toBe('100');
  });
});

describe('Meetings count functionality', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    global.fetch = jest.fn().mockResolvedValue({
      ok: true,
      json: () => Promise.resolve([]),
    });
  });

  // Тест для строк 82-90: Установка meetingsCount при загрузке данных потока
  describe('fetchStreamData meetings count handling', () => {
    it('should set meetingsCount from predefined options', async () => {
      global.fetch
        .mockResolvedValueOnce({ ok: true, json: () => Promise.resolve([]) }) // fetchCheckboxesData
        .mockResolvedValueOnce({ 
          ok: true, 
          json: () => Promise.resolve({
            name: 'Test Stream',
            startDate: '2024-01-01T00:00:00Z',
            endDate: '2024-01-10T00:00:00Z',
            trackStartDate: '2024-01-05T00:00:00Z',
            meetingsCount: 5,
            ntiMarkets: []
          })
        });

      const { result } = renderHook(() => useStreamForm(1, mockNavigate));
      
      await waitFor(() => {
        expect(result.current.meetingsCount).toBe('5');
        expect(result.current.showCustomInput).toBe(false);
        expect(result.current.customMeetingsCount).toBe('');
      });
    });

    it('should set custom meetingsCount when value not in predefined options', async () => {
      global.fetch
        .mockResolvedValueOnce({ ok: true, json: () => Promise.resolve([]) })
        .mockResolvedValueOnce({ 
          ok: true, 
          json: () => Promise.resolve({
            name: 'Test Stream',
            startDate: '2024-01-01T00:00:00Z',
            endDate: '2024-01-10T00:00:00Z',
            trackStartDate: '2024-01-05T00:00:00Z',
            meetingsCount: 7, // 7 нет в [5, 10, 15, 20]
            ntiMarkets: []
          })
        });

      const { result } = renderHook(() => useStreamForm(1, mockNavigate));
      
      await waitFor(() => {
        expect(result.current.meetingsCount).toBe('custom');
        expect(result.current.showCustomInput).toBe(true);
        expect(result.current.customMeetingsCount).toBe('7');
      });
    });

    it('should handle empty meetingsCount', async () => {
      global.fetch
        .mockResolvedValueOnce({ ok: true, json: () => Promise.resolve([]) })
        .mockResolvedValueOnce({ 
          ok: true, 
          json: () => Promise.resolve({
            name: 'Test Stream',
            startDate: '2024-01-01T00:00:00Z',
            endDate: '2024-01-10T00:00:00Z',
            trackStartDate: '2024-01-05T00:00:00Z',
            meetingsCount: null,
            ntiMarkets: []
          })
        });

      const { result } = renderHook(() => useStreamForm(1, mockNavigate));
      
      await waitFor(() => {
        expect(result.current.meetingsCount).toBe('');
        expect(result.current.showCustomInput).toBe(false);
        expect(result.current.customMeetingsCount).toBe('');
      });
    });
  });

  // Тест для строк 121-122: Обработка изменения meetingsCount
  describe('handleMeetingsCountChange', () => {
    it('should show custom input when "custom" is selected', () => {
      const { result } = renderHook(() => useStreamForm(null, mockNavigate));
      
      act(() => {
        result.current.handleMeetingsCountChange({ target: { value: 'custom' } });
      });
      
      expect(result.current.showCustomInput).toBe(true);
      expect(result.current.customMeetingsCount).toBe('');
    });

    it('should hide custom input when predefined option is selected', () => {
      const { result } = renderHook(() => useStreamForm(null, mockNavigate));
      
      // Сначала установим custom
      act(() => {
        result.current.handleMeetingsCountChange({ target: { value: 'custom' } });
      });
      
      // Затем выбираем предопределенное значение
      act(() => {
        result.current.handleMeetingsCountChange({ target: { value: '5' } });
      });
      
      expect(result.current.showCustomInput).toBe(false);
      expect(result.current.customMeetingsCount).toBe('');
      expect(result.current.meetingsCount).toBe('5');
    });
  });

  // Тест для строк 147-149: Автофокус на custom input
  describe('Custom input autofocus', () => {
    it('should focus on custom input when shown', () => {
      const focusMock = jest.fn();
      const querySelectorMock = jest.spyOn(document, 'querySelector').mockReturnValue({
        focus: focusMock
      });

      const { result } = renderHook(() => useStreamForm(null, mockNavigate));
      
      act(() => {
        result.current.setShowCustomInput(true);
      });

      expect(querySelectorMock).toHaveBeenCalledWith('.create-stream-custom-input');
      expect(focusMock).toHaveBeenCalled();

      querySelectorMock.mockRestore();
    });

    it('should not try to focus if input element not found', () => {
      const querySelectorMock = jest.spyOn(document, 'querySelector').mockReturnValue(null);
      const consoleErrorMock = jest.spyOn(console, 'error').mockImplementation(() => {});

      const { result } = renderHook(() => useStreamForm(null, mockNavigate));
      
      act(() => {
        result.current.setShowCustomInput(true);
      });

      // Не должно быть ошибок, даже если элемент не найден
      expect(consoleErrorMock).not.toHaveBeenCalled();

      querySelectorMock.mockRestore();
      consoleErrorMock.mockRestore();
    });
  });

  // Тест для строк 247-248, 252-253: Валидация meetingsCount
  describe('Meetings count validation', () => {
    
    it('should show error when custom meetingsCount is empty', () => {
      const { result } = renderHook(() => useStreamForm(null, mockNavigate));
      
      act(() => {
        result.current.handleNameChange({ target: { value: 'Stream' } });
        result.current.handleStartDateChange({ target: { value: '2024-01-01' } });
        result.current.handleEndDateChange({ target: { value: '2024-01-02' } });
        result.current.handleTrackStartDateChange({ target: { value: '2024-01-01' } });
        result.current.handleMeetingsCountChange({ target: { value: 'custom' } }); // custom выбран
        result.current.handleCheckboxChange(1);
        // customMeetingsCount не установлен
      });

      act(() => {
        result.current.handleSubmit();
      });

      expect(result.current.error).toBe('Пожалуйста, заполните количество встреч.');
    });

    

    
  });
  describe('handleCustomMeetingsCountChange', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    global.fetch = jest.fn().mockResolvedValue({
      ok: true,
      json: () => Promise.resolve([]),
    });
  });

  it('should set customMeetingsCount for empty string', () => {
    const { result } = renderHook(() => useStreamForm(null, mockNavigate));
    
    act(() => {
      result.current.handleCustomMeetingsCountChange({ 
        target: { value: '' } 
      });
    });
    
    expect(result.current.customMeetingsCount).toBe('');
  });

  it('should set customMeetingsCount for valid numeric input (1)', () => {
    const { result } = renderHook(() => useStreamForm(null, mockNavigate));
    
    act(() => {
      result.current.handleCustomMeetingsCountChange({ 
        target: { value: '1' } 
      });
    });
    
    expect(result.current.customMeetingsCount).toBe('1');
  });

  it('should set customMeetingsCount for valid numeric input (100)', () => {
    const { result } = renderHook(() => useStreamForm(null, mockNavigate));
    
    act(() => {
      result.current.handleCustomMeetingsCountChange({ 
        target: { value: '100' } 
      });
    });
    
    expect(result.current.customMeetingsCount).toBe('100');
  });

  it('should set customMeetingsCount for valid numeric input (middle value)', () => {
    const { result } = renderHook(() => useStreamForm(null, mockNavigate));
    
    act(() => {
      result.current.handleCustomMeetingsCountChange({ 
        target: { value: '50' } 
      });
    });
    
    expect(result.current.customMeetingsCount).toBe('50');
  });

  it('should NOT set customMeetingsCount for non-numeric input', () => {
    const { result } = renderHook(() => useStreamForm(null, mockNavigate));
    
    // Сохраняем начальное значение
    const initialValue = result.current.customMeetingsCount;
    
    act(() => {
      result.current.handleCustomMeetingsCountChange({ 
        target: { value: 'abc' } 
      });
    });
    
    // Значение не должно измениться
    expect(result.current.customMeetingsCount).toBe(initialValue);
  });

  it('should NOT set customMeetingsCount for negative number', () => {
    const { result } = renderHook(() => useStreamForm(null, mockNavigate));
    
    const initialValue = result.current.customMeetingsCount;
    
    act(() => {
      result.current.handleCustomMeetingsCountChange({ 
        target: { value: '-5' } 
      });
    });
    
    expect(result.current.customMeetingsCount).toBe(initialValue);
  });

  it('should NOT set customMeetingsCount for zero', () => {
    const { result } = renderHook(() => useStreamForm(null, mockNavigate));
    
    const initialValue = result.current.customMeetingsCount;
    
    act(() => {
      result.current.handleCustomMeetingsCountChange({ 
        target: { value: '0' } 
      });
    });
    
    expect(result.current.customMeetingsCount).toBe(initialValue);
  });

  it('should NOT set customMeetingsCount for number greater than 100', () => {
    const { result } = renderHook(() => useStreamForm(null, mockNavigate));
    
    const initialValue = result.current.customMeetingsCount;
    
    act(() => {
      result.current.handleCustomMeetingsCountChange({ 
        target: { value: '101' } 
      });
    });
    
    expect(result.current.customMeetingsCount).toBe(initialValue);
  });

  it('should NOT set customMeetingsCount for decimal number', () => {
    const { result } = renderHook(() => useStreamForm(null, mockNavigate));
    
    const initialValue = result.current.customMeetingsCount;
    
    act(() => {
      result.current.handleCustomMeetingsCountChange({ 
        target: { value: '5.5' } 
      });
    });
    
    expect(result.current.customMeetingsCount).toBe(initialValue);
  });

  it('should NOT set customMeetingsCount for alphanumeric input', () => {
    const { result } = renderHook(() => useStreamForm(null, mockNavigate));
    
    const initialValue = result.current.customMeetingsCount;
    
    act(() => {
      result.current.handleCustomMeetingsCountChange({ 
        target: { value: '10abc' } 
      });
    });
    
    expect(result.current.customMeetingsCount).toBe(initialValue);
  });

  it('should handle multiple valid inputs sequentially', () => {
    const { result } = renderHook(() => useStreamForm(null, mockNavigate));
    
    act(() => {
      result.current.handleCustomMeetingsCountChange({ 
        target: { value: '10' } 
      });
    });
    expect(result.current.customMeetingsCount).toBe('10');
    
    act(() => {
      result.current.handleCustomMeetingsCountChange({ 
        target: { value: '' } 
      });
    });
    expect(result.current.customMeetingsCount).toBe('');
    
    act(() => {
      result.current.handleCustomMeetingsCountChange({ 
        target: { value: '25' } 
      });
    });
    expect(result.current.customMeetingsCount).toBe('25');
  });

  it('should handle multiple invalid inputs sequentially without changing value', () => {
    const { result } = renderHook(() => useStreamForm(null, mockNavigate));
    
    // Сначала устанавливаем валидное значение
    act(() => {
      result.current.handleCustomMeetingsCountChange({ 
        target: { value: '30' } 
      });
    });
    expect(result.current.customMeetingsCount).toBe('30');
    
    // Пытаемся установить невалидное значение
    act(() => {
      result.current.handleCustomMeetingsCountChange({ 
        target: { value: 'invalid' } 
      });
    });
    // Значение должно остаться прежним
    expect(result.current.customMeetingsCount).toBe('30');
    
    // Пытаемся установить другое невалидное значение
    act(() => {
      result.current.handleCustomMeetingsCountChange({ 
        target: { value: '150' } 
      });
    });
    // Значение должно остаться прежним
    expect(result.current.customMeetingsCount).toBe('30');
  });
});
});
describe('deleteStream', () => {
  const alertMock = jest.spyOn(window, 'alert').mockImplementation(() => {});
  const mockNavigate = jest.fn();
  const backendHost = 'http://localhost:8080/backend';

  beforeEach(() => {
    jest.clearAllMocks();
    global.fetch = jest.fn();
    alertMock.mockClear();
    mockNavigate.mockClear();
    
    // Сбрасываем мок перед каждым тестом
    getCsrfConfigForFetch.mockReturnValue({ 'X-CSRF-Token': 'test-token' });
    
    // Мокаем успешную загрузку чекбоксов для всех тестов
    global.fetch.mockResolvedValueOnce({
      ok: true,
      json: () => Promise.resolve([]),
    });
  });

  afterAll(() => {
    alertMock.mockRestore();
  });

  it('should successfully delete stream and navigate to streams page', async () => {
  console.log('Starting test...');
  
  global.fetch
    .mockResolvedValueOnce({ ok: true, json: () => Promise.resolve([]) }) // чекбоксы
    .mockResolvedValueOnce({ ok: true }); // delete

  const { result } = renderHook(() => useStreamForm(123, mockNavigate));

  await waitFor(() => expect(result.current.checkboxesData2).toEqual([]));
  console.log('Checkboxes loaded');

  await act(async () => {
    console.log('Calling deleteStream...');
    await result.current.deleteStream();
    console.log('deleteStream completed');
  });

  console.log('Fetch calls:', global.fetch.mock.calls.length);
  console.log('Alert calls:', alertMock.mock.calls);
  console.log('Navigate calls:', mockNavigate.mock.calls);

  expect(alertMock).toHaveBeenCalledWith('Поток успешно удален!');
  expect(mockNavigate).toHaveBeenCalledWith('/streams');
});

  it('should set error when network request fails', async () => {
    global.fetch.mockRejectedValueOnce(new Error('Network error'));

    const { result } = renderHook(() => useStreamForm(123, mockNavigate));

    // Ждем завершения начальной загрузки
    await waitFor(() => {
      expect(result.current.checkboxesData2).toEqual([]);
    });

    await act(async () => {
      await result.current.deleteStream();
    });

    expect(getCsrfConfigForFetch).toHaveBeenCalled();
    expect(result.current.error).toBe('Не удалось удалить поток.');
    expect(alertMock).not.toHaveBeenCalled();
    expect(mockNavigate).not.toHaveBeenCalled();
  });

  it('should set error when response is not ok', async () => {
    global.fetch.mockResolvedValueOnce({
      ok: false,
    });

    const { result } = renderHook(() => useStreamForm(123, mockNavigate));

    // Ждем завершения начальной загрузки
    await waitFor(() => {
      expect(result.current.checkboxesData2).toEqual([]);
    });

    await act(async () => {
      await result.current.deleteStream();
    });

    expect(getCsrfConfigForFetch).toHaveBeenCalled();
    expect(result.current.error).toBe('Не удалось удалить поток.');
    expect(alertMock).not.toHaveBeenCalled();
    expect(mockNavigate).not.toHaveBeenCalled();
  });

  it('should include CSRF headers in the request', async () => {
    global.fetch.mockResolvedValueOnce({
      ok: true,
    });

    const { result } = renderHook(() => useStreamForm(123, mockNavigate));

    // Ждем завершения начальной загрузки
    await waitFor(() => {
      expect(result.current.checkboxesData2).toEqual([]);
    });

    await act(async () => {
      await result.current.deleteStream();
    });

    expect(getCsrfConfigForFetch).toHaveBeenCalled();
    expect(global.fetch).toHaveBeenCalledWith(
      expect.any(String),
      expect.objectContaining({
        headers: { 'X-CSRF-Token': 'test-token' },
      })
    );
  });

  it('should use correct streamId in the URL', async () => {
    global.fetch.mockResolvedValueOnce({
      ok: true,
    });

    const testStreamId = 456;
    const { result } = renderHook(() => useStreamForm(testStreamId, mockNavigate));

    // Ждем завершения начальной загрузки
    await waitFor(() => {
      expect(result.current.checkboxesData2).toEqual([]);
    });

    await act(async () => {
      await result.current.deleteStream();
    });

    expect(getCsrfConfigForFetch).toHaveBeenCalled();
    expect(global.fetch).toHaveBeenCalledWith(
      `${backendHost}/api/v1/admin/stream/${testStreamId}`,
      expect.objectContaining({
        method: 'DELETE',
        headers: { 'X-CSRF-Token': 'test-token' },
      })
    );
  });
});
