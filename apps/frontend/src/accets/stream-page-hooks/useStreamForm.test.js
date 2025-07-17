const { renderHook, act, waitFor } = require('@testing-library/react');
const { useStreamForm } = require('./useStreamForm');

beforeEach(() => {
  jest.clearAllMocks();
  global.fetch = jest.fn();
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


it('не валидирует, если дата начала позже даты окончания', () => {
  const { result } = renderHook(() => useStreamForm(null, mockNavigate));
  act(() => {
    result.current.handleNameChange({ target: { value: 'Stream' } });
    result.current.handleStartDateChange({ target: { value: '03012024' } });
    result.current.handleEndDateChange({ target: { value: '02012024' } });
    result.current.handleCheckboxChange(1);
  });

  act(() => {
    result.current.handleSubmit();
  });

  expect(result.current.error).toBe('Дата начала должна быть раньше даты конца.');
});

it('не валидирует, если не выбрано ни одного чекбокса', () => {
  const { result } = renderHook(() => useStreamForm(null, mockNavigate));
  act(() => {
    result.current.handleNameChange({ target: { value: 'Stream' } });
    result.current.handleStartDateChange({ target: { value: '01012024' } });
    result.current.handleEndDateChange({ target: { value: '02012024' } });
  });

  act(() => {
    result.current.handleSubmit();
  });

  expect(result.current.error).toBe('Пожалуйста, укажите хотя бы один рынок НТИ.');
});

it('не валидирует, если поля не заполнены', () => {
  const { result } = renderHook(() => useStreamForm(null, mockNavigate));

  act(() => {
    result.current.handleSubmit();
  });

  expect(result.current.error).toBe('Пожалуйста, заполните все обязательные поля.');
});
it('should handle submission errors', async () => {
    global.fetch.mockImplementation(() => Promise.reject(new Error('Network error')));
    
    const { result } = renderHook(() => useStreamForm(null, mockNavigate));
    
    act(() => {
      result.current.handleNameChange({ target: { value: 'Test Stream' } });
      result.current.handleStartDateChange({ target: { value: '01012023' } });
      result.current.handleEndDateChange({ target: { value: '02012023' } });
      result.current.handleCheckboxChange(1);
    });
    
    await act(async () => {
      await result.current.handleSubmit(false);
    });
    
    expect(result.current.error).toBe('Не удалось создать поток или загрузить изображение.');
  });
  it('should handle checkbox change - adding and removing', () => {
    const { result } = renderHook(() => useStreamForm(null, mockNavigate));
    
    // Test adding checkbox
    act(() => {
      result.current.handleCheckboxChange(1);
    });
    expect(result.current.selectedCheckboxes).toEqual([1]);
    
    // Test adding second checkbox
    act(() => {
      result.current.handleCheckboxChange(2);
    });
    expect(result.current.selectedCheckboxes).toEqual([1, 2]);
    
    // Test removing checkbox
    act(() => {
      result.current.handleCheckboxChange(1);
    });
    expect(result.current.selectedCheckboxes).toEqual([2]);
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

  it('не валидирует, если дата начала позже даты окончания', () => {
    const { result } = renderHook(() => useStreamForm(null, mockNavigate));
    act(() => {
      result.current.handleNameChange({ target: { value: 'Stream' } });
      result.current.handleStartDateChange({ target: { value: '03012024' } });
      result.current.handleEndDateChange({ target: { value: '02012024' } });
      result.current.handleCheckboxChange(1);
    });

    act(() => {
      result.current.handleSubmit();
    });

    expect(result.current.error).toBe('Дата начала должна быть раньше даты конца.');
  });

  it('не валидирует, если не выбрано ни одного чекбокса', () => {
    const { result } = renderHook(() => useStreamForm(null, mockNavigate));
    act(() => {
      result.current.handleNameChange({ target: { value: 'Stream' } });
      result.current.handleStartDateChange({ target: { value: '01012024' } });
      result.current.handleEndDateChange({ target: { value: '02012024' } });
    });

    act(() => {
      result.current.handleSubmit();
    });

    expect(result.current.error).toBe('Пожалуйста, укажите хотя бы один рынок НТИ.');
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
  it('should handle different error messages for create and edit modes', async () => {
    global.fetch.mockImplementation(() => Promise.reject(new Error('Network error')));
    
    // Test create mode
    const { result, rerender } = renderHook((props) => useStreamForm(props.streamId, mockNavigate), {
      initialProps: { streamId: null }
    });
    
    act(() => {
      result.current.handleNameChange({ target: { value: 'Test Stream' } });
      result.current.handleStartDateChange({ target: { value: '01012023' } });
      result.current.handleEndDateChange({ target: { value: '02012023' } });
      result.current.handleCheckboxChange(1);
    });
    
    await act(async () => {
      await result.current.handleSubmit(false);
    });
    
    expect(result.current.error).toBe('Не удалось создать поток или загрузить изображение.');
    
    // Test edit mode
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
it('не валидирует неправильную дату', () => {
  const { result } = renderHook(() => useStreamForm(null, mockNavigate));

  act(() => {
    result.current.handleNameChange({ target: { value: 'Stream' } });
    result.current.handleStartDateChange({ target: { value: '31022024' } }); // 31.02 — неверно
    result.current.handleEndDateChange({ target: { value: '01032024' } });
    result.current.handleCheckboxChange(1);
  });

  act(() => {
    result.current.handleSubmit();
  });

  expect(result.current.error).toBe('Некорректный формат даты. Используйте формат ДД.ММ.ГГГГ.');
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

  afterEach(() => {
    alertMock.mockClear();
  });

  

  it('успешно обновляет поток без загрузки нового изображения', async () => {
    const { result } = renderHook(() => useStreamForm(123, mockNavigate));

    act(() => {
      result.current.handleNameChange({ target: { value: 'Stream Edit' } });
      result.current.handleStartDateChange({ target: { value: '01012024' } });
      result.current.handleEndDateChange({ target: { value: '02012024' } });
      result.current.handleCheckboxChange(1);
      // imageFile не установлен (null)
      result.current.imageFile = null;
    });

    // Мокаем PATCH /admin/stream/123
    global.fetch.mockResolvedValueOnce({
      ok: true,
      json: async () => ({ id: 123 }),
    });

    await act(async () => {
      await result.current.handleSubmit(true);
    });

    expect(alertMock).toHaveBeenCalledWith('Поток успешно обновлен!');
    expect(mockNavigate).toHaveBeenCalledWith('/streams');
  });

  it('создает поток и загружает дефолтное изображение, если imageFile не указан', async () => {
    const { result } = renderHook(() => useStreamForm(null, mockNavigate));

    act(() => {
      result.current.handleNameChange({ target: { value: 'Stream' } });
      result.current.handleStartDateChange({ target: { value: '01012024' } });
      result.current.handleEndDateChange({ target: { value: '02012024' } });
      result.current.handleCheckboxChange(1);
      result.current.imageFile = null;
    });

    // Мокаем создание потока
    global.fetch
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({ id: 999 }),
      })
      // Мокаем fetch('rabbit.png') для дефолтного изображения
      .mockResolvedValueOnce({
        ok: true,
        blob: async () => new Blob(['dummy']),
      })
      // Мокаем загрузку дефолтного изображения
      .mockResolvedValueOnce({
        ok: true,
      });

    await act(async () => {
      await result.current.handleSubmit(false);
    });

    expect(alertMock).toHaveBeenCalledWith('Поток успешно создан!');
    expect(mockNavigate).toHaveBeenCalledWith('/streams');
  });

  it('выводит ошибку, если не удалось создать поток', async () => {
    const { result } = renderHook(() => useStreamForm(null, mockNavigate));

    act(() => {
      result.current.handleNameChange({ target: { value: 'Stream' } });
      result.current.handleStartDateChange({ target: { value: '01012024' } });
      result.current.handleEndDateChange({ target: { value: '02012024' } });
      result.current.handleCheckboxChange(1);
      result.current.imageFile = null;
    });

    // fetch отклоняется — имитируем ошибку
    global.fetch.mockRejectedValueOnce(new Error('Network error'));

    await act(async () => {
      await result.current.handleSubmit(false);
    });

    expect(result.current.error).toBe('Не удалось создать поток или загрузить изображение.');
    expect(alertMock).not.toHaveBeenCalled();
  });

  it('выводит ошибку, если загрузка изображения не удалась', async () => {
    const { result } = renderHook(() => useStreamForm(null, mockNavigate));
    const mockFile = new File(['dummy'], 'image.png', { type: 'image/png' });

    act(() => {
      result.current.handleNameChange({ target: { value: 'Stream' } });
      result.current.handleStartDateChange({ target: { value: '01012024' } });
      result.current.handleEndDateChange({ target: { value: '02012024' } });
      result.current.handleCheckboxChange(1);
      result.current.imageFile = mockFile;
    });

    // fetch для создания потока успешный
    global.fetch
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({ id: 123 }),
      })
      // fetch для загрузки изображения возвращает ошибку
      .mockResolvedValueOnce({
        ok: false,
      });

    await act(async () => {
      await result.current.handleSubmit(false);
    });

    expect(result.current.error).toBe('Не удалось создать поток или загрузить изображение.');
    expect(alertMock).not.toHaveBeenCalled();
  });
});

});
