import './stream-checkboxes.css';

export default function StreamCheckboxes({ checkboxesRef, handleShowCheckboxes, showCheckboxes, checkboxesData, selectedCheckboxes, handleCheckboxChange }) {
  return (
    <div className="stream-checkboxes_Stream-bb stream-checkboxes_Stream-header-chosefrom-buttw2323131">
      <div className={`stream-checkboxes_Stream-header-chosefrom-butt ${showCheckboxes ? "stream-checkboxes_remove-above-border-radius" : ""}`} ref={checkboxesRef}>
        <div
          className="stream-checkboxes_Stream-header-chosefrom-butt-cont"
          onClick={handleShowCheckboxes}
          onKeyDown={(e) => {
            if (e.key === 'Enter' || e.key === ' ') {
              e.preventDefault();
              handleShowCheckboxes();
            }
          }}
          tabIndex={0}
          role="button"
          aria-label="Выбрать рынок"
        >
          <b className="stream-checkboxes_Stream-header-chosefrom-butt-label">Рынок</b>
          <div className="stream-checkboxes_Stream-header-chosefrom-butt-pic"></div>
        </div>
        {showCheckboxes && (
          <div className="stream-checkboxes_Stream-header-checkboxes">
            {checkboxesData.map((item, index) => (
              <div
                key={item.id}
                className={`stream-checkboxes_Stream-header-checkbox ${index < 5 ? 'stream-checkboxes_first-row' : 'stream-checkboxes_second-row'}`}
              >
                <input
                  type="checkbox"
                  id={`checkbox-${item.id}`}
                  checked={selectedCheckboxes.includes(item.id)}
                  onChange={() => handleCheckboxChange(item.id)}
                />
                <label className="stream-checkboxes_Stream-header-checkbox-label" htmlFor={`checkbox-${item.id}`}>
                  {item.displayName || item.name}
                </label>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
