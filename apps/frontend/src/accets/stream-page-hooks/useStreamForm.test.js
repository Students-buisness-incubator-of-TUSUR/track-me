const { renderHook, act, waitFor } = require('@testing-library/react');
const { useStreamForm } = require('./useStreamForm');

beforeEach(() => {
  jest.clearAllMocks();
  global.fetch = jest.fn();
});
// Добавим в начало файла, после других импортов
const mockNavigate = jest.fn();

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
      result.current.handleStartDateChange({ target: { value: '01022025' } });
    });
    expect(result.current.startDate).toBe('01.02.2025');
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

it('валидирует форму корректно', () => {
  const { result } = renderHook(() => useStreamForm(null, mockNavigate));

  act(() => {
    result.current.handleNameChange({ target: { value: 'Stream' } });
    result.current.handleStartDateChange({ target: { value: '01012024' } });
    result.current.handleEndDateChange({ target: { value: '02012024' } });
    result.current.handleCheckboxChange(1);
  });

  expect(result.current.name).toBe('Stream');
  expect(result.current.startDate).toBe('01.01.2024');
  expect(result.current.endDate).toBe('02.01.2024');
});


// Тест на дату начала позже даты окончания
it('не валидирует, если дата начала позже даты окончания', () => {
  const { result } = renderHook(() => useStreamForm(null, mockNavigate));
  act(() => {
    result.current.handleNameChange({ target: { value: 'Stream' } });
    result.current.handleStartDateChange({ target: { value: '03012024' } });
    result.current.handleEndDateChange({ target: { value: '02012024' } });
    result.current.handleTrackStartDateChange({ target: { value: '02012024' } });
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
    result.current.handleStartDateChange({ target: { value: '01012024' } });
    result.current.handleEndDateChange({ target: { value: '02012024' } });
    result.current.handleTrackStartDateChange({ target: { value: '01012024' } });
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
    result.current.handleStartDateChange({ target: { value: '31022024' } }); // 31.02 — неверно
    result.current.handleEndDateChange({ target: { value: '01032024' } });
    result.current.handleTrackStartDateChange({ target: { value: '01022024' } });
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
    result.current.handleStartDateChange({ target: { value: '01012023' } });
    result.current.handleEndDateChange({ target: { value: '02012023' } });
    result.current.handleTrackStartDateChange({ target: { value: '01012023' } });
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
    result.current.handleStartDateChange({ target: { value: '01012023' } });
    result.current.handleEndDateChange({ target: { value: '02012023' } });
    result.current.handleTrackStartDateChange({ target: { value: '01012023' } });
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
      result.current.handleStartDateChange({ target: { value: '01022025' } });
    });
    expect(result.current.startDate).toBe('01.02.2025');
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
      result.current.handleStartDateChange({ target: { value: '01012024' } });
      result.current.handleEndDateChange({ target: { value: '02012024' } });
      result.current.handleCheckboxChange(1);
    });

    expect(result.current.name).toBe('Stream');
    expect(result.current.startDate).toBe('01.01.2024');
    expect(result.current.endDate).toBe('02.01.2024');
  });

  

 
  it('не валидирует, если поля не заполнены', () => {
    const { result } = renderHook(() => useStreamForm(null, mockNavigate));

    act(() => {
      result.current.handleSubmit();
    });

    expect(result.current.error).toBe('Пожалуйста, заполните все обязательные поля.');
  });
