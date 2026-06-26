"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import api from "../../lib/axios";
import { AxiosError } from "axios";

export default function RegisterPage() {
  const router = useRouter();
  const [userName, setUserName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  const handleRegister = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    setMessage("");

    try {
      await api.post("/auth/register", { userName, email, password });

      setMessage("Registration successful! Redirecting to login...");
      setTimeout(() => router.push("/login"), 1500);
    } catch (err: unknown) {
      if (err instanceof AxiosError) {
        setError(err.response?.data ?? err.message ?? "Registration failed");
      } else {
        setError("Something went wrong. Please try again.");
      }
    }
  };

  return (
    <main className="flex min-h-screen items-center justify-center bg-gray-50 p-4">
      <div className="w-full max-w-md bg-white p-8 rounded-xl shadow-lg border border-gray-100">
        <h2 className="text-2xl font-bold text-center text-gray-800 mb-6">Join the Platform</h2>

        {error && <div className="mb-4 p-3 text-sm text-red-600 bg-red-50 rounded-lg">{error}</div>}
        {message && <div className="mb-4 p-3 text-sm text-green-600 bg-green-50 rounded-lg">{message}</div>}

        <form onSubmit={handleRegister} className="space-y-4">
          <input
            type="text" placeholder="Username" required
            value={userName} onChange={(e) => setUserName(e.target.value)}
            className="w-full px-4 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 outline-none text-black"
          />
          <input
            type="email" placeholder="University Email" required
            value={email} onChange={(e) => setEmail(e.target.value)}
            className="w-full px-4 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 outline-none text-black"
          />
          <input
            type="password" placeholder="Password" required
            value={password} onChange={(e) => setPassword(e.target.value)}
            className="w-full px-4 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500 outline-none text-black"
          />
          <button type="submit" className="w-full bg-blue-600 text-white font-semibold py-2 rounded-lg hover:bg-blue-700 transition">
            Register
          </button>
        </form>
        <p className="mt-4 text-center text-sm text-gray-600">
          Already have an account? <Link href="/login" className="text-blue-600 hover:underline">Log in</Link>
        </p>
      </div>
    </main>
  );
}
