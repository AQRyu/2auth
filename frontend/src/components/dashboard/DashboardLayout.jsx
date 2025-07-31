import {
  BarChart3,
  LogOut,
  Menu,
  Settings,
  Shield,
  Users,
  X,
} from "lucide-react";
import { useState } from "react";
import { useAuthStore } from "../../store/authStore.js";
import { cn } from "../../utils/helpers.js";

const navigation = [
  { name: "Dashboard", href: "#dashboard", icon: BarChart3, current: true },
  { name: "Users", href: "#users", icon: Users, current: false },
  { name: "Security", href: "#security", icon: Shield, current: false },
  { name: "Settings", href: "#settings", icon: Settings, current: false },
];

export function DashboardLayout({ children }) {
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [currentTab, setCurrentTab] = useState("dashboard");
  const { user, logout } = useAuthStore();

  const handleTabChange = (tabName) => {
    setCurrentTab(tabName);
    setSidebarOpen(false); // Close mobile sidebar when tab changes
  };

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Mobile sidebar */}
      <div
        className={cn(
          "fixed inset-0 z-50 lg:hidden",
          sidebarOpen ? "block" : "hidden"
        )}
      >
        <div
          className="fixed inset-0 bg-gray-600/75"
          onClick={() => setSidebarOpen(false)}
        />
        <div className="fixed inset-y-0 left-0 w-64 bg-white shadow-xl">
          <div className="flex h-16 items-center justify-between px-6 border-b border-gray-200">
            <h1 className="text-xl font-bold text-gray-900">2Auth</h1>
            <button
              onClick={() => setSidebarOpen(false)}
              className="rounded-md p-2 text-gray-400 hover:text-gray-600"
            >
              <X className="h-6 w-6" />
            </button>
          </div>
          <nav className="mt-6 px-6">
            <ul className="space-y-1">
              {navigation.map((item) => (
                <li key={item.name}>
                  <button
                    onClick={() => handleTabChange(item.name.toLowerCase())}
                    className={cn(
                      "group flex w-full items-center rounded-md px-3 py-2 text-sm font-medium transition-colors",
                      currentTab === item.name.toLowerCase()
                        ? "bg-blue-50 text-blue-600"
                        : "text-gray-700 hover:bg-gray-50 hover:text-gray-900"
                    )}
                  >
                    <item.icon className="mr-3 h-5 w-5 flex-shrink-0" />
                    {item.name}
                  </button>
                </li>
              ))}
            </ul>
          </nav>
        </div>
      </div>

      {/* Desktop sidebar */}
      <div className="hidden lg:fixed lg:inset-y-0 lg:flex lg:w-64 lg:flex-col">
        <div className="flex flex-grow flex-col overflow-y-auto bg-white border-r border-gray-200">
          <div className="flex h-16 items-center px-6 border-b border-gray-200">
            <div className="flex items-center">
              <div className="w-8 h-8 bg-blue-600 rounded-lg flex items-center justify-center">
                <Shield className="w-5 h-5 text-white" />
              </div>
              <h1 className="ml-3 text-xl font-bold text-gray-900">2Auth</h1>
            </div>
          </div>
          <nav className="mt-6 flex-1 px-6">
            <ul className="space-y-1">
              {navigation.map((item) => (
                <li key={item.name}>
                  <button
                    onClick={() => handleTabChange(item.name.toLowerCase())}
                    className={cn(
                      "group flex w-full items-center rounded-md px-3 py-2 text-sm font-medium transition-colors",
                      currentTab === item.name.toLowerCase()
                        ? "bg-blue-50 text-blue-600"
                        : "text-gray-700 hover:bg-gray-50 hover:text-gray-900"
                    )}
                  >
                    <item.icon className="mr-3 h-5 w-5 flex-shrink-0" />
                    {item.name}
                  </button>
                </li>
              ))}
            </ul>
          </nav>

          {/* User info at bottom */}
          <div className="border-t border-gray-200 p-6">
            <div className="flex items-center">
              <div className="w-10 h-10 bg-gray-200 rounded-full flex items-center justify-center">
                <span className="text-sm font-medium text-gray-700">
                  {user?.username?.charAt(0).toUpperCase()}
                </span>
              </div>
              <div className="ml-3 flex-1">
                <p className="text-sm font-medium text-gray-900">
                  {user?.username}
                </p>
                <p className="text-xs text-gray-500">{user?.role}</p>
              </div>
              <button
                onClick={logout}
                className="ml-3 rounded-md p-2 text-gray-400 hover:text-gray-600"
                title="Logout"
              >
                <LogOut className="h-5 w-5" />
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Main content */}
      <div className="lg:pl-64">
        {/* Top header for mobile */}
        <div className="sticky top-0 z-40 lg:hidden">
          <div className="flex h-16 items-center gap-x-4 border-b border-gray-200 bg-white px-4 shadow-sm">
            <button
              onClick={() => setSidebarOpen(true)}
              className="rounded-md p-2 text-gray-400 hover:text-gray-600"
            >
              <Menu className="h-6 w-6" />
            </button>
            <div className="flex flex-1 items-center justify-between">
              <h1 className="text-lg font-semibold text-gray-900">
                2Auth Dashboard
              </h1>
              <button
                onClick={logout}
                className="rounded-md p-2 text-gray-400 hover:text-gray-600"
              >
                <LogOut className="h-5 w-5" />
              </button>
            </div>
          </div>
        </div>

        {/* Page content */}
        <main className="py-6">
          <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
            {children({ currentTab })}
          </div>
        </main>
      </div>
    </div>
  );
}

export default DashboardLayout;
