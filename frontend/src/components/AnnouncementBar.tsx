"use client";

import { useEffect, useState } from "react";

const MESSAGES = [
  "Free worldwide shipping over $50",
  "Summer sale — up to 30% off select styles",
  "New arrivals dropping every week",
];

export function AnnouncementBar() {
  const [index, setIndex] = useState(0);

  useEffect(() => {
    const id = window.setInterval(() => setIndex((i) => (i + 1) % MESSAGES.length), 4000);
    return () => window.clearInterval(id);
  }, []);

  return (
    <div className="bg-black py-2 text-center text-xs font-medium tracking-wide text-white">
      <p aria-live="polite">{MESSAGES[index]}</p>
    </div>
  );
}
