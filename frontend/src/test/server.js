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
  http.get("http://localhost:8000/api/student/notifications", async () => {
    return HttpResponse.json({
      content: [
        {
          id: 1,
          title: "Upcoming assessment",
          message: "Weekly Test 1 starts at 25 Mar 2026, 10:00 AM",
          createdAt: "2026-03-23T10:00:00",
          read: false,
        },
      ],
      unreadCount: 1,
      page: 0,
      size: 6,
      totalElements: 1,
      totalPages: 1,
    });
  }),
  http.patch("http://localhost:8000/api/student/notifications/:id/read", async () => {
    return HttpResponse.json({ message: "Notification marked as read" }, { status: 200 });
  }),
];

export const server = setupServer(...handlers);
