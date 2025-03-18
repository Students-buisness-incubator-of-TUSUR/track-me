import React, { useState, useEffect,useRef, useCallback } from 'react';
import './create-stream-page.css';
import { useParams } from 'react-router-dom';

export default function EditStream() {
  const { id } = useParams();
  const [name, setName] = useState('');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [showCheckboxes2, setShowCheckboxes2] = useState(false);
  const [error, setError] = useState(null);
  const [checkboxesData2, setCheckboxesData2] = useState([]);
  const [selectedCheckboxes, setSelectedCheckboxes] = useState([]);
    const [image, setImage] = useState(null);
    // eslint-disable-next-line
    const [imageFile, setImageFile] = useState(null);
  const backendHost = process.env.REACT_APP_BACKEND_HOST || 'http://localhost:8080';
  const checkboxesRef = useRef(null);

  const fetchStreamData = useCallback(async () => {
    const token = localStorage.getItem("accessToken");
    if (!token) {
      setError("Ошибка: отсутствует токен авторизации. Выполните вход.");
      return;
    }
    try {
      const response = await fetch(`${backendHost}/api/v1/admin/stream/${id}`, {
        method: 'GET',
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });

      if (!response.ok) {
        throw new Error('Ошибка при загрузке данных потока');
      }

      const result = await response.json();
      setName(result.name);
      setStartDate(new Date(result.startDate).toISOString().split('T')[0]);
      setEndDate(new Date(result.endDate).toISOString().split('T')[0]);

      if (result.ntiMarkets && result.ntiMarkets.length > 0) {
        const selectedMarketIds = result.ntiMarkets.map((market) => market.id);
        setSelectedCheckboxes(selectedMarketIds);
      }
    } catch (error) {
      console.error('Ошибка при загрузке данных потока:', error);
      setError('Не удалось загрузить данные потока.');
    }
  }, [backendHost, id]);

  const fetchNtiMarkets = useCallback(async () => {
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
        throw new Error('Ошибка при загрузке рынков НТИ');
      }

      const result = await response.json();
      const formattedMarkets = result.map((market) => ({
        id: market.id,
        name: market.displayName,
      }));
      setCheckboxesData2(formattedMarkets);
    } catch (error) {
      console.error('Ошибка при загрузке рынков НТИ:', error);
      setError('Не удалось загрузить рынки НТИ.');
    }
  }, [backendHost]);
  useEffect(() => {
    const handleClickOutside = (event) => {
      if (checkboxesRef.current && !checkboxesRef.current.contains(event.target)) {
        setShowCheckboxes2(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, []);

  useEffect(() => {
    fetchStreamData();
    fetchNtiMarkets();
  }, [fetchStreamData, fetchNtiMarkets]);

  const handleNameChange = (e) => setName(e.target.value);
  const handleStartDateChange = (e) => setStartDate(e.target.value);
  const handleEndDateChange = (e) => setEndDate(e.target.value);

  const handleImageUpload = (e) => {
    const file = e.target.files[0];
    if (file && file.type.startsWith('image/')) {
      const reader = new FileReader();
      reader.onloadend = () => setImage(reader.result);
      reader.readAsDataURL(file);
      setImageFile(file);
    } else {
      setError('Пожалуйста, выберите файл изображения.');
    }
  };

  const handleShowCheckboxes2 = () => setShowCheckboxes2(!showCheckboxes2);

  const handleCheckboxChange = (id) => {
    if (selectedCheckboxes.includes(id)) {
      setSelectedCheckboxes(selectedCheckboxes.filter((checkboxId) => checkboxId !== id));
    } else {
      if (selectedCheckboxes.length < 3) {
        setSelectedCheckboxes([...selectedCheckboxes, id]);
      } else {
        alert('Можно выбрать не более трёх чекбоксов.');
      }
    }
  };

  const handleUpdateButtonClick = async () => {
    setError('');

    if (!name || !startDate || !endDate) {
      setError('Пожалуйста, заполните все поля.');
      return;
    }

    const requestData = {
      name,
      endDate,
      ntiMarketIds: selectedCheckboxes,
      description: "useless описание",
      active: true
    };

    try {
      const token = localStorage.getItem("accessToken");
      if (!token) {
        setError("Ошибка: отсутствует токен авторизации. Выполните вход.");
        return;
      }

      const updateStreamResponse = await fetch(`${backendHost}/api/v1/admin/stream/${id}`, {
        method: 'PATCH',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify(requestData),
      });

      if (!updateStreamResponse.ok) {
        throw new Error('Ошибка при обновлении потока');
      }
      

      alert('Поток успешно обновлен!');
    } catch (error) {
      console.error('Ошибка:', error);
      setError('Не удалось обновить поток или загрузить изображение. Пожалуйста, попробуйте снова.');
    }
  };

  return (
    <div className="create-stream">
      <div className="create-stream-cont">
        <div className="create-stream-cont-left">
          <label className="create-stream-title">Редактирование потока</label>
          <div className="create-stream-row">
            <div className="create-stream-col">
              <h1 className='create-stream-h1'>Название потока:</h1>
              <h1 className='create-stream-h1'>Дата начала:</h1>
              <h1 className='create-stream-h1'>Дата конца:</h1>
            </div>
            <div className="create-stream-col">
              <input
                className='create-stream-input'
                placeholder='Текст названия'
                value={name}
                onChange={handleNameChange}
              />
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
          {error && <p className="create-stream-error-message">{error}</p>}
        </div>
        <div className="create-stream-cont-right">
          <div className="create-stream-input-pic" onClick={() => document.getElementById('image-upload').click()}>
            {image ? (
              <img src={image} alt="Uploaded" className="create-stream-uploaded-image" />
            ) : (
              <div className="create-stream-input-pic-placeholder"></div>
            )}
            <input
              type="file"
              id="image-upload"
              accept="image/*"
              style={{ display: 'none' }}
              onChange={handleImageUpload}
            />
          </div>
          <button className='create-stream-input-button' onClick={handleUpdateButtonClick}>
            Обновить
          </button>
        </div>
      </div>
      <div className="Stream-b Stream-header-chosefrom-buttw">
        <div className="Stream-header-chosefrom-butt2" ref={checkboxesRef}>
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
                    className="custom-checkbox"
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