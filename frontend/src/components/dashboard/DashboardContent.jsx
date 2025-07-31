import DashboardOverview from "./DashboardOverview.jsx";
import UserManagement from "./UserManagement.jsx";

function SecuritySettings() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Security Settings</h1>
        <p className="text-gray-600 mt-1">
          Manage security policies and settings
        </p>
      </div>
      <div className="bg-white rounded-lg border border-gray-200 p-6">
        <p className="text-gray-600">Security settings panel coming soon...</p>
      </div>
    </div>
  );
}

function Settings() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">Settings</h1>
        <p className="text-gray-600 mt-1">Configure application settings</p>
      </div>
      <div className="bg-white rounded-lg border border-gray-200 p-6">
        <p className="text-gray-600">General settings panel coming soon...</p>
      </div>
    </div>
  );
}

export function DashboardContent({ currentTab }) {
  switch (currentTab) {
    case "dashboard":
      return <DashboardOverview />;
    case "users":
      return <UserManagement />;
    case "security":
      return <SecuritySettings />;
    case "settings":
      return <Settings />;
    default:
      return <DashboardOverview />;
  }
}

export default DashboardContent;
