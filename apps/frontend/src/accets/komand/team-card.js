import { useCallback, useEffect, useMemo, useState } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import "./team-card.css";
import MeetingCreate from "../meeting-card/MeetingCreate.js";
import { getCsrfConfigForFetch } from "../../utils/csrf-utils";
import { validateMeetingDateChange } from "../../utils/date-utils";
import Header from "../header/header";
import { useGetUserInfo } from "../../services/util";
import { fetchTrackers } from "../../services/requests";
import InputBox from "../input-box/input-box";
import { ReactComponent as CloseIcon } from '../../files/close.svg';
import TextBox from "../text-box/text-box";
import { adminRoleName, backendURLBackend, backendURLMeeting, backendURLSSO, superadminRoleName } from "../../services/constants";
import CheckBox from "../check-box/check-box";

const backendHost = backendURLBackend;
const backendHost1 = backendURLSSO;
const backendHost2 = backendURLMeeting;

const getMeetingStatusClass = (status) => {
  switch (status) {
    case "COMPLETED":
      return "team-card_meeting-status-completed";
    case "NOT_HAPPENED":
    case "COMPLETED_AS_NOT_HAPPENED":
      return "team-card_meeting-status-not-happened";
    case "SCHEDULED":
      return "team-card_meeting-status-scheduled"
    default:
      return "";
  }
};

