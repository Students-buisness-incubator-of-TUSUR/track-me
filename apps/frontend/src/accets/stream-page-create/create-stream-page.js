import React from 'react';
import './create-stream-page.css';
import { useNavigate } from 'react-router-dom';
import { useStreamForm } from '../stream-page-hooks/useStreamForm';
import CustomSelect from './CustomSelect'; // Импортируем новый компонент
import StreamCheckboxes from '../stream-checkboxes/stream-checkboxes';

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
  } = useStreamForm(null, navigate);

  // Варианты для выпадающего списка
  const meetingOptions = [5, 10, 15, 20];

  return (
    <div className="create-stream_main">

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
        <h1 className="create-stream_title">Создание потока</h1>
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
            <button className="create-stream_input-button" onClick={() => handleSubmit()}>
              Создать
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
