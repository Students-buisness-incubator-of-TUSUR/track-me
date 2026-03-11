import React, { useId, useState } from "react";
import "./input-box.css";
import { ReactComponent as PenIcon } from '../../files/pen.svg';

const InputBox = ({
    placeholder,
    placeholderIsAbove = false,
    autoComplete,
    errorText,
    name,
    type,
    value,
    onEditClick = null,
    className,
    ...props
}) => {
    const [isFocused, setIsFocused] = useState(false);
    const id = useId();

    return (
        <div className={`input-box_container-with-helper ${className ?? ''}`}>
            {placeholderIsAbove && (
                <label className="input-box_label-above" htmlFor={id}>
                    {placeholder}
                </label>
            )}
            <div
                className={`input-box_container${placeholderIsAbove ? " input-box_container--above" : ""}`}
            >
                <input
                    id={id}
                    onFocus={() => setIsFocused(true)}
                    onBlur={() => setIsFocused(false)}
                    placeholder="fake"
                    name={name}
                    type={type}
                    value={value}
                    {...props}
                />
                {!placeholderIsAbove && <label htmlFor={id}>{placeholder}</label>}
                {onEditClick && (
                    <button
                        type="button"
                        onClick={onEditClick}
                    >
                        <PenIcon/>
                    </button>
                )}
            </div>
            <span>
                {isFocused ? errorText : value !== "" ? errorText : ""}
            </span>
        </div>
    );
};

export default InputBox;

