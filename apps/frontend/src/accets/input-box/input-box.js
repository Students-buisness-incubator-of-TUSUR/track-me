import React, { useId, useState } from "react";
import "./input-box.css";
import { ReactComponent as PenIcon } from '../../files/pen.svg';
import PropTypes from "prop-types";

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
                <label data-testid="label-above" className="input-box_label-above" htmlFor={id}>
                    {placeholder}
                </label>
            )}
            <div
                className={`input-box_container${placeholderIsAbove ? " input-box_container--above" : ""}`}
            >
                <input
                    data-testid="input"
                    id={id}
                    onFocus={() => setIsFocused(true)}
                    onBlur={() => setIsFocused(false)}
                    placeholder="fake"
                    name={name}
                    type={type}
                    value={value}
                    {...props}
                />
                {!placeholderIsAbove && <label data-testid="label-inside" htmlFor={id}>{placeholder}</label>}
                {onEditClick && (
                    <button
                        data-testid="edit-btn"
                        type="button"
                        onClick={onEditClick}
                    >
                        <PenIcon/>
                    </button>
                )}
            </div>
            <span data-testid="error-text">
                {isFocused ? errorText : value !== "" ? errorText : ""}
            </span>
        </div>
    );
};


InputBox.propTypes = {
    placeholder: PropTypes.string,
    placeholderIsAbove: PropTypes.bool,
    autoComplete: PropTypes.string,
    errorText: PropTypes.string,
    name: PropTypes.string,
    type: PropTypes.string,
    value: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
    onEditClick: PropTypes.func,
    className: PropTypes.string,
};

export default InputBox;