it('should handle date formatting for different input lengths', () => {
    const { result } = renderHook(() => useStreamForm(null, mockNavigate));
    
    // Test partial date (day only)
    act(() => {
      result.current.handleStartDateChange({ target: { value: '01' } });
    });
    expect(result.current.startDate).toBe('01');
    
    // Test day and month
    act(() => {
      result.current.handleStartDateChange({ target: { value: '0102' } });
    });
    expect(result.current.startDate).toBe('01.02');
    
    // Test full date
    act(() => {
      result.current.handleStartDateChange({ target: { value: '01022023' } });
    });
    expect(result.current.startDate).toBe('01.02.2023');
    
    // Test with non-digit characters
    act(() => {
      result.current.handleStartDateChange({ target: { value: '01/02/2023' } });
    });
    expect(result.current.startDate).toBe('01.02.2023');
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
    result.current.handleStartDateChange({ target: { value: '01012023' } });
    result.current.handleEndDateChange({ target: { value: '02012023' } });
    result.current.handleTrackStartDateChange({ target: { value: '01012023' } });
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
    expect(result.current.startDate).toBe('01.07.2025');
    expect(result.current.endDate).toBe('10.07.2025');
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
    result.current.handleStartDateChange({ target: { value: '01012024' } });
    result.current.handleEndDateChange({ target: { value: '02012024' } });
    result.current.handleTrackStartDateChange({ target: { value: '01012024' } });
    result.current.handleMeetingsCountChange({ target: { value: '5' } });
    result.current.handleCheckboxChange(1);
  });

  await act(async () => {
    await result.current.handleSubmit(true);
  });

  expect(alertMock).toHaveBeenCalledWith('Поток успешно обновлен!');
  expect(mockNavigate).toHaveBeenCalledWith('/streams');
});

  it('создает поток и загружает дефолтное изображение, если imageFile не указан', async () => {
    global.fetch
      .mockResolvedValueOnce({ ok: true, json: () => Promise.resolve({ id: 999 }) }) // POST
      .mockResolvedValueOnce({ ok: true, blob: () => Promise.resolve(new Blob()) }) // rabbit.png
      .mockResolvedValueOnce({ ok: true }); // image upload
    
    const { result } = renderHook(() => useStreamForm(null, mockNavigate));

    act(() => {
      result.current.handleNameChange({ target: { value: 'Stream' } });
      result.current.handleStartDateChange({ target: { value: '01012024' } });
      result.current.handleEndDateChange({ target: { value: '02012024' } });
      result.current.handleTrackStartDateChange({ target: { value: '01012024' } });
      result.current.handleMeetingsCountChange({ target: { value: '5' } });
      result.current.handleCheckboxChange(1);
    });
    
    await act(async () => {
      await result.current.handleSubmit(false);
    });
    
    expect(alertMock).toHaveBeenCalledWith('Поток успешно создан!');
    expect(mockNavigate).toHaveBeenCalledWith('/streams');
  });

  it('выводит ошибку, если не удалось создать поток', async () => {
    global.fetch
      .mockRejectedValueOnce(new Error('Network error')); // POST
    
    const { result } = renderHook(() => useStreamForm(null, mockNavigate));

    act(() => {
      result.current.handleNameChange({ target: { value: 'Stream' } });
      result.current.handleStartDateChange({ target: { value: '01012024' } });
      result.current.handleEndDateChange({ target: { value: '02012024' } });
      result.current.handleTrackStartDateChange({ target: { value: '01012024' } });
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
      result.current.handleStartDateChange({ target: { value: '01012024' } });
      result.current.handleEndDateChange({ target: { value: '02012024' } });
      result.current.handleTrackStartDateChange({ target: { value: '01012024' } });
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
      result.current.handleStartDateChange({ target: { value: '02012024' } }); // 02.01.2024
      result.current.handleEndDateChange({ target: { value: '03012024' } }); // 03.01.2024
      result.current.handleTrackStartDateChange({ target: { value: '01012024' } }); // 01.01.2024 (before start)
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
      result.current.handleStartDateChange({ target: { value: '01012024' } }); // 01.01.2024
      result.current.handleEndDateChange({ target: { value: '02012024' } }); // 02.01.2024
      result.current.handleTrackStartDateChange({ target: { value: '03012024' } }); // 03.01.2024 (after end)
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

describe('Date formatting for partial input', () => {
  it('should format partial date input correctly', () => {
    const { result } = renderHook(() => useStreamForm(null, mockNavigate));
    
    // Test for lines 83-84 - partial date formatting
    act(() => {
      result.current.handleStartDateChange({ target: { value: '01' } });
    });
    expect(result.current.startDate).toBe('01');

    act(() => {
      result.current.handleStartDateChange({ target: { value: '0102' } });
    });
    expect(result.current.startDate).toBe('01.02');

    act(() => {
      result.current.handleStartDateChange({ target: { value: '010220' } });
    });
    expect(result.current.startDate).toBe('01.02.20');

    // Same for track start date
    act(() => {
      result.current.handleTrackStartDateChange({ target: { value: '03' } });
    });
    expect(result.current.trackStartDate).toBe('03');

    act(() => {
      result.current.handleTrackStartDateChange({ target: { value: '0304' } });
    });
    expect(result.current.trackStartDate).toBe('03.04');
  });
});