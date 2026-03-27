import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import '@testing-library/jest-dom';
import CheckBox from "./check-box";

jest.mock("../../files/arrow-top.svg", () => ({
  ReactComponent: () => <svg data-testid="arrow" />,
}));

describe("CheckBox", () => {
  it("renders the title button", () => {
    render(<CheckBox title="Filters" />);
    expect(screen.getByRole("button", { name: /filters/i })).toBeInTheDocument();
  });

  it("is closed by default", () => {
    const { container } = render(<CheckBox title="Filters"><input /><input /></CheckBox>);
    expect(container.querySelector(".check-box_checkboxes")).not.toBeInTheDocument();
  });

  it("opens on button click", () => {
    render(
      <CheckBox title="Filters">
        <input type="checkbox" aria-label="opt1" />
        <input type="checkbox" aria-label="opt2" />
      </CheckBox>
    );
    fireEvent.click(screen.getByRole("button"));
    expect(screen.getByLabelText("opt1")).toBeInTheDocument();
  });

  it("closes on second button click", () => {
    render(
      <CheckBox title="Filters">
        <input type="checkbox" aria-label="opt1" />
      </CheckBox>
    );
    const btn = screen.getByRole("button");
    fireEvent.click(btn);
    fireEvent.click(btn);
    expect(screen.queryByLabelText("opt1")).not.toBeInTheDocument();
  });

  it("closes when clicking outside", () => {
    render(
      <div>
        <CheckBox title="Filters">
          <input type="checkbox" aria-label="opt1" />
        </CheckBox>
        <div data-testid="outside" />
      </div>
    );
    fireEvent.click(screen.getByRole("button"));
    expect(screen.getByLabelText("opt1")).toBeInTheDocument();
    fireEvent.mouseDown(screen.getByTestId("outside"));
    expect(screen.queryByLabelText("opt1")).not.toBeInTheDocument();
  });

  it("groups children into pairs", () => {
    const { container } = render(
      <CheckBox title="Filters">
        <input aria-label="a" /><input aria-label="b" />
        <input aria-label="c" /><input aria-label="d" />
      </CheckBox>
    );
    fireEvent.click(screen.getByRole("button"));
    const rows = container.querySelectorAll(".check-box_row");
    expect(rows).toHaveLength(2);
    expect(rows[0].children).toHaveLength(2);
  });

  it("applies custom className", () => {
    const { container } = render(<CheckBox className="custom" />);
    expect(container.firstChild).toHaveClass("check-box_container", "custom");
  });

  it("button gets opened class when open", () => {
    render(<CheckBox title="Filters" />);
    const btn = screen.getByRole("button");
    fireEvent.click(btn);
    expect(btn).toHaveClass("check-box_button--opened");
  });
});
