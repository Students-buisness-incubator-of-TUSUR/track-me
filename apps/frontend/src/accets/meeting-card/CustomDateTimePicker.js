// CustomDateTimePicker.jsx
import React, { useState, useRef, useEffect } from 'react';
import './CustomDateTimePicker.css';

export const formatTimeToInput = (time) => {
    if (!time) return '12:00';
    const [hours = '12', minutes = '00'] = time.split(':');
    return `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}`;
};

const CustomDateTimePicker = ({ value,  onChange = () => {}, min, max, disabled }) => {
    const [showPicker, setShowPicker] = useState(false);
    const [selectedDate, setSelectedDate] = useState(value ? value.split('T')[0] : '');
    const [selectedTime, setSelectedTime] = useState(value ? (value.split('T')[1] || '12:00').slice(0, 5) : '12:00');
    const pickerRef = useRef(null);

    // Функция для правильного форматирования даты в YYYY-MM-DD
    const formatDateToInput = (date) => {
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        return `${year}-${month}-${day}`;
    };

    // Функция для получения даты без временной зоны
    const getLocalDateString = (date) => {
        return formatDateToInput(date);
    };

    useEffect(() => {
        if (value) {
            const [datePart, timePart = '12:00'] = value.split('T');
            setSelectedDate(datePart);
            setSelectedTime(timePart.slice(0, 5));
        }
    }, [value]);

    useEffect(() => {
        const handleClickOutside = (event) => {
            if (pickerRef.current && !pickerRef.current.contains(event.target)) {
                setShowPicker(false);
            }
        };

        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    const formatDisplay = () => {
        if (!selectedDate) return 'Выберите дату';
        
        try {
            const [year, month, day] = selectedDate.split('-').map(Number);
            const date = new Date(year, month - 1, day);
            return `${date.toLocaleDateString('ru-RU', {
                day: 'numeric',
                month: 'long',
                year: 'numeric'
            })} ${selectedTime}`;
        } catch (e) {
            return selectedDate;
        }
    };

    const handleDayClick = (dateString) => {
        setSelectedDate(dateString);
        onChange(`${dateString}T${selectedTime || '12:00'}`);
        setTimeout(() => setShowPicker(false), 300);
    };

    const handleTimeChange = (event) => {
        const timeValue = formatTimeToInput(event.target.value);
        setSelectedTime(timeValue);
        if (selectedDate) {
            onChange(`${selectedDate}T${timeValue}`);
        }
    };

    // Генерация дней месяца для календаря
    const generateCalendarDays = () => {
        if (!selectedDate) return [];
        
        const [year, month] = selectedDate.split('-').map(Number);
        const dateMonth = month - 1; // JS месяцы 0-11
        
        // Первый день месяца
        const firstDay = new Date(year, dateMonth, 1);
        // Последний день месяца
        const lastDay = new Date(year, dateMonth + 1, 0);
        // День недели первого дня (0-6, где 0 - воскресенье)
        const firstDayOfWeek = firstDay.getDay();
        // Количество дней в месяце
        const daysInMonth = lastDay.getDate();
        
        const days = [];
        
        // Пустые дни перед первым числом (понедельник - первый день недели)
        for (let i = 0; i < (firstDayOfWeek === 0 ? 6 : firstDayOfWeek - 1); i++) {
            days.push(null);
        }
        
        // Сегодняшняя дата в локальном формате
        const today = new Date();
        const todayString = getLocalDateString(today);
        
        // Дни месяца
        for (let i = 1; i <= daysInMonth; i++) {
            const dayDate = new Date(year, dateMonth, i);
            const dateString = getLocalDateString(dayDate);
            const isToday = dateString === todayString;
            const isSelected = dateString === selectedDate;
            
            // Проверяем ограничения min/max
            let isDisabled = false;
            
            if (min) {
                const minDate = min.split('T')[0];
                if (dateString < minDate) isDisabled = true;
            }
            
            if (max) {
                const maxDate = max.split('T')[0];
                if (dateString > maxDate) isDisabled = true;
            }
            
            days.push({
                date: i,
                dateString,
                isToday,
                isSelected,
                isDisabled
            });
        }
        
        return days;
    };

    const getMonthName = () => {
        if (!selectedDate) return '';
        const [year, month] = selectedDate.split('-').map(Number);
        const date = new Date(year, month - 1, 1);
        return date.toLocaleDateString('ru-RU', {
            month: 'long',
            year: 'numeric'
        });
    };

    const navigateMonth = (direction) => {
        if (!selectedDate) return;
        
        const [year, month] = selectedDate.split('-').map(Number);
        const newDate = new Date(year, month - 1 + direction, 1);
        setSelectedDate(getLocalDateString(newDate));
    };

    const getDayNames = () => {
        return ['Пн', 'Вт', 'Ср', 'Чт', 'Пт', 'Сб', 'Вс'];
    };

    // Быстрое создание сегодняшней даты
    const getTodayString = () => {
        return getLocalDateString(new Date());
    };

    // Быстрое создание завтрашней даты
    const getTomorrowString = () => {
        const tomorrow = new Date();
        tomorrow.setDate(tomorrow.getDate() + 1);
        return getLocalDateString(tomorrow);
    };

    return (
        <div className={`custom-datetime-wrapper ${showPicker ? 'open' : ''}`} ref={pickerRef}>
            <div 
    className={`custom-datetime-display ${disabled ? 'disabled' : ''}`}
    onClick={() => !disabled && setShowPicker(!showPicker)}
    onKeyDown={(e) => {
        if (!disabled && (e.key === 'Enter' || e.key === ' ' || e.key === 'Spacebar')) {
            e.preventDefault();
            setShowPicker(!showPicker);
        }
    }}
    tabIndex={disabled ? -1 : 0}
    role="button"
    aria-haspopup="dialog"
    aria-expanded={showPicker}
    aria-label={`Выбрать дату. Текущая дата: ${selectedDate ? formatDisplay() : 'не выбрана'}`}
>
    <span className="display-text">{formatDisplay()}</span>
    <span className="custom-datetime-icon">
        {showPicker ? '▲' : '▼'}
    </span>
</div>
            
            {showPicker && !disabled && (
                <div className="custom-datetime-picker">
                    <div className="picker-header">
                        <button 
                            className="nav-button prev" 
                            onClick={() => navigateMonth(-1)}
                            aria-label="Предыдущий месяц"
                        >
                            ‹
                        </button>
                        <span className="month-name">{getMonthName()}</span>
                        <button 
                            className="nav-button next" 
                            onClick={() => navigateMonth(1)}
                            aria-label="Следующий месяц"
                        >
                            ›
                        </button>
                        <button 
                            className="close-button" 
                            onClick={() => setShowPicker(false)}
                            aria-label="Закрыть"
                        >
                            ×
                        </button>
                    </div>
                    
                    <div className="calendar-grid">
                        {/* Дни недели */}
                        <div className="weekdays">
                            {getDayNames().map((day, index) => (
                                <div key={index} className="weekday">{day}</div>
                            ))}
                        </div>
                        
                        {/* Дни месяца */}
                        <div className="calendar-days">
                            {generateCalendarDays().map((day, index) => {
                                if (day === null) {
                                    return <div key={index} className="calendar-day empty"></div>;
                                }
                                
                                return (
                                    <div
    key={index}
    className={`calendar-day ${day.isToday ? 'today' : ''} ${day.isSelected ? 'selected' : ''} ${day.isDisabled ? 'disabled' : ''}`}
    onClick={() => !day.isDisabled && handleDayClick(day.dateString)}
    onKeyDown={(e) => {
        if (!day.isDisabled && (e.key === 'Enter' || e.key === ' ' || e.key === 'Spacebar')) {
            e.preventDefault();
            handleDayClick(day.dateString);
        }
    }}
    tabIndex={day.isDisabled ? -1 : 0}
    role="button"
    aria-label={`${day.date} ${getMonthName()} ${day.isSelected ? ', выбрано' : ''} ${day.isToday ? ', сегодня' : ''} ${day.isDisabled ? ', недоступно' : ''}`}
    aria-disabled={day.isDisabled}
    title={day.isDisabled ? 'Дата недоступна' : `Выбрать ${day.date}`}
>
    <span className="day-number">{day.date}</span>
    {day.isToday && <span className="today-indicator"></span>}
</div>
                                );
                            })}
                        </div>
                    </div>

                    <div className="time-selector">
                        <span className="time-label">Выберите время</span>
                        <div className="time-picker">
                            <select
                                aria-label="Час"
                                value={selectedTime.split(':')[0]}
                                onChange={(e) => handleTimeChange({ target: { value: `${e.target.value}:${selectedTime.split(':')[1]}` } })}
                                className="time-select"
                            >
                                {Array.from({ length: 24 }, (_, hour) => {
                                    const hourValue = String(hour).padStart(2, '0');
                                    return <option key={hourValue} value={hourValue}>{hourValue}</option>;
                                })}
                            </select>
                            <span className="time-separator">:</span>
                            <select
                                aria-label="Минуты"
                                value={selectedTime.split(':')[1]}
                                onChange={(e) => handleTimeChange({ target: { value: `${selectedTime.split(':')[0]}:${e.target.value}` } })}
                                className="time-select"
                            >
                                {['00', '05', '10', '15', '20', '25', '30', '35', '40', '45', '50', '55'].map((minute) => (
                                    <option key={minute} value={minute}>{minute}</option>
                                ))}
                            </select>
                        </div>
                    </div>

                    {/* Быстрый выбор дат */}
                    <div className="quick-actions">
                        <button 
                            className="quick-button"
                            onClick={() => {
                                handleDayClick(getTodayString());
                            }}
                        >
                            Сегодня
                        </button>
                        <button 
                            className="quick-button"
                            onClick={() => {
                                handleDayClick(getTomorrowString());
                            }}
                        >
                            Завтра
                        </button>
                    </div>
                </div>
            )}
        </div>
    );
};

export default CustomDateTimePicker;