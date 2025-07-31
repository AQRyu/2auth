import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  Bell,
  Database,
  Mail,
  Save,
  Server,
  Settings as SettingsIcon,
} from "lucide-react";
import { useState } from "react";
import toast from "react-hot-toast";
import { api } from "../../services/api.js";

function Settings() {
  const queryClient = useQueryClient();
  const [hasChanges, setHasChanges] = useState(false);

  // Fetch application settings
  const { data: settings, isLoading } = useQuery({
    queryKey: ["app-settings"],
    queryFn: async () => {
      const response = await api.get("/api/admin/settings");
      return response.data;
    },
    // Mock data for now
    initialData: {
      application: {
        name: "2Auth System",
        description: "Enterprise Two-Factor Authentication Management",
        version: "1.0.0",
        environment: "production",
      },
      database: {
        host: "localhost",
        port: 5432,
        name: "auth_db",
        connectionPoolSize: 10,
        maxConnections: 100,
      },
      server: {
        port: 8080,
        host: "0.0.0.0",
        ssl: true,
        corsEnabled: true,
        allowedOrigins: ["http://localhost:5174", "https://auth.company.com"],
      },
      email: {
        enabled: true,
        smtp: {
          host: "smtp.gmail.com",
          port: 587,
          username: "noreply@company.com",
          ssl: true,
        },
        from: "noreply@company.com",
        fromName: "2Auth System",
      },
      notifications: {
        loginAlerts: true,
        failureAlerts: true,
        systemAlerts: true,
        emailNotifications: true,
      },
    },
  });

  // Update settings mutation
  const updateSettingsMutation = useMutation({
    mutationFn: async (newSettings) => {
      const response = await api.put("/api/admin/settings", newSettings);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["app-settings"] });
      toast.success("Settings updated successfully");
      setHasChanges(false);
    },
    onError: (error) => {
      toast.error(error.response?.data?.message || "Failed to update settings");
    },
  });

  const [formData, setFormData] = useState(settings || {});

  const handleInputChange = (section, key, value) => {
    setFormData((prev) => ({
      ...prev,
      [section]: {
        ...prev[section],
        [key]: value,
      },
    }));
    setHasChanges(true);
  };

  const handleNestedInputChange = (section, subsection, key, value) => {
    setFormData((prev) => ({
      ...prev,
      [section]: {
        ...prev[section],
        [subsection]: {
          ...prev[section][subsection],
          [key]: value,
        },
      },
    }));
    setHasChanges(true);
  };

  const handleArrayChange = (section, key, index, value) => {
    setFormData((prev) => ({
      ...prev,
      [section]: {
        ...prev[section],
        [key]: prev[section][key].map((item, i) =>
          i === index ? value : item
        ),
      },
    }));
    setHasChanges(true);
  };

  const addArrayItem = (section, key) => {
    setFormData((prev) => ({
      ...prev,
      [section]: {
        ...prev[section],
        [key]: [...prev[section][key], ""],
      },
    }));
    setHasChanges(true);
  };

  const removeArrayItem = (section, key, index) => {
    setFormData((prev) => ({
      ...prev,
      [section]: {
        ...prev[section],
        [key]: prev[section][key].filter((_, i) => i !== index),
      },
    }));
    setHasChanges(true);
  };

  const handleSave = () => {
    updateSettingsMutation.mutate(formData);
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center space-x-3">
          <SettingsIcon className="h-8 w-8 text-blue-600" />
          <div>
            <h1 className="text-2xl font-bold text-gray-900">
              Application Settings
            </h1>
            <p className="text-gray-600">
              Configure system-wide settings and preferences
            </p>
          </div>
        </div>

        {hasChanges && (
          <button
            onClick={handleSave}
            disabled={updateSettingsMutation.isPending}
            className="inline-flex items-center px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50"
          >
            <Save className="h-4 w-4 mr-2" />
            {updateSettingsMutation.isPending ? "Saving..." : "Save Changes"}
          </button>
        )}
      </div>

      {/* Application Settings */}
      <div className="bg-white rounded-lg shadow p-6">
        <div className="flex items-center space-x-3 mb-4">
          <SettingsIcon className="h-5 w-5 text-blue-600" />
          <h2 className="text-lg font-semibold text-gray-900">Application</h2>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Application Name
            </label>
            <input
              type="text"
              value={formData.application?.name || ""}
              onChange={(e) =>
                handleInputChange("application", "name", e.target.value)
              }
              className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Version
            </label>
            <input
              type="text"
              value={formData.application?.version || ""}
              onChange={(e) =>
                handleInputChange("application", "version", e.target.value)
              }
              className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
            />
          </div>

          <div className="md:col-span-2">
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Description
            </label>
            <textarea
              value={formData.application?.description || ""}
              onChange={(e) =>
                handleInputChange("application", "description", e.target.value)
              }
              rows={3}
              className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Environment
            </label>
            <select
              value={formData.application?.environment || ""}
              onChange={(e) =>
                handleInputChange("application", "environment", e.target.value)
              }
              className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
            >
              <option value="development">Development</option>
              <option value="staging">Staging</option>
              <option value="production">Production</option>
            </select>
          </div>
        </div>
      </div>

      {/* Database Settings */}
      <div className="bg-white rounded-lg shadow p-6">
        <div className="flex items-center space-x-3 mb-4">
          <Database className="h-5 w-5 text-green-600" />
          <h2 className="text-lg font-semibold text-gray-900">Database</h2>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Host
            </label>
            <input
              type="text"
              value={formData.database?.host || ""}
              onChange={(e) =>
                handleInputChange("database", "host", e.target.value)
              }
              className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Port
            </label>
            <input
              type="number"
              value={formData.database?.port || ""}
              onChange={(e) =>
                handleInputChange("database", "port", parseInt(e.target.value))
              }
              className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Database Name
            </label>
            <input
              type="text"
              value={formData.database?.name || ""}
              onChange={(e) =>
                handleInputChange("database", "name", e.target.value)
              }
              className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Connection Pool Size
            </label>
            <input
              type="number"
              value={formData.database?.connectionPoolSize || ""}
              onChange={(e) =>
                handleInputChange(
                  "database",
                  "connectionPoolSize",
                  parseInt(e.target.value)
                )
              }
              className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
            />
          </div>
        </div>
      </div>

      {/* Server Settings */}
      <div className="bg-white rounded-lg shadow p-6">
        <div className="flex items-center space-x-3 mb-4">
          <Server className="h-5 w-5 text-purple-600" />
          <h2 className="text-lg font-semibold text-gray-900">Server</h2>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Host
            </label>
            <input
              type="text"
              value={formData.server?.host || ""}
              onChange={(e) =>
                handleInputChange("server", "host", e.target.value)
              }
              className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Port
            </label>
            <input
              type="number"
              value={formData.server?.port || ""}
              onChange={(e) =>
                handleInputChange("server", "port", parseInt(e.target.value))
              }
              className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
            />
          </div>

          <div className="flex items-center justify-between">
            <div>
              <label className="text-sm font-medium text-gray-700">
                Enable SSL
              </label>
              <p className="text-sm text-gray-500">
                Use HTTPS for secure connections
              </p>
            </div>
            <label className="relative inline-flex items-center cursor-pointer">
              <input
                type="checkbox"
                checked={formData.server?.ssl || false}
                onChange={(e) =>
                  handleInputChange("server", "ssl", e.target.checked)
                }
                className="sr-only peer"
              />
              <div className="w-11 h-6 bg-gray-200 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-blue-300 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-blue-600"></div>
            </label>
          </div>

          <div className="flex items-center justify-between">
            <div>
              <label className="text-sm font-medium text-gray-700">
                Enable CORS
              </label>
              <p className="text-sm text-gray-500">
                Allow cross-origin requests
              </p>
            </div>
            <label className="relative inline-flex items-center cursor-pointer">
              <input
                type="checkbox"
                checked={formData.server?.corsEnabled || false}
                onChange={(e) =>
                  handleInputChange("server", "corsEnabled", e.target.checked)
                }
                className="sr-only peer"
              />
              <div className="w-11 h-6 bg-gray-200 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-blue-300 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-blue-600"></div>
            </label>
          </div>

          <div className="md:col-span-2">
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Allowed Origins
            </label>
            <div className="space-y-2">
              {formData.server?.allowedOrigins?.map((origin, index) => (
                <div key={index} className="flex items-center space-x-2">
                  <input
                    type="text"
                    value={origin}
                    onChange={(e) =>
                      handleArrayChange(
                        "server",
                        "allowedOrigins",
                        index,
                        e.target.value
                      )
                    }
                    className="flex-1 px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                  />
                  <button
                    onClick={() =>
                      removeArrayItem("server", "allowedOrigins", index)
                    }
                    className="px-3 py-2 text-red-600 hover:text-red-800"
                  >
                    Remove
                  </button>
                </div>
              ))}
              <button
                onClick={() => addArrayItem("server", "allowedOrigins")}
                className="text-sm text-blue-600 hover:text-blue-800"
              >
                + Add Origin
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Email Settings */}
      <div className="bg-white rounded-lg shadow p-6">
        <div className="flex items-center space-x-3 mb-4">
          <Mail className="h-5 w-5 text-red-600" />
          <h2 className="text-lg font-semibold text-gray-900">Email</h2>
        </div>

        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <div>
              <label className="text-sm font-medium text-gray-700">
                Enable Email
              </label>
              <p className="text-sm text-gray-500">
                Send email notifications and alerts
              </p>
            </div>
            <label className="relative inline-flex items-center cursor-pointer">
              <input
                type="checkbox"
                checked={formData.email?.enabled || false}
                onChange={(e) =>
                  handleInputChange("email", "enabled", e.target.checked)
                }
                className="sr-only peer"
              />
              <div className="w-11 h-6 bg-gray-200 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-blue-300 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-blue-600"></div>
            </label>
          </div>

          {formData.email?.enabled && (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 border-t pt-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  SMTP Host
                </label>
                <input
                  type="text"
                  value={formData.email?.smtp?.host || ""}
                  onChange={(e) =>
                    handleNestedInputChange(
                      "email",
                      "smtp",
                      "host",
                      e.target.value
                    )
                  }
                  className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  SMTP Port
                </label>
                <input
                  type="number"
                  value={formData.email?.smtp?.port || ""}
                  onChange={(e) =>
                    handleNestedInputChange(
                      "email",
                      "smtp",
                      "port",
                      parseInt(e.target.value)
                    )
                  }
                  className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Username
                </label>
                <input
                  type="text"
                  value={formData.email?.smtp?.username || ""}
                  onChange={(e) =>
                    handleNestedInputChange(
                      "email",
                      "smtp",
                      "username",
                      e.target.value
                    )
                  }
                  className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  From Email
                </label>
                <input
                  type="email"
                  value={formData.email?.from || ""}
                  onChange={(e) =>
                    handleInputChange("email", "from", e.target.value)
                  }
                  className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  From Name
                </label>
                <input
                  type="text"
                  value={formData.email?.fromName || ""}
                  onChange={(e) =>
                    handleInputChange("email", "fromName", e.target.value)
                  }
                  className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                />
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Notification Settings */}
      <div className="bg-white rounded-lg shadow p-6">
        <div className="flex items-center space-x-3 mb-4">
          <Bell className="h-5 w-5 text-yellow-600" />
          <h2 className="text-lg font-semibold text-gray-900">Notifications</h2>
        </div>

        <div className="space-y-4">
          {Object.entries(formData.notifications || {}).map(([key, value]) => (
            <div key={key} className="flex items-center justify-between">
              <div>
                <label className="text-sm font-medium text-gray-700 capitalize">
                  {key.replace(/([A-Z])/g, " $1").trim()}
                </label>
                <p className="text-sm text-gray-500">
                  {key === "loginAlerts" && "Notify on successful logins"}
                  {key === "failureAlerts" && "Notify on failed login attempts"}
                  {key === "systemAlerts" && "Notify on system events"}
                  {key === "emailNotifications" &&
                    "Send notifications via email"}
                </p>
              </div>
              <label className="relative inline-flex items-center cursor-pointer">
                <input
                  type="checkbox"
                  checked={value}
                  onChange={(e) =>
                    handleInputChange("notifications", key, e.target.checked)
                  }
                  className="sr-only peer"
                />
                <div className="w-11 h-6 bg-gray-200 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-blue-300 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-blue-600"></div>
              </label>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

export default Settings;
