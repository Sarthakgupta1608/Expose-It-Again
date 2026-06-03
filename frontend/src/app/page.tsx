import Link from "next/link";

export default function LandingPage() {
  return (
    <main className="flex flex-col items-center justify-center min-h-screen bg-gray-50 text-gray-900">
      <div className="text-center space-y-6 p-8 max-w-2xl">
        <h1 className="text-5xl font-extrabold tracking-tight text-blue-600">
          Student Project Verification
        </h1>
        <p className="text-lg text-gray-600">
          The central hub to submit, review, and verify academic projects securely.
        </p>
        <div className="flex justify-center gap-4 pt-4">
          <Link
            href="/login"
            className="px-6 py-3 text-white bg-blue-600 rounded-lg hover:bg-blue-700 transition font-medium"
          >
            Sign In
          </Link>
          <Link
            href="/register"
            className="px-6 py-3 text-blue-600 bg-white border border-blue-600 rounded-lg hover:bg-blue-50 transition font-medium"
          >
            Create Account
          </Link>
        </div>
      </div>
    </main>
  );
}
