import React from "react";
import "./text-box.css";
import PropTypes from "prop-types";

const TextBox = ({
    className,
    ...props
}) => {
    return (
        <div className={`text-box_container ${className ?? ''}`}>
            <textarea {...props} />
        </div>
    );
};


TextBox.propTypes = {
    className: PropTypes.string,
};

export default TextBox;

