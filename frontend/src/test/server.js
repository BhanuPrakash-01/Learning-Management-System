import { setupServer } from "msw/node";
import { http, HttpResponse } from "msw";

export const handlers = [
  http.post("http://localhost:8000/api/auth/register", async () => {
    return HttpResponse.json({ message: "Registration successful" }, { status: 200 });
  }),
  http.get("http://localhost:8000/api/student/home", async () => {
    return HttpResponse.json({
      student: {
        name: "Test User",
        rollNumber: "23EG106D01",
        branch: "CSE",
        batchYear: 2025,
        section: "A",
      },
      assignedAssessments: 5,
      completedAssessments: 2,
      completionPercentage: 40,
      upcoming: [],
      recentAttempts: [],
      currentStreak: 2,
      bestStreak: 5,
      leaderboardScore: 120,
    });
  }),
];

export const server = setupServer(...handlers);
