import React, {useCallback, useEffect, useRef, useState} from 'react';
import './create-stream-page.css';
import {useNavigate} from "react-router-dom";
import axios from "axios";

export default function CreateStream() {
    const [name, setName] = useState('');
    const navigate = useNavigate();
    const [startDate, setStartDate] = useState('');
    const [endDate, setEndDate] = useState('');
    const [showCheckboxes2, setShowCheckboxes2] = useState(false);
    const [error, setError] = useState(null);
    const [checkboxesData2, setCheckboxesData2] = useState([]);
    const [selectedCheckboxes, setSelectedCheckboxes] = useState([]);
    const [image, setImage] = useState(null);
    const [imageFile, setImageFile] = useState(null);
    const backendHost = (process.env.REACT_APP_BACKEND_URI || 'http://localhost:8080') + '/backend';
    const checkboxesRef = useRef(null);

    // Фетчим данные для чекбоксов рынков как на stream-page.js (без проверки токена)
    const fetchCheckboxesData = useCallback(async () => {
        try {
            const response = await axios.get(`${backendHost}/api/v1/streams/nti-markets`, {
                withCredentials: true,
            });
            setCheckboxesData2(response.data);
        } catch (error) {
            setError("Не удалось загрузить данные для чекбоксов.");
        }
    }, [backendHost]);

    useEffect(() => {
        fetchCheckboxesData();
    }, [fetchCheckboxesData]);

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

    const handleShowCheckboxes2 = () => {
        setShowCheckboxes2(!showCheckboxes2);
    };

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

    const handleNameChange = (e) => {
        setName(e.target.value);
    };

    const handleStartDateChange = (e) => {
        let value = e.target.value.replace(/\D/g, '');
        if (value.length > 8) value = value.slice(0, 8);

        if (value.length > 4) {
            value = `${value.slice(0, 2)}.${value.slice(2, 4)}.${value.slice(4)}`;
        } else if (value.length > 2) {
            value = `${value.slice(0, 2)}.${value.slice(2)}`;
        }

        setStartDate(value);
    };

    const handleEndDateChange = (e) => {
        let value = e.target.value.replace(/\D/g, '');
        if (value.length > 8) value = value.slice(0, 8);

        if (value.length > 4) {
            value = `${value.slice(0, 2)}.${value.slice(2, 4)}.${value.slice(4)}`;
        } else if (value.length > 2) {
            value = `${value.slice(0, 2)}.${value.slice(2)}`;
        }

        setEndDate(value);
    };

    const isValidDate = (date) => {
        const [day, month, year] = date.split('.').map(Number);
        if (isNaN(day) || isNaN(month) || isNaN(year)) return false;
        if (month < 1 || month > 12) return false;
        if (day < 1 || day > 31) return false;
        const daysInMonth = new Date(year, month, 0).getDate();
        if (day > daysInMonth) return false;
        return true;
    };

    const formatDate = (date) => {
        const [day, month, year] = date.split('.').map(Number);
        return `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
    };

    const handleImageUpload = (e) => {
        const file = e.target.files[0];
        if (file) {
            const reader = new FileReader();
            reader.onloadend = () => {
                setImage(reader.result);
            };
            reader.readAsDataURL(file);
            setImageFile(file);
        }
    };

    // Переписанное создание потока — убран accessToken, юзер инфо есть в redux store (но серверу не посылается)
    const handleCreateButtonClick = async () => {
        setError('');

        if (!name || !startDate || !endDate) {
            setError('Пожалуйста, заполните все поля.');
            return;
        }

        if (!isValidDate(startDate) || !isValidDate(endDate)) {
            setError('Некорректный формат даты. Используйте формат ДД.ММ.ГГГГ.');
            return;
        }

        const [startDay, startMonth, startYear] = startDate.split('.').map(Number);
        const [endDay, endMonth, endYear] = endDate.split('.').map(Number);

        const startDateObj = new Date(startYear, startMonth - 1, startDay);
        const endDateObj = new Date(endYear, endMonth - 1, endDay);

        if (startDateObj > endDateObj) {
            setError('Дата начала должна быть раньше даты конца.');
            return;
        }

        const requestData = {
            name,
            startDate: formatDate(startDate),
            endDate: formatDate(endDate),
            ntiMarketIds: selectedCheckboxes,
            description: "useless описание",
            // Можно передавать user.id или user.name, если это согласовано с бекендом
        };

        try {
            // Создать поток
            const createStreamResponse = await axios.post(
                `${backendHost}/api/v1/admin/stream`,
                requestData,
                {
                    headers: {
                        "Content-Type": "application/json",
                    },
                    withCredentials: true,
                }
            );
            const streamResult = createStreamResponse.data;
            // Загрузка изображения
            if (imageFile) {
                const formData = new FormData();
                formData.append('file', imageFile);
                await axios.post(
                    `${backendHost}/api/v1/streams/${streamResult.id}/image`,
                    formData,
                    {
                        headers: {
                            "Content-Type": "multipart/form-data",
                        },
                        withCredentials: true,
                    }
                );
            } else {
                const defaultImageResponse = await fetch('rabbit.png');
                const defaultImageBlob = await defaultImageResponse.blob();
                const formData = new FormData();
                formData.append('file', defaultImageBlob, 'rabbit.png');
                await axios.post(
                    `${backendHost}/api/v1/streams/${streamResult.id}/image`,
                    formData,
                    {
                        headers: {
                            "Content-Type": "multipart/form-data",
                        },
                        withCredentials: true,
                    }
                );
            }
            alert('Поток успешно создан!');
            navigate("/streams");
        } catch (error) {
            setError('Не удалось создать поток или загрузить изображение. Пожалуйста, попробуйте снова.');
        }
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
                            <input
                                id="myInput"
                                className='create-stream-input'
                                placeholder='Текст названия'
                                value={name}
                                onChange={handleNameChange}
                            />
                            <input
                                id="myInput"
                                className='create-stream-input-date'
                                placeholder='__.__.____'
                                value={startDate}
                                onChange={handleStartDateChange}
                            />
                            <input
                                id="myInput"
                                className='create-stream-input-date'
                                placeholder='__.__.____'
                                value={endDate}
                                onChange={handleEndDateChange}
                            />
                        </div>
                    </div>
                    <div className="Stream-bb Stream-header-chosefrom-buttw">
                        <div className="Stream-header-chosefrom-butt2" ref={checkboxesRef}>
                            <div className="Stream-header-chosefrom-butt-cont"
                                 onClick={handleShowCheckboxes2}>
                                <b className="Stream-header-chosefrom-butt-label">Рынок</b>
                                <div className="Stream-header-chosefrom-butt-pic"></div>
                            </div>
                            {showCheckboxes2 && (
                                <div className="Stream-header-checkboxes">
                                    {checkboxesData2.map((item, index) => (
                                        <div key={item.id} className={`Stream-header-checkbox ${index < 5 ? 'first-row' : 'second-row'}`}>
                                            <input
                                                type="checkbox"
                                                checked={selectedCheckboxes.includes(item.id)}
                                                onChange={() => handleCheckboxChange(item.id)}
                                            />
                                            <label className="Stream-header-checkbox-label" htmlFor={`checkbox-${item.id}`}>
                                            {item.displayName || item.name}
                                            </label>
                                        </div>
                                    ))}
                                </div>
                            )}
                        </div>
                    </div>
                    {error && <p className="create-stream-error-message">{error}</p>}
                </div>
                <div className="create-stream-cont-right">
                    <div className="create-stream-input-pic"
                         onClick={() => document.getElementById('image-upload').click()}>
                        {image ? (
                            <img src={image} alt="Uploaded"
                                 className="create-stream-uploaded-image"/>
                        ) : (
                            <div className="create-stream-input-pic-placeholder"></div>
                        )}
                        <input
                            type="file"
                            id="image-upload"
                            accept="image/*"
                            style={{display: 'none'}}
                            onChange={handleImageUpload}
                        />
                    </div>
                    <button className='create-stream-input-button'
                            onClick={handleCreateButtonClick}>
                        Создать
                    </button>
                </div>
            </div>
        </div>
    );
}