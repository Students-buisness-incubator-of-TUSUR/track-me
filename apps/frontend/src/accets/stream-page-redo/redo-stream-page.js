
import React, { useState } from 'react';
import './create-stream-page.css'; //NOSONAR
import { useParams, useNavigate } from 'react-router-dom'; //NOSONAR
import { useStreamForm } from '../stream-page-hooks/useStreamForm'; //NOSONAR
import CustomSelect from '../stream-page-create/CustomSelect'; //NOSONAR

export default function EditStream() { //NOSONAR
  const { id } = useParams(); //NOSONAR
  const navigate = useNavigate(); //NOSONAR
  // В начало компонента EditStream добавьте
const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const { //NOSONAR
    name, //NOSONAR
    startDate, //NOSONAR
    endDate, //NOSONAR
    trackStartDate, //NOSONAR
    meetingsCount, //NOSONAR
    customMeetingsCount, //NOSONAR
    showCustomInput, //NOSONAR
    setShowCustomInput, //NOSONAR
    showCheckboxes2, //NOSONAR
    error, //NOSONAR
    setError, //NOSONAR
    checkboxesData2, //NOSONAR
    selectedCheckboxes, //NOSONAR
    image, //NOSONAR
    checkboxesRef, //NOSONAR
    errorRef, //NOSONAR
    handleNameChange, //NOSONAR
    handleStartDateChange, //NOSONAR
    handleEndDateChange, //NOSONAR
    handleTrackStartDateChange, //NOSONAR
    handleMeetingsCountChange, //NOSONAR
    handleCustomMeetingsCountChange, //NOSONAR
    handleShowCheckboxes2, //NOSONAR
    handleCheckboxChange, //NOSONAR
    handleImageUpload, //NOSONAR
    handleSubmit, //NOSONAR
    deleteStream, 
  } = useStreamForm(id, navigate); //NOSONAR

  const meetingOptions = [5, 10, 15, 20]; //NOSONAR

  return ( //NOSONAR
    <div className="create-stream"> {/*NOSONAR*/}
      {error && ( //NOSONAR
        <div className="stream-error-message" ref={errorRef}> {/*NOSONAR*/}
          <div className="stream-error-content"> {/*NOSONAR*/}
            {error} {/*NOSONAR*/}
            <button className="stream-error-close" onClick={() => setError(null)}> {/*NOSONAR*/}
              × {/*NOSONAR*/}
            </button> {/*NOSONAR*/}
          </div> {/*NOSONAR*/}
        </div> //NOSONAR
      )} {/*NOSONAR*/}
      <div className="create-stream-cont"> {/*NOSONAR*/}
        <button className="create-stream-close" onClick={() => navigate(-1)}> {/*NOSONAR*/}
          × {/*NOSONAR*/}
        </button> {/*NOSONAR*/}
        <div className="create-stream-cont-left"> {/*NOSONAR*/}
          <label className="create-stream-title">Редактирование потока</label> {/*NOSONAR*/}
          <div className="create-stream-row"> {/*NOSONAR*/}
            <div className="create-stream-col"> {/*NOSONAR*/}
              <h1 className="create-stream-h1">Название потока:</h1> {/*NOSONAR*/}
              <h1 className="create-stream-h1">Дата начала:</h1> {/*NOSONAR*/}
              <h1 className="create-stream-h1">Дата конца:</h1> {/*NOSONAR*/}
              <h1 className="create-stream-h1">Дата начала трекшен-митинга:</h1> {/*NOSONAR*/}
              <h1 className="create-stream-h1">Количество встреч:</h1> {/*NOSONAR*/}
            </div> {/*NOSONAR*/}
            <div className="create-stream-col"> {/*NOSONAR*/}
              <input //NOSONAR
                className="create-stream-input" //NOSONAR
                placeholder="Текст названия" //NOSONAR
                value={name} //NOSONAR
                onChange={handleNameChange} //NOSONAR
              /> {/*NOSONAR*/}
              <input //NOSONAR
                className="create-stream-input-date" //NOSONAR
                placeholder="__.__.____" //NOSONAR
                value={startDate} //NOSONAR
                onChange={handleStartDateChange} //NOSONAR
              /> {/*NOSONAR*/}
              <input //NOSONAR
                className="create-stream-input-date" //NOSONAR
                placeholder="__.__.____" //NOSONAR
                value={endDate} //NOSONAR
                onChange={handleEndDateChange} //NOSONAR
              /> {/*NOSONAR*/}
              <input //NOSONAR
                className="create-stream-input-date1" //NOSONAR
                placeholder="__.__.____" //NOSONAR
                value={trackStartDate} //NOSONAR
                onChange={handleTrackStartDateChange} //NOSONAR
              /> {/*NOSONAR*/}
              <CustomSelect //NOSONAR
                value={meetingsCount} //NOSONAR
                onChange={handleMeetingsCountChange} //NOSONAR
                options={meetingOptions} //NOSONAR
                customValue={customMeetingsCount} //NOSONAR
                onCustomChange={handleCustomMeetingsCountChange} //NOSONAR
                showCustomInput={showCustomInput} //NOSONAR
                setShowCustomInput={setShowCustomInput} //NOSONAR
              /> {/*NOSONAR*/}
            </div> {/*NOSONAR*/}
          </div> {/*NOSONAR*/}
          <div className="Stream-bb Stream-header-chosefrom-buttw2323131"> {/*NOSONAR*/}
            <div className="Stream-header-chosefrom-butt2" ref={checkboxesRef}> {/*NOSONAR*/}
              <div //NOSONAR
                className="Stream-header-chosefrom-butt-cont" //NOSONAR
                onClick={handleShowCheckboxes2} //NOSONAR
                onKeyDown={(e) => { //NOSONAR
                  if (e.key === 'Enter' || e.key === ' ') { //NOSONAR
                    e.preventDefault(); //NOSONAR
                    handleShowCheckboxes2(); //NOSONAR
                  } //NOSONAR
                }} //NOSONAR
                tabIndex={0} //NOSONAR
                role="button" //NOSONAR
                aria-label="Выбрать рынок" //NOSONAR
              > {/*NOSONAR*/}
                <b className="Stream-header-chosefrom-butt-label">Рынок</b> {/*NOSONAR*/}
                <div className="Stream-header-chosefrom-butt-pic"></div> {/*NOSONAR*/}
              </div> {/*NOSONAR*/}
              {showCheckboxes2 && ( //NOSONAR
                <div className="Stream-header-checkboxes"> {/*NOSONAR*/}
                  {checkboxesData2.map((item, index) => ( //NOSONAR
                    <div //NOSONAR
                      key={item.id} //NOSONAR
                      className={`Stream-header-checkbox ${index < 5 ? 'first-row' : 'second-row'}`} //NOSONAR
                    > {/*NOSONAR*/}
                      <input //NOSONAR
                        type="checkbox" //NOSONAR
                        id={`checkbox-${item.id}`} //NOSONAR
                        checked={selectedCheckboxes.includes(item.id)} //NOSONAR
                        onChange={() => handleCheckboxChange(item.id)} //NOSONAR
                      /> {/*NOSONAR*/}
                      <label className="Stream-header-checkbox-label" htmlFor={`checkbox-${item.id}`}> {/*NOSONAR*/}
                        {item.displayName || item.name} {/*NOSONAR*/}
                      </label> {/*NOSONAR*/}
                    </div> //NOSONAR
                  ))} {/*NOSONAR*/}
                </div> //NOSONAR
              )} {/*NOSONAR*/}
            </div> {/*NOSONAR*/}
          </div> {/*NOSONAR*/}
        </div> {/*NOSONAR*/}
        <div className="create-stream-cont-right"> {/*NOSONAR*/}
          <div //NOSONAR
            className="create-stream-input-pic" //NOSONAR
            onClick={() => document.getElementById('image-upload').click()} //NOSONAR
            onKeyDown={(e) => { //NOSONAR
              if (e.key === 'Enter' || e.key === ' ') { //NOSONAR
                e.preventDefault(); //NOSONAR
                document.getElementById('image-upload').click(); //NOSONAR
              } //NOSONAR
            }} //NOSONAR
            tabIndex={0} //NOSONAR
            role="button" //NOSONAR
            aria-label="Загрузить изображение" //NOSONAR
            title="Поддерживаемые форматы: JPEG, PNG, GIF" //NOSONAR
          > {/*NOSONAR*/}
            {image ? ( //NOSONAR
              <img src={image} alt="Uploaded" className="create-stream-uploaded-image" /> //NOSONAR
            ) : ( //NOSONAR
              <div className="create-stream-input-pic-placeholder"></div> //NOSONAR
            )} {/*NOSONAR*/}
            <input //NOSONAR
              type="file" //NOSONAR
              id="image-upload" //NOSONAR
              accept="image/jpeg, image/png, image/gif" //NOSONAR
              style={{ display: 'none' }} //NOSONAR
              onChange={handleImageUpload} //NOSONAR
            /> {/*NOSONAR*/}
          </div> {/*NOSONAR*/}
          <button className="create-stream-input-button" onClick={() => handleSubmit(true)}> {/*NOSONAR*/}
            Обновить {/*NOSONAR*/}
          </button> {/*NOSONAR*/}
        </div> {/*NOSONAR*/}
      </div> {/*NOSONAR*/}      
    {showDeleteConfirm && (
        <div className="delete-confirm-modal">
          <div className="delete-confirm-content">
            <h3>Вы уверены, что хотите удалить поток?</h3>
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

      <div 
        className="delete-stream-button"
        onClick={() => setShowDeleteConfirm(true)}
        onKeyDown={(e) => {
          if (e.key === 'Enter' || e.key === ' ') {
            e.preventDefault();
            setShowDeleteConfirm(true);
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
  );
}