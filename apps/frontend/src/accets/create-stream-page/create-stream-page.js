import React, { useState, useEffect, useCallback } from 'react';
import './create-stream-page.css';

export default function CreateStream() {
  const [startDate, setStartDate] = useState(''); // Состояние для даты начала
  const [endDate, setEndDate] = useState(''); // Состояние для даты конца
  const [showCheckboxes2, setShowCheckboxes2] = useState(false);
  const [error, setError] = useState(null); // Состояние для отслеживания ошибок
  const [checkboxesData2, setCheckboxesData2] = useState([]); // Состояние для данных чекбоксов
  const [selectedCheckboxes, setSelectedCheckboxes] = useState([]); // Состояние для выбранных чекбоксов
  const backendHost = process.env.REACT_APP_BACKEND_HOST || 'http://localhost:8080';

  const fetchCheckboxesData = useCallback(async () => {
    const token = localStorage.getItem("accessToken");
    if (!token) {
      setError("Ошибка: отсутствует токен авторизации. Выполните вход.");
      return;
    }
    try {
      const response = await fetch(`${backendHost}/api/v1/streams/nti-markets`, {
        method: 'GET',
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });
  
      if (!response.ok) {
        throw new Error('Ошибка при загрузке данных для чекбоксов');
      }
  
      const result = await response.json();
      const formattedData = result.map((item) => ({
        id: item.id,
        name: item.displayName,
        description: item.name,
      }));
      setCheckboxesData2(formattedData);
    } catch (error) {
      console.error('Ошибка при загрузке данных для чекбоксов:', error);
      setError('Не удалось загрузить данные для чекбоксов.');
    }
  }, [backendHost]); // Теперь эта функция будет пересоздаваться только при изменении `backendHost`
  
  useEffect(() => {
    fetchCheckboxesData();
  }, [fetchCheckboxesData]); // Теперь `fetchCheckboxesData` безопасно используется в зависимостях
  

  const handleShowCheckboxes2 = () => {
    setShowCheckboxes2(!showCheckboxes2);
  };

  // Функция для обработки выбора чекбокса
  const handleCheckboxChange = (id) => {
    if (selectedCheckboxes.includes(id)) {
      // Если чекбокс уже выбран, удаляем его из списка
      setSelectedCheckboxes(selectedCheckboxes.filter((checkboxId) => checkboxId !== id));
    } else {
      // Если чекбокс не выбран, добавляем его в список, если выбрано меньше трёх
      if (selectedCheckboxes.length < 3) {
        setSelectedCheckboxes([...selectedCheckboxes, id]);
      } else {
        alert('Можно выбрать не более трёх чекбоксов.');
      }
    }
  };

  // Функция для обработки ввода даты начала
  const handleStartDateChange = (e) => {
    let value = e.target.value.replace(/\D/g, ''); // Удаляем всё, кроме цифр
    if (value.length > 8) value = value.slice(0, 8); // Ограничиваем длину 8 символами

    // Форматируем значение в маску __.__.____
    if (value.length > 4) {
      value = `${value.slice(0, 2)}.${value.slice(2, 4)}.${value.slice(4)}`;
    } else if (value.length > 2) {
      value = `${value.slice(0, 2)}.${value.slice(2)}`;
    }

    setStartDate(value); // Обновляем состояние даты начала
  };

  // Функция для обработки ввода даты конца
  const handleEndDateChange = (e) => {
    let value = e.target.value.replace(/\D/g, ''); // Удаляем всё, кроме цифр
    if (value.length > 8) value = value.slice(0, 8); // Ограничиваем длину 8 символами

    // Форматируем значение в маску __.__.____
    if (value.length > 4) {
      value = `${value.slice(0, 2)}.${value.slice(2, 4)}.${value.slice(4)}`;
    } else if (value.length > 2) {
      value = `${value.slice(0, 2)}.${value.slice(2)}`;
    }

    setEndDate(value); // Обновляем состояние даты конца
  };

  // Функция для проверки корректности даты
  const isValidDate = (date) => {
    const [day, month, year] = date.split('.').map(Number);

    // Проверка на корректность дня, месяца и года
    if (isNaN(day) || isNaN(month) || isNaN(year)) return false;
    if (month < 1 || month > 12) return false;
    if (day < 1 || day > 31) return false;

    // Проверка на корректность дней в месяце
    const daysInMonth = new Date(year, month, 0).getDate();
    if (day > daysInMonth) return false;

    return true;
  };

  // Функция для обработки нажатия на кнопку
  const handleCreateButtonClick = () => {
    setError(''); // Сбрасываем ошибку

    // Проверка на заполненность полей
    if (!startDate || !endDate) {
      setError('Пожалуйста, заполните обе даты.');
      return;
    }

    // Проверка на корректность формата дат
    if (!isValidDate(startDate) || !isValidDate(endDate)) {
      setError('Некорректный формат даты. Используйте формат ДД.ММ.ГГГГ.');
      return;
    }

    // Преобразуем даты в объекты Date для сравнения
    const [startDay, startMonth, startYear] = startDate.split('.').map(Number);
    const [endDay, endMonth, endYear] = endDate.split('.').map(Number);

    const startDateObj = new Date(startYear, startMonth - 1, startDay);
    const endDateObj = new Date(endYear, endMonth - 1, endDay);

    // Проверка, что дата начала раньше даты конца
    if (startDateObj > endDateObj) {
      setError('Дата начала должна быть раньше даты конца.');
      return;
    }

    // Если всё в порядке, можно выполнить действие (например, отправить данные)
    alert('Поток успешно создан!');
  };

  return (
    <div className="create-stream">
      <div className="create-stream-cont">
        <div className="create-stream-cont-left">
          <label className="create-stream-title">Создание потока</label>
          <div className="create-stream-row">
            <div className="create-stream-col">
              <h1 className='create-stream-h1'>Название потока:</h1>
              <h1 className='create-stream-h1'>Дата начала:</h1>
              <h1 className='create-stream-h1'>Дата конца:</h1>
            </div>
            <div className="create-stream-col">
              <input className='create-stream-input' placeholder='Текст названия'></input>
              <input
                className='create-stream-input-date'
                placeholder='__.__.____'
                value={startDate}
                onChange={handleStartDateChange}
              />
              <input
                className='create-stream-input-date'
                placeholder='__.__.____'
                value={endDate}
                onChange={handleEndDateChange}
              />
            </div>
          </div>
          {error && <p className="create-stream-error-message">{error}</p>} {/* Отображение ошибки */}
        </div>
        <div className="create-stream-cont-right">
          <div className="create-stream-input-pic"></div>
          <button className='create-stream-input-button' onClick={handleCreateButtonClick}>
            Создать
          </button>
        </div>
      </div>
      <div className="Stream-b Stream-header-chosefrom-buttw">
        <div className="Stream-header-chosefrom-butt2">
          <div className="Stream-header-chosefrom-butt-cont" onClick={handleShowCheckboxes2}>
            <b className="Stream-header-chosefrom-butt-label">Рынок</b>
            <div className="Stream-header-chosefrom-butt-pic"></div>
          </div>
          {showCheckboxes2 && (
            <div className="Stream-header-checkboxes">
              {checkboxesData2.map((formattedData, index) => (
                <div key={formattedData.id} className={`Stream-header-checkbox ${index < 5 ? 'first-row' : 'second-row'}`}>
                  <input
                    type="checkbox"
                    class="custom-checkbox"
                    id={formattedData.id}
                    checked={selectedCheckboxes.includes(formattedData.id)}
                    onChange={() => handleCheckboxChange(formattedData.id)}
                    disabled={selectedCheckboxes.length >= 3 && !selectedCheckboxes.includes(formattedData.id)}
                  />
                  <label className='Stream-header-checkbox-label'>{formattedData.name}</label>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}