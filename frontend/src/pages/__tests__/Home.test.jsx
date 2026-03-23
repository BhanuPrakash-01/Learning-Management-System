import { render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import StudentHome from "../student/Home";

describe("StudentHome page", () => {
  it("renders identity information from API payload", async () => {
    render(
      <MemoryRouter>
        <StudentHome />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText(/Test User/)).toBeInTheDocument();
      expect(screen.getByText(/23EG106D01/)).toBeInTheDocument();
      expect(screen.getByText(/Assigned Assessments/)).toBeInTheDocument();
    });
  });
});
