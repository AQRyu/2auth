import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { useEffect } from "react";
import { Toaster } from "react-hot-toast";
import LoginForm from "./components/auth/LoginForm.jsx";
import DashboardContent from "./components/dashboard/DashboardContent.jsx";
import DashboardLayout from "./components/dashboard/DashboardLayout.jsx";
import { useAuthStore } from "./store/authStore.js";

// Create a client
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      staleTime: 5 * 60 * 1000, // 5 minutes
    },
  },
});

function AppContent() {
  const { isAuthenticated, user, initializeAuth } = useAuthStore();

  // Initialize auth state on app start
  useEffect(() => {
    initializeAuth();
  }, [initializeAuth]);

  // Show login form if not authenticated
  if (!isAuthenticated) {
    return <LoginForm />;
  }

  // Show dashboard for authenticated users
  return (
    <DashboardLayout>
      {({ currentTab }) => <DashboardContent currentTab={currentTab} />}
    </DashboardLayout>
  );
}

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <AppContent />
      <Toaster
        position="top-right"
        toastOptions={{
          duration: 4000,
          style: {
            background: "#363636",
            color: "#fff",
          },
          success: {
            style: {
              background: "#10B981",
            },
          },
          error: {
            style: {
              background: "#EF4444",
            },
          },
        }}
      />
    </QueryClientProvider>
  );
}

export default App;
