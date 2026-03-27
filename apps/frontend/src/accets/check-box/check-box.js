import React, { Children, useEffect, useRef, useState } from "react";
import "./check-box.css";
import PropTypes from "prop-types";
import { ReactComponent as ArrowTop } from '../../files/arrow-top.svg';

const CheckBox = ({
    className = "",
    title = "",
    children,
    ...props
}) => {
    const [isOpen, setIsOpen] = useState(false);

    // check if clicked somewhere outside the reference element
    const wrapperRef = useRef(null);
    useEffect(() => {
        function handleClickOutside(e) {
            if (wrapperRef.current && !wrapperRef.current.contains(e.target)) {
                setIsOpen(false);
            }
        }
        document.addEventListener("mousedown", handleClickOutside);
        return () => document.removeEventListener("mousedown", handleClickOutside);
    }, []);

    // make pairs out of children
    const pairs = [];
    const kids = Children.toArray(children);
    for (let i = 0; i < kids.length; i += 2) {
        pairs.push(kids.slice(i, i + 2));
    }

    return (
        <div className={`check-box_container ${className}`} ref={wrapperRef}>
            <button
                className={`check-box_button ${isOpen ? "check-box_button--opened" : ""}`}
                onClick={() => setIsOpen((prev) => !prev)}
            >
                {title}
                <ArrowTop />
            </button>
            {isOpen && (
                <div
                    className="check-box_checkboxes"
                >
                    {pairs.map((pair, i) => (
                        <div className="check-box_row" key={i}>
                            {pair}
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
};


CheckBox.propTypes = {
    className: PropTypes.string,
    title: PropTypes.string,
};

export default CheckBox;

