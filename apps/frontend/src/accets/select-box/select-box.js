import React from "react";
import "./select-box.css";
import PropTypes from "prop-types";

const SelectBox = ({
    className = "",
    children,
    ...props
}) => {
    return (
        <div className={`select-box_container ${className}`}>
            <select {...props}>
                {children}
            </select>
        </div>
    );
};


SelectBox.propTypes = {
    className: PropTypes.string,
};

export default SelectBox;

