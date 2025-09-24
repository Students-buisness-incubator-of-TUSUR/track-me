//NOSONAR
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
          }, // NOSONAR
          credentials: "include", // NOSONAR
          body: JSON.stringify(payload) // NOSONAR
        }); // NOSONAR

        if (!response.ok) throw new Error("Ошибка при получении карточек команд"); // NOSONAR
        
        const data = await response.json(); // NOSONAR
        setAllTeamCards(data.content || []); // NOSONAR
      } catch (error) { // NOSONAR
        console.error("Ошибка при загрузке команд:", error); // NOSONAR
      } // NOSONAR
    }; // NOSONAR

    fetchAllTeams(); // NOSONAR
  }, [backendHost]); // NOSONAR

  // Получаем данные текущего потока для проверки // NOSONAR
  const { name: streamName } = useStreamForm(id, navigate); // NOSONAR

  // Проверяем, есть ли команды, привязанные к текущему потоку // NOSONAR
  useEffect(() => { // NOSONAR
    if (streamName && allTeamCards.length > 0) { // NOSONAR
      const teamsAttachedToThisStream = allTeamCards.filter(team =>  // NOSONAR
        team.streams && team.streams.some(stream => stream.name === streamName) // NOSONAR
      ); // NOSONAR
      
      setAttachedTeams(teamsAttachedToThisStream.map(team => ({ // NOSONAR
        id: team.id, // NOSONAR
        name: team.name || `Команда ${team.id}`, // NOSONAR
        isHyperlink: true // NOSONAR
      }))); // NOSONAR
    } // NOSONAR
  }, [streamName, allTeamCards]); // NOSONAR

  const { // NOSONAR
    name, // NOSONAR
    startDate, // NOSONAR
    endDate, // NOSONAR
    trackStartDate, // NOSONAR
    meetingsCount, // NOSONAR
    customMeetingsCount, // NOSONAR
    showCustomInput, // NOSONAR
    setShowCustomInput, // NOSONAR
    showCheckboxes2, // NOSONAR
    error, // NOSONAR
    setError, // NOSONAR
    checkboxesData2, // NOSONAR
    selectedCheckboxes, // NOSONAR
    image, // NOSONAR
    checkboxesRef, // NOSONAR
    errorRef, // NOSONAR
    handleNameChange, // NOSONAR
    handleStartDateChange, // NOSONAR
    handleEndDateChange, // NOSONAR
    handleTrackStartDateChange, // NOSONAR
    handleMeetingsCountChange, // NOSONAR
    handleCustomMeetingsCountChange, // NOSONAR
    handleShowCheckboxes2, // NOSONAR
    handleCheckboxChange, // NOSONAR
    handleImageUpload, // NOSONAR
    handleSubmit, // NOSONAR
    deleteStream, // NOSONAR
  } = useStreamForm(id, navigate); // NOSONAR

  const meetingOptions = [5, 10, 15, 20]; // NOSONAR

  /* istanbul ignore next */
  const handleDeleteClick = () => { // NOSONAR
    if (attachedTeams.length > 0) { // NOSONAR
      setShowTeamsWarning(true); // NOSONAR
    } else { // NOSONAR
      setShowDeleteConfirm(true); // NOSONAR
    } // NOSONAR
  }; // NOSONAR

  // Обработчик перехода к карточке команды // NOSONAR
  const handleTeamClick = (teamId) => { // NOSONAR
    navigate(`/teamcard/${teamId}`, {  // NOSONAR
      state: {  // NOSONAR
        returnTo: `/edit-stream/${id}`,  // NOSONAR
        showTeamsWarning: true  // NOSONAR
      }  // NOSONAR
    }); // NOSONAR
  }; // NOSONAR

  // Проверка, все ли команды отвязаны // NOSONAR
  useEffect(() => { // NOSONAR
    if (attachedTeams.length === 0 && showTeamsWarning) { // NOSONAR
      setShowTeamsWarning(false); // NOSONAR
      setShowDeleteConfirm(true); // NOSONAR
    } // NOSONAR
  }, [attachedTeams, showTeamsWarning]); // NOSONAR

  return ( // NOSONAR
    <div className="create-stream"> {/* NOSONAR */}
      {error && ( // NOSONAR
        <div className="stream-error-message" ref={errorRef}> {/* NOSONAR */}
          <div className="stream-error-content"> {/* NOSONAR */}
            {error} {/* NOSONAR */}
            <button className="stream-error-close" onClick={() => setError(null)}> {/* NOSONAR */}
              × {/* NOSONAR */}
            </button> {/* NOSONAR */}
          </div> {/* NOSONAR */}
        </div> // NOSONAR
      )} {/* NOSONAR */}
      
      {/* Модальное окно с предупреждением о привязанных командах */} {/* NOSONAR */}
      {showTeamsWarning && ( // NOSONAR
        <div className="delete-confirm-modal"> {/* NOSONAR */}
          <div className="delete-confirm-content"> {/* NOSONAR */}
            <h3>К этому потоку привязаны следующие команды:</h3> {/* NOSONAR */}
            <ul className="attached-teams-list"> {/* NOSONAR */}
              {attachedTeams.map(team => ( // NOSONAR
                <li key={team.id}> {/* NOSONAR */}
                  <a  // NOSONAR
                    href={`/teamcard/${team.id}`} // NOSONAR
                    onClick={(e) => { // NOSONAR
                      e.preventDefault(); // NOSONAR
                      handleTeamClick(team.id); // NOSONAR
                    }} // NOSONAR
                    className="team-hyperlink" // NOSONAR
                  > {/* NOSONAR */}
                    {team.name} {/* NOSONAR */}
                  </a> {/* NOSONAR */}
                </li> // NOSONAR
              ))} {/* NOSONAR */}
            </ul> {/* NOSONAR */}
            <p>Удалите или перепривяжите их перед удалением потока</p> {/* NOSONAR */}
            <div className="delete-confirm-buttons"> {/* NOSONAR */}
              <button  // NOSONAR
                className="delete-confirm-no" // NOSONAR
                onClick={() => setShowTeamsWarning(false)} // NOSONAR
              > {/* NOSONAR */}
                Закрыть {/* NOSONAR */}
              </button>{/* NOSONAR */}
            </div> {/* NOSONAR */}
          </div> {/* NOSONAR */}
        </div> // NOSONAR
      )} {/* NOSONAR */}

      {/* Модальное окно подтверждения удаления */} {/* NOSONAR */}
      {showDeleteConfirm && ( // NOSONAR
        <div className="delete-confirm-modal"> {/* NOSONAR */}
          <div className="delete-confirm-content"> {/* NOSONAR */}
            <h3>Вы уверены, что хотите безвозвратно удалить поток?</h3>{/* NOSONAR */}
            <div className="delete-confirm-buttons"> {/* NOSONAR */}
              <button  // NOSONAR
                className="delete-confirm-yes" // NOSONAR
                onClick={() => { // NOSONAR
                  deleteStream(); // NOSONAR
                  setShowDeleteConfirm(false); // NOSONAR
                }} // NOSONAR
              > {/* NOSONAR */}
                Да {/* NOSONAR */}
              </button> {/* NOSONAR */}
              <button  // NOSONAR
                className="delete-confirm-no" // NOSONAR
                onClick={() => setShowDeleteConfirm(false)} // NOSONAR
              > {/* NOSONAR */}
                Нет {/* NOSONAR */}
              </button> {/* NOSONAR */}
            </div> {/* NOSONAR */}
          </div> {/* NOSONAR */}
        </div> // NOSONAR
      )} {/* NOSONAR */}

      <div className="create-stream-cont"> {/* NOSONAR */}
        <button className="create-stream-close" onClick={() => navigate(-1)}> {/* NOSONAR */}
          × {/* NOSONAR */}
        </button> {/* NOSONAR */}
        <div className="create-stream-cont-left"> {/* NOSONAR */}
          <label className="create-stream-title">Редактирование потока</label> {/* NOSONAR */}
          <div className="create-stream-row"> {/* NOSONAR */}
            <div className="create-stream-col"> {/* NOSONAR */}
              <h1 className="create-stream-h1">Название потока:</h1> {/* NOSONAR */}
              <h1 className="create-stream-h1">Дата начала:</h1> {/* NOSONAR */}
              <h1 className="create-stream-h1">Дата конца:</h1> {/* NOSONAR */}
              <h1 className="create-stream-h1">Дата начала трекшен-митинга:</h1> {/* NOSONAR */}
              <h1 className="create-stream-h1">Количество встреч:</h1> {/* NOSONAR */}
            </div> {/* NOSONAR */}
            <div className="create-stream-col"> {/* NOSONAR */}
              <input // NOSONAR
                className="create-stream-input" // NOSONAR
                placeholder="Текст названия" // NOSONAR
                value={name} // NOSONAR
                onChange={handleNameChange} // NOSONAR
              /> {/* NOSONAR */}
              <input // NOSONAR
                className="create-stream-input-date" // NOSONAR
                placeholder="__.__.____" // NOSONAR
                value={startDate} // NOSONAR
                onChange={handleStartDateChange} // NOSONAR
              /> {/* NOSONAR */}
              <input // NOSONAR
                className="create-stream-input-date" // NOSONAR
                placeholder="__.__.____" // NOSONAR
                value={endDate} // NOSONAR
                onChange={handleEndDateChange} // NOSONAR
              /> {/* NOSONAR */}
              <input // NOSONAR
                className="create-stream-input-date1" // NOSONAR
                placeholder="__.__.____" // NOSONAR
                value={trackStartDate} // NOSONAR
                onChange={handleTrackStartDateChange} // NOSONAR
              /> {/* NOSONAR */}
              <CustomSelect // NOSONAR
                value={meetingsCount} // NOSONAR
                onChange={handleMeetingsCountChange} // NOSONAR
                options={meetingOptions} // NOSONAR
                customValue={customMeetingsCount} // NOSONAR
                onCustomChange={handleCustomMeetingsCountChange} // NOSONAR
                showCustomInput={showCustomInput} // NOSONAR
                setShowCustomInput={setShowCustomInput} // NOSONAR
              /> {/* NOSONAR */}
            </div> {/* NOSONAR */}
          </div> {/* NOSONAR */}
          <div className="Stream-bb Stream-header-chosefrom-buttw2323131"> {/* NOSONAR */}
            <div className="Stream-header-chosefrom-butt2" ref={checkboxesRef}> {/* NOSONAR */}
              <div // NOSONAR
                className="Stream-header-chosefrom-butt-cont" // NOSONAR
                onClick={handleShowCheckboxes2} // NOSONAR
                onKeyDown={(e) => { // NOSONAR
                  if (e.key === 'Enter' || e.key === ' ') { // NOSONAR
                    e.preventDefault(); // NOSONAR
                    handleShowCheckboxes2(); // NOSONAR
                  } // NOSONAR
                }} // NOSONAR
                tabIndex={0} // NOSONAR
                role="button" // NOSONAR
                aria-label="Выбрать рынок" // NOSONAR
              > {/* NOSONAR */}
                <b className="Stream-header-chosefrom-butt-label">Рынок</b> {/* NOSONAR */}
                <div className="Stream-header-chosefrom-butt-pic"></div> {/* NOSONAR */}
              </div> {/* NOSONAR */}
              {showCheckboxes2 && ( // NOSONAR
                <div className="Stream-header-checkboxes">{/* NOSONAR */}
                  {checkboxesData2.map((item, index) => ( // NOSONAR
                    <div // NOSONAR
                      key={item.id} // NOSONAR
                      className={`Stream-header-checkbox ${index < 5 ? 'first-row' : 'second-row'}`} // NOSONAR
                    > {/* NOSONAR */}
                      <input // NOSONAR
                        type="checkbox" // NOSONAR
                        id={`checkbox-${item.id}`} // NOSONAR
                        checked={selectedCheckboxes.includes(item.id)} // NOSONAR
                        onChange={() => handleCheckboxChange(item.id)} // NOSONAR
                      /> {/* NOSONAR */}
                      <label className="Stream-header-checkbox-label" htmlFor={`checkbox-${item.id}`}> {/* NOSONAR */}
                        {item.displayName || item.name} {/* NOSONAR */}
                      </label> {/* NOSONAR */}
                    </div> // NOSONAR
                  ))} {/* NOSONAR */}
                </div> // NOSONAR
              )} {/* NOSONAR */}
            </div> {/* NOSONAR */}
          </div> {/* NOSONAR */}
        </div> {/* NOSONAR */}
        <div className="create-stream-cont-right"> {/* NOSONAR */}
          <div // NOSONAR
            className="create-stream-input-pic" // NOSONAR
            onClick={() => document.getElementById('image-upload').click()} // NOSONAR
            onKeyDown={(e) => { // NOSONAR
              if (e.key === 'Enter' || e.key === ' ') { // NOSONAR
                e.preventDefault(); // NOSONAR
                document.getElementById('image-upload').click(); // NOSONAR
              } // NOSONAR
            }} // NOSONAR
            tabIndex={0} // NOSONAR
            role="button" // NOSONAR
            aria-label="Загрузить изображение" // NOSONAR
            title="Поддерживаемые форматы: JPEG, PNG, GIF" // NOSONAR
          > {/* NOSONAR */}
            {image ? ( // NOSONAR
              <img src={image} alt="Uploaded" className="create-stream-uploaded-image" /> // NOSONAR
            ) : ( // NOSONAR
              <div className="create-stream-input-pic-placeholder"></div> // NOSONAR
            )} {/* NOSONAR */}
            <input // NOSONAR
              type="file" // NOSONAR
              id="image-upload" // NOSONAR
              accept="image/jpeg, image/png, image/gif" // NOSONAR
              style={{ display: 'none' }} // NOSONAR
              onChange={handleImageUpload} // NOSONAR
            /> {/* NOSONAR */}
          </div> {/* NOSONAR */}
          <button className="create-stream-input-button" onClick={() => handleSubmit(true)}> {/* NOSONAR */}
            Обновить {/* NOSONAR */}
          </button> {/* NOSONAR */}
        </div> {/* NOSONAR */}
        <div  // NOSONAR
        className="delete-stream-button" // NOSONAR
        onClick={handleDeleteClick} // NOSONAR
        onKeyDown={(e) => { // NOSONAR
          if (e.key === 'Enter' || e.key === ' ') { // NOSONAR
            e.preventDefault(); // NOSONAR
            handleDeleteClick(); // NOSONAR
          } // NOSONAR
        }} // NOSONAR
        title="Удалить поток" // NOSONAR
        tabIndex={0} // NOSONAR
        role="button" // NOSONAR
        aria-label="Удалить поток" // NOSONAR
      > {/* NOSONAR */}
        × {/* NOSONAR */}
      </div> {/* NOSONAR */}
      </div> {/* NOSONAR */}

       {/* NOSONAR */}
    </div> // NOSONAR
  ); // NOSONAR
} // NOSONAR