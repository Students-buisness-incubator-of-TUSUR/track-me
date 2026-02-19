import React from 'react';
import './create-stream-page.css';
import { useNavigate } from 'react-router-dom';
import { useStreamForm } from '../stream-page-hooks/useStreamForm';
import CustomSelect from './CustomSelect'; // Импортируем новый компонент

export default function CreateStream() {
  const navigate = useNavigate();
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
  } = useStreamForm();

  // Варианты для выпадающего списка
  const meetingOptions = [5, 10, 15, 20];

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
      <div className="create-stream-cont">
        <button className="create-stream-close" onClick={() => navigate(-1)}>
          ×
        </button>
        <div className="create-stream-cont-left">
          <label className="create-stream-title">Создание потока</label>
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
                type="date"
                name="startDate"
                className="create-stream-input-date"
                value={startDate}
                onChange={handleStartDateChange}
              />
              <input
                type="date"
                name="endDate"
                className="create-stream-input-date"
                value={endDate}
                onChange={handleEndDateChange}
              />
              <input
                type="date"
                name="trackStartDate"
                className="create-stream-input-date1"
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
                  if (e.key === 'Enter' || e.key === ' ') {
                    e.preventDefault();
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
                      className={`Stream-header-checkbox ${index < 5 ? 'first-row' : 'second-row'}`}
                    >
                      <input
                        type="checkbox"
                        id={`checkbox-${item.id}`}
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
        </div>
        <div className="create-stream-cont-right">
          <div
            className="create-stream-input-pic"
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
          <button className="create-stream-input-button" onClick={() => handleSubmit()}>
            Создать
          </button>
        </div>
      </div>
    </div>
  );
}
