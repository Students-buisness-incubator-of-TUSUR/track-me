import React, {useEffect, useState} from "react";
import {useNavigate} from "react-router-dom";
import "./team-card-create.css";
import penIcon from "./pen.png";
import { getCsrfConfigForFetch } from "../../utils/csrf-utils";
const backendHost = process.env.REACT_APP_BACKEND_URI + '/backend';
const backendHost1 = process.env.REACT_APP_BACKEND_URI + '/sso';
const TeamCard = () => {
    const navigate = useNavigate();

    const [error, setError] = useState("");
    const [streams, setStreams] = useState([]);
    const [markets, setMarkets] = useState([]);
    const [trackers, setTrackers] = useState([]);
    const [currentUser, setCurrentUser] = useState(null);
    const [showStreams, setShowStreams] = useState(false);
    const [showNTI, setShowNTI] = useState(false);
    const [showTRL, setShowTRL] = useState(false);
    const [showTrackers, setShowTrackers] = useState(false);
    const [selectedMarkets, setSelectedMarkets] = useState([]);

    const [selectedTRL, setSelectedTRL] = useState(null);
    const [selectedTracker, setSelectedTracker] = useState(null);
    const [formData, setFormData] = useState({
        name: "",
        meetingRoomLink: "",
        description: "",
        tracker: "",
        streamId: null
    });
    const [isLoading, setIsLoading] = useState(false);

    // Close dropdowns when clicking outside
    useEffect(() => {
        const handleClickOutside = (event) => {
            if (!event.target.closest('.create-dropdown-block') && !event.target.closest('.tracker-select-container')) {
                setShowNTI(false);
                setShowTRL(false);
                setShowStreams(false);
                setShowTrackers(false);
            }
        };

        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    // Получаем информацию о текущем пользователе
    useEffect(() => {
        fetch(`${backendHost1}/api/v1/account/info`, {
            credentials: "include",
        })
            .then((response) => response.json())
            .then((userData) => {
                console.log("userData", userData);
                setCurrentUser(userData);
                const isAdmin = userData.roles?.includes("ADMIN") || userData.roles?.includes("SUPER_ADMIN");

                
                // Если пользователь не админ, устанавливаем его имя в поле трекера
                if (!isAdmin) {
                    setFormData(prev => ({...prev, tracker: userData.fullName
                        
                    }));
                }
            })
            .catch((error) => {
                console.error("Ошибка при получении данных пользователя:", error);
                setError("Ошибка при получении данных пользователя");
            });
    }, []);

    const trlLevels = [
        {id: 1, label: "0-2"},
        {id: 2, label: "3-5"},
        {id: 3, label: "6-8"},
        {id: 4, label: "9-10"},
    ];

    // Загрузка потоков
    useEffect(() => {
    fetch(`${backendHost}/api/v1/streams?page=0&size=150`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json", 
            ...getCsrfConfigForFetch()
        },
        body: JSON.stringify({filters: []}),
        credentials: "include"
    })
        .then((res) => res.ok ? res.json() : Promise.reject(new Error("Ошибка при загрузке потоков")))
        .then((data) => {
            if (data?.content) {
                const filteredStreams = data.content.filter(stream => 
                    stream.active === true 
                );
                setStreams(filteredStreams);
            }
        })
        .catch((err) => {
            console.error("Ошибка при загрузке потоков:", err);
            setError("Ошибка при загрузке потоков");
        });
}, []);

    // Загрузка рынков НТИ
    useEffect(() => {
        fetch(`${backendHost}/api/v1/streams/nti-markets`, {
            method: "GET",
            credentials: "include",
        })
            .then((res) => res.ok ? res.json() : null)
            .then((data) => {
                if (data) setMarkets(data);
            })
            .catch(() => setError("Ошибка при загрузке рынков НТИ"));
    }, []);

    // Загрузка списка трекеров для админа
    // Загрузка списка трекеров для админа (новый вариант)
useEffect(() => {
  if (currentUser?.roles?.includes("ADMIN") || currentUser?.roles?.includes("SUPER_ADMIN"))
 {
    console.log("Запрашиваем трекеров...");
    fetch(`${backendHost1}/api/v1/users/trackers?page=0&size=100000`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        ...getCsrfConfigForFetch()
      },
      credentials: "include",
      body: JSON.stringify({ filters: [] })
    })
      .then((res) => res.ok ? res.json() : Promise.reject(res))
      .then((data) => {
        console.log("Получены трекеры:", data);
        if (data?.content) {
          setTrackers(data.content);
        } else {
          setTrackers([]);
        }
      })
      .catch((err) => {
        console.error("Ошибка при загрузке трекеров", err);
        setError("Ошибка при загрузке трекеров");
      });
  }
}, [currentUser]);



    const handleChange = (e) => {
        const {name, value} = e.target;
        setFormData(prev => ({...prev, [name]: value}));
    };

    const handleMarketSelect = (market) => {
    setSelectedMarkets(prev => {
        if (prev.some(m => m.id === market.id)) {
            return prev.filter(m => m.id !== market.id); // снять выбор
        } else {
            return [...prev, market]; // добавить в выбор
        }
    });
};


    const handleTRLSelect = (trl) => {
        setSelectedTRL(trl);
        setShowTRL(false);
    };

    const handleStreamSelect = (streamId) => {
        setFormData(prev => ({...prev, streamId}));
        setShowStreams(false);
    };

    const handleTrackerSelect = (tracker) => {
        setSelectedTracker(tracker);
        setFormData(prev => ({
            ...prev,
            tracker: tracker.fullName,
            trackerId: tracker.id,
  trackerUsername: tracker.username // Добавляем ID трекера в formData
            
        }));
        setShowTrackers(false);
    };

    const validateForm = () => {
        const errors = [];
        if (!formData.name?.trim()) errors.push("Название команды обязательно");
        if (!formData.meetingRoomLink?.trim()) errors.push("Ссылка на комнату для встречи обязательна");
        if (selectedMarkets.length === 0) errors.push("Выберите хотя бы один рынок НТИ");
        if (!selectedTRL) errors.push("Выберите уровень TRL");
        if (!formData.streamId) errors.push("Привяжите к потоку");
        const isAdmin = currentUser?.roles?.includes("ADMIN") || currentUser?.roles?.includes("SUPER_ADMIN");
if (isAdmin && !selectedTracker) {
    errors.push("Выберите трекера");
}
        return errors;
    };

    const handleCreate = async () => {
        const errors = validateForm();
        if (errors.length > 0) {
            setError(errors.join("\n"));
            return;
        }

        setIsLoading(true);
        try {
            const payload = {
                name: formData.name,
                meetingRoomLink: formData.meetingRoomLink,
                description: formData.description || "Описание карточки команды",
                ntiMarketIds: selectedMarkets.map(m => m.id),
                readinessLevel: selectedTRL.label
            };

            let url = `${backendHost}`;

            // Добавляем username в URL если выбран трекер
            if (currentUser && (currentUser.roles?.includes("ADMIN") || currentUser.roles?.includes("SUPER_ADMIN"))) {
                url += `/api/v1/admin/team-card?streamId=${formData.streamId}&username=${formData.trackerUsername}`;
            } else {
                url += `/api/v1/team-card?streamId=${formData.streamId}`;
            }

            const response = await fetch(url, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    ...getCsrfConfigForFetch()
                },
                credentials: "include",
                body: JSON.stringify(payload),
            });

            if (!response.ok) {
                throw new Error(response.status === 401 ? "Ошибка авторизации" : "Ошибка создания команды");
            }

            const data = await response.json();
            console.log("Created team card data:", data); 
            // После успешного создания переходим на страницу карточки
            navigate(`/teamcard/${data.id}`, {
  state: {
    streamId: formData.streamId,
  }
});
        } catch (error) {
            setError(error.message);
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="create-card-container">
            <button className="create-card-close" onClick={() => navigate(-1)}>×</button>

            <div className="create-card-left">
                <div className="create-card-info">
                    <span className="create-card-label" >Трекер:</span>
                    <div className="create-input-wrapper-with-pen">
                    <div className="create-input-wrapper">
  {(currentUser?.roles?.includes("ADMIN") || currentUser?.roles?.includes("SUPER_ADMIN"))
 ? (
    <div className="tracker-select-container">
      <input
        className="create-input"
        name="tracker"
        value={formData.tracker}
        onClick={() => setShowTrackers(!showTrackers)}
        readOnly
        placeholder="Выберите трекера"
      />
      {showTrackers && (
        <div className="trackers-dropdown">
          {trackers
  .filter((tracker) => tracker.enabled) // Показывать только подтвержденных
  .map((tracker) => (
    <div
      key={tracker.id}
      className="tracker-option"
      onClick={() => handleTrackerSelect(tracker)}
    >
      {tracker.fullName}
    </div>
))}

        </div>
      )}
    </div>
  ) : (
    <input
      className="create-input"
      name="tracker"
      value={formData.tracker}
      readOnly
    />
  )}
</div>

                    {(currentUser?.roles?.includes("ADMIN") || currentUser?.roles?.includes("SUPER_ADMIN")) && (
  <img src={penIcon} alt="edit" className="create-edit-icon"/>
)}

  </div>
                </div>

                <div className="create-card-info">
                    <span className="create-card-label">Название команды:</span>
                    <div className="create-input-wrapper-with-pen">
                        <div className="create-input-wrapper">
                            <input
                                className="create-input"
                                name="name"
                                value={formData.name}
                                onChange={handleChange}
                                placeholder="Введите название команды"
                            />
                        </div>
                        <img src={penIcon} alt="edit" className="create-edit-icon"/>
                    </div>
                </div>

                <div className="create-card-info">
                    <span className="create-card-label">Ссылка на комнату для встречи:</span>
                    <div className="create-input-wrapper-with-pen">
                        <div className="create-input-wrapper">
                            <input
                                className="create-input"
                                name="meetingRoomLink"
                                value={formData.meetingRoomLink}
                                onChange={handleChange}
                                placeholder="https://webinar.tusur.ru/b/abc-qwe-zxc-vbn"
                            />
                        </div>
                        <img src={penIcon} alt="edit" className="create-edit-icon"/>
                    </div>
                </div>

                <div className={`create-dropdown-block${showStreams ? " open" : ""}`}>
  <div className="create-dropdown-toggle" onClick={() => setShowStreams(!showStreams)}>
    {
      streams.find(s => s.id === formData.streamId)?.name || "Поток"
    }
  </div>
  {showStreams && (
    <div className="create-checkbox-list">
      {streams.map((stream) => (
        <div key={stream.id} className="create-checkbox-item create-radio-style">
          <input
            type="radio"
            name="stream"
            checked={formData.streamId === stream.id}
            onChange={() => handleStreamSelect(stream.id)}
          />
          <label className="data-create-team">{stream.name}</label>
        </div>
      ))}
    </div>
  )}
</div>



                <div className={`create-dropdown-block${showNTI ? " open" : ""}`}>
  <div className="create-dropdown-toggle" onClick={() => setShowNTI(!showNTI)}>
  {selectedMarkets.length > 0
    ? selectedMarkets.slice(0, 2).map(m => m.displayName).join(", ") +
        (selectedMarkets.length > 2 ? ` +${selectedMarkets.length - 2}` : "")
    : "Рынки НТИ"}
</div>


  {showNTI && (
    <div className="create-checkbox-list">
      {markets.map((market) => (
        <div key={market.id} className="create-checkbox-item create-radio-style">
          <input
  type="checkbox"
  name="ntiMarket"
  checked={selectedMarkets.some(m => m.id === market.id)}
  onChange={() => handleMarketSelect(market)}
/>

          <label className="data-create-team">{market.displayName}</label>
        </div>
      ))}
    </div>
  )}
</div>


                <div className={`create-dropdown-block${showTRL ? " open" : ""}`}>
                    <div className="create-dropdown-toggle" onClick={() => setShowTRL(!showTRL)}>
                        {selectedTRL ? selectedTRL.label : "TRL"}
                    </div>
                    {showTRL && (
  <div className="create-checkbox-list">
    {trlLevels.map((trl) => (
      <div key={trl.id} className="create-checkbox-item create-radio-style">
        <input
          type="radio"
          name="trl"
          checked={selectedTRL?.label === trl.label}
          onChange={() => handleTRLSelect(trl)}
        />
        <label className="data-create-team">{trl.label}</label>
      </div>
    ))}
  </div>
)}

                </div>

                

                <div className="create-team-description">
                    <span className="create-team-description-label">Описание:
                        <img src={penIcon} alt="edit" className="create-edit-icon"/>    
                    </span>
                    
                    <div className="create-team-description-wrapper">
                        <textarea
  className="create-description-input"
  name="description"
  placeholder="Введите описание карточки команды"
  onChange={handleChange} // добавить!
  value={formData.description}
/>

                    </div>
                </div>
            </div>

            <div className="create-right-panel">
                <div className="create-meetings-block">
                    {/* <div className="create-meetings-exist">
                        <div className="create-meeting">
                            <span class="meeting-date">25.04</span>
                            <span class="meeting-title">Встреча 1</span> 
                        </div>
                        <div className="create-meeting">   
                        </div>
                    </div> */}
                    <button
    className="create-meeting-add"
    onClick={() => {
        setError("Сначала создайте карточку команды");
    }}
>
    Запланировать
</button>

                    <div className="fake-scrollbar"></div>
                </div>
            </div>

            

            {error && (
                <button
                    className="error-message"
                    style={{whiteSpace: 'pre-line', border: "none", cursor: "pointer"}}
                    onClick={() => setError("")}
                >
                    {error}
                </button>
            )}

            <div className="create-button-container">
                <button
                    className="create-button"
                    onClick={handleCreate}
                    
                    disabled={isLoading}
                >
                    
                    {isLoading ? "Создание..." : "Создать"}
                </button>
            </div>
        </div>
    );
};

export default TeamCard;
