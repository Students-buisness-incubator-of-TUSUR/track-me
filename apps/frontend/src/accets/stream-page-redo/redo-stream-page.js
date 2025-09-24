import React, { useState, useEffect } from 'react';
import './create-stream-page.css';
import { useParams, useNavigate } from 'react-router-dom';
import { useStreamForm } from '../stream-page-hooks/useStreamForm';
import CustomSelect from '../stream-page-create/CustomSelect';
import { getCsrfConfigForFetch } from "../../utils/csrf-utils";

export default function EditStream() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [showTeamsWarning, setShowTeamsWarning] = useState(false);
  const [attachedTeams, setAttachedTeams] = useState([]);
  const [allTeamCards, setAllTeamCards] = useState([]);
  const backendHost = (process.env.REACT_APP_BACKEND_URI || 'http://localhost:8080') + '/backend';

  // Загрузка всех команд для проверки привязок
  useEffect(() => {
    const fetchAllTeams = async () => {
      try {
        const endpoint = `${backendHost}/api/v1/admin/team-cards?page=0&size=1000`;
        const payload = { filters: [] };
        
        const response = await fetch(endpoint, {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            ...getCsrfConfigForFetch()
          },
          credentials: "include",
          body: JSON.stringify(payload)
        });

        if (!response.ok) throw new Error("Ошибка при получении карточек команд"); // NOSONAR
        
        const data = await response.json();// NOSONAR
        setAllTeamCards(data.content || []);// NOSONAR
      } catch (error) {
        console.error("Ошибка при загрузке команд:", error);// NOSONAR
      }
    };

    fetchAllTeams();
  }, [backendHost]);

  // Получаем данные текущего потока для проверки
  const { name: streamName } = useStreamForm(id, navigate);

  // Проверяем, есть ли команды, привязанные к текущему потоку
  useEffect(() => {
    if (streamName && allTeamCards.length > 0) {// NOSONAR
      const teamsAttachedToThisStream = allTeamCards.filter(team => // NOSONAR
        team.streams && team.streams.some(stream => stream.name === streamName)// NOSONAR
      );
      
      setAttachedTeams(teamsAttachedToThisStream.map(team => ({// NOSONAR
        id: team.id,
        name: team.name || `Команда ${team.id}`,// NOSONAR
        isHyperlink: true
      })));
    }
  }, [streamName, allTeamCards]);

  const {
    name,
    startDate,
    endDate,
    trackStartDate,
    meetingsCount,
    customMeetingsCount,
    showCustomInput,
    setShowCustomInput,
    showCheckboxes2,
    error,
    setError,
    checkboxesData2,
    selectedCheckboxes,
    image,
    checkboxesRef,
    errorRef,
    handleNameChange,
    handleStartDateChange,
    handleEndDateChange,
    handleTrackStartDateChange,
    handleMeetingsCountChange,
    handleCustomMeetingsCountChange,
    handleShowCheckboxes2,
    handleCheckboxChange,
    handleImageUpload,
    handleSubmit,
    deleteStream,
  } = useStreamForm(id, navigate);

  const meetingOptions = [5, 10, 15, 20];

  // Обработчик нажатия на кнопку удаления
  const handleDeleteClick = () => {
    if (attachedTeams.length > 0) {// NOSONAR
      setShowTeamsWarning(true);// NOSONAR
    } else {
      setShowDeleteConfirm(true);
    }
  };

  // Обработчик перехода к карточке команды
  const handleTeamClick = (teamId) => {
    navigate(`/teamcard/${teamId}`, { // NOSONAR
      state: { 
        returnTo: `/edit-stream/${id}`, 
        showTeamsWarning: true 
      } 
    });
  };

  // Проверка, все ли команды отвязаны
  useEffect(() => {
    if (attachedTeams.length === 0 && showTeamsWarning) {// NOSONAR
      setShowTeamsWarning(false);// NOSONAR
      setShowDeleteConfirm(true);// NOSONAR
    }
  }, [attachedTeams, showTeamsWarning]);

  return (
    <div className="create-stream">
      {error && (
        <div className="stream-error-message" ref={errorRef}>
          <div className="stream-error-content">
            {error}
            <button className="stream-error-close" onClick={() => setError(null)}>
              ×
            </button>
          </div>
        </div>
      )}
      
      {/* Модальное окно с предупреждением о привязанных командах */}
      {showTeamsWarning && (// NOSONAR
        <div className="delete-confirm-modal">
          <div className="delete-confirm-content">
            <h3>К этому потоку привязаны следующие команды:</h3>
            <ul className="attached-teams-list">
              {attachedTeams.map(team => (
                <li key={team.id}>{/* NOSONAR */}
                  <a 
                    href={`/teamcard/${team.id}`}
                    onClick={(e) => {
                      e.preventDefault();// NOSONAR
                      handleTeamClick(team.id);// NOSONAR
                    }}
                    className="team-hyperlink"
                  >
                    {team.name}
                  </a>
                </li>
              ))}
            </ul>
            <p>Удалите или перепривяжите их перед удалением потока</p>
            <div className="delete-confirm-buttons">
              <button 
                className="delete-confirm-no"
                onClick={() => setShowTeamsWarning(false)}// NOSONAR
              >
                Закрыть
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Модальное окно подтверждения удаления */}
      {showDeleteConfirm && (
        <div className="delete-confirm-modal">
          <div className="delete-confirm-content">
            <h3>Вы уверены, что хотите безвозвратно удалить поток?</h3>
            <div className="delete-confirm-buttons">
              <button 
                className="delete-confirm-yes"
                onClick={() => {
                  deleteStream();
                  setShowDeleteConfirm(false);
                }}
              >
                Да
              </button>
              <button 
                className="delete-confirm-no"
                onClick={() => setShowDeleteConfirm(false)}
              >
                Нет
              </button>
            </div>
          </div>
        </div>
      )}

      <div className="create-stream-cont">
        <button className="create-stream-close" onClick={() => navigate(-1)}>
          ×
        </button>
        <div className="create-stream-cont-left">
          <label className="create-stream-title">Редактирование потока</label>
          <div className="create-stream-row">
            <div className="create-stream-col">
              <h1 className="create-stream-h1">Название потока:</h1>
              <h1 className="create-stream-h1">Дата начала:</h1>
              <h1 className="create-stream-h1">Дата конца:</h1>
              <h1 className="create-stream-h1">Дата начала трекшен-митинга:</h1>
              <h1 className="create-stream-h1">Количество встреч:</h1>
            </div>
            <div className="create-stream-col">
              <input
                className="create-stream-input"
                placeholder="Текст названия"
                value={name}
                onChange={handleNameChange}
              />
              <input
                className="create-stream-input-date"
                placeholder="__.__.____"
                value={startDate}
                onChange={handleStartDateChange}
              />
              <input
                className="create-stream-input-date"
                placeholder="__.__.____"
                value={endDate}
                onChange={handleEndDateChange}
              />
              <input
                className="create-stream-input-date1"
                placeholder="__.__.____"
                value={trackStartDate}
                onChange={handleTrackStartDateChange}
              />
              <CustomSelect
                value={meetingsCount}
                onChange={handleMeetingsCountChange}
                options={meetingOptions}
                customValue={customMeetingsCount}
                onCustomChange={handleCustomMeetingsCountChange}
                showCustomInput={showCustomInput}
                setShowCustomInput={setShowCustomInput}
              />
            </div>
          </div>
          <div className="Stream-bb Stream-header-chosefrom-buttw2323131">
            <div className="Stream-header-chosefrom-butt2" ref={checkboxesRef}>
              <div
                className="Stream-header-chosefrom-butt-cont"
                onClick={handleShowCheckboxes2}
                onKeyDown={(e) => {
                  if (e.key === 'Enter' || e.key === ' ') {// NOSONAR
                    e.preventDefault();// NOSONAR
                    handleShowCheckboxes2();
                  }
                }}
                tabIndex={0}
                role="button"
                aria-label="Выбрать рынок"
              >
                <b className="Stream-header-chosefrom-butt-label">Рынок</b>
                <div className="Stream-header-chosefrom-butt-pic"></div>
              </div>
              {showCheckboxes2 && (
                <div className="Stream-header-checkboxes">
                  {checkboxesData2.map((item, index) => (
                    <div
                      key={item.id}
                      className={`Stream-header-checkbox ${index < 5 ? 'first-row' : 'second-row'}`}// NOSONAR
                    >
                      <input
                        type="checkbox"
                        id={`checkbox-${item.id}`}
                        checked={selectedCheckboxes.includes(item.id)}
                        onChange={() => handleCheckboxChange(item.id)}
                      />
                      <label className="Stream-header-checkbox-label" htmlFor={`checkbox-${item.id}`}>
                        {item.displayName || item.name}{/* NOSONAR */}
                      </label>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        </div>
        <div className="create-stream-cont-right">
          <div
            className="create-stream-input-pic"
            onClick={() => document.getElementById('image-upload').click()}// NOSONAR
            onKeyDown={(e) => {
              if (e.key === 'Enter' || e.key === ' ') {// NOSONAR
                e.preventDefault();
                document.getElementById('image-upload').click();
              }
            }}
            tabIndex={0}
            role="button"
            aria-label="Загрузить изображение"
            title="Поддерживаемые форматы: JPEG, PNG, GIF"
          >
            {image ? (// NOSONAR
              <img src={image} alt="Uploaded" className="create-stream-uploaded-image" />
            ) : (
              <div className="create-stream-input-pic-placeholder"></div>
            )}
            <input
              type="file"
              id="image-upload"
              accept="image/jpeg, image/png, image/gif"
              style={{ display: 'none' }}
              onChange={handleImageUpload}
            />
          </div>
          <button className="create-stream-input-button" onClick={() => handleSubmit(true)}>
            Обновить
          </button>
        </div>
        <div 
        className="delete-stream-button"
        onClick={handleDeleteClick}
        onKeyDown={(e) => {
          if (e.key === 'Enter' || e.key === ' ') {// NOSONAR
            e.preventDefault();
            handleDeleteClick();
          }
        }}
        title="Удалить поток"
        tabIndex={0}
        role="button"
        aria-label="Удалить поток"
      >
        ×
      </div>
      </div>

      
    </div>
  );
}