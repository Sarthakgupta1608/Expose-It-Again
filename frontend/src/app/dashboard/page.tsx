"use client";

import { useState, useEffect, useCallback } from "react";
import dynamic from "next/dynamic";

const MapPicker = dynamic(() => import("../../components/MapPicker"), { ssr: false });

// ─── Types ────────────────────────────────────────────────────────────────────
interface PostResponse {
  id: string;
  title: string;
  description: string;
  authorName: string;
  latitude: number;
  longitude: number;
  status: string;
  upvotes: number; // Mapped from the new No Slop backend
  categories?: Category[];
  mediaUrls?: string[];
  createdAt?: string; // Spring Boot Instant returns an ISO-8601 string
}

type FeedMode = "trending" | "nearby" | "filter" | "search";

const CATEGORIES = [
  "INFRASTRUCTURE",
  "SANITATION",
  "WATER_SUPPLY",
  "ELECTRICITY",
  "PUBLIC_TRANSPORT",
  "CORRUPTION",
  "OTHER"
] as const;
type Category = (typeof CATEGORIES)[number];

const CATEGORY_META: Record<Category, { label: string; color: string; bg: string; dot: string }> = {
  INFRASTRUCTURE:   { label: "Infrastructure", color: "text-violet-700",  bg: "bg-violet-100",  dot: "bg-violet-500" },
  SANITATION:       { label: "Sanitation",     color: "text-amber-700",   bg: "bg-amber-100",   dot: "bg-amber-500" },
  WATER_SUPPLY:     { label: "Water Supply",   color: "text-blue-700",    bg: "bg-blue-100",    dot: "bg-blue-500" },
  ELECTRICITY:      { label: "Electricity",    color: "text-yellow-700",  bg: "bg-yellow-100",  dot: "bg-yellow-500" },
  PUBLIC_TRANSPORT: { label: "Transport",      color: "text-emerald-700", bg: "bg-emerald-100", dot: "bg-emerald-500" },
  CORRUPTION:       { label: "Corruption",     color: "text-red-700",     bg: "bg-red-100",     dot: "bg-red-500" },
  OTHER:            { label: "Other",          color: "text-slate-600",   bg: "bg-slate-100",   dot: "bg-slate-400" },
};

// ─── Helpers ──────────────────────────────────────────────────────────────────
function timeAgo(dateString?: string) {
  if (!dateString) return "";
  const diff = Date.now() - new Date(dateString).getTime();
  const m = Math.floor(diff / 60000);
  if (m < 1) return "just now";
  if (m < 60) return `${m}m ago`;
  const h = Math.floor(m / 60);
  if (h < 24) return `${h}h ago`;
  return `${Math.floor(h / 24)}d ago`;
}

// ─── Sub-components ───────────────────────────────────────────────────────────
function CategoryPill({ cat }: { cat: string }) {
  const meta = CATEGORY_META[cat as Category] ?? { label: cat, color: "text-gray-600", bg: "bg-gray-100", dot: "bg-gray-400" };
  return (
    <span className={`inline-flex items-center gap-1.5 text-xs font-semibold px-2.5 py-1 rounded-full ${meta.bg} ${meta.color}`}>
      <span className={`w-1.5 h-1.5 rounded-full ${meta.dot}`} />
      {meta.label}
    </span>
  );
}

