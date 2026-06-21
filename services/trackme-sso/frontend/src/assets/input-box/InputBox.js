import React, { useState } from "react";
import "./InputBox.css";

const InputBox = ({ placeholder, type, autoComplete, name, value, onChange, required, pattern, errorText }) => {
    const [isFocused, setIsFocused] = useState(false);
    const id = "input-box-" + name + "-" + type;

    return (
        <div className="input-box_container-with-helper">
            <div className="input-box_container">
                <input
                    id={id}
                    type={type}
                    className=""
                    name={name}
                    value={value}
                    onChange={onChange}
                    required={required}
                    pattern={pattern}
                    onFocus={() => setIsFocused(true)}
                    onBlur={() => setIsFocused(false)}
                    autoComplete={autoComplete}
                    placeholder="fake" // input is non empty if placeholder is not shown for CSS
                />
                <label htmlFor={id}>{placeholder}</label>
            </div>
            <span>{isFocused ? errorText : (value !== "" ? errorText : "")}</span>
        </div>
    );
};

export default InputBox;
