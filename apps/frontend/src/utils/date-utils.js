export const getMonday = (date) => {
  const d = new Date(date);
  const day = d.getDay(); // 0 = воскресенье, 1 = понедельник, ..., 6 = суббота
  const diff = d.getDate() - (day === 0 ? 6 : day - 1); // сдвигаемся к понедельнику
  const monday = new Date(d);
  monday.setDate(diff);
  monday.setHours(0, 0, 0, 0);
  return monday.toISOString().split('T')[0]; // возвращаем строку YYYY-MM-DD
};

export const getMeetingsByWeek = (meetings) => {
  const weeks = {};
  
  meetings.forEach(meeting => {
    if (!meeting?.startDate) return;
    
    try {
      // Получаем понедельник недели для этой встречи
      const mondayKey = getMonday(meeting.startDate);
      
      // Увеличиваем счетчик для этой недели
      weeks[mondayKey] = (weeks[mondayKey] || 0) + 1;
    } catch (error) {
      console.error('Ошибка при обработке даты встречи:', meeting.startDate, error);
    }
  });
  
  return weeks;
};

export const validateMeetingWeekLimit = (allMeetings, newMeetingDate, isNewMeeting = false) => {
  if (!newMeetingDate) {
    return {
      isValid: false,
      count: 0,
      monday: null,
      errorMessage: "Не указана дата встречи"
    };
  }
  
  try {
    // Нормализуем дату
    const date = new Date(newMeetingDate);
    if (isNaN(date.getTime())) {
      return {
        isValid: false,
        count: 0,
        monday: null,
        errorMessage: "Некорректная дата встречи"
      };
    }
    
    // Получаем понедельник для новой даты
    const mondayKey = getMonday(date);
    
    // Подсчитываем встречи по неделям
    const meetingsByWeek = getMeetingsByWeek(allMeetings);
    
    // Сколько встреч уже есть на этой неделе
    let countThisWeek = meetingsByWeek[mondayKey] || 0;
    
    // Если это новая встреча, добавляем ее к подсчету
    if (isNewMeeting) {
      countThisWeek += 1;
    }
    
    // Форматируем даты для сообщения об ошибке
    // const formatDateForDisplay = (dateString) => {
    //   const [year, month, day] = dateString.split('-');
    //   return `${day}.${month}.${year}`;
    // };
    
    //const weekStart = formatDateForDisplay(mondayKey);
    const weekEndDate = new Date(mondayKey);
    weekEndDate.setDate(weekEndDate.getDate() + 6);
    // const weekEnd = weekEndDate.toLocaleDateString('ru-RU', {
    //   day: '2-digit',
    //   month: '2-digit',
    //   year: 'numeric'
    // });
    
    return {
      isValid: countThisWeek <= 2,
      count: countThisWeek,
      monday: mondayKey,
      errorMessage: countThisWeek > 2 
        ? `Нельзя сохранить: на этой неделе уже 2 встречи`
        : null
    };
    
  } catch (error) {
    console.error('Ошибка при валидации даты:', error);
    return {
      isValid: false,
      count: 0,
      monday: null,
      errorMessage: "Ошибка при проверке даты встречи"
    };
  }
};

// Функция для проверки переноса даты существующей встречи
export const validateMeetingDateChange = (allMeetings, meetingId, newDate) => {
  // Создаем копию встреч без текущей редактируемой
  const meetingsWithoutCurrent = allMeetings.filter(m => m.id !== meetingId);
  
  // Проверяем как будто добавляем новую встречу
  return validateMeetingWeekLimit(
    meetingsWithoutCurrent, // Все встречи кроме редактируемой
    newDate,               // Новая дата
    true                   // Рассматриваем как добавление новой встречи
  );
};

// Функция для получения диапазона недели
export const getWeekRange = (date) => {
  const monday = new Date(getMonday(date));
  const sunday = new Date(monday);
  sunday.setDate(sunday.getDate() + 6);
  
  return {
    start: monday.toISOString().split('T')[0],
    end: sunday.toISOString().split('T')[0]
  };
};