import React, { useEffect, useState } from 'react';
import './create-stream-page.css';
import { useNavigate, useParams } from 'react-router-dom';
import { useStreamForm } from '../stream-page-hooks/useStreamForm';
import CustomSelect from './CustomSelect'; // Импортируем новый компонент
import StreamCheckboxes from '../stream-checkboxes/stream-checkboxes';
import { fetchTeams } from '../../services/requests';

// id == null: Create stream; id != null: Edit stream
export default function CreateStream() {
  const params = useParams();
  const id = params?.id || null;
  const isEditMode = id !== null;
  const navigate = useNavigate();

  const meetingOptions = [5, 10, 15, 20];

  const {
    name,
    startDate,
    endDate,
    trackStartDate,
    meetingsCount,
    customMeetingsCount,
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
    showCustomInput,
    setShowCustomInput,
    deleteStream,
  } = useStreamForm(id, navigate);

  // Edit mode stuff next
  const streamName = name;
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [showTeamsWarning, setShowTeamsWarning] = useState(false);
  const [attachedTeams, setAttachedTeams] = useState([]);
  const [allTeamCards, setAllTeamCards] = useState([]);

  useEffect(() => {
    if (!isEditMode) return;
    const fetchAllTeams = async () => {
      try {
        const response = await fetchTeams(0, 1000)

        if (!response.ok) throw new Error("Ошибка при получении карточек команд");

        const data = await response.json();
        setAllTeamCards(data.content || []);
      } catch (error) {
        console.error("Ошибка при загрузке команд:", error);
      }
    };

    fetchAllTeams();
  }, [isEditMode]);

  useEffect(() => {
    if (!isEditMode) return;
    if (streamName && allTeamCards.length > 0) {
      const teamsAttachedToThisStream = allTeamCards.filter(team =>
        team.streams && team.streams.some(stream => stream.name === streamName)
      );

      setAttachedTeams(teamsAttachedToThisStream.map(team => ({
        id: team.id,
        name: team.name || `Команда ${team.id}`,
        isHyperlink: true
      })));
    }
  }, [streamName, allTeamCards, isEditMode]);

  const handleTeamClick = (teamId) => {
    navigate(`/teamcard/${teamId}`, {
      state: {
        returnTo: `/edit-stream/${id}`,
        showTeamsWarning: true
      }
    });
  };

  const handleDeleteClick = () => {
    if (attachedTeams.length > 0) {
      setShowTeamsWarning(true);
    } else {
      setShowDeleteConfirm(true);
    }
  };

  useEffect(() => {
    if (!isEditMode) return;
    if (attachedTeams.length === 0 && showTeamsWarning) {
      setShowTeamsWarning(false);
      setShowDeleteConfirm(true);
    }
  }, [attachedTeams, showTeamsWarning, isEditMode]);

  return (
    <div className="create-stream_main">
      {showTeamsWarning && (
        <div data-testid="delete-teams-modal" className="create-stream_delete-confirm-modal">
          <div className="create-stream_delete-confirm-content">
            <h3>К этому потоку привязаны следующие команды:</h3>
            <ul className="create-stream_attached-teams-list">
              {attachedTeams.map(team => (
                <li key={team.id}>
                  <a
                    href={`/teamcard/${team.id}`}
                    onClick={(e) => {
                      e.preventDefault();
                      handleTeamClick(team.id);
                    }}
                    className="create-stream_team-hyperlink"
                  >
                    {team.name}
                  </a>
                </li>
              ))}
            </ul>
            <p>Удалите или перепривяжите их перед удалением потока</p>
            <div className="create-stream_delete-confirm-buttons">
              <button
                className="create-stream_delete-confirm-no"
                onClick={() => setShowTeamsWarning(false)}
              >
                Закрыть
              </button>
            </div>
          </div>
        </div>
      )}

      {showDeleteConfirm && (
        <div data-testid="delete-confirm-modal" className="create-stream_delete-confirm-modal">
          <div className="create-stream_delete-confirm-content">
            <h3>Вы уверены, что хотите безвозвратно удалить поток?</h3>
            <div className="create-stream_delete-confirm-buttons">
              <button
                data-testid="delete-confirm-yes"
                className="create-stream_delete-confirm-yes"
                onClick={() => {
                  deleteStream();
                  setShowDeleteConfirm(false);
                }}
              >
                Да
              </button>
              <button
                className="create-stream_delete-confirm-no"
                onClick={() => setShowDeleteConfirm(false)}
              >
                Нет
              </button>
            </div>
          </div>
        </div>
      )}

      {error && (
        <div className="create-stream_error-message" ref={errorRef}>
          <div className="create-stream_error-content">
            {error}
            <button id="create-stream_error-close" className="create-stream_error-close" onClick={() => setError(null)}>
              ×
            </button>
          </div>
        </div>
      )}
      <div className="create-stream_cont">
        <button id="create-stream_stream-close" className="create-stream_close" onClick={() => navigate(-1)}>
          ×
        </button>
        <h1 className="create-stream_title">{isEditMode ? "Редактирование потока" : "Создание потока"}</h1>
        <div className='create-stream_cont-row'>
          <div className="create-stream_cont-col create-stream_cont-col-left">
            <div className='create-stream_input-cont'>
              <label>Название потока:</label>
              <input
                className='create-stream_input-cont-input'
                type="text"
                placeholder="Текст названия"
                value={name}
                onChange={handleNameChange}
              />
            </div>
            <div className='create-stream_input-cont'>
              <label>Дата начала:</label>
              <input
                className='create-stream_input-cont-input'
                type="date"
                name="startDate"
                value={startDate}
                onChange={handleStartDateChange}
              />
            </div>
            <div className='create-stream_input-cont'>
              <label>Дата конца:</label>
              <input
                className='create-stream_input-cont-input'
                type="date"
                name="endDate"
                value={endDate}
                onChange={handleEndDateChange}
              />
            </div>
            <div className='create-stream_input-cont'>
              <label>Дата начала трекшен-митинга:</label>
              <input
                className='create-stream_input-cont-input'
                type="date"
                name="trackStartDate"
                value={trackStartDate}
                onChange={handleTrackStartDateChange}
              />
            </div>
            <div className='create-stream_input-cont'>
              <label>Количество встреч:</label>
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
            <StreamCheckboxes
              checkboxesRef={checkboxesRef}
              checkboxesData={checkboxesData2}
              selectedCheckboxes={selectedCheckboxes}
              handleCheckboxChange={handleCheckboxChange}
              handleShowCheckboxes={handleShowCheckboxes2}
              showCheckboxes={showCheckboxes2}
            />
          </div>
          <div className="create-stream_cont-col create-stream_cont-col-right">
            <div
              className="create-stream_input-pic"
              onClick={() => document.getElementById('image-upload').click()}
              onKeyDown={(e) => {
                if (e.key === 'Enter' || e.key === ' ') {
                  e.preventDefault();
                  document.getElementById('image-upload').click();
                }
              }}
              tabIndex={0}
              role="button"
              aria-label="Загрузить изображение"
              title="Поддерживаемые форматы: JPEG, PNG, GIF"
            >
              {image ? (
                <img src={image} alt="Uploaded" className="create-stream_uploaded-image" />
              ) : (
                <div className="create-stream_input-pic-placeholder"></div>
              )}
              <input
                type="file"
                id="image-upload"
                accept="image/jpeg, image/png, image/gif"
                style={{ display: 'none' }}
                onChange={handleImageUpload}
              />
            </div>
            <div className="create-stream_action-button-cont">
              <button
                data-testid="action-button"
                className="create-stream_input-button"
                onClick={() => handleSubmit(isEditMode)}
              >
                {isEditMode ? "Обновить" : "Создать"}
              </button>
              {isEditMode && <button
                data-testid="button-delete"
                className="create-stream_input-button create-stream_input-button-delete"
                onClick={() => handleDeleteClick()}
              >
                X
              </button>}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
