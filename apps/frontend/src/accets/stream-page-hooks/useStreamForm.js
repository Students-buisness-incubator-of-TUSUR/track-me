const { useCallback, useEffect, useRef, useState, useMemo } = require('react');
const { getCsrfConfigForFetch } = require('../../utils/csrf-utils');
const useStreamForm = (streamId = null, navigate = () => {}) => {
  const [name, setName] = useState('');
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
  const errorRef = useRef(null);
  const [trackStartDate, setTrackStartDate] = useState('');

const [meetingsCount, setMeetingsCount] = useState('');
const [customMeetingsCount, setCustomMeetingsCount] = useState('');
const [showCustomInput, setShowCustomInput] = useState(false);

// Добавьте в начало хука useStreamForm
const meetingOptions = useMemo(() => [5, 10, 15, 20], []);
const handleCustomMeetingsCountChange = (e) => {
  const value = e.target.value;
  if (value === '' || (/^\d+$/.test(value) && Number(value) >= 1 && Number(value) <= 100)) {
    setCustomMeetingsCount(value);
  }
};

  useEffect(() => {
    if (error && errorRef.current) {
      errorRef.current.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }
  }, [error]);

  const fetchCheckboxesData = useCallback(async () => {
    try {
      const response = await fetch(`${backendHost}/api/v1/streams/nti-markets`, {
        credentials: 'include',
      });
      if (!response.ok) throw new Error('Network error');
      const data = await response.json();
      setCheckboxesData2(data);
    } catch (error) {
      setError('Не удалось загрузить данные для чекбоксов.');
    }
  }, [backendHost]);

  const fetchStreamImage = useCallback(async (id) => {
    try {
      const response = await fetch(`${backendHost}/api/v1/streams/${id}/image`, {
        credentials: 'include',
      });
      if (!response.ok) return null;
      const blob = await response.blob();
      return URL.createObjectURL(blob);
    } catch (error) {
      return null;
    }
  }, [backendHost]);

  const fetchStreamData = useCallback(async () => {
  if (!streamId) return;
  try {
    const response = await fetch(`${backendHost}/api/v1/admin/stream/${streamId}`, {
      credentials: 'include',
    });
    if (!response.ok) throw new Error('Network error');
    const result = await response.json();
    setName(result.name);
    const formatForDisplay = (dateStr) => {
      const [year, month, day] = dateStr.split('T')[0].split('-');
      return `${day}.${month}.${year}`;
    };
    setStartDate(formatForDisplay(result.startDate));
    setEndDate(formatForDisplay(result.endDate));
    setTrackStartDate(result.trackStartDate ? formatForDisplay(result.trackStartDate) : '');
    
    // Исправляем установку meetingsCount
    if (result.meetingsCount) {
      const meetingsCountValue = String(result.meetingsCount);
      // Проверяем, есть ли значение в предопределенных опциях
      if (meetingOptions.includes(Number(meetingsCountValue))) {
        setMeetingsCount(meetingsCountValue);
      } else {
        // Если значения нет в опциях, устанавливаем "custom" и заполняем customMeetingsCount
        setMeetingsCount('custom');
        setCustomMeetingsCount(meetingsCountValue);
        setShowCustomInput(true);
      }
    } else {
      setMeetingsCount('');
    }
    
    if (result.ntiMarkets && result.ntiMarkets.length > 0) {
      const selectedMarketIds = result.ntiMarkets.map((market) => market.id);
      setSelectedCheckboxes(selectedMarketIds);
    }
    const imageUrl = await fetchStreamImage(streamId);
    if (imageUrl) setImage(imageUrl);
  } catch (error) {
    setError('Не удалось загрузить данные потока.');
  }
}, [backendHost, streamId, fetchStreamImage, meetingOptions]);
const handleTrackStartDateChange = (e) => {
  let value = e.target.value.replace(/\D/g, '');
  if (value.length > 8) value = value.slice(0, 8);
  if (value.length > 4) {
    value = `${value.slice(0, 2)}.${value.slice(2, 4)}.${value.slice(4)}`;
  } else if (value.length > 2) {
    value = `${value.slice(0, 2)}.${value.slice(2)}`;
  }
  setTrackStartDate(value);
}
const handleMeetingsCountChange = (e) => {
  const value = e.target.value;
  setMeetingsCount(value);
  
  if (value === 'custom') {
    setShowCustomInput(true);
    setCustomMeetingsCount('');
  } else {
    setShowCustomInput(false);
    setCustomMeetingsCount('');
  }
};

  useEffect(() => {
    fetchCheckboxesData();
    if (streamId) fetchStreamData();
  }, [fetchCheckboxesData, fetchStreamData, streamId]);

  useEffect(() => {
    const handleClickOutside = (event) => {
      if (checkboxesRef.current && !checkboxesRef.current.contains(event.target)) {
        setShowCheckboxes2(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);
// В хуке добавьте автофокус при появлении поля ввода
useEffect(() => {
  if (showCustomInput) {
    // Автофокус на поле ввода при его появлении
    const input = document.querySelector('.create-stream-custom-input');
    if (input) {
      input.focus();
    }
  }
}, [showCustomInput]);
  const handleShowCheckboxes2 = () => setShowCheckboxes2(!showCheckboxes2);

  const handleCheckboxChange = (id) => {
    if (selectedCheckboxes.includes(id)) {
      setSelectedCheckboxes(selectedCheckboxes.filter((checkboxId) => checkboxId !== id));
    } else if (selectedCheckboxes.length < 3) {
      setSelectedCheckboxes([...selectedCheckboxes, id]);
    } else {
      alert('Можно выбрать не более трёх чекбоксов.');
    }
  };

  const handleNameChange = (e) => setName(e.target.value);

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
  if (!date) return true; // Пустая дата допустима для trackStartDate
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
      if (!file.type.match('image.*')) {
        setError('Пожалуйста, выберите файл изображения (JPEG, PNG, GIF)');
        return;
      }
      const reader = new FileReader();
      reader.onloadend = () => setImage(reader.result);
      reader.readAsDataURL(file);
      setImageFile(file);
    }
  };

  const validateForm = () => {
  if (!name || !startDate || !endDate || !trackStartDate || !meetingsCount) {
    setError('Пожалуйста, заполните все обязательные поля.');
    return false;
  }
  if (selectedCheckboxes.length === 0) {
    setError('Пожалуйста, укажите хотя бы один рынок НТИ.');
    return false;
  }
  if (!isValidDate(startDate) || !isValidDate(endDate) || !isValidDate(trackStartDate)) {
    setError('Некорректный формат даты. Используйте формат ДД.ММ.ГГГГ.');
    return false;
  }
  const [startDay, startMonth, startYear] = startDate.split('.').map(Number);
  const [endDay, endMonth, endYear] = endDate.split('.').map(Number);
  const [trackDay, trackMonth, trackYear] = trackStartDate.split('.').map(Number);
  const startDateObj = new Date(startYear, startMonth - 1, startDay);
  const endDateObj = new Date(endYear, endMonth - 1, endDay);
  const trackStartDateObj = new Date(trackYear, trackMonth - 1, trackDay);
  if (startDateObj > endDateObj) {
    setError('Дата начала должна быть раньше даты конца.');
    return false;
  }
  if (trackStartDateObj < startDateObj || trackStartDateObj > endDateObj) {
    setError('Дата начала трекшен-митинга должна быть между датой начала и конца потока.');
    return false;
  }
  if (!meetingsCount || (meetingsCount === 'custom' && !customMeetingsCount)) {
  setError('Пожалуйста, заполните количество встреч.');
  return false;
}

if (meetingsCount === 'custom' && (Number(customMeetingsCount) < 1 || Number(customMeetingsCount) > 100)) {
  setError('Количество встреч должно быть от 1 до 100.');
  return false;
}
  return true;
};

  const handleSubmit = async (isEditMode = false) => {
  setError('');
  if (!validateForm()) return;

  const finalMeetingsCount = meetingsCount === 'custom' ? Number(customMeetingsCount) : Number(meetingsCount);

const requestData = {
  name,
  startDate: formatDate(startDate),
  endDate: formatDate(endDate),
  trackStartDate: formatDate(trackStartDate),
  meetingsCount: finalMeetingsCount,
  ntiMarketIds: selectedCheckboxes,
  description: 'useless описание',
};

  try {
    const url = isEditMode
      ? `${backendHost}/api/v1/admin/stream/${streamId}`
      : `${backendHost}/api/v1/admin/stream`;
    const method = isEditMode ? 'PATCH' : 'POST';

    const response = await fetch(url, {
      method,
      headers: { 'Content-Type': 'application/json', ...getCsrfConfigForFetch() },
      credentials: 'include',
      body: JSON.stringify(requestData),
    });
    if (!response.ok) throw new Error('Network error');
    const streamResult = await response.json();

    if (imageFile) {
      const formData = new FormData();
      formData.append('file', imageFile);
      const imageResponse = await fetch(
        `${backendHost}/api/v1/streams/${isEditMode ? streamId : streamResult.id}/image`,
        {
          method: 'POST',
          headers: { ...getCsrfConfigForFetch() },
          credentials: 'include',
          body: formData,
        }
      );
      if (!imageResponse.ok) throw new Error('Image upload failed');
    } else if (!isEditMode) {
      const defaultImageResponse = await fetch('rabbit.png');
      if (!defaultImageResponse.ok) throw new Error('Default image fetch failed');
      const defaultImageBlob = await defaultImageResponse.blob();
      const formData = new FormData();
      formData.append('file', defaultImageBlob, 'rabbit.png');
      const imageResponse = await fetch(
        `${backendHost}/api/v1/streams/${streamResult.id}/image`,
        {
          method: 'POST',
          headers: { ...getCsrfConfigForFetch() },
          credentials: 'include',
          body: formData,
        }
      );
      if (!imageResponse.ok) throw new Error('Image upload failed');
    }

    alert(isEditMode ? 'Поток успешно обновлен!' : 'Поток успешно создан!');
    navigate('/streams');
  } catch (error) {
    setError(
      isEditMode
        ? 'Не удалось обновить поток или загрузить изображение.'
        : 'Не удалось создать поток или загрузить изображение.'
    );
  }
};

  return {
  name,
  startDate,
  endDate,
  trackStartDate,
  meetingsCount,
  customMeetingsCount,
  showCustomInput,
  setShowCustomInput, // Добавляем в возвращаемые значения
  handleMeetingsCountChange,
  handleCustomMeetingsCountChange,
  showCheckboxes2,
  error,
  checkboxesData2,
  selectedCheckboxes,
  image,
  checkboxesRef,
  errorRef,
  handleNameChange,
  handleStartDateChange,
  handleEndDateChange,
  handleTrackStartDateChange,
  handleShowCheckboxes2,
  handleCheckboxChange,
  handleImageUpload,
  handleSubmit,
};
};

export { useStreamForm };