function PostCard({ post }: { post: PostResponse }) {
  const [expanded, setExpanded] = useState(false);
  const isLong = post.description.length > 160;

  return (
    <article className="group relative bg-white border border-gray-100 rounded-2xl p-5 hover:border-indigo-200 hover:shadow-lg transition-all duration-200">
      <span className="absolute left-0 top-4 bottom-4 w-1 rounded-r-full bg-indigo-400 opacity-0 group-hover:opacity-100 transition-opacity" />

      <div className="flex items-start justify-between gap-3 mb-2">
        <h3 className="font-bold text-gray-900 text-base leading-snug">{post.title}</h3>
        <div className="flex flex-wrap gap-1 shrink-0">
          <span className="text-[10px] font-bold tracking-wider px-2 py-1 rounded bg-gray-900 text-white uppercase">{post.status}</span>
          {post.categories?.map((c) => <CategoryPill key={c} cat={c} />)}
        </div>
      </div>

      <p className="text-sm text-gray-600 leading-relaxed mb-3">
        {isLong && !expanded ? post.description.slice(0, 160) + "…" : post.description}
        {isLong && (
          <button onClick={() => setExpanded((v) => !v)} className="ml-1 text-indigo-500 font-medium hover:underline text-xs">
            {expanded ? "less" : "more"}
          </button>
        )}
      </p>

      <div className="flex flex-wrap items-center justify-between gap-3 pt-3 border-t border-gray-100">
        <div className="flex items-center gap-3 text-xs text-gray-400">
          <span className="font-mono bg-gray-50 px-2 py-0.5 rounded border border-gray-200">
            {post.latitude?.toFixed(4)}, {post.longitude?.toFixed(4)}
          </span>
          {post.authorName && <span>by <span className="font-medium text-gray-600">{post.authorName}</span></span>}
          {post.createdAt && <span>{timeAgo(post.createdAt)}</span>}
        </div>
        <div className="flex items-center gap-2 shrink-0">
          <span className="font-bold text-indigo-600 tabular-nums bg-indigo-50 px-2 py-1 rounded-md">
            ⇧ {post.upvotes?.toLocaleString() ?? 0}
          </span>
        </div>
      </div>
    </article>
  );
}

