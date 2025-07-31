import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { AlertTriangle, Clock, Key, RefreshCw, Shield } from "lucide-react";
import { useState } from "react";
import toast from "react-hot-toast";
import { api } from "../../services/api.js";

function SecuritySettings() {
  const queryClient = useQueryClient();
  const [isGeneratingSecret, setIsGeneratingSecret] = useState(false);

  // Fetch security settings
  const { data: securitySettings, isLoading } = useQuery({
    queryKey: ["security-settings"],
    queryFn: async () => {
      const response = await api.get("/api/admin/security-settings");
      return response.data;
    },
    // Mock data for now
    initialData: {
      totpEnabled: true,
      sessionTimeout: 3600,
      maxLoginAttempts: 5,
      lockoutDuration: 900,
      requirePasswordChange: false,
      passwordMinLength: 8,
      secretKey: "••••••••••••••••",
      lastSecretGeneration: "2024-01-15T10:30:00Z",
    },
  });

  // Update security settings mutation
  const updateSettingsMutation = useMutation({
    mutationFn: async (settings) => {
      const response = await api.put("/api/admin/security-settings", settings);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["security-settings"] });
      toast.success("Security settings updated successfully");
    },
    onError: (error) => {
      toast.error(error.response?.data?.message || "Failed to update settings");
    },
  });

  // Generate new secret key mutation
  const generateSecretMutation = useMutation({
    mutationFn: async () => {
      const response = await api.post(
        "/api/admin/security-settings/generate-secret"
      );
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["security-settings"] });
      toast.success("New secret key generated successfully");
      setIsGeneratingSecret(false);
    },
    onError: (error) => {
      toast.error(
        error.response?.data?.message || "Failed to generate secret key"
      );
      setIsGeneratingSecret(false);
    },
  });

  const handleSettingChange = (key, value) => {
    const updatedSettings = { ...securitySettings, [key]: value };
    updateSettingsMutation.mutate(updatedSettings);
  };

  const handleGenerateSecret = () => {
    setIsGeneratingSecret(true);
    generateSecretMutation.mutate();
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
      <div className="flex items-center space-x-3 mb-6">
        <Shield className="h-8 w-8 text-blue-600" />
        <div>
          <h1 className="text-2xl font-bold text-gray-900">
            Security Settings
          </h1>
          <p className="text-gray-600">
            Configure authentication and security policies
          </p>
        </div>
      </div>

      {/* TOTP Settings */}
      <div className="bg-white rounded-lg shadow p-6">
        <div className="flex items-center space-x-3 mb-4">
          <Key className="h-5 w-5 text-green-600" />
          <h2 className="text-lg font-semibold text-gray-900">
            Two-Factor Authentication
          </h2>
        </div>

        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <div>
              <label className="text-sm font-medium text-gray-700">
                Enable TOTP
              </label>
              <p className="text-sm text-gray-500">
                Require time-based one-time passwords
              </p>
            </div>
            <label className="relative inline-flex items-center cursor-pointer">
              <input
                type="checkbox"
                checked={securitySettings.totpEnabled}
                onChange={(e) =>
                  handleSettingChange("totpEnabled", e.target.checked)
                }
                className="sr-only peer"
              />
              <div className="w-11 h-6 bg-gray-200 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-blue-300 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-blue-600"></div>
            </label>
          </div>

          <div className="border-t pt-4">
            <div className="flex items-center justify-between mb-3">
              <div>
                <label className="text-sm font-medium text-gray-700">
                  Secret Key
                </label>
                <p className="text-sm text-gray-500">
                  Used for TOTP generation
                </p>
              </div>
              <button
                onClick={handleGenerateSecret}
                disabled={isGeneratingSecret}
                className="inline-flex items-center px-3 py-2 border border-gray-300 shadow-sm text-sm leading-4 font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50"
              >
                {isGeneratingSecret ? (
                  <RefreshCw className="h-4 w-4 mr-2 animate-spin" />
                ) : (
                  <RefreshCw className="h-4 w-4 mr-2" />
                )}
                Generate New
              </button>
            </div>
            <div className="flex items-center space-x-3">
              <code className="flex-1 bg-gray-100 px-3 py-2 rounded-md text-sm font-mono">
                {securitySettings.secretKey}
              </code>
            </div>
            <p className="text-xs text-gray-500 mt-2">
              Last generated:{" "}
              {new Date(securitySettings.lastSecretGeneration).toLocaleString()}
            </p>
          </div>
        </div>
      </div>

      {/* Session Settings */}
      <div className="bg-white rounded-lg shadow p-6">
        <div className="flex items-center space-x-3 mb-4">
          <Clock className="h-5 w-5 text-blue-600" />
          <h2 className="text-lg font-semibold text-gray-900">
            Session Management
          </h2>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Session Timeout (seconds)
            </label>
            <input
              type="number"
              value={securitySettings.sessionTimeout}
              onChange={(e) =>
                handleSettingChange("sessionTimeout", parseInt(e.target.value))
              }
              className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
              min="300"
              max="86400"
            />
            <p className="text-xs text-gray-500 mt-1">
              Current: {Math.floor(securitySettings.sessionTimeout / 60)}{" "}
              minutes
            </p>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Max Login Attempts
            </label>
            <input
              type="number"
              value={securitySettings.maxLoginAttempts}
              onChange={(e) =>
                handleSettingChange(
                  "maxLoginAttempts",
                  parseInt(e.target.value)
                )
              }
              className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
              min="1"
              max="20"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Lockout Duration (seconds)
            </label>
            <input
              type="number"
              value={securitySettings.lockoutDuration}
              onChange={(e) =>
                handleSettingChange("lockoutDuration", parseInt(e.target.value))
              }
              className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
              min="60"
              max="3600"
            />
            <p className="text-xs text-gray-500 mt-1">
              Current: {Math.floor(securitySettings.lockoutDuration / 60)}{" "}
              minutes
            </p>
          </div>
        </div>
      </div>

      {/* Password Policy */}
      <div className="bg-white rounded-lg shadow p-6">
        <div className="flex items-center space-x-3 mb-4">
          <AlertTriangle className="h-5 w-5 text-yellow-600" />
          <h2 className="text-lg font-semibold text-gray-900">
            Password Policy
          </h2>
        </div>

        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <div>
              <label className="text-sm font-medium text-gray-700">
                Require Password Change
              </label>
              <p className="text-sm text-gray-500">
                Force users to change password on next login
              </p>
            </div>
            <label className="relative inline-flex items-center cursor-pointer">
              <input
                type="checkbox"
                checked={securitySettings.requirePasswordChange}
                onChange={(e) =>
                  handleSettingChange("requirePasswordChange", e.target.checked)
                }
                className="sr-only peer"
              />
              <div className="w-11 h-6 bg-gray-200 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-blue-300 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-blue-600"></div>
            </label>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Minimum Password Length
            </label>
            <input
              type="number"
              value={securitySettings.passwordMinLength}
              onChange={(e) =>
                handleSettingChange(
                  "passwordMinLength",
                  parseInt(e.target.value)
                )
              }
              className="w-full max-w-xs px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
              min="6"
              max="50"
            />
          </div>
        </div>
      </div>

      {/* Warning */}
      <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4">
        <div className="flex">
          <AlertTriangle className="h-5 w-5 text-yellow-400 mt-0.5" />
          <div className="ml-3">
            <h3 className="text-sm font-medium text-yellow-800">Important</h3>
            <p className="text-sm text-yellow-700 mt-1">
              Changes to security settings take effect immediately. Ensure all
              users are aware of policy changes to avoid account lockouts.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}

export default SecuritySettings;
