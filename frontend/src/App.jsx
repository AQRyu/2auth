import { useState } from 'react'

function App() {
  const [count, setCount] = useState(0)

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-600 to-purple-700 flex items-center justify-center">
      <div className="bg-white rounded-xl shadow-2xl p-8 max-w-md w-full mx-4">
        <h1 className="text-3xl font-bold text-gray-800 mb-6 text-center">
          2Auth Dashboard
        </h1>
        <div className="text-center">
          <button
            onClick={() => setCount((count) => count + 1)}
            className="bg-blue-600 hover:bg-blue-700 text-white font-semibold py-2 px-6 rounded-lg transition-colors duration-200 shadow-md"
          >
            Count is {count}
          </button>
          <p className="mt-4 text-gray-600">
            TailwindCSS v4 is working! 🎉
          </p>
        </div>
      </div>
    </div>
  )
}

export default App