export const getCommandCountText = (count) => {
    if (count === 0) return "0 команд";
    const lastDigit = count % 10;
    const lastTwoDigits = count % 100;
    
    if (lastTwoDigits >= 11 && lastTwoDigits <= 19) {
        return `${count} команд`;
    }
    
    if (lastDigit === 1) {
        return `${count} команда`;
    }
    
    if (lastDigit >= 2 && lastDigit <= 4) {
        return `${count} команды`;
    }
    
    return `${count} команд`;
};
const TeamCard = () => {
  const navigate = useNavigate();
  const { id } = useParams();

  const [trackerSearchTerm, setTrackerSearchTerm] = useState("");
const [isTrackerDropdownOpen, setIsTrackerDropdownOpen] = useState(false);
  const [showMeetingCreate, setShowMeetingCreate] = useState(false);
  const location = useLocation();
  const passedUsername = location.state?.username;
 const query = useMemo(() => new URLSearchParams(location.search), [location.search]);
  const from = location.state?.from || "/team-cards";

  const [role, setRole] = useState(null);
  const [username, setUsername] = useState(null);

  const [allTeamCards, setAllTeamCards] = useState([]); // eslint-disable-line no-unused-vars
  const [teamCardsCount, setTeamCardsCount] = useState(0);


  const [teamData, setTeamData] = useState({});
  const [editedData, setEditedData] = useState({});
  const [originalData, setOriginalData] = useState({});
  const [meetings, setMeetings] = useState([]);
  const [currentPage, setCurrentPage] = useState(0); // eslint-disable-line no-unused-vars
  const [totalPages, setTotalPages] = useState(1); // eslint-disable-line no-unused-vars
  const [streams, setStreams] = useState([]);
  const [ntiMarkets, setNtiMarkets] = useState([]);
  const [trackers, setTrackers] = useState([]);
  const [trackerFullName, setTrackerFullName] = useState("");
  const forceEdit = query.get("edit") === "true";
  const [isEditing, setIsEditing] = useState(forceEdit);
  const [selectedMarket, setSelectedMarket] = useState(null);
  const [selectedTRL, setSelectedTRL] = useState(null);
  const [selectedStreamId, setSelectedStreamId] = useState(null);
  const [editingMeetingId, setEditingMeetingId] = useState(null);
  const [newMeetingDate, setNewMeetingDate] = useState('');
  const [meetingError, setMeetingError] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [apiError, setApiError] = useState(null); // eslint-disable-line no-unused-vars
  const [maxMeetingsCount, setMaxMeetingsCount] = useState(0);
  const trlLevels = useMemo(() => [
    { id: 1, label: "0-2" },
    { id: 2, label: "3-5" },
    { id: 3, label: "6-8" },
    { id: 4, label: "9-10" },
  ], []);
  const [streamInfo, setStreamInfo] = useState(null);
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [meetingToDelete, setMeetingToDelete] = useState(null);
  const filteredTrackers = useMemo(() => {
  if (!trackerSearchTerm.trim()) return trackers;
  return trackers.filter(tracker => 
    tracker.fullName?.toLowerCase().includes(trackerSearchTerm.toLowerCase()) ||
    tracker.username?.toLowerCase().includes(trackerSearchTerm.toLowerCase())
  );
}, [trackers, trackerSearchTerm]);

  useEffect(() => {
    if (teamData.streams && teamData.streams.length > 0) {
      setStreamInfo(teamData.streams[0]); // берем первый поток
    }
  }, [teamData]);

  // Форматируем даты для отображения
  const formatDates = (start, end) => {
    if (!start || !end) return '';

    const formatDate = (dateStr) => {
      const date = new Date(dateStr);
      return date.toLocaleDateString('ru-RU', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric'
      }).replace(/\s/g, '');
    };

    return `${formatDate(start)} - ${formatDate(end)}`;
  };
  useEffect(() => {
    if (teamData.stream?.id) {
      setSelectedStreamId(teamData.stream.id);
    }
  }, [teamData]);

  useEffect(() => {
    if (!role) return;

    const fetchFullName = async () => {
      try {
        if (role === "ADMIN" || role === "SUPER_ADMIN") {
          const usernameToFetch = passedUsername || teamData.username;
          if (!usernameToFetch) return;

          const res = await fetch(`${backendHost1}/api/v1/users/${usernameToFetch}/info`, {
            credentials: "include"
          });
          if (!res.ok) throw new Error("Ошибка получения данных пользователя");
          const data = await res.json();
          setTrackerFullName(data.fullName || data.username || "");
        } else if (role === "TRACKER") {
          const res = await fetch(`${backendHost1}/api/v1/account/info`, {
            credentials: "include"
          });
          if (!res.ok) throw new Error("Ошибка получения данных текущего пользователя");
          const data = await res.json();
          setTrackerFullName(data.fullName || data.username || "");
        }
      } catch (err) {
        handleApiError(err, "загрузке ФИО трекера");
      }
    };

    fetchFullName();
  }, [role, passedUsername, teamData.username]);

  useEffect(() => {
    if (streamInfo?.meetingsCount) {
      setMaxMeetingsCount(streamInfo.meetingsCount);
    }
  }, [streamInfo]);
  const checkMeetingCreation = () => {
    if (meetings.length >= maxMeetingsCount) {
      setMeetingError(`Невозможно создать новую встречу. Максимальное количество встреч в потоке: ${maxMeetingsCount}`);
      setTimeout(() => setMeetingError(""), 3000); // Автоскрытие через 3 секунды
      return false;
    }
    setMeetingError(""); // Сбрасываем ошибку если все ок
    return true;
  };

  const user = useGetUserInfo();
  useEffect(() => {
    setRole(user.roles[0]);
    setUsername(user.username);
  }, [user]);

  useEffect(() => {
    const streamIdFromTeam = teamData?.streams?.[0]?.id;

    if (!streamIdFromTeam) return;

    const fetchTeamCardsCount = async () => {
      try {
        const url = new URL(`${backendHost}/api/v1/team-card/count`);
        url.searchParams.append("streamId", streamIdFromTeam);

        const response = await fetch(url.toString(), {
          credentials: "include",
        });

        if (!response.ok) {
          throw new Error(`Ошибка при получении количества карточек: ${response.status}`);
        }

        const data = await response.json();
        const count = typeof data === "number" ? data : data.count || 0;
        setTeamCardsCount(count);
      } catch (error) {
        handleApiError(error, "получении количества карточек команд");
      }
    };

    fetchTeamCardsCount();
  }, [teamData]); // зависимость от teamData

  const handleApiError = (error, context) => {
    console.error(`Error in ${context}:`, error);
    setApiError(`Ошибка при ${context}: ${error.message}`);
  };
  
  const loadMeetings = useCallback(async () => {
    try {
      const response = await fetch(
        `${backendHost2}/api/v1/meetings?teamCardId=${id}&page=${currentPage}&size=1000`,
        { credentials: 'include' }
      );
      if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
      const data = await response.json();

      const sortedMeetings = (data.content || []).sort((a, b) => {
        const numA = parseInt(a.number) || 0;
        const numB = parseInt(b.number) || 0;
        return numA - numB;
      });

      setMeetings(sortedMeetings);
      setTotalPages(data.totalPages || 1);
    } catch (error) {
      handleApiError(error, "загрузке встреч");
    }
  }, [id, currentPage]);

  const loadTeamCard = useCallback(async () => {
    try {
      const endpoint = (role === "ADMIN" || role === "SUPER_ADMIN")
        ? `${backendHost}/api/v1/admin/team-cards?page=0&size=1000`
        : `${backendHost}/api/v1/team-cards?page=0&size=1000`;

      const response = await fetch(endpoint, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          ...getCsrfConfigForFetch()
        },
        credentials: "include",
        body: JSON.stringify({ filters: [] })
      });

      if (!response.ok) throw new Error("Ошибка при получении карточек");
      
      const data = await response.json();
      const found = data.content?.find(card => String(card.id) === String(id));

      if (found) {
        setTeamData(found);
        setEditedData(prev => ({
          ...prev,
          ...found,
          ntiMarketIds: prev.ntiMarketIds || found.ntiMarkets?.map(m => m.id) || [],
          readinessLevel: prev.readinessLevel || found.readinessLevel,
          description: prev.description || found.description,
          meetingRoomLink: prev.meetingRoomLink || found.meetingRoomLink || "",
        }));
      }
    } catch (error) {
      console.error("Ошибка при обновлении карточки команды:", error);
    }
  }, [id, role]);

  useEffect(() => {
    loadMeetings();
  }, [loadMeetings]);

  useEffect(() => {
    const handleClickOutside = (event) => {
      if (!event.target.closest('.dropdown-block')) {
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  useEffect(() => {
    const handleVisibilityChange = () => {
      if (document.visibilityState === 'visible') {
        loadMeetings();
        loadTeamCard();
      }
    };

    const handlePopState = () => {
      loadMeetings();
      loadTeamCard();
    };

    document.addEventListener('visibilitychange', handleVisibilityChange);
    window.addEventListener('popstate', handlePopState);

    return () => {
      document.removeEventListener('visibilitychange', handleVisibilityChange);
      window.removeEventListener('popstate', handlePopState);
    };
  }, [loadMeetings, loadTeamCard]);

  useEffect(() => {
    const refreshParam = query.get("refresh");
    if (refreshParam) {
      loadMeetings();
      loadTeamCard();
    }
  }, [location.search, loadMeetings, loadTeamCard, query]);

  useEffect(() => {
    const interval = setInterval(() => {
      loadMeetings();
      loadTeamCard();
    }, 30000);

    return () => clearInterval(interval);
  }, [loadMeetings, loadTeamCard]);
  
  useEffect(() => {
    if (!username || !role || !id) return;

    const endpoint = (role === "ADMIN" || role === "SUPER_ADMIN")
      ? `${backendHost}/api/v1/admin/team-cards?page=0&size=1000`
      : `${backendHost}/api/v1/team-cards?page=0&size=1000`;

    const payload = {
      filters: []
    };
    fetch(endpoint, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        ...getCsrfConfigForFetch()
      },
      credentials: "include",
      body: JSON.stringify(payload)
    })
      .then(res => {
        if (!res.ok) throw new Error("Ошибка при получении карточек");
        return res.json();
      })
      .then(data => {
        const cards = data.content || [];
        setAllTeamCards(cards);
        const found = data.content?.find(card => String(card.id) === String(id));


        if (found) {
          setTeamData(found);
          setEditedData(found); // если нужно редактирование
        } else {
          setApiError("Карточка команды не найдена");
        }
      })
      .catch(err => handleApiError(err, "поиске карточки команды"));
  }, [username, role, id]);



  useEffect(() => {
    if (role === "ADMIN" || role === "SUPER_ADMIN") {
      fetchTrackers({
        page: 0,
        size: 1000,
        sort: ["fullName,asc"],
        filters: [
        { fieldName: "accountNonLocked", type: "EQ", value: true }
      ]
      })
        .then(async (res) => {
          if (!res.ok) throw new Error(`Ошибка: ${res.status}`);
          return res.json();
        })
        .then((data) => {
          // ⚠️ фильтруем только enabled === true
          const activeTrackers = (data.content || []).filter(t => t.enabled === true);
          setTrackers(activeTrackers);
        })
        .catch((err) => {
          handleApiError(err, "загрузке трекеров");
          setTrackers([]);
        });
    }
  }, [role]);


  // Замените useEffect загрузки потоков на этот:
  useEffect(() => {
    if (role === "ADMIN" || role === "SUPER_ADMIN") {
      fetch(`${backendHost}/api/v1/streams?page=0&size=1500`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          ...getCsrfConfigForFetch()
        },
        credentials: 'include',
        body: JSON.stringify({ filters: [] }),
      })
        .then(async (res) => {
          if (!res.ok) throw new Error(`Ошибка: ${res.status}`);
          return res.json();
        })
        .then((data) => {
          const allStreams = Array.isArray(data.content) ? data.content : [];

          // Фильтруем: активные потоки + текущий поток команды (если есть)
          const currentTeamStreamId = teamData.streams?.[0]?.id;
          const filteredStreams = allStreams.filter(stream =>
            stream.active === true || stream.id === currentTeamStreamId
          );

          const streamsWithNames = filteredStreams.map((s) => ({
            id: s.id,
            name: s.name,
            active: s.active,
            isCurrentTeamStream: s.id === currentTeamStreamId
          }));

          setStreams(streamsWithNames);
        })
        .catch((err) => handleApiError(err, "загрузке потоков"));
    }
  }, [role, teamData.streams]); // Добавляем teamData.streams в зависимости

  useEffect(() => {
    fetch(`${backendHost}/api/v1/streams/nti-markets`, {
      credentials: 'include',
    })
      .then(res => res.json())
      .then(data => {
        setNtiMarkets(data);
      })
      .catch(err => handleApiError(err, "загрузке рынков НТИ"));
  }, []);
  useEffect(() => {
    if (!teamData || !teamData.id) return;

    setEditedData(prev => ({
      ...prev,
      ntiMarketIds: teamData.ntiMarkets?.map(m => m.id) || prev.ntiMarketIds || [],
      readinessLevel: teamData.readinessLevel || prev.readinessLevel,
      description: teamData.description || prev.description,
      meetingRoomLink: teamData.meetingRoomLink || prev.meetingRoomLink || "",
    }));
  }, [teamData]);

  useEffect(() => {
    if (!selectedStreamId && streamInfo?.id) {
      setSelectedStreamId(streamInfo.id);
    }
  }, [streamInfo, selectedStreamId]);

  useEffect(() => {
    if (teamData.ntiMarket) {
      setSelectedMarket(teamData.ntiMarket);
    }
    if (teamData.readinessLevel) {
      const trl = trlLevels.find(t => t.label === teamData.readinessLevel);
      setSelectedTRL(trl || null);
    }
  }, [teamData, trlLevels]);



  const handleTRLSelect = (trl) => {
    setSelectedTRL(trl);
    if (isEditing) {
      setEditedData(prev => ({ ...prev, readinessLevel: trl.label }));
    }
  };

  const handleChange = (e) => {
    setEditedData({ ...editedData, [e.target.name]: e.target.value });
  };

  const hasUnsavedChanges = () => {
  if (!isEditing || !originalData) return false;
  
  const fieldsToCheck = ['name', 'meetingRoomLink', 'description', 'ntiMarketIds', 'readinessLevel', 'username'];
  for (const field of fieldsToCheck) {
    const original = originalData[field];
    const current = editedData[field];
    
    if (Array.isArray(original) && Array.isArray(current)) {
      const compareFn = (a, b) => {
        if (typeof a === 'number' && typeof b === 'number') return a - b;
        return String(a).localeCompare(String(b));
      };
      // Используем slice() для создания копии, затем sort() (мутирует копию)
      const sortedOriginal = original.slice().sort(compareFn);
      const sortedCurrent = current.slice().sort(compareFn);
      if (JSON.stringify(sortedOriginal) !== JSON.stringify(sortedCurrent)) return true;
    } else if (original !== current) {
      return true;
    }
  }
  return false;
};

  const handleMeetingClick = async (meeting) => {
    if (hasUnsavedChanges()) {
      await handleSave();
    }
    navigate(`/meeting/${meeting.id}?teamId=${id}&username=${username}`);
  };

  useEffect(() => {
    if (Array.isArray(ntiMarkets)) {
      setSelectedMarket(
        ntiMarkets.filter(m =>
          editedData.ntiMarketIds?.includes(m.id)
        )
      );
    } else {
      setSelectedMarket([]); // Устанавливаем пустой массив, если ntiMarkets не массив
    }
  }, [editedData.ntiMarketIds, ntiMarkets]);
  const handleSave = async () => {
    setIsLoading(true);
    setApiError(null);

    try {
      // 1. Проверка заполненности
      if (!editedData.name?.trim() ||
        !editedData.meetingRoomLink?.trim() ||
        !editedData.description?.trim() ||
        !editedData.ntiMarketIds ||
        !editedData.readinessLevel ||
        ((role === "ADMIN" || role === "SUPER_ADMIN") && !editedData.username)) {
        throw new Error("Пожалуйста, заполните все обязательные поля");
      }
      let usernameToSend = editedData.username;
      if ((role === "ADMIN" || role === "SUPER_ADMIN") && trackers.length) {
        const sel = trackers.find(t => t.id === editedData.username);
        if (sel && sel.username) {
          usernameToSend = sel.username;
        }
      }

      // 2. Выбираем endpoint
      const baseEndpoint =
        (role === "ADMIN" || role === "SUPER_ADMIN")
          ? `${backendHost}/api/v1/admin/team-card`
          : `${backendHost}/api/v1/team-card`;

      // 2. Параметры запроса
      const params = new URLSearchParams();
      params.append("teamCardId", id);
      params.append("streamId", selectedStreamId);
      if (role === "ADMIN" || role === "SUPER_ADMIN") {
        params.append("username", usernameToSend);
      }



      // 4. Тело запроса
      const patchData = {
        name: editedData.name.trim(),
        meetingRoomLink: editedData.meetingRoomLink.trim(),
        description: editedData.description.trim(),
        ntiMarketIds: editedData.ntiMarketIds,
        readinessLevel: editedData.readinessLevel,
      };

      // 5. Отправка PATCH
      const response = await fetch(
        `${baseEndpoint}?${params.toString()}`,
        {
          method: "PATCH",
          headers: { "Content-Type": "application/json", ...getCsrfConfigForFetch() },
          credentials: "include",
          body: JSON.stringify(patchData),
        }
      );

      if (!response.ok) {
        const errText = await response.text();
        throw new Error(`Ошибка: ${response.status} ${errText}`);
      }

      const updated = await response.json();
      setTeamData(updated);
      setEditedData(updated);
      setOriginalData(null);
      setIsEditing(false);

    } catch (error) {
      handleApiError(error, "сохранении карточки");
    } finally {
      setIsLoading(false);
    }
  };


  const handleDateChange = (meetingId, currentDate) => {
    try {
      const localDate = new Date(currentDate);
      const offset = localDate.getTimezoneOffset() * 60000;
      const localISOTime = new Date(localDate - offset).toISOString().slice(0, 16);

      const selectedDate = new Date(localISOTime);

      // ✅ Используем функцию валидации для проверки переноса
      const validation = validateMeetingDateChange(
        meetings,           // Все встречи
        meetingId,         // Исключаем редактируемую встречу из подсчета
        selectedDate       // Новая дата
      );

      if (!validation.isValid) {
        setMeetingError(validation.errorMessage);
        setTimeout(() => setMeetingError(""), 5000); // Увеличиваем время показа ошибки
        return;
      }

      setEditingMeetingId(meetingId);
      setNewMeetingDate(localISOTime);
    } catch (error) {
      handleApiError(error, "изменении даты встречи");
    }
  };



  const saveMeetingDate = async () => {
    if (!editingMeetingId || !newMeetingDate || !id) return;

    try {
      const isoDate = new Date(newMeetingDate).toISOString();

      // ✅ ВАЖНО: создаем копию встреч БЕЗ текущей редактируемой
      const meetingsWithoutCurrent = meetings.filter(m => m.id !== editingMeetingId);

      // ✅ Проверяем на копии данных
      const validation = validateMeetingDateChange(
        meetingsWithoutCurrent,  // Все встречи кроме редактируемой
        null,                    // Не нужно исключать, мы уже убрали
        isoDate                  // Новая дата
      );

      if (!validation.isValid) {
        setMeetingError(validation.errorMessage);
        setTimeout(() => setMeetingError(""), 5000);
        setEditingMeetingId(null);
        return;
      }

      // Отправляем на сервер
      const response = await fetch(
        `${backendHost2}/api/v1/update-meeting/${editingMeetingId}?teamCardId=${id}`,
        {
          method: 'PATCH',
          headers: {
            'Content-Type': 'application/json',
            ...getCsrfConfigForFetch()
          },
          credentials: 'include',
          body: JSON.stringify({ startDate: isoDate })
        }
      );

      if (!response.ok) throw new Error('Ошибка при обновлении даты');

      // ✅ Получаем обновленную встречу с сервера и обновляем стейт
      const updatedMeeting = await response.json();

      setMeetings(prev =>
        prev.map(meeting =>
          meeting.id === editingMeetingId
            ? updatedMeeting
            : meeting
        )
      );

      setEditingMeetingId(null);
      setMeetingError(""); // Сбрасываем ошибку при успехе
    } catch (error) {
      handleApiError(error, "сохранении даты встречи");
      setEditingMeetingId(null);
    }
  };

  const deleteMeeting = async () => {
    if (!meetingToDelete) return;

    try {
      const response = await fetch(
        `${backendHost2}/api/v1/delete-meeting/${meetingToDelete}`,
        {
          method: "DELETE",
          headers: {
            "Content-Type": "application/json",
            ...getCsrfConfigForFetch(),
          },
          credentials: "include",
        }
      );

      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(`Ошибка при удалении: ${response.status} ${errorText}`);
      }

      // Успешно удалено → оптимистичное обновление UI
      setMeetings(prev => prev.filter(m => m.id !== meetingToDelete));
      setEditingMeetingId(null);
      setShowDeleteModal(false);
      setMeetingToDelete(null);
      
      // ✅ Перезагружаем и встречи, и данные карточки (включая рейтинг)
      await loadMeetings();
      await loadTeamCard();
    } catch (error) {
      console.error("Ошибка удаления встречи:", error);
      setMeetingError("Не удалось удалить встречу. Попробуйте позже.");
      setTimeout(() => setMeetingError(""), 3000);
      setShowDeleteModal(false);
      
      // При ошибке перезагружаем данные для восстановления актуального состояния
      await loadMeetings();
      await loadTeamCard();
    }
  };

  const handleDeactivate = async () => {
    const isAdmin = role === "ADMIN" || role === "SUPER_ADMIN";

    if (!window.confirm('Вы уверены, что хотите деактивировать карточку команды?')) {
      return;
    }

    const baseEndpoint = isAdmin
      ? `${backendHost}/api/v1/admin/team-card`
      : `${backendHost}/api/v1/team-card`;

    const params = new URLSearchParams();
    params.append("id", id);

    if (isAdmin) {
      const userIdOrUsername = teamData?.user?.id || teamData?.username || "";
      params.append("username", userIdOrUsername); // ← важно, если бэкенд требует
    }

    try {
      const response = await fetch(`${baseEndpoint}?${params.toString()}`, {
        method: "DELETE",
        headers: { ...getCsrfConfigForFetch() },
        credentials: "include",
      });

      if (!response.ok) {
        throw new Error(`Ошибка при удалении: ${response.status}`);
      }

      navigate(from); // или `navigate(-1)` для возврата
    } catch (error) {
      handleApiError(error, "удалении карточки");
    }
  };

  return (
    <>
      <Header userRole={role} />
      {meetingError && (
        <div className="team-card_error-message" data-testid="meeting-error">
          {meetingError}
        </div>
      )}
      {showDeleteModal && (
        <button
          type="button"
          className="team-card_confirm-modal-overlay"
          onClick={() => setShowDeleteModal(false)}
          aria-label="Закрыть модальное окно"
          data-testid="delete-modal-overlay"
        >
          <div
            className="team-card_confirm-modal"
            onClick={(e) => e.stopPropagation()}
            onKeyDown={(e) => {
              if (e.key === 'Enter' || e.key === ' ') {
                e.stopPropagation();
              }
            }}
            role="dialog"
            aria-modal="true"
            aria-labelledby="delete-meeting-title"
            tabIndex={-1}
          >
            <h3 id="delete-meeting-title">Подтвердите удаление</h3>
            <p>
              Вы уверены, что хотите удалить эту встречу? <br />
              <strong>Это действие нельзя отменить.</strong>
            </p>
            <div className="team-card_confirm-modal-buttons">
              <button
                className="team-card_confirm-button no"
                onClick={(e) => {
                  e.stopPropagation();
                  setShowDeleteModal(false);
                }}
                data-testid="close-confirm-meeting"
              >
                Отмена
              </button>
              <button
                className="team-card_confirm-button yes"
                onClick={(e) => {
                  e.stopPropagation();
                  deleteMeeting();
                }}
                data-testid="confirm-delete-meeting"
              >
                Удалить
              </button>
            </div>
          </div>
        </button>
      )}
      <div className="team-card_main">
        <div className="team-card_container">
          <div className="team-card_header">
            {teamData.averageGrade !== undefined && teamData.averageGrade !== null && (
              <div
                className={`team-card_team-rating ${teamData.averageGrade >= 0.51 ? 'team-card_rating-green' :
                    teamData.averageGrade >= 0.26 ? 'team-card_rating-yellow' :
                      'team-card_rating-red'
                  }`}
              >
                {teamData.averageGrade.toLocaleString('ru-RU', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
              </div>
            )}
            <button
              onClick={() => navigate(-1)}
              className="team-card_close-button"
            >
              <CloseIcon />
            </button>
            <button
              onClick={isEditing ? handleSave : () => { setOriginalData({...editedData}); setIsEditing(true); }}
              className="team-card_etc-button"
            >
              {isLoading ? "Сохранение..." : (isEditing ? "Сохранить" : "Редактировать")}
            </button>
          </div>
          <div className="team-card_row">
            <div className="team-card_fields">
              <div className="team-card_field">
  <p>Трекер:</p>
  {isEditing && role !== "TRACKER" ? (
    <div className="check-box_container team-card_field-nti-checkbox">
      <div 
  className={`check-box_container team-card_field-nti-checkbox ${!editedData.username ? 'placeholder' : ''}`}
  onClick={() => setIsTrackerDropdownOpen(!isTrackerDropdownOpen)}
  onKeyDown={(e) => {
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault();
      setIsTrackerDropdownOpen(!isTrackerDropdownOpen);
    }
  }}
  role="button"
  tabIndex={0}
  aria-expanded={isTrackerDropdownOpen}
  aria-haspopup="listbox"
>
        <div 
    className="check-box_button" 
    style={{ 
        color: 'rgba(0, 0, 0, 1)',
        backgroundImage: 'url("data:image/svg+xml,%3Csvg xmlns=\'http://www.w3.org/2000/svg\' width=\'16\' height=\'16\' viewBox=\'0 0 24 24\' fill=\'none\' stroke=\'%23666\' stroke-width=\'2\' stroke-linecap=\'round\' stroke-linejoin=\'round\'%3E%3Cpolyline points=\'6 9 12 15 18 9\'%3E%3C/polyline%3E%3C/svg%3E")',
        backgroundRepeat: 'no-repeat',
        backgroundPosition: 'right 8px center',
        
    }}
>
    {editedData.username 
        ? trackers.find(t => t.username === editedData.username)?.fullName || editedData.username
        : "Выберите трекера"}
</div>
      </div>
      
      {isTrackerDropdownOpen && (
        <div className="team-card_field-select-dropdown">
          {/* Строка поиска внутри выпадающего списка */}
          <div className="team-card_field-select-search">
            <input
              type="text"
              className="team-card_field-select-search-input"
              placeholder="Поиск по ФИО..."
              value={trackerSearchTerm}
              onChange={(e) => setTrackerSearchTerm(e.target.value)}
              onClick={(e) => e.stopPropagation()}
              autoFocus
            />
          </div>
          
          <div className="team-card_field-select-options">
            {filteredTrackers.length === 0 ? (
              <div className="team-card_field-select-empty">Трекеры не найдены</div>
            ) : (
              filteredTrackers.map(tracker => (
                <div
  key={tracker.id}
  className={`team-card_field-select-option ${
    editedData.username === tracker.username ? 'selected' : ''
  }`}
  onClick={() => {
    setEditedData(prev => ({ ...prev, username: tracker.username }));
    setTrackerSearchTerm("");
    setIsTrackerDropdownOpen(false);
  }}
  onKeyDown={(e) => {
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault();
      setEditedData(prev => ({ ...prev, username: tracker.username }));
      setTrackerSearchTerm("");
      setIsTrackerDropdownOpen(false);
    }
  }}
  role="option"
  tabIndex={0}
  aria-selected={editedData.username === tracker.username}
>
                  <div className="team-card_field-select-option-name">{tracker.fullName}</div>
                  <div className="team-card_field-select-option-username">@{tracker.username}</div>
                </div>
              ))
            )}
          </div>
        </div>
      )}
      {role === "TRACKER" && (
        <InputBox
          className="team-card_field-input"
          name="username"
          value={trackerFullName || editedData.username || ""}
          readOnly
        />
      )}
    </div>
) : (
  <InputBox
    className="team-card_field-input"
    name="username"
    value={trackerFullName || editedData.username || ""}
    readOnly
  />
)}
              </div>
              <div className="team-card_field">
                <p>Название команды:</p>
                <InputBox
                  className="team-card_field-input"
                  name="name"
                  value={editedData.name || ""}
                  onChange={handleChange}
                  readOnly={!isEditing}
                />
              </div>
              <div className="team-card_field">
                <p>Рынки НТИ:</p>
                {isEditing ? (
                  <CheckBox
                    className="team-card_field-nti-checkbox"
                    title={(selectedMarket?.length > 0
                      ? selectedMarket.slice(0, 2).map(m => m.displayName).join(", ") +
                      (selectedMarket.length > 2 ? ` +${selectedMarket.length - 2}` : "")
                      : "Рынки НТИ")}
                  >
                    {ntiMarkets.map(market => (
                      <label key={market.id}>
                        <input
                          type="checkbox"
                          checked={editedData.ntiMarketIds?.includes(market.id)}
                          onChange={() => {
                            setEditedData(prev => {
                              const already = prev.ntiMarketIds?.includes(market.id);
                              return {
                                ...prev,
                                ntiMarketIds: already
                                  ? prev.ntiMarketIds.filter(id => id !== market.id)
                                  : [...(prev.ntiMarketIds || []), market.id]
                              };
                            });
                          }}
                        />
                        {market.displayName}
                      </label>
                    ))}
                  </CheckBox>
                ) : (
                  <div className="team-card_field-nti-container">
                    {(teamData.ntiMarkets || []).map((market) => (
                      <InputBox
                        key={market.id}
                        className="team-card_field-nti-input"
                        value={market.displayName}
                        readOnly
                      />
                    ))}
                  </div>
                )}
              </div>
              <div className="team-card_field">
                <p>TRL:</p>
                {isEditing ? (
                  <CheckBox
                    className="team-card_field-trl-checkbox"
                    title={selectedTRL?.label || "TRL"}
                  >
                    {trlLevels.map(trl => (
                      <label key={trl.id}>
                        <input
                          type="radio"
                          name="trl"
                          checked={selectedTRL?.id === trl.id}
                          onChange={() => {
                            handleTRLSelect(trl);
                          }}
                        />
                        {trl.label}
                      </label>
                    ))}
                  </CheckBox>
                ) : (
                  <InputBox
                    className="team-card_field-trl-input"
                    value={selectedTRL?.label || "TRL"}
                    readOnly
                  />
                )}
              </div>
              {isEditing && (
                <div className="team-card_field">
                  <p>Ссылка на комнату:</p>
                  <InputBox
                    className="team-card_field-input"
                    name="meetingRoomLink"
                    value={editedData.meetingRoomLink || ""}
                    onChange={handleChange}
                    readOnly={!isEditing}
                  />
                </div>
              )}
              {[adminRoleName, superadminRoleName].includes(role) && isEditing && (
                <div className="team-card_field" data-testid="stream-field">
                  <p>Поток:</p>
                  <CheckBox
                    className="team-card_field-stream-checkbox"
                    title={streams.find(s => s.id === selectedStreamId)?.name || streamInfo?.name || "Поток"}
                  >
                    {streams.map(stream => (
                      <label
                        key={stream.id}
                        tabIndex={0}
                        onClick={() => {
                          setSelectedStreamId(stream.id);
                        }}
                        onKeyDown={(e) => {
                          if (e.key === 'Enter' || e.key === ' ') {
                            setSelectedStreamId(stream.id);
                          }
                        }}
                      >
                        <input
                          type="radio"
                          name="stream"
                          checked={selectedStreamId === stream.id}
                          onChange={() => {
                            setSelectedStreamId(stream.id);
                          }}
                        />
                        {stream.name}
                      </label>
                    ))}
                  </CheckBox>
                </div>
              )}
              <div className="team-card_field team-card_description-field">
                <p>Описание:</p>
                <TextBox
                  className="team-card_field-description-input"
                  name="description"
                  value={editedData.description || ""}
                  onChange={handleChange}
                  readOnly={!isEditing}
                  placeholder="Описание карточки"
                />
              </div>
            </div>
            <div className="team-card_meetings-container">
              <div className="team-card_meetings-list">
                {meetings.map((meeting) => editingMeetingId === meeting.id ? (
                  <div
                    className={`team-card_meetings-button team-card_meeting-editing-container team-card_meeting-text ${getMeetingStatusClass(meeting.status)}`}
                  >
                    <input
                      className="team-card_meeting-edit-date"
                      type="datetime-local"
                      value={newMeetingDate}
                      onChange={(e) => setNewMeetingDate(e.target.value)}
                      min={new Date().toISOString().slice(0, 16)}
                    />
                    <button
                      className="team-card_meeting-edit-button team-card_meeting-edit-button-save"
                      onClick={(e) => {
                        e.stopPropagation();
                        saveMeetingDate();
                      }}
                    >
                      Сохранить
                    </button>
                    <button
                      className="team-card_meeting-edit-button team-card_meeting-edit-button-cancel"
                      onClick={(e) => {
                        e.stopPropagation();
                        setEditingMeetingId(null);
                      }}
                    >
                      Отмена
                    </button>
                    {[adminRoleName, superadminRoleName].includes(role) && (
                      <button
                        className="team-card_meeting-edit-button team-card_meeting-edit-button-delete"
                        onClick={(e) => {
                          e.stopPropagation();
                          setMeetingToDelete(meeting.id);
                          setShowDeleteModal(true);
                        }}
                        title="Удалить встречу"
                        data-testid="delete-meeting"
                      >
                        Удалить
                      </button>
                    )}
                  </div>
                ) : (
                  <button
                    className={`team-card_meetings-button team-card_meeting-text ${getMeetingStatusClass(meeting.status)}`}
                    onClick={() => handleMeetingClick(meeting)}
                  >
                    <button
                      className="team-card_meeting-date team-card_meeting-text"
                      onClick={(e) => { e.stopPropagation(); handleDateChange(meeting.id, meeting.startDate); }}
                    >
                      {new Date(meeting.startDate).toLocaleDateString('ru-RU', {
                        day: '2-digit',
                        month: '2-digit'
                      })}
                    </button>
                    <span>Встреча {meeting.number}</span>
                  </button>
                ))}
              </div>
              <button
                className="team-card_meetings-button"
                onClick={() => {
                  if (checkMeetingCreation()) {
                    setShowMeetingCreate(true);
                  }
                }}
              >
                Запланировать
              </button>
              {showMeetingCreate && (
                <MeetingCreate
                  teamId={id}
                  onClose={() => setShowMeetingCreate(false)}
                  userRole={role}
                />
              )}
            </div>
          </div>
          {[adminRoleName, superadminRoleName].includes(role) && isEditing && (
            <div
              className="team-card_deactivate-button-container"
            >
              <button
                className="team-card_etc-button team-card_deactivate-button"
                onClick={handleDeactivate}
              >
                Деактивировать
              </button>
            </div>
          )}
          {streamInfo && (
            <div className="team-card_stream-container">
              <span>Поток:</span>
              <div className="team-card_stream-data">
                <span>{streamInfo.name}</span>
                <span>{getCommandCountText(teamCardsCount)}</span>
                <span>{formatDates(streamInfo.startDate, streamInfo.endDate)}</span>
              </div>
            </div>
          )}
        </div>
      </div>
    </>
  );
};


export default TeamCard;
