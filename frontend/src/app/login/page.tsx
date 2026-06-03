"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";

export default function LoginPage() {
  const router = useRouter();
  const [userName, setUserName] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");

    try {
      const response = await fetch("http://localhost:8080/api/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        credentials: "include", // CRITICAL: This allows the browser to accept the cookies!
        body: JSON.stringify({ userName, password }),
      });

      if (!response.ok) {
        const errorData = await response.text();
        throw new Error(errorData || "Invalid credentials");
      }

      // Cookies are now set in the browser. Route to the secure area!
      router.push("/dashboard");
      router.refresh(); // Forces Next.js to re-evaluate the proxy state

    } catch (err: any) {
      setError(err.message);
    }
  };

  return (
    <main className="flex min-h-screen items-center justify-center bg-gray-50 p-4">
      <div className="w-full max-w-md bg-white p-8 rounded-xl shadow-lg border border-gray-100">
        <h2 className="text-2xl font-bold text-center text-gray-800 mb-6">Welcome Back</h2>

        {error && <div className="mb-4 p-3 text-sm text-red-600 bg-red-50 rounded-lg">{error}</div>}

        <form onSubmit={handleLogin} className="space-y-4">
          <input
            type="text" placeholder="Username" required
            value={userName} onChange={(e) => setUserName(e.target.value)}
            className="w-full px-4 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 outline-none text-black"
          />
          <input
            type="password" placeholder="Password" required
            value={password} onChange={(e) => setPassword(e.target.value)}
            className="w-full px-4 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 outline-none text-black"
          />
          <button type="submit" className="w-full bg-gray-900 text-white font-semibold py-2 rounded-lg hover:bg-gray-800 transition">
            Authenticate
          </button>
        </form>
        <p className="mt-4 text-center text-sm text-gray-600">
          Need an account? <Link href="/register" className="text-blue-600 hover:underline">Register here</Link>
        </p>
      </div>
    </main>
  );
}