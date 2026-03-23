import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import Register from "../Register";

describe("Register page", () => {
  it("shows validation for invalid roll number format in step 2", async () => {
    render(
      <MemoryRouter>
        <Register />
      </MemoryRouter>
    );

    fireEvent.change(screen.getByLabelText(/Full name/i), { target: { value: "Test User" } });
    fireEvent.change(screen.getByLabelText(/Email address/i), { target: { value: "test@anurag.ac.in" } });
    fireEvent.change(screen.getByLabelText(/Password/i), { target: { value: "Password@123" } });
    fireEvent.click(screen.getByRole("button", { name: /Continue to Academic Details/i }));

    fireEvent.change(screen.getByLabelText(/Roll number/i), { target: { value: "BADROLL" } });
    fireEvent.change(screen.getByLabelText(/Branch/i), { target: { value: "CSE" } });
    fireEvent.change(screen.getByLabelText(/Batch year/i), { target: { value: "2025" } });
    fireEvent.change(screen.getByLabelText(/Section/i), { target: { value: "A" } });

    fireEvent.click(screen.getByRole("button", { name: /Register/i }));

    await waitFor(() => {
      expect(screen.getByText(/Roll number must match 23EG106D01 format/i)).toBeInTheDocument();
    });
  });
});