// ─── Main Page ────────────────────────────────────────────────────────────────
export default function DashboardPage() {
  const [location, setLocation] = useState({ lat: 30.7699, lon: 76.5756 });

  const [feedMode, setFeedMode] = useState<FeedMode>("trending");
  const [feed, setFeed] = useState<PostResponse[]>([]);
  const [feedLoading, setFeedLoading] = useState(false);
  const [feedError, setFeedError] = useState<string | null>(null);

  const [selectedCategories, setSelectedCategories] = useState<Set<Category>>(new Set());
  const [searchQuery, setSearchQuery] = useState("");

  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [postCategory, setPostCategory] = useState<Category>("OTHER");
  const [initialLikes, setInitialLikes] = useState(0); // Dev backdoor restored
  const [submitting, setSubmitting] = useState(false);

  const [toast, setToast] = useState<{ msg: string; kind: "ok" | "err" } | null>(null);
  const showToast = (msg: string, kind: "ok" | "err" = "ok") => {
    setToast({ msg, kind });
    setTimeout(() => setToast(null), 3500);
  };

  const fetchFeed = useCallback(async () => {
    setFeedLoading(true);
    setFeedError(null);
    try {
      let url = "";
      if (feedMode === "trending") {
        url = `http://localhost:8080/api/posts/trending`;
      } else if (feedMode === "nearby") {
        url = `http://localhost:8080/api/posts/nearby/trending?lat=${location.lat}&lon=${location.lon}`;
      } else if (feedMode === "search") {
        if (!searchQuery.trim()) {
          setFeed([]);
          setFeedLoading(false);
          return;
        }
        url = `http://localhost:8080/api/posts/search?q=${encodeURIComponent(searchQuery)}`;
      } else {
        if (selectedCategories.size === 0) {
          setFeed([]);
          setFeedLoading(false);
          return;
        }
        const params = [...selectedCategories].map((c) => `categories=${c}`).join("&");
        url = `http://localhost:8080/api/posts/filter?${params}`;
      }

      const res = await fetch(url, { credentials: "include" });
      if (!res.ok) throw new Error(`Server returned ${res.status}`);
      const data: PostResponse[] = await res.json();
      setFeed(data);
    } catch (e: unknown) {
      const msg = e instanceof Error ? e.message : "Unknown error";
      setFeedError(msg);
      setFeed([]);
    } finally {
      setFeedLoading(false);
    }
  }, [feedMode, location.lat, location.lon, selectedCategories, searchQuery]);

  useEffect(() => {
    if (feedMode !== "search" || searchQuery.trim() !== "") {
      void fetchFeed();
    }
  }, [fetchFeed, feedMode]);

  const handleCreatePost = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      const payload = {
        title,
        description,
        latitude: location.lat,
        longitude: location.lon,
        categories: [postCategory],
        mediaUrls: [],
        likes: initialLikes // Dev backdoor sent to backend
      };

      const res = await fetch("http://localhost:8080/api/posts", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
        credentials: "include",
      });

      if (!res.ok) {
        const err = await res.text();
        throw new Error(err);
      }

      setTitle("");
      setDescription("");
      setPostCategory("OTHER");
      setInitialLikes(0);
      showToast("Issue Reported Successfully ✓");
      void fetchFeed();
    } catch (e: unknown) {
      showToast(e instanceof Error ? e.message : "Failed to create post", "err");
    } finally {
      setSubmitting(false);
    }
  };

  const toggleCategory = (cat: Category) => {
    setSelectedCategories((prev) => {
      const next = new Set(prev);
      if (next.has(cat)) next.delete(cat);
      else next.add(cat);
      return next;
    });
  };

  return (
    <div className="min-h-screen bg-[#f5f4f0] font-['Instrument_Sans',sans-serif] text-gray-900">
      <style>{`@import url('https://fonts.googleapis.com/css2?family=Instrument+Sans:wght@400;500;600;700&family=Playfair+Display:wght@700&display=swap');`}</style>

      {toast && (
        <div className={`fixed top-5 right-5 z-50 flex items-center gap-3 px-5 py-3 rounded-xl shadow-2xl text-sm font-semibold transition-all ${toast.kind === "ok" ? "bg-emerald-600 text-white" : "bg-red-600 text-white"}`}>
          <span>{toast.kind === "ok" ? "✓" : "✕"}</span>
          {toast.msg}
        </div>
      )}

      <header className="bg-white border-b border-gray-200 px-6 py-4 flex items-center justify-between sticky top-0 z-40 shadow-sm">
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 rounded-lg bg-indigo-600 flex items-center justify-center text-white text-lg font-bold shadow">E</div>
          <span className="font-['Playfair_Display',serif] text-xl font-bold tracking-tight text-gray-900">ExposeIt</span>
          <span className="text-xs font-medium text-gray-400 bg-gray-100 px-2 py-0.5 rounded ml-1">Civic Engine</span>
        </div>
        <div className="flex items-center gap-2 text-xs text-gray-500 font-mono bg-gray-50 border border-gray-200 rounded-lg px-3 py-1.5">
          <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
          {location.lat.toFixed(4)}, {location.lon.toFixed(4)}
        </div>
      </header>

      <main className="max-w-7xl mx-auto px-4 sm:px-6 py-8 flex flex-col lg:flex-row gap-6">

        <aside className="w-full lg:w-[340px] flex flex-col gap-5 shrink-0">
          <section className="bg-white rounded-2xl border border-gray-200 shadow-sm overflow-hidden">
            <div className="px-5 pt-5 pb-3">
              <h2 className="font-semibold text-gray-900 text-sm uppercase tracking-widest mb-1">Incident Location</h2>
              <p className="text-xs text-gray-400">Click map to pinpoint issue</p>
            </div>
            <div className="relative h-52 z-0">
              <MapPicker onLocationSelect={(lat: number, lon: number) => setLocation({ lat, lon })} />
            </div>
            <div className="px-5 py-3 flex justify-between items-center bg-indigo-50 border-t border-indigo-100">
              <span className="text-xs font-medium text-indigo-700">Active pin</span>
              <span className="text-xs font-mono text-indigo-600">{location.lat.toFixed(5)}, {location.lon.toFixed(5)}</span>
            </div>
          </section>

          <section className="bg-white rounded-2xl border border-gray-200 shadow-sm p-5">
            <h2 className="font-semibold text-gray-900 text-sm uppercase tracking-widest mb-4">Report Issue</h2>
            <form onSubmit={handleCreatePost} className="flex flex-col gap-3">
              <input
                required
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                placeholder="E.g., Broken water pipe flooding street"
                className="w-full text-sm px-3 py-2 rounded-lg border border-gray-200 focus:outline-none focus:ring-2 focus:ring-indigo-400 bg-gray-50 placeholder:text-gray-400"
              />
              <textarea
                required
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="Provide detailed information about the issue..."
                rows={3}
                className="w-full text-sm px-3 py-2 rounded-lg border border-gray-200 focus:outline-none focus:ring-2 focus:ring-indigo-400 bg-gray-50 placeholder:text-gray-400 resize-y"
              />

              <div>
                <label className="text-xs font-medium text-gray-500 mb-1 block">Category</label>
                <div className="grid grid-cols-2 gap-2">
                  {CATEGORIES.map((cat) => {
                    const meta = CATEGORY_META[cat];
                    const active = postCategory === cat;
                    return (
                      <button
                        key={cat}
                        type="button"
                        onClick={() => setPostCategory(cat)}
                        className={`text-xs font-semibold px-3 py-2 rounded-lg border transition ${active ? `${meta.bg} ${meta.color} border-transparent ring-2 ring-offset-1 ring-indigo-400` : "bg-white text-gray-500 border-gray-200 hover:border-gray-300"}`}
                      >
                        {meta.label}
                      </button>
                    );
                  })}
                </div>
              </div>

              {/* Dev backdoor: Initial Likes */}
              <div className="flex items-center gap-3 bg-gray-50 rounded-lg border border-gray-200 px-3 py-2">
                <label className="text-xs font-medium text-gray-500 flex-1">Starting upvotes</label>
                <input
                  type="number"
                  min={0}
                  value={initialLikes}
                  onChange={(e) => setInitialLikes(parseInt(e.target.value) || 0)}
                  className="w-20 text-sm text-center px-2 py-1 rounded border border-gray-200 focus:outline-none focus:ring-2 focus:ring-indigo-400 bg-white"
                />
              </div>

              <button
                type="submit"
                disabled={submitting}
                className="mt-2 w-full bg-indigo-600 hover:bg-indigo-700 disabled:bg-indigo-300 text-white text-sm font-semibold py-2.5 rounded-xl transition active:scale-95 shadow-sm"
              >
                {submitting ? "Encrypting & Saving..." : "Submit Report →"}
              </button>
            </form>
          </section>
        </aside>

        <div className="flex-1 flex flex-col gap-5 min-w-0">
          <div className="bg-white rounded-2xl border border-gray-200 shadow-sm p-2 flex gap-1">
            {(
              [
                { mode: "trending" as FeedMode, label: "🌐 Global", hint: "Most upvoted" },
                { mode: "nearby" as FeedMode,   label: "📍 Nearby",  hint: "Your city" },
                { mode: "filter" as FeedMode,   label: "🏷 Filter",  hint: "By issue type" },
                { mode: "search" as FeedMode,   label: "🔍 Search",  hint: "Fuzzy text match" },
              ] as const
            ).map(({ mode, label, hint }) => (
              <button
                key={mode}
                onClick={() => setFeedMode(mode)}
                className={`flex-1 flex flex-col items-center py-2.5 px-3 rounded-xl text-xs font-semibold transition ${feedMode === mode ? "bg-indigo-600 text-white shadow" : "text-gray-500 hover:bg-gray-50"}`}
              >
                <span className="text-sm">{label}</span>
                <span className={`text-[10px] font-normal mt-0.5 ${feedMode === mode ? "text-indigo-200" : "text-gray-400"}`}>{hint}</span>
              </button>
            ))}
          </div>

          {feedMode === "search" && (
            <div className="bg-white rounded-2xl border border-gray-200 shadow-sm px-5 py-4 flex gap-3">
              <input
                type="text"
                placeholder="Search for 'pothole', 'broken lamp'..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                onKeyDown={(e) => e.key === "Enter" && fetchFeed()}
                className="flex-1 text-sm px-4 py-2 rounded-lg border border-gray-200 focus:outline-none focus:ring-2 focus:ring-indigo-400 bg-gray-50"
              />
              <button onClick={() => fetchFeed()} className="bg-indigo-600 text-white px-5 py-2 rounded-lg text-sm font-semibold hover:bg-indigo-700 transition">
                Search
              </button>
            </div>
          )}

          {feedMode === "filter" && (
            <div className="bg-white rounded-2xl border border-gray-200 shadow-sm px-5 py-4">
              <p className="text-xs font-medium text-gray-500 uppercase tracking-widest mb-3">Select categories to filter</p>
              <div className="flex flex-wrap gap-2">
                {CATEGORIES.map((cat) => {
                  const meta = CATEGORY_META[cat];
                  const active = selectedCategories.has(cat);
                  return (
                    <button
                      key={cat}
                      onClick={() => toggleCategory(cat)}
                      className={`inline-flex items-center gap-1.5 text-sm font-semibold px-4 py-2 rounded-full border transition ${active ? `${meta.bg} ${meta.color} border-transparent ring-2 ring-offset-1 ring-indigo-400` : "bg-white text-gray-500 border-gray-200 hover:border-gray-300"}`}
                    >
                      <span className={`w-2 h-2 rounded-full ${meta.dot}`} />
                      {meta.label}
                    </button>
                  );
                })}
              </div>
            </div>
          )}

          <div className="flex items-center justify-between">
            <h2 className="font-['Playfair_Display',serif] text-2xl font-bold text-gray-900">
              {feedMode === "trending" ? "Top Issues" : feedMode === "nearby" ? "Local Reports" : feedMode === "search" ? "Search Results" : "Filtered Issues"}
            </h2>
            <div className="flex items-center gap-3">
              {!feedLoading && <span className="text-xs text-gray-400">{feed.length} result{feed.length !== 1 ? "s" : ""}</span>}
              <button onClick={() => void fetchFeed()} disabled={feedLoading} className="text-xs font-medium text-indigo-600 hover:text-indigo-800 bg-white border border-gray-200 px-3 py-1.5 rounded-lg hover:border-indigo-300 transition disabled:opacity-50">
                {feedLoading ? "Syncing…" : "↺ Sync Data"}
              </button>
            </div>
          </div>

          {feedLoading ? (
            <div className="flex flex-col gap-3">
              {[1, 2, 3].map((i) => (
                <div key={i} className="bg-white rounded-2xl border border-gray-100 p-5 animate-pulse">
                  <div className="h-4 bg-gray-100 rounded w-2/3 mb-3" />
                  <div className="h-3 bg-gray-100 rounded w-full mb-2" />
                  <div className="h-3 bg-gray-100 rounded w-4/5" />
                </div>
              ))}
            </div>
          ) : feedError ? (
            <div className="bg-red-50 border border-red-200 rounded-2xl p-6 text-center">
              <p className="text-red-700 font-semibold text-sm mb-1">API Error</p>
              <p className="text-red-500 text-xs font-mono">{feedError}</p>
              <button onClick={() => void fetchFeed()} className="mt-3 text-xs text-red-600 underline">Retry</button>
            </div>
          ) : feed.length === 0 ? (
            <div className="bg-white border border-dashed border-gray-300 rounded-2xl p-12 text-center">
              <div className="text-4xl mb-3">🗂</div>
              <p className="text-gray-500 font-medium">No records found</p>
              <p className="text-xs text-gray-400 mt-1">Adjust your filters or submit a new report.</p>
            </div>
          ) : (
            <div className="flex flex-col gap-3">
              {feed.map((post) => <PostCard key={post.id} post={post} />)}
            </div>
          )}
        </div>
      </main>
    </div>
  );
}